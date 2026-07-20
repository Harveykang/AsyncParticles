package fun.qu_an.minecraft.asyncparticles.client.task;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;

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
		AsyncTickBehavior.getInstance().ensureLevelRunning(task, ExceptionUtil::toThrowDirectly);
	}

	public int getPriority() {
		return priority;
	}

	public boolean isParallel() {
		return parallel;
	}

	@Override
	public String toString() {
		return "DefaultEndTickEvent{" +
			"priority=" + priority +
			", parallel=" + parallel +
			'}' + task;
	}
}
