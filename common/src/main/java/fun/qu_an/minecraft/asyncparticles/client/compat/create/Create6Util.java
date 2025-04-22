package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.LevelAccessor;

import java.lang.ref.WeakReference;
import java.util.Map;

public class Create6Util {
	@ExpectPlatform
	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(LevelAccessor level) {
		throw new AssertionError();
	}
}
