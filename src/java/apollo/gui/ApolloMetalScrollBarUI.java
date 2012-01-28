
package apollo.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Adjustable;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Insets;
import java.awt.Color;
import java.awt.IllegalComponentStateException;

import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import javax.swing.plaf.*;
import javax.swing.plaf.metal.MetalScrollBarUI;


public class ApolloMetalScrollBarUI extends MetalScrollBarUI {

  protected Dimension getMinimumThumbSize() {
    return new Dimension(7, scrollBarWidth);
  }
}
