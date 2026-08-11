package fun.qu_an.minecraft.asyncparticles.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesMixinConfig.MixinConfigObj;
import static fun.qu_an.minecraft.asyncparticles.client.config.ClothConfigMenus.modifyOriginal;
import static fun.qu_an.minecraft.asyncparticles.client.config.ClothConfigMenus.testClass;

// No more NoClassDefFoundError
public class ClothConfigMixinMenus {
	static void buildCategory(@Nullable MixinConfigBundle bundle,
	                          ConfigCategory mixinCategory,
	                          ConfigEntryBuilder entryBuilder,
	                          ConfigEntryBuilder revertEntryBuilder) {
		if (bundle == null) {
			bundle = MixinConfigBundle.create();
		}
		AsyncParticlesMixinConfig.MixinConfigObj displayConfig = bundle.displayConfig();
		AsyncParticlesMixinConfig.MixinConfigObj defaultConfig = bundle.defaultConfig();
		AsyncParticlesMixinConfig.MixinConfigObj originalConfig = bundle.originalConfig();

		mixinCategory.addEntry(modifyOriginal(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap"),
				displayConfig.isSafeClassInstanceMultiMap())
			.setDefaultValue(defaultConfig.isSafeClassInstanceMultiMap())
			.setSaveConsumer(displayConfig::setSafeClassInstanceMultiMap)
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
			.build(), originalConfig.isSafeClassInstanceMultiMap()));
		mixinCategory.addEntry(modifyOriginal(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap"),
				displayConfig.isSafeBlockEntityMap())
			.setDefaultValue(defaultConfig.isSafeBlockEntityMap())
			.setSaveConsumer(displayConfig::setSafeBlockEntityMap)
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap.tooltip"))
			.requireRestart()
			.build(), originalConfig.isSafeBlockEntityMap()));
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
				List.copyOf(displayConfig.getNoCulling()))
			.setDefaultValue(List.copyOf(originalConfig.getNoCulling()))
			.setCellErrorSupplier(s -> testClass(s, Particle.class, s1 -> defaultConfig.getNoCulling().contains(s1)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(displayConfig.getNoCulling());
				displayConfig.setNoCulling(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		mixinCategory.addEntry(modifyOriginal(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
				List.copyOf(displayConfig.getNoLightCache()))
			.setDefaultValue(List.copyOf(originalConfig.getNoLightCache()))
			.setCellErrorSupplier(s -> testClass(s, Particle.class, s1 -> defaultConfig.getNoLightCache().contains(s1)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getNoLightCache());
				displayConfig.setNoLightCache(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()), originalConfig.getNoLightCache()));
		mixinCategory.addEntry(modifyOriginal(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockProvider"), List.copyOf(displayConfig.getLockProvider()))
			.setDefaultValue(List.copyOf(originalConfig.getLockProvider()))
			.setCellErrorSupplier(s -> testClass(s, Particle.class, s1 -> defaultConfig.getLockProvider().contains(s1)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getLockProvider());
				displayConfig.setLockProvider(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()), originalConfig.getLockProvider()));
		mixinCategory.addEntry(modifyOriginal(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockRequired"), List.copyOf(displayConfig.getLockRequired()))
			.setDefaultValue(List.copyOf(originalConfig.getLockRequired()))
			.setCellErrorSupplier(s -> testClass(s, Particle.class, s1 -> defaultConfig.getLockRequired().contains(s1)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getLockRequired());
				displayConfig.setLockRequired(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()), originalConfig.getLockRequired()));
		mixinCategory.addEntry(modifyOriginal(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.replaceRandom"), List.copyOf(displayConfig.getReplaceRandom()))
			.setDefaultValue(List.copyOf(originalConfig.getReplaceRandom()))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getReplaceRandom());
				displayConfig.setReplaceRandom(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.replaceRandom.tooltip"),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()), originalConfig.getReplaceRandom()));
		mixinCategory.addEntry(modifyOriginal(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource"),
				displayConfig.isSafeLegacyRandomSource())
			.setDefaultValue(defaultConfig.isSafeLegacyRandomSource())
			.setSaveConsumer(displayConfig::setSafeLegacyRandomSource)
			.setTooltip(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource.tooltip"))
//			.requireRestart()
			.build(), originalConfig.isSafeLegacyRandomSource()));
	}

	static void addModCompatCategory(ConfigEntryBuilder entryBuilder,
	                                 ConfigEntryBuilder mixinEntryBuilder,
	                                 MixinConfigBundle mixinBundle,
	                                 @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> vsEntries,
	                                 @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> sableEntries,
	                                 @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> createEntries) {
		MixinConfigObj defaultConfig = AsyncParticlesMixinConfig.getDefaultConfig();
		MixinConfigObj newConfig = AsyncParticlesMixinConfig.getDefaultConfig();
		MixinConfigObj lastConfig = AsyncParticlesMixinConfig.getCurrentConfig();
		List<String> contraptionNoParticleCollision = List.copyOf(lastConfig.getContraptionNoParticleCollision());
		createEntries.add(modifyOriginal(new StringListListEntryFixRestart(mixinEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.create.contraptionsNoParticleCollision"), contraptionNoParticleCollision)
			.setDefaultValue(contraptionNoParticleCollision)
			.setCellErrorSupplier(s -> testClass(s, Particle.class, s1 -> defaultConfig.getContraptionNoParticleCollision().contains(s1)))
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
			.build()), mixinBundle.originalConfig().getContraptionNoParticleCollision()));
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

	record MixinConfigBundle(
		AsyncParticlesMixinConfig.MixinConfigObj displayConfig,
		AsyncParticlesMixinConfig.MixinConfigObj defaultConfig,
		AsyncParticlesMixinConfig.@Nullable MixinConfigObj originalConfig) {
		public static MixinConfigBundle create() {
			return new MixinConfigBundle(AsyncParticlesMixinConfig.getCurrentConfig(), AsyncParticlesMixinConfig.getDefaultConfig(), null);
		}

		@Override
		public MixinConfigObj originalConfig() {
			return originalConfig == null ? displayConfig : originalConfig;
		}
	}
}
