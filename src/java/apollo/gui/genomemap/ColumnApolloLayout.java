package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import apollo.gui.*;

import org.apache.log4j.*;

/**
 * An ApolloLayoutManager which lays out its views in a single column.
 */

public class ColumnApolloLayout extends ApolloLayoutManager {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ColumnApolloLayout.class);

  public ColumnApolloLayout() {
    super();
    free = null;
  }

  public void layoutViews(Container target) {
    if (target instanceof ApolloPanel) {
      ApolloPanel ap                 = (ApolloPanel)target;
      layoutViews(ap.getViews(), ap.getBounds(),0);
    }
  }

  public void layoutViews(Vector views,
                          Rectangle targetBounds, 
                          int startVertPos) {
    int         unmanagedHeight    = 0;
    int         standardHeight     = 0;
    int         vertPosition       = startVertPos;
    Rectangle   bounds             = new Rectangle();
    int         numHeightUnmanaged = 0;
    int         numUnmanaged       = 0;
    int         numManaged         = 0;
    int         fillHeight         = 0;
    int         countManaged       = 0;
    int         countVisible       = 0;

    logger.debug("Layout views");

    //Algorithm:
    // 1. Find any Vertically unmanaged Views and get their heights
    for(int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v.isVisible()) {
        if (viewmap.containsKey(v)) {
          if (((String)viewmap.get(v)).equals(HORIZONTAL)) {
            unmanagedHeight += v.getPreferredSize().height;
            numHeightUnmanaged++;
          }
          else if (((String)viewmap.get(v)).equals(NONE)) {
            numUnmanaged++;
          }
        } else {
          logger.warn("ColumnLayoutManager found an unmanaged view");
          numUnmanaged++;
        }
        countVisible++;
      }
    }

    /* 2. Subtract these from the the total height to give the 
       available height for the other views */
    numManaged = countVisible-numHeightUnmanaged-numUnmanaged;
    if (numManaged != 0) {
      standardHeight = (targetBounds.height-unmanagedHeight)/numManaged;
      // 12/20/01 standardHeight is sometimes coming out 0, 
      // which makes the next line fail.  Not sure what it should be set 
      // to if it's 0, but let's try 1.  --NH
      if (standardHeight == 0) {
        logger.debug("ColumnApolloLayout.standardHeight == 0.  Setting it to 1.");
        standardHeight = 1;
      }
      // 3. Get fill height for last unmanaged view
      fillHeight = (targetBounds.height-unmanagedHeight)%standardHeight;
    }


    // 4. Reset bounds on views
    for(int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v.isVisible()) {
        if (viewmap.containsKey(v)) {
          String layoutConstraint = (String)viewmap.get(v);
          if (layoutConstraint.equals(NONE)) {
            // Bounds determined by view
            v.setBounds(v.getBounds());
          } else if (layoutConstraint.equals(HORIZONTAL)) {
            // Height determined by view width determined by manager
            bounds.x          = 0;
            bounds.y          = vertPosition;
            bounds.width      = targetBounds.width;
            bounds.height     = v.getPreferredSize().height;

            v.setBounds(bounds);

            // For recursive calls the bounds can end up different.
            // When this occurs NO more layout should be done (it
            // will already have been done in the recursive call).
            if (bounds.y != v.getBounds().y ||
                bounds.height != v.getBounds().height) {
              return;
            }

            vertPosition     += v.getPreferredSize().height;

          } else { // Height and width determined by manager - constraint BOTH

            countManaged++;

            bounds.x      = 0;
            bounds.y      = vertPosition;
            bounds.width  = targetBounds.width;

            bounds.height = standardHeight;

            int vertPositionIncrease = bounds.height;

            if (countManaged==numManaged) {
              bounds.height += fillHeight;
              vertPosition  += fillHeight;
            }

            v.setBounds(bounds);
            // sorry - cant be standard height with hack above
            vertPosition += vertPositionIncrease;//standardHeight;
          }
        }
        else if (v instanceof ViewI &&
                 !((String)viewmap.get(v)).equals(NONE)) {
          bounds.width      = targetBounds.width;
        } else {
          v.setBounds(v.getBounds());
        }
        v.setInvalidity(true);
      }else{
         bounds.x          = 0;   
         bounds.y          = 0;
         bounds.width      = targetBounds.width;
         bounds.height     = 10;  // the actual size is irrelevant here...
         v.setBounds(bounds); 
      }
    }
  }

}
