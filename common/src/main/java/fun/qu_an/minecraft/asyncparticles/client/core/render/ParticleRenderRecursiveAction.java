package fun.qu_an.minecraft.asyncparticles.client.core.render;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.AsyncQuadParticleGroup;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class ParticleRenderRecursiveAction extends RecursiveAction {
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncRenderBehavior.THREADS)) / Math.log(2)) + 2;
	private final Spliterator<SingleQuadParticle> spliterator;
	private final AsyncQuadParticleGroup group;
	private final Frustum frustum;
	private final Camera camera;
	private final float partialTick;
	private final int depth;

	public ParticleRenderRecursiveAction(Spliterator<SingleQuadParticle> spliterator,
										 AsyncQuadParticleGroup group,
										 Frustum frustum,
										 Camera camera,
										 float partialTick) {
		this(spliterator, group, frustum, camera, partialTick, 0);
	}

	private ParticleRenderRecursiveAction(Spliterator<SingleQuadParticle> spliterator,
										 AsyncQuadParticleGroup group,
										 Frustum frustum,
										 Camera camera,
										 float partialTick,
										 int depth) {
		this.spliterator = spliterator;
		this.group = group;
		this.frustum = frustum;
		this.camera = camera;
		this.partialTick = partialTick;
		this.depth = depth;
	}

	@Override
	public void compute() {
		Spliterator<SingleQuadParticle> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new ParticleRenderRecursiveAction(sub, group, frustum, camera, partialTick, depth + 1)
				.fork();
			ForkJoinTask<Void> right = new ParticleRenderRecursiveAction(spliterator, group, frustum, camera, partialTick, depth + 1)
				.fork();
			left.join();
			right.join();
		} else {
			float f2 = partialTick + 1f;
			spliterator.forEachRemaining(particle -> {
				if (!frustum.pointInFrustum(particle.x, particle.y, particle.z)) {
					return;
				}
				ParticleAddon particleAddon = (ParticleAddon) particle;
				SingleQuadParticle.Layer layer = particle.getLayer();
				if (particleAddon.asyncparticles$isRenderSync()) {
					AsyncRenderBehavior.recordSync(layer, particle);
					return;
				}
				float f3 = particleAddon.asyncparticles$isTicked() ? partialTick : f2;
				try {
					particle.extract(group.getRenderState(), camera, f3);
				} catch (Throwable var9) {
					CrashReport crashReport = CrashReport.forThrowable(var9, "Rendering Particle");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
					crashReportCategory.setDetail("Particle", particle::toString);
					crashReportCategory.setDetail("Particle Type", group.getParticleType()::toString);
					throw new ReportedException(crashReport);
				}
			});
		}
	}
}
