package com.dreamlink.communication.client;

import java.net.Socket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.util.Notice;

@SuppressLint("UseValueOf")
public class SocketClientTask extends AsyncTask<String, Void, Socket> {

	private SocketCommunicationManager manager;
	private Context activity;
	private ProgressDialog progressDialog;
	private Notice notice;

	private SocketClient client;

	public SocketClientTask(Context context,
			SocketCommunicationManager mangaer, int what) {
		this.activity = context;
		this.manager = mangaer;

		client = new SocketClient();
		notice = new Notice(activity);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog = ProgressDialog.show(activity, "Client",
				"Connecting to server...");
	}

	@Override
	protected Socket doInBackground(String... arg) {
		return client.startClient(arg[0], new Integer(arg[1]));
	}

	@Override
	protected void onPostExecute(Socket result) {
		super.onPostExecute(result);

		closeDialog();

		if (result == null) {
			notice.showToast("Connect to server fail.");
		} else {
			notice.showToast("Connected to server.");
			manager.addCommunication(result);
		}
	}

	private void closeDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

}