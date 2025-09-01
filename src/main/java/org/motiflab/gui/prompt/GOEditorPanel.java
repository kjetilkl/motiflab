
package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.GOengine;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.gui.JSearchTextField;
import org.motiflab.gui.MotifLabGUI;

/**
 * This class is a small panel containing a 3-column table for showing and editing Gene Ontology terms.
 * 
 * @author kjetikl
 */
public class GOEditorPanel extends JPanel {

    private MotifLabEngine engine;
    private boolean editable=true;    
    private JPanel GOPanel;
    private JTable GOtable;
    private JButton addGOtermButton;
    
    
    public GOEditorPanel(MotifLabEngine engine, Collection<String> terms) {
        super();
        this.engine=engine;
        this.setLayout(new BorderLayout());
        setupGOPanel(terms);
        this.add(GOPanel);
        
    }
    
   private void setupGOPanel(Collection<String> terms) {
        GOPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        Object[][] tabledata=new Object[((terms!=null)?terms.size():0)][3]; // 3 columns: GO-term, domain, and description
        if (terms!=null && !terms.isEmpty()) {
            int i=0;
            for (String term:terms) {
                tabledata[i][0]=term;
                tabledata[i][1]=null; // no need to fill these in...
                tabledata[i][2]=null; // no need to fill these in...   
                i++;
            }
            engine.getGeneOntologyEngine().getGOdescriptions(terms); // this will force the GOengine to cache the existing terms
        }
        final DefaultTableModel GOtablemodel=new DefaultTableModel(tabledata, new String[]{"GO term","Domain","Description"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return (editable && column==0); // only the first column should be editable! The others are derived from the GO accession number (via a lookup-table)
            }
            @Override
            public Object getValueAt(int row, int column) {
                String term=(String)super.getValueAt(row, 0);  // only return the GO accession (value in first column) for all columns!
                if (column==0) return term;
                String[] info=(term==null || term.isEmpty())?null:engine.getGeneOntologyEngine().getGOdescriptionAndDomain(term);
                if (info==null) return term;
                else return info[column-1];
            }
        };
        GOtable=new JTable(GOtablemodel);
        GOtable.getTableHeader().setReorderingAllowed(false);
        GOtable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        GOtable.setDefaultRenderer(Object.class, new GOtermRenderer());
        GOtable.setDefaultEditor(Object.class, new GOtermEditor());
        GOtable.setCellSelectionEnabled(true);
        GOtable.getColumn("GO term").setMaxWidth(100);
        GOtable.getColumn("GO term").setMinWidth(100);
        GOtable.getColumn("GO term").setPreferredWidth(100);        
        GOtable.getColumn("Domain").setMaxWidth(60);
        GOtable.getColumn("Domain").setMinWidth(60);
        GOtable.getColumn("Domain").setPreferredWidth(60);
        GOtable.setAutoCreateRowSorter(true);        
        GOtablemodel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                GOtable.repaint();
            }
        });
        JPanel internalpanel=new JPanel(new BorderLayout());
        JPanel bottomPanel=new JPanel(new BorderLayout());
        JPanel controlspanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel searchPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSearchTextField searchField = new JSearchTextField();
        searchField.enableRowFiltering(GOtable);
        searchField.enableSimpleTableSearch(GOtable);
        searchPanel.add(searchField);
        addGOtermButton=new JButton("Add GO term");
        addGOtermButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String[] newRowValues=new String[GOtable.getColumnCount()];
               for (int j=0;j<newRowValues.length;j++) newRowValues[j]="";
               newRowValues[newRowValues.length-1]=null;
               GOtablemodel.addRow(newRowValues);
               int newrow=getFirstEmptyRowInTable(GOtable);
               if (newrow>=0) {
                   boolean canedit=GOtable.editCellAt(newrow, 0);
                   if (canedit) {
                       GOtable.changeSelection(newrow, 0, false, false);
                       GOtable.requestFocus();
                   }
               }
            }
        });
        if (editable) controlspanel.add(addGOtermButton);
        bottomPanel.add(searchPanel,BorderLayout.WEST);
        bottomPanel.add(controlspanel,BorderLayout.CENTER);
        internalpanel.add(new JScrollPane(GOtable),BorderLayout.CENTER);
        internalpanel.add(bottomPanel,BorderLayout.SOUTH);
        internalpanel.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        Dimension dim=new Dimension(500,290);
        internalpanel.setMinimumSize(dim);
        internalpanel.setPreferredSize(dim);
        //internalpanel.setMaximumSize(dim);
        GOPanel.add(internalpanel);        
        GOtable.getRowSorter().toggleSortOrder(2); // sort alphabetically by description
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
    
    /** Returns the GO terms in the table as a String array. These will on the form "GO:nnnnnnn"  */
    public String[] getGOterms() {
        int rows=GOtable.getRowCount();
        ArrayList<String> GOterms=new ArrayList<String>();        
        for (int i=0;i<rows;i++) {
           Object string=GOtable.getValueAt(i, 0);
           if (string==null) continue;
           String term=string.toString().trim();
           if (term.isEmpty()) continue;
           if (!GOterms.contains(term)) GOterms.add(term);
        }
        String[] termsAsArray=new String[GOterms.size()];
        termsAsArray=GOterms.toArray(termsAsArray);   
        return termsAsArray;
    }
    
    
    private class GOtermRenderer extends DefaultTableCellRenderer {
        public GOtermRenderer() {
             super();
             //this.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String term=(String)table.getValueAt(row, 0);
            String[] info=(term==null || term.isEmpty())?null:engine.getGeneOntologyEngine().getGOdescriptionAndDomain(term);
            
            if (column==0) { // term column (GO accession)
                this.setText((value!=null)?value.toString():"");
                this.setForeground(Color.BLACK);
            } else if (column==1) { // domain
                this.setText((info!=null)?info[0]:"");
                this.setForeground(Color.BLACK);
            } else if (column==2) { // description column         
                this.setText((info!=null)?info[1]:"");
                this.setForeground(Color.BLACK);
            }
            // tool-tip-text
            if (term==null || term.isEmpty()) {
                this.setToolTipText("");
            } else if (info==null || info[0].isEmpty()) {
               this.setToolTipText("Unknown gene ontology term \""+term+"\""); 
            } else {
               String domain=GOengine.getFullDomain(info[0]); 
               String body=MotifLabGUI.formatTooltipString(info[1], 120,false,false);               
               String tooltip="<html><b>"+term+"</b>&nbsp;&nbsp;&nbsp;&nbsp;"+domain+"&nbsp;&nbsp;<br><br>"+body+"</html>";
               this.setToolTipText(tooltip);
            }            
            return this;
        }
    }    
    
     public class GOtermEditor extends DefaultCellEditor {

        public GOtermEditor() {
            super(new JTextField());
            ((JTextField)getComponent()).setBorder(new javax.swing.border.LineBorder(Color.black));
        }

        @Override
        public boolean stopCellEditing() {
            JTextField textfield = (JTextField)getComponent();
            String term=textfield.getText();
            if (term==null) return super.stopCellEditing();
            else term=term.trim();
            if (term.isEmpty()) return super.stopCellEditing();
            else if (GOengine.checkGOterm(term)) {
                return super.stopCellEditing();
            } else { // term is invalid
                java.awt.Toolkit.getDefaultToolkit().beep();
                return false; //don't let the editor go away
            }
        }
        @Override
        public Object getCellEditorValue() {
            JTextField textfield = (JTextField)getComponent();
            String term=textfield.getText();
            if (term==null) return null;
            else term=term.trim();
            if (term.isEmpty()) return null;
            else {
                if (term.startsWith("GO:") || term.startsWith("go:")) term=term.substring(3);
                try {
                    int value=Integer.parseInt(term);
                    return "GO:"+String.format("%07d", value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

      }      
    
    public boolean isEditable() {
        return editable;
    }
     
    public void setEditable(boolean editable) {
        this.editable=editable;
        addGOtermButton.setVisible(editable); 
    }  
}
