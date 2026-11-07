package fun.qu_an.minecraft.asyncparticles.client.coremod.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormEarlyCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneEarlyCompat;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjusterRegistrar;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

public class APMixinPluginNeoForge implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		MixinClassAdjusterRegistrar.register(new AdjusterContraptionNoParticleCollision());
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	//	private static final int L = "fun.qu_an.minecraft.asyncparticles.client.mixin.".length();
	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.".length();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!IS_CLIENT) {
			return false;
		}
		String mixinPackageName = mixinClassName.substring(PACKAGE_LENGTH);
		String[] split = mixinPackageName.split("\\.");
		if (split.length == 1) {
			return true;
		}
		return switch (split[0]) {
			case "neoforge" -> IS_FORGE;
			case "compat" -> "neoforge".equals(split[1]) && switch (split[2]) {
				case "particlestorm" -> FORGE_PARTICLE_STORM_LOADED && ParticleStormEarlyCompat.AVAILABLE
					&& switch (split[3]) {
						case "v1_4_x" -> !FORGE_PARTICLE_STORM_LATER_THAN_1_5_0;
						case "v1_5_0" -> FORGE_PARTICLE_STORM_LATER_THAN_1_5_0;
						default -> true;
					};
				case "polytone_create" -> FORGE_CREATE_LOADED && PolytoneEarlyCompat.AVAILABLE;
				case "create" -> FORGE_CREATE_LOADED;
				case "sable_create" -> SABLE_LOADED && FORGE_CREATE_LOADED;
				case "simple_weather" -> FORGE_SIMPLE_WEATHER_LOADED;
				case "simple_weather_create" -> FORGE_SIMPLE_WEATHER_LOADED && CREATE_LOADED;
				case "weather2" -> FORGE_WEATHER2_LOADED;
				case "weather2_create" -> FORGE_WEATHER2_LOADED && CREATE_LOADED;
				case "weather2_vs" -> FORGE_WEATHER2_LOADED && VS_LOADED;
				default -> throw new IllegalArgumentException("Unknown neoforge compat mixin: " + mixinClassName);
			};
			default -> throw new IllegalArgumentException("Unknown mixin: " + mixinClassName);
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}
}
