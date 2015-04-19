package org.oparisy.fields.render;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.tools.common.Tools;

/**
 * How to use this class:
 * <ol>
 * <li>setDeltaTime
 * <li>updateDirection
 * <li>onKeyXXX
 * <li>getMatrix
 */
public class Camera {

	private Vector3f position = new Vector3f(0, 0, 20);

	// Horizontal angle (initial value: toward -Z)
	private float horizontalAngle = 3.14f;

	// vertical angle : (initial value: 0, look at the horizon)
	private float verticalAngle = 0.0f;

	private float speed = 3.0f; // 3 units / second
	private float mouseSpeed = 5E-7f;

	private double deltaTime;

	private Vector3f direction;

	private Vector3f right;

	/** Set elapsed time (in seconds) */
	public void setDeltaTime(double elapsedSec) {
		this.deltaTime = elapsedSec;
	}

	public void updateDirection(int dx, int dy, int width, int height) {

		// Compute new orientation
		horizontalAngle += mouseSpeed * dx / deltaTime;
		verticalAngle += mouseSpeed * dy / deltaTime;

		direction = new Vector3f();
		direction.x = (float) (Math.cos(verticalAngle) * Math.sin(horizontalAngle));
		direction.y = (float) Math.sin(verticalAngle);
		direction.z = (float) (Math.cos(verticalAngle) * Math.cos(horizontalAngle));

		right = new Vector3f();
		right.x = (float) Math.sin(horizontalAngle - 3.14f / 2.0f);
		right.y = 0;
		right.z = (float) Math.cos(horizontalAngle - 3.14f / 2.0f);
	}

	public void onKeyUp() {
		Vector3f.add(position, Tools.scale(direction, (float) (deltaTime * speed)), position);
	}

	public void onKeyDown() {
		Vector3f.sub(position, Tools.scale(direction, (float) (deltaTime * speed)), position);
	}

	public void onKeyLeft() {
		Vector3f.sub(position, Tools.scale(right, (float) (deltaTime * speed)), position);
	}

	public void onKeyRight() {
		Vector3f.add(position, Tools.scale(right, (float) (deltaTime * speed)), position);
	}

	public Matrix4f getMatrix() {
		Vector3f target = Vector3f.add(position, direction, null);
		Vector3f up = Vector3f.cross(right, direction, null);
		return Tools.gluLookAt(position, target, up);
	}
}
