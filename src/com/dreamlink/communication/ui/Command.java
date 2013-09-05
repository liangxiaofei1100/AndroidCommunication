package com.dreamlink.communication.ui;


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
	/**copy command*/
	public static final String COPY = "COPY";
	/**stop send file command*/
	public static final String STOP_SEND_FILE = "STOP#&SEND*&FILE%";
	
	/////////////////////////////////
	/**command split*/
	public static final String AITE = "@";
	/**file info split*/
	public static final String SEPARTOR = ",";
	/**Dir flag*/
	public static final String DIR_FLAG = "<DIR>";
	/**file flag*/
	public static final String FILE_FLGA = "";
	/**if is dir,set the size to 0*/
	public static final int DIR_SIZE = 0;
	/**enter*/
	public static final String ENTER = "\n";
	/**LRETN*/
	public static final String LSRETN = "lsretn";
	/**tell server ls root dir*/
	public static final String ROOT_PATH = "root";
	/**end flag*/
	public static final String END_FLAG = "&_&";
	/**stop success*/
	public static final String STOP_SUCCESS = "stop_success";
			
}
