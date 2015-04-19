package tests.textdraw;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.oparisy.fields.tools.common.MessageHandler;
import org.oparisy.fields.tools.common.Tools;

public class TextDrawSample {

	private static final int SCREEN_WIDTH = 640;
	private static final int SCREEN_HEIGHT = 400;

	// Resources (shader sources) location
	private static final String RES = TextDrawSample.class.getPackage().getName().replace(".", "/");

	private static final String vertexShaderFile = RES + "/" + "render.vs";
	private static final String fragmentShaderFile = RES + "/" + "render.fs";

	private static final int FONT_WIDTH = 5;
	private static final int FONT_HEIGHT = 5;
	private static final int CHAR_HORIZONTAL_MARGIN = 1;

	private static final int SCALE = 2;

	// A quad
	private static final float quad_vertex[] = {
			//
			FONT_WIDTH * SCALE, 0,//
			0, 0,//
			FONT_WIDTH * SCALE, FONT_HEIGHT * SCALE,//
			0, FONT_HEIGHT * SCALE };

	// Will be scaled in fragment shader
	private static final float uv_data[] = {
			//
			1, 0,//
			0, 0,//
			1, 1,//
			0, 1 //
	};

	private int vertexBuffer;
	private int uvBuffer;

	private int program;

	private int posID;
	private int uvID;

	private int pvmID;
	private int font;
	private int samplerID;
	private int asciiID;
	private int charPosID;

	private void setup() throws LWJGLException, Exception, Error {
		DisplayMode mode = new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT);
		Display.setDisplayMode(mode);
		Display.setTitle(this.getClass().getSimpleName());

		// Set up ARB_debug_output (requires a debug context)
		PixelFormat pixelFormat = new PixelFormat();
		ContextAttribs contextAttributes = new ContextAttribs(3, 3).withDebug(true).withProfileCompatibility(true);
		Display.create(pixelFormat, contextAttributes);
		ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new MessageHandler()));

		System.out.println("GL version: " + glGetString(GL_VERSION));

		// Program must be compiled before binding buffers!
		// (otherwise I get a Out of memory 1285 error)
		int vs = Tools.makeShader(GL_VERTEX_SHADER, vertexShaderFile);
		int fs = Tools.makeShader(GL_FRAGMENT_SHADER, fragmentShaderFile);
		program = glCreateProgram();
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		Tools.glLinkProgramChecked(program);

		// Get program attributes
		posID = Tools.glGetAttribLocationChecked(program, "vertexPosition_modelspace");
		uvID = Tools.glGetAttribLocationChecked(program, "vertexUV");

		pvmID = Tools.glGetUniformLocationChecked(program, "PVM");
		charPosID = Tools.glGetUniformLocationChecked(program, "charPos");
		samplerID = Tools.glGetUniformLocationChecked(program, "myTextureSampler");
		asciiID = Tools.glGetUniformLocationChecked(program, "ascii");

		// Create a VAO to store buffers configuration
		glBindVertexArray(glGenVertexArrays());

		// Vertices for one triangle
		vertexBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(quad_vertex), GL_STATIC_DRAW);
		glVertexAttribPointer(posID, 2, GL_FLOAT, false, 0, 0);

		uvBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(uv_data), GL_STATIC_DRAW);
		glVertexAttribPointer(uvID, 2, GL_FLOAT, false, 0, 0);

		BufferedImage fontImg = ImageIO.read(Tools.loadResource("fields/font.png"));
		font = uploadTexture(fontImg);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

		// Required by glUniformMatrix4
		glUseProgram(program);

		// Set up projection matrix (will not change)
		// Identity view matrix (2D projection)
		Matrix4f projectionMatrix = Tools.glOrtho(0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, -1);
		glUniformMatrix4(pvmID, false, Tools.buildFloatBuffer(projectionMatrix));

		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		Util.checkGLError();
	}

	private void render() {

		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// Bind texture to texture unit 0
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, font);
		glUniform1i(samplerID, 0);

		// TODO Put those in setup?
		glEnableVertexAttribArray(posID);
		glEnableVertexAttribArray(uvID);

		// Draw
		int x = 0, y = 0;
		for (int chr : "Hello World".toUpperCase().toCharArray()) {
			if (chr != 32) {
				glUniform1i(asciiID, chr);
				glUniformMatrix4(charPosID, false, Tools.buildFloatBuffer(new Matrix4f().translate(new Vector2f(x, y))));
				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			}
			x += (FONT_WIDTH + CHAR_HORIZONTAL_MARGIN) * SCALE;
		}

		// TODO Are those needed here?
		// glDisableVertexAttribArray(posID);
		// glDisableVertexAttribArray(colorID);
	}

	private void start() throws Exception {
		setup();

		while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			render();
			Display.update();
		}

		Display.destroy();
	}

	public static int uploadTexture(BufferedImage img) {
		img = Tools.flipVertically(img);
		ByteBuffer pixels = Tools.imageToByteBuffer(img);

		// Create one OpenGL texture
		int textureID = glGenTextures();

		// "Bind" the newly created texture: it will be modified by all future texture functions
		glBindTexture(GL_TEXTURE_2D, textureID);

		// Upload the image to OpenGL
		int format = Tools.getGLFormat(img);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, img.getWidth(), img.getHeight(), 0, format, GL_UNSIGNED_BYTE, pixels);

		return textureID;
	}

	public static void main(String[] argv) {
		TextDrawSample tut = new TextDrawSample();
		try {
			tut.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
