package fun.qu_an.minecraft.asyncparticles.client.api;

public final class DefaultEndTickEvent implements EndTickEvent {
	private final Runnable task;
	private final int priority;
	private final boolean parallel;

	public DefaultEndTickEvent(Runnable task, int priority, boolean parallel) {
		this.task = task;
		this.priority = priority;
		this.parallel = parallel;
	}

	@Override
	public void run() {
		task.run();
	}

	public int getPriority() {
		return priority;
	}

	public boolean isParallel() {
		return parallel;
	}
}
