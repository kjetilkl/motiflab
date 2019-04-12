/*
 * DownloadFilesDialog.java
 *
 * Created on Mar 3, 2017, 1:46:57 PM
 */
package motiflab.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * This class shows a GUI dialog that downloads a list of remote files while tracking the progress
 * 
 * @author kjetikl
 */
public class DownloadFilesDialog extends javax.swing.JDialog {

    private boolean abort=false;
    private boolean isFinished=false;
    private boolean errors=false;
    private int totalProgress=0;
    private int totalFiles=0;
    private ArrayList<Object[]> files=null;
    private HashMap<String,Object> status=null; // the key is the URL (as string). 
                                                // The value is either NULL (download not started), Boolean.FALSE (download in progress), Boolean.TRUE (download finished) or an Exception that ocurred during download
    private DownloadListener listener=null;
    
    /** 
     * Creates new form DownloadFilesDialog 
     * To show the dialog and start downloading, use the "download()" method (this will block if the dialog is modal)
     * This object can be used to access information when the download is complete or aborted.
     */
    public DownloadFilesDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);        
        initComponents();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                abortButton.doClick();
            }
        });   
    }
    
    
    /** 
     * Start downloading a set of files. This should be called on the EDT. If the dialog is modal, this method will block.
     * The download itself will be performed in a background thread and the dialog will show the progress.
     * When the download is complete, the dialog's ABORT button will change into a CLOSE button.
     * The DownloadFilesDialog can be queried afterwards to check if everything went OK.
     * @param files Specifies files to download. Each entry should be an Object[]{URL,File,Integer/Long}  
     *              where URL is the remote URL to download from, File is the local file to save to
     *              and Integer is the size of the remote file in bytes (can be NULL or 0 if size is unknown)
     *              URL and File could be also Strings which will then be converted into URL/File.
     * 
     */
    public void download(ArrayList<Object[]> filesList) {
        this.abort=false;
        this.isFinished=false;
        this.errors=false;
        this.files=filesList;
        status=new HashMap<>();
        for (Object[] file:files) {
            if (file[0]!=null) status.put((file[0].toString()), null);
        }   
        listener=new DownloadListener();
        SwingWorker worker=new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                totalProgress=0;
                for (Object[] file:files) {
                   totalProgress++;
                   String currentURL=null;
                   try {
                      URL url;
                      File destination;
                      if (file[0] instanceof URL) url=(URL)file[0];
                      else url=new URL(file[0].toString());
                      if (file[1] instanceof File) destination=(File)file[1];
                      else destination=new File(file[1].toString());
                      Long thisfilesize=null;
                      if (file[2] instanceof Long) thisfilesize=(Long)file[2];
                      else if (file[2] instanceof Integer) thisfilesize=new Long((Integer)file[2]);
                      currentURL=url.toString();
                      status.put(currentURL, Boolean.FALSE);
                      if (listener!=null) listener.propertyChange(new PropertyChangeEvent(currentURL, "newfile", null, thisfilesize));                      
                      motiflab.engine.util.FileUtilities.copyLargeURLToFile(url, destination, listener);  
                      status.put(currentURL, Boolean.TRUE);
                   } catch (Exception e) {
                      errors=true;
                      status.put(currentURL, e);
                   } 
                }
                return null;
            }
            @Override
            protected void done() {
               isFinished=true; 
               abortButton.setText("Close");
               DownloadFilesDialog.this.revalidate();
               DownloadFilesDialog.this.repaint();
            }
        };
        totalProgress=0;
        totalFiles=files.size();
        worker.execute();
        this.setVisible(true); // this will block if the dialog is modal
    }

    /** Returns TRUE if the download is finished (note that there can still be errors) */
    public boolean isDone() {
        return isFinished;
    }
    
    /** Returns TRUE if problems were encountered during download */
    public boolean isError() {
        return errors;
    }   
    
    /** Returns TRUE if the download was aborted */
    public boolean isAborted() {
        return abort;
    }       
    
    /** Abort this download (programmatically click the ABORT button) */
    public void doAbort() {
        abort=true;
        abortButton.doClick();        
    }
    
    public HashMap<String,Object> getStatus() {
        return status;
    }
    
    /** Returns the number of files that have been downloaded so far (or at least attempted downloaded) */
    public int getProgress() {
        return totalProgress;
    }
    
    private class DownloadListener implements PropertyChangeListener {
        long filesize = 0;  // size of current file
        String filename="<file>";  // name of current file
               
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
                 if (evt.getPropertyName().equals("progress")) reportProgress((Long)evt.getNewValue());
            else if (evt.getPropertyName().equals("error"))    reportError((Exception)evt.getNewValue());
            else if (evt.getPropertyName().equals("done"))     reportDone();
            else if (evt.getPropertyName().equals("newfile"))  startNewFile(evt);
        }
        
        private void reportProgress(final long size) {
            Runnable runner=new Runnable() {
                @Override
                public void run() {            
                    // Current file progress
                    if (filesize==0) { // total filesize is not known. Just report absolute number
                       totalProgressBar.setString(""+size);  // format this better!! in KB or MB
                    } else {
                        int progress=(int)Math.floor((double)size/(double)filesize*100.0);
                        totalProgressBar.setValue(progress);
                        totalProgressBar.setString(size+" / "+filesize); // format this better!! in KB or MB              
                    }
                    // Finely adjust the total progress also?      
                    // ....
                }
            };
            SwingUtilities.invokeLater(runner);
        }
        
        private void reportDone() {
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    progressbar.setValue(100);
                    progressbar.setString("Done!");
                    totalProgressBar.setValue(100);
                    totalProgressBar.setString("Done!");
                }
            };
            SwingUtilities.invokeLater(runner);
        }
        
        private void reportError(Exception e) {
            // To Be added later                    
        }
        
        private void startNewFile(PropertyChangeEvent evt) {
            filename=(String)evt.getSource();
            Object filesizeObject=evt.getNewValue();
            if (filesizeObject instanceof Long) filesize=(Long)filesizeObject;
            else if (filesizeObject instanceof Integer) filesize=(Integer)filesizeObject;
            else filesize=0;
            Runnable runner=new Runnable() {                
                @Override
                public void run() {
                    filenameLabel.setText(filename);
                    if (filesize==0) {
                        progressbar.setIndeterminate(true);
                        progressbar.setString("0");
                    } else {
                        progressbar.setIndeterminate(false);
                        progressbar.setValue(0);
                        progressbar.setString("0 / "+filesize);
                    }               
                    int progress=(int)Math.floor((double)totalProgress/(double)totalFiles*100.0);
                    totalProgressBar.setValue(progress);
                    totalProgressBar.setString(totalProgress+" / "+totalFiles); 
                }
            };
            SwingUtilities.invokeLater(runner);                                   
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

        controlsPanel = new javax.swing.JPanel();
        abortButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        innerPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        filenameLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        jPanel3 = new javax.swing.JPanel();
        totalProgressLabel = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        totalProgressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DownloadFilesDialog.class);
        setTitle(resourceMap.getString("")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        controlsPanel.setName("controlsPanel"); // NOI18N
        controlsPanel.setPreferredSize(new java.awt.Dimension(400, 40));
        controlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 10));

        abortButton.setText(resourceMap.getString("abortButton.text")); // NOI18N
        abortButton.setName("abortButton"); // NOI18N
        abortButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abortButtonPressed(evt);
            }
        });
        controlsPanel.add(abortButton);

        getContentPane().add(controlsPanel, java.awt.BorderLayout.SOUTH);

        mainPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(400, 160));
        mainPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        innerPanel.setName("innerPanel"); // NOI18N
        innerPanel.setPreferredSize(new java.awt.Dimension(390, 140));
        innerPanel.setLayout(new javax.swing.BoxLayout(innerPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 30));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 15));

        filenameLabel.setText(resourceMap.getString("filenameLabel.text")); // NOI18N
        filenameLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        filenameLabel.setName("filenameLabel"); // NOI18N
        jPanel1.add(filenameLabel);

        innerPanel.add(jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 30));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(380, 26));
        progressbar.setStringPainted(true);
        jPanel2.add(progressbar);

        innerPanel.add(jPanel2);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(400, 30));
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 15));

        totalProgressLabel.setText(resourceMap.getString("totalProgressLabel.text")); // NOI18N
        totalProgressLabel.setToolTipText(resourceMap.getString("totalProgressLabel.toolTipText")); // NOI18N
        totalProgressLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        totalProgressLabel.setName("totalProgressLabel"); // NOI18N
        jPanel3.add(totalProgressLabel);

        innerPanel.add(jPanel3);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(400, 30));
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        totalProgressBar.setName("totalProgressBar"); // NOI18N
        totalProgressBar.setPreferredSize(new java.awt.Dimension(380, 26));
        totalProgressBar.setStringPainted(true);
        jPanel4.add(totalProgressBar);

        innerPanel.add(jPanel4);

        mainPanel.add(innerPanel);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void abortButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_abortButtonPressed
        if (!isFinished) abort=true;
        this.setVisible(false);
    }//GEN-LAST:event_abortButtonPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton abortButton;
    private javax.swing.JPanel controlsPanel;
    private javax.swing.JLabel filenameLabel;
    private javax.swing.JPanel innerPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JProgressBar totalProgressBar;
    private javax.swing.JLabel totalProgressLabel;
    // End of variables declaration//GEN-END:variables
}
