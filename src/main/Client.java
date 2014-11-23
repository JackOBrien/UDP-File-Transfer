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
	private final int TIMEOUT = 2500;
	
	private int clientPort;
	
	private int serverPort;
	
	private InetAddress serverAddr;
	
	private DatagramSocket clientSocket;
	
	public Client() throws SocketException, IOException {
		promptUser();
		initalizeClient();
		establishConnection();
		requestFile();
		acceptFile();
	}
	
	private void promptUser() {
		Scanner scan = new Scanner(System.in);
		
		// TODO: fix retry
		
		System.out.print("Please specify a port number: ");
		String input = scan.nextLine();
		
		try {
			clientPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
			promptUser();
		}
		
		System.out.print("Enter server IPv4 address: ");
		String destAddr = scan.nextLine();
		
		try {
			serverAddr = InetAddress.getByName(destAddr);
		} catch (UnknownHostException e) {
			System.err.println("Invalid IPv4 address");
			promptUser();
		}
		
		System.out.print("Enter server port number: ");
		input = scan.nextLine();
		
		try {
			serverPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
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
		msg += " port " + serverPort;
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
				
				if (!head.getAckFlag() || !head.getSynFlag()) {
					packet = null;
					i--;
					continue;
				}
					
			} catch (SocketTimeoutException e) {}
		}
		
		if (packet == null) {
			msg = "Unable to establish conenction";
			System.err.println(msg);
			return;
		}
		
		byte[] bytes = packet.getData();
		String availableFiles = "";
		
		for (int i = Header.HEADER_SIZE; i < bytes.length; i++) {
			availableFiles += (char) bytes[i];
		}
		
		String[] files = availableFiles.split(";");
		
		System.out.println(new String(new char[30]).replace('\0', '-'));
		System.out.println("Available files on " + 
				serverAddr.getHostAddress() + ":");
		
		for (String file : files) {
			System.out.println("\t" + file);
		}
	}
	
	private void requestFile() {
		Scanner scan = new Scanner(System.in);
		
		System.out.print("\nSelect a file: ");
		String reqFile = scan.nextLine();
		
		Header head = new Header();
		
		byte[] headData = head.getBytes();
		
		int ackPackLen = headData.length + reqFile.length();
		head.setLength(ackPackLen);
		
		byte[] packData = new byte[ackPackLen];
		
		/* Populate ACK data array with header data */
		for (int i = 0; i < headData.length; i++) {
			packData[i] = headData[i];
		}
		
		/* Populate ACK data array with list of files */
		for (int i = 0; i < reqFile.length(); i++) {
			packData[i + headData.length] = (byte) reqFile.charAt(i);
		}
		
		/* Send ACK header to client */
		try {
			send(packData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		scan.close();
	}
	
	private void acceptFile() {
		DatagramPacket ackPacket = null;
		
		do {
			
			try {
				ackPacket = receive();
			} catch (SocketTimeoutException e) {
				System.err.println("Connection timed out");
				return;
			}
			//hi
			catch (IOException e) {
				e.printStackTrace();
			}
					
		} while (ackPacket == null);
		
		
	}
	
	private void send(byte[] data) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(data, data.length,
				serverAddr, serverPort);
		
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
		try {
			new Client();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
