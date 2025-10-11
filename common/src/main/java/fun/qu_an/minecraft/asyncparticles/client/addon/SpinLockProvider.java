package fun.qu_an.minecraft.asyncparticles.client.addon;

import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;

public interface SpinLockProvider {
	/**
	 * Mark as default for calling in Mixins.
	 */
	default SpinLock asyncparticles$getSpinLock() {
		throw new UnsupportedOperationException("Missing implementation.");
	}
}
