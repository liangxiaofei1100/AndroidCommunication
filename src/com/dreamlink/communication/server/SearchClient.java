package com.dreamlink.communication.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import android.content.Context;

import com.dreamlink.communication.Search;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class SearchClient implements Runnable {
	interface OnSearchListener {
		void onSearchSuccess(String clientIP);

		void onSearchFail();

		void onOffLine(String clinetIP);
	}

	private static final String TAG = "SearchClient";

	// Broadcast address.
	private InetAddress broadAddress;
	// Socket for receive broadcast message.
	private MulticastSocket broadSocket;
	// Socket for send broadcast message.
	private DatagramSocket sender;

	private String msg;
	private String ip;
	private String hostName;

	private OnSearchListener mListener;
	private boolean mStopped = false;

	public SearchClient(OnSearchListener listener) {
		try {
			broadSocket = new MulticastSocket(Search.BROADCAST_INT_PORT);
			broadSocket.setSoTimeout(Search.TIME_OUT);
			broadAddress = InetAddress.getByName(Search.BROADCAST_IP);
			sender = new DatagramSocket();
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
			ip = addr.getHostAddress().toString();
			hostName = addr.getHostName().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		msg = ip + "@" + hostName;

		join(broadAddress);
		startListenServerMessage();
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

			String[] message;
			while (!mStopped) {
				try {
					inPacket = new DatagramPacket(new byte[1024], 1024);
					broadSocket.receive(inPacket); // ���չ㲥��Ϣ������Ϣ��װ��inPacket��
					message = new String(inPacket.getData(), 0,
							inPacket.getLength()).split("@"); // ��ȡ��Ϣ�����и�ͷ�����ж��Ǻ�����Ϣ��find--���ߣ�retn--�ش�offl--���ߣ�

					if (message[1].equals(ip))
						// ignore myself.
						continue;
					if (message[0].equals("find")) {
						// message: find��
						Log.d(TAG, "find client success, ip: " + message[1]
								+ ", client ip: " + message[2]);
						if (mListener != null) {
							mListener.onSearchSuccess(message[2]);
						}

						returnUserMsg(message[1]);
					} else if (message[0].equals("offl")) { // �����������Ϣ
						// Message: off line.
						Log.d(TAG, "off line, ip: " + message[1]
								+ ", client ip: " + message[2]);
						if (mListener != null) {
							mListener.onOffLine(message[2]);
						}
					}

				} catch (Exception e) {
					Log.e(TAG, "GetPacket error. " + e.toString());
					if (mListener != null) {
						mListener.onSearchFail();
					}
				}
			}
		}
	}

	// ���������ڵ����߻����յ��㲥��Ϣʱ��Ӧ�����͹㲥��ip��ַ�������ͷ�����Ϣ���ﵽ������Ϣ��Ŀ��
	private void returnUserMsg(String ip) {
		byte[] b = new byte[1024];
		DatagramPacket packet;
		try {
			b = ("retn@" + msg).getBytes();
			packet = new DatagramPacket(b, b.length, InetAddress.getByName(ip),
					Search.BROADCAST_INT_PORT);
			sender.send(packet);
			Log.d(TAG, "returnUserMsg sucess: " + ip);
		} catch (Exception e) {
			Log.e(TAG, "returnUserMsg fail: " + ip);
		}
	}

}
