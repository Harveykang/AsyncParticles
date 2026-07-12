package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(value = ClassInstanceMultiMap.class)
public class MixinClassInstanceMultiMap_SafeClassInstanceMultiMap_Off {
	@Unique
	private static boolean asyncparticles$accessedOffThread = false;

	@WrapMethod(method = "getAllInstances")
	public List<?> onGetAllInstances(Operation<List<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call();
		}
		asyncparticles$alert();
		return Collections.emptyList();
	}

	@WrapMethod(method = "iterator")
	public Iterator<?> onIterator(Operation<Iterator<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call();
		}
		asyncparticles$alert();
		return Collections.emptyIterator();
	}

	@WrapMethod(method = "find")
	public Collection<?> onFind(Class<?> class_, Operation<Collection<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call(class_);
		}
		asyncparticles$alert();
		return Collections.emptyList();
	}

	@Unique
	private static void asyncparticles$alert() {
		if (!asyncparticles$accessedOffThread) {
			LogUtils.getLogger().warn("[AsyncParticles] Entity storage accessed off the main thread!\nConsider enabling 'safeClassInstanceMultiMap'.", new IllegalStateException(""));
			ThreadUtil.enqueueClientTask(() -> {
				Minecraft.getInstance().gui.getChat().addMessage(
					Component.literal("[AsyncParticles] ").append(
					Component.translatable("chat.asyncparticles.warn.get_entities_off_main_thread",
							Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap")))
						.withStyle(ChatFormatting.DARK_RED)
				);
			});
			asyncparticles$accessedOffThread = true;
		}
	}
}
