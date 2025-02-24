package fun.qu_an.minecraft.asyncparticles.client.forge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import net.minecraftforge.fml.common.Mod;

@Mod(AsyncparticlesClient.MOD_ID)
public final class AsyncparticlesClientForge {
	public AsyncparticlesClientForge() {
		// Run our common setup.
		AsyncparticlesClient.init();
	}
}
