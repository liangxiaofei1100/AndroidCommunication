package com.dreamlink.communication.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

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
	private MulticastSocket broadSocket;
	// Socket for send broadcast message.
	private DatagramSocket mSocket;
	private DatagramPacket mPacket;

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
			broadSocket = new MulticastSocket(Search.BROADCAST_RECEIVE_PORT);
			broadSocket.setSoTimeout(Search.TIME_OUT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mListener = listener;
	}

	@Override
	public void run() {
		try {
			join(InetAddress.getByName(Search.BROADCAST_IP));
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		startListenServerMessage();
		try {
			mSocket = new DatagramSocket(Search.BROADCAST_SEND_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		byte[] buffer = NetWorkUtil.getLocalIpAddress().getBytes();
		try {
			mPacket = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(Search.BROADCAST_IP),
					Search.BROADCAST_RECEIVE_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		while (!mStopped) {
			startBroadcastData();
		}
	}

	private void startBroadcastData() {

		try {
			mSocket.send(mPacket);
			Log.d(TAG,
					"Send broadcast ok, data = "
							+ new String(mPacket.getData()));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Send broadcast fail, data = " + mPacket.getData());
		}

		try {
			Thread.sleep(Search.BROADCAST_SLEEP_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void startSearch(Context context) {
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
		mStarted = false;
		mStopped = true;
		NetWorkUtil.releaseWifiMultiCastLock();
	}

	/**
	 * Join broadcast group.
	 */
	private void join(InetAddress groupAddr) {
		try {
			// Join broadcast group.
			broadSocket.joinGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "Join group fail");
		}
	}

	/**
	 * Listen client message.
	 */
	private void startListenServerMessage() {
		new Thread(new GetPacket()).start();

	}

	/**
	 * Get message from client.
	 * 
	 */
	class GetPacket implements Runnable {
		public void run() {
			DatagramPacket inPacket;

			String message;
			while (!mStopped) {
				try {
					inPacket = new DatagramPacket(new byte[1024], 1024);
					broadSocket.receive(inPacket);
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
