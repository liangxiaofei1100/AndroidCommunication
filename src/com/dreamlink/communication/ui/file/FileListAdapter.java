package com.dreamlink.communication.ui.file;

import java.util.List;

import com.dreamlink.communication.R;

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
		return mData.size();
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
		
		String size = mData.get(position).getFormatFileSize();
		String date = mData.get(position).getFormateDate();
		
		if (mData.get(position).isDir) {
			holder.iconView.setImageResource(R.drawable.folder);
			holder.fileInfoText.setText(date);
		}else {
			holder.iconView.setImageResource(R.drawable.file);
			holder.fileInfoText.setText(size + " | " + date);
		}
		
		holder.filenameText.setText(mData.get(position).fileName);
		
		return view;
	}

}
