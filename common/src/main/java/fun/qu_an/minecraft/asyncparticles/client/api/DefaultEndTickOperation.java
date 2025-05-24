package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class DefaultEndTickOperation implements EndTickOperation {
	private final Runnable task;
	private final ResourceLocation id;
	private final boolean ordered;

	public DefaultEndTickOperation(ResourceLocation id, Runnable task, boolean ordered) {
		this.task = task;
		this.id = id;
		this.ordered = ordered;
	}

	@Override
	public boolean isOrdered() {
		return ordered;
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
