package com.dreamlink.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;

import android.os.Handler;
import android.os.Message;

public class SocketCommunication extends Thread {

	public interface OnCommunicationChangedListener {
		void OnCommunicationEstablished(SocketCommunication communication);

		void OnCommunicationLost(SocketCommunication communication);
	}

	private boolean clientFlag = false;
	private int whatMsgSocket;
	private int whatMsgNotice;

	private Socket socket;
	private Handler handler;

	private DataInputStream dataInputStream = null;
	private DataOutputStream dataOutputStream = null;

	private OnCommunicationChangedListener mListener;

	public SocketCommunication(Socket socket, Handler handler,
			int whatMsgSocket, int whatMsgNotice) {
		this.socket = socket;
		this.handler = handler;
		this.whatMsgSocket = whatMsgSocket;
		this.whatMsgNotice = whatMsgNotice;
	}

	public void setClientFlag(boolean flag) {
		clientFlag = flag;
	}
	public InetAddress getConnectIP(){
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

			while (true) {
				if (dataInputStream.available() > 0) {
					byte[] msg = new byte[dataInputStream.available()];
					dataInputStream.read(msg, 0, dataInputStream.available());

					sendHandler(whatMsgSocket, new String(msg));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();

			dataInputStream = null;
			dataOutputStream = null;

			mListener.OnCommunicationLost(this);

			sendHandler(whatMsgNotice, "Get message fail");
		}
	}

	public void sendHandler(int what, Object object) {
		Message message = handler.obtainMessage(what, object);
		message.arg1 = (int) this.getId();
		message.sendToTarget();
		// handler.obtainMessage(what, object).sendToTarget();
	}

	public void sendMsg(String msg) {
		try {
			if (dataOutputStream != null) {
				dataOutputStream.write(msg.getBytes());
				dataOutputStream.flush();
			} else {
				mListener.OnCommunicationLost(this);
				sendHandler(whatMsgNotice, "Connection lost.");
			}

		} catch (IOException e) {
			e.printStackTrace();
			mListener.OnCommunicationLost(this);
			sendHandler(whatMsgNotice, "Send fail");
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