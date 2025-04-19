package fun.qu_an.minecraft.asyncparticles.client.compat.create.forge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import net.minecraft.world.level.LevelAccessor;

import java.lang.ref.WeakReference;
import java.util.Map;

@SuppressWarnings("unused")
public class Create6CompatImpl {
	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(LevelAccessor level) {
		return ContraptionHandler.loadedContraptions.get(level);
	}
}
