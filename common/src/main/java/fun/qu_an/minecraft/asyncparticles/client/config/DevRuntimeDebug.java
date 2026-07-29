package fun.qu_an.minecraft.asyncparticles.client.config;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backend;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.GlCommands;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.util.TriState;
import org.lwjgl.opengl.GL;

import java.util.List;
import java.util.Set;

public class DevRuntimeDebug {
	public static boolean syncAllParticles = false;
	public static TransformFeedbackGlCommand transformFeedbackGlCommand = TransformFeedbackGlCommand.AUTO;
	public static TriState directStateAccess = TriState.DEFAULT;
	private static boolean changed = false;

	public static boolean isSyncAllParticles() {
		return syncAllParticles;
	}

	public static void apply() {
		if (changed || transformFeedbackGlCommand != TransformFeedbackGlCommand.AUTO || directStateAccess != TriState.DEFAULT) {
			if (!Backends.isGl()) {
				transformFeedbackGlCommand = TransformFeedbackGlCommand.AUTO;
				directStateAccess = TriState.DEFAULT;
				changed = false;
			} else {
				Backends.glTf = switch (transformFeedbackGlCommand) {
					case AUTO -> Backends.getGlTf(GL.getCapabilities());
					case GL30 -> new GlCommands.TransformFeedback.GL30();
					case ARB2 -> new GlCommands.TransformFeedback.ARB2();
					case GL45 -> new GlCommands.TransformFeedback.GL45();
				};
				Backends.gl = switch (directStateAccess) {
					case TRUE -> Backends.getGl(Backends.backend == Backend.OPENGL_ES, true, Backends.gl.vertexAttribBinding());
					case FALSE -> Backends.getGl(Backends.backend == Backend.OPENGL_ES, false, Backends.gl.vertexAttribBinding());
					case DEFAULT -> {
						Set<String> enabledExtensions = RenderSystem.getDevice().getDeviceInfo().underlyingExtensions();
						boolean GL_ARB_direct_state_access = enabledExtensions.contains("GL_ARB_direct_state_access");
						yield Backends.getGl(Backends.backend == Backend.OPENGL_ES, GL_ARB_direct_state_access, Backends.gl.vertexAttribBinding());
					}
				};
				GpuParticleBehavior.getInstance().close();
				changed = transformFeedbackGlCommand != TransformFeedbackGlCommand.AUTO || directStateAccess != TriState.DEFAULT;
			}
		}
	}

	public enum TransformFeedbackGlCommand {
		AUTO,
		GL30,
		ARB2,
		GL45
	}
}
