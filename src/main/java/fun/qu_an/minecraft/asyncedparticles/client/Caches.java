package fun.qu_an.minecraft.asyncedparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.util.ArrayList;
import java.util.List;

public class Caches {
	public static Operation<Void> texturesOperation;
	public static volatile boolean cancelled = false;
	public static final List<Runnable> particleOperations = new ArrayList<>(5);

	public static void addParticleOperation(Runnable r) {
		particleOperations.add(r);
	}
}
