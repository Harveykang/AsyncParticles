package fun.qu_an.minecraft.asyncparticles.client.mixin.photon_editor;

import com.lowdragmc.photon.client.gameobject.emitter.ParticleQueueRenderType;
import com.lowdragmc.photon.client.gameobject.emitter.PhotonParticleRenderType;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ParticleQueueRenderType.class)
public class MixinParticleQueueRenderType {
	@Mutable
	@Shadow(remap = false)
	@Final
	protected Map<PhotonParticleRenderType, Queue<IParticle>> particles;

	@Inject(method = "<init>", remap = false, at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		particles = new ConcurrentHashMap<>();
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	@Redirect(method = "pipeQueue", at = @At(value = "INVOKE", target = "Ljava/util/Queue;addAll(Ljava/util/Collection;)Z"))
	private boolean onPipeQueue(Queue<IParticle> queue, Collection<IParticle> collection) {
		if (InternalRenderingMode.isSync()) {
			return queue.addAll(collection);
		} else {
			synchronized (queue) {
				return queue.addAll(collection);
			}
		}
	}
}
