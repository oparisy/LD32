package org.oparisy.fields.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Util;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.tools.common.Tools;

public class OverlayProgram {

	private static final String vertexShaderFile = "fields/overlay.vs";
	private static final String fragmentShaderFile = "fields/overlay.fs";

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

	public OverlayProgram() throws Exception {
		int vs = Tools.makeShader(GL_VERTEX_SHADER, vertexShaderFile);
		int fs = Tools.makeShader(GL_FRAGMENT_SHADER, fragmentShaderFile);
		program = Tools.makeProgram(vs, fs);
		
		// Get program attributes
		posID = Tools.glGetAttribLocationChecked(program, "vertexPosition_modelspace");
		uvID = Tools.glGetAttribLocationChecked(program, "vertexUV");

		pvmID = Tools.glGetUniformLocationChecked(program, "PVM");
		charPosID = Tools.glGetUniformLocationChecked(program, "charPos");
		samplerID = Tools.glGetUniformLocationChecked(program, "myTextureSampler");
		asciiID = Tools.glGetUniformLocationChecked(program, "ascii");

		// Vertices for one triangle
		vertexBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(quad_vertex), GL_STATIC_DRAW);
		//glVertexAttribPointer(posID, 2, GL_FLOAT, false, 0, 0);

		uvBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(uv_data), GL_STATIC_DRAW);
		//glVertexAttribPointer(uvID, 2, GL_FLOAT, false, 0, 0);

		BufferedImage fontImg = ImageIO.read(Tools.loadResource("fields/font.png"));
		font = uploadTexture(fontImg);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		Util.checkGLError();
	}

	public void use() {
		glUseProgram(program);
	}

	public void setProjectionMatrix(Matrix4f overlayMatrix) {
		glUniformMatrix4(pvmID, false, Tools.buildFloatBuffer(overlayMatrix));
	}
	
	public void bindBuffers() {
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glVertexAttribPointer(posID, 2, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
		glVertexAttribPointer(uvID, 2, GL_FLOAT, false, 0, 0);
	}

	/** Bind texture to texture unit 0 */
	public void bindTexture() {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, font);
		glUniform1i(samplerID, 0);
	}

	public void enableVertexAttribArrays() {
		glEnableVertexAttribArray(posID);
		glEnableVertexAttribArray(uvID);
	}

	public void disableVertexAttribArrays() {
		glDisableVertexAttribArray(posID);
		glDisableVertexAttribArray(uvID);
	}

	public void drawText(int x, int y, String string, float scale) {
		for (int chr : string.toUpperCase().toCharArray()) {
			if (chr != 32) {
				glUniform1i(asciiID, chr);
				glUniformMatrix4(charPosID, false, Tools.buildFloatBuffer(new Matrix4f().translate(new Vector2f(x, y)).scale(new Vector3f(scale,scale,scale))));
				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			}
			x += (FONT_WIDTH + CHAR_HORIZONTAL_MARGIN) * SCALE* scale;
		}
	}
	
	// TODO Factorize with Main
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
}
