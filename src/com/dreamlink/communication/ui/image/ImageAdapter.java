package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter{
	private LayoutInflater inflater;
	private List<ImageInfo> mImageLists = new ArrayList<ImageInfo>();
	
	private ImageLoader mImageLoader;
	private DisplayImageOptions mOptions;
	private AsyncImageLoader asyncImageLoader;
	private boolean mIdleFlag = true;

	public ImageAdapter(LayoutInflater inflater, List<ImageInfo> list, ImageLoader imageLoader, DisplayImageOptions options) {
		this.inflater = inflater;
		mImageLists = list;
		mImageLoader = imageLoader;
		mOptions = options;
	}
	
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
//		String imageUrl = DreamConstant.FILE_EX + imageInfo.getPath();
//		mImageLoader.displayImage(imageUrl, imageView, mOptions);
		
		/////////////////
		String imageUrl = imageInfo.getPath();
		if (!mIdleFlag) {
			if (AsyncImageLoader.bitmapCache.size() >0 &&
					AsyncImageLoader.bitmapCache.get(imageUrl) != null) {
				holder.imageView.setImageBitmap(AsyncImageLoader.bitmapCache.get(imageUrl).get());
			}else {
				holder.imageView.setImageResource(R.drawable.zapya_data_photo_l);
			}
		}else {
			Bitmap bitmap = asyncImageLoader.loadImage(imageUrl, FileInfoManager.TYPE_IMAGE, BaseImageFragment.bitmapCaches, 
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
				holder.imageView.setImageResource(R.drawable.zapya_data_photo_l);
			}
		}
		
		////////////////////
		
		return convertView;
	}
}
