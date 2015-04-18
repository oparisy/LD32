package org.oparisy.fields;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

public class Wall {
	private PhysicalState state;
	private float w, h;

	public Wall(float x, float y, float w, float h, World world) {
		this.w = w;
		this.h = h;
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(w, h);
		state = new PhysicalState(x, y, world, BodyType.STATIC, shape);
	}

	public PhysicalState getState() {
		return state;
	}

	public float getW() {
		return w;
	}

	public float getH() {
		return h;
	}
}
