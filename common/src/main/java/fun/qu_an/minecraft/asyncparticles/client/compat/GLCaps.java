package fun.qu_an.minecraft.asyncparticles.client.compat;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public class GLCaps {
	public static final TFExt supportsTransformFeedback;
	public static final boolean supportsExplicitAttribLocation;
//	public static final boolean supportsUniformBufferObject;

	static {
		GLCapabilities glCaps = GL.getCapabilities();
		supportsExplicitAttribLocation = glCaps.OpenGL33 ||
			glCaps.GL_ARB_explicit_attrib_location; // FIXME fix this!!!
		if (glCaps.OpenGL40 || glCaps.GL_ARB_transform_feedback3) {
			supportsTransformFeedback = TFExt.TF3;
		} else if (glCaps.GL_ARB_transform_feedback2) {
			supportsTransformFeedback = TFExt.TF2;
		} else if (glCaps.OpenGL30 || glCaps.GL_EXT_transform_feedback) {
			supportsTransformFeedback = TFExt.TF1;
		} else {
			supportsTransformFeedback = TFExt.UNSUPPORTED; // impossible
		}
//		supportsUniformBufferObject = glCaps.OpenGL31 ||
//			glCaps.GL_ARB_uniform_buffer_object;
	}

	public static boolean supportsGpuAcceleration() {
		return supportsTransformFeedback.isSupported() && supportsExplicitAttribLocation;
	}

	public enum TFExt {
		UNSUPPORTED {
			@Override
			public boolean isSupportsTfo() {
				return false;
			}

			@Override
			public boolean isSupported() {
				return false;
			}
		},
		TF1 {
			@Override
			public boolean isSupportsTfo() {
				return false;
			}

			@Override
			public boolean isSupported() {
				return true;
			}
		},
		TF2 {
			@Override
			public boolean isSupportsTfo() {
				return true;
			}

			@Override
			public boolean isSupported() {
				return true;
			}
		},
		TF3 {
			@Override
			public boolean isSupportsTfo() {
				return true;
			}

			@Override
			public boolean isSupported() {
				return true;
			}
		};

		public abstract boolean isSupportsTfo();

		public abstract boolean isSupported();
	}
}
