package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RemoteShareAdapter extends BaseAdapter {
	
	private LayoutInflater mInflater = null;
	private List<User> mData = new ArrayList<User>();
	public RemoteShareAdapter(Context context, List<User> data){
		mInflater = LayoutInflater.from(context);
		mData = data;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = mInflater.inflate(R.layout.ui_remoteshare_item, null);
		TextView textView = (TextView) convertView.findViewById(R.id.server_name);
		textView.setText(mData.get(position).getUserName());
		return convertView;
	}

}
