package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.config.StringListListEntryFixRestart;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.*;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.MixinConfigObj;
import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.getToSaveConfig;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	public static Runnable buildCategory(ConfigCategory mixinCategory,
										 ConfigEntryBuilder entryBuilder,
										 ConfigEntryBuilder revertEntryBuilder) {
		MixinConfigObj defaultConfig = new MixinConfigObj();
		MixinConfigObj newConfig = new MixinConfigObj();
		MixinConfigObj lastConfig = getToSaveConfig();
		List<String> lastNoCulling = List.copyOf(lastConfig.getNoCulling());
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap"),
				lastConfig.isSafeClassInstanceMultiMap())
			.setDefaultValue(defaultConfig.isSafeClassInstanceMultiMap())
			.setSaveConsumer(newConfig::setSafeClassInstanceMultiMap)
			.setTooltipSupplier(() -> {
				if ((!IRONS_SPELLBOOKS_LOADED ||
					 !IRONS_SPELLBOOKS_LESS_THAN_3_13_0) &&
					!MAKE_BUBBLES_POP_LOADED) {
					return Optional.of(new Component[]{
						Component.translatable("text.cloth-config.restart_required")
							.withStyle(ChatFormatting.DARK_RED),
						Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip")
					});
				} else {
					ArrayList<Component> list = new ArrayList<>();

					list.add(Component.translatable("text.cloth-config.restart_required")
						.withStyle(ChatFormatting.DARK_RED));
					list.add(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip")
						.withStyle(ChatFormatting.STRIKETHROUGH));
					if (IRONS_SPELLBOOKS_LOADED &&
						IRONS_SPELLBOOKS_LESS_THAN_3_13_0) {
						list.add(Component.translatable("config.asyncparticles.limited", "Iron's Spells 'n Spellbooks")
							.withStyle(ChatFormatting.DARK_RED));
					}
					if (MAKE_BUBBLES_POP_LOADED) {
						list.add(Component.translatable("config.asyncparticles.limited", "Make Bubbles Pop")
							.withStyle(ChatFormatting.DARK_RED));
					}
					return Optional.of(list.toArray(new Component[0]));
				}
			})
			.requireRestart()
			.setRequirement(() -> (!IRONS_SPELLBOOKS_LOADED ||
								   !IRONS_SPELLBOOKS_LESS_THAN_3_13_0) &&
								  !MAKE_BUBBLES_POP_LOADED)
			.build());
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
