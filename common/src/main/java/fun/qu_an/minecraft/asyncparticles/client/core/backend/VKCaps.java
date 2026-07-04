package fun.qu_an.minecraft.asyncparticles.client.core.backend;

public interface VKCaps {
	boolean isSupported();

	boolean pushDescriptor();

	boolean synchronization2();

	class VKCapsImpl implements VKCaps {
		private final boolean pushDescriptor;
		private final boolean synchronization2;

		public VKCapsImpl(boolean pushDescriptor, boolean synchronization2) {
			this.pushDescriptor = pushDescriptor;
			this.synchronization2 = synchronization2;
		}

		@Override
		public boolean isSupported() {
			return true;
		}

		@Override
		public boolean pushDescriptor() {
			return pushDescriptor;
		}

		@Override
		public boolean synchronization2() {
			return synchronization2;
		}
	}

	class Unsupported implements VKCaps {
		@Override
		public boolean isSupported() {
			return false;
		}

		@Override
		public boolean pushDescriptor() {
			return false;
		}

		@Override
		public boolean synchronization2() {
			return false;
		}
	}
}
