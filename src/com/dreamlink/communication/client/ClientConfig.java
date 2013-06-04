package com.dreamlink.communication.client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Notice;

public class ClientConfig {
	public interface OnClientConfigListener {
		public void onClientConfig(String serverIP, String portNumber);
	}

	private Context mContext;
	private Notice mNotice;
	private LayoutInflater mLayoutInflater;
	private OnClientConfigListener mListener;

	public ClientConfig(Context context, OnClientConfigListener listener) {
		mContext = context;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListener = listener;
		mNotice = new Notice(mContext);
	}

	public void showConfigDialog() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.ic_launcher));
		alertConfig.setTitle("Client");

		View convertView = mLayoutInflater.inflate(R.layout.popup_client, null);

		final EditText editText = (EditText) convertView
				.findViewById(R.id.editText1);
		final EditText editText2 = (EditText) convertView
				.findViewById(R.id.editText2);

		editText.setText("192.168.43.1");
		editText2.setText("55555");

		alertConfig.setView(convertView);

		alertConfig.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (editText.getText().toString().length() > 0
								&& editText2.getText().toString().length() > 0) {
							if (mListener != null) {
								mListener.onClientConfig(editText.getText()
										.toString(), editText2.getText()
										.toString());
							}
						} else {
							mNotice.showToast("Please input the server IP and port number.");
						}
					}
				});

		alertConfig.setNeutralButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		alertConfig.create().show();
	}
}
