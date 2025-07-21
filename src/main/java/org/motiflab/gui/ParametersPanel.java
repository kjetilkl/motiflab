/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.motiflab.engine.ParameterCondition;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.TextMap;
import org.motiflab.gui.operationdialog.OperationDialog;

/**
 *
 * @author kjetikl
 */
public class ParametersPanel extends JPanel {
    private HashMap<String,JComponent> parameterWidgets; // this stores the input widgets for all parameters
    private Parameter[] parameterTemplates;
    private ParameterSettings parameters; 
    private Font smallfont=new Font(Font.SERIF,Font.PLAIN,10);
    private String filterType=null; // can be used to hide some parameters based on a filter-type (e.g. "input" or "output"-parameters)
    private boolean advancedParametersVisible=false;
    private JLabel showAdvancedLabel=null;
    private Object[][] components=null; // same size as the number of rows and 5 columns. First index [0] is a boolean=explicitly hidden, second [1] is a boolean=isAdvanced, the three next are gui widget components of the row: name label [2], the widget itself [3] and a panel with extra stuff [4] ("required" label and help icon)
    private Color advancedParametersColor=new Color(100,100,160);
    private HashMap<String,Integer> panelMap=new HashMap<String, Integer>(); // maps name of Parameter to index in the components[] list
    private WidgetActionListener widgetListener;
    private HashMap<String,ArrayList<ParameterCondition>> conditions=null; // a list of conditions for each parameter that is being monitored. The key is the name of the monitored parameter
    private boolean showAdvancedParameters=false;
    private ActionListener externalActionListener=null;
    
    Icon informationIcon=null;
    OperationDialog dialog=null;
    MotifLabEngine engine=null;

   /** */
    public ParametersPanel(Parameter[] parameterTemplates, ParameterSettings parameterValues, OperationDialog dialog) {
        this(parameterTemplates, parameterValues, dialog, null, null);
    }
    public ParametersPanel(Parameter[] parameterTemplates, ParameterSettings parameterValues, MotifLabEngine engine) {
        this(parameterTemplates, parameterValues, null, null, engine);
    }

    /** */
    public ParametersPanel(Parameter[] parameterTemplates, ParameterSettings parameterValues, OperationDialog dialog, String filter, MotifLabEngine engine) {
        this.parameterTemplates=parameterTemplates;
        this.parameters=parameterValues;
        this.dialog=dialog;
        this.filterType=filter;
        this.engine=engine;
        if (this.engine==null) this.engine=MotifLabEngine.getEngine(); // ad-hoc workaround
        if (dialog!=null) this.informationIcon=dialog.getInformationIcon();
        else {
            java.net.URL iconURL=getClass().getResource("/org/motiflab/gui/resources/icons/help_icon.png");
            if (iconURL!=null) informationIcon=new javax.swing.ImageIcon(iconURL);
        }
        parameterWidgets=new HashMap<String, JComponent>();
        this.setBorder(BorderFactory.createEmptyBorder(0,40,0,0));
        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.ipadx=10;
        constraints.ipady=5;
        widgetListener=new WidgetActionListener();
        int visibleParameters=parameterTemplates.length;
        if (parameterTemplates==null || visibleParameters==0) return;
        this.components=new Object[visibleParameters][5]; // 5 = 2 boolean flags (currently shown+advanced) + 3 widget columns in this panel
        int row=-1;
        for (Parameter par:parameterTemplates) {            
            row++;
            boolean alwaysHidden=false;
            if (par.isHidden()) alwaysHidden=true; // newer show hidden parameters
            if (filterType!=null && par.getFilterType()!=null && !filterType.equalsIgnoreCase(par.getFilterType())) alwaysHidden=true; // hide this parameter because it is not applicable for this panel            
            components[row][0]=alwaysHidden;
            boolean isAdvanced=par.isAdvanced();
            components[row][1]=isAdvanced;
            constraints.gridy=row;
            constraints.gridx=0;
            String parametername=par.getName();
            panelMap.put(parametername,row);
            JLabel namelabel=new JLabel(parametername);
            if (isAdvanced) namelabel.setForeground(advancedParametersColor);
            JLabel helplabel;
            String description=par.getDescription();
            if (description!=null) {
                helplabel=new JLabel(informationIcon);
                if (description.startsWith("<html>")) helplabel.setToolTipText(description);
                else helplabel.setToolTipText(MotifLabGUI.formatTooltipString(description, 100));
            } else helplabel=new JLabel("");
            this.add(namelabel,constraints);
            components[row][2]=namelabel;
            Object userSelected=null;
            if (parameters!=null) userSelected=parameters.getParameterAsString(parametername,parameterTemplates);
            else userSelected=par.getDefaultValue();
            JComponent newcomponent=getWidgetFor(par,userSelected);
            parameterWidgets.put(parametername,newcomponent);
            constraints.fill=GridBagConstraints.HORIZONTAL;
            constraints.gridx=1;
            this.add(newcomponent,constraints);
            components[row][3]=newcomponent;
            constraints.fill=GridBagConstraints.NONE;
            JPanel additionals=new JPanel(new FlowLayout(FlowLayout.LEADING));
            additionals.add(helplabel);
            if (par.isRequired()) {
                JLabel isRequiredLabel=new JLabel("Required");
                isRequiredLabel.setForeground(java.awt.Color.RED);
                isRequiredLabel.setFont(smallfont);
                additionals.add(isRequiredLabel);
            } else additionals.add(new JLabel(" "));
            constraints.gridx=2;     
            this.add(additionals,constraints);
            components[row][4]=additionals;  
            if (alwaysHidden) {
                ((JComponent)components[row][2]).setVisible(false);
                ((JComponent)components[row][3]).setVisible(false);
                ((JComponent)components[row][4]).setVisible(false);    
            }
            if (par.hasConditions()) {
                for (ParameterCondition condition:par.getConditions()) {
                    String monitored=condition.getMonitoredParameter();
                    if (conditions==null) conditions=new HashMap<String, ArrayList<ParameterCondition>>();
                    if (!conditions.containsKey(monitored)) conditions.put(monitored, new ArrayList<ParameterCondition>());
                    conditions.get(monitored).add(condition);
                }
            }
        } // end for each parameter
        if (hasAdvancedParameters()) {
           constraints.gridy=row+1;
           constraints.gridx=0;
           constraints.gridwidth=3;
           constraints.ipady=15;
           showAdvancedLabel=new JLabel("Show advanced parameters...");
           showAdvancedLabel.setForeground(Color.BLUE);
           showAdvancedLabel.setIcon(new MiscIcons(MiscIcons.BOX_WITH_PLUS));
           showAdvancedLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
           showAdvancedLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    advancedParametersVisible=!advancedParametersVisible; // toggle visibility setting
                    setVisibilityOnAdvancedParameters(advancedParametersVisible);
                    if (advancedParametersVisible) {
                        showAdvancedLabel.setText("Hide advanced parameters...");
                        ((MiscIcons)showAdvancedLabel.getIcon()).setIconType(MiscIcons.BOX_WITH_MINUS);
                    } else {
                        showAdvancedLabel.setText("Show advanced parameters...");
                        ((MiscIcons)showAdvancedLabel.getIcon()).setIconType(MiscIcons.BOX_WITH_PLUS);
                    }
                }
           });
           this.add(showAdvancedLabel,constraints);
           setVisibilityOnAdvancedParameters(advancedParametersVisible);
        }
        initalizeBasedOnConditions();
    }
    
    /**
     * updates the panels based on initial conditions
     */
    private void initalizeBasedOnConditions() {
        for (JComponent comp:parameterWidgets.values()) {
            parameterUpdated(comp);            
        }
    }

    /**
     * Changes the visibility of advanced parameters.
     * Note that parameters that have been explicitly marked as hidden
     * (for example with the setVisibilityOnParameter() method)
     * will not be affected by this call.
     * @param visible 
     */
    private void setVisibilityOnAdvancedParameters(boolean visible) {
        showAdvancedParameters=visible;
        for (int i=0;i<components.length;i++) {
            if (((Boolean)components[i][0]).booleanValue()) continue; // this parameter has been explicitly hidden and should not be shown
            if (((Boolean)components[i][1]).booleanValue()) { // if true then the parameter is advanced
                ((JComponent)components[i][2]).setVisible(visible);
                ((JComponent)components[i][3]).setVisible(visible);
                ((JComponent)components[i][4]).setVisible(visible);
            }
        }
        this.revalidate();
        this.repaint();
    }
    
    /**
     * Sets the visibility of the given parameter in the panel
     * Note that parameters that are explicitly marked as hidden can never be shown
     * Neither will parameters that do not pass the filter-setting of the panel
     * Advanced parameters will only be shown if this has been selected by the user
     * @param parameterName
     * @param visible 
     */
    public void setVisibilityOnParameter(Parameter parameter, boolean visible) {   
        if (parameter.isHidden()) return; // newer show hidden parameters
        if (filterType!=null && parameter.getFilterType()!=null && !filterType.equalsIgnoreCase(parameter.getFilterType())) return; // hide this parameter because it is not applicable for this panel            
        String parameterName = parameter.getName();           
        if (!panelMap.containsKey(parameterName)) return;
        int index=panelMap.get(parameterName);
        if (index>=0 && index<components.length) {
            components[index][0]=(!visible); // this marks that the parameter should be explicitly hidden
            if (parameter.isAdvanced() && !showAdvancedParameters) return;            
            ((JComponent)components[index][2]).setVisible(visible);
            ((JComponent)components[index][3]).setVisible(visible);
            ((JComponent)components[index][4]).setVisible(visible);            
        }
        this.revalidate();
        this.repaint();        
    }
    
    /** Copies the values of the widgets into the dialogs ParameterSettings object which can be retrieved with getParameterSettings() */
    public void setParameters() {
        if (parameters==null) parameters=new ParameterSettings();
        if (parameterTemplates!=null && parameterTemplates.length>0) {
            for (Parameter par:parameterTemplates) {
               Object value;
               String parametername=par.getName();
               Class parameterclass=par.getType();
//               Object factoryDefault=par.getDefaultValue();
//               if (par.isHidden() || (filterType!=null && par.getFilterType()!=null && !filterType.equalsIgnoreCase(par.getFilterType()))) { // this parameter is not displayed in this panel (ever)
//                   parameters.setParameter(parametername, factoryDefault);
//                   continue;
//               }
               JComponent widget=parameterWidgets.get(parametername);
               value=getValueFromWidget(widget, parameterclass);
               parameters.setParameter(parametername, value);
            } // end for each parameter
        } // end additional parameters > 0
    }
    
    public Object getValueFromWidget(JComponent widget, Class parameterclass) {
        Object value=null;
        if (parameterclass==Boolean.class) {
           if (((JCheckBox)widget).isSelected()) value=Boolean.TRUE; else value=Boolean.FALSE;
       } else if (Number.class.isAssignableFrom(parameterclass) || NumericMap.class.isAssignableFrom(parameterclass)) {
            if (widget instanceof JSpinner) {
                value=((JSpinner)widget).getValue();
            }
            else if (widget instanceof JComboBox) value=(String)((JComboBox)widget).getSelectedItem();
            else if (widget instanceof JTextField) value=((JTextField)widget).getText().trim();
            else if (widget instanceof JPasswordField) value=new String(((JPasswordField)widget).getPassword());
            else value=null; // this should preferably not happen
       } else { // value should be a string?
           if (widget instanceof JComboBox) value=(String)((JComboBox)widget).getSelectedItem();
           else if (widget instanceof JTextField) value=postprocess(((JTextField)widget).getText().trim());
           else if (widget instanceof JPasswordField) value=new String(((JPasswordField)widget).getPassword());
           else value=null; // this should preferably not happen
       }    
       return value;
    }
    
    public void setValueForParameterInWidget(Parameter parameter, String value) {
        JComponent widget=parameterWidgets.get(parameter.getName());
        if (widget instanceof JComboBox) {
            ((JComboBox)widget).setSelectedItem(value);
        } else if (widget instanceof JTextField) {
            ((JTextField)widget).setText(value);
        } else if (widget instanceof JPasswordField) {
            ((JPasswordField)widget).setText(value);
        } else if (widget instanceof JCheckBox) {
            boolean set=(value!=null && value.toLowerCase().equals("true"));
            ((JCheckBox)widget).setSelected(set);
        } else if (widget instanceof JSpinner) {
            try {
                Double val=Double.parseDouble(value);
                ((JSpinner)widget).setValue(val);
            } catch (Exception e) {}
        }
    }
    
    public Parameter getParameterForName(String parameterName) {
        for (Parameter par:parameterTemplates) {
            if (par.getName().equals(parameterName)) return par;
        }          
        return null;
    }
    
    /**
     * Returns the parameter settings object associated with this panel
     * Note that the method 'setParameters()' must be called first in order
     * to update the parameter settings with the current values in the panel
     * @return
     */
    public ParameterSettings getParameterSettings() {
        return parameters;
    }
    
    /**
     * Returns a new GUI widget to represent the parameter in the panel
     * @param parameter
     * @param userSelected
     * @return 
     */
    private JComponent getWidgetFor(Parameter parameter, Object userSelected) {
        Class type=parameter.getType();
        Object factoryDefaultValue=parameter.getDefaultValue();
        Object values=parameter.getAllowedValues();
        boolean optional=parameter.isOptional();
        if (type==Integer.class && dialog!=null) {
            DefaultComboBoxModel boxmodel;
            boxmodel=dialog.getDataCandidates(NumericVariable.class);
            JComboBox box=new JComboBox(boxmodel);
            box.setEditable(true);
            if (userSelected==null) userSelected=factoryDefaultValue;
            box.setSelectedItem(userSelected);
            box.addActionListener(widgetListener);
            return box; 
        } else if (type==Double.class && dialog!=null) {
            DefaultComboBoxModel boxmodel;
            boxmodel=dialog.getDataCandidates(NumericVariable.class);
            JComboBox box=new JComboBox(boxmodel);
            box.setEditable(true);
            if (userSelected==null) userSelected=factoryDefaultValue;
            box.setSelectedItem(userSelected);
            box.addActionListener(widgetListener);
            return box;      
        } else if (NumericMap.class.isAssignableFrom(type) && dialog!=null) {
            DefaultComboBoxModel boxmodel;
            if (values instanceof Class) boxmodel=dialog.getDataCandidates((Class)values,optional);
            else if (values instanceof Class[]) boxmodel=dialog.getDataCandidates((Class[])values,optional);
            else boxmodel=dialog.getDataCandidates(type,optional);
            JComboBox box=new JComboBox(boxmodel);
            box.setEditable(contains(values,NumericVariable.class));
            if (inBox(boxmodel,userSelected)) box.setSelectedItem(userSelected);
            else if (inBox(boxmodel,factoryDefaultValue)) box.setSelectedItem(factoryDefaultValue);
            else if (boxmodel.getSize()>0) box.setSelectedIndex(0);
            box.addActionListener(widgetListener);
            return box;     
        } else if (TextMap.class.isAssignableFrom(type) && dialog!=null) {
            DefaultComboBoxModel boxmodel;
            if (values instanceof Class) boxmodel=dialog.getDataCandidates((Class)values,optional);
            else if (values instanceof Class[]) boxmodel=dialog.getDataCandidates((Class[])values,optional);
            else boxmodel=dialog.getDataCandidates(type,optional);
            JComboBox box=new JComboBox(boxmodel);
            //box.setEditable(true);
            if (inBox(boxmodel,userSelected)) box.setSelectedItem(userSelected);
            else if (inBox(boxmodel,factoryDefaultValue)) box.setSelectedItem(factoryDefaultValue);
            else if (boxmodel.getSize()>0) box.setSelectedIndex(0);
            box.addActionListener(widgetListener);
            return box;     
        } else if (type==Integer.class && dialog==null) {
            if (userSelected==null) userSelected=factoryDefaultValue;
            int selected=1;
            if (userSelected!=null) {
                if (userSelected instanceof Integer) selected=(Integer)userSelected;
                else if (userSelected instanceof String) {
                    try {selected=Integer.parseInt((String)userSelected);} catch (NumberFormatException e) {}
                }
            }
            int step=1;
            int min=Integer.MIN_VALUE;
            int max=Integer.MAX_VALUE;
            if (values!=null && values instanceof Integer[]) {
                min=((Integer[])values)[0];
                max=((Integer[])values)[1];
            }            
            JSpinner widget=new JSpinner(new SpinnerNumberModel(selected, min, max, step));
            widget.addChangeListener(widgetListener);
            return widget; 
        } if ((type==Double.class || NumericMap.class.isAssignableFrom(type)) && dialog==null) {
            if (userSelected==null) userSelected=factoryDefaultValue;
            double selected=1;
            if (userSelected!=null) {
                if (userSelected instanceof Double) selected=(Double)userSelected;
                else if (userSelected instanceof String) {
                    try {selected=Double.parseDouble((String)userSelected);} catch (NumberFormatException e) {}
                }
            }
            double step=0.01f;
            double min=-Double.MAX_VALUE;
            double max=Double.MAX_VALUE;
            if (values!=null && values instanceof Double[]) {
                min=((Double[])values)[0];
                max=((Double[])values)[1];
            }            
            JSpinner widget=new JSpinner(new SpinnerNumberModel(selected, min, max, step));
            widget.addChangeListener(widgetListener);
            return widget; 
        } else if (type==Boolean.class) {
            boolean isSel=false;
            if (userSelected!=null && userSelected instanceof Boolean) isSel=((Boolean)userSelected).booleanValue();
            else if (userSelected!=null && userSelected instanceof String) isSel=Boolean.parseBoolean((String)userSelected);
            else isSel=(factoryDefaultValue!=null)?((Boolean)factoryDefaultValue).booleanValue():false;
            JCheckBox box=new JCheckBox("", isSel);
            box.addActionListener(widgetListener);
            return box;
        } else if (values!=null && (values instanceof Class || values instanceof Class[])) {
            DefaultComboBoxModel boxmodel;
            if (dialog!=null) {
                if (values instanceof Class) boxmodel=dialog.getDataCandidates((Class)values,optional);
                else boxmodel=dialog.getDataCandidates((Class[])values,optional);
            } else {
               ArrayList<String> itemslist;
               if (values instanceof Class) itemslist=engine.getNamesForAllDataItemsOfType((Class)values);
               else itemslist=engine.getNamesForAllDataItemsOfTypes((Class[])values);
               if (optional) itemslist.add(0,""); // create a blank entry at the start of the list
               String[] items=new String[itemslist.size()];
               items=itemslist.toArray(items);
               boxmodel=new DefaultComboBoxModel(items);
            }
            JComboBox box=new JComboBox(boxmodel);
            if (inBox(boxmodel,userSelected)) box.setSelectedItem(userSelected);
            else if (inBox(boxmodel,factoryDefaultValue)) box.setSelectedItem(factoryDefaultValue);
            else if (boxmodel.getSize()>0) box.setSelectedIndex(0);
            box.addActionListener(widgetListener);
            return box;           
        } else { // I don't know what values this parameter takes. Use strings or drop-down boxes
            if (values==null || !(values instanceof Object[])) {
                JTextField textfield=new JTextField(16);
                if (parameter.hasAttributeValue("ui","password")) textfield=new JPasswordField(16); //                
                if (userSelected!=null) {
                    textfield.setText(preprocess(userSelected.toString()));
                }
                else if (factoryDefaultValue!=null) textfield.setText(factoryDefaultValue.toString());
                else textfield.setText("");
                textfield.setCaretPosition(0);
                textfield.addActionListener(widgetListener);
                textfield.addFocusListener(widgetListener);
                return textfield;
            } else {
               JComboBox box=new JComboBox((Object[])values);
               if (inArray((Object[])values,userSelected)) box.setSelectedItem(userSelected);
               else if (inBox(box.getModel(),factoryDefaultValue)) box.setSelectedItem(factoryDefaultValue);
               else if (box.getModel().getSize()>0) box.setSelectedIndex(0);
               box.addActionListener(widgetListener);
               return box;  
            }
        }
    }
    
    
    private boolean contains(Object list, Object element) {
        if (list instanceof Object[]) return inArray((Object[])list,element);
        else return list.equals(element);
    }
    
    private boolean inArray(Object[] list, Object element) {
        for (Object e:list) {if (e.equals(element)) return true;}
        return false;
    }
    
    /** Returns true if the specified element is present in the ComboBoxModel*/
    private boolean inBox(ComboBoxModel model, Object element) {
        if (element==null) return false;
        int size=model.getSize();
        for (int i=0;i<size;i++) {
            Object item=model.getElementAt(i);
            if (item.equals(element)) return true;
        }
        return false;
    }

    private boolean hasAdvancedParameters() {
        for (Parameter param:parameterTemplates) {
            if (filterType!=null && param.getFilterType()!=null && !filterType.equalsIgnoreCase(param.getFilterType())) continue; // ignore this parameter
            if (param.isAdvanced()) return true;
        }
        return false;
    }
    
    /** Performs necessary escaping of special characters before displaying strings in textfields */
    private String preprocess(String string) {
        //return string.replace("\t", "\\t");
        return string; // The new version only uses strings with special characters escaped. The conversion is now the responsibility of the class using the parameter value
    }
    
    /** Performs necessary unescaping of special characters in string retrieved from textfields */    
    private String postprocess(String string) {        
        // return string.replace("\\t", "\t");
        return string; // The new version only uses strings with special characters escaped. The conversion is now the responsibility of the class using the parameter value
    }
    
    /* This method should be called every time the value of a parameter is updated in order to update dependent parameters */
    private void parameterUpdated(JComponent source) {
        if (conditions==null) return; // nothing to do        
        String parameterName=null;       
        for (Entry<String,JComponent> entry : parameterWidgets.entrySet()) {
            if (source.equals(entry.getValue())) {
                parameterName=entry.getKey();
                break;
            }
        }    
        ArrayList<ParameterCondition> list=conditions.get(parameterName); // conditions monitoring this parameter's value
        if (list!=null && !list.isEmpty()) {
            Parameter targetParameter=getParameterForName(parameterName);
            Class parameterclass=targetParameter.getType();    
            Object value=getValueFromWidget(source, parameterclass);
            Class selectedClass=null;
            if (Data.class.isAssignableFrom(parameterclass)) { // parameter refers to a data object
                if (value instanceof String && ((String)value).isEmpty()) value=null;
                else {
                   selectedClass=getTypeForDataItem((String)value); 
                   // if selectedClass is null then no known data item was selected
                }
            } else if (parameterclass==Boolean.class) {
                selectedClass=parameterclass;
            } else if (parameterclass==Double.class) {
                if (value instanceof String) { // this could be the name of a Numeric Variable or a literal number written as text
                    if (((String)value).isEmpty()) value=null;
                    else {
                       selectedClass=getTypeForDataItem((String)value);
                       if (selectedClass==null) { // not a data object. Check if it is a number
                           try {
                               double number=Double.parseDouble((String)value);
                               value=new Double(number);
                               selectedClass=Double.class;
                           } catch (NumberFormatException e) {}
                       }
                    } // non-empty String                   
                } else selectedClass=parameterclass;
            } else if (parameterclass==Integer.class) {
                if (value instanceof String) { // this could be the name of a Numeric Variable or a literal number written as text
                    if (((String)value).isEmpty()) value=null;
                    else {
                       selectedClass=getTypeForDataItem((String)value);
                       if (selectedClass==null) { // not a data object. Check if it is a number
                           try {
                               int number=Integer.parseInt((String)value);
                               value=new Integer(number);
                               selectedClass=Integer.class;
                           } catch (NumberFormatException e) {}
                       }
                    } // non-empty String                   
                } else selectedClass=parameterclass;
            } else if (parameterclass==String.class) {
                if (value instanceof String && ((String)value).isEmpty()) value=null;
                selectedClass=parameterclass;
            } 
            
            for (ParameterCondition condition:list) { // conditions monitoring this parameter's value
               boolean satisfied=condition.isSatisfied(value, selectedClass, engine);
               Object[] action=condition.getAction(satisfied);
               if (action!=null) {              
                   String affectedParameterName=(String)action[0];
                   String actionCommand=(String)action[1];
                   Object newValue=action[2];
                   Parameter affected=getParameterForName(affectedParameterName);
                   if (actionCommand.equalsIgnoreCase("visibility")) {                      
                       Boolean show=(Boolean)newValue;                     
                       setVisibilityOnParameter(affected, show);
                   } else if (actionCommand.equalsIgnoreCase("setValue")) {
                       setValueForParameterInWidget(affected,(String)newValue);
                   } else if (actionCommand.equalsIgnoreCase("setToValueOf")) {
                       Parameter target=getParameterForName((String)newValue);
                       if (target!=null) {
                           JComponent pWidget=parameterWidgets.get(target.getName());
                           if (pWidget!=null) {
                               Object widgetValue=getValueFromWidget(pWidget, target.getClass());
                               if (widgetValue==null) widgetValue="";
                               setValueForParameterInWidget(affected,widgetValue.toString());
                           }
                       }
                   }
               }
            }
        }       
    }
    
    private class WidgetActionListener implements ActionListener, ChangeListener, FocusListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent source=(JComponent)e.getSource();
            parameterUpdated(source);   
            if (externalActionListener!=null) externalActionListener.actionPerformed(e);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JComponent source=(JComponent)e.getSource();
            parameterUpdated(source);
        }

        @Override
        public void focusGained(FocusEvent e) {}

        @Override
        public void focusLost(FocusEvent e) {
            JComponent source=(JComponent)e.getSource();
            parameterUpdated(source);
        }
    }
    
    private Class getTypeForDataItem(String dataname) {
        if (dialog!=null) {
            return dialog.getDataTypeTable().getClassFor(dataname);
        } else if (engine!=null) {
            return engine.getClassForDataItem(dataname);
        } else return null;
    }
    
    /**
     * Adds an external action listener to this panel. This listener will be notified
     * whenever an action event occurs on components that supports action listeners,
     * including JComboBoxes and JTextFields
     * @param listener 
     */
    public void addExternalActionListener(ActionListener listener) {
        externalActionListener=listener;
    }
}
