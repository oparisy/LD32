package org.oparisy.fields.tools.loadbmp;

import static org.lwjgl.opengl.ARBDebugOutput.*;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.ARBDebugOutputCallback;

/** Adapted from LWJGL. Only display messages once to avoid clogging console. */
public class MessageHandler implements ARBDebugOutputCallback.Handler {
	
	private Set<String> encountered = new HashSet<String>();
	
	public void handleMessage(final int source, final int type, final int id, final int severity, final String message) {
		StringBuilder sb = new StringBuilder();
		sb.append("[LWJGL] ARB_debug_output message\n");
		sb.append("\tID: " + id + "\n");

		String description;
		switch (source) {
		case GL_DEBUG_SOURCE_API_ARB:
			description = "API";
			break;
		case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB:
			description = "WINDOW SYSTEM";
			break;
		case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB:
			description = "SHADER COMPILER";
			break;
		case GL_DEBUG_SOURCE_THIRD_PARTY_ARB:
			description = "THIRD PARTY";
			break;
		case GL_DEBUG_SOURCE_APPLICATION_ARB:
			description = "APPLICATION";
			break;
		case GL_DEBUG_SOURCE_OTHER_ARB:
			description = "OTHER";
			break;
		default:
			description = printUnknownToken(source);
		}
		sb.append("\tSource: " + description + "\n");

		switch (type) {
		case GL_DEBUG_TYPE_ERROR_ARB:
			description = "ERROR";
			break;
		case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB:
			description = "DEPRECATED BEHAVIOR";
			break;
		case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB:
			description = "UNDEFINED BEHAVIOR";
			break;
		case GL_DEBUG_TYPE_PORTABILITY_ARB:
			description = "PORTABILITY";
			break;
		case GL_DEBUG_TYPE_PERFORMANCE_ARB:
			description = "PERFORMANCE";
			break;
		case GL_DEBUG_TYPE_OTHER_ARB:
			description = "OTHER";
			break;
		default:
			description = printUnknownToken(type);
		}
		sb.append("\tType: " + description + "\n");

		switch (severity) {
		case GL_DEBUG_SEVERITY_HIGH_ARB:
			description = "HIGH";
			break;
		case GL_DEBUG_SEVERITY_MEDIUM_ARB:
			description = "MEDIUM";
			break;
		case GL_DEBUG_SEVERITY_LOW_ARB:
			description = "LOW";
			break;
		default:
			description = printUnknownToken(severity);
		}
		sb.append("\tSeverity: " + description + "\n");

		sb.append("\tMessage: " + message + "\n");
		
		String msg = sb.toString();
		if (!encountered.contains(msg)) {
			encountered.add(msg);
			System.out.println(msg);
		}
	}

	private String printUnknownToken(final int token) {
		return "Unknown (0x" + Integer.toHexString(token).toUpperCase() + ")";
	}
}
