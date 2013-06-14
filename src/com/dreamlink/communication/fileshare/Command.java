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
	/**����ָ���*/
	public static final String AITE = "@";
	/**�ļ����Էָ���*/
	public static final String SEPARTOR = ",";
	/**Ŀ¼��ʶ*/
	public static final String DIR_FLAG = "<DIR>";
	/**�ļ���ʶ*/
	public static final String FILE_FLGA = "";
	/**�����Ŀ¼����С��Ϊ0*/
	public static final int DIR_SIZE = 0;
	/**���б�ʶ*/
	public static final String ENTER = "\n";
	/**LS���������ʶ��*/
	public static final String LSRETN = "lsretn";
	/**��Ŀ¼*/
	public static final String ROOT_PATH = "root";
	/**������ʶ��*/
	public static final String END_FLAG = "&_&";
}
