package fun.qu_an.minecraft.asyncparticles.client.compat.forge;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;

@SuppressWarnings("unused")
public class MappingsImpl {
	public static String getRenderMethod() {
		return FMLLoader.getNameFunction("srg")
			.map(f -> f.apply(INameMappingService.Domain.METHOD, "m_5744_"))
			.orElse("m_5744_");
	}

	public static String getFireworkSparkClass() {
		return "net.minecraft.client.particle.FireworkParticles$SparkParticle";
	}
}
