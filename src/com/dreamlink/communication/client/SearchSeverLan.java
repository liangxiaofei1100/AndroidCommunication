package com.dreamlink.communication.client;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchSeverLan implements Runnable {

	private static final String TAG = "SearchSeverLan";

	// multicast address.
	private InetAddress mMulticastAddress;
	// Socket for receive multicast message.
	private MulticastSocket mReceiveSocket;

	private OnSearchListener mListener;

	private boolean mStopped = false;
	private boolean mStarted = false;

	private static SearchSeverLan mInstance;

	private Context mContext;

	public static SearchSeverLan getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SearchSeverLan(context);
		}
		return mInstance;
	}

	private SearchSeverLan(Context context) {
		mContext = context;
	}

	public void setOnSearchListener(OnSearchListener listener) {
		mListener = listener;
	}

	@Override
	public void run() {
		try {
			mReceiveSocket = new MulticastSocket(Search.MULTICAST_RECEIVE_PORT);
			mReceiveSocket.setSoTimeout(Search.TIME_OUT);
			mMulticastAddress = InetAddress.getByName(Search.MULTICAST_IP);
		} catch (Exception e) {
			e.printStackTrace();
		}

		join(mMulticastAddress);
		startListenServerMessage();
	}

	public void startSearch() {
		Log.d(TAG, "Start search.");
		if (mStarted) {
			Log.d(TAG, "startSearch() ignore, search is already started.");
			return;
		}
		mStarted = true;
		NetWorkUtil.acquireWifiMultiCastLock(mContext);
		Thread searchThread = new Thread(this);
		searchThread.start();
	}

	public void stopSearch() {
		Log.d(TAG, "Stop search");
		mStarted = false;
		mStopped = true;
		leaveGroup(mMulticastAddress);
		NetWorkUtil.releaseWifiMultiCastLock();

		closeSocket();
		mInstance = null;
	}

	private void closeSocket() {
		if (mReceiveSocket != null) {
			mReceiveSocket.close();
			mReceiveSocket = null;
		}
	}

	/**
	 * Join broadcast group.
	 */
	private void join(InetAddress groupAddr) {
		try {
			// Join broadcast group.
			mReceiveSocket.joinGroup(groupAddr);
		} catch (Exception e) {
			Log.e(TAG, "Join group fail. " + e.toString());
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
			Log.e(TAG, "leave group fail. " + e.toString());
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
							mListener.onSearchStop();
						}
					}
				}
			}
		}
	}
}
