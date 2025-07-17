package fun.qu_an.minecraft.asyncparticles.client.util;

import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;

public enum CollisionType {
	NONE {
		@Override
		public boolean canSpawnRainEffect(RainEffect rainEffect) {
			return false;
		}
	},
	MOVING {
		@Override
		public boolean canSpawnRainEffect(RainEffect rainEffect) {
			return rainEffect == RainEffect.ALWAYS;
		}
	},
	STATIONARY {
		@Override
		public boolean canSpawnRainEffect(RainEffect rainEffect) {
			return rainEffect != RainEffect.NONE;
		}
	};

	public abstract boolean canSpawnRainEffect(RainEffect rainEffect);
}
