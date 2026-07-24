package fun.qu_an.minecraft.asyncparticles.client.core.backend;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.util.List;
import java.util.Locale;

public class Backends {
	public static final GlCommands gl;
	public static final GlCommands.TransformFeedback glTf;
	public static final GlCommands.ComputeShader glCs;
	public static final VkCommands vk;
	public static final Backend backend;

	static {
		GpuDevice device = RenderSystem.getDevice();
		String backendName = device.getBackendName();
		if (backendName.toLowerCase(Locale.ROOT).contains("opengl")) {
			String glVersion = GL11C.glGetString(GL11C.GL_VERSION);
			String glRenderer = GL11C.glGetString(GL11C.GL_RENDERER);
			String driverInfo = device.getVendor();
			boolean GL_ES = (glVersion != null && glVersion.toLowerCase(Locale.ROOT).contains("opengl es"))
				|| (glRenderer != null && glRenderer.toLowerCase(Locale.ROOT).contains("opengl es"))
				|| (driverInfo.toLowerCase(Locale.ROOT).contains("opengl es"));
			GLCapabilities glCapabilities = org.lwjgl.opengl.GL.getCapabilities();

			List<String> enabledExtensions = device.getEnabledExtensions();
			boolean GL_ARB_direct_state_access = enabledExtensions.contains("GL_ARB_direct_state_access");
			boolean GL_ARB_vertex_attrib_binding = enabledExtensions.contains("GL_ARB_vertex_attrib_binding");

			gl = getGl(GL_ES, GL_ARB_direct_state_access, GL_ARB_vertex_attrib_binding);
			glTf = getGlTf(glCapabilities);
			glCs = getGlCs(GL_ES, glCapabilities);
			vk = new VkCommands.Unsupported();
			backend = GL_ES ? Backend.OPENGL_ES : Backend.OPENGL;
		} else if (backendName.toLowerCase(Locale.ROOT).contains("vulkan")) {
			gl = new GlCommands.Unsupported();
			glTf = new GlCommands.TransformFeedback.Unsupported();
			glCs = new GlCommands.ComputeShader.Unsupported();
			vk = getVkCaps();
			backend = Backend.VULKAN;
		} else {
			throw new ExceptionInInitializerError("Unsupported backend: " + backendName);
		}
	}

	private static GlCommands getGl(boolean GL_ES, boolean GL_ARB_direct_state_access, boolean GL_ARB_vertex_attrib_binding) {
		if (GL_ES) {
			return new GlCommands.GL_ES(
				GL_ARB_direct_state_access,
				GL_ARB_vertex_attrib_binding);
		} else {
			return new GlCommands.GL(
				GL_ARB_direct_state_access,
				GL_ARB_vertex_attrib_binding
			);
		}
	}

	private static GlCommands.TransformFeedback getGlTf(GLCapabilities glCapabilities) {
		if (glCapabilities.OpenGL45) {
			return new GlCommands.TransformFeedback.GL45();
		} else if (glCapabilities.OpenGL40) {
			return new GlCommands.TransformFeedback.GL40();
		} else if (glCapabilities.GL_ARB_transform_feedback2) {
			return new GlCommands.TransformFeedback.ARB2();
		} else if (glCapabilities.OpenGL30) {
			return new GlCommands.TransformFeedback.GL30();
		} else {
			return new GlCommands.TransformFeedback.Unsupported(); // impossible
		}
	}

	private static GlCommands.ComputeShader getGlCs(boolean GL_ES, GLCapabilities glCapabilities) {
		if (GL_ES) {
			return new GlCommands.ComputeShader.Unsupported();
		} else if (glCapabilities.OpenGL43) {
			return new GlCommands.ComputeShader.GL43();
		} else if (glCapabilities.GL_ARB_compute_shader &&
			glCapabilities.GL_ARB_shader_storage_buffer_object &&
			glCapabilities.GL_ARB_shader_atomic_counters) {
			return new GlCommands.ComputeShader.ARB();
		} else {
			return new GlCommands.ComputeShader.Unsupported();
		}
	}

	private static VkCommands getVkCaps() {
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
		return new VkCommands.Vk(pushDescriptor, synchronization2);
	}
	public static void init() {
	}

	public static boolean supportsGpuAcceleration() {
		if (isGl()) {
			return glTf.isSupported();
		}
		if (isVk()) {
			return vk.pushDescriptor() && vk.synchronization2();
		}
		return false;
	}

	public static boolean isGl() {
		return backend == Backend.OPENGL || backend == Backend.OPENGL_ES;
	}

	public static boolean isVk() {
		return backend == Backend.VULKAN;
	}

	public static String debugInfo() {
		GpuDevice device = RenderSystem.getDevice();
		return backend.name() + "{\n" + (Backends.isGl()
			? "[\n"
			  + gl + ",\n"
			  + glTf + ",\n"
			  + glCs
			  + "\n]"
			: vk) +
			"\n},\n"
			+ device.getEnabledExtensions();
	}
}
