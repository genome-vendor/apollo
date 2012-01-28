package apollo.dataadapter.graph;

import java.util.Properties;
import java.util.Iterator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import apollo.dataadapter.*;

import apollo.gui.GenericFileAdapterGUI;
import apollo.gui.GenericFileFilter;

import org.bdgp.io.*;

import apollo.util.GuiUtil;

public class GraphAdapterGUI extends GenericFileAdapterGUI implements ApolloDataAdapterGUI
{

  private Color color = Color.LIGHT_GRAY;
  
	/** Construct a GraphAdapterGUI object
	 * 
	 * @param op - IOOperation for this panel
	 */
	public GraphAdapterGUI(IOOperation op)
	{
    super(op);
		String desc = "Graph data";
		GenericFileFilter filter = new GenericFileFilter();
		filter.addExtension("sgr");
		filter.addExtension("wig");
		if (filter.getExtensions().size() > 0) {
			Iterator<String> i = filter.getExtensions().iterator();
			desc += " (*." + i.next();
			while (i.hasNext()) {
				desc += ", *." + i.next();
			}
			desc += ")";
		}
		filter.setDescription(desc);
		setFileFilter(filter);
	}
  
  public void buildGUI()
  {
    super.buildGUI();

    JButton colorButton = new JButton("Choose color");
    colorButton.setFont(getFont());
    colorButton.addActionListener(new ColorActionListener());
    getPanel().removeAll();
    setLayout(new GridBagLayout());
    add(panel2, GuiUtil.makeConstraintAt(0, 0, 1));
    GridBagConstraints c = GuiUtil.makeConstraintAt(0, 1, 1);
    c.anchor = GridBagConstraints.EAST;
    add(colorButton, c);
  }

	/** Main method to be executed for appending data
	 */
	public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException
	{
		GraphAdapter adapter = (GraphAdapter)driver;
		if (op.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
			adapter.setGraphFilename(getSelectedPath());
			return adapter.addToCurationSet(color);
		}
		else {
			throw new org.bdgp.io.DataAdapterException("This adapter only works for ApolloDataAdapterI.OP_APPEND_DATA");
		}
	}
	
	public Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException {
		StateInformation stateInformation = new StateInformation();
	    stateInformation.setProperty(StateInformation.DATA_FILE_NAME, getSelectedPath());
	    return stateInformation;
	}
  
  public class ColorActionListener implements ActionListener
  {
    
    public void actionPerformed(ActionEvent e)
    {
      color = JColorChooser.showDialog(null, "Choose color", color);
    }
    
  }

}
