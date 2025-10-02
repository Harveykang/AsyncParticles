package fun.qu_an.minecraft.asyncparticles.client.particle.buffer;


import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL42C;

public class AtomicCounterBuffer {
	public final int bufferId;

    public AtomicCounterBuffer() {
        bufferId = GL15C.glGenBuffers();
        // 初始化为 0
        bind();
		GL15C.glBufferData(GL42C.GL_ATOMIC_COUNTER_BUFFER, 8, GL15C.GL_DYNAMIC_DRAW);
        int[] zero = {0, 0}; // particle count and overflow count
		GL15C.glBufferSubData(GL42C.GL_ATOMIC_COUNTER_BUFFER, 0, zero);
        unbind();
    }

    public void bind(int bindingPoint) {
		GL30C.glBindBufferBase(GL42C.GL_ATOMIC_COUNTER_BUFFER, bindingPoint, bufferId);
    }

    public int debugReadCounter() {
        bind(); // 临时绑定
        int[] value = new int[1];
		GL15C.glGetBufferSubData(GL42C.GL_ATOMIC_COUNTER_BUFFER, 0, value);
        unbind();
        return value[0];
    }

	public void bind() {
		GL15C.glBindBuffer(GL42C.GL_ATOMIC_COUNTER_BUFFER, bufferId);
    }

	public static void unbind() {
		GL15C.glBindBuffer(GL42C.GL_ATOMIC_COUNTER_BUFFER, 0);
    }

    public void delete() {
		GL15C.glDeleteBuffers(bufferId);
    }
}
