
package org.motiflab.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
/**
* ExcelAdapter enables Copy-Paste Clipboard functionality on JTables.
* The clipboard data format used by the adapter is compatible with
* the clipboard format used by Excel. This provides for clipboard
* interoperability between enabled JTables and Excel.
*
* This code was published as Java Tip #77 at JavaWorld.com, 09/20/99
* @author Ashok Banerjee
* @author Jignesh Mehta
*/
public class ExcelAdapter implements ActionListener
   {
   private String rowstring,value;
   private Clipboard system;
   private StringSelection stsel;
   private JTable jTable1 ;
   private boolean expandable=false;
   private int convertNumerical=CONVERT_NONE;
   private Action oldTabAction=null;
   private Action oldArrowdownAction=null;

   public static final int CONVERT_NONE=0;
   public static final int CONVERT_TO_DOUBLE=1;
   public static final int CONVERT_TO_INTEGER=2;
   public static final int CONVERT_TO_DOUBLE_OR_INTEGER=3;


/**
 * Creates a new ExcelAdapter which allows for copy-paste functionality
 *
 * @param myJTable The target JTable
 * @param expandable if TRUE the table is allowed to expand by adding more rows if the number of rows in the clipboard object exceeds the current number of rows in the JTable
 * @param convertNumerical dictates how to handle cells containing numerical data.
 *                         If this is set to CONVERT_NONE no type conversion will be made and all pasted cells will contain String data
 *                         If this is set to CONVERT_TO_DOUBLE all cells whose contents can be parsed as numerical will be converted to instances of Double
 *                         If this is set to CONVERT_TO_INTEGER all cells whose contents can be parsed as numerical will be converted to instances of Int (and rounded if necessary)
 *                         If this is set to CONVERT_TO_DOUBLE_OR_INTEGER all cells whose contents can be parsed as numerical will be converted to instances of Double or Int (depending on the value)
 */
public ExcelAdapter(JTable myJTable, boolean expandable, int convertNumerical)
   {
      jTable1 = myJTable;
      this.expandable=expandable;
      this.convertNumerical=convertNumerical;
      InputMap inputMap=jTable1.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);
      // Identifying the copy KeyStroke user can modify this
      // to copy on some other Key combination.
      KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
      // Identifying the Paste KeyStroke user can modify this
      //to copy on some other Key combination.
      KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
      KeyStroke tabstroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0,false);
      KeyStroke arrowdown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0,false);
      oldTabAction=jTable1.getActionMap().get(inputMap.get(tabstroke));
      oldArrowdownAction=jTable1.getActionMap().get(inputMap.get(arrowdown));
      jTable1.registerKeyboardAction(this,"Copy",copy,JComponent.WHEN_FOCUSED);
      jTable1.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
      jTable1.registerKeyboardAction(this,"Delete",delete,JComponent.WHEN_FOCUSED);
      jTable1.registerKeyboardAction(this,"PressedTab",tabstroke,JComponent.WHEN_FOCUSED);
      jTable1.registerKeyboardAction(this,"PressedArrowdown",arrowdown,JComponent.WHEN_FOCUSED);
      
      system = Toolkit.getDefaultToolkit().getSystemClipboard();
   }
   /**
    * Public Accessor methods for the Table on which this adapter acts.
    */
public JTable getJTable() {return jTable1;}
public void setJTable(JTable jTable1) {this.jTable1=jTable1;}
   /**
    * This method is activated on the Keystrokes we are listening to
    * in this implementation. Here it listens for Copy and Paste ActionCommands.
    * Selections comprising non-adjacent cells result in invalid selection and
    * then copy action cannot be performed.
    * Paste is done by aligning the upper left corner of the selection with the
    * 1st element in the current selection of the JTable.
    */
public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().compareTo("Copy")==0)  {
         StringBuilder sbf=new StringBuilder();
         // Check to ensure we have selected only a contiguous block of
         // cells
         int numcols=jTable1.getSelectedColumnCount();
         int numrows=jTable1.getSelectedRowCount();
         int[] rowsselected=jTable1.getSelectedRows();
         int[] colsselected=jTable1.getSelectedColumns();
         if (rowsselected.length==0 || colsselected.length==0) return;
         if (!((numrows-1==rowsselected[rowsselected.length-1]-rowsselected[0] &&
                numrows==rowsselected.length) &&
                (numcols-1==colsselected[colsselected.length-1]-colsselected[0] &&
                numcols==colsselected.length)))
         {
            JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                                          "Invalid Copy Selection",
                                          JOptionPane.ERROR_MESSAGE);
            return;
         }
         for (int i=0;i<numrows;i++)  {
            for (int j=0;j<numcols;j++) {
               sbf.append(jTable1.getValueAt(rowsselected[i],colsselected[j]));
               if (j<numcols-1) sbf.append("\t");
            }
            sbf.append("\n");
         }
         stsel  = new StringSelection(sbf.toString());
         system = Toolkit.getDefaultToolkit().getSystemClipboard();
         system.setContents(stsel,stsel);
      }
      if (e.getActionCommand().compareTo("Paste")==0)  {
          int[] rows=jTable1.getSelectedRows();
          int[] cols=jTable1.getSelectedColumns();
          if (rows.length==0 || cols.length==0) return;
          int startRow=rows[0];
          int startCol=cols[0];
          try
          {
             String trstring= (String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor));
             //System.out.println("String is:"+trstring);
             StringTokenizer st1=new StringTokenizer(trstring,"\n");
             for(int i=0;st1.hasMoreTokens();i++)
             {
                rowstring=st1.nextToken();
                StringTokenizer st2=new StringTokenizer(rowstring,"\t");
                for(int j=0;st2.hasMoreTokens();j++)
                {
                   value=(String)st2.nextToken();
                   if (startRow+i==jTable1.getRowCount() && expandable) { // add row if necessary
                       TableModel model = jTable1.getModel();
                       if (model instanceof DefaultTableModel) {
                           Object[] newrow=new Object[jTable1.getColumnCount()];
                           for (int k=0;k<newrow.length;k++) newrow[k]="";
                           ((DefaultTableModel)model).addRow(newrow);
                       }
                   }
                   if (startRow+i<jTable1.getRowCount()&& startCol+j<jTable1.getColumnCount()) {
                        Object typeValue=value;
                        if (value!=null && convertNumerical!=CONVERT_NONE) {
                            try {
                               Double doubleval=Double.parseDouble(value);
                               if (convertNumerical==CONVERT_TO_INTEGER || (convertNumerical==CONVERT_TO_DOUBLE_OR_INTEGER && doubleval.intValue()==doubleval.doubleValue())) typeValue=new Integer(doubleval.intValue());
                               else typeValue=doubleval;
                            } catch (NumberFormatException ex) {}
                        }
                        if (jTable1.isCellEditable(startRow+i,startCol+j)) {
                            jTable1.setValueAt(typeValue,startRow+i,startCol+j);
                            //System.out.println("Convert["+typeValue.getClass().getSimpleName()+"] "+value+"=>"+typeValue+" at row="+startRow+i+"  column="+startCol+j+"  convertNumerical="+convertNumerical);
                        }
                   }
               }
            }
         }
         catch(Exception ex){}
      } 
      if (e.getActionCommand().compareTo("Delete")==0) {
          int[] rows=jTable1.getSelectedRows();
          int[] cols=jTable1.getSelectedColumns();   
          if (cols.length==jTable1.getColumnCount() && (jTable1.getModel() instanceof DefaultTableModel) && expandable) { // delete complete rows
              DefaultTableModel model = (DefaultTableModel)jTable1.getModel();
              for (int i=0;i<rows.length;i++) {
                 model.removeRow(rows[i]-i);
              }   
              if (model.getRowCount()==0) {
                   Object[] newrow=new Object[jTable1.getColumnCount()];
                   for (int k=0;k<newrow.length;k++) newrow[k]=null;
                   model.addRow(newrow);                  
              }
          } else { // delete only cell content
              for (int i=0;i<rows.length;i++) {
                 for (int j=0;j<cols.length;j++) {
                    jTable1.setValueAt(null,rows[i],cols[j]);
                 }
              }
          }
      }
      if (e.getActionCommand().compareTo("PressedTab")==0) {
          if (jTable1.getModel() instanceof DefaultTableModel && expandable) {
             int currentRow=jTable1.getSelectedRow();
             int currentColumn=jTable1.getSelectedColumn();
             if (currentRow+1==jTable1.getRowCount() && currentColumn+1==jTable1.getColumnCount()) {
                 Object[] newrow=new Object[jTable1.getColumnCount()];
                 for (int k=0;k<newrow.length;k++) newrow[k]=null;
                 ((DefaultTableModel)jTable1.getModel()).addRow(newrow);
             }
         }
         oldTabAction.actionPerformed(e);
      }
      if (e.getActionCommand().compareTo("PressedArrowdown")==0) {
         if (jTable1.getModel() instanceof DefaultTableModel && expandable) {
             int currentRow=jTable1.getSelectedRow();
             if (currentRow+1==jTable1.getRowCount()) {
                 Object[] newrow=new Object[jTable1.getColumnCount()];
                 for (int k=0;k<newrow.length;k++) newrow[k]=null;
                 ((DefaultTableModel)jTable1.getModel()).addRow(newrow);
             }
         }
         oldArrowdownAction.actionPerformed(e);
      }     
   }
}
