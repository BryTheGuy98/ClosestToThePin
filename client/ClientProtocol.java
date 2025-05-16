package client;

import tage.GhostNPC;
import tage.networking.client.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import org.joml.Vector3f;
/**
 * a protocol for handling server communication, as well as ghost avatar and NPC management
 */
public class ClientProtocol extends GameConnectionClient{
	private MyGame game;
	private UUID id;
	private GhostManager gManager;
	private GhostNPC gNPC;
	private final float closeDist = 15.0f;
	
	public ClientProtocol(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		gManager = game.getGhostManager();
	}
	/**
	 * function for creating NPC
	 * @param pos starting position
	 * @param rot starting rotation
	 * @throws IOException error handling
	 */
	public void createNPC (Vector3f pos, float rot) throws IOException {
		if (gNPC == null) {
			gNPC = new GhostNPC(0, game.getNPCShape(), game.getNPCTex(), pos);
			gNPC.setRot(rot);
		}
	}
	/**
	 * function for moving NPC
	 * @param pos new position
	 * @param rot new rotation
	 */
	public void updateNPC (Vector3f pos, float rot) {
		if (gNPC == null) { 
			try { createNPC(pos, rot); }
			catch (IOException e) { System.out.println("error creating npc"); }
		}
		gNPC.setPos(pos);
		gNPC.setRot(rot);
	}
	
	@Override
	protected void processPacket(Object msg) { 
		String strMessage = (String) msg;
		String[] msgTokens = strMessage.split(",");
		if(msgTokens.length > 0) {
			if(msgTokens[0].compareTo("join") == 0) 
				{ // format: join, success or join, failure
				if(msgTokens[1].compareTo("success") == 0) { 
					game.setConnected(true);
					System.out.println("Connected to Server");
					sendCreateMessage(game.getAvatarPosition());
					sendWantsAvtDetails(id);
				}
				else if(msgTokens[1].compareTo("failure") == 0) { game.setConnected(false); }
			}		
			if(msgTokens[0].compareTo("bye") == 0) 
				{ // format: bye, remoteId
				UUID ghostID = UUID.fromString(msgTokens[1]);
				gManager.removeGhostAvatar(ghostID);
				}
			if ((msgTokens[0].compareTo("sndAvtD") == 0 ) 
					|| (msgTokens[0].compareTo("create")==0))
						{ // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
						UUID ghostID = UUID.fromString(msgTokens[1]);
						Vector3f ghostPosition = new Vector3f(
								Float.parseFloat(msgTokens[2]),
								Float.parseFloat(msgTokens[3]),
								Float.parseFloat(msgTokens[4]));
						try { gManager.createGhost(ghostID, ghostPosition); }
						catch (IOException e) { System.out.println("error creating ghost avatar"); }
						}
			if(msgTokens[0].compareTo("avtD") == 0) 
			{ // format: avtD,targetID
				UUID targetID = UUID.fromString(msgTokens[1]);
				sendAvatarDetails(targetID, game.getAvatarPosition());
			}
			if(msgTokens[0].compareTo("move") == 0) 
			{	UUID ghostID = UUID.fromString(msgTokens[1]);
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]),
						Float.parseFloat(msgTokens[4]));
				gManager.updateGhostAvatar(ghostID, ghostPosition);
			}
			if (msgTokens[0].compareTo("createNPC") == 0) { 
				// create a new ghost NPC
				// Parse out the position
				float x = Float.parseFloat(msgTokens[1]);
				float y = Float.parseFloat(msgTokens[2]);
				float z = Float.parseFloat(msgTokens[3]);
				float rot = Float.parseFloat(msgTokens[4]);
				y = y + game.getGroundY(x, z);
				Vector3f ghostPosition = new Vector3f(x, y, z);
				try{ createNPC(ghostPosition, rot); }
				catch (IOException e) { } // error creating ghost avatar
			}
			if (msgTokens[0].compareTo("moveNPC") == 0) { 
				// create a new ghost NPC
				// Parse out the position
				float x = Float.parseFloat(msgTokens[1]);
				float y = Float.parseFloat(msgTokens[2]);
				float z = Float.parseFloat(msgTokens[3]);
				float rot = Float.parseFloat(msgTokens[4]);
				y = y + game.getGroundY(x, z);
				Vector3f ghostPosition = new Vector3f(x, y, z);
				updateNPC(ghostPosition, rot);
			}
			if (msgTokens[0].compareTo("checkNear") == 0) { 
	//			System.out.println("checking NPC distance...");
				checkNPCDist();
			}
			if (msgTokens[0].compareTo("hscore") == 0) { 
				int score = Integer.parseInt(msgTokens[1]);
				game.setHScore(score);
			}
			if (msgTokens[0].compareTo("playSound") == 0) {
				Vector3f position = new Vector3f(
						Float.parseFloat(msgTokens[1]),
						Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]));
				game.playNPCSound(position);
			}
		}
	}
	/**
	 * function for joining server
	 */
	public void sendJoinMessage() // format: join, localId
	{
		try { sendPacket(new String("join," + id.toString())); }
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * function for creating a ghost avatar within other players games
	 * @param pos initial position
	 */
	public void sendCreateMessage(Vector3f pos)
	{ // format: (create, localId, x,y,z)
	try {	String message = new String("create," + id.toString());
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
	catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * function for disconnecting from server
	 */
	public void sendByeMessage() {
		// format: (bye, clientid)
		try { String message = new String("bye," + id.toString());
			sendPacket(message);
		} catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * function for when another player requests avatar data to create a ghost avatar
	 * @param targetID recipient ID
	 * @param pos avatar location
	 */
	public void sendAvatarDetails(UUID targetID, Vector3f pos) {
		// format: sndAvtD,thisID,targetID,x,y,z
		try {	String message = new String("sndAvtD," + id.toString());
			message += "," + targetID.toString();
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send a request for other player's avatar details to create ghost avatars
	 * @param thisID client ID
	 */
	public void sendWantsAvtDetails(UUID thisID) {
		try { String message = new String("avdD," + id.toString());
		sendPacket(message);
	} catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send new avatar location
	 * @param pos new location
	 */
	public void sendMoveMessage(Vector3f pos) {
		 // format: (move, localId, x,y,z)
		try {	String message = new String("move," + id.toString());
		message += "," + pos.x()+"," + pos.y() + "," + pos.z();
		sendPacket(message);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * check if the client avatar is close enough to the NPC to trigger its AI
	 */
	public void checkNPCDist() {
		Vector3f avtPos = game.getAvatarPosition();
		Vector3f npcPos = gNPC.getPos();
		if (game.checkDist(avtPos, npcPos, closeDist)) {
			try {	String message = new String("isNear," + id.toString());
		//	System.out.println("Sending NPC distance check...");
					sendPacket(message);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	/**
	 * send score to server
	 * @param score client score
	 * @param holeNum client hole number
	 */
	public void sendScore(int score, int holeNum) {
		try {	String message = new String("score");
		message += "," + score;
		message += "," + holeNum;
		sendPacket(message);
	} catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send coordinates to play sfx for avatars
	 * @param pos sound coordinates
	 */
	public void sendSound(Vector3f pos) {
		try {	String message = new String("sound," + id.toString());
		message += "," + pos.x()+"," + pos.y() + "," + pos.z();
		sendPacket(message);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
}