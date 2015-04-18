package org.oparisy.fields;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

public class Enemy {
	private PhysicalState state;

	public Enemy(float x, float y, World world) {
		CircleShape cs = new CircleShape();
		cs.m_radius = 1f;
		state = new PhysicalState(x, y, world, BodyType.STATIC, cs);
	}

	public PhysicalState getState() {
		return state;
	}
}
