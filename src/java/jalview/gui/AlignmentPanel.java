package jalview.gui;

import java.awt.*;
import java.util.*;

import jalview.gui.event.*;
import jalview.datamodel.*;
import jalview.gui.schemes.*;
import jalview.io.*;
import jalview.util.*;

import javax.swing.*;

public class AlignmentPanel extends ControlledPanel implements AlignViewportListener,
                                                               FontChangeListener {

  public SeqPanel   seqPanel;
  public IdPanel    idPanel;
  public Component  parent;

  public Vector     selectedColumns;
  public boolean    groupEdit = false;

  protected AlignViewport av;

  public AlignmentPanel(Component p ,AlignViewport av, Controller c) {
    parent          = p;

    this.av         = av;

    setController(c);

    seqPanel        = new SeqPanel  (av,controller);
    idPanel         = new IdPanel   (av,controller);
    //	scalePanel      = new ScalePanel(av,controller);
    selectedColumns = new Vector();


    componentInit();
    //System.out.println("Loaded AlignmentPanel");
  }

  void setReadOnlyMode(boolean readOnly) { 
    seqPanel.setReadOnlyMode(readOnly);
  }

  void setMapper(Mapper mapper) { seqPanel.setMapper(mapper); }

  public int countSelected() {
    return av.getSelection().size();
  }

  ScrollFillPanel scrollFill;
  public void componentInit() {
    setLayout(new BorderLayout());
    add("Center",seqPanel);

    JPanel holder = new JPanel();
    holder.setLayout(new BorderLayout());
    JPanel holder2 = new JPanel();
    holder2.setLayout(new BorderLayout());
    
    holder2.add("North",new SeqFillPanel(SeqFillPanel.TOP,av,controller));
    holder2.add("Center",idPanel);

    holder.add("Center",holder2);
    holder.add("North"  ,new FillPanel(ScaleCanvas.HEIGHT));
    scrollFill = new ScrollFillPanel(seqPanel.getHScroller());
    holder.add("South"  ,scrollFill);
    add("West"  ,holder);
    //	add("North" ,scalePanel);

    selectAll(false);

    //System.out.println("Finished AlignmentPanel.componentInit");
  }



  public void selectAll(boolean flag) {
    int i = 0;
    if (flag) {
      while (i < seqPanel.align.getHeight()) {
        if (! av.getSelection().contains(av.getAlignment().getSequenceAt(i))) {
          av.getSelection().addElement(av.getAlignment().getSequenceAt(i));
        }
        i++;
      }
    } else {
      av.getSelection().clear();
    }
  }


  public void update(Graphics g) {
    paint(g);
  }


  public boolean handleAlignViewportEvent(AlignViewportEvent e) {

    if (e.getType() != AlignViewportEvent.LIMITS) {
      //doLayout();
      WindowUtil.invalidateComponents(this);
      validateTree();


      if (e.getType() == AlignViewportEvent.THRESHOLD) {

	av.getAlignment().setThreshold(av.getThreshold());
	controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.FONT));
	
      }

    } else {
      repaint();
    }
    return true;
  }

  public boolean handleFontChangeEvent(FontChangeEvent e) {
//    System.out.println("AlignmentPanel handleFCE");
//    validateTree();
    return true;
  }

  public void reshape(int x, int y, int width, int height) {

    setFont(av.getFont());
    super.reshape(x,y,width,height);

  }

  public void setSequenceColor(ColourSchemeI c) {
    DrawableAlignment da = av.getAlignment();

    int count = countSelected();

    if (count == 0) {
	da.setColourScheme(c);
    } else {

      SequenceGroup sg = da.addGroup();
      Selection sel = av.getSelection();

      for (int i=0; i < sel.size(); i++) {
	  da.removeFromGroup(da.findGroup(sel.sequenceAt(i)),sel.sequenceAt(i));
	  da.addToGroup(sg,sel.sequenceAt(i));
      }

      da.setColourScheme(sg,c);
      
    }

  }

  public void setFont(Font f) {
    if (seqPanel != null) {
      seqPanel.setFont(f);
    }
  }

}
class FillPanel extends JPanel {
  int height;
  public FillPanel(int height) {
    setBackground(Color.white);
    this.height = height;
  }
  public Dimension minimumSize() {
    return new Dimension(20,height);
  }
 
  public Dimension preferredSize() {
    return minimumSize();
  }
  public void setHeight(int height) {
    this.height = height;
  }
}
