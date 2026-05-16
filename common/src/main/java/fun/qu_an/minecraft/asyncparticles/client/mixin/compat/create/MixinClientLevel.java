package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionHeightMap;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionHeightMapProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// FIXME: thread safe and reletive pos storage
@Mixin(ClientLevel.class)
public class MixinClientLevel implements ContraptionHeightMapProvider {
	@Unique
	private final ContraptionHeightMap asyncparticles$contraptionHeightMap = new ContraptionHeightMap();
	@Override
	public ContraptionHeightMap asyncparticles$getHeightMap() {
		return asyncparticles$contraptionHeightMap;
	}
}
