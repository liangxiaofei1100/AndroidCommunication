package com.dreamlink.communication.ui;

import java.util.Map;
import java.util.Vector;

import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerAdapter extends BaseAdapter {
	private LayoutInflater inflater = null;
	private Vector<Map<String, Object>> serverData = new Vector<Map<String,Object>>();
	public ServerAdapter(Context context, Vector<Map<String, Object>> data){
		inflater = LayoutInflater.from(context);
		serverData = data;
	}
	
	@Override
	public int getCount() {
		return serverData.size();
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

	class ViewHolder{
		ImageView userIcon;
		TextView userName;
		TextView tipView;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View view = null;
		ViewHolder holder  = null;
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = inflater.inflate(R.layout.ui_server_list_item, parent, false);
			
			holder.userIcon = (ImageView) view.findViewById(R.id.user_icon);
			holder.userName = (TextView) view.findViewById(R.id.user_name);
			holder.tipView = (TextView) view.findViewById(R.id.user_tip);
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		holder.userIcon.setImageResource(R.drawable.dm_icon_default_user);
		holder.userName.setText(serverData.get(position).get("name") + "");
		holder.tipView.setText("点击加入");
		
		return view;
	}

}
