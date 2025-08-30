package fun.qu_an.minecraft.asyncparticles.client.particle.forge;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;

@SuppressWarnings("unused")
public class GpuParticlesImpl {
	public static String getRenderMethod() {
		return FMLLoader.getNameFunction("srg")
			.map(f -> f.apply(INameMappingService.Domain.METHOD, "m_5744_"))
			.orElse("m_5744_");
	}
}
