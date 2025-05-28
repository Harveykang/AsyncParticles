package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.util.SpinLock;

public interface ISpinLockProvider {
	/**
	 * Mark as default for calling in Mixins.
	 */
	default SpinLock asyncparticles$getSpinLock() {
		throw new UnsupportedOperationException("Missing implementation.");
	}
}
