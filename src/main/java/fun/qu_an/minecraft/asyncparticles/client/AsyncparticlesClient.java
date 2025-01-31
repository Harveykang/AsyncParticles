package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class AsyncparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncparticles";
	public static final Logger LOGGER = LogManager.getLogger();
//	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
//	public static final ForkJoinPool EXECUTOR = new ForkJoinPool(Math.min(8, Runtime.getRuntime().availableProcessors()), forkJoinPool -> {
//		ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
//			protected void onTermination(Throwable throwable) {
//				if (throwable != null) {
//					AsyncparticlesClient.LOGGER.warn("{} died", this.getName(), throwable);
//				} else {
//					AsyncparticlesClient.LOGGER.debug("{} shutdown", this.getName());
//				}
//
//				super.onTermination(throwable);
//			}
//		};
//		forkJoinWorkerThread.setName("Particle-" + WORKER_COUNT.getAndIncrement());
//		return forkJoinWorkerThread;
//	}, AsyncparticlesClient::onThreadException, true);
//
//	public static void onThreadException(Thread thread, Throwable throwable) {
//		Util.pauseInIde(throwable);
//		if (throwable instanceof CompletionException) {
//			throwable = throwable.getCause();
//		}
//
//		if (throwable instanceof ReportedException) {
//			Bootstrap.realStdoutPrintln(((ReportedException)throwable).getReport().getFriendlyReport());
//			System.exit(-1);
//		}
//
//		LOGGER.error("Caught exception in thread {}", thread, throwable);
//	}
	@Override
	public void onInitializeClient() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
