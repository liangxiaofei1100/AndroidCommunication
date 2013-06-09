package com.dreamlink.communication.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchSeverLanAndroidAP implements Runnable {

	private static final String TAG = "SearchSeverLanAndroidAP";

	// Socket for receive server respond.
	private DatagramSocket mReceiveRespondSocket;

	// Address for send search request.
	private InetAddress mSendRequestAddress;
	// Socket for send search request.
	private DatagramSocket mSendRequestSocket;
	private DatagramPacket mSendRequestPacket;

	private OnSearchListener mListener;

	private boolean mStopped = false;
	private boolean mStarted = false;

	private static SearchSeverLanAndroidAP mInstance;

	private Context mContext;

	public static SearchSeverLanAndroidAP getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SearchSeverLanAndroidAP(context);
		}
		return mInstance;
	}

	private SearchSeverLanAndroidAP(Context context) {
		mContext = context;
	}

	public void setOnSearchListener(OnSearchListener listener) {
		mListener = listener;
	}

	@Override
	public void run() {
		try {
			mReceiveRespondSocket = new DatagramSocket(
					Search.ANDROID_AP_RECEIVE_PORT);
			mReceiveRespondSocket.setSoTimeout(Search.TIME_OUT);
			// request ip
			mSendRequestAddress = InetAddress
					.getByName(Search.ANDROID_AP_ADDRESS);
			mSendRequestSocket = new DatagramSocket();
			// request data
			byte[] request = Search.ANDROID_AP_CLIENT_REQUEST.getBytes();
			mSendRequestPacket = new DatagramPacket(request, request.length,
					mSendRequestAddress, Search.ANDROID_AP_RECEIVE_PORT);
		} catch (Exception e) {
			Log.e(TAG, "SearchSeverAPMode error" + e);
		}

		startListenServerMessage();

		if (!NetWorkUtil.isWifiApEnabled(mContext)) {
			while (!mStopped) {
				sendSearchRequest();
				try {
					Thread.sleep(Search.ANDROID_AP_SEARCH_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void sendSearchRequest() {
		if (mSendRequestSocket != null) {
			try {
				mSendRequestSocket.send(mSendRequestPacket);
				Log.d(TAG, "Send broadcast ok, data = "
						+ new String(mSendRequestPacket.getData()));
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "Send broadcast fail, data = "
						+ new String(mSendRequestPacket.getData()) + " " + e);
			}
		} else {
			Log.e(TAG, "sendSearchRequest() fail, mSendRequestSocket is null");
		}
	}

	public void startSearch() {
		Log.d(TAG, "Start search.");
		if (mStarted) {
			Log.d(TAG, "startSearch() ignore, search is already started.");
			return;
		}
		mStarted = true;
		Thread searchThread = new Thread(this);
		searchThread.start();
	}

	public void stopSearch() {
		Log.d(TAG, "Stop search");
		mStarted = false;
		mStopped = true;

		closeSocket();

		mInstance = null;
	}

	private void closeSocket() {
		if (mReceiveRespondSocket != null) {
			mReceiveRespondSocket.close();
			mReceiveRespondSocket = null;
		}
		if (mSendRequestSocket != null) {
			mSendRequestSocket.close();
			mSendRequestSocket = null;
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
					mReceiveRespondSocket.receive(inPacket);
					message = new String(inPacket.getData(), 0,
							inPacket.getLength());
					Log.d(TAG, "Received broadcast, message: " + message);

					if (message.equals(Search.ANDROID_AP_SERVER_RESPOND)) {
						// Android AP is server.
						Log.d(TAG, "Android AP is server.");
						if (mListener != null) {
							mListener.onSearchSuccess(inPacket.getAddress()
									.getHostAddress());
						}
					} else if (message
							.startsWith(Search.ANDROID_AP_SERVER_REQUEST)) {
						Log.d(TAG, "This client is an AP. Found a server.");
						if (mListener != null) {
							mListener.onSearchSuccess(inPacket.getAddress()
									.getHostAddress());
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
