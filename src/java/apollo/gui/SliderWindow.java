package apollo.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.Serializable;
import apollo.util.GuiUtil;

public class SliderWindow extends JPanel implements ChangeListener {

  protected JSlider slider;
  protected JTextField field;
  protected String  labelStr;
  protected String  title;
  BoundedRangeModel model;
  protected JFrame  frame;

  boolean isAdjusting = false;

  /** Makes self and makes a JFrame and puts self into it. BoundedRangeModel is
      the model for the active curation graphs WindowScoreCalculator at the time
      the Tweeker was brought up. If the active curSet has changed slider 
      window will continue working with the now inactive cur set. Perhaps there
      could be an ActiveCurationChangeEvent on active cur change. Or there could be
      a dropdown list of cur sets to choose from, or should that be in Tweeker? */
  public SliderWindow(String title, String label, BoundedRangeModel model,
                      Point location) {
    this.labelStr = label;
    this.title = title;
    this.model = model;

    buildGUI();

    this.frame = new JFrame(title);
    this.frame.setLocation(location);
    setPreferredSize(new Dimension(300,100));
    this.frame.getContentPane().setLayout(new BorderLayout());
    this.frame.getContentPane().add(this,BorderLayout.CENTER);
    // Tried this to keep it from stretching out after showing the restriction enzyme
    // panel, but it didn't work right.
    //    this.frame.getContentPane().setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    //    this.frame.getContentPane().add(this);
    this.frame.setSize(new Dimension(300,100));
    //    JSlider slider = new JSlider();  // not used
    this.frame.setVisible(true);
  }

  /** Contructs self but does not put up a JFrame like other constructor does */
  public SliderWindow(BoundedRangeModel model) {
    this.model = model;
    buildGUI();
  }

//   void setModel(BoundedRangeModel model) {
//     this.model = model;
//     buildGUI();
//   }

  protected void buildGUI() {

    ActionListener numberChange = new FieldActionListener();

    JPanel enclosure = new JPanel();
    enclosure.setLayout( new GridBagLayout());

    add( enclosure, BorderLayout.CENTER );

    JLabel l = new JLabel(labelStr);
    slider = new JSlider(model);
    slider.setOrientation(JSlider.HORIZONTAL);
    slider.setMajorTickSpacing( (model.getMaximum() - model.getMinimum()) / 4);
    slider.setMinorTickSpacing( (model.getMaximum() - model.getMinimum()) / 10);
    slider.setPaintTicks( true );
    slider.setPaintLabels( true );
    enclosure.add( slider ,GuiUtil.makeConstraintAt(0,0,0,true));
    field = new JTextField();
    field.setPreferredSize(new Dimension(40,40));
    l.setLabelFor(slider); // what does this do?
    JPanel fieldHolder = new JPanel(new BorderLayout());
    field.addActionListener(numberChange);
    field.setText("" + model.getValue());
    fieldHolder.add(field,BorderLayout.CENTER);

    enclosure.add(fieldHolder, GuiUtil.makeConstraintAt(1,0,0,true));

    slider.addChangeListener( this );
  }

  public void stateChanged( ChangeEvent e ) {
    if ( e.getSource() instanceof JSlider ) {
      int currentValue = slider.getValue();
      field.setText(String.valueOf(currentValue));
    }
  }

  class FieldActionListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      int currentValue = Integer.parseInt(field.getText());

      model.setValue(currentValue);
    }
  }
}
