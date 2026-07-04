package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.vulkanmod.vulkan.Vulkan;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.List;
import java.util.Locale;

public class BackendCaps {
	public static final boolean GL_ARB_explicit_attrib_location;
	public static final boolean GL_ARB_direct_state_access;
	public static final boolean GL_ARB_vertex_attrib_binding;
	public static final GLCaps.TfSupport glTfSupport;
	public static final GLCaps.CsSupport glCsSupport;
	public static final VKCaps vkCaps;
	private static final boolean isGl;

	static {
		GpuDevice device = RenderSystem.getDevice();
		String backendName = device.getBackendName();
		if (backendName.toLowerCase(Locale.ROOT).contains("opengl")) {
			GLCapabilities glCaps = GL.getCapabilities();
			GL_ARB_explicit_attrib_location = glCaps.OpenGL33 ||
				glCaps.GL_ARB_explicit_attrib_location;
			List<String> enabledExtensions = device.getEnabledExtensions();
			GL_ARB_direct_state_access = enabledExtensions.contains("GL_ARB_direct_state_access");
			GL_ARB_vertex_attrib_binding = enabledExtensions.contains("GL_ARB_vertex_attrib_binding");
			glTfSupport = getGlTfSupport(glCaps);
			glCsSupport = getGlCsSupport(glCaps);
			vkCaps = new VKCaps.Unsupported();
			isGl = true;
		} else if (backendName.toLowerCase(Locale.ROOT).contains("vulkan")) {
			GL_ARB_explicit_attrib_location = false;
			GL_ARB_direct_state_access = false;
			GL_ARB_vertex_attrib_binding = false;
			glTfSupport = new GLCaps.TfSupport.Unsupported();
			glCsSupport = new GLCaps.CsSupport.Unsupported();
			vkCaps = getVkCaps();
			isGl = false;
		} else {
			throw new ExceptionInInitializerError("Unsupported backend: " + backendName);
		}
	}

	private static GLCaps.@NonNull TfSupport getGlTfSupport(GLCapabilities glCaps) {
		if (glCaps.OpenGL45) {
			return new GLCaps.TfSupport.GL_45();
		} else if (glCaps.OpenGL40) {
			return new GLCaps.TfSupport.GL_40();
		} else if (glCaps.GL_ARB_transform_feedback2) {
			return new GLCaps.TfSupport.ARB_2();
		} else if (glCaps.OpenGL30) {
			return new GLCaps.TfSupport.GL_30();
		} else {
			return new GLCaps.TfSupport.Unsupported(); // impossible
		}
	}

	private static GLCaps.@NonNull CsSupport getGlCsSupport(GLCapabilities glCaps) {
		if (glCaps.OpenGL43) {
			return new GLCaps.CsSupport.GL_43();
		} else if (glCaps.GL_ARB_compute_shader &&
			glCaps.GL_ARB_shader_storage_buffer_object &&
			glCaps.GL_ARB_shader_atomic_counters) {
			return new GLCaps.CsSupport.ARB();
		} else {
			return new GLCaps.CsSupport.Unsupported();
		}
	}

	private static VKCaps.@NonNull VKCapsImpl getVkCaps() {
		// check device capabilities via Vulkan API directly
		VkDevice vkDevice = Vulkan.getVkDevice();
		boolean isVk13;
		try (MemoryStack s = MemStackUtil.stackPush()) {
			VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(s);
			VK10.vkGetPhysicalDeviceProperties(vkDevice.getPhysicalDevice(), props);
			isVk13 = props.apiVersion() >= VK13.VK_API_VERSION_1_3;
		}
		boolean pushDescriptor;
		boolean synchronization2;
		if (isVk13) {
			pushDescriptor = true;
			synchronization2 = true;
		} else {
			pushDescriptor = VK10.vkGetDeviceProcAddr(vkDevice, "vkCmdPushDescriptorSetKHR") != 0L;
			synchronization2 = VK10.vkGetDeviceProcAddr(vkDevice, "vkCmdPipelineBarrier2KHR") != 0L;
		}
		return new VKCaps.VKCapsImpl(pushDescriptor, synchronization2);
	}

	public static void init() {
	}

	public static boolean supportsGpuAcceleration() {
		if (isGl()) {
			return glTfSupport.isTfSupported() && GL_ARB_explicit_attrib_location;
		}
		return isVk();
	}

	public static boolean isGl() {
		return isGl;
	}

	public static boolean isVk() {
		return !isGl;
	}

	public static String debugInfo() {
		GpuDevice device = RenderSystem.getDevice();
		return device.getBackendName() + "{\n" + (BackendCaps.isGl()
			? BackendCaps.glTfSupport.getClass().getSimpleName()
			  + "GL_ARB_explicit_attrib_location: " + BackendCaps.GL_ARB_explicit_attrib_location
			  + ",\nGL_ARB_direct_state_access: " + BackendCaps.GL_ARB_direct_state_access
			  + ",\nGL_ARB_vertex_attrib_binding: " + BackendCaps.GL_ARB_vertex_attrib_binding
			: BackendCaps.vkCaps.getClass().getSimpleName())
			+ "\n},\n"
			+ device.getEnabledExtensions();
	}
}
