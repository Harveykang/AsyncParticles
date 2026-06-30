package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines.*;

public class VkCompParticleRenderer implements IParticleRenderer {
	private static final int WORKGROUP_SIZE = 64;
	private static final int GROUP_RECORD_INTS = 3;
	private static final int SOURCE_SLOT_COUNT = 3;
	private static final long GPU_WAIT_TIMEOUT_NS = 5_000_000_000L;

	// VK backend
	private final VulkanDevice vkBackend;
	private final VkDevice device;

	// source buffers (pooled, persistently mapped)
	private final SourceSlot[] sourceSlots = new SourceSlot[SOURCE_SLOT_COUNT];
	private ByteBuffer mappedBuffer;
	private int processingSrcIdx;
	private int renderSrcIdx = -1;
	private long sourceGenerationCounter;

	// particle state
	private int particleLimit;
	private final List<SingleQuadParticle> pendingAppends = new ReferenceArrayList<>();

	// compute pipeline
	private final SubmitSlot submitSlot;
	private long pipelineLayout;
	private long pipeline;
	private long descriptorSetLayout;
	private long descriptorPool;

	// multi-draw / compute result
	private boolean computed;

	public VkCompParticleRenderer(int particleLimit) {
		this.particleLimit = particleLimit;
		vkBackend = (VulkanDevice) RenderSystem.getDevice().backend;
		device = vkBackend.vkDevice();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i] = new SourceSlot();
		}
		submitSlot = new SubmitSlot();

		int raw = 2 * Math.max(particleLimit, AsyncParticlesConfig.MIN_PARTICLE_LIMIT) * RAW_PARTICLE.getVertexSize();
		createSourceBuffers(raw);
		createIndirectionBuffers(particleLimit * 4);

		long proceed = 4L * particleLimit * IDENTITY_PARTICLE.getVertexSize();
		submitSlot.createTargetBuffer(proceed);

		createComputePipeline();
		createCommandResources();
		submitSlot.updateIndirectionDescriptor();
	}

	/* buffer creation */

	private void createSourceBuffers(int size) {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i].createBuffer(size);
		}
	}

	private void createIndirectionBuffers(int size) {
		submitSlot.createIndirectionBuffer(size);
	}

	private int findMemType(int typeFilter, int props) {
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkPhysicalDeviceMemoryProperties mp = VkPhysicalDeviceMemoryProperties.calloc(stack);
			VK10.vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), mp);
			for (int i = 0; i < mp.memoryTypeCount(); i++) {
				if ((typeFilter & (1 << i)) != 0 && (mp.memoryTypes(i).propertyFlags() & props) == props) {
					return i;
				}
			}
		}
		throw new RuntimeException("No suitable Vulkan memory type");
	}

	/* pipeline */

	private void createComputePipeline() {
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkDescriptorSetLayoutBinding.Buffer b = VkDescriptorSetLayoutBinding.calloc(3, stack);
			for (int i = 0; i < 3; i++) {
				b.get(i).binding(i).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT);
			}

			VkDescriptorSetLayoutCreateInfo dci = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(b);
			LongBuffer pdsl = stack.mallocLong(1);
			VK10.vkCreateDescriptorSetLayout(device, dci, null, pdsl);
			descriptorSetLayout = pdsl.get(0);

			VkPushConstantRange.Buffer pcRange = VkPushConstantRange.calloc(1, stack);
			pcRange.get(0).stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT).offset(0).size(40);

			VkPipelineLayoutCreateInfo plci = VkPipelineLayoutCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
				.pSetLayouts(stack.longs(descriptorSetLayout)).pPushConstantRanges(pcRange);
			LongBuffer ppl = stack.mallocLong(1);
			VK10.vkCreatePipelineLayout(device, plci, null, ppl);
			pipelineLayout = ppl.get(0);

			byte[] spirv = loadSpirv();
			ByteBuffer spirvBuf = stack.malloc(spirv.length);
			spirvBuf.put(spirv).flip();
			VkShaderModuleCreateInfo smci = VkShaderModuleCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
				.pCode(spirvBuf);
			LongBuffer psm = stack.mallocLong(1);
			VK10.vkCreateShaderModule(device, smci, null, psm);
			long sm = psm.get(0);

			VkPipelineShaderStageCreateInfo sci = VkPipelineShaderStageCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
				.stage(VK10.VK_SHADER_STAGE_COMPUTE_BIT).module(sm).pName(stack.UTF8("main"));
			VkComputePipelineCreateInfo.Buffer cpci = VkComputePipelineCreateInfo.calloc(1, stack).sType(VK10.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
				.stage(sci).layout(pipelineLayout);
			LongBuffer pp = stack.mallocLong(1);
			VK10.vkCreateComputePipelines(device, 0L, cpci, null, pp);
			pipeline = pp.get(0);
			VK10.vkDestroyShaderModule(device, sm, null);
		}
	}

	private byte[] loadSpirv() {
		try (InputStream is = getClass().getResourceAsStream("/assets/asyncparticles/particle_gpu_acceleration/vk_particle_compute.spv")) {
			if (is == null) {
				throw new RuntimeException("particle_compute.spv not found — compile .comp with: glslc -fshader-stage=compute vk_particle_compute.comp -o particle_compute.spv --target-env=vulkan1.0");
			}
			return is.readAllBytes();
		} catch (Exception e) {
			throw new RuntimeException("Failed to load SPIR-V", e);
		}
	}

	/* command resources */

	private void createCommandResources() {
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkDescriptorPoolSize.Buffer ps = VkDescriptorPoolSize.calloc(1, stack);
			ps.get(0).type(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(3);
			VkDescriptorPoolCreateInfo dpci = VkDescriptorPoolCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
				.maxSets(1).pPoolSizes(ps);
			LongBuffer pdp = stack.mallocLong(1);
			VK10.vkCreateDescriptorPool(device, dpci, null, pdp);
			descriptorPool = pdp.get(0);
			LongBuffer layouts = stack.mallocLong(1).put(0, descriptorSetLayout);
			VkDescriptorSetAllocateInfo ai2 = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
				.descriptorPool(descriptorPool).pSetLayouts(layouts);
			LongBuffer pds2 = stack.mallocLong(1);
			VK10.vkAllocateDescriptorSets(device, ai2, pds2);
			submitSlot.descriptorSet = pds2.get(0);
		}
	}

	@Override
	public void beginFrame(float deltaPartialTick) {
		computed = false;
	}

	@Override
	public boolean isMapped() {
		return mappedBuffer != null;
	}

	@Override
	public boolean isShouldSkip() {
		return renderSrcIdx == -1;
	}

	@Override
	public void prepareBuffer() {
		mappedBuffer = sourceSlots[processingSrcIdx].mapped.position(0).limit(2 * particleLimit * RAW_PARTICLE.getVertexSize());
	}

	@Override
	public void flushBufferAndSwap(Vec3 prevGpuCamPos) {
		int pi = processingSrcIdx;
		SourceSlot source = sourceSlots[pi];
		int tc = source.tickCount;
		int ac = pendingAppends.size();
		if (ac > 0) {
			if (!isMapped()) {
				source.camPosition = prevGpuCamPos;
				prepareBuffer();
			}
			extractAppendParticles(prevGpuCamPos, pendingAppends, source.layerBatches);
		}

		// Reset for next frame
		if (tc + ac == 0) {
			renderSrcIdx = -1;
		} else {
			renderSrcIdx = pi;
			processingSrcIdx = acquireSourceSlot(pi);
		}
		mappedBuffer = null;
	}

	private void extractAppendParticles(Vec3 prevGpuCamPos, List<SingleQuadParticle> pending, List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		final double cx = prevGpuCamPos.x;
		final double cy = prevGpuCamPos.y;
		final double cz = prevGpuCamPos.z;
		int pi = processingSrcIdx;
		SourceSlot sourceSlot = sourceSlots[pi];
		int offset = particleLimit * vertexSize;
		long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offset;

		Map<SingleQuadParticle.Layer, List<SingleQuadParticle>> layerMap = new Reference2ReferenceOpenHashMap<>();
		for (int i = 0, pendingSize = Math.min(particleLimit, pending.size()); i < pendingSize; i++) {
			SingleQuadParticle p = pending.get(i);
			layerMap.computeIfAbsent(p.getLayer(), _ -> new ReferenceArrayList<>(appendCount / 2)).add(p);
		}

		int baseCount = 0;
		int baseWrite = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, List<SingleQuadParticle>> entry : layerMap.entrySet()) {
				List<SingleQuadParticle> list = entry.getValue();
				LayerBatch batch = null;
				for (LayerBatch b : batches) {
					if (b.layer == entry.getKey()) {
						batch = b;
						break;
					}
				}
				int layerAppend = list.size();
				if (batch == null) {
					batch = new LayerBatch(entry.getKey());
					batch.tickOffset = sourceSlot.tickCount;
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				for (SingleQuadParticle particle : list) {
					GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getXo() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getYo() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZo() - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getX() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getY() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZ() - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(0f));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(1f));
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU1());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV1());
					ptr += 4L;

					int oColor = gpuParticle.asyncparticles$getOColor();
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, oColor);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getColor(oColor));
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getLightCoords(0f));
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getORoll());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getRoll());

					gpuParticle.asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + baseWrite, vertexSize);
					baseWrite += vertexSize;
				}
				baseCount += layerAppend;
			}
		}
	}

	private int acquireSourceSlot(int idx) {
		int firstCandidate = (idx + 1) % SOURCE_SLOT_COUNT;
		sourceSlots[firstCandidate].waitReady();
		return firstCandidate;
	}

	@Override
	public <T extends Collection<SingleQuadParticle>> void tick(Vec3 camPos, Map<SingleQuadParticle.Layer, T> particles) {
		if (!isMapped()) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingSrcIdx;
		SourceSlot source = sourceSlots[pi];
		source.prepared = false;
		source.camPosition = camPos;

		final double cx = camPos.x;
		final double cy = camPos.y;
		final double cz = camPos.z;
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = RAW_PARTICLE.getVertexSize();

		List<LayerBatch> batches = source.layerBatches;
		batches.clear();

		int position = 0;
		int baseCount = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, T> entry : particles.entrySet()) {
				Collection<SingleQuadParticle> collection = entry.getValue();
				if (baseCount + collection.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit
						+ ", baseCount: " + baseCount
						+ ", collection size: " + collection.size());
				}

				int tickCount = 0;
				for (SingleQuadParticle particle : collection) {
					GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;
					if (!particle.isAlive() || !gpuParticle.asyncparticles$shouldRender()) {
						continue;
					}

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getXo() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getYo() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZo() - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getX() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getY() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZ() - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(0f));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(1f));
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU1());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV1());
					ptr += 4L;

					int oColor = gpuParticle.asyncparticles$getOColor();
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, oColor);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getColor(oColor));
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getLightCoords(0f));
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getORoll());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getRoll());

					gpuParticle.asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + position, vertexSize);
					position += vertexSize;
					tickCount++;
				}

				if (tickCount > 0) {
					LayerBatch batch = new LayerBatch(entry.getKey());
					batch.tickOffset = baseCount;
					batch.tickCount = tickCount;
					batches.add(batch);
				}
				baseCount += tickCount;
			}
		}
		source.tickCount = baseCount;

		// Clear to prepare pending list
		pendingAppends.clear();
	}

	@Override
	public void append(Vec3 cam, SingleQuadParticle particle) {
		if (particle.isAlive() && ((GpuParticleAddon) particle).asyncparticles$shouldRender()) {
			pendingAppends.add(particle);
		}
	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (computed) {
			return;
		}
		if (renderSrcIdx == -1) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
//		assert renderSrcIdx >= 0;
		RenderSystem.assertOnRenderThread();
		SourceSlot source = sourceSlots[renderSrcIdx];
		if (!source.prepared) {
			source.buildLayout();
		}
		if (source.workgroupCount == 0) {
			renderSrcIdx = -1;
			computed = true;
			return;
		}
		if (!submitSlot.isPreparedFor(source)) {
			submitSlot.prepare(source);
		}
		dispatch(source, camera, partialTicks);
		computed = true;
	}

	private void dispatch(SourceSlot source, Camera camera, float partialTicks) {
		VulkanCommandEncoder commandEncoder = vkBackend.createCommandEncoder();
		VkCommandBuffer cb = commandEncoder.commandBuffer();

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VK10.vkCmdBindPipeline(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
			VK10.vkCmdBindDescriptorSets(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, stack.longs(submitSlot.descriptorSet), null);

			float leftX = camera.leftVector().x();
			float leftY = camera.leftVector().y();
			float leftZ = camera.leftVector().z();
			float upX = camera.upVector().x();
			float upY = camera.upVector().y();
			float upZ = camera.upVector().z();
			float camX = (float) (source.camPosition.x - camera.position().x);
			float camY = (float) (source.camPosition.y - camera.position().y);
			float camZ = (float) (source.camPosition.z - camera.position().z);

//			assert workgroupCount > 0;
			ByteBuffer pc = stack.malloc(40);
			pc.putFloat(partialTicks);
			pc.putFloat(leftX);
			pc.putFloat(leftY);
			pc.putFloat(leftZ);
			pc.putFloat(upX);
			pc.putFloat(upY);
			pc.putFloat(upZ);
			pc.putFloat(camX);
			pc.putFloat(camY);
			pc.putFloat(camZ);
			pc.flip();
			VK10.vkCmdPushConstants(cb, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, pc);
			VK10.vkCmdDispatch(cb, source.workgroupCount, 1, 1);

			VkMemoryBarrier.Buffer mb = VkMemoryBarrier.calloc(1, stack)
				.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_BARRIER).srcAccessMask(VK10.VK_ACCESS_SHADER_WRITE_BIT)
				.dstAccessMask(VK10.VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);
			VK10.vkCmdPipelineBarrier(cb, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, 0, mb, null, null);
		}

		source.lastSubmitIndex = commandEncoder.currentSubmitIndex;
	}

	@Override
	public ComputeResult awaitCompute() {
		return renderSrcIdx == -1 ? null : submitSlot.preparedComputeResult;
	}

	@Override
	public void resize(int particleLimit) {
		if (particleLimit != this.particleLimit) {
			waitForAllSourceSlots();
			this.particleLimit = particleLimit;
			int raw = 2 * particleLimit * RAW_PARTICLE.getVertexSize();
			for (SourceSlot sourceSlot : sourceSlots) {
				sourceSlot.destroyBuffer();
			}
			createSourceBuffers(raw);
			long proceed = 4L * particleLimit * IDENTITY_PARTICLE.getVertexSize();
			submitSlot.createTargetBuffer(proceed);
		}
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getComputeLayers() {
		if (renderSrcIdx < 0) {
			return List.of();
		}
		List<LayerBatch> batches = sourceSlots[renderSrcIdx].layerBatches;
		ArrayList<SingleQuadParticle.Layer> result = new ArrayList<>(batches.size());
		for (LayerBatch b : batches) {
			result.add(b.layer);
		}
		return result;
	}

	@Override
	public void reset() {
		waitForAllSourceSlots();
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.reset();
		}
		submitSlot.clearPrepared();
		pendingAppends.clear();
		processingSrcIdx = 0;
		renderSrcIdx = -1;
		mappedBuffer = null;
	}

	private void waitForAllSourceSlots() {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i].waitReady();
		}
	}

	@Override
	public void close() {
		VK10.vkDeviceWaitIdle(device);
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.destroyBuffer();
		}
		submitSlot.destroy();
		VK10.vkDestroyPipeline(device, pipeline, null);
		VK10.vkDestroyPipelineLayout(device, pipelineLayout, null);
		VK10.vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
		VK10.vkDestroyDescriptorPool(device, descriptorPool, null);
	}

	private final class SourceSlot {
		private static final ComputeResult.ParticleSlice[] EMPTY_SLICES = new ComputeResult.ParticleSlice[0];
		private static final int[] EMPTY_INT_ARR = new int[0];
		private long srcBuf;
		private long srcMem;
		private ByteBuffer mapped;
		private final List<LayerBatch> layerBatches = new ReferenceArrayList<>();
		private int tickCount;
		private Vec3 camPosition = Vec3.ZERO;
		private long lastSubmitIndex = -1L;
		private long generation;
		private int totalCount;
		private int workgroupCount;
		private int[] groupRecords = EMPTY_INT_ARR;
		private ComputeResult.ParticleSlice[] slices = EMPTY_SLICES;
		private boolean prepared;

		private void createBuffer(int size) {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkBufferCreateInfo ci = VkBufferCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(size).usage(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT).sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
				LongBuffer pb = stack.mallocLong(1);
				VK10.vkCreateBuffer(device, ci, null, pb);
				srcBuf = pb.get(0);

				VkMemoryRequirements mr = VkMemoryRequirements.malloc(stack);
				VK10.vkGetBufferMemoryRequirements(device, srcBuf, mr);
				int mt = findMemType(mr.memoryTypeBits(), VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

				VkMemoryAllocateInfo ai = VkMemoryAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(mr.size()).memoryTypeIndex(mt);
				LongBuffer pm = stack.mallocLong(1);
				VK10.vkAllocateMemory(device, ai, null, pm);
				srcMem = pm.get(0);
				VK10.vkBindBufferMemory(device, srcBuf, srcMem, 0);

				PointerBuffer pp = stack.mallocPointer(1);
				VK10.vkMapMemory(device, srcMem, 0, size, 0, pp);
				mapped = pp.getByteBuffer(0, size);
			}
		}

		private void destroyBuffer() {
			VK10.vkUnmapMemory(device, srcMem);
			VK10.vkFreeMemory(device, srcMem, null);
			srcMem = VK10.VK_NULL_HANDLE;
			VK10.vkDestroyBuffer(device, srcBuf, null);
			srcBuf = VK10.VK_NULL_HANDLE;
		}

		private void buildLayout() {
			this.generation = ++sourceGenerationCounter;

			List<LayerBatch> batches = this.layerBatches;
			int sz = batches.size();
			this.slices = new ComputeResult.ParticleSlice[sz];

			int groups = 0;
			for (LayerBatch batch : batches) {
				groups += (batch.tickCount + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE;
				groups += (batch.appendCount + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE;
			}
			int[] records = new int[groups * GROUP_RECORD_INTS];
			int recordPos = 0;
			int dstBase = 0;
			for (int i = 0; i < sz; i++) {
				LayerBatch batch = batches.get(i);
				int layerTotal = batch.tickCount + batch.appendCount;
				this.slices[i] = new ComputeResult.ParticleSlice(batch.layer, dstBase, layerTotal);
				recordPos = writeSegmentGroups(records, recordPos, batch.tickOffset, dstBase, batch.tickCount);
				recordPos = writeSegmentGroups(records, recordPos,
					particleLimit + batch.appendOffset,
					dstBase + batch.tickCount,
					batch.appendCount);
				dstBase += layerTotal;
			}
			this.totalCount = dstBase;
			this.workgroupCount = groups;
			this.groupRecords = records;
			this.prepared = true;
		}

		private int writeSegmentGroups(int[] records, int recordPos, int srcBase, int dstBase, int count) {
			for (int localBase = 0; localBase < count; localBase += WORKGROUP_SIZE) {
				int groupCount = Math.min(WORKGROUP_SIZE, count - localBase);
				records[recordPos++] = srcBase + localBase;
				records[recordPos++] = dstBase + localBase;
				records[recordPos++] = groupCount;
			}
			return recordPos;
		}

		private void reset() {
			totalCount = 0;
			workgroupCount = 0;
			groupRecords = EMPTY_INT_ARR;
			slices = EMPTY_SLICES;
			tickCount = 0;
			camPosition = Vec3.ZERO;
			layerBatches.clear();
			lastSubmitIndex = -1L;
			prepared = false;
		}

		private void waitReady() {
			if (lastSubmitIndex != -1L) {
				if (!vkBackend.createCommandEncoder().awaitSubmitCompletion(lastSubmitIndex, GPU_WAIT_TIMEOUT_NS)) {
					throw new IllegalStateException("Timeout waiting for GPU particle source slot submit: " + lastSubmitIndex);
				}
				lastSubmitIndex = -1L;
			}
		}
	}

	private final class SubmitSlot {
		private VulkanGpuBuffer targetBuffer;
		private long descriptorSet;
		private long indBuf;
		private long indMem;
		private ByteBuffer indMapped;
		private int indSize;
		private long preparedSourceGeneration = -1L;
		private ComputeResult preparedComputeResult;

		private void createIndirectionBuffer(int size) {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkBufferCreateInfo ci = VkBufferCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(size).usage(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT).sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
				LongBuffer pb = stack.mallocLong(1);
				VK10.vkCreateBuffer(device, ci, null, pb);
				indBuf = pb.get(0);

				VkMemoryRequirements mr = VkMemoryRequirements.malloc(stack);
				VK10.vkGetBufferMemoryRequirements(device, indBuf, mr);
				int mt = findMemType(mr.memoryTypeBits(), VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

				VkMemoryAllocateInfo ai = VkMemoryAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(mr.size()).memoryTypeIndex(mt);
				LongBuffer pm = stack.mallocLong(1);
				VK10.vkAllocateMemory(device, ai, null, pm);
				indMem = pm.get(0);
				VK10.vkBindBufferMemory(device, indBuf, indMem, 0);

				PointerBuffer pp = stack.mallocPointer(1);
				VK10.vkMapMemory(device, indMem, 0, size, 0, pp);
				indMapped = pp.getByteBuffer(0, size);
				indSize = size;
			}
		}

		private void ensureIndirectionSize(int bytes) {
			if (bytes <= indSize) {
				return;
			}
			VK10.vkUnmapMemory(device, indMem);
			VK10.vkDestroyBuffer(device, indBuf, null);
			VK10.vkFreeMemory(device, indMem, null);
			createIndirectionBuffer(bytes);
			updateIndirectionDescriptor();
			clearPrepared();
		}

		private boolean isPreparedFor(SourceSlot source) {
			return preparedSourceGeneration == source.generation;
		}

		private void uploadGroupRecords(int[] records) {
			indMapped.asIntBuffer().position(0).put(records);
		}

		private void updateIndirectionDescriptor() {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkDescriptorBufferInfo.Buffer bi = VkDescriptorBufferInfo.calloc(1, stack);
				bi.get(0).buffer(indBuf).offset(0).range(VK10.VK_WHOLE_SIZE);
				VkWriteDescriptorSet.Buffer ws = VkWriteDescriptorSet.calloc(1, stack);
				ws.get(0).sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(descriptorSet).dstBinding(2)
					.descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).pBufferInfo(bi);
				VK10.vkUpdateDescriptorSets(device, ws, null);
			}
		}

		private void createTargetBuffer(long size) {
			if (targetBuffer != null) {
				targetBuffer.close();
			}
			clearPrepared();
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkBufferCreateInfo bufCI = VkBufferCreateInfo.calloc(stack).sType$Default()
					.size(size)
					.usage(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
					.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
				VmaAllocationCreateInfo allocCI = VmaAllocationCreateInfo.calloc(stack)
					.usage(Vma.VMA_MEMORY_USAGE_GPU_ONLY)
					.preferredFlags(VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
				LongBuffer bufPtr = stack.callocLong(1);
				PointerBuffer allocPtr = stack.callocPointer(1);
				Vma.vmaCreateBuffer(vkBackend.vma(), bufCI, allocCI, bufPtr, allocPtr, null);
				long vkBuf = bufPtr.get(0);
				long alloc = allocPtr.get(0);
				targetBuffer = new VulkanGpuBuffer(vkBuf, GpuBuffer.USAGE_VERTEX, size) {
					boolean closed;

					@Override
					public void destroy() {
						Vma.vmaDestroyBuffer(vkBackend.vma(), vkBuffer(), alloc);
					}

					@Override
					public boolean isClosed() {
						return this.closed;
					}

					@Override
					public void close() {
						if (!isClosed()) {
							this.closed = true;
							vkBackend.createCommandEncoder().queueForDestroy(this);
						}
					}

					@Override
					public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
						throw new UnsupportedOperationException();
					}
				};
			}
		}

		private void ensureTargetSize(long bytes) {
			if (bytes > targetBuffer.size()) {
				createTargetBuffer(bytes);
			}
		}

		private void clearPrepared() {
			preparedSourceGeneration = -1L;
			preparedComputeResult = null;
		}

		private void updateDescriptor(SourceSlot source) {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkDescriptorBufferInfo.Buffer bi = VkDescriptorBufferInfo.calloc(2, stack);
				bi.get(0).buffer(source.srcBuf).offset(0).range(VK10.VK_WHOLE_SIZE);
				bi.get(1).buffer(targetBuffer.vkBuffer()).offset(0).range(VK10.VK_WHOLE_SIZE);
				VkWriteDescriptorSet.Buffer ws = VkWriteDescriptorSet.calloc(2, stack);
				ws.get(0).sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(descriptorSet).dstBinding(0)
					.descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).pBufferInfo(bi.position(0));
				ws.get(1).sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(descriptorSet).dstBinding(1)
					.descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).pBufferInfo(bi.position(1));
				VK10.vkUpdateDescriptorSets(device, ws, null);
			}
		}

		private void destroy() {
			VK10.vkUnmapMemory(device, indMem);
			VK10.vkFreeMemory(device, indMem, null);
			indMem = VK10.VK_NULL_HANDLE;
			VK10.vkDestroyBuffer(device, indBuf, null);
			indBuf = VK10.VK_NULL_HANDLE;
			if (targetBuffer != null) {
				targetBuffer.close();
				targetBuffer = null;
			}
		}

		public void prepare(SourceSlot source) {
			int needBytes = source.totalCount * 4 * IDENTITY_PARTICLE.getVertexSize();
			this.ensureTargetSize(needBytes);
			this.ensureIndirectionSize(source.groupRecords.length * Integer.BYTES);
			this.uploadGroupRecords(source.groupRecords);
			this.updateDescriptor(source);
			this.preparedSourceGeneration = source.generation;
			this.preparedComputeResult = ComputeResult.of(this.targetBuffer, source.totalCount, source.slices);
		}
	}
}
