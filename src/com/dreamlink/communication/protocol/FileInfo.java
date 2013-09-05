package com.dreamlink.communication.protocol;

import java.io.File;
import java.io.Serializable;

public class FileInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6587172190406939461L;
	public String mFileName;
	public long mFileSize;

	public FileInfo(File file) {
		mFileName = file.getName();
		mFileSize = file.length();
	}

	/**
	 * @return the FileName
	 */
	public String getFileName() {
		return mFileName;
	}

	/**
	 * @return the FileSize
	 */
	public long getFileSize() {
		return mFileSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FileInfo [mFileName=" + mFileName + ", mFileSize=" + mFileSize
				+ "]";
	}
}
