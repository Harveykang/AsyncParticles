package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vulkanmod.RendererAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.*;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod.AccessorBuffer;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod.AccessorVkGlFramebuffer;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.index.AutoIndexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleVertexFormats.RAW_PARTICLE;

public class VkCompParticleRenderer<C> implements IParticleRenderer {
	public static final int WORKGROUP_SIZE = 64;
	private static final int GROUP_RECORD_INTS = 3;
	private static final int PUSH_CONSTANT_BYTES = 44;
	private static final int DESCRIPTOR_SET_LAYOUT_SIZE = 3;
	private static final int SOURCE_SLOT_COUNT = 3;
	private static final long GPU_WAIT_TIMEOUT_NS = 5_000_000_000L;
	private static final int PARTICLE_VERTEX_SIZE = DefaultVertexFormat.PARTICLE.getVertexSize();

	// VK backend
	private final VkDevice device;
	private final IVkParticleBatchStrategy<C> batchStrategy;
	private final C renderInfo;

	// source buffers (pooled, persistently mapped)
	@SuppressWarnings("unchecked")
	private final SourceSlot[] sourceSlots = (SourceSlot[]) new VkCompParticleRenderer<?>.SourceSlot[SOURCE_SLOT_COUNT];
	private ByteBuffer mappedBuffer;
	private int processingSrcIdx;
	private int renderSrcIdx = -1;
	private long sourceGenerationCounter;

	// particle state
	private int particleLimit;
	private final List<TextureSheetParticle> pendingAppends = new ReferenceArrayList<>();

	// compute pipeline
	private SubmitSlot[] submitSlots;
	private int renderSubmitIdx = -1;
	private long pipelineLayout;
	private long pipeline;
	private long descriptorSetLayout;
	private long descriptorPool;

	// multi-draw / compute result
	private IComputeReuseCondition reuseCondition;

	@SuppressWarnings("unchecked")
	public VkCompParticleRenderer(int particleLimit, IVkParticleBatchStrategy<C> batchStrategy) {
		this.batchStrategy = batchStrategy;
		this.renderInfo = batchStrategy.createVkRenderInfo();
		this.particleLimit = particleLimit;
		device = DeviceManager.vkDevice;
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sourceSlots[i] = new SourceSlot();
		}
		submitSlots = (SubmitSlot[]) new VkCompParticleRenderer<?>.SubmitSlot[Renderer.getFramesNum()];
		for (int i = 0; i < submitSlots.length; i++) {
			submitSlots[i] = new SubmitSlot();
		}

		int raw = 2 * Math.max(particleLimit, AsyncParticlesConfig.MIN_PARTICLE_LIMIT) * RAW_PARTICLE.getVertexSize();
		createSourceBuffers(raw);

		int proceed = 4 * particleLimit * PARTICLE_VERTEX_SIZE;
		createTargetBuffers(proceed);
		createIndirectionBuffers(particleLimit * 4);

		createComputePipeline();
		createCommandResources();
	}

	/* buffer creation */

	private void createSourceBuffers(int size) {
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.createBuffer(size);
		}
	}

	@SuppressWarnings("unchecked")
	private void createSubmitSlots(int size, int particleLimit) {
		submitSlots = (SubmitSlot[]) new VkCompParticleRenderer<?>.SubmitSlot[Renderer.getFramesNum()];
		for (int i = 0; i < submitSlots.length; i++) {
			submitSlots[i] = new SubmitSlot();
		}
		createTargetBuffers(size);
		createIndirectionBuffers(particleLimit * 4);
		createCommandResources();
	}

	private void createTargetBuffers(int size) {
		for (SubmitSlot submitSlot : submitSlots) {
			submitSlot.createTargetBuffer(size);
		}
	}

	private void createIndirectionBuffers(int size) {
		for (SubmitSlot slot : submitSlots) {
			slot.createIndirectionBuffer(size);
		}
	}

	private int findMemType(int typeFilter, int props) {
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkPhysicalDeviceMemoryProperties mp = VkPhysicalDeviceMemoryProperties.malloc(stack);
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
			VkDescriptorSetLayoutBinding.Buffer b = VkDescriptorSetLayoutBinding.calloc(DESCRIPTOR_SET_LAYOUT_SIZE, stack);
			for (int i = 0; i < DESCRIPTOR_SET_LAYOUT_SIZE; i++) {
				b.get(i).binding(i).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT);
			}

			VkDescriptorSetLayoutCreateInfo dci = VkDescriptorSetLayoutCreateInfo.calloc(stack)
				.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
				.pBindings(b);
			LongBuffer pdsl = stack.mallocLong(1);
			VK10.vkCreateDescriptorSetLayout(device, dci, null, pdsl);
			descriptorSetLayout = pdsl.get(0);

			VkPushConstantRange.Buffer pcRange = VkPushConstantRange.calloc(1, stack);
			pcRange.get(0).stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT).offset(0).size(PUSH_CONSTANT_BYTES);

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
		if (descriptorPool != VK10.VK_NULL_HANDLE) {
			destroyDescriptorResources();
		}
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			VkDescriptorPoolSize.Buffer ps = VkDescriptorPoolSize.calloc(1, stack);
			ps.get(0).type(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(DESCRIPTOR_SET_LAYOUT_SIZE * submitSlots.length);
			VkDescriptorPoolCreateInfo dpci = VkDescriptorPoolCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
				.maxSets(submitSlots.length).pPoolSizes(ps);
			LongBuffer pdp = stack.mallocLong(1);
			VK10.vkCreateDescriptorPool(device, dpci, null, pdp);
			descriptorPool = pdp.get(0);

			LongBuffer layouts = stack.mallocLong(submitSlots.length);
			for (int i = 0; i < submitSlots.length; i++) {
				layouts.put(i, descriptorSetLayout);
			}
			VkDescriptorSetAllocateInfo ai2 = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
				.descriptorPool(descriptorPool).pSetLayouts(layouts);
			LongBuffer pds2 = stack.mallocLong(submitSlots.length);
			VK10.vkAllocateDescriptorSets(device, ai2, pds2);
			for (int i = 0; i < submitSlots.length; i++) {
				submitSlots[i].descriptorSet = pds2.get(i);
			}
		}
	}

	private void destroyDescriptorResources() {
		VK10.vkDestroyDescriptorPool(device, descriptorPool, null);
		descriptorPool = VK10.VK_NULL_HANDLE;
	}

	@Override
	public void beginFrame(float deltaPartialTick) {
		reuseCondition = null;
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
			extractAppendParticles(pendingAppends, source.layerBatches);
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

	private void extractAppendParticles(List<TextureSheetParticle> pending, List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = ParticleVertexFormats.RAW_PARTICLE.getVertexSize();
		int pi = processingSrcIdx;
		SourceSlot sourceSlot = sourceSlots[pi];
		int offset = particleLimit * vertexSize;
		long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offset;

		Map<IParticleBatchKey, List<TextureSheetParticle>> layerMap = new Object2ReferenceOpenHashMap<>();
		for (int i = 0, pendingSize = Math.min(particleLimit, appendCount); i < pendingSize; i++) {
			TextureSheetParticle p = pending.get(i);
			layerMap.computeIfAbsent(batchStrategy.keyFor(p), a -> new ReferenceArrayList<>()).add(p);
		}

		int baseCount = 0;
		int baseWrite = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			long address = stack.nmalloc(vertexSize);
			for (Map.Entry<IParticleBatchKey, List<TextureSheetParticle>> entry : layerMap.entrySet()) {
				IParticleBatchKey key = entry.getKey();
				List<TextureSheetParticle> list = entry.getValue();
				LayerBatch batch = null;
				for (LayerBatch b : batches) {
					if (b.key.equals(key)) {
						batch = b;
						break;
					}
				}
				int layerAppend = list.size();
				if (batch == null) {
					batch = batchStrategy.createBatch(key, sourceSlot.camPosition);
					batch.tickOffset = sourceSlot.tickCount;
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				batchStrategy.extractLayer(renderInfo, list, batch, address, bufferAddress + baseWrite, sourceSlot.camPosition, true);
				baseWrite += layerAppend * vertexSize;
				baseCount += layerAppend;
			}
		}
	}

	private int acquireSourceSlot(int idx) {
		int newIdx = (idx + 1) % SOURCE_SLOT_COUNT;
		sourceSlots[newIdx].waitReady();
		return newIdx;
	}

	private int acquireSubmitSlot(int idx) {
		return (idx + 1) % submitSlots.length;
	}

	@Override
	public <T extends Collection<TextureSheetParticle>> void tick(Vec3 camPos, Map<ParticleRenderType, T> particles) {
		if (!isMapped()) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingSrcIdx;
		SourceSlot source = sourceSlots[pi];
		source.prepared = false;
		source.camPosition = camPos;

		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = RAW_PARTICLE.getVertexSize();

		List<LayerBatch> batches = source.layerBatches;
		batches.clear();

		int position = 0;
		int baseCount = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<IParticleBatchKey, Collection<TextureSheetParticle>> entry : batchStrategy.groupParticles(particles).entrySet()) {
				Collection<TextureSheetParticle> collection = entry.getValue();
				if (baseCount + collection.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit
						+ ", baseCount: " + baseCount
						+ ", collection size: " + collection.size());
				}

				LayerBatch batch = batchStrategy.createBatch(entry.getKey(), camPos);
				int tickCount = batchStrategy.extractLayer(renderInfo, collection, batch, address, bufferAddress + position, camPos, false);
				position += tickCount * vertexSize;

				if (tickCount > 0) {
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
	public void append(Vec3 cam, TextureSheetParticle particle) {
		if (particle.isAlive() && IParticleRenderer.shouldRenderParticle(((GpuParticleAddon) particle), cam)) {
			pendingAppends.add(particle);
		}
	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (reuseCondition != null && reuseCondition.matches(camera, partialTicks)) {
			return;
		}
		if (!Renderer.isRecording()) {
			reuseCondition = null;
			renderSrcIdx = -1;
			return;
		}
		if (isShouldSkip()) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
//		assert renderSrcIdx >= 0;
		ThreadUtil.assertOnMainThread();
		SourceSlot source = sourceSlots[renderSrcIdx];
		if (!source.prepared) {
			source.buildLayout();
		}
		if (source.workgroupCount == 0) {
			reuseCondition = null;
			return;
		}
		IComputeReuseCondition condition = batchStrategy.prepareView(renderInfo, source.layerBatches, camera, partialTicks);
		if (condition == null) {
			reuseCondition = null;
			return;
		}
		SubmitSlot submit = submitSlots[renderSubmitIdx = acquireSubmitSlot(renderSubmitIdx)];
		if (!submit.isPreparedFor(source)) {
			submit.waitReady();
			submit.prepare(source);
		}
		dispatch(source, submit, camera, partialTicks);
		reuseCondition = condition;
	}

	private void dispatch(SourceSlot source, SubmitSlot submit, Camera camera, float partialTicks) {
		Renderer renderer = Renderer.getInstance();
		VkCommandBuffer cb = Renderer.getCommandBuffer();
		long frame = ((RendererAddon) renderer).asyncparticles$getActualFrame();
		Framebuffer boundFramebuffer = renderer.getBoundFramebuffer();
		RenderPass boundRenderPass = renderer.getBoundRenderPass();
		VkGlFramebuffer framebuffer = VkGlFramebuffer.getBoundFramebuffer();
		if (framebuffer != null && (framebuffer.getFramebuffer() != boundFramebuffer || boundRenderPass == null)) {
			framebuffer = null;
		}

		// vkCmdDispatch and vkCmdPipelineBarrier cannot be called inside an active render pass.
		// VulkanMod keeps the render pass active for the entire frame, so we must end it
		// before issuing compute commands and restore it afterward.
		renderer.endRenderPass();

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			// VulkanMod has waited for uses at least getFramesNum() frames ago.
			// Multiple dispatches can rotate back to this target before that wait covers it.
			if (submit.lastFrameUsed >= 0 && frame - submit.lastFrameUsed < Renderer.getFramesNum()) {
				VkBufferMemoryBarrier.Buffer targetReuseBarrier = VkBufferMemoryBarrier.calloc(1, stack)
					.sType(VK10.VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
					.srcAccessMask(VK10.VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT | VK10.VK_ACCESS_SHADER_WRITE_BIT)
					.dstAccessMask(VK10.VK_ACCESS_SHADER_WRITE_BIT)
					.buffer(submit.targetBuffer.getId())
					.offset(0)
					.size(VK10.VK_WHOLE_SIZE);
				VK10.vkCmdPipelineBarrier(cb,
					VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT | VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
					VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
					0,
					null,
					targetReuseBarrier,
					null);
			}

			VK10.vkCmdBindPipeline(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
			VK10.vkCmdBindDescriptorSets(cb, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, stack.longs(submit.descriptorSet), null);

			float leftX = camera.getLeftVector().x();
			float leftY = camera.getLeftVector().y();
			float leftZ = camera.getLeftVector().z();
			float upX = camera.getUpVector().x();
			float upY = camera.getUpVector().y();
			float upZ = camera.getUpVector().z();
			ByteBuffer pc = stack.malloc(PUSH_CONSTANT_BYTES);
			pc.putFloat(partialTicks);
			pc.putFloat(leftX);
			pc.putFloat(leftY);
			pc.putFloat(leftZ);
			pc.putFloat(upX);
			pc.putFloat(upY);
			pc.putFloat(upZ);
			pc.position(0);
			batchStrategy.dispatchVkBatches(renderInfo, pipelineLayout, pc, source.layerBatches, source.camPosition, camera, partialTicks, source.workgroupCount);

			VkBufferMemoryBarrier.Buffer bb = VkBufferMemoryBarrier.calloc(1, stack)
				.sType(VK10.VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
				.srcAccessMask(VK10.VK_ACCESS_SHADER_WRITE_BIT)
				.dstAccessMask(VK10.VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT)
				.buffer(submit.targetBuffer.getId())
				.offset(0)
				.size(VK10.VK_WHOLE_SIZE);
			VK10.vkCmdPipelineBarrier(cb,
				VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
				VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
				0,
				null,
				bb,
				null);
		}

		// Off-screen passes use LOAD/STORE. Resume without beginRendering(), which
		// flips the viewport and resets scissor/culling after ImmPtl has configured them.
		if (framebuffer != null) {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				boundFramebuffer.beginRenderPass(cb, boundRenderPass, stack);
			}
			renderer.setBoundFramebuffer(boundFramebuffer);
			AccessorVkGlFramebuffer.setBoundFramebuffer(framebuffer);
		} else {
			renderer.getMainPass().rebindMainTarget();
		}

		source.lastFrameUsed = frame;
		submit.lastFrameUsed = frame;
	}

	@Override
	public ParticleLayout awaitCompute() {
		return reuseCondition == null || isShouldSkip() || renderSubmitIdx == -1 ? null : submitSlots[renderSubmitIdx].preparedParticleLayout;
	}

	@Override
	public void render(ParticleRenderType renderType) {
		if (reuseCondition == null) {
			return;
		}
		if (renderSubmitIdx == -1) {
			throw new IllegalStateException("No compute result to render!");
		}
		SubmitSlot submit = submitSlots[renderSubmitIdx];
		ParticleLayout particleLayout = submit.preparedParticleLayout;
		if (particleLayout == null) {
			return;
		}
		RenderSystem.assertOnRenderThread();
		ParticleLayout.ParticleSlice particleSlice = particleLayout.slices().get(batchStrategy.keyFor(renderType));
		if (particleSlice == null) {
			return;
		}

		ShaderInstance shader = RenderSystem.getShader();
		Objects.requireNonNull(shader);

		IParticleRenderer.prepareShader(shader);
		shader.apply();

		Drawer drawer = Renderer.getDrawer();
		int vertexCount = particleSlice.vertexCount();
		AutoIndexBuffer quadsIndexBuffer = drawer.getAutoIndexBuffer(VertexFormat.Mode.QUADS, vertexCount);
		quadsIndexBuffer.checkCapacity(vertexCount);
		Buffer vertexBuffer = submit.targetBuffer;

		((AccessorBuffer) vertexBuffer).setOffset((long) particleSlice.vertexOffset() * PARTICLE_VERTEX_SIZE);
		drawer.drawIndexed(vertexBuffer, quadsIndexBuffer.getIndexBuffer(), particleSlice.indexCount());
//		submit.lastFrameUsed = ((RendererAddon) Renderer.getInstance()).asyncparticles$getActualFrame();
//		((AccessorBuffer) vertexBuffer).setOffset(0L);

		shader.clear();
	}

	@Override
	public void resize(int particleLimit) {
		waitForAllSubmissions();
		int oldLimit = this.particleLimit;
		this.particleLimit = particleLimit;

		int raw = 2 * particleLimit * RAW_PARTICLE.getVertexSize();
		for (int i = 0; i < sourceSlots.length; i++) {
			// The slot being rendered this tick still feeds the next compute();
			// migrate its contents. Others are fully rewritten by the next tick().
			if (i == renderSrcIdx) {
				sourceSlots[i].recreateBufferPreservingContents(raw, oldLimit, particleLimit);
			} else {
				sourceSlots[i].destroy();
				sourceSlots[i].createBuffer(raw);
			}
		}

		for (SubmitSlot submitSlot : submitSlots) {
			submitSlot.destroy();
		}
		int proceed = 4 * particleLimit * PARTICLE_VERTEX_SIZE;
		createSubmitSlots(proceed, particleLimit);
		renderSubmitIdx = -1;
		reuseCondition = null;
		mappedBuffer = null;
	}

	@Override
	public Collection<ParticleRenderType> getComputeLayers() {
		if (renderSrcIdx < 0) {
			return List.of();
		}
		List<LayerBatch> batches = sourceSlots[renderSrcIdx].layerBatches;
		Set<ParticleRenderType> result = new ObjectLinkedOpenHashSet<>();
		for (LayerBatch b : batches) {
			result.add(b.key.layer());
		}
		return result;
	}

	@Override
	public void reset() {
		waitForAllSubmissions();
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.reset();
		}

		resetSubmitSlots();

		pendingAppends.clear();
		processingSrcIdx = 0;
		renderSrcIdx = -1;
		renderSubmitIdx = -1;
		reuseCondition = null;
		mappedBuffer = null;
	}

	private void resetSubmitSlots() {
		if (submitSlots.length == Renderer.getFramesNum()) {
			for (SubmitSlot slot : submitSlots) {
				slot.reset();
			}
		} else {
			for (SubmitSlot slot : submitSlots) {
				slot.destroy();
			}

			int proceed = 4 * particleLimit * PARTICLE_VERTEX_SIZE;
			createSubmitSlots(proceed, particleLimit);
		}
	}

	private void waitForAllSubmissions() {
		Vulkan.waitIdle(); // Port from 26.2, as a workaround we wait idle for 26.1
	}

	@Override
	public void close() {
		VK10.vkDeviceWaitIdle(device);
		for (SourceSlot sourceSlot : sourceSlots) {
			sourceSlot.destroy();
		}
		for (SubmitSlot slot : submitSlots) {
			slot.destroy();
		}
		VK10.vkDestroyPipeline(device, pipeline, null);
		VK10.vkDestroyPipelineLayout(device, pipelineLayout, null);
		VK10.vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
		destroyDescriptorResources();
	}

	private final class SourceSlot {
		private static final int[] EMPTY_RECORDS = new int[0];
		public long lastFrameUsed;
		private long srcBuf;
		private long srcMem;
		private ByteBuffer mapped;
		private final List<LayerBatch> layerBatches = new ReferenceArrayList<>();
		private int tickCount;
		private Vec3 camPosition = Vec3.ZERO;
		private long generation;
		private ParticleLayout particleLayout;
		private int workgroupCount;
		private int[] groupRecords = EMPTY_RECORDS;
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

		private void destroy() {
			VK10.vkUnmapMemory(device, srcMem);
			VK10.vkDestroyBuffer(device, srcBuf, null);
			srcBuf = VK10.VK_NULL_HANDLE;
			VK10.vkFreeMemory(device, srcMem, null);
			srcMem = VK10.VK_NULL_HANDLE;
			mapped = null;
		}

		private void recreateBufferPreservingContents(int newSize, int oldLimit, int newLimit) {
			long oldBuf = srcBuf;
			long oldMem = srcMem;
			ByteBuffer oldMapped = mapped;

			createBuffer(newSize);

			int vertexSize = RAW_PARTICLE.getVertexSize();
			long regionBytes = (long) Math.min(oldLimit, newLimit) * vertexSize;
			long newAddress = MemoryUtil.memAddress(mapped);
			long oldAddress = MemoryUtil.memAddress(oldMapped);
			MemoryUtil.memCopy(oldAddress, newAddress, regionBytes);
			MemoryUtil.memCopy(
				oldAddress + (long) oldLimit * vertexSize,
				newAddress + (long) newLimit * vertexSize,
				regionBytes);

			VK10.vkUnmapMemory(device, oldMem);
			VK10.vkDestroyBuffer(device, oldBuf, null);
			VK10.vkFreeMemory(device, oldMem, null);

			prepared = false;
		}

		private void buildLayout() {
			this.generation = ++sourceGenerationCounter;
			VkLayoutResult layoutResult = batchStrategy.buildVkLayout(layerBatches, particleLimit);
			particleLayout = layoutResult.particleLayout();
			workgroupCount = layoutResult.workgroupCount();
			int[] records = new int[workgroupCount * GROUP_RECORD_INTS];
			int recordPos = 0;
			for (LayerBatch batch : layerBatches) {
				recordPos = writeSegmentGroups(records, recordPos, batch.tickOffset, batch.targetOffset, batch.tickCount);
				recordPos = writeSegmentGroups(records, recordPos,
					particleLimit + batch.appendOffset, batch.targetOffset + batch.tickCount, batch.appendCount);
			}
			groupRecords = records;
			this.prepared = true;
		}

		private static int writeSegmentGroups(int[] records, int recordPos, int srcBase, int dstBase, int count) {
			for (int localBase = 0; localBase < count; localBase += WORKGROUP_SIZE) {
				int groupCount = Math.min(WORKGROUP_SIZE, count - localBase);
				records[recordPos++] = srcBase + localBase;
				records[recordPos++] = dstBase + localBase;
				records[recordPos++] = groupCount;
			}
			return recordPos;
		}

		private void reset() {
			waitReady();
			particleLayout = null;
			workgroupCount = 0;
			groupRecords = EMPTY_RECORDS;
			tickCount = 0;
			camPosition = Vec3.ZERO;
			layerBatches.clear();
			prepared = false;
		}

		private void waitReady() {
			if (lastFrameUsed != -1) {
				long currentFrame = ((RendererAddon) Renderer.getInstance()).asyncparticles$getActualFrame();
				long diff = currentFrame - lastFrameUsed;
				// Check if we need to wait
				if (diff > 0 && diff < Renderer.getFramesNum()) {
					long fence = ((RendererAddon) Renderer.getInstance()).asyncparticles$getInFlightFence(lastFrameUsed);
					Synchronization.waitFence(fence);
				}
				lastFrameUsed = -1;
			}
		}
	}

	private final class SubmitSlot {
		private Buffer targetBuffer;
		private long lastFrameUsed = -1L;
		private long indBuf;
		private long indMem;
		private ByteBuffer indMapped;
		private int indSize;
		private long descriptorSet;
		private long preparedSourceGeneration = -1L;
		private ParticleLayout preparedParticleLayout;

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
			clearPrepared();
		}

		private boolean isPreparedFor(SourceSlot source) {
			return preparedSourceGeneration == source.generation;
		}

		private void uploadGroupRecords(int[] records) {
			indMapped.asIntBuffer().position(0).put(records);
		}

		private void createTargetBuffer(int size) {
			if (targetBuffer != null) {
				MemoryManager.getInstance().addToFreeable(targetBuffer);
			}

			Buffer buffer = new Buffer("ParticleTargetBuffer", VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, MemoryTypes.GPU_MEM);
			MemoryManager.getInstance().createBuffer(
				buffer,
				size,
				VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
				VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			targetBuffer = buffer;
			reset();
		}

		private void ensureTargetSize(int bytes) {
			if (targetBuffer == null || bytes > targetBuffer.getBufferSize()) {
				createTargetBuffer(bytes);
			}
		}

		private void reset() {
			clearPrepared();
			lastFrameUsed = -1L;
		}

		private void clearPrepared() {
			preparedSourceGeneration = -1L;
			preparedParticleLayout = null;
		}

		private void updateDescriptorFor(SourceSlot source) {
			try (MemoryStack stack = MemStackUtil.stackPush()) {
				VkDescriptorBufferInfo.Buffer bi = VkDescriptorBufferInfo.calloc(DESCRIPTOR_SET_LAYOUT_SIZE, stack);
				bi.get(0).buffer(source.srcBuf).offset(0).range(VK10.VK_WHOLE_SIZE);
				bi.get(1).buffer(targetBuffer.getId()).offset(0).range(VK10.VK_WHOLE_SIZE);
				bi.get(2).buffer(indBuf).offset(0).range(VK10.VK_WHOLE_SIZE);

				VkWriteDescriptorSet.Buffer ws = VkWriteDescriptorSet.calloc(DESCRIPTOR_SET_LAYOUT_SIZE, stack);
				for (int i = 0; i < DESCRIPTOR_SET_LAYOUT_SIZE; i++) {
					ws.get(i).sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
						.dstSet(descriptorSet).dstBinding(i)
						.descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
						.pBufferInfo(bi.position(i));
				}
				ws.position(0);
				VK10.vkUpdateDescriptorSets(device, ws, null);
			}
		}

		private void destroy() {
			VK10.vkUnmapMemory(device, indMem);
			VK10.vkDestroyBuffer(device, indBuf, null);
			indBuf = VK10.VK_NULL_HANDLE;
			VK10.vkFreeMemory(device, indMem, null);
			indMem = VK10.VK_NULL_HANDLE;
			if (targetBuffer != null) {
				MemoryManager.getInstance().addToFreeable(targetBuffer);
				targetBuffer = null;
			}
		}

		private void waitReady() {
			if (lastFrameUsed < 0) {
				return;
			}
			RendererAddon renderer = (RendererAddon) Renderer.getInstance();
			long diff = renderer.asyncparticles$getActualFrame() - lastFrameUsed;
			// New sources are published before rendering; same-frame views reuse prepared data.
			if (diff > 0 && diff < Renderer.getFramesNum()) {
				Synchronization.waitFence(renderer.asyncparticles$getInFlightFence(lastFrameUsed));
			}
			lastFrameUsed = -1L;
		}

		public void prepare(SourceSlot source) {
			int needBytes = source.particleLayout.totalCount() * 4 * PARTICLE_VERTEX_SIZE;
			this.ensureTargetSize(needBytes);
			this.ensureIndirectionSize(source.groupRecords.length * Integer.BYTES);
			this.uploadGroupRecords(source.groupRecords);
			this.updateDescriptorFor(source);
			this.preparedSourceGeneration = source.generation;
			this.preparedParticleLayout = source.particleLayout;
		}
	}
}
