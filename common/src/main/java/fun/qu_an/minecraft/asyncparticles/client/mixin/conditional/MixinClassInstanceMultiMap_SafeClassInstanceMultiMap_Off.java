package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.compat.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(value = ClassInstanceMultiMap.class)
public class MixinClassInstanceMultiMap_SafeClassInstanceMultiMap_Off {
	@WrapMethod(method = "getAllInstances")
	public List<?> onGetAllInstances(Operation<List<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call();
		}
		Diagnostic.illegalEntityStorageAccess();
		return Collections.emptyList();
	}

	@WrapMethod(method = "iterator")
	public Iterator<?> onIterator(Operation<Iterator<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call();
		}
		Diagnostic.illegalEntityStorageAccess();
		return Collections.emptyIterator();
	}

	@WrapMethod(method = "find")
	public Collection<?> onFind(Class<?> class_, Operation<Collection<?>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call(class_);
		}
		Diagnostic.illegalEntityStorageAccess();
		return Collections.emptyList();
	}
}
