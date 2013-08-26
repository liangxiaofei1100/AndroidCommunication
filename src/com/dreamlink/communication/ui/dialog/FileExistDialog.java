package com.dreamlink.communication.ui.dialog;

import java.io.File;
import java.security.PublicKey;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.file.FileInfoManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileExistDialog extends Dialog implements OnItemClickListener {

	private ListView mListView;
	private LayoutInflater mInflater = null;
	private Context mContext = null;
	
	private String[] menuName = null;
	private String[] menuTip = null;
	
	private MyAdapter mAdapter;
	private String mFileName;
	private String mRenameStr;
	private String mFilePath;
	
	private onMenuItemClickListener mListener = null;
	public FileExistDialog(Context context, String path, String fileName) {
		super(context);
		mInflater = LayoutInflater.from(context);
		mContext = context;
		mFilePath = path;
		mFileName = fileName;
	}
	
	public interface onMenuItemClickListener{
		public void onMenuItemClick(int position);
	}
	
	public void setOnMenuItemClickListener(onMenuItemClickListener listener){
		mListener = listener;
	}
	
	public String getRenameStr(){
		return mRenameStr;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_file_exist_dialog);
		
		menuName = mContext.getResources().getStringArray(R.array.file_save_menu);
		menuTip = mContext.getResources().getStringArray(R.array.file_save_menu_tip);

		menuTip[0] = mContext.getResources().getString(R.string.copy_replace_msg, mFileName);
		
		mRenameStr = FileInfoManager.autoRename(mFileName);
		while(new File(mFilePath + "/" + mRenameStr).exists()) {
			mRenameStr = FileInfoManager.autoRename(mRenameStr);
		}
		menuTip[1] = mContext.getResources().getString(R.string.copy_rename_msg, mRenameStr);
		
		mListView = (ListView) findViewById(R.id.listview);
		mListView.setOnItemClickListener(this);
		mAdapter = new MyAdapter();
		mListView.setAdapter(mAdapter);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mListener.onMenuItemClick(position);
	}
	
	private class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return menuName.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = mInflater.inflate(R.layout.ui_file_exist_dialog_item, null);
			TextView nameView = (TextView) convertView.findViewById(R.id.menu_name);
			TextView tipView = (TextView) convertView.findViewById(R.id.menu_tip);
			
			nameView.setText(menuName[position]);
			tipView.setText(menuTip[position]);
			
			return convertView;
		}
		
	}

}
