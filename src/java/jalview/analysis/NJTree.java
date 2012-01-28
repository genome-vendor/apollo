package jalview.analysis;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.util.*;
import jalview.gui.schemes.ResidueProperties;

import java.util.*;
import java.awt.*;

public class NJTree {

  Vector cluster;
  AlignSequenceI[] sequence;
  
  int done[];
  int noseqs;
  int noClus;
  
  float distance[][];
  
  int mini;
  int minj;
  float ri;
  float rj;
  
  Vector groups = new Vector();
  SequenceNode maxdist;
  SequenceNode top;

  float maxDistValue;
  float maxheight;
  
  int ycount;
  
  Vector node;

  String type;
  String pwtype;

  Object found = null;
  Object leaves = null;

  public NJTree(SequenceNode node) {
    top = node;
    maxheight = findHeight(top);
  }	

  public NJTree(AlignSequenceI[] sequence) {
    this(sequence,"NJ","BL");
  }
  
  public NJTree(AlignSequenceI[] sequence,String type,String pwtype ) {

    this.sequence = sequence;
    this.node     = new Vector();
    this.type     = type;
    this.pwtype   = pwtype;

    if (!(type.equals("NJ"))) {
      type = "AV";
    }

    if (!(pwtype.equals("PID"))) {
      type = "BL";
    }

    int i=0;
    
    done = new int[sequence.length];

    while (i < sequence.length && sequence[i] != null) {
      done[i] = 0;
      i++;
    }
    
    noseqs = i++;

    distance = findDistances();
    
    makeLeaves();
    
    noClus = cluster.size();

    cluster();

  }


  public void cluster() {

    while (noClus > 2) {
      if (type.equals("NJ")) {
        float mind = findMinNJDistance();
      } else {
        float mind = findMinDistance();
      }

      Cluster c = joinClusters(mini,minj);

      done[minj] = 1;

      cluster.setElementAt(null,minj);
      cluster.setElementAt(c,mini);

      noClus--;
    }

    boolean onefound = false;

    int one = -1;
    int two = -1;

    for (int i=0; i < noseqs; i++) {
      if (done[i] != 1) {
        if (onefound == false) {
          two = i;
          onefound = true;
        } else {
          one = i;
        }
      }
    }

    Cluster c = joinClusters(one,two);
    top = (SequenceNode)(node.elementAt(one));

    reCount(top);
    findHeight(top);
    findMaxDist(top);
    
  }	

  public Cluster joinClusters(int i, int j) {

    float dist = distance[i][j];

    int noi = ((Cluster)cluster.elementAt(i)).value.length;
    int noj = ((Cluster)cluster.elementAt(j)).value.length;

    int[] value = new int[noi + noj];

    for (int ii = 0; ii < noi;ii++) {
      value[ii] =  ((Cluster)cluster.elementAt(i)).value[ii];
    }

    for (int ii = noi; ii < noi+ noj;ii++) {
      value[ii] =  ((Cluster)cluster.elementAt(j)).value[ii-noi];
    }

    Cluster c = new Cluster(value);
    
    ri = findr(i,j);
    rj = findr(j,i);

    if (type.equals("NJ")) {
      findClusterNJDistance(i,j);
    } else {
      findClusterDistance(i,j);
    }

    SequenceNode sn = new SequenceNode();

    sn.setLeft((SequenceNode)(node.elementAt(i)));
    sn.setRight((SequenceNode)(node.elementAt(j)));

    SequenceNode tmpi = (SequenceNode)(node.elementAt(i));
    SequenceNode tmpj = (SequenceNode)(node.elementAt(j));

    if (type.equals("NJ")) {
      findNewNJDistances(tmpi,tmpj,dist);
    } else {
      findNewDistances(tmpi,tmpj,dist);
    }

    tmpi.setParent(sn);
    tmpj.setParent(sn);

    node.setElementAt(sn,i);
    return c;
  }

  public void findNewNJDistances(SequenceNode tmpi, SequenceNode tmpj, float dist) {

    float ih = 0;
    float jh = 0;

    SequenceNode sni = tmpi;
    SequenceNode snj = tmpj;

    tmpi.dist = (dist + ri - rj)/2;
    tmpj.dist = (dist - tmpi.dist);

    if (tmpi.dist < 0) {
      tmpi.dist = 0;
    }
    if (tmpj.dist < 0) {
      tmpj.dist = 0;
    }
  }

  public void findNewDistances(SequenceNode tmpi,SequenceNode tmpj,float dist) {

    float ih = 0;
    float jh = 0;

    SequenceNode sni = tmpi;
    SequenceNode snj = tmpj;

    while (sni != null) {
      ih = ih + sni.dist;
      sni = (SequenceNode)sni.left();
    }

    while (snj != null) {
      jh = jh + snj.dist;
      snj = (SequenceNode)snj.left();
    }

    tmpi.dist = (dist/2 - ih);
    tmpj.dist = (dist/2 - jh);
  }



  public void findClusterDistance(int i, int j) {

    int noi = ((Cluster)cluster.elementAt(i)).value.length;
    int noj = ((Cluster)cluster.elementAt(j)).value.length;

    // New distances from cluster to others
    float[] newdist = new float[noseqs];

    for (int l = 0; l < noseqs; l++) {
      if ( l != i && l != j) {
        newdist[l] = (distance[i][l] * noi + distance[j][l] * noj)/(noi + noj);
      } else {
        newdist[l] = 0;
      }
    }

    for (int ii=0; ii < noseqs;ii++) {
      distance[i][ii] = newdist[ii];
      distance[ii][i] = newdist[ii];
    }
  }

  public void findClusterNJDistance(int i, int j) {

    int noi = ((Cluster)cluster.elementAt(i)).value.length;
    int noj = ((Cluster)cluster.elementAt(j)).value.length;

    // New distances from cluster to others
    float[] newdist = new float[noseqs];

    for (int l = 0; l < noseqs; l++) {
      if ( l != i && l != j) {
        newdist[l] = (distance[i][l] + distance[j][l] - distance[i][j])/2;
      } else {
        newdist[l] = 0;
      }
    }

    for (int ii=0; ii < noseqs;ii++) {
      distance[i][ii] = newdist[ii];
      distance[ii][i] = newdist[ii];
    }
  }

  public float findr(int i, int j) {

    float tmp = 1;
    for (int k=0; k < noseqs;k++) {
      if (k!= i && k!= j && done[k] != 1) {
        tmp = tmp + distance[i][k];
      }
    }

    if (noClus > 2) {
      tmp = tmp/(noClus - 2);
    }

    return tmp;
  }

  public float findMinNJDistance() {

    float min = 100000;

    for (int i=0; i < noseqs-1; i++) {
      for (int j=i+1;j < noseqs;j++) {
        if (done[i] != 1 && done[j] != 1) {
          float tmp = distance[i][j] - (findr(i,j) + findr(j,i));
          if (tmp < min) {
            
            mini = i;
            minj = j;

            min = tmp;
            
          }	
        }
      }
    }
    return min;
  }

  public float findMinDistance() {

    float min = 100000;

    for (int i=0; i < noseqs-1;i++) {
      for (int j = i+1; j < noseqs;j++) {
        if (done[i] != 1 && done[j] != 1) {
          if (distance[i][j] < min) {
            mini = i;
            minj = j;

            min = distance[i][j];
          }
        }
      }
    }
    return min;
  }

  public float[][] findDistances() {

    float[][] distance = new float[noseqs][noseqs];

    if (pwtype.equals("PID")) {
      for (int i = 0; i < noseqs-1; i++) {
        for (int j = i; j < noseqs; j++) {
          if (j==i) {
            distance[i][i] = 0;
          } else {
            distance[i][j] = 100-Comparison.compare(sequence[i],sequence[j]);
            distance[j][i] = distance[i][j];
          }
        }
      }
    } else if (pwtype.equals("BL")) {
      int   maxscore = 0;

      for (int i = 0; i < noseqs-1; i++) {
        for (int j = i; j < noseqs; j++) {
          int score = 0;
          for (int k=0; k < sequence[i].getLength(); k++) {
            score += ResidueProperties.getBLOSUM62(String.valueOf(sequence[i].getBaseAt(k)),
						   String.valueOf(sequence[j].getBaseAt(k)));
          }
          distance[i][j] = (float)score;
          if (score > maxscore) {
            maxscore = score;
          }
        }
      }
      for (int i = 0; i < noseqs-1; i++) {
        for (int j = i; j < noseqs; j++) {
          distance[i][j] =  (float)maxscore - distance[i][j];
          distance[j][i] = distance[i][j];
        }
      }
    } else if (pwtype.equals("SW")) {
      float max = -1;
      for (int i = 0; i < noseqs-1; i++) {
        for (int j = i; j < noseqs; j++) {
          AlignSeq as = new AlignSeq(sequence[i],sequence[j],"pep");
          as.calcScoreMatrix();
          as.traceAlignment();
          as.printAlignment();
          distance[i][j] = (float)as.maxscore;
          if (max < distance[i][j]) {
            max = distance[i][j];
          }
        }
      }
      for (int i = 0; i < noseqs-1; i++) {
        for (int j = i; j < noseqs; j++) {
          distance[i][j] =  max - distance[i][j];
          distance[j][i] = distance[i][j];
        }
      }
    }
    
    return distance;
  }

  public void makeLeaves() {
    cluster = new Vector();

    for (int i=0; i < noseqs; i++) {
      SequenceNode sn = new SequenceNode();

      sn.setElement(sequence[i]);
      sn.setName(sequence[i].getName());
      node.addElement(sn);

      int[] value = new int[1];
      value[0] = i;

      Cluster c = new Cluster(value);
      cluster.addElement(c);
    }	
  }

  public Vector findLeaves(SequenceNode node, Vector leaves) {
    if (node == null) {
      return leaves;
    }

    if (node.left() == null && node.right() == null) {
      leaves.addElement(node);
      return leaves;
    } else {
      findLeaves((SequenceNode)node.left(),leaves);
      findLeaves((SequenceNode)node.right(),leaves);
    }
    return leaves;
  }	

  public Object findLeaf(SequenceNode node, int count) {
    found = _findLeaf(node,count);

    return found;
  }
  public Object _findLeaf(SequenceNode node,int count) {
    if (node == null) {
      return null;
    }
    if (node.ycount == count) {
      found = node.element();
      return found;
    } else {
      _findLeaf((SequenceNode)node.left(),count);
      _findLeaf((SequenceNode)node.right(),count);
    }
    System.out.println("returning");
    return found;
  }
  
  public void printNode(SequenceNode node) {
    if (node == null) {
      return;
    }
    if (node.left() == null && node.right() == null) {
      if (jalview.gui.Config.DEBUG) {
	System.out.println("Leaf = " + ((AlignSequenceI)node.element()).getName());
	System.out.println("Dist " + ((SequenceNode)node).dist);
	System.out.println("Boot " + node.getBootstrap());
      }
    } else {
      if (jalview.gui.Config.DEBUG) 
	System.out.println("Dist " + ((SequenceNode)node).dist);
      printNode((SequenceNode)node.left());
      printNode((SequenceNode)node.right());
    }
  }
  public void findMaxDist(SequenceNode node) {
    if (node == null) {
      return;
    }
    if (node.left() == null && node.right() == null) {
      if (jalview.gui.Config.DEBUG) 
	System.out.println("Dist " + ((SequenceNode)node).dist);
      float dist = ((SequenceNode)node).dist;
      if (dist > maxDistValue) {
	  maxdist      = (SequenceNode)node;
	  maxDistValue = dist;
      }
    } else {
      findMaxDist((SequenceNode)node.left());
      findMaxDist((SequenceNode)node.right());
    }
  }
    public Vector getGroups() {
	return groups;
    }
    public float getMaxHeight() {
	return maxheight;
    }
  public void  groupNodes(SequenceNode node, float threshold) {
    if (node == null) {
      return;
    }

    if (node.height/maxheight > threshold) {
      groups.addElement(node);
    } else {
      groupNodes((SequenceNode)node.left(),threshold);
      groupNodes((SequenceNode)node.right(),threshold);
    }
  }

  public float findHeight(SequenceNode node) {

    if (node == null) {
      return maxheight;
    }

    if (node.left() == null && node.right() == null) {
      node.height = ((SequenceNode)node.parent()).height + node.dist;
      if (node.height > maxheight) {
        return node.height;
      } else {
        return maxheight;
      }
    } else {
      if (node.parent() != null) {
        node.height = ((SequenceNode)node.parent()).height + node.dist;
      } else {
        maxheight = 0;
        node.height = (float)0.0;
      }

      maxheight = findHeight((SequenceNode)(node.left()));
      maxheight = findHeight((SequenceNode)(node.right()));
    }
    return maxheight;
  }
  public SequenceNode reRoot() {
    if (maxdist != null) {
      ycount = 0;
      float tmpdist = maxdist.dist;

      // New top
      SequenceNode sn = new SequenceNode();
      sn.setParent(null);

      // New right hand of top
      SequenceNode snr = (SequenceNode)maxdist.parent();
      changeDirection(snr,maxdist);
      System.out.println("Printing reversed tree");
      printN(snr);
      snr.dist = tmpdist/2;
      maxdist.dist = tmpdist/2;

      snr.setParent(sn);
      maxdist.setParent(sn);

      sn.setRight(snr);
      sn.setLeft(maxdist);

      top = sn;

      ycount = 0;
      reCount(top);
      findHeight(top);

    }
    return top;
  }
  public static void printN(SequenceNode node) {
    if (node == null) {
      return;
    }

    if (node.left() != null && node.right() != null) {
      printN((SequenceNode)node.left());
      printN((SequenceNode)node.right());
    } else {
      System.out.println(" name = " + ((AlignSequenceI)node.element()).getName());
    }
    System.out.println(" dist = " + ((SequenceNode)node).dist + " " + ((SequenceNode)node).count + " " + ((SequenceNode)node).height);
  }

    public void reCount(SequenceNode node) {
	ycount = 0;
	_reCount(node);
    }
  public void _reCount(SequenceNode node) {
    if (node == null) {
      return;
    }

    if (node.left() != null && node.right() != null) {
      _reCount((SequenceNode)node.left());
      _reCount((SequenceNode)node.right());

      SequenceNode l = (SequenceNode)node.left();
      SequenceNode r = (SequenceNode)node.right();

      ((SequenceNode)node).count  = l.count + r.count;
      ((SequenceNode)node).ycount = (l.ycount + r.ycount)/2;

    } else {
      ((SequenceNode)node).count = 1;
      ((SequenceNode)node).ycount = ycount++;
    }

  }
    public void swapNodes(SequenceNode node) {
	if (node == null) {
	    return;
	}
	SequenceNode tmp = (SequenceNode)node.left();

	node.setLeft(node.right());
	node.setRight(tmp);
    }
  public void changeDirection(SequenceNode node, SequenceNode dir) {
    if (node == null) {
      return;
    }
    if (node.parent() != top) {
      changeDirection((SequenceNode)node.parent(), node);

      SequenceNode tmp = (SequenceNode)node.parent();

      if (dir == node.left()) {
        node.setParent(dir);
        node.setLeft(tmp);
      } else if (dir == node.right()) {
        node.setParent(dir);
        node.setRight(tmp);
      }

    } else {
      if (dir == node.left()) {
        node.setParent(node.left());

        if (top.left() == node) {
          node.setRight(top.right());
        } else {
          node.setRight(top.left());
        }
      } else {
        node.setParent(node.right());

        if (top.left() == node) {
          node.setLeft(top.right());
        } else {
          node.setLeft(top.left());
        }
      }
    }
  }
    public void setMaxDist(SequenceNode node) {
	this.maxdist = maxdist;
    }
    public SequenceNode getMaxDist() {
	return maxdist;
    }
    public SequenceNode getTopNode() {
	return top;
    }

}



class Cluster {

  int[] value;

  public Cluster(int[] value) {
    this.value = value;
  }

}

