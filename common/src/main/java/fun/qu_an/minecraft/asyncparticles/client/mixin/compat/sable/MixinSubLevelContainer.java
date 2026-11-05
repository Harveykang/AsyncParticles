package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.List;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class MixinSubLevelContainer {
	@Shadow
	@Final
	private List<SubLevel> allSubLevels;

	@WrapMethod(method = "queryIntersecting")
	private Iterable<SubLevel> queryIntersectingSafely(BoundingBox3dc bounds, Operation<Iterable<SubLevel>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call(bounds);
		}
		Object[] elements;
		int size;
		List<SubLevel> allSubLevels = this.allSubLevels;
		if (allSubLevels instanceof ReferenceArrayList<?> list) {
			elements = list.elements();
			size = Math.min(list.size(), elements.length);
		} else if (allSubLevels instanceof ObjectArrayList<?> list) {
			elements = list.elements();
			size = Math.min(list.size(), elements.length);
		} else {
			elements = allSubLevels.toArray();
			size = elements.length;
		}
		List<SubLevel> intersecting = new ReferenceArrayList<>(size);
		for (int i = 0; i < size; i++) {
			SubLevel subLevel = (SubLevel) elements[i];
			if (subLevel != null && subLevel.boundingBox().intersects(bounds)) {
				intersecting.add(subLevel);
			}
		}
		return Collections.unmodifiableList(intersecting);
	}

	@WrapMethod(method = "getAllSubLevels")
	private List<? extends SubLevel> getAllSubLevelsSafely(Operation<List<? extends SubLevel>> original) {
		if (!ThreadUtil.isOnParticleThread()) {
			return original.call();
		}
		Object[] elements;
		int size;
		List<? extends SubLevel> allSubLevels = original.call();
		if (allSubLevels instanceof ReferenceArrayList<?> list) {
			elements = list.elements();
			size = Math.min(list.size(), elements.length);
		} else if (allSubLevels instanceof ObjectArrayList<?> list) {
			elements = list.elements();
			size = Math.min(list.size(), elements.length);
		} else {
			elements = allSubLevels.toArray();
			size = elements.length;
		}
		List<SubLevel> intersecting = new ReferenceArrayList<>(size);
		for (int i = 0; i < size; i++) {
			SubLevel subLevel = (SubLevel) elements[i];
			if (subLevel != null) {
				intersecting.add(subLevel);
			}
		}
		return Collections.unmodifiableList(intersecting);
	}
}
