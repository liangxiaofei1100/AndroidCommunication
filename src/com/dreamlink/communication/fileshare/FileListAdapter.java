package com.dreamlink.communication.fileshare;

import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileListAdapter extends BaseAdapter {
	private static final String TAG = "FileListAdapter";
	
	private Context mContext;
	private List<FileInfo> mData;
	
	private LayoutInflater mInflater = null;
	
	public FileListAdapter(Context context,List<FileInfo> dataList){
		mContext = context;
		mData = dataList;
		
		mInflater = LayoutInflater.from(mContext);
	}
	
	static class ViewHolder{
		ImageView iconView;
		TextView filenameText;
		TextView fileInfoText;
	}
	
	@Override
	public int getCount() {
		return mData.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;
		
		if (position == 0) {
			view = mInflater.inflate(R.layout.filelist_top, parent, false);
			return view;
		}
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.filelist_item, parent, false);
			
			holder.iconView = (ImageView) view.findViewById(R.id.icon);
			holder.filenameText = (TextView) view.findViewById(R.id.filename_text);
			holder.fileInfoText = (TextView) view.findViewById(R.id.filesize_date_text);
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		if (mData.get(position - 1).isDir) {
			holder.iconView.setImageResource(R.drawable.folder);
		}else {
			holder.iconView.setImageResource(R.drawable.file);
		}
		
		holder.filenameText.setText(mData.get(position -1).fileName);
		
		String size = mData.get(position -1).getFormatFileSize();
		String date = mData.get(position -1).getFormateDate();
		if ("".equals(size)) {
			holder.fileInfoText.setText(date);
		}else {
			holder.fileInfoText.setText(size + " | " + date);
		}
		
		return view;
	}

}
