package apollo.gui.detailviewers.sequencealigner;

import java.util.HashMap;
import java.util.Map;

import org.bdgp.util.DNAUtils;

import apollo.datamodel.CurationSet;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;

public class ReferenceFactory implements ReferenceFactoryI {
  
  private CurationSet curationSet;
  private Map<Strand, SequenceI> strandReferences;
  private Map<Strand, Map<ReadingFrame, SequenceI>> strandFrameMaps;
  
  public ReferenceFactory(CurationSet cs) {
    this.curationSet = cs;
    this.strandReferences = new HashMap<Strand, SequenceI>();
    this.strandFrameMaps = 
      new HashMap<Strand, Map<ReadingFrame, SequenceI>>();
    
    this.strandFrameMaps.put(
        Strand.FORWARD,
        new HashMap<ReadingFrame, SequenceI>());
    
    this.strandFrameMaps.put(
        Strand.REVERSE,
        new HashMap<ReadingFrame, SequenceI>());
  }
  
  public SequenceI getReference(Strand s) {
    SequenceI result = strandReferences.get(s);
    
    if (result == null) {
      result = getReferenceSequence(s);
      strandReferences.put(s, result);
    }
    
    return result;
  }
  
  public SequenceI getReference(Strand s, ReadingFrame rf) {
    Map<ReadingFrame, SequenceI> frameReferences = strandFrameMaps.get(s);
    SequenceI result = frameReferences.get(rf);
    
    if (result == null && rf != ReadingFrame.NONE) {
      result = translateToAA(getReference(s), rf);
      frameReferences.put(rf, result);
    } else if (rf == ReadingFrame.NONE) {
      result = getReference(s);
    }
    
    return result;
  }
  
  private SequenceI getReferenceSequence(Strand s) {
    Strand curationStrand = Strand.valueOf(curationSet.getStrand());
    SequenceI result = curationSet.getRefSequence();
    
    if (curationStrand != s) {
      RangeI temp_range = result.getRange();
      SequenceI newReference = 
        new Sequence("Reverse Complement: " + result.getName(),
            result.getReverseComplement());
      newReference.setResidueType(result.getResidueType());
      newReference.setRange(new Range(
          temp_range.getName(), temp_range.getEnd(), temp_range.getStart()));
      result = newReference;
    }

    return result;
  }
  
  
  private SequenceI translateToAA(SequenceI SeqDNA, ReadingFrame frame) {
    String dna = SeqDNA.getResidues();

    String aa = DNAUtils.translate(dna, frame.getDNAUtilsValue(),
                                 DNAUtils.ONE_LETTER_CODE);
    SequenceI SeqAA = new Sequence(
        "FRAME " + frame + " " + SeqDNA.getName(), aa);
    SeqAA.setResidueType(SequenceI.AA);
    SeqAA.setRange(SeqDNA.getRange());
    
    return SeqAA;
  }

}
