package com.dreamlink.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.io.IOException;

import com.dreamlink.communication.TrafficStaticInterface.TrafficStaticsRxListener;
import com.dreamlink.communication.TrafficStaticInterface.TrafficStaticsTxListener;
import com.dreamlink.communication.lib.util.ArrayUtil;
import com.dreamlink.communication.util.Log;

/**
 * Thread for send and receive message.</br>
 * 
 * Before send packet, add packet size header to ensure the received packet is
 * the right packet. The packet with size header is like [packet size][packet
 * data]. After received packet, remove the packet size header to get the
 * original packet. For Details, see {@link #encode(byte[])} and
 * {@link #decode(DataInputStream)} </br>
 * 
 */
public class SocketCommunication extends Thread {
	private static final String TAG = "SocketCommunication";
	/** Socket server port */
	public static final int PORT = SocketPort.COMMUNICATION_SERVER_PORT;

	/**
	 * Listen socket connect and disconnect event.
	 * 
	 */
	public interface OnCommunicationChangedListener {
		/**
		 * There is a new socket connected, and the communication is ready.
		 * 
		 * @param communication
		 *            The established communication.
		 */
		void OnCommunicationEstablished(SocketCommunication communication);

		/**
		 * There is socket disconnected, and the commnunication is lost.
		 * 
		 * @param communication
		 */
		void OnCommunicationLost(SocketCommunication communication);
	}

	/**
	 * Call back listener for notify there is a message received.
	 * 
	 */
	public interface OnReceiveMessageListener {
		void onReceiveMessage(byte[] msg,
				SocketCommunication socketCommunication);
	}

	private Socket mSocket;
	private OnReceiveMessageListener mOnReceiveMessageListener;

	private DataInputStream mDataInputStream = null;
	private DataOutputStream mDataOutputStream = null;

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

	private TrafficStaticsRxListener mRxListener = TrafficStatics.getInstance();
	private TrafficStaticsTxListener mTxListener = TrafficStatics.getInstance();

	public SocketCommunication(Socket socket, OnReceiveMessageListener listener) {
		this.mSocket = socket;
		this.mOnReceiveMessageListener = listener;
	}

	public InetAddress getConnectedAddress() {
		return mSocket.getInetAddress();
	}

	public void setOnCommunicationChangedListener(
			OnCommunicationChangedListener listener) {
		mListener = listener;
	}

	@Override
	public void run() {

		try {
			mDataInputStream = new DataInputStream(mSocket.getInputStream());
			mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
			mListener.OnCommunicationEstablished(this);
			mReceiveBuffer = new byte[RECEIVE_BUFFER_SIZE];
			while (!mSocket.isClosed()) {
				boolean isContinue = decode(mDataInputStream);
				if (!isContinue) {
					mListener.OnCommunicationLost(this);
					break;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Read socket error. " + e);
			mDataInputStream = null;
			mDataOutputStream = null;
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
	 * @return true: continue decode, false : stop decode.
	 * @throws IOException
	 */
	private synchronized boolean decode(DataInputStream in) throws IOException {
		int dataReceivedLength = 0;
		if (mRemainPacket == null) {
			// There is no remain packet.
			// 1. Get the first 4 bit header for packet length.
			int packetLength = 0;
			if (mRemainHeader == null) {
				// There is no remain head.
				dataReceivedLength = in.read(mHeadBuffer);
				if (dataReceivedLength == -1) {
					Log.d(TAG, "Connection lost. dataReceivedLength = -1");
					return false;
				}
				if (dataReceivedLength < HEAD_SIZE) {
					// Received data is less than one header data, Save it and
					// return for next read.
					mRemainHeader = Arrays.copyOfRange(mHeadBuffer, 0,
							dataReceivedLength);
					return true;
				} else if (dataReceivedLength == HEAD_SIZE) {
					// Get the packet length.
					packetLength = ArrayUtil.byteArray2Int(mHeadBuffer);
					mRemainHeader = null;
				}
			} else {
				// There is remain header data left.
				// Read the remain header data to get the header data.
				dataReceivedLength = in.read(mHeadBuffer, 0, HEAD_SIZE
						- mRemainHeader.length);
				if (dataReceivedLength == -1) {
					Log.d(TAG, "Connection lost. dataReceivedLength = -1");
					return false;
				}
				if (mRemainHeader.length + dataReceivedLength < HEAD_SIZE) {
					// remain head + data received less than one header data.
					// Save the header data and return for next read.
					mRemainHeader = ArrayUtil.join(mRemainHeader, Arrays
							.copyOfRange(mHeadBuffer, 0, dataReceivedLength));
					return true;
				} else if (mRemainHeader.length + dataReceivedLength == HEAD_SIZE) {
					// remain head + data received is one header
					mHeadBuffer = ArrayUtil.join(mRemainHeader, Arrays
							.copyOfRange(mHeadBuffer, 0, dataReceivedLength));
					packetLength = ArrayUtil.byteArray2Int(mHeadBuffer);
					mRemainHeader = null;
				}
			}
			if (packetLength > RECEIVE_BUFFER_SIZE || packetLength < 1) {
				// Decode header error.
				Log.e(TAG,
						"Decode header error. packageLength is bad number. packageLength = "
								+ packetLength);
				return true;
			}

			// 2. Read received data.
			dataReceivedLength = in.read(mReceiveBuffer, 0, packetLength);
			if (dataReceivedLength == -1) {
				Log.d(TAG, "Connection lost. dataReceivedLength = -1");
				return false;
			}
			if (dataReceivedLength == packetLength) {
				// Received data is just one packet. Return for next read.
				receiveMessage(Arrays.copyOfRange(mReceiveBuffer, 0,
						dataReceivedLength));
				return true;
			} else if (dataReceivedLength < packetLength) {
				// Received data is less than one packet.
				// 1. Save last packet length.
				mLastPacketLength = packetLength;
				// 2. Save the received data into buff.
				mRemainPacket = Arrays.copyOfRange(mReceiveBuffer, 0,
						dataReceivedLength);
			}
		} else {
			// There is remain packet left.
			if (mLastPacketLength > RECEIVE_BUFFER_SIZE) {
				// Packet length error. drop it.
				mReceiveBuffer = null;
				return true;
			}
			// Read the remain packet data.
			dataReceivedLength = in.read(mReceiveBuffer, 0, mLastPacketLength
					- mRemainPacket.length);
			if (dataReceivedLength == -1) {
				Log.d(TAG, "Connection lost. dataReceivedLength = -1");
				return false;
			}
			if (dataReceivedLength + mRemainPacket.length == mLastPacketLength) {
				// remain packet + received data is one packet.
				receiveMessage(ArrayUtil.join(mRemainPacket, Arrays
						.copyOfRange(mReceiveBuffer, 0, dataReceivedLength)));
				mRemainPacket = null;
				return true;
			} else if (dataReceivedLength + mRemainPacket.length < mLastPacketLength) {
				// remain packet + received data is less than one packet.
				// Save the received data into buff.
				mRemainPacket = ArrayUtil.join(mRemainPacket, Arrays
						.copyOfRange(mReceiveBuffer, 0, dataReceivedLength));
			}
		}
		return true;
	}

	private void receiveMessage(byte[] msg) {
		mOnReceiveMessageListener.onReceiveMessage(msg, this);
		mRxListener.addRxBytes(msg.length);
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
		Log.d(TAG, "encode() msg.length = " + msg.length);
		return result;
	}

	/**
	 * Send message to the connected socket
	 * 
	 * @param msg
	 * @return return true if send success, return false if send fail.
	 */
	public boolean sendMessage(byte[] msg) {
		try {
			if (mDataOutputStream != null) {
				mDataOutputStream.write(encode(msg));
				mDataOutputStream.flush();

				mTxListener.addTxBytes(msg.length);
			} else {
				mListener.OnCommunicationLost(this);
				return false;
			}
		} catch (IOException e) {
			Log.e(TAG, "sendMessage fail. msg = " + msg + ", error = " + e);
			mListener.OnCommunicationLost(this);
		}
		return true;
	}

	/**
	 * Disconnect from the connected socket and stop communication.
	 */
	public void stopComunication() {
		try {
			if (mDataInputStream != null && mDataOutputStream != null) {
				mDataInputStream.close();
				mDataOutputStream.close();
				mListener.OnCommunicationLost(this);
			}
			mSocket.close();
		} catch (IOException e) {
			Log.e(TAG, "stopComunication fail." + e);
		} catch (Exception e) {
			Log.e(TAG, "stopComunication fail." + e);
		}
	}

}