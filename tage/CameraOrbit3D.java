package tage;

import tage.*;
import java.lang.Math;
import org.joml.*;
/**
 *  A custom object used to ensure the game's camera remains in an orbiting position around the dolphin
 */
public class CameraOrbit3D {
	private Engine e;
	private GameObject camOb;
	private GameObject av;
	private float camXZ;	// 
	private float camY;
	private float camZoom;
	
	/**
	 * constructor
	 * @param engine the game engine object
	 * @param cOb the camera object
	 * @param avatar the "avatar" the camera will orbit around
	 */
	public CameraOrbit3D(Engine engine, GameObject cOb, GameObject avatar) {
		e = engine;
		camOb = cOb;
		av = avatar;
		camXZ = 180.0f;
		camY = 20.0f;
		camZoom = 10.0f;
	}
	
	/**
	 *  calculates the xyz coordinates for the camera based on the local rotation and zoom values
	 */
	public void updateCamPosition() {
	/*
	Vector3f avRot = av.getWorldForwardVector();
	double avAngle = Math.toDegrees((double) avRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));
	float totalAz = camXZ - (float)avAngle; */
	float totalAz = camXZ;
	double theta = Math.toRadians(totalAz);
	double phi = Math.toRadians(camY);
	float x = camZoom * (float)(Math.cos(phi) * Math.sin(theta));
	float y = camZoom * (float)(Math.sin(phi));
	float z = camZoom * (float)(Math.cos(phi) * Math.cos(theta));
	camOb.setLocalLocation(new Vector3f(x,y,z).add(av.getWorldLocation()));
	camOb.lookAt(av);
	}
	
	/**
	 * a function to change the X/Z rotation of the camera
	 * @param amt the amount to adjust the rotation
	 */
	public void xzRot(float amt) {
		camXZ += amt;
		camXZ = camXZ %= 360.0f;
		updateCamPosition();
	}
	
	/**
	 *  a function to change the elevation of the camera
	 * @param amt the amount to adjust the elevation
	 */
	public void yRot(float amt) {
		camY += amt;
		if (camY > 80.0f) {
			camY = 80.0f;
		}
		else if (camY < -80.0f){
			camY = -80.0f;
		}
		updateCamPosition();
	}
	/**
	 * a function to change the zoom level of the camera
	 * @param amt the amount to adjust the zoom
	 */
	public void zoom(float amt) {
		camZoom += amt;
		if (camZoom < 3.0f) {
			camZoom = 3.0f;
		}
		else if (camZoom > 30.0f) {
			camZoom = 30.0f;
		}
		updateCamPosition();
	}
	
	public float getCamXZ() { return camXZ; }
	public float getCamY() { return camY; }
}