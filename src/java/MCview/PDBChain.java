/* Copyright (C) 1998  Michele Clamp
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package MCview;

import jalview.datamodel.*;
import jalview.gui.schemes.ResidueProperties;
import jalview.gui.*;
import apollo.datamodel.*;

import java.awt.*;
import java.util.*;


public class PDBChain {

  public String id;
  public Vector bonds = new Vector();
  public Vector atoms = new Vector();
  public Vector residues = new Vector();
  public int offset;

  public AlignSequenceI sequence;
  public boolean isVisible = false;

  public DrawableSequence ds;

  public PDBChain(String id) {
    this.id = id;
  }

  public String print() {
    String tmp = "";
    for (int i=0; i < bonds.size() ;i++) {
      tmp = tmp + ((Bond)bonds.elementAt(i)).at1.resName + " " + ((Bond)bonds.elementAt(i)).at1.resNumber +" " + offset+ "\n";
    }
    return tmp;
  }
  public void makeCaBondList() {
    for (int i = 0; i < (residues.size() - 1) ; i++) {
      Residue tmpres = (Residue)residues.elementAt(i);
      Residue tmpres2 = (Residue)residues.elementAt(i+1);
      myAtom at1 = tmpres.findAtom("CA");
      myAtom at2 = tmpres2.findAtom("CA");
      if ((at1 != null) && (at2 != null)) {
        if (at1.chain.equals(at2.chain)) {
          makeBond(at1,at2);
        }
      }
    }
  }

  public void makeBond(myAtom at1, myAtom at2) {
    float[] start = new float[3];
    float[] end = new float[3];

    start[0] = at1.x;
    start[1] = at1.y;
    start[2] = at1.z;

    end[0] = at2.x;
    end[1] = at2.y;
    end[2] = at2.z;

    bonds.addElement(new Bond(start, end, at1,at2));
  }

  public void makeResidueList() {
    int count = 0;
    String seq = "";
    for (int i = 0; i < atoms.size(); i++) {

      myAtom tmp = (myAtom)atoms.elementAt(i);
      String resName = tmp.resName;
      int resNumber = tmp.resNumber;
      int res = resNumber;

      if (i ==0) {
        offset = resNumber;
      }
      Vector resAtoms = new Vector();

      resAtoms.addElement((myAtom)atoms.elementAt(i));
      i++;
      resNumber = ((myAtom)atoms.elementAt(i)).resNumber;

      //Add atoms to a vector while the residue number
      //remains the same
      while ((resNumber == res) && (i < atoms.size())) {

        resAtoms.addElement((myAtom)atoms.elementAt(i));
        i++;
        if (i < atoms.size()) {
          resNumber = ((myAtom)atoms.elementAt(i)).resNumber;
        } else {
          resNumber++;
        }
      }

      //We need this to keep in step with the outer for i = loop
      i--;

      //Make a new Residue object with the new atoms vector
      residues.addElement(new Residue(resAtoms, resNumber - 1,count));
      count++;
      Residue tmpres = (Residue)residues.lastElement();
      myAtom tmpat = (myAtom)tmpres.atoms.elementAt(0);

      // Keep totting up the sequence
      if (ResidueProperties.getAA3Hash().get(tmpat.resName) == null) {
        System.out.println("Null aa3Hash for " + tmpat.resName);
      } else {
        String tmpres2 =
          ResidueProperties.aa[((Integer)ResidueProperties.getAA3Hash().get(tmpat.resName)).intValue()];
        seq = seq + tmpres2;
      }
      //      System.out.println(tmpat.resName + " " + tmpres2);
    }
    sequence = new AlignSequence("PDB_seq",seq,1,seq.length());
    System.out.println("Sequence = " + seq);
    System.out.println("No of residues = " +residues.size());
  }

  public void setChargeColours() {
    for (int i = 0; i < bonds.size(); i++) {
      try {
        Bond b = (Bond)bonds.elementAt(i);

        if (b.at1.resName.toUpperCase().equals("ASP") || b.at1.resName.toUpperCase().equals("GLU")) {
          b.startCol = Color.red;
        } else if (b.at1.resName.toUpperCase().equals("LYS") || b.at1.resName.toUpperCase().equals("ARG")) {
          b.startCol = Color.blue;
        } else if (b.at1.resName.toUpperCase().equals("CYS")) {
          b.startCol = Color.yellow;
        } else {
          int atno = ((Integer)ResidueProperties.getAA3Hash().get(b.at1.resName.toUpperCase())).intValue();
          b.startCol = Color.lightGray;
        }
        if (b.at2.resName.toUpperCase().equals("ASP") || b.at2.resName.toUpperCase().equals("GLU")) {
          b.endCol = Color.red;
        } else if (b.at2.resName.toUpperCase().equals("LYS") || b.at2.resName.toUpperCase().equals("ARG")) {
          b.endCol = Color.blue;
        } else if (b.at2.resName.toUpperCase().equals("CYS")) {
          b.endCol = Color.yellow;
        } else {
          int atno = ((Integer)ResidueProperties.getAA3Hash().get(b.at2.resName.toUpperCase())).intValue();
          b.endCol = Color.lightGray;
        }
      } catch (Exception e) {
        Bond b = (Bond)bonds.elementAt(i);
        b.startCol = Color.gray;
        b.endCol = Color.gray;
      }
    }
  }

  public void setHydrophobicityColours() {
    float hydmin = (float)ResidueProperties.getHydmin();
    float hydmax = (float)ResidueProperties.getHydmax();
    double[] hyd  = ResidueProperties.getHyd();

    Hashtable AA3Hash = ResidueProperties.getAA3Hash();

    for (int i = 0; i < bonds.size(); i++) {
      try {
        Bond b = (Bond)bonds.elementAt(i);

        int   atno = ((Integer)AA3Hash.get(b.at1.resName.toUpperCase())).intValue();
        float red  = ((float)hyd[atno] - hydmin)/(hydmax - hydmin);

        if (red > (float)1.0) {
          red = (float)1.0;
        }
        if (red < (float)0.0) {
          red = (float)0.0;
        }

        b.startCol = new Color(red,(float)0.0,(float)1.0-red);
        atno = ((Integer)AA3Hash.get(b.at2.resName.toUpperCase())).intValue();

        red = ((float)hyd[atno] - hydmin)/(hydmax - hydmin);

        if (red > (float)1.0) {
          red = (float)1.0;
        }
        if (red < (float)0.0) {
          red = (float)0.0;
        }

        b.endCol = new Color(red,(float)0.2,(float)1.0-red);
      } catch (Exception e) {
        Bond b = (Bond)bonds.elementAt(i);
        b.startCol = Color.gray;
        b.endCol = Color.gray;
      }
    }
  }

  public void colourBySequence() {
    colourBySequence(this.ds);
  }

  public void colourBySequence(DrawableSequence seq) {
  }

  public void setChainColours() {
    for (int i = 0; i < bonds.size(); i++) {
      Bond tmp = (Bond)bonds.elementAt(i);
      try {
        tmp.startCol = (Color) ResidueProperties.getChainColours().get(id);
        tmp.endCol   = (Color) ResidueProperties.getChainColours().get(id);
      } catch (Exception e) {
        tmp.startCol = Color.lightGray;
        tmp.endCol   = Color.lightGray;
      }
    }
  }
}


