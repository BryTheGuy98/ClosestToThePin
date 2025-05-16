package client;

/**
 * this class is used to disable a sound effect if it already recently played
 */
public class SoundBuffer {
	long prevTime;
	int timeBuffer;
	
	public SoundBuffer(int tb) {
		prevTime = 0;
		timeBuffer = tb;
	}
	
	/**
	 * function to check if enough time has passed to play a sound
	 * @return indicates whether the sound can be played or not
	 */
	public boolean buffer() {
		long currTime = System.currentTimeMillis();
		boolean out = false;
		if (currTime - prevTime > timeBuffer) {
			out = true;
		}
		prevTime = currTime;
		return out;
	}
}