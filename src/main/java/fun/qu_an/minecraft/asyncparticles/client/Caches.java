package fun.qu_an.minecraft.asyncparticles.client;

import java.util.*;

public class Caches {
	public static boolean cancelled = false;
	public static final List<Runnable> parallelOperations = new ArrayList<>();
	public static boolean shouldTickParticles = true;
	public static Runnable animateOperation;
}
