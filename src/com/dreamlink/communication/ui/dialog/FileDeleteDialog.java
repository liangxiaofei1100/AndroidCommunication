package com.dreamlink.communication.ui.dialog;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FileDeleteDialog extends Dialog implements android.view.View.OnClickListener {

	private TextView mFileNameView;
	private TextView mFileSizeView;
	private Button mOkButton,mCancelButton;
	
	private String mFilePath;
	
	private Context mContext;
	
	private OnDelClickListener mClickListener;
	public FileDeleteDialog(Context context, int theme, String path) {
		super(context, theme);
		mFilePath = path;
		mContext = context;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_filedelete_dialog);
		
		mFileNameView = (TextView) findViewById(R.id.name_view);
		mFileSizeView = (TextView) findViewById(R.id.size_view);
		File file = new File(mFilePath);
		mFileNameView.setText(file.getName());
		mFileSizeView.setText(mContext.getResources().getString(R.string.size, DreamUtil.getFormatSize(file.length())));
		
		mOkButton = (Button) findViewById(R.id.left_button);
		mCancelButton = (Button) findViewById(R.id.right_button);
		mOkButton.setText(android.R.string.ok);
		mCancelButton.setText(android.R.string.cancel);
		mOkButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.left_button:
			mClickListener.onClick(v, mFilePath);
			dismiss();
			break;
		case R.id.right_button:
			dismiss();
			break;

		default:
			break;
		}
	}
	
	public interface OnDelClickListener{
		public void onClick(View view, String path);
	}
	
	public void setOnClickListener(OnDelClickListener listener){
		mClickListener = listener;
	}

}
