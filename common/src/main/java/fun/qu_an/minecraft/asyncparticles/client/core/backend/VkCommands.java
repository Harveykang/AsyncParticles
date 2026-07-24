package fun.qu_an.minecraft.asyncparticles.client.core.backend;

public abstract class VkCommands {
	private final boolean pushDescriptor;
	private final boolean synchronization2;

	public VkCommands(boolean pushDescriptor, boolean synchronization2) {
		this.pushDescriptor = pushDescriptor;
		this.synchronization2 = synchronization2;
	}

	public boolean isSupported() {
		return true;
	}

	public boolean pushDescriptor() {
		return pushDescriptor;
	}

	public boolean synchronization2() {
		return synchronization2;
	}

	@Override
	public String toString() {
		return "VkCommands." + this.getClass().getSimpleName() + "{" +
			"pushDescriptor=" + pushDescriptor +
			", synchronization2=" + synchronization2 +
			'}';
	}

	public static class Vk extends VkCommands {
		public Vk(boolean pushDescriptor, boolean synchronization2) {
			super(pushDescriptor, synchronization2);
		}
	}

	public static class Unsupported extends VkCommands {
		public Unsupported() {
			super(false, false);
		}

		@Override
		public boolean isSupported() {
			return false;
		}
	}
}
