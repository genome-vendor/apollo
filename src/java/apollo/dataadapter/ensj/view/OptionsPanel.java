package apollo.dataadapter.ensj.view;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import apollo.util.GuiUtil;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

public class OptionsPanel extends JPanel{
  
  private View _view;
  public static final String S_AND_S_RESET_LABEL = "Start+Stop reset";
  public static final String AGGRESSIVE_NAMING_LABEL = "Aggressive naming";
  public static final String ADD_RESULT_GENES_LABEL = "Add result genes as annotations";
  public static final String ADD_TRANSCRIPT_SUPPORT_LABEL = "Add support to transcripts";
  public static final String TYPE_PREFIX_LABEL = "Prefix to add to types";
  
  private JCheckBox _ssResetCheckBox = new JCheckBox(S_AND_S_RESET_LABEL);
  private JCheckBox _namingCheckBox = new JCheckBox(AGGRESSIVE_NAMING_LABEL);
  private JCheckBox _geneAddCheckBox = new JCheckBox(ADD_RESULT_GENES_LABEL);
  private JCheckBox _supportAddCheckBox = new JCheckBox(ADD_TRANSCRIPT_SUPPORT_LABEL);

  private JLabel _typePrefixLabel = new JLabel(TYPE_PREFIX_LABEL);
  private JTextField _typePrefixTextField = new JTextField(20);


  public OptionsPanel(View view){
    _view = view;
    initialiseGUI();
  }

  private View getView(){
    return _view;
  }

  public void initialiseGUI(){
    setLayout(new java.awt.GridBagLayout());
    Dimension fieldSize = new Dimension(400,25);

    
    add(getSSResetCheckBox(), GuiUtil.makeConstraintAt(0,0, 10, true));
    add(getNamingCheckBox(), GuiUtil.makeConstraintAt(0,1, 10, true));
    add(getGeneAddCheckBox(), GuiUtil.makeConstraintAt(0,2, 10, true));
    add(getSupportAddCheckBox(), GuiUtil.makeConstraintAt(0,3, 10, true));
    add(getTypePrefixLabel(), GuiUtil.makeConstraintAt(0,4,1));
    add(getTypePrefixTextField(), GuiUtil.makeConstraintAt(1,4,3));
    getTypePrefixTextField().setPreferredSize(fieldSize);
    getTypePrefixTextField().setMinimumSize(fieldSize);
  }

  public void update(Model model){
    OptionsModel myModel = model.getOptionsModel();
    

    myModel.setResetGeneStartAndStop(getCheckBoxString(getSSResetCheckBox()));

    myModel.setAggressiveGeneNaming(getCheckBoxString(getNamingCheckBox()));

    myModel.setAddResultGenesAsAnnotations(getCheckBoxString(getGeneAddCheckBox()));

    myModel.setAddSupportToTranscripts(getCheckBoxString(getSupportAddCheckBox()));

    myModel.setTypePrefix(getTypePrefixTextField().getText());
  }

  public void read(Model model){
    OptionsModel myModel = model.getOptionsModel();

    setCheckBoxFromString(getSSResetCheckBox(), myModel.resetGeneStartAndStop());
    getSSResetCheckBox().setText(S_AND_S_RESET_LABEL);

    setCheckBoxFromString(getNamingCheckBox(), myModel.aggressiveGeneNaming());
    getNamingCheckBox().setText(AGGRESSIVE_NAMING_LABEL);

    setCheckBoxFromString(getGeneAddCheckBox(), myModel.addResultGenesAsAnnotations());
    getGeneAddCheckBox().setText(ADD_RESULT_GENES_LABEL);

    setCheckBoxFromString(getSupportAddCheckBox(), myModel.addSupportToTranscripts());
    getSupportAddCheckBox().setText(ADD_TRANSCRIPT_SUPPORT_LABEL);

    getTypePrefixTextField().setText(myModel.typePrefix());

  }
  
  private String getCheckBoxString(JCheckBox box){
    if(box.isSelected()){
      return Boolean.TRUE.toString();
    }else{
      return Boolean.FALSE.toString();
    }
  }

  private void setCheckBoxFromString(JCheckBox box, String value){
    if(Boolean.TRUE.toString().equals(value)){
      box.setSelected(true);
    }else{
      box.setSelected(false);
    }
  }

  private JCheckBox getSSResetCheckBox() {
    return _ssResetCheckBox;
  }

  private JCheckBox getNamingCheckBox() {
    return _namingCheckBox;
  }

  private JCheckBox getGeneAddCheckBox() {
    return _geneAddCheckBox;
  }

  private JCheckBox getSupportAddCheckBox() {
    return _supportAddCheckBox;
  }

  private JTextField getTypePrefixTextField() {
    return _typePrefixTextField;
  }
  private JLabel getTypePrefixLabel() {
    return _typePrefixLabel;
  }
}
