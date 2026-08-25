package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull requests are merged.
 */
public class MixinTransformerExtension {
	public static Optional<List<IMixinConfig>> getPendingConfigs(Object reference) {
		Optional<IMixinTransformer> mixinTransformer = tryAs(reference);
		if (mixinTransformer.isEmpty()) {
			return Optional.empty();
		}
		Field pendingConfigs;
		Field mixinProcessor;
		try {
			Class<?> mixinTransformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
			mixinProcessor = mixinTransformerClass.getDeclaredField("processor");
			mixinProcessor.setAccessible(true);
		} catch (Exception e) {
			return Optional.empty();
		}
		try {
			Class<?> mixinProcessorClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinProcessor");
			pendingConfigs = mixinProcessorClass.getDeclaredField("pendingConfigs");
			pendingConfigs.setAccessible(true);
		} catch (Exception e) {
			return Optional.empty();
		}
		try {
			@SuppressWarnings("unchecked")
			List<IMixinConfig> value = (List<IMixinConfig>) pendingConfigs.get(mixinProcessor.get(mixinTransformer.get()));
			return Optional.ofNullable(value);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static Optional<IMixinTransformer> tryAs(Object reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.MixinTransformer")) {
			return Optional.of((IMixinTransformer) reference);
		}
		Object delegate;
		try {
			Field field = recursiveGetDelegate(reference.getClass());
			field.setAccessible(true);
			delegate = field.get(reference);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			return Optional.empty();
		}
		if (delegate instanceof IMixinTransformer) {
			return tryAs(delegate);
		} else {
			return Optional.empty();
		}
	}

	private static Field recursiveGetDelegate(Class<?> reference) throws NoSuchFieldException {
		if (!IMixinTransformer.class.isAssignableFrom(reference)) {
			throw new NoSuchFieldException();
		}
		try {
			return reference.getDeclaredField("delegate");
		} catch (NoSuchFieldException e) {
			return recursiveGetDelegate(reference.getSuperclass());
		}
	}
}
