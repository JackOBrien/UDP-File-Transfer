package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	private final int PACKET_SIZE = 1024;
	
	/** Timeout in milliseconds */
	private final int TIMEOUT = 200;
	
	private int clientPort;
	
	private InetAddress serverAddr;
	
	private DatagramSocket clientSocket;
	
	public Client() throws SocketException, IOException {
		promptUser();
		initalizeClient();
		establishConnection();
	}
	
	private void promptUser() {
		Scanner scan = new Scanner(System.in);
		System.out.print("Please specify a port number: ");
		String input = scan.nextLine();
		
		try {
			clientPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
			promptUser();
		}
		
		System.out.println("Enter server IPv4 address: ");
		String destAddr = scan.nextLine();
		
		try {
			serverAddr = InetAddress.getByName(destAddr);
		} catch (UnknownHostException e) {
			System.err.println("Invalid IPv4 address");
			promptUser();
		}
		
		scan.close();
	}
	
	private void initalizeClient() throws SocketException {
		try {
			clientSocket = new DatagramSocket(clientPort);
			clientSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) {
			String message = "Problem starting client on port ";
			message += clientPort;
			message += "\nIs there another instance of this client?";
			throw new SocketException(message);
		}
	}
	
	private void establishConnection() throws IOException {
		String msg = "Attempting to connect to server at ";
		msg += serverAddr.getHostAddress();
		msg += " port " + clientPort;
		System.out.println(msg);
		
		Header synHeader = new Header();
		synHeader.setSynFlag(true);
		
		byte[] sendHeader = synHeader.getBytes();
		
		send(sendHeader);
		
		final int attempts = 3;
		
		DatagramPacket packet = null;
		
		for (int i = 0; i < attempts; i++) {
			
			try {
				packet = receive();
				byte[] data = packet.getData();
				
				Header head = new Header(data);
				
				if (!head.getAckFlag()) {
					packet = null;
					continue;
				}
					
			} catch (SocketTimeoutException e) {}
		}
		
		if (packet == null) {
			msg = "Unable to establish conenction";
			System.err.println(msg);
		}
		
		byte[] bytes = packet.getData();
		String availableFiles = "";
		
		for (int i = Header.HEADER_SIZE; i < bytes.length; i++) {
			availableFiles += (char) bytes[i];
		}
		
		String[] paths = availableFiles.split(";");
		
		System.out.println("Available files on " + 
				serverAddr.getHostAddress());
		
		for (String path : paths) {
			System.out.println("\t" + path);
		}
	}
	
	private void send(byte[] data) throws IOException {
		DatagramPacket sendPacket = 
				new DatagramPacket(data, data.length);
		
		clientSocket.send(sendPacket);
	}
	
	private DatagramPacket receive() throws IOException, 
		SocketTimeoutException {
				
		byte[] recvData = new byte[1024];
		
		DatagramPacket recvPacket = 
				new DatagramPacket(recvData,recvData.length);
		
		clientSocket.receive(recvPacket);

		return recvPacket;
	}
	
	public static void main(String[] args) {
		
	}
}
