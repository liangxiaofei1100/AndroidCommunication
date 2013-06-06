package com.dreamlink.communication.client;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchSever implements Runnable {
	interface OnSearchListener {
		void onSearchSuccess(String serverIP);

		void onSearchFail();
	}

	private static final String TAG = "SearchSever";

	// Broadcast address.
	private InetAddress mBroadAddress;
	// Socket for receive broadcast message.
	private MulticastSocket mReceiveSocket;

	private OnSearchListener mListener;

	private boolean mStopped = false;
	private boolean mStarted = false;

	private static SearchSever mInstance;

	public static SearchSever getInstance(OnSearchListener listener) {
		if (mInstance == null) {
			mInstance = new SearchSever(listener);
		}
		return mInstance;
	}

	private SearchSever(OnSearchListener listener) {
		try {
			mReceiveSocket = new MulticastSocket(Search.BROADCAST_RECEIVE_PORT);
			mReceiveSocket.setSoTimeout(Search.TIME_OUT);
			mBroadAddress = InetAddress.getByName(Search.BROADCAST_IP);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mListener = listener;
	}

	@Override
	public void run() {
		join(mBroadAddress);
		startListenServerMessage();
	}

	public void startSearch(Context context) {
		Log.d(TAG, "Start search.");
		if (mStarted) {
			Log.d(TAG, "startSearch() ignore, search is already started.");
			return;
		}
		mStarted = true;
		NetWorkUtil.acquireWifiMultiCastLock(context);
		Thread searchThread = new Thread(this);
		searchThread.start();
	}

	public void stopSearch() {
		Log.d(TAG, "Stop search");
		mStarted = false;
		mStopped = true;
		leaveGroup(mBroadAddress);
		NetWorkUtil.releaseWifiMultiCastLock();

		mInstance = null;
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
	 * Leave broadcast group.
	 */
	private void leaveGroup(InetAddress groupAddr) {
		try {
			// leave broadcast group.
			mReceiveSocket.leaveGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "leave group fail");
		}
	}

	/**
	 * Listen server message.
	 */
	private void startListenServerMessage() {
		new Thread(new GetPacket()).start();
	}

	/**
	 * Get message from server.
	 * 
	 */
	class GetPacket implements Runnable {
		public void run() {
			DatagramPacket inPacket;

			String message;
			while (!mStopped) {
				try {
					inPacket = new DatagramPacket(new byte[1024], 1024);
					mReceiveSocket.receive(inPacket);
					message = new String(inPacket.getData(), 0,
							inPacket.getLength());
					Log.d(TAG, "Received broadcast, message: " + message);

					if (message.equals(NetWorkUtil.getLocalIpAddress())) {
						// ignore myself.
					} else {
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
