package jalview.gui.popups;

import jalview.gui.*;
import jalview.datamodel.*;

import MCview.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;
import java.net.*;

import javax.swing.*;

public class PDBPopup extends Popup implements ItemListener {
  java.io.PrintStream o = System.out;

  DefaultListModel listModel;

  JList pdbList;

  JLabel title1;
  JLabel title1a;
  JLabel title2;
  JLabel sequenceLabel;

  JTextField tf;
  JLabel tfLabel;
  JButton fetch;

  DrawableAlignment da;
  Vector codes;
  AlignSequenceI seq;

  public PDBPopup(JFrame parent, AlignViewport av, Controller c,String title, DrawableAlignment da) {
    super(parent,av,c,title);
    System.out.println("poppoy");
    this.da = da;

    title1 = new JLabel("Select a pdb code obtained from the database entry");
    title2 = new JLabel("Or enter a code yourself");

    sequenceLabel = new JLabel("No code selected");
    title1a = new JLabel();
    listModel = new DefaultListModel();

    pdbList = new JList(listModel);

    addCodes();

    tf = new JTextField(20);

    tfLabel = new JLabel("Enter PDB code");
    fetch = new JButton("Fetch structure");

    fetch.addActionListener(pal);

    apply.setLabel("Fetch structure");

    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5,5,5,5);

    add(title1,gb,gbc,0,0,2,1);         // Title for pdb codes already present
    add(pdbList,gb,gbc,0,1,1,3);        // list of pdb codes
    add(sequenceLabel,gb,gbc,1,1,1,1);  // Label for the pdb list - selected sequence
    add(title1a,gb,gbc,1,2,1,1);  // Label for the pdb list - selected sequence
    gbc.fill = GridBagConstraints.NONE;
    add(apply,gb,gbc,1,3,1,1);          // Fetch structure

    gbc.fill = GridBagConstraints.BOTH;
    add(title2,gb,gbc,0,4,2,1);         // title 2 for text input
    //    add(tfLabel,gb,gbc,0,5,1,1);        // label for text input
    add(tf,gb,gbc,0,5,1,1);             // field for typing
    gbc.fill = GridBagConstraints.NONE;
    add(fetch,gb,gbc,1,5,1,1);          // button for fetching the structure



    gbc.fill = GridBagConstraints.BOTH;

    //status = new Label("Status:",Label.LEFT);
    add(status,gb,gbc,0,6,2,1);
    gbc.fill = GridBagConstraints.NONE;
    add(close,gb,gbc,0,7,2,1);

    pack();
    show();

  }

  public void addCodes() {
      //codes = da.getPDBCodes();
      codes = new Vector();
    if (codes.size() == 0) {
      sequenceLabel.setText("No PDB codes available");
    } else {
      for (int i=0; i < codes.size() ;i++) {
        listModel.addElement((String)codes.elementAt(i));
      }
    }
  }


  /*
    public boolean handleEvent(Event e) {
      if (e.target == apply && e.id == 1001) {
        applyCommand();
      } else if (e.target == fetch && e.id == 1001 &! tf.getText().equals("")) {
        if (da.ds[0].pdbcode == null) {
  	da.ds[0].pdbcode = new Vector();
        }
        da.ds[0].pdbcode.insertElementAt(tf.getText(),0);
        seq = da.ds[0];
        applyCommand();
      } else if (e.target == pdbList) {
   
        String code = (String)listModel.getSelectedValue();
   
        int i=0;
   
        while( i < da.ds.length && da.ds[i] != null) {
  	if (da.ds[i].pdbcode != null) {
  	  for (int j = 0; j < da.ds[i].pdbcode.size(); j++) {
  	    //   System.out.println("Code = " + code + " " + (String)da.ds[i].pdbcode.elementAt(j));
  	    if (((String)da.ds[i].pdbcode.elementAt(j)).equals(code)) {
  	      seq = da.ds[i];
  	    }
  	  }
  	}
  	i++;
        }
        sequenceLabel.setText("PDB structure is attached to :");
        title1a.setText(seq.getName());
        return true;
      } else {
        super.handleEvent(e);
      }
      return super.handleEvent(e);
    }
  */

  public void itemStateChanged(ItemEvent evt) {
    if (evt.getSource() == pdbList) {

      String code = (String)pdbList.getSelectedValue();

      int i=0;

      while( i < da.getSequences().size()) {
        if (da.getSequenceAt(i).getPDBCodes() != null) {
          for (int j = 0; j < da.getSequenceAt(i).getPDBCodes().size(); j++) {
            //   System.out.println("Code = " + code + " " + (String)da.ds[i].pdbcode.elementAt(j));
            if (((String)da.getSequenceAt(i).getPDBCodes().elementAt(j)).equals(code)) {
              seq = da.getSequenceAt(i);
            }
          }
        }
        i++;
      }
      sequenceLabel.setText("PDB structure is attached to :");
      title1a.setText(seq.getName());
    } else {
      System.out.println("Error: Unknown item source " + evt.getSource() + " in " + this);
    }
  }

  protected void otherAction(ActionEvent evt) {
    if (evt.getSource() == fetch) {
      if (da.getSequenceAt(0).getPDBCodes() == null) {
        System.out.println("ERROR: Sequence has null PDBCodes - should not be possible.");
        //da.getSequenceAt(0).setPDBCodes(new Vector());
      }
      //da.ds[0].pdbcode.insertElementAt(tf.getText(),0);
      System.out.println("Note - didn't do an insert in pdbcode but an add.");
      da.getSequenceAt(0).addPDBCode(tf.getText());
      seq = da.getSequenceAt(0);
      applyAction(evt);
    } else {
      System.out.println("Error: Unknown action source " + evt.getSource() + " in " + this);
    }
  }

  /*
    public void applyCommand() {
  */

  public void fetchPDBStructure(AlignSequenceI seq, String srsServer) throws UnknownHostException,IOException {
    if (seq.getPDBCodes().size() > 0) {
      System.out.println("code = " + seq.getPDBCodes().elementAt(0));
      PDBfile pdb = new PDBfile("http://" + srsServer + "wgetz?-e+[pdb-id:" + seq.getPDBCodes().elementAt(0) + "]","URL");
//      seq.setPDBfile(pdb);
//      ((PDBChain)pdb.chains.elementAt(seq.maxchain)).isVisible = true;
//      ((PDBChain)pdb.chains.elementAt(seq.maxchain)).ds = seq;
//      ((PDBChain)pdb.chains.elementAt(seq.maxchain)).colourBySequence();
 
      rotFrame f = new rotFrame(pdb);
      f.resize(500,500);
      f.show();
 
    } else {
      System.out.println("No pdb code found");
    }
  } 

  protected void applyAction(ActionEvent evt) {
    if (seq != null) {
      try {
        fetchPDBStructure(seq,Config.getSRSServer());
      } catch (UnknownHostException e) {

        System.out.print("\07");
        System.out.flush();

        status.setText("ERROR: host can't be contacted");
        status.validate();
        System.out.println(e);
      }
      catch (IOException e) {
        System.out.print("\07");
        System.out.flush();

        status.setText("ERROR: IO exception in fetching pdb entry");
        status.validate();
        System.out.println(e);
      }
    } else {
      sequenceLabel.setText("No sequence found");
    }
  }
}
