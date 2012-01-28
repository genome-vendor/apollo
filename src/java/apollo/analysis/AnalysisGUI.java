package apollo.analysis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import apollo.analysis.RemoteBlastNCBI.BlastOptions;
import apollo.analysis.filter.AnalysisFilter;
import apollo.analysis.filter.AnalysisInput;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.Style;
import apollo.config.TierProperty;
import apollo.datamodel.CurationSet;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.CurationManager;
import apollo.util.FeatureList;
import apollo.util.GuiUtil;

/** Provides GUI for running different analysis tools and importing the results into Apollo.
 * 
 * @author elee
 *
 */

public class AnalysisGUI extends JFrame {

  private static final long serialVersionUID = 1L;

  private JTabbedPane pane;
  private Integer start;
  private Integer end;
  private Integer strand;
  private FeatureList fl;

  /** Constructor.
   * 
   */
  public AnalysisGUI()
  {
    this(CurationManager.getActiveCurationState().getCurationSet().getLow(),
        CurationManager.getActiveCurationState().getCurationSet().getHigh(),
        1);
  }

  /** Constructor.
   * 
   * @param start - start genomic position of the segment to analyze
   * @param end - end genomic position of the segment to analyze
   */
  public AnalysisGUI(int start, int end)
  {
    this(start, end, 1, null);
  }
  
  /** Constructor.
   * 
   * @param start - start genomic position of the segment to analyze
   * @param end - end genomic position of the segment to analyze
   * @param strand - strand for the genomic region (1 for plus, -1 for minus)
   */
  public AnalysisGUI(int start, int end, int strand)
  {
    this(start, end, strand, null);
  }
  
  /** Constructor.
   * 
   * @param start - start genomic position of the segment to analyze
   * @param end - end genomic position of the segment to analyze
   * @param fl - FeatureList containing features that may be used as part of analysis
   */
  public AnalysisGUI(int start, int end, int strand, FeatureList fl)
  {
    super("Run Analysis");
    //init(start - cs.getLow() + 1, end - cs.getLow() + 1);
    init(start, end, strand, fl);
  }
  
  private class AnalysisThread implements Runnable
  {
    public void run()
    {
      try {
        AnalysisPane analysis = getAnalysisPane((Container)pane.getSelectedComponent());
        String type = analysis.runAnalysis();
        analysis.setTier(type);
        apollo.util.IOUtil.informationDialog("Remote analysis complete");
        StrandedZoomableApolloPanel szap = CurationManager.getActiveCurationState().getSZAP();
        szap.setFeatureSet(szap.getCurationSet());
      }
      catch (Exception e) {
        apollo.util.IOUtil.errorDialog("Error running analysis: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
  
  private AnalysisPane getAnalysisPane(Container container)
  {
    if (container instanceof AnalysisPane) {
      return (AnalysisPane)container;
    }
    for (Component child : container.getComponents()) {
      if (child instanceof Container) {
        AnalysisPane c = getAnalysisPane((Container)child);
        if (c != null) {
          return c;
        }
      }
    }
    return null;
  }
  
  private void init(int start, int end, int strand, FeatureList fl)
  {
    this.start = start;
    this.end = end;
    this.strand = strand;
    this.fl = fl;
    
    setLayout(new GridBagLayout());
    
    pane = new JTabbedPane();
    GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 2);
    add(pane, c);
    pane.addTab("NCBI-BLAST", new NCBIBlastPane());
    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel2.add(new NCBIPrimerBlastPane());
    pane.addTab("NCBI Primer-BLAST", panel2);

    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    JButton runButton = new JButton("Run");
    //add(runButton, c);
    runButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        //setVisible(false);
        new Thread(new AnalysisThread()).start();
        dispose();
      }
    });
    
    c.gridx = 1;
    c.gridy = 1;
    JButton cancelButton = new JButton("Cancel");
    //add(cancelButton, c);
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        dispose();
      }
    });
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(runButton);
    buttonPanel.add(cancelButton);
    c.gridx = 0;
    ++c.gridy;
    c.gridwidth = 2;
    add(buttonPanel, c);

    
    pack();
    setVisible(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }
  
  private abstract class AnalysisPane extends JPanel {
    
    public abstract String runAnalysis() throws Exception;
    
    public boolean setTier(String type)
    {
      PropertyScheme scheme = Config.getPropertyScheme();
      TierProperty tp = scheme.getTierProperty(type, false);
      if (tp == null) {
        tp = new TierProperty(type, true, true);
        tp.setSorted(true);
        scheme.addTierType(tp);
      }
      FeatureProperty fp = tp.getFeatureProperty(type);
      if (fp == null) {
        Vector<String> types = new Vector<String>();
        types.add(type);
        fp = new FeatureProperty(tp, type, types);
        tp.addFeatureProperty(fp);
        return true;
      }
      return false;
    }
    
  }
  
  private class NCBIPrimerBlastPane extends AnalysisPane
  {

    private static final long serialVersionUID = 1l;

    private PrimerOptsPanel primerOptsPanel;
    private PrimerCheckOptsPanel primerCheckOptsPanel;
    private PrimerPostProcessPanel primerPostProcessPanel;
    
    public NCBIPrimerBlastPane()
    {
      super();
      init();
    }
    
    public String runAnalysis() throws Exception {
      RemotePrimerBlastNCBI primer = new RemotePrimerBlastNCBI(getOptions());
      CurationSet cs = CurationManager.getActiveCurationState().getCurationSet();
      String type = primer.runAnalysis(cs, cs.getRefSequence().getSubSequence(start, end), start - 1, fl);
      return type;
    }
    
    public boolean setTier(String type)
    {
      boolean newTier = super.setTier(type);
      if (newTier) {
        PropertyScheme scheme = Config.getPropertyScheme();
        FeatureProperty fp = scheme.getFeatureProperty(type);
        fp.setStyle("DrawablePrimerSet");
      }
      return newTier;
    }
    
    private RemotePrimerBlastNCBI.PrimerBlastOptions getOptions()
    {
      RemotePrimerBlastNCBI.PrimerBlastOptions opts = new RemotePrimerBlastNCBI.PrimerBlastOptions();
      primerOptsPanel.setOptions(opts);
      primerCheckOptsPanel.setOptions(opts);
      primerPostProcessPanel.setOptions(opts);
      return opts;
    }
    
    private void init()
    {
      setLayout(new GridBagLayout());
      GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

      primerOptsPanel = new PrimerOptsPanel();
      primerOptsPanel.setBorder(BorderFactory.createTitledBorder("Primer Parameters"));
      add(primerOptsPanel, c);
      ++c.gridy;
      primerCheckOptsPanel = new PrimerCheckOptsPanel();
      primerCheckOptsPanel.setBorder(BorderFactory.createTitledBorder("Primer Pair Specificity Checking Parameters"));
      add(primerCheckOptsPanel, c);
      ++c.gridy;
      primerPostProcessPanel = new PrimerPostProcessPanel();
      primerPostProcessPanel.setBorder(BorderFactory.createTitledBorder("Post Processing Options"));
      add(primerPostProcessPanel, c);
    }
    
    private class PrimerOptsPanel extends JPanel
    {
      
      private static final long serialVersionUID = 1L;
      
      private JTextField startField;
      private JTextField endField;
      private JTextField forwardPrimerField;
      private JTextField reversePrimerField;
      private JTextField minPcrSizeField;
      private JTextField maxPcrSizeField;
      private JTextField numPrimersField;
      private JTextField minTmField;
      private JTextField optTmField;
      private JTextField maxTmField;
      private JTextField maxTmDiffField;
      
      public PrimerOptsPanel()
      {
        super();
        init();
      }
      
      public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts)
      {
        if (forwardPrimerField.getText().length() > 0) {
          opts.setPrimerLeftInput(forwardPrimerField.getText());
        }
        if (reversePrimerField.getText().length() > 0) {
          opts.setPrimerLeftInput(reversePrimerField.getText());
        }
        if (minPcrSizeField.getText().length() > 0) {
          opts.setPrimerProductMin(Integer.parseInt(minPcrSizeField.getText()));
        }
        if (maxPcrSizeField.getText().length() > 0) {
          opts.setPrimerProductMax(Integer.parseInt(maxPcrSizeField.getText()));
        }
        if (numPrimersField.getText().length() > 0) {
          opts.setPrimerNumReturn(Integer.parseInt(numPrimersField.getText()));
        }
        if (minTmField.getText().length() > 0) {
          opts.setPrimerMinTm(Double.parseDouble(minTmField.getText()));
        }
        if (optTmField.getText().length() > 0) {
          opts.setPrimerOptTm(Double.parseDouble(optTmField.getText()));
        }
        if (maxTmField.getText().length() > 0) {
          opts.setPrimerMaxTm(Double.parseDouble(maxTmField.getText()));
        }
        if (maxTmDiffField.getText().length() > 0) {
          opts.setPrimerMaxDiffTm(Double.parseDouble(maxTmDiffField.getText()));
        }
        start = Integer.parseInt(startField.getText());
        end = Integer.parseInt(endField.getText());
      }
      
      private void init()
      {
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);
        
        startField = new JTextField(Integer.toString(start), 6);
        endField = new JTextField(Integer.toString(end), 6);
        
        c.gridwidth = 1;
        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Start"), c);
        ++c.gridx;
        add(new JLabel("End"), c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Genomic region"), c);
        ++c.gridx;
        add(startField, c);
        ++c.gridx;
        add(endField, c);
        
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Custom forward primer"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        forwardPrimerField = new JTextField(10);
        add(forwardPrimerField, c);
        
        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Custom reverse primer"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        reversePrimerField = new JTextField(10);
        add(reversePrimerField, c);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Min"), c);
        ++c.gridx;
        add(new JLabel("Max"), c);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("PCR product size"), c);
        ++c.gridx;
        minPcrSizeField = new JTextField("200", 6);
        add(minPcrSizeField, c);
        ++c.gridx;
        maxPcrSizeField = new JTextField("1000", 6);
        add(maxPcrSizeField, c);
        
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Number of primers to return"), c);
        ++c.gridx;
        numPrimersField = new JTextField("10", 6);
        add(numPrimersField, c);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Min"), c);
        ++c.gridx;
        add(new JLabel("Opt"), c);
        ++c.gridx;
        add(new JLabel("Max"), c);
        ++c.gridx;
        add(new JLabel("Max Tm difference"), c);
        
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Primer melting temperatures (Tm)"), c);
        ++c.gridx;
        minTmField = new JTextField("57.0", 4);
        add(minTmField, c);
        ++c.gridx;
        optTmField = new JTextField("60.0", 4);
        add(optTmField, c);
        ++c.gridx;
        maxTmField = new JTextField("63.0", 4);
        add(maxTmField, c);
        ++c.gridx;
        maxTmDiffField = new JTextField("3", 4);
        add(maxTmDiffField, c);

      }
      
    }
    
    private class PrimerCheckOptsPanel extends JPanel
    {
      
      private static final long serialVersionUID = 1L;
      
      private JCheckBox searchSpecificPrimerCheckbox;
      private JTextField organismField;
      private JComboBox databaseComboBox;
      private JComboBox totalPrimerSpecificityMismatchComboBox;
      private JComboBox primer3endSpecificityMismatchComboBox;
      private JComboBox mismatchRegionLengthComboBox;
      private JTextField productSizeDeviationField;
    
      public PrimerCheckOptsPanel()
      {
        super();
        init();
      }
      
      public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts)
      {
        opts.setSearchSpecificPrimer(searchSpecificPrimerCheckbox.isSelected());
        if (organismField.getText().length() > 0) {
          opts.setOrganism(organismField.getText());
        }
        opts.setPrimerSpecificityDatabase((RemotePrimerBlastNCBI.PrimerBlastOptions.Database)databaseComboBox.getSelectedItem());
        opts.setTotalPrimerSpecificityMismatch((Integer)totalPrimerSpecificityMismatchComboBox.getSelectedItem());
        opts.setPrimer3endSpecificityMismatch((Integer)primer3endSpecificityMismatchComboBox.getSelectedItem());
        opts.setMismatchRegionLength((Integer)mismatchRegionLengthComboBox.getSelectedItem());
        if (productSizeDeviationField.getText().length() > 0) {
          opts.setProductSizeDeviation(Integer.parseInt(productSizeDeviationField.getText()));
        }
      }
      
      private void init()
      {
        setLayout(new GridBagLayout());
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);
        
        add(new JLabel("Specificity check"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        searchSpecificPrimerCheckbox = new JCheckBox("Enable search for primer pairs specific to the intended PCR template", true);
        add(searchSpecificPrimerCheckbox, c);
        
        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Organism"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        organismField = new JTextField(CurationManager.getActiveCurationState().getCurationSet().getOrganism(), 20);
        add(organismField, c);
        
        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Database"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        ++c.gridx;
        databaseComboBox = new JComboBox(RemotePrimerBlastNCBI.PrimerBlastOptions.Database.values());
        add(databaseComboBox, c);
        
        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Primer specificity stringency"), c);
        ++c.gridx;
        c.insets.left = 5;
        add(new JLabel("At least"), c);
        c.insets.left = 0;
        ++c.gridx;
        totalPrimerSpecificityMismatchComboBox = new JComboBox(new Integer[] { 1, 2, 3, 4 });
        totalPrimerSpecificityMismatchComboBox.setSelectedIndex(1);
        add(totalPrimerSpecificityMismatchComboBox, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        ++c.gridx;
        add(new JLabel("total mismatches to unintended targets, including"), c);
        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        c.insets.left = 5;
        add(new JLabel("at least"), c);
        c.insets.left = 0;
        ++c.gridx;
        primer3endSpecificityMismatchComboBox = new JComboBox(new Integer[] { 1, 2, 3, 4 });
        primer3endSpecificityMismatchComboBox.setSelectedIndex(1);
        add(primer3endSpecificityMismatchComboBox, c);
        ++c.gridx;
        add(new JLabel("mismatches within the last"), c);
        ++c.gridx;
        mismatchRegionLengthComboBox = new JComboBox(new Integer[] { 1, 2, 3, 4, 5, 6, 7 , 8 , 9 ,10 });
        mismatchRegionLengthComboBox.setSelectedIndex(4);
        add(mismatchRegionLengthComboBox, c);
        ++c.gridx;
        add(new JLabel("bps at the 3' end"), c);
        
        c.gridx = 0;
        ++c.gridy;
        add(new JLabel("Misprimed product size deviation"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        ++c.gridx;
        productSizeDeviationField = new JTextField("1000", 4);
        c.insets.left = 5;
        add(productSizeDeviationField, c);
        c.insets.left = 0;
      }
      
    }
    
    private class PrimerPostProcessPanel extends JPanel
    {
      
      private static final long serialVersionUID = 1l;
      
      private JCheckBox removePairsNotInExonsCheckBox;
      
      public PrimerPostProcessPanel()
      {
        super();
        init();
      }
      
      public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts)
      {
        opts.setRemovePairsNotInExons(removePairsNotInExonsCheckBox.isSelected());
      }
      
      private void init()
      {
        setLayout(new GridBagLayout());
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);
        
        removePairsNotInExonsCheckBox = new JCheckBox("Remove pairs where each primer is not fully contained within distinct exons", fl != null);
        if (fl == null) {
          removePairsNotInExonsCheckBox.setEnabled(false);
        }
        add(removePairsNotInExonsCheckBox, c);
      }
      
    }
    
  }
  
  private class NCBIBlastPane extends AnalysisPane
  {
    
    private static final long serialVersionUID = 1L;
    
    private BlastRunOptsPanel runOptsPanel;
    private BlastPostProcessPanel postProcessPanel;

    public NCBIBlastPane()
    {
      super();
      init();
    }
    
    /* (non-Javadoc)
     * @see apollo.analysis.AnalysisGUI.AnalysisPane#runAnalysis()
     */
    public String runAnalysis() throws Exception
    {
      RemoteBlastNCBI blast = new RemoteBlastNCBI((RemoteBlastNCBI.BlastType)runOptsPanel.getBlastType(),
          getOptions());
      CurationSet cs = CurationManager.getActiveCurationState().getCurationSet();
      String type = blast.runAnalysis(cs, cs.getRefSequence().getSubSequence(start, end), strand, start - 1);
      AnalysisFilter filter = new AnalysisFilter();
      AnalysisInput ai = new AnalysisInput();
      postProcessPanel.setInputs(ai);
      ai.setAnalysisType(type);
      filter.cleanUp(cs, type, ai);
      return type;
    }
    
    public boolean setTier(String type)
    {
      boolean newTier = super.setTier(type);
      if (newTier) {
        PropertyScheme scheme = Config.getPropertyScheme();
        scheme.getFeatureProperty(type).setSizeByScore(true);
      }
      return newTier;
    }
    
    private RemoteBlastNCBI.BlastOptions getOptions()
    {
      RemoteBlastNCBI.BlastOptions opts = runOptsPanel.getBlastOptions();
      //opts.setStart(start);
      //opts.setEnd(end);
      return opts;
    }
    
    private void init()
    {
      runOptsPanel = new BlastRunOptsPanel();
      runOptsPanel.setBorder(BorderFactory.createTitledBorder("Run options"));
      postProcessPanel = new BlastPostProcessPanel(getBackground());
      postProcessPanel.setBorder(BorderFactory.createTitledBorder("Post processing options"));
      
      setLayout(new GridBagLayout());
      GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

      add(runOptsPanel, c);
      
      c.gridx = 0;
      ++c.gridy;
      add(postProcessPanel, c);
    }
    
    private class BlastPostProcessPanel extends apollo.analysis.filter.BlastFilterPanel {

      private static final long serialVersionUID = 1l;
      
      public BlastPostProcessPanel(Color c)
      {
        super(c);
        queryIsGenomic.setSelected(true);
        remove(queryIsGenomic);
      }
    }
    
    private class BlastRunOptsPanel extends JPanel
    {
      
      private static final long serialVersionUID = 1;
      
      private JTextField startField;
      private JTextField endField;
      private JComboBox types;
      private JCheckBox filterLowComplexity;
      private JCheckBox filterHumanRepeats;
      private JCheckBox filterMaskLookup;
      private JTextField gapOpenCost;
      private JTextField gapExtendCost;
      private JTextField numOfHits;
      
      public BlastRunOptsPanel()
      {
        super();
        init();
      }
      
      public BlastOptions getBlastOptions()
      {
        BlastOptions opts = new BlastOptions();
        Style style = Config.getStyle();
        opts.setGeneticCode(style.getGeneticCodeNumber());
        if (filterLowComplexity.isSelected()) {
          opts.setFilterLowComplexity(true);
        }
        if (filterHumanRepeats.isSelected()) {
          opts.setFilterHumanRepeats(true);
        }
        if (filterMaskLookup.isSelected()) {
          opts.setFilterMaskLookup(true);
        }
        opts.setGapOpenCost(Integer.parseInt(gapOpenCost.getText()));
        opts.setGapExtendCost(Integer.parseInt(gapExtendCost.getText()));
        opts.setNumberOfHits(Integer.parseInt(numOfHits.getText()));
        start = Integer.parseInt(startField.getText());
        end = Integer.parseInt(endField.getText());
        return opts;
      }
      
      public Object getBlastType()
      {
        return types.getSelectedItem();
      }
      
      private void init()
      {
        setLayout(new GridBagLayout());
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);
        
        JLabel spacer1 = new JLabel();
        spacer1.setPreferredSize(new JCheckBox().getPreferredSize());
        add(spacer1, c);

        gapOpenCost = new JTextField(6);
        gapExtendCost = new JTextField(6);
        numOfHits = new JTextField("100", 6);

        types = new JComboBox(RemoteBlastNCBI.BlastType.values());
        types.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            if (e.getItem() == RemoteBlastNCBI.BlastType.blastn) {
              gapOpenCost.setText("5");
              gapExtendCost.setText("2");
            }
            else {
              gapOpenCost.setText("11");
              gapExtendCost.setText("1");
            }
          }
        });
        types.setSelectedIndex(1);
        types.setSelectedIndex(0);

        startField = new JTextField(Integer.toString(start), 6);
        endField = new JTextField(Integer.toString(end), 6);
        
        c.gridwidth = 1;
        c.gridx = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Start"), c);
        ++c.gridx;
        add(new JLabel("End"), c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Genomic region"), c);
        ++c.gridx;
        add(startField, c);
        ++c.gridx;
        add(endField, c);

        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("BLAST type"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        add(types, c);

        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        JLabel gapOpenLabel = new JLabel("Gap open cost");
        gapOpenLabel.setPreferredSize(new JLabel("Split hits that are tandemly duplicated on subject into separate hits").getPreferredSize());
        add(gapOpenLabel, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(gapOpenCost, c);
        
        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Gap extend cost"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(gapExtendCost, c);

        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Number of hits"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(numOfHits, c);
        
        filterLowComplexity = new JCheckBox();
        filterHumanRepeats = new JCheckBox();
        filterMaskLookup = new JCheckBox();
        
        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterLowComplexity, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out low complexity sequence"), c);

        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterHumanRepeats, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out human repeats"), c);

        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterMaskLookup, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out masked sequence"), c);
        
      }
    }
  
  }
  
}
