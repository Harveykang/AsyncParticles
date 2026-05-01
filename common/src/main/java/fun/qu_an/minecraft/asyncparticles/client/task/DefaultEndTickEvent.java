package fun.qu_an.minecraft.asyncparticles.client.task;

public final class DefaultEndTickEvent implements EndTickEvent {
	private final Runnable task;
	private final int priority;
	private final boolean parallel;

	public DefaultEndTickEvent(int priority, boolean parallel, Runnable task) {
		this.priority = priority;
		this.parallel = parallel;
		this.task = task;
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

	@Override
	public String toString() {
		return "EndTickEvent{" +
				"priority=" + priority +
				", parallel=" + parallel +
				'}' + task;
	}
}
