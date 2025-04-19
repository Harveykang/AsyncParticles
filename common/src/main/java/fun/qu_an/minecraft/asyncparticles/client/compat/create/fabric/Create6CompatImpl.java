package fun.qu_an.minecraft.asyncparticles.client.compat.create.fabric;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.level.LevelAccessor;

import java.lang.ref.WeakReference;
import java.util.Map;

@SuppressWarnings("unused")
public class Create6CompatImpl {
	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(LevelAccessor level) {
		throw new UnsupportedOperationException("Create 6 is not supported");
	}
}
