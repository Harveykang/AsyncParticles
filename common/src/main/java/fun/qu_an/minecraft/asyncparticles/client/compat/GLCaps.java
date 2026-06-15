package fun.qu_an.minecraft.asyncparticles.client.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;

import java.util.List;

public class GLCaps {
	public static final boolean supportsExplicitAttribLocation;
	public static final boolean supportsDirectStateAccess;
	public static final boolean supportsARBVertexAttribBinding;
	public static final TfSupport tfSupport;
	public static final CsSupport csSupport;

	static {
		GLCapabilities glCaps = GL.getCapabilities();
		supportsExplicitAttribLocation = glCaps.OpenGL33 ||
			glCaps.GL_ARB_explicit_attrib_location; // FIXME fix this!!!
		List<String> enabledExtensions = RenderSystem.getDevice().getEnabledExtensions();
		supportsDirectStateAccess = enabledExtensions.contains("GL_ARB_direct_state_access");
		supportsARBVertexAttribBinding = enabledExtensions.contains("GL_ARB_vertex_attrib_binding");
		if (glCaps.OpenGL45) {
			tfSupport = new TfSupport.GL_45();
		} else if (glCaps.OpenGL40) {
			tfSupport = new TfSupport.GL_40();
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

		void deleteTransformFeedback(int tf);

		void glBindTransformFeedback(int tf);

		void glBindTransformFeedbackBuffer(int vbo);

		void glBindTransformFeedbackBufferBase(int tf, int index, int vbo);

		void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size);

		void glBeginTransformFeedback(int primitiveMode);

		void glEndTransformFeedback();

		void glPauseTransformFeedback();

		void glResumeTransformFeedback(int primitiveMode);

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
				if (supportsDirectStateAccess) {
					ARBDirectStateAccess.glTransformFeedbackBufferBase(tf, index, vbo);
				} else {
					GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, index, vbo);
				}
			}

			@Override
			public void glBindTransformFeedbackBufferRange(int tf, int index, int vbo, long offset, long size) {
				if (supportsDirectStateAccess){
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

		class ARB_3 extends ARB_2 {
		}

		class GL_40 extends GL_30 {
			@Override
			public boolean isSupportsTfo() {
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

		class GL_45 extends GL_40 {
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
