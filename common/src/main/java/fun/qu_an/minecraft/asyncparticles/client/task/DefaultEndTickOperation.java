package fun.qu_an.minecraft.asyncparticles.client.task;

import net.minecraft.resources.ResourceLocation;

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
		task.run();
	}

	@Override
	public String toString() {
		return "EndTickOperation{" +
				"id=" + id +
				", parallel=" + parallel +
				'}';
	}
}
