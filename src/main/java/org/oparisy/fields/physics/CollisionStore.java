package org.oparisy.fields.physics;

import java.util.HashMap;
import java.util.Map;

/** This class is reponsible for "remembering" recent collisions (for sound playing purpose) */
public class CollisionStore {

	// 0.2s
	private static final long MAX_TIME = 200;

	Map<String, Long> lastCollisions = new HashMap<String, Long>();

	/** All objetcs must have unique names for this to work */
	public boolean recentlyCollided(GameEntity obj1, GameEntity obj2) {
		long currentTime = System.currentTimeMillis();

		// Order by name since collision pairs are in arbitrary order
		String key = obj1.getName().compareTo(obj2.getName()) < 0 ? (obj1.getName() + obj2.getName()) : (obj2.getName() + obj1.getName());

		Long lastCollision = lastCollisions.get(key);
		if (lastCollision == null || lastCollision + MAX_TIME < currentTime) {
			// No recent collision
			lastCollisions.put(key, currentTime);
			return false;
		} else {
			return true;
		}
	}
}
