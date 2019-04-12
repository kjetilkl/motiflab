/*
 * ProtocolEditor.java
 *
 * Created on 7. oktober 2008, 14:44
 */

package motiflab.gui;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.TextUI;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import motiflab.engine.DataListener;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.SystemError;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.data.*;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.DataTypeTable;
import motiflab.engine.protocol.ParseError;
import motiflab.gui.operationdialog.OperationDialog;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_analyze;
import motiflab.engine.protocol.StandardProtocol;
import motiflab.external.ExternalProgram;



/**
 * 
 *
 * @author  kjetikl
 */
public class ProtocolEditor extends javax.swing.JPanel implements DataListener, Searchable {
    private ProtocolEditorPane protocolEditor;
    private LineNumberPanel leftmargin;
    private JPanel scrollingPanel;
    private JPanel statuspanel;
    private GradientPanel headerpanel;
    private JLabel headerlabel;
    private JButton changeProtocolButton;
    private InfoPanelIcon changeProtocolButtonIcon;
    private AbstractDocument document;
    private MotifLabGUI gui;
    private Protocol protocol;
    private ProtocolEditorContextMenu contextmenu;
    private JMenuItem editLineMenuItem;
    private String fontName=null;
    private int fontSize=0;    
    private Font font;
    private boolean antialias=true;    
    private JScrollPane protocolPanelScroll;
    private int lineHeight=0;
    private int leading=0;
    private OperationTask editTask; // this represents the task currently being edited
    private javax.swing.Icon errorIcon=new MiscIcons(MiscIcons.ERROR_ICON);
    private javax.swing.Timer repaintTimer;
    private CaretPositionPanel caretPositionPanel;
    private JLabel insertOrOverwriteLabel;
    private ImageIcon redbullet;
    private ImageIcon greenbullet;
    private ImageIcon yellowbullet;
    private ImageIcon blackbullet;
    private ImageIcon checkbullet;
    private ImageIcon executionAbortedBullet;
    private ImageIcon executionErrorBullet;
    private ImageIcon currentLineIndicatorIcon;
    private JLabel errorBulletLabel;
    private HashMap<String,Color> patternColors; // colors for 'global' identifiers
    private HashSet<String> variablenamepatterns; // Compiled regex patterns for variable names
    private int parseErrorsDetected; //
    private JLabel statusLabel;
    private ProtocolDocumentListener protocolDocumentListener;
    private GUIUndoManager undoManager;
    private DocumentListener undoRedoDocumentListener;   
    private ProtocolManager protocolManager;
    private JLabel currentExecutingLineIndicatorLabel;
    private JPopupMenu changeProtocolSubmenu;
    private ChangeProtocolMenuListener changeProtocolMenuListener;
    private boolean notifyExecutionProgress=false; // draw icons in the editor-margin to show progress of protocol execution
    private String previousSearchstring="";
    private Object[] lastException=null; // triple: [Protocol name (string), line number (Integer), error message (String)]

    private static final Color orange=new Color(230,180,0);
    private Color color_comments=Color.gray;
    private Color color_displaysetting=Color.CYAN;
    private Color color_textstring=new Color(0,200,0); // the ones inside double quotes
    private Color color_number=Color.MAGENTA;
    private Color color_data=Color.BLUE;
    private Color color_datatypes=orange;
    private Color color_operations=Color.RED;
    private Color color_analyses=orange;
    private Color color_dataformats=orange;
    private Color color_programs=orange;
    private Color color_flow_control=new Color(255,153,153); // pink

    public static final String COLOR_COMMENTS="Comments";
    public static final String COLOR_DISPLAY_SETTINGS="Display settings";
    public static final String COLOR_TEXT="Text strings";
    public static final String COLOR_NUMBER="Numbers";
    public static final String COLOR_DATA="Data objects";
    public static final String COLOR_DATAFORMAT="Data formats";
    public static final String COLOR_DATATYPE="Data types";
    public static final String COLOR_OPERATION="Operations";
    public static final String COLOR_ANALYSIS="Analyses";
    public static final String COLOR_PROGRAMS="Programs";
    public static final String COLOR_FLOW_CONTROL="Flow control";    
    
    public static final String FONT_NAME="ProtocolEditor_fontName";
    public static final String FONT_SIZE="ProtocolEditor_fontSize";
    public static final String ANTIALIAS="ProtocolEditor_antialias";

    /** Creates new form OutputPanel */
    public ProtocolEditor(MotifLabGUI gui) {
        initComponents();
        this.gui=gui;
        Preferences preferences = Preferences.userNodeForPackage(MotifLabGUI.class); 
        String useFontName=preferences.get(FONT_NAME,null);
        int useFontSize=preferences.getInt(FONT_SIZE,0);
        antialias=preferences.getBoolean(ANTIALIAS,false);
        
        protocolDocumentListener = new ProtocolDocumentListener();
        undoManager = gui.getUndoManager();
        undoRedoDocumentListener = gui.getUndoRedoDocumentListener(); 
        protocolManager = new ProtocolManager(this);
        variablenamepatterns=new HashSet<String>();
        setupProtocolColors();
        java.net.URL redBulletURL=getClass().getResource("resources/icons/redbullet.png");
        java.net.URL greenBulletURL=getClass().getResource("resources/icons/greenbullet.png");
        java.net.URL yellowBulletURL=getClass().getResource("resources/icons/yellowbullet.png");
        java.net.URL blackBulletURL=getClass().getResource("resources/icons/blackbullet.png");
        java.net.URL checkBulletURL=getClass().getResource("resources/icons/checkbullet.png");
        java.net.URL executionAbortedBulletURL=getClass().getResource("resources/icons/smallStopButton.png");
        java.net.URL executionErrorBulletURL=getClass().getResource("resources/icons/crossbullet.png");
        java.net.URL currentLineIndicatorURL=getClass().getResource("resources/icons/redRightArrow.png");
        redbullet=new ImageIcon(redBulletURL); 
        greenbullet=new ImageIcon(greenBulletURL); 
        yellowbullet=new ImageIcon(yellowBulletURL); 
        blackbullet=new ImageIcon(blackBulletURL);
        checkbullet=new ImageIcon(checkBulletURL);
        executionAbortedBullet=new ImageIcon(executionAbortedBulletURL);
        executionErrorBullet=new ImageIcon(executionErrorBulletURL);
        currentLineIndicatorIcon=new ImageIcon(currentLineIndicatorURL);
        this.setLayout(new BorderLayout());   
        protocolEditor=new ProtocolEditorPane();
        setFont(useFontName,useFontSize);
        protocolEditor.setEditorKit(new ColoringEditorKit());
        protocolEditor.setSelectionColor(new Color(200,200,255));
        errorBulletLabel=new JLabel(greenbullet);
        Dimension errorBulletLabelSize=new Dimension(30,20);
        errorBulletLabel.setPreferredSize(errorBulletLabelSize);
        errorBulletLabel.setMinimumSize(errorBulletLabelSize);
        errorBulletLabel.setMaximumSize(errorBulletLabelSize);
        errorBulletLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));               
        errorBulletLabel.setHorizontalAlignment(JLabel.CENTER);            
        caretPositionPanel=new CaretPositionPanel();
        Dimension caretPositionPanelSize=new Dimension(60,20);
        caretPositionPanel.setPreferredSize(caretPositionPanelSize);
        caretPositionPanel.setMinimumSize(caretPositionPanelSize);
        caretPositionPanel.setMaximumSize(caretPositionPanelSize);
        caretPositionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));               
        protocolEditor.addCaretListener(caretPositionPanel);
        leftmargin=new LineNumberPanel();
        Dimension leftmarginsize=new Dimension(46,200);
        leftmargin.setPreferredSize(leftmarginsize);
        leftmargin.setMinimumSize(leftmarginsize);
        statuspanel=new JPanel();
        Dimension statusfieldsize=new Dimension(200,24);
        statuspanel.setPreferredSize(statusfieldsize);
        statuspanel.setMinimumSize(statusfieldsize);
        statuspanel.setLayout(new BorderLayout());
        JPanel insetPanel1=new JPanel();
        insetPanel1.setLayout(new BoxLayout(insetPanel1, BoxLayout.LINE_AXIS));
        insetPanel1.add(errorBulletLabel);
        insetPanel1.add(caretPositionPanel);
        insertOrOverwriteLabel=new JLabel("INS");
        Dimension insertOrOverwriteLabelSize=new Dimension(34,20);
        insertOrOverwriteLabel.setPreferredSize(insertOrOverwriteLabelSize);
        insertOrOverwriteLabel.setMinimumSize(insertOrOverwriteLabelSize);
        insertOrOverwriteLabel.setMaximumSize(insertOrOverwriteLabelSize);
        insetPanel1.add(insertOrOverwriteLabel);
        insertOrOverwriteLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        insertOrOverwriteLabel.setHorizontalAlignment(JLabel.CENTER);
        statuspanel.add(insetPanel1,BorderLayout.WEST);
        statusLabel=new JLabel("");
        statuspanel.add(statusLabel,BorderLayout.CENTER);
        scrollingPanel=new JPanel();
        scrollingPanel.setLayout(new BorderLayout());
        scrollingPanel.add(leftmargin,BorderLayout.WEST);
        scrollingPanel.add(protocolEditor,BorderLayout.CENTER);
        headerpanel=new GradientPanel();
        Dimension headersize=new Dimension(200,21);
        headerpanel.setPreferredSize(headersize);
        headerpanel.setMinimumSize(headersize);      
        headerpanel.setBorder(BorderFactory.createEmptyBorder(2,5,2,2));
        headerpanel.setLayout(new BoxLayout(headerpanel, BoxLayout.X_AXIS));
        headerlabel=new JLabel("");
        headerlabel.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
        headerlabel.setForeground(java.awt.Color.WHITE);
        headerpanel.add(headerlabel);
        changeProtocolButtonIcon=new InfoPanelIcon(InfoPanelIcon.TRIANGLE_DOWN_ICON);
        changeProtocolButton=new JButton(changeProtocolButtonIcon);
        changeProtocolButton.setPreferredSize(new Dimension(27, 18));
        JPanel changeProtocolButtonPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        changeProtocolButtonPanel.setOpaque(false);
        changeProtocolButtonPanel.add(changeProtocolButton);
        headerpanel.add(changeProtocolButtonPanel);
        headerpanel.setBackground(java.awt.Color.BLUE);
        headerpanel.setOpaque(true);
        this.add(headerpanel,BorderLayout.NORTH);
        this.add(statuspanel,BorderLayout.SOUTH);
        protocolPanelScroll=new JScrollPane(scrollingPanel);    
        this.add(protocolPanelScroll,java.awt.BorderLayout.CENTER);
        currentExecutingLineIndicatorLabel=new JLabel(currentLineIndicatorIcon);
        contextmenu=new ProtocolEditorContextMenu();
        
        protocolEditor.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }           
        });
        repaintTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reparse();
                scrollingPanel.repaint();
            }
        });
        protocolEditor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ProtocolEditor.this.gui.setSearchTargetComponent(null);
            }
        });
        repaintTimer.setRepeats(false);
        repaintTimer.setCoalesce(true);
        gui.getEngine().addDataListener(this);
        changeProtocolSubmenu=new JPopupMenu();
        changeProtocolMenuListener=new ChangeProtocolMenuListener();
        changeProtocolButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                changeProtocolSubmenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        });
        changeProtocolButton.setEnabled(false);
        installProtocol(null);
    }
        
    /** Returns a reference to the GUI */
    public MotifLabGUI getGUI() {
        return gui;
    }
   
    
    public final void setFont(String fontname, int size) {
        if (fontname==null) fontname=Font.MONOSPACED;
        if (size<6) size=12;
        if (this.fontName!=null && this.fontName.equals(fontname) && this.fontSize==size) return; // no need for update
        this.fontName=fontname;
        this.fontSize=size;
        font=new Font(fontName,Font.PLAIN,fontSize);
        FontMetrics fontmetrics=protocolEditor.getFontMetrics(font);
        lineHeight=fontmetrics.getHeight();
        leading=fontmetrics.getLeading();
        protocolEditor.setFont(font); 
        repaint();
    }
    
    public final void setFont(String fontname) {
        Font currentFont=protocolEditor.getFont();
        int size=(currentFont!=null)?currentFont.getSize():12;
        setFont(fontname,size);        
    }
    
    public final void setFontSize(int size) {
        Font currentFont=protocolEditor.getFont();
        String fontname=(currentFont!=null)?currentFont.getFontName():null;
        setFont(fontname,size);
    }    

    public void setAntialias(boolean antialias) {
        if (this.antialias!=antialias) {
           this.antialias=antialias;
           repaint();
        }        
    }
    
    /**
     * Installs a new Protocol in the ProtocolEditor
     * (this will register all required listeners, set the document in the editor and
     * install the name of the protocol in the header, plus update relevant GUI actions and buttons)
     * Note: to change the protocol currently being edited, consider using changeProtocol() instead
     * since that method will also create and queue an undoable event
     */
    private void installProtocol(Protocol newprotocol) {
        setNotifyExecutionProgress(false);
        if (protocol!=null) protocolManager.setCaretPosition(protocol, protocolEditor.getCaretPosition()); // store caret position in current protocol
        protocol=newprotocol; // link to field variable
        if (protocol!=null) {
            document=(AbstractDocument)newprotocol.getDocument(); // link to field variable
            document.addUndoableEditListener(undoManager);
            document.addDocumentListener(undoRedoDocumentListener); 
            document.addDocumentListener(protocolDocumentListener); 
            protocolEditor.setDocument(document);
            protocolManager.setCurrent(protocol.getName());
            protocolEditor.setVisible(true);
            try {protocolEditor.setCaretPosition(protocolManager.getCaretPosition(protocol));} catch(Exception e){}
            leftmargin.setVisible(true);
            headerpanel.setVisible(true);
            caretPositionPanel.setText("1:1 ");
            gui.setCloseEnabled(true);
            gui.setSaveEnabled(((StandardProtocol)newprotocol).isDirty());
            gui.setSaveAsEnabled(true);
            if (!gui.isRecording()) {
                gui.setRecordEnabled(true);
                gui.setExecuteEnabled(true);
                gui.setStopEnabled(false);
                gui.unselectRecordPlayStop();
            }
            gui.enableCommentSelectedLinesInProtocol(true);
        } else {
            protocolManager.setCurrent(null);
            protocolEditor.setVisible(false);
            headerpanel.setVisible(false);
            leftmargin.setVisible(false);
            caretPositionPanel.setText("");
            caretPositionPanel.repaint();
            gui.setCloseEnabled(false);
            gui.setSaveEnabled(false);
            gui.setSaveAsEnabled(false);
            gui.setRecordEnabled(false);
            gui.setRecordMode(false);
            gui.setExecuteEnabled(false);
            gui.setStopEnabled(false);
            gui.enableCommentSelectedLinesInProtocol(false);
        }
        populateProtocolList();
        updateProtocolRendering();
    }

    /**
     * Reparses the protocol and updates the errors
     */
    public void updateProtocolRendering() {
        leftmargin.removeAll(); // removes old parse errors 
        leftmargin.repaint();
        reparse(); // this will also set the header!
        scrollingPanel.repaint();
    }
    
    
   /**
    * Populates the list of currently open protocols in the header
    */
   public void populateProtocolList() {
        changeProtocolSubmenu.removeAll();
        //System.err.println("protocolListChanges: "+manager.getSize()+" protocols");
        if (protocolManager.getSize()==0) { // no open protocol scripts. Disable play/rec-buttons
            changeProtocolButton.setEnabled(false);
        } else { // protocol scripts are present
           changeProtocolButton.setEnabled(true);
           Protocol currentProtocol=getProtocol();
           String currentProtocolName="";
           if (currentProtocol!=null) currentProtocolName=currentProtocol.getName();
           for (int i=0;i<protocolManager.getSize();i++) {
               Protocol prot=(Protocol)protocolManager.getElementAt(i);
               JCheckBoxMenuItem item=new JCheckBoxMenuItem(prot.getName());
               if (currentProtocolName.equals(prot.getName())) item.setSelected(true);
               else item.setSelected(false);
               item.addActionListener(changeProtocolMenuListener);
               changeProtocolSubmenu.add(item);
           }
        }
    }
    
    /**
     * This method installs a brand new Protocol script in the editor
     * If the user supplies a Protocol script as an argument, this protocol will be
     * installed, else a new Protocol with a generic name will be created and installed instead
     * The call to this method results in an undoable event being registered on the undo queue
     */
    public void newProtocol(Protocol newprotocol) {
        String undoname="Open protocol";
        if (newprotocol==null) {
            undoname="New protocol";
            newprotocol=protocolManager.createNewProtocol();
        } 
        protocolManager.addProtocol(newprotocol);        
        String newProtocolName=newprotocol.getName();
        changeProtocol(newProtocolName, undoname, true);
    }
    
    /**
     * This method installs a brand new Protocol script in the editor
     * If the user supplies a Protocol script as an argument, this protocol will be
     * installed, else a new Protocol with a generic name will be created and installed instead
     * The call to this method results in an undoable event being registered on the undo queue
     */
    public void newProtocol(Protocol newprotocol, String undoname) {
        if (newprotocol==null) {
            newprotocol=protocolManager.createNewProtocol();
        } 
        protocolManager.addProtocol(newprotocol);        
        String newProtocolName=newprotocol.getName();
        changeProtocol(newProtocolName, undoname, true);
    }    
    
    /**
     * Replaces the contents of the old protocol with the contents of the new protocol
     * The old protocol should already be open (but not necessarily "installed"), while
     * the new protocol should not be installed (it will overwrite the old) 
     * This method is called when a user "opens" a file which is already open
     * @param protocolName The name of the protocol to be reverted
     * @param newProtocol The new protocol to replace the old
     */
    public void revertProtocol(String protocolName, Protocol newProtocol) {
        Protocol oldProtocol = protocolManager.getProtocol(protocolName);
        RecordingCompoundEdit undoableEdit= new RecordingCompoundEdit("Revert protocol");
        undoManager.forwardUndoEvents(undoableEdit);
        PlainDocument doc=(PlainDocument)oldProtocol.getDocument();
        PlainDocument newdoc=(PlainDocument)newProtocol.getDocument();
        //changeProtocol(protocolName, null, true);
        changeProtocol(protocolName);
        try {
            doc.replace(0, doc.getLength(), newdoc.getText(0, newdoc.getLength()), null);
        } catch (Exception e) {}
        undoableEdit.end();
        undoManager.forwardUndoEvents(null);
        undoManager.addEdit(undoableEdit);                
    }
    
    /**
     * This method will install a previously opened Protocol script with the given 
     * name in the ProtocolEditor. If no Protocol with the specified name is currently 
     * registered with the ProtocolManager then nothing will happen
     * @param protocolName The name of the Protocol to change to
     * @param undoableEventName A name for the undoable event. If this is NULL it will default to "Change protocol"
     * @param undoable (if this is set to TRUE an undoable event will be queued)
     */
    public void changeProtocol(String protocolName, String undoableEventName, boolean undoable) {
        if (undoableEventName==null) undoableEventName="Change protocol";
        String oldProtocolName=null;
        if (protocol!=null) oldProtocolName=protocol.getName();       
        installProtocolEdit edit=new installProtocolEdit(undoableEventName,oldProtocolName, protocolName); // this will automatically uninstall the old protocol and install the new
        if (undoable) undoManager.addEdit(edit);
        
               
    }

    /**
     * This method will install a previously opened Protocol script with the given 
     * name in the ProtocolEditor. If no Protocol with the specified name is currently 
     * registered with the ProtocolManager then nothing will happen
     * A call to this method will not result in the queuing of an undoable event! 
     * @param protocolName The name of the Protocol to change to
     */    
    public void changeProtocol(String protocolName) {
        String oldProtocolName=null;
        if (protocol!=null) oldProtocolName=protocol.getName();    
        changeInstalledProtocol(oldProtocolName,protocolName);
    }    
    
    /** 
     * Parses the protocol script and updates information about ParseErrors and data types 
     * which can be used to "beautify" the rendering of the protocol. This method should be 
     * called intermittently 
     */
    private void reparse() {
        //errorBulletLabel.setIcon(yellowbullet);
        parseErrorsDetected=0;
        if (protocol!=null) {
            DataTypeTable lookup=protocol.getDataTypeLookupTable();
            lookup.clear();
            lookup.populateFromEngine(); //
            int numlines=getNumberOfLines();
            if (numlines>0) {
                for (int i=0;i<numlines;i++) {
                   int ypos=(i+1)*lineHeight-leading;
                   try {
                       protocol.parseCommand(i+1,false);
                   } catch (ParseError pe) {
                       JLabel errorLabel=new JLabel(errorIcon);
                       leftmargin.add(errorLabel);
                       errorLabel.setLocation(5, ypos-8);
                       errorLabel.setSize(8,8);
                       errorLabel.setToolTipText(pe.getMessage());
                       parseErrorsDetected++;
                   }         
                }   
                setupDataPatternColors(); // register the names of data objects in the editors color map
            }
        } // end if protocol != null
        leftmargin.validate();
        if (parseErrorsDetected>0) {
            errorBulletLabel.setIcon(redbullet);
            statusLabel.setText("      "+parseErrorsDetected+" parse error"+((parseErrorsDetected==1)?"":"s")+" detected");
        }
        else {
            if (protocol==null) errorBulletLabel.setIcon(blackbullet);
            else errorBulletLabel.setIcon(greenbullet);
            statusLabel.setText("");
        }   
        if (protocol!=null) {
            if (((StandardProtocol)protocol).isDirty()) setHeader(protocol.getName()+" *");
            else setHeader(protocol.getName());
        } else {
             setHeader("");
        }
        headerlabel.repaint();
    }
    
    private void setHeader(String headertext) {
        headerlabel.setText(headertext);
    }
    
    private void showContextMenu(MouseEvent e) {   
        // reparse();
        int linenumber=e.getY()/lineHeight+1;
        editTask=null;
        try {
            ExecutableTask ed=protocol.parseCommand(linenumber,true); //
            if (ed instanceof OperationTask) {
                editTask=(OperationTask)ed;
                editTask.setLineNumber(linenumber);
            }
        } catch (Exception ex) {
//            String msg=ex.getMessage();
//            if (msg!=null && !msg.equals("null")) gui.logMessage(msg);
        }
        if (editTask!=null) editLineMenuItem.setEnabled(true);
        else editLineMenuItem.setEnabled(false);
        //if (editTask!=null) editTask.debug();
        contextmenu.show(e.getComponent(),e.getX(),e.getY());
    }
    

    /** Returns the number of the line that currently contains the cursor (starting at 1) */
    public int getCurrentLine() {
        return protocolEditor.getCurrentLine();  
    }
    
    /** Returns the number of lines (or commands) in the script (including comment lines) */
    public int getNumberOfLines() {
        if (document==null || document.getLength()==0) return 0;
        Element root=document.getDefaultRootElement();
        return root.getElementCount();
    }

    /** Returns the start and end lines (starting at 0) of the currently selected text
     *  or NULL if no text is selected in the protocol editor
     */
    public int[] getSelectedLines() {
        if (document==null || document.getLength()==0) return null;
        String selection=protocolEditor.getSelectedText();
        if (selection==null || selection.isEmpty()) return null;
        int startpos=protocolEditor.getSelectionStart();
        int endpos=protocolEditor.getSelectionEnd()-1; // -1 is to avoid including lines with caret before first position
        Element root=document.getDefaultRootElement();
        int startline=root.getElementIndex(startpos);
        int endline=root.getElementIndex(endpos);
        return new int[]{startline,endline};
    }   
  
    /**
     * Selects the specified lines in the protocol
     * @param range specifies as min and max lines
     */
    public void selectLines(int[] range) {
         Element root=document.getDefaultRootElement();
         Element startElement=root.getElement(range[0]);
         Element endElement=root.getElement(range[1]);
         int start=startElement.getStartOffset();
         int end=endElement.getEndOffset();
         protocolEditor.select(start, end);                
    }
    
    /** Add a comment sign # before the selected lines in the protocol */
    public void commentSelectedLines() {
        int[] range=getSelectedLines();
        if (range==null) return;
        int min=range[0];
        int max=range[1];
        Element root=document.getDefaultRootElement();
        Element startElement=root.getElement(min);
        Element endElement=root.getElement(max);
        int start=startElement.getStartOffset();
        int end=endElement.getEndOffset();
        if (end>document.getLength()) end=document.getLength();  
        RecordingCompoundEdit compoundEdit=new RecordingCompoundEdit("comment lines"); // using compoundEdit because replaceText() results in 2 edit events (one for remove and one for insert)
        undoManager.forwardUndoEvents(compoundEdit);
        try {
            String text=protocol.getDocument().getText(start, (end-start)); // note: not using (end-start+1)   
            int length=text.length();          
            text="#"+text; // add # to first line
            text=text.replaceAll("\n", "\n#"); // add # to subsequent lines    
            if (text.endsWith("\n#")) text=text.substring(0,text.length()-1);
            ((StandardProtocol)protocol).replaceText(start, length, text); 
            selectLines(range); // update selection
        } catch (Exception e) {gui.logMessage("comment error(end="+end+" length="+protocol.getDocument().getLength()+"): "+e.getMessage());}
        compoundEdit.end();
        undoManager.forwardUndoEvents(null);
        undoManager.addEdit(compoundEdit);
    }
    
    /** Remove any comment signs # before the selected lines in the protocol */
    public void uncommentSelectedLines() {
        int[] range=getSelectedLines();
        if (range==null) return;
        int min=range[0];
        int max=range[1];
        Element root=document.getDefaultRootElement();
        Element startElement=root.getElement(min);
        Element endElement=root.getElement(max);
        int start=startElement.getStartOffset();
        int end=endElement.getEndOffset();
        if (end>document.getLength()) end=document.getLength();  
        RecordingCompoundEdit compoundEdit=new RecordingCompoundEdit("uncomment lines"); // using compoundEdit because replaceText() results in 2 edit events (one for remove and one for insert)
        undoManager.forwardUndoEvents(compoundEdit);        
        try {
            String text=protocol.getDocument().getText(start, (end-start));
            int length=text.length();
            while (text.startsWith(" ")) text=text.substring(1);
            if (text.startsWith("#")) text=text.substring(1);
            text=text.replaceAll("\n *#", "\n"); // remove leading spaces on a line followed by a single #
            ((StandardProtocol)protocol).replaceText(start, length, text); 
            selectLines(range); // update selection
        } catch (Exception e) {gui.logMessage("uncomment error(end="+end+" length="+protocol.getDocument().getLength()+"): "+e.getMessage());}
        compoundEdit.end();
        undoManager.forwardUndoEvents(null);
        undoManager.addEdit(compoundEdit);        
    }    
    
    /** Executes the protocol line currently occupied by the caret */
    public void executeCurrentLine() {
        protocolEditor.executeAction.actionPerformed(null);
    }    

    /**
     * This is a callback-method called by the GUI when a protocol has started executing
     * @param runningProtocol
     * @param line
     */
    public void protocolExecutionStarted(String protocolName) {
       notifyExecutionProgress=true;
       if (!protocol.getName().equals(protocolName)) return;
       currentExecutingLineIndicatorLabel.setIcon(currentLineIndicatorIcon);
       currentExecutingLineIndicatorLabel.setSize(currentLineIndicatorIcon.getIconWidth(), currentLineIndicatorIcon.getIconHeight());
       currentExecutingLineIndicatorLabel.setToolTipText("Currently executing line");
       leftmargin.removeAll();
       leftmargin.repaint();
    }

    /**
     * This is a callback-method called by the GUI when a line of a protocol has started executing
     * It will place the "currently executing line" icon in the margin before the protocol line
     * @param runningProtocol
     * @param line
     */
    public void protocolLineExecutionStarted(String protocolName, int line) {      
       if (!(notifyExecutionProgress && protocol.getName().equals(protocolName))) return;
       int ypos=line*lineHeight-leading;
       leftmargin.remove(currentExecutingLineIndicatorLabel); // just in case it is already there, we remove it before adding it back
       leftmargin.add(currentExecutingLineIndicatorLabel);
       currentExecutingLineIndicatorLabel.setLocation(5, ypos-currentExecutingLineIndicatorLabel.getHeight());
       leftmargin.repaint();
    }

    /**
     * This is a callback-method called by the GUI when a line of a protocol has finished executing
     * It will remove the "currently executing line" icon from the margin and replace it with an "OK" icon to indicate
     * that the execution of the line finished successfully
     * @param runningProtocol
     * @param line
     */
    public void protocolLineExecutionFinished(String protocolName, int line) {      
       if (!(notifyExecutionProgress && protocol.getName().equals(protocolName))) return;
       leftmargin.remove(currentExecutingLineIndicatorLabel);
       int ypos=line*lineHeight-leading;
       JLabel bulletLabel=new JLabel(checkbullet);
       leftmargin.add(bulletLabel);
       bulletLabel.setLocation(5, ypos-10);
       bulletLabel.setSize(10,10);
       leftmargin.repaint();
    }

    /**
     * This is a callback-method called by the GUI when the execution of a line of a protocol has been aborted by the user
     * @param runningProtocol
     * @param line
     */
    public void protocolLineExecutionAborted(String protocolName, int line) {
       if (!(notifyExecutionProgress && protocol.getName().equals(protocolName))) return;
       int ypos=line*lineHeight-leading;
       currentExecutingLineIndicatorLabel.setIcon(executionAbortedBullet);
       currentExecutingLineIndicatorLabel.setSize(executionAbortedBullet.getIconWidth(),executionAbortedBullet.getIconHeight());
       currentExecutingLineIndicatorLabel.setLocation(5, ypos-currentExecutingLineIndicatorLabel.getHeight());
       currentExecutingLineIndicatorLabel.setToolTipText("Execution aborted by user at line "+line);
       leftmargin.repaint();
    }

    /**
     * This is a callback-method called by the GUI when the execution of a line of a protocol has been stopped due to an error
     * @param runningProtocol
     * @param line
     */
    public void protocolLineExecutionError(String protocolName, int line) {
       if (!(notifyExecutionProgress && protocol.getName().equals(protocolName))) return;
       int ypos=line*lineHeight-leading;
       currentExecutingLineIndicatorLabel.setIcon(executionErrorBullet);
       currentExecutingLineIndicatorLabel.setSize(executionErrorBullet.getIconWidth(),executionErrorBullet.getIconHeight());
       currentExecutingLineIndicatorLabel.setLocation(5, ypos-currentExecutingLineIndicatorLabel.getHeight());
       String errorMessage="Execution stopped due to an error at line "+line;
       if (lastException!=null) {
           String prot=((String)lastException[0]);
           if (prot!=null && prot.equals(protocolName) && ((Integer)lastException[1])==line) errorMessage+=" : "+(String)lastException[2];
       }
       currentExecutingLineIndicatorLabel.setToolTipText(errorMessage);
       leftmargin.repaint();
    }
    /**
     * Returns a reference to the Protocol script currently being edited
     * @return
     */
    public Protocol getProtocol() {
        return protocol;
    }
    
    /**
     * Returns a reference to the ProtocolManager which keeps track of all
     * Protocol scripts that are currently open (but not necessarily "installed" in the ProtocolEditor)
     * @return
     */    
    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
    
    /**
     * Returns the current position of the caret
     * @return
     */
    public int getCaretPosition() {
        return protocolEditor.getCaretPosition();
    } 

    
    /** Creates a new protocol based on the current protocol but with all macros expanded */
    public void expandMacrosInCurrentProtocol() {
        if (protocol!=null && protocol instanceof StandardProtocol) {            
            String newprotocolname=protocol.getName()+"-[macro expanded]";
//            if (protocolManager.isProtocolOpen(newprotocolname)) {
//                int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "A protocol with the same name is already open.\nWould you like to replace this protocol?","Expand macros",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
//                if (choice!=JOptionPane.OK_OPTION) return;
//            }
            try {
              Protocol newProtocol=((StandardProtocol)protocol).expandAllMacros();
              newProtocol.setName(newprotocolname);             
              if (protocolManager.getProtocol(newprotocolname)!=null) revertProtocol(newprotocolname,newProtocol); // replace existing protocol with same name
              else newProtocol(newProtocol); // installs the new protocol script and adds it to the ProtocolManager
            } catch (ParseError e) {
              JOptionPane.showMessageDialog(gui.getFrame(), e.getMessage(),"Macro error" ,JOptionPane.ERROR_MESSAGE);   
            }
        }      
    }      
    
   /**
     * Inserts an operation-command into the protocol at the line corresponding to
     * the specified caret position. If the caret position is in the middle of a line
     * already occupied by a command the new command will be inserted before the old
     * command (i.e. the new command will be placed at the specified line and the old
     * command (and subsequent lines) will be bumped down 1 line).
     * @param task
     * @param pos
     */
    public boolean insertOperationAt(ExecutableTask task, int pos) {
        Element lineelement=document.getParagraphElement(pos);
        if (lineelement==null) return appendOperation(task);
        else {
            pos=lineelement.getStartOffset();
            return protocol.insertOperationAt(task, pos);
        }       
    }
    
    public boolean insertStringAt(String string, int pos) {
        Element lineelement=document.getParagraphElement(pos);
        if (lineelement==null) return false;
        else return ((StandardProtocol)protocol).insertStringAt(string, pos);      
    }    
    
    /**
     * Appends an operation-command to the end of the protocol 
     * @param task
     */
    public boolean appendOperation(ExecutableTask task){
        return protocol.appendOperation(task);
    }

    /**
     * Replaces the command currently occupying the given line with a new operation
     * @param task
     */
    public boolean replaceCommandAtLine(ExecutableTask task, int line){
        Element lineelement=document.getDefaultRootElement().getElement(line-1);
        if (lineelement==null) return false;
        RecordingCompoundEdit undoableEdit=new RecordingCompoundEdit("replace");
        undoManager.forwardUndoEvents(undoableEdit);
        protocolEditor.select(lineelement.getStartOffset(), lineelement.getEndOffset());
        protocolEditor.replaceSelection(task.getCommandString(protocol)+"\n");
        undoableEdit.end();
        undoManager.forwardUndoEvents(null);
        undoManager.addEdit(undoableEdit);
        return true;
    }
    
    /** This method can be used to notify the Protocol Editor of the last exception that occurred while executing a protocol
     *  This information is used for tooltips in the margin
     *  @param exceptionMessage The message for the last exception. Set to NULL to clear
     */
    public void setException(Exception exception) {      
        if (exception==null || getProtocol()==null) lastException=null;
        else {
            int lineNumber=-1;
            if (exception instanceof ExecutionError) lineNumber=((ExecutionError)exception).getLineNumber();
            else if (exception instanceof ParseError) lineNumber=((ParseError)exception).getLineNumber();
            String exceptionMessage=exception.getMessage();
            lastException=new Object[]{getProtocol().getName(),lineNumber,exceptionMessage};
        }
    }
    
    /**
     * A call to this method launches the modal operations dialog where the
     * user can choose parameters for an operation
     * @param replace Set this to true if the command should replace the one currently under the caret
     * or false it a new command should be inserted before the one currently under the caret
     */
    public void launchOperationEditor(OperationTask operationtask, boolean replace) {
        String opstring=operationtask.getOperationName();
        operationtask.setParameter(OperationTask.ENGINE, gui.getEngine());
        //logMessage("Launcing Operations Editor: Operation="+opstring+", source="+sourceDatastring+", target="+targetDatastring);
        int linenumber=0;
        if (replace) linenumber=operationtask.getLineNumber();
        else {
            linenumber=document.getDefaultRootElement().getElementIndex(protocolEditor.getCaretPosition())+1;         
        }
        DataTypeTable typetable=protocol.getDataTypeStateAtCommand(linenumber-1);
        try {
            Class dialogclass = Class.forName("motiflab.gui.operationdialog.OperationDialog_"+opstring);
            OperationDialog dialog=(OperationDialog)dialogclass.newInstance();
            dialog.initialize(operationtask,typetable,gui);
            dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
            dialog.setVisible(true);               
            if (dialog.okPressed()) {
               ExecutableTask executable=dialog.getOperationTask(); // because the task might have changed!
               if (replace) replaceCommandAtLine(executable, operationtask.getLineNumber());
               else insertOperationAt(executable, protocolEditor.getCaretPosition());                   
            }
            dialog.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(gui.getFrame(), e.getClass().toString()+"\n"+e.getMessage(),"Operations editor" ,JOptionPane.ERROR_MESSAGE);
            if (!(e instanceof ExecutionError || e instanceof ParseError || e instanceof SystemError)) {
                 gui.getEngine().reportError(e);
                 e.printStackTrace(System.err);
            } 
        }                                
  } 
    
    /**
     * Looks for matches to the regular expression in the document and replaces
     * all found occurrences with a new string
     * @param oldexpression a regular expression to search for
     * @param newString the new string which will replace the old expression
     */
    public void replaceExpression(String oldexpression, String newString) {
        if (document==null) return;
        try {
           String fullText=document.getText(0, document.getLength());
           fullText=fullText.replaceAll(oldexpression, newString);
           document.replace(0, document.getLength(),fullText,null);
        } catch (BadLocationException ble) {System.err.println("SYSTEM ERROR: "+ble.getMessage());}
    }

    
    /** Specifies whether the ProtocolEditor should display language keywords,
     *  data object names etc. in different colors
     */
    public void setUseColors(boolean flag) {
        ColoringEditorKit editorKit=(ColoringEditorKit)protocolEditor.getEditorKit();
        editorKit.setUseColors(flag);
        protocolEditor.revalidate();
        protocolEditor.repaint();
    }

    /** 
     * Sets up the colors used to highlight keywords in the protocol editor
     * based on the current settings in the Preferences package
     */
    public final void setupProtocolColors() {
        Preferences preferences = Preferences.userNodeForPackage(ProtocolEditor.class);
        color_comments=PreferencesDialog.getColorSetting(preferences,COLOR_COMMENTS,color_comments);
        color_displaysetting=PreferencesDialog.getColorSetting(preferences,COLOR_DISPLAY_SETTINGS,color_displaysetting);
        color_textstring=PreferencesDialog.getColorSetting(preferences,COLOR_TEXT,color_textstring);
        color_number=PreferencesDialog.getColorSetting(preferences,COLOR_NUMBER,color_number);
        color_data=PreferencesDialog.getColorSetting(preferences,COLOR_DATA,color_data);
        color_dataformats=PreferencesDialog.getColorSetting(preferences,COLOR_DATAFORMAT,color_dataformats);
        color_datatypes=PreferencesDialog.getColorSetting(preferences,COLOR_DATATYPE,color_datatypes);
        color_operations=PreferencesDialog.getColorSetting(preferences,COLOR_OPERATION,color_operations);
        color_analyses=PreferencesDialog.getColorSetting(preferences,COLOR_ANALYSIS,color_analyses);
        color_programs=PreferencesDialog.getColorSetting(preferences,COLOR_PROGRAMS,color_programs);
        color_flow_control=PreferencesDialog.getColorSetting(preferences,COLOR_FLOW_CONTROL,color_flow_control);        
        // now assign the colors to all the tokens that should be colored
        if (patternColors==null) patternColors=new HashMap<String,Color>();
        else patternColors.clear();
        for (String typename:Operation_new.getAvailableTypes()) {
            patternColors.put(typename,color_datatypes); // data types
        }   
        for (DataFormat dataformat:gui.getEngine().getAllDataFormats()) {
            patternColors.put(dataformat.getName(), color_dataformats); // Data formats
        } 
        for (ExternalProgram program:gui.getEngine().getAllExternalPrograms()) {
            patternColors.put(program.getName(), color_programs); // External programs
        } 
        for (String analysis:gui.getEngine().getAnalysisNames()) {
            patternColors.put(analysis, color_analyses); //
        } 
        for (Operation operation:gui.getEngine().getAllOperations()) {
            patternColors.put(operation.getName(),color_operations); // operations
        } 
    }
    
    
    private void setupDataPatternColors() { // this is called each time the protocol is parsed to add new data items
        DataTypeTable lookup=protocol.getDataTypeLookupTable();
        for (String dataname:lookup.getAllDataItemsOfType(Data.class)) {
            variablenamepatterns.add(dataname); // all data names
        }        
    }

    /** Returns a list of names of settings and their default colors
     *  This is used by e.g. PreferencesDialog to dynamically setup the panel
     *  to change colors
     */
    public Object[][] getColorSettingsClasses() {
         return new Object[][]{
             new Object[]{COLOR_OPERATION,color_operations},
             new Object[]{COLOR_DATA,color_data},
             new Object[]{COLOR_NUMBER,color_number},
             new Object[]{COLOR_TEXT,color_textstring},
             new Object[]{COLOR_DATATYPE,color_datatypes},
             new Object[]{COLOR_DATAFORMAT,color_dataformats},
             new Object[]{COLOR_ANALYSIS,color_analyses},
             new Object[]{COLOR_PROGRAMS,color_programs},
             new Object[]{COLOR_DISPLAY_SETTINGS,color_displaysetting},
             new Object[]{COLOR_COMMENTS,color_comments},
             new Object[]{COLOR_FLOW_CONTROL,color_flow_control}                 
        };
    }

   /**
    * Loads a new protocol from a specified source (File or URL) and adds it to the ProtocolManager
    * This method does not ask for file
    * @param input This should either be a File or an URL
    */
   public void openProtocolFile(final Object input) {
        if (!(input instanceof File || input instanceof URL)) {
            gui.logMessage("SYSTEM ERROR: openProtocolFile() expected File or URL argument but got:"+((input==null)?"NULL":input.getClass().toString()));
            return;
        }
        final ProtocolEditor parent=this;
        String urlfilename=null; // just the filename-part (not including path);
        if (input instanceof URL) {
            String filename=((URL)input).getPath();
            int endpos=filename.indexOf('?');
            if (endpos<0) endpos=filename.length();
            int startpos=filename.lastIndexOf('/',endpos-1);
            if (startpos<0) startpos=0; else startpos++;
            urlfilename=filename.substring(startpos,endpos);
            //endpos=filename.indexOf('.');
            //if (endpos>0) urlfilename=urlfilename.substring(0,endpos); // remove suffix also
        }
        final String filename=(input instanceof File)?((File)input).getName():urlfilename;
        final String filepath=(input instanceof File)?((File)input).getName():((URL)input).toString();
        gui.statusMessage("Opening protocol script \""+filepath+"\"");
        SwingWorker worker=new SwingWorker<StandardProtocol, Void>() {
            Exception ex=null;
            @Override 
            public StandardProtocol doInBackground() {
                BufferedReader inputStream=null;
                StringBuilder text=new StringBuilder();
                try {
                    InputStream stream=MotifLabEngine.getInputStreamForDataSource(input);
                    inputStream=new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while((line=inputStream.readLine())!=null) {text.append(line);text.append("\n");}
                } catch (IOException e) { 
                    ex=e;
                    return null;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
                }
                StandardProtocol newProtocol=new StandardProtocol(gui.getEngine(),text.toString());
                return newProtocol;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(parent, ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     return;
                }
                try {
                    StandardProtocol prot=get();
                    if (input instanceof File) prot.setFileName(((File)input).getAbsolutePath());
                    else prot.setFileName(((URL)input).toString());
                    prot.setName(filename);
                    if (protocolManager.getProtocol(filename)!=null) revertProtocol(filename,prot); // revert existing protocol to saved version
                    else newProtocol(prot); // installs the new protocol script and adds it to the ProtocolManager
                    protocolManager.registerRecent(prot);
                    if (input instanceof URL) setDirtyFlag(true); // mark protocols loaded from URL as dirty since these are not saved locally
                    gui.getMainWindow().setSelectedTab("Protocol");  
                    gui.logMessage("Opened protocol script \""+filepath+"\"");
                } catch (Exception e) {
                    String message=null;
                    if (e instanceof IOException) message=e.getMessage();
                    else message=e.getClass().getSimpleName()+":"+e.getMessage();
                    gui.logMessage("Unable to open protocol script \""+filepath+"\"  ("+message+")");
                    //e.printStackTrace(System.err);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    } 
   
   
   /** Displays a file dialog and lets the user choose a protocol to open */
    public void openProtocolFile() {
        final JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
        fc.setDialogTitle("Open Protocol");
        FileNameExtensionFilter txtFilter=new FileNameExtensionFilter("Text Files (*.txt)", "TXT","txt");
        fc.addChoosableFileFilter(txtFilter);
        fc.setFileFilter(txtFilter);        
        int returnValue=fc.showOpenDialog(this);
        if (returnValue!=JFileChooser.APPROVE_OPTION) return; // user cancelled
        File file=fc.getSelectedFile();
        if (protocolManager.isProtocolOpen(file.getName())) {
            int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "A protocol named \""+file.getName()+"\" is already open.\nWould you like to revert to the saved version?","Open Protocol",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return;            
        }   
        gui.setLastUsedDirectory(file.getParentFile());
        openProtocolFile(file);
    }

   /** Displays a dialog and lets the user enter the URL of a protocol to open */
    public void openProtocolFileFromURL() {
        String input=JOptionPane.showInputDialog(gui.getFrame(), "Enter URL address","Open Protocol From URL",JOptionPane.INFORMATION_MESSAGE);
        if (input==null || input.isEmpty()) return;
        if (!(input.startsWith("http://") || input.startsWith("https://") || input.startsWith("ftp://") || input.startsWith("ftps://"))) input="http://"+input;
        URL url=null;
        try {
            url=new URL(input);
        } catch (MalformedURLException e) {
             JOptionPane.showMessageDialog(gui.getFrame(), "Not a proper URL:\n"+input, "URL Error", JOptionPane.ERROR_MESSAGE);
             return;
        }
        String filename=url.getPath();
        int endpos=filename.indexOf('?');
        if (endpos<0) endpos=filename.length();
        int startpos=filename.lastIndexOf('/',endpos-1);
        if (startpos<0) startpos=0; else startpos++;
        filename=filename.substring(startpos,endpos);
        if (protocolManager.isProtocolOpen(filename)) {
            int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "A protocol named \""+filename+"\" is already open.\nWould you like to replace this protocol?","Open Protocol",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return;
        }
        openProtocolFile(url);
    }
    
   /** 
    * Tries to open a protocol from a given file. However, if a protocol with the same name
    * is already open it will ask the user if he/she wants to revert to the saved file
    * (Note: "same name" means the last part of the filename is the same (full path))
    * @param input This should be a File or URL object
    */    
    public void openProtocolFileButAskToRevert(Object input) {
        String filename=null; // just the filename-part (not including path);
        if (input instanceof URL) {
            String path=((URL)input).getPath();
            int endpos=path.indexOf('?');
            if (endpos<0) endpos=path.length();
            int startpos=path.lastIndexOf('/',endpos-1);
            if (startpos<0) startpos=0; else startpos++;
            filename=path.substring(startpos,endpos);
        } else if (input instanceof File) {
            filename=((File)input).getName();
        } else {
            gui.logMessage("SYSTEM ERROR: openProtocolFileButAskToRevert() expected File or URL argument but got:"+((input==null)?"NULL":input.getClass().toString()));
            return;
        }
        //
        if (protocolManager.isProtocolOpen(filename)) {
            int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "A protocol named \""+filename+"\" is already open.\nWould you like to revert to the saved version?","Open Protocol",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return;            
        }       
        openProtocolFile(input);
    }
    
    
   /** 
    * Saves the current Protocol script to file.
    * If the supplied boolean argument is true, a dialog will be displayed to allow the user to
    * select a filename for the protocol. If the argument is false, the filename currently associated
    * with the script will be used (if the protocol has not been saved locally before a dialog will
    * be displayed even if the argument is false)
    * @return FALSE if the user cancelled the save interactively or TRUE if the data was saved (or attempted)
    */
    public boolean saveProtocolFile(boolean askForNewName) {
        boolean dirty=protocolManager.saveProtocolFile((StandardProtocol)protocol,askForNewName);
        setDirtyFlag(dirty);
        return dirty;
    }   
            
    /**
     * Closes the current protocol file (and asks to user if he/she wants to save the protocol first)
     */
    public void closeProtocolFile() {
        if (((StandardProtocol)protocol).isDirty() && gui.doPromptBeforeDiscard()) {
            int response=JOptionPane.showConfirmDialog(this, "Save changes to "+protocol.getName()+"?","Close protocol",JOptionPane.YES_NO_CANCEL_OPTION);
            if (response==JOptionPane.CANCEL_OPTION) return;
            else if (response==JOptionPane.OK_OPTION) saveProtocolFile(false);
        }
        protocolManager.closeProtocol(protocol.getName());       
    }
    
    
    /** This method should be called when the system exits. It will perform cleanup() */
    public void shutdown() {
       protocolManager.shutdown();
    }
   

    // ----- Find and Replace -----

    private boolean wrapsearch=false;
    private boolean silentsearch=false;
    private boolean searchIsCaseSensitive=false;

    @Override
    public boolean find(String searchstring) {
       if (searchstring==null || searchstring.isEmpty()) {
            previousSearchstring="";
            protocolEditor.requestFocusInWindow();
            protocolEditor.select(0,0);
            return false;
       }
       if (document==null || document.getLength()==0) return false;
       // has the search string changed?
       if ((searchIsCaseSensitive && !searchstring.equals(previousSearchstring)) || (!searchIsCaseSensitive && !searchstring.equalsIgnoreCase(previousSearchstring))) {
           wrapsearch=true;
           previousSearchstring=searchstring;
           protocolEditor.requestFocusInWindow();
           protocolEditor.select(0,0);
       }
       char[] string=(searchIsCaseSensitive)?searchstring.toCharArray():searchstring.toLowerCase().toCharArray(); // case-insensitive search
       
       int matchAt=-1; // position of matching character in text
       Segment text = new Segment();
       int offs = protocolEditor.getCaretPosition(); // start of search in document. Default to current caret position.
       int selStart=protocolEditor.getSelectionStart();
       int selEnd=protocolEditor.getSelectionEnd();
       int selectionsize=selEnd-selStart;
       if (selectionsize>0) offs=selStart+1; // Previous match is already highlighted. Start next search from position after beginning of selection  
       if (offs>=document.getLength()) offs=0; // caret at end of document. Wrap right away
       if (wrapsearch) {offs=0;wrapsearch=false;}
       text.setPartialReturn(true);
       boolean matchfound=false;
       int nleft = document.getLength()-offs;
       loop:
       while (nleft > 0) {
           try {document.getText(offs, nleft, text);} catch (Exception e) {}
           for (int i=0;i<=text.count-string.length;i++) {
               for (int j=0;j<string.length;j++) {
                    char docChar=text.charAt(i+j);
                    if (!searchIsCaseSensitive)docChar=Character.toLowerCase(docChar);
                    if (docChar!=string[j]) break; // break j-loop
                    if (j==string.length-1) {
                        matchfound=true;
                        matchAt=offs+i;
                        break loop;
                    }
               }
           }
           nleft -= text.count;
           offs += text.count;
       }
       if (matchfound) {
           protocolEditor.requestFocusInWindow();
           protocolEditor.select(matchAt, matchAt+string.length);
           if (!silentsearch) gui.statusMessage(""); // clear message in case of wrapped-search (so that the "No matches found" message is not still showing)
           return true;
       } else {           
           if (!silentsearch) gui.statusMessage("[Protocol] Searching for '"+searchstring+"'.   No more matches found      (Press CTRL+F to wrap search)");
           wrapsearch=true;
           return false;
       }
    }

    @Override
    public boolean isSearchCaseSensitive() {
        return searchIsCaseSensitive;
    }

    @Override
    public void setSearchIsCaseSensitive(boolean flag) {
        searchIsCaseSensitive=flag;
    }

    @Override
    public void searchAndReplace() {
        if (getProtocol()==null) return;
        FindReplaceDialog dialog=new FindReplaceDialog(this, gui);
        dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
        dialog.setVisible(true);
        dialog.dispose();
    }

    @Override
    public boolean replaceCurrent(String searchstring, String replacestring) {
        String currentSelection=protocolEditor.getSelectedText();
        silentsearch=true;
        boolean wasreplaced=false;
        if (currentSelection==null 
             || ( searchIsCaseSensitive && !currentSelection.equals(searchstring))
             || (!searchIsCaseSensitive && !currentSelection.equalsIgnoreCase(searchstring))
        ) {
            find(searchstring);
        } else {
            protocolEditor.replaceSelection(replacestring);
            wasreplaced=true;
        }
        silentsearch=false;
        return wasreplaced;
    }

    @Override
    public int replaceAll(String searchstring, String replacestring) {
        previousSearchstring=""; // this will reset search
        boolean found=true;
        silentsearch=true;
        int count=0;
        while (found) {
            found=find(searchstring);
            if (found) {
                replaceCurrent(searchstring, replacestring);
                count++;
            }
        }
        silentsearch=false;
        gui.statusMessage("Searching for '"+searchstring+"'.  Replaced "+count+" occurrence"+((count==1)?".":"s."));
        return count;
    }

    @Override
    public boolean supportsReplace() {
        return true;
    }

    @Override
    public String getSelectedTextForSearch() {
       String selection=protocolEditor.getSelectedText();
       if (selection!=null && selection.trim().isEmpty()) return null;
       else return selection;
    }      


// ********    DataListener Interface implementation    **********
    
    @Override
    public void dataAdded(Data data) {
        if (!gui.isExecutingProtocol()) clearMarginAndRestartTimer();
    }
    @Override
    public void dataRemoved(Data data) {
        if (!gui.isExecutingProtocol()) {
            if (!data.isTemporary()) clearMarginAndRestartTimer();
        }
    }
    @Override
    public void dataUpdated(Data data) {
        if (!gui.isExecutingProtocol()) clearMarginAndRestartTimer();
    }

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}
    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {}
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}

    private void clearMarginAndRestartTimer() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaintTimer.stop();
                leftmargin.removeAll();
                leftmargin.repaint();
                repaintTimer.start();
            }
        });
    }
 
    
    /** Prints the contents of the Protocol Editor to a selected printer */
    public void print() throws PrinterException {
        protocolEditor.print();
    }    

    /** Sets the 'dirty flag' of the current protocol.
     *  A protocol is considered dirty if it has been changed since it was last saved
     */
    public void setDirtyFlag(boolean dirty) {
       if (protocol==null) return;
       ((StandardProtocol)protocol).setDirtyFlag(dirty);
       gui.setSaveEnabled(dirty);
       if (dirty) setHeader(protocol.getName()+" *");
       else setHeader(protocol.getName());
       headerlabel.repaint(); // update asterisk mark behind protocol name
    }

    /** Specifies whether execution progress of the current protocol should be 'visualized'
     *  by drawing little icons in the margin before each protocol line as it is executed
     */
    public void setNotifyExecutionProgress(boolean flag) {
        notifyExecutionProgress=flag;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setName("Form"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables


// *****************************   PRIVATE CLASSES   ***********************************************
   
/** Listens to selection made in the protocol list in the header */
private class ChangeProtocolMenuListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        if (gui.isRecording()) {// notify user that recording is on
            JOptionPane.showMessageDialog(gui.getFrame(), "You are currently in \"record mode\".\nSubsequent operations will be recorded in the new protocol", "Notification", JOptionPane.INFORMATION_MESSAGE);
        }
        //protocolEditor.changeProtocol(e.getActionCommand(),null,true); // queue undoable event
        changeProtocol(e.getActionCommand());
    }
}

/** 
 * Listens to updates in the protocol document and calls starts (or resets) the timer which 
 * will eventually (after 1 second) trigger a reparsing of the document
 */    
private class ProtocolDocumentListener implements DocumentListener {
            @Override
            public void insertUpdate(DocumentEvent e) { // resets current timer and starts over              
                setDirtyFlag(true);
                setNotifyExecutionProgress(false);
                ((ColoringEditorKit)protocolEditor.getEditorKit()).clearCachedColoringInformation();
                errorBulletLabel.setIcon(yellowbullet);
                repaintTimer.stop();
                leftmargin.removeAll(); 
                leftmargin.repaint();
                repaintTimer.start();
            }
            @Override
            public void removeUpdate(DocumentEvent e) { // resets current timer and starts over     
                setDirtyFlag(true);
                setNotifyExecutionProgress(false);
                ((ColoringEditorKit)protocolEditor.getEditorKit()).clearCachedColoringInformation();
                errorBulletLabel.setIcon(yellowbullet);
                repaintTimer.stop();
                leftmargin.removeAll(); 
                leftmargin.repaint();
                repaintTimer.start();
            }
            @Override
            public void changedUpdate(DocumentEvent e) { // resets current timer and starts over     
                setDirtyFlag(true);
                setNotifyExecutionProgress(false);
                ((ColoringEditorKit)protocolEditor.getEditorKit()).clearCachedColoringInformation();
                errorBulletLabel.setIcon(yellowbullet);
                repaintTimer.stop();
                leftmargin.removeAll(); 
                leftmargin.repaint();
                repaintTimer.start();
            }       
}    
   
/** Changes the installed protocol from the old to the new
 *  and makes sure all listeners are removed from the old protocol
 */
public void changeInstalledProtocol(String oldProtocolName, String newProtocolName) {   
        Protocol oldProtocol=protocolManager.getProtocol(oldProtocolName);
        Protocol newProtocol=protocolManager.getProtocol(newProtocolName);
        if (oldProtocol!=null) removeListeners(oldProtocol); // removes listeners from "current" protocol document
        installProtocol(newProtocol); 
    
}
 private void removeListeners(Protocol oldProtocol) {
        Document doc=oldProtocol.getDocument();
        doc.removeDocumentListener(protocolDocumentListener);
        doc.removeDocumentListener(undoRedoDocumentListener);
        doc.removeUndoableEditListener(undoManager);
 }
 
/**
 * Changes the currently installed (i.e. being edited) Protocol script with a new one 
 */
private class installProtocolEdit extends AbstractUndoableEdit {
    String oldProtocolName=null;
    String newProtocolName=null;
    String editName="";
    
    public installProtocolEdit(String editName, String oldprotocolName, String newprotocolName) {
        this.editName=editName;
        oldProtocolName=oldprotocolName;
        newProtocolName=newprotocolName;  
        changeInstalledProtocol(oldProtocolName,newProtocolName);
    }
    
    @Override
    public String getPresentationName() {
        return editName;
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        changeInstalledProtocol(oldProtocolName,newProtocolName);   
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        changeInstalledProtocol(newProtocolName,oldProtocolName);  
    }

       
    
}    
    
          
    
 private class ProtocolEditorContextMenu extends JPopupMenu {
        public ProtocolEditorContextMenu() {
            MotifLabEngine engine=gui.getEngine();
            editLineMenuItem=new JMenuItem("Edit");
            editLineMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (editTask!=null) launchOperationEditor(editTask,true); // 
                }
            });

            OperationsMenuListener operationActionListener = new OperationsMenuListener();
            JMenu operationsSubMenu=gui.getOperationContextMenu("Insert operation",null,operationActionListener);
            
            this.add(editLineMenuItem);
            this.add(operationsSubMenu);
            this.add(new JSeparator());  
            this.add(gui.getAction("executeCurrentProtocolSelection"));  
            this.add(gui.getAction("executeProtocolFromCurrentLine"));            
            this.add(new JSeparator());            
            this.add(gui.getAction("commentSelectedLinesInProtocol"));         
            this.add(gui.getAction("uncommentSelectedLinesInProtocol"));             
            this.add(new JSeparator());
            JMenuItem menuitem;
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("cut"));
            menuitem.setIcon(null);
            this.add(menuitem);
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("copy"));
            menuitem.setIcon(null);
            this.add(menuitem);
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("paste"));
            menuitem.setIcon(null);
            this.add(menuitem);            
                 
        }   
        
         /**
         * An inner class that listens to popup-menu events related to the operations submenu and notifies the gui events
         */
        private class OperationsMenuListener implements ActionListener {
           public void actionPerformed(ActionEvent e) {
                MotifLabEngine engine=gui.getEngine();
                Operation operation=engine.getOperation(e.getActionCommand());
                if (operation!=null) {
                    OperationTask parameters=new OperationTask(operation.getName());
                    parameters.setParameter(OperationTask.OPERATION, operation);
                    launchOperationEditor(parameters,false);
                } else if (engine.getAnalysis(e.getActionCommand())!=null) { // operation is an analysis
                    operation=engine.getOperation("analyze"); 
                    OperationTask parameters=new OperationTask(operation.getName());
                    String analysisName=e.getActionCommand();                   
                    parameters.setParameter(OperationTask.OPERATION, operation);
                    parameters.setParameter(Operation_analyze.ANALYSIS, analysisName);
                    launchOperationEditor(parameters,false);                  
                }
           }            
        }
    
}
    
private class LineNumberPanel extends JPanel {
    public LineNumberPanel() {
        super();
        setLayout(null);
        setOpaque(false);
        setBackground(new java.awt.Color(255,245,100));
    }
    @Override
    public void paintComponent(Graphics g) {
        g.setFont(font);
        g.setColor(java.awt.Color.BLACK);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, (antialias)?MotifLabGUI.ANTIALIAS_MODE:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int height=this.getHeight();
        int width=this.getWidth();
        int numlines=getNumberOfLines();
        if (numlines==0) return;
        for (int i=0;i<numlines;i++) {
           int ypos=(i+1)*lineHeight-leading;
           if (ypos>height) break;
           String lineString=""+(i+1);
           int lnwidth=font.getStringBounds(lineString, ((Graphics2D)g).getFontRenderContext()).getBounds().width;
           int xpos=width-(lnwidth+5); // 5px margin
           g.drawString(""+lineString,xpos,ypos);
        }        
    }
}

private class CaretPositionPanel extends JLabel implements CaretListener {
        
        public CaretPositionPanel() {
            this.setHorizontalTextPosition(JLabel.RIGHT);
            this.setHorizontalAlignment(JLabel.RIGHT);
            this.setText("1:1 ");
        }    
        @Override
        public void caretUpdate(CaretEvent e) {
            if (protocol==null){
                this.setText(" ");
            } else {
                int pos=getCaretPosition();
                Element root=document.getDefaultRootElement();
                int line=root.getElementIndex(pos);
                Element lineElement=root.getElement(line);
                int linestartsat=lineElement.getStartOffset();
                this.setText((line+1)+":"+(pos-linestartsat+1)+" ");   
            }
        }
    
}


private class ProtocolEditorPane extends JEditorPane {
	private boolean isOvertypeMode=false;
	private Caret defaultCaret;
	private Caret overtypeCaret;
        private Action changeOvertypeModeAction;
        private final String OVERTYPEMODE="OvertypeMode"; 
        private ExecuteCommandAction executeAction;
        
    public ProtocolEditorPane() {
        super();
        executeAction=new ExecuteCommandAction();
        Keymap parentKeyMap=getKeymap();
        Keymap newmap=addKeymap("ProtocolEditorKeymap", parentKeyMap);
        KeyStroke enter=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK);
        newmap.addActionForKeyStroke(enter, executeAction);
        KeyStroke enterShift=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK);
        newmap.addActionForKeyStroke(enterShift, executeAction);
        this.setKeymap(newmap);
        defaultCaret = getCaret();
	overtypeCaret = new OvertypeCaret();
	overtypeCaret.setBlinkRate(defaultCaret.getBlinkRate());	
        changeOvertypeModeAction=new AbstractAction("OvertypeMode") { 
            @Override
            public void actionPerformed(ActionEvent e) {
               setOvertypeMode(!isOvertypeMode());
            }
        };
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, true), OVERTYPEMODE);
        getActionMap().put(OVERTYPEMODE, changeOvertypeModeAction);                
    }
    
    @Override
    public void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, (antialias)?MotifLabGUI.ANTIALIAS_MODE:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        super.paintComponent(g);
    }
    
    /** Returns the overtypemode*/
    public boolean isOvertypeMode(){
	return isOvertypeMode;
    }
    /** Sets the overtype mode */
    public void setOvertypeMode(boolean isOvertypeMode) {
	this.isOvertypeMode = isOvertypeMode;
	int pos = getCaretPosition(); 
	if (isOvertypeMode()){
	   setCaret(overtypeCaret); 
           insertOrOverwriteLabel.setText("OVR");
	} else  {
	   setCaret(defaultCaret);
           insertOrOverwriteLabel.setText("INS");
	}
        setCaretPosition( pos );
    }
 
    @Override
    public void replaceSelection(String text){
	if (isOvertypeMode()){
            int pos = getCaretPosition();
            if (getSelectedText()==null &&  pos<getDocument().getLength()) moveCaretPosition(pos+1);		
	}
        super.replaceSelection(text);
    }

    /** Executes the protocol line currently occupied by the caret */
    public void executeCurrentLine() {
        executeAction.actionPerformed(null);
    }    

    /** Returns the line number (starting at 1) of the line that currently contains the caret (cursor) */
    public int getCurrentLine() {
        int pos=getCaretPosition();
        Element root=document.getDefaultRootElement();
        int line=root.getElementIndex(pos)+1;
        return line;
    }
    
    private class ExecuteCommandAction extends AbstractAction {
         @Override
         public void actionPerformed(ActionEvent e) {
              // NB!!!!! The ActionEvent argument can sometimes be 'null' so it is necessary to check for this!!!
              int line=getCurrentLine();
              ExecutableTask task=null;
              DataTypeTable lookup=protocol.getDataTypeLookupTable();
              lookup.clear();
              lookup.populateFromEngine(); //
              try {
                  task=protocol.parseCommand(line,false);
              } catch (ParseError pe) {gui.logMessage("Parse Error: "+pe.getMessage());}
              if (task!=null) {

                  if (task instanceof OperationTask) {
                      if (e!=null && (e.getModifiers() & ActionEvent.SHIFT_MASK)==ActionEvent.SHIFT_MASK) ((OperationTask)task).setParameter("_SHOW_RESULTS", Boolean.FALSE);
                      gui.launchOperationTask((OperationTask)task,false);
                  } // false => do not record in protocol
                  else gui.launchTask(task);
              };
          }    
    } // end inner class: ProtocolEditorPane.ExecuteCommandAction   
    
    
    
    private class OvertypeCaret extends DefaultCaret {	
       @Override
	public void paint(Graphics g) {
	    if (isVisible()) {
	      try {
		   JTextComponent component = getComponent();
		   TextUI mapper = component.getUI();
		   Rectangle r = mapper.modelToView(component, getDot());
		   //g.setColor(component.getCaretColor());
		   g.setColor(Color.BLACK);
                   int charwidth = g.getFontMetrics().charWidth( 'w' );
		   if (x!=r.x||y!=r.y) {
                      repaint(); 
                      x=r.x;y=r.y;height=r.height;
                   }
                   g.setColor(Color.BLACK);
                   g.setXORMode(component.getBackground());
		   g.fillRect(r.x, r.y, charwidth, r.height);
              }
              catch (Exception e) {}
           }
        }
       
	@Override
	protected synchronized void damage(Rectangle r) {
            if (r == null) return;
            JTextComponent component = getComponent();
            x = r.x;
            y = r.y;
            width = component.getFontMetrics( component.getFont() ).charWidth( 'w' );
            height = r.height;
            repaint();           
	}        
    } // end inner class: ProtocolEditorPane.OvertypeCaret    
    
} // end of class ProtocolEditorPane


    


/** This editor kit can color keywords and special tokens in the text */
private class ColoringEditorKit extends DefaultEditorKit implements ViewFactory {
        private boolean shouldUseColors=true;
        private Object[] datatypenames=null; // each entry is a String[] containing a datatypename split into words e.g. [Motif], [Sequence,Collection] or [DNA,sequence,dataset]
        private HashMap<Integer,SortedMap> colorcache=new HashMap<Integer,SortedMap>(); // caches information about keyword coloring, must be updated each time the document is changed
        
        public void setUseColors(boolean flag) {
            shouldUseColors=flag;
        }
        public void clearCachedColoringInformation() {
            colorcache.clear();
        }
        
        @Override
        public ViewFactory getViewFactory() {return this;}
        @Override
        public View create (Element elem) {
             return new CustomView(elem);
        }
        public ColoringEditorKit() {
            super();
            String[] availabletypes=Operation_new.getAvailableTypes();
            String[] analysesnames=gui.getEngine().getAnalysisNames(); // I add the analysis names also because these can have multiple words in their names too
            datatypenames=new Object[availabletypes.length+analysesnames.length];
            int i=0;
            for (String typename:availabletypes) {
                String[] words=typename.split(" ");
                if (words.length<2) continue;
                datatypenames[i]=words; // keep only the ones with multiple words
                i++;
            }       
            for (String typename:analysesnames) {
                String[] words=typename.split(" ");
                if (words.length<2) continue;
                datatypenames[i]=words; // keep only the ones with multiple words
                i++;
            }
            datatypenames=Arrays.copyOf(datatypenames,i);
        }
 
    //private Pattern splitpattern=Pattern.compile("\\W"); // split on non-word characters (not letter, number or underscore)
    private Pattern splitpattern=Pattern.compile("[^\\w&&[^\\.\\+\\-]]"); // split on all non-word characters, except .-+ which could be in numbers
    /** This is a slightly modified version of String.split() which also includes the matched expressions (i.e. both the parts and the separators between them are included) */        
    private String[] split(String input) {
        int index = 0;
        ArrayList<String> matchList = new ArrayList<String>();
        Matcher m = splitpattern.matcher(input);
        // Add segments before each match found
        while(m.find()) {
            String match = input.substring(index, m.start());
            if (match.length()>0) matchList.add(match);
            index = m.end();
            String mnw=input.substring(m.start(),index);
            if (mnw.length()>0) matchList.add(mnw); // this will include the matched non-word character in the matchList                              
        }        
        if (index == 0) return new String[] {input}; // If no match was found, return this        
        matchList.add(input.substring(index, input.length())); // Add remaining segment
        // Construct result
        int resultSize = matchList.size();
        while (resultSize > 0 && matchList.get(resultSize-1).equals("")) resultSize--; // remove blanks at the end      
        String[] result = new String[resultSize];
        return matchList.subList(0, resultSize).toArray(result);
    }
    
    
    /** This method splits up a textline into 'tokens' that can possibly consist of several words.
     *  All words in a token belong together as a unit and should be given a single color
     *  The method works by first splitting the text into a String[] containing atomic parts which could be words or single character separators between words
     *  Next, it collects atoms belonging together into the same array elements by adding to the text of the previous elements and setting no longer used elements to NULL.
     *  E.g. if the String[] contains three elements after the first split [word1,word2,word3] and word2 should be grouped together with word1,
     *  the first array element will be set to "word1+word2" and the second element is set to NULL to signal that it is no longer used.
     */
    private String[] preprocessLineForColoring(String text) {
        String[] words=split(text);    
        int startQuote=-1; 
        // merge tokens within same quotes
        for (int i=0;i<words.length;i++) {
            if (words[i].trim().equals("\"")) {
                if (startQuote<0) {// First encounter of ". This is an opening quote!
                    startQuote=i;
                } else {// A quote following an opening quote will usually be a closing quote, but it could also be escaped so we must check what comes before it!
                    if (isQuoteEscaped(words[startQuote])) { // this does not work with escaped \'s 
                       words[startQuote]+=words[i]; // The quote was escaped so the quote is not finished yet. move this word into the 'quote' array element
                       words[i]=null;                       
                    } else { // this is a proper closing quote
                        words[startQuote]+=words[i];
                        words[i]=null;
                        startQuote=-1;
                    }
                }
            } else if (startQuote>0) { // within a running quote
                words[startQuote]+=words[i]; // move this word into the 'quote' array element
                words[i]=null;
            }
        }
        // merge tokens for numbers (e.g. -94.23)
        for (int i=0;i<words.length;i++) {
            //if (words[i]==null || !words[i].matches("\\s*\\d+\\s*")) continue;
            if (words[i]==null || !words[i].matches("\\s*[+-]?\\d*\\.?\\d+(E[+-]\\d+)?\\s*")) continue; // is this word a number?
            if (!variablenamepatterns.contains(words[i])) words[i]="\f"+words[i]; // the formfeed marks this a numerical token so that we don't have to parse it again
       }
        // contract data type names that span several words
        for (int i=0;i<words.length;i++) {
            if (words[i]==null || words[i].startsWith("\f")) continue;
            int matchingwordcount=getMatchingDataType(words,i); // returns 0 of no match is found
            if (matchingwordcount>0) {
                for (int j=1;j<matchingwordcount*2-1;j++) {
                    words[i]+=words[i+j];
                    words[i+j]=null;
                }
            }            
        }    
        return words; 
    }
    
    /** Returns TRUE if the given string ends in a backslash which would serve as an escape for any character coming after it
     *  If the string ends with an even number of backslashes these will serve to escape each other and the method would then return FALSE.
     *  E.g. if the string is "word\" the method will return TRUE, but if the string is "word\\" the ending backslash is not an escape but has itself been escaped.
     */
    private boolean isQuoteEscaped(String string) {
        // Count the number of backslashes from the end of the string. Return true if this is an odd number
        int count=0;
        int length=string.length();
        for (int i=1;i<=length;i++) {
            char c=string.charAt(length-i);
            if (c=='\\') count++; else break;
        }
        return (count%2==1);        
    }
    
    private int getMatchingDataType(String[] words, int pos) {
        for (int i=0;i<datatypenames.length;i++) {
            String[] datatypename=(String[])datatypenames[i];
            if (!words[pos].equals(datatypename[0])) continue; // first word does not match at this position
            if (pos+(datatypename.length-1)*2>=words.length) continue; // the datatype name is too long to fit here anyway
            boolean match=true;
            for (int j=1;j<datatypename.length;j++) {
                if (words[pos+j*2]==null || !words[pos+j*2].equals(datatypename[j])) {match=false;break;}
            }
            if (match) return datatypename.length;
        }
        return 0;
    }
    
    /** processes a line of text and determines which 'tokens' (words or group of words) it consists of.
     *  The returned sorted map has an integer key which is the start of the token and the value
     *  is a pair of objects (Object[]) consisting of 
     *    [0] an Integer holding the end position of the token
     *    [1] a color object which should be used to color that token (this color can be null if the token should not be colored in a special way).
     */
    private SortedMap<Integer, Object[]> processLineForColoring(String text) {
        SortedMap<Integer, Object[]> colorMap = new TreeMap<Integer, Object[]>(); // holds start position (key) and end position and color info for the same matching regex      
        if (text.matches("\\s*#.*\n?")) {colorMap.put(0,new Object[]{new Integer(text.length()),color_comments});}
        else if (text.matches("\\s*@.*\n?") || text.matches("\\s*\\$.*\n?") || text.matches("\\s*!.*\n?")) {colorMap.put(0,new Object[]{new Integer(text.length()),color_displaysetting});}
        else if (text.matches("\\s*if .*\n?") || text.matches("\\s*else\n?") || text.matches("\\s*else .*\n?") || text.matches("\\s*end if\n?")) {colorMap.put(0,new Object[]{new Integer(text.length()),color_flow_control});}
        else {
            String[] words=preprocessLineForColoring(text); // Each word is a token and can have multiple 'sub words'. NB: Don't trim this text!
            int wordstart=0;
            int bracelevel=0; // number of left-braces encountered not closed by a right-brace
            for (String word:words) {
                if (word==null) continue; // this has been merged in preprocessing
                Color color=null;
                if (word.startsWith("\f")) {
                    color=color_number; // the \\f character is a flag to mark numerical tokens introduced in the preprocessing step above
                    word=word.substring(1); // remove the formfeed mark
                }
                //gui.logMessage("["+word+"] ("+word.length()+")"); // for debugging
                int wordend=wordstart+word.length();  
                word=word.trim();
                if (word.equals("{")) bracelevel++;
                else if(word.equals("}") && bracelevel>0) bracelevel--;
                if (color==null) {
                          if (patternColors.containsKey(word) && bracelevel==0) color=patternColors.get(word); // do not color reserved words within braces
                     else if (variablenamepatterns.contains(word)) color=color_data;
                     else if (word.startsWith("\"") && word.endsWith("\"")) color=color_textstring;
                }
                if (color!=null) {
                    colorMap.put(wordstart, new Object[]{new Integer(wordend),color});      
                }
                wordstart=wordend;               
            }
        }
        return colorMap;
    }   
    
        
  private class CustomView extends PlainView {// Note this is an inner class of ColoringEditorKit
    
   public CustomView(Element elem) {
       super(elem);
   }

    @Override
   protected void drawLine(int lineIndex, Graphics g, int x, int y) {
        Element line = getElement().getElement(lineIndex);
	Element elem;
	
	try {
	    if (line.isLeaf()) {
                drawElement(lineIndex, line, g, x, y);
	    } else {
	        // this line contains the composed text.
	        int count = line.getElementCount();
		for(int i = 0; i < count; i++) {
		    elem = line.getElement(i);
		    x = drawElement(lineIndex, elem, g, x, y);
		}
	    }
        } catch (BadLocationException e) {
            System.err.print("Can't render line: " + lineIndex);
        }
    }
   
    private int drawElement(int lineIndex, Element elem, Graphics g, int x, int y) throws BadLocationException {
	int p0 = elem.getStartOffset();
        int p1 = elem.getEndOffset();
        p1 = Math.min(getDocument().getLength(), p1);
	x = drawUnselectedText(g, x, y, p0, p1);        
        return x;
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    protected int drawUnselectedText(Graphics graphics, int x, int y, int p0, int p1) throws BadLocationException {

        Document doc = getDocument();
        String text = doc.getText(p0, p1 - p0);
        Segment segment = getLineBuffer();

        if (!shouldUseColors) {
            graphics.setColor(Color.black);
             doc.getText(p0, text.length(), segment);
             x = Utilities.drawTabbedText(segment, x, y, graphics, this, 0);
            return x; // Note early return           
        }
        
        // --- draw colored text below this line ---       

        // determine the tokens in this line of text and the colors to use for them
        SortedMap<Integer, Object[]> colorMap=processLineForColoring(text);
          
        // Colour the parts
        int i = 0;        
        for (Map.Entry<Integer, Object[]> entry : colorMap.entrySet()) {
            int start = entry.getKey();
            Object[] value = entry.getValue();
            int end = (Integer)value[0];
            Color color=(Color)value[1];

            if (i < start) { // paint black text at the start of the line
                graphics.setColor(Color.black);
                doc.getText(p0 + i, start - i, segment);
                x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
            }

            graphics.setColor(color);
            i = end;
            doc.getText(p0 + start, i - start, segment);
            x = Utilities.drawTabbedText(segment, x, y, graphics, this, start);       
        }

        // Paint possible remaining text black
        if (i < text.length()) {
            graphics.setColor(Color.black);
            doc.getText(p0 + i, text.length() - i, segment);
            x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
        }
        return x;
    }                

  } // end of private class CustomView                
    } // end of private class ColoringEditorKit
 



private class GradientPanel extends JPanel {
    private Color leftEnd=new Color(80,80,255);
    private Color rightEnd=new Color(150,230,255);
    private Color leftEndDisabled=new Color(50,50,50);
    private Color rightEndDisabled=new Color(180,180,180);
    public GradientPanel() {
        super();
        this.setOpaque(true);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Color leftColor=(protocol!=null)?leftEnd:leftEndDisabled;
        Color rightColor=(protocol!=null)?rightEnd:rightEndDisabled;       
        Graphics2D g2=(Graphics2D)g;
        int w=getWidth();
        int h=getHeight();
        GradientPaint gradient=new GradientPaint(0,0,leftColor,w,h,rightColor);
        g2.setPaint(gradient);
        g2.fillRect(0,0,w,h);
    }
    
} // end of private class GradientPanel



} // end of class ProtocolEditor

