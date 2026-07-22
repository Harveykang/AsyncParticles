package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface TaskContainer {
	void addTask(@NotNull Runnable task);

	void addTasks(Collection<? extends Runnable> tasks);
}
