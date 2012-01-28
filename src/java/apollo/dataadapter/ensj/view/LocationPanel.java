package apollo.dataadapter.ensj.view;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import apollo.util.GuiUtil;

import apollo.gui.PopupPropertyListener;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

public class LocationPanel extends JPanel{
  
  private View _view;
  private JLabel _stableIDLabel = new JLabel("Stable ID");
  private JLabel _coordSystemLabel = new JLabel("Coordinate System");
  private JLabel _seqRegionLabel = new JLabel("Seq Region Name");
  private JLabel _startLabel = new JLabel("Start - End");
  
  private UpdateWorkRoundComboBox _stableIDDropdown = new UpdateWorkRoundComboBox();
  private UpdateWorkRoundComboBox _coordSystemDropdown = new UpdateWorkRoundComboBox();
  private UpdateWorkRoundComboBox _seqRegionDropdown = new UpdateWorkRoundComboBox();
  private JTextField _startField = new JTextField(10);
  private JTextField _endField = new JTextField(10);
  
  private JLabel _historyLabel = new JLabel("History");
  private UpdateWorkRoundComboBox _historyDropdown = new UpdateWorkRoundComboBox();

  public LocationPanel(View view){
    _view = view;
    initialiseGUI();
  }

  private View getView(){
    return _view;
  }

  public void initialiseGUI(){
    setLayout(new java.awt.GridBagLayout());
    
    Dimension dropdownSize = new Dimension(200,25);
    
    add(getStableIDLabel(), GuiUtil.makeConstraintAt(0, 0, 10, true));
    getStableIDDropdown().setPreferredSize(dropdownSize);
    getStableIDDropdown().setMinimumSize(dropdownSize);
    getStableIDDropdown().getPopup().addPropertyChangeListener(new PopupPropertyListener());
    getStableIDDropdown().setEditable(true);
    add(getStableIDDropdown(), GuiUtil.makeConstraintAt(1, 0, 3, 10, true));
    
    //
    //When you type into the stable id dropdown, the other fields will be cleared
    getView().getAdapterGUI().addKeyRouter(getStableIDDropdown().getEditor().getEditorComponent(), Controller.USE_STABLE_ID_LOCATION);
    getView().getAdapterGUI().addActionRouter(getStableIDDropdown(), Controller.USE_STABLE_ID_LOCATION);
    
    add(getCoordSystemLabel(), GuiUtil.makeConstraintAt(0, 1, 10, true));
    getCoordSystemDropdown().setPreferredSize(dropdownSize);
    getCoordSystemDropdown().getPopup().addPropertyChangeListener(new PopupPropertyListener());
    add(getCoordSystemDropdown(), GuiUtil.makeConstraintAt(1, 1, 3, 10, true));
    
    //
    //When you select a coord system, the stable id dropdown is blanked out
    getView().getAdapterGUI().addActionRouter(getCoordSystemDropdown(), Controller.USE_SEQ_REGION_LOCATION);
    //When you select a coord system, clear out the seq-region dd (they will be re-initialised).
    getView().getAdapterGUI().addActionRouter(getCoordSystemDropdown(), Controller.CLEAR_SEQ_REGIONS);
    getView().getAdapterGUI().addPopupRouter(getCoordSystemDropdown(), Controller.FIND_COORD_SYSTEMS);

    add(getSeqRegionLabel(), GuiUtil.makeConstraintAt(0, 2, 10, true));
    getSeqRegionDropdown().setPreferredSize(dropdownSize);
    getSeqRegionDropdown().setEditable(true);
    getSeqRegionDropdown().getPopup().addPropertyChangeListener(new PopupPropertyListener());
    
    //
    //When the user selects a seq-region, the lengths are auto-populated into the start/end fields.
    getView().getAdapterGUI().addActionRouter(getSeqRegionDropdown(), Controller.SELECT_SEQ_REGION);

    add(getSeqRegionDropdown(), GuiUtil.makeConstraintAt(1, 2, 3, 10, true));

    //
    //When you popup the dd, the seq=regions are loaded automatically.
    getView().getAdapterGUI().addPopupRouter(getSeqRegionDropdown(), Controller.FIND_SEQ_REGIONS);
    //
    //When you select a Seq region, the stable id dropdown is blanked out
    getView().getAdapterGUI().addKeyRouter(getSeqRegionDropdown().getEditor().getEditorComponent(), Controller.USE_SEQ_REGION_LOCATION);
    getView().getAdapterGUI().addActionRouter(getSeqRegionDropdown(), Controller.USE_SEQ_REGION_LOCATION);

    add(getStartLabel(), GuiUtil.makeConstraintAt(0,3, 10, true));
    getStartField().setPreferredSize(dropdownSize);
    getStartField().setMinimumSize(dropdownSize);
    add(getStartField(), GuiUtil.makeConstraintAt(1,3, 10, false));

    add(new JLabel("-"), GuiUtil.makeConstraintAt(2,3, 10, true));
    getEndField().setPreferredSize(dropdownSize);
    getEndField().setMinimumSize(dropdownSize);
    add(getEndField(), GuiUtil.makeConstraintAt(3,3, 10, false));
    
    //
    //When you type into start/end fields, stable id dd is blanked out
    getView().getAdapterGUI().addKeyRouter(getStartField(), Controller.USE_SEQ_REGION_LOCATION);
    getView().getAdapterGUI().addKeyRouter(getEndField(), Controller.USE_SEQ_REGION_LOCATION);

    add(getHistoryLabel(), GuiUtil.makeConstraintAt(0, 4, 10, true));
    getHistoryDropdown().setPreferredSize(dropdownSize);
    getHistoryDropdown().getPopup().addPropertyChangeListener(new PopupPropertyListener());

    add(getHistoryDropdown(), GuiUtil.makeConstraintAt(1, 4, 3, 10, true));
    getView().getAdapterGUI().addActionRouter(getHistoryDropdown(), Controller.LOAD_SEQ_REGION_BY_HISTORY);
    
  }

  public void update(Model model){
    LocationModel myModel = model.getLocationModel();
    myModel.setStableID((String)getStableIDDropdown().getSelectedItem());
    myModel.setSelectedCoordSystem((String)getCoordSystemDropdown().getSelectedItem());
    myModel.setSelectedSeqRegion((String)getSeqRegionDropdown().getSelectedItem());
    myModel.setStart(getStartField().getText());
    myModel.setEnd(getEndField().getText());
    myModel.setSelectedHistoryLocation((String)getHistoryDropdown().getSelectedItem());
  }

  public void read(Model model){
    LocationModel myModel = model.getLocationModel();

    getStableIDDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getStableIDHistory())));
    getStableIDDropdown().setSelectedItem(myModel.getStableID());

    getCoordSystemDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getCoordSystems())));
    getCoordSystemDropdown().setSelectedItem(myModel.getSelectedCoordSystem());

    getSeqRegionDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getSeqRegions())));
    getSeqRegionDropdown().setSelectedItem(myModel.getSelectedSeqRegion());

    
    getStartField().setText(myModel.getStart());
    getEndField().setText(myModel.getEnd());
    
    getHistoryDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getLocationHistory())));
    getHistoryDropdown().setSelectedItem(myModel.getSelectedHistoryLocation());
  }

  private JLabel getStableIDLabel(){
    return _stableIDLabel;
  }

  private JLabel getCoordSystemLabel(){
    return _coordSystemLabel;
  }
  
  private JLabel getSeqRegionLabel(){
    return _seqRegionLabel;
  }
  
  private JLabel getStartLabel(){
    return _startLabel;
  }
  
  private JLabel getHistoryLabel(){
    return _historyLabel;
  }
  
  private UpdateWorkRoundComboBox getStableIDDropdown(){
    return _stableIDDropdown;
  }
  
  private UpdateWorkRoundComboBox getCoordSystemDropdown(){
    return _coordSystemDropdown;
  }
  
  private UpdateWorkRoundComboBox getSeqRegionDropdown(){
    return _seqRegionDropdown;
  }
  
  private UpdateWorkRoundComboBox getHistoryDropdown(){
    return _historyDropdown;
  }
  
  private JTextField getStartField(){
    return _startField;
  }
  
  private JTextField getEndField(){
    return _endField;
  }
}
