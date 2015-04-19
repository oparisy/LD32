package org.oparisy.fields.physics;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

public class Wall extends GameEntity {
	private float w, h;

	public Wall(float x, float y, float w, float h, World world, String name) {
		super(name);
		this.w = w;
		this.h = h;
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(w, h);
		setPhysicalState(new PhysicalState(x, y, world, BodyType.STATIC, shape, this));
	}

	public float getW() {
		return w;
	}

	public float getH() {
		return h;
	}
}
