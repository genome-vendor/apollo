package apollo.util;

import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;

import org.obo.datamodel.OBOSession;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.Synonym;
import org.obo.datamodel.SynonymedObject;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;

import java.io.IOException;

/**This class will parse OBO files and generate indexes on different identifiers
 * (e.g. id, name, etc)
 * 
 * @author elee
 *
 */

public class OBOParser {

	//instance variables
	private OBOSession session;
	private Map<String, Pair<IdentifiedObject, String> > nameMap;
	
	/**Build an OBOParser from a filename (defaults to non-case sensitive)
	 * 
	 * @param fname - OBO filename to parse
	 */
	public OBOParser(String fname) throws IOException, OBOParseException
	{
		List<String> fnames = new LinkedList<String>();
		fnames.add(fname);
		init(new LinkedList<String>(fnames), false);
	}
	
	/**Build an OBOParser from a collection of filenames (defaults to non-case sensitive)
	 * 
	 * @param fnames - OBO filenames to parse
	 */
	public OBOParser(Collection<String> fnames) throws IOException, OBOParseException
	{
		init(fnames, false);
	}

	/**Build an OBOParser from a filename and a flag for whether the indexing should be case sensitive
	 * 
	 * @param fname - OBO filename to parse
	 * @param isCaseSensitive - whether the indexing should be case sensitive
	 */
	public OBOParser(String fname, boolean isCaseSensitive) throws IOException, OBOParseException
	{
		List<String> fnames = new LinkedList<String>();
		fnames.add(fname);
		init(new LinkedList<String>(fnames), isCaseSensitive);
	}
	
	/**Build an OBOParser from a collection of filenames and a flag for whether the indexing should be case sensitive
	 * 
	 * @param fname - OBO filenames to parse
	 * @param isCaseSensitive - whether the indexing should be case sensitive
	 */
	public OBOParser(Collection<String> fnames, boolean isCaseSensitive) throws IOException, OBOParseException
	{
		init(new LinkedList<String>(fnames), isCaseSensitive);
	}
	
	/**Get a term IdentifiedObject by ID
	 * 
	 * @param id - ID of the term object
	 * @return IdentifiedObject for the ID (null if not found)
	 */
	public IdentifiedObject getTermById(String id)
	{
		return session.getObject(id);
	}
	
	/**Get a term IdentifiedObject by name
	 * 
	 * @param name - name of the term object
	 * @return IdentifiedObject for the name (null if not found)
	 */
	public Pair<IdentifiedObject, String> getTermByName(String name)
	{
		return nameMap.get(name);
	}
	
	/**Check to see if a term with the given ID exists
	 * 
	 * @param id - ID to search against
	 * @return whether a term exists with the given ID
	 */
	public boolean hasTermWithId(String id)
	{
		return getTermById(id) != null;
	}
	
	/**Check to see if a term with the given name exists
	 * 
	 * @param name - name to search against
	 * @return whether a term exists with the given name
	 */
	public boolean hasTermWithName(String name)
	{
		return getTermByName(name) != null;
	}
	
	private void init(Collection<String> fnames, boolean isCaseSensitive) throws IOException, OBOParseException
	{
		DefaultOBOParser parser = new DefaultOBOParser();
		OBOParseEngine engine = new OBOParseEngine(parser);
		engine.setPaths(fnames);
		engine.parse();
		session = parser.getSession();
		if (!isCaseSensitive) {
			nameMap = new TreeMap<String, Pair<IdentifiedObject, String> >();
		}
		else {
			nameMap = new TreeMap<String, Pair<IdentifiedObject, String> >(new java.util.Comparator<String>() {
				public int compare(String s1, String s2)
				{
					return s1.compareToIgnoreCase(s2);
				}
			});
		}
		for (IdentifiedObject term : session.getObjects()) {
			nameMap.put(term.getName(), new Pair<IdentifiedObject, String>(term, term.getName()));
			if (term instanceof SynonymedObject) {
			  for (Synonym s : ((SynonymedObject)term).getSynonyms()) {
			    nameMap.put(s.getText(), new Pair<IdentifiedObject, String>(term, s.getText()));
			  }
			}
		}
	}

}
