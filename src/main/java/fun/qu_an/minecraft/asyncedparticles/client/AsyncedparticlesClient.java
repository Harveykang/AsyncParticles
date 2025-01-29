package fun.qu_an.minecraft.asyncedparticles.client;

import fun.qu_an.minecraft.asyncedparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.server.Bootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ErrorManager;

public class AsyncedparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncedparticles";
	public static final Logger LOGGER = LogManager.getLogger();
	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
	public static final ForkJoinPool EXECUTOR = new ForkJoinPool(Math.min(5, Runtime.getRuntime().availableProcessors()), forkJoinPool -> {
		ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
			protected void onTermination(Throwable throwable) {
				if (throwable != null) {
					AsyncedparticlesClient.LOGGER.warn("{} died", this.getName(), throwable);
				} else {
					AsyncedparticlesClient.LOGGER.debug("{} shutdown", this.getName());
				}

				super.onTermination(throwable);
			}
		};
		forkJoinWorkerThread.setName("Particle-" + WORKER_COUNT.getAndIncrement());
		return forkJoinWorkerThread;
	}, AsyncedparticlesClient::onThreadException, true);

	public static void onThreadException(Thread thread, Throwable throwable) {
		Util.pauseInIde(throwable);
		if (throwable instanceof CompletionException) {
			throwable = throwable.getCause();
		}

		if (throwable instanceof ReportedException) {
			Bootstrap.realStdoutPrintln(((ReportedException)throwable).getReport().getFriendlyReport());
			System.exit(-1);
		}

		LOGGER.error("Caught exception in thread {}", thread, throwable);
	}
	@Override
	public void onInitializeClient() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
