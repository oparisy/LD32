package org.oparisy.fields;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.tools.common.Tools;

/** Wrap the OpenGL shading program */
public class ShadingProgram {

	private static final String vertexShaderFile = "fields/StandardShading.vertexshader";
	private static final String fragmentShaderFile = "fields/StandardShading.fragmentshader";

	private int program;

	/** Shader uniforms */
	private int mvpID;
	private int vID;
	private int mID;
	private int lightPosID;
	private int samplerID;

	/** Vertex attributes */
	private int posID;
	private int uvID;
	private int normalID;

	public ShadingProgram() throws Exception, Error {
		int vs = Tools.makeShader(GL_VERTEX_SHADER, vertexShaderFile);
		int fs = Tools.makeShader(GL_FRAGMENT_SHADER, fragmentShaderFile);
		program = Tools.makeProgram(vs, fs);
		glDeleteShader(vs);
		glDeleteBuffers(fs);
		getAttribsAndUniforms();
	}

	public void bindMeshBuffers(OpenGLMesh openGLMesh) {
		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getVertexBuffer());
		glVertexAttribPointer(posID, 3, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getUvBuffer());
		glVertexAttribPointer(uvID, 2, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getNormalBuffer());
		glVertexAttribPointer(normalID, 3, GL_FLOAT, false, 0, 0);
	}

	private void getAttribsAndUniforms() {
		posID = Tools.glGetAttribLocationChecked(program, "vertexPosition_modelspace");
		uvID = Tools.glGetAttribLocationChecked(program, "vertexUV");
		normalID = Tools.glGetAttribLocationChecked(program, "vertexNormal_modelspace");

		mvpID = Tools.glGetUniformLocationChecked(program, "MVP");
		mID = Tools.glGetUniformLocationChecked(program, "M");
		vID = Tools.glGetUniformLocationChecked(program, "V");
		lightPosID = Tools.glGetUniformLocationChecked(program, "LightPosition_worldspace");
		samplerID = Tools.glGetUniformLocationChecked(program, "myTextureSampler");
	}

	public void enableVertexAttribArrays() {
		glEnableVertexAttribArray(posID);
		glEnableVertexAttribArray(uvID);
		glEnableVertexAttribArray(normalID);
	}

	public void disableVertexAttribArrays() {
		glDisableVertexAttribArray(posID);
		glDisableVertexAttribArray(uvID);
		glDisableVertexAttribArray(normalID);
	}

	public void use() {
		glUseProgram(program);
	}

	public void setViewMatrix(Matrix4f viewMatrix) {
		glUniformMatrix4(vID, false, Tools.buildFloatBuffer(viewMatrix));
	}

	public void setLightPos(Vector3f lightPos) {
		glUniform3f(lightPosID, lightPos.x, lightPos.y, lightPos.z);
	}

	/** Bind texture to texture unit 0 */
	public void bindTexture(int texture) {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glUniform1i(samplerID, 0);
	}

	public void updateMVPMatrix(Matrix4f vpMatrix, Matrix4f modelMatrix) {
		Matrix4f mvpMatrix = Matrix4f.mul(vpMatrix, modelMatrix, null);
		glUniformMatrix4(mvpID, false, Tools.buildFloatBuffer(mvpMatrix));
		glUniformMatrix4(mID, false, Tools.buildFloatBuffer(modelMatrix));
	}
}
