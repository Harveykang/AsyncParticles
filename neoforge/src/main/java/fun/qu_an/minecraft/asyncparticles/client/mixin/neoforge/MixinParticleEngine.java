package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
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
		Comparator<ParticleRenderType> vanillaComparator = Comparator.comparingInt(renderOrder::indexOf);
		int[] orderGenerator = new int[1]; // single-threaded
		Map<ParticleRenderType, Integer> insertionOrder = new IdentityHashMap<>();
		Map<ParticleRenderType, Queue<Particle>> newTreeMap =
			Maps.newTreeMap((o1, o2) -> {
				// FIXME: why i have to write this shit?
				RenderSystem.assertOnRenderThread();
				if (o1 == o2) {
					return 0;
				}
				BindingTesselator bTesselator1 = AsyncRenderer.getBTesselator(o1, textureManager);
				BindingTesselator bTesselator2 = AsyncRenderer.getBTesselator(o2, textureManager);
				if (bTesselator1 == BindingTesselator.EMPTY && bTesselator2 == BindingTesselator.EMPTY) {
					return asyncParticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
				}
				if (bTesselator2 == BindingTesselator.EMPTY) {
					return -1;
				}
				if (bTesselator1 == BindingTesselator.EMPTY) {
					return 1;
				}
				boolean vanillaOne = renderOrder.contains(o1);
				boolean vanillaTwo = renderOrder.contains(o2);

				if (vanillaOne && vanillaTwo) {
					return vanillaComparator.compare(o1, o2);
				}
				if (!vanillaOne && !vanillaTwo) {
					return asyncParticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
				}
				return vanillaOne ? -1 : 1;
			});
		newTreeMap.putAll(particles);
		particles = newTreeMap;
		new TreeMap<>();
	}

	@Unique
	private static int asyncParticles$compareWithIdentityHashCode(ParticleRenderType o1, ParticleRenderType o2, Map<ParticleRenderType, Integer> insertionOrder, int[] orderGenerator) {
		int hashCompare = Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
		if (hashCompare != 0) {
			return hashCompare;
		}
		return Integer.compare(insertionOrder.computeIfAbsent(o1, k -> orderGenerator[0]++),
			insertionOrder.computeIfAbsent(o2, k -> orderGenerator[0]++));
	}
}
