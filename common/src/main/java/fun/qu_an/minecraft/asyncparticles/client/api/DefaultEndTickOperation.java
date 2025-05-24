package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class DefaultEndTickOperation implements EndTickOperation {
	private final Runnable task;
	private final ResourceLocation id;
	private final boolean parallel;

	public DefaultEndTickOperation(ResourceLocation id, Runnable task, boolean parallel) {
		this.task = task;
		this.id = id;
		this.parallel = parallel;
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
}
