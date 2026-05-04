package net.irisshaders.iris.gl.shader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderCompileException extends RuntimeException {
	private static final Pattern ERROR_LINE_PATTERN = Pattern.compile("(\\d+)?:(\\d+)\\(?\\d*\\)?\\s*:(.*)");
	private static final Pattern NVIDIA_ERROR_PATTERN = Pattern.compile("(\\d+)\\((\\d+)\\)\\s*:(.*)");

	private final String filename;
	private final String error;
	private final String shaderSource;
	private int errorLine = -1;
	private int errorColumn = -1;

	public ShaderCompileException(String filename, String error) {
		super(filename + ": " + error);
		this.filename = filename;
		this.error = error;
		this.shaderSource = null;
		parseErrorLocation();
	}

	public ShaderCompileException(String filename, String error, String shaderSource) {
		super(filename + ": " + error);
		this.filename = filename;
		this.error = error;
		this.shaderSource = shaderSource;
		parseErrorLocation();
	}

	public ShaderCompileException(String filename, Exception error) {
		super(error);
		this.filename = filename;
		this.error = error.getMessage();
		this.shaderSource = null;
		parseErrorLocation();
	}

	public ShaderCompileException(String filename, Exception error, String shaderSource) {
		super(error);
		this.filename = filename;
		this.error = error.getMessage();
		this.shaderSource = shaderSource;
		parseErrorLocation();
	}

	private void parseErrorLocation() {
		if (error == null) return;

		String[] lines = error.split("\\r?\\n");
		for (String line : lines) {
			Matcher intelAmdMatcher = ERROR_LINE_PATTERN.matcher(line);
			if (intelAmdMatcher.find()) {
				try {
					if (intelAmdMatcher.group(1) != null) {
						errorLine = Integer.parseInt(intelAmdMatcher.group(1));
					}
					errorColumn = Integer.parseInt(intelAmdMatcher.group(2));
					return;
				} catch (NumberFormatException ignored) {
				}
			}

			Matcher nvidiaMatcher = NVIDIA_ERROR_PATTERN.matcher(line);
			if (nvidiaMatcher.find()) {
				try {
					errorLine = Integer.parseInt(nvidiaMatcher.group(1));
					errorColumn = Integer.parseInt(nvidiaMatcher.group(2));
					return;
				} catch (NumberFormatException ignored) {
				}
			}
		}
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(filename).append("] Shader compilation failed\n");
		sb.append("Error: ").append(error).append("\n");

		if (errorLine > 0) {
			sb.append("Location: line ").append(errorLine);
			if (errorColumn > 0) {
				sb.append(", column ").append(errorColumn);
			}
			sb.append("\n");

			if (shaderSource != null) {
				sb.append("\nContext:\n");
				sb.append(getSourceContext());
			}
		}

		return sb.toString();
	}

	public String getSourceContext() {
		if (shaderSource == null || errorLine <= 0) {
			return "";
		}

		String[] lines = shaderSource.split("\\r?\\n");
		StringBuilder context = new StringBuilder();

		int start = Math.max(0, errorLine - 4);
		int end = Math.min(lines.length, errorLine + 2);

		for (int i = start; i < end; i++) {
			String lineNum = String.format("%4d", i + 1);
			String prefix = (i + 1 == errorLine) ? " >>> " : "     ";
			context.append(prefix).append(lineNum).append(" | ").append(lines[i]).append("\n");
		}

		return context.toString();
	}

	public String getError() {
		return error;
	}

	public String getFilename() {
		return filename;
	}

	public int getErrorLine() {
		return errorLine;
	}

	public int getErrorColumn() {
		return errorColumn;
	}

	public String getShaderSource() {
		return shaderSource;
	}
}
