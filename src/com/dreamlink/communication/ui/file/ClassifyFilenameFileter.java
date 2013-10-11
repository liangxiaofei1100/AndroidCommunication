package com.dreamlink.communication.ui.file;

import java.io.File;
import java.io.FilenameFilter;

public class ClassifyFilenameFileter implements FilenameFilter{
	String[] types;
	
	public ClassifyFilenameFileter(){
	}
	
	public ClassifyFilenameFileter(String[] types){
		super();
		this.types = types;
	}

	@Override
	public boolean accept(File dir, String filename) {
		if (dir.isDirectory()) {
			return true;
		}
		
		for(String type: types){
			if (filename.endsWith(type)) {
				return true;
			}
		}
		return false;
	}

}
