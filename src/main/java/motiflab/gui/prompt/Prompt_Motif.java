package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifClassification;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.protocol.ParseError;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.MotifClassDialog;
import motiflab.gui.MotifComparisonPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.MotifLogo;
import motiflab.gui.SaveMotifLogoImageDialog;
import motiflab.gui.SimpleDataPanelIcon;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class Prompt_Motif extends Prompt {
    private javax.swing.JLabel bindingFactorsLabel;
    private javax.swing.JScrollPane bindingFactorsScrollPane;
    private javax.swing.JTextArea bindingFactorsTextArea;
    private javax.swing.JTextArea classTextArea;
    private javax.swing.JLabel classLabel;
    private javax.swing.JLabel consensusLabel;
    private javax.swing.JTextField consensusTextField;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JComboBox partCombobox;
    private javax.swing.JLabel icLabel;
    private javax.swing.JLabel gcLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel logoLabel;
    private SequenceLogoPanel logoPanel;
    private StructurePanel structurePanel;
    private javax.swing.JTextField longNameTextField;
    private javax.swing.JLabel matrixLabel;
    private javax.swing.JScrollPane matrixScrollPane;
    private javax.swing.JTable matrixTable;
    private javax.swing.JLabel motifLongNameLabel;
    private javax.swing.JLabel motifShortNameLabel;
    private javax.swing.JTextArea organismTextArea;
    private javax.swing.JScrollPane organismScrollPane;
    private javax.swing.JLabel organismsLabel;
    private javax.swing.JTextArea expressionTextArea;
    private javax.swing.JScrollPane expressionScrollPane;
    private javax.swing.JTextArea descriptionTextArea;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JLabel expressionLabel;
    private javax.swing.JComboBox qualityCombobox;
    private javax.swing.JLabel partLabel;
    private javax.swing.JLabel qualityLabel;
    private javax.swing.JTextField shortNameTextField;
    private javax.swing.JLabel sizeLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel representationPanel;
    private javax.swing.JPanel factorsPanel;
    private javax.swing.JPanel expressionPanel;
    private GOEditorPanel GOPanel;   
    private javax.swing.JPanel descriptionPanel; 
    private javax.swing.JTabbedPane tabbedpane;
    private javax.swing.JCheckBox showreverse;
    private javax.swing.JCheckBox scalebyICBox;
    private javax.swing.JCheckBox sortbyFreqBox;   
    private JTable userproptable; 
    private VisualizationSettings settings=null;
    private String currentMotifClass=null;
    private TableModelListener tableModelListener=null;
    private JPanel commonpanel = null;
    private MotifListPanel interactionsPanel;
    private MotifListPanel alternativesPanel;
    private javax.swing.JPanel userPropertiesPanel;    
    private MotifComparisonPanel motifComparisonPanel;
    private JButton addPropertyButton;
    private NumberFormat percentageFormat;
    private NumberFormat decimalFormat;
    
    private Motif data;
    private final java.awt.Frame parenthack;
    private String oldConsensus=null;
    private boolean trackTableChanges=true; // this is used to "turn on or off" listening to updates in the matrix table
    
    public Prompt_Motif(MotifLabGUI gui, String prompt, Motif dataitem, VisualizationSettings settings) {
        this(gui,prompt,dataitem,settings,true);
    }    
    
    public Prompt_Motif(MotifLabGUI gui, String prompt, Motif dataitem, VisualizationSettings settings, boolean modal) {
        super(gui,prompt, modal);
        parenthack=gui.getFrame();
        this.settings=settings;
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new Motif(gui.getGenericDataitemName(Motif.class, null));
        setDataItemName(data.getName());
        setDataItemNameLabelText("Motif ID  ");
        SimpleDataPanelIcon motificon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        motificon.setForegroundColor(settings.getFeatureColor(data.getName()));
        setDataItemColorIcon(motificon);
        setTitle("Motif");
        mainPanel=new JPanel();
        JPanel insetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        insetPanel.add(mainPanel);
        initComponents();
        percentageFormat=NumberFormat.getPercentInstance();
        percentageFormat.setMinimumFractionDigits(1);
        percentageFormat.setMaximumFractionDigits(1);
        decimalFormat=new DecimalFormat("#.###");
        setMainPanel(new JScrollPane(insetPanel));
        DefaultTableModel model=null;
        if (data.getMatrix()==null) data.setMatrix(new double[][]{{0.25,0.25,0.25,0.25},{0.25,0.25,0.25,0.25},{0.25,0.25,0.25,0.25},{0.25,0.25,0.25,0.25}});
        model=new DefaultTableModel(convertMatrix(data.getMatrix()),new String[]{"A","C","G","T"});
        matrixTable.setModel(model);
        matrixTable.setFillsViewportHeight(true);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        matrixTable.setCellSelectionEnabled(true);
        matrixTable.getTableHeader().setReorderingAllowed(false);
        matrixTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        matrixScrollPane.setMaximumSize(new Dimension(1000,220));
        ExcelAdapter adapter=new ExcelAdapter(matrixTable,true, ExcelAdapter.CONVERT_TO_DOUBLE);
        sizeLabel.setText("Size: "+data.getLength()+" bp");
        icLabel.setText("IC: "+decimalFormat.format(data.getICcontent()));
        gcLabel.setText("GC: "+percentageFormat.format(data.getGCcontent()));
        shortNameTextField.setText(data.getShortName());
        shortNameTextField.setCaretPosition(0);
        longNameTextField.setText(data.getLongName());
        longNameTextField.setCaretPosition(0);
        consensusTextField.setText(data.getConsensusMotif());
        consensusTextField.setCaretPosition(0);
        descriptionTextArea.setText(data.getDescription());
        descriptionTextArea.setCaretPosition(0);        
        qualityCombobox.setSelectedItem(data.getQuality());
        currentMotifClass=data.getClassification();
        classTextArea.setText(MotifClassification.getFullLevelsString(currentMotifClass));
        String organismText=data.getOrganisms();
        if (organismText!=null) organismText=organismText.replace(',', '\n');
        organismTextArea.setText(organismText);
        organismTextArea.setCaretPosition(0);
        String expressionText=data.getTissueExpressionAsString();
        if (expressionText!=null) expressionText=expressionText.replace(',', '\n');
        expressionTextArea.setText(expressionText);
        expressionTextArea.setCaretPosition(0);
        String bindingFactorsText=data.getBindingFactors();
        if (bindingFactorsText!=null) bindingFactorsText=bindingFactorsText.replace(',', '\n');        
        bindingFactorsTextArea.setText(bindingFactorsText);
        bindingFactorsTextArea.setCaretPosition(0);
        partCombobox.setSelectedItem(data.getPart());
        oldConsensus=data.getConsensusMotif();        
        classTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {                 
                if (e.getClickCount()>=1) { // this used to be == 2 but I decided to change it to make it more obvious how to edit the property
                    MotifClassDialog dialog=new MotifClassDialog(parenthack,currentMotifClass);
                    dialog.setLocation(parenthack.getWidth()/2-dialog.getWidth()/2, parenthack.getHeight()/2-dialog.getHeight()/2);
                    dialog.setVisible(true);
                    if (dialog.isOKClicked()) {
                        currentMotifClass=dialog.getSelectedClass();
                        classTextArea.setText(MotifClassification.getFullLevelsString(currentMotifClass));
                    }
                }
            }
            }
        );
        tableModelListener=new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (!trackTableChanges) return;
                updateConsensusFromPWM(); // this will parse the tables and update the matrix in the data object as well as the IUPAC string
                logoPanel.setClearOnNextPaint();
                logoPanel.repaint(); // updates sequence logo
                structurePanel.dataUpdated();
            }
        };
        model.addTableModelListener(tableModelListener);
        consensusTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                onConsensusUpdate();
            }            
        });
        consensusTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onConsensusUpdate();
            }
        });
        matrixTable.getSelectionModel().addListSelectionListener(logoPanel);
        pack();  
        // check if the dialog window fits. If not resize it
        Dimension dim=getSize();
        this.setMaximumSize(dim);
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        if (dim.height>screen.height-100) {
            setSize(screen.height-100,dim.width);
        }
        if (dataitem!=null) focusOKButton();
        structurePanel.analyzeStructure();
    }
    
    private void onConsensusUpdate() {
        String newconsensus=consensusTextField.getText().trim();
        if (!newconsensus.equalsIgnoreCase(oldConsensus)) {
            oldConsensus=newconsensus;
            updatePWMfromConsensus(newconsensus);
            logoPanel.setClearOnNextPaint();
            logoPanel.repaint(); // updates sequence logo
            structurePanel.dataUpdated();
        }
    }
    
    
    /** This method should be called if the user updates the consensus-string in the prompt
     *  it will update the PWM matrix to fit the consensus string
     */
    private void updatePWMfromConsensus(String newconsensus) {
        // check if the multiple sequences have been entered
        Double[][] matrix=null;
        String[] sequences=newconsensus.split("[^a-zA-Z]+");
        if (sequences.length>1) { // multiple aligned sequences. 
            int consensuslength=sequences[0].length();
            matrix=new Double[consensuslength][4];
            // initialize the matrix with zeros! 
            for (int i=0;i<consensuslength;i++) {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
            for (int i=0;i<sequences.length;i++) {
                 if (sequences[i].length()!=consensuslength) {reportError("Aligned sequences are not of equal length");return;}                  
                 for (int j=0;j<sequences[i].length();j++) {
                    char base=Character.toUpperCase(sequences[i].charAt(j));
                         if (base=='A' || base=='a') {matrix[j][0]+=1f;}
                    else if (base=='C' || base=='c') {matrix[j][1]+=1f;}
                    else if (base=='G' || base=='g') {matrix[j][2]+=1f;}
                    else if (base=='T' || base=='t') {matrix[j][3]+=1f;}
                 }                 
            }
        } else { // single consensus sequence
            matrix=new Double[newconsensus.length()][4];
            for (int i=0;i<newconsensus.length();i++) {
                char base=Character.toUpperCase(newconsensus.charAt(i));
                     if (base=='A') {matrix[i][0]=12.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='C') {matrix[i][0]=0.0;matrix[i][1]=12.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='G') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=12.0;matrix[i][3]=0.0;}
                else if (base=='T') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=12.0;}
                else if (base=='R') {matrix[i][0]=6.0;matrix[i][1]=0.0;matrix[i][2]=6.0;matrix[i][3]=0.0;}
                else if (base=='Y') {matrix[i][0]=0.0;matrix[i][1]=6.0;matrix[i][2]=0.0;matrix[i][3]=6.0;}
                else if (base=='M') {matrix[i][0]=6.0;matrix[i][1]=6.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='K') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=6.0;matrix[i][3]=6.0;}
                else if (base=='W') {matrix[i][0]=6.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=6.0;}
                else if (base=='S') {matrix[i][0]=0.0;matrix[i][1]=6.0;matrix[i][2]=6.0;matrix[i][3]=0.0;}
                else if (base=='B') {matrix[i][0]=0.0;matrix[i][1]=4.0;matrix[i][2]=4.0;matrix[i][3]=4.0;}
                else if (base=='D') {matrix[i][0]=4.0;matrix[i][1]=0.0;matrix[i][2]=4.0;matrix[i][3]=4.0;}
                else if (base=='H') {matrix[i][0]=4.0;matrix[i][1]=4.0;matrix[i][2]=0.0;matrix[i][3]=4.0;}
                else if (base=='V') {matrix[i][0]=4.0;matrix[i][1]=4.0;matrix[i][2]=4.0;matrix[i][3]=0.0;}
                else {matrix[i][0]=3.0;matrix[i][1]=3.0;matrix[i][2]=3.0;matrix[i][3]=3.0;}
            }
        }
        matrix=Motif.normalizeMatrix(matrix);
        DefaultTableModel model=new DefaultTableModel(matrix,new String[]{"A","C","G","T"});
        matrixTable.setModel(model);
        model.addTableModelListener(tableModelListener);
        updateConsensusFromPWM();
    }
    
    /** This method should be called if the user updates the PWM matrix in the prompt
     *  it will update the consensus string to fit the PWM matrix
     */
    private void updateConsensusFromPWM() {
        double[][] matrix=null;
        try {
           matrix=parseMatrixTable();
           data.setMatrix(matrix); // necessary for the sequence logo to be updated
           consensusTextField.setText(Motif.getConsensusForMatrix(matrix));
           oldConsensus=consensusTextField.getText();
           icLabel.setText("IC: "+decimalFormat.format(Motif.calculateInformationContent(matrix, false)));
           sizeLabel.setText("Size: "+matrix.length+" bp");
           gcLabel.setText("GC: "+percentageFormat.format(Motif.calculateGCContent(matrix)));
           commonpanel.revalidate();
           reportError(""); // clear the error message
        } catch (ParseError e) {
           reportError(e.getMessage());
        }
    }
    
    
    
    
    @Override
    public Data getData() {
        return data;
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof Motif) data=(Motif)newdata; 
    }     

    @Override
    public boolean onOKPressed() {  
        if (showreverse.isSelected()) {
            boolean ok=reverseMatrix(); // get matrix back in direct orientation before proceeding
            if (!ok) {showreverse.setSelected(true);return false;} // reversal failed              
        } 
        double[][] matrix=null;
        try {
           matrix=parseMatrixTable();
           data.setMatrix(matrix);
        } catch (ParseError e) {
           tabbedpane.setSelectedComponent(representationPanel);
           reportError(e.getMessage());
           return false;
        }
        String consensus=consensusTextField.getText();
        if (consensus!=null && !consensus.trim().isEmpty()) data.setConsensusMotif(consensus.trim()); 
        else data.setConsensusMotif(Motif.getConsensusForMatrix(matrix));  
        String shortName=shortNameTextField.getText();
        if (shortName!=null && !shortName.trim().isEmpty()) data.setShortName(shortName.trim()); else data.setShortName(null); // else data.setShortName(data.getConsensusMotif());
        String longName=longNameTextField.getText();
        if (longName!=null && !longName.trim().isEmpty()) data.setLongName(longName.trim()); else data.setLongName(null);
        try {
            data.setClassification(currentMotifClass);
        } catch (ExecutionError e) {
           tabbedpane.setSelectedComponent(factorsPanel);
           reportError(e.getMessage());
           return false;
        }
        data.setPart((String)partCombobox.getSelectedItem());
        int quality=((Integer)qualityCombobox.getSelectedItem()).intValue();
        data.setQuality(quality);
        
        String description=descriptionTextArea.getText();
        if (description!=null && !description.trim().isEmpty()) data.setDescription(description);
        else data.setDescription(null);        
        
        String organisms=organismTextArea.getText();
        if (organisms!=null) organisms=processString(organisms.trim());
        if (organisms!=null && !organisms.isEmpty()) data.setOrganisms(organisms);
        else data.setOrganisms((String)null);
        String factors=bindingFactorsTextArea.getText();
        if (factors!=null) factors=processString(factors.trim());
        if (factors!=null && !factors.isEmpty()) data.setBindingFactors(factors);
        else data.setBindingFactors((String)null);
        data.setICcontent(Motif.calculateInformationContent(matrix, false));
        data.setInteractionPartnerNames(interactionsPanel.getMotifList());
        data.setKnownDuplicatesNames(alternativesPanel.getMotifList());
        String tissues=expressionTextArea.getText();
        if (tissues!=null) tissues=processString(tissues.trim());
        if (tissues!=null && !tissues.isEmpty()) {
            String[] tissuesList=tissues.split("\\s*,\\s*");
            data.setTissueExpression(tissuesList);
        } else data.setTissueExpression((String[])null);
        // parse user-defined properties
        int rows=userproptable.getRowCount();
        for (int i=0;i<rows;i++) {
           Object key=userproptable.getValueAt(i, 0);
           if (key==null || (key instanceof String && ((String)key).trim().isEmpty())) continue;
           if (!Motif.isValidUserDefinedPropertyKey(key.toString())) {
               tabbedpane.setSelectedComponent(userPropertiesPanel);
               userproptable.setRowSelectionInterval(i, i);
               userproptable.scrollRectToVisible(userproptable.getCellRect(i,0,true));
               reportError("Not a valid property name: '"+key+"'");
               return false;
           }
           Object value=userproptable.getValueAt(i, 1);
           if (value==null) value=""; //
           if (value instanceof String && ((String)value).contains(";")) {
               tabbedpane.setSelectedComponent(userPropertiesPanel);
               userproptable.setRowSelectionInterval(i, i);
               userproptable.scrollRectToVisible(userproptable.getCellRect(i,0,true));
               reportError("Value for property '"+key+"' contains illegal character ';'");
               return false;               
           }
           value=Motif.getObjectForPropertyValueString(value.toString());
           data.setUserDefinedPropertyValue(key.toString(), value);
        }
        // parse GO terms
        String[] termsAsArray=GOPanel.getGOterms();
        try {
          data.setGOterms(termsAsArray);
        } catch (ParseError p) {
           reportError(p.getMessage());
           return false;
        }
        // update name 
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);  
        return true;
    }

    /** 
     * Converts a double[][] matrix to a Object[][] matrix (actually Double[][])
     * so that it can be used as argument for a DefaultTableModel
     * (autoboxing can not convert arrays of primitives to arrays of objects)
     */
    private Double[][] convertMatrix(double[][] m) {
        if (m==null) return null;
        Double[][] res=new Double[m.length][4];
        for(int i=0;i<m.length;i++) {
            res[i][0]=(Double)m[i][0];
            res[i][1]=(Double)m[i][1];
            res[i][2]=(Double)m[i][2];
            res[i][3]=(Double)m[i][3];
        }
        return res;
    }
    
    
    /** Returns the values to use for the matrix */
    private double[][] parseMatrixTable() throws ParseError {
        int rowcount=matrixTable.getRowCount();
        int highestset=0;
        for (int i=0;i<rowcount;i++) {
            Object value1=matrixTable.getValueAt(i, 0);
            Object value2=matrixTable.getValueAt(i, 1);
            Object value3=matrixTable.getValueAt(i, 2);
            Object value4=matrixTable.getValueAt(i, 3);
            boolean Aset=false;boolean Cset=false;boolean Gset=false;boolean Tset=false;
            if (value1!=null && !(value1.toString()).isEmpty()) Aset=true;
            if (value2!=null && !(value2.toString()).isEmpty()) Cset=true;
            if (value3!=null && !(value3.toString()).isEmpty()) Gset=true;
            if (value4!=null && !(value4.toString()).isEmpty()) Tset=true;
            boolean someset=(Aset||Cset||Gset||Tset);
            if (someset && !Aset) throw new ParseError("Missing value for A in row "+(i+1));
            if (someset && !Cset) throw new ParseError("Missing value for C in row "+(i+1));
            if (someset && !Gset) throw new ParseError("Missing value for G in row "+(i+1));
            if (someset && !Tset) throw new ParseError("Missing value for T in row "+(i+1));
            if (someset) highestset=i; else break;
        }
        double[][] matrix=new double[highestset+1][4];
        for (int i=0;i<=highestset;i++) {
            try {matrix[i][0]=Double.parseDouble(matrixTable.getValueAt(i, 0).toString());} catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for base A in row "+(i+1));}
            try {matrix[i][1]=Double.parseDouble(matrixTable.getValueAt(i, 1).toString());} catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for base C in row "+(i+1));}
            try {matrix[i][2]=Double.parseDouble(matrixTable.getValueAt(i, 2).toString());} catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for base G in row "+(i+1));}
            try {matrix[i][3]=Double.parseDouble(matrixTable.getValueAt(i, 3).toString());} catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for base T in row "+(i+1));}
        }
        return matrix;
    }
    
    private String processString(String input) {
        input=input.replaceAll("\n", ",");
        return input;
    }
    
    private void reportError(String msg) {
        if (msg==null) msg="NULL";
        errorLabel.setText(msg);
    }
    
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        motifShortNameLabel = new javax.swing.JLabel();
        motifLongNameLabel = new javax.swing.JLabel();
        consensusLabel = new javax.swing.JLabel();
        shortNameTextField = new javax.swing.JTextField();
        longNameTextField = new javax.swing.JTextField(40);
        consensusTextField = new javax.swing.JTextField(40);
        matrixLabel = new javax.swing.JLabel();
        matrixScrollPane = new javax.swing.JScrollPane();
        matrixTable = new javax.swing.JTable();
        logoPanel = new SequenceLogoPanel(true);
        logoLabel = new javax.swing.JLabel();
        classLabel = new javax.swing.JLabel();
        partLabel = new javax.swing.JLabel();
        qualityLabel = new javax.swing.JLabel();
        classTextArea = new javax.swing.JTextArea();
        qualityCombobox = new javax.swing.JComboBox();
        partCombobox = new javax.swing.JComboBox(new String[]{Motif.FULL,Motif.HALFSITE,Motif.DIMER,Motif.OLIGOMER});
        sizeLabel = new javax.swing.JLabel();
        icLabel = new javax.swing.JLabel();
        gcLabel = new javax.swing.JLabel();
        bindingFactorsLabel = new javax.swing.JLabel();
        bindingFactorsScrollPane = new javax.swing.JScrollPane();
        bindingFactorsTextArea = new javax.swing.JTextArea();
        organismsLabel = new javax.swing.JLabel();
        organismScrollPane = new javax.swing.JScrollPane();
        organismTextArea = new javax.swing.JTextArea();
        expressionLabel = new javax.swing.JLabel();
        expressionScrollPane = new javax.swing.JScrollPane();
        expressionTextArea = new javax.swing.JTextArea();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();        
        errorLabel = new javax.swing.JLabel();
        showreverse=new JCheckBox("Show reverse complement");
        showreverse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean ok=reverseMatrix();
                if (!ok) showreverse.setSelected(!showreverse.isSelected()); // undo the selection
            }
        });   
        scalebyICBox=new JCheckBox("Scale by IC",logoPanel.getScaleByICcontent());
        scalebyICBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logoPanel.setScaleByICcontent(scalebyICBox.isSelected());
                logoPanel.setClearOnNextPaint();
                logoPanel.repaint();
            }
        });  
        sortbyFreqBox=new JCheckBox("Sort by Frequency",logoPanel.getSortBaseByFrequency());
        sortbyFreqBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logoPanel.setSortBaseByFrequency(sortbyFreqBox.isSelected());
                logoPanel.setClearOnNextPaint();
                logoPanel.repaint();
            }
        });
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(Prompt_Motif.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        mainPanel.setName("Form"); // NOI18N
        mainPanel.setLayout(new BorderLayout());
        tabbedpane = new javax.swing.JTabbedPane();
        commonpanel = new JPanel(new java.awt.GridBagLayout());
        representationPanel = new JPanel(new java.awt.GridBagLayout());
        factorsPanel = new JPanel(new java.awt.GridBagLayout());
        expressionPanel = new JPanel(new java.awt.GridBagLayout());
        descriptionPanel = new JPanel(new BorderLayout());
        interactionsPanel = new MotifListPanel("Interactions",data.getInteractionPartnerNames());
        alternativesPanel = new MotifListPanel("Alternatives",data.getKnownDuplicatesNames());
        motifComparisonPanel = new MotifComparisonPanel(gui, Prompt_Motif.this, data, false);
        Border tabsBorder=BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),BorderFactory.createEmptyBorder(8,14,8,14));
        representationPanel.setBorder(tabsBorder);
        descriptionPanel.setBorder(tabsBorder);
        factorsPanel.setBorder(tabsBorder);
        expressionPanel.setBorder(tabsBorder);
        interactionsPanel.setBorder(tabsBorder);
        alternativesPanel.setBorder(tabsBorder);
        motifComparisonPanel.setBorder(tabsBorder);
        Dimension d=new Dimension(290,300);
        motifComparisonPanel.setPreferredSize(d);
        alternativesPanel.setPreferredSize(d);
        interactionsPanel.setPreferredSize(d);

        commonpanel.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));

        motifShortNameLabel.setText(resourceMap.getString("motifShortNameLabel.text")); // NOI18N
        motifShortNameLabel.setName("motifShortNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        commonpanel.add(motifShortNameLabel, gridBagConstraints);

        motifLongNameLabel.setText(resourceMap.getString("motifLongNameLabel.text")); // NOI18N
        motifLongNameLabel.setName("motifLongNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        commonpanel.add(motifLongNameLabel, gridBagConstraints);

        shortNameTextField.setColumns(22);
        shortNameTextField.setName("shortNameTextField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        commonpanel.add(shortNameTextField, gridBagConstraints);

        longNameTextField.setName("longNameTextField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        commonpanel.add(longNameTextField, gridBagConstraints);

        sizeLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        sizeLabel.setText(resourceMap.getString("sizeLabel.text")); // NOI18N
        sizeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sizeLabel.setInheritsPopupMenu(false);
        sizeLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        sizeLabel.setMinimumSize(new java.awt.Dimension(120, 14));
        sizeLabel.setName("sizeLabel"); // NOI18N
        sizeLabel.setPreferredSize(new java.awt.Dimension(120, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        commonpanel.add(sizeLabel, gridBagConstraints);

        icLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        icLabel.setText(resourceMap.getString("icLabel.text")); // NOI18N
        icLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        icLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        icLabel.setMinimumSize(new java.awt.Dimension(120, 14));
        icLabel.setName("icLabel"); // NOI18N
        icLabel.setPreferredSize(new java.awt.Dimension(120, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        commonpanel.add(icLabel, gridBagConstraints);

        gcLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        gcLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        gcLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        gcLabel.setMinimumSize(new java.awt.Dimension(120, 14));
        gcLabel.setName("gcLabel"); // NOI18N
        gcLabel.setPreferredSize(new java.awt.Dimension(120, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        commonpanel.add(gcLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        commonpanel.add(new JLabel("  "), gridBagConstraints); // just for spacing


        consensusLabel.setText(resourceMap.getString("consensusLabel.text")); // NOI18N
        consensusLabel.setName("consensusLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        representationPanel.add(consensusLabel, gridBagConstraints);

        consensusTextField.setName("consensusTextField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        representationPanel.add(consensusTextField, gridBagConstraints);

        matrixLabel.setText(resourceMap.getString("matrixLabel.text")); // NOI18N
        matrixLabel.setName("matrixLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        representationPanel.add(matrixLabel, gridBagConstraints);

        matrixScrollPane.setName("matrixScrollPane"); // NOI18N

        matrixTable.setName("matrixTable"); // NOI18N
        matrixScrollPane.setPreferredSize(new java.awt.Dimension(300, 160));
        matrixScrollPane.setViewportView(matrixTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        representationPanel.add(matrixScrollPane, gridBagConstraints);

        // logoPanel.setBackground(resourceMap.getColor("logoPanel.background")); // NOI18N
        logoPanel.setMinimumSize(new java.awt.Dimension(100, 66));
        logoPanel.setName("logoPanel"); // NOI18N
        logoPanel.setPreferredSize(new java.awt.Dimension(100, 66));

        javax.swing.GroupLayout logoPanelLayout = new javax.swing.GroupLayout(logoPanel);
        logoPanel.setLayout(logoPanelLayout);
        logoPanelLayout.setHorizontalGroup(
            logoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 268, Short.MAX_VALUE)
        );
        logoPanelLayout.setVerticalGroup(
            logoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 66, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        representationPanel.add(logoPanel, gridBagConstraints);

        logoLabel.setText(resourceMap.getString("logoLabel.text")); // NOI18N
        logoLabel.setName("logoLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        representationPanel.add(logoLabel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 4, 0);
        JPanel icpanel=new JPanel(new FlowLayout(FlowLayout.LEADING,0,0));
        icpanel.add(showreverse);
        icpanel.add(new JLabel("           "));
        icpanel.add(scalebyICBox);
        icpanel.add(new JLabel("           "));
        icpanel.add(sortbyFreqBox);
        representationPanel.add(icpanel, gridBagConstraints);


        organismsLabel.setText(resourceMap.getString("organismsLabel.text")); // NOI18N
        organismsLabel.setName("organismsLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        expressionPanel.add(organismsLabel, gridBagConstraints);

        organismScrollPane.setName("organismScrollPane"); // NOI18N
        organismTextArea.setName("organismList"); // NOI18N
        organismTextArea.setColumns(36);
        organismTextArea.setRows(7);
        organismScrollPane.setViewportView(organismTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        expressionPanel.add(organismScrollPane, gridBagConstraints);

        expressionLabel.setText(resourceMap.getString("expressionLabel.text")); // NOI18N
        expressionLabel.setName("expressionLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        expressionPanel.add(expressionLabel, gridBagConstraints);

        expressionScrollPane.setName("expressionScrollPane"); // NOI18N
        expressionTextArea.setName("expressionList"); // NOI18N
        expressionTextArea.setColumns(36);
        expressionTextArea.setRows(7);
        expressionScrollPane.setViewportView(expressionTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        expressionPanel.add(expressionScrollPane, gridBagConstraints);


        bindingFactorsLabel.setText(resourceMap.getString("bindingFactorsLabel.text")); // NOI18N
        bindingFactorsLabel.setName("bindingFactorsLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        factorsPanel.add(bindingFactorsLabel, gridBagConstraints);

        bindingFactorsScrollPane.setName("bindingFactorsScrollPane"); // NOI18N

        bindingFactorsTextArea.setColumns(36);
        bindingFactorsTextArea.setRows(7);
        bindingFactorsTextArea.setName("bindingFactorsTextArea"); // NOI18N
        bindingFactorsScrollPane.setViewportView(bindingFactorsTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        factorsPanel.add(bindingFactorsScrollPane, gridBagConstraints);

        classLabel.setText(resourceMap.getString("classLabel.text")); // NOI18N
        classLabel.setName("classLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        factorsPanel.add(classLabel, gridBagConstraints);

        classTextArea.setName("classTextArea"); // NOI18N
        classTextArea.setColumns(36);
        classTextArea.setRows(6);
        classTextArea.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        factorsPanel.add(classTextArea, gridBagConstraints);

        partLabel.setText(resourceMap.getString("partLabel.text")); // NOI18N
        partLabel.setName("partLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        factorsPanel.add(partLabel, gridBagConstraints);

        partCombobox.setName("partCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        factorsPanel.add(partCombobox, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        factorsPanel.add(new JLabel("             "), gridBagConstraints);

        qualityLabel.setText(resourceMap.getString("qualityLabel.text")); // NOI18N
        qualityLabel.setName("qualityLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        factorsPanel.add(qualityLabel, gridBagConstraints);

        qualityCombobox.setModel(new javax.swing.DefaultComboBoxModel(new Integer[] {1,2,3,4,5,6}));
        qualityCombobox.setName("qualityCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        factorsPanel.add(qualityCombobox, gridBagConstraints);


        structurePanel = new StructurePanel();
        StructurePanelWithControls structurePanelWithControls=new StructurePanelWithControls(structurePanel);
        structurePanelWithControls.setBorder(tabsBorder);

        errorLabel.setFont(resourceMap.getFont("errorLabel.font")); // NOI18N
        errorLabel.setForeground(resourceMap.getColor("errorLabel.foreground")); // NOI18N
        errorLabel.setText("   "); // NOI18N
        errorLabel.setName("errorLabel"); // NOI18N
        
        setupUserDefinedPropertiesPanel();
        userPropertiesPanel.setBorder(tabsBorder);
        
        setupGOPanel();
        GOPanel.setBorder(tabsBorder);        
        
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        JPanel descriptionInternalPanel=new JPanel(new BorderLayout());
        descriptionInternalPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        descriptionScrollPane.setViewportView(descriptionTextArea);
        descriptionInternalPanel.add(descriptionScrollPane);
        descriptionPanel.add(descriptionInternalPanel);
                
        tabbedpane.addTab("Motif", representationPanel);
        tabbedpane.addTab("Structure", structurePanelWithControls);
        tabbedpane.addTab("Description", descriptionPanel);
        tabbedpane.addTab("Factors", factorsPanel);
        tabbedpane.addTab("Expression", expressionPanel);
        tabbedpane.addTab("GO", GOPanel);
        tabbedpane.addTab("Properties", userPropertiesPanel);
        tabbedpane.addTab("Interactions", interactionsPanel);
        tabbedpane.addTab("Alternatives", alternativesPanel);
        tabbedpane.addTab("Compare", motifComparisonPanel);
        mainPanel.add(commonpanel,BorderLayout.NORTH);
        mainPanel.add(tabbedpane,BorderLayout.CENTER);
        mainPanel.add(errorLabel,BorderLayout.SOUTH);

    }

    private void setupUserDefinedPropertiesPanel() {
        userPropertiesPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        String[] allProperties=Motif.getAllUserDefinedProperties(engine);
        Object[][] propdata=new Object[allProperties.length][3]; // 3 columns: key and value and classtype
        for (int i=0;i<allProperties.length;i++) {
            //Object value=data.getUserDefinedPropertyValue(allProperties[i]);
            Class propclass=Motif.getClassForUserDefinedProperty(allProperties[i],engine);
            propdata[i][0]=allProperties[i]; // key
            propdata[i][1]=data.getUserDefinedPropertyValueAsType(allProperties[i],String.class); // convert to string for display purposes
            propdata[i][2]=propclass;          
        }
        final DefaultTableModel userproptablemodel=new DefaultTableModel(propdata, new String[]{"Property","Value","Type"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column<2; // type column is not editable
            }            
        };
        userproptable=new JTable(userproptablemodel);
        userproptable.setAutoCreateRowSorter(true);
        userproptable.getTableHeader().setReorderingAllowed(false);
        userproptable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        userproptable.setDefaultRenderer(Object.class, new UserDefinedPropertiesRenderer());
        userproptable.setCellSelectionEnabled(true);
        userproptablemodel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType()==TableModelEvent.UPDATE && e.getColumn()==1) { // update value column
                    Object stringvalue=userproptablemodel.getValueAt(e.getFirstRow(), 1);
                    Object value=null;
                    if (stringvalue!=null && !stringvalue.toString().trim().isEmpty()) value=Motif.getObjectForPropertyValueString(stringvalue.toString());
                    if (value!=null) userproptablemodel.setValueAt(value.getClass(),e.getFirstRow(),2);
                    else userproptablemodel.setValueAt(null, e.getFirstRow(), 2);
                    userproptable.repaint();
                }
            }
        });
        JPanel internalpropspanel=new JPanel(new BorderLayout());
        JPanel propscontrolspanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        addPropertyButton=new JButton("Add Property");
        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String[] newRowValues=new String[userproptable.getColumnCount()];
               for (int j=0;j<newRowValues.length;j++) newRowValues[j]="";
               newRowValues[newRowValues.length-1]=null;
               userproptablemodel.addRow(newRowValues);
               int newrow=getFirstEmptyRowInTable(userproptable);
               if (newrow>=0) {
                   boolean canedit=userproptable.editCellAt(newrow, 0);
                   if (canedit) {
                       userproptable.changeSelection(newrow, 0, false, false);
                       userproptable.requestFocus();
                   }
               }
            }
        });
        if (isDataEditable()) propscontrolspanel.add(addPropertyButton);
        internalpropspanel.add(new JScrollPane(userproptable),BorderLayout.CENTER);
        internalpropspanel.add(propscontrolspanel,BorderLayout.SOUTH);
        internalpropspanel.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        Dimension dim=new Dimension(500,290);
        internalpropspanel.setMinimumSize(dim);
        internalpropspanel.setPreferredSize(dim);
        internalpropspanel.setMaximumSize(dim);
        userPropertiesPanel.add(internalpropspanel);
        userproptable.getRowSorter().toggleSortOrder(0);
    }
    
    private int getFirstEmptyRowInTable(JTable table) {
        for (int i=0;i<table.getRowCount();i++) {
            boolean empty=true;
            for (int j=0;j<table.getColumnCount();j++) {
                Object val=table.getValueAt(i, j);
                if (val!=null && !val.toString().isEmpty()) {empty=false;break;}
            }
            if (empty) return i;
        }
        return -1;
    }    
    
    private void setupGOPanel() {
        GOPanel=new GOEditorPanel(engine, data.getGOterms());
    }

          
    private class UserDefinedPropertiesRenderer extends DefaultTableCellRenderer {
         public UserDefinedPropertiesRenderer() {
             super();
             //this.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object propKey=table.getValueAt(row, 0);
            Object propValue=table.getValueAt(row, 1);
            boolean keyDefined=(propKey!=null && !propKey.toString().trim().isEmpty());
            boolean valueDefined=(propValue!=null && !propValue.toString().trim().isEmpty());
            if (column==0) { // key column
                if (keyDefined) {
                    if (valueDefined) this.setForeground(Color.BLACK);
                    else this.setForeground(Color.LIGHT_GRAY);
                } else { // not key is given
                    if (valueDefined) {
                        this.setText("*** Missing property name ***");
                        this.setForeground(Color.RED);
                    }
                }
                setToolTipText((keyDefined)?propKey.toString():"*** Missing property name ***");
            } else if (column==1) { // value column
                if (valueDefined) {
                    String valuestring=value.toString();
                    if (valuestring.contains(",")) {
                        String split=valuestring.replaceAll("\\s*,\\s*", "<br>");   
                        setToolTipText("<html>"+split+"</html>");  
                    } else setToolTipText(valuestring);
                } else setToolTipText("");
            } else { // type column
                Object classTypeObject=table.getValueAt(row, 2);
                if (classTypeObject==null || !valueDefined || !keyDefined) this.setText("");
                if (classTypeObject instanceof Class) {
                    Class classType = (Class)classTypeObject;
                    if (classType.equals(ArrayList.class)) this.setText("List");
                    else if (Number.class.isAssignableFrom(classType)) this.setText("Numeric");
                    else if (classType.equals(String.class)) this.setText("Text");
                    else this.setText(classType.getSimpleName());
                } else this.setText("");
            }
            return this;
        }
    }
    

    /** reverse complements the current matrix in the table (and the selection also) */
    private boolean reverseMatrix() {
        boolean tracking=trackTableChanges;
        double[][] matrix;     
        try {
           trackTableChanges=false; // set this flag to false to avoid table model listener responding and updating everything everytime 
           matrix=parseMatrixTable();
        } catch (ParseError e) {
           tabbedpane.setSelectedComponent(representationPanel);
           reportError(e.getMessage());
           trackTableChanges=tracking;
           return false;
        }
        // determine currently selected region (selection will also be reversed)
        int minrowsel=-1;
        int maxrowsel=-1;
        int mincolsel=-1;
        int maxcolsel=-1;
        for (int i=0;i<matrix.length;i++) {
            for (int j=0;j<4;j++) {
                matrixTable.setValueAt(matrix[i][j], (matrix.length-1)-i, 3-j);
                if (matrixTable.isCellSelected(i, j)) {
                    if (minrowsel<0) minrowsel=i;
                    if (i>maxrowsel) maxrowsel=i;
                    if (mincolsel<0) mincolsel=j;
                    if (j>maxcolsel) maxcolsel=j;                   
                }
            }
        }       
        trackTableChanges=tracking; // turn tracking back on
        ((DefaultTableModel)matrixTable.getModel()).fireTableCellUpdated(0,0); // just to notify listeners that the table has changed
        matrixTable.clearSelection();
        if (minrowsel>=0 && mincolsel>=0) {
            minrowsel=(matrix.length-1)-minrowsel;
            maxrowsel=(matrix.length-1)-maxrowsel;  
            mincolsel=3-mincolsel;
            maxcolsel=3-maxcolsel;  
            matrixTable.setRowSelectionInterval(minrowsel, maxrowsel);
            matrixTable.setColumnSelectionInterval(mincolsel, maxcolsel);          
        }       
        return true;
    }

    /** A panel containing the sequence logo */
    private class SequenceLogoPanel extends JPanel implements MouseListener, MouseMotionListener, ListSelectionListener {
        private Font font;
        private Font rulerfont;
        private char[] bases;
        private Color[] basecolors;
        private double[] letterXoffset=new double[4];        
        private int fontheight=70; // if you change this size you must also change the ascentCorrection below accordingly (but I am not sure about the relationship...)
        private int ascentCorrection=-7; // this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
        private int xoffset=0;
        private int yoffset=0;
        private boolean clearOnNextPaint=false;
        private boolean backgroundByIC=false;
        private int selectionStart=-1;
        private int selectionEnd=-1;
        private int firstAnchor=-1;
        private int secondAnchor=-1;
        private int logoWidthInPixels=0;
        private boolean scaleByIC=true; // scale column in logo by IC-content
        private boolean sortBasesByFrequency=true; // sort bases in column by frequency of occurrence so that most frequent base appears on top of stack
        private boolean traprecursivecall=false;
     
        public SequenceLogoPanel(Boolean enableSelection) {
            setOpaque(true);
            font=MotifLogo.getLogoFont(fontheight);
            rulerfont=new Font(Font.SANS_SERIF,Font.PLAIN,10);
            bases=new char[]{'A','C','G','T'};
            basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};
            if (enableSelection) {
                this.addMouseListener(this);
                this.addMouseMotionListener(this);
            }
        }

        public void setUseGradientBackground(boolean flag) {
            backgroundByIC=flag;
            this.repaint();
        }

        /** Returns the (maximal) width of the logo in 1-to-1 scale*/
        public int getLogoWidth(Graphics g) {
            FontMetrics metrics=g.getFontMetrics(font);
            int widthG=metrics.charWidth('G');
            int logowidth=data.getLength()*widthG;
            return logowidth;
        }

        /** Sets a selection interval which will be highlighted in the logo (if values are positive) */
        public void setSelectionInterval(int start, int end) {
            selectionStart=start;
            selectionEnd=end;
        }



        @Override
        public void paintComponent(Graphics graphics) {
            //super.paintComponent(graphics);
            double[][] matrix=data.getMatrixAsFrequencyMatrix(); 
            if (matrix==null) return;           
            Graphics2D g = (Graphics2D)graphics;
            int panelwidth=getWidth();
            if (clearOnNextPaint) {
                g.setColor(getBackground());
                g.fillRect(0, 0, panelwidth, getHeight());
                clearOnNextPaint=false;
            }
            FontMetrics metrics=g.getFontMetrics(font);
            int ascent=metrics.getAscent()-ascentCorrection;
            int widthA=metrics.charWidth('A');     
            int widthC=metrics.charWidth('C');     
            int widthG=metrics.charWidth('G');     
            int widthT=metrics.charWidth('T');
            letterXoffset[0]=(widthG-widthA)/2;
            letterXoffset[1]=(widthG-widthC)/2;
            letterXoffset[3]=(widthG-widthT)/2;
            int logowidth=matrix.length*widthG;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(font);
            g.setColor(java.awt.Color.RED);
            AffineTransform restore=g.getTransform();
            AffineTransform save=g.getTransform();
            double scaleX=1.0;
            if (logowidth>panelwidth) scaleX=((double)panelwidth/(double)logowidth); // scale X-direction so that logo fits irrespective of size
            if (scaleX!=1.0) save.scale(scaleX,1); // scale X-direction so that logo fits irrespective of size
            AffineTransform scaleY = new AffineTransform();
            if (!backgroundByIC) {
                g.setColor(Color.WHITE);
                g.fillRect(xoffset, yoffset, logowidth, ascent);
            }
            if (selectionStart>=0 && selectionStart<matrix.length && selectionEnd>=selectionStart) { // draw selection background
               int selectedBases=selectionEnd-selectionStart+1;
               int selectionX=xoffset+(int)(selectionStart*widthG*scaleX);
               int selectionWidth=(int)(selectedBases*widthG*scaleX);
               g.setColor(matrixTable.getSelectionBackground());
               g.fillRect(selectionX+1, yoffset+1, selectionWidth-2, ascent-2);
               g.drawRect(selectionX+1, yoffset+1, selectionWidth-2, ascent-2);
            }
            double xpos=xoffset;
            double[] ic=new double[matrix.length];
            for (int i=0;i<matrix.length;i++) {
                double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
                ic[i]=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
            }
            for (int i=0;i<matrix.length;i++) {
                double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
                double total=counts[0]+counts[1]+counts[2]+counts[3];
                int[] sorted=new int[4]; // sorted in ascending order. Values are base-indices (i.e. 0=>A, 1=>C, 2=>G, 3=>T)
                int indexA=0, indexC=0, indexG=0, indexT=0;
                if (counts[0]>=counts[1]) indexA++;
                if (counts[0]>=counts[2]) indexA++;
                if (counts[0]>=counts[3]) indexA++;                
                if (counts[1]>counts[0]) indexC++;
                if (counts[1]>=counts[2]) indexC++;
                if (counts[1]>=counts[3]) indexC++;               
                if (counts[2]>counts[0]) indexG++;
                if (counts[2]>counts[1]) indexG++;
                if (counts[2]>=counts[3]) indexG++;                
                if (counts[3]>counts[0]) indexT++;
                if (counts[3]>counts[1]) indexT++;
                if (counts[3]>counts[2]) indexT++;
                sorted[indexA]=0;
                sorted[indexC]=1;
                sorted[indexG]=2;
                sorted[indexT]=3;
                if (backgroundByIC) {
                    g.setTransform(save);
                    double convertedIC1=((ic[i]/2.0)*9.0)+1.0; // converts IC to a number between 1 and 10
                    double convertedIC2=((ic[(i+1<matrix.length)?(i+1):i]/2.0)*9.0)+1.0; // converts IC to a number between 1 and 10
                    int value1=(int)(255*Math.log10(convertedIC1));
                    int value2=(int)(255*Math.log10(convertedIC2));
                    Color color1=new Color(value1,value1,value1);
                    Color color2=new Color(value2,value2,value2);

                    java.awt.GradientPaint gradient = new java.awt.GradientPaint((int)(xpos+widthG*0.7), yoffset, color1, (int)(xpos+widthG*1.3), yoffset, color2, false); // I use 0.7 to 1.3 to make the "constant-color-columns" a little bit "wider"
                    g.setPaint(gradient);
                    int startx=(i==0)?(int)xpos:(int)(xpos+widthG*0.5);
                    int xwidth=(int)(widthG+1);
                    if (i==0) xwidth=(int)(widthG*1.5+1);
                    else if (i==matrix.length-1) xwidth=(int)(widthG*0.5);
                    g.draw(new Rectangle(startx, yoffset, xwidth, ascent));
                    g.fill(new Rectangle(startx, yoffset, xwidth, ascent));
                }
                double currentYoffset=yoffset;
                if (scaleByIC) currentYoffset+=(ascent-(ascent*ic[i]/2f));
                for (int j=3;j>=0;j--) { // draws letters from top to bottom (most frequent first)
                    int base=(sortBasesByFrequency)?sorted[j]:(3-j);
                    double fraction=counts[base]/total;
                    scaleY.setTransform(save);
                    scaleY.translate(letterXoffset[base],currentYoffset); // translated in 1-to-1 scale                    
                    if (scaleByIC) scaleY.scale(1,ic[i]/2f); // scale by IC-content in position
                    scaleY.scale(1, fraction);   
                    g.setColor(basecolors[base]);
                    g.setTransform(scaleY);
                    g.drawString(""+bases[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                    currentYoffset+=ascent*fraction*((scaleByIC)?(ic[i]/2f):1.0);
                } // end for each base  
                xpos+=widthG; // 
            } // end for each position
            // restore 1-to-1 scale
            g.setTransform(restore);
            g.setFont(rulerfont);
            g.setColor(Color.BLACK);
            g.drawLine(xoffset, yoffset, xoffset, ascent+yoffset); // draw vertical axis
            logoWidthInPixels=(logowidth>panelwidth)?panelwidth-1:logowidth;
            g.drawRect(xoffset, yoffset, logoWidthInPixels, ascent); // bounding box
            FontMetrics rulermetrics=g.getFontMetrics(rulerfont);
            for (int i=0;i<=matrix.length;i++) { // draw axis ticks and ruler
               int x=xoffset+(int)(i*widthG*scaleX);
               g.drawLine(x, ascent+yoffset, x, ascent+yoffset+3); // tick line
               if (i<matrix.length && (scaleX>0.28 || (i+1)%2==0)) { // draw base number, but only every second (even numbers) if scale is less than 28%
                   String tickMark=""+(i+1);
                   int markwidth=rulermetrics.stringWidth(tickMark);
                   int tickOffset=xoffset+(int)Math.round((i+0.5)*widthG*scaleX-markwidth/2.0);
                   g.drawString(tickMark, tickOffset, ascent+yoffset+14);
               }
            }
            // draw selected interval outline
//            if (selectionStart>=0 && selectionStart<matrix.length && selectionEnd>=selectionStart) {
//               int selectedBases=selectionEnd-selectionStart+1;
//               int selectionX=xoffset+(int)(selectionStart*widthG*scaleX);
//               int selectionWidth=(int)(selectedBases*widthG*scaleX);
//               g.setColor(Color.MAGENTA);
//               g.drawRect(selectionX+1, yoffset+1, selectionWidth-2, ascent-2);
//            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount()==2) {
                 showSaveMotifLogoDialog(); 
            }
        } 
        
        @Override
        public void mouseEntered(MouseEvent e) {} // unneeded interface implementation
        @Override
        public void mouseExited(MouseEvent e) {} // unneeded interface implementation
        @Override
        public void mouseMoved(MouseEvent e) {} // unneeded interface implementation
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                SequenceLogoContextMenu popupmenu=new SequenceLogoContextMenu(this);
                popupmenu.show(e.getComponent(), e.getX(),e.getY());
                return;
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                SequenceLogoContextMenu popupmenu=new SequenceLogoContextMenu(this);
                popupmenu.show(e.getComponent(), e.getX(),e.getY());
                return;
            }
            if (e.getButton()!=MouseEvent.BUTTON1) return;
            int x=e.getX()-xoffset;
            if (x<0 || x>=logoWidthInPixels) {
                selectionStart=-1;
                selectionEnd=-1;
                firstAnchor=-1;
                secondAnchor=-1;
                matrixTable.clearSelection();
            } else {
                double pixelsprbase=(double)logoWidthInPixels/(double)data.getLength();
                int base=(int)(x/pixelsprbase);
                selectionStart=base;
                selectionEnd=base;
                firstAnchor=base;
                secondAnchor=base;
                //traprecursivecall=true; // this will trap the first call but not the second!
                matrixTable.changeSelection(selectionStart, 0, false, false);
                matrixTable.changeSelection(selectionEnd, matrixTable.getColumnCount()-1, false, true);
             }
            //setClearOnNextPaint();
            //repaint();// the changeSelection will cause the logo to be repainted!
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int x=e.getX()-xoffset;
            if (selectionStart<0) return; // no selection
            if (x<0) x=0;
            else if(x>=logoWidthInPixels) x=logoWidthInPixels-1;
            double pixelsprbase=(double)logoWidthInPixels/(double)data.getLength();
            int base=(int)(x/pixelsprbase);
            if (base==secondAnchor) {
               return; // no update
            }
            secondAnchor=base;
            if (secondAnchor>=firstAnchor) {selectionStart=firstAnchor;selectionEnd=secondAnchor;}
            else {selectionStart=secondAnchor;selectionEnd=firstAnchor;}
            traprecursivecall=true; // this will trap the first call but not the second!
            matrixTable.changeSelection(selectionStart, 0, false, false);
            traprecursivecall=true;
            matrixTable.changeSelection(selectionEnd, matrixTable.getColumnCount()-1, false, true);
            int rowIndexStart = matrixTable.getSelectedRow();
            int rowIndexEnd = matrixTable.getSelectionModel().getMaxSelectionIndex();
            logoPanel.setSelectionInterval(rowIndexStart, rowIndexEnd);
            logoPanel.setClearOnNextPaint();
            logoPanel.repaint();
            traprecursivecall=false;
           //setClearOnNextPaint();
            //repaint();// the changeSelection will cause the logo to be repainted!
         }


        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (traprecursivecall) {traprecursivecall=false;return;}
            // If cell selection is enabled, both row and column change events are fired
            int rowIndexStart = matrixTable.getSelectedRow();
            int rowIndexEnd = matrixTable.getSelectionModel().getMaxSelectionIndex();
            logoPanel.setSelectionInterval(rowIndexStart, rowIndexEnd);
            logoPanel.setClearOnNextPaint();
            logoPanel.repaint();
        }

        
        public void setClearOnNextPaint() {
            clearOnNextPaint=true;
        }

        public void setScaleByICcontent(boolean doscale) {
            scaleByIC=doscale;
        }

        public boolean getScaleByICcontent() {
            return scaleByIC;
        }

        public void setSortBaseByFrequency(boolean dosort) {
            sortBasesByFrequency=dosort;
        }

        public boolean getSortBaseByFrequency() {
            return sortBasesByFrequency;
        }


 } // end class SequenceLogo


   private void showSaveMotifLogoDialog() {
        SaveMotifLogoImageDialog saveLogoPanel=new SaveMotifLogoImageDialog(gui, (Motif)data, this.isModal());
        saveLogoPanel.setLocation(gui.getFrame().getWidth()/2-saveLogoPanel.getWidth()/2, gui.getFrame().getHeight()/2-saveLogoPanel.getHeight()/2);
        saveLogoPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        saveLogoPanel.setVisible(true);       
   } 
    

   private class SequenceLogoContextMenu extends JPopupMenu implements ActionListener {
       SequenceLogoPanel logopanel;

       public SequenceLogoContextMenu(SequenceLogoPanel logopanel) {
          this.logopanel=logopanel;
          JCheckBoxMenuItem reversemenuitem=new JCheckBoxMenuItem("Reverse Complement",showreverse.isSelected());
          reversemenuitem.addActionListener(this);
          add(reversemenuitem);          
          JCheckBoxMenuItem scalemenuitem=new JCheckBoxMenuItem("Scale by IC-content",scalebyICBox.isSelected());
          scalemenuitem.addActionListener(this);
          add(scalemenuitem);
          JCheckBoxMenuItem sortmenuitem=new JCheckBoxMenuItem("Sort Bases by Frequency",sortbyFreqBox.isSelected());
          sortmenuitem.addActionListener(this);
          add(sortmenuitem);
          JMenuItem clearmenuitem=new JMenuItem("Clear Selection");
          clearmenuitem.addActionListener(this);
          add(clearmenuitem);
          JMenuItem savemenuitem=new JMenuItem("Save Motif Logo");
          savemenuitem.addActionListener(this);
          add(savemenuitem);          
       }

       @Override
       public void actionPerformed(ActionEvent e) {
            String action=e.getActionCommand();
            if (action.equalsIgnoreCase("Reverse Complement")) {
                showreverse.doClick();
            } else if (action.equalsIgnoreCase("Scale by IC-content")) {
                scalebyICBox.doClick();
            } else if (action.equalsIgnoreCase("Sort Bases by Frequency")) {
                sortbyFreqBox.doClick();
            } else if (action.equalsIgnoreCase("Clear Selection")) {
                matrixTable.clearSelection();
            } else if (action.equalsIgnoreCase("Save Motif Logo")) {
                showSaveMotifLogoDialog();
            }
         }

  } // END: class SequenceLogoContextMenu

  private class StructurePanelWithControls extends JPanel {
      private StructurePanel structurePanel;
      private JPanel controlspanel;
      private JSlider thresholdslider;
      private JCheckBox bgcheckbox;

         public StructurePanelWithControls(StructurePanel panel) {
            this.structurePanel=panel;
            this.setLayout(new BorderLayout());
            controlspanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
            thresholdslider=new JSlider(JSlider.HORIZONTAL);
            thresholdslider.setPreferredSize(new Dimension(100,20));
            thresholdslider.setMinimum(1); //
            thresholdslider.setMaximum(100); //
            bgcheckbox=new JCheckBox("Shade by IC-content", true);
            controlspanel.add(new JLabel("Lenient "));
            controlspanel.add(thresholdslider);
            controlspanel.add(new JLabel(" Strict                "));

            controlspanel.add(bgcheckbox);
            add(controlspanel,BorderLayout.NORTH);
            add(structurePanel,BorderLayout.CENTER);
            structurePanel.analyzeStructure();
            bgcheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    structurePanel.structureLogoPanel.setUseGradientBackground(bgcheckbox.isSelected());
                    repaint();
                }
            });
            thresholdslider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int value=thresholdslider.getValue();
                    structurePanel.columnthreshold=value/100.0; //
                    structurePanel.dataUpdated();
                    repaint();
                }
            });
            thresholdslider.setValue(1); //
        }
  }

  private class StructurePanel extends JPanel { 
        SequenceLogoPanel structureLogoPanel;
        private int logoHeight=66;
        private int maxLogoWidth=600;
        private ArrayList<StructureArrow> arrows=new ArrayList<StructureArrow>();
        private Color[] colorlist=new Color[]{Color.GREEN,Color.RED,Color.YELLOW,Color.BLUE,Color.MAGENTA,Color.CYAN,Color.WHITE};
        boolean removepartials=true;
        double columnthreshold=0.8;

        public StructurePanel() {
            this.setLayout(null);
            structureLogoPanel=new SequenceLogoPanel(false);
            structureLogoPanel.setPreferredSize(new Dimension(maxLogoWidth,logoHeight));
            structureLogoPanel.setUseGradientBackground(true);
            this.add(structureLogoPanel);
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int width=this.getWidth();
            int height=this.getHeight();

            int topLeftY=(int)((height-logoHeight)/2.0);
            int widthLogo=structureLogoPanel.getLogoWidth(graphics);
            int showLogoWidth=(widthLogo<maxLogoWidth)?widthLogo:maxLogoWidth;
            int topLeftX=(int)((width-showLogoWidth)/2.0);
            float basewidth=(float)showLogoWidth/(float)data.getLength();
            structureLogoPanel.setBounds(topLeftX, topLeftY, showLogoWidth+1,logoHeight);
            for (StructureArrow arrow:arrows) {
                paintArrow(graphics, arrow.direction, arrow.pos, arrow.length, arrow.color, arrow.line, basewidth, topLeftX, topLeftY);
            }
        }


        private void paintArrow(Graphics g, int direction, int pos, int length, Color color, int line, float basewidth, int topLeftX, int topLeftY) {
            int arrowheight=14;
            int y=topLeftY+line*arrowheight;
            if (line>=0) y+=logoHeight;
            int x=(int)(topLeftX+basewidth*pos+1); // the +1 and -5 below is just to finetune the size
            if (direction<0) x+=1;
            int width=(int)(basewidth*length-3);
            paintArrow(g, direction, x, y, width, color);
        }

        /**
         * Paints an arrow with on the given pixel-coordinate and with the given pixel-width
         */
        private void paintArrow(Graphics g, int direction, int x, int y, int width, Color color) {
            g.setColor(Color.black);
            g.drawLine(x+1,y-2, x+width-1, y-2);     // horizontal black outline of arrow (without head)
            g.drawLine(x+1,y+2, x+width-1, y+2);     // horizontal black outline of arrow (without head)
            if (direction==VisualizationSettings.DIRECT) {
                g.drawLine(x,y-2, x, y+2);
                g.drawLine(x+width-5,y-5, x+width-5, y+5);
                g.drawLine(x+width-4,y-5, x+width-4, y+5);
                g.drawLine(x+width-3,y-4, x+width-3, y+4);
                g.drawLine(x+width-2,y-3, x+width-2, y+3);
                g.drawLine(x+width-1,y-2, x+width-1, y+2);
                g.drawLine(x+width,  y-1, x+width, y+1);
                g.drawLine(x+width+1,y, x+width+1, y);
            } else {
                g.drawLine(x+width,y-2, x+width, y+2);
                g.drawLine(x+5,y-5, x+5, y+5);
                g.drawLine(x+4,y-5, x+4, y+5);
                g.drawLine(x+3,y-4, x+3, y+4);
                g.drawLine(x+2,y-3, x+2, y+3);
                g.drawLine(x+1,y-2, x+1, y+2);
                g.drawLine(x,  y-1, x, y+1);
                g.drawLine(x-1,y, x-1, y);
            }
            g.setColor(color);
            g.drawLine(x+1,y-1, x+width-1, y-1);     // horizontal line of arrow (without head)
            g.drawLine(x+1,y, x+width-1, y); // horizontal line of arrow (without head)
            g.drawLine(x+1,y+1, x+width-1, y+1); // horizontal line of arrow (without head)
            if (direction==VisualizationSettings.DIRECT) {
                g.drawLine(x+width-4,y-4, x+width-4, y+4);
                g.drawLine(x+width-3,y-3, x+width-3, y+3);
                g.drawLine(x+width-2,y-2, x+width-2, y+2);
                g.drawLine(x+width-1,y-1, x+width-1, y+1);
                g.drawLine(x+width,  y, x+width, y);
            } else {
                g.drawLine(x+4,y-4, x+4, y+4);
                g.drawLine(x+3,y-3, x+3, y+3);
                g.drawLine(x+2,y-2, x+2, y+2);
                g.drawLine(x+1,y-1, x+1, y+1);
                g.drawLine(x,  y, x, y);
            }
        }

        public void dataUpdated() {
            analyzeStructure();
            structureLogoPanel.setClearOnNextPaint();
            structureLogoPanel.repaint(); // updates sequence logo            
        }

        /** Analyses the self-repeating (sub)structure of the matrix and creates a list of arrows */
        public void analyzeStructure() {
            arrows.clear();
            double[][] matrix=null;
            try {
                matrix=parseMatrixTable();
            } catch (ParseError e) {return;}
            if (matrix==null) return;
            matrix=Motif.getMatrixAsFrequencyMatrix(matrix);
            int linecounter=0;
            int size=matrix.length;
            for (int i=0;i<size;i++) {
                double sum=matrix[i][0]+matrix[i][1]+matrix[i][2]+matrix[i][3];
                for (int j=0;j<4;j++) matrix[i][j]=matrix[i][j]/sum;
            }
            // compare subwindows at different lengths and positions (starting with size=2 and going up)
            for (int windowsize=2;windowsize<=size/2;windowsize++) {
                for (int pos1=0;pos1<=size-windowsize*2;pos1++) {
                    boolean foundany=false;
                    Color color=colorlist[linecounter%colorlist.length];
                    for (int pos2=pos1+windowsize;pos2<=size-windowsize;pos2++) { // use: int pos2=pos1+windowsize to prevent overlapping subwindows?
                        int res=compareWindows(pos1, pos2, windowsize, matrix);
                        if (res!=0) {
                           foundany=true;
                           arrows.add(new StructureArrow(pos2, windowsize, (res>0)?VisualizationSettings.DIRECT:VisualizationSettings.REVERSE, linecounter, color));
                        }
                    }
                    if (foundany) {
                        arrows.add(new StructureArrow(pos1, windowsize, VisualizationSettings.DIRECT, linecounter, color));
                        linecounter++;
                    }
                }
            }

            for (int index=arrows.size()-1;index>=0;index--) {
                StructureArrow current=arrows.get(index);
                if(current.isprotected && current.length>2) {
                    markRepeatingSubstructures(current);
                } else {
                    if (!isCoveredByProtected(current)) { // this is "top level" => keep this
                        current.isprotected=true;
                        markLineAsProtected(current.line, true);
                        markRepeatingSubstructures(current);
                    } 
                }
            }
            int current=-1;
            int maxline=0;
            for (StructureArrow arrow:arrows) {
                if (arrow.line!=current) {
                    current=arrow.line;
                    if (arrow.line>maxline) maxline=arrow.line;
                }
            }
            for (int i=0;i<=maxline;i++) {
                if (isSubsetOfOtherLine(i)) markLineAsProtected(i, false); // remove "duplicate" line by marking it as unprotected
            }

            if (removepartials) removeUnprotected();
            // reorder lines
            int line=-1;
            current=-1;
            int lines=0;
            int[] map=new int[maxline+1]; // renumbers lines (0,1,2,3,...) to display lines (-1,1,-2,2,-3,3...)
            int[] colormap=new int[maxline+1]; // renumbers lines (0,1,2,3,...) to display lines (-1,1,-2,2,-3,3...)
            for (StructureArrow arrow:arrows) {
                if (arrow.line!=current) {
                    current=arrow.line;
                    map[current]=line;
                    colormap[current]=lines%colorlist.length;
                    if (line<0) line=line*(-1);
                    else line=line*(-1)-1;
                    lines++;
                }
            }
            for (StructureArrow arrow:arrows) {
                arrow.color=colorlist[colormap[arrow.line]];
                arrow.line=map[arrow.line];
            }
        }

        private void removeUnprotected() {
             Iterator<StructureArrow> iter=arrows.iterator();
             while (iter.hasNext()) {
                 StructureArrow a=iter.next();
                 if (!a.isprotected) iter.remove();
             }
        }

        private void markLineAsProtected(int line, boolean flag) {
             for (StructureArrow arrow:arrows) {
                 if (arrow.line==line) arrow.isprotected=flag;
             }
        }

        private boolean isCoveredByProtected(StructureArrow arrow) {
            int index=getNextAscending(arrow);
            if (index<0) return false;
            while (index<arrows.size()) {
                StructureArrow other=arrows.get(index);
                if (other.isprotected && other.pos<=arrow.pos && other.pos+other.length>=arrow.pos+arrow.length) return true;
                index++;
            }
            return false;
        }


        /** Returns the index in the arrows list of the arrow listed after the argument */
        private int getNextAscending(StructureArrow arrow) {
            for (int i=0;i<arrows.size()-1;i++) {
                if (arrows.get(i)==arrow) return i+1;
            }
            return -1;
        }

        /** Returns the index in the arrwos list of the arrow listed before the argument */
        private int getNextDescending(StructureArrow arrow) {
            for (int i=0;i<arrows.size();i++) {
                if (arrows.get(i)==arrow) return i-1;
            }
            return -1;
        }

        /** Returns true if the segments spanned by the arrow contains some repeated subsegments
         *  The methods also marks the line of these substructures as protected
         */
        private boolean markRepeatingSubstructures(StructureArrow arrow) {
            // it has repeated subsegments if there are multiple arrows one the same line fully covered by the arrow segment
            boolean isrepeating=false;
            int substructline=Integer.MAX_VALUE;
            int index=getNextDescending(arrow);
            for (int i=index;i>=0;i--) {
               StructureArrow other=arrows.get(i);
               if (other.pos>=arrow.pos && other.pos+other.length<=arrow.pos+arrow.length) { //
                   if (substructline==other.line) {
                       if (!isArrowsOverlappingOnLine(substructline)) {isrepeating=true;break;}
                   }
                   else substructline=other.line;
               } 
            }
            if (isrepeating) {// mark the non-overlapping repeating subsegments as protected!
                markLineAsProtected(substructline,true);
            }
            return isrepeating;
        }

        private boolean isArrowsOverlappingOnLine(int line) {
            ArrayList<StructureArrow> targetline=new ArrayList<StructureArrow>();
            for (StructureArrow arrow:arrows) {
                if (arrow.line==line) targetline.add(arrow);
            }
            for (int i=0;i<targetline.size()-1;i++) {
               StructureArrow outer=targetline.get(i);
               for (int j=i+1;j<targetline.size();j++) {
                  StructureArrow inner=targetline.get(j);
                  if (outer.pos>inner.pos+inner.length-1 || inner.pos>outer.pos+outer.length-1) continue;
                  else return true; // overlap!
               }
            }
            return false;
        }

        /** Returns TRUE if the line with the given index contains arrows that are fully covered by larger arrows on a different line */
        private boolean isSubsetOfOtherLine(int line) {
            ArrayList<StructureArrow> targetline=getLine(line);
            ArrayList<StructureArrow> otherline;
            for (int i=0;i<arrows.size()-1;i++) {
               if (i==line) continue;
               otherline=getLine(i);
               int covered=0;
               for (StructureArrow target:targetline) {
                   for (StructureArrow other:otherline) {
                       if (target.pos==other.pos && target.length==other.length) {covered++;break;}
                   }
               }
               if (covered==targetline.size()) return true;              
            }
            return false;
        }

        private ArrayList<StructureArrow> getLine(int line) {
            ArrayList<StructureArrow> targetline=new ArrayList<StructureArrow>();
            for (StructureArrow arrow:arrows) {
                if (arrow.line==line) targetline.add(arrow);
            }
            return targetline;
        }

        /** Compares to subsegments of windowsize at pos1 and pos2
         *  If the two subsegments are equal the function returns 1
         *  If they are equal on in reverse orientation the function returns -1
         *  If they have no similarity the function returns 0
         */
        private int compareWindows(int pos1, int pos2, int windowsize, double[][] matrix) {
            double directscore=0;
            double reversescore=0;
            for (int i=0;i<windowsize;i++) {
                double ratio=allr(matrix[pos1+i],matrix[pos2+i]);
                if (ratio<columnthreshold) {directscore=0;break;} // all columns must be above threshold, not just the average
                else directscore+=ratio;
            }
            for (int i=0;i<windowsize;i++) {
                double ratio=allr(matrix[pos1+i],reverseColumn(matrix[pos2+(windowsize-1)-i]));
                if (ratio<columnthreshold) {reversescore=0;break;}
                else reversescore+=ratio;
            }
            double threshold=columnthreshold*windowsize;
            if (directscore<threshold && reversescore<threshold) return 0;
            else if (directscore>reversescore) return 1;
            else return -1;
        }

        /** Returns the average log-likelihood ratio for the two given columns */
        private double allr(double[] colA, double[] colB) {
            double p=0.25; double pseudo=0.01;
            double[] columnA=new double[]{colA[0],colA[1],colA[2],colA[3]};
            double[] columnB=new double[]{colB[0],colB[1],colB[2],colB[3]};
            for (int i=0;i<columnA.length;i++) if (columnA[i]==0) columnA[i]=pseudo;
            for (int i=0;i<columnB.length;i++) if (columnB[i]==0) columnB[i]=pseudo;
            double leftSide=columnB[0]*Math.log(columnA[0]/p)+columnB[1]*Math.log(columnA[1]/p)+columnB[2]*Math.log(columnA[2]/p)+columnB[3]*Math.log(columnA[3]/p);
            double rightSide=columnA[0]*Math.log(columnB[0]/p)+columnA[1]*Math.log(columnB[1]/p)+columnA[2]*Math.log(columnB[2]/p)+columnA[3]*Math.log(columnB[3]/p);
            return (leftSide+rightSide)/2;
        }

        private double[] reverseColumn(double[] col) {
            return new double[]{col[3],col[2],col[1],col[0]};
        }

        private class StructureArrow {
             int direction=0;
             int pos=0;
             int length=0;
             int line=0;
             Color color=null;
             boolean isprotected=false;

             public StructureArrow(int pos, int length, int direction, int line, Color color) {
                 this.pos=pos;
                 this.length=length;
                 this.direction=direction;
                 this.line=line;
                 this.color=color;
             }
             @Override
             public String toString() {
                 return "{"+pos+"-"+(pos+length-1)+" ("+length+") dir="+direction+"  line="+line+"  protected="+isprotected+"   color="+color+"}";
             }
        }
  } // end class: StructurePanel


 private class MotifListPanel extends JPanel {
    String attribute="";
    DefaultTableModel model;
    GenericMotifBrowserPanel motifspanel;
    JButton editButton=null;

    
    public MotifListPanel(String attributename, ArrayList<String> motifnames) {
       this.attribute=attributename;
       this.setLayout(new BorderLayout());
       String[] columnNames=new String[]{"Motif","Name","Logo"};
       Object[][] tableData=new Object[motifnames.size()][3];
       for (int i=0;i<motifnames.size();i++) {
           Object[] row=new Object[3];
           String motifname=motifnames.get(i);
           row[0]=motifname;
           Data m=engine.getDataItem(motifname);
           if (m!=null && m instanceof Motif) {
               String showname=((Motif)m).getLongName();
               if (showname==null || showname.isEmpty()) showname=((Motif)m).getShortName();
               row[1]=showname;
               row[2]=((Motif)m);
           }
           tableData[i]=row;
       }
       model=new DefaultTableModel(tableData, columnNames) {
             @Override
             public Class getColumnClass(int c) {
                switch (c) {
                    case 0:return String.class;
                    case 1:return String.class;
                    case 2:return Motif.class;
                    default:return Object.class;
                }
             }

             @Override
             public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
             }

       };
       motifspanel=new GenericMotifBrowserPanel(gui, model, true, isModal());
       editButton=new JButton("Edit "+attribute);
       editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MotifCollection col=new MotifCollection(data.getName()+"_"+attribute);
                JTable table=motifspanel.getTable();
                for (int i=0;i<table.getRowCount();i++) {
                    String mname=(String)table.getValueAt(i, 0);
                    col.addMotifName(mname);
                }
                Prompt_MotifCollection selectionPrompt=new Prompt_MotifCollection(gui, null, col, true, "Select motifs", true);
                selectionPrompt.setHeaderVisible(false);
                selectionPrompt.setLocation(gui.getFrame().getWidth()/2-selectionPrompt.getWidth()/2, gui.getFrame().getHeight()/2-selectionPrompt.getHeight()/2);
                selectionPrompt.setVisible(true);
                col=(MotifCollection)selectionPrompt.getData();
                selectionPrompt.dispose();
                while (model.getRowCount()>0) model.removeRow(0); // remove all current motifs
                for (String mname:col.getAllMotifNames()) {
                     Data motifdata=engine.getDataItem(mname);
                     Motif motif=null;
                     if (motifdata!=null && motifdata instanceof Motif) {
                          motif=(Motif)motifdata;
                     } else {
                         motif=new Motif(mname);
                         motif.setMatrix(new double[][]{{0.25f,0.25f,0.25f,0.25f}});
                     }
                     Object[] row=new Object[3];
                     String showname=motif.getLongName();
                     if (showname==null || showname.isEmpty()) showname=motif.getShortName();
                     row[0]=motif.getName();
                     row[1]=showname;
                     row[2]=motif;
                     model.addRow(row);
                } // end add motifs from collection
            }
        });
       if (isDataEditable()) motifspanel.getControlsPanel().add(editButton);
       motifspanel.getTable().getColumn("Motif").setPreferredWidth(120);
       motifspanel.getTable().getRowSorter().toggleSortOrder(0);
       this.add(motifspanel);
    }
    
    public String[] getMotifList() {
        JTable table=motifspanel.getTable();
        String[] list=new String[table.getRowCount()];
        for (int i=0;i<table.getRowCount();i++) {
            String mname=(String)table.getValueAt(i, 0);
            list[i]=mname;
        }
        return list;
    }
    
    public void setEditable(boolean editable) {
        editButton.setVisible(editable);
    }

} // end class MotifListPanel

 
     @Override
    public void setDataEditable(boolean editable) {
        super.setDataEditable(editable);
        if (interactionsPanel!=null) interactionsPanel.setEditable(editable);
        if (alternativesPanel!=null) alternativesPanel.setEditable(editable);      
        addPropertyButton.setVisible(editable);
        GOPanel.setEditable(editable);        
    }  
 
}
