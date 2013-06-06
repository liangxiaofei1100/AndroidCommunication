package com.dreamlink.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class SocketServer {

	public interface OnClientConnectedListener {
		public Socket onClientConnected(Socket socket);
	}

	private final int TIME_OUT = 0;

	private Socket socket;

	private ServerSocket server;

	private boolean mIsServerStarted = false;

	private static SocketServer mInstance = new SocketServer();

	private SocketServer() {

	}

	public static SocketServer getInstance() {
		return mInstance;
	}

	/**
	 * start server. return the client socket by callback interface
	 * #OnClientConnectedListener.
	 * 
	 * @param port
	 *            server port number.
	 * @param callback
	 *            interface.
	 * @return
	 */
	public Socket startServer(int port, OnClientConnectedListener listener) {

		if (mIsServerStarted) {
			return null;
		}

		if (server == null) {
			try {
				server = new ServerSocket(port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while (true) {
			mIsServerStarted = true;
			try {
				if (server != null) {
					server.setSoTimeout(TIME_OUT);
					socket = server.accept();
				}
				listener.onClientConnected(socket);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		mIsServerStarted = false;
		return null;
	}

	public boolean isServerStarted() {
		return mIsServerStarted;
	}
	/**branch liucheng_1*/
	public void stopServer() {
		if (server != null) {
			try {
				server.close();
				server = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}