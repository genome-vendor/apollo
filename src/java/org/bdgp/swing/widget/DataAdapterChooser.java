package org.bdgp.swing.widget;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import org.bdgp.swing.*;
import java.util.*;

public class DataAdapterChooser extends JDialog implements Commitable {

    protected DataAdapterRegistry registry;
    protected IOOperation op;
    protected Object output;
    protected Object input;
    protected boolean cancelled;
    protected boolean failed;
    protected DataAdapterException exception;

    protected JPanel mainPanel = new JPanel();
    protected JPanel buttonPanel = new JPanel();
    protected JComboBox adapterList = new JComboBox();
    protected JButton okButton = new JButton("Ok");
    protected JButton cancelButton = new JButton("Cancel");
  //    protected JLabel adapterListLabel = new JLabel("Choose data adapter");
  protected JLabel adapterListLabel = new JLabel("<html><FONT color=black><B>" + "Choose data source:" + "</B>");

    protected TitledBorder pluginBorder = new TitledBorder("");

    private JPanel centerPanel = new JPanel();
    protected Component currentGUI;
    protected VisualDataAdapter currentAdapter;
    protected ProgressListener progressListener;
    protected JProgressBar progressBar = new JProgressBar();

    protected boolean failfast;
    protected boolean showChooser;

    // Number of rows (data adapters) to show before adding scrollbar
    protected int ADAPTER_LIST_LENGTH = 12;

    MultiProperties adapterProperties = null;
    File propertiesFile = null;
    DataAdapterUIListener uiChangeListener;

    protected Point preferredPos;

    public DataAdapterChooser(VisualDataAdapter currentAdapter,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super((JFrame) null, title, true);
	init(null, op, input, failfast, false, currentAdapter);
    }

    public DataAdapterChooser(Dialog parent,
			      VisualDataAdapter currentAdapter,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super((JFrame) null, title, true);
	init(null, op, input, failfast, false, currentAdapter);
    }

    public DataAdapterChooser(Frame parent,
			      VisualDataAdapter currentAdapter,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super((JFrame) null, title, true);
	init(null, op, input, failfast, false, currentAdapter);
    }

    public DataAdapterChooser(DataAdapterRegistry registry,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super((JFrame) null, title, true);
	init(registry, op, input, failfast, true, null);
    }

    public DataAdapterChooser(Dialog parent,
			      DataAdapterRegistry registry,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super((JFrame) null, title, true);
	init(registry, op, input, failfast, true, null);
    }

    public DataAdapterChooser(Frame parent,
			      DataAdapterRegistry registry,
			      IOOperation op,
			      String title,
			      Object input,
			      boolean failfast) {
	super(parent, title, true);
	init(registry, op, input, failfast, true, null);
    }

    protected void init(DataAdapterRegistry registry,
			IOOperation op,
			Object input,
			boolean failfast,
			boolean showChooser,
			VisualDataAdapter currentAdapter) {
	this.currentAdapter = currentAdapter;
	this.showChooser = showChooser;
	this.registry = registry;
	this.op = op;
	this.input = input;
	this.output = null;
	cancelled = true;
	failed = false;
	this.failfast = failfast;
	uiChangeListener = new DataAdapterUIListener() {
		public void setNewUI(DataAdapterUIEvent e) {
		    System.err.println("Got UI change event");
		    setUI((Component) e.getUI());
		}
	    };
    }

    protected class CommitRunnable implements Runnable {

	public void run() {
	    doCommit();
	}
    }

    public void setBackground(Color color) {
	super.setBackground(color);
	if (mainPanel != null)
	    mainPanel.setBackground(color);
	if (buttonPanel != null)
	    buttonPanel.setBackground(color);
    }

    public void setForeground(Color color) {
	super.setForeground(color);
	if (adapterList != null)
	    adapterList.setForeground(color);
	if (okButton != null)
	    okButton.setForeground(color);
	if (cancelButton != null)
	    cancelButton.setForeground(color);
    }

    public void setControlBackground(Color color) {
	if (adapterList != null)
	    adapterList.setBackground(color);
	if (okButton != null)
	    okButton.setBackground(color);
	if (cancelButton != null)
	    cancelButton.setBackground(color);
    }

    public void setLabelColor(Color color) {
	if (pluginBorder != null)
	    pluginBorder.setTitleColor(color);
	if (adapterListLabel != null)
	    adapterListLabel.setForeground(color);
    }

    public void show() {
	if (showChooser) {
	    Vector adapters = getUsableAdapterList();
	    if (adapters.size() == 0)
		JOptionPane.showMessageDialog(this,
					      "No data adapters "+
					      "support this operation!");
	}
	buildGUI();
	installListeners();
	boolean adapterSelected = false;
	if (adapterProperties != null && showChooser) {
	    String recentAdapter = adapterProperties.getProperty(
				    "DataAdapterChooser.recentAdapter");
	    if (recentAdapter != null) {
		for(int i=0; i < adapterList.getItemCount(); i++) {
		    DataAdapter adapter = (DataAdapter)
			adapterList.getItemAt(i);
		    if (adapter.getClass().getName().equals(recentAdapter)) {
			adapterList.setSelectedIndex(i);
			adapterSelected = true;
			break;
		    }
		}
	    }
	}
	if (!adapterSelected && showChooser)
	    adapterList.setSelectedIndex(0);
	if (!showChooser) {
	    DataAdapterUI ui = currentAdapter.getUI(op);
	    if (ui == null || !(ui instanceof Component))
		throw new RuntimeException("Adapter "+currentAdapter+
		    "refuses to provide a displayable UI");
	    setUI((Component) ui);
	}

	if (preferredPos == null) {
	    Dimension screenSize   =
		Toolkit.getDefaultToolkit().getScreenSize();
	    setLocation((screenSize.width - getSize().width) / 2,
			(screenSize.height - getSize().height) / 2);
	} else
	    setLocation(preferredPos.x, preferredPos.y);

	super.show();
    }

    /**
     * The window will appear at this position when show() is called. If the
     * position given is null, the window will be centered (which will ruin
     * everything on Linux dual display machines running jdk1.3.x or earlier)
     */
    public void setPreferredPos(Point preferred) {
	this.preferredPos = preferred;
    }

    public void cancel() {
	setVisible(false);
    }

    protected void setUI(Component ui) {
	if (currentGUI != null) {
	    if (currentGUI instanceof InteractiveDataAdapterUI) {
		((InteractiveDataAdapterUI) currentGUI).
		    removeDataAdapterUIListener(uiChangeListener);
		((InteractiveDataAdapterUI) currentGUI).
		    setControllingObject(null);
		mainPanel.add(buttonPanel);
	    }
	    centerPanel.remove(currentGUI);
	}
	currentGUI = ui;
	((DataAdapterUI) currentGUI).setDataAdapter(currentAdapter);
	if (currentGUI instanceof InteractiveDataAdapterUI) {
	    ((InteractiveDataAdapterUI) currentGUI).
		addDataAdapterUIListener(uiChangeListener);
	    ((InteractiveDataAdapterUI) currentGUI).
		setControllingObject(this);
	    mainPanel.remove(buttonPanel);
	}
	if (adapterProperties != null) {
	    Properties props = adapterProperties.getProperties(
		currentAdapter.getClass().getName());
	    ((DataAdapterUI) currentGUI).setProperties(props);
	} else
	    ((DataAdapterUI) currentGUI).setProperties(null);
	((DataAdapterUI) currentGUI).setInput(input);
	centerPanel.add(currentGUI, "Center");
	currentGUI.setFont(getFont());
	currentGUI.validate();
	centerPanel.validate();
	pluginBorder.setTitle(currentAdapter.getName());
	pack();
    }

    public void displayCurrentGUI() {
	if (showChooser) {
	    VisualDataAdapter adapter = (VisualDataAdapter)
		adapterList.getSelectedItem();
	    currentAdapter = adapter;
            // Number of rows to show before adding scrollbar
            adapterList.setMaximumRowCount(ADAPTER_LIST_LENGTH);
	}
	setUI((Component) currentAdapter.getUI(op));
	/*
	if (currentGUI != null) {
	    if (currentGUI instanceof InteractiveDataAdapterUI) {
		((InteractiveDataAdapterUI) currentGUI).
		    removeDataAdapterUIListener(uiChangeListener);
		((InteractiveDataAdapterUI) currentGUI).
		    setControllingObject(null);
	    }
	    centerPanel.remove(currentGUI);
	}
	currentGUI = (Component) adapter.getUI(op);
	((DataAdapterUI) currentGUI).setDataAdapter(adapter);
	if (currentGUI instanceof InteractiveDataAdapterUI) {
	    ((InteractiveDataAdapterUI) currentGUI).
		addDataAdapterUIListener(uiChangeListener);
	    ((InteractiveDataAdapterUI) currentGUI).setControllingObject(this);
	}
	if (adapterProperties != null) {
	    Properties props = adapterProperties.getProperties(
	       			       adapter.getClass().getName());
	    ((DataAdapterUI) currentGUI).setProperties(props);
	} else
	    ((DataAdapterUI) currentGUI).setProperties(null);
	centerPanel.add(currentGUI, "Center");
	currentGUI.setFont(getFont());
	currentGUI.validate();
	centerPanel.validate();
	pluginBorder.setTitle(adapter.getName());
	pack();
	*/
    }

    public DataAdapter getDataAdapter() {
	return currentAdapter;
    }

    public void commit() {
	CommitRunnable runnable = new CommitRunnable();
	Thread committer = new Thread(runnable);
	committer.start();
    }

    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	if (showChooser)
	    adapterList.setEnabled(enabled);
	okButton.setEnabled(enabled);
	cancelButton.setEnabled(enabled);
    }

    public void setPropertiesFile(String file) {
	setPropertiesFile(new File(file));
    }

    public void setPropertiesFile(File file) {
	propertiesFile = file;
	try {
	    adapterProperties = new MultiProperties();
	    adapterProperties.load(new FileInputStream(file));
	} catch (IOException e) {
	}
    }

    public void setProperties(MultiProperties properties) {
	adapterProperties = properties;
    }

    public MultiProperties getProperties() {
	return adapterProperties;
    }

    public void doCommitWithExceptions() throws DataAdapterException {
	try {
	    setEnabled(false);
	    centerPanel.remove(currentGUI);
	    centerPanel.add(progressBar, "South");
	    centerPanel.validate();
	    centerPanel.repaint();
	    progressBar.setStringPainted(true);
	    progressBar.setString("Saving...");
	    progressBar.setValue(progressBar.getMinimum());
	    currentAdapter.init();
	    currentAdapter.addProgressListener(progressListener);
	    output = ((DataAdapterUI) currentGUI).
		doOperation(input);
	    currentAdapter.
		removeProgressListener(progressListener);
	    
	    Properties newProperties =
		((DataAdapterUI) currentGUI).getProperties();

	    if (adapterProperties != null) {
		if (newProperties != null) {
		    adapterProperties.
			setProperties(
			    currentAdapter.
			    getClass().getName(),
			    newProperties);
		}
		adapterProperties.
		    setProperty("DataAdapterChooser.recentAdapter",
				currentAdapter.getClass().
				getName());
		if (propertiesFile != null) {
		    try {
			adapterProperties.
			    save(new
				FileOutputStream(propertiesFile),
				  "DataAdapterChooser "+
				  "properties file");
		    } catch (IOException e) {
			e.printStackTrace();
				// don't bother
		    }
		}
	    }
	    cancelled = false;
	    failed = false;
	    exception = null;
	    setEnabled(true);
	    setVisible(false);
	} catch (DataAdapterException e) {
	    centerPanel.remove(progressBar);
	    centerPanel.add(currentGUI, "Center");
	    centerPanel.validate();
	    setEnabled(true);
	    repaint();		    
	    exception = e;
	    failed = true;
	}
	if (exception != null)
	    throw exception;
    }

    public void doCommit() {
	try {
	    doCommitWithExceptions();
	} catch (DataAdapterException e) {
	    exception = e;
	    failed = true;
	    cancelled = true;
	    if (failfast)
		setVisible(false);
	    else
		{
		    JPanel errorPanel = new JPanel();
		    errorPanel.setLayout(new BoxLayout(errorPanel,
						       BoxLayout.Y_AXIS));
		    Box labelBox = new Box(BoxLayout.X_AXIS);
		    
		    JLabel errorLabel = new JLabel("Operation failed!");
		    labelBox.add(errorLabel);
		    labelBox.add(Box.createHorizontalGlue());
		    
		    JTextArea errorText = new JTextArea(24, 80);
		    errorText.setFont(new Font("Courier", 0,
					       getFont().getSize()));
		    errorText.setText(e.toString());
		    errorPanel.add(labelBox);
		    errorPanel.add(Box.createVerticalStrut(15));
		    errorPanel.add(new JScrollPane(errorText));
		    JOptionPane.showMessageDialog(this, errorPanel);
		}
	}
    }

    public boolean isCancelled() {
	return cancelled;
    }

    public DataAdapterException getException() {
	return exception;
    }

    public boolean isFailure() {
	return failed;
    }

    public void installListeners() {
	try {
	    if (showChooser) {
		adapterList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    displayCurrentGUI();
			}
		    });
	    }
	    cancelButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			cancel();
		    }
		});
	    okButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			commit();
		    }
		});
	    progressListener = new ProgressBarProgressListener(progressBar);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void buildGUI() {
	if (showChooser) {
	    Vector adapters = getUsableAdapterList();
	    // TO ADD:
	    // if there are no data adapters, complain
	    // --
	    adapterList.removeAllItems();
	    for(int i=0; i < adapters.size(); i++)
		adapterList.addItem(adapters.get(i));
	}

	mainPanel.removeAll();
	buttonPanel.removeAll();
	centerPanel.removeAll();

	progressBar.setStringPainted(true);
	progressBar.setFont(getFont());

	pluginBorder.setTitleFont(getFont());
	centerPanel.setBorder(pluginBorder);
	centerPanel.setLayout(new BorderLayout());
	if (showChooser)
	    adapterList.setFont(getFont());
	okButton.setFont(getFont());
	cancelButton.setFont(getFont());

	buttonPanel.setOpaque(false);
	centerPanel.setOpaque(false);
	//	buttonPanel.setPreferredSize(new Dimension(150,20));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(okButton);
	buttonPanel.add(Box.createHorizontalStrut(10));
	buttonPanel.add(cancelButton);
	buttonPanel.add(Box.createHorizontalGlue());
	
	adapterListLabel.setFont(getFont());	

	Box adapterListLabelBox = new Box(BoxLayout.X_AXIS);
	adapterListLabelBox.add(adapterListLabel);
	adapterListLabelBox.add(Box.createHorizontalGlue());

	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

	if (showChooser) {
	    mainPanel.add(Box.createVerticalStrut(10));
	    mainPanel.add(adapterListLabelBox);
	    mainPanel.add(adapterList);
	}
	mainPanel.add(Box.createVerticalStrut(10));
	mainPanel.add(centerPanel);
	mainPanel.add(Box.createVerticalStrut(10));
	mainPanel.add(buttonPanel);
	mainPanel.add(Box.createVerticalStrut(10));
	//	mainPanel.add(Box.createVerticalGlue());
	setContentPane(mainPanel);
	pack();
    }

    public Vector getUsableAdapterList() {
	if (registry == null)
	    return null;
	Vector adapterList = new Vector();
	String [] names = registry.getAdapterNames(op, true);
	for(int i=0; i < names.length; i++) {
	    VisualDataAdapter adapter = (VisualDataAdapter)
		registry.getAdapter(names[i]);
	    if (adapter != null && (adapter.getUI(op) instanceof Component))
		adapterList.addElement(adapter);
	    adapter.getUI(op).setInput(input);
	}
	return adapterList;
    }

    public Object getOutput() {
	return output;
    }
}

class ProgressBarProgressListener implements ProgressListener {

    JProgressBar bar;

    public ProgressBarProgressListener(JProgressBar bar) {
	this.bar = bar;
    }

    public void progressMade(ProgressEvent event) {
	final ProgressEvent e = event;
	Runnable r = new Runnable() {
		public void run() {
		    String displayMe = null;
		    if (e.getDescription() == null)
			bar.setStringPainted(false);
		    else
			displayMe = e.getDescription();

		    int val;
		    if (e instanceof ReusableProgressEvent) {
			val = ((ReusableProgressEvent) e).getFastVal();
		    } else if (e.getValue() == null)
			val = -1;
		    else {
			val = (int) e.getValue().doubleValue();
		    }
		    bar.setValue(val);
		    if (displayMe == null)
			displayMe = val+"%";
		    else
			displayMe += " "+val+"%";
		    if (displayMe != null)
			bar.setString(displayMe);
		}
	    };
	try {
	    SwingUtilities.invokeLater(r);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

  public void failedDataAdapter (DataAdapterException e)
    {
    }
}

