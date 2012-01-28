package apollo.gui.detailviewers.sequencealigner.listeners;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanelHeaderTable;
import apollo.gui.detailviewers.sequencealigner.SeqFeatureTableModel;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorFactory;

public class ResultHeaderMouseListener implements MouseListener {
  
  private MultiSequenceAlignerPanel panel;
  private ComparatorFactory.TYPE lastSort;
  private int sortCount;
  
  public ResultHeaderMouseListener(MultiSequenceAlignerPanel panel) {
    this.panel = panel;
    this.sortCount = 0;
  }

  public void mouseClicked(MouseEvent e) {
    Point point = e.getPoint();
    Object source = e.getSource();
    if (source instanceof JTableHeader) {
      JTableHeader tableHeader = (JTableHeader) source;
      MultiTierPanelHeaderTable table = (MultiTierPanelHeaderTable) tableHeader.getTable();
      JTableHeader header = table.getTableHeader();
      int column = header.columnAtPoint(point);
      SeqFeatureTableModel model = (SeqFeatureTableModel) table.getModel();
      SeqFeatureTableModel.ColumnTypes colType =  model.getColumnType(column);
      
      ComparatorFactory.TYPE sortType = null;
      if (colType == SeqFeatureTableModel.ColumnTypes.NAME) {
        sortType = ComparatorFactory.TYPE.NAME;
      } else if (colType == SeqFeatureTableModel.ColumnTypes.SCORE) {
        sortType = ComparatorFactory.TYPE.SCORE;
      } else if (colType == SeqFeatureTableModel.ColumnTypes.ID) {
      } else if (colType == SeqFeatureTableModel.ColumnTypes.TYPE) {
        sortType = ComparatorFactory.TYPE.TYPE;
      } else if (colType == SeqFeatureTableModel.ColumnTypes.STRAND) {
      } else if (colType == SeqFeatureTableModel.ColumnTypes.FRAME) {
        sortType = ComparatorFactory.TYPE.FRAME;
      }
      
      Comparator<SeqFeatureI> c = 
        ComparatorFactory.makeComparatorSeqFeature(sortType);
      
      // toggle between normal and reverse ordering for multiple clicks on
      // same header
      if (sortType == lastSort) {
        if (sortCount % 2 == 0)
          c = ComparatorFactory.makeReverseComparator(c);
        sortCount++;
      } else {
        sortCount = 0;
      }
      lastSort = sortType;
      
      panel.getResultComparator().add(c);
      panel.reformatResultPanel();
      panel.getResultPanel().repaint();
    }
    
  }

  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub

  }

}
