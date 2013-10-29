package com.dreamlink.communication.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileDeleteDialog extends Dialog implements android.view.View.OnClickListener {

	private TextView mDialogTitle;
	
	private View mDeletingView;
	private TextView mFileNameView;
	private ProgressBar mProgressBar;
	private ListView mDeleteListView;
	
	private Button mOkButton,mCancelButton;
	
	private String mFilePath;
	private int mSize;
	private List<String> mDeleteNameList = new ArrayList<String>();
	
	private Context mContext;
	
	private OnDelClickListener mClickListener;
	
	private int progress;
	private String fileName;
	private static final int MSG_UPDATE_PROGRESS = 0x10;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				mProgressBar.setProgress(progress);
				mFileNameView.setText(fileName);
				break;

			default:
				break;
			}
		};
	};
	
	
	public FileDeleteDialog(Context context, int theme, String path) {
		super(context, theme);
		mFilePath = path;
		mContext = context;
	}
	
	public FileDeleteDialog(Context context, int theme, int size){
		super(context, theme);
		mContext = context;
		mSize = size;
	}
	
	public FileDeleteDialog(Context context, List<String> nameList){
		super(context);
		mContext = context;
		mDeleteNameList = nameList;
	}
	
	public FileDeleteDialog(Context context, int size){
		super(context);
		mContext = context;
		mSize = size;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_filedelete_dialog);
		
		setTitle(R.string.delete_confirm);
		
		mDeletingView = findViewById(R.id.rl_deleting);
		
		mProgressBar = (ProgressBar) findViewById(R.id.bar_delete);
		mProgressBar.setMax(mDeleteNameList.size());
		
		mFileNameView = (TextView) findViewById(R.id.name_view);
		mDialogTitle = (TextView) findViewById(R.id.title_veiw);
		
		mDeleteListView = (ListView) findViewById(R.id.lv_delete);
		
		mOkButton = (Button) findViewById(R.id.left_button);
		mCancelButton = (Button) findViewById(R.id.right_button);
		mOkButton.setText(android.R.string.ok);
		mCancelButton.setText(android.R.string.cancel);
		mOkButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
		
		if (mDeleteNameList.size() > 0) {
			mDeleteListView.setVisibility(View.VISIBLE);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, 
					R.layout.ui_filedelete_dialog_item, mDeleteNameList);
			mDeleteListView.setAdapter(adapter);
			mDeletingView.setVisibility(View.GONE);
		}else {
			mDeletingView.setVisibility(View.VISIBLE);
			mDeleteListView.setVisibility(View.GONE);
		}
	}
	
	public void setProgress(int progress, String fileName){
		this.progress = progress;
		this.fileName = fileName;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.left_button:
			mClickListener.onClick(v, mFilePath);
			setTitle("正在删除");
			mDialogTitle.setVisibility(View.GONE);
			mDeleteListView.setVisibility(View.GONE);
			mDeletingView.setVisibility(View.VISIBLE);
			mOkButton.setVisibility(View.GONE);
			break;
		case R.id.right_button:
			mClickListener.onClick(v, mFilePath);
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
