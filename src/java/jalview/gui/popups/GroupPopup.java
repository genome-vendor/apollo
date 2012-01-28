package jalview.gui.popups;

import jalview.datamodel.*;
import jalview.gui.*;
import jalview.gui.schemes.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

public class GroupPopup extends Popup implements ListSelectionListener,ItemListener {
  java.io.PrintStream o = System.out;

  DefaultListModel groupModel;
  JList groupList;
  JLabel groupLabel;

  DefaultListModel memberModel;
  JList memberList;
  JLabel memberLabel;
  
  DefaultListModel ungroupedModel;
  JList ungroupedList;
  JLabel ungroupedLabel;

  JButton add;
  JButton del;
  JButton addAll;
  JButton delAll;

  JLabel groupProperties;
  
  Choice colourScheme;
  JLabel colourSchemeLabel;
  JCheckBox boxes;
  JCheckBox text;
  JCheckBox colourText;
  JCheckBox display;

  JButton addGroup;
  JButton delGroup;
  JButton addSelected;
  
  DrawableAlignment da;

  public GroupPopup(JFrame parent, AlignViewport av, Controller c,String title, DrawableAlignment da) {
    super(parent,av,c,title);

    this.da = da;

    groupModel = new DefaultListModel();
    groupList = new JList(groupModel);
    groupList.addListSelectionListener(this);

    groupLabel = new JLabel("Groups");

    memberModel = new DefaultListModel();
    memberList = new JList(memberModel);

    memberLabel = new JLabel("Group members");

    ungroupedModel = new DefaultListModel();
    ungroupedList = new JList(ungroupedModel);
    ungroupedLabel = new JLabel("Ungrouped sequences");

    add = new JButton("<- Add");
    add.addActionListener(pal);

    del = new JButton("Delete ->");
    del.addActionListener(pal);

    addAll = new JButton("<- Add all");
    addAll.addActionListener(pal);

    delAll = new JButton("Delete all ->");
    delAll.addActionListener(pal);

    groupProperties = new JLabel("Group properties");
    
    colourScheme = new Choice();
    for (int i=0; i < ColourProperties.getColourSchemeNames().size(); i++) {
      colourScheme.addItem((String)ColourProperties.getColourSchemeNames().elementAt(i));
    }
    colourScheme.addItemListener(this);
    colourSchemeLabel = new JLabel("Colour scheme");

    boxes = new JCheckBox("Display boxes");
    boxes.setSelected(true);
    boxes.addItemListener(this);

    text = new JCheckBox("Display text");
    text.setSelected(true);
    text.addItemListener(this);

    colourText = new JCheckBox("Colour text");
    colourText.setSelected(false);
    colourText.addItemListener(this);

    display = new JCheckBox("Display group");
    display.setSelected(true);
/* SMJS NOTE NO LISTENER HERE */

    addGroup = new JButton("Add new group");
    addGroup.addActionListener(pal);

    delGroup = new JButton("Delete selected group");
    delGroup.addActionListener(pal);

    addSelected = new JButton("Add selected IDs");
    addSelected.addActionListener(pal);

    JPanel listp = new JPanel(); 
    listp.setLayout(new GridLayout(1,4,10,5)); 
    JPanel adddel = new JPanel(); 
    adddel.setLayout(new GridLayout(4,1,5,5));
    JPanel props = new JPanel();
    props.setLayout(new GridLayout(6,1,5,5));
    JPanel gprops = new JPanel();
    gprops.setLayout(new GridLayout(3,1,5,5));
    

    gbc.fill = GridBagConstraints.NONE; 
    gbc.insets = new Insets(2,2,2,2);

    listp.add(groupList);
    listp.add(memberList);


    adddel.add(add);
    adddel.add(del);
    adddel.add(addAll);
    adddel.add(delAll);

    listp.add(adddel);
    listp.add(ungroupedList);

    
    props.add(groupProperties);

    props.add(colourScheme);
    props.add(boxes);
    props.add(text);
    props.add(colourText);

    gprops.add(addGroup);
    gprops.add(delGroup);
    gprops.add(addSelected);


    add(listp,gb,gbc,0,0,2,3);
    add(props,gb,gbc,0,3,1,1);
    add(gprops,gb,gbc,1,3,1,1);

    add(apply,gb,gbc,0,4,1,1);
    add(close,gb,gbc,1,4,1,1);

    pack();
    show();

    listGroups();
    showUngrouped();

  }

  public void listGroups() {
    if (groupModel.getSize() > 0) {
      groupModel.clear();
    }

    Vector g = da.getGroups();
      
    for (int i = 0;i < g.size(); i++) {
      SequenceGroup sg = (SequenceGroup)g.elementAt(i);
      groupModel.addElement(i + " (" + sg.getSize() + " sequences)");
    }
    
  }

  public void showUngrouped() {
    if (ungroupedModel.getSize() > 0) {
      ungroupedModel.clear();
    }
    for (int i = 0; i < da.getSequences().size();i++) {
	AlignSequenceI s = da.getSequenceAt(i);
	if (da.findGroup(s) == null) {
	  ungroupedModel.addElement(s.getName());
	}
    }
  }
  
  public void showGroup(SequenceGroup sg) {
    
    if (memberModel.getSize() > 0) {
      memberModel.clear();
    }
    for (int j = 0; j < sg.getSize(); j++) {
      AlignSequenceI s = (AlignSequenceI)sg.getSequenceAt(j);
      memberModel.addElement(s.getName());
    }
    displayProperties(sg);
  }

  public void setProperties(SequenceGroup sg) {
    int schemeno = colourScheme.getSelectedIndex();
    ColourSchemeI cs  = ColourSchemeFactory.get(schemeno);
    
    if (cs instanceof ResidueColourScheme) {
      System.err.println("Consensus setting not implemented");
    }

    da.setColourScheme(sg,cs);

    sg.setDisplayBoxes(boxes.isSelected());
    sg.setDisplayText(text.isSelected());
    sg.setColourText(colourText.isSelected());
    da.displayBoxes(sg);
    da.displayText(sg);
    da.colourText(sg);
  }

  public void displayProperties(SequenceGroup sg) {
      //    int num  = ColourSchemeFactory.get(sg.getColourScheme());
//      String name = (String)ColourProperties.getColourSchemeNames().elementAt(num);
//      colourScheme.select(name);

//      boxes.setSelected(sg.getDisplayBoxes());
//      text.setSelected(sg.getDisplayText());
//      colourText.setSelected(sg.getColourText());
  }
  
  protected void applyAction(ActionEvent evt) {
    applyCommand();
    System.out.println("Applied");
  }

    protected void otherAction(ActionEvent evt) {
	if (evt.getSource() == addGroup ) {
	    SequenceGroup sg = da.addGroup();
	    setProperties(sg);
	    da.setColourScheme(sg);
	    listGroups();
	    groupList.setSelectedIndex(da.getGroups().size()-1);
      
	    if (memberModel.getSize() > 0) {
		memberModel.clear();
	    }
	} else if (evt.getSource() == delGroup) {
	    delGroup.requestFocus();
	    int i = groupList.getSelectedIndex();
	    if (i >= 0) {
		da.deleteGroup((SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex()));
		listGroups();
		if (memberModel.getSize() > 0) {
		    memberModel.clear();
		}
		showUngrouped();
	    }
	} else if (evt.getSource() == add) {
	    add.requestFocus();
	    int sel = groupList.getSelectedIndex();
	    if (sel >= 0) {
		SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
		Object[] selseqs = ungroupedList.getSelectedValues();
		for (int i = 0; i < selseqs.length; i++) {
		    da.addToGroup(sg,da.findName((String)selseqs[i]));
		}
		listGroups();
		groupList.setSelectedIndex(sel);
		showGroup(sg);
		showUngrouped();
	    }
	} else if (evt.getSource() == addAll) {
	    addAll.requestFocus();
	    int sel = groupList.getSelectedIndex();
	    if (sel >= 0) {
		SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
		
		for (int i = 0; i < ungroupedModel.getSize(); i++ ) {
		    da.addToGroup(sg,da.findName((String)ungroupedModel.get(i)));
		}
		listGroups();
		groupList.setSelectedIndex(sel);
		showGroup(sg);
		showUngrouped();
	    }
	} else if (evt.getSource() == delAll) {
	    delAll.requestFocus();
	    int sel = groupList.getSelectedIndex();
	    if (sel >= 0) {
		SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
		
		for (int i = 0; i < memberModel.getSize(); i++ ) {
		    da.removeFromGroup(sg,da.findName((String)memberModel.get(i)));
		}
		listGroups();
		groupList.setSelectedIndex(sel);
		showGroup(sg);
		showUngrouped();
	    }
    } else if (evt.getSource() == del) {
      del.requestFocus();
      int sel = groupList.getSelectedIndex();
      if (sel >= 0) {
	SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());

	if (memberList.getSelectedValues() != null) {
	  Object[] selseqs = memberList.getSelectedValues();
	  for (int i = 0; i < selseqs.length; i++) {
	    da.removeFromGroup(sg,da.findName((String)selseqs[i]));
	  }
	}
	listGroups();
	groupList.setSelectedIndex(sel);
	showGroup(sg);
	showUngrouped();
      }
    } else if (evt.getSource() == addSelected) {
      addSelected.requestFocus();
      if (parent instanceof AlignFrame) {
	int sel = groupList.getSelectedIndex();
	if (sel >= 0) {
	  SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
          System.err.println("add selected not implemented");
	  // SMJS Vector selseqs = ((AlignFrame)parent).ap.sel;
          Vector selseqs = new Vector();
	  int i = 0;
	  while (i < selseqs.size()) {
	    SequenceGroup found = da.findGroup((AlignSequence)selseqs.elementAt(i));
	    da.removeFromGroup(found,(AlignSequence)selseqs.elementAt(i));
	    if (found.getSize() == 0) {
	      da.deleteGroup(found);
	    }
	    
	    da.addToGroup(sg,(AlignSequence)selseqs.elementAt(i));
	    i++;
	  }
	  listGroups();
	  groupList.setSelectedIndex(sel);
	  showGroup(sg);
	  showUngrouped();
	}
      }
    } else {
      System.out.println("ERROR: Unhandled ActionEvent in GroupPopup");
    }
  }

  public void valueChanged(ListSelectionEvent evt) {
     
    if (evt.getSource() == groupList) {
      Vector g = da.getGroups();
      if (groupList.getSelectedIndex() >= 0) {
	showGroup((SequenceGroup)g.elementAt(groupList.getSelectedIndex()));
      }
    }
  }
  public void itemStateChanged(ItemEvent evt) {
    if (evt.getSource() == text) {
      int sel = groupList.getSelectedIndex();
      if (sel >= 0) {
	SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
	sg.setDisplayText(text.isSelected());
	da.displayText(sg);
      }
    } else if (evt.getSource() == colourText) {
      int sel = groupList.getSelectedIndex();
      if (sel >= 0) {
	SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());
	sg.setColourText(colourText.isSelected());
	da.colourText(sg);
      }
    } else if (evt.getSource() == colourScheme) {
      int sel = groupList.getSelectedIndex();
      if (sel >= 0) {
	SequenceGroup sg = (SequenceGroup)da.getGroups().elementAt(groupList.getSelectedIndex());

	int schemeno = colourScheme.getSelectedIndex();
	ColourSchemeI cs = ColourSchemeFactory.get(schemeno);

	if (cs instanceof ResidueColourScheme) {
          System.err.println("Consensus setting not implemented");
	}
	
	da.setColourScheme(sg);
      }
    } else {
      System.out.println("ERROR: Unhandled ItemEvent in GroupPopup");
    }
  }
  
  public void applyCommand() {
    if (parent instanceof AlignFrame) { 
      AlignFrame af = (AlignFrame)parent;
      af.updateFont();
    }
  }
}
