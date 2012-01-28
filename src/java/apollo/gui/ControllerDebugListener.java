package apollo.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.ResultChangeEvent;
import apollo.editor.ResultChangeListener;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.PropSchemeChangeListener;
import apollo.gui.event.*;


public class ControllerDebugListener extends NonWindowControlledObject implements
      BaseFocusListener,
      AnnotationChangeListener,
      FeatureSelectionListener,
      LazyLoadListener,
      DataLoadListener,
      ResultChangeListener,
  PropSchemeChangeListener {


  public ControllerDebugListener(Controller c) {
    super(c);
  }

  public boolean handleAnnotationChangeEvent (AnnotationChangeEvent evt) {
    return true;
  }

  public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
    return true;
  }

  public boolean handleDataLoadEvent (DataLoadEvent evt) {
    return true;
  }

  public boolean handleLazyLoadEvent (LazyLoadEvent evt) {
    System.out.println("Handling LazyLoadEvent " + evt);
    return true;
  }

  public boolean handleResultChangeEvent (ResultChangeEvent evt) {
    return true;
  }

  public boolean handlePropSchemeChangeEvent (PropSchemeChangeEvent evt) {
    return true;
  }

  public boolean handleBaseFocusEvent (BaseFocusEvent evt) {
    return true;
  }
}
