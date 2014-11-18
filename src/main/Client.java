package main;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	private final int PACKET_SIZE = 1024;
	
	private int clientPort;
	
	private InetAddress serverAddr;
	
	private DatagramSocket clientSocket;
	
	public Client() {
		
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
		} catch (SocketException e) {
			String message = "Problem starting client on port ";
			message += clientPort;
			message += "\nIs there another instance of this client?";
			throw new SocketException(message);
		}
	}
	
	private void establishConnection() {
		String msg = "Attempting to connect to server at ";
		msg += serverAddr.getHostAddress();
		msg += " port " + clientPort;
		System.out.println(msg);
	}
	
	public static void main(String[] args) {
		
	}
}
