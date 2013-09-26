package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * @unuse 先观察CursorAdapter的使用情况，再决定是否删除
 * */
public class ImageAdapter extends BaseAdapter{
	private LayoutInflater inflater;
	private List<ImageInfo> mImageLists = new ArrayList<ImageInfo>();
	
	private AsyncImageLoader asyncImageLoader;
	private boolean mIdleFlag = true;
	
	public ImageAdapter(Context context, List<ImageInfo> list){
		inflater = LayoutInflater.from(context);
		mImageLists = list;
		asyncImageLoader = new AsyncImageLoader(context);
	}
	
	@Override
	public int getCount() {
		return mImageLists.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setIdleFlag(boolean flag){
		this.mIdleFlag = flag;
	}
	
	private class ViewHolder{
		ImageView imageView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (null == convertView || null == convertView.getTag()) {
			convertView = inflater.inflate(R.layout.ui_picture_item, parent, false);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.picture_imageview);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		ImageInfo imageInfo = mImageLists.get(position);
		
		String imageUrl = imageInfo.getPath();
		if (!mIdleFlag) {
			if (AsyncImageLoader.bitmapCache.size() >0 &&
					AsyncImageLoader.bitmapCache.get(imageUrl) != null) {
				holder.imageView.setImageBitmap(AsyncImageLoader.bitmapCache.get(imageUrl).get());
			}else {
				holder.imageView.setImageResource(R.drawable.photo_l);
			}
		}else {
			Bitmap bitmap = asyncImageLoader.loadImage(imageUrl, FileInfoManager.TYPE_IMAGE, ImageFragment.bitmapCaches, 
					holder.imageView, 
					new ILoadImageCallback() {
						@Override
						public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
							imageView.setImageBitmap(bitmap);
						}
					});
			
			if (null != bitmap) {
				holder.imageView.setImageBitmap(bitmap);
			}else {
				holder.imageView.setImageResource(R.drawable.photo_l);
			}
		}
		
		return convertView;
	}
}
