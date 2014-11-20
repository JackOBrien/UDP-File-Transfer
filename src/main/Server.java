package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Server {
	 
	private final int PACKET_SIZE = 1024;
	
	private int serverPort;
	
	private DatagramSocket serverSocket;
	
	private InetAddress clientAddr;
	
	private String requestedFile;

	public Server() throws SocketException {
		promptUser();
		initializeServer();
	}
	
	private void promptUser() {
		Scanner scan = new Scanner(System.in);
		System.out.print("Please specify a port number: ");
		String input = scan.nextLine();
		
		try {
			serverPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
			promptUser();
		}
		scan.close();
	}
	
	private void initializeServer() throws SocketException {
		try {
			serverSocket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			String message = "Problem hosting server on port ";
			message += serverPort;
			message += "\nIs there another instance of this server?";
			throw new SocketException(message);
		}
		
		String msg = "Server started on port " + serverPort;
		System.out.println(msg);
	}
	
	private DatagramPacket receive() {
		byte[] buf = new byte[PACKET_SIZE];
		
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		
		try {
			serverSocket.receive(packet);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return null;
		}
		
		return packet;
	}
	
	private void establishConnection() {
		DatagramPacket recvPacket = null;
		
		do {
			recvPacket = receive();
		} while (recvPacket == null);
		
		// TODO: Check for connection request
		
		clientAddr = recvPacket.getAddress();
		
		// TODO: Get requested file path
		requestedFile = null;
	}
	
	private byte[] fileToBytes(String path) throws IOException {
		Path p = Paths.get(path);
		return Files.readAllBytes(p);
	}
	
	private void sendFile(byte[] fileData) {
		
		for (int i = 0; i < fileData.length; i += PACKET_SIZE) {
			// TODO: Build packet
			byte[] packetData = new byte[PACKET_SIZE];
			
			/* Populate packet byte array */
			for (int k = 0; k < PACKET_SIZE && k < fileData.length; k++) {
				packetData[k] = fileData[i + k];
			}
			
			try {
				send(packetData);
			} catch (IOException e) {
				// TODO: Resend packet
			}
		}
	}
	
	private void send(byte[] data) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(data, data.length);
		
		serverSocket.send(sendPacket);
	}
	
	public void begin() {

		while (true) {
			establishConnection();
			byte[] data = null;

			try {
				data = fileToBytes(requestedFile);
			} catch (IOException e) {
				// TODO: Send error to client
				System.err.println("File '" + requestedFile + "' not found.");
				continue;
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			new Server();
		} catch (SocketException e) {
			System.err.println(e.getMessage());
		}
	}
}

