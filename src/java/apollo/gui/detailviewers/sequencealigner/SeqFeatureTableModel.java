package apollo.gui.detailviewers.sequencealigner;

import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bdgp.swing.FastTranslatedGraphics;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

public class SeqFeatureTableModel extends AbstractTableModel implements List<SeqFeatureI> {

  public enum ColumnTypes { 
    
    NAME, SCORE, ID, TYPE, STRAND, FRAME;
    
    public String translate(SeqFeatureI feature) {

      switch (this) {
      case NAME: return feature.getName();
      case SCORE: return String.valueOf(feature.getScore());
      case ID: return feature.getId();
      case TYPE:
        FeatureProperty fp = Config.getPropertyScheme()
          .getFeatureProperty(feature.getTopLevelType());
        
        return fp != null ? fp.getDisplayType(): "";
      case STRAND: return Strand.valueOf(feature.getStrand()).toString();
      case FRAME: return Integer.valueOf(
          ReadingFrame.valueOf(feature.getFrame()).toInt()).toString();
      }
      
      throw new AssertionError("Unknown ColumnType: " + this);
    }
  }
  
  private List<SeqFeatureI> features;
  private List<ColumnTypes> cols;
  
  public SeqFeatureTableModel(List<ColumnTypes> cols) {
    this.features = new ArrayList<SeqFeatureI>();
    this.cols = cols;
  }
  
  public SeqFeatureTableModel(int size, List<ColumnTypes> cols) {
    this.features = new ArrayList<SeqFeatureI>(size);
    this.cols = cols;
  }

  public int getColumnCount() {
    return cols.size();
  }

  /** For now the only row will be the name */
  public int getRowCount() {
    return size();
  }

  public Object getValueAt(int row, int column) {
    SeqFeatureI feature = features.get(row);
    if (feature == null) {
      return "";
    }
    
    return cols.get(column).translate(feature);
  }
  
  public String getColumnName(int column) {
    return cols.get(column).toString();
  }
  
  public ColumnTypes getColumnType(int column) {
    return cols.get(column);
  }

  // List interface. All methods that modify the structure of this list
  // will notify listeners with fireTableDataChanged();
  public boolean add(SeqFeatureI o) {
    boolean result = features.add(o);
    fireTableDataChanged();
    return result;
  }

  public void add(int index, SeqFeatureI element) {
    features.add(index, element);
    fireTableDataChanged();
  }

  public boolean addAll(Collection<? extends SeqFeatureI> c) {
    boolean result = features.addAll(c);
    fireTableDataChanged();
    return result;
  }

  public boolean addAll(int index, Collection<? extends SeqFeatureI> c) {
    boolean result = features.addAll(index, c);
    fireTableDataChanged();
    return result;
  }

  public void clear() {
    features.clear();
    fireTableDataChanged();
  }

  public boolean contains(Object o) {
    return features.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return features.containsAll(c);
  }

  public SeqFeatureI get(int index) {
    return features.get(index);
  }

  public int indexOf(Object o) {
    return features.indexOf(o);
  }

  public boolean isEmpty() {
    return features.isEmpty();
  }

  public Iterator<SeqFeatureI> iterator() {
    return features.iterator();
  }

  public int lastIndexOf(Object o) {
    return features.lastIndexOf(o);
  }

  public ListIterator<SeqFeatureI> listIterator() {
    return features.listIterator();
  }

  public ListIterator<SeqFeatureI> listIterator(int index) {
    return features.listIterator(index);
  }

  public boolean remove(Object o) {
    boolean result = features.remove(o);
    fireTableDataChanged();
    return result;
  }

  public SeqFeatureI remove(int index) {
    SeqFeatureI result = features.remove(index);
    fireTableDataChanged();
    return result;
  }

  public boolean removeAll(Collection<?> c) {
    boolean result = features.removeAll(c);
    fireTableDataChanged();
    return result;
  }

  public boolean retainAll(Collection<?> c) {
    boolean result = features.retainAll(c);
    fireTableDataChanged();
    return result;
  }

  public SeqFeatureI set(int index, SeqFeatureI element) {
    if (index == 15) {
        int i = 1;
    }
    SeqFeatureI result = features.set(index, element);
    fireTableRowsUpdated(index, index);
    return result;
  }

  public int size() {
    return features.size();
  }

  public List<SeqFeatureI> subList(int fromIndex, int toIndex) {
    return features.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return features.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return features.toArray(a);
  }

}
