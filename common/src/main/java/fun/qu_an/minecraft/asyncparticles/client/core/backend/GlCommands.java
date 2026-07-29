package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.opengl.GlStateManager;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public abstract class GlCommands {
	private final boolean directStateAccess;
	private final boolean vertexAttribBinding;

	public GlCommands(boolean directStateAccess, boolean vertexAttribBinding) {
		this.directStateAccess = directStateAccess;
		this.vertexAttribBinding = vertexAttribBinding;
	}

	public boolean isSupported() {
		return true;
	}

	public abstract void glMultiDrawArrays(int mode, int[] first, int[] count);

	public abstract void glCopyBufferSubData(int read, int write, long readOff, long writeOff, long size);

	public boolean directStateAccess() {
		return directStateAccess;
	}

	public boolean vertexAttribBinding() {
		return vertexAttribBinding;
	}

	@Override
	public String toString() {
		return "GLCommands." + this.getClass().getSimpleName() + "{" +
			"directStateAccess=" + directStateAccess +
			", vertexAttribBinding=" + vertexAttribBinding +
			'}';
	}

	public abstract void glBufferData(int buffer, long size, int usage);

	public abstract int glGenBuffers();

	public abstract int glGenVertexArrays();

	public abstract ByteBuffer glMapBufferRange(int buffer, int offset, int length, int access, ByteBuffer oldBuffer);

	public abstract void glFlushMappedBufferRange(int buffer, int offset, int length);

	public abstract void glUnmapBuffer(int buffer);

	public static class GLonES extends GlCommands {
		public GLonES(boolean directStateAccess, boolean vertexAttribBinding) {
			super(directStateAccess, vertexAttribBinding);
		}

		@Override
		public void glMultiDrawArrays(int mode, int[] first, int[] count) {
			if (ConfigHelper.mobileCompatMultiDraw()) {
				for (int i = 0; i < first.length; i++) {
					GL11C.glDrawArrays(mode, first[i], count[i]);
				}
			} else {
				GL14C.glMultiDrawArrays(mode, first, count);
			}
		}

		@Override
		public void glCopyBufferSubData(int read, int write, long readOff, long writeOff, long size) {
			GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, read);
			GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, write);
			GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOff, writeOff, size);
			GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
			GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
		}

		@Override
		public void glBufferData(int buffer, long size, int usage) {
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, size, usage);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		}

		@Override
		public int glGenBuffers() {
			return GL15C.glGenBuffers();
		}

		@Override
		public int glGenVertexArrays() {
			return GL30C.glGenVertexArrays();
		}

		@Override
		public ByteBuffer glMapBufferRange(int buffer, int offset, int length, int access, ByteBuffer oldBuffer) {
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
			ByteBuffer byteBuffer = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER, offset, length, access, oldBuffer);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
			return byteBuffer;
		}

		@Override
		public void glFlushMappedBufferRange(int buffer, int offset, int length) {
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
			GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, 0, length);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		}

		@Override
		public void glUnmapBuffer(int buffer) {
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
			GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		}
	}

	public static class GL extends GlCommands {
		public GL(boolean directStateAccess, boolean vertexAttribBinding) {
			super(directStateAccess, vertexAttribBinding);
		}

		@Override
		public void glMultiDrawArrays(int mode, int[] first, int[] count) {
			GL14C.glMultiDrawArrays(mode, first, count);
		}

		@Override
		public void glCopyBufferSubData(int read, int write, long readOff, long writeOff, long size) {
			if (directStateAccess()) {
				ARBDirectStateAccess.glCopyNamedBufferSubData(read, write, readOff, writeOff, size);
			} else {
				GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, read);
				GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, write);
				GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOff, writeOff, size);
				GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
				GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
			}
		}

		@Override
		public void glBufferData(int buffer, long size, int usage) {
			if (directStateAccess()) {
				ARBDirectStateAccess.glNamedBufferData(buffer, size, usage);
			} else {
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
				GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, size, usage);
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
			}
		}

		@Override
		public int glGenBuffers() {
			return directStateAccess() ? ARBDirectStateAccess.glCreateBuffers() : GL15C.glGenBuffers();
		}

		@Override
		public int glGenVertexArrays() {
			return directStateAccess() ? ARBDirectStateAccess.glCreateVertexArrays() : GL30C.glGenVertexArrays();
		}

		@Override
		public ByteBuffer glMapBufferRange(int buffer, int offset, int length, int access, ByteBuffer oldBuffer) {
			if (directStateAccess()) {
				return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, access, oldBuffer);
			} else {
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
				ByteBuffer byteBuffer = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER, offset, length, access, oldBuffer);
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
				return byteBuffer;
			}
		}

		@Override
		public void glFlushMappedBufferRange(int buffer, int offset, int length) {
			if (directStateAccess()) {
				ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
			} else {
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
				GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, offset, length);
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
			}
		}

		@Override
		public void glUnmapBuffer(int buffer) {
			if (directStateAccess()) {
				ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
			} else {
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
				GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
				GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
			}
		}
	}

	public static class Unsupported extends GlCommands {
		public Unsupported() {
			super(false, false);
		}

		@Override
		public boolean isSupported() {
			return false;
		}

		@Override
		public void glMultiDrawArrays(int mode, int[] first, int[] count) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void glCopyBufferSubData(int read, int write, long read1, long write1, long size) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void glBufferData(int buffer, long size, int usage) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int glGenBuffers() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int glGenVertexArrays() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer glMapBufferRange(int buffer, int offset, int length, int access, ByteBuffer oldBuffer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void glFlushMappedBufferRange(int buffer, int offset, int length) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void glUnmapBuffer(int buffer) {
			throw new UnsupportedOperationException();
		}
	}

	public abstract static class ComputeShader {
		public abstract boolean isSupported();

		public abstract void glBindShaderStorageBuffer(int ssbo);

		public abstract void glBindShaderStorageBufferBase(int bindingPoint, int ssbo);

		public abstract void glShaderStorageBufferData(int size, int usage);

		public String toString() {
			return "ComputeShader." + this.getClass().getSimpleName();
		}

		public static class Unsupported extends ComputeShader {
			@Override
			public boolean isSupported() {
				return false;
			}

			@Override
			public void glBindShaderStorageBuffer(int ssbo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindShaderStorageBufferBase(int bindingPoint, int ssbo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glShaderStorageBufferData(int size, int usage) {
				throw new UnsupportedOperationException();
			}
		}

		public static class ARB extends ComputeShader {
			@Override
			public boolean isSupported() {
				return true;
			}

			@Override
			public void glBindShaderStorageBuffer(int ssbo) {
				GL30C.glBindBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, ssbo);
			}

			@Override
			public void glBindShaderStorageBufferBase(int bindingPoint, int ssbo) {
				GL30C.glBindBufferBase(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, bindingPoint, ssbo);
			}

			@Override
			public void glShaderStorageBufferData(int size, int usage) {
				GL15C.glBufferData(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, size, usage);
			}
		}

		public static class GL43 extends ARB {
		}
	}

	public abstract static class TransformFeedback {
		public abstract boolean isTfObjectSupported();

		public abstract boolean isSupported();

		public abstract int genTransformFeedback();

		public abstract void deleteTransformFeedback(int tf);

		public abstract void glBindTransformFeedback(int tf);

		public abstract void glBindTransformFeedbackBuffer(int buffer);

		public abstract void glBindTransformFeedbackBufferBase(int tf, int index, int buffer);

		public abstract void glBindTransformFeedbackBufferRange(int tf, int index, int buffer, long offset, long size);

		public abstract void glBeginTransformFeedback(int primitiveMode);

		public abstract void glEndTransformFeedback();

		public abstract void glPauseTransformFeedback();

		public abstract void glResumeTransformFeedback(int primitiveMode);

		public abstract void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs);

		public String toString() {
			return "TransformFeedback." + this.getClass().getSimpleName();
		}

		public static class Unsupported extends TransformFeedback {
			@Override
			public boolean isTfObjectSupported() {
				return false;
			}

			@Override
			public boolean isSupported() {
				return false;
			}

			@Override
			public int genTransformFeedback() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void deleteTransformFeedback(int tf) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedback(int tf) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBuffer(int buffer) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int buffer) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int buffer, long offset, long size) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBeginTransformFeedback(int primitiveMode) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glEndTransformFeedback() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glPauseTransformFeedback() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glResumeTransformFeedback(int primitiveMode) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs) {
				throw new UnsupportedOperationException();
			}
		}

		public static class GL30 extends TransformFeedback {
			@Override
			public boolean isTfObjectSupported() {
				return false;
			}

			@Override
			public boolean isSupported() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return 0;
			}

			@Override
			public void deleteTransformFeedback(int tf) {
			}

			@Override
			public void glBindTransformFeedback(int tf) {
			}

			@Override
			public void glBindTransformFeedbackBuffer(int buffer) {
				GL30C.glBindBuffer(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, buffer);
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int buffer) {
				GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer);
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int buffer, long offset, long size) {
				GL30C.glBindBufferRange(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer, offset, size);
			}

			@Override
			public void glBeginTransformFeedback(int primitiveMode) {
				GL30C.glBeginTransformFeedback(primitiveMode);
			}

			@Override
			public void glEndTransformFeedback() {
				GL30C.glEndTransformFeedback();
			}

			@Override
			public void glPauseTransformFeedback() {
				glEndTransformFeedback();
			}

			@Override
			public void glResumeTransformFeedback(int primitiveMode) {
				glBeginTransformFeedback(primitiveMode);
			}

			@Override
			public void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs) {
				GL30C.glTransformFeedbackVaryings(tshProg, varyings, glInterleavedAttribs);
			}
		}

		public static class ARB2 extends GL30 {
			@Override
			public boolean isTfObjectSupported() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return Backends.gl.directStateAccess() ? ARBDirectStateAccess.glCreateTransformFeedbacks() : ARBTransformFeedback2.glGenTransformFeedbacks();
			}

			@Override
			public void deleteTransformFeedback(int tf) {
				ARBTransformFeedback2.glDeleteTransformFeedbacks(tf);
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int buffer) {
				if (Backends.gl.directStateAccess()) {
					ARBDirectStateAccess.glTransformFeedbackBufferBase(tf, index, buffer);
				} else {
					int binding = GL11C.glGetInteger(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK_BINDING);
					if (binding == tf) {
						GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer);
					} else {
						glBindTransformFeedback(tf);
						GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer);
						glBindTransformFeedback(binding);
					}
				}
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int buffer, long offset, long size) {
				if (Backends.gl.directStateAccess()) {
					ARBDirectStateAccess.glTransformFeedbackBufferRange(tf, index, buffer, offset, size);
				} else {
					int binding = GL11C.glGetInteger(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK_BINDING);
					if (binding == tf) {
						GL30C.glBindBufferRange(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer, offset, size);
					} else {
						glBindTransformFeedback(tf);
						GL30C.glBindBufferRange(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer, offset, size);
						glBindTransformFeedback(binding);
					}
				}
			}

			@Override
			public void glBindTransformFeedback(int tf) {
				ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, tf);
			}

			@Override
			public void glPauseTransformFeedback() {
				ARBTransformFeedback2.glPauseTransformFeedback();
			}

			@Override
			public void glResumeTransformFeedback(int primitiveMode) {
				ARBTransformFeedback2.glResumeTransformFeedback();
			}
		}

		public static class GL45 extends ARB2 {
			@Override
			public int genTransformFeedback() {
				return GL45C.glCreateTransformFeedbacks();
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int buffer) {
				GL45C.glTransformFeedbackBufferBase(tf, index, buffer);
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int buffer, long offset, long size) {
				GL45C.glTransformFeedbackBufferRange(tf, index, buffer, offset, size);
			}
		}
	}
}
