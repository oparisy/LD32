package org.oparisy.fields.render;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.io.InputStream;

import org.oparisy.fields.tools.common.NonIndexedOBJLoader;
import org.oparisy.fields.tools.common.Tools;

/** A mesh, as uploaded to the GPU */
public class OpenGLMesh {
	
	private int triangleCount;
	private int vertexBuffer;
	private int uvBuffer;
	private int normalBuffer;
	private NonIndexedOBJLoader loader;

	public OpenGLMesh(InputStream obj) throws Exception {
		loader = new NonIndexedOBJLoader();
		loader.loadObj(obj);
		triangleCount = loader.getFaceCount();
	}
	
	public void uploadToGPU() {
		vertexBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(loader.getVertexData()), GL_STATIC_DRAW);

		uvBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(loader.getUVData()), GL_STATIC_DRAW);

		normalBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, normalBuffer);
		glBufferData(GL_ARRAY_BUFFER, Tools.buildFloatBuffer(loader.getNormalData()), GL_STATIC_DRAW);
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	public int getVertexBuffer() {
		return vertexBuffer;
	}

	public int getUvBuffer() {
		return uvBuffer;
	}

	public int getNormalBuffer() {
		return normalBuffer;
	}
}
