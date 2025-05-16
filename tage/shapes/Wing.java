package tage.shapes;

import org.joml.Vector2f;
import org.joml.Vector3f;

import tage.Utils;
import tage.shapes.ManualObject;

/**
 * 	A custom ManualObject object to create a low-poly "wing".
 */
public class Wing extends ManualObject{
		private Vector3f[] v = new Vector3f[14];
		private Vector2f[] tx = new Vector2f[14];
		private Vector3f[] n = new Vector3f[14];
		private int[] in = new int[] {
				// Section A
					0, 2, 3, 	0, 1, 2, 	2, 6, 7,	2, 3, 7,	0, 1, 4,	1, 5, 4,	1, 6, 5, 	1, 2, 6, 	0, 3, 6, 	0, 4, 6,
				// Section B
					5, 8, 10,	5, 6, 10,	6, 10, 11,	6, 7, 11,	5, 4, 8,	5, 8, 9,
				// Section C
					9, 10, 13,	8, 11, 12,	10, 11, 12,	10, 13, 12,	9, 8, 12,	9, 13, 12
		};
		/**
		 *  constructor
		 */
		public Wing() {
			super();
				//	Section A
					v[0] = new Vector3f().set(0, 0, 0); v[1] = new Vector3f().set(0, .25f, 0);	v[2] = new Vector3f().set(0, .25f, -1); v[3] = new Vector3f().set(0, 0, -1);
				// Section B
					v[4] = new Vector3f().set(1.5f, 0, 0);	v[5] = new Vector3f().set(1.5f, .25f, 0);	v[6] = new Vector3f().set(1.5f, .25f, -1);	v[7] = new Vector3f().set(1.5f, 0, -1);
				// Section C
					v[8] = new Vector3f().set(2.5f, 0, -.75f); v[9] = new Vector3f().set(2.5f, .25f, -.75f);	v[10] = new Vector3f().set(2.5f, .25f, -1.5f);	v[11] = new Vector3f().set(2.5f, 0, -1.5f);
				// Tip
					v[12] = new Vector3f().set(3, 0, -2);	v[13] = new Vector3f().set(3, .25f, -2);
				
				
					for (int i = 0; i < tx.length; ++i) {
					tx[i] = new Vector2f().set(0, 0);
					i++;
					tx[i] = new Vector2f().set(1, 1);	}
				
					for (int i = 0; i < n.length; ++i) { n[i] = new Vector3f().set(0, 0, 1); }
					
				setNumVertices(in.length);
				setVerticesIndexed(in, v);
				setTexCoordsIndexed(in, tx);
				setNormalsIndexed(in, n);
				
				setMatAmb(Utils.goldAmbient());
				setMatDif(Utils.goldDiffuse());
				setMatSpe(Utils.goldSpecular());
				setMatShi(Utils.goldShininess());
		}
				
	}