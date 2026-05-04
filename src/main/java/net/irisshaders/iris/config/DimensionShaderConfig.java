package net.irisshaders.iris.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.irisshaders.iris.Iris;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DimensionShaderConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type CONFIG_TYPE = new TypeToken<Map<String, String>>() {}.getType();
	private final Path configPath;
	private Map<String, String> dimensionToShaderMap = new HashMap<>();

	public DimensionShaderConfig(Path configPath) {
		this.configPath = configPath;
	}

	public void load() throws IOException {
		if (!Files.exists(configPath)) {
			dimensionToShaderMap = new HashMap<>();
			return;
		}

		try (InputStream is = Files.newInputStream(configPath)) {
			String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			dimensionToShaderMap = GSON.fromJson(json, CONFIG_TYPE);
			if (dimensionToShaderMap == null) {
				dimensionToShaderMap = new HashMap<>();
			}
		}
	}

	public void save() throws IOException {
		try (OutputStream os = Files.newOutputStream(configPath)) {
			String json = GSON.toJson(dimensionToShaderMap);
			os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
	}

	public Optional<String> getShaderForDimension(String dimensionId) {
		return Optional.ofNullable(dimensionToShaderMap.get(dimensionId));
	}

	public void setShaderForDimension(String dimensionId, String shaderPackName) {
		dimensionToShaderMap.put(dimensionId, shaderPackName);
	}

	public void removeShaderForDimension(String dimensionId) {
		dimensionToShaderMap.remove(dimensionId);
	}

	public Map<String, String> getAllMappings() {
		return new HashMap<>(dimensionToShaderMap);
	}

	public boolean hasMapping(String dimensionId) {
		return dimensionToShaderMap.containsKey(dimensionId);
	}
}
