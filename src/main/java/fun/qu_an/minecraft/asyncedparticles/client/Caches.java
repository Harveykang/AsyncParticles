package fun.qu_an.minecraft.asyncedparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.util.concurrent.Future;

public class Caches {
	public static Operation<Void> particlesOperation;
	public static Operation<Void> texturesOperation;
	public static Future<?> textureTickAsync;
	public static Future<?> particleTickAsync;
	public static volatile boolean texturesOperationCancelled;
	public static volatile boolean particlesOperationCancelled;
}
