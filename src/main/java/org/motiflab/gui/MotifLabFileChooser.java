
package org.motiflab.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.motiflab.engine.MotifLabEngine;
// import org.motiflab.engine.datasource.ExtendedFileSystemView;

/**
 *
 * @author kjetikl
 */
public class MotifLabFileChooser extends JFileChooser {
    
    public MotifLabFileChooser(File directory, MotifLabEngine engine) {
        // --- I experienced problems with the ExtendedFileSystemView in Windows10 (only the Desktop was showing, but not other drives), so I disabled it ---
        // super(ExtendedFileSystemView.getFileSystemView(engine.getDataRepositories()));
        // ExtendedFileSystemView newfilesystem=(ExtendedFileSystemView)getFileSystemView();
        // this.setFileView(newfilesystem.getFileView()); // this fixes the icons      
        this.setCurrentDirectory(directory);
        // add possibility to delete files by pressing the DELETE key
        InputMap inputmap=this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionmap=this.getActionMap();       
        AbstractAction deleteKeyAction=new AbstractAction("DDeleteFFileAAction") {  @Override public void actionPerformed(ActionEvent e) {deleteSelectedFilesInDialog();} };
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DDeleteFFileAAction");
        actionmap.put("DDeleteFFileAAction", deleteKeyAction);
                
    }
    
    private void deleteSelectedFilesInDialog() {
        File[] files=getSelectedFiles();
        if (files==null || files.length==0) {
            File selected=getSelectedFile();
            if (selected==null) return;
            files=new File[]{selected};
        }
        int filecount=0;
        int foldercount=0;
        for (File file:files) {
            if (file.isDirectory()) foldercount++; else filecount++;
        }
        String message="Do you really want to delete ";
             if (filecount==1 && foldercount==0) message+="the file \""+files[0].getName()+"\" ?";
        else if (filecount==0 && foldercount==1) message+="the folder \""+files[0].getName()+"\" ?";
        else if (filecount>1 && foldercount==0) message+="these "+filecount+" files?";
        else if (filecount==0 && foldercount>1) message+="these "+foldercount+" folders?";
        else if (filecount>=1 && foldercount>=1) message+="the selected files and folders?";
        //message+="This action can not be undone!";
        int choice=JOptionPane.showConfirmDialog(this, message, "Delete selected files", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice==JOptionPane.OK_OPTION) {
            try {
                for (File file:files) {
                    file.delete();
                }    
            } catch (Exception e) {
                String errormessage=(e instanceof IOException)?e.getMessage():e.toString();
                JOptionPane.showMessageDialog(this, "Delete error", errormessage, JOptionPane.ERROR_MESSAGE); 
            }
            getUI().rescanCurrentDirectory(this);
        }
    }
    
    /** Searches a component recursively and returns the first child found of the target class */
    private Component findComponent(Component comp, Class targetClass) {       
        if (targetClass.isAssignableFrom(comp.getClass())) return comp;
        if (JComponent.class.isAssignableFrom(comp.getClass())) {
            Component[] components = ((JComponent)comp).getComponents();
            for(int i = 0; i < components.length; i++) {
                Component child = findComponent(components[i],targetClass);
                if (child != null) return child;
            }
        }
        return null;
    }      
    
}
