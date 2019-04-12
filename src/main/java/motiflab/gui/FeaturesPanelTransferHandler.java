/*
 
 
 */

package motiflab.gui;

import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import java.awt.datatransfer.*;
import javax.swing.TransferHandler;
import javax.swing.JList;
import javax.swing.JComponent;


/**
 * This class implements support for DataTransfer (Cut/Copy/Paste and Drag'n'Drop)
 * from and to the datapanels listing all Sequences and Features available. 
 * Specifically, if a Sequence or Feature is dragged onto the Procotol Editor, 
 * the name of the dataset should be copied to the caret position. 
 * If a Sequence/Feature is dropped (or pasted) onto an operation in the 
 * operationspanel (or vice versa) the operation-editor should pop up with the 
 * dataset preselected as source and target
 * 
 * @author kjetikl
 */
public class FeaturesPanelTransferHandler extends TransferHandler {
    private JList list;
    private MotifLabGUI gui;
    
          public FeaturesPanelTransferHandler(JList list, MotifLabGUI gui) {
              this.list=list;
              this.gui=gui;
          }
    
    
          @Override
          /**
           * This method is called repeatedly during a drag gesture to confirm
           * whether the area below the cursor can accept the datatransfer.
           * If it can accept the transfer at the current drop location the function
           * should return true, if not it should return false
           */
          public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import Strings
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }
                // OK, the import supports String transfer but is the location right?
                if (info.isDrop()) {
                  JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                  if (dl.getIndex() == -1) {
                    return false;
                  }
                }
                return true;
            }
          
            /**
             * The following method is called automatically on a successful drop
             * and should handle the actual import processing. The method should
             * return true if the import of the dataobject went OK
             * @param info
             * @return
             */
            @Override
            public boolean importData(TransferHandler.TransferSupport info) {
                // just to be sure, check one more time if we can import data of this type
                if (!canImport(info)) {
                    return false;
                }
                // Since TransferHandlers and TransferSupport can also be used by Cut/Copy/Paste
                // the following method call checks whether importData was called as the 
                // result of a drag'n'drop gesture or the result of a paste
                if (!info.isDrop()) { // if this is not the result of drag'n'drop we don't bother
                    return false;
                }
                JList.DropLocation droplocation = (JList.DropLocation)info.getDropLocation();
                FeaturesPanelListModel listModel = (FeaturesPanelListModel)list.getModel();                         
                // Get the string that is being dropped.
                Transferable t = info.getTransferable();
                String dragSourceName="<unknown>";
                try {
                    dragSourceName = (String)t.getTransferData(DataFlavor.stringFlavor);
                } 
                catch (Exception e) { 
                        gui.getEngine().errorMessage(e.getMessage(),0);
                        return false;
                } // unable to get data as string. Import is cancelled!
                
                // check whether the dragged item represents an operation or a dataitem in the same panel
                Operation operation=gui.getEngine().getOperation(dragSourceName); // is it an operation? 
                boolean isFeatureData=true;
                String[] sourceNames=dragSourceName.split(",");
                Data[] sourceData=new Data[sourceNames.length];
                int dropIndex=droplocation.getIndex();
                boolean dropToEnd=dropIndex >= listModel.getSize();
                for (int i=0;i<sourceNames.length;i++) {
                    Data dragDataset=listModel.getDataByName(sourceNames[i]);
                    if (dragDataset==null) isFeatureData=false;
                    if (dropToEnd) sourceData[i]=dragDataset;
                    else sourceData[sourceData.length-(i+1)]=dragDataset; // process in reverse order later if drop<end
                }            
                if (droplocation.isInsert()) { // item dropped between datasets (or beginning/end)    
                    if (operation!=null) return false; // operations can not be inserted between datasets
                    if (!isFeatureData) return false; // it is not feature data eiter?
                    gui.getVisualizationSettings().enableFeaturePanelListening(false);
                    for (Data dragDataset:sourceData) {
                        if (dropIndex == 0) {
                            //gui.debugMessage(dragSourceName + " dropped at beginning of list");
                            listModel.relocate(dragDataset, 0, false);
                        } else if (dropToEnd) {
                            //gui.debugMessage(dragSourceName + " dropped at end of list");
                            listModel.relocate(dragDataset, dropIndex-1, false);
                        } else {
                            String value1 = ((Data)listModel.get(dropIndex-1)).getName();  
                            String value2 = ((Data)listModel.get(dropIndex)).getName();
                            //gui.debugMessage(dragSourceName + " dropped between \"" + value1 + "\" and \"" + value2 + "\"");
                            int dragIndex=listModel.indexOf(dragDataset);
                            if (dragIndex<dropIndex) dropIndex--;
                            listModel.relocate(dragDataset,dropIndex, false);
                        }
                    }
                    listModel.setupMasterTrackorderList();
                    gui.getVisualizationSettings().enableFeaturePanelListening(true);
                    gui.getVisualizationSettings().rearrangeDataTracks();
                } else { // item dropped on top of a dataset
                    String dropTargetName = ((Data)listModel.get(droplocation.getIndex())).getName();
                    //gui.debugMessage(dragSourceName + " dropped on top of " + "\"" + dropTargetName + "\"");
                    if (operation!=null) { // an operation was dragged onto a dataset
                        Data dataset=gui.getEngine().getDataItem(dropTargetName);
                        OperationTask parameters=new OperationTask(operation.getName());
                        parameters.setParameter(OperationTask.OPERATION, operation);
                        parameters.setParameter(OperationTask.SOURCE, dataset);
                        gui.launchOperationEditor(parameters);
                    } else { // something else was dragged onto the dataset, possibly another dataset
                        return false; // after careful consideration, I will not allow this
                    }
                }                
		return true;
            }
            
            /**
             * This method declares which kinds of transfer actions are allowed
             * when the "DataPanel" (JList) component acts as a source for datatransfer.
             * In this case we only allow that datasets can be 'copied' out of
             * the list but never removed from it (at least not with DnD).
             * 
             * @param c 
             * @return
             */
            public int getSourceActions(JComponent c) {
                return COPY;
            }
            
            
            /* The createTransferable() method bundles up the data that should
             * be transferred when the component acts as a source for datatransfer
             * (a drag gesture is initiated on the component).   
             * 
             */
            @Override
            protected Transferable createTransferable(JComponent c) {
                Object[] selectedDatasets = list.getSelectedValues();
                if (selectedDatasets==null || selectedDatasets.length==0) return null;
                if (selectedDatasets.length==1) return new StringSelection(selectedDatasets[0].toString());
                else {
                    String selection=selectedDatasets[0].toString(); // for now I only return a string representation of the selected item     
                    for (int i=1;i<selectedDatasets.length;i++) selection+=","+selectedDatasets[i].toString();
                    return new StringSelection(selection);               
                }
            }
            



}
