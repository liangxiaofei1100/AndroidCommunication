package com.dreamlink.communication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.dreamlink.communication.aidl.User;

/**
 * Show Login request.
 * 
 * 
 * 
 * @see {@code LoginProtocol}
 * 
 */
public class AllowLoginDialog {
	public interface AllowLoginCallBack {
		/**
		 * When allow or disallow is selected, this call back will be invoked.
		 * 
		 * @param user
		 * @param communication
		 * @param isAllow
		 */
		void onLoginComfirmed(User user, SocketCommunication communication,
				boolean isAllow);
	}

	private Context mContext;

	public AllowLoginDialog(Context context) {
		mContext = context;
	}

	public void show(final User user, final SocketCommunication communication,
			final AllowLoginCallBack callBack) {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.app_logo));
		alertConfig.setTitle(R.string.login_dialog_title);
		alertConfig.setMessage(mContext.getString(R.string.login_dialog_message, user.getUserName()));
		alertConfig.setPositiveButton(R.string.login_dialog_allow, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				callBack.onLoginComfirmed(user, communication, true);
			}
		});
		alertConfig.setNegativeButton(R.string.login_dialog_disallow, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				callBack.onLoginComfirmed(user, communication, false);
			}
		});

		alertConfig.create().show();
	}
}
