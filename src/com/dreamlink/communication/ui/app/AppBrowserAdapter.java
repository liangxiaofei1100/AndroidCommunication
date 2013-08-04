package com.dreamlink.communication.ui.app;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppBrowserAdapter extends BaseAdapter {
	private static final String TAG = "AppBrowserAdapter";
	private LayoutInflater mInflater = null;
	private List<AppEntry> mList = new ArrayList<AppEntry>();
	
	
	public AppBrowserAdapter(Context context, List<AppEntry> list){
		mInflater = LayoutInflater.from(context);
		mList = list;
	}
	
	class ViewHolder{
		ImageView iconView;
		TextView nameView;
		TextView sizeView;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;
		
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_app_normal_item, parent, false);
			holder.iconView = (ImageView) view.findViewById(R.id.app_icon_text_view);
			holder.nameView = (TextView) view.findViewById(R.id.app_name_textview);
			holder.sizeView = (TextView) view.findViewById(R.id.app_size_textview);
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		AppEntry item = mList.get(position);
		holder.iconView.setImageDrawable(item.getIcon());
		holder.nameView.setText(item.getLabel());
		holder.sizeView.setText(item.getFormatSize());
		
		return view;
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

}
