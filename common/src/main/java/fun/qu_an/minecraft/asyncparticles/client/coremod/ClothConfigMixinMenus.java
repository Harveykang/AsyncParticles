package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
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
	public static Object buildCategory(ConfigCategory mixinCategory,
									   ConfigEntryBuilder entryBuilder,
									   ConfigEntryBuilder revertEntryBuilder) {
		Mixin$Particle defaultConfig = new Mixin$Particle();
		Mixin$Particle newConfig = new Mixin$Particle();
		Mixin$Particle lastConfig = getToSaveConfig();
		mixinCategory.addEntry(entryBuilder
			.startBooleanToggle(Component.translatable("config.asyncparticles.mixin.particle.redirectFleroviumCulling"),
				lastConfig.isRedirectFleroviumCulling())
			.setDefaultValue(defaultConfig.isRedirectFleroviumCulling())
			.setSaveConsumer(newConfig::setRedirectFleroviumCulling)
			.setTooltipSupplier(() -> {
				if (!ModListHelper.FORGE_FLEROVIUM_LOADED || !ModListHelper.SHIMMER_LOADED) {
					return Optional.of(new Component[]{
						Component.translatable("config.asyncparticles.mixin.particle.redirectFleroviumCulling.tooltip")
					});
				} else {
					return Optional.of(new Component[]{
						Component.translatable("config.asyncparticles.mixin.particle.redirectFleroviumCulling.tooltip")
							.withStyle(ChatFormatting.STRIKETHROUGH),
						Component.translatable("config.asyncparticles.incompatibility", "Shimmer")
							.withStyle(ChatFormatting.DARK_RED)
					});
				}
			})
			.requireRestart()
			.setRequirement(() -> ModListHelper.FORGE_FLEROVIUM_LOADED && !ModListHelper.SHIMMER_LOADED)
			.build());
		List<String> lastNoCulling = List.copyOf(lastConfig.getNoCulling());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
				lastNoCulling)
			.setDefaultValue(lastNoCulling)
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
		List<String> lastNoLightCache = List.copyOf(lastConfig.getNoLightCache());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
				lastNoLightCache)
			.setDefaultValue(lastNoLightCache)
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
		List<String> lastLockProvider = List.copyOf(lastConfig.getLockProvider());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockProvider"), lastLockProvider)
			.setDefaultValue(lastLockProvider)
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
		List<String> lastLockRequired = List.copyOf(lastConfig.getLockRequired());
		mixinCategory.addEntry(new StringListListEntryFixRestart(revertEntryBuilder
			.startStrList(Component.translatable("config.asyncparticles.mixin.particle.lockRequired"), lastLockRequired)
			.setDefaultValue(lastLockRequired)
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
		AsyncParticlesMixinConfig.setAndSave((Mixin$Particle) newConfig);
	}
}
