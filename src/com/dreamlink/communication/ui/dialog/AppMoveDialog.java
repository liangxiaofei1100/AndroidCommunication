package com.dreamlink.communication.ui.dialog;

import com.dreamlink.communication.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AppMoveDialog extends Dialog implements android.view.View.OnClickListener {
	
	private TextView mNameView;
	private ProgressBar mProgressBar;
	
	private Button mCancelButton;
	private Context mContext;
	
	private int progress;
	private String mAppLabel;
	private int max;
	private static final int MSG_UPDATE_PROGRESS = 0x10;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				mProgressBar.setProgress(progress);
				mNameView.setText(mAppLabel);
				break;

			default:
				break;
			}
		};
	};
	
	public AppMoveDialog(Context context, int size){
		super(context);
		mContext = context;
		max = size;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_app_move_dialog);
		
		setTitle("移动应用");
		
		mProgressBar = (ProgressBar) findViewById(R.id.bar_move);
		mProgressBar.setMax(max);
		
		mNameView = (TextView) findViewById(R.id.name_view);
		
		mCancelButton = (Button) findViewById(R.id.button);
		mCancelButton.setText(android.R.string.cancel);
		mCancelButton.setOnClickListener(this);
	}
	
	public void setProgress(int progress, String fileName){
		this.progress = progress;
		this.mAppLabel = fileName;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.right_button:
			dismiss();
			break;

		default:
			break;
		}
	}

}
