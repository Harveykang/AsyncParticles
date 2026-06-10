package fun.qu_an.minecraft.asyncparticles.client.compat;

import org.lwjgl.opengl.*;

public class GLCaps {
	public static final boolean supportsExplicitAttribLocation;
	public static final TfSupport tfSupport;
	public static final CsSupport csSupport;

	static {
		GLCapabilities glCaps = GL.getCapabilities();
		supportsExplicitAttribLocation = glCaps.OpenGL33 ||
			glCaps.GL_ARB_explicit_attrib_location; // FIXME fix this!!!
		if (glCaps.OpenGL40) {
			tfSupport = new TfSupport.GL_40();
		} else if (glCaps.GL_ARB_transform_feedback3) {
			tfSupport = new TfSupport.ARB_3();
		} else if (glCaps.GL_ARB_transform_feedback2) {
			tfSupport = new TfSupport.ARB_2();
		} else if (glCaps.OpenGL30) {
			tfSupport = new TfSupport.GL_30();
		} else {
			tfSupport = new TfSupport.Unsupported(); // impossible
		}
		if (glCaps.OpenGL43) {
			csSupport = new CsSupport.GL_43();
		} else if (glCaps.GL_ARB_compute_shader &&
			glCaps.GL_ARB_shader_storage_buffer_object &&
			glCaps.GL_ARB_shader_atomic_counters) {
			csSupport = new CsSupport.ARB();
		} else {
			csSupport = new CsSupport.Unsupported();
		}
	}

	public static boolean supportsGpuAcceleration() {
		return (tfSupport.isSupported()) && supportsExplicitAttribLocation;
	}

	public static void init() {
	}

	public interface CsSupport {
		boolean isSupported();

		void glBindShaderStorageBuffer(int ssbo);

		void glBindShaderStorageBufferBase(int bindingPoint, int ssbo);

		class Unsupported implements CsSupport {
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
		}

		class ARB implements CsSupport {
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
		}

		class GL_43 extends ARB {
		}
	}

	public interface TfSupport {
		boolean isSupportsTfo();

		boolean isSupported();

		int genTransformFeedback();

		void glBindTransformFeedback(int tf);

		void glBindTransformFeedbackBuffer(int index, int vbo);

		void glBeginTransformFeedback(int primitiveMode);

		void glEndTransformFeedback();

		void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs);

		class Unsupported implements TfSupport {
			@Override
			public boolean isSupportsTfo() {
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
			public void glBindTransformFeedback(int tf) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void glBindTransformFeedbackBuffer(int index, int vbo) {
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
			public void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs) {
				throw new UnsupportedOperationException();
			}
		}

		class GL_30 implements TfSupport {
			@Override
			public boolean isSupportsTfo() {
				return false;
			}

			@Override
			public boolean isSupported() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return -1;
			}

			@Override
			public void glBindTransformFeedback(int tf) {
			}

			@Override
			public void glBindTransformFeedbackBuffer(int index, int vbo) {
				GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, vbo);
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
			public void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs) {
				GL30C.glTransformFeedbackVaryings(tshProg, varyings, glInterleavedAttribs);
			}
		}

		class ARB_2 extends GL_30 {
			@Override
			public boolean isSupportsTfo() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return ARBTransformFeedback2.glGenTransformFeedbacks();
			}

			@Override
			public void glBindTransformFeedback(int tf) {
				ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, tf);
			}
		}

		class ARB_3 extends ARB_2 {
		}

		class GL_40 implements TfSupport {
			@Override
			public boolean isSupportsTfo() {
				return true;
			}

			@Override
			public boolean isSupported() {
				return true;
			}

			@Override
			public int genTransformFeedback() {
				return GL40C.glGenTransformFeedbacks();
			}

			@Override
			public void glBindTransformFeedback(int tf) {
				GL40C.glBindTransformFeedback(GL40C.GL_TRANSFORM_FEEDBACK, tf);
			}

			@Override
			public void glBindTransformFeedbackBuffer(int index, int vbo) {
				GL40C.glBindBufferBase(GL40C.GL_TRANSFORM_FEEDBACK_BUFFER, index, vbo);
			}

			@Override
			public void glBeginTransformFeedback(int primitiveMode) {
				GL40C.glBeginTransformFeedback(primitiveMode);
			}

			@Override
			public void glEndTransformFeedback() {
				GL40C.glEndTransformFeedback();
			}

			@Override
			public void glTransformFeedbackVaryings(int tshProg, String[] varyings, int glInterleavedAttribs) {
				GL40C.glTransformFeedbackVaryings(tshProg, varyings, glInterleavedAttribs);
			}
		}
	}
}
