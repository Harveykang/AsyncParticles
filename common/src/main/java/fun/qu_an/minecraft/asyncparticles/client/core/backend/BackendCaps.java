package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.List;

public class BackendCaps {
	public static final boolean GL_ARB_explicit_attrib_location;
	public static final boolean GL_ARB_direct_state_access;
	public static final boolean GL_ARB_vertex_attrib_binding;
	public static final GLCaps.TfSupport glTfSupport;
	public static final GLCaps.CsSupport glCsSupport;
	private static final boolean isGl;

	static {
		GpuDevice device = RenderSystem.getDevice();
		GLCapabilities glCaps = GL.getCapabilities();
		GL_ARB_explicit_attrib_location = glCaps.OpenGL33 ||
			glCaps.GL_ARB_explicit_attrib_location;
		List<String> enabledExtensions = device.getEnabledExtensions();
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
		if (glCaps.OpenGL43) {
			glCsSupport = new GLCaps.CsSupport.GL_43();
		} else if (glCaps.GL_ARB_compute_shader &&
			glCaps.GL_ARB_shader_storage_buffer_object &&
			glCaps.GL_ARB_shader_atomic_counters) {
			glCsSupport = new GLCaps.CsSupport.ARB();
		} else {
			glCsSupport = new GLCaps.CsSupport.Unsupported();
		}
		isGl = true;
	}

	public static void init() {
	}

	public static boolean supportsGpuAcceleration() {
		if (isGl()) {
			return glTfSupport.isTfSupported() && GL_ARB_explicit_attrib_location;
		}
		return false;
	}

	public static boolean isGl() {
		return isGl;
	}

	public static boolean isVk() {
		return !isGl;
	}
}
