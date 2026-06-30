package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

public final class TaskHelper {
	private final ForkJoinPool executor;
	private final List<Group> groups = new ReferenceArrayList<>();
	private final List<Runnable> tasks = new ReferenceArrayList<>();
	private final List<ForkJoinTask<?>> futures = new ReferenceArrayList<>();
	private final Consumer<Exception> exceptionHandler;

	public TaskHelper(ForkJoinPool executor, Consumer<Exception> exceptionHandler) {
		this.executor = executor;
		this.exceptionHandler = exceptionHandler;
	}

	public void addTask(@NonNull Runnable task) {
		tasks.add(task);
	}

	public void groupTasks(boolean parallel) {
		if (tasks.isEmpty()) {
			return;
		}
		List<Runnable> taskSnapshot = new ReferenceArrayList<>(this.tasks);
		this.tasks.clear();
		groups.add(parallel ? new ParallelGroup(taskSnapshot) : new SequentialGroup(taskSnapshot));
	}

	public void submitImmediately(@NonNull Runnable task) {
		futures.add(executor.submit(task));
	}

	public void submitAll() {
		if (!tasks.isEmpty()) {
			groupTasks(false);
		}
		if (groups.isEmpty()) {
			return;
		}

		List<Runnable> groupsSnapshot = new ReferenceArrayList<>(groups);
		groups.clear();

		ForkJoinTask<?> compoundFuture = executor.submit(() -> {
			for (Runnable group : groupsSnapshot) {
				try {
					group.run();
				} catch (Exception e) {
					throw ExceptionUtil.toThrowDirectly(e);
				}
			}
		});
		futures.add(compoundFuture);
	}

	public void waitForCompletion() {
		waitForCompletion(exceptionHandler);
	}

	public void waitForCompletion(Consumer<Exception> exceptionHandler) {
		if (futures.isEmpty()) {
			return;
		}
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
		if (!groups.isEmpty()) {
			groups.forEach(Group::runAll);
			groups.clear();
		}
		if (!tasks.isEmpty()) {
			tasks.forEach(Runnable::run);
			tasks.clear();
		}
	}

	public void disposeTasks() {
		tasks.clear();
		groups.clear();
	}

	public int taskCount() {
		return tasks.size();
	}

	private static sealed abstract class Group implements Runnable permits ParallelGroup, SequentialGroup {
		protected final List<Runnable> tasks;

		private Group(List<Runnable> tasks) {
			this.tasks = tasks;
		}

		protected void runAll() {
			for (Runnable task : tasks) {
				task.run();
			}
		}
	}

	private final class ParallelGroup extends Group implements Runnable {
		public ParallelGroup(List<Runnable> tasks) {
			super(tasks);
		}

		@Override
		public void run() {
			List<ForkJoinTask<?>> groupFutures = new ReferenceArrayList<>(tasks.size());
			for (Runnable task : tasks) {
				groupFutures.add(executor.submit(task));
			}
			for (ForkJoinTask<?> future : groupFutures) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					exceptionHandler.accept(e);
				}
			}
		}
	}

	private static final class SequentialGroup extends Group implements Runnable {
		private SequentialGroup(List<Runnable> tasks) {
			super(tasks);
		}

		@Override
		public void run() {
			runAll();
		}
	}
}
