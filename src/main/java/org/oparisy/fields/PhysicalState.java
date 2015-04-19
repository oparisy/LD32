package org.oparisy.fields;

import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

/** A physical object has a 2D position and velocity */
// Good explanations here: http://thisiswhatiknowabout.blogspot.fr/2011/12/jbox2d-tutorial-creating-object-body.html
public class PhysicalState {

	private BodyDef bd;
	private FixtureDef fd;
	private Body body;
	private GameEntity entity;

	public PhysicalState(float x, float y, World world, BodyType type, Shape shape, GameEntity entity) {

		this.entity = entity;

		bd = new BodyDef();
		bd.position.set(x, y);
		bd.type = type;

		fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 0.5f;
		fd.friction = 0.3f;
		fd.restitution = 0.5f;

		body = world.createBody(bd);
		Fixture fixture = body.createFixture(fd);

		// Used to navigate back to this instance when processing collisions
		body.setUserData(this);
		fixture.setUserData(this);

		// Avoid "floating" boxes
		body.setLinearDamping(0.1f);
	}

	public Vec2 getPosition() {
		return body.getPosition();
	}

	public float getAngle() {
		return body.getAngle();
	}

	// Only for STATIC objects (player...)
	public void addToPos(Vec2 delta) {
		body.setTransform(body.getPosition().add(delta), body.getAngle());
	}

	public void applyForce(Vec2 force, Vec2 point) {
		body.applyForce(force, point);
	}
	
	public GameEntity getEntity() {
		return entity;
	}
	
	public Vec2 getLinarVelocity() {
		return body.getLinearVelocity();
	}
}
