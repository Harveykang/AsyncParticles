package fun.qu_an.minecraft.asyncparticles.client.mixin.forge;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import it.unimi.dsi.fastutil.Pair;
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

@Mixin(value = ParticleEngine.class, priority = 9500) // after common:MixinParticleEngine_Late
public class MixinParticleEngine_Late {
	@Mutable
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	public static List<ParticleRenderType> RENDER_ORDER;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onInit(ClientLevel level, TextureManager textureManager, CallbackInfo ci) {
		List<ParticleRenderType> renderOrder = RENDER_ORDER;
		AtomicInteger orderGenerator = new AtomicInteger(); // single-threaded
		Map<ParticleRenderType, Integer> insertionOrder = Collections.synchronizedMap(new IdentityHashMap<>(0));
		Map<ParticleRenderType, Queue<Particle>> newTreeMap =
			Maps.newTreeMap((o1, o2) -> {
				// FIXME: why i have to write this shit?
				if (o1 == o2) {
					return 0;
				}
				Pair<VertexFormat.Mode, VertexFormat> bTesselator1 = AsyncRenderer.getVertexFormatPair(o1, textureManager);
				Pair<VertexFormat.Mode, VertexFormat> bTesselator2 = AsyncRenderer.getVertexFormatPair(o2, textureManager);
				if (bTesselator1 == AsyncRenderer.EMPTY_FORMAT && bTesselator2 == AsyncRenderer.EMPTY_FORMAT) {
					return asyncparticles$compareWithIdentityHashCode(o1, o2, insertionOrder, orderGenerator);
				}
				if (bTesselator2 == AsyncRenderer.EMPTY_FORMAT) {
					return -1;
				}
				if (bTesselator1 == AsyncRenderer.EMPTY_FORMAT) {
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
