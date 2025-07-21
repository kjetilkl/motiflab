/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.operations.PromptConstraints;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SimpleDataPanelIcon;


/**
 *
 * @author kjetikl
 */
public class Prompt_NumericVariable extends Prompt {
    
    private NumericVariable data;
    private JPanel mainpanel;
    private JLabel errorMessageLabel;
    private JComponent valueComponent; 
    private PromptConstraints constraints=null;
    private JPanel internPanel;
    private JLabel valueLabel;

    
    public Prompt_NumericVariable(MotifLabGUI gui, String prompt, NumericVariable dataitem) {
        this(gui,prompt,dataitem,true);
    }
    
    public Prompt_NumericVariable(MotifLabGUI gui, String prompt, NumericVariable dataitem, boolean modal) {
        super(gui,prompt, modal);
        if (dataitem!=null) {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new NumericVariable(gui.getGenericDataitemName(NumericVariable.class, null), 0);
        setDataItemName(data.getName());
        setTitle("Numeric Variable");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.NUMERIC_VARIABLE_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
        setDataItemIcon(icon, true);
        valueComponent=new JTextField(15);
        ((JTextField)valueComponent).setText(data.getValue().toString());
        mainpanel=new JPanel();
        mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
        errorMessageLabel=new JLabel(" "); // This label is used to display potential error messages not the prompt message!
        errorMessageLabel.setFont(errorMessageFont);
        errorMessageLabel.setForeground(java.awt.Color.RED);
        Dimension emlDim=new Dimension(200,30);
        errorMessageLabel.setPreferredSize(emlDim);
        errorMessageLabel.setMaximumSize(emlDim);
        errorMessageLabel.setMinimumSize(emlDim);
        internPanel=new JPanel();
        internPanel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        valueLabel=new JLabel("Value");
        internPanel.add(valueLabel);
        internPanel.add(valueComponent);
        //internPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 0, 5, 5));        
        mainpanel.add(internPanel);
        mainpanel.add(errorMessageLabel);
        Dimension dim1=getDataNameLabelDimensions();
        valueLabel.setPreferredSize(dim1);
        valueLabel.setMaximumSize(dim1);
        valueLabel.setMinimumSize(dim1);       
        JPanel wrapper=new JPanel();
        wrapper.setLayout(new FlowLayout(FlowLayout.LEADING));
        wrapper.add(mainpanel);
        setMainPanel(wrapper);       
        pack();
        if (dataitem!=null) focusOKButton();
    }
    
    
    @Override
    public boolean onOKPressed() {
        if (!(valueComponent instanceof JTextField)) {
                 if (valueComponent instanceof JComboBox) data.setValue(((Number) ((JComboBox)valueComponent).getSelectedItem()).doubleValue());
            else if (valueComponent instanceof JSliderWrapper)   data.setValue(((Number) ((JSliderWrapper)valueComponent).getValue()).doubleValue());
            else if (valueComponent instanceof JSpinner)  data.setValue(((Number) ((JSpinner)valueComponent).getValue()).doubleValue());
            else if (valueComponent instanceof JListWrapper) {
                Number value=((JListWrapper)valueComponent).getValue();
                if (value!=null) data.setValue(value.doubleValue());
            }
        } else {
            try {
                JTextField valueTextField=(JTextField)valueComponent;
                double value=Double.parseDouble(valueTextField.getText().trim());
                if (constraints!=null) {
                    String msg=constraints.isValueAllowed(value,engine);
                    if (msg!=null) {errorMessageLabel.setText(msg);return false;}
                }
                data.setValue(value);
            } catch (NumberFormatException e) {
                errorMessageLabel.setText("Please enter a valid numeric value");
                return false;
            } 
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);        
        return true;
    }

    @Override
    public Data getData() {
       return data; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof NumericVariable) data=(NumericVariable)newdata; 
    }     
    
    @Override
    public void setConstraints(PromptConstraints promptconstraint) {
        constraints=promptconstraint;
        if (valueComponent!=null) internPanel.remove(valueComponent); 
        String type=null;
        if (constraints!=null) {
            type=(String)promptconstraint.getValue(PromptConstraints.WIDGET); // any preferred widgets?
            if (type==null) {
               Object allowedValues=constraints.getValue(PromptConstraints.ALLOWED_VALUES);                
               if (allowedValues!=null) type=PromptConstraints.MENU; // explicit list of allowed values provided
               else type=PromptConstraints.SPINNER;
            }
        }    
        valueComponent=getValueComponent(type,data.getValue());        
        internPanel.add(valueComponent);
        pack();
    }       
    
    
    private JComponent getValueComponent(String type, double value) {
        JComponent vcomponent=null;
        if (type==null) type="";
        if (type.equalsIgnoreCase(PromptConstraints.MENU)) {
             Object values=constraints.getValue(PromptConstraints.ALLOWED_VALUES);
             if (values instanceof Object[]) values=resolveAllowedValues((Object[])values);
             if (!(values instanceof Number[])) values=setupAllowedValuesFromRange();
             if (values instanceof Number[]) {
                 vcomponent=new JComboBox((Number[])values);
                 if (values instanceof Integer[]) ((JComboBox)vcomponent).setSelectedItem((int)value);
                 else ((JComboBox)vcomponent).setSelectedItem(value);                             
             } 
        } else if (type.equalsIgnoreCase(PromptConstraints.LIST)) {
             Object values=constraints.getValue(PromptConstraints.ALLOWED_VALUES);
             if (values instanceof Object[]) values=resolveAllowedValues((Object[])values);
             if (!(values instanceof Number[])) values=setupAllowedValuesFromRange();
             if (values instanceof Number[]) {
                 vcomponent=new JListWrapper((Number[])values, value);
             }                  
        } else if (type.equalsIgnoreCase(PromptConstraints.SLIDER)) {
            Object parameters=getValueMinMaxStep(value);
            int min=0;
            int max=0;
            int step=1;
            if (parameters instanceof int[]) {
               min=((int[])parameters)[1];
               max=((int[])parameters)[2];
               step=((int[])parameters)[3];
            } else {
               min=(int)((double[])parameters)[1];
               max=(int)((double[])parameters)[2];            
               step=(int)((double[])parameters)[3];            
            }             
            if (value<min) value=min;
            if (value>max) value=max;
            vcomponent=new JSliderWrapper((int)value, min, max, step);
        } else if (type.equalsIgnoreCase(PromptConstraints.SPINNER)) { 
            Object parameters=getValueMinMaxStep(value);
            if (parameters instanceof int[]) {
               int[]par=(int[])parameters; 
               if (par[0]<par[1]) par[0]=par[1]; // current value was less than minimum. Set it to minimum
               if (par[0]>par[2]) par[0]=par[2]; // current value was greater than minimum. Set it to maximum
               vcomponent=new JSpinner(new SpinnerNumberModel(par[0], par[1], par[2], par[3])); 
            } else {
               double[]par=(double[])parameters; 
               if (par[0]<par[1]) par[0]=par[1]; // current value was less than minimum. Set it to minimum
               if (par[0]>par[2]) par[0]=par[2]; // current value was greater than minimum. Set it to maximum               
               vcomponent=new JSpinner(new SpinnerNumberModel(par[0], par[1], par[2], par[3]));                
            }    
            Dimension dim=new Dimension(100,29);
            vcomponent.setPreferredSize(dim);
            vcomponent.setMinimumSize(dim);
            vcomponent.setMaximumSize(dim);
        } 
        if (vcomponent==null) {// return regular textfield if not set above
           vcomponent=new JTextField(15);
           ((JTextField)vcomponent).setText(""+value);
        }
        return vcomponent;        
    }

    /** Takes a list containing a mix of numbers and data references and returns a list of numbers */
    private Double[] resolveAllowedValues(Object[] values) {
        ArrayList<Double> list=new ArrayList<>(values.length);
        for (Object value:values) {
            if (value instanceof Number) list.add(((Number)value).doubleValue());
            else if (value instanceof String) {
                NumericVariable var=(NumericVariable)engine.getDataItem((String)value,NumericVariable.class);
                if (var!=null) list.add(var.getValue());
                else gui.logMessage("Warning: '"+((String)value)+"' is not a Numeric Variable. Value is ignored in prompt");
            }
        }
        Double[] newlist=new Double[list.size()];
        return list.toArray(newlist);
    }
    
    /** Returns an array of numbers containing allowed values as defined by a range */
    private Number[] setupAllowedValuesFromRange() {
        Object parameters=getValueMinMaxStep(0); // dummy value used here just to get the min/max/step array      
        if (parameters instanceof int[]) {
           int min=((int[])parameters)[1];
           int max=((int[])parameters)[2];
           int step=((int[])parameters)[3];
           ArrayList<Integer> list=new ArrayList<>();
           for (int i=min;i<=max;i+=step) {
               list.add(i);
           }
           Integer[] array=new Integer[list.size()];
           return list.toArray(array);
        } else {
           double min=((double[])parameters)[1];
           double max=((double[])parameters)[2];            
           double step=((double[])parameters)[3];    
           ArrayList<Double> list=new ArrayList<>();
           double value=min;
           int index=0;
           while (value<=max) {
               value=min+(step*index);
               list.add(value); 
               index++;
           }
           Double[] array=new Double[list.size()];
           return list.toArray(array);           
        }         
    }
    
    private Object getValueMinMaxStep(double value) {       
        Object minValue=constraints.getValue(PromptConstraints.MIN_VALUE);
        Object maxValue=constraints.getValue(PromptConstraints.MAX_VALUE);
        Object stepValue=constraints.getValue(PromptConstraints.STEP_VALUE);
        
        if (minValue instanceof String) { // this could be a Numeric Variable. If so replace with the value of the variable
            NumericVariable numvar=(NumericVariable)engine.getDataItem((String)minValue, NumericVariable.class);
            if (numvar!=null) {
                double doublevalue=numvar.getValue();
                if (doublevalue==Math.rint(doublevalue)) minValue=new Integer((int)Math.rint(doublevalue));
                else minValue=new Double(doublevalue);
            } else {
                gui.logMessage("Warning: '"+((String)minValue)+"' is not a Numeric Variable. Minimum limit is ignored in prompt");
            } 
        }
        if (maxValue instanceof String) { // this could be a Numeric Variable. If so replace with the value of the variable
            NumericVariable numvar=(NumericVariable)engine.getDataItem((String)maxValue, NumericVariable.class);
            if (numvar!=null) {
                double doublevalue=numvar.getValue();
                if (doublevalue==Math.rint(doublevalue)) maxValue=new Integer((int)Math.rint(doublevalue));
                else maxValue=new Double(doublevalue);
            } else {
                gui.logMessage("Warning: '"+((String)maxValue)+"' is not a Numeric Variable. Maximum limit is ignored in prompt");
            } 
        }
        if (stepValue instanceof String) { // this could be a Numeric Variable. If so replace with the value of the variable
            NumericVariable numvar=(NumericVariable)engine.getDataItem((String)stepValue, NumericVariable.class);
            if (numvar!=null) {
                double doublevalue=numvar.getValue();
                if (doublevalue==Math.rint(doublevalue)) stepValue=new Integer((int)Math.rint(doublevalue));
                else stepValue=new Double(doublevalue);
            } else {
                gui.logMessage("Warning: '"+((String)stepValue)+"' is not a Numeric Variable. Step value is ignored in prompt");
            } 
        }        
        
        if (minValue instanceof Double || maxValue instanceof Double || stepValue instanceof Double) { // at least one double value. Treat all as double
            double min=(minValue instanceof Double)?((Double)minValue):-Double.MAX_VALUE;
            double max=(maxValue instanceof Double)?((Double)maxValue):Double.MAX_VALUE;
            double step=(stepValue instanceof Double)?((Double)stepValue):(new Double(1.0));
            if (step<=0) step=1.0;
            return new double[]{value, min, max, step};
        } else {
             // all should now be integer values
            int min=(minValue instanceof Integer)?((Integer)minValue):Integer.MIN_VALUE;
            int max=(maxValue instanceof Integer)?((Integer)maxValue):Integer.MAX_VALUE;
            int step=(stepValue instanceof Integer)?((Integer)stepValue):(new Integer(1));
            if (step<=0) step=1;
            return new int[]{(int)value, min, max, step};          
        }         
    }
    
    private class JSliderWrapper extends JPanel {
        JLabel label;
        JSlider slider;
        
        public JSliderWrapper(int value, int min, int max, int step) {
            label=new JLabel(" ");
            Dimension dim=new Dimension(50, 28);
            label.setMinimumSize(dim);
            label.setPreferredSize(dim);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            slider=new JSlider(min, max); 
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    Object source=e.getSource();
                    if (source instanceof JSlider) {
                        label.setText(""+((JSlider)source).getValue());
                    }
                }
            }); 
            slider.setValue(value);
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(slider);
            add(label);
        }
        
        public double getValue() {
            return (double)slider.getValue();
        }
    }
    
    private class JListWrapper extends JPanel {
        JList list;
        
        public JListWrapper(Number[] values, Number value) {
             list=new JList((Number[])values);
             list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  
             if (values instanceof Integer[])  list.setSelectedValue(value.intValue(), true);
             else list.setSelectedValue(value.doubleValue(), true);
             this.setLayout(new BorderLayout());
             JScrollPane scroller=new JScrollPane(list);
             Dimension dim=new Dimension(getDataNameFieldDimensions().width,80);
             scroller.setPreferredSize(dim);
             scroller.setMinimumSize(dim);
             scroller.setMaximumSize(dim);
             this.add(scroller);
             list.addMouseListener(new java.awt.event.MouseAdapter() {
                 @Override
                 public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount()==2) {
                        getOKbutton().doClick();
                    }
                 } 
             });             
        }
        
        public Number getValue() {
            Number value=(Number)list.getSelectedValue();
            if (list.getModel().getSize()==0) return 0;
            if (value==null) list.getModel().getElementAt(0); // no selection has been made. Just use the first value
            return value;
        }
    }
}
