package org.oparisy.fields.tools.loadbmp;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;
import org.oparisy.fields.tools.common.Tools;

/**
 * Adapted from {@link http
 * ://duriansoftware.com/joe/An-intro-to-modern-OpenGL.-Chapter-2:-Hello-World:-The-Slideshow.html}
 */
public class MainLoadBMP {

	private static final boolean USE_DEBUG_CONTEXT = true;

	private static final String RES = MainLoadBMP.class.getPackage().getName().replace(".", "/");

	private float g_vertex_buffer_data[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };

	private byte g_element_buffer_data[] = { 0, 1, 2, 3 };

	private static final int FLOAT_SIZE = 4;

	private BufferedImage image;

	private int vertexBuffer;
	private int elementBuffer;
	private int texture;

	private int program;

	private int positionAttribute;
	private int textureUniform;

	public void start() throws Exception {

		image = ImageIO.read(Tools.loadResource(RES + "/uvtemplate.bmp"));

		// Set up OpenGL through Display
		DisplayMode mode = new DisplayMode(image.getWidth(), image.getHeight());
		Display.setDisplayMode(mode);
		Display.setTitle("Hello World");
		if (USE_DEBUG_CONTEXT) {
			// Set up ARB_debug_output (requires a debug context)
			PixelFormat pixelFormat = new PixelFormat();
			ContextAttribs contextAttributes = new ContextAttribs(3, 2).withDebug(true).withProfileCompatibility(true);
			Display.create(pixelFormat, contextAttributes);
			ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new MessageHandler()));
		} else {
			Display.create();
		}

		setupResources();

		while (!Display.isCloseRequested()) {
			render();
			Display.update();
		}

		Display.destroy();
	}

	private void setupResources() throws Exception {
		vertexBuffer = Tools.makeFloatBuffer(GL_ARRAY_BUFFER, g_vertex_buffer_data);
		elementBuffer = Tools.makeByteBuffer(GL_ELEMENT_ARRAY_BUFFER, g_element_buffer_data);

		// Make buffers
		texture = Tools.makeTexture(image, GL_RGB, GL_BGR);

		// Make shaders, link program
		int vertexShader = Tools.makeShader(GL_VERTEX_SHADER, RES + "/hello-gl.v.glsl");
		int fragmentShader = Tools.makeShader(GL_FRAGMENT_SHADER, RES + "/hello-gl.f.glsl");
		program = Tools.makeProgram(vertexShader, fragmentShader);

		// Get shader variables location
		textureUniform = Tools.glGetUniformLocationChecked(program, "texture");
		positionAttribute = Tools.glGetAttribLocationChecked(program, "position");
		Util.checkGLError();
	}

	private void render() {

		// Activate shader program
		glUseProgram(program);

		// Assign textures
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glUniform1i(textureUniform, 0);

		// Set up vertex array
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glVertexAttribPointer(positionAttribute, 2, GL_FLOAT, false, 2 * FLOAT_SIZE, 0);
		glEnableVertexAttribArray(positionAttribute);

		// Submit rendering job
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
		glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_BYTE, 0);

		// Clean up state
		glDisableVertexAttribArray(positionAttribute);
		glDisableVertexAttribArray(textureUniform);
		Util.checkGLError();
	}

	public static void main(String[] argv) {
		MainLoadBMP displayExample = new MainLoadBMP();
		try {
			displayExample.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
