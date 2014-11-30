package main;

import java.io.File;
import java.io.FileOutputStream;
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
	
	private final int RECV_WINDOW = 5;
	
	private String fileName;
	
	private int numPackets;
	
	private int fileSize;
	
	/** Timeout in milliseconds */
	private final int TIMEOUT = 2500;
	
	private int clientPort;
	
	private int serverPort;
	
	private InetAddress serverAddr;
	
	private DatagramSocket clientSocket;
	
	public Client() throws SocketException, IOException {
		promptUser();
		initalizeClient();
		
		if (!establishConnection()) return;
		
		if (!requestFile()) return;
		
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
	
	private boolean establishConnection() throws IOException {
		String msg = "Attempting to connect to server at ";
		msg += serverAddr.getHostAddress();
		msg += " port " + serverPort;
		System.out.println(msg);
		
		Header synHeader = new Header();
		synHeader.setSynFlag(true);
		
		byte[] sendHeader = synHeader.getBytes();
		
		final int attempts = 3;
		
		DatagramPacket packet = null;
		
		for (int i = 0; i < attempts; i++) {
			
			send(sendHeader);
			
			try {
				packet = receive();
				byte[] data = packet.getData();
				
				Header head = new Header(data);
				
				if (!head.getAckFlag() || !head.getSynFlag()) {
					System.err.println("Received unexpected packet");
					packet = null;
					i--;
					continue;
				}
					
			} catch (SocketTimeoutException e) {
				System.err.println("Connection attempt " + 
						(i + 1) + " timed out.");
			}
		}
		
		if (packet == null) {
			msg = "Unable to establish conenction";
			System.err.println(msg);
			return false;
		}
		
		byte[] bytes = packet.getData();
		String availableFiles = "";
		
		/* Loop through data field */
		for (int i = Header.HEADER_SIZE; i < bytes.length; i++) {
			availableFiles += (char) bytes[i];
		}
		
		if (availableFiles.isEmpty()) {
			System.err.println("Server has no files to send");
			return false;
		}
		
		String[] files = availableFiles.split(";");
		
		System.out.println(new String(new char[30]).replace('\0', '-'));
		System.out.println("Available files on " + 
				serverAddr.getHostAddress() + ":");
		
		for (String file : files) {
			System.out.println("\t" + file);
		}
		
		return true;
	}
	
	private boolean requestFile() throws IOException {
		Scanner scan = new Scanner(System.in);
		
		System.out.print("\nSelect a file: ");
		fileName = scan.nextLine();
		scan.close();
		
		Header head = new Header();
		head.setReqFlag(true);
		
		byte[] headData = head.getBytes();
		
		int reqPackLen = headData.length + fileName.length();
		
		byte[] packData = new byte[reqPackLen];
		
		/* Populate REQ data array with header data */
		for (int i = 0; i < headData.length; i++) {
			packData[i] = headData[i];
		}
		
		/* Populate ACK data array with requested file */
		for (int i = 0; i < fileName.length(); i++) {
			packData[i + headData.length] = (byte) fileName.charAt(i);
		}
		
		System.out.println("Requesting file \"" + fileName + "\"");
		
		final int attempts = 3;
		
		DatagramPacket reqAckPack = null;
		
		for (int i = 0; i < attempts; i++) {
			
			send(packData);
			
			try {
				reqAckPack = receive();
				byte[] data = reqAckPack.getData();
				
				Header recvHead = new Header(data);
				
				if (!recvHead.getAckFlag() || !recvHead.getReqFlag()) {
					System.err.println("Received unexpected packet");
					reqAckPack = null;
					i--;
					continue;
				}
					
			} catch (SocketTimeoutException e) {
				System.err.println("Request " + (i + 1) + " timed out.");
			}
		}
		
		if (reqAckPack == null) {
			String msg = "Server not responding to request";
			System.err.println(msg);
			return false;
		}
		
		byte[] reqAckData = reqAckPack.getData();
		
		int statusCode = (int) reqAckData[Header.HEADER_SIZE];
		
		/* Checks for non "good" status */
		if (statusCode != (1 << 7)) {
			String msg = "Server does not recognize requested file";
			System.err.println(msg);
			return false;
		}
		
		numPackets = (int) (reqAckData[Header.HEADER_SIZE + 1] << 24 |
				reqAckData[Header.HEADER_SIZE + 2] << 16 |
				reqAckData[Header.HEADER_SIZE + 3] << 8 |
				reqAckData[Header.HEADER_SIZE + 4]);
		
		fileSize = (int) (reqAckData[Header.HEADER_SIZE + 5] << 24 |
				reqAckData[Header.HEADER_SIZE + 6] << 16 |
				reqAckData[Header.HEADER_SIZE + 7] << 8 |
				reqAckData[Header.HEADER_SIZE + 8]);
		
		String msg = "File \"" + fileName + "\" is " + fileSize + " bytes";
		System.out.println(msg);
		
		return true;
		
	}
	
	private void acceptFile() throws IOException {
		int lastReceived = 0;
		int bytesReceived = 0;
		
		final String path = File.separator + fileName;
		
		File file = new File(path);
		file.createNewFile();
		
		FileOutputStream fos = new FileOutputStream(path, true);
		
		while (lastReceived != numPackets) {
			for (int i = 0; i < RECV_WINDOW; i++) {
				DatagramPacket recvPack = null;

				try {
					recvPack = receive();
				} catch (SocketTimeoutException e) {
					break;
				}
				
				Header head = new Header(recvPack.getData());
				int seqNum = head.getSequenceNum();

				// TODO: Check the checksum

				if (seqNum != lastReceived + 1) {
					String msg = "Got unexpected packet. Sequence number: ";
					msg += seqNum;
					System.err.println(msg);
					i--;
					continue;
				}
				
				lastReceived = seqNum;

				byte[] bytes = head.getBytes();
				int hSize = Header.HEADER_SIZE;
				
				bytesReceived += bytes.length - hSize;

				fos.write(bytes, hSize, bytes.length - hSize);
				
				System.out.println(bytesReceived / fileSize + "%");
			}

			// Send ACK to Sever
			Header ackPack = new Header();
			ackPack.setAckFlag(true);
			ackPack.setSequenceNum(lastReceived);
		}
		
		System.out.println("File transfer complete.");
		
		fos.close();
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
