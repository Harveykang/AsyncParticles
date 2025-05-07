package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.config.StringListListEntryFixRestart;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.*;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	public static Object buildCategory(ConfigCategory mixinCategory, ConfigEntryBuilder entryBuilder) {
		Mixin$Particle defaultConfig = new Mixin$Particle();
		Mixin$Particle newConfig = new Mixin$Particle();
		List<String> defaultNoCulling = List.copyOf(config.getNoCulling());
		mixinCategory.addEntry(new StringListListEntryFixRestart(entryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
				defaultNoCulling)
			.setDefaultValue(defaultNoCulling)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoCulling().contains(s)))
			.setSaveConsumer(l -> newConfig.setNoCulling(l.isEmpty()
				? defaultConfig.getNoCulling()
				: Collections.unmodifiableSet(new LinkedHashSet<>(l))))
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> defaultNoLightCache = List.copyOf(config.getNoLightCache());
		mixinCategory.addEntry(new StringListListEntryFixRestart(entryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
				defaultNoLightCache)
			.setDefaultValue(defaultNoLightCache)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoLightCache().contains(s)))
			.setSaveConsumer(l -> newConfig.setNoLightCache(l.isEmpty()
				? defaultConfig.getNoLightCache()
				: Collections.unmodifiableSet(new LinkedHashSet<>(l))))
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> defaultLockProvider = List.copyOf(config.getLockProvider());
		mixinCategory.addEntry(new StringListListEntryFixRestart(entryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockProvider"), defaultLockProvider)
			.setDefaultValue(defaultLockProvider)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockProvider().contains(s)))
			.setSaveConsumer(l -> newConfig.setLockProvider(l.isEmpty()
				? defaultConfig.getLockProvider()
				: Collections.unmodifiableSet(new LinkedHashSet<>(l))))
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> defaultLockRequired = List.copyOf(config.getLockRequired());
		mixinCategory.addEntry(new StringListListEntryFixRestart(entryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockRequired"), defaultLockRequired)
			.setDefaultValue(defaultLockRequired)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockRequired().contains(s)))
			.setSaveConsumer(l -> newConfig.setLockRequired(l.isEmpty()
				? defaultConfig.getLockRequired()
				: Collections.unmodifiableSet(new LinkedHashSet<>(l))))
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		return newConfig;
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

	public static void onSave(Object newConfig) throws IOException {
		AsyncParticlesMixinConfig.config = (Mixin$Particle) newConfig;
		AsyncParticlesMixinConfig.save();
	}
}
