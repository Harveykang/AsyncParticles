package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.google.common.collect.Maps;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class)
public class MixinParticleEngine {
	@Mutable
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	public static List<ParticleRenderType> RENDER_ORDER;
	@Inject(method = "<init>", order = 9500, at = @At("RETURN"))
	public void onInit(ClientLevel level, TextureManager textureManager, CallbackInfo ci) {
		List<ParticleRenderType> renderOrder = List.copyOf(RENDER_ORDER);
		AtomicInteger orderGenerator = new AtomicInteger(); // single-threaded
		Map<ParticleRenderType, Integer> insertionOrder = Collections.synchronizedMap(new IdentityHashMap<>(0));
		Map<ParticleRenderType, Queue<Particle>> newTreeMap =
			Maps.newTreeMap((o1, o2) -> {
				// FIXME: why i have to write this shit?
				if (o1 == o2) {
					return 0;
				}
				BindingTesselator bTesselator1 = AsyncRenderer.getBTesselator(o1, textureManager);
				BindingTesselator bTesselator2 = AsyncRenderer.getBTesselator(o2, textureManager);
				if (bTesselator1.shouldSync && bTesselator2.shouldSync) {
					return asyncparticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
				}
				if (bTesselator2.shouldSync) {
					return -1;
				}
				if (bTesselator1.shouldSync) {
					return 1;
				}

				int vanillaOne = -1;
				int vanillaTwo = -1;
				for (int i = 0; i < renderOrder.size(); i++) {
					ParticleRenderType geti = renderOrder.get(i);
					if (geti == o1) {
						vanillaOne = i;
					} else if (geti == o2) {
						vanillaTwo = i;
					}
				}

				if (vanillaOne >= 0 && vanillaTwo >= 0) {
					return Integer.compare(vanillaOne, vanillaTwo);
				}
				if (vanillaOne == -1 && vanillaTwo == -1) {
					return asyncparticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
				}
				return vanillaOne >= 0 ? -1 : 1;
			});
		newTreeMap.putAll(particles);
		particles = newTreeMap;
		new TreeMap<>();
	}

	@Unique
	private static int asyncparticles$compareWithIdentityHashCode(ParticleRenderType o1, ParticleRenderType o2, Map<ParticleRenderType, Integer> insertionOrder, AtomicInteger orderGenerator) {
		int hashCompare = Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
		if (hashCompare != 0) {
			return hashCompare;
		}
		return Integer.compare(insertionOrder.computeIfAbsent(o1, k -> orderGenerator.getAndIncrement()),
			insertionOrder.computeIfAbsent(o2, k -> orderGenerator.getAndIncrement()));
	}
}
