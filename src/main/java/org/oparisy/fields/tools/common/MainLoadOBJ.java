package org.oparisy.fields.tools.common;

import java.io.FileReader;

/** Testing purpose */
public class MainLoadOBJ {

	public static void main(String[] args) {
		try {
			for (String filepath : args) {
				new OBJLoader() {

					protected void onMaterialDeclaration(String filename) {
					}

					protected void onMaterialUse(String materialName) {
					}

					protected void onGroupName(String name) {
					}

					protected void onVertex(double x, double y, double z) {
					}

					protected void onNormal(double x, double y, double z) {
					}

					protected void onTextureCoords(double u, double v) {
					}

					protected void onTextureCoords(double u, double v, double w) {
					}

					protected void onFace(FaceComponent[] vertices) {
					}

					protected void onSmoothShading(String arg) {
					}

					protected void onObject(String name) {
					}
				}.loadObj(new FileReader(filepath));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
