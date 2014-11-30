package main;

public class Header {

	/** Header length in bytes */
	public static final int HEADER_SIZE = 8;
	
	private byte[] data;
	
	public Header() {
		data = new byte[HEADER_SIZE];
	}
	
	public Header(byte[] data) {
		this.data = data;
	}
	
	public void setSequenceNum(int seqNum) {
		byte[] converted = intToByteArr(4, seqNum);
		
		for (int i = 0; i < converted.length; i++) {
			data[i] = converted[i];
		}
	}
	
	public void setSynFlag(boolean flag) {
		if (flag) {
			data[6] |= 1 << 7; 
		} else {
			data[6] &= ~(1 << 7);
		}
	}
	
	public void setAckFlag(boolean flag) {
		if (flag) {
			data[6] |= 1 << 6; 
		} else {
			data[6] &= ~(1 << 6);
		}
	}
	
	public void setReqFlag(boolean flag) {
		if (flag) {
			data[6] |= 1 << 5; 
		} else {
			data[6] &= ~(1 << 5);
		}
	}
	
	public boolean getAckFlag() {
		int x = (1 << 7);
		return (data[6] & x) == x;
	}
	
	public boolean getSynFlag() {
		int x = (1 << 6);
		return (data[6] & x) == x;
	}
	
	public boolean getReqFlag() {
		int x = (1 << 5);
		return (data[6] & x) == x;
	}
	
	public int getSequenceNum() {
		return (int) (data[0] << 24 | data[1] << 16 | data[2] << 8 | data[3]);
	}
	
	public byte[] getBytes() {
		return data;
	}
	
	public void setChecksum(byte[] dataField) {
		data[4] = 0; // Sets the checksum
		data[5] = 0; // to zero
		
		int hLen = data.length;
		int dLen = dataField.length;
		
		byte[] checksumData = new byte[hLen + dLen];
		System.arraycopy(data, 0, checksumData, 0, hLen);
		System.arraycopy(dataField, 0, checksumData, hLen, dLen);
		
		short checksum = calculateChecksum(data);
		
		data[4] = (byte) (checksum & 0xFF00);
		data[5] = (byte) (checksum & 0xFF);
	}
	
	private byte[] intToByteArr(int numBytes, int toConvert) {
		byte[] data = new byte[numBytes];
		
		for (int i = numBytes - 1, offset = 0; i >= 0; i--, offset += 8) {
			data[i] = (byte) (toConvert >> offset);
		}
		
		return data;
	}
	
	public static byte[] createStatusPacket(boolean good, int numPackets, 
			int numBytes) {
		
		Header head = new Header();
		head.setAckFlag(true);
		head.setReqFlag(true);
		
		final int dataLen = 9;
		byte[] dataField = new byte[dataLen];
		
		dataField[0] = (byte) ((good? 1:0) << 7);
		
		if (good) {
			byte[] convertedPack = head.intToByteArr(4, numPackets);
			byte[] convertedByte = head.intToByteArr(4, numPackets);
		
			System.arraycopy(convertedPack, 0, dataField, 0, 3);
			System.arraycopy(convertedByte, 0, dataField, 4, 7);
		}
		
		return dataField;
	}
	
	public static short calculateChecksum(byte[] buf) {
		short sum = 0;
		
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
		
		return (short) ~(sum & 0xFFFF);
	}
}