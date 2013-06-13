package com.dreamlink.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;

import android.os.Handler;
import android.os.Message;

public class SocketCommunication extends Thread {
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
			while (true) {
				if (dataInputStream.available() > 0) {
					byte[] msg = new byte[dataInputStream.available()];
					dataInputStream.read(msg, 0, dataInputStream.available());
					iCommunicate.receiveMessage(msg, this);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			dataInputStream = null;
			dataOutputStream = null;
			mListener.OnCommunicationLost(this);

		}
	}

	public void sendMsg(byte[] msg) {
		try {
//			msg = this.getName() + " : " + msg;
			if (dataOutputStream != null) {
				dataOutputStream.write(msg);
				dataOutputStream.flush();
			} else {
				mListener.OnCommunicationLost(this);
			}

		} catch (IOException e) {
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