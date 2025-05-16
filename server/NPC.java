package server;

import org.joml.Random;

/**
 * a server-side class for managing NPCs
 */
public class NPC { 
	double locationX, locationY, locationZ;
	double dir = 0.1;
	double size = 1.0;
	double azRot;
	boolean flying = false;
	boolean walking = false;
	long flyingStartTime;
	
	public NPC() { 
		locationX=0.0;
		locationY=0.0;
		locationZ=0.0;
		azRot = 0.0;
	}
	
	/**
	 * function to randomize the NPC's starting location
	 * @param seedX
	 * @param seedZ
	 */
	public void randomizeLocation(int seedX, int seedZ) {
		locationX = ((double)seedX)/4.0 - 5.0;
		locationY = 0;
		locationZ = -2;
	}
	
	/**
	 * update the NPCs location based on the current status
	 */
	public void updateLocation() { 
		if (flying || walking) {
			double rads = Math.toRadians(azRot);
			double dx = Math.sin(rads); 
			double dz = Math.cos(rads); 
        
			locationX += dx;
        	locationZ += dz;
        	
        	if (flying) {
        		long currTime = System.currentTimeMillis();
        		double time = (double) (currTime - flyingStartTime) / 1000.0;
			
        		locationY += (time * 1.5);
        	}
		}
	}
	
	public double getX() { return locationX; }
	public double getY() { return locationY; }
	public double getZ() { return locationZ; }
	public void setRot(double r) { azRot = r; }
	public double getRot() { return azRot; }
	public boolean isFlying() { return flying; }
	public void toggleWalking() { walking = !walking; }
	public boolean isWalking() { return walking; }
	
	/**
	 * setup variables for when the AI state changes to "flying"
	 */
	public void startFlying() { 
		flying = true;
		flyingStartTime = System.currentTimeMillis();
		}
	
	/**
	 * function to randomize rotation
	 */
	public void randomRot() {
		Random r = new Random();
		double newRot = r.nextInt(360);
		azRot = newRot;
	}
}