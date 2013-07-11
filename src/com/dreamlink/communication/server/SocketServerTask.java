package com.dreamlink.communication.server;

import java.net.Socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;

import com.dreamlink.communication.server.SocketServer.OnClientConnectedListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

/**
 * This class is a AsyncTask, used for creating server socket and accept client
 * socket connection.</br>
 * 
 * After connected server, start communication with client socket.</br>
 * 
 */
@SuppressLint("UseValueOf")
public class SocketServerTask extends AsyncTask<String, Socket, Socket>
		implements OnClientConnectedListener {
	private static final String TAG = "SocketServerTask";

	public interface OnClientConnectedListener {
		/**
		 * A client connected.
		 * 
		 * @param clientSocket
		 */
		void onClientConnected(Socket clientSocket);
	}

	private OnClientConnectedListener mOnClientConnectedListener;

	private Message message;

	private Notice notice;

	private SocketServer server;

	public SocketServerTask(Context context) {
		server = SocketServer.getInstance();
		notice = new Notice(context);
	}

	public void setOnClientConnectedListener(OnClientConnectedListener listener) {
		mOnClientConnectedListener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (server.isServerStarted()) {
			notice.showToast("Server is already started");
		}
	}

	@Override
	protected Socket doInBackground(String... arg) {
		if (server.isServerStarted()) {
			notice.showToast("Server is already started");
			return null;
		} else {
			return server.startServer(new Integer(arg[0]), this);
		}
	}

	@Override
	protected void onPostExecute(Socket result) {
		super.onPostExecute(result);
		if (server.isServerStarted()) {
			return;
		}
		if (result != null) {
			notice.showToast("Client connected.");
			message.obj = result;
		} else {
			Log.d(TAG, "onPostExecute, result is null.");
		}
	}

	@Override
	protected void onProgressUpdate(Socket... values) {
		if (values == null) {
			notice.showToast("Waiting for client timeout.");
		} else if (values.length == 1) {
			notice.showToast("Client connected.");
			if (mOnClientConnectedListener != null) {
				mOnClientConnectedListener.onClientConnected(values[0]);
			}
		}
	}

	@Override
	public Socket onClientConnected(Socket socket) {
		publishProgress(socket);
		return socket;
	}

}
