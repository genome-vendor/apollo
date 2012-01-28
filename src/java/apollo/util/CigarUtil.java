package apollo.util;

import java.lang.StringBuffer;

import org.bdgp.util.DNAUtils;

public class CigarUtil {

  public static String roll(String coord_seq, String align_seq,
                            int untranslate) {
    StringBuffer cigar = new StringBuffer();
    // these are pairs of 1. alignment type and 2. the length
    // alignment type is M (match), I (insert), or D (delete)
    // length is relative to the base coordinate system
    int delete_pos = coord_seq.indexOf ('-');
    int insert_pos = align_seq.indexOf ('-');
    while (delete_pos >=0 || insert_pos >= 0) {
      int next_match;
      int edit_start;
      String edit;
      if (delete_pos < 0 || (delete_pos >= 0 && 
			     insert_pos >= 0 && insert_pos < delete_pos)) {
	edit = " I ";
	edit_start = insert_pos;
	next_match = insert_pos + 1;
	while (next_match < align_seq.length() && 
	       align_seq.charAt(next_match) == '-')
	  next_match++;
      }
      else {
	edit = " D ";
	edit_start = delete_pos;
	next_match = delete_pos + 1;
	while (next_match < coord_seq.length() && 
	       coord_seq.charAt(next_match) == '-')
	  next_match++;
      }
      cigar.append (" M " + (edit_start * untranslate));
      cigar.append (edit + ((next_match - edit_start) * untranslate));
      /* this should be safe because the two sequences are 
         always the same length */
      if (next_match < coord_seq.length()) {
	coord_seq = coord_seq.substring(next_match);
	align_seq = align_seq.substring(next_match);
      }
      else {
	coord_seq = "";
	align_seq = "";
      }
      delete_pos = coord_seq.indexOf ('-');
      insert_pos = align_seq.indexOf ('-');
    }
    if (coord_seq.length() > 0) {
      cigar.append (" M " + (coord_seq.length() * untranslate));
    }
    return cigar.toString().trim();
  }

}
