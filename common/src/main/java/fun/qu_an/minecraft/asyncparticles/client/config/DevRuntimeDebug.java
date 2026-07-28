package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.GlCommands;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import org.lwjgl.opengl.GL;

public class DevRuntimeDebug {
	public static boolean syncAllParticles = false;
	public static TransformFeedbackGlCommand transformFeedbackGlCommand = TransformFeedbackGlCommand.AUTO;
	private static boolean changed = false;

	public static boolean isSyncAllParticles() {
		return syncAllParticles;
	}

	public static void apply() {
		if (changed || transformFeedbackGlCommand != TransformFeedbackGlCommand.AUTO) {
			if (!Backends.isGl()) {
				transformFeedbackGlCommand = TransformFeedbackGlCommand.AUTO;
				changed = false;
			} else {
				Backends.glTf = switch (transformFeedbackGlCommand) {
					case AUTO -> Backends.getGlTf(GL.getCapabilities());
					case GL30 -> new GlCommands.TransformFeedback.GL30();
					case ARB2 -> new GlCommands.TransformFeedback.ARB2();
					case GL40 -> new GlCommands.TransformFeedback.GL40();
					case GL45 -> new GlCommands.TransformFeedback.GL45();
				};
				GpuParticleBehavior.getInstance().close();
				changed = transformFeedbackGlCommand != TransformFeedbackGlCommand.AUTO;
			}
		}
	}

	public enum TransformFeedbackGlCommand {
		AUTO,
		GL30,
		ARB2,
		GL40,
		GL45
	}
}
