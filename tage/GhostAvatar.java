package tage;

import java.util.UUID;
import org.joml.*;

public class GhostAvatar extends GameObject {
	private UUID id;
	
	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) { 
		super(GameObject.root(), s, t);
		this.id = id;
		this.setPos(p);
	}
	
	public void setPos(Vector3f pos) {
		this.setLocalLocation(pos);
	}
	
	public Vector3f getPos() {
		return this.getWorldLocation();
	}
	
	public void setID (UUID newID) {
		id = newID;
	}
	
	public UUID getID() {
		return id;
	}
}