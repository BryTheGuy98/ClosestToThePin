package client;

import java.io.IOException;
import java.util.*;
import org.joml.*;
import tage.*;

/**
 * a client-side class for managing ghost avatars of other players
 */
public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvs = new Vector<GhostAvatar>();
	
	public GhostManager(VariableFrameRateGame vfrg) { game = (MyGame)vfrg; }
	/**
	 * function to add a ghost avatar to the vector
	 * @param id avatar id
	 * @param p avatar position
	 * @throws IOException error handling
	 */
	public void createGhost(UUID id, Vector3f p) throws IOException { 
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
		Matrix4f initialScale = (new Matrix4f()).scaling(1.0f);
		newAvatar.setLocalScale(initialScale);
		ghostAvs.add(newAvatar);
	}
	/**
	 * remove an avatar from the vector
	 * @param id id of avatar to be removed
	 */
	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAv = findAvatar(id);
		if(ghostAv != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostAv);
			ghostAvs.remove(ghostAv);
		}
		else { System.out.println("unable to find ghost in list"); }
	}
	/**
	 * search the vector for a specific avatar based on id
	 * @param id avatar id to find
	 * @return GhostAvatar object if found, null if not
	 */
	private GhostAvatar findAvatar(UUID id) { 
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvs.iterator();
		while(it.hasNext()) {
			ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0) { 
				return ghostAvatar;
			}
		}
	return null;
	}
	/**
	 * confirms if a avatar exists in the vector
	 * @param id avatar id to find
	 * @return true if found, false if not
	 */
	public boolean ghostExists(UUID id) {
		GhostAvatar tempAv;
		Iterator<GhostAvatar> it = ghostAvs.iterator();
		while(it.hasNext()) {
			tempAv = it.next();
			if(tempAv.getID().compareTo(id) == 0) { 
				return true;
			}
		}
	return false;
	}
	/**
	 * update avatar position
	 * @param id id of avatar to update
	 * @param position new position
	 */
	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) { ghostAvatar.setPos(position); }
		else { System.out.println("unable to find ghost in list"); }
	}
}