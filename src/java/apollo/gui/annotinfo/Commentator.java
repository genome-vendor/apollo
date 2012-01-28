package apollo.gui.annotinfo;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.event.*;
import apollo.datamodel.*;
import apollo.editor.AnnotationAddEvent;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.AnnotationDeleteEvent;
import apollo.editor.AnnotationUpdateEvent;
import apollo.editor.TransactionSubpart;
import apollo.editor.UserName;
import java.util.Vector;
import apollo.config.Config;
import apollo.gui.event.*;

import org.apache.log4j.*;

/** Currently Commentator changes are reflected in comments box while editing,
    so the events have catered to this. This could be altered to a commit-cancel
    paradigm, where comment changes are displayed in FED comment box only after
    a cancel, and thats when events are fired too. Not sure best way to go here.

    These are my thoughts on redoing the gui for comments (yes i know we did a
    redo which really helped but I think we need to go one step further)
    In main annot info window have comment list - of comment text not author/date 
    or maybe both but minimally comment text (maybe just 1st 30 chars?) - anyways
    there would be an add,delete and edit buttons. Edit and delete buttons would be
    greyed out unless a comment was selected. If add comment was pressed the
    commentator box would come up (minus the comment list, add button, and delete
    buttons). The comment would then be constructed via the dropdown list and editing.
    and close would be hit - I suppose there could also be a cancel button to cancel
    the add. If a comment was selected in the comment list (in the main window NOT
    the commentator) edit and delete would be enabled (ungreyed). Delete would just
    delete it. Edit would popup the commentator as with add. With comments in this
    list in main window comments could be taken out of "comments" window. Comments
    indow could turn into a "properties" window and show all the other properties
    as it does now.
*/

class Commentator extends JFrame { //implements ListSelectionListener

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Commentator.class);

  private static int panelWidth = 560;

  // For saying whether we're looking at annotation or transcript comments,
  // so we can pull up the appropriate list of canned comments
  public static int ANNOTATION = 1;
  public static int TRANSCRIPT = 2;

  protected static Color bgColor = new Color(255, 240, 210);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Vector cannedComments;

  private JButton addButton = new JButton("Add");
  JButton delButton = new JButton("Delete");
  JButton closeButton = new JButton("Close");

  //  BorderLayout borderLayout = new BorderLayout();
  private JList userCommentListGui = new JList();

  /** Where comment text is displayed and edited */
  private JTextArea commentText = new JTextArea();
  private JCheckBox isInternalCheckBox = new JCheckBox("For internal viewing only");
  private JComboBox cannedCommentListGui;
  /** inner class for listening for text changes in commentText */
  private CommentDocumentListener commentDocumentListener =
    new CommentDocumentListener();

  private FeatureEditorDialog featureEditorDialog;
  private AnnotatedFeatureI annot;
  private Vector no_comment = new Vector();

  /** Whether comment currently being worked on is new (ADD) or old (UPDATE) */
  private boolean currentCommentIsNew = false;
  /** Whether user has added own stuff to comment via keyboard, if so then 
      do update/add on focus listen, rather than update for every key */
  private boolean commentEdited = false;
  
  /** inner class listens for annot change events for refresh */
  private CommentChangeListener commentChangeListener;

  Commentator(FeatureEditorDialog featureEditorDialog) {
    this.featureEditorDialog = featureEditorDialog;
    initGui();
    attachListeners();
    validate();
    repaint();
  }

  
  private Vector synchCommentListWithAnnot() {
    if (annot == null) {
      String m = "Prog error: Commentator has no annot but synchWithAnnot has been "
        +"called";
      throw new NullPointerException(m);
    }
    Vector all_comments = annot.getComments();
    // If we're not internal, remove the internal-
    // only comments from the list to show
    Vector listed_comments;
    if (!apollo.config.Config.internalMode()) {
      listed_comments = new Vector();
      for (int i=0; i < all_comments.size(); i++) {
        Comment c = (Comment) all_comments.elementAt(i);
        if (!c.isInternal())
          listed_comments.addElement(c);
      }
    }
    else 
      listed_comments = all_comments;
    userCommentListGui.setListData(listed_comments);
    return listed_comments;
  }

  private void attachListeners() {
    // fixme: what could go wrong? Is it safe to ignore these?
    // if internal checkbox is checked then fire event - only if there is content
    // in the comment - only if update i guess - not new
    isInternalCheckBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          if (!currentCommentIsNew) {
            commentEdited = true;
            commitCurrentComment();
          }
        } } );
    
    commentText.getDocument().addDocumentListener(commentDocumentListener);
    commentText.addFocusListener(new CommentTextFocusListener());

    userCommentListGui.addListSelectionListener(new UserCommentListSelListener());
    commentChangeListener = new CommentChangeListener();
    featureEditorDialog.getController().addListener(commentChangeListener);
  }

  public void dispose() {
    featureEditorDialog.getController().removeListener(commentChangeListener);
    super.dispose();
  }

  /** Inner class for refreshing commentator on external comment changes, eg undo */
  private class CommentChangeListener implements AnnotationChangeListener {
    public boolean handleAnnotationChangeEvent(AnnotationChangeEvent e) {
      if (!e.isCommentChange() || e.getSource() == featureEditorDialog)
        return false;
      if (annot == null) // this happens if commentator has not been brought up yet
        return false;
      synchCommentListWithAnnot();
      return true;
    }
  }

  private void initButton (JButton button) {
    button.setBackground (Color.white);
    button.setMaximumSize(new Dimension(80, 20));
    button.setMinimumSize(new Dimension(80, 20));
    button.setPreferredSize(new Dimension(80, 20));
  }

  private void initGui() {

    // lets make the buttons first
    initButton (addButton);
    addButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addComment(); 
        } } );

    initButton (delButton);
    delButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          deleteComment();
        }
      }
                                );

    initButton (closeButton);
    closeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          removeEmptyComments();
          commitCurrentComment();
          commentEdited = false;
          userCommentListGui.clearSelection();
          setVisible(false);
        }
      }
                            );

    // and a box to hold all of the buttons, nicely spaced
    Box buttonBox = new Box(BoxLayout.X_AXIS);
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(addButton);
    buttonBox.add(Box.createHorizontalStrut(10));
    buttonBox.add(delButton);
    buttonBox.add(Box.createHorizontalStrut(10));
    buttonBox.add(closeButton);
    buttonBox.add(Box.createHorizontalGlue());
    
    userCommentListGui.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane listScroll = new JScrollPane(userCommentListGui);
    Dimension listDim = new Dimension(panelWidth / 2, 120);
    listScroll.setMaximumSize(listDim);
    listScroll.setMinimumSize(listDim);
    listScroll.setPreferredSize(listDim);

    JLabel help_text = new JLabel();
    help_text.setForeground(Color.black);
    help_text.setFont(new java.awt.Font("Dialog", Font.BOLD, 12));
    String font = "<FONT FACE=Dialog color=black>";
    help_text.setText("<html>" + font +
                      "Select an author/date pair to edit an existing " +
                      "comment, or click 'Add' to create a new comment."+
                      "</font></html>");

    /* this goes to the right of the list box which is 120 pixels
       high. Use these well? */
    Box helpBox = new Box(BoxLayout.Y_AXIS);
    helpBox.add(Box.createVerticalGlue());
    helpBox.add(Box.createVerticalStrut(20));
    helpBox.add(help_text);
    helpBox.add(Box.createVerticalStrut(10));
    helpBox.add(Box.createVerticalGlue());
    
    /* This arranges the comment selection list and the help text
       horizontally */
    Box selBox = new Box(BoxLayout.X_AXIS);
    selBox.add(listScroll);
    selBox.add(Box.createHorizontalStrut(20));
    selBox.add(helpBox);
    selBox.add(Box.createHorizontalStrut(10));
    selBox.add(Box.createHorizontalGlue());

    // We have to put something in the cannedComments vector or else the
    // combo box will be unhappy, but we don't yet know if we want
    // annotation or transcript comments.  They will be filled in later.
    cannedComments = new Vector();
    cannedComments.add("No canned comments defined yet");
    cannedCommentListGui = new JComboBox(cannedComments);
    cannedCommentListGui.setFont(new java.awt.Font("Dialog", 0, 10));
    cannedCommentListGui.setBackground (bgColor);
    cannedCommentListGui.setForeground (Color.black);
    cannedCommentListGui.setMaximumSize(new Dimension(panelWidth, 20));
    // Don't enable canned comment pulldown unless there's
    // at least one comment to edit (it will get enabled later if there is)
    cannedCommentListGui.setEnabled(false);
    cannedCommentListGui.addItemListener(new CannedListItemListener());

    // next the stuff for entering the comment text itself
    commentText.setLineWrap(true);
    commentText.setWrapStyleWord(true);
    commentText.setBorder(null);
    // not editable until a comment is selected
    commentText.setEditable(false);
    JScrollPane textScroll = new JScrollPane(commentText);
    textScroll.setHorizontalScrollBarPolicy(
      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    textScroll.setVerticalScrollBarPolicy(
      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    textScroll.setMinimumSize(new Dimension(panelWidth / 2, 200));
    textScroll.setPreferredSize(new Dimension(panelWidth, 200));
    textScroll.setMaximumSize(new Dimension(panelWidth, 200));

    Container content = getContentPane();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    // I'd like the components not to be jammed against the left edge of
    // the window, but this doesn't seem to help
    //    content.add(Box.createHorizontalStrut(12)); // Doesn't help
    content.add(Box.createVerticalStrut(10));
    content.add(selBox);
    content.add(Box.createVerticalStrut(20));
    content.add(cannedCommentListGui);
    content.add(Box.createVerticalStrut(10));
    content.add(textScroll);
    content.add(Box.createVerticalStrut(10));

    // We only want to add the isInternalCheckBox if we're internal;
    // otherwise outsiders will be making comments internal and then
    // not seeing them show up, because they're not internal!
    isInternalCheckBox.setFont(new java.awt.Font("Dialog", Font.BOLD, 12));
    isInternalCheckBox.setBackground(bgColor);
    isInternalCheckBox.setSelected(false);
    isInternalCheckBox.setEnabled(false);
    // Attempt to left-justify isInternalCheckBox (didn't work)
    isInternalCheckBox.setPreferredSize(new Dimension(panelWidth, 15));
    isInternalCheckBox.setMinimumSize(new Dimension(panelWidth, 15));

    if (apollo.config.Config.internalMode()) {
      // I want this checkbox to be left-justified--not sure how to do that
      //      content.add(Box.createHorizontalGlue());  // ? not helping
      content.add(isInternalCheckBox);
      content.add(Box.createVerticalStrut(20));
    }

    content.add(buttonBox);
    content.add(Box.createVerticalStrut(10));
    content.setBackground(bgColor);

    setSize(new Dimension(panelWidth + 24, 500));
  }


  private void clearFields() {
    // need to remove comment document listener before setting blank text or
    // it will wipe out all the comments
    commentText.getDocument().removeDocumentListener(commentDocumentListener);
    commentText.setText("");
    commentText.getDocument().addDocumentListener(commentDocumentListener);
    if (annot == null)
      userCommentListGui.setListData(no_comment);
    userCommentListGui.clearSelection();
    addButton.setEnabled(false);
    delButton.setEnabled(false);
  }

  private void deleteComment() {
    Comment c = null;
    try {
      c = getCurrentComment();
    }
    catch (CommentSelectionOutOfSynchException e) {
      logger.error("Comment list out of synch. comment not deleted", e);
      return;
    }
    if (c == null)
      return; // can this happen?

    int index = getCurrentCommentIndex(); // this is ok - has to be current comment
    annot.deleteComment(c);
    removeEmptyComments();
    if( !c.getText().trim().equals("")  ) {
       Object source = featureEditorDialog;
       TransactionSubpart sp = TransactionSubpart.COMMENT;
       AnnotationDeleteEvent ade = new AnnotationDeleteEvent(source,annot,sp);
       ade.setOldComment(c);
       ade.setSubpartRank(index);
       featureEditorDialog.fireAnnotEvent(ade);
    }
    // May need to filter out internal comments
    Vector listed_comments = synchCommentListWithAnnot();
    while (index >= listed_comments.size())
      index--;
    if (listed_comments.size() > 0) {
      userCommentListGui.setSelectedIndex(index);
      selectComment((Comment) userCommentListGui.getSelectedValue());
    }
    else
      userCommentListGui.clearSelection();
    featureEditorDialog.refreshCommentField();
  }

  /** Remove blank comments that got added but were never filled in. Presently
   called with close button - needs to also be called on window close! */
  private void removeEmptyComments() {
    Comment selectedComment = null;
    try { selectedComment = getCurrentComment(); }
    catch (CommentSelectionOutOfSynchException e) {}

    Vector all_comments = annot.getComments();
    for (int i = all_comments.size() - 1; i >= 0; i--) {
      Comment c = (Comment) all_comments.elementAt(i);
      if (c.getText().trim().equals("")) {
        annot.deleteComment(c);
        // if we are removing selected comment set edited to false & clear sel
        if (c == selectedComment) {
          commentEdited = false;
          userCommentListGui.clearSelection();
        }
      }
    }
  }

  /** Create a new comment. Adds comment to annot */
  private void addComment() {
    if (annot == null)
      return;

    Comment newComment = createNewComment();
    synchCommentListWithAnnot();  // May need to filter out internal comments
    userCommentListGui.setSelectedValue(newComment, true);
    selectComment(newComment);
    featureEditorDialog.refreshCommentField();
  }

  /** Create new comment and add it to annot */
  private Comment createNewComment() {
    Comment newComment = null;
    // Check if there's already an empty comment that we should use as
    // the new one.
    Vector all_comments = annot.getComments();
    for (int i = 0; i < all_comments.size() && newComment == null; i++) {
      Comment c = (Comment) all_comments.elementAt(i);
      newComment = (c.getText().trim().equals("")) ? c : null;
    }
    if (newComment == null) {
      String name = UserName.getUserName();
      newComment = new Comment(annot.getId(),
                               "",
                               name,
                               System.currentTimeMillis());
      // add the empty comment. this will get removed at close time if comment is
      // never filled in (removeEmptyComments). Need empty comment for comment list.
      annot.addComment(newComment);
      // Need to set selected index to 0 because otherwise if user wanted
      // to use the same comment as last time (which would come up selected)
      // they couldn't, because it would not register as a valueChanged event.
      if (cannedCommentListGui.getItemCount() > 0)
        cannedCommentListGui.setSelectedIndex(0);
    }
    currentCommentIsNew = true;
    return newComment;
  }

  protected void selectComment(Comment c) {
    commentText.getDocument().removeDocumentListener(commentDocumentListener);
    if (c != null) {
      commentText.setText(c.getText());
      commentText.setEditable(true);
      cannedCommentListGui.setEnabled(true);
      if(Config.getCheckOwnership() && 
      		!UserName.getUserName().equals(c.getPerson()) ) {
         delButton.setEnabled(false);
      } else {
        delButton.setEnabled(true);
      }
      isInternalCheckBox.setSelected(c.isInternal());
      isInternalCheckBox.setEnabled(true);
    } else {
      commentText.setText("");
      commentText.setEditable(false);
      cannedCommentListGui.setEnabled(false);
      delButton.setEnabled(false);
      isInternalCheckBox.setSelected(false);
      isInternalCheckBox.setEnabled(false);
    }
    commentText.getDocument().addDocumentListener(commentDocumentListener);
  }



  /** Depending on whether we're looking at annotation or transcript comments,
   *  put up the appropriate list of canned comments. */
  public void setType(int type) {
    if (type == ANNOTATION)
      cannedComments = Config.getAnnotationComments();
    else
      cannedComments = Config.getTranscriptComments();

    cannedCommentListGui.removeAllItems();

    // getAnn/TransComments returns null if there is none. 
    // This prevents null pointer exception. print msg about lack of canned comments?
    if (cannedComments == null)
      return;

    // I couldn't find a way to add the new comments all at once--have to iterate thru
    for (int i = 0; i < cannedComments.size(); i++)
      cannedCommentListGui.addItem(cannedComments.elementAt(i));
    cannedCommentListGui.setMaximumRowCount(Math.min (cannedComments.size(),
                                            16));
  }

  private void setColor(Color bgColor) {
    this.bgColor = bgColor;
    isInternalCheckBox.setBackground(bgColor);
    cannedCommentListGui.setBackground (bgColor);
    this.getContentPane().setBackground(bgColor);
  }

  private class CommentSelectionOutOfSynchException extends Exception {}

  private Comment getCurrentComment() throws CommentSelectionOutOfSynchException {
    if (getCurrentCommentIndex() >= userCommentListGui.getModel().getSize())
      throw new CommentSelectionOutOfSynchException();
    return (Comment)userCommentListGui.getSelectedValue();
  }

  private int getCurrentCommentIndex() {
    return userCommentListGui.getSelectedIndex();
  }


  /** From ListSelectionListener interface. 
   * loads selections from comment list into GUI */
  private class UserCommentListSelListener implements ListSelectionListener {
    /** list selection happens before focus lost which screws up modifying model with
        focus lost (need new gui!!). so have to check if gui has been edited here too
        before we change the list */
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) // get 2 events - 1st is adjusting
        checkForEditedComment(e);
      selectComment((Comment)userCommentListGui.getSelectedValue());
    }
    
    private void checkForEditedComment(ListSelectionEvent e) {
      try {
        Comment c = getPreviouslySelectedComment(e); // throws exception
        commitEditedComment(c);
      }
      catch (CommentSelectionOutOfSynchException ex) {
        logger.error("Can't get prev sel comment.", ex);
      }
    }

    /** Get previously selected comment as proscribed by list sel event. its an
        odd event - it gives you a first and last index like a range, one of 
        which is the formerly selected, one of which is the new and this can
        be discerned by querying for the currently selected. */
    private Comment getPreviouslySelectedComment(ListSelectionEvent e) 
      throws CommentSelectionOutOfSynchException {
      int i = getCurrentCommentIndex();
      int lastIndex = i == e.getFirstIndex() ? e.getLastIndex() : e.getFirstIndex();
      
      // this can throw an array index out of range exception - check for
      if (lastIndex >= userCommentListGui.getModel().getSize())
        throw new CommentSelectionOutOfSynchException(); 

      return (Comment)userCommentListGui.getModel().getElementAt(lastIndex);
    }

  }

  /** If item from list canned list is selected, model is modified and update
      or add event is fired depending on if comment is new or old */
  private class CannedListItemListener implements ItemListener {

    public void itemStateChanged(ItemEvent e) {
      
      // filter out deselections
      if (e.getStateChange() == ItemEvent.DESELECTED)
        return;

      int row = cannedCommentListGui.getSelectedIndex();
      if (row == 0) // row 0 is add your own comment (hand type)
        return;

      commentText.setText((String) cannedCommentListGui.getSelectedItem());
      commentEdited = true;
      commitCurrentComment(); // change model and fire event
    }

  }

  //private void commitComment() { commitComment(getCurrentComment()); }

  void commitCurrentComment() {
    try {
      Comment com = getCurrentComment(); // throws out of synch exception
      commitEditedComment(com);
    }
    catch (CommentSelectionOutOfSynchException e) {
      logger.error("Comment list out of synch. comment not committed", e);
      return;
    }
  }

  private void commitEditedComment(Comment c) {
    if (commentEdited)
      commitComment(c);
    commentEdited = false;
  }

  /** sets model and fires event */
  private void commitComment(Comment comment) {
    if (comment == null)
      return;

    // fire event
    TransactionSubpart sp = TransactionSubpart.COMMENT;
    Object source = featureEditorDialog;

    // clone oldComment for undo...
    Comment oldComment = comment.cloneComment(); 
    setCommentGuiToModel(comment); // makes changes to comment in model

    // cant use currentCommentIndex as may be different with list selection
    int commentIndex = getCommentIndex(comment); // check for -1?

    // new -> add
    if (currentCommentIsNew) {
      AnnotationAddEvent aae = new AnnotationAddEvent(source,annot,sp);
      // need to put in which comment is new comment (index?) should be last one
      aae.setSubpartRank(commentIndex);
      // empty comment is actually already added in createNewComment
      //annot.addComment(getCurrentComment());
      currentCommentIsNew = false;
      featureEditorDialog.fireAnnotEvent(aae);
    }

    // not new - update
    else {
      AnnotationUpdateEvent aue = new AnnotationUpdateEvent(source,annot,sp);
      // set old comment for undo, need index of updated comment as well
      aue.setSubpartRank(commentIndex);
      aue.setOldComment(oldComment); // for undo
      featureEditorDialog.fireAnnotEvent(aue);
    }
    featureEditorDialog.refreshCommentField();
  }

  private int getCommentIndex(Comment comment) {
    return annot.getCommentIndex(comment); // exception on -1?
  }

  /** Set comment model from gui values */
  private void setCommentGuiToModel(Comment comment) {
    comment.setText(apollo.util.IOUtil.stripControlChars(commentText.getText()));
    comment.setIsInternal(isInternalCheckBox.isSelected());
  }

  /** If comment text box has been edited and lose focus -> commit comment 
   */
  private class CommentTextFocusListener implements FocusListener {
    public void focusLost(FocusEvent e) {
      // do update and add events here - check if actually different?
      //setCommentModel();
      //userCommentListGui.updateUI(); // ??
      //commitEditedComment(getCurrentComment());
      commitCurrentComment();
    }
    
    public void focusGained(FocusEvent e) {}

  }


  /** Listens for changes to JTextAra commentTexts Document,
      and update comments. Calling updateUI causes a null pointer 
      (not sure why) and it doesnt seem necasary, so the call is
      not made. If its deemed necasary the null pointer will have 
      to be figured out (its a rather odd one) */
  private class CommentDocumentListener implements DocumentListener {
    public void changedUpdate(DocumentEvent e) { processCommentEdit(); }
    public void insertUpdate(DocumentEvent e) { processCommentEdit(); }
    public void removeUpdate(DocumentEvent e) { processCommentEdit(); }

    private void processCommentEdit() {
      commentEdited = true;
      // preserve oldComment? //updateComment(); // ??
    }
  }

  public void setFeature (AnnotatedFeatureI cf, Color bgColor) {
    this.annot = cf;
    if (annot != null) {
      setTitle (cf.getName() + " comments");
      logger.info("Editing comments for " + cf.getName());  // for help with debugging
      addButton.setEnabled(true);
      //      addComment();
      synchCommentListWithAnnot();
      setColor (bgColor);
    } else {
      setTitle ("Comments");
      clearFields();
    }
  }

}

  /*
  private void updateComment(char in, int pos) {
    Comment c = (Comment) commentList.getSelectedValue();
    if (c != null) {
      StringBuffer b = new StringBuffer(commentText.getText());
      if (pos >= 0 && pos <= b.length()) {
        b.insert(pos, in);
        c.setText(apollo.util.IOUtil.stripControlChars(b.toString()));
        c.setIsInternal(isInternalCheckBox.isSelected());
        featureEditorDialog.refreshCommentField();
      }
    }
  }
  */
