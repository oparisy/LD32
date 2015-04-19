package org.oparisy.fields.physics;

public class AliveEntity extends GameEntity {
	
	private float health;

	public AliveEntity(String name) {
		super(name);
		this.health = 100;
	}

	public float getHealth() {
		return health;
	}

	public void setHealth(float health) {
		this.health = health;
	}
}
