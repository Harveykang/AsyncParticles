package fun.qu_an.minecraft.asyncparticles.client.task;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import net.minecraft.resources.ResourceLocation;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

public final class DefaultEndTickOperation implements EndTickOperation {
	private final Runnable task;
	private final ResourceLocation id;
	private final boolean parallel;

	public DefaultEndTickOperation(ResourceLocation id, boolean parallel, Runnable task) {
		this.id = id;
		this.parallel = parallel;
		this.task = task;
	}

	@Override
	public boolean isParallel() {
		return parallel;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public void run() {
		try {
			task.run();
		} catch (Exception e) {
			if (!AsyncTickBehavior.INSTANCE.isTolerable(e) || AsyncTickBehavior.INSTANCE.exceptionTracker.addException(getId(), e)) {
				throw toThrowDirectly(e);
			}
		}
	}

	@Override
	public String toString() {
		return "EndTickOperation{" +
				"id=" + id +
				", parallel=" + parallel +
				'}';
	}
}
