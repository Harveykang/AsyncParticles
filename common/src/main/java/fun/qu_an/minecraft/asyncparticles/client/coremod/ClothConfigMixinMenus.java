package fun.qu_an.minecraft.asyncparticles.client.coremod;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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
        mixinCategory
            .addEntry(entryBuilder
                .startStrList(Component.translatable("config.asyncparticles.mixin.particle.noCulling"),
                    List.copyOf(config.getNoCulling()))
                .setDefaultValue(List.copyOf(defaultConfig.getNoCulling()))
                .setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoCulling().contains(s)))
                .setSaveConsumer(l -> newConfig.setNoCulling(l.isEmpty()
                    ? defaultConfig.getNoCulling()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(l))))
                .setTooltip(Component.translatable("config.asyncparticles.mixin.delete-all-to-reset"))
                .requireRestart()
                .build())
            .addEntry(entryBuilder
                .startStrList(Component.translatable("config.asyncparticles.mixin.particle.noLightCache"),
                    List.copyOf(config.getNoLightCache()))
                .setDefaultValue(List.copyOf(defaultConfig.getNoLightCache()))
                .setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getNoLightCache().contains(s)))
                .setSaveConsumer(l -> newConfig.setNoLightCache(l.isEmpty()
                    ? defaultConfig.getNoLightCache()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(l))))
                .setTooltip(Component.translatable("config.asyncparticles.mixin.delete-all-to-reset"))
                .requireRestart()
                .build())
            .addEntry(entryBuilder
                .startStrList(Component.translatable("config.asyncparticles.mixin.particle.spinLockRequired"),
                    List.copyOf(config.getSpinLockRequired()))
                .setDefaultValue(List.copyOf(defaultConfig.getSpinLockRequired()))
                .setCellErrorSupplier(s -> testParticleClass(s, defaultConfig.getSpinLockRequired().contains(s)))
                .setSaveConsumer(l -> newConfig.setSpinLockRequired(l.isEmpty()
                    ? defaultConfig.getSpinLockRequired()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(l))))
                .setTooltip(Component.translatable("config.asyncparticles.mixin.delete-all-to-reset"))
                .requireRestart()
                .build());
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
