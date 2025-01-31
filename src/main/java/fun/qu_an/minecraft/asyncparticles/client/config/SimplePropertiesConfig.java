package fun.qu_an.minecraft.asyncparticles.client.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SimplePropertiesConfig {
	public static final Path CONFIG_FILE = Paths.get("config", "asyncparticles.properties");
	public static int limit = 32768;

	public static void load() throws IOException {
		Properties properties = new Properties();
		if (!Files.exists(CONFIG_FILE)) {
			Files.createDirectories(CONFIG_FILE.getParent());
			Files.createFile(CONFIG_FILE);
			properties.setProperty("limit", String.valueOf(32768));
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
		} else {
			properties.load(Files.newInputStream(CONFIG_FILE));
		}
		String s = properties.getProperty("limit");
		int i;
		try {
			i = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			properties.setProperty("limit", String.valueOf(32768));
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
			i = 32768;
		}
		limit = i;
	}

}
