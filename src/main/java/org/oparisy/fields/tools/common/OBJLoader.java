package org.oparisy.fields.tools.common;

import static java.lang.Double.parseDouble;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;

public abstract class OBJLoader {

	/** Note that indexes are 1-based */
	public static class FaceComponent {

		private int vertex;
		private Integer textureCoord;
		private Integer normal;

		public FaceComponent(int vertex, Integer textureCoord, Integer normal) {
			this.vertex = vertex;
			this.textureCoord = textureCoord;
			this.normal = normal;
		}

		public int getVertex() {
			return vertex;
		}

		public Integer getTextureCoord() {
			return textureCoord;
		}

		public Integer getNormal() {
			return normal;
		}
	}

	// Prefixes
	private static final String MTLLIB = "mtllib ";
	private static final String USEMTL = "usemtl ";
	private static final String G = "g";
	private static final String V = "v ";
	private static final String S = "s ";
	private static final String VN = "vn ";
	private static final String VT = "vt ";
	private static final String O = "o ";

	public void loadObj(InputStream is) throws Exception {
		loadObj(new InputStreamReader(is));
	}

	public void loadObj(Reader reader) throws Exception {
		BufferedReader br = new BufferedReader(reader);
		try {
			for (String line; (line = br.readLine()) != null;) {
				process(line);
			}
		} finally {
			// Java 1.6 compliant
			br.close();
		}
	}

	private void process(String line) {
		if (line.isEmpty() || line.startsWith("#")) {
			// Ignore comments and empty lines
		}

		else if (line.startsWith(MTLLIB)) {
			onMaterialDeclaration(StringUtils.removeStart(line, MTLLIB).trim());
		}

		else if (line.startsWith(USEMTL)) {
			onMaterialUse(StringUtils.removeStart(line, USEMTL).trim());
		}

		else if (line.startsWith(G)) {
			onGroupName(StringUtils.removeStart(line, G).trim());
		}

		else if (line.startsWith(V)) {
			String args = StringUtils.removeStart(line, V).trim();
			String[] comp = StringUtils.split(args, ' ');
			if (comp.length != 3) {
				throw new Error("Unhandled");
			}
			onVertex(parseDouble(comp[0]), parseDouble(comp[1]),
					parseDouble(comp[2]));
		}

		else if (line.startsWith(VN)) {
			String args = StringUtils.removeStart(line, VN).trim();
			String[] comp = StringUtils.split(args, ' ');
			if (comp.length != 3) {
				throw new Error("Unhandled");
			}
			onNormal(parseDouble(comp[0]), parseDouble(comp[1]),
					parseDouble(comp[2]));
		}

		else if (line.startsWith(VT)) {
			String args = StringUtils.removeStart(line, VT).trim();
			String[] comp = StringUtils.split(args, ' ');
			if (comp.length == 2) {
				onTextureCoords(parseDouble(comp[0]), parseDouble(comp[1]));
			} else if (comp.length == 3) {
				onTextureCoords(parseDouble(comp[0]), parseDouble(comp[1]),
						parseDouble(comp[2]));
			} else {
				throw new Error("Unhandled");
			}
		}

		else if (line.startsWith("f ")) {
			String args = StringUtils.removeStart(line, "f ").trim();
			String[] comps = StringUtils.split(args, ' ');

			int idx = 0;
			FaceComponent[] vertices = new FaceComponent[comps.length];
			for (String comp : comps) {
				String[] arr = StringUtils.splitPreserveAllTokens(comp, '/');
				FaceComponent cpnt;
				switch (arr.length) {
				case 1:
					// Vertex
					cpnt = new FaceComponent(Integer.parseInt(arr[0]), null,
							null);
					break;
				case 2:
					// Vertex/texture-coordinate
					cpnt = new FaceComponent(Integer.parseInt(arr[0]),
							Integer.parseInt(arr[1]), null);
					break;
				case 3:
					// Vertex/texture-coordinate/normal (texture can be empty)
					Integer textureCoord = StringUtils.isEmpty(arr[1]) ? null
							: Integer.parseInt(arr[1]);
					cpnt = new FaceComponent(Integer.parseInt(arr[0]),
							textureCoord, Integer.parseInt(arr[2]));
					break;
				default:
					throw new Error("Unhandled number of components ("
							+ arr.length + ")");
				}
				vertices[idx++] = cpnt;

			}

			onFace(vertices);
		}

		else if (line.startsWith(S)) {
			onSmoothShading(StringUtils.removeStart(line, S).trim());
		}
		
		else if (line.startsWith(O)) {
			onObject(StringUtils.removeStart(line, O).trim());
		}

		else {
			throw new Error("Unhandled line format: \"" + line + "\"");
		}
	}

	protected abstract void onMaterialDeclaration(String filename);

	protected abstract void onMaterialUse(String materialName);

	/** "name" can be empty */
	protected abstract void onGroupName(String name);

	protected abstract void onVertex(double x, double y, double z);

	protected abstract void onNormal(double x, double y, double z);

	protected abstract void onTextureCoords(double u, double v);

	protected abstract void onTextureCoords(double u, double v, double w);

	protected abstract void onFace(FaceComponent[] vertices);

	protected abstract void onSmoothShading(String arg);
	
	protected abstract void onObject(String name);
}
