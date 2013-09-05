package com.dreamlink.communication.ui.history;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryMsgAdapter extends BaseAdapter {

	private LayoutInflater inflater = null;
	private List<HistoryInfo> list = new ArrayList<HistoryInfo>();
	public HistoryMsgAdapter(Context context, List<HistoryInfo> list){
		inflater = LayoutInflater.from(context);
		this.list = list;
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
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
		// TODO Auto-generated method stub
		ViewHolder holder = null;
		boolean type = list.get(position).getMsgType();
		String time = list.get(position).getFormatDate();
		System.out.println(position + ".type=" + type + ":" + time);
		if (null == convertView) {
			if (type) {
				convertView = inflater.inflate(R.layout.ui_history_item_msg_left, null);
			}else {
				convertView = inflater.inflate(R.layout.ui_history_item_msg_right, null);
			}
			
			holder = new ViewHolder();
			
			holder.progressBar = (ProgressBar) convertView.findViewById(R.id.bar_send_title);
			holder.dateView = (TextView) convertView.findViewById(R.id.tv_sendtime);
			holder.userNameView = (TextView) convertView.findViewById(R.id.tv_username);
			holder.fileNameView = (TextView) convertView.findViewById(R.id.tv_send_file_name);
			holder.fileSizeView = (TextView) convertView.findViewById(R.id.tv_send_file_size);
			
			convertView.setTag(holder);
		}else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.dateView.setText(time);
		
		return convertView;
	}
	
	class ViewHolder{
		ProgressBar progressBar;
		TextView dateView;
		TextView userNameView;
		TextView fileNameView;
		TextView fileSizeView;
	}

}
