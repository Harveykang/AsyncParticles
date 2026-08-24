package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public record LevelBundle(
	ClientLevel level,
	LocalPlayer player,
	Entity cameraEntity) {
	public static boolean isLevelRunning() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.player != null && mc.getCameraEntity() != null && !mc.isPaused();
	}

	public static boolean isLevelAvailable() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.player != null && mc.getCameraEntity() != null;
	}

	public boolean isLevelReset() {
		Minecraft mc = Minecraft.getInstance();
		return level != mc.level || player != mc.player || cameraEntity != mc.getCameraEntity();
	}
}
