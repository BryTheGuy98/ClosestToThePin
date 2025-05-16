package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import org.joml.*;
import tage.networking.server.*;

/**
 * connects multiple clients together for multiplayer games. Also controls NPC AI.
 */
public class Server extends GameConnectionServer<UUID> { 
	int numPlayers = 0;
	int hScore = 0;
	int holeNum = 0;
	private NPC npc;
	Random rn = new Random();
	long thinkStartTime, tickStartTime;
	long lastThinkUpdateTime, lastTickUpdateTime;
	double criteria = 2.0;
	boolean nearNPC;
	
	public Server(int localPort) throws IOException { 
		super(localPort, ProtocolType.UDP);
		startNPCcontroller();
		}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int sndPort) {
		String message = (String) o;
		String[] msgTokens = message.split(",");
		if(msgTokens.length > 0) {
			// case where server receives a JOIN message
			// format: join,localid
			if(msgTokens[0].compareTo("join") == 0) {
				try{ 	IClientInfo ci;
						ci = getServerSocket().createClientInfo(senderIP, sndPort);
						UUID clientID = UUID.fromString(msgTokens[1]);
						addClient(ci, clientID);
						numPlayers++;
						sendJoinedMessage(clientID, true);
						sendCreateNPC(clientID);
						if (hScore != 0) {
							sendHScore(clientID);
						}
					}
				catch (IOException e){ e.printStackTrace(); }
			}
			// case where server receives a CREATE message
			// format: create,localid,x,y,z
			if(msgTokens[0].compareTo("create") == 0 && numPlayers > 1) { 
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				sendCreateMessages(clientID, pos);
			}
			// case where server receives a BYE message
			// format: bye,localid
			if(msgTokens[0].compareTo("bye") == 0) { 
				UUID clientID = UUID.fromString(msgTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
				numPlayers--;
			}
			// request avatar details
			if(msgTokens[0].compareTo("avdD") == 0 && numPlayers > 1) { 
				UUID sourceID = UUID.fromString(msgTokens[1]);
				wantsAvtDetails(sourceID);
			}
			// case where server receives a DETAILS-FOR message
			// format: sndAvtD,thisID,targetID,x,y,z
			if(msgTokens[0].compareTo("sndAvtD") == 0) { 
				UUID sourceID = UUID.fromString(msgTokens[1]);
				UUID targetID = UUID.fromString(msgTokens[2]);
				String[] pos = {msgTokens[3], msgTokens[4], msgTokens[5]};
				sendAvtDetails(sourceID, targetID, pos);
			}
			// case where server receives a MOVE message
			if(msgTokens[0].compareTo("move") == 0 && numPlayers > 1) { 
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				sendMoveMessages(clientID, pos);
			}
			if(msgTokens[0].compareTo("isNear") == 0) { 
		//		System.out.println("Recieved npc distance check");
				nearNPC = true;
			}
			if(msgTokens[0].compareTo("score") == 0) { 
				int score = Integer.parseInt(msgTokens[1]);
				int swingNum = Integer.parseInt(msgTokens[2]);
				if (swingNum > holeNum) {
					holeNum = swingNum;
					hScore = score;
					sendHScoreAll();
				}
				else if (swingNum == holeNum && score < hScore) {
					hScore = score;
					sendHScoreAll();
				}
			}
			if(msgTokens[0].compareTo("sound") == 0) { 
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				sendSound(clientID, pos);
			}
		}
	}
	/**
	 * responds to a client whether they've connected successfully
	 * @param clientID target client ID
	 * @param success connection status
	 */
	public void sendJoinedMessage(UUID clientID, boolean success) { 
		// format: join, success or join, failure
		try { 	String message = new String("join,");
				if (success) {
					message += "success";
					System.out.println("Client " + clientID.toString() + " joined the server");
				}
				else {
					message += "failure";
					System.out.println("Client " + clientID.toString() + " failed to join the server");
				}
				sendPacket(message, clientID);
			}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * sends a message to all connected clients to create a new ghost avatar
	 * @param clientID id of new player (used to block sending the message to them)
	 * @param position avatar location
	 */
	public void sendCreateMessages(UUID clientID, String[] position) { 
		// format: create, remoteId, x, y, z
		try { 	String message = new String("create," + clientID.toString());
				message += "," + position[0];
				message += "," + position[1];
				message += "," + position[2];
				System.out.println("Creating ghost avatar of client " + clientID.toString());
				forwardPacketToAll(message, clientID);
			}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * forward a request for avatar details to other players
	 * @param clientID requesting client id, used to block sending message to them
	 */
	public void wantsAvtDetails(UUID clientID) {
		try { 	String message = new String("avtD," + clientID.toString());
			System.out.println("Client " + clientID.toString() + " requesting existing avatar details");
			forwardPacketToAll(message, clientID);
			}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * forward message to play sound to other players
	 * @param sourceID source client id, used to block sending message to them
	 * @param pos sound location
	 */
	public void sendSound(UUID sourceID, String[] pos) {
		try { 	String message = new String("playSound");
				message += "," + pos[0];
				message += "," + pos[1];
				message += "," + pos[2];
				forwardPacketToAll(message, sourceID);
		} 	catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * forward client messages sending requested avatar details
	 * @param sourceID sender ID
	 * @param targetID recipient ID
	 * @param pos avatar position
	 */
	public void sendAvtDetails(UUID sourceID, UUID targetID, String[] pos) {
		try { 	String message = new String("sndAvtD," + sourceID.toString());
			message += "," + pos[0];
			message += "," + pos[1];
			message += "," + pos[2];
			System.out.println("Sending Avatar details for client " + sourceID.toString() + " to client " + targetID.toString() + ": (" + pos[0] + "," + pos[1] + "," + pos[2] + ")");
			sendPacket(message, targetID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * forward updated avatar positions to all other players
	 * @param clientID sender id, used to block sending message to them
	 * @param position avatar position
	 */
	public void sendMoveMessages(UUID clientID, String[] position)
	{
		try { 	String message = new String("move," + clientID.toString());
		message += "," + position[0];
		message += "," + position[1];
		message += "," + position[2];
	//	System.out.println("Moving avatar of client " + clientID.toString());
		forwardPacketToAll(message, clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * forward disconnection message to all other players, so the avatar may be removed
	 * @param clientID
	 */
	public void sendByeMessages(UUID clientID)
	{ 
		try { 	String message = new String("bye," + clientID.toString());
			System.out.println("Client " + clientID.toString() + " disconnected from the server");
			forwardPacketToAll(message, clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send NPC info to players
	 */
	public void sendNPCinfo() {
		if (numPlayers > 0) {
		try { 	String message = "moveNPC";
				message += "," + npc.getX();
				message += "," + npc.getY();
				message += "," + npc.getZ();
				message += "," + npc.getRot();
				sendPacketToAll(message);
		}
		catch (IOException e) { e.printStackTrace(); }
		}
	}
	/**
	 * send instructions to create NPC character to newly connected clients
	 * @param targetID target client ID
	 */
	public void sendCreateNPC(UUID targetID) {
		String message = "createNPC";
		message += "," + npc.getX();
		message += "," + npc.getY();
		message += "," + npc.getZ();
		message += "," + npc.getRot();
		System.out.println("Sending NPC info to client " + targetID.toString());
		try { sendPacket(message, targetID); }
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * [OBSOLETE] send NPC rotation data 
	 */
	public void sendRotNPC() {
		try { 	String message = "rotNPC";
		message += "," + npc.getRot();
		sendPacketToAll(message);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send a request to check if any avatars are close enough to the NPC to trigger its AI
	 */
	public void sendCheckNear() {
		try { 	String message = "checkNear, n";
		sendPacketToAll(message);
		} catch  (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send high score info to newly-conencted client
	 * @param id recipient client
	 */
	public void sendHScore(UUID id) {
		try { 	String message = "hscore";
		message += "," + hScore;
		sendPacket(message, id);
		} catch  (IOException e) { e.printStackTrace(); }
	}
	/**
	 * send updated high score info to all players
	 */
	public void sendHScoreAll() {
		try { 	String message = "hscore";
		message += "," + hScore;
		sendPacketToAll(message);
		} catch  (IOException e) { e.printStackTrace(); }
	}
	
	// NPC Controller
	/**
	 * startup function to initialize NPCController
	 */
	public void startNPCcontroller() {
		thinkStartTime = System.nanoTime();
		tickStartTime = System.nanoTime();
		lastThinkUpdateTime = thinkStartTime;
		lastTickUpdateTime = tickStartTime;
		setupNPCs();
		npcLoop();
	}
	/**
	 * startuo function to initialize NPCs
	 */
	public void setupNPCs() { 
		npc = new NPC();
		npc.randomizeLocation(rn.nextInt(40),rn.nextInt(40));
		npc.setRot(rn.nextInt(360));
	}
	
	/**
	 * this is the NPC AI loop. There are 4 possible outcomes:
	 * 1. If already flying, do nothing
	 * 2. if not flying, but an avatar is close, start flying
	 * 3. if walking, stop walking.
	 * 4. if a 50% chance succeeds, start walking
	 */
	public void npcLoop() { 
		while (true) {
			long currentTime = System.nanoTime();
			float elapsedThinkMilliSecs = (currentTime-lastThinkUpdateTime)/(1000000.0f);
			float elapsedTickMilliSecs = (currentTime-lastTickUpdateTime)/(1000000.0f);
			if (elapsedTickMilliSecs >= 25.0f) {	// tick
				lastTickUpdateTime = currentTime;
				npc.updateLocation();
				sendNPCinfo();
			}
			if (elapsedThinkMilliSecs >= 250.0f) {	// think
				lastThinkUpdateTime = currentTime;
				if (!npc.isFlying()) 	// if flying, change nothing
					sendCheckNear();	// if avatar is near, start flying
					if (nearNPC) {
						npc.startFlying();
					}
					else if (npc.isWalking()) {	// if walking, stop walking
						npc.toggleWalking();
					}
					else if (rn.nextFloat() < 0.50){	// 50% chance to start walking in a random direction
						npc.randomRot();
						npc.toggleWalking();
					}
				}
			Thread.yield();
		}
	}
}