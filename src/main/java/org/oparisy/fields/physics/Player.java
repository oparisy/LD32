package org.oparisy.fields.physics;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

public class Player extends AliveEntity {
	
	public Player(float x, float y, World world, String name) {
		super(name);
		CircleShape cs = new CircleShape();
		cs.m_radius = 0.5f;
		setPhysicalState(new PhysicalState(x, y, world, BodyType.STATIC, cs, this));
	}
}
