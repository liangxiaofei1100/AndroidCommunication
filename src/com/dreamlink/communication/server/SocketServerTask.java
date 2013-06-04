package com.dreamlink.communication.server;

import java.net.Socket;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.dreamlink.communication.server.SocketServer.OnClientConnectedListener;
import com.dreamlink.communication.util.Notice;

@SuppressLint("UseValueOf")
public class SocketServerTask extends AsyncTask<String, Socket, Socket>
		implements OnClientConnectedListener {

	private Handler handler;
	private Message message;

	private Context context;
	private ProgressDialog progressDialog;
	private Notice notice;

	private SocketServer server;

	public SocketServerTask(Context context, Handler handler, int what) {
		this.context = context;
		this.handler = handler;

		message = new Message();
		message.what = what;

		server = SocketServer.getInstance();
		notice = new Notice(context);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (server.isServerStarted()) {
			notice.showToast("Server is already started");
		} else {
			progressDialog = ProgressDialog.show(context, "Server",
					"Waiting for client...");
		}
	}

	@Override
	protected Socket doInBackground(String... arg) {
		if (server.isServerStarted()) {
			notice.showToast("Server is already started");
			// TODO
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

		closeDialog();

		if (result == null) {
			notice.showToast("Waiting for client timeout.");
		} else {
			notice.showToast("Client connected.");
			message.obj = result;
			handler.dispatchMessage(message);
		}
	}

	@Override
	protected void onProgressUpdate(Socket... values) {
		super.onProgressUpdate(values);
		closeDialog();

		if (values == null) {
			notice.showToast("Waiting for client timeout.");
		} else if (values.length == 1) {
			notice.showToast("Client connected.");
			message.obj = values[0];
			handler.dispatchMessage(message);
		} else {
			// should not be here.
		}
	}

	private void closeDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	@Override
	public Socket onClientConnected(Socket socket) {
		publishProgress(socket);
		return socket;
	}

}
