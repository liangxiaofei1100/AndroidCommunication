package com.dreamlink.communication.util;

public class ArrayUtil {
	/**
	 * Join two arrays to a new array.
	 * 
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static byte[] join(byte[] a1, byte[] a2) {
		byte[] result = new byte[a1.length + a2.length];
		System.arraycopy(a1, 0, result, 0, a1.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);
		return result;
	}

	/**
	 * convert int to byte array.
	 * 
	 * @param i
	 * @return
	 */
	public static byte[] int2ByteArray(int i) {
		byte[] result = new byte[4];
		// first 4 bit
		result[0] = (byte) ((i >> 24) & 0xff);
		// second 4 bit
		result[1] = (byte) ((i >> 16) & 0xff);
		// third 4 bit
		result[2] = (byte) ((i >> 8) & 0xff);
		// fourth 4 bit.
		result[3] = (byte) (i & 0xff);
		return result;
	}

	/**
	 * convert byte array to int.
	 * 
	 * @param array
	 * @return
	 */
	public static int byteArray2Int(byte[] array) {
		int reuslt = 0;
		byte loop;
		for (int i = 0; i < 4; i++) {
			loop = array[i];
			int offset = array.length - i - 1;
			reuslt += (loop & 0xFF) << (8 * offset);
		}
		return reuslt;
	}
}
