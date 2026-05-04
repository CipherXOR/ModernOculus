package net.irisshaders.iris.gl.shader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlUtil;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.texture.format.TextureFormat;
import net.irisshaders.iris.texture.format.TextureFormatLoader;
import net.minecraft.Util;
import net.minecraftforge.fml.loading.LoadingModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StandardMacros {
	private static final Pattern SEMVER_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.*(?<bugfix>\\d*)(.*)");

	private static volatile ImmutableList<StringPair> cachedEnvironmentDefines;
	private static volatile ImmutableMap<String, String> cachedRenderStages;
	private static volatile ImmutableSet<String> cachedGlExtensions;
	private static volatile String cachedMcVersion;
	private static volatile String cachedGlVersion;
	private static volatile String cachedGlslVersion;
	private static volatile String cachedOsString;
	private static volatile String cachedVendor;
	private static volatile String cachedRenderer;

	private static void define(List<StringPair> defines, String key) {
		defines.add(new StringPair(key, ""));
	}

	private static void define(List<StringPair> defines, String key, String value) {
		defines.add(new StringPair(key, value));
	}

	public static ImmutableList<StringPair> createStandardEnvironmentDefines() {
		if (cachedEnvironmentDefines != null) {
			return cachedEnvironmentDefines;
		}

		ArrayList<StringPair> standardDefines = new ArrayList<>();

		define(standardDefines, "MC_VERSION", getMcVersion());
		define(standardDefines, "MC_GL_VERSION", getGlVersion(GL20C.GL_VERSION));
		define(standardDefines, "MC_GLSL_VERSION", getGlVersion(GL20C.GL_SHADING_LANGUAGE_VERSION));
		define(standardDefines, getOsString());
		define(standardDefines, getVendor());
		define(standardDefines, getRenderer());
		define(standardDefines, "IS_IRIS");
		define(standardDefines, "IS_OCULUS");

		if (LoadingModList.get().getModFileById("distanthorizons") != null && DHCompat.hasRenderingEnabled()) {
			define(standardDefines, "DISTANT_HORIZONS");
		}

		define(standardDefines, "DH_BLOCK_UNKNOWN", String.valueOf(0));
		define(standardDefines, "DH_BLOCK_LEAVES", String.valueOf(1));
		define(standardDefines, "DH_BLOCK_STONE", String.valueOf(2));
		define(standardDefines, "DH_BLOCK_WOOD", String.valueOf(3));
		define(standardDefines, "DH_BLOCK_METAL", String.valueOf(4));
		define(standardDefines, "DH_BLOCK_DIRT", String.valueOf(5));
		define(standardDefines, "DH_BLOCK_LAVA", String.valueOf(6));
		define(standardDefines, "DH_BLOCK_DEEPSLATE", String.valueOf(7));
		define(standardDefines, "DH_BLOCK_SNOW", String.valueOf(8));
		define(standardDefines, "DH_BLOCK_SAND", String.valueOf(9));
		define(standardDefines, "DH_BLOCK_TERRACOTTA", String.valueOf(10));
		define(standardDefines, "DH_BLOCK_NETHER_STONE", String.valueOf(11));
		define(standardDefines, "DH_BLOCK_WATER", String.valueOf(12));
		define(standardDefines, "DH_BLOCK_GRASS", String.valueOf(13));
		define(standardDefines, "DH_BLOCK_AIR", String.valueOf(14));
		define(standardDefines, "DH_BLOCK_ILLUMINATED", String.valueOf(15));

		for (String glExtension : getGlExtensions()) {
			define(standardDefines, glExtension);
		}

		define(standardDefines, "MC_NORMAL_MAP");
		define(standardDefines, "MC_SPECULAR_MAP");
		define(standardDefines, "MC_RENDER_QUALITY", "1.0");
		define(standardDefines, "MC_SHADOW_QUALITY", "1.0");
		define(standardDefines, "MC_HAND_DEPTH", Float.toString(HandRenderer.DEPTH));

		TextureFormat textureFormat = TextureFormatLoader.getFormat();
		if (textureFormat != null) {
			for (String define : textureFormat.getDefines()) {
				define(standardDefines, define);
			}
		}

		getRenderStages().forEach((stage, index) -> define(standardDefines, stage, index));

		for (String irisDefine : getIrisDefines()) {
			define(standardDefines, irisDefine);
		}

		cachedEnvironmentDefines = ImmutableList.copyOf(standardDefines);
		return cachedEnvironmentDefines;
	}

	public static String getMcVersion() {
		if (cachedMcVersion != null) {
			return cachedMcVersion;
		}

		String version = Iris.getReleaseTarget();

		if (version == null) {
			throw new IllegalStateException("Could not get the current minecraft version!");
		}

		String[] splitVersion = version.split("\\.");

		if (splitVersion.length < 2) {
			Iris.logger.error("Could not parse game version \"" + version + "\"");
			splitVersion = Iris.getBackupVersionNumber().split("\\.");
		}

		String major = splitVersion[0];
		String minor = splitVersion[1];
		String bugfix;

		if (splitVersion.length < 3) {
			bugfix = "00";
		} else {
			bugfix = splitVersion[2];
		}

		if (minor.length() == 1) {
			minor = 0 + minor;
		}
		if (bugfix.length() == 1) {
			bugfix = 0 + bugfix;
		}

		cachedMcVersion = major + minor + bugfix;
		return cachedMcVersion;
	}

	public static String getGlVersion(int name) {
		if (name == GL20C.GL_VERSION && cachedGlVersion != null) {
			return cachedGlVersion;
		}
		if (name == GL20C.GL_SHADING_LANGUAGE_VERSION && cachedGlslVersion != null) {
			return cachedGlslVersion;
		}

		String info = GlStateManager._getString(name);

		Matcher matcher = SEMVER_PATTERN.matcher(Objects.requireNonNull(info));

		if (!matcher.matches()) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		String major = group(matcher, "major");
		String minor = group(matcher, "minor");
		String bugfix = group(matcher, "bugfix");

		if (bugfix == null) {
			bugfix = "0";
		}

		if (major == null || minor == null) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		String result = major + minor + bugfix;

		if (name == GL20C.GL_VERSION) {
			cachedGlVersion = result;
		} else if (name == GL20C.GL_SHADING_LANGUAGE_VERSION) {
			cachedGlslVersion = result;
		}

		return result;
	}

	public static String group(Matcher matcher, String name) {
		try {
			return matcher.group(name);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return null;
		}
	}

	public static String getOsString() {
		if (cachedOsString != null) {
			return cachedOsString;
		}

		cachedOsString = switch (Util.getPlatform()) {
			case OSX -> "MC_OS_MAC";
			case LINUX -> "MC_OS_LINUX";
			case WINDOWS -> "MC_OS_WINDOWS";
			default -> "MC_OS_UNKNOWN";
		};

		return cachedOsString;
	}

	public static String getVendor() {
		if (cachedVendor != null) {
			return cachedVendor;
		}

		String vendor = Objects.requireNonNull(GlUtil.getVendor()).toLowerCase(Locale.ROOT);
		if (vendor.startsWith("ati")) {
			cachedVendor = "MC_GL_VENDOR_ATI";
		} else if (vendor.startsWith("intel")) {
			cachedVendor = "MC_GL_VENDOR_INTEL";
		} else if (vendor.startsWith("nvidia")) {
			cachedVendor = "MC_GL_VENDOR_NVIDIA";
		} else if (vendor.startsWith("amd")) {
			cachedVendor = "MC_GL_VENDOR_AMD";
		} else if (vendor.startsWith("x.org")) {
			cachedVendor = "MC_GL_VENDOR_XORG";
		} else {
			cachedVendor = "MC_GL_VENDOR_OTHER";
		}

		return cachedVendor;
	}

	public static String getRenderer() {
		if (cachedRenderer != null) {
			return cachedRenderer;
		}

		String renderer = Objects.requireNonNull(GlUtil.getRenderer()).toLowerCase(Locale.ROOT);
		if (renderer.startsWith("amd")) {
			cachedRenderer = "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("ati")) {
			cachedRenderer = "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("radeon")) {
			cachedRenderer = "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("gallium")) {
			cachedRenderer = "MC_GL_RENDERER_GALLIUM";
		} else if (renderer.startsWith("intel")) {
			cachedRenderer = "MC_GL_RENDERER_INTEL";
		} else if (renderer.startsWith("geforce")) {
			cachedRenderer = "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("nvidia")) {
			cachedRenderer = "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("quadro")) {
			cachedRenderer = "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("nvs")) {
			cachedRenderer = "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("mesa")) {
			cachedRenderer = "MC_GL_RENDERER_MESA";
		} else {
			cachedRenderer = "MC_GL_RENDERER_OTHER";
		}

		return cachedRenderer;
	}

	public static Set<String> getGlExtensions() {
		if (cachedGlExtensions != null) {
			return cachedGlExtensions;
		}

		int numExtensions = GL30C.glGetInteger(GL30C.GL_NUM_EXTENSIONS);

		String[] extensions = new String[numExtensions];

		for (int i = 0; i < numExtensions; i++) {
			extensions[i] = GL30C.glGetStringi(GL30C.GL_EXTENSIONS, i);
		}

		cachedGlExtensions = Arrays.stream(extensions).map(s -> "MC_" + s).collect(ImmutableSet.toImmutableSet());
		return cachedGlExtensions;
	}

	public static Map<String, String> getRenderStages() {
		if (cachedRenderStages != null) {
			return cachedRenderStages;
		}

		ImmutableMap.Builder<String, String> stages = ImmutableMap.builder();
		for (WorldRenderingPhase phase : WorldRenderingPhase.values()) {
			stages.put("MC_RENDER_STAGE_" + phase.name(), String.valueOf(phase.ordinal()));
		}

		cachedRenderStages = stages.build();
		return cachedRenderStages;
	}

	public static List<String> getIrisDefines() {
		return List.of();
	}
}
