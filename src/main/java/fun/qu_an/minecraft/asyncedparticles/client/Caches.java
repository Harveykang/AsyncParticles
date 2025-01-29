package fun.qu_an.minecraft.asyncedparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Caches {
	public static Operation<Void> particlesOperation;
	public static Operation<Void> texturesOperation;
	public static Future<?> textureTickAsync;
	public static Future<?> particleTickAsync;
}
