package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;

public class FileClassifyAdapter extends BaseAdapter{
	private LayoutInflater inflater;
	private List<FileInfo> mItemList = new ArrayList<FileInfo>();
	private List<List<FileInfo>> mAllList = new ArrayList<List<FileInfo>>();
	
	private int mSize = 0;
	public  boolean isMain = true;
	
	private Context mContext;
	
	private AsyncImageLoader bitmapLoader;
	
	private static final int[] TYPE_ICONS = {
		R.drawable.default_video_icon,
		R.drawable.default_document_icon,
		R.drawable.default_ebook_icon,
		R.drawable.default_apk_icon,
		R.drawable.default_archives_icon,
		R.drawable.default_big_file_icon
	};
	
	private int file_type = -1;
	
	private int current_position = -1;
	
	public FileClassifyAdapter(Context context, List<FileInfo> itemList, int type){
		mContext  = context;
		inflater = LayoutInflater.from(context);
		this.mItemList = itemList;
		mSize = itemList.size();
		isMain = false;
		file_type = type;
		bitmapLoader = new AsyncImageLoader(context);
	}
	
	public FileClassifyAdapter(FragmentActivity activity, List<List<FileInfo>> allList) {
		mContext  = activity;
		inflater = LayoutInflater.from(activity);
		this.mAllList = allList;
		mSize = FileClassifyFragment.file_types.length;
		isMain = true;
	}

	@Override
	public int getCount() {
		return mSize;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setPosition(int position){
		this.current_position = position;
	}

	private class ViewHolder{
		ImageView iconView;
		TextView titleView;
		TextView tipView;
	}
	
	private class ViewHolderItem{
		ImageView iconView;
		TextView nameView;
		TextView infoView;
		
		LinearLayout mainLayout;
		LinearLayout expandLayout;
		LinearLayout openLayout;
		LinearLayout sendLayout;
		LinearLayout deleteLayout;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		
		if (isMain) {
			ViewHolder holder = null;
			if (null ==convertView || null == convertView.getTag()) {
				holder = new ViewHolder();
				view = inflater.inflate(R.layout.ui_file_classify_item, null);
				holder.iconView = (ImageView) view.findViewById(R.id.classify_icon);
				holder.titleView = (TextView) view.findViewById(R.id.classify_name);
				holder.tipView = (TextView) view.findViewById(R.id.classify_tip);
				
				view.setTag(holder);
			}else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			
			holder.iconView.setImageResource(TYPE_ICONS[position]);
			int size = 0;
			if (mAllList.size() > 0) {
				size = mAllList.get(position).size();
			}
			holder.titleView.setText(FileClassifyFragment.file_types[position] + "(" + size + ")");
			holder.tipView.setText(FileClassifyFragment.file_types_tips[position]);
		}else {
			//item view
			ViewHolderItem holderItem = null;
			if (null == convertView || null == convertView.getTag()) {
				holderItem = new ViewHolderItem();
				view = inflater.inflate(R.layout.ui_file_classify_item2, null);
				
				holderItem.iconView = (ImageView) view.findViewById(R.id.file_icon);
				holderItem.nameView = (TextView) view.findViewById(R.id.file_name_textview);
				holderItem.infoView = (TextView) view.findViewById(R.id.file_info_textview);
				
				holderItem.mainLayout = (LinearLayout) view.findViewById(R.id.file_layout_main);
				holderItem.expandLayout = (LinearLayout) view.findViewById(R.id.file_layout_expand);
				holderItem.openLayout = (LinearLayout) view.findViewById(R.id.file_item_open);
				holderItem.sendLayout = (LinearLayout) view.findViewById(R.id.file_item_send);
				holderItem.deleteLayout = (LinearLayout) view.findViewById(R.id.file_item_delete);
				
				view.setTag(holderItem);
			}else {
				view = convertView;
				holderItem = (ViewHolderItem) view.getTag();
			}
			
			FileInfo fileInfo = mItemList.get(position);
			holderItem.nameView.setText(fileInfo.fileName);
			holderItem.infoView.setText(fileInfo.getFormateDate() + " | " + fileInfo.getFormatFileSize());
			//icon view
			if (FileInfoManager.TYPE_VIDEO ==  file_type) {
				Bitmap videoBitmap = bitmapLoader.loadVideoBitmap(fileInfo.filePath, holderItem.iconView, new ILoadImageCallback() {
					@Override
					public void onObtainDrawable(Drawable drawable, ImageView imageView) {
						// TODO Auto-generated method stub
					}
					@Override
					public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
						if (null != bitmap) {
							imageView.setImageBitmap(bitmap);
						}
					}
				});
				if (null != videoBitmap) {
					holderItem.iconView.setImageBitmap(videoBitmap);
				}else {
					holderItem.iconView.setImageDrawable(fileInfo.icon);
				}
			}else if (FileInfoManager.TYPE_APK == file_type) {
				Drawable cacheDrawable = bitmapLoader.loadApkDrawable(fileInfo.filePath, holderItem.iconView, 
						new ILoadImageCallback() {
					@Override
					public void onObtainDrawable(Drawable drawable, ImageView imageView) {
						if (null != drawable) {
							imageView.setImageDrawable(drawable);
						}
					}
					@Override
					public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
						// TODO Auto-generated method stub
					}
				});
				
				if (null != cacheDrawable) {
					holderItem.iconView.setImageDrawable(cacheDrawable);
				}else {
					holderItem.iconView.setImageDrawable(fileInfo.icon);
				}
			}else {
				holderItem.iconView.setImageDrawable(fileInfo.icon);
			}
			
			holderItem.mainLayout.setOnClickListener(new MainOnClickLinstener(position));
			holderItem.openLayout.setOnClickListener(new OpenOnClickListener(position));
			holderItem.sendLayout.setOnClickListener(new SendOnClickListener(position));
			holderItem.deleteLayout.setOnClickListener(new DeleteOnClickListener(position));
			
			if (current_position == position) {
				holderItem.expandLayout.setVisibility(View.VISIBLE);
				holderItem.openLayout.setClickable(true);
				holderItem.sendLayout.setClickable(true);
				holderItem.deleteLayout.setClickable(true);
			}else {
				holderItem.expandLayout.setVisibility(View.GONE);
				holderItem.openLayout.setClickable(false);
				holderItem.sendLayout.setClickable(false);
				holderItem.deleteLayout.setClickable(false);
			}
		}
		
		return view;
	}
	
	private class MainOnClickLinstener implements OnClickListener{
		int position;
		MainOnClickLinstener(int position){
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
			if (current_position == position) {
				current_position = -1;
			}else {
				current_position = position;
			}
			notifyDataSetChanged();
		}
		
	}
	
	private class OpenOnClickListener implements OnClickListener{
		int position;
		OpenOnClickListener(int position){
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
			Intent intent = FileInfoManager.getAudioFileIntent(mItemList.get(position).filePath);
			mContext.startActivity(intent);
		}
	}
	
	private class SendOnClickListener implements OnClickListener{
		int position;
		SendOnClickListener(int position){
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
		}
		
	}
	
	private class DeleteOnClickListener implements OnClickListener{
		int position;
		DeleteOnClickListener(int position){
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
		}
		
	}

}