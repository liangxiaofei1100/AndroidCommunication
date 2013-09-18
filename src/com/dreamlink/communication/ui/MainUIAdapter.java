package com.dreamlink.communication.ui;

import java.util.Map;

import com.dreamlink.communication.R;

import android.R.integer;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class MainUIAdapter extends BaseAdapter{
	private LayoutInflater inflater = null;
	private String[] menus = {"朝颜天地","网上邻居","精品推荐","图片","音乐","视频","应用","游戏"};
	private String[] tips = {"省流量","易分享","送朝元","","","","",""};
	private int[] normal_icons = {
			R.drawable.tiandi_normal,R.drawable.network_normal,
			R.drawable.tuijian_normal, R.drawable.image_normal,
			R.drawable.music_normal, R.drawable.video_normal,
			R.drawable.app_normal, R.drawable.game_normal
	};
	private int[] down_icons = {
			R.drawable.tiandi_pressed, R.drawable.network_pressed,
			R.drawable.tuijian_pressed, R.drawable.image_pressed,
			R.drawable.music_pressed, R.drawable.video_pressed,
			R.drawable.app_pressed, R.drawable.game_pressed
	};
	private int[] colors = { R.color.text_color_orange,
			R.color.text_color_blue, R.color.text_color_green,
			R.color.transparent, R.color.transparent, R.color.transparent,
			R.color.transparent, R.color.transparent };

	private GridView gridView;
	private int clickPosition = -1;
	
	public MainUIAdapter(Context context, GridView gridView){
		inflater = LayoutInflater.from(context);
		this.gridView = gridView;
	}
	
	
	public void setClickPosition(int position){
		clickPosition = position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (null == view) {
			view = inflater.inflate(R.layout.ui_mainframe_item, null);
		}
		ImageView imageView  = (ImageView) view.findViewById(R.id.iv_menu_icon);
		TextView nameView = (TextView) view.findViewById(R.id.tv_menu_name);
		TextView tipView = (TextView) view.findViewById(R.id.tv_menu_tip);
		
		int pos = position % getCount();
		imageView.setImageResource(normal_icons[pos]);
		nameView.setText(menus[pos]);
		tipView.setText(tips[pos]);
		tipView.setTextColor(colors[pos]);
		
		int row = 4; // 设置GrideView的行数
		int h = (gridView.getHeight() - 20 * (row - 1)) / row; // 设置GridView每行的高度；
		GridView.LayoutParams params = new GridView.LayoutParams(
				LayoutParams.FILL_PARENT, h);
		view.setLayoutParams(params);
		return view;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return menus.length;
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
}
