package com.dreamlink.communication.ui.dialog;

import com.dreamlink.communication.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * This is delete dialog,any user can call it by fragment.show
 * there has a bug
 *java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
 */
public class DeleteDialog extends DialogFragment {
	private static final String TAG = "ConfirmDialog";
	private static final String FILE_PATH = "file_path";
	private static String file_path = "";
	private ConfirmListener mListener;
	
	/**
	 * create confirm dialog instance
	 * and you can set some parms
	 */
	public static DeleteDialog newInstance(String parm){
		DeleteDialog fragment = new DeleteDialog();
		file_path = parm;
//		Bundle args = new Bundle();
//		args.putString(FILE_PATH, parm);
//		fragment.setArguments(args);
		return fragment;
	}
	
	public void setConfirmListener(ConfirmListener listener){
		mListener = listener;
	}
	
	/**caller show implement*/
	public interface ConfirmListener{
		void confirm(String path);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
//		final String file_path = getArguments().getString(FILE_PATH);
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.delete_confirm)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(getResources().getString(R.string.confirm_msg, file_path))
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mListener.confirm(file_path);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
	}
}
