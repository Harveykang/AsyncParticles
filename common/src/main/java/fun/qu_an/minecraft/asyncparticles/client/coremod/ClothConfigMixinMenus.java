package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.config.StringListListEntryFixRestart;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
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

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.Mixin$Particle;
import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.getToSaveConfig;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	public static Runnable buildCategory(ConfigCategory mixinCategory,
										 ConfigEntryBuilder entryBuilder,
										 ConfigEntryBuilder revertEntryBuilder) {
		Mixin$Particle defaultConfig = new Mixin$Particle();
		Mixin$Particle newConfig = new Mixin$Particle();
		Mixin$Particle lastConfig = getToSaveConfig();
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap"),
				lastConfig.isSafeClassInstanceMultiMap())
			.setDefaultValue(defaultConfig.isSafeClassInstanceMultiMap())
			.setSaveConsumer(newConfig::setSafeClassInstanceMultiMap)
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
				.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip"))
			.requireRestart()
			.build());
		List<String> lastNoCulling = List.copyOf(lastConfig.getNoCulling());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
				lastNoCulling)
			.setDefaultValue(lastNoCulling)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoCulling().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getNoCulling());
				newConfig.setNoCulling(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> lastNoLightCache = List.copyOf(lastConfig.getNoLightCache());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
				lastNoLightCache)
			.setDefaultValue(lastNoLightCache)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoLightCache().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getNoLightCache());
				newConfig.setNoLightCache(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> lastLockProvider = List.copyOf(lastConfig.getLockProvider());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockProvider"), lastLockProvider)
			.setDefaultValue(lastLockProvider)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockProvider().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getLockProvider());
				newConfig.setLockProvider(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> lastLockRequired = List.copyOf(lastConfig.getLockRequired());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockRequired"), lastLockRequired)
			.setDefaultValue(lastLockRequired)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockRequired().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getLockRequired());
				newConfig.setLockRequired(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> lastReplaceRandom = List.copyOf(lastConfig.getReplaceRandom());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.replaceRandom"), lastReplaceRandom)
			.setDefaultValue(lastReplaceRandom)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getReplaceRandom().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getReplaceRandom());
				newConfig.setReplaceRandom(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("config.asyncparticles.mixin.replaceRandom.tooltip"),
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource"),
				lastConfig.isSafeLegacyRandomSource())
			.setDefaultValue(defaultConfig.isSafeLegacyRandomSource())
			.setSaveConsumer(newConfig::setSafeLegacyRandomSource)
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource.tooltip"))
			.requireRestart()
			.build());
		return () -> {
			try {
				newConfig.flat();
				AsyncParticlesMixinConfig.save(newConfig);
			} catch (IOException e) {
				throw ExceptionUtil.toThrowDirectly(e);
			}
		};
	}

	private static Optional<Component> testParticleClass(String s, boolean b) {
		if (b) {
			return Optional.empty();
		}
		Class<?> aClass;
		try {
			aClass = Class.forName(s);
		} catch (ClassNotFoundException e) {
			return Optional.of(Component.translatable("config.asyncparticles.mixin.particle.invalid-class"));
		}
		if (!Particle.class.isAssignableFrom(aClass)) {
			return Optional.of(Component.translatable("config.asyncparticles.mixin.particle.invalid-class"));
		}
		return Optional.empty();
	}
}
