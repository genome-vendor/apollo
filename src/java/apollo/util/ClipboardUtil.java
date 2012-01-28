package apollo.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import apollo.datamodel.Sequence;
import apollo.seq.io.FastaFile;

public class ClipboardUtil {

  public static void copyTextToClipboard(String text) {
    StringSelection contents = new StringSelection(text);
    Clipboard clipboard = Toolkit.getDefaultToolkit().
                          getSystemClipboard();
    clipboard.setContents(contents, contents);
  }

  // this is currently not used - if we need this we need state and singleton instance
//   public SequenceI getSeqFromClipboard() {
//     return (SequenceI) clipboard_seqs.elementAt (0);
//   }

  /** should this go in SequenceUtil? */
  public static void copySeqToClipboard(Sequence seq) {
    //clipboard_seqs.removeAllElements(); -- not used
    //clipboard_seqs.addElement (seq);
    String fasta = FastaFile.format (seq.getName(),
                                     seq.getResidues(),
                                     50);
    copyTextToClipboard (fasta);
  }

}
