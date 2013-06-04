package com.dreamlink.communication.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchSever implements Runnable {
	interface OnSearchListener {
		void onSearchSuccess(String serverIP);

		void onSearchFail();

		void onOffLine(String serverIP);
	}

	private static final String TAG = "SearchSever";

	// Broadcast address.
	private InetAddress mBroadAddress;
	// Socket for receive broadcast message.
	private MulticastSocket mReceiveSocket;
	// Socket for send broadcast message.
	private DatagramSocket mSenderSocket;

	private String mMessage;
	private String mIP;
	private String mHostName;

	private OnSearchListener mListener;

	private boolean mStopped = false;

	public SearchSever(OnSearchListener listener) {
		try {
			mReceiveSocket = new MulticastSocket(Search.BROADCAST_INT_PORT);
			mReceiveSocket.setSoTimeout(Search.TIME_OUT);
			mBroadAddress = InetAddress.getByName(Search.BROADCAST_IP);
			mSenderSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mListener = listener;
	}

	@Override
	public void run() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			mIP = addr.getHostAddress().toString();
			mHostName = addr.getHostName().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		mMessage = mIP + "@" + mHostName;

		join(mBroadAddress);
		startListenServerMessage();
		sendGetServerMessage();
	}

	public void startSearch(Context context) {
		// TODO
		NetWorkUtil.acquireWifiMultiCastLock(context);
		Thread searchThread = new Thread(this);
		searchThread.start();
	}

	public void stopSearch() {
		mStopped = true;
		NetWorkUtil.releaseWifiMultiCastLock();
	}

	/**
	 * Join broadcast group.
	 */
	private void join(InetAddress groupAddr) {
		try {
			// Join broadcast group.
			mReceiveSocket.joinGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "Join group fail");
		}
	}

	/**
	 * Listen server message.
	 */
	private void startListenServerMessage() {
		new Thread(new GetPacket()).start();
	}

	// Broadcast message to find server.
	private void sendGetServerMessage() {
		byte[] b = new byte[1024];
		DatagramPacket packet;
		try {
			b = ("find@" + mMessage).getBytes();
			// Broad cast message to port.
			packet = new DatagramPacket(b, b.length, mBroadAddress,
					Search.BROADCAST_INT_PORT);
			mSenderSocket.send(packet);
		} catch (Exception e) {
		}
	}

	/**
	 * Get message from server.
	 * 
	 */
	class GetPacket implements Runnable {
		public void run() {
			DatagramPacket inPacket;

			String[] message;
			while (!mStopped) {
				try {
					inPacket = new DatagramPacket(new byte[1024], 1024);
					mReceiveSocket.receive(inPacket); // 接收广播信息并将信息封装到inPacket中
					message = new String(inPacket.getData(), 0,
							inPacket.getLength()).split("@"); // 获取信息，并切割头部，判断是何种信息（find--上线，retn--回答，offl--下线）

					if (message[1].equals(mIP))
						// ignore myself.
						continue;
					if (message[0].equals("find")) {
						// message: find。
						Log.d(TAG, "find Server, ip: " + message[1]
								+ ", server ip: " + message[2]);
						// returnUserMsg(message[1]);
					} else if (message[0].equals("retn")) {
						// message from server.
						Log.d(TAG, "Found Server success, ip: " + message[1]
								+ ", server ip: " + message[2]);
						if (mListener != null) {
							mListener.onSearchSuccess(message[2]);
						}
					} else if (message[0].equals("offl")) {
						// off line message
						Log.d(TAG, "Server off line , ip: " + message[1]
								+ ", server ip: " + message[2]);
						if (mListener != null) {
							mListener.onOffLine(message[2]);
						}
					}

				} catch (Exception e) {
					Log.e(TAG, "GetPacket error," + e.toString());
					e.printStackTrace();
					if (mListener != null) {
						mListener.onSearchFail();
					}
				}
			}
		}
	}

}
