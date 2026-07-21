package fun.qu_an.minecraft.asyncparticles.client.task;

import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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

	public void addTask(@NotNull Runnable task) {
		tasks.add(task);
	}

	public void addTasks(Collection<? extends Runnable> tasks) {
		this.tasks.addAll(tasks);
	}

	public void groupTasks(boolean parallel) {
		if (tasks.isEmpty()) {
			return;
		}
		List<Runnable> taskSnapshot = new ReferenceArrayList<>(this.tasks);
		this.tasks.clear();
		groups.add(parallel ? new ParallelGroup(taskSnapshot) : new SequentialGroup(taskSnapshot));
	}

	public void groupTasks(boolean parallel, Consumer<Exception> exceptionHandler) {
		if (tasks.isEmpty()) {
			return;
		}
		List<Runnable> taskSnapshot = new ReferenceArrayList<>(this.tasks);
		this.tasks.clear();
		groups.add(parallel ? new ParallelGroup(taskSnapshot, exceptionHandler) : new SequentialGroup(taskSnapshot, exceptionHandler));
	}

	public void submitImmediately(@NotNull Runnable task) {
		futures.add(executor.submit(task));
	}

	public void submitAll(Consumer<Exception> exceptionHandler) {
		if (!tasks.isEmpty()) {
			groupTasks(false);
		}
		if (groups.isEmpty()) {
			return;
		}

		if (groups.size() == 1) {
			Group group = groups.remove(0);
			ForkJoinTask<?> task = executor.submit(() -> {
				try {
					group.run();
				} catch (Exception e) {
					exceptionHandler.accept(e);
				}
			});
			futures.add(task);
			return;
		}

		List<Runnable> groupsSnapshot = new ReferenceArrayList<>(groups);
		groups.clear();

		ForkJoinTask<?> compoundFuture = executor.submit(() -> {
			for (Runnable group : groupsSnapshot) {
				try {
					group.run();
				} catch (Exception e) {
					exceptionHandler.accept(e);
				}
			}
		});
		futures.add(compoundFuture);
	}

	public void submitAll() {
		submitAll(ExceptionUtil::toThrowDirectly);
	}

	public void submitAll(Runnable whenStarted, Runnable whenCompleted, Consumer<Exception> exceptionHandler) {
		if (!tasks.isEmpty()) {
			groupTasks(false);
		}
		if (groups.isEmpty()) {
			return;
		}

		if (groups.size() == 1) {
			Group group = groups.remove(0);
			ForkJoinTask<?> task = executor.submit(() -> {
				try {
					whenStarted.run();
					try {
						group.run();
					} catch (Exception e) {
						exceptionHandler.accept(e);
					}
				} finally {
					whenCompleted.run();
				}
			});
			futures.add(task);
			return;
		}

		List<Runnable> groupsSnapshot = new ReferenceArrayList<>(groups);
		groups.clear();

		ForkJoinTask<?> compoundFuture = executor.submit(() -> {
			try {
				whenStarted.run();
				for (Runnable group : groupsSnapshot) {
					try {
						group.run();
					} catch (Exception e) {
						exceptionHandler.accept(e);
					}
				}
			} finally {
				whenCompleted.run();
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
		protected final Consumer<Exception> exceptionHandler;

		private Group(List<Runnable> tasks) {
			this(tasks, ExceptionUtil::toThrowDirectly);
		}

		public Group(List<Runnable> tasks, Consumer<Exception> exceptionHandler) {
			this.tasks = tasks;
			this.exceptionHandler = exceptionHandler;
		}

		protected void runAll() {
			for (Runnable task : tasks) {
				try {
					task.run();
				} catch (Exception e) {
					exceptionHandler.accept(e);
				}
			}
		}
	}

	private final class ParallelGroup extends Group implements Runnable {
		public ParallelGroup(List<Runnable> tasks) {
			super(tasks);
		}

		public ParallelGroup(List<Runnable> tasks, Consumer<Exception> exceptionHandler) {
			super(tasks, exceptionHandler);
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

		public SequentialGroup(List<Runnable> tasks, Consumer<Exception> exceptionHandler) {
			super(tasks, exceptionHandler);
		}

		@Override
		public void run() {
			runAll();
		}
	}
}
