package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals.ImmPtlEarlyCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals.ImmPtlComputeReuseCondition;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.*;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.IGlParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ShaderCompiler;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.IVkParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkLayoutResult;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.Renderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleVertexFormats.*;

public final class ParticleStormPipeline implements IGlParticlePipeline<ParticleStormPipeline.Context>,
	IVkParticlePipeline<ParticleStormPipeline.Context> {
	public static final ParticleVertexFormat INPUT = new ParticleVertexFormat(
		VEC3F, VEC3F, VEC2F, UV0_2F, UV0_2F, COLOR, UV2_2S, VEC3F, VEC4F, VEC4F);
	private static final int CAMERA_ROLL_ONLY = 0;
	private static final int CAMERA_XYZ = 1;
	private static final int FIXED_FACING = 6;
	private static final Vector3f X_AXIS = new Vector3f(1, 0, 0);

	@Override
	public Context createGlState() {
		return new Context();
	}

	@Override
	public Context createVkState() {
		return new Context();
	}

	@Override
	public byte[] getSpirv() {
		return ShaderCompiler.readSpirv("/assets/asyncparticles/compat/particlestorm/vk_quad.spv");
	}

	@Override
	public ParticleVertexFormat getInputFormat() {
		return INPUT;
	}

	@Override
	public int getVkPushConstantSize() {
		return 60;
	}

	@Override
	public IParticleBatchKey batchKey(TextureSheetParticle particle) {
		return (IParticleBatchKey) particle.getRenderType();
	}

	@Override
	public IParticleRenderKey renderKey(ParticleRenderType layer) {
		return (IParticleRenderKey) layer;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T extends Collection<TextureSheetParticle>> Map<IParticleBatchKey, Collection<TextureSheetParticle>>
	groupParticles(Map<ParticleRenderType, T> particles) {
		return (Map) particles;
	}

	@Override
	public LayerBatch createBatch(IParticleBatchKey key, Vec3 snapshotCamPos) {
		return new Batch(key);
	}

	@Override
	public Vec3 positionOrigin(LayerBatch batch, Vec3 snapshotCamPos) {
		return snapshotCamPos;
	}

	@Override
	public IComputeReuseCondition prepareView(Context context, List<LayerBatch> batches, Camera camera, float partialTicks) {
		return ImmPtlEarlyCompat.AVAILABLE
			? ImmPtlComputeReuseCondition.of(Minecraft.getInstance().level, camera, partialTicks)
			: IComputeReuseCondition.DUMMY;
	}

	@Override
	public int extractBatch(Context context,
							Collection<TextureSheetParticle> particles,
							LayerBatch layerBatch,
							long address,
							long bufferAddress,
							Vec3 cameraPos,
							boolean appending) {
		Batch batch = (Batch) layerBatch;
		Quaternionf facing = context.facing;
		int vertexSize = INPUT.getVertexSize();
		int count = 0;
		boolean cameraOnlyLayer = batch.cameraOnly;
		Vec3 origin = positionOrigin(batch, cameraPos);
		final double cx = origin.x;
		final double cy = origin.y;
		final double cz = origin.z;
		for (TextureSheetParticle textureParticle : particles) {
			ParticleStormParticle snapshot = (ParticleStormParticle) textureParticle;
			if (!appending && (!textureParticle.isAlive() || !IParticleRenderer.shouldRenderParticle(snapshot, cameraPos))) {
				continue;
			}
			MolangParticleInstance particle = (MolangParticleInstance) textureParticle;
			int mode = switch (particle.getFacingCameraMode()) {
				case ROTATE_XYZ -> CAMERA_XYZ;
				case ROTATE_Y -> 2;
				case LOOKAT_XYZ -> 3;
				case LOOKAT_Y -> 4;
				case LOOKAT_DIRECTION -> 5;
				default -> FIXED_FACING;
			};
			facing.identity();
			if (mode == FIXED_FACING) {
				particle.getFacingCameraMode().setRotation(particle, facing, null, 0f);
			} else if (mode == 5) {
				MathHelper.setFromUnitVectors(X_AXIS, particle.getFacingDirection(), facing);
			}
			MemoryUtil.memPutFloat(address, (float) (snapshot.asyncparticles$getXo() - cx));
			MemoryUtil.memPutFloat(address + 4L, (float) (snapshot.asyncparticles$getYo() - cy));
			MemoryUtil.memPutFloat(address + 8L, (float) (snapshot.asyncparticles$getZo() - cz));
			MemoryUtil.memPutFloat(address + 12L, (float) (snapshot.asyncparticles$getX() - cx));
			MemoryUtil.memPutFloat(address + 16L, (float) (snapshot.asyncparticles$getY() - cy));
			MemoryUtil.memPutFloat(address + 20L, (float) (snapshot.asyncparticles$getZ() - cz));
			float[] size = snapshot.asyncparticles$getBillboardSize();
			MemoryUtil.memPutFloat(address + 24, size[0]);
			MemoryUtil.memPutFloat(address + 28, size[1]);
			MemoryUtil.memPutFloat(address + 32, snapshot.asyncparticles$getU0());
			MemoryUtil.memPutFloat(address + 36, snapshot.asyncparticles$getV0());
			MemoryUtil.memPutFloat(address + 40, snapshot.asyncparticles$getU1());
			MemoryUtil.memPutFloat(address + 44, snapshot.asyncparticles$getV1());
			MemoryUtil.memPutInt(address + 48, snapshot.asyncparticles$getOColor());
			MemoryUtil.memPutInt(address + 52, snapshot.asyncparticles$getParticleStormLight(0f));
			float xRotO = snapshot.asyncparticles$getXRotO();
			MemoryUtil.memPutFloat(address + 56, xRotO);
			float yRotO = snapshot.asyncparticles$getYRotO();
			MemoryUtil.memPutFloat(address + 60, yRotO);
			MemoryUtil.memPutFloat(address + 64, snapshot.asyncparticles$getORoll());
			float xRot = snapshot.asyncparticles$getXRot();
			MemoryUtil.memPutFloat(address + 68, xRot);
			float yRot = snapshot.asyncparticles$getYRot();
			MemoryUtil.memPutFloat(address + 72, yRot);
			MemoryUtil.memPutFloat(address + 76, snapshot.asyncparticles$getRoll());
			if (mode == CAMERA_XYZ && xRotO == 0.0f && yRotO == 0.0f && xRot == 0.0f && yRot == 0.0f) {
				mode = CAMERA_ROLL_ONLY;
			}
			cameraOnlyLayer &= mode == CAMERA_ROLL_ONLY;
			MemoryUtil.memPutFloat(address + 80, mode);
			MemoryUtil.memPutFloat(address + 84, facing.x);
			MemoryUtil.memPutFloat(address + 88, facing.y);
			MemoryUtil.memPutFloat(address + 92, facing.z);
			MemoryUtil.memPutFloat(address + 96, facing.w);
			MemoryUtil.memCopy(address, bufferAddress, vertexSize);
			bufferAddress += vertexSize;
			count++;
		}
		batch.cameraOnly = cameraOnlyLayer;
		return count;
	}

	@Override
	public void prepareGlCompute(Context context, Camera camera, float partialTicks, Vec3 snapshotCameraPos,
								int particleCount, int transformFeedback, int targetBuffer) {
		ParticleStormShader.INSTANCE.setup(context.renderCameraOnly, camera, partialTicks, snapshotCameraPos);
		if (transformFeedback == 0) {
			long particleBytes = 4L * IDENTITY_PARTICLE.getVertexSize();
			Backends.glTf.glBindTransformFeedbackBufferRange(0, 0, targetBuffer, 0L, particleCount * particleBytes);
		}
	}

	@Override
	public ParticleLayout buildGlLayout(Context context, List<LayerBatch> batches, int particleLimit) {
		int size = batches.size();
		Map<IParticleRenderKey, ParticleLayout.Slice> slices = new Object2ReferenceOpenHashMap<>(size);
		IntArrayList first = new IntArrayList(size * 2);
		IntArrayList count = new IntArrayList(size * 2);
		int totalCount = 0;
		context.renderCameraOnly = true;
		for (LayerBatch batch : batches) {
			context.renderCameraOnly &= ((Batch) batch).cameraOnly;
			int tickCount = batch.tickCount;
			int appendCount = batch.appendCount;
			if (tickCount > 0) {
				first.add(batch.tickOffset);
				count.add(tickCount);
			}
			if (appendCount > 0) {
				first.add(particleLimit + batch.appendOffset);
				count.add(appendCount);
			}
			batch.targetOffset = totalCount;
			int batchCount = tickCount + appendCount;
			if (batchCount > 0) {
				slices.put((IParticleRenderKey) batch.key, new ParticleLayout.Slice(batch.key.layer(), totalCount, batchCount));
				totalCount += batchCount;
			}
		}
		context.multiDrawFirst = first.toIntArray();
		context.multiDrawCount = count.toIntArray();
		return ParticleLayout.of(totalCount, slices);
	}

	@Override
	public void drawGlCompute(Context context,
							  Camera camera,
							  float partialTicks,
							  Vec3 snapshotCamPos,
							  List<LayerBatch> batches,
							  int particleLimit) {
		Backends.gl.glMultiDrawArrays(GL11C.GL_POINTS, context.multiDrawFirst, context.multiDrawCount);
		ParticleStormCompat.lastGpuComputeShader = context.renderCameraOnly
			? "facing_screen_quad.vert (simple)" : "common_quad.vert (complex)";
	}

	@Override
	public VkLayoutResult buildVkLayout(List<LayerBatch> batches, int particleLimit) {
		Map<IParticleRenderKey, ParticleLayout.Slice> slices = new Object2ReferenceOpenHashMap<>(batches.size());
		int totalCount = 0;
		int groups = 0;
		for (LayerBatch batch : batches) {
			batch.targetOffset = totalCount;
			int count = batch.count();
			if (count > 0) {
				slices.put((IParticleRenderKey) batch.key, new ParticleLayout.Slice(batch.key.layer(), totalCount, count));
				totalCount += count;
			}
			groups += (batch.tickCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE;
			groups += (batch.appendCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE;
		}
		return new VkLayoutResult(ParticleLayout.of(totalCount, slices), groups);
	}

	@Override
	public void dispatchVkBatches(Context context, long pipelineLayout, ByteBuffer constants,
								  List<LayerBatch> batches, Vec3 snapshotCamPos, Camera camera,
								  float partialTicks, int groupCount) {
		Vec3 position = camera.getPosition();
		Quaternionf rotation = camera.rotation();
		constants.putFloat(28, (float) (snapshotCamPos.x - position.x));
		constants.putFloat(32, (float) (snapshotCamPos.y - position.y) + 1.0e-4f);
		constants.putFloat(36, (float) (snapshotCamPos.z - position.z));
		constants.putInt(40, 0);
		constants.putFloat(44, rotation.x);
		constants.putFloat(48, rotation.y);
		constants.putFloat(52, rotation.z);
		constants.putFloat(56, rotation.w);
		constants.position(0).limit(60);
		VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
		VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, constants);
		VK10.vkCmdDispatch(commandBuffer, groupCount, 1, 1);
		ParticleStormCompat.lastGpuComputeShader = "vk_quad.spv (Vulkan)";
	}

	private static final class Batch extends LayerBatch {
		boolean cameraOnly = true;

		Batch(IParticleBatchKey key) {
			super(key);
		}
	}

	public static class Context {
		final Quaternionf facing = new Quaternionf();
		boolean renderCameraOnly;
		int[] multiDrawFirst;
		int[] multiDrawCount;
	}
}
