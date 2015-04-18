package org.oparisy.fields;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
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
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.oparisy.fields.tools.common.ControllerSetup;
import org.oparisy.fields.tools.common.Tools;

public class Main {

	private static final int WALL_BOTTOM = 5;

	private static final int WALL_TOP = -5;

	private static final int WALL_RIGHT = 9;

	private static final int WALL_LEFT = -9;

	private static final float WALL_WIDTH = 0.5f;

	/* Beyond this distance, player does not exerce a force */
	private static final double MAX_PLAYER_INFLUENCE = 10;

	private static final int MAX_CONTACTS = 64; // maximum number of contact
												// points per body

	// Note that player has no velocity (only boxes are simulated)
	private PhysicalState physicalPlayer;
	private List<PhysicalState> physicalBox = new ArrayList<PhysicalState>();

	private Controller controller;

	// Projection fixed parameters
	private static final float FOV = 45.0f;
	private static final float ZNEAR = 0.1f;
	private static final float ZFAR = 100f;

	private static final int SCREEN_HEIGHT = 768;
	private static final int SCREEN_WIDTH = 1024;

	// Resources (shader sources) location
	private static final String RES = "fields"; //Main.class.getPackage().getName().replace(".", "/");

	private static final String vertexShaderFile = RES + "/" + "StandardShading.vertexshader";
	private static final String fragmentShaderFile = RES + "/" + "StandardShading.fragmentshader";

	private int texture;

	private int program;

	/** Shader uniforms */
	private int mvpID;
	private int vID;
	private int mID;
	private int lightPosID;
	private int samplerID;

	/** Vertex attributes */
	private int posID;
	private int uvID;
	private int normalID;

	private OpenGLMesh playerMesh;
	private Matrix4f playerMatrix;

	private OpenGLMesh boxMesh;
	private List<Matrix4f> boxesMatrix = new ArrayList<Matrix4f>();

	private Camera camera = new Camera();
	private Vector3f lightPos = new Vector3f(4, 4, 4);

	private Matrix4f viewMatrix;
	private Matrix4f mvpMatrix;

	private double lastTime;
	private Matrix4f vpMatrix;

	private World world;

	private List<Wall> walls = new ArrayList<Wall>();

	private OpenGLMesh enemyMesh;
	private List<Enemy> enemies = new ArrayList<Enemy>();
	private List<Matrix4f> enemiesMatrix = new ArrayList<Matrix4f>();

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

	public void buildProgram() throws Exception, Error {
		int vs = Tools.makeShader(GL_VERTEX_SHADER, vertexShaderFile);
		int fs = Tools.makeShader(GL_FRAGMENT_SHADER, fragmentShaderFile);
		program = Tools.makeProgram(vs, fs);
		glDeleteShader(vs);
		glDeleteBuffers(fs);
	}

	private void run() throws Exception {
		setupRender();

		setupPhysics();
		setupPhysicalEntities();

		detectController();

		mainLoop();

		closeODE();
		Display.destroy();
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

		// Program must be compiled before binding buffers (why?)
		buildProgram();

		// Required in OpenGL 3
		int vao = glGenVertexArrays();
		glBindVertexArray(vao);

		// Upload data to GPU
		playerMesh.uploadToGPU();
		boxMesh.uploadToGPU();
		enemyMesh.uploadToGPU();
		texture = uploadTexture(img);

		// Required by render()
		posID = Tools.glGetAttribLocationChecked(program, "vertexPosition_modelspace");
		uvID = Tools.glGetAttribLocationChecked(program, "vertexUV");
		normalID = Tools.glGetAttribLocationChecked(program, "vertexNormal_modelspace");

		mvpID = Tools.glGetUniformLocationChecked(program, "MVP");
		mID = Tools.glGetUniformLocationChecked(program, "M");
		vID = Tools.glGetUniformLocationChecked(program, "V");
		lightPosID = Tools.glGetUniformLocationChecked(program, "LightPosition_worldspace");
		samplerID = Tools.glGetUniformLocationChecked(program, "myTextureSampler");

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

			/*
			 * Between each integrator step the user can call functions to apply forces to the rigid body. These forces are added to
			 * "force accumulators" in the rigid body object. When the next integrator step happens, the sum of all the applied forces will
			 * be used to push the body around. The forces accumulators are set to zero after each integrator step.
			 */

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

				// TODO Damping is not satisfying => this could be a solution? (no physical movement simulation for player)
				/*
				 * How do I make "one way" collision interaction
				 * 
				 * Suppose you need to have two bodies (A and B) collide. The motion of A should affect the motion of B as usual, but B
				 * should not influence A at all. This might be necessary, for example, if B is a physically simulated camera in a VR
				 * environment. The camera needs collision response so that it doesn't enter into any scene objects by mistake, but the
				 * motion of the camera should not affect the simulation. How can this be achieved?
				 * 
				 * To solve this, attach a contact joint between B and null (the world), then set the contact joint's motion fields to match
				 * A's velocities at the contact point. See the demo_motion.cpp sample distributed with ODE for an example.
				 */

				// DVector3C linVel = playerBody.getLinearVel();
				// System.out.println("Linear velocity: " + linVel.length());

				// if (Math.abs(x)<0.01f && Math.abs(y) < 0.01f) {
				// playerBody.addForce(linVel.get0() * -0.5, linVel.get(1) * -0.5, 0);
				// } else {

				// Detect collisions with borders (walls)
				// TODO Take player size into account
				Vec2 playerDeltaPos = new Vec2(x / 20f, y / 20f);
				Vec2 newPos = physicalPlayer.getPosition().add(playerDeltaPos);
				if (newPos.x < WALL_LEFT + WALL_WIDTH || newPos.x > WALL_RIGHT - WALL_WIDTH) {
					playerDeltaPos.x = 0;
				}
				if (newPos.y < WALL_TOP + WALL_WIDTH || newPos.y > WALL_BOTTOM - WALL_WIDTH) {
					playerDeltaPos.y = 0;
				}

				physicalPlayer.addToPos(playerDeltaPos);
				// physicalPlayer.setX(physicalPlayer.getX() + x / 20f);
				// physicalPlayer.setX(physicalPlayer.getX() + y / 20f);
				//
				// System.out.println("Position: " + physicalPlayer.getPosx() + " " + physicalPlayer.getPosy());
				// }

				if (controller.isButtonPressed(0) || controller.isButtonPressed(1)) {
					for (PhysicalState physicalBox : this.physicalBox) {
						// Compute vector from box to player
						Vec2 delta = physicalBox.getPosition().sub(physicalPlayer.getPosition());
						// float vecx = physicalBox.getX() - physicalPlayer.getX();
						// float vecy = physicalBox.getY() - physicalPlayer.getY();
						// DVector3C playerPosition = physicalPlayer.getBody().getPosition();
						// DVector3C boxPosition = physicalBox.getBody().getPosition();
						// double vecx = boxPosition.get0() - playerPosition.get0();
						// double vecy = boxPosition.get1() - playerPosition.get1();
						// double vecz = boxPosition.get2() - playerPosition.get2();
						//

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

	private void runIA() {
		// Move enemies according to player position
		for (Enemy enemy : enemies) {
			Vec2 dir = physicalPlayer.getPosition().sub(enemy.getState().getPosition());
			dir.normalize();
			dir = dir.mul(0.005f); // Enemy speed
			enemy.getState().addToPos(dir);
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
		// Matrix4f boxMatrix = new Matrix4f();
		// boxMatrix.translate(new Vector3f(0, 5, 2));
		// Get position and orientation from simulated object

		// DVector3C pos = physicalBox.getGeom().getPosition();

		for (PhysicalState physicalBox : this.physicalBox) {
			Matrix4f boxMatrix = new Matrix4f();
			boxMatrix.translate(new Vector3f(physicalBox.getPosition().x, physicalBox.getPosition().y, 0));
			boxMatrix.scale(new Vector3f(0.25f, 0.25f, 0.25f));

			boxesMatrix.add(boxMatrix);
		}
	}

	private void updatePlayerMatrix() {

		// Get position and orientation from simulated object
		// DVector3C pos = physicalPlayer.getGeom().getPosition();

		playerMatrix = new Matrix4f();
		playerMatrix.translate(new Vector3f(physicalPlayer.getPosition().x, physicalPlayer.getPosition().y, 0));
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

		glUseProgram(program);
		glUniformMatrix4(vID, false, Tools.buildFloatBuffer(viewMatrix));

		glUniform3f(lightPosID, lightPos.x, lightPos.y, lightPos.z);

		// Bind texture to texture unit 0
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glUniform1i(samplerID, 0);

		// Prepare program for buffers binding
		glEnableVertexAttribArray(posID);
		glEnableVertexAttribArray(uvID);
		glEnableVertexAttribArray(normalID);

		// Render player
		bindMeshBuffers(playerMesh);

		{
			Matrix4f modelMatrix = playerMatrix;// Matrix4f.translate(new Vector3f(i * 5, 0, 0), playerMatrix, null);

			this.mvpMatrix = Matrix4f.mul(vpMatrix, modelMatrix, null);
			glUniformMatrix4(mvpID, false, Tools.buildFloatBuffer(mvpMatrix));
			glUniformMatrix4(mID, false, Tools.buildFloatBuffer(modelMatrix));

			glDrawArrays(GL_TRIANGLES, 0, playerMesh.getTriangleCount() * 3);
		}

		// Render boxes
		bindMeshBuffers(boxMesh);

		for (Matrix4f boxMatrix : boxesMatrix) {
			Matrix4f modelMatrix = boxMatrix;

			this.mvpMatrix = Matrix4f.mul(vpMatrix, modelMatrix, null);
			glUniformMatrix4(mvpID, false, Tools.buildFloatBuffer(mvpMatrix));
			glUniformMatrix4(mID, false, Tools.buildFloatBuffer(modelMatrix));

			glDrawArrays(GL_TRIANGLES, 0, boxMesh.getTriangleCount() * 3);
		}

		// Render walls
		bindMeshBuffers(boxMesh);

		for (Wall wall : walls) {
			Matrix4f modelMatrix = new Matrix4f();
			PhysicalState ps = wall.getState();
			modelMatrix.translate(new Vector3f(ps.getPosition().x, ps.getPosition().y, 0));
			modelMatrix.scale(new Vector3f(wall.getW(), wall.getH(), Math.min(wall.getW(), wall.getH())));

			this.mvpMatrix = Matrix4f.mul(vpMatrix, modelMatrix, null);
			glUniformMatrix4(mvpID, false, Tools.buildFloatBuffer(mvpMatrix));
			glUniformMatrix4(mID, false, Tools.buildFloatBuffer(modelMatrix));

			glDrawArrays(GL_TRIANGLES, 0, boxMesh.getTriangleCount() * 3);
		}

		// Render enemies
		bindMeshBuffers(enemyMesh);

		for (Matrix4f enemy : enemiesMatrix) {
			Matrix4f modelMatrix = enemy;

			this.mvpMatrix = Matrix4f.mul(vpMatrix, modelMatrix, null);
			glUniformMatrix4(mvpID, false, Tools.buildFloatBuffer(mvpMatrix));
			glUniformMatrix4(mID, false, Tools.buildFloatBuffer(modelMatrix));

			glDrawArrays(GL_TRIANGLES, 0, enemyMesh.getTriangleCount() * 3);
		}

		glDisableVertexAttribArray(posID);
		glDisableVertexAttribArray(uvID);
		glDisableVertexAttribArray(normalID);
	}

	private void bindMeshBuffers(OpenGLMesh openGLMesh) {
		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getVertexBuffer());
		glVertexAttribPointer(posID, 3, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getUvBuffer());
		glVertexAttribPointer(uvID, 2, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, openGLMesh.getNormalBuffer());
		glVertexAttribPointer(normalID, 3, GL_FLOAT, false, 0, 0);
	}

	private void runOneSimulationStep() {
		float timeStep = 1.0f / 60.f;
		int velocityIterations = 6;
		int positionIterations = 2;

		world.step(timeStep, velocityIterations, positionIterations);
	}

	private void closeODE() {

	}

	private void setupPhysicalEntities() {
		createPhysicalPlayer();
		createPhysicalBox();
		createWalls();
		createEnemies();
	}

	private void createEnemies() {
		CircleShape cs = new CircleShape();
		cs.m_radius = 0.5f;
		enemies.add(new Enemy(1, -3, world));
	}

	private void createWalls() {
		// Left
		walls.add(new Wall(WALL_LEFT, 0, WALL_WIDTH, 6.3f, world));

		// Right
		walls.add(new Wall(WALL_RIGHT, 0, WALL_WIDTH, 6.3f, world));

		// Top
		walls.add(new Wall(0, WALL_TOP, 10, WALL_WIDTH, world));

		// Bottom
		walls.add(new Wall(0, WALL_BOTTOM, 10, WALL_WIDTH, world));
	}

	private void createPhysicalBox() {
		for (int i = 0; i < 10; i++) {
			CircleShape cs = new CircleShape();
			cs.m_radius = 0.5f;
			physicalBox.add(new PhysicalState(5 - i, 5, world, BodyType.DYNAMIC, cs));
		}
	}

	private void createPhysicalPlayer() {
		CircleShape cs = new CircleShape();
		cs.m_radius = 0.5f;
		physicalPlayer = new PhysicalState(0, 0, world, BodyType.STATIC, cs);
	}

	private void setupPhysics() {
		Vec2 gravity = new Vec2(0.0f, 0.0f);
		boolean doSleep = true;
		world = new World(gravity, doSleep);
	}

	public static void main(String[] args) {
		System.out.println("Starting.");
		try {
			// new Main().runPhysics();
			new Main().run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			System.out.println("Exiting.");
		}
	}
}
