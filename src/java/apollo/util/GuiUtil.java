package apollo.util;

import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class GuiUtil {

  /** constrain width,height to screen size, returns as Dimension */
  public static Dimension fitToScreen(int width, int height) {
    return fitToScreen(new Dimension(width,height));
  }
  /** constrain Dimension to screen size */
  public static Dimension fitToScreen(Dimension dim) {
    Dimension ss   = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension d = new Dimension();
    d.height = dim.height > ss.height ? ss.height : dim.height;
    d.width = dim.width > ss.width ? ss.width : dim.width;
    return d;
  }

  public static JLabel makeJLabelWithFont(String label) {
    return makeJLabelWithFont(label,null);
  }

  public static JLabel makeJLabelWithFont(String label,String example) {
    String l = label;
    /* We need this size tag on SuSE linux (and on Solaris).
       On RedHat linux, it looks better without the size tag. */
       l = "<html><FONT color=black><B>"+label+"</B>";
    if (example!=null)
      l +=  "<BR>(e.g. "+example+")</FONT></HTML>";
    /*
    if (apollo.util.IOUtil.isUnix())
      l = "<html><FONT FACE=Geneva,Arial,Helvetica size=2 color=black><B>"+
	label+"</B>";
    else
      l = "<html><FONT FACE=Geneva,Arial,Helvetica color=black><B>"+
	label+"</B>";
    */
    JLabel jLabel = new JLabel(l);
    /* I seem to get the best results if I set both preferred and max. 
       This might need more tweeking. I didnt think max would be needed 
       with the horizontal glue added below, but it makes a difference.
    jLabel.setPreferredSize(new Dimension(120,40));
    jLabel.setMaximumSize(new Dimension(200,70)); */
    jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel.setVerticalAlignment(SwingConstants.CENTER);
    return jLabel;
  }

  /**
   * A convenience to make GridBagConstraints when you're laying out GridBag
  **/
  public static GridBagConstraints makeConstraintAt(int x, int y, int width) {
    return makeConstraintAt (x, y, width, 0, false);
  }//end makeConstraintAt

  // makes various GridBagConstraints needed by GUI
  public static GridBagConstraints makeConstraintAt(int x, int y,
						    int padamt,
						    boolean fill) {
    return makeConstraintAt (x, y, 1, padamt, fill);
  }
   
  /** pad is horizontal, weights are 0.0 */
  public static GridBagConstraints makeConstraintAt (int x, int y,
						     int width,
						     int padamt,
						     boolean fill) {
    GridBagConstraints g = makeWeightConst(x,y,width,padamt,fill);
    g.weightx = 0.0;
    return g;
  }

  public static GridBagConstraints makeWeightConst(int x, int y,
						    int padamt,
						    boolean fill) {
    return makeWeightConst (x, y, 1, padamt, fill);
  }

  public static GridBagConstraints makeWeightConst (int x, int y,
						     int width,
						     int padamt,
						     boolean fill) {
    int height = 1;
    return makeConstraint(x,y,width,height,padamt,fill);
  }

  /** weightx is 1.0, weighty is 0.0 */
  public static GridBagConstraints makeConstraint(int x, int y, int width,
                                                  int height, int padamt,
                                                  boolean horFill) {
    int fill = horFill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
    double weighty = 0.0;
    return makeConstraint(x,y,width,height,weighty,padamt,fill);
  }

  public static GridBagConstraints makeConstraint(int x, int y, int width,
                                                  int height, double weighty,
                                                  int padamt, int fill) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = width;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    gbc.weighty = weighty;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = fill;
    gbc.insets = new Insets(0, padamt, 0, padamt);
    return gbc;
  }

  public static JTextField makeNumericTextField() {
    JTextField jtf = new JTextField();
    jtf.addKeyListener(NumericKeyFilter.getFilter());
    // wierd - uses width from preferred and height from max
    jtf.setPreferredSize(new Dimension(75,25)); // static var?
    return jtf;
  }

}
