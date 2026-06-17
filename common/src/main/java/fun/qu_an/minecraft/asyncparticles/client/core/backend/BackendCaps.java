package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.Locale;
import java.util.Set;

public class BackendCaps {
	public static final boolean GL_ARB_explicit_attrib_location;
	public static final boolean GL_ARB_direct_state_access;
	public static final boolean GL_ARB_vertex_attrib_binding;
	public static final GLCaps.TfSupport glTfSupport;
//	public static final GLCaps.CsSupport glCsSupport;

	static {
		String backendName = RenderSystem.getDevice().getDeviceInfo().backendName();
		if (backendName.toLowerCase(Locale.ROOT).contains("opengl")) {
			GLCapabilities glCaps = GL.getCapabilities();
			GL_ARB_explicit_attrib_location = glCaps.OpenGL33 ||
				glCaps.GL_ARB_explicit_attrib_location; // FIXME fix this!!!
			Set<String> enabledExtensions = RenderSystem.getDevice().getDeviceInfo().underlyingExtensions();
			GL_ARB_direct_state_access = enabledExtensions.contains("GL_ARB_direct_state_access");
			GL_ARB_vertex_attrib_binding = enabledExtensions.contains("GL_ARB_vertex_attrib_binding");
			if (glCaps.OpenGL45) {
				glTfSupport = new GLCaps.TfSupport.GL_45();
			} else if (glCaps.OpenGL40) {
				glTfSupport = new GLCaps.TfSupport.GL_40();
			} else if (glCaps.GL_ARB_transform_feedback2) {
				glTfSupport = new GLCaps.TfSupport.ARB_2();
			} else if (glCaps.OpenGL30) {
				glTfSupport = new GLCaps.TfSupport.GL_30();
			} else {
				glTfSupport = new GLCaps.TfSupport.Unsupported(); // impossible
			}
//			if (glCaps.OpenGL43) {
//				glCsSupport = new GLCaps.CsSupport.GL_43();
//			} else if (glCaps.GL_ARB_compute_shader &&
//				glCaps.GL_ARB_shader_storage_buffer_object &&
//				glCaps.GL_ARB_shader_atomic_counters) {
//				glCsSupport = new GLCaps.CsSupport.ARB();
//			} else {
//				glCsSupport = new GLCaps.CsSupport.Unsupported();
//			}
		} else if (backendName.toLowerCase(Locale.ROOT).contains("vulkan")) {
			GL_ARB_explicit_attrib_location = false;
			GL_ARB_direct_state_access = false;
			GL_ARB_vertex_attrib_binding = false;
			glTfSupport = new GLCaps.TfSupport.Unsupported();
		} else {
			throw new ExceptionInInitializerError("Unsupported backend: " + backendName);
		}
	}

	public static void init() {
	}

	public static boolean supportsGpuAcceleration() {
		return glTfSupport.isSupported() && GL_ARB_explicit_attrib_location;
	}
}
