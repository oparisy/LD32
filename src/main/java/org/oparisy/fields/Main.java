package org.oparisy.fields;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.tools.audio.SoundManager;
import org.oparisy.fields.tools.common.ControllerSetup;
import org.oparisy.fields.tools.common.Tools;

public class Main {

	private static final int BOX_NB = 2;

	private static final int WALL_BOTTOM = 5;

	private static final int WALL_TOP = -5;

	private static final int WALL_RIGHT = 9;

	private static final int WALL_LEFT = -9;

	private static final float WALL_WIDTH = 0.5f;

	/* Beyond this distance, player does not exerce a force */
	private static final double MAX_PLAYER_INFLUENCE = 10;

	private Player player;
	private List<Box> physicalBox = new ArrayList<Box>();

	private Controller controller;

	// Projection fixed parameters
	private static final float FOV = 45.0f;
	private static final float ZNEAR = 0.1f;
	private static final float ZFAR = 100f;

	private static final int SCREEN_HEIGHT = 768;
	private static final int SCREEN_WIDTH = 1024;

	// Resources (shader sources) location
	private static final String RES = "fields"; // Main.class.getPackage().getName().replace(".", "/");

	private int texture;

	private OpenGLMesh playerMesh;
	private Matrix4f playerMatrix;

	private OpenGLMesh boxMesh;
	private List<Matrix4f> boxesMatrix = new ArrayList<Matrix4f>();

	private Camera camera = new Camera();
	private Vector3f lightPos = new Vector3f(4, 4, 4);

	private Matrix4f viewMatrix;

	private double lastTime;
	private Matrix4f vpMatrix;

	private World world;

	private List<Wall> walls = new ArrayList<Wall>();
	private List<Matrix4f> wallsMatrix = new ArrayList<Matrix4f>();

	private OpenGLMesh enemyMesh;
	private List<Enemy> enemies = new ArrayList<Enemy>();
	private List<Matrix4f> enemiesMatrix = new ArrayList<Matrix4f>();

	private ShadingProgram shadingProgram;

	private org.oparisy.fields.tools.audio.SoundManager soundManager;

	private int enemyKill;

	private int gameOver;

	private int highOuch;

	private int ouch;

	private int weeUup;

	private int collision;

	public int uploadTexture(BufferedImage img) {
		img = Tools.flipVertically(img);
		ByteBuffer pixels = Tools.imageToByteBuffer(img);

		// Create one OpenGL texture
		int textureID = glGenTextures();

		// "Bind" the newly created texture: it will be modified by all future texture functions
		glBindTexture(GL_TEXTURE_2D, textureID);

		// Upload the image to OpenGL
		int format = Tools.getGLFormat(img);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, img.getWidth(), img.getHeight(), 0, format, GL_UNSIGNED_BYTE, pixels);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

		return textureID;
	}

	private void run() throws Exception {
		setupSound();
		setupRender();

		setupPhysics();
		setupPhysicalEntities();

		detectController();

		mainLoop();

		Display.destroy();
		soundManager.destroy();
	}

	private void setupSound() {
		soundManager = new SoundManager();
		soundManager.initialize(16);

		// load our sound data
		collision = soundManager.addSound("fields/collision.wav");
		enemyKill = soundManager.addSound("fields/enemyKill.wav");
		gameOver = soundManager.addSound("fields/gameOver.wav");
		highOuch = soundManager.addSound("fields/highOuch.wav");
		ouch = soundManager.addSound("fields/ouch.wav");
		weeUup = soundManager.addSound("fields/weeUup.wav");
	}

	private void detectController() {
		controller = ControllerSetup.getController();
		if (controller == null) {
			System.out.println("No controller detected");
		} else {
			System.out.println("Controller detected: " + controller.getName());
		}
	}

	private void setupRender() throws LWJGLException, IOException, Error, Exception {
		DisplayMode mode = new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT);
		Display.setDisplayMode(mode);
		Display.setTitle(this.getClass().getSimpleName());
		PixelFormat pixelFormat = new PixelFormat().withSamples(4); // 4x antialiasing
		Display.create(pixelFormat);

		System.out.println("GL version: " + glGetString(GL_VERSION));

		// Load resources
		BufferedImage img = ImageIO.read(Tools.loadResource(RES + "/uvmap.png"));

		playerMesh = new OpenGLMesh(Tools.loadResource(RES + "/suzanne.obj"));
		boxMesh = new OpenGLMesh(Tools.loadResource(RES + "/boxuv.obj"));
		enemyMesh = new OpenGLMesh(Tools.loadResource(RES + "/enemy.obj"));

		shadingProgram = new ShadingProgram();

		// Required in OpenGL 3
		int vao = glGenVertexArrays();
		glBindVertexArray(vao);

		// Upload data to GPU
		playerMesh.uploadToGPU();
		boxMesh.uploadToGPU();
		enemyMesh.uploadToGPU();
		texture = uploadTexture(img);

		// Global OpenGL setup
		glClearColor(0.0f, 0.0f, 0.4f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		// glEnable(GL_CULL_FACE);
	}

	public void mainLoop() {

		Mouse.setGrabbed(true);

		this.lastTime = Tools.getTimeSeconds();

		int fps = 0;
		double elapsedSinceLastFPS = 0;

		boolean quit = false;
		while (!Display.isCloseRequested() && !quit) {

			// Get environment informations
			double sec = Tools.getTimeSeconds();
			double elapsedSec = sec - lastTime;
			this.lastTime = sec;

			int dx = -Mouse.getDX();
			int dy = Mouse.getDY();

			// Compute new viewpoint
			camera.setDeltaTime(elapsedSec);
			camera.updateDirection(dx, dy, SCREEN_WIDTH, SCREEN_HEIGHT);

			// Must be called after camera.updateDirection
			if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
				camera.onKeyUp();
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
				camera.onKeyDown();
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
				camera.onKeyLeft();
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
				camera.onKeyRight();
			}

			quit = Keyboard.isKeyDown(Keyboard.KEY_Q) || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);

			if (controller != null && ControllerSetup.isControllerInitialised()) {

				// StringBuilder sb = new StringBuilder();
				// for (int i=0; i<controller.getAxisCount(); i++) {
				// sb.append(controller.getAxisName(i) + ": " + controller.getAxisValue(i) + " ");
				// }
				// System.out.println(sb.toString());

				// XBox 360 mappings?
				float x = controller.getAxisValue(1);
				float y = -controller.getAxisValue(0);
				// System.out.println("x: " + x + " y: " + y);

				// Detect collisions with borders (walls)
				// TODO Take player size into account
				Vec2 playerDeltaPos = new Vec2(x / 20f, y / 20f);
				Vec2 newPos = player.getState().getPosition().add(playerDeltaPos);
				if (newPos.x < WALL_LEFT + WALL_WIDTH || newPos.x > WALL_RIGHT - WALL_WIDTH) {
					playerDeltaPos.x = 0;
				}
				if (newPos.y < WALL_TOP + WALL_WIDTH || newPos.y > WALL_BOTTOM - WALL_WIDTH) {
					playerDeltaPos.y = 0;
				}

				player.getState().addToPos(playerDeltaPos);
				// physicalPlayer.setX(physicalPlayer.getX() + x / 20f);
				// physicalPlayer.setX(physicalPlayer.getX() + y / 20f);
				//
				// System.out.println("Position: " + physicalPlayer.getPosx() + " " + physicalPlayer.getPosy());
				// }

				if (controller.isButtonPressed(0) || controller.isButtonPressed(1)) {
					for (Box box : this.physicalBox) {
						PhysicalState physicalBox = box.getState();

						// Compute vector from box to player
						Vec2 delta = physicalBox.getPosition().sub(player.getState().getPosition());

						// Compute an attraction force
						float coef = 0.3f;
						float len = (float) delta.length();
						if (len > MAX_PLAYER_INFLUENCE) {
							coef /= 0.03f;
						}

						if (len > 0.02f) {
							// Divided by len => unit vector, then by len => inverse of distance
							Vec2 force = delta.negate().mul(coef / (len * len));
							// float newPosX = physicalBox.getX() - vecx * coef / (len * len);
							// float newPosY = physicalBox.getY() - vecy * coef / (len * len);
							//
							// boolean isNearBox = false;
							// // for (PhysicalState otherBox : this.physicalBox) {
							// // float dist = distance(newPosX, newPosY, otherBox.getX(), otherBox.getY());
							// // if (dist < 1f) {
							// // isNearBox = true;
							// // otherBox.addToPos((newPosX-otherBox.getX())/(2*dist), (newPosY-otherBox.getY())/(2*dist));
							// // break;
							// // }
							// // }
							//
							// if (!isNearBox) {
							// physicalBox.setX(newPosX);
							// physicalBox.setY(newPosY);
							// }

							if (controller.isButtonPressed(1)) {
								// Repulsive force
								force = force.negate();
							}

							physicalBox.applyForce(force, physicalBox.getPosition());
						}

						// //physicalBox.getBody().addForce(new DVector3(vecx*0.1, vecy*0.1, vecz*0.1));
						// physicalBox.getBody().addForce(1,0,0);
					}
				}
			}

			runOneSimulationStep();

			runIA();

			updatePlayerMatrix();

			updateBoxesMatrix();

			// Could be done out of the render loop since walls do not move
			updateWallsMatrix();

			updateEnemyMatrix();

			updateVPMatrix();

			render();
			Display.update();

			// Update FPS
			fps++;
			elapsedSinceLastFPS += elapsedSec;
			if (elapsedSinceLastFPS > 1) {
				elapsedSinceLastFPS -= 1;
				Display.setTitle(this.getClass().getSimpleName() + " - " + fps + " FPS");
				fps = 0;
			}
		}
	}

	private void updateWallsMatrix() {
		wallsMatrix.clear();
		for (Wall wall : walls) {
			Matrix4f modelMatrix = new Matrix4f();
			PhysicalState ps = wall.getState();
			modelMatrix.translate(new Vector3f(ps.getPosition().x, ps.getPosition().y, 0));
			modelMatrix.scale(new Vector3f(wall.getW(), wall.getH(), Math.min(wall.getW(), wall.getH())));
			wallsMatrix.add(modelMatrix);
		}
	}

	private void runIA() {
		// Compute a force to "steer" enemy to player
		for (Enemy enemy : enemies) {

			// System.out.println("Player position: " + player.getState().getPosition());
			// System.out.println("Enemy position: " + enemy.getState().getPosition());

			Vec2 dir = player.getState().getPosition().sub(enemy.getState().getPosition());
			dir.normalize();

			// System.out.println("Enemy => player direction: " + dir);

			Vec2 f;

			Vec2 enemyDir = enemy.getState().getLinarVelocity();
			if (enemyDir.length() < 1e-2) {
				// Enemy is too slow; just apply a force to player
				f = dir.mul(0.005f);
			} else {
				// Take enemy speed into account

				enemyDir.normalize();

				// System.out.println("Enemy direction: " + enemyDir);

				// Will go from 2 (opposed direction) to 0 (same direction)
				float error = 1 - Vec2.dot(dir, enemyDir);

				// System.out.println("error: " + error);

				f = dir.mul(error * -0.005f);
			}

			// System.out.println("Applied force: " + f);
			enemy.getState().applyForce(dir, enemy.getState().getPosition());
		}

		/*
		 * // Move enemies according to player position
		 * for (Enemy enemy : enemies) {
		 * Vec2 dir = player.getState().getPosition().sub(enemy.getState().getPosition());
		 * dir.normalize();
		 * dir = dir.mul(0.005f); // Enemy speed
		 * enemy.getState().addToPos(dir);
		 * }
		 */
	}

	private void updateEnemyMatrix() {
		enemiesMatrix.clear();
		for (Enemy enemy : this.enemies) {
			Matrix4f modelMatrix = new Matrix4f();
			modelMatrix.translate(new Vector3f(enemy.getState().getPosition().x, enemy.getState().getPosition().y, 0));
			modelMatrix.scale(new Vector3f(1f, 1f, 1f));

			enemiesMatrix.add(modelMatrix);
		}
	}

	private void updateBoxesMatrix() {
		boxesMatrix.clear();
		// Get position and orientation from simulated object
		for (Box box : this.physicalBox) {
			PhysicalState physicalBox = box.getState();
			Matrix4f boxMatrix = new Matrix4f();
			boxMatrix.translate(new Vector3f(physicalBox.getPosition().x, physicalBox.getPosition().y, 0));
			boxMatrix.scale(new Vector3f(0.25f, 0.25f, 0.25f));

			boxesMatrix.add(boxMatrix);
		}
	}

	private void updatePlayerMatrix() {
		playerMatrix = new Matrix4f();
		playerMatrix.translate(new Vector3f(player.getState().getPosition().x, player.getState().getPosition().y, 0));
		playerMatrix.scale(new Vector3f(0.5f, 0.5f, 0.5f));
	}

	private void updateVPMatrix() {
		// Camera and view parameters
		// TODO Can be computed once per resize
		float aspect = (float) SCREEN_WIDTH / SCREEN_HEIGHT; // Note the cast!
		Matrix4f projectionMatrix = Tools.gluPerspective(FOV, aspect, ZNEAR, ZFAR);

		this.viewMatrix = camera.getMatrix();

		this.vpMatrix = Matrix4f.mul(projectionMatrix, viewMatrix, null);
	}

	private void render() {

		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		shadingProgram.use();
		shadingProgram.setViewMatrix(viewMatrix);
		shadingProgram.setLightPos(lightPos);
		shadingProgram.bindTexture(texture);

		// Prepare program for buffers binding
		shadingProgram.enableVertexAttribArrays();

		// Render player
		shadingProgram.bindMeshBuffers(playerMesh);
		{
			shadingProgram.updateMVPMatrix(vpMatrix, playerMatrix);
			glDrawArrays(GL_TRIANGLES, 0, playerMesh.getTriangleCount() * 3);
		}

		// Render boxes
		shadingProgram.bindMeshBuffers(boxMesh);
		for (Matrix4f boxMatrix : boxesMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, boxMatrix);
			glDrawArrays(GL_TRIANGLES, 0, boxMesh.getTriangleCount() * 3);
		}

		// Render walls
		shadingProgram.bindMeshBuffers(boxMesh);
		for (Matrix4f wallMatrix : wallsMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, wallMatrix);
			glDrawArrays(GL_TRIANGLES, 0, boxMesh.getTriangleCount() * 3);
		}

		// Render enemies
		shadingProgram.bindMeshBuffers(enemyMesh);
		for (Matrix4f enemy : enemiesMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, enemy);
			glDrawArrays(GL_TRIANGLES, 0, enemyMesh.getTriangleCount() * 3);
		}

		shadingProgram.disableVertexAttribArrays();
	}

	private void runOneSimulationStep() {
		float timeStep = 1.0f / 60.f;
		int velocityIterations = 6;
		int positionIterations = 2;

		world.step(timeStep, velocityIterations, positionIterations);
	}

	private void setupPhysicalEntities() {
		createPhysicalPlayer();
		createPhysicalBox();
		createWalls();
		createEnemies();
	}

	private void createEnemies() {
		enemies.add(new Enemy(1, -3, world, "Enemy"));
	}

	private void createWalls() {
		// Left
		walls.add(new Wall(WALL_LEFT, 0, WALL_WIDTH, 6.3f, world, "Left Wall"));

		// Right
		walls.add(new Wall(WALL_RIGHT, 0, WALL_WIDTH, 6.3f, world, "Right Wall"));

		// Top
		walls.add(new Wall(0, WALL_TOP, 10, WALL_WIDTH, world, "Top Wall"));

		// Bottom
		walls.add(new Wall(0, WALL_BOTTOM, 10, WALL_WIDTH, world, "Bottom Wall"));
	}

	private void createPhysicalBox() {
		for (int i = 0; i < BOX_NB; i++) {
			physicalBox.add(new Box(5 - i, 5, world, "Box#" + i));
		}
	}

	private void createPhysicalPlayer() {
		player = new Player(0, 0, world, "Player");
	}

	private void setupPhysics() {
		Vec2 gravity = new Vec2(0.0f, 0.0f);
		boolean doSleep = true;
		world = new World(gravity, doSleep);

		// See http://www.iforce2d.net/b2dtut/collision-callbacks
		world.setContactListener(new ContactListener() {
			public void preSolve(Contact contact, Manifold oldManifold) {
			}

			public void postSolve(Contact contact, ContactImpulse impulse) {
				onPostSolve(contact, impulse);
			}

			public void beginContact(Contact contact) {
				onBeginContact(contact);
			}

			public void endContact(Contact contact) {
				onEndContact(contact);
			}
		});
	}

	private CollisionStore collisionStore = new CollisionStore();

	/**
	 * A contact occured. This method provide "collision strength" informations, see
	 * http://blog.allanbishop.com/box-2d-2-1a-tutorial-part-6-collision-strength/
	 */
	protected void onPostSolve(Contact contact, ContactImpulse impulse) {
		// Seem to be called even when there is not collision => early check
		// Low-impulse collisions append on contact; filter thoseto avoid continuously playing sounds
		if (impulse.normalImpulses[0] < 1e-2) {
			return;
		}

		PhysicalState obj1 = (PhysicalState) contact.getFixtureA().getUserData();
		PhysicalState obj2 = (PhysicalState) contact.getFixtureB().getUserData();

		GameEntity e1 = obj1.getEntity();
		GameEntity e2 = obj2.getEntity();

		// Order entities to limit combinations to: Box/Box, Box/Enemy, Box/Player, Box/Wall, Enemy/Player, Enemy/Wall, Player/Wall
		// Enemy/Player are not detected here (since both are static bodies)
		if (e1.getClass().getName().compareTo(e2.getClass().getName()) > 0) {
			GameEntity tmp = e1;
			e1 = e2;
			e2 = tmp;
		}

		if ((e1 instanceof Player || e1 instanceof Enemy) && e2 instanceof Wall) {
			// No gameplay effect, no sound
			return;
		}

		if (e1 instanceof Box && (e2 instanceof Box || e2 instanceof Wall)) {
			// No gameplay effect, collision sound
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(collision);
			}
			return;
		}

		List<String> impStr = new ArrayList<String>();
		for (int i = 0; i < impulse.normalImpulses.length; i++) {
			impStr.add(Float.toString(impulse.normalImpulses[i]));
		}

		// System.out.println("Contact between " + e1.getName() + " and " + e2.getName() + "; normal impulse is ["
		// + StringUtils.join(impStr, ", ") + "]");

		if (e1 instanceof Enemy && e2 instanceof Player) {
			// Player was hit by enemy! High damage for player
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.highOuch);
			}
			return;
		}

		if (e2 instanceof Enemy) {
			// Enemy is hit by a box! High damage
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.weeUup);
			}
			return;
		}

		if (e2 instanceof Player) {
			// Player is hit by a box! Low damage
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.ouch);
			}
			return;
		}
	}

	/** A contact occured between two fixtures */
	private void onBeginContact(Contact contact) {
		// PhysicalState obj1 = (PhysicalState) contact.getFixtureA().getUserData();
		// PhysicalState obj2 = (PhysicalState) contact.getFixtureB().getUserData();
		// System.out.println("Begin contact between " + obj1.getName() + " and " + obj2.getName());
	}

	/** A contact ended between two fixtures */
	private void onEndContact(Contact contact) {
		// PhysicalState obj1 = (PhysicalState) contact.getFixtureA().getUserData();
		// PhysicalState obj2 = (PhysicalState) contact.getFixtureB().getUserData();
		// System.out.println("End contact between " + obj1.getName() + " and " + obj2.getName());
	}

	public static void main(String[] args) {
		System.out.println("Starting.");
		try {
			new Main().run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			System.out.println("Exiting.");
		}
	}
}
