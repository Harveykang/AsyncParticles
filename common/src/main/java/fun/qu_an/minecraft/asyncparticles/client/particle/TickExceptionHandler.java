package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

public class TickExceptionHandler {
	private static final TickExceptionHandler INSTANCE = new TickExceptionHandler();
	final AtomicBoolean cancelled = new AtomicBoolean(false);
	final Object cancelledLock = new Object();
	private final ExceptionTracker<Object> exceptionTracker = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);

	public static TickExceptionHandler getInstance() {
		return INSTANCE;
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
	 * @return Should break the tick loop
	 */
	public boolean onTickingParticleException(Particle particle, Throwable t) {
		if (!ThreadUtil.isOnParticleTickerThread() || !(t instanceof Exception e)) {
			throw constructCrashReport(particle, t);
		}
		boolean tolerable = isTolerable(e);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !exceptionTracker.addException(particleClass, e)) {
			return false;
		}
		if (ConfigHelper.markSyncIfTickFailed()) {
			((ParticleAddon) particle).asyncparticles$setTickSync();
			if (!AsyncTickBehavior.getInstance().shouldSync(particleClass)) {
				if (!tolerable) {
					AsyncTickBehavior.LOGGER.warn("Exception while ticking particle {}, marking as sync", particle, e);
				} else {
					AsyncTickBehavior.LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
						e.getClass().getName(),
						particle,
						AsyncParticlesClient.ISSUE_URL,
						e);
				}
				AsyncTickBehavior.getInstance().markAsSync(particleClass);
			}
			AsyncTickBehavior.getInstance().recordSync(particle);
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

	public void tickExceptionally(Throwable t) {
		if (!(t instanceof Exception e)) {
			throw toThrowDirectly(t);
		}
		Minecraft mc = Minecraft.getInstance();
		if (isTolerable(e) &&
			(mc.level == null || mc.player == null || mc.cameraEntity == null)) {
			AsyncTickBehavior.LOGGER.warn("Exception while executing tick tasks.", e);
			return;
		}
		throw toThrowDirectly(t);
	}

	public ReportedException constructCrashReport(Particle particle, Throwable t) {
		AsyncTickBehavior.getInstance().debugLater(AsyncTickBehavior.LOGGER::info);
		AsyncTickBehavior.getInstance().tryDebug();
		AsyncRenderBehavior.getInstance().debugLater(AsyncTickBehavior.LOGGER::info);
		AsyncRenderBehavior.getInstance().tryDebug();
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
