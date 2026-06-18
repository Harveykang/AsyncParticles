package fun.qu_an.minecraft.asyncparticles.client.core.backend;

public interface VKCaps {
	boolean isComputeShaderSupported();

	boolean isTimelineSemaphoreSupported();

	boolean isSameQueueFamilyAsGraphics();

	class VKCapsImpl implements VKCaps {
		private final boolean isComputeShaderSupported;
		private final boolean timelineSemaphoreSupported;
		private final boolean sameQueueFamilyAsGraphics;

		public VKCapsImpl(boolean isComputeShaderSupported, boolean timelineSemaphoreSupported, boolean sameQueueFamilyAsGraphics) {
			this.isComputeShaderSupported = isComputeShaderSupported;
			this.timelineSemaphoreSupported = timelineSemaphoreSupported;
			this.sameQueueFamilyAsGraphics = sameQueueFamilyAsGraphics;
		}

		@Override
		public boolean isTimelineSemaphoreSupported() {
			return timelineSemaphoreSupported;
		}

		@Override
		public boolean isSameQueueFamilyAsGraphics() {
			return sameQueueFamilyAsGraphics;
		}

		@Override
		public boolean isComputeShaderSupported() {
			return isComputeShaderSupported;
		}

		public boolean sameQueueFamilyAsGraphics() {
			return sameQueueFamilyAsGraphics;
		}
	}

	class Unsupported implements VKCaps {
		@Override
		public boolean isComputeShaderSupported() {
			return false;
		}

		@Override
		public boolean isTimelineSemaphoreSupported() {
			return false;
		}

		@Override
		public boolean isSameQueueFamilyAsGraphics() {
			return false;
		}
	}
}
