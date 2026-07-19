package fun.qu_an.minecraft.asyncparticles.client.task;

import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class DefaultEndTickOperation implements EndTickOperation {
	private final Runnable task;
	private final ResourceLocation id;
	private final boolean parallel;
	private final Consumer<Exception> exceptionHandler;

	public DefaultEndTickOperation(ResourceLocation id, boolean parallel, Runnable task) {
		this(id, parallel, task, ExceptionUtil::toThrowDirectly);
	}

	public DefaultEndTickOperation(ResourceLocation id, boolean parallel, Runnable task, Consumer<Exception> exceptionHandler) {
		this.id = id;
		this.parallel = parallel;
		this.task = task;
		this.exceptionHandler = exceptionHandler;
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
		GameUtil.ensureLevelRunning(task, exceptionHandler);
	}
}
