package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class TaskManager {
	private final ForkJoinPool executor;
	private final List<Runnable> tasks = new ArrayList<>();
	private final List<ForkJoinTask<?>> futures = new ArrayList<>();
	private final Consumer<Exception> exceptionHandler;

	public TaskManager(ForkJoinPool executor, Consumer<Exception> exceptionHandler) {
		this.executor = executor;
		this.exceptionHandler = exceptionHandler;
	}

	public void addTask(@NonNull Runnable task) {
		tasks.add(task);
	}

	public void submitImmediately(@NonNull Runnable task) {
		futures.add(executor.submit(task));
	}

	public void submitAllSequentially() {
		Runnable[] tasksArray = tasks.toArray(Runnable[]::new);
		tasks.clear();
		futures.add(executor.submit(() -> {
			for (Runnable runnable : tasksArray) {
				runnable.run();
			}
		}));
	}

	public void waitForCompletion() {
		waitForCompletion(exceptionHandler);
	}

	public void waitForCompletion(Consumer<Exception> exceptionHandler) {
		for (ForkJoinTask<?> task : futures) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				exceptionHandler.accept(e);
			}
		}
		futures.clear();
	}

	public ForkJoinPool executor() {
		return executor;
	}

	public boolean isRunning() {
		return !futures.isEmpty();
	}

	public void runAllTasks() {
		tasks.forEach(Runnable::run);
		tasks.clear();
	}

	public void disposeTasks() {
		tasks.clear();
	}

	public int taskCount() {
		return tasks.size();
	}
}
