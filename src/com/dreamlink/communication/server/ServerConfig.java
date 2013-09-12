package com.dreamlink.communication.server;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

/**
 * This class is use for show config server dialog. It can be used in manual
 * setting server. For now, we use auto search to config client. see
 * {@link SearchClient}.
 */
public class ServerConfig {

	public interface OnServerConfigListener {
		public void onServerConfig(String portNumber);
	}

	private Context mContext;
	private Notice mNotice;
	private OnServerConfigListener mListener;

	public ServerConfig(Context context, OnServerConfigListener listener) {
		mContext = context;
		mListener = listener;
		mNotice = new Notice(mContext);
	}

	public void showConfigDialog() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.app_logo));
		alertConfig.setTitle("Server");

		final EditText editText = new EditText(mContext);
		editText.setHint("Port");
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setText("55555");
		alertConfig.setView(editText);

		alertConfig.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (editText.getText().toString().length() > 0) {
							if (mListener != null) {
								mListener.onServerConfig(editText.getText()
										.toString());
							}
						} else {
							mNotice.showToast("Please input the port number.");
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
