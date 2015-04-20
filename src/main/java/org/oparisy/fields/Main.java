package org.oparisy.fields;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
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
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.physics.AliveEntity;
import org.oparisy.fields.physics.Box;
import org.oparisy.fields.physics.CollisionStore;
import org.oparisy.fields.physics.Enemy;
import org.oparisy.fields.physics.GameEntity;
import org.oparisy.fields.physics.PhysicalState;
import org.oparisy.fields.physics.Player;
import org.oparisy.fields.physics.Wall;
import org.oparisy.fields.render.Camera;
import org.oparisy.fields.render.OpenGLMesh;
import org.oparisy.fields.render.OverlayProgram;
import org.oparisy.fields.render.ShadingProgram;
import org.oparisy.fields.tools.audio.SoundManager;
import org.oparisy.fields.tools.common.ControllerSetup;
import org.oparisy.fields.tools.common.MessageHandler;
import org.oparisy.fields.tools.common.Tools;

public class Main {

	private static final int BOX_NB = 4;

	private static final int WALL_BOTTOM = 5;

	private static final int WALL_TOP = -5;

	private static final int WALL_RIGHT = 9;

	private static final int WALL_LEFT = -9;

	private static final float WALL_WIDTH = 0.5f;

	private static final float PLAYER_RADIUS = 0.5f;

	/* Beyond this distance, player does not exerce a force */
	private static final double MAX_PLAYER_INFLUENCE = 10;

	private Player player;
	private List<Box> physicalBox = new ArrayList<Box>();

	private Controller controller;

	// Projection fixed parameters
	private static final float FOV = 45.0f;
	private static final float ZNEAR = 0.1f;
	private static final float ZFAR = 100f;

	boolean fullScreen = true;
	private boolean vsync = true;

	private static final int WINDOWED_SCREEN_WIDTH = 1024;
	private static final int WINDOWED_SCREEN_HEIGHT = 768;

	private int SCREEN_WIDTH;
	private int SCREEN_HEIGHT;

	// Resources (shader sources) location
	// private static final String RES = "fields"; // Main.class.getPackage().getName().replace(".", "/");

	private int playerTexture;

	private OpenGLMesh playerMesh;
	private Matrix4f playerMatrix;

	// Those are wrenches now
	private OpenGLMesh boxMesh;
	private List<Matrix4f> boxesMatrix = new ArrayList<Matrix4f>();

	private Camera camera = new Camera();
	private Vector3f lightPos = new Vector3f(2, 2, 5);

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
	private OverlayProgram overlayProgram;

	private SoundManager soundManager;

	private int enemyKill;

	private int gameOver;

	private int highOuch;

	private int ouch;

	private int weeUup;

	private int collision;

	// private int font;

	private Matrix4f overlayMatrix;

	private CollisionStore collisionStore = new CollisionStore();

	private boolean playerLost = false;
	private boolean playerWon = false;
	private double score = 0;

	private double elapsedSec;

	private int defaultTexture;
	private int wrenchTexture;

	private OpenGLMesh wrenchMesh;

	private List<String> homeText;

	public static int uploadTexture(BufferedImage img) {
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

		System.out.println("Waiting for keypress");

		updateOverlayMatrix();

		Mouse.setGrabbed(true);
		detectController();

		glClearColor(0.0f, 0.0f, 0.4f, 0.0f);
		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			renderHome();
			Display.update();

			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)
					|| (controller != null && ControllerSetup.isControllerInitialised() && controller.isButtonPressed(0))) {
				break;
			}
		}
		glClearColor(0.0f, 0.1f, 0.2f, 0.0f);

		setupPhysics();
		setupPhysicalEntities();

		// Perhaps it was plugged after reading home screen
		detectController();

		mainLoop();

		Display.destroy();
		soundManager.destroy();
	}

	private void renderHome() {
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		overlayProgram.use();
		overlayProgram.setProjectionMatrix(overlayMatrix);
		overlayProgram.bindTexture();
		overlayProgram.enableVertexAttribArrays();
		overlayProgram.bindBuffers();

		int y = 4;
		for (String line : homeText) {
			overlayProgram.drawText(4, y, line, 1.2f);
			y += 24;
		}

		overlayProgram.disableVertexAttribArrays();
		glDisable(GL_BLEND);
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

		setDisplayMode();
		Display.setTitle(this.getClass().getSimpleName());

		// Set up ARB_debug_output (requires a debug context)
		PixelFormat pixelFormat = new PixelFormat().withSamples(4); // 4x antialiasing
		ContextAttribs contextAttributes = new ContextAttribs(3, 3).withDebug(true).withProfileCompatibility(true);
		Display.create(pixelFormat, contextAttributes);
		ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new MessageHandler()));

		System.out.println("GL version: " + glGetString(GL_VERSION));

		// Load resources
		BufferedImage img = ImageIO.read(Tools.loadResource("fields/playerMap.png"));
		BufferedImage img2 = ImageIO.read(Tools.loadResource("fields/uvmap.png"));
		BufferedImage img3 = ImageIO.read(Tools.loadResource("fields/wrenchMap.png"));

		playerMesh = new OpenGLMesh(Tools.loadResource("fields/player.obj"));
		wrenchMesh = new OpenGLMesh(Tools.loadResource("fields/wrench.obj"));
		boxMesh = new OpenGLMesh(Tools.loadResource("fields/boxuv.obj"));
		enemyMesh = new OpenGLMesh(Tools.loadResource("fields/enemy.obj"));

		homeText = IOUtils.readLines(Tools.loadResource("fields/home.txt"));

		// Create a VAO to store buffers configuration
		// Required in OpenGL 3 (or not. But do not do this twice, it will crash!)
		glBindVertexArray(glGenVertexArrays());

		shadingProgram = new ShadingProgram();

		// Upload data to GPU
		playerMesh.uploadToGPU();
		boxMesh.uploadToGPU();
		enemyMesh.uploadToGPU();
		wrenchMesh.uploadToGPU();
		playerTexture = uploadTexture(img);
		defaultTexture = uploadTexture(img2);
		wrenchTexture = uploadTexture(img3);
		// font = uploadTexture(fontImg);

		overlayProgram = new OverlayProgram();

		// Global OpenGL setup
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		// glEnable(GL_CULL_FACE);
	}

	/** Set display mode, taking fullscreen into acocunt */
	private void setDisplayMode() {
		if (fullScreen) {
			SCREEN_WIDTH = 1920;
			SCREEN_HEIGHT = 1080;
		} else {
			SCREEN_WIDTH = WINDOWED_SCREEN_WIDTH;
			SCREEN_HEIGHT = WINDOWED_SCREEN_HEIGHT;
		}

		Tools.setDisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT, fullScreen);
		// DisplayMode mode = new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT);
		// Display.setDisplayMode(mode);
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
			elapsedSec = sec - lastTime;
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

			while (Keyboard.next()) {
				if (Keyboard.getEventKeyState()) {
					if (Keyboard.getEventKey() == Keyboard.KEY_F) {
						fullScreen = !fullScreen;
						setDisplayMode();
					} else if (Keyboard.getEventKey() == Keyboard.KEY_V) {
						vsync = !vsync;
						Display.setVSyncEnabled(vsync);
					}
				}
			}

			float x = 0, y = 0;
			boolean attraction = false;
			boolean repulsion = false;

			if (controller != null && ControllerSetup.isControllerInitialised()) {

				// StringBuilder sb = new StringBuilder();
				// for (int i=0; i<controller.getAxisCount(); i++) {
				// sb.append(controller.getAxisName(i) + ": " + controller.getAxisValue(i) + " ");
				// }
				// System.out.println(sb.toString());

				// XBox 360 mappings?
				x = controller.getAxisValue(1);
				y = -controller.getAxisValue(0);

				attraction = controller.isButtonPressed(0);
				repulsion = controller.isButtonPressed(1);
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
				y = 1;
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
				y = -1;
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
				x = -1;
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
				x = 1;
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				attraction = true;
			}

			// System.out.println("x: " + x + " y: " + y);

			// Detect collisions with borders (walls)
			// TODO Take player size into account
			Vec2 playerDeltaPos = new Vec2(x / 20f, y / 20f);
			Vec2 newPos = player.getState().getPosition().add(playerDeltaPos);
			if (newPos.x - PLAYER_RADIUS < WALL_LEFT + WALL_WIDTH || newPos.x + PLAYER_RADIUS > WALL_RIGHT - WALL_WIDTH) {
				playerDeltaPos.x = 0;
			}
			if (newPos.y - PLAYER_RADIUS < WALL_TOP + WALL_WIDTH || newPos.y + PLAYER_RADIUS > WALL_BOTTOM - WALL_WIDTH) {
				playerDeltaPos.y = 0;
			}

			if (!playerWon && !playerLost) {
				player.getState().addToPos(playerDeltaPos);
			}

			if (attraction || repulsion) {
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

						if (repulsion) {
							// Repulsive force
							force = force.negate();
						}

						physicalBox.applyForce(force, physicalBox.getPosition());
					}
				}
			}

			if (!playerWon && !playerLost) {
				runOneSimulationStep();
			}

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

			if (!playerWon && !playerLost) {
				score += elapsedSec * 100;
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

			Vec2 dir = player.getState().getPosition().sub(enemy.getState().getPosition());
			dir.normalize();

			// Enemy speed
			Vec2 f = dir.mul(0.5f);
			enemy.getState().applyForce(f, enemy.getState().getPosition());
		}
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
			boxMatrix.rotate(physicalBox.getAngle(), new Vector3f(0, 0, 1));

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

		updateOverlayMatrix();
	}

	private void updateOverlayMatrix() {
		// Set up projection matrix (will not change until next resize)
		// Identity view matrix (2D projection)
		overlayMatrix = Tools.glOrtho(0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, -1);
	}

	private void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		renderEntities();
		renderOverlay();
		// renderHome();
	}

	private void renderEntities() {
		shadingProgram.use();
		shadingProgram.setViewMatrix(viewMatrix);

		shadingProgram.setLightPos(lightPos);
		shadingProgram.bindTexture(playerTexture);

		// Prepare program for buffers binding
		shadingProgram.enableVertexAttribArrays();

		// Render player
		if (playerLost) {
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE);
		}
		shadingProgram.bindMeshBuffers(playerMesh);
		{
			shadingProgram.updateMVPMatrix(vpMatrix, playerMatrix);
			glDrawArrays(GL_TRIANGLES, 0, playerMesh.getTriangleCount() * 3);
		}
		if (playerLost) {
			glDisable(GL_BLEND);
		}

		// Render wrenches
		shadingProgram.bindTexture(wrenchTexture);
		shadingProgram.bindMeshBuffers(wrenchMesh);
		for (Matrix4f boxMatrix : boxesMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, boxMatrix);
			glDrawArrays(GL_TRIANGLES, 0, wrenchMesh.getTriangleCount() * 3);
		}

		shadingProgram.bindTexture(defaultTexture);

		// Render walls
		shadingProgram.bindMeshBuffers(boxMesh);
		for (Matrix4f wallMatrix : wallsMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, wallMatrix);
			glDrawArrays(GL_TRIANGLES, 0, boxMesh.getTriangleCount() * 3);
		}

		// Render enemies
		if (playerWon) {
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE);
		}
		shadingProgram.bindMeshBuffers(enemyMesh);
		for (Matrix4f enemy : enemiesMatrix) {
			shadingProgram.updateMVPMatrix(vpMatrix, enemy);
			glDrawArrays(GL_TRIANGLES, 0, enemyMesh.getTriangleCount() * 3);
		}
		if (playerWon) {
			glDisable(GL_BLEND);
		}

		shadingProgram.disableVertexAttribArrays();
	}

	private void renderOverlay() {
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		overlayProgram.use();
		overlayProgram.setProjectionMatrix(overlayMatrix);
		overlayProgram.bindTexture();
		overlayProgram.enableVertexAttribArrays();
		overlayProgram.bindBuffers();
		overlayProgram.drawText(4, 04, "Player Health " + String.format("%d", (int) player.getHealth()), 2);
		overlayProgram.drawText(4, 28, "Enemy Health  " + String.format("%d", (int) enemies.get(0).getHealth()), 2);
		overlayProgram.drawText(4, 52, "Score         " + String.format("%d", (int) score), 2);

		if (playerWon || playerLost) {
			String message = playerWon ? "You won" : "You lost";

			Matrix4f wooble = new Matrix4f();
			wooble.rotate((float) (15. * Math.PI / 180.), new Vector3f(0, 0, 1));

			overlayProgram.setProjectionMatrix(Matrix4f.mul(overlayMatrix, wooble, null));
			overlayProgram.drawText(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 4, message, 3);
		}

		overlayProgram.disableVertexAttribArrays();
		glDisable(GL_BLEND);
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
			physicalBox.add(new Box(5 - i * 1.5f, 5, world, "Box#" + i));
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
				if (!playerWon && !playerLost) {
					onPostSolve(contact, impulse);
				}
			}

			public void beginContact(Contact contact) {
			}

			public void endContact(Contact contact) {
			}
		});
	}

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

		float force = impulse.normalImpulses[0];

		// System.out.println("Contact between " + e1.getName() + " and " + e2.getName() + "; normal impulse is ["
		// + StringUtils.join(impStr, ", ") + "]");

		if (e1 instanceof Enemy && e2 instanceof Player) {
			// Player was hit by enemy! High damage for player
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.highOuch);
			}

			dealDamage((Player) e2, force * 6f);
			return;
		}

		if (e2 instanceof Enemy) {
			// Enemy is hit by a box! High damage
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.weeUup);
			}

			dealDamage((Enemy) e2, force * 4f);
			return;
		}

		if (e2 instanceof Player) {
			// Player is hit by a box! Low damage
			if (!collisionStore.recentlyCollided(e1, e2)) {
				soundManager.playEffect(this.ouch);
			}

			dealDamage((Player) e2, force);
			return;
		}
	}

	private void dealDamage(AliveEntity entity, float damage) {

		if (entity instanceof Enemy) {
			score += damage * damage * 1000;
		}

		float newHealth = entity.getHealth() - damage;
		entity.setHealth(newHealth);
		if (newHealth < 0) {
			System.out.println(entity.getName() + " is dead!");

			if (entity instanceof Enemy) {
				onEnemyDeath((Enemy) entity);
			} else {
				onPlayerDeath();
			}
		} else {
			System.out.println(entity.getName() + " health is now " + Math.ceil(newHealth) + "%");
		}
	}

	private void onPlayerDeath() {
		player.setHealth(0);
		soundManager.playEffect(this.gameOver);
		playerLost = true;
	}

	private void onEnemyDeath(Enemy entity) {
		entity.setHealth(0);
		soundManager.playEffect(this.enemyKill);
		playerWon = true;
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
