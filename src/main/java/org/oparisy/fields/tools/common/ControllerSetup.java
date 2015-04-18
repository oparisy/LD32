package org.oparisy.fields.tools.common;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

/**
 * A wrapper detecting and setting up an LWJGL Controller.
 * Adapted from http://pastebin.java-gaming.org/01ecb5e001c
 */
public class ControllerSetup {

	private final static float DEFAULT_DEAD_ZONE = .75f;

	private static Controller controller;

	private static float[] initialAxisValues;

	private static boolean initialized = false;

	public static Controller getController() {
		if (!initialized) {
			initController();
			initialized = true;
		}

		// if (!isControllerInitialised()) {
		// return null;
		// }
		return controller;
	}

	private static void initController() {
		try {
			Controllers.create();

			for (int i = 0; i < Controllers.getControllerCount(); i++) {
				Controller c = Controllers.getController(i);
				System.out.println("Checking controller: " + c.getName());

				if (c.getAxisCount() >= 4 && c.getButtonCount() >= 4) {
					// This'll do - two analogue sticks and 4 buttons should be enough for anyone
					controller = c;

					// Set dead zones
					for (int j = 0; j < 4; j++) {
						c.setDeadZone(j, DEFAULT_DEAD_ZONE);
					}

					initialAxisValues = new float[4];
					for (int j = 0; j < 4; j++) {
						initialAxisValues[j] = c.getAxisValue(j);
					}

					return;
				}
			}

		} catch (Exception e) {
			System.err.println("No gamepads or joysticks enabled due to " + e);
		}
	}

	/** If false but getController did not return null, controller is not ready; do not use its value */
	public static boolean isControllerInitialised() {
		if (controller == null) {
			return false;
		}

		if (initialAxisValues == null) {
			return true;
		}

		for (int i = 0; i < 4; i++) {
			if (controller.getAxisValue(i) == initialAxisValues[i] && initialAxisValues[i] != 0.0f) {
				return false;
			}
		}
		initialAxisValues = null;
		return true;
	}
}
