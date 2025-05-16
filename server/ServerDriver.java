package server;

import java.io.IOException;
import java.util.Scanner;

public class ServerDriver {
	private Server thisUDPServer;

	public ServerDriver(int serverPort) { 
		try { 	thisUDPServer = new Server(serverPort); }
		catch (IOException e) { e.printStackTrace(); } 
	}
	
	public static void main(String[] args) { 
		int serverport;
		String input;
		Scanner scan = new Scanner(System.in);
		System.out.print("Enter server port number: ");
		input = scan.nextLine();
		serverport = Integer.parseInt(input);
		
		System.out.println("Server Starting...");
		ServerDriver app = new ServerDriver(serverport);
		/*
		if(args.length >= 1) {
			System.out.println("Server Starting...");
			ServerDriver app = new ServerDriver(Integer.parseInt(args[0])); 
		} */
	} 
}