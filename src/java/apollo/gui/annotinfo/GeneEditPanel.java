package apollo.gui.annotinfo;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.*;
import java.io.IOException;

import junit.framework.TestCase;

import apollo.datamodel.*;
import apollo.editor.AddTransaction;
import apollo.editor.DeleteTransaction;
import apollo.editor.AnnotationCompoundEvent;
import apollo.editor.AnnotationEditor;
import apollo.editor.AnnotationUpdateEvent;
import apollo.editor.ChangeList;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionSubpart;
import apollo.editor.TransactionUtil;
import apollo.editor.UpdateTransaction;
import apollo.editor.UserName;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PeptideStatus;
import apollo.gui.event.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.SeqFeatureUtil;
import apollo.util.HTMLUtil;
import apollo.util.GuiUtil;
import apollo.dataadapter.chadoxml.ChadoXmlWrite;

import org.apache.log4j.*;

/**
 * GeneEditPanel
 *
 * Panel to layout comments, gene and transcript names, etc
 *
 * Editing is disabled in browser mode. In browser mode just used 
 * to view the comments.
 */
public class GeneEditPanel extends FeatureEditPanel {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GeneEditPanel.class);

  /** There is only one row of data, which is 32 high on Linux, but on Mac,
      needs more room. */
  private static final Dimension tableSize = new Dimension(250,80);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private TranscriptEditPanel transcriptPanel;      // transcript editor

  private ReadWriteField featureTypeList;
  /** bigCommentField is where all the comments from gene and trans 
      are displayed, not edited */
  JTextArea bigCommentField = new JTextArea(12,3);
  //  private JLabel          peptideStatusLabel = new JLabel();
  private ReadWriteField  peptideStatus;
  JTextArea sequencingComments = new JTextArea();

  private JCheckBox      isDicistronicCheckbox = new JCheckBox();

  // Kludge to deal with problem where selecting a row in the table triggers
  // the event TWICE.
  private int table_row_selected = -1;

  /** Vector of feature properties for type field, not used for readOnly */
  private Vector annotFeatureProps;

  private DbxRefTableModel dbXRefModel; // dbxref table model

  Box dataBox = new Box(BoxLayout.Y_AXIS);
  JButton addAnnotComment;
  JButton addTransComment;
  private Commentator commentator;

  private String GO_URL = "http://godatabase.org/cgi-bin/go.cgi?view=details&search_constraint=terms&depth=0&query=";  // Should go in style

  private boolean warnedAboutEditingIDs = false;

  private IDFocusListener idFocusListener; // inner class for id edits
  private GeneNameListener geneNameListener; // inner class for name edits

  private JTable dbxrefTable;
  
  GeneEditPanel(FeatureEditorDialog featureEditorDialog,
                boolean isReadOnly) {
    super (featureEditorDialog, isReadOnly);
    if (isReadOnly) {
      featureTypeList = new ReadWriteField(false);
      peptideStatus = new ReadWriteField(false);
    } 
    else {
      featureTypeList = new ReadWriteField(true);
      peptideStatus = new ReadWriteField(true);
    }
    /* This is a vector of FeatureProperty (not Strings)
       that includes all the valid annotation types */
    this.annotFeatureProps = getAnnotationFeatureProps();
    jbInit();
    attachListeners();
  }

  protected void jbInit() {
    super.jbInit ();

    // ids include "DEBUG" to indicate that Apollo is in debug mode
    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)w
    if (Config.getStyle().showIdField() || Config.DEBUG) {
      String dbg = Config.DEBUG && !Config.getStyle().showIdField() ? "(DEBUG)" : "";
      addField ("ID"+dbg, featureIDField.getComponent());
    }
    addSynonymGui();
    addIsProblematicCheckbox();

    addField("Type",featureTypeList.getComponent());
    featureTypeList.setListBackground(bgColor);
    loadTypes();  // populate featureTypeList and add listener

    if (Config.getStyle().showIsDicistronicCheckbox()) {
      addField("Is dicistronic?", isDicistronicCheckbox);
      isDicistronicCheckbox.setEnabled(!isReadOnly);
      isDicistronicCheckbox.setBackground(bgColor);
      isDicistronicCheckbox.addActionListener(new DicistronicButtonListener());
    }

    if (Config.getStyle().showEvalOfPeptide()) {
      addField("Evaluation of peptide", peptideStatus.getComponent());
      peptideStatus.setListBackground(bgColor);
    }

    // pad out bottom of fields panel, for layout - otherwise syns will consume
    super.addFieldsPanelBottomGlue();

    // a box for all the gene fields and the dbxref table
    Box dataBox = new Box(BoxLayout.Y_AXIS);
    dataBox.add(fieldsPanel);
    dataBox.add(Box.createVerticalStrut(5));

    if (showDbXRefs()) {
      JPanel dbxrefPanel = new JPanel();
      dbxrefPanel.setBackground(bgColor);
      dbxrefPanel.add(getDbTablePane());
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new GridBagLayout());
      buttonPanel.setBackground(bgColor);
      final JButton newDbxref = new JButton("add");
      //final JButton editDbxref = new JButton("edit");
      final JButton deleteDbxref = new JButton("delete");
      
      ActionListener al = new ActionListener() {
        public void actionPerformed(ActionEvent ae)
        {
          JFrame parentFrame = (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, GeneEditPanel.this);
          if (ae.getSource() == newDbxref) {
            DbXref dbxref = new DbXref("id", "", "");
            new DbxrefDialog(parentFrame, "Add DbXref", dbxref);
            dbXRefModel.addDbxRef(dbxref);
          }
          /*
          else if (ae.getSource() == editDbxref) {
            int idx = dbxrefTable.getSelectedRow();
            if (idx >= 0) {
              DbXref newDbxref = dbXRefModel.getDbxrefAt(idx);
              DbXref oldDbxref = new DbXref(newDbxref);
              new DbxrefDialog(parentFrame,
                  "Edit DbXref", newDbxref);
              dbXRefModel.updateDbxRef(oldDbxref, newDbxref);
            }
          }
          */
          else if (ae.getSource() == deleteDbxref) {
            int idx = dbxrefTable.getSelectedRow();
            if (idx >= 0) {
              DbXref dbxref = dbXRefModel.getDbxrefAt(idx);
              dbXRefModel.deleteDbxRef(dbxref);
            }
          }
        }
      };
      
      newDbxref.addActionListener(al);
      //editDbxref.addActionListener(al);
      deleteDbxref.addActionListener(al);
      
      setupSynButton(newDbxref);
      //setupSynButton(editDbxref);
      setupSynButton(deleteDbxref);
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      buttonPanel.add(newDbxref, c);
      //c.gridy = 1;
      //buttonPanel.add(editDbxref, c);
      c.gridy = 2;
      buttonPanel.add(deleteDbxref, c);
      dbxrefPanel.add(buttonPanel);
      
      /*
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridheight = 1;
      c.gridx = 1;
      dbxrefPanel.add(newDbxref, c);
      c.gridy = 1;
      dbxrefPanel.add(editDbxref, c);
      c.gridy = 2;
      //dbxrefPanel.add(deleteDbxref, c);
      */
      dataBox.add(dbxrefPanel);
    }

    Box topBox = new Box(BoxLayout.X_AXIS);
    topBox.add(dataBox);
    topBox.add(Box.createHorizontalStrut(12));
    transcriptPanel = new TranscriptEditPanel(featureEditorDialog, isReadOnly);
    topBox.add(transcriptPanel);

    Box bottomBox = getCommentBox();

    featureBox.add(topBox);
    // Add some space between data panel and big comment field
    featureBox.add(Box.createVerticalStrut(12));
    featureBox.add(bottomBox);

    border = new TitledBorder("Annotation");
    border.setTitleColor (Color.black);
    featureBox.setBorder(border);

  }

  private boolean showDbXRefs() {
    return Config.getStyle().showDbXRefTable();
  }

  private class DicistronicButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean b = isDicistronicCheckbox.isSelected();
      setBooleanSubpart(b,TransactionSubpart.IS_DICISTRONIC);
    }
  }    

  private JScrollPane getDbTablePane() {
    dbXRefModel = new DbxRefTableModel();
    /* table needs to be declared final so it can be accessed 
       from inside ListSelectionListener */
    dbxrefTable = new JTable(dbXRefModel);
    dbxrefTable.getTableHeader().setReorderingAllowed(false);
    dbxrefTable.getColumnModel().getColumn(0).setHeaderValue("ID Value");
    dbxrefTable.getColumnModel().getColumn(1).setHeaderValue("DB Name");
    dbxrefTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    dbxrefTable.getSelectionModel().addListSelectionListener
      (new ListSelectionListener() {
          /* For some reason, this is triggered TWICE whenever 
             you select something in the table. */
          public void valueChanged(ListSelectionEvent e) {
            int row_index = dbxrefTable.getSelectedRow();

            /* Don't do anything if the same row is selected twice in a row
               (which always happens because for some reason valueChanged 
               is triggered twice for every selection) */
            if (row_index == table_row_selected)
              return;

            table_row_selected = row_index;
            String id = (String) dbXRefModel.getValueAt(row_index, 0);
            String dbname = (String) dbXRefModel.getValueAt(row_index, 1);
            if (id.startsWith("FBgn") ||
                id.startsWith("GO:") ||
                id.startsWith("FBti")) {
              String url = "";
              if (id.startsWith("FB")) {
                if (apollo.config.Config.getExternalRefURL()==null)
                  return;
                url = apollo.config.Config.getExternalRefURL() + id;
              }
              else if (id.startsWith("GO:"))
                url = GO_URL + id;

              HTMLUtil.loadIntoBrowser(url);
            } 
            else {
              logger.warn("Don't know how to pull up info for id " + id + ", db = " + dbname);
            }
          }
        }
       );

    JScrollPane tableHolder = new JScrollPane(dbxrefTable);
    // Need to constrain table or else it takes up too much space
    tableHolder.setPreferredSize(GeneEditPanel.tableSize);
    tableHolder.setMaximumSize(GeneEditPanel.tableSize);
    tableHolder.setBackground(bgColor);
    return tableHolder;
  }

  private Box getCommentBox() {
    bigCommentField.setWrapStyleWord(true);
    bigCommentField.setLineWrap(true);
    bigCommentField.setEditable(false);
    JScrollPane bigCommentPane 
      = new JScrollPane(bigCommentField,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    bigCommentPane.setPreferredSize(new Dimension(450, 300));


    Box commentBox = new Box(BoxLayout.X_AXIS);
    commentBox.add(Box.createHorizontalStrut(12));
    commentBox.add(bigCommentPane);
    commentBox.add(Box.createHorizontalStrut(8));

    Box buttonBox = new Box(BoxLayout.Y_AXIS);
    buttonBox.add(Box.createVerticalStrut(20));
    if (!isReadOnly) {
      commentator = new Commentator(featureEditorDialog);
      commentator.setVisible(false);
      addAnnotComment = initCommentButton();
      addTransComment = initCommentButton();
      buttonBox.add(addAnnotComment);
      buttonBox.add(Box.createVerticalStrut(20));
      buttonBox.add(addTransComment);
      buttonBox.add(Box.createVerticalStrut(20));
    }

    if (Config.getStyle().seqErrorEditingIsEnabled()) {
      sequencingComments.setWrapStyleWord(true);
      sequencingComments.setLineWrap(true);
      sequencingComments.setEditable(false);
      sequencingComments.setPreferredSize(new Dimension(250, 160));
      sequencingComments.setBackground(new Color(255, 250, 205));
      JScrollPane seqPane 
        = new JScrollPane(sequencingComments,
                          JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      Border border = new TitledBorder("Genomic sequencing errors");
      seqPane.setBorder(border);
      seqPane.setBackground(bgColor);
      seqPane.setPreferredSize(new Dimension(250, 160));
      buttonBox.add(seqPane);
    }

    commentBox.add(buttonBox);
    commentBox.add(Box.createHorizontalGlue());
    commentBox.setBorder(new TitledBorder("Comments and properties"));
    return commentBox;
  }

  private JButton initCommentButton() {
    JButton addComment = new JButton();
    addComment.setBackground (Color.white);
    Dimension buttonSize = new Dimension(250, 40);
    addComment.setMaximumSize(buttonSize);
    addComment.setMinimumSize(buttonSize);
    addComment.setPreferredSize(buttonSize);
    addComment.setText("Edit comments"); // this gets replaced
    addComment.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editComments(e);
        }
      }
                                 );
    return addComment;
  }

  void clearFields() {
    super.clearFields();
    isDicistronicCheckbox.setSelected(false);
  }

  protected void disposeCommentator() {
    if (!isReadOnly)
      commentator.dispose();
  }


  /** Overrides FeatureEditPanel.loadSelectedFeature */
  protected void loadSelectedFeature() {

    if (getEditedFeature() == null) {
      clearFields();
      return;
    }
    
    isGoodUser();
    setGuiNameFromModel();
    getNameField().setEditable(goodUser);
    
    setGuiIDFromModel();
    if(idIsEditable())
      featureIDField.setEditable(goodUser);
    else
      featureIDField.setEditable(idIsEditable());
      
    loadSynonymGui();
    isProblematicCheckbox.setSelected(getEditedFeature().isProblematic());
    isProblematicCheckbox.setEnabled(goodUser);
    
    /* This is to repair any differences that are solely a result
       of case differences */
    FeatureProperty fp = getFeatureProperty();

    featureTypeList.setValue(fp.getDisplayType());
    featureTypeList.setEnabled(goodUser);
    
    refreshCommentField(getEditedFeature());

    if (getEditedFeature().isProteinCodingGene()) {
      StringBuffer buf = new StringBuffer();
      HashMap adjustments = getEditedFeature().getGenomicErrors();
      if (adjustments != null) {
        Iterator positions = adjustments.keySet().iterator();
        while (positions.hasNext()) {
          String position = (String) positions.next();
          SequenceEdit seq_edit = (SequenceEdit) adjustments.get(position);
          String more_info = (seq_edit.getResidue() != null ? 
                              " of " + seq_edit.getResidue() : "");
          if (seq_edit.getEditType().equals(SequenceI.SUBSTITUTION)) {
            // also say what is being replaced
            SequenceI refSeq = getEditedFeature().getRefSequence();
            if (refSeq != null &&
                refSeq.isSequenceAvailable(seq_edit.getPosition())) {
              char base = refSeq.getBaseAt(seq_edit.getPosition());
              more_info = more_info + " for " + base;
            }
          }
          buf.append(seq_edit.getEditType() + more_info +
                     " at base " + position + "\n");
        }
      }
      sequencingComments.setText(buf.toString());
      sequencingComments.setVisible(true);
    }
    else
      sequencingComments.setVisible(false);

    if (showDbXRefs()) {
      dbXRefModel.setListData(collectDbXrefs());
    }

    setPeptideStatus (getEditedFeature());

    if (getEditedFeature().isProteinCodingGene()) {
      if (getEditedFeature().getProperty("dicistronic").equals("true"))
        isDicistronicCheckbox.setSelected(true);
      else
        isDicistronicCheckbox.setSelected(false);
    }
    isDicistronicCheckbox.setVisible(getEditedFeature().isProteinCodingGene());
    border.setTitle(getEditedFeature().getFeatureType() + " " + getEditedFeature().getName());

    loadTranscript();
    setWindowTitleWithFeatureName();
    updateUI();
  }

  private FeatureProperty getFeatureProperty() {
    return Config.getPropertyScheme().getFeatureProperty(getEditedFeature().getFeatureType());
  }

  // No longer used, it looks like
  //  private UpdateTransaction makeUpdateTransaction(TransactionSubpart ts) {
  //    UpdateTransaction u = new UpdateTransaction(getEditedFeature(),ts);
  //    u.setSource(getFeatureEditorDialog());
  //    return u;
  //  }

//   /** Set model name to gui name, return name UpdateTransaction */
//   private UpdateTransaction commitGeneNameChange() {
//     String guiName = getGuiName();
//     String oldName = getEditedFeature().getName();
//     UpdateTransaction ut = makeUpdateTransaction(TransactionSubpart.NAME);
//     ut.setPreValue(oldName);
//     ut.setSubpartValue(guiName);
//     ut.editModel();
//     return ut;
//   }
  private CompoundTransaction commitGeneAndTranscriptNameChange() {
    if (!nameHasChanged())
      return null; 
    String newName = getGuiName();
    logger.debug("GeneEditPanel changing name from "+getModelName()+" to "+getGuiName());
    CompoundTransaction ct = getNameAdapter().setAnnotName(getEditedFeature(),newName);
//     CompoundTransaction compTrans = new CompoundTransaction();
//     // should their ba a nameAdapter.setGeneName that does everything? probably
//     compTrans.addTransaction(commitGeneNameChange());
//     compTrans.addTransaction(updateTranscriptNames());
    ct.setSource(getFeatureEditorDialog());
    setWindowTitleWithFeatureName();
    //loadSelectedFeature(); // cant do here - screws up id change
    //transcriptPanel.loadSelectedFeature();
    return ct;
    // Would like to reset list of genes as well as window title
    // Problem is that we don't really have access to that list to
    // stick the new changed gene in there.
    //      featureEditorDialog.loadGUI(getEditedFeature()); // ?
  }

  // If annotation name changes, change the transcript names to match.
//   private CompoundTransaction updateTranscriptNames() {
//     AnnotatedFeatureI annot = getEditedFeature();
//     //ChangeList changes = createChangeList();
//     CompoundTransaction compTrans = new CompoundTransaction();
//     for(int i=0; i < annot.size(); i++) {
//       AnnotatedFeatureI trans = annot.getFeatureAt(i).getAnnotatedFeature();
//       // Set transcript name based on parent's name
//       Transaction t = getNameAdapter().setTranscriptNameFromAnnot(trans, annot);
//       compTrans.addTransaction(t);
//     }
//     // Need to force the Transcript name field in TranscriptPanel to update
//     transcriptPanel.loadSelectedFeature();
//     //updateFeature(); // i dont think this is needed anymore //return changes;
//     return compTrans;
//   }

  private void setWindowTitleWithFeatureName() {
    String title = getEditedFeature().getName() + " Annotation Information";
    getFeatureEditorDialog().setTitle(title);
  }

  private boolean idIsEditable() {
    // if the id is not displayed theres no way to edit it
    // super user always returns true at this point (see Style.isSuperUser)
    return Config.getStyle().showIdField() 
      && Config.getStyle().isSuperUser(UserName.getUserName());
  }

  /** sets id in annot feat from gui, returns update id event (shouldnt this also do
      transcript ids? where does that happen? */
  //private AnnotationCompoundEvent commitIdChange() {
  private CompoundTransaction commitIdChange() {
    //AnnotationUpdateEvent aue = makeUpdateEvent(TransactionSubpart.ID);
    //aue.setOldString(getEditedFeature().getId());
    //aue.setOldId(getEditedFeature().getId());
    //getEditedFeature().setId(getGuiId());
    // eventually should return a ChangeList
    //getNameAdapter().setId(getEditedFeature(),getGuiId());
    // fly name adapter sets all transcript ids as well

    //// --- TransactionUtil.setId(getEditedFeature(),getGuiId(),getNameAdapter());

    CompoundTransaction ct = getNameAdapter().setAnnotId(getEditedFeature(),getGuiId());
    ct.setSource(getFeatureEditorDialog());
    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
    if (Config.DEBUG) transcriptPanel.loadSelectedFeature(); // debug trans id
    //AnnotationCompoundEvent ae = new AnnotationCompoundEvent(ct); //return ae; 
    return ct;
  }

  private String getGuiId() { return featureIDField.getValue().trim(); }

  private void setGuiIDFromModel() {
    setGuiId(getEditedFeature().getId());
  }
  
  private void setGuiId(String id) {
    featureIDField.setValue(id);
  }

  // Should we be sorting these in alphabetical order or something?
  private Vector collectDbXrefs() {
    Vector db_xrefs = new Vector();
    db_xrefs.addAll(getEditedFeature().getDbXrefs());
    for(int i=0; i < getEditedFeature().size(); i++) {
      Transcript t = (Transcript) getEditedFeature().getFeatureAt(i);
      db_xrefs.addAll(t.getDbXrefs());
      // Chado XML has dbxrefs for peptides--collect these, too.
      //SequenceI pep = t.getPeptideSequence();
      AnnotatedFeatureI pep = t.getProteinFeat();
      if (pep != null)
        db_xrefs.addAll(pep.getDbXrefs());
    }
    return db_xrefs;
  }

  /**  Return true if id has changed */
  private boolean idChanged(String new_id, String current_id) {
    return (new_id != null &&
            !new_id.equals("") &&
            !new_id.equals(current_id));
  }
  
  /**
   * provides a list of valid annotation types (as feature properties)
   */
  protected static Vector getAnnotationFeatureProps() {
    return Config.getPropertyScheme().getAnnotationFeatureProps();
  }

  protected FeatureProperty findFeatureProp(String typeName) {
    Iterator props = annotFeatureProps.iterator();
    FeatureProperty prop = null;
    while (props.hasNext()) {
      prop = (FeatureProperty)props.next();
      String t = prop.getDisplayType();
      if (t.equals(typeName))
        return prop;
    }
    return null;
  }

  // loads typeList (annotation types--gene, tRNA, etc.) into pulldown list
  void loadTypes() {
    Iterator props = annotFeatureProps.iterator();
    int numTypes = 0;
    while (props.hasNext()) {
      featureTypeList.addItem(((FeatureProperty)props.next()).getDisplayType());
      ++numTypes;
    }
    featureTypeList.setMaximumRowCount(numTypes);

    // We only want to react to action events AFTER populating the list
    if (!isReadOnly) {
      FeatureTypeItemListener ftil = new FeatureTypeItemListener();
      ((JComboBox)featureTypeList.getComponent()).addItemListener(ftil);
    }
  }

  void refreshCommentField() {
    refreshCommentField(getEditedFeature());
  }

  private void refreshCommentField(AnnotatedFeatureI editedFeature) {
    // update clone with latest comments
    if (!isReadOnly) {
      String shortName = apollo.gui.DetailInfo.getName(editedFeature);
      addAnnotComment.setText("Edit " + shortName + " comments");
    }
    bigCommentField.setText(getCommentString(editedFeature));
    bigCommentField.setCaretPosition(0);
  }

  private String getCommentString(AnnotatedFeatureI sf) {
    StringBuffer commentList = new StringBuffer();
    String desc = sf.getDescription();
    if (desc != null && !desc.equals("")) {
      commentList.append(sf.getName() + " description:\n");
      commentList.append(desc + "\n");
    }
    Vector comments = sf.getComments();
    int cnum = 0;
    for(int i=0; i < comments.size(); i++) {
      Comment comment = (Comment) comments.elementAt(i);
      if ((!comment.isInternal() || apollo.config.Config.internalMode()) 
          && !comment.getText().equals("")) {
        if (++cnum == 1)
          commentList.append(sf.getName() + " comments:\n");
        commentList.append("        "  + (cnum) + ". " +
                           comment.getText()+"\n");
      }
    }
    // Add desired properties for this annotation and transcripts
    commentList.append(showProperties(sf));

    // Go through transcripts (but not exons!) in alphabetical order
    if (!(sf instanceof Transcript) && sf.canHaveChildren() && sf.size() > 0) {
      FeatureSetI fs = (FeatureSetI)sf;
      Vector sorted_set
        = SeqFeatureUtil.sortFeaturesAlphabetically(fs.getFeatures());

      for(int i=0; i < sorted_set.size(); i++) {
        AnnotatedFeatureI t = (AnnotatedFeatureI)sorted_set.elementAt(i);
        commentList.append(getCommentString(t));
      }
    }

    return commentList.toString();
  }

  /** For the specified SeqFeature, find all properties of interest,
   *  label with the appropriate label, and return a string buffer
   *  that can be appended to the string buffer that holds the
   *  comments for that transcript. */

  private StringBuffer showProperties(SeqFeatureI sf) {
    Hashtable properties = sf.getPropertiesMulti();
    StringBuffer propList = new StringBuffer();
    boolean first = true;
    Enumeration e = properties.keys();
    boolean hasProperties = false;

    // For one-level annots, put owner here IF we're in internal mode
    if (getFeatureEditorDialog().getTranscript() == null &&
        apollo.config.Config.internalMode()) {
      propList.append(sf.getName() + " properties:\n");
      first = false;
      propList.append("        " + "Author: " + ((AnnotatedFeature)sf).getOwner());
      propList.append("\n");
    }
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String label = key.toUpperCase();
      if (label.equalsIgnoreCase("DATE"))
        label = "LAST MODIFIED ON";
      if (!key.equalsIgnoreCase("sp_status") &&
          (!key.equalsIgnoreCase("internal_synonym") ||
           apollo.config.Config.internalMode()) &&
          !key.equalsIgnoreCase("encoded_symbol") &&
          !key.equalsIgnoreCase("problem") &&
          !key.equalsIgnoreCase("status") &&
          !key.equalsIgnoreCase("dicistronic") &&
          !key.equalsIgnoreCase("symbol") &&
          // If data was read from Chado XML, there may be boring properties
          // that we don't want to show.
          !ChadoXmlWrite.isSpecialProperty(key)) {
        if (first) {
          propList.append(sf.getName() + " properties:\n");
          first = false;
        }
        Vector values = (Vector) properties.get(key);
        if (values != null && values.size() > 0) {
          hasProperties = true;
          propList.append("        " + label + ": ");
          for (int i = 0; i < values.size(); i++) {
            if (i > 0)
              propList.append(", ");
            String value = (String) values.elementAt(i);
            if (value.startsWith(apollo.dataadapter.chadoxml.ChadoXmlAdapter.FIELD_LABEL))
              value = value.substring((apollo.dataadapter.chadoxml.ChadoXmlAdapter.FIELD_LABEL).length());
            propList.append(value);
          }
          propList.append("\n");
        }
//       } else if (key.equalsIgnoreCase("internal_synonym") ||
//                  key.equals("encoded_symbol")) {
//         // Whoa, do we really want to REMOVE the internal_synonym?
//         // This isn't going to happen, since we deal with it in the "if" above.
//         sf.removeProperty(key);
      }
    }
    if (hasProperties)
      propList.append("\n");
    return propList;
  }

  /** Was called setGene, but that was confusing, because all it does 
      is set the peptide status. */
  private void setPeptideStatus (AnnotatedFeatureI g) {
    peptideStatus.removeAllItems ();

    if (!g.isProteinCodingGene()) {
      peptideStatus.addItem ("NOT A PEPTIDE");
      peptideStatus.setValue ("NOT A PEPTIDE");
    } 
    else {
      Hashtable curator_values = Config.getPeptideStates();

      String pep_status = g.getProperty ("sp_status");
      PeptideStatus this_status = Config.getPeptideStatus (pep_status);

      /* these are the computed values that cannot be over-ridden
         by a curator
      */
      peptideStatus.addItem (this_status.getText());

      Enumeration e = curator_values.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        PeptideStatus a_status = (PeptideStatus) curator_values.get (key);
        if (a_status.getPrecedence() >= this_status.getPrecedence() &&
            !a_status.getText().equals (this_status.getText()) &&
            a_status.getCurated()) {
          peptideStatus.addItem (a_status.getText());
        }
      }
      peptideStatus.setValue (this_status.getText());
    }
  }

  /** Called by GeneEditPanel.loadSelectedFeature */
  private void loadTranscript () {
    Transcript trans = getFeatureEditorDialog().getTranscript();
    // if trans is null then we have hit a 1 level annot and disable trans
    if (trans == null) {
      //if (Config.DO_ONE_LEVEL_ANNOTS)
      enableTranscripts(false);
      //else logger.error("loadTranscript: transcript is null");
      return;
    }
    //transcriptPanel.setOriginalFeature(realTrans);
    // Don't just use the first transcript!  Use the one user selected.
    //Transcript t = featureEditorDialog.getTranscriptClone();
    // Load even if it's not a protein coding gene, in order to get any
    // name changes.  If it's not protein coding, we won't show the transcript panel.
    transcriptPanel.loadAnnotation(trans);  // hmmmmm...
    // 02/23/2004: Sima has declared that all annotations (except miscellaneous curator's
    // observations) have transcripts. // should check numLevels == 1
    //    if (trans.isProteinCodingGene()) {
    // 11/17/2005: Wow, do we really want "miscellaneous curator's observation" hardcoded
    // here, when there are now so many types of one-level annots??
    if (!trans.getRefFeature().getTopLevelType().equalsIgnoreCase("miscellaneous curator's observation")) {
      enableTranscripts(true);
      if (!isReadOnly) {
        String shortName = apollo.gui.DetailInfo.getName(trans);
        addTransComment.setText("Edit " + shortName + " comments");
      }
    }
    // Annot with no transcripts/kids (right now, only miscellaneous curator's observations)
    else {
      // nullifies transcript clone - otherwise new annot would take on 
      // someone elses transcript
      enableTranscripts(false);
    }
  }

  private void enableTranscripts(boolean enable) {
    if (!isReadOnly) {
      addTransComment.setEnabled(enable && !isReadOnly);
      addTransComment.setVisible(enable && !isReadOnly);
    }
    transcriptPanel.setVisible(enable);
  }
  
  /** Pull up comment editor */
  private void editComments(ActionEvent e) {

    // before we wipe out the old commentator it has to commit any edits that are hanging
    commentator.commitCurrentComment();

    JButton button = (JButton) e.getSource();
    if (button == addAnnotComment) {
      commentator.setFeature(getEditedFeature(), getBackground());
      commentator.setType(commentator.ANNOTATION);
    }
    else {
      // transcript.getSelectedClonedAnnot
      commentator.setFeature(transcriptPanel.getEditedFeature(),
                             transcriptPanel.getBackground());
      commentator.setType(commentator.TRANSCRIPT);
    }
    if (commentator.getState() == Frame.ICONIFIED)
      commentator.setState(Frame.NORMAL);
    commentator.setVisible(true);
  }
  
  // not used anymore
//   private boolean idAndNameHaveSameFormat() {
//     AnnotatedFeatureI af = getEditedFeature();
//     return getNameAdapter().idAndNameHaveSameFormat(af,getGuiId(),getGuiName());
//   }

  /** Returns true if name and id have same format but are unequal */
  private boolean idAndNameInconsistent() {
    // if name and id in same format, should be identical
    AnnotatedFeatureI af = getEditedFeature();
    if (!getNameAdapter().idAndNameHaveSameFormat(af,getGuiId(),getGuiName()))
      return false; // not same format - cant be inconsistent
    return !getGuiId().equals(getGuiName());
  }

  private boolean nameAndIdAreEqual() {
    return getGuiId().equals(getGuiName());
  }


  /** User has changed id and wants name to be id as well. */
  private CompoundTransaction setGuiAndModelName(String newName) {
    // change gui
    setGuiName(newName);
    // commit to model
    CompoundTransaction compTrans = commitGeneAndTranscriptNameChange();
    return compTrans;
  }


  /** Focus driven edits occur in textboxes -> synonyms, names, and ids. If the user
      selects a new annot in annot tree the focus event comes too late AFTER the
      loading of new annot, so this checks for edits before loadAnnotation sets the
      new annotation in place. this just checks syns, gene & trans panels override 
      to check name & ids. */
  protected void checkFocusDrivenEdits() {
    super.checkFocusDrivenEdits(); // synonyms
    if (idIsEditable()) // no need to check if id cant even be edited
      idFocusListener.updateIdFromGui();
    geneNameListener.updateName();
  }
  


  /** Listens for item selected events from featureTypeList. 
   *   1st check that type has actually changed. If so set clones type and biotype.
   *   If protein coding gene set translation start. Also update peptide status 
   *   list for new type.
  *    This was an anonymous class - i felt it was too big for that - confusing.
   *   I probably made this anonymous class to begin with -  I guess im losing 
   *   my enthusiasm for anonymous classes - MG
   *
   *   Also need to possibly change the gene id,name and trans and exon!
  */
  private class FeatureTypeItemListener implements ItemListener {

    public void itemStateChanged(ItemEvent e) {

      if (!(e.getStateChange() == ItemEvent.SELECTED))
        return;  // This is not an event we want to respond to.
      
      changeType();
    }
    
    private void changeType() {

      String oldType = getEditedFeature().getFeatureType();
      FeatureProperty fp = findFeatureProp(featureTypeList.getValue());
      // there can be more than one analysis type??
      // YES and this is problematic! analysis types are analogous to datatypes
      // in the tiers file and there can be many datatypes per FeatureProps which
      // are analogous to a tiers file Type. yikes! so the picking of the first one
      // is somewhat arbitrary but is now an awkward standard. BUT to see if the type
      // has changed we should examine all of the analysis types to see if the 
      // oldType matches it. otherwise if you load a type that is not first in the
      // list this will mistaeknly think that you have changed the type
      // we need SO!!
      String newType = fp.getAnalysisType(0);
//    if (oldType.equals(newType)) return;// if type hasnt changed nothing to do
      for (int i=0; i<fp.getAnalysisTypesSize(); i++) {
        if (oldType.equals(fp.getAnalysisType(i)))
          return; // if type hasnt changed nothing to do
      }

      // This is a multi-event! change type & possibly change translation
      // & possibly change ids and names

      TransactionSubpart ts = TransactionSubpart.TYPE;
      UpdateTransaction ut = new UpdateTransaction(getEditedFeature(),ts);
      ut.setSource(getFeatureEditorDialog());
      ut.setOldSubpartValue(oldType);
      ut.setNewSubpartValue(newType); // should be setNewValue()

      // ID Change - type may change id - put this in AnnEditor?
      //String newIdPrefix = getNameAdapter().getIDPrefix(getEditedFeature());
      //if (!newIdPrefix.equals(oldIdPrefix)) {
      String oldId = getEditedFeature().getId();
      ut.setOldId(oldId);

      if (getNameAdapter().typeChangeCausesIdChange(oldType,newType)) {
        //StrandedFeatureSetI annots = getCurSet().getAnnots();
        //String cn = getCurSet().getName();
        //String tempId = getNameAdapter().generateId(annots,cn,getEditedFeature());
        String newId = getNameAdapter().getNewIdFromTypeChange(oldId,oldType,newType);
        ut.setNewId(newId);
        // this is a bit funny but the transaction will need the name adapter to do
        // an undo. if we were sending out all the id and name events/transactions
        // this wouldnt be needed - but we arent at the moment (theres no need with
        // type change doing a del-add, but its worth considering adding these events
        // also TransactionManager would have to not do its changing of id events
        // to del-adds (until commit time). for now this will do.
        ut.setNameAdapter(getNameAdapter());

        //AnnotationUpdateEvent ae = getEditor().setID(getEditedFeature(),newId);
        //setIdsAndNamesFromId(newId); // -> Transaction.editModel
        // dont fire event, for now type change does delete and add, might need to
        // be more subtle with an integrated db - but hard with 3 and 1 level annots
        //fireAnnotEvent(ae);
      }
      
      // Have to do type change after id change. need old id to detect if same format
      // as name above.
      
      
      //ut.setFeatureId(oldId);
      // this does type change and id & name change
      ut.editModel(); // trying this out - i think this is the way to go!
      //getEditedFeature().setType(newType);
      //getEditedFeature().setBioType(newType);

      // if protein coding gene (type->gene), set transaction start
      if (getEditedFeature().isProteinCodingGene()) {
        int trans_count = getEditedFeature().size();
        for (int i = 0; i < trans_count; i++) {
          AnnotatedFeatureI transClone 
            = (AnnotatedFeatureI)getEditedFeature().getFeatureAt(i);
          if (transClone.getTranslationStart() == 0)
            transClone.calcTranslationStartForLongestPeptide();
          // send out translation start & stop event
        }
      }

      // for now this is the only event we need - causes delete and add in 
      // transaction manager - may need to be more subtle some day (integrated db)
      //      AnnotatedFeatureI annot = getEditedFeature();
      //      Object source = getFeatureEditorDialog();
      //AnnotationUpdateEvent a = new AnnotationUpdateEvent(source,annot,ts,true);
      // new way to try - 
      AnnotationUpdateEvent a = new AnnotationUpdateEvent(ut);
      //a.setOldId(oldId);
      //a.setOldString(oldType);
      fireAnnotEvent(a);

      // May need to reset the peptide status box
      setPeptideStatus(getEditedFeature());
      loadSelectedFeature();
      //updateFeature(); // why is this needed?
    }

//     private CurationSet getCurSet() { // not used anymore
//       return getCurState().getCurationSet();
//     }
//     private AnnotationEditor getEditor() { // not used anymore
//       return getCurState().getAnnotationEditor(getEditedFeature().isForwardStrand());
//     }
//     private GuiCurationState getCurState() { / not used anymore
//       return getFeatureEditorDialog().getCurationState();
//     }

  } // --- end of FeatureTypeItemListener inner class ---


  private void attachListeners() {
    featureIDField.getComponent().addKeyListener(new IDKeyListener());
    idFocusListener = new IDFocusListener();
    featureIDField.addFocusListener(idFocusListener);
    geneNameListener = new GeneNameListener(getNameField());
  }

  private class IDKeyListener implements KeyListener {
    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {
      if (!warnedAboutEditingIDs) {
        // !! Make this an annoying popup warning
        logger.warn("Warning: editing IDs is considered dangerous!  Proceed at your own risk.");
        warnedAboutEditingIDs = true;
      }
    }
  }



  /** ID FOCUS LISTENER INNER CLASS
      Inner Class for dealing with ID edits - if id has been edited and 
      there is a focus lost from id field than check out the id - if ok 
      change model and fire event - if not revert it back to old id - 
      this is a quickie standin for a real ID gui */
  private class IDFocusListener implements FocusListener {

    boolean idBeingEdited = false;

    public void focusGained(FocusEvent f) {
      idBeingEdited = true;
    }
    public void focusLost(FocusEvent f) {
      updateIdFromGui();
      idBeingEdited = false;
    }

    /** Change annot id from gui if id is acceptable. If not revert gui to model kinda
        funny but without proper ID gui this is the best we can do at the moment */
    private void updateIdFromGui() {

      // checkForFocusDrivenEdits can call this when the gui has not been edited
      // but the id has been edited (split) which is detected here
      if (!idBeingEdited)
        return;

      if (getGuiId().equals(getEditedFeature().getId()))
        return; // if no net changes to id -> return

      if (!idFormatOk()) {
        badIdFormatErrorPopup(); // error msg
        setGuiIDFromModel(); // revert
        return; // done
      }

      CompoundTransaction compTrans = new CompoundTransaction(this);
      
      // If id and name have same format but are not equal bring up dialog asking
      // if user wants to make name the same as id - if not cancel/revert, if
      // so commit both name and id changes
      if (idAndNameInconsistent()) {
        boolean userWantsNameAndIdChange = queryUserOnNameChange();

        if (!userWantsNameAndIdChange) {
          setGuiIDFromModel(); // revert back to model id
          return; // cancel/exit
        }

        // else make name change, id change done below
        CompoundTransaction nameTrans = setGuiAndModelName(getGuiId());
        compTrans.addTransaction(nameTrans);
      }

      // id changes
      CompoundTransaction idTrans = commitIdChange();
      compTrans.addTransaction(idTrans);
      compTrans.setSource(featureEditorDialog);
      AnnotationCompoundEvent ace = new AnnotationCompoundEvent(compTrans);
      fireAnnotEvent(ace);
      loadSelectedFeature();
    }


    /** Brings up dialog. Returns true if user wants both name and id changed. */
    private boolean queryUserOnNameChange() {
      String nm = getEditedFeature().getName();
      String m = "Name "+nm+" and ID "+getGuiId()+" have the same pattern but are"
        +" not the same.\nWould you like to change the name as well or cancel your ID"
        +" edit?";
      String title = "Change both name and ID?";
      int question = JOptionPane.QUESTION_MESSAGE;
      String updateName = "Update name";
      String cancel = "Cancel ID Update";
      Object[] vals = new Object[] {updateName,cancel};
      int opt = JOptionPane.DEFAULT_OPTION;
      int i = JOptionPane.showOptionDialog(null,m,title,opt,question,null,vals,null);
      return i == 0; // 0 is update name
    }

    private boolean idFormatOk() {
      String guiID = getGuiId();
      // Make sure ID is in a legal format
      return getNameAdapter().checkFormat(getEditedFeature(),guiID);
    }

    private void badIdFormatErrorPopup() {
      String err = "Error: annotation ID is not in a valid format: " + getGuiId();
      if (haveIdFormat())
        err += "\nID format should be: " + getIdFormat();
      errorPopup(err);
    }

  
    private boolean haveIdFormat() {
      FeatureProperty fp = getFeatureProperty();
      return fp != null && fp.getIdFormat() != null;
    }
    
    private String getIdFormat() {
      return getFeatureProperty().getIdFormat();
    }
    
  } // --- end of IDFocusListener inner class ---



  /** on focus lost from name field, checks name and commits it if ok.
      commit is modifying model and firing update event. */
  private class GeneNameListener implements FocusListener,// KeyListener, 
    DocumentListener {

    private boolean inFocus = false;
    private boolean documentEdited = false;
    //private boolean gotKeystroke = false;
    private boolean nameCommitted = true;

    private GeneNameListener(ReadWriteField nameField) {
      nameField.addFocusListener(this);
      //nameField.addKeyListener(this);
      nameField.getDocument().addDocumentListener(this);
    }

    private boolean nameBeingEdited() {
      // cant require keystroke as can edit with pasting from mouse w no keystroke
      return inFocus  && documentEdited && !nameCommitted; // && gotKeystroke
    }

    /** FocusListener */
    public void focusGained(FocusEvent e) {
      // replaced with document listener, focus events can be willy nilly
      inFocus = true;
    }

    /** FocusListener */
    public void focusLost(FocusEvent e) {
      updateName();
      resetEditFlags();
    }

    private void resetEditFlags() {
      nameCommitted = true;
      inFocus = false;
      documentEdited = false;
      //gotKeystroke = false;
    }

//     /** KeyListener */
//     public void keyPressed(KeyEvent e) {
//       gotKeystroke = true;
//     }
//     public void keyReleased(KeyEvent e) {
//       gotKeystroke = true;
//     }
//     public void keyTyped(KeyEvent e) {
//       gotKeystroke = true;
//     }

    private void setDocumentEdited() {
      //if (!gotKeystroke) 
      // return;
      documentEdited = true;
      nameCommitted = false;
    }

    public void removeUpdate(DocumentEvent d) {
      setDocumentEdited();
    }
    public void insertUpdate(DocumentEvent d) {
      setDocumentEdited();
    }
    public void changedUpdate(DocumentEvent d) {
      setDocumentEdited();
    }

    /** Check if name is in same format as id. If so and they are not equal,
        reject the edit - if id editing is enabled suggest editing the id. */
    private void updateName() {

      // checkForFocusDrivenEdits can call this when the gui has not been edited
      // but the name has been edited (split) which is detected here
      if (!nameBeingEdited())
        return;

      //if (getGuiName().equals(getModelName())) {
      if (!nameHasChanged()) { // checks for "" which doesnt count as name change
        resetEditFlags();
        return; // no change -> exit
      }

      if(getNameAdapter().checkName(getGuiName(),getEditedFeature().getClass()))
      {
        logger.info("checking for duplicate name");
        String e = "Error: gene name already exists in data store " + getGuiName();
        errorPopup(e);
      	setGuiNameFromModel();
      	resetEditFlags();
      	return;
      }


      if (idAndNameInconsistent()) {
        String e = "Error: annotation symbol " + getGuiName() +
          " is inconsistent with ID " + getGuiId(); 
        if (idIsEditable())
          e += "\nTo do this edit use the ID field.";
        errorPopup(e);
        setGuiNameFromModel();
        resetEditFlags();
        return;
      }

      // name checks out -> commit
      CompoundTransaction compTrans = commitGeneAndTranscriptNameChange();
      AnnotationCompoundEvent ace = new AnnotationCompoundEvent(compTrans);
      fireAnnotEvent(ace);
      loadSelectedFeature();
      resetEditFlags();
    } // end of updateName()

  } // end of NameFocusListener inner class

  void testNameUndo(TestCase testCase) {
    // track original name
    String originalName = getModelName(); // same as gui name at this point
    // trigger editing model with focus lost & gained
    geneNameListener.focusGained(null);
    //geneNameListener.keyTyped(null); // dont do keys - pasting
    geneNameListener.insertUpdate(null);
    // edit gui
    setGuiName("new-name-test");
    geneNameListener.focusLost(null);
    // test that model now reflects gui name
    testCase.assertEquals(getGuiName(),getModelName());
    // now undo the change
    getFeatureEditorDialog().undo();
    // now make sure original name is reinstated
    testCase.assertEquals(originalName,getModelName());
  }
  
  private void initDbXRefPanel()
  {
    JPanel dbxrefPanel = new JPanel();
    //dbxrefPanel.setLayout(new GridBagLayout());
    //GridBagConstraints c = new GridBagConstraints();
    //c.gridheight = GridBagConstraints.REMAINDER;
    //dbxrefPanel.setBackground(bgColor);
    dbxrefPanel.add(getDbTablePane());
    dataBox.add(dbxrefPanel);
  }
  
  private class DbxrefDialog extends JDialog
  {
    private JLabel currentLabel;
    private JCheckBox currentCheckBox;
    private JLabel dbNameLabel;
    private JTextField dbNameField;
    private JLabel descriptionLabel;
    private JTextField descriptionField;
    /*
    private JLabel idTypeLabel;
    private JTextField idTypeField;
    */
    private JLabel idValueLabel;
    private JTextField idValueField;
    private JLabel versionLabel;
    private JTextField versionField;
    private JLabel ontologyLabel;
    private JCheckBox ontologyCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    
    public DbxrefDialog(Frame owner, String title, final DbXref dbxref)
    {
      super(owner, title, true);
      int textFieldLength = 15;
      /*
      idTypeLabel = new JLabel("Id type");
      idTypeField = new JTextField(dbxref.getIdType(), textFieldLength);
      */
      idValueLabel = new JLabel("ID");
      idValueLabel.setForeground(Color.RED);
      idValueField = new JTextField(dbxref.getIdValue(), textFieldLength);
      dbNameLabel = new JLabel("Database");
      dbNameLabel.setForeground(Color.RED);
      dbNameField = new JTextField(dbxref.getDbName(), textFieldLength);
      descriptionLabel = new JLabel("Description");
      descriptionField = new JTextField(dbxref.getDescription(), textFieldLength);
      versionLabel = new JLabel("Version");
      versionField = new JTextField(dbxref.getVersion(), textFieldLength);
      currentLabel = new JLabel("Is current?");
      currentCheckBox = new JCheckBox("", dbxref.getCurrent() > 0);
      ontologyLabel = new JLabel("Is ontology?");
      ontologyCheckBox = new JCheckBox("", dbxref.isOntology());
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(5, 5, 5, 5);
      c.anchor = GridBagConstraints.WEST;
      panel.add(idValueLabel, c);
      c.gridx = 0;
      c.gridx = 1;
      panel.add(idValueField, c);
      c.gridy = 1;
      c.gridx = 0;
      panel.add(dbNameLabel, c);
      c.gridy = 1;
      c.gridx = 1;
      panel.add(dbNameField, c);
      c.gridy = 2;
      c.gridx = 0;
      panel.add(descriptionLabel, c);
      c.gridy = 2;
      c.gridx = 1;
      panel.add(descriptionField, c);
      /*
      c.gridy = 3;
      c.gridx = 0;
      panel.add(idTypeLabel, c);
      c.gridy = 3;
      c.gridx = 1;
      panel.add(idTypeField, c);
      */
      c.gridy = 4;
      c.gridx = 0;
      panel.add(versionLabel, c);
      c.gridy = 4;
      c.gridx = 1;
      panel.add(versionField, c);
      c.gridy = 5;
      c.gridx = 0;
      panel.add(currentLabel, c);
      c.gridy = 5;
      c.gridx = 1;
      panel.add(currentCheckBox, c);
      c.gridy = 6;
      c.gridx = 0;
      panel.add(ontologyLabel, c);
      c.gridy = 6;
      c.gridx = 1;
      panel.add(ontologyCheckBox, c);
      add(panel);
      
      ActionListener okCancelListener = new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          boolean done = true;
          if (ae.getSource() == okButton) {
            String id = idValueField.getText().trim();
            String db = dbNameField.getText().trim();
            String description = descriptionField.getText().trim();
            String version = versionField.getText().trim();
            int current = currentCheckBox.isSelected() ? 1 : 0;
            boolean ontology = ontologyCheckBox.isSelected();
            if (id.length() == 0 || db.length() == 0) {
              done = false;
              JOptionPane.showMessageDialog(DbxrefDialog.this, "Both ID and database must be provided",
                  "Error", JOptionPane.ERROR_MESSAGE);
            }
            dbxref.setIdValue(id);
            dbxref.setDbName(db);
            if (description.length() > 0) {
              dbxref.setDescription(description);
            }
            if (version.length() > 0) {
              dbxref.setVersion(version);
            }
            dbxref.setCurrent(current);
            dbxref.setIsOntology(ontology);
          }
          if (done) {
            dispose();
          }
        }
      };
      
      okButton = new JButton("OK");
      okButton.addActionListener(okCancelListener);
      cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(okCancelListener);
      JPanel okCancelPanel = new JPanel();
      okCancelPanel.add(okButton);
      okCancelPanel.add(cancelButton);
      c.gridy = 10;
      c.gridx = 0;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(okCancelPanel, c);
      
      pack();
      setVisible(true);
    }
  }

  /***
   ** DBXREFTABLEMODEL class
   */
  private class DbxRefTableModel extends AbstractTableModel {

    private Vector data;

    DbxRefTableModel() {
      data = new Vector();
    }

    void setListData(Vector in) {
      data = in;
      fireTableDataChanged();
    }

    public int getRowCount() {
      return data.size();
    }

    public boolean isCellEditable(int r, int c) {
      return false;
    }

    public void setValueAt(Object o, int row, int column) {
      DbXref db = (DbXref) data.elementAt(row);
      if (column == 0)
        db.setIdValue((String) o);
      else
        db.setDbName((String) o);
    }

    public int getColumnCount() {
      return 2;
    }

    public Object getValueAt(int row, int column) {
      DbXref db = (DbXref) data.elementAt(row);
      if (column == 0)
        return db.getIdValue();
      else
        return db.getDbName();
    }
    
    public DbXref getDbxrefAt(int idx)
    {
      return (DbXref)data.get(idx);
    }
    
    public void addDbxRef(DbXref dbxref)
    {
      int idx = data.size();
      data.add(dbxref);
      getEditedFeature().addDbXref(dbxref);
      TransactionSubpart ts = TransactionSubpart.DBXREF;
      int subpartRank = getEditedFeature().getDbXrefs().size()-1;
      AddTransaction at = new AddTransaction(this, getEditedFeature(), ts, dbxref, subpartRank);;
      fireAnnotEvent(at.generateAnnotationChangeEvent());
      fireTableRowsInserted(idx, idx);
    }
    
    public void deleteDbxRef(DbXref dbxref)
    {
      int idx = dbxrefTable.getSelectedRow();
      data.removeElementAt(idx);
      getEditedFeature().getIdentifier().deleteDbXref(dbxref);
      // since the selected row is no longer available, need to reset
      // stored selected index as well
      table_row_selected = -1;
      TransactionSubpart ts = TransactionSubpart.DBXREF;
      int subpartRank = getEditedFeature().getDbXrefs().indexOf(dbxref);
      DeleteTransaction dt = new DeleteTransaction(this, getEditedFeature(), ts, dbxref, subpartRank);;
      fireAnnotEvent(dt.generateAnnotationChangeEvent());
      fireTableRowsDeleted(idx, 0);
    }

    /*
    public void updateDbxRef(DbXref oldDbxref, DbXref newDbxref)
    {
      int idx = dbxrefTable.getSelectedRow();
      CompoundTransaction ct = new CompoundTransaction(this);
      ct.addTransaction(generateDeleteTransaction(oldDbxref));
      ct.addTransaction(generateAddTransaction(newDbxref));
      fireAnnotEvent(new AnnotationCompoundEvent(ct));
      fireTableDataChanged();
    }
    */

    /*
    private AddTransaction generateAddTransaction(DbXref dbxref)
    {
      data.add(dbxref);
      getEditedFeature().addDbXref(dbxref);
      TransactionSubpart ts = TransactionSubpart.DBXREF;
      int subpartRank = getEditedFeature().getDbXrefs().size()-1;
      return new AddTransaction(this, getEditedFeature(), ts, dbxref, subpartRank);
    }
    */

    /*
    private DeleteTransaction generateDeleteTransaction(DbXref dbxref)
    {
      int idx = dbxrefTable.getSelectedRow();
      data.removeElementAt(idx);
      getEditedFeature().getIdentifier().deleteDbXref(dbxref);
      // since the selected row is no longer available, need to reset
      // stored selected index as well
      table_row_selected = -1;
      TransactionSubpart ts = TransactionSubpart.DBXREF;
      int subpartRank = getEditedFeature().getDbXrefs().indexOf(dbxref);
      return new DeleteTransaction(this, getEditedFeature(), ts, dbxref, subpartRank);
    }
    */
    
  } // end of DbxRefTableModel class - make inner class?
  
} // end of GeneEditPanel class
