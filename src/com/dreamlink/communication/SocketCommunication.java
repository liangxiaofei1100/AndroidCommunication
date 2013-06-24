package com.dreamlink.communication;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.io.IOException;

import com.dreamlink.communication.util.ArrayUtil;
import com.dreamlink.communication.util.Log;

/**
 * Thread for send and receive message.</br>
 * 
 * Send or receive has processed the send or receive packet. The process detail
 * see {@link #encode(byte[])} and {@link #decode(DataInputStream)} </br>
 * 
 */
public class SocketCommunication extends Thread {
	private static final String TAG = "SocketCommunication";
	public static final String PORT = "55555";

	public interface OnCommunicationChangedListener {
		void OnCommunicationEstablished(SocketCommunication communication);

		void OnCommunicationLost(SocketCommunication communication);
	}

	public interface ICommunicate {
		void receiveMessage(byte[] msg, SocketCommunication socketCommunication);

		void sendMessage(byte[] msg);
	}

	private Socket socket;
	private ICommunicate iCommunicate;

	private DataInputStream dataInputStream = null;
	private DataOutputStream dataOutputStream = null;

	private OnCommunicationChangedListener mListener;

	/** The size of head */
	private static final int HEAD_SIZE = 4;
	/** Buffer of head */
	private byte[] mHeadBuffer = new byte[HEAD_SIZE];
	/** One receive max size */
	private static final int RECEIVE_BUFFER_SIZE = 10 * 1024;
	/** Buffer of one time receive. */
	private byte[] mReceiveBuffer;

	/** The remain head of last receive. */
	private byte[] mRemainHeader;
	/** The last packet length. It is use for next read */
	private int mLastPacketLength;
	/** The remain packet of last receive. */
	private byte[] mRemainPacket;

	public SocketCommunication(Socket socket, ICommunicate iCommunicate) {
		this.socket = socket;
		this.iCommunicate = iCommunicate;
	}

	public InetAddress getConnectIP() {
		return socket.getInetAddress();
	}

	public void setOnCommunicationChangedListener(
			OnCommunicationChangedListener listener) {
		mListener = listener;
	}

	@Override
	public void run() {
		super.run();

		try {
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			mListener.OnCommunicationEstablished(this);
			mReceiveBuffer = new byte[RECEIVE_BUFFER_SIZE];
			while (true) {
				decode(dataInputStream);
			}
		} catch (IOException e) {
			e.printStackTrace();
			dataInputStream = null;
			dataOutputStream = null;
			mListener.OnCommunicationLost(this);
		}
	}

	/**
	 * Decode the encoded package.</br>
	 * 
	 * 1. Before decode:</br>
	 * 
	 * 1.1 Normal received data is:
	 * [header1+msg1][header2+msg2][header3+msg3].</br>
	 * 
	 * 1.2 But in some conditions received data may break or more than one
	 * packet like: [hearder1+msg1.1][msg1.2+header2+msg2+header3+msg3]</br>
	 * 
	 * After decode: [msg1][msg2][msg3]</br>
	 * 
	 * Decode process:</br>
	 * 
	 * 1. Read 4 bit header to get the packet length.</br>
	 * 
	 * 2. Based on the packet length, read the input to get the packet
	 * data.</br>
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private synchronized void decode(DataInputStream in) throws IOException {
		int dataReceivedLength = 0;
		if (mRemainPacket == null) {
			Log.d(TAG, "There is no remain packet");
			// There is no remain packet.
			// Get the first 4 bit header for packet length.
			int packetLength = 0;
			if (mRemainHeader == null) {
				// There is no remain head.
				Log.d(TAG, "There is no remain head");
				// TODO dataReceivedLength maybe -1. This condition should
				// process.
				dataReceivedLength = in.read(mHeadBuffer);
				if (dataReceivedLength == -1) {
					Log.d(TAG, "Connection lost. dataReceivedLength = -1");
					return;
				}
				Log.d(TAG, "received header size: " + dataReceivedLength);
				if (dataReceivedLength < HEAD_SIZE) {
					Log.d(TAG,
							"dataReceivedLength < HEAD_SIZE, dataReceivedLength = "
									+ dataReceivedLength);
					// Received data is less than one header data, Save it and
					// return for next read.
					mRemainHeader = Arrays.copyOfRange(mHeadBuffer, 0,
							dataReceivedLength);
					return;
				} else if (dataReceivedLength == HEAD_SIZE) {
					// Get the packet length.
					packetLength = ArrayUtil.byteArray2Int(mHeadBuffer);
					mRemainHeader = null;
				}
			} else {
				Log.d(TAG, "There is remain header data left.");
				// There is remain header data left.
				// Read the remain header data to get the header data.
				dataReceivedLength = in.read(mHeadBuffer, 0, HEAD_SIZE
						- mRemainHeader.length);
				if (mRemainHeader.length + dataReceivedLength < HEAD_SIZE) {
					Log.d(TAG,
							"remain head + data received < HEAD_SIZE, dataReceivedLength = "
									+ dataReceivedLength);
					// remain head + data received less than one header data.
					// Save the header data and return for next read.
					mRemainHeader = ArrayUtil.join(mRemainHeader, Arrays
							.copyOfRange(mHeadBuffer, 0, dataReceivedLength));
					return;
				} else if (mRemainHeader.length + dataReceivedLength == HEAD_SIZE) {
					Log.d(TAG, "remain head + data received is one header");
					// remain head + data received is one header
					mHeadBuffer = ArrayUtil.join(mRemainHeader, Arrays
							.copyOfRange(mHeadBuffer, 0, dataReceivedLength));
					packetLength = ArrayUtil.byteArray2Int(mHeadBuffer);
					mRemainHeader = null;
				} else {
					// Should not be here.
					Log.e(TAG, "Decode header error.");
					return;
				}
			}
			Log.d(TAG, "Received package, length = " + packetLength);
			if (packetLength > RECEIVE_BUFFER_SIZE || packetLength < 1) {
				Log.e(TAG,
						"Decode header error. packageLength is bad number. packageLength = "
								+ packetLength);
				return;
			}
			// Read received data.
			dataReceivedLength = in.read(mReceiveBuffer, 0, packetLength);
			if (dataReceivedLength == packetLength) {
				Log.d(TAG, "received data is on packet");
				// received data is ok.
				iCommunicate.receiveMessage(Arrays.copyOfRange(mReceiveBuffer,
						0, dataReceivedLength), this);
				return;
			} else if (dataReceivedLength < packetLength) {
				Log.d(TAG,
						"received data is less than on package. packageLength = "
								+ packetLength + ", dataReceivedLength = "
								+ dataReceivedLength);
				// received data is less than on packet.
				// 1. Save last packet length.
				mLastPacketLength = packetLength;
				// 2. Save the received data into buff.
				mRemainPacket = Arrays.copyOfRange(mReceiveBuffer, 0,
						dataReceivedLength);
			} else {
				// should not be here
				Log.e(TAG, "Decode data error.");
			}
		} else {
			Log.d(TAG, "There is remain packet left");
			// There is remain packet left.
			Log.d(TAG, "mLastPacketLength - mRemainPacket.length = "
					+ (mLastPacketLength - mRemainPacket.length));
			if (mLastPacketLength > RECEIVE_BUFFER_SIZE) {
				Log.e(TAG,
						"Decode header error. packageLength is too long. mLastPacketLength = "
								+ mLastPacketLength);
				mReceiveBuffer = null;
				return;
			}
			// Read the remain packet data.
			dataReceivedLength = in.read(mReceiveBuffer, 0, mLastPacketLength
					- mRemainPacket.length);
			if (dataReceivedLength + mRemainPacket.length == mLastPacketLength) {
				Log.d(TAG,
						"remain packet + received data is one packet.mLastPacketLength = "
								+ mLastPacketLength);
				// remain packet + received data is one packet.
				iCommunicate.receiveMessage(
						ArrayUtil.join(mRemainPacket, Arrays.copyOfRange(
								mHeadBuffer, 0, dataReceivedLength)), this);
				mRemainPacket = null;
				return;
			} else if (dataReceivedLength + mRemainPacket.length < mLastPacketLength) {
				Log.d(TAG,
						"remain packet + received data is less than one packet. mLastPacketLength = "
								+ mLastPacketLength);
				// remain packet + received data is less than one packet.
				// Save the received data into buff.
				mRemainPacket = ArrayUtil.join(mRemainPacket, Arrays
						.copyOfRange(mReceiveBuffer, 0, dataReceivedLength));
			} else {
				// Should not be here.
				Log.e(TAG, "parse data error.");
			}
		}
	}

	/**
	 * Add 4 bits msg length header before msg.</br>
	 * 
	 * Before encode: [msg1][msg2][msg3]</br>
	 * 
	 * After encode: [header1+msg1][header2+msg2][header3+msg3]</br>
	 * 
	 * @param msg
	 * @return encoded msg.
	 */
	private byte[] encode(byte[] msg) {
		byte[] result = ArrayUtil
				.join(ArrayUtil.int2ByteArray(msg.length), msg);
		return result;
	}

	public void sendMsg(byte[] msg) {

		try {
			// msg = this.getName() + " : " + msg;
			if (dataOutputStream != null) {
				dataOutputStream.write(encode(msg));
				dataOutputStream.flush();
			} else {
				mListener.OnCommunicationLost(this);
			}
		} catch (IOException e) {
			e.printStackTrace();
			mListener.OnCommunicationLost(this);
		}
	}

	/**
	 * send file
	 * 
	 * @param file
	 */
	public void sendMsg(File file) {
		Log.d(TAG, "sendMsg-->" + file.getName());
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(file)));
			Log.d(TAG, "open file ok.");
			if (dataOutputStream != null) {
				Log.d(TAG, "Connection is ok");
				int bufferSize = 4 * 1024;
				byte[] buf = new byte[bufferSize];
				int read_len = 0;
				int total = 0;
				// if file size is 0bytes,cannot into while xunhuan
				// and then the client will crash
				// so we do something error handler
				if (file.length() == 0) {
					Log.d(TAG, "the file's size is 0");
					// 这样做，虽然客户端不会卡住进度条，
					// 但是不应该write 0 吧，0是1bytes啊 大小变了
					// 暂时先这样，解决0bytes卡主bug再说
					dataOutputStream.write(0);
					dataOutputStream.flush();
				} else {
					while ((read_len = dis.read(buf)) != -1) {
						Log.d(TAG, "read_len = " + read_len);
						if (read_len < bufferSize) {
							Log.d(TAG, "send file: " + read_len);
							// read length less than buff.
							sendMsg(Arrays.copyOfRange(buf, 0, read_len));
						} else {
							Log.d(TAG, "send file: " + read_len);
							// read length less than buff.
							sendMsg(buf);
						}
					}
					Log.d(TAG, "read_len11 = " + read_len);
				}
			} else {
				mListener.OnCommunicationLost(this);
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found exception:" + e.toString());
			e.printStackTrace();
			mListener.OnCommunicationLost(this);
		} catch (IOException e) {
			Log.e(TAG, "IO exception:" + e.toString());
			e.printStackTrace();
			mListener.OnCommunicationLost(this);
		}
	}

	public void stopComunication() {
		try {
			socket.close();

			if (dataInputStream != null && dataOutputStream != null) {
				dataInputStream.close();
				dataOutputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}