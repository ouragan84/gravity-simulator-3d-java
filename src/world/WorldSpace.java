package world;

import java.awt.Color;
import java.util.*;

import entity.IEntity;
import entity.SphereGravity;
import entity.builder.BasicEntityBuilder;
import entity.builder.ComplexEntityBuilder;
import input.Keyboard;
import point.MyPoint;
import point.MyVector;
import point.PointConverter;
import shapes.LineSegment;

@SuppressWarnings("unused")
public class WorldSpace {
	
	// settings
	private static String method = "Heun";  // change to "Euler" or "Midpoint" or "Heun" or "Ralston"
	private static boolean randomInitialCondition = true; // change to true or false
	private static int numberOfRandomPlanets = 5;
	private static final Color[] colors = {Color.CYAN, Color.GREEN, Color.RED, Color.YELLOW, Color.ORANGE, Color.BLUE, Color.GRAY, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.green, Color.PINK};
	
	// IVP settings
	private static boolean stopWhenTimeReached = false; // change to true or false
	private static double InitialTime = 0.0;
	private static double TargetTime = 5.0;
	
	// trail settings
	private static int tracesPerPlanets = 120;
	private static int framesPerTraces = 2;
	private static double traceDecayFactor = 0.98d;
	
	// center of mass settings
	private static boolean showRealCenterOfMass = false; // change to true or false, the true center of mass is in magenta
	private static boolean showExpeectedCenterOfMass = false; // change to true or false, the expected center of mass is in white
	
	private EntityManager eManager;
	private Camera camera;
	private int FPS;
	private SphereGravity[] planets;
	public LinkedList<LineSegment[]> traces;
	private LineSegment[] currentTraces;
	private int framesCounter = 0;
	private double currentTime;
	private boolean reachedTime;
	
	// center of gravity
	private IEntity centerOfMass;
	private IEntity centerOfMassExpected;
	private MyVector COMExpextInitalPos;
	private MyVector initialVelocities;
	private double massTotal;
	
	public WorldSpace(EntityManager eManager, int fps) {
		this.eManager = eManager;
		this.FPS = fps;
	}

	public List<IEntity> Init() {

		currentTime = 0;
		reachedTime = false;

		// create camera
		this.camera = new ControlCamera(new MyPoint(1000, -200, 300), new MyVector(0, -20, -45), new MyVector(5, 5, 5), new MyVector(600, 600, 600), new MyVector(1000, 600, 600), FPS, eManager);
		PointConverter.camera = this.camera;
		
		List<IEntity> entities = new ArrayList<IEntity>();
		traces = new LinkedList<LineSegment[]>();
		
		entities.add(BasicEntityBuilder.createLine(new MyPoint(0,0,-1000), new MyPoint(0,0,1000), 40, new MyPoint(0,0,0), new MyVector(1, 1, 1), new MyVector(0, 0, 0), Color.WHITE, false));
		
		for(int i = -10; i <= 10; i++) {
			entities.add(BasicEntityBuilder.createLine(new MyPoint(i*100,-1000,0), new MyPoint(i*100,1000,0), 40, new MyPoint(0,0,0), new MyVector(1, 1, 1), new MyVector(0, 0, 0), Color.WHITE, false));
			entities.add(BasicEntityBuilder.createLine(new MyPoint(-1000,i*100,0), new MyPoint(1000,i*100,0), 40, new MyPoint(0,0,0), new MyVector(1, 1, 1), new MyVector(0, 0, 0), Color.WHITE, false));
		}
		
		if(randomInitialCondition) {
			planets = new SphereGravity[numberOfRandomPlanets];
			int[] rands = new int[7];
			Random r = new Random();
			for(int i = 0; i < numberOfRandomPlanets; i++) {
				for(int j = 0; j < 7; j++) {
					if (j==0) rands[j] = r.nextInt(3)+2;
					else if (j<=4) rands[j] = r.nextInt(9)*50-200;
					else rands[j] = r.nextInt(8)*100+100;
					System.out.println(rands[j]);
				}
				planets[i] = new SphereGravity(rands[0]*1e7, new MyVector(rands[1], rands[2], rands[3]), new MyPoint(rands[4], rands[5], rands[6]), colors[i], i);
			}
		}else {
			
			// change initial conditions here!!!   new SphereGravity(Mass, Velocity, Position, Color, Index),
			planets = new SphereGravity[]{
					new SphereGravity(4.5e7, new MyVector(0, -175, 0), new MyPoint(700, 500, 500), colors[0], 0),
					new SphereGravity(4.5e7, new MyVector(0, 175, 0), new MyPoint(300, 500, 500), colors[1], 1),
					};
		}
		
		System.out.println("Initial Conditions : ");
		printPlanets();
		
		currentTime = InitialTime;
		
		// center of mass setting
		if(showExpeectedCenterOfMass || showRealCenterOfMass) {
			initialVelocities = new MyVector();
			
			float x = 0,y = 0,z = 0;
			massTotal = 0;
			
			for(SphereGravity p : planets) {
				initialVelocities = initialVelocities.add(p.velocity.scale(p.Mass));
				x += p.position.x * p.Mass;
				y += p.position.y * p.Mass;
				z += p.position.z * p.Mass;
				massTotal += p.Mass;
			}
			
			MyPoint COMPos = new MyPoint(x/massTotal, y/massTotal, z/massTotal);
			initialVelocities = initialVelocities.scale(1/massTotal);
			System.out.println("\nInitial Position of the center of mass " + COMPos + " m\n");
			System.out.println("\nInitial Velocity of the center of mass " + initialVelocities + " m/s\n");
			
			if(showExpeectedCenterOfMass) {
				centerOfMassExpected = BasicEntityBuilder.createSphereType1(COMPos, COMPos, new MyVector(1, 1, 1), new MyVector(0, 0, 0), Color.WHITE, 20, 15);
				COMExpextInitalPos = new MyVector(x/massTotal, y/massTotal, z/massTotal);
				entities.add(centerOfMassExpected);
			}
			
			if(showRealCenterOfMass) {
				centerOfMass = BasicEntityBuilder.createSphereType1(new MyPoint(0, 0, 0), new MyPoint(0, 0, 0), new MyVector(1, 1, 1), new MyVector(0, 0, 0), Color.MAGENTA, 20, 15);
				entities.add(centerOfMass);
			}
		}
		
		for(SphereGravity p : planets) {
			entities.add(p.getSphere());
		}
		
		return entities;
	}
	
	public void Update() {

		Keyboard keyboard = this.eManager.getKeyboard();
		if(keyboard.getRestart()){
			this.Init();
			return;
		}
		
		if(!reachedTime && currentTime >= TargetTime) {
			System.out.println("\nTarget time reached : ");
			printPlanets();
			reachedTime = true;
		}
		
		double deltaT = 1.0d/FPS;
		currentTime += deltaT;
		
		if(!stopWhenTimeReached || !reachedTime) {
			
			createTrace();
			
			if(method == "Euler") {
				
				EulerMethod(deltaT);
				
			}else if(method == "Midpoint") {
				
				MidpointMethod(deltaT);
				
			}else if(method == "Heun") {
				
				HeunMethod(deltaT);
				
			}else if(method == "Ralston") {
				
				RalstonMethod(deltaT);
				
			}
			
			UpdateCenterOfMass(deltaT);
			
			finishTrace();
		}
		
		this.camera.update();
	}
	
	public void EulerMethod(double deltaT) {

		MyPoint[] InitialPositions = new MyPoint[planets.length];
		for (int i = 0; i < planets.length; i++){
			InitialPositions[i] = planets[i].position;
		}

		MyVector[] Accelerations = MyVector.initArray(new MyVector[planets.length]);
		
		// loop through all combinations of planets
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate distance and common gravity factor
				MyVector distance = new MyVector( planets[i].position, planets[j].position );
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				
				// add to the acceleration of the bodies
				Accelerations[i] = Accelerations[i].add( gravityFactor.scale( planets[j].Mass ) );
				Accelerations[j] = Accelerations[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
	
		// loop once again through each planet to add to velocity and position (and include gravitational constant G)
		for (int i = 0; i < planets.length; i++) {
			planets[i].velocity = planets[i].velocity.add( Accelerations[i].scale( SphereGravity.gravConst ) );
			planets[i].position = planets[i].position.add( planets[i].velocity.scale( deltaT ) );
			planets[i].updateWorldPosition();
		}

		handleCollisions(InitialPositions);
	}
	
	public void MidpointMethod(double deltaT) {

		MyPoint[] InitialPositions = new MyPoint[planets.length];
		for (int i = 0; i < planets.length; i++){
			InitialPositions[i] = planets[i].position;
		}

		MyVector[] AccelerationsInitial = MyVector.initArray(new MyVector[planets.length]);
		MyPoint[] MidPointPositions = MyPoint.initArray(new MyPoint[planets.length]);
		MyVector[] AccelerationsMidpoint = MyVector.initArray(new MyVector[planets.length]);
		
		// loop through all combinations of planets
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate distance and common gravity factor
				MyVector distance = new MyVector( planets[i].position, planets[j].position );
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				
				// add to the acceleration of the bodies
				AccelerationsInitial[i] = AccelerationsInitial[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsInitial[j] = AccelerationsInitial[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// calculating where the planets would be in deltaT/2 seconds
		for (int i = 0; i < planets.length; i++) {
			MyVector velocity = planets[i].velocity.add( AccelerationsInitial[i].scale( SphereGravity.gravConst ) );
			MidPointPositions[i] = planets[i].position.add( velocity.scale( deltaT/2 ) );
		}
		
		// loop through all combinations of planets again
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate acceleration in planets' midpoint position
				MyVector distance = new MyVector( MidPointPositions[i], MidPointPositions[j]);
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				AccelerationsMidpoint[i] = AccelerationsMidpoint[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsMidpoint[j] = AccelerationsMidpoint[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// loop once again to apply the change in velocity/position
		for (int i = 0; i < planets.length; i++) {
			planets[i].velocity = planets[i].velocity.add( AccelerationsMidpoint[i].scale( SphereGravity.gravConst ) );
			planets[i].position = planets[i].position.add( planets[i].velocity.scale( deltaT ) );
			planets[i].updateWorldPosition();
		}

		handleCollisions(InitialPositions);
	}
	
	public void HeunMethod(double deltaT) {

		MyPoint[] InitialPositions = new MyPoint[planets.length];
		for (int i = 0; i < planets.length; i++){
			InitialPositions[i] = planets[i].position;
		}

		MyVector[] AccelerationsInitial = MyVector.initArray(new MyVector[planets.length]);
		MyPoint[] EndPointPositions = MyPoint.initArray(new MyPoint[planets.length]);
		MyVector[] AccelerationsEndPoint = MyVector.initArray(new MyVector[planets.length]);
		
		// loop through all combinations of planets
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate distance and common gravity factor
				MyVector distance = new MyVector( planets[i].position, planets[j].position );
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				
				// add to the acceleration of the bodies
				AccelerationsInitial[i] = AccelerationsInitial[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsInitial[j] = AccelerationsInitial[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// calculating where the planets would be in deltaT seconds
		for (int i = 0; i < planets.length; i++) {
			MyVector velocity = planets[i].velocity.add( AccelerationsInitial[i].scale( SphereGravity.gravConst ) );
			EndPointPositions[i] = planets[i].position.add( velocity.scale( deltaT ) );
		}
		
		// loop through all combinations of planets again
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate acceleration in planets' endPoint position
				MyVector distance = new MyVector( EndPointPositions[i], EndPointPositions[j]);
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				AccelerationsEndPoint[i] = AccelerationsEndPoint[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsEndPoint[j] = AccelerationsEndPoint[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// loop once again to apply the change in velocity/position
		for (int i = 0; i < planets.length; i++) {
			planets[i].velocity = planets[i].velocity.add( (( AccelerationsInitial[i].scale(.5) ).add( AccelerationsEndPoint[i].scale(.5)) ).scale( SphereGravity.gravConst ) );
			planets[i].position = planets[i].position.add( planets[i].velocity.scale( deltaT ) );
			planets[i].updateWorldPosition();
		}

		handleCollisions(InitialPositions);
	}
	
	public void RalstonMethod(double deltaT) {

		MyPoint[] InitialPositions = new MyPoint[planets.length];
		for (int i = 0; i < planets.length; i++){
			InitialPositions[i] = planets[i].position;
		}

		MyVector[] AccelerationsInitial = MyVector.initArray(new MyVector[planets.length]);
		MyPoint[] ThreeFourthPointPositions = MyPoint.initArray(new MyPoint[planets.length]);
		MyVector[] AccelerationsThreeFourthPoint = MyVector.initArray(new MyVector[planets.length]);
		
		// loop through all combinations of planets
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate distance and common gravity factor
				MyVector distance = new MyVector( planets[i].position, planets[j].position );
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				
				// add to the acceleration of the bodies
				AccelerationsInitial[i] = AccelerationsInitial[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsInitial[j] = AccelerationsInitial[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// calculating where the planets would be in 2/3*deltaT seconds
		for (int i = 0; i < planets.length; i++) {
			MyVector velocity = planets[i].velocity.add( AccelerationsInitial[i].scale( SphereGravity.gravConst ) );
			ThreeFourthPointPositions[i] = planets[i].position.add( velocity.scale( .75 * deltaT ) );
		}
		
		// loop through all combinations of planets again
		for (int i = 0; i < planets.length; i++) {
			for (int j = i+1; j < planets.length; j++ ) {
				// calculate acceleration in planets' 2/3 point position
				MyVector distance = new MyVector( ThreeFourthPointPositions[i], ThreeFourthPointPositions[j]);
				MyVector gravityFactor = distance.scale( 1 / Math.pow( distance.magnitude(), 3.0d ) );
				AccelerationsThreeFourthPoint[i] = AccelerationsThreeFourthPoint[i].add( gravityFactor.scale( planets[j].Mass ) );
				AccelerationsThreeFourthPoint[j] = AccelerationsThreeFourthPoint[j].add( gravityFactor.scale( - planets[i].Mass ) );
			}
		}
		
		// loop once again to apply the change in velocity/position
		for (int i = 0; i < planets.length; i++) {
			planets[i].velocity = planets[i].velocity.add( (( AccelerationsInitial[i].scale(1.0/3) ).add( AccelerationsThreeFourthPoint[i].scale(2.0/3)) ).scale( SphereGravity.gravConst ) );
			planets[i].position = planets[i].position.add( planets[i].velocity.scale( deltaT ) );
			planets[i].updateWorldPosition();
		}

		handleCollisions(InitialPositions);
	}

	public void handleCollisions(MyPoint[] InitialPositions){
		for (int i = 0; i < planets.length; i++) {
			for(int j = i+1; j < planets.length; j++){
				if(MyPoint.dist(planets[i].position, planets[j].position) < planets[i].radius + planets[j].radius){
					//collision
//					System.out.println("COLLISION");

					MyVector v1 = planets[i].velocity;
					MyVector v2 = planets[j].velocity;

					double m1 = planets[i].Mass;
					double m2 = planets[j].Mass;

					planets[i].velocity = MyVector.add( MyVector.scale(v1, (m1-m2)/(m1+m2)), MyVector.scale(v2, 2*m2/(m1+m2)) );
					planets[j].velocity = MyVector.add( MyVector.scale(v2, (m2-m1)/(m1+m2)), MyVector.scale(v1, 2*m1/(m1+m2)) );

					planets[i].position = InitialPositions[i];
					planets[j].position = InitialPositions[j];
				}
			}
		}
	}
	
	public void createTrace() {
		if(framesCounter > 0) {
			return;
		}
		
		currentTraces = new LineSegment[planets.length];
		for(int i = 0; i < currentTraces.length; i++) {
			currentTraces[i] = new LineSegment(planets[i].position, new MyPoint());
			currentTraces[i].setColor(planets[i].color);
		}
	}
	
	public void finishTrace() {
		if(framesCounter < framesPerTraces) {
			framesCounter++;
			return;
		}
		framesCounter = 0;
		
		if(traces.size() > tracesPerPlanets-1)
			traces.removeFirst();
		
		for(LineSegment[] lines: traces) {
			for(LineSegment line: lines) {
				Color c = line.getColor();
				int red = (int) (c.getRed() * traceDecayFactor);
				int blue = (int) (c.getBlue() * traceDecayFactor);
				int green = (int) (c.getGreen() * traceDecayFactor);
				line.setColor(new Color(red, green, blue));
			}
		}
		
		for(int i = 0; i < planets.length; i++) {
			currentTraces[i].setEndPoint(planets[i].position);
		}
		
		traces.addLast(currentTraces);
	}
	
	public void UpdateCenterOfMass(double deltaT) {
		if(showExpeectedCenterOfMass) {
			MyVector NewCOM = COMExpextInitalPos.add(initialVelocities.scale(currentTime));
			centerOfMassExpected.setPos(NewCOM.x, NewCOM.y, NewCOM.z);
		}
		
		if(showRealCenterOfMass) {
			float x = 0,y = 0,z = 0;
			
			for(SphereGravity p : planets) {			
				x += p.position.x * p.Mass;
				y += p.position.y * p.Mass;
				z += p.position.z * p.Mass;
			}
			centerOfMass.setPos(x/massTotal, y/massTotal, z/massTotal);
		}
	}
	
	private void printPlanets() { 
		System.out.println("Planet#     Mass(kg)                Position(m)                 Velocity(m/s)");
		for(SphereGravity p: planets) {
			System.out.println(p.entityIndex + "  " + p.Mass + "  " + p.position + "  " + p.velocity);
		}
	}
	
	public Camera getWorldCamera() {
		return this.camera;
	}
}
