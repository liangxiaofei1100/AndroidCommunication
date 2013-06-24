package com.dreamlink.communication;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;

import com.dreamlink.communication.util.Log;

import android.os.Handler;
import android.os.Message;

public class SocketCommunication extends Thread {
	private static final String TAG = "SocketCommunication";
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
//					synchronized (dataInputStream) {
						int length = dataInputStream.available();
						byte[] msg = new byte[length];
						dataInputStream.read(msg, 0, msg.length);
						iCommunicate.receiveMessage(msg, this);
//					}
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
	
	/**
	 * send file 
	 * @param file
	 */
	public void sendMsg(File file){
		Log.d(TAG, "sendMsg-->" + file.getName());
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			
			if (dataOutputStream != null) {
				int bufferSize = 1024;
				byte[] buf = new byte[bufferSize];
				int read_len = 0;
				int total = 0;
				//if file size is 0bytes,cannot into while xunhuan
				//and then the client will crash
				//so we do something error handler
				if (file.length() == 0) {
					Log.d(TAG, "the file's size is 0");
					//这样做，虽然客户端不会卡住进度条，
					//但是不应该write 0 吧，0是1bytes啊 大小变了
					//暂时先这样，解决0bytes卡主bug再说
					dataOutputStream.write(0);
					dataOutputStream.flush();
				}else {
					while ((read_len = dis.read(buf)) != -1) {
						//for debug
//						total += read_len;
//						Log.d(TAG, "total=" + total);
						//for debug
						dataOutputStream.write(buf, 0, read_len);
						dataOutputStream.flush();
					}
				}
			}else {
				mListener.OnCommunicationLost(this);
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found exception:" + e.toString());
			e.printStackTrace();
			mListener.OnCommunicationLost(this);
		} catch (IOException e) {
			Log.e(TAG, "IO exception:" + e.toString());
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