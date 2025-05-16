package tage;

import org.joml.Vector3f;

/**
 * a custom class to help check if an object is within a specific zone in 3d space
 */
public class Zone {
	private float topX, bottomX, topY, bottomY, topZ, bottomZ;
	
	public Zone(float tx, float bx, float ty, float by, float tz, float bz) {
		topX = tx;
		bottomX = bx;
		topY = ty;
		bottomY = by;
		topZ = tz;
		bottomZ = bz;
	}
	
	/**
	 * function to check if a point is inside the zone
	 * @param point point to check
	 * @return true if the point is inside the zone, false if not
	 */
	public boolean isInZone (Vector3f point) {
		float x = point.x();
		float y = point.y();
		float z = point.z();
		
		if ((x > bottomX && x < topX) && (y > bottomY && y < topY) && (z > bottomZ && z < topZ)) return true;
		else return false;
	}
	
	public float getTopX() { return topX; }
	public float getBottomX() { return bottomX; }
	public float getTopY() { return topY; }
	public float getBottomY() { return bottomY; }
	public float getTopZ() { return topZ; }
	public float getBottomZ() { return bottomZ; }
}