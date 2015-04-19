package org.oparisy.fields;

/** Represent an element of the game (player, wall...) */
public abstract class GameEntity {
	private String name;
	private PhysicalState physicalState;
	
	public GameEntity(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	/** Only do this in constructor */
	protected void setPhysicalState(PhysicalState state) {
		this.physicalState = state;
	}
	
	public PhysicalState getState() {
		return physicalState;
	}
}
