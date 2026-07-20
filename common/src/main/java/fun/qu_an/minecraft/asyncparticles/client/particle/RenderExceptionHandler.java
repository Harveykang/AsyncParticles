package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class RenderExceptionHandler {
	private static final RenderExceptionHandler INSTANCE = new RenderExceptionHandler();
	private final ExceptionTracker<Class<? extends Particle>> exceptionTracker = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getRenderFailurePerSecondThreshold
	);

	public static RenderExceptionHandler getInstance() {
		return INSTANCE;
	}

	public void onRenderOffMainThreadExceptionally(ParticleRenderType particleRenderType, Particle particle, Throwable t) {
		boolean tolerable = TickExceptionHandler.getInstance().isTolerable(t);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !exceptionTracker.addException(particleClass, t)) {
			return;
		}
		((ParticleAddon) particle).asyncparticles$setRenderSync();
		if (!AsyncRenderBehavior.getInstance().shouldSync(particleClass)) {
			if (!tolerable) {
				AsyncRenderBehavior.LOGGER.warn("Exception while rendering particle {}, marking as sync", particle, t);
			} else {
				AsyncRenderBehavior.LOGGER.warn("Exception {} thrown while rendering particle {} exceeds the threshold, please contact the author: {}",
					t.getClass().getName(),
					particle,
					AsyncParticlesClient.ISSUE_URL,
					t);
			}
			AsyncRenderBehavior.getInstance().markAsSync(particleClass);
		}
		AsyncRenderBehavior.getInstance().recordSync(particleRenderType, particle);
	}

	public Void renderAsyncExceptionally(Throwable e) {
		AsyncRenderBehavior.LOGGER.error("Error rendering particle", e);
		Minecraft mc1 = Minecraft.getInstance();
		if (mc1.level != null && mc1.player != null && mc1.cameraEntity != null) {
			throw ExceptionUtil.toThrowDirectly(e);
		}
		return null;
	}

	public ReportedException constructCrashReport(Particle particle, ParticleRenderType particleRenderType, Throwable t) {
		AsyncTickBehavior.getInstance().debugLater(AsyncRenderBehavior.LOGGER::info);
		AsyncTickBehavior.getInstance().tryDebug();
		AsyncRenderBehavior.getInstance().debugLater(AsyncRenderBehavior.LOGGER::info);
		AsyncRenderBehavior.getInstance().tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Rendering Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
		return new ReportedException(crashReport);
	}

	public boolean onRenderOnMainThreadExceptionally(Throwable t, Particle particle, ParticleRenderType prt) {
		if (!(t instanceof Exception e)) {
			throw constructCrashReport(particle, prt, t);
		}
		TickExceptionHandler tickExceptionHandler = TickExceptionHandler.getInstance();
		if (!tickExceptionHandler.cancelled.getAcquire()) { // idempotency
			synchronized (tickExceptionHandler.cancelledLock) {
				if (!tickExceptionHandler.cancelled.getAcquire()) {
					if (!ConfigHelper.isTickAsync() || ConfigHelper.isGpuOnlyAsyncParticleTick()) {
						throw constructCrashReport(particle, prt, t);
					}
					tickExceptionHandler.cancelled.setRelease(true);
					Diagnostic.errorParticleRenderOnMainThread(e);
				}
			}
		}
		return true;
	}
}
