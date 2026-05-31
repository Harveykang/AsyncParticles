package fun.qu_an.minecraft.asyncparticles.client.config.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;

public class AsyncParticlesModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AsyncParticlesConfig::newConfigScreen;
	}
}
