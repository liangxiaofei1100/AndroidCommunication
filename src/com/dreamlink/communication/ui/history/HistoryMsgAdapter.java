package com.dreamlink.communication.ui.history;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.nfc.tech.NfcA;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryMsgAdapter extends BaseAdapter {

	private LayoutInflater inflater = null;
	private List<HistoryInfo> list = new ArrayList<HistoryInfo>();
	private double progress = 0;
	private int status = -1;
	private double max = 0;
	private double prev = 0;
	
	public HistoryMsgAdapter(Context context, List<HistoryInfo> list){
		inflater = LayoutInflater.from(context);
		this.list = list;
	}
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		HistoryInfo historyInfo = list.get(position);
		if (historyInfo.getMsgType()) {
			return 0;
		}else {
			return 1;
		}
	}
	
	@Override
	public int getViewTypeCount() {
		//如果你的list中有不同的视图类型，就一定要重写这个方法，并配合getItemViewType一起使用
		return 2;
	}
	
	public void setStatus(int status){
		this.status = status;
	}
	
	public void setProgress(double progress){
		this.progress = progress;
	}
	
	public void setMax(double max){
		this.max = max;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder = null;
		View view = null;
		boolean type = list.get(position).getMsgType();
		if (null == convertView || null == convertView.getTag()) {
			if (type) {
				view = inflater.inflate(R.layout.ui_history_item_msg_left, null);
			}else {
				view = inflater.inflate(R.layout.ui_history_item_msg_right, null);
			}
			
			holder = new ViewHolder();
			
			holder.titleBar = (ProgressBar) view.findViewById(R.id.bar_send_title);
			holder.transferBar = (ProgressBar) view.findViewById(R.id.bar_progressing);
			holder.transferBar.setMax(100);
			holder.dateView = (TextView) view.findViewById(R.id.tv_sendtime);
			holder.userNameView = (TextView) view.findViewById(R.id.tv_username);
			holder.fileNameView = (TextView) view.findViewById(R.id.tv_send_file_name);
			holder.fileSizeView = (TextView) view.findViewById(R.id.tv_send_file_size);
			holder.isSendMsg = type;
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		String time = list.get(position).getFormatDate();
		String fileName = list.get(position).getFileInfo().fileName;
		String userName = list.get(position).getUser().getUserName();
		double max = list.get(position).getMax();
		
		holder.dateView.setText(time);
		holder.userNameView.setText(userName);
		holder.fileNameView.setText(fileName);
		switch (status) {
		case HistoryManager.STATUS_SENDING:
			double percent = progress / max;
			Log.d("HistoryActivity", "percent=" + percent + "|prev=" + prev);
			if (prev != percent * 100) {
				Log.d("HistoryActivity", "percent=" + HistoryManager.nf.format(percent));
				holder.transferBar.setProgress((int)(percent * 100));
				holder.fileSizeView.setText(HistoryManager.nf.format(percent) + " | " + list.get(position).getFileInfo().getFormatFileSize());
				prev = percent * 100;
			}
			break;

		default:
			break;
		}
		
		return view;
	}
	
	class ViewHolder{
		ProgressBar titleBar;
		ProgressBar transferBar;
		TextView dateView;
		TextView userNameView;
		TextView fileNameView;
		TextView fileSizeView;
		boolean isSendMsg = true;
	}

}
