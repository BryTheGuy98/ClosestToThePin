package client;

import tage.*;
import tage.audio.*;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.shapes.*;
import tage.nodeControllers.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.joml.*;
import net.java.games.input.Event;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;
import java.util.Scanner;

/**
 *  This the main class that initializes the game and houses all necessary variables
 */
public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	
	private InputManager im;

	private double currTime, prevTime, elapsedTime;
	private HUDCooldown hudCD;
	private boolean paused=false;
	private IAudioManager audioMgr;
	private Sound grassHit, grassSound, grassHitNPC;

	private GameObject gBall, camOb, terr, flag, golfStand, controlLine;
	private ObjShape gBallS, cubeS, terrS, npcS, golfStandS, line;
	private AnimatedShape flagS;
	private TextureImage gBallT, gBallT2, cleartx, grass, hills, flagT, npcT, golfStandT;
	private Light light1;
	String hudStatus, popup;
	int score = 0; int hScore = 0;
	int holeNum = 1;
	private CameraOrbit3D co3d;
	private int skybox;
	private boolean debugmode = false;
	private boolean physRender = false;
	private boolean physicsOn = false;
	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol = ProtocolType.UDP;
	private ClientProtocol protClient;
	private boolean isClientConnected = false;
	private PhysicsEngine pEngine;
	private PhysicsObject ballP, groundP;
	private float[] vals = new float[16];
	private SoundBuffer grassBounce;
	private int ballStopCounter = 0;
	private int controlMode; // 0 = aiming, 1 = power, 2 = offset, 3 = physics, 4 = game over, 5 = OOB
	private float angle = 0.0f;
	private float power = 0.0f;
	private float offset = 0.0f;
	private float tick;
	private final Matrix4f postPos = new Matrix4f().translation(0.1f,39f,-249.75f);
	private final float stopDist = 0.02f;
	private final int totalNumHoles = 3;
	

	public MyGame(String serverAddress, int serverPort) { 
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		}

	public static void main(String[] args)
	{	String ip;
		int serverport;
		String input;
		Scanner scan = new Scanner(System.in);
		
		System.out.print("Enter ip address (format: #.#.#.#): ");
		input = scan.nextLine();
		ip = input;
		
		System.out.print("Enter server port number: ");
		input = scan.nextLine();
		serverport = Integer.parseInt(input);
		
		MyGame game = new MyGame(ip, serverport);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	gBallS = new ImportedModel("golfball.obj");
		cubeS = new Cube();
		terrS = new TerrainPlane(1000);
		npcS = new ImportedModel("bird3.obj");
		golfStandS = new ImportedModel("golfstand.obj");
		Vector3f start = new Vector3f(0, 0, 0);
		Vector3f end = new Vector3f(0, 0, 10);
		line = new Line(start, end);
		
		flagS = new AnimatedShape("flag.rkm", "flag.rks");
		flagS.loadAnimation("FLAP", "flag_flap.rka");
	}

	@Override
	public void loadTextures()
	{	gBallT = new TextureImage("golfball.png");
		gBallT2 = new TextureImage("golfball2.png");
		cleartx = new TextureImage("blank.jpg");	// for placeholders
		npcT = new TextureImage("bird.png");
		grass = new TextureImage("grass.png");
		hills = new TextureImage("hills.jpg");
		flagT = new TextureImage("flag.png");
		golfStandT = new TextureImage("golfstand.png");
	}

	@Override
	public void loadSkyBoxes() {
		skybox = (engine.getSceneGraph()).loadCubeMap("skybox");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(skybox);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}
	
	@Override
	public void loadSounds() {
		AudioResource res1, res2;
		audioMgr = engine.getAudioManager();
		res1 = audioMgr.createAudioResource("grass_impact.wav", AudioResourceType.AUDIO_SAMPLE);
		res2 = audioMgr.createAudioResource("grass-blowing-in-wind.wav", AudioResourceType.AUDIO_SAMPLE);
		grassHit = new Sound(res1, SoundType.SOUND_EFFECT, 100, false);
		grassHit.initialize(audioMgr);
		grassHit.setMaxDistance(10.0f);
		grassHit.setMinDistance(0.5f);
		grassHit.setRollOff(5.0f);
		grassHitNPC = new Sound(res1, SoundType.SOUND_EFFECT, 100, false);
		grassHitNPC.initialize(audioMgr);
		grassHitNPC.setMaxDistance(10.0f);
		grassHitNPC.setMinDistance(0.5f);
		grassHitNPC.setRollOff(5.0f);
		grassBounce = new SoundBuffer(400);
		
		grassSound = new Sound(res2, SoundType.SOUND_EFFECT, 30, true);
		grassSound.initialize(audioMgr);
		grassSound.setMaxDistance(10.0f);
		grassSound.setMinDistance(0.5f);
		grassSound.setRollOff(5.0f);
	}
	
	@Override
	public void buildObjects()	// V = y, U = x, N = -z
	{	Matrix4f initialTranslation, initialScale;
	
		// golf stand
		golfStand = new GameObject(GameObject.root(), golfStandS, golfStandT);
		initialTranslation = (new Matrix4f()).translation(0f,0f,-250f);
		golfStand.setLocalTranslation(initialTranslation);
		golfStand.globalYaw((float) Math.toRadians(90.0f));
		
		// player golf ball
		gBall = new GameObject(GameObject.root(), gBallS, gBallT);
		initialTranslation = postPos;
		initialScale = (new Matrix4f()).scaling(1.0f);
		gBall.setLocalTranslation(initialTranslation);
		gBall.setLocalScale(initialScale);
		
		// control line
		controlLine = new GameObject(GameObject.root(), line, cleartx);
		controlLine.getRenderStates().setColor(new Vector3f(1, 0, 0));
		controlLine.setParent(gBall);
		controlLine.propagateTranslation(true);
		controlLine.propagateRotation(false);
		
		
		// camera object
		camOb = new GameObject(GameObject.root(), cubeS, cleartx);
		initialTranslation = (new Matrix4f().translation(0, 1, 5));
		initialScale = (new Matrix4f()).scaling(0.1f);
		camOb.setLocalTranslation(initialTranslation);
		camOb.setLocalScale(initialScale);
		camOb.getRenderStates().disableRendering();
		camOb.lookAt(gBall);
		
		// terrain
		terr = new GameObject(GameObject.root(), terrS, grass);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(300.0f, 100.0f, 600.0f);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(hills);
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(32);
		
		// flag (animated)
		initialTranslation = (new Matrix4f()).translation(0f,1.01f,50f);
		flag = new GameObject(GameObject.root(), flagS, flagT);
		flag.setLocalTranslation(initialTranslation);
	}

	@Override
	public void initializeLights()
	{	
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 50.0f, 2.0f));
		light1.setDirection(new Vector3f(0f, 1f, 0f));
		light1.setRange(300f);
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	// set initial values
		prevTime = System.currentTimeMillis();
		currTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);
		score = 0;
		hudStatus = "";
		hudCD = new HUDCooldown();
		tick = 1.0f;
		
		// input manager and functions
		im = engine.getInputManager();	
		
		Key_Move_Fwd kmf = new Key_Move_Fwd();
		Key_Move_Bwd kmb = new Key_Move_Bwd();
		Key_Move_Left ktl = new Key_Move_Left();
		Key_Move_Right ktr = new Key_Move_Right();
		ZoomIn zi = new ZoomIn();
		ZoomOut zo = new ZoomOut();
		KeyCamLeft kcl = new KeyCamLeft();
		KeyCamRight kcr = new KeyCamRight();
		Key_RotUp kru = new Key_RotUp();
		Key_RotDown krd = new Key_RotDown();
		ToggleDebug td = new ToggleDebug();
		TogglePhysRender tpr = new TogglePhysRender();
		TogglePhysics tp = new TogglePhysics();
		Boing b = new Boing();
		Click c = new Click();
		HUDPopupTest thp = new HUDPopupTest();
		TestSwing tw = new TestSwing();
		
		// keyboard controls
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, kmf, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, kmb, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, ktl, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, ktr, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.Q, zi, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.E, zo, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.LEFT, kcl, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.RIGHT, kcr, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.UP, kru, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.DOWN, krd, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.X, td, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.O, tpr, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.P, tp, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.B, b, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.SPACE, c, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.H, thp, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.T, tw, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		
		
		// gamepad controls (only loads if a gamepad is present)
		if (im.getFirstGamepadName() != null) {
			String gp = im.getFirstGamepadName();
			RStickX rStickX = new RStickX();
			RStickY rStickY = new RStickY();
			
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.Z, rStickX, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.RZ, rStickY, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._5, zi, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._7, zo, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._1, c, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._4, td, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._3, tw, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		}
		


		// ------------- positioning the camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		
		
		// node controllers
		
		
		// orbiting camera handler
		co3d = new CameraOrbit3D(engine, camOb, gBall);
		
		// networking startup
		setupNetworking();
		
		// physics world
		// --- initialize physics system ---
		float[] gravity = {0f, -5f, 0f};
		pEngine = (engine.getSceneGraph()).getPhysicsEngine();
		pEngine.setGravity(gravity);
		// --- create physics world ---
		float mass = 1.0f;
		float up[ ] = {0,1,0};
		float radius = 1.25f;
		double[ ] tempTransform;
		
		Matrix4f translation = new Matrix4f(gBall.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		ballP = (engine.getSceneGraph()).addPhysicsSphere(mass, tempTransform, radius);
		ballP.setBounciness(0.8f);
		ballP.setSleepThresholds(0.50f, 0.50f);
		gBall.setPhysicsObject(ballP);
		
		translation = new Matrix4f(terr.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		groundP = (engine.getSceneGraph()).addPhysicsStaticPlane(tempTransform, up, 0);
		groundP.setBounciness(1.0f);
		terr.setPhysicsObject(groundP);
		
		
		
		// initial animations
		flagS.playAnimation("FLAP", 0.25f, AnimatedShape.EndType.LOOP, 0);
		
		// initialize sound
		grassSound.setLocation(getAvatarPosition());
		grassHit.setLocation(getAvatarPosition());
		setEarParameters();
		grassSound.play();
	}
	/**
	 * sets up the networking functions
	 */
	private void setupNetworking() {
		isClientConnected = false;
		try { 	protClient = new ClientProtocol(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this); } 
		catch (UnknownHostException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		if (protClient == null) { System.out.println("missing protocol host"); }
		else{ 
			// ask client protocol to send initial join message
			// to server, with a unique identifier for this client
			protClient.sendJoinMessage();
		}
	}

	@Override
	public void update() {
		prevTime = currTime;
		currTime = System.currentTimeMillis();
		if (!paused) elapsedTime += (currTime - prevTime) / 1000;
		Vector3f oldPos = getAvatarPosition();
		
		// read controller input
		im.update((float)elapsedTime);
		
		// update physics
		if (physicsOn) {
			AxisAngle4f aa = new AxisAngle4f();
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			Matrix4f mat3 = new Matrix4f().identity();
			checkForCollisions();
			pEngine.update((float)elapsedTime);
			for (GameObject go:engine.getSceneGraph().getGameObjects()) {
				if (go.getPhysicsObject() != null) {
					// set translation
					mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3,0,mat.m30());
					mat2.set(3,1,mat.m31());
					mat2.set(3,2,mat.m32());
					go.setLocalTranslation(mat2);
					// set rotation
					mat.getRotation(aa);
					mat3.rotation(aa);
					go.setLocalRotation(mat3);
				}
			}
		}
			
		// build and set HUD
		int h = (int) (engine.getRenderSystem().getViewport("MAIN").getActualHeight());
		int w = (int) (engine.getRenderSystem().getViewport("MAIN").getActualWidth());
		String hud1;
		String hud2 = "";
		if (debugmode) {
			Vector3f pos = getAvatarPosition();
			hud1 = "(" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")";
		}
		else {
			hud1 = "Score = " + Integer.toString(score);
		}
		if (isClientConnected) hud2 = "Top Score = " + Integer.toString(hScore);
		if (debugmode) hudStatus = "Debug Mode";
		else if (controlMode == 1) hudStatus = "Power: " + Integer.toString((int) power);
		else if (controlMode == 3) hudStatus = Integer.toString((int) getDist(gBall, flag)) + " ft";
		else if (controlMode == 4 && isClientConnected && score == hScore) hudStatus = "You Win!";
		else hudStatus = "";
		if (controlMode == 4) popup = "That's the game!";
		else if (hudCD.checkCooldown()) popup = "";
		Vector3f hud1Color = new Vector3f(1, .5f, 0);
		Vector3f hud2Color = new Vector3f(.3f,.3f, 1);
		Vector3f hudpopupColor = new Vector3f(1f,.7f, .7f);
		
		
		(engine.getHUDmanager()).setHUD1(hud1, hud1Color, w/32, h/32);
		(engine.getHUDmanager()).setHUD2(hudStatus, hud2Color, (int) (w*.6), h/32);
		(engine.getHUDmanager()).setHUD3(hud2, hud1Color, w/32, h/16);
		(engine.getHUDmanager()).setHUD4(popup, hudpopupColor, w/2, h/2);
		
		Vector3f newPos = getAvatarPosition();
		boolean notMoved = (checkDist(oldPos, newPos, stopDist));
		
		// gameplay effects
		if (newPos.y() < -20.0f && !hudCD.checkCooldown()) {	// out of bounds
			physicsOn = false;
			hudCD.setCooldown(4);
			popup = "Out of Bounds!";
			controlMode = 5;
		}
		else if (controlMode == 5 && hudCD.checkCooldown()) {
			ballStopCounter = 0;
			finalizeSwing();
		}
		else {
			switch (controlMode) {
			case 0:
				angle += tick;
				if (angle > 30.0f) { tick = -1.0f; angle = 30.0f; }
				else if (angle < -30.0f) { tick = 1.0f; angle = -30.0f; }
				setAimAngle();
				break;
			case 1:
				power += tick;
				if (power < 0) { tick = 0.5f; power = 0f; }
				else if (power > 100f) { tick = -0.5f; power = 100f; }
				break;
			case 2:
				offset += tick;
				if (offset < -10f) { tick = 0.2f; offset = -10f; }
				else if (offset > 10f) { tick = -0.2f; offset = 10f; }
				break;
			case 3:
				if (notMoved) ballStopCounter++;
				else ballStopCounter = 0;
				if (ballStopCounter >= 50) {
					ballStopCounter = 0;
					finalizeSwing();
				}
				break;
			case 4:
				break;
			case 5:
				break;
			}
		}
		
		//update animations
		flagS.updateAnimation();
		
		// update camera position
		co3d.updateCamPosition();
		Vector3f loc = camOb.getWorldLocation();
		float height = terr.getHeight(loc.x(), loc.z());
		if (loc.y() < height) camOb.setLocalLocation(new Vector3f(loc.x(), height + 0.10f, loc.z()));
		updateCam();
		
		// update sound parameters
		grassSound.setLocation(loc);
		setEarParameters();
		
		// if a minimum distance is moved, the new position is send to the server
		if (isClientConnected && !notMoved) protClient.sendMoveMessage(newPos);
		
		// load network data
		processNetwork((float) elapsedTime);
	}
	/**
	 * function for processing network data
	 * @param elapsedTime time
	 */
	protected void processNetwork(float elapsedTime) {
		if (protClient != null) protClient.processPackets();
	}

	@Override
	public void keyPressed(KeyEvent e)	// V = y, U = x, N = -z
	{	
		super.keyPressed(e);
	}

	/**
	 * 	[OBSOLETE] An input action class for when the left control stick is moved left or right. Now updated to use global yaw.
	 * 	In this case, doing so rotates the view left or right.
	 */
	public class LStickX extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				gBall.globalYaw(e.getValue()/-30.0f);
			}
		}
	}
	/**
	 * 	[OBSOLETE] An input action class for when the left control stick is moved up or down.
	 * 	In this case, it moves the camera or dolphin forward or backward
	 */
	public class LStickY extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				gBall.moveZ(e.getValue()/-10.0f);
			}
		}
	}
	
	/**
	 * 	An input action class for using the right stick to control the camera rotation
	 */
	public class RStickX extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				co3d.xzRot(-1.0f*e.getValue());
			}
		}
	}
	/**
	 * 	An input action class for using the right stick to control the camera height
	 */
	public class RStickY extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				co3d.yRot(e.getValue());
			}
		}
	}
	/**
	 * [DEBUG] An input action class for moving forward with the keyboard (always towards the world Z axis).
	 */
	public class Key_Move_Fwd extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
		//	gBall.moveZ(1/10f);
			if (debugmode) ballP.applyForce(0, 0, 1f, 0, 0, 0);
		}
	}
	/**
	 *  [DEBUG] An input action class for moving backwards with the keyboard (always towards the world -Z axis).
	 */
	public class Key_Move_Bwd extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (debugmode) ballP.applyForce(0, 0, -1f, 0, 0, 0);
		}
	}
	/**
	 *  [DEBUG] An input action class for moving to the left with the keyboard (always towards the world -X axis).
	 */
	public class Key_Move_Left extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (debugmode) ballP.applyForce(1f, 0, 0, 0, 0, 0);
		}
	}
	/**
	 * 	[DEBUG] An input action class for moving to the left with the keyboard (always towards the world X axis).
	 */
	public class Key_Move_Right extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (debugmode) ballP.applyForce(-1f, 0, 0, 0, 0, 0);
		}
	}
	/**
	 * [DEBUG] an action class to help demonstrate physics. Bounces the avatar into the air.
	 */
	public class Boing extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (debugmode) ballP.applyForce(0, 750f, 0, 0, 0, 0);
		}
	}
	/**
	 * 	An input action class for rotating the view upward using the keyboard.
	 */
	public class Key_RotUp extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			co3d.yRot(-1.0f);
		}
	}
	/**
	 * 	An input action class for rotating the view downward using the keyboard.
	 */
	public class Key_RotDown extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.yRot(1.0f);
		}
	}
	/**
	 * 	An action class for orbiting the camera left with the keyboard/
	 */
	public class KeyCamLeft extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.xzRot(1.0f);
		}
	}
	/**
	 * 	An action class for orbiting the camera right with the keyboard/
	 */
	public class KeyCamRight extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.xzRot(-1.0f);
		}
	}
	/**
	 * 	An action class for zooming the camera in closer (both keyboard and gamepad)
	 */
	public class ZoomIn extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.zoom(-0.1f);
		}
	}
	/**
	 * 	An action class for zooming the camera farther away  (both keyboard and gamepad)
	 */
	public class ZoomOut extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.zoom(0.1f);
		}
	}
	
	/**
	 * [DEBUG} An action class for turning debug mode on/off
	 */
	public class ToggleDebug extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			debugmode = !debugmode;
		}
	}
	/**
	 * [DEBUG] action class for toggling physics rendering
	 */
	public class TogglePhysRender extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			physRender = !physRender;
			if (physRender) engine.enablePhysicsWorldRender();
			else engine.disablePhysicsWorldRender();
		}
	}
	/**
	 * [DEBUG] action class for toggling physics simulation
	 */
	public class TogglePhysics extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			physicsOn = !physicsOn;
		}
	}
	/**
	 * [DEBUG] prints a popup message the the HUD.
	 */
	public class HUDPopupTest extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			if (debugmode) {
				popup = "Popup Message!";
				hudCD.setCooldown(5);
			}
		}
	}
	
	/**
	 * action class for the main click functions (both keyboard and gamepad)
	 */
	public class Click extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			switch (controlMode) {	// TODO
			case 0: startPower();
				break;
			case 1: // startOffset();
					startSwing();
				break;
			case 2: startSwing();
				break;
			case 3: 
				break;
			case 4:
				break;
			case 5:
				break;
			}
		}
	}
	/**
	 *  [DEBUG] A function to test swing the ball with a consistent angle
	 */
	public class TestSwing extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			if (debugmode && controlMode < 3) {
				controlMode = 3;
				controlLine.setLocalScale(new Matrix4f().scale(0.0f));
				physicsOn = true;
				ballP.applyForce(0, 0, 1000f, 0, 0, 0);
			}
		}
	}
	

		/* 	--D-pad values--
		 * 	Up/Left: 	0.125
		 * 	Up: 		0.25
		 * 	Up/Right:	0.375
		 * 	Right: 		0.5
		 * 	Down/Right:	0.625
		 * 	Down:		0.75
		 * 	Down/Left:	0.875
		 * 	Left: 		1.0		*/

	/**
	 * control class for disconnecting from server
	 */
	private class Disconnect extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt) { 
			if(protClient != null && isClientConnected == true) { protClient.sendByeMessage(); }
		}
	}

	/**
	 * 	A void function called by the update() function every time.
	 * 	It moves the view camera into the same position and rotation as the "CamOb" object, which is not rendered.
	 * 	This is done to simplify camera movement functions.
	 */
	public void updateCam() {
		Vector3f loc, fwd, up, right;
		Camera cam;
		cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		loc = camOb.getWorldLocation();
		fwd = camOb.getWorldForwardVector();
		up = camOb.getWorldUpVector();
		right = camOb.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc);
	}
	
	/**
	 * 	A custom object used to allow the game HUD to not update until a specified time elapses.
	 */
	public class HUDCooldown{
		double cooldownTime;
		
		public HUDCooldown() {
			this.cooldownTime = 0;
		}
		/**
		 * sets the timer during which the HUD won't update.
		 * @param time The time in seconds before the HUD can update again.
		 */
		public void setCooldown(int time) {
		cooldownTime = elapsedTime + time;
		}
		/**
		 * Returns a boolean informing other functions whether the HUD may be updated.
		 * @return True of enough time has passed, false if not
		 */
		public boolean checkCooldown() {
			if (elapsedTime >= cooldownTime) return true;
			else return false;
		}
	}
	/**
	 * Tells other functions whether two GameObjects are a specified distance from each other.
	 * Makes use of the getDist function.
	 * @param o1 The first GameObject to compare.
	 * @param o2 The second GameObject to compare.
	 * @param dist The distance you'd like to check whether the two GameObjects are within.
	 * @return True if the two objects are closer then the specified distance, or false if they are not.
	 */
	public boolean checkDist(GameObject o1, GameObject o2, float dist) {
		float temp = getDist(o1, o2);
		if (temp <= dist) return true;
		else return false;
	}
	/**
	 * Tells other functions whether two GameObjects are a specified distance from each other.
	 * Makes use of the getDist function. This version uses Vec3 location data.
	 * @param loc1 Vec3 location 1
	 * @param loc2 Vec3 location 2
	 * @param dist The distance you'd like to check whether the two GameObjects are within.
	 * @return True if the two objects are closer then the specified distance, or false if they are not.
	 */
	public boolean checkDist(Vector3f loc1, Vector3f loc2, float dist) {
		float temp = getDist(loc1, loc2);
		if (temp <= dist) return true;
		else return false;
	}
	/**
	 * Returns the distance between two GameObjects.
	 * @param o1 The first GameObject to compare.
	 * @param o2 The second GameObject to compare.
	 * @return the distance between the two objects.
	 */
	public float getDist(GameObject o1, GameObject o2) {
		Vector3f loc1, loc2;
		float x1, y1, z1, x2, y2, z2;
		loc1 = o1.getWorldLocation();
		x1 = loc1.x(); y1 = loc1.y(); z1 = loc1.z();
		loc2 = o2.getWorldLocation();
		x2 = loc2.x(); y2 = loc2.y(); z2 = loc2.z();
		
		return (float) Math.sqrt(((x2-x1)*(x2-x1))+((y2-y1)*(y2-y1))+((z2-z1)*(z2-z1)));
	}
	/**
	 * Returns the distance between two Vec3 points.
	 * @param loc1 The first location to compare.
	 * @param loc2 The second location to compare.
	 * @return the distance between the two points.
	 */
	public float getDist(Vector3f loc1, Vector3f loc2) {
		float x1, y1, z1, x2, y2, z2;
		x1 = loc1.x(); y1 = loc1.y(); z1 = loc1.z();
		x2 = loc2.x(); y2 = loc2.y(); z2 = loc2.z();
		
		return (float) Math.sqrt(((x2-x1)*(x2-x1))+((y2-y1)*(y2-y1))+((z2-z1)*(z2-z1)));
	}
	
	/**
	 * update online connection status
	 * @param b connection status
	 */
	public void setConnected(boolean b) {
		isClientConnected = b;
	}
	
	public GameObject getAvatar() { return gBall; }
	public Vector3f getAvatarPosition() { return gBall.getWorldLocation(); }
	public ObjShape getGhostShape() { return gBallS; }
	public TextureImage getGhostTexture() { return gBallT2; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	public TextureImage getNPCTex() { return npcT; }
	public ObjShape getNPCShape() { return npcS; }
	
	/**
	 * function for 3d sound properties
	 */
	public void setEarParameters()
	{ Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
	audioMgr.getEar().setLocation(getAvatarPosition());
	audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}
	
	/**
	 * assistant function for physics calculation
	 * @param arr an array of doubles
	 * @return an array of floats
	 */
	private float[] toFloatArray(double[] arr) { 
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (float)arr[i];
		}
		return ret;
	}
	
	/**
	 * assistant function for physics calculation
	 * @param arr array of floats
	 * @return array of doubles
	 */
	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	/**
	 * function for network games to update the high score value
	 * @param newScore the new high score value to write
	 */
	public void setHScore(int newScore) {
		hScore = newScore;
	}
	
	/** 
	 * function used by NPC handler to get terrain ground coordinate
	 * @param x x-coordinate
	 * @param z z-coordinate
	 * @return y-coordinate of the terrain
	 */
	public float getGroundY(float x, float z) {
		float y = terr.getHeight(x, z);
		return y;
	}
	/**
	 * function used by client protocol to play sfx for avatars
	 * @param loc
	 */
	public void playNPCSound(Vector3f loc) {
		grassHitNPC.setLocation(loc);
		grassHitNPC.play();
	}
	
	/**
	 * function for physics calculations. Also plays the grass bounce sfx if something collides with the terrain.
	 */
	private void checkForCollisions() { 
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
		dynamicsWorld = ((JBulletPhysicsEngine)pEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		int groundID = groundP.getUID();
		for (int i=0; i<manifoldCount; i++) { 
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++){
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f) { 
				//	System.out.println("---- hit between " + obj1 + " and " + obj2);
					if (obj1.getUID() == groundID || obj2.getUID() == groundID) {
						if (grassBounce.buffer()) {
							grassHit.setLocation(getAvatarPosition());
							grassHit.play();
							if (isClientConnected) protClient.sendSound(getAvatarPosition());
						}
					}
					break;
				}
			}
		} 
	}
	/**
	 * updates the aiming line based on the aim angle
	 */
	private void setAimAngle() {
		float angleZ = (float) Math.cos(Math.toRadians(angle));
		float angleY = (float) Math.sin(Math.toRadians(angle));
		
		controlLine.setLocalRotation(new Matrix4f().setRotationXYZ(0.0f, angleY, angleZ));
		
	}
	/**
	 * function for updating the scene to aiming mode
	 */
	private void startAiming() {
		gBall.setLocalTranslation(postPos);
		Matrix4f translation = new Matrix4f(gBall.getLocalTranslation());
		double[] tempTransform = toDoubleArray(translation.get(vals));
		ballP.setTransform(tempTransform);
		ballP.setLinearVelocity(new float[] {0, 0, 0});
		physicsOn = false;
		angle = 0.0f;
		if (holeNum > totalNumHoles) {
			controlMode = 4;
			gBall.setLocalScale(new Matrix4f().scale(0.0f));
		}
		else {
			controlLine.setLocalScale(new Matrix4f().scale(1.0f));
			controlLine.setLocalRotation(new Matrix4f().setRotationXYZ(0f, 0f, 0f));
			controlMode = 0;
		}
	}
	/**
	 * function for updating the scene to power mode
	 */
	private void startPower() {
		// TODO
		controlMode = 1;
		power = 0.0f;
		tick = 0.5f;
	}

	private void startOffset() {
		// TODO
		controlMode = 2;
		offset = 0;
		tick = 0.2f;
	}
	/**
	 * function for hitting the ball
	 */
	private void startSwing() {
		controlMode = 3;
		controlLine.setLocalScale(new Matrix4f().scale(0.0f));
		// TODO: calculate XYZ force values
		float force = power * 30.0f;
		float angleZ = (float) Math.cos(Math.toRadians(angle));
		float angleX = (float) Math.sin(Math.toRadians(angle));
		float forceX = force * angleX;
		float forceZ = force * angleZ;
		physicsOn = true;
		ballP.applyForce(forceX, 0, forceZ, 0, 0, 0);
	}
	/**
	 * function for recording the player's score when the ball stops
	 */
	private void finalizeSwing() {
		physicsOn = false;
		float dist = getDist(gBall, flag);
		score += (int) dist;
		if (isClientConnected) protClient.sendScore(score, holeNum);
		holeNum++;
		startAiming();
	}
}