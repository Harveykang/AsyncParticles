package fun.qu_an.minecraft.asyncparticles.client.core.backend;

public interface VKCaps {
	boolean isComputeShaderSupported();

	boolean isTimelineSemaphoreSupported();

	class VKCapsImpl implements VKCaps {
		private final boolean isComputeShaderSupported;
		private final boolean timelineSemaphoreSupported;

		public VKCapsImpl(boolean isComputeShaderSupported, boolean timelineSemaphoreSupported) {
			this.isComputeShaderSupported = isComputeShaderSupported;
			this.timelineSemaphoreSupported = timelineSemaphoreSupported;
		}

		@Override
		public boolean isTimelineSemaphoreSupported() {
			return timelineSemaphoreSupported;
		}

		@Override
		public boolean isComputeShaderSupported() {
			return isComputeShaderSupported;
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
	}
}
