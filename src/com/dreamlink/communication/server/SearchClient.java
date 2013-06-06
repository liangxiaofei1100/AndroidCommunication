package com.dreamlink.communication.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchClient implements Runnable {
	interface OnSearchListener {
		void onSearchSuccess(String serverIP);

		void onSearchFail();
	}

	private static final String TAG = "SearchClient";

	// Broadcast address.
	// Socket for receive broadcast message.
	private MulticastSocket mBroadcastReceiveSocket;
	// Socket for send broadcast message.
	private DatagramSocket mSocket;

	private OnSearchListener mListener;
	private boolean mStopped = false;
	private boolean mStarted = false;

	private static SearchClient mInstance;

	public static SearchClient getInstance(OnSearchListener listener) {
		if (mInstance == null) {
			mInstance = new SearchClient(listener);
		}
		return mInstance;
	}

	private SearchClient(OnSearchListener listener) {
		try {
			mBroadcastReceiveSocket = new MulticastSocket(
					Search.BROADCAST_RECEIVE_PORT);
			mBroadcastReceiveSocket.setSoTimeout(Search.TIME_OUT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mListener = listener;
	}

	@Override
	public void run() {
		// Listen other server messages.
		joinGroup(Search.BROADCAST_IP);
		new Thread(new GetBroadcastPacket()).start();

		// Broadcast our message.
		try {
			mSocket = new DatagramSocket(Search.BROADCAST_SEND_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		DatagramPacket packet = null;
		byte[] localIPAddresss = NetWorkUtil.getLocalIpAddress().getBytes();
		try {
			packet = new DatagramPacket(localIPAddresss,
					localIPAddresss.length,
					InetAddress.getByName(Search.BROADCAST_IP),
					Search.BROADCAST_RECEIVE_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		while (!mStopped) {
			startBroadcastData(packet);
		}
	}

	private void startBroadcastData(DatagramPacket packet) {
		if (mSocket == null) {
			Log.e(TAG, "startBroadcastData() fail, mSocket is null");
			return;
		}
		try {
			mSocket.send(packet);
			Log.d(TAG,
					"Send broadcast ok, data = " + new String(packet.getData()));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,
					"Send broadcast fail, data = "
							+ new String(packet.getData()));
		}

		try {
			Thread.sleep(Search.BROADCAST_SLEEP_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void startSearch(Context context) {
		Log.d(TAG, "Start search");
		if (mStarted) {
			Log.d(TAG, "startSearch() igonre, search is already started.");
			return;
		}
		mStarted = true;
		NetWorkUtil.acquireWifiMultiCastLock(context);

		Thread searchThread = new Thread(this);
		searchThread.start();
	}

	public void stopSearch() {
		Log.d(TAG, "Stop search.");
		mStarted = false;
		mStopped = true;
		closeSocket();
		NetWorkUtil.releaseWifiMultiCastLock();

		mInstance = null;
	}

	private void closeSocket() {
		if (mSocket != null) {
			mSocket.close();
		}

		if (mBroadcastReceiveSocket != null) {
			leaveGroup(Search.BROADCAST_IP);
			mBroadcastReceiveSocket.close();
		}
	}

	/**
	 * Join broadcast group.
	 */
	private void joinGroup(String broadcastIP) {
		try {
			InetAddress groupAddr = InetAddress.getByName(broadcastIP);
			// Join broadcast group.
			mBroadcastReceiveSocket.joinGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "Join group fail");
		}
	}

	/**
	 * Leave broadcast group.
	 */
	private void leaveGroup(String broadcastIP) {
		try {
			InetAddress groupAddr = InetAddress.getByName(broadcastIP);
			// leave broadcast group.
			mBroadcastReceiveSocket.leaveGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "leave group fail");
		}
	}

	/**
	 * Get message from other server broadcast.
	 * 
	 */
	class GetBroadcastPacket implements Runnable {

		public void run() {
			DatagramPacket inPacket;

			String message;
			while (!mStopped) {
				try {
					inPacket = new DatagramPacket(new byte[1024], 1024);
					mBroadcastReceiveSocket.receive(inPacket);
					message = new String(inPacket.getData(), 0,
							inPacket.getLength());
					Log.d(TAG, "Received broadcast message: " + message);

					if (message.equals(NetWorkUtil.getLocalIpAddress())) {
						// ignore.
					} else {
						// Got another server. because client will not broadcast
						// message.
						if (mListener != null) {
							mListener.onSearchSuccess(message);
						}
					}
				} catch (Exception e) {
					if (e instanceof SocketTimeoutException) {
						// time out, search again.
						Log.d(TAG, "GetPacket time out. search again.");
					} else {
						Log.e(TAG, "GetPacket error," + e.toString());
						if (mListener != null) {
							mListener.onSearchFail();
						}
					}
				}
			}
		}
	}
}
