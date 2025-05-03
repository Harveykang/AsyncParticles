package fun.qu_an.minecraft.asyncparticles.client.coremod;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.*;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	public static void buildCategory(ConfigCategory mixinCategory, ConfigEntryBuilder entryBuilder) {
		Mixin$Particle defaultConfig = new Mixin$Particle();
		mixinCategory
			.addEntry(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
					List.copyOf(config.noCulling))
				.setDefaultValue(List.copyOf(defaultConfig.noCulling))
				.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.noCulling.contains(s)))
				.requireRestart()
				.build())
			.addEntry(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
					List.copyOf(config.noLightCache))
				.setDefaultValue(List.copyOf(defaultConfig.noLightCache))
				.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.noLightCache.contains(s)))
				.requireRestart()
				.build())
			.addEntry(entryBuilder
				.startStrList(Component.translatable("config.asyncparticles.mixin.particle.spinLockRequired"),
					List.copyOf(config.spinLockRequired))
				.setDefaultValue(List.copyOf(defaultConfig.spinLockRequired))
				.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.spinLockRequired.contains(s)))
				.requireRestart()
				.build());
	}

	private static Optional<Component> testParticleClass(String s, boolean b) {
		if (b) {
			return Optional.empty();
		}
		Class<?> aClass;
		try {
			aClass = Class.forName(s);
		} catch (ClassNotFoundException e) {
			return java.util.Optional.of(Component.translatable("config.asyncparticles.mixin.particle.invalid-class"));
		}
		if (!Particle.class.isAssignableFrom(aClass)) {
			return java.util.Optional.of(Component.translatable("config.asyncparticles.mixin.particle.invalid-class"));
		}
		return Optional.empty();
	}

	public static void onSave() throws IOException {
		AsyncParticlesMixinConfig.save();
	}
}
