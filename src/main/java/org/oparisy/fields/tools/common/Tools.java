package org.oparisy.fields.tools.common;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTAbgr;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Util;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Tools {

	private static final int MAX_INFOLOG_LENGTH = 10240;

	public static int makeFloatBuffer(int target, float[] data) {
		// Sending data to OpenGL requires the usage of (flipped) buffers
		FloatBuffer dataBuffer = buildFloatBuffer(data);

		// Create and bind buffer
		int glBuffer = glGenBuffers();
		glBindBuffer(target, glBuffer);

		// Set up buffer
		glBufferData(target, dataBuffer, GL_STATIC_DRAW);

		// Deselect (bind to 0) the VBO
		glBindBuffer(target, 0);

		return glBuffer;
	}

	public static FloatBuffer buildFloatBuffer(float[] data) {
		FloatBuffer dataBuffer = BufferUtils.createFloatBuffer(data.length);
		dataBuffer.put(data);
		dataBuffer.flip();
		return dataBuffer;
	}

	public static FloatBuffer buildFloatBuffer(Vector3f[] array) {
		FloatBuffer dataBuffer = BufferUtils.createFloatBuffer(3 * array.length);
		for (Vector3f v : array) {
			v.store(dataBuffer);
		}

		dataBuffer.flip();
		return dataBuffer;
	}

	/** Return a buffer storing this matrix in column-major order. It is flipped (ready for reading). */
	public static FloatBuffer buildFloatBuffer(Matrix4f mat) {
		FloatBuffer dataBuffer = BufferUtils.createFloatBuffer(4 * 4);
		mat.store(dataBuffer);
		dataBuffer.flip();
		return dataBuffer;
	}

	public static IntBuffer buildIntBuffer(int[] array) {
		IntBuffer dataBuffer = BufferUtils.createIntBuffer(array.length);
		dataBuffer.put(array);
		dataBuffer.flip();
		return dataBuffer;
	}

	public static int makeByteBuffer(int target, byte[] data) {
		// Sending data to OpenGL requires the usage of (flipped) buffers
		ByteBuffer dataBuffer = BufferUtils.createByteBuffer(data.length);
		dataBuffer.put(data);
		dataBuffer.flip();

		// Create and bind buffer
		int glBuffer = glGenBuffers();
		glBindBuffer(target, glBuffer);

		// Set up buffer
		glBufferData(target, dataBuffer, GL_STATIC_DRAW);

		// Deselect (bind to 0) the VBO
		glBindBuffer(target, 0);

		return glBuffer;
	}

	public static int makeTextureFromTarga(String filename) throws Exception {
		// Load texture data from file
		InputStream is = loadResource(filename);
		BufferedImage image = TargaReader.getImage(is);

		// Upload the texture.
		// Take advantage of the fact that the Targa loader returns a bottom to top array!
		return makeTextureNoSwapping(image, GL_RGB8, GL_BGRA);
	}

	/** Flip an image, as required for proper OpenGL upload. */
	public static BufferedImage flipVertically(BufferedImage image) {
		// Source: http://stackoverflow.com/a/9559043/38096
		AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
		tx.translate(0, -image.getHeight(null));
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(image, null);
	}

	/**
	 * Upload a texture to the GPU.
	 * Note that the image will be swapped to conform with OpenGL conventions
	 * (see http://stackoverflow.com/questions/16875490/opengl-invert-textures-orientation-during-pixel-transfer).
	 */
	public static int makeTexture(BufferedImage image, int internalFormat, int externalFormat) {
		image = flipVertically(image);
		return makeTextureNoSwapping(image, internalFormat, externalFormat);
	}

	/**
	 * Upload a texture to the GPU.
	 * Note that the image will appear upside down if not swapped before upload or at UV coordinates level
	 * (see http://www.idevgames.com/forums/thread-1632.html or
	 * http://stackoverflow.com/questions/16875490/opengl-invert-textures-orientation-during-pixel-transfer).
	 */
	private static int makeTextureNoSwapping(BufferedImage image, int internalFormat, int externalFormat) {

		// Get image data
		ByteBuffer pixels = imageToByteBuffer(image);

		// Build and configure texture object
		int texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		// Upload data to GPU memory
		glTexImage2D(GL_TEXTURE_2D, 0, /* target, level of detail */
				internalFormat, /* internal format */
				image.getWidth(), image.getHeight(), 0, /* width, height, border */
				externalFormat, GL_UNSIGNED_BYTE, /* external format, type */
				pixels /* pixels */
		);

		return texture;
	}

	public static ByteBuffer imageToByteBuffer(BufferedImage image) {

		// get raw data (an int by pixel)
		DataBuffer buffer = image.getRaster().getDataBuffer();

		// Convert texture data to a ByteBuffer
		ByteBuffer pixels;
		if (buffer instanceof DataBufferInt) {
			int[] rawPixels = ((DataBufferInt) buffer).getData();
			pixels = BufferUtils.createByteBuffer(rawPixels.length * 4);
			pixels.asIntBuffer().put(rawPixels);

		} else {
			byte[] rawPixels = ((DataBufferByte) buffer).getData();
			pixels = BufferUtils.createByteBuffer(rawPixels.length);
			pixels.put(rawPixels);
			pixels.flip();
		}

		return pixels;
	}

	public static InputStream loadResource(String filename) throws Error, IOException {
		URL url = Tools.class.getClassLoader().getResource(filename);
		if (url == null) {
			throw new Error("Could not open file \"" + filename + "\"");
		}

		InputStream is = url.openStream();
		return is;
	}

	public static int makeShader(int type, String filename) throws Exception, Error {

		// Load and compile shader source
		String code = IOUtils.toString(loadResource(filename));
		int shader = glCreateShader(type);
		if (shader == 0) {
			throw new Error("Error creating shader type " + type);
		}
		glShaderSource(shader, code);
		glCompileShader(shader);

		// Error checking
		if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
			throw new Exception("Failed to compile shader \"" + filename + "\":\n" + glGetShaderInfoLog(shader, MAX_INFOLOG_LENGTH));
		}

		Util.checkGLError();
		return shader;
	}

	public static int makeProgram(int vertexShader, int fragmentShader) throws Exception {
		int program = glCreateProgram();
		glAttachShader(program, vertexShader);
		glAttachShader(program, fragmentShader);
		glLinkProgramChecked(program);
		return program;
	}

	public static void glLinkProgramChecked(int program) throws Exception {
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			throw new Exception("Failed to link program:\n" + glGetProgramInfoLog(program, MAX_INFOLOG_LENGTH));
		}

		glValidateProgram(program);
		if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL_FALSE) {
			throw new Exception("Invalid shader program:\n" + glGetProgramInfoLog(program, MAX_INFOLOG_LENGTH));
		}

		Util.checkGLError();
	}

	public static int glGetUniformLocationChecked(int program, String name) {
		int result = glGetUniformLocation(program, name);
		if (result == -1) {
			throw new Error("Unknown uniform \"" + name + "\"");
		}
		return result;
	}

	public static int glGetAttribLocationChecked(int program, String name) {
		int result = glGetAttribLocation(program, name);
		if (result == -1) {
			throw new Error("Unknown attribute \"" + name + "\"");
		}
		return result;
	}

	private static String f(float f) {
		return String.format(Locale.ENGLISH, "%9.5f", f);
	}

	public static String dump(Matrix4f m) {
		StringBuilder sb = new StringBuilder();
		sb.append(f(m.m00) + " " + f(m.m10) + " " + f(m.m20) + " " + f(m.m30)).append("\n");
		sb.append(f(m.m01) + " " + f(m.m11) + " " + f(m.m21) + " " + f(m.m31)).append("\n");
		sb.append(f(m.m02) + " " + f(m.m12) + " " + f(m.m22) + " " + f(m.m32)).append("\n");
		sb.append(f(m.m03) + " " + f(m.m13) + " " + f(m.m23) + " " + f(m.m33)).append("\n");
		return sb.toString();
	}

	/**
	 * Set the display mode to be used
	 * 
	 * @param width
	 *            The width of the display required
	 * @param height
	 *            The height of the display required
	 * @param fullscreen
	 *            True if we want fullscreen mode
	 */
	public static void setDisplayMode(int width, int height, boolean fullscreen) {

		// return if requested DisplayMode is already set
		if ((Display.getDisplayMode().getWidth() == width) && (Display.getDisplayMode().getHeight() == height)
				&& (Display.isFullscreen() == fullscreen)) {
			return;
		}

		try {
			DisplayMode targetDisplayMode = null;

			if (fullscreen) {
				DisplayMode[] modes = Display.getAvailableDisplayModes();
				int freq = 0;

				for (int i = 0; i < modes.length; i++) {
					DisplayMode current = modes[i];

					if ((current.getWidth() == width) && (current.getHeight() == height)) {
						if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
							if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
								targetDisplayMode = current;
								freq = targetDisplayMode.getFrequency();
							}
						}

						// if we've found a match for bpp and frequence against
						// the original display mode then it's probably best to go
						// for this one since it's most likely compatible with the monitor
						if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel())
								&& (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
							targetDisplayMode = current;
							break;
						}
					}
				}
			} else {
				targetDisplayMode = new DisplayMode(width, height);
			}

			if (targetDisplayMode == null) {
				System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
				return;
			}

			Display.setDisplayMode(targetDisplayMode);
			Display.setFullscreen(fullscreen);

		} catch (LWJGLException e) {
			System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
		}
	}

	/**
	 * @param eye
	 *            the camera position
	 * @param center
	 *            the camera target
	 * @param up
	 *            the camera up vector
	 * @return The camera matrix (in column-major convention), as specified by {@link https://www.opengl.org/wiki/GluLookAt_code}.
	 */
	public static Matrix4f gluLookAt(Vector3f eye, Vector3f center, Vector3f up) {
		Vector3f forward = Vector3f.sub(center, eye, null).normalise(null);
		Vector3f side = Vector3f.cross(forward, up, null).normalise(null);
		Vector3f.cross(side, forward, up);

		// Build an identity matrix, then fill it
		Matrix4f m = new Matrix4f();
		m.m00 = side.x;
		m.m10 = side.y;
		m.m20 = side.z;

		m.m01 = up.x;
		m.m11 = up.y;
		m.m21 = up.z;

		m.m02 = -forward.x;
		m.m12 = -forward.y;
		m.m22 = -forward.z;

		m.translate(eye.negate(null));
		return m;
	}

	/**
	 * 
	 * @param fov
	 *            FOV angle (degrees) in the y direction
	 * @param aspect
	 *            aspect ratio; beware that width / height != ((float) width) / height
	 * @param screenHeight
	 *            Screen height
	 * @param zNear
	 *            near clipping plane
	 * @param zFar
	 *            far clipping plane
	 * @return The projection matrix (in column-major convention), as specified by {@link https://www.opengl.org/wiki/GluPerspective_code}
	 */
	public static Matrix4f gluPerspective(float fov, float aspect, float zNear, float zFar) {
		float ymax = (float) (zNear * Math.tan(fov * Math.PI / 360.0));
		float xmax = ymax * aspect;
		return glhFrustumf2(-xmax, xmax, -ymax, ymax, zNear, zFar);
	}

	private static Matrix4f glhFrustumf2(float left, float right, float bottom, float top, float zNear, float zFar) {
		float temp = 2.0f * zNear;
		float temp2 = right - left;
		float temp3 = top - bottom;
		float temp4 = zFar - zNear;
		Matrix4f m = new Matrix4f();
		m.m00 = temp / temp2;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = temp / temp3;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = (right + left) / temp2;
		m.m21 = (top + bottom) / temp3;
		m.m22 = (-zFar - zNear) / temp4;
		m.m23 = -1.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = (-temp * zFar) / temp4;
		m.m33 = 0.0f;
		return m;
	}

	/**
	 * 
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param near
	 * @param far
	 *            "-far specifies the location of the far clipping plane"
	 * @return The orthographic matrix (in column-major convention), as specified by {@link http
	 *         ://www.songho.ca/opengl/gl_projectionmatrix.html}. <br/>
	 * <br/>
	 *         Axis orientation will depend on values used for borders.
	 *         In the common case where left=top=0, right=width and botton=height,
	 *         the origin will be at the top left corner of viewport, with Y axis pointing downward.
	 *         Note that the Y axis of an unprojected OpenGL viewport
	 *         points upward, as does the usual mathematical frame, so models may need to be mirorred.
	 */
	public static Matrix4f glOrtho(float left, float right, float bottom, float top, float near, float far) {
		float rml = right - left;
		float tmb = top - bottom;
		float fmn = far - near;
		Matrix4f m = new Matrix4f();
		m.m00 = 2 / rml;
		m.m01 = 0;
		m.m02 = 0;
		m.m03 = 0;

		m.m10 = 0;
		m.m11 = 2 / tmb;
		m.m12 = 0;
		m.m13 = 0;

		m.m20 = 0;
		m.m21 = 0;
		m.m22 = -2 / fmn;
		m.m23 = 0;

		m.m30 = -(right + left) / rml; // Note the initial minus sign; some specs lost it!
		m.m31 = -(top + bottom) / tmb;
		m.m32 = -(far + near) / fmn;
		m.m33 = 1;
		return m;
	}

	public static String[] getAvailableExtensions() {
		String[] result;
		if (GLContext.getCapabilities().OpenGL30) {
			int numExtensions = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
			result = new String[numExtensions];
			for (int i = 0; i < numExtensions; i++) {
				result[i] = GL30.glGetStringi(GL11.GL_EXTENSIONS, i);
			}
		} else {
			String extensionsAsString = GL11.glGetString(GL11.GL_EXTENSIONS);
			result = extensionsAsString.split(" ");
		}
		return result;
	}

	public static double getTimeSeconds() {
		// return (Sys.getTime() * 1000.0) / Sys.getTimerResolution();
		return ((double) System.nanoTime()) / 1E9;
	}

	/** Return a scaled copy of this vector */
	public static Vector3f scale(Vector3f v, float scale) {
		Vector3f result = new Vector3f(v);
		result.scale(scale);
		return result;
	}

	/** Return the OpenGL format (as expected by glTexImage2D) equivalent to this BufferedImage format */
	public static int getGLFormat(BufferedImage img) {
		switch (img.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR:
			return GL_BGR;
		case BufferedImage.TYPE_4BYTE_ABGR:
			return EXTAbgr.GL_ABGR_EXT;
		default:
			throw new Error("Unhandled image format: " + img.getType());
		}
	}

	/**
	 * Ensure that currently bound FBO is complete (ie, that all required render buffers are bound).
	 * If not, an Error will be thrown.
	 */
	public static void validateCurrentFrameBuffer() {
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			throw new Error("glCheckFramebufferStatus: error " + status);
		}
	}

	/** Time elapsed, from an arbitrary start point, in ms */
	public static float getTime_ms() {
		return (Sys.getTime() * 1000) / (float) Sys.getTimerResolution();
	}

	/** Return the relative path of this class. Useful in conjonction with loadResource and derived (makeShader, etc.). */
	public static String getRelativePath(Class<?> clz) {
		return clz.getPackage().getName().replace(".", "/") + "/";
	}

	/** Return the relative path of the class of this object. Useful in conjonction with loadResource and derived (makeShader, etc.). */
	public static String getRelativePath(Object obj) {
		return getRelativePath(obj.getClass());
	}
}
