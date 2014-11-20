package main;

import java.nio.ByteBuffer;


public class Header {

	/** Header length in bytes */
	public static final int HEADER_SIZE = 96;
	
	private byte[] data;
	
	public Header() {
		data = new byte[HEADER_SIZE];
	}
	
	public Header(byte[] data) {
		this.data = data;
	}
	
	public void setSequenceNum(int seqNum) {
		final int numBytes = 4;
		
		byte[] seqArr = 
				ByteBuffer.allocate(numBytes).putInt(seqNum).array();
		
		for(int i = 0; i < numBytes; i++) {
			data[i] = seqArr[i];
		}
	}
	
	public void setAckNum(int ackNum) {
		final int numBytes = 4;
		
		byte[] ackArr = 
				ByteBuffer.allocate(numBytes).putInt(ackNum).array();
		
		for(int i = 0; i < numBytes; i++) {
			data[i + 4] = ackArr[i];
		}
	}
	
	/** TODO: Find out if we need this */
	public void setLength(int length) {
		final int numBytes = 2;
		
		byte[] ackArr = 
				ByteBuffer.allocate(numBytes).putInt(length).array();
		
		for(int i = 0; i < numBytes; i++) {
			data[i + 10] = ackArr[i];
		}
	}
	
	public void setSynFlag(boolean flag) {
		if (flag) {
			data[11] |= 2; 
		} else {
			data[11] &= ~(2);
		}
	}
	
	public void setAckFlag(boolean flag) {
		if (flag) {
			data[11] |= 1; 
		} else {
			data[11] &= ~(1);
		}
	}
	
	public boolean getAckFlag() {
		return (data[11] & 2) == 1;
	}
	
	public boolean getSynFlag() {
		return (data[11] & 1) == 1;
	}
	
	public byte[] getBytes() {
		setChecksum();
		return data;
	}
	
	private void setChecksum() {
		data[8] = 0; // Sets the checksum
		data[9] = 0; // to zero
		
		long checksum = calculateChecksum(data);
		
		byte[] csumArr = 
				ByteBuffer.allocate(2).putLong(checksum).array();
		
		data[8] = csumArr[0];
		data[9] = csumArr[1];
		
	}
	
	private long calculateChecksum(byte[] buf) {
		
		long sum = 0;
			
		for (int i = 0; i < buf.length; i++) {
			sum += (buf[i++] & 0xFF) << 8;
			
			/* Check for odd length */
			if (i == buf.length) break;
			
			sum += (buf[i] & 0xFF);
			
			/* Check for carry bits */
			if (sum >>> 16 > 0) {
				sum &= 0xFFFF + sum >>> 16;
				sum &= 0xFFFF;
			}
		}
		
		return ~(sum & 0xFFFF);
	}
}