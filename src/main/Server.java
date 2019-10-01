package main;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Server {

    private final int MAX_PACKET_SIZE = 1024;

    private final int SEND_WINDOW = 5;

    private final String PATH = "files/";

    private final int TIMEOUT = 4000;

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

    public static void main(String[] args) {
        Server s = null;

        try {
            s = new Server();
        } catch (SocketException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            s.begin();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            serverSocket.setSoTimeout(TIMEOUT);
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

        files = "";

        if (fileArr == null) return;

        for (int i = 0; i < fileArr.length; i++) {

            if (fileArr[i].isFile()) {
                files += fileArr[i].getName() + ";";
            }
        }
    }

    private DatagramPacket receive() throws SocketTimeoutException,
            BadChecksumException {
        byte[] buf = new byte[MAX_PACKET_SIZE];

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            serverSocket.receive(packet);
        } catch (SocketTimeoutException se) {
            throw se;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }

        int expected = Header.calculateChecksum(buf);
        int received = new Header(buf).getChecksum();

        if (expected != received)
            throw new BadChecksumException(expected, received);

        return packet;
    }

    private void establishConnection() {
        DatagramPacket recvPacket = null;

        do {
            try {
                recvPacket = receive();
            } catch (SocketTimeoutException e) {
                continue;
            } catch (BadChecksumException bc) {
                System.err.println(bc.getMessage());
                continue;
            }
        } while (recvPacket == null);

        byte[] data = recvPacket.getData();
        Header head = new Header(data);

		/* If received non SYN packet, retry */
        if (!head.getSynFlag() || head.getReqFlag() || head.getAckFlag()) {

            System.err.println("Received unexpected packet");
            establishConnection();
        }

        clientAddr = recvPacket.getAddress();
        clientPort = recvPacket.getPort();

        System.out.println("Received SYN packet from " +
                clientAddr.getHostAddress() + " " + " on port " + clientPort);

        getAvailFiles();

        Header ackHead = new Header();
        ackHead.setAckFlag(true);
        ackHead.setSynFlag(true);
        ackHead.setChecksum(files.getBytes());

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
            System.out.println("Sent ACK to client");
        } catch (IOException e) {
            establishConnection();
        }

        DatagramPacket reqPack = null;

		/* Wait for REQ from client */
        do {
            try {
                reqPack = receive();

                Header recvHead = new Header(recvPacket.getData());

				/* Check for proper header */
                if (!recvHead.getReqFlag() || recvHead.getAckFlag() ||
                        recvHead.getSynFlag()) {
                    System.err.println("Received unexpected packet");
                    recvHead = null;
                    continue;
                }

            } catch (SocketTimeoutException e) {
                continue;
            } catch (BadChecksumException bc) {
                System.err.println(bc.getMessage());
                continue;
            }
        } while (reqPack == null);

        byte[] bytes = reqPack.getData();
        requestedFile = "";

        for (int i = Header.HEADER_SIZE; i < bytes.length; i++) {
            char c = (char) bytes[i];

            if (c == '\0') break;

            requestedFile += c;
        }

        System.out.println("Client is requesting \"" + requestedFile + "\"");
    }

    private byte[] fileToBytes(String path) throws IOException {
        Path p = Paths.get(PATH + path);
        return Files.readAllBytes(p);
    }

    private boolean sendFile(byte[] fileData, int numPackets)
            throws IOException {
        int lastAck = 0;

        int numRetry = 0;
        final int attempts = 3;

        byte[] statusPacket = Header.createStatusPacket(true, numPackets,
                fileData.length);

        for (int i = 0; i < numPackets; i = lastAck) {

            if (i == 0) {
                send(statusPacket);
                System.out.println("Sending request acknowledgement to client");
            }

            for (int x = i; x < SEND_WINDOW + i && x < numPackets; x++) {
                Header head = new Header();
                head.setSequenceNum(x + 1);

                // TODO: Checksum junk

                final int maxData = MAX_PACKET_SIZE - Header.HEADER_SIZE;
                int leftToSend = fileData.length - (maxData) * x;
                int packSize = leftToSend;

                if (packSize > maxData)
                    packSize = maxData;

                packSize += Header.HEADER_SIZE;

                byte[] packetData = new byte[packSize];

				/* Populate packet byte array */
                for (int k = Header.HEADER_SIZE; k < packSize; k++) {
                    packetData[k] =
                            fileData[(x * maxData) + (k - Header.HEADER_SIZE)];
                }

                head.setChecksum(packetData);

                System.arraycopy(head.getBytes(), 0, packetData, 0,
                        Header.HEADER_SIZE);

                try {
                    send(packetData);
                    System.out.println("  Sent packet number " + (x + 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            DatagramPacket ackPacket = null;

            try {
                ackPacket = receive();

                Header head = new Header(ackPacket.getData());

                if (!head.getAckFlag() || head.getReqFlag() ||
                        head.getSynFlag()) {
                    System.err.println("Received unexpected packet");
                    continue;
                }

            } catch (SocketTimeoutException e) {

                if (++numRetry > attempts) {
                    System.err.println("Client not responding.");
                    return false;
                }

                System.err.println("Acknowledgement timed out. Resending "
                        + "packet " + lastAck + "\n");
                continue;
            } catch (BadChecksumException be) {
                System.err.println(be.getMessage());
                continue;
            }
            numRetry = 0;

            Header head = new Header(ackPacket.getData());
            lastAck = head.getSequenceNum();

            System.out.println(" -Got acknowledgement of packet " +
                    lastAck + "\n");
        }

        return true;
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

    public void begin() throws IOException {

        while (true) {
            establishConnection();
            byte[] data = null;

            try {
                data = fileToBytes(requestedFile);
            } catch (IOException e) {
                byte[] status = Header.createStatusPacket(false, 0, 0);
                send(status);

                System.err.println("File '" + requestedFile +
                        "' not found.");
                continue;
            }


            int numPackets = (int) Math.ceil(((double) data.length) /
                    ((double) MAX_PACKET_SIZE - Header.HEADER_SIZE));

            System.out.println("-- Starting file transfer --");

            if (sendFile(data, numPackets)) {
                System.out.println("File transfer complete.");
            }
        }
    }
}

