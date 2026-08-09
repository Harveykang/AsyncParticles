package fun.qu_an.minecraft.asyncparticles.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collector;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.COSYCRITTERS_LOADED;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.MAKE_BUBBLES_POP_LOADED;
import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesMixinConfig.MixinConfigObj;
import static fun.qu_an.minecraft.asyncparticles.client.config.ClothConfigMenus.modifyOriginal;

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
				if (MAKE_BUBBLES_POP_LOADED || COSYCRITTERS_LOADED) {
					return limitedTooltip(Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip"),
						MAKE_BUBBLES_POP_LOADED ? "Make Bubbles Pop" : null,
						COSYCRITTERS_LOADED ? "CosyCritters" : null);
				} else {
					return Optional.of(new Component[]{
						Component.translatable("text.cloth-config.restart_required")
							.withStyle(ChatFormatting.DARK_RED),
						Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap.tooltip")
					});
				}
			})
			.requireRestart()
			.setRequirement(() -> !MAKE_BUBBLES_POP_LOADED && !COSYCRITTERS_LOADED)
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
		List<String> lastAsyncTickableParticleGroups = List.copyOf(displayConfig.getAsyncTickableParticleGroups());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.asyncTickableGroup"),
				lastAsyncTickableParticleGroups)
			.setDefaultValue(lastAsyncTickableParticleGroups)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getAsyncTickableParticleGroups().contains(s)))
			.setSaveConsumer(l -> {
				LinkedHashSet<String> s = new LinkedHashSet<>(l);
				s.addAll(defaultConfig.getAsyncTickableParticleGroups());
				displayConfig.setAsyncTickableParticleGroups(Collections.unmodifiableSet(s));
			})
			.setTooltip(
				Component.translatable("text.cloth-config.restart_required")
					.withStyle(ChatFormatting.DARK_RED),
				Component.translatable("config.asyncparticles.mixin.tooltip"))
			.requireRestart()
			.build()));
		List<String> lastNoLightCache = List.copyOf(displayConfig.getNoLightCache());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
				lastNoLightCache)
			.setDefaultValue(lastNoLightCache)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoLightCache().contains(s)))
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
			.build()));
		List<String> lastLockProvider = List.copyOf(displayConfig.getLockProvider());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockProvider"), lastLockProvider)
			.setDefaultValue(lastLockProvider)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockProvider().contains(s)))
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
			.build()));
		List<String> lastLockRequired = List.copyOf(displayConfig.getLockRequired());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockRequired"), lastLockRequired)
			.setDefaultValue(lastLockRequired)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getLockRequired().contains(s)))
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
			.build()));
		List<String> lastReplaceRandom = List.copyOf(displayConfig.getReplaceRandom());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.replaceRandom"), lastReplaceRandom)
			.setDefaultValue(lastReplaceRandom)
			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getReplaceRandom().contains(s)))
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
			.build()));
		mixinCategory.addEntry(modifyOriginal(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource"),
				displayConfig.isSafeLegacyRandomSource())
			.setDefaultValue(defaultConfig.isSafeLegacyRandomSource())
			.setSaveConsumer(displayConfig::setSafeLegacyRandomSource)
			.setTooltip(Component.translatable("config.asyncparticles.mixin.particle.safeLegacyRandomSource.tooltip"))
//			.requireRestart()
			.build(), originalConfig.isSafeLegacyRandomSource()));
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
	                                        @SuppressWarnings("rawtypes") List<AbstractConfigListEntry> createEntries) {
		MixinConfigObj defaultConfig = AsyncParticlesMixinConfig.getDefaultConfig();
		MixinConfigObj newConfig = AsyncParticlesMixinConfig.getDefaultConfig();
		MixinConfigObj lastConfig = AsyncParticlesMixinConfig.getCurrentConfig();
//		List<String> contraptionNoParticleCollision = List.copyOf(lastConfig.getContraptionNoParticleCollision());
//		createEntries.add(new StringListListEntryFixRestart(mixinEntryBuilder
//			.startStrList(Component.translatable("config.asyncparticles.mixin.create.contraptionsNoParticleCollision"), contraptionNoParticleCollision)
//			.setDefaultValue(contraptionNoParticleCollision)
//			.setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getContraptionNoParticleCollision().contains(s)))
//			.setSaveConsumer(l -> {
//				LinkedHashSet<String> s = new LinkedHashSet<>(l);
//				s.addAll(defaultConfig.getContraptionNoParticleCollision());
//				newConfig.setContraptionNoParticleCollision(Collections.unmodifiableSet(s));
//			})
//			.setTooltip(
//				Component.translatable("text.cloth-config.restart_required")
//					.withStyle(ChatFormatting.DARK_RED),
//				Component.translatable("config.asyncparticles.mixin.create.contraptionsNoParticleCollision.tooltip"),
//				Component.translatable("config.asyncparticles.mixin.tooltip"))
//			.requireRestart()
//			.build()));
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
