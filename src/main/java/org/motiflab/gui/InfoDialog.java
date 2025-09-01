/*
 * InfoDialog.java
 *
 * Created on 11. februar 2010, 10:53
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.lang.math.Fraction;

/**
 * This class represents a general information dialog that can be
 * used to display documents (possibly in HTML format) in a popup dialog window.
 * The dialog also contains a single "Close" button that can be used to dispose of the window
 * Clicking hyperlinks in a HTML-document will either open the new page in the same window
 * or in an external Web Browser depending on the OpenLinksInExternalBrowser flag.
 * Hyperlinks referring to local files ("file:" prefix) will open the parent directory
 * in a file browser
 * @author  kjetikl
 */
public class InfoDialog extends javax.swing.JDialog implements HyperlinkListener {
    private boolean openLinksInExternalBrowser=true;
    private ArrayList<URL> history=null;
    private JButton backButton=null;
    private JButton forwardButton=null;
    private int historyIndex=0;
    private String errorMessage=null;


    /** Creates new form InfoDialog with standard size 750x600*/
    public InfoDialog(MotifLabGUI gui, String title, String document) {
        this(gui,title,document,750,600);
    }

    /** Creates new form InfoDialog with given displaying the given Web page */
    public InfoDialog(MotifLabGUI gui, String title, URL url, int width, int height) {
       this(gui, title, url, width, height, true);
    }    
    
    /** Creates new form InfoDialog with given size displaying the given string document (HTML-formatted) */
    public InfoDialog(MotifLabGUI gui, String title, String document, int width, int height) {
        super((gui!=null)?gui.getFrame():null, title, true);  
        Component parent=(gui!=null)?gui.getFrame():getOwner();
        initComponents();
        progressbar.setIndeterminate(true);
        progressbar.setVisible(false);
        documentPane.setContentType("text/html");
        documentPane.setText(document);
        documentPane.setCaretPosition(0);
        documentPane.setFocusable(true);
        documentPane.setEditable(false);
        documentPane.addHyperlinkListener(this);
        getRootPane().setDefaultButton(okButton);
        Dimension d=new Dimension(width,height);
        this.setPreferredSize(d);
        this.setLocation((parent.getWidth()-d.width)/2, (parent.getHeight()-d.height)/2);
        pack();
    }

    /** Creates new form InfoDialog with given displaying the given Web page */
    public InfoDialog(MotifLabGUI gui, String title, URL url, int width, int height, boolean openLinksInExternalBrowser) {
        super((gui!=null)?gui.getFrame():null, title, true);       
        this.openLinksInExternalBrowser=openLinksInExternalBrowser;
        Component parent=(gui!=null)?gui.getFrame():getOwner();
        initComponents();
        progressbar.setIndeterminate(true);
        progressbar.setVisible(false);
        documentPane.setFocusable(true);
        documentPane.setEditable(false);
        documentPane.addHyperlinkListener(this);
        documentPane.setContentType("text/html");
        setPage(url);
        getRootPane().setDefaultButton(okButton);
        Dimension d=new Dimension(width,height);
        this.setPreferredSize(d);
        this.setLocation((parent.getWidth()-d.width)/2, (parent.getHeight()-d.height)/2);
        if (!openLinksInExternalBrowser) {
            history=new ArrayList<URL>();
            topPanel.setPreferredSize(new Dimension(100, 30));
            backButton=new JButton(new MiscIcons(MiscIcons.LEFT_ARROW));
            forwardButton=new JButton(new MiscIcons(MiscIcons.RIGHT_ARROW));
            MiscIcons backDisabledIcon=new MiscIcons(MiscIcons.LEFT_ARROW);
            MiscIcons forwardDisabledIcon=new MiscIcons(MiscIcons.RIGHT_ARROW);
            backDisabledIcon.setForegroundColor(Color.LIGHT_GRAY);
            forwardDisabledIcon.setForegroundColor(Color.LIGHT_GRAY);
            backButton.setDisabledIcon(backDisabledIcon);
            forwardButton.setDisabledIcon(forwardDisabledIcon);
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                   goBack();
                }
            });
            forwardButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                   goForward();
                }
            });
            topPanel.add(backButton);
            topPanel.add(forwardButton);
            history.add(url);
            historyIndex=0;
        }
        pack();
    }


    public void setMonospacedFont(int size) {
        documentPane.setFont(new Font(Font.MONOSPACED,Font.PLAIN,size));
    }

    public void setErrorMessage(String message) {
        errorMessage=message;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        URL url=event.getURL();
        if (url==null) return; // this can happen
        String protocol=url.getProtocol();
        if (protocol!=null && protocol.equals("file")) { // open in file browser
            try {
                File file=new File(url.getFile());
                File dir=(file.isDirectory())?file:file.getParentFile();
                if (dir!=null && dir.isDirectory()) java.awt.Desktop.getDesktop().browse(getFileURI(dir.getAbsolutePath()));
            } catch(Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to launch external file browser to show directory for: "+event.getURL().getFile(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (openLinksInExternalBrowser) {
            try {
                java.awt.Desktop.getDesktop().browse(url.toURI());
            } catch(java.lang.UnsupportedOperationException e) { 
                JOptionPane.showMessageDialog(this, "Unable to launch external web browser", "ERROR", JOptionPane.ERROR_MESSAGE);
            } catch(Exception error) {
                JOptionPane.showMessageDialog(this, "Unable to show page: "+event.getURL().toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        } else { // open new page in this window
            historyIndex++;
            if (historyIndex>=history.size()) history.add(url);
            else history.set(historyIndex, url);
            int size=history.size();
            for (int i=size-1;i>historyIndex;i--) history.remove(i); // clear rest of history after current index
            setPage(url);
            backButton.setEnabled(true);
            forwardButton.setEnabled(false);
        }
      }
    }

/** generate uri according to the filePath
     * Thanks to http://stackoverflow.com/users/515455/zammbi for this method
     * @param filePath
     * @return
     * @throws Exception
     */
private static URI getFileURI(String filePath) throws Exception {
    URI uri = null;
    filePath = filePath.trim();
    if (filePath.indexOf("http") == 0 || filePath.indexOf("\\") == 0) {
        if (filePath.indexOf("\\") == 0){
            filePath = "file:" + filePath;
            filePath = filePath.replaceAll("#", "%23");
        }
        filePath = filePath.replaceAll(" ", "%20");
        URL url = new URL(filePath);
        uri = url.toURI();
    } else {
        File file = new File(filePath);
        uri = file.toURI();
    }
    return uri;
}

    private void setPage(URL url) {
        progressbar.setVisible(true);
        LoadListener loadListener=new LoadListener();
        documentPane.setContentType("text/html");
        documentPane.setBackground(Color.WHITE);
        documentPane.addPropertyChangeListener("page", loadListener);
        try {
           documentPane.setPage(url);
        } catch (Exception e) { // apparently this doesn't apply if page does not load properly...
            if (errorMessage!=null) documentPane.setText(errorMessage);
            else documentPane.setText("<b>Unable to load page:</b>&nbsp;&nbsp;"+url.toExternalForm());
            progressbar.setVisible(false);
        }
        documentPane.removePropertyChangeListener(loadListener);
        documentPane.setCaretPosition(0);
    }

    private void goBack() {
        historyIndex--; if (historyIndex<0) historyIndex=0; //just in case
        URL url=history.get(historyIndex);
        setPage(url);
        backButton.setEnabled(historyIndex>0);
        forwardButton.setEnabled(historyIndex<history.size()-1);
    }

    private void goForward() {
        historyIndex++; if (historyIndex>=history.size()) historyIndex=history.size()-1; //just in case
        URL url=history.get(historyIndex);
        setPage(url);
        backButton.setEnabled(historyIndex>0);
        forwardButton.setEnabled(historyIndex<history.size()-1);
    }

    private class LoadListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            progressbar.setVisible(false);
        }

    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        documentPane = new javax.swing.JEditorPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        jPanel2 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 10));
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        documentPane.setEditable(false);
        documentPane.setName("documentPane"); // NOI18N
        jScrollPane1.setViewportView(documentPane);

        mainPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 40));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 10));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 5));

        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(80, 22));
        jPanel1.add(progressbar);

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.WEST);

        jPanel2.setName("jPanel2"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(InfoDialog.class);
        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonClicked(evt);
            }
        });
        jPanel2.add(okButton);

        buttonsPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(100, 10));
        buttonsPanel.add(jPanel3, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonClicked
// TODO add your handling code here:
    setVisible(false);//GEN-LAST:event_okButtonClicked
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JEditorPane documentPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

}
