package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;
import java.util.List;

import apollo.datamodel.SeqFeatureI;

public interface MultiComparator <T> extends Comparator<T>, List<Comparator<T>>{

}
