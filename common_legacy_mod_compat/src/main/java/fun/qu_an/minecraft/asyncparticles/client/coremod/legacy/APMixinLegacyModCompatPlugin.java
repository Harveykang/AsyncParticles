package fun.qu_an.minecraft.asyncparticles.client.coremod.legacy;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

public class APMixinLegacyModCompatPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {

	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	private static final int PACKAGE_LENGTH = AsyncParticlesClient.class.getPackage().getName().length() +
											  ".mixin.legacy.".length();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixinPackageName = mixinClassName.substring(PACKAGE_LENGTH);
		String[] split = mixinPackageName.split("\\.");
		if (split.length == 1) {
			throw new IllegalArgumentException("Unknown mixin: " + mixinClassName);
		}
		return switch (split[0]) {
			case "fabric" -> {
				if (split.length == 2) {
					yield !IS_FORGE;
				}
				yield switch (split[1]) {
					case "particlerain" -> FABRIC_PARTICLERAIN_LOADED && IS_LEGACY_PARTICLERAIN;
					case "particlerain_vs" -> FABRIC_PARTICLERAIN_LOADED && IS_LEGACY_PARTICLERAIN && VS_LOADED;
					case "particlerain_create" -> FABRIC_PARTICLERAIN_LOADED && IS_LEGACY_PARTICLERAIN && CREATE_LOADED;
					default -> throw new IllegalArgumentException("Unknown fabric mixin: " + mixinClassName);
				};
			}
			case "flywheel" -> FLYWHEEL_LOADED &&
							   versionCheck("flywheel", "0.6", "1.0");
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
