package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

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
	
	private static final String FILE_EX = "file://";

	public ImageAdapter(LayoutInflater inflater, List<ImageInfo> list, ImageLoader imageLoader, DisplayImageOptions options) {
		this.inflater = inflater;
		mImageLists = list;
		mImageLoader = imageLoader;
		mOptions = options;
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ImageView imageView;
		if (null == convertView) {
			imageView = (ImageView) inflater.inflate(R.layout.ui_picture_item, parent, false);
		} else {
			imageView = (ImageView) convertView;
		}

		ImageInfo imageInfo = mImageLists.get(position);
		String imageUrl = FILE_EX + imageInfo.getPath();
		mImageLoader.displayImage(imageUrl, imageView, mOptions);
		return imageView;
	}
}
