package fun.qu_an.minecraft.asyncparticles.client.api;

public final class DefaultEndTickEvent implements EndTickEvent {
	private final Runnable task;
	private final int priority;
	private final boolean ordered;

	public DefaultEndTickEvent(Runnable task, int priority, boolean ordered) {
		this.task = task;
		this.priority = priority;
		this.ordered = ordered;
	}

	@Override
	public void run() {
		task.run();
	}

	public int getPriority() {
		return priority;
	}

	public boolean isOrdered() {
		return ordered;
	}
}
