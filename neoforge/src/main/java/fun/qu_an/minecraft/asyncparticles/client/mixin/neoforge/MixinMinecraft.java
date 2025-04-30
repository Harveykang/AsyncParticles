package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.google.common.collect.Maps;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = Minecraft.class, priority = 1500)
public class MixinMinecraft {
	@Shadow
	@Final
	public ParticleEngine particleEngine;

	@Inject(method = "run", at = @At("HEAD"))
	private void onRun(CallbackInfo ci) { // Later than mixin.MixinMinecraft
		ThreadUtil.enqueueClientTask(() -> { // Do it later.
			List<ParticleRenderType> renderOrder = ParticleEngine.RENDER_ORDER;
			AtomicInteger orderGenerator = new AtomicInteger();
			Map<ParticleRenderType, Integer> insertionOrder = Collections.synchronizedMap(new IdentityHashMap<>(0));
			Map<ParticleRenderType, Queue<Particle>> newTreeMap =
				Maps.newTreeMap((o1, o2) -> {
					// FIXME: why do i have to write this shit?
					if (o1 == o2) {
						return 0;
					}
					BindingTesselator bTesselator1 = AsyncRenderer.getBTesselator(o1, particleEngine.textureManager);
					BindingTesselator bTesselator2 = AsyncRenderer.getBTesselator(o2, particleEngine.textureManager);
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
						if (vanillaOne == -1 && geti == o1) {
							vanillaOne = i;
						} else if (vanillaTwo == -1 && geti == o2) {
							vanillaTwo = i;
						}
						if (vanillaOne >= 0 && vanillaTwo >= 0) {
							return Integer.compare(vanillaOne, vanillaTwo);
						}
					}

					if (vanillaOne == -1 && vanillaTwo == -1) {
						return asyncparticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
					}
					return vanillaOne >= 0 ? -1 : 1;
				});
			newTreeMap.putAll(particleEngine.particles);
			particleEngine.particles = newTreeMap;
		});
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
