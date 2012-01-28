package apollo.util;

import java.io.File;

public class FileUtil {
	
	/** Get the extension for a File
	 * 
	 * @param f - File object to get the extension from
	 * @return the extension of the File object
	 */
	public static String getExtension(File file)
	{
		return getExtension(file.getName());
	}
	
	/** Get the extension of a String filename
	 * 
	 * @param fname - filename to get the extension from
	 * @return the extension of the filename
	 */
	public static String getExtension(String fname)
	{
		String ext = null;
        int i = fname.lastIndexOf('.');

        if (i > 0 &&  i < fname.length() - 1) {
            ext = fname.substring(i + 1).toLowerCase();
        }
        return ext;		
	}

}
