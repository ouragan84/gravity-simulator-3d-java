package entity;

import java.awt.Color;

import entity.builder.BasicEntityBuilder;
import point.MyPoint;
import point.MyVector;

public class SphereGravity {
	
	public double Mass;
	public MyVector velocity;
	public MyPoint position;
	public Color color;
	public int entityIndex;
	
	private IEntity Sphere;
	public double radius;
	
	public static final double density = 5515.0d;//kg/m^3
	public static final double gravConst = 9e-3;
	
	public SphereGravity(double mass, MyVector initV, MyPoint initP, Color color, int index) {
		this.Mass = mass;
		this.position = initP;
		this.color = color;
		this.entityIndex = index;
		this.velocity = initV;
		this.radius = Math.pow(3*Mass/(density), 1.0/3);
		this.Sphere = BasicEntityBuilder.createSphereType1(this.position, this.position, new MyVector(1, 1, 1), new MyVector(0, 0, 0), this.color, radius, 15);
	}
	
	public IEntity getSphere() {
		return this.Sphere;
	}
	
	public void setPos(double x, double y, double z) {
		this.position.x = x;
		this.position.y = y;
		this.position.z = z;
		
		this.updateWorldPosition();
	}
	
	public void setVelocity(double x, double y, double z) {
		this.velocity.x = x;
		this.velocity.y = y;
		this.velocity.z = z;
	}

	public void updateWorldPosition() {
		this.Sphere.setPos(position.x, position.y, position.z);
	}

}