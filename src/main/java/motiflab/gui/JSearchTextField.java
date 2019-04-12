/*
 *  Copyright 2010 Georgios Migdos <cyberpython@gmail.com>.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package motiflab.gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultRowSorter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.analysis.GroupRowSorter;
import motiflab.gui.prompt.MyDefaultRowSorter;

/**
 *
 * @author Georgios Migdos <cyberpython@gmail.com>
 * 
 * Slightly modified by Kjetil Klepper for this project
 */
public class JSearchTextField extends JTextField implements FocusListener, MouseListener {

    private Icon icon;
    private ImageIcon grayIcon;
    private ImageIcon colorIcon;
    //private Insets dummyInsets;
    private String textWhenNotFocused = "Search...";
    private boolean hideNonMatching=false;
    private SearchFilter filter=null;
    private boolean inProgress=false;
    private Polygon hourglass=null;
    private String[][] structuredStringFilter=null;
    SwingWorker worker=null;
    

    public JSearchTextField(){
        super();
        this.icon = null;
        Dimension dim=new Dimension(180,30);
        this.setPreferredSize(dim);        
        java.net.URL grayIconURL=getClass().getResource("/motiflab/gui/resources/icons/searchTextfieldIcon16.png");
        if (grayIconURL!=null) grayIcon=new ImageIcon(grayIconURL);  
        java.net.URL colorIconURL=getClass().getResource("/motiflab/gui/resources/icons/searchTextfieldIconColor16.png");
        if (colorIconURL!=null) colorIcon=new ImageIcon(colorIconURL);           
        setIcon((hideNonMatching)?colorIcon:grayIcon);
        hourglass=new Polygon(new int[]{0,4,4,0,0,4,4,0}, new int[]{0,0,2,6,8,8,6,2}, 8);
        hourglass.translate(6, 8);
        this.addFocusListener(this);
    }

    public final void setIcon(Icon icon){
        this.icon = icon;
    }

    public Icon getIcon(){
        return this.icon;
    }


    @Override
    protected void paintComponent(Graphics g) {     
        super.paintComponent(g);
        int textX = 2;
        if(this.icon!=null){
            int iconWidth = icon.getIconWidth();
            int iconHeight = icon.getIconHeight();
            int x = 5; // dummyInsets.left + 5;//this is our icon's x
            textX = x+iconWidth+2; //this is the x where text should start
            int y = (this.getHeight() - iconHeight)/2;
            icon.paintIcon(this, g, x, y);
            if (inProgress) {  
                Object oldValue=((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);   
                g.drawPolygon(hourglass);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldValue);  
            }
        }
        setMargin(new Insets(2, textX, 2, 2));
        if (!this.hasFocus() && this.getText().equals("")) {
            int width = this.getWidth();
            int height = this.getHeight();
            Font prev = g.getFont();
            Font italic = prev.deriveFont(Font.ITALIC);
            Color prevColor = g.getColor();
            g.setFont(italic);
            g.setColor(UIManager.getColor("textInactiveText"));
            int h = g.getFontMetrics().getHeight();
            int textBottom = (height - h) / 2 + h - 4;
            int x = this.getInsets().left;
            Graphics2D g2d = (Graphics2D) g;
            RenderingHints hints = g2d.getRenderingHints();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, MotifLabGUI.ANTIALIAS_MODE);
            g2d.drawString(textWhenNotFocused, x, textBottom);
            g2d.setRenderingHints(hints);
            g.setFont(prev);
            g.setColor(prevColor);
        }

    }

    @Override
    public void focusGained(FocusEvent e) {
        this.repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        this.repaint();
    }

    /**
     * Enables this search field to be used to filter rows on the given table.
     * This functionality can be toggled by the user by clicking on 
     * the magnifying glass icon in the search field
     * @param flag 
     */
    public void enableRowFiltering(JTable table) {
        filter=new SearchFilter(table); // this will also install the filter on the table!
        this.addMouseListener(this);
        this.setToolTipText("Click on the magnifying glass to enable (orange icon) or disable (gray icon) filtering of non-matching rows");
    }
    
    /**
     * Returns TRUE if the "Hide non-matching" option has been chosen 
     * @return 
     */
    public boolean shouldHideNonMatching() {
        return hideNonMatching;
    }
    
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (filter!=null) {
           int textX = 2;
           if (this.icon!=null) {
                int iconWidth = icon.getIconWidth();
                int x = 5; // dummyInsets.left + 5;//this is our icon's x
                textX = x+iconWidth+2; //this is the x where text should start             
           }
           if (e.getX()<textX) {
                hideNonMatching=!hideNonMatching; // invert current selection
                setIcon((hideNonMatching)?colorIcon:grayIcon);
                filter.updateFilter();
                JSearchTextField.this.repaint();
           }
        }
    }    
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}    
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}    
    
    /**
     * Returns true if the text currently in the search field matches the provided value (an empty search string matches all values).
     * The search field can contain a boolean search expression using "|" or "," to denote alternative values (OR)
     * or "&" to separate between several substrings that must all be present. Both boolean operators can be in use at the
     * same time, in which case the OR operator takes precedence. 
     * @param value
     * @return 
     */
    public boolean isSearchMatch(String value) {
       String filterString=filter.filterString;           
       if (structuredStringFilter==null) return (filterString==null)?true:value.contains(filterString);
       for (int i=0;i<structuredStringFilter.length;i++) { // for each OR-level
           String[] ands=structuredStringFilter[i]; // must match all entries in this         
           if (ands!=null && ands.length>0) {
              int matches=0;
              for (String string:ands) if (string.isEmpty() || value.contains(string)) matches++;
              if (matches==ands.length) return true; // matching all AND entries 
           }
       }
       return false;               
    }
    
    private class SearchFilter extends RowFilter<Object, Object> implements ActionListener, KeyListener {
        String filterString;      
        JTable table;
        
        public SearchFilter(JTable table) {
            super();
            this.table=table;
            boolean ok=true;
            if (table.getRowSorter() instanceof DefaultRowSorter) ((DefaultRowSorter)table.getRowSorter()).setRowFilter(this); 
            else if (table.getRowSorter() instanceof MyDefaultRowSorter) ((MyDefaultRowSorter)table.getRowSorter()).setRowFilter(this);
            else if (table.getRowSorter() instanceof GroupRowSorter) ((GroupRowSorter)table.getRowSorter()).setRowFilter(this);
            else ok=false;
            if (ok) {
                JSearchTextField.this.addActionListener(this);
                JSearchTextField.this.addKeyListener(this);                  
            }
            // the following property change listener is used to assign the filter
            // to new rowSorters that might be installed (e.g. with the "AutoCreateRowSorter" option)
            table.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("rowSorter")) {
                        Object newsorter=evt.getNewValue();
                        if (newsorter instanceof DefaultRowSorter) ((DefaultRowSorter)newsorter).setRowFilter(SearchFilter.this); 
                        else if (newsorter instanceof MyDefaultRowSorter) ((MyDefaultRowSorter)newsorter).setRowFilter(SearchFilter.this);
                        else if (newsorter instanceof GroupRowSorter) ((GroupRowSorter)newsorter).setRowFilter(SearchFilter.this);
                    }
                }
            });
        }
        
        @Override
        public boolean include(Entry<? extends Object, ? extends Object> entry) {   
           if (filterString==null || !hideNonMatching) return true; // do not filter any rows      
           for (int i = entry.getValueCount() - 1; i >= 0; i--) {
               Object cellvalue=entry.getValue(i);
               if (cellvalue instanceof Color) continue; // ignore this
               String valueString=null;
               if (cellvalue instanceof Motif) valueString=((Motif)cellvalue).getPresentationName().toLowerCase();               
               else if (cellvalue instanceof Module) valueString=((Module)cellvalue).getNamePlusSingleMotifNames().toLowerCase();                             
               else valueString=entry.getStringValue(i).toLowerCase();
               if (isSearchMatch(valueString)) return true; // check one column at a time                       
           }
           return false;
        }
        
        private void updateFilter() {
            filterString=JSearchTextField.this.getText().trim();
            if (filterString.isEmpty()) filterString=null;
            else filterString=filterString.toLowerCase();
            if (filterString!=null && (filterString.indexOf(',')>0 || filterString.indexOf('|')>0 || filterString.indexOf('&')>0)) { // boolean search
                  String[] ors=filterString.split("\\s*(,|\\|)\\s*"); 
                  structuredStringFilter=new String[ors.length][];
                  for (int i=0;i<ors.length;i++) {
                     String[] ands=ors[i].split("\\s*&\\s*"); 
                     structuredStringFilter[i]=ands;
                  }                 
            } else structuredStringFilter=null;
                    
            if (table.getRowSorter() instanceof DefaultRowSorter) ((DefaultRowSorter)table.getRowSorter()).sort(); 
            else if (table.getRowSorter() instanceof MyDefaultRowSorter) ((MyDefaultRowSorter)table.getRowSorter()).sort(); 
            else if (table.getRowSorter() instanceof GroupRowSorter) ((GroupRowSorter)table.getRowSorter()).sort();           
            
   // ---- To do the sorting in a background thread ? Comment out the three lines above and uncomment the ones below instead ---          
//            inProgress=true; JSearchTextField.this.repaint();
//            if (worker!=null) worker.cancel(true);
//            worker=new SwingWorker<Boolean, Void>() {
//                @Override
//                public Boolean doInBackground() {
//                    if (table.getRowSorter() instanceof DefaultRowSorter) ((DefaultRowSorter)table.getRowSorter()).sort(); 
//                    else if (table.getRowSorter() instanceof MyDefaultRowSorter) ((MyDefaultRowSorter)table.getRowSorter()).sort(); 
//                    else if (table.getRowSorter() instanceof GroupRowSorter) ((GroupRowSorter)table.getRowSorter()).sort();                         
//                    return Boolean.TRUE;
//                } 
//                @Override
//                public void done() { // this method is invoked on the EDT!
//                    inProgress=false; JSearchTextField.this.repaint();
//                    worker=null;                    
//                }
//            }; 
//            worker.execute();                      
           
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            updateFilter();
        }
            
        @Override
        public void keyReleased(KeyEvent e) {
            updateFilter();
        }
        @Override
        public void keyPressed(KeyEvent e) {}
        @Override
        public void keyTyped(KeyEvent e) {}
                        
  } // end class SearchFilter    
    
}

