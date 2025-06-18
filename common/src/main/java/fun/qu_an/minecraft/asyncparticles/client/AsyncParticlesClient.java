package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;

import java.net.URI;

public class AsyncParticlesClient {
	public static final String MOD_ID = "asyncparticles";
	public static final String ISSUE_URL_STR = "https://github.com/Harveykang/AsyncParticles/issues";
	public static final URI ISSUE_URI = URI.create(ISSUE_URL_STR);

	public static void init() {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		try {
			ConfigHelper.load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
