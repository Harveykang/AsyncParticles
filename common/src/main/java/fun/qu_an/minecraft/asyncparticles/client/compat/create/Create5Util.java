package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import net.minecraft.world.level.LevelAccessor;

import java.lang.ref.WeakReference;
import java.util.Map;

class Create5Util {
	static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(LevelAccessor level) {
		return ContraptionHandler.loadedContraptions.get(level);
	}
}
