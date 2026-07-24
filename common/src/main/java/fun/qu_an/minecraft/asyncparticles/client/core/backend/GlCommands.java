package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import org.lwjgl.opengl.*;

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

	public static class GL_ES extends GlCommands {
		public GL_ES(boolean directStateAccess, boolean vertexAttribBinding) {
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
	}

	public static class GL extends GlCommands {
		public GL(boolean directStateAccess, boolean vertexAttribBinding) {
			super(directStateAccess, vertexAttribBinding);
		}

		@Override
		public void glMultiDrawArrays(int mode, int[] first, int[] count) {
			GL14C.glMultiDrawArrays(mode, first, count);
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

		public abstract void glBindTransformFeedbackBuffer(int vbo);

		public abstract void glBindTransformFeedbackBufferBase(int tf, int index, int vbo);

		public abstract void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size);

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
			public void glBindTransformFeedbackBuffer(int vbo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int vbo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size) {
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
			public void glBindTransformFeedbackBuffer(int vbo) {
				GL30C.glBindBuffer(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, vbo);
			}

			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int vbo) {
				if (Backends.gl.directStateAccess()) {
					ARBDirectStateAccess.glTransformFeedbackBufferBase(tf, index, vbo);
				} else {
					GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, vbo);
				}
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size) {
				if (Backends.gl.directStateAccess()) {
					ARBDirectStateAccess.glTransformFeedbackBufferRange(tf, index, vbo, offset, size);
				} else {
					GL30C.glBindBufferRange(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, vbo, offset, size);
				}
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
				return ARBTransformFeedback2.glGenTransformFeedbacks();
			}

			@Override
			public void deleteTransformFeedback(int tf) {
				ARBTransformFeedback2.glDeleteTransformFeedbacks(tf);
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

		public static class ARB3 extends ARB2 {
		}

		public static class GL40 extends GL30 {
			@Override
			public boolean isTfObjectSupported() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return GL40C.glGenTransformFeedbacks();
			}

			@Override
			public void deleteTransformFeedback(int tf) {
				GL40C.glDeleteTransformFeedbacks(tf);
			}

			@Override
			public void glBindTransformFeedback(int tf) {
				GL40C.glBindTransformFeedback(GL40C.GL_TRANSFORM_FEEDBACK, tf);
			}

			@Override
			public void glPauseTransformFeedback() {
				GL40C.glPauseTransformFeedback();
			}

			@Override
			public void glResumeTransformFeedback(int primitiveMode) {
				GL40C.glResumeTransformFeedback();
			}
		}

		public static class GL45 extends GL40 {
			@Override
			public void glBindTransformFeedbackBufferBase(int tf, int index, int vbo) {
				GL45C.glTransformFeedbackBufferBase(tf, index, vbo);
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size) {
				GL45C.glTransformFeedbackBufferRange(tf, index, vbo, offset, size);
			}
		}
	}
}
