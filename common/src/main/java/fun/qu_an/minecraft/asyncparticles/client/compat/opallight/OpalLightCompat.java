package fun.qu_an.minecraft.asyncparticles.client.compat.opallight;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;
import org.mesdag.opallight.light.color.LightColorData;
import org.mesdag.opallight.light.color.LightColorSampler;
import org.mesdag.opallight.light.data.LightDataLoader;
import org.mesdag.opallight.light.engine.LightColorCache;

public final class OpalLightCompat {
	private static final ParticleThreadLocal<Sampler> SAMPLERS = ParticleThreadLocal.withInitial(ThreadUtil::isOnMainThread, Sampler::new);
	public static final OpalLightDestructionSampler DESTRUCTION_SAMPLER = new OpalLightDestructionSampler();
	private static volatile Snapshot snapshot;

	private record Snapshot(ClientLevel level, Long2ObjectOpenHashMap<Long2LongOpenHashMap> sections,
							float skyBrightness, float ambientLight) {
	}

	/* Destruction color prediction */
	public static void beginDestroy(ClientLevel level, BlockPos pos, BlockState state) {
		if (!state.isAir() && state.getLightEmission() == 0 && state.getLightBlock(level, pos) > 0
			&& LightDataLoader.INSTANCE.getProfile(state) == null) {
			LightColorCache colors = LightColorCache.get(level);
			if (!colors.isEmpty()) {
				try {
					DESTRUCTION_SAMPLER.prepare(level, pos, state, colors);
				} catch (RuntimeException | Error e) {
					DESTRUCTION_SAMPLER.clear();
					throw e;
				}
			}
		}
	}

	public static void endDestroy() {
		DESTRUCTION_SAMPLER.clear();
	}

	public static void cacheDestructionColor(Particle particle) {
		OpalLightDestructionSampler destruction = DESTRUCTION_SAMPLER;
		if (destruction.level == particle.level && particle instanceof TerrainParticle) {
			((OpalLightParticleAddon) particle).asyncparticles$setCachedColor(destruction.sample(particle.x, particle.y, particle.z));
		}
	}

	/* Snapshot for worker threads */
	public static void capture() {
		ThreadUtil.assertOnMainThread();
		ClientLevel level = Minecraft.getInstance().level;
		LightColorCache colors = LightColorCache.get(level);
		if (colors.isEmpty()) {
			clear();
			return;
		}
		Long2ObjectOpenHashMap<Long2LongOpenHashMap> sections = new Long2ObjectOpenHashMap<>();
		// Opal publishes replacement section maps, its own propagation snapshots also share them read-only.
		colors.forEachSection(key -> sections.put(key, colors.getSection(key)));
		float skyBrightness = level.getSkyFlashTime() > 0 ? 1.0F : level.getSkyDarken(1.0F) * 0.95F + 0.05F;
		snapshot = new Snapshot(level, sections, skyBrightness, level.dimensionType().ambientLight());
	}

	public static void clear() {
		snapshot = null;
	}

	/* Apply colors for gpu particles */
	public static void apply(Particle particle, boolean append, long address) {
		ClientLevel level = particle.level;
		double x = particle.x, y = particle.y, z = particle.z;
		OpalLightParticleAddon addon = (OpalLightParticleAddon) particle;
		int cachedColor = addon.asyncparticles$getCachedColor();
		// Append displays the birth snapshot. Consume its color only during the first regular extraction.
		if (!append && cachedColor != 0) {
			addon.asyncparticles$setCachedColor(0);
		}
		long color;
		float skyBrightness;
		float ambientLight;
		if (ThreadUtil.isOnMainThread()) {
			LightColorCache colors = LightColorCache.get(level);
			if (colors.isEmpty()) {
				return;
			}
			long initialColor = ConfigHelper.isOpalLightDestructionColorPrediction() ? unpackColor(cachedColor) : 0L;
			if (initialColor != 0L) {
				color = initialColor;
			} else {
				color = colors.sample(x, y, z);
				if (color == 0L) {
					return;
				}
			}
			skyBrightness = level.getSkyFlashTime() > 0 ? 1.0F : level.getSkyDarken(1.0F) * 0.95F + 0.05F;
			ambientLight = level.dimensionType().ambientLight();
		} else {
			Snapshot current = snapshot;
			if (current == null || current.level() != level) {
				return;
			}
			long initialColor = ConfigHelper.isOpalLightDestructionColorPrediction() ? unpackColor(cachedColor) : 0L;
			if (initialColor != 0L) {
				color = initialColor;
			} else {
				color = SAMPLERS.get().sample(current, x, y, z);
				if (color == 0L) {
					return;
				}
			}
			skyBrightness = current.skyBrightness();
			ambientLight = current.ambientLight();
		}

		// color
		float red = LightColorData.channel(color, 32);
		float green = LightColorData.channel(color, 16);
		float blue = LightColorData.channel(color, 0);
		float strength = Math.max(red, Math.max(green, blue));
		float edge = Math.clamp(strength / 0.2F, 0.0F, 1.0F);
		float opacity = edge * edge * (3.0F - 2.0F * edge);
		float tint = 0.35F * opacity;
		red = 1.0F - tint * (strength - red);
		green = 1.0F - tint * (strength - green);
		blue = 1.0F - tint * (strength - blue);
		long oldColor = address + GpuParticleAddon.oCOLOR_OFFSET;
		long newColor = address + GpuParticleAddon.COLOR_OFFSET;
		MemoryUtil.memPutInt(oldColor, tint(MemoryUtil.memGetInt(oldColor), red, green, blue));
		MemoryUtil.memPutInt(newColor, tint(MemoryUtil.memGetInt(newColor), red, green, blue));

		// light
		int light = MemoryUtil.memGetInt(address + GpuParticleAddon.LIGHT_OFFSET);
		float sky = Mth.clamp((light >>> 16) / 240.0F, 0.0F, 1.0F);
		float brightness = Mth.lerp(ambientLight, sky / (4.0F - 3.0F * sky), 1.0F) * skyBrightness;
		float visibility = 1.0F - Mth.clamp(brightness, 0.0F, 1.0F);
		int blockLight = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 12.0F * opacity * visibility) << 4;
		MemoryUtil.memPutInt(address + GpuParticleAddon.LIGHT_OFFSET,
			light & 0xFFFF0000 | Math.max(light & 0xFFFF, blockLight));
	}

	private static int tint(int color, float red, float green, float blue) {
		return FastColor.ABGR32.color(FastColor.ABGR32.alpha(color),
			Math.round(FastColor.ABGR32.blue(color) * blue),
			Math.round(FastColor.ABGR32.green(color) * green),
			Math.round(FastColor.ABGR32.red(color) * red));
	}

	private static long unpackColor(int rgb) {
		return ((long) (rgb >>> 16 & 0xFF) * 257 << 32)
			| ((long) (rgb >>> 8 & 0xFF) * 257 << 16) | (long) (rgb & 0xFF) * 257;
	}

	private static final class Sampler implements LightColorSampler.Colors {
		private final LightColorSampler sampler = new LightColorSampler(this, false);
		private final float[] sampled = new float[3];
		private Snapshot current;

		@Override
		public long get(int x, int y, int z) {
			Long2LongOpenHashMap section = current.sections().get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
			return section == null ? 0L : section.get(BlockPos.asLong(x, y, z));
		}

		private long sample(Snapshot snapshot, double x, double y, double z) {
			current = snapshot;
			try {
				sampler.sample(snapshot.level(), x, y, z, Mth.floor(x), Mth.floor(y), Mth.floor(z), sampled);
				return LightColorData.pack(sampled[0], sampled[1], sampled[2]);
			} finally {
				current = null;
			}
		}
	}
}
