package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

public class TickExceptionHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	public final AtomicBoolean cancelled = new AtomicBoolean(false);
	public final Object cancelledLock = new Object();
	private final ExceptionTracker<Object> exceptionTracker = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private final AsyncTickBehavior asyncTickBehavior;

	public TickExceptionHandler(AsyncTickBehavior asyncTickBehavior) {
		this.asyncTickBehavior = asyncTickBehavior;
	}

	public boolean isTolerable(@NotNull Throwable e) {
		if (!(e instanceof Exception)) {
			return false;
		}
		Throwable rootCause = ExceptionUtil.getRootCause(e);
		return rootCause instanceof MissingPaletteEntryException
			|| rootCause instanceof NullPointerException
			|| rootCause instanceof IndexOutOfBoundsException
			|| rootCause instanceof ArrayIndexOutOfBoundsException
			|| (rootCause instanceof ConcurrentModificationException && ConfigHelper.suppressCME());
	}

	/**
	 * This runs on particle tick or main thread when particle task is exceptionally thrown
	 * @return Should break the tick loop
	 */
	public boolean onTickParticleException(Particle particle, Throwable t) {
		if (!ThreadUtil.isOnParticleTickerThread() || !(t instanceof Exception e)) {
			throw constructCrashReport(particle, t);
		}
		LevelBundle levelBundle = AsyncTickBehavior.getInstance().getLevelBundle();
		if (levelBundle == null || levelBundle.isLevelReset()) {
			return true;
		}
		boolean tolerable = isTolerable(e);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !exceptionTracker.addException(particleClass, e)) {
			return false;
		}
		if (!cancelled.getAcquire()) { // idempotency
			synchronized (cancelledLock) {
				if (!cancelled.getAcquire()) {
					cancelled.setRelease(true);
					if (ConfigHelper.isGpuOnlyAsyncParticleTick()) {
						Diagnostic.errorAsyncGpuParticleTick(e);
					} else {
						Diagnostic.errorAsyncParticleTick(e);
					}
				}
			}
		}
		return true;
	}

	/**
	 * This runs on main thread when the entire tick task is exceptionally thrown
	 */
	public void tickExceptionally(Throwable t) {
		if (!(t instanceof Exception e)) {
			throw toThrowDirectly(t);
		}
		Minecraft mc = Minecraft.getInstance();
		if (isTolerable(e) &&
			(mc.level == null || mc.player == null || mc.getCameraEntity() == null)) {
			LOGGER.warn("Exception while executing tick tasks.", e);
			return;
		}
		throw toThrowDirectly(t);
	}

	public ReportedException constructCrashReport(Particle particle, Throwable t) {
		while (t instanceof CompletionException || t instanceof ExecutionException) {
			t = t.getCause();
		}
		if (t instanceof ReportedException re) {
			return re;
		}
		asyncTickBehavior.debugLater(LOGGER::info);
		asyncTickBehavior.tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
		return new ReportedException(crashReport);
	}

	public void resetCancelled() {
		cancelled.setRelease(false);
	}
}
