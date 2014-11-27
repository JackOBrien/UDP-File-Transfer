package main;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Server {
	 
	private final int PACKET_SIZE = 1024;
	
	private final String PATH = "files/";
	
	private String files;
	
	private int serverPort;
	
	private DatagramSocket serverSocket;
	
	private InetAddress clientAddr;
	
	private int clientPort;
	
	private String requestedFile;

	public Server() throws SocketException {
		files = "";
		
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
	
	private void getAvailFiles() {
		File folder = new File(PATH);		
		File[] fileArr = folder.listFiles();
		
		if (fileArr == null) return;
		
		for (int i = 0; i < fileArr.length; i++) {
			
			if (fileArr[i].isFile()) {
				files += fileArr[i].getName() + ";";
			}
		}
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
		
		System.out.println("Got a packet!");
		
		byte[] data = recvPacket.getData();
		Header head = new Header(data);
		
		/* If received non SYN packet, retry */
		if (!head.getSynFlag()) {
			establishConnection();
		}
		
		clientAddr = recvPacket.getAddress();
		clientPort = recvPacket.getPort();
		
		getAvailFiles();
		
		Header ackHead = new Header();
		ackHead.setAckFlag(true);
		ackHead.setSynFlag(true);
		
		byte[] headData = ackHead.getBytes();
		
		int ackPackLen = headData.length + files.length();
		
		byte[] packData = new byte[ackPackLen];
		
		/* Populate ACK data array with header data */
		for (int i = 0; i < headData.length; i++) {
			packData[i] = headData[i];
		}
		
		/* Populate ACK data array with list of files */
		for (int i = 0; i < files.length(); i++) {
			packData[i + headData.length] = (byte) files.charAt(i);
		}
		
		/* Send ACK header to client */
		try {
			send(packData);
		} catch (IOException e) {
			establishConnection();
		}
		
		DatagramPacket reqPack = null;
		
		do {
			reqPack = receive();
		} while (reqPack == null);
		
		byte[] bytes = reqPack.getData();
		 requestedFile = "";
		
		for (int i = Header.HEADER_SIZE; i < bytes.length; i++) {
			requestedFile += (char) bytes[i];
		}
	}
	
	private byte[] fileToBytes(String path) throws IOException {
		Path p = Paths.get(PATH + path);
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
	

	/****************************************************************
	 * Sends the given data to the most recent connected client on 
	 * the port the server is hosted on.
	 * 
	 * @param data
	 * @throws IOException
	 ***************************************************************/
	private void send(byte[] data) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(data, 
				data.length, clientAddr, clientPort);
		
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
				System.err.println("File '" + requestedFile + 
						"' not found.");
				continue;
			}
			
			sendFile(data);
		}
	}
	
	public static void main(String[] args) {
		Server s = null;
		
		try {
			s = new Server();
		} catch (SocketException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		s.begin();		
	}
}

