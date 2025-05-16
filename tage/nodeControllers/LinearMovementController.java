package tage.nodeControllers;

import tage.*;
import org.joml.*;

/**
 *  A custom NodeController for making an object move continuously in one direction.
 */
public class LinearMovementController extends NodeController {
	private float moveX = 0.0f;
	private float moveY = 0.10f;
	private float moveZ = 0.0f;
	private long timer = System.currentTimeMillis();
	private Vector3f currLoc, newLoc, up, right, fwd;
	private Engine e;
	
	public LinearMovementController() {
		super();
	}
	public LinearMovementController(Engine engine, float x, float y, float z) {
		super();
		moveX = x;
		moveY = y;
		moveZ = z;
		e = engine;
	}
	public LinearMovementController(Engine engine) {
		super();
		e = engine;
	}
	/**
	 * sets a delay before the movement starts
	 * @param time time (in seconds) to delay the movement
	 */
	public void setTimer(float time) {
		timer = System.currentTimeMillis() + (long) (time * 1000.0f);
	}

	@Override
	public void apply(GameObject go) {
		if (System.currentTimeMillis() >= timer) {
			currLoc = go.getWorldLocation();
			up = go.getWorldUpVector();
			right = go.getWorldRightVector();
			fwd = go.getWorldForwardVector();
			newLoc = currLoc.add(up.mul(moveY).add(right.mul(moveX)).add(fwd.mul(moveZ)));		
			go.setLocalLocation(newLoc);
		}
	}
	
}