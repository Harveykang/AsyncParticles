package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
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

import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines.*;

public class VkSepQCompParticleRenderer implements IParticleRenderer {
	private static final int WORKGROUP_SIZE = 64;
	private static final int GROUP_RECORD_INTS = 3;
	private static final int SOURCE_SLOT_COUNT = 3;
	private static final int SUBMIT_SLOT_COUNT = 2;
	private static final long GPU_WAIT_TIMEOUT_NS = 5_000_000_000L;

	// VK backend
	private final VulkanDevice vkBackend;
	private final VkDevice device;
	private final VulkanQueue computeQueue;
	private final int queueFamilyIndex;
	private final boolean sameSubmitQueue;

	// source buffers (pooled, persistently mapped)
	private final SourceSlot[] sourceSlots = new SourceSlot[SOURCE_SLOT_COUNT];
	private ByteBuffer mappedBuffer;
	private int processingSrcIdx;
	private int renderSrcIdx = -1;
	private long sourceGenerationCounter;

	// particle state
	private int particleLimit;
	private final List<SingleQuadParticle> pendingAppends = new ReferenceArrayList<>();
	private int appendCount;
	private boolean shouldSkip = true;

	// target buffer (compute output -> vertex input)
	private final SubmitSlot[] submitSlots = new SubmitSlot[SUBMIT_SLOT_COUNT];

	// compute pipeline
	private long pipelineLayout;
	private long pipeline;
	private long descriptorSetLayout;
	private long descriptorPool;

	// command resources
	private long commandPool;
	private final VkCommandBuffer[] vkCommandBuffer = new VkCommandBuffer[SUBMIT_SLOT_COUNT];

	// Timeline semaphore (VK 1.2+)
	private long timelineSemaphore;
	private long timelineCounter;
	private final boolean useTimelineSemaphore;

	// multi-draw / compute result
	private boolean computed;
	private ComputeResult computeResult;
	private boolean shouldWait;

	public VkSepQCompParticleRenderer(int particleLimit) {
		this.particleLimit = particleLimit;
		vkBackend = (VulkanDevice) RenderSystem.getDevice().backend;
		device = vkBackend.vkDevice();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i] = new SourceSlot();
		}
		for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
			submitSlots[i] = new SubmitSlot();
		}

		useTimelineSemaphore = BackendCaps.vkCaps.isTimelineSemaphoreSupported();
		if (useTimelineSemaphore && false) { // Same queue is faster
			computeQueue = vkBackend.computeQueue();
			sameSubmitQueue = computeQueue == vkBackend.graphicsQueue();
		} else {
			computeQueue = vkBackend.graphicsQueue();
			sameSubmitQueue = true;
		}

		queueFamilyIndex = computeQueue.queueFamilyIndex();

		int raw = 2 * Math.max(particleLimit, AsyncParticlesConfig.MIN_PARTICLE_LIMIT) * RAW_PARTICLE.getVertexSize();
		createSourceBuffers(raw);
		createIndirectionBuffers(particleLimit * 4);

		long proceed = 4L * particleLimit * IDENTITY_PARTICLE.getVertexSize();
		for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
			submitSlots[i].createTargetBuffer(proceed);
		}

		createComputePipeline();
		createCommandResources();
		for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
			submitSlots[i].updateIndirectionDescriptor();
		}
	}

	/* buffer creation */

	private void createSourceBuffers(int size) {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i].createBuffer(size);
		}
	}

	private void createIndirectionBuffers(int size) {
		for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
			submitSlots[i].createIndirectionBuffer(size);
		}
	}

	private int findMemType(int typeFilter, int props) {
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkPhysicalDeviceMemoryProperties mp = VkPhysicalDeviceMemoryProperties.calloc(stack);
			VK10.vkGetPhysicalDeviceMemoryProperties(vkBackend.vkDevice().getPhysicalDevice(), mp);
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
			ps.get(0).type(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(3 * SUBMIT_SLOT_COUNT);
			VkDescriptorPoolCreateInfo dpci = VkDescriptorPoolCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
				.maxSets(SUBMIT_SLOT_COUNT).pPoolSizes(ps);
			LongBuffer pdp = stack.mallocLong(1);
			VK10.vkCreateDescriptorPool(device, dpci, null, pdp);
			descriptorPool = pdp.get(0);
			LongBuffer layouts = stack.mallocLong(SUBMIT_SLOT_COUNT);
			for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
				layouts.put(i, descriptorSetLayout);
			}
			VkDescriptorSetAllocateInfo ai2 = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
				.descriptorPool(descriptorPool).pSetLayouts(layouts);
			LongBuffer pds2 = stack.mallocLong(SUBMIT_SLOT_COUNT);
			VK10.vkAllocateDescriptorSets(device, ai2, pds2);
			for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
				submitSlots[i].descriptorSet = pds2.get(i);
			}
			if (sameSubmitQueue) {
				VkCommandPoolCreateInfo cpci = VkCommandPoolCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
					.queueFamilyIndex(queueFamilyIndex).flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
				LongBuffer pc = stack.mallocLong(1);
				VK10.vkCreateCommandPool(device, cpci, null, pc);
				commandPool = pc.get(0);
				VkCommandBufferAllocateInfo cai = VkCommandBufferAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
					.commandPool(commandPool).level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(SUBMIT_SLOT_COUNT);
				PointerBuffer pcb = stack.mallocPointer(SUBMIT_SLOT_COUNT);
				VK10.vkAllocateCommandBuffers(device, cai, pcb);
				for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
					vkCommandBuffer[i] = new VkCommandBuffer(pcb.get(i), device);
				}
			} else if (useTimelineSemaphore) {
				VkSemaphoreTypeCreateInfo sci = VkSemaphoreTypeCreateInfo.calloc(stack).sType$Default()
					.semaphoreType(VK12.VK_SEMAPHORE_TYPE_TIMELINE).initialValue(0);
				VkSemaphoreCreateInfo semCI = VkSemaphoreCreateInfo.calloc(stack).sType$Default()
					.pNext(sci);
				LongBuffer ts = stack.mallocLong(1);
				VK12.vkCreateSemaphore(device, semCI, null, ts);
				timelineSemaphore = ts.get(0);
			} else {
				throw new AssertionError();
			}
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
		return shouldSkip;
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
		appendCount = ac;

		// Reset for next frame
		setComputeStatus(tc + ac == 0);
		if (shouldSkip) {
			renderSrcIdx = -1;
		} else {
			renderSrcIdx = pi;
			processingSrcIdx = acquireSourceSlot(pi);
		}
		mappedBuffer = null;
	}

	private void extractAppendParticles(Vec3 cam, List<SingleQuadParticle> pending, List<LayerBatch> batches) {
		int ac = pending.size();
		int vertexSize = RAW_PARTICLE.getVertexSize();
		int pi = processingSrcIdx;
		SourceSlot source = sourceSlots[pi];
		int offset = particleLimit * vertexSize;
		final double cx = cam.x, cy = cam.y, cz = cam.z;
		long bufAddr = MemoryUtil.memAddress(mappedBuffer) + offset;

		var lm = new Reference2ReferenceOpenHashMap<SingleQuadParticle.Layer, List<SingleQuadParticle>>();
		for (var p : pending) lm.computeIfAbsent(p.getLayer(), k -> new ReferenceArrayList<>(ac / 2)).add(p);

		int baseCount = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			long addr = stack.nmalloc(vertexSize);
			for (var e : lm.entrySet()) {
				var list = e.getValue();
				LayerBatch batch = null;
				for (var b : batches) {
					if (b.layer == e.getKey()) {
						batch = b;
						break;
					}
				}
				int layerAppend = list.size();
				if (batch == null) {
					batch = new LayerBatch(e.getKey());
					batch.tickOffset = source.tickCount;
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				int bw = baseCount * vertexSize;
				for (var p : list) {
					var gpuParticle = (GpuParticleAddon) p;

					long ptr = addr;
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

					gpuParticle.asyncparticles$postTick(addr);

					MemoryUtil.memCopy(addr, bufAddr + bw, vertexSize);
					bw += vertexSize;
				}
				baseCount += layerAppend;
			}
		}
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
				if (baseCount > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit);
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
		if (baseCount > particleLimit) {
			throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit);
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
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();
		int sourceIdx = renderSrcIdx;
		if (sourceIdx < 0) {
			setComputeStatus(true);
			computed = true;
			return;
		}
		SourceSlot source = sourceSlots[sourceIdx];
		if (!source.prepared) {
			source.buildLayout();
		}
		if (source.workgroupCount == 0) {
			setComputeStatus(true);
		} else {
			int submitSlot = currentSubmitSlot();
			SubmitSlot submit = submitSlots[submitSlot];
			timelineCounter++;
			submit.waitReadyForWrite();
			if (!submit.isPreparedFor(source)) {
				prepareSubmitSlot(source, submit);
			}
			computeResult = submit.preparedComputeResult;
			dispatch(source, submit, submitSlot, camera, partialTicks);
		}
		computed = true;
	}

	private void setComputeStatus(boolean shouldSkip) {
		this.shouldSkip = shouldSkip;
		computeResult = null;
		shouldWait = false;
	}

	private int currentSubmitSlot() {
		if (sameSubmitQueue) {
			return 0;
		}
		return (int) (vkBackend.createCommandEncoder().currentSubmitIndex % SUBMIT_SLOT_COUNT);
	}

	private void prepareSubmitSlot(SourceSlot source, SubmitSlot submit) {
		int needBytes = source.totalCount * 4 * IDENTITY_PARTICLE.getVertexSize();
		submit.ensureTargetSize(needBytes);
		submit.ensureIndirectionSize(source.groupRecords.length * Integer.BYTES);
		submit.uploadGroupRecords(source.groupRecords);
		submit.updateDescriptor(source);
		submit.preparedSourceGeneration = source.generation;
		submit.preparedComputeResult = ComputeResult.of(submit.targetBuffer, source.totalCount, source.slices);
	}

	private int acquireSourceSlot(int reservedSrcIdx) {
		int firstCandidate = -1;
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			if (i == reservedSrcIdx) {
				continue;
			}
			if (firstCandidate == -1) {
				firstCandidate = i;
			}
			if (sourceSlots[i].isReady()) {
				return i;
			}
		}
		if (firstCandidate == -1) {
			throw new IllegalStateException("No source slot available");
		}
		sourceSlots[firstCandidate].waitReady();
		return firstCandidate;
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

	private void dispatch(SourceSlot source, SubmitSlot submitSlotResources, int submitSlot, Camera camera, float partialTicks) {
		VulkanCommandEncoder commandEncoder = sameSubmitQueue ? vkBackend.createCommandEncoder() : null;
		VkCommandBuffer cb = sameSubmitQueue ? commandEncoder.allocateAndBeginTransientCommandBuffer() : vkCommandBuffer[submitSlot];

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			if (!sameSubmitQueue) {
				VkCommandBufferBeginInfo bi = VkCommandBufferBeginInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
					.flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
				VK10.vkBeginCommandBuffer(cb, bi);
			}
			VK10.vkCmdBindPipeline(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
			VK10.vkCmdBindDescriptorSets(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, stack.longs(submitSlotResources.descriptorSet), null);

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

			if (sameSubmitQueue) {
				VkMemoryBarrier.Buffer mb = VkMemoryBarrier.calloc(1, stack)
					.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_BARRIER).srcAccessMask(VK10.VK_ACCESS_SHADER_WRITE_BIT)
					.dstAccessMask(VK10.VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);
				VK10.vkCmdPipelineBarrier(cb, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, 0, mb, null, null);
			}
			VK10.vkEndCommandBuffer(cb);
		}

		if (sameSubmitQueue) {
			commandEncoder.execute(cb);
			source.lastSubmitIndex = commandEncoder.currentSubmitIndex;
		} else {
			if (useTimelineSemaphore) {
				try (VulkanQueue.Submission submit = computeQueue.beginSubmit()) {
					submit.executeCommands(cb);
					submit.signalSemaphore(timelineSemaphore, timelineCounter, KHRSynchronization2.VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR);
				}
				source.lastTimelineValue = timelineCounter;
				submitSlotResources.lastComputeTimelineValue = timelineCounter;
			} else {
				throw new AssertionError();
			}
			shouldWait = true;
		}
	}

	@Override
	public ComputeResult awaitCompute() {
		if (!computed) {
			return null;
		}
		if (sameSubmitQueue || !shouldWait) {
			return computeResult;
		}
		if (useTimelineSemaphore) {
			VulkanCommandEncoder commandEncoder = vkBackend.createCommandEncoder();
			commandEncoder.waitSemaphore(timelineSemaphore, timelineCounter, KHRSynchronization2.VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT_KHR);
			int submitSlot = currentSubmitSlot();
			submitSlots[submitSlot].lastGraphicsSubmitIndex = commandEncoder.currentSubmitIndex;
			submitSlots[submitSlot].lastComputeTimelineValue = 0L;
		} else {
			throw new AssertionError();
		}
		shouldWait = false;
		return computeResult;
	}

	@Override
	public void resize(int particleLimit) {
		if (computeResult != null) {
			throw new IllegalStateException("Cannot resize while rendering!");
		}
		waitForAllSourceSlots();
		VK10.vkDeviceWaitIdle(device);
		this.particleLimit = particleLimit;
		int raw = 2 * particleLimit * RAW_PARTICLE.getVertexSize();
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.destroyBuffer();
		}
		createSourceBuffers(raw);
		long proceed = 4L * particleLimit * IDENTITY_PARTICLE.getVertexSize();
		for (int i = 0; i < SUBMIT_SLOT_COUNT; i++) {
			submitSlots[i].createTargetBuffer(proceed);
			submitSlots[i].clearBusy();
		}
		computeResult = null;
		shouldWait = false;
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getComputeLayers() {
		if (renderSrcIdx < 0) {
			return List.of();
		}
		var batches = sourceSlots[renderSrcIdx].layerBatches;
		var result = new ArrayList<SingleQuadParticle.Layer>(batches.size());
		for (var b : batches) result.add(b.layer);
		return result;
	}

	@Override
	public void reset() {
		VK10.vkDeviceWaitIdle(device);
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.reset();
		}
		for (SubmitSlot submitSlot : submitSlots) {
			submitSlot.clearBusy();
			submitSlot.clearPrepared();
		}
		pendingAppends.clear();
		appendCount = 0;
		processingSrcIdx = 0;
		renderSrcIdx = -1;
		mappedBuffer = null;
		setComputeStatus(true);
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
		for (SubmitSlot submitSlot : submitSlots) {
			submitSlot.destroy();
		}
		VK10.vkDestroyPipeline(device, pipeline, null);
		VK10.vkDestroyPipelineLayout(device, pipelineLayout, null);
		VK10.vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
		VK10.vkDestroyDescriptorPool(device, descriptorPool, null);
		if (sameSubmitQueue) {
			for (VkCommandBuffer commandBuffer : vkCommandBuffer) {
				VK10.vkFreeCommandBuffers(device, commandPool, commandBuffer);
			}
			VK10.vkDestroyCommandPool(device, commandPool, null);
		}
		if (timelineSemaphore != 0L) {
			VK12.vkDestroySemaphore(device, timelineSemaphore, null);
		}
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
		private long lastTimelineValue;
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
			VK10.vkDestroyBuffer(device, srcBuf, null);
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
			int baseCount = 0;
			for (int i = 0; i < sz; i++) {
				LayerBatch batch = batches.get(i);
				int layerTotal = batch.tickCount + batch.appendCount;
				this.slices[i] = new ComputeResult.ParticleSlice(batch.layer, baseCount, layerTotal);
				recordPos = writeSegmentGroups(records, recordPos, batch.tickOffset, baseCount, batch.tickCount);
				recordPos = writeSegmentGroups(records, recordPos,
					particleLimit + batch.appendOffset,
					baseCount + batch.tickCount,
					batch.appendCount);
				baseCount += layerTotal;
			}
			this.totalCount = baseCount;
			this.workgroupCount = groups;
			this.groupRecords = records;
			this.prepared = true;
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
			lastTimelineValue = 0L;
			prepared = false;
		}

		private boolean isReady() {
			if (sameSubmitQueue) {
				return lastSubmitIndex == -1L || vkBackend.createCommandEncoder().awaitSubmitCompletion(lastSubmitIndex, 0L);
			}
			if (lastTimelineValue == 0L) {
				return true;
			}
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				LongBuffer value = stack.mallocLong(1);
				int result = VK12.vkGetSemaphoreCounterValue(device, timelineSemaphore, value);
				if (result != VK10.VK_SUCCESS) {
					throw new IllegalStateException("Failed to query compute timeline semaphore: " + result);
				}
				return value.get(0) >= lastTimelineValue;
			}
		}

		private void waitReady() {
			if (sameSubmitQueue) {
				if (lastSubmitIndex != -1L) {
					if (!vkBackend.createCommandEncoder().awaitSubmitCompletion(lastSubmitIndex, GPU_WAIT_TIMEOUT_NS)) {
						throw new IllegalStateException("Timeout waiting for GPU particle source slot submit: " + lastSubmitIndex);
					}
					lastSubmitIndex = -1L;
				}
				return;
			}
			if (lastTimelineValue == 0L) {
				return;
			}
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack).sType$Default();
				waitInfo.pSemaphores(stack.longs(timelineSemaphore));
				waitInfo.pValues(stack.longs(lastTimelineValue));
				waitInfo.semaphoreCount(1);
				int result = VK12.vkWaitSemaphores(device, waitInfo, GPU_WAIT_TIMEOUT_NS);
				if (result != VK10.VK_SUCCESS) {
					throw new IllegalStateException("Timeout waiting for GPU particle source slot timeline: " + lastTimelineValue + " (" + result + ")");
				}
				lastTimelineValue = 0L;
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
		private long lastComputeTimelineValue;
		private long lastGraphicsSubmitIndex = -1L;
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
			return preparedSourceGeneration == source.generation && preparedComputeResult != null;
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
					.usage(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
				int graphicsFamily = vkBackend.graphicsQueue().queueFamilyIndex();
				int computeFamily = computeQueue.queueFamilyIndex();
				if (graphicsFamily != computeFamily) {
					bufCI.sharingMode(VK10.VK_SHARING_MODE_CONCURRENT)
						.pQueueFamilyIndices(stack.ints(computeFamily, graphicsFamily));
				} else {
					bufCI.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
				}
				VmaAllocationCreateInfo allocCI = VmaAllocationCreateInfo.calloc(stack);
				allocCI.usage(8);
				LongBuffer bufPtr = stack.callocLong(1);
				PointerBuffer allocPtr = stack.callocPointer(1);
				Vma.vmaCreateBuffer(vkBackend.vma(), bufCI, allocCI, bufPtr, allocPtr, null);
				long vkBuf = bufPtr.get(0);
				long alloc = allocPtr.get(0);
				targetBuffer = new VulkanGpuBuffer(vkBuf, GpuBuffer.USAGE_VERTEX, size) {
					boolean closed;

					@Override
					public void destroy() {
						Vma.vmaDestroyBuffer(vkBackend.vma(), vkBuf, alloc);
						closed = true;
					}

					@Override
					public boolean isClosed() {
						return closed;
					}

					@Override
					public void close() {
						if (!isClosed()) {
							closed = true;
							destroy();
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

		private void waitReadyForWrite() {
			if (sameSubmitQueue) {
				return;
			}
			if (lastGraphicsSubmitIndex != -1L) {
				if (!vkBackend.createCommandEncoder().awaitSubmitCompletion(lastGraphicsSubmitIndex, GPU_WAIT_TIMEOUT_NS)) {
					throw new IllegalStateException("Timeout waiting for GPU particle submit slot graphics submit: " + lastGraphicsSubmitIndex);
				}
				clearBusy();
				return;
			}
			if (lastComputeTimelineValue == 0L) {
				return;
			}
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack).sType$Default();
				waitInfo.pSemaphores(stack.longs(timelineSemaphore));
				waitInfo.pValues(stack.longs(lastComputeTimelineValue));
				waitInfo.semaphoreCount(1);
				int result = VK12.vkWaitSemaphores(device, waitInfo, GPU_WAIT_TIMEOUT_NS);
				if (result != VK10.VK_SUCCESS) {
					throw new IllegalStateException("Timeout waiting for GPU particle submit slot timeline: " + lastComputeTimelineValue + " (" + result + ")");
				}
				lastComputeTimelineValue = 0L;
			}
		}

		private void clearBusy() {
			lastComputeTimelineValue = 0L;
			lastGraphicsSubmitIndex = -1L;
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
			VK10.vkDestroyBuffer(device, indBuf, null);
			if (targetBuffer != null) {
				targetBuffer.close();
			}
		}
	}
}
