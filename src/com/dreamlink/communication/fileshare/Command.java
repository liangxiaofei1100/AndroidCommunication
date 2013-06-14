package com.dreamlink.communication.fileshare;


public class Command {

	// File operations.
	/** get current directory. */
	public static final String PWD = "PWD";
	/** change current directory */
	public static final String CD = "CD";
	/** change current directory to parent directory */
	public static final String CDUP = "CDUP";
	/** List files of current directory */
	public static final String LS = "LS";
	/** delete file */
	public static final String DELETE = "DELETE";
	/** Rename file */
	public static final String RENAME = "RENAME";
	/** Download file */
	public static final String GET = "GET";
	/** Upload file */
	public static final String PUT = "PUT";
	
	/////////////////////////////////
	/**命令分隔符*/
	public static final String AITE = "@";
	/**文件属性分隔符*/
	public static final String SEPARTOR = ",";
	/**目录标识*/
	public static final String DIR_FLAG = "<DIR>";
	/**文件标识*/
	public static final String FILE_FLGA = "";
	/**如果是目录，大小置为0*/
	public static final int DIR_SIZE = 0;
	/**换行标识*/
	public static final String ENTER = "\n";
	/**LS返回命令标识符*/
	public static final String LSRETN = "lsretn";
	/**根目录*/
	public static final String ROOT_PATH = "root";
	/**结束标识符*/
	public static final String END_FLAG = "&_&";
}
