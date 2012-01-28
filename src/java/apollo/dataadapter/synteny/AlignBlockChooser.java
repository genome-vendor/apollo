package apollo.dataadapter.synteny;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.ensembl.datamodel.*;
import apollo.main.Apollo;
import apollo.util.GuiUtil;

/**
 * I am popped up by the compara adapter if the user is required to choose
 * between different align blocks that a fixed genomic range is mapped to - 
 * for instance, if human chr A:x-y maps to mosquito chr B:l-m, AND 
 * mosquito chr C: p-q, then we need the user to choose between these
 * regions before we can display the alignments from human<->mosquito.
 * ...I display a set of radion buttons for each region, and ask the
 * user to select one.
**/
public class AlignBlockChooser
extends JDialog{
  private HashMap alignHash;//key is chromosome - value is Object[]
  private HashMap locationHash = new HashMap();//key is button - value is the Object[] in alignHash.
  private static Object[] userChoice;
  private JButton oKButton = new JButton("OK");
  private JLabel[] labels;
  private ButtonGroup buttonGroup = new ButtonGroup();
  private java.util.ArrayList buttonList = new ArrayList();
  private JComponent parent;
  
  public class OKButtonListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      JRadioButton button = null;
      Enumeration buttons = 
        AlignBlockChooser.this.getButtonGroup().getElements();
      
      while(buttons.hasMoreElements()){
        button = (JRadioButton)buttons.nextElement();
        if(button.isSelected()){
          setUserChoice((Object[])getLocationHash().get(button));
        }//end if
      }//end while
      
      AlignBlockChooser.this.dispose();
    }
  }
  
  public AlignBlockChooser(final HashMap alignHash){
    super((JFrame)null, true);
    this.alignHash = alignHash;
    initialiseGUI();
    pack();
    show();
  }//end AlignBlockChooser
  
  private void initialiseGUI(){
    HashMap hash = getAlignHash();
    String key = null;
    Object[] align = null;
    Location alignLimit = null;
    Integer alignCount = null;
    int counter = 0;
    JRadioButton radioButton = null;
    String buttonLabel = null;
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    OKButtonListener oKButtonListener = null;
    ArrayList entryArray  = new ArrayList(hash.entrySet());
  
    contentPanel.add(
      new JLabel("Select one of the following regions from the target-species to be loaded:"),
      GuiUtil.makeConstraintAt(0, counter,  1)
    );

    Comparator alignCountOrder = new Comparator() {
       public int compare(Object o1, Object o2) {
          Object [] align1 = (Object[])((Map.Entry)o1).getValue();
          int alignCount1 = ((Integer)align1[1]).intValue();
          Object [] align2 = (Object[])((Map.Entry)o2).getValue();
          int alignCount2 = ((Integer)align2[1]).intValue();
          if (alignCount1 < alignCount2) return -1;
          else if (alignCount1 > alignCount2) return 1;
          return 0;
       }
    };

    Collections.sort(entryArray,alignCountOrder);
    
    counter++;
 
    Iterator entryIter = entryArray.iterator();
  
    while(entryIter.hasNext()){
      Map.Entry entry = (Map.Entry)entryIter.next();
      align = (Object[])entry.getValue();
      alignLimit = (Location)align[0];
      alignCount = (Integer)align[1];
      buttonLabel = 
        alignLimit.getSeqRegionName()+"  :  "+alignLimit.getStart()+" - "+
        alignLimit.getEnd()+
        "  ("+alignCount.toString()+" aligns)";
      radioButton = new JRadioButton(buttonLabel);
      getLocationHash().put(radioButton, align);
      getButtonGroup().add(radioButton);
      contentPanel.add(radioButton, GuiUtil.makeConstraintAt(0, counter,  1));
      counter++;
    }//end while

    radioButton.setSelected(true);
    
    oKButtonListener = new OKButtonListener();
    
    oKButton.addActionListener(oKButtonListener);
    
    GridBagConstraints constraint = GuiUtil.makeConstraintAt(0, counter, 1);
    constraint.anchor = GridBagConstraints.EAST;
    contentPanel.add(oKButton, constraint);
    
    JScrollPane scroller = new JScrollPane(contentPanel);
    //this.getContentPane().add(contentPanel, BorderLayout.CENTER);
    this.getContentPane().add(scroller, BorderLayout.CENTER);

    
  }//end initialiseGUI


  
  private HashMap getAlignHash(){
    return alignHash;
  }//end getAlignHash
  
  public static Object[] getUserAlignChoice(final HashMap alignHash){

    Runnable showWindow = 
      new Runnable(){
        public void run(){
          Apollo.setLog4JDiagnosticContext();
          AlignBlockChooser chooser = new AlignBlockChooser(alignHash);
          Apollo.clearLog4JDiagnosticContext();
        }
      };
    
    try{
      SwingUtilities.invokeAndWait(showWindow);
    }catch(java.lang.reflect.InvocationTargetException exception){
      throw new IllegalStateException("SwingUtilities invokeAndWait has failed: this is a fatal problem: "+exception.getClass()+exception.getMessage());
    }catch(InterruptedException exception){
      throw new IllegalStateException("SwingUtilities invokeAndWait has failed: this is a fatal problem"+exception.getClass()+exception.getMessage());
    }

    return AlignBlockChooser.getUserChoice();
  }//end getUserAlignChoice
  
  private static void setUserChoice(Object[] choice){
    userChoice = choice;
  }//end setUserChoice
  
  private static Object[] getUserChoice(){
    return userChoice;
  }//end getUserChoice
  
  private ButtonGroup getButtonGroup(){
    return buttonGroup;
  }//end getButtonGroup
  
  private HashMap getLocationHash(){
    return locationHash;
  }//end getLocationHash

}
