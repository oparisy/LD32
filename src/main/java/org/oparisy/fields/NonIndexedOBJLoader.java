package org.oparisy.fields;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.oparisy.fields.tools.common.OBJLoader;

public class NonIndexedOBJLoader extends OBJLoader {

	/** The vertices, as found in the OBJ file */
	private List<Float> vertices = new ArrayList<Float>();

	/** The texture coordinates, as found in the OBJ file */
	private List<Float> uvs = new ArrayList<Float>();
	
	/** The normals, as found in the OBJ file */
	private List<Float> normals = new ArrayList<Float>();

	/** The vertices (3*3 floats by triangle, duplicated if necessary) */
	private List<Float> triVertices = new ArrayList<Float>();

	/** The texture coordinates (3*2 by triangle, duplicated if necessary) */
	private List<Float> triUvs = new ArrayList<Float>();
	
	/** The normals (3*3 floats by triangle, duplicated if necessary) */
	private List<Float> triNormals = new ArrayList<Float>();

	/** the number of faces */
	private int faceCount = 0;

	/** "name" can be empty */
	protected void onGroupName(String name) {
		throw new Error("Unhandled");
	}

	protected void onVertex(double x, double y, double z) {
		vertices.add((float) x);
		vertices.add((float) y);
		vertices.add((float) z);
	}

	protected void onTextureCoords(double u, double v) {
		uvs.add((float) u);
		uvs.add((float) v);
	}
	

	protected void onNormal(double x, double y, double z) {
		normals.add((float) x);
		normals.add((float) y);
		normals.add((float) z);
	}

	protected void onFace(FaceComponent[] components) {
		if (components.length != 3) {
			throw new Error("Unhandled");
		}

		faceCount ++;
		for (FaceComponent fc : components) {
			triVertices.add(vertices.get(3 * (fc.getVertex() - 1) + 0));
			triVertices.add(vertices.get(3 * (fc.getVertex() - 1) + 1));
			triVertices.add(vertices.get(3 * (fc.getVertex() - 1) + 2));

			triUvs.add(uvs.get(2 * (fc.getTextureCoord() - 1) + 0));
			triUvs.add(uvs.get(2 * (fc.getTextureCoord() - 1) + 1));
			
			triNormals.add(normals.get(3 * (fc.getNormal() - 1) + 0));
			triNormals.add(normals.get(3 * (fc.getNormal() - 1) + 1));
			triNormals.add(normals.get(3 * (fc.getNormal() - 1) + 2));
		}
	}

	public int getFaceCount() {
		return faceCount;
	}

	public float[] getVertexData() {
		return ArrayUtils.toPrimitive(triVertices.toArray(new Float[triVertices.size()]));
	}

	public float[] getUVData() {
		return ArrayUtils.toPrimitive(triUvs.toArray(new Float[triUvs.size()]));
	}

	public float[] getNormalData() {
		return ArrayUtils.toPrimitive(triNormals.toArray(new Float[triNormals.size()]));
	}

	protected void onMaterialDeclaration(String filename) {
		// Ignored
	}

	protected void onMaterialUse(String materialName) {
		// Ignored
	}

	protected void onSmoothShading(String arg) {
		// Ignored
	}

	protected void onTextureCoords(double u, double v, double w) {
		throw new Error("Unhandled");
	}

	protected void onObject(String name) {
		// Ignored
	}
}
