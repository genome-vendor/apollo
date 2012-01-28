package apollo.gui;

import apollo.util.FileUtil;

import java.io.File;

import java.util.Set;
import java.util.TreeSet;

import javax.swing.filechooser.FileFilter;

/** This is a generic file filter for filtering results for javax.swing.FileChooser
 * 
 * @author elee
 *
 */

public class GenericFileFilter extends FileFilter {

	//instance variables
	protected Set<String> extensions;
	protected String description;
	
	/** Builds a GenericFileFilter object
	 *
	 */
	public GenericFileFilter()
	{
		this(null);
	}

	/** Builds a GenericFileFilter object
	 * 
	 * @param description - the description of this filter to be displayed by FileChooser
	 */
	public GenericFileFilter(String description)
	{
		super();
		extensions = new TreeSet<String>();
		setDescription(description);
	}

	/** Whether the given file is accepted by this filter
	 * 
	 * @param f - File to be tested
	 * @return true if a File is valid
	 */
	@Override
	public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
		String ext = FileUtil.getExtension(f);
		if (ext == null) { return false; }
		return extensions.contains(ext);
	}

	/** The description of this filter
	 * 
	 * @return description of this filter
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/** Set the description of this filter
	 * 
	 * @param description - new description of this filter
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	/** Adds a file extension for filtering
	 * 
	 * @param extension - file extension to be added for filtering
	 */
	public void addExtension(String extension)
	{
		extensions.add(extension);
	}
	
	/** Get the file extensions added to this filter
	 * 
	 * @return the file extensions added to this filter
	 */
	public Set<String> getExtensions()
	{
		return extensions;
	}
	
}
