package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public record LevelBundle(
	ClientLevel level,
	LocalPlayer player,
	Entity cameraEntity) {
	public boolean isLevelReset() {
		Minecraft mc = Minecraft.getInstance();
		return level != mc.level || player != mc.player || cameraEntity != mc.cameraEntity;
	}
}
