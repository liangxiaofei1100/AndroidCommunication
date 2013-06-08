package com.dreamlink.communication.server;

import java.net.Socket;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.dreamlink.communication.SocketCommunicationManager;
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
	private SocketCommunicationManager manager;

	public SocketServerTask(Context context, SocketCommunicationManager manager) {
		this.context = context;
		server = SocketServer.getInstance();
		notice = new Notice(context);
		this.manager = manager;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (server.isServerStarted()) {
			notice.showToast("Server is already started");
		} else {
			showProgressDialog();
		}
	}

	private void showProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			// already showing, ignore.
			return;
		}

		// show progress dialog.
		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Server");
		progressDialog.setMessage("Waiting for client...");
		progressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Hide",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						progressDialog.dismiss();
					}
				});
		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancel(true);
						server.stopServer();
						progressDialog.dismiss();
					}
				});
		progressDialog.show();
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
			manager.addCommunication(values[0]);
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
