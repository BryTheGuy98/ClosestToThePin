package tage;

import org.joml.*;
import org.joml.Math;

/**
 * a client-side class for handling ghost NPCs
 */
public class GhostNPC extends GameObject {
	private int id;
	
	public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p) { 
		super(GameObject.root(), s, t);
		this.id = id;
		setPos(p);
	}
	
	public void setPos(Vector3f pos) { this.setLocalLocation(pos); }
	public Vector3f getPos() { return this.getWorldLocation(); }
	public void setID (int newID) { id = newID; }
	public int getID() { return id; }
	
	/**
	 * update the npc's rotation
	 * @param rot rotation angle (Y-axis)
	 */
	public void setRot(float rot) {
	    float radiansY = (float) Math.toRadians(rot + 180.0f);
	    Matrix4f rotationMatrix = new Matrix4f().setRotationXYZ(0f, radiansY, 0f);
	    this.setLocalRotation(rotationMatrix);
	}
}