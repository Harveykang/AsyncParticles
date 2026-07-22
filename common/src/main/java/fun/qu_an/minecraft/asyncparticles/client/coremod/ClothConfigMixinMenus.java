package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.config.StringListListEntryFixRestart;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.*;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	public static Runnable buildCategory(ConfigCategory mixinCategory,
	                                     ConfigEntryBuilder entryBuilder,
	                                     ConfigEntryBuilder revertEntryBuilder) {
		MixinConfigObj defaultConfig = new MixinConfigObj();
		MixinConfigObj newConfig = new MixinConfigObj();
		MixinConfigObj lastConfig = getToSaveConfig();
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.splitTick"),
				lastConfig.isParticleSplitTick())
			.setDefaultValue(defaultConfig.isParticleSplitTick())
			.setSaveConsumer(newConfig::setParticleSplitTick)
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.particle.splitTick.tooltip"))
			.requireRestart()
			.build());
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap"),
				lastConfig.isSafeClassInstanceMultiMap())
			.setDefaultValue(defaultConfig.isSafeClassInstanceMultiMap())
			.setSaveConsumer(newConfig::setSafeClassInstanceMultiMap)
			.setTooltipSupplier(() -> {
				if ((IRONS_SPELLBOOKS_LOADED &&
					IRONS_SPELLBOOKS_LESS_THAN_3_13_0) ||
					MAKE_BUBBLES_POP_LOADED ||
					COSYCRITTERS_LOADED ||
					IMMERSIVE_PORTALS_LOADED) {
					return limitedTooltip(
						Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip"),
						IRONS_SPELLBOOKS_LOADED ? "Irons Spellbooks" : null,
						MAKE_BUBBLES_POP_LOADED ? "Make Bubbles Pop" : null,
						COSYCRITTERS_LOADED ? "CosyCritters" : null,
						IMMERSIVE_PORTALS_LOADED ? "Immersive Portals" : null
					);
				} else {
					return Optional.of(new Component[]{
						Component.translatable("text.cloth-config.restart_required")
							.withStyle(ChatFormatting.DARK_RED),
						Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip")
					});
				}
			})
			.requireRestart()
			.setRequirement(() -> (!IRONS_SPELLBOOKS_LOADED ||
				!IRONS_SPELLBOOKS_LESS_THAN_3_13_0) &&
				!MAKE_BUBBLES_POP_LOADED &&
				!COSYCRITTERS_LOADED &&
				!IMMERSIVE_PORTALS_LOADED)
			.build());
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap"),
				lastConfig.isSafeBlockEntityMap())
			.setDefaultValue(defaultConfig.isSafeBlockEntityMap())
			.setSaveConsumer(newConfig::setSafeBlockEntityMap)
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap.tooltip"))
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
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.replaceRandom.tooltip"),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource"),
				lastConfig.isSafeLegacyRandomSource())
			.setDefaultValue(defaultConfig.isSafeLegacyRandomSource())
			.setSaveConsumer(newConfig::setSafeLegacyRandomSource)
			.setTooltip(
				Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource.tooltip"))
//			.requireRestart()
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

	public static void addModCompatCategory(ConfigEntryBuilder entryBuilder,
	                                        ConfigEntryBuilder mixinEntryBuilder,
	                                        @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> vsEntries,
	                                        @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> sableEntries,
	                                        @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> createEntries) {
		MixinConfigObj defaultConfig = new MixinConfigObj();
		MixinConfigObj newConfig = new MixinConfigObj();
		MixinConfigObj lastConfig = getToSaveConfig();
		List<String> contraptionNoParticleCollision = List.copyOf(lastConfig.getContraptionNoParticleCollision());
		createEntries.add(new StringListListEntryFixRestart(mixinEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.create.contraptionsNoParticleCollision"), contraptionNoParticleCollision)
			.setDefaultValue(contraptionNoParticleCollision)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getContraptionNoParticleCollision().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getContraptionNoParticleCollision());
				newConfig.setContraptionNoParticleCollision(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.create.contraptionsNoParticleCollision.tooltip"),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
	}

	private static Optional<Component[]> limitedTooltip(MutableComponent description, Object... modNames) {
		Component modNamesStr = Arrays.stream(modNames)
			.filter(Objects::nonNull)
			.map(modName -> modName instanceof Component ? (Component) modName : Component.literal(String.valueOf(modName)))
			.collect(Collector.of(Component::empty, MutableComponent::append, (a, b) -> a.append(", ").append(b)));

		return Optional.of(new MutableComponent[]{
			description.withStyle(ChatFormatting.STRIKETHROUGH),
			Component.translatable("config.asyncparticles.limited", modNamesStr)
				.withStyle(ChatFormatting.YELLOW)
		});
	}
}
