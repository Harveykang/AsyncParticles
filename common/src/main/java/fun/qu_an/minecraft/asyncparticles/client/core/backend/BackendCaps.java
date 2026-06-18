package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.util.Locale;
import java.util.Set;

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
		String backendName = device.getDeviceInfo().backendName();
		if (backendName.toLowerCase(Locale.ROOT).contains("opengl")) {
			GLCapabilities glCaps = GL.getCapabilities();
			GL_ARB_explicit_attrib_location = glCaps.OpenGL33 ||
				glCaps.GL_ARB_explicit_attrib_location; // FIXME fix this!!!
			Set<String> enabledExtensions = device.getDeviceInfo().underlyingExtensions();
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
			vkCaps = new VKCaps.Unsupported();
			isGl = true;
		} else if (backendName.toLowerCase(Locale.ROOT).contains("vulkan")) {
			GL_ARB_explicit_attrib_location = false;
			GL_ARB_direct_state_access = false;
			GL_ARB_vertex_attrib_binding = false;
			glTfSupport = new GLCaps.TfSupport.Unsupported();
			glCsSupport = new GLCaps.CsSupport.Unsupported();
			VulkanDevice vkBackend = (VulkanDevice) device.backend;
			boolean isVk12;
			try (MemoryStack s = MemStackUtil.stackPush()) {
				VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(s);
				VK10.vkGetPhysicalDeviceProperties(vkBackend.vkDevice().getPhysicalDevice(), props);
				isVk12 = props.apiVersion() >= VK12.VK_API_VERSION_1_2;
			}
			boolean isSync2 = isVk12 && VK10.vkGetDeviceProcAddr(vkBackend.vkDevice(), "vkQueueSubmit2KHR") != 0L;
			vkCaps = new VKCaps.VKCapsImpl(
				true,
				isVk12 && isSync2,
				vkBackend.computeQueue().queueFamilyIndex() == vkBackend.graphicsQueue().queueFamilyIndex()
			);
			isGl = false;
		} else {
			throw new ExceptionInInitializerError("Unsupported backend: " + backendName);
		}
	}

	public static void init() {
	}

	public static boolean supportsGpuAcceleration() {
		if (isGl()) {
			return glTfSupport.isTfSupported() && GL_ARB_explicit_attrib_location;
		}
		return vkCaps.isComputeShaderSupported();
	}

	public static boolean isGl() {
		return isGl;
	}

	public static boolean isVk() {
		return !isGl;
	}
}
