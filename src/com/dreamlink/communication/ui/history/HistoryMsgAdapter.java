package com.dreamlink.communication.ui.history;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryMsgAdapter extends BaseAdapter {
	private static final String TAG = "HistoryMsgAdapter";
	private LayoutInflater inflater = null;
	private List<HistoryInfo> list = new ArrayList<HistoryInfo>();
	private int status = -1;
	private Context mContext;
	
	public HistoryMsgAdapter(Context context, List<HistoryInfo> list){
		this.mContext = context;
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
		if (HistoryManager.TYPE_RECEIVE == historyInfo.getMsgType()) {
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		View view = null;
		int type = list.get(position).getMsgType();
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			if (HistoryManager.TYPE_RECEIVE == type) {
				view = inflater.inflate(R.layout.ui_history_item_msg_left, null);
			}else {
				view = inflater.inflate(R.layout.ui_history_item_msg_right, null);
			}
			
			holder.receiveUserNameView = (TextView) view.findViewById(R.id.tv_send_title_msg);
			holder.titleBar = (ProgressBar) view.findViewById(R.id.bar_send_title);
			holder.transferBar = (ProgressBar) view.findViewById(R.id.bar_progressing);
			holder.transferBar.setMax(100);
			holder.dateView = (TextView) view.findViewById(R.id.tv_sendtime);
			holder.userNameView = (TextView) view.findViewById(R.id.tv_username);
			holder.fileNameView = (TextView) view.findViewById(R.id.tv_send_file_name);
			holder.fileSizeView = (TextView) view.findViewById(R.id.tv_send_file_size);
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		String time = list.get(position).getFormatDate();
		String fileName = list.get(position).getFileInfo().getFileName();
		String userName = list.get(position).getSendUserName();
		double max = list.get(position).getMax();
		double progress = list.get(position).getProgress();
		int status = list.get(position).getStatus();
		Log.d(TAG, "getView().status=" + status);
//		
		holder.dateView.setText(time);
		holder.userNameView.setText(userName);
		holder.fileNameView.setText(fileName);
		holder.fileSizeView.setTextColor(Color.BLACK);
		holder.receiveUserNameView.setTextColor(Color.BLACK);
		switch (status) {
		case HistoryManager.STATUS_PRE_SEND:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.pre_send, 
					list.get(position).getReceiveUser().getUserName()));
			holder.fileSizeView.setText(HistoryManager.nf.format(0) + " | " + DreamUtil.getFormatSize(max));
			break;
			
		case HistoryManager.STATUS_SENDING:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.sending, 
					list.get(position).getReceiveUser().getUserName()));
		case HistoryManager.STATUS_RECEIVING:
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent = progress / max;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent));
			holder.transferBar.setProgress((int)(percent * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent) + " | " + DreamUtil.getFormatSize(max));
			break;
		case HistoryManager.STATUS_SEND_SUCCESS:
		case HistoryManager.STATUS_RECEIVE_SUCCESS:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.send_ok, 
					list.get(position).getReceiveUser().getUserName()));
			holder.transferBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(max));
			break;
		case HistoryManager.STATUS_SEND_FAIL:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.send_fail, 
					list.get(position).getReceiveUser().getUserName()));
			holder.receiveUserNameView.setTextColor(Color.RED);
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent2 = progress / max;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent2));
			holder.transferBar.setProgress((int)(percent2 * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent2) + " | " + DreamUtil.getFormatSize(max));
		case HistoryManager.STATUS_RECEIVE_FAIL:
			holder.fileSizeView.setText(R.string.receive_fail);
			holder.fileSizeView.setText(Color.RED);
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
		TextView receiveUserNameView;
	}

}
