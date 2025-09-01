/*
 * OutputPanel.java
 *
 * Created on 7. oktober 2008, 14:44
 */

package org.motiflab.gui;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.net.URI;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.OutputData;

/**
 * The OutputPanel class implements a GUI widget (subclass of JPanel) which can be used
 * to display the contents of OutputData objects in the GUI. The OutputPanels are 
 * shown as tabbed panes in the main panel of the GUI.
 * @author  kjetikl
 */
public class OutputPanel extends javax.swing.JPanel implements Searchable, HyperlinkListener {
    private JEditorPane outputEditor;
    private OutputData outputdata;
    private Document document;
    private MotifLabGUI gui;
    private String previousSearchstring="";
    
    /** Creates new form OutputPanel */
    public OutputPanel(OutputData outputdata,MotifLabGUI gui) {
        this.gui=gui;
        this.outputdata=outputdata;
        initComponents();
        this.setLayout(new BorderLayout());   
        this.document=outputdata.getDocument();   
        if (document==null) {
            document=new PlainDocument();
            try {document.insertString(0, "ERROR", null);} catch (BadLocationException ble) {}
        }
        Object tabsize=(gui==null)?8:gui.getVisualizationSettings().getSetting(VisualizationSettings.SYSTEM_SETTING+".tab");
        if (tabsize!=null && tabsize instanceof Integer) {
            setTabSize((Integer)tabsize);
        }
        outputEditor=new JEditorPane();
        if (outputdata.isBinary()) { // this type of OutputData can not be displayed directly. Show a proxy document instead
            outputEditor.setEditorKit(new HTMLEditorKit());
            outputEditor.setDocument(((HTMLEditorKit)outputEditor.getEditorKit()).createDefaultDocument());
            String message="<html><h2>"+outputdata.getName()+"</h2>This document is in the format \""+outputdata.getDataFormat()+"\" which can not be displayed by MotifLab. However, the contents of this document can still be correctly saved to file.</html>";
            try {outputEditor.setText(message);} catch (Exception e) {throw new NullPointerException(e.getMessage());}   
            outputEditor.setCaretPosition(0);            
        } else if (outputdata.isHTMLformatted()) {
            outputEditor.setEditorKit(new HTMLEditorKit());
            outputEditor.setDocument(((HTMLEditorKit)outputEditor.getEditorKit()).createDefaultDocument());
            outputEditor.addHyperlinkListener(this);
            outputEditor.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            try {
                String text=document.getText(0, document.getLength());
                outputEditor.setText(text);  
            } catch (Exception e) {throw new NullPointerException(e.getMessage());}   
            outputEditor.setCaretPosition(0);
        } else {          
           outputEditor.setDocument(document); 
           outputEditor.setCaretPosition(document.getLength()); // place cursor at end so text can be appended later
           outputEditor.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        }
        outputEditor.setEditable(false); // this is the best solution. In this case we don't encounter problems/inconsistencies related to undo/redo
        JScrollPane outputPanelScroll=new JScrollPane(outputEditor);   
        //outputPanelScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(outputPanelScroll,java.awt.BorderLayout.CENTER);  
        outputEditor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (OutputPanel.this.gui!=null) OutputPanel.this.gui.setSearchTargetComponent(null);
            }           
        });
    }

    
    /** 
     * Returns the Document used to display the data contained in the associated output object
     * Note that this could be the same as the document in the output data object, but it could also
     * be a different object (for HTML formatted output data)
     */
    public Document getDocument() {
        return document; 
    }
    
    /** Returns the OutputData object displayed in this OutputPanel */
    public OutputData getOutputData() {
        return outputdata; 
    }
    /** Returns the JEditorPane used to display the OutputData document*/
    public JEditorPane getEditor() {
        return outputEditor;
    }
    
    /** Prints the contents of the output object to a selected printer */
    public void print() throws PrinterException {
        outputEditor.print();
    }
    
    public final void setTabSize(int tab) {
        if (document!=null && document instanceof PlainDocument) {
            ((PlainDocument)document).putProperty(PlainDocument.tabSizeAttribute, tab);
        }        
    }
    
    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        try {
            String urlString=event.getDescription();
            if (urlString.startsWith("motiflab:")) { // internal command
                String commandString=urlString.substring("motiflab:".length());
                if (gui!=null) gui.executeInternalCommand(commandString);
            } 
            else if (urlString.startsWith("#")) { // relative anchor within same document
                outputEditor.scrollToReference(urlString.substring(1));
            } 
            else { // normal link
                URL url=event.getURL();
                String urlstring=event.getDescription();                
                // first check if the link could possibly reference an OutputData object which is already open in another tab
                if (urlstring.indexOf('.')>0) {
                    String name=urlstring.substring(0,urlstring.lastIndexOf('.'));
                    String suffix=urlstring.substring(urlstring.lastIndexOf('.')+1);
                    if (gui!=null && gui.getMainWindow().hasTab(name,suffix)) {
                        gui.executeInternalCommand("showTab="+name);
                        return;
                    }
                }
                // external link                
                Desktop desktop=java.awt.Desktop.getDesktop();
                if (desktop==null) throw new ExecutionError("Unable to access desktop");
                if (url==null) { // this might be a relative url                
                    if (outputdata!=null) {                       
                        Object source=outputdata.getSourceFile();
                        if (source instanceof String) source=gui.getEngine().getFile((String)source); // this will probably fail for DataRepositories since these are not known to external browsers
                        if (source instanceof File) {                          
                             File imageFile=new File(((File)source).getParent(), urlstring);
                             url=new URL("file:///"+imageFile.getAbsolutePath());
                        } else if (source instanceof URL) {
                             url=new URL((URL)source,urlstring); // I have not tested this so I am not sure if it works as expected
                        }  
                    }                 
                }
                if (url==null) throw new ExecutionError("No URL");
                URI uri=url.toURI();
                if (uri==null) throw new ExecutionError("Unable to convert URL to URI");
                desktop.browse(uri);
            }
        } catch(Exception e) {
            if (gui!=null) {
                JOptionPane.showMessageDialog(gui.getFrame(), "Unable to launch external web browser to show page: "+event.getDescription()+"\n"+e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                gui.logMessage("Error:"+e.getClass().getSimpleName()+":"+e.getMessage(),15);
            }
        }
      }
    }    


    // ----- Find and Replace ------

    private boolean wrapsearch=false;
    private boolean silentsearch=false;
    private boolean searchIsCaseSensitive=false;

    @Override
    public boolean find(String searchstring) {
       if (searchstring==null || searchstring.isEmpty()) {
            previousSearchstring="";
            outputEditor.requestFocusInWindow();
            outputEditor.select(0,0);
            return false;
       }
       if (document==null || document.getLength()==0) return false;
       if ((searchIsCaseSensitive && !searchstring.equals(previousSearchstring)) || (!searchIsCaseSensitive && !searchstring.equalsIgnoreCase(previousSearchstring))) {
           wrapsearch=true;
           previousSearchstring=searchstring;
           outputEditor.requestFocusInWindow();
           outputEditor.select(0,0);
       }
       if (outputdata.isHTMLformatted()) {
           return findInHTML(searchstring);
       }
       char[] string=(searchIsCaseSensitive)?searchstring.toCharArray():searchstring.toLowerCase().toCharArray(); // case-insensitive search

       int matchAt=-1; // position of matching character in text
       Segment text = new Segment();
       int offs = outputEditor.getCaretPosition(); // start of search in document
       int selStart=outputEditor.getSelectionStart();
       int selEnd=outputEditor.getSelectionEnd();
       int selectionsize=selEnd-selStart;
       if (selectionsize>0) offs=selStart+1; // Previous match is already highlighted. Start next search from position after beginning of selection  
       if (offs>=document.getLength()) offs=0; // caret at end of document. Wrap right away
       if (wrapsearch) {offs=0;wrapsearch=false;}
       text.setPartialReturn(true);
       int nleft = document.getLength()-offs;
       boolean foundMatch=false;
       loop:
       while (nleft > 0) {
           try {document.getText(offs, nleft, text);} catch (Exception e) {}
           for (int i=0;i<=text.count-string.length;i++) {
               for (int j=0;j<string.length;j++) {
                    char docChar=text.charAt(i+j);
                    if (!searchIsCaseSensitive)docChar=Character.toLowerCase(docChar);
                    if (docChar!=string[j]) break; // break j-loop
                    if (j==string.length-1) {
                        foundMatch=true;
                        matchAt=offs+i;
                        break loop;
                    }
               }
           }
           nleft -= text.count;
           offs += text.count;
       }
       if (foundMatch) {
           outputEditor.requestFocusInWindow();
           outputEditor.select(matchAt, matchAt+string.length);
           if (!silentsearch && gui!=null) gui.statusMessage(""); // clear message in case of wrapped-search (so that the "No matches found" message is not still showing)
           return true;
       } else {
           //outputEditor.select(0,0);
           if (!silentsearch && gui!=null) gui.statusMessage("Searching output for '"+searchstring+"'.   No more matches found      (Press CTRL+F to wrap search)");
           wrapsearch=true;
           return false;
       }
    }
 
     private boolean findInHTML(String searchstring) {
       char[] string=(searchIsCaseSensitive)?searchstring.toCharArray():searchstring.toLowerCase().toCharArray(); // case-insensitive search
       HTMLDocument htmldoc=(HTMLDocument)outputEditor.getDocument();
       Element root=htmldoc.getDefaultRootElement();
       int last = outputEditor.getCaretPosition(); // start of search in document
       int selStart=outputEditor.getSelectionStart();
       int selEnd=outputEditor.getSelectionEnd();
       int selectionsize=selEnd-selStart;
       if (selectionsize>0) last=last-selectionsize+1;
       if (wrapsearch) {last=0;wrapsearch=false;}
       //gui.logMessage("find after "+last);
       int matchAt=parseNode(root, htmldoc, string, last);
       if (matchAt>=0) {
           outputEditor.requestFocusInWindow();
           outputEditor.select(matchAt, matchAt+string.length);
           if (!silentsearch && gui!=null) gui.statusMessage(""); // clear message in case of wrapped-search (so that the "No matches found" message is not still showing)
           return true;
       } else {
           //outputEditor.select(0,0);
           if (!silentsearch && gui!=null) gui.statusMessage("Searching for '"+searchstring+"'.   No more matches found      (Press CTRL+F to wrap search)");
           wrapsearch=true;
           return false;
       }
    }

    private int parseNode(Element node, HTMLDocument htmldoc, char[] searchstring, int after) {
       //System.err.println(node.toString());
       int start=node.getStartOffset();
       int end=node.getEndOffset();
       if (end<after) return -1; // start position of search is after span of this node
       int children=node.getElementCount();
       if (node.getName().equals("content")) {
            int match=findTextInNode(htmldoc, start, end, searchstring, after);
            if (match>=0) return match;
       }
       for (int i=0;i<children;i++) {
           Element child=node.getElement(i);
           int match=parseNode(child,htmldoc,searchstring, after);
           if (match>=0 && match>=after) return match;
       }
       return -1;
    }

    private int findTextInNode(HTMLDocument htmldoc, int start, int end, char[] searchstring, int after) {
         int matchAt=-1; // position of matching character in text
         Segment text=new Segment();
         try {htmldoc.getText(start, end-start,text);} catch(Exception e) {}
         //gui.logMessage("Searching["+start+"-"+end+"]=>"+text.toString()+"  after="+after+"   range="+start+"-"+(start+text.count));
         boolean foundMatch=false;
         int pos=after-start;
         if (pos<0) pos=0;
         loop:
         for (int i=pos;i<=text.count-searchstring.length;i++) {
            //gui.logMessage("   ["+(start+i)+"] compare text "+(Character.toLowerCase(text.charAt(i)))+" to search "+searchstring[ssIndex]+"  => "+(Character.toLowerCase(text.charAt(i))==searchstring[ssIndex]));
               for (int j=0;j<searchstring.length;j++) {
                    char docChar=text.charAt(i+j);
                    if (!searchIsCaseSensitive)docChar=Character.toLowerCase(docChar);
                    if (docChar!=searchstring[j]) break; // break j-loop
                    if (j==searchstring.length-1) {
                        foundMatch=true;
                        matchAt=start+i;
                        break loop;
                    }
               }
         }
         if (foundMatch) return matchAt; else return -1;
    }


   @Override
    public void searchAndReplace() { // this is currently 'disabled' by supportsReplace()==false, since it does not work with undo/redo yet
        if (gui==null) return;
        FindReplaceDialog dialog=new FindReplaceDialog(this, gui);
        dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
        outputEditor.setEditable(true); // enable edits for replace only!
        document.addUndoableEditListener(gui.getUndoManager());
        //document.addDocumentListener(gui.getUndoRedoDocumentListener());
        dialog.setVisible(true);
        dialog.dispose();
        //document.removeDocumentListener(gui.getUndoRedoDocumentListener());
        document.removeUndoableEditListener(gui.getUndoManager());
        outputEditor.setEditable(false);
        gui.updateUndoRedoStates();
    }

    @Override
    public boolean replaceCurrent(String searchstring, String replacestring) {
        String currentSelection=outputEditor.getSelectedText();
        silentsearch=true;
        boolean wasreplaced=false;
        if (currentSelection==null
             || ( searchIsCaseSensitive && !currentSelection.equals(searchstring))
             || (!searchIsCaseSensitive && !currentSelection.equalsIgnoreCase(searchstring))
        ) find(searchstring);
        else {
            outputEditor.replaceSelection(replacestring);
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
        if (gui!=null) gui.statusMessage("Searching for '"+searchstring+"'.  Replaced "+count+" occurrence"+((count==1)?".":"s.")); 
        return count;
    }

    @Override
    public boolean supportsReplace() {
        return false;
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
    public String getSelectedTextForSearch() {
       String selection=outputEditor.getSelectedText();
       if (selection!=null && selection.trim().isEmpty()) return null;
       else return selection;
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

}
