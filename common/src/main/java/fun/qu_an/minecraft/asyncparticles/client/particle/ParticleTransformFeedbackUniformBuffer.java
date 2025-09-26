package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ParticleTransformFeedbackUniformBuffer {
	public static ParticleTransformFeedbackUniformBuffer TF_UNIFORM_BUFFER = new ParticleTransformFeedbackUniformBuffer();
	private static final int SIZE = 64;
	public final int ubo;
	private ByteBuffer buffer;

	protected ParticleTransformFeedbackUniformBuffer() {
		ubo = GL15C.glGenBuffers();
		GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, ubo);
		GL15C.glBufferData(GL31C.GL_UNIFORM_BUFFER, SIZE, GL15C.GL_DYNAMIC_DRAW);
		GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, 0);
	}

	public void linkUniformBlock(int prog) {
		int blockIndex = GL31C.glGetUniformBlockIndex(prog, "FrameInfo");
		if (blockIndex == GL31C.GL_INVALID_INDEX) {
			throw new RuntimeException("Uniform block 'FrameInfo' not found in program!");
		}

		GL31C.glUniformBlockBinding(prog, blockIndex, 0);
	}

	public void update(Camera camera, float partialTicks, Vec3 lastCamPos) {
		GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, ubo);
		buffer = GL30C.glMapBufferRange(GL31C.GL_UNIFORM_BUFFER,
			0,
			SIZE,
			GL30C.GL_MAP_WRITE_BIT |
				GL30C.GL_MAP_INVALIDATE_BUFFER_BIT |
				GL30C.GL_MAP_UNSYNCHRONIZED_BIT |
				0,
			buffer);

		Vec3 camPos = camera.getPosition();
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		float camDx = (float) (lastCamPos.x - camPos.x);
		float camDy = (float) (lastCamPos.y - camPos.y);
		float camDz = (float) (lastCamPos.z - camPos.z);
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			long address = stack.nmalloc(SIZE);
			long ptr = address;

			MemoryUtil.memPutFloat(ptr, partialTicks);
			ptr += 16L;

			MemoryUtil.memPutFloat(ptr, left.x);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, left.y);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, left.z);
			ptr += 8L;

			MemoryUtil.memPutFloat(ptr, up.x);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, up.y);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, up.z);
			ptr += 8L;

			MemoryUtil.memPutFloat(ptr, camDx);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, camDy);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, camDz);

			MemoryUtil.memCopy(address, MemoryUtil.memAddress(buffer), SIZE);
		}
		GL30C.glUnmapBuffer(GL31C.GL_UNIFORM_BUFFER);
		GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, 0);
	}

	public void bindUBO(int bindingPoint) {
		GL30C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, bindingPoint, ubo);
	}
}
