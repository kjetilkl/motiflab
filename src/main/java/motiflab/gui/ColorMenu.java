/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 *
 * @author kjetikl
 */
public class ColorMenu extends JMenu {

     private static Object[] colors=new Object[] {
         new Object[]{"RED", Color.RED},
         new Object[]{"ORANGE", new Color(255,204,0)},
         new Object[]{"YELLOW", Color.YELLOW},
         new Object[]{"LIGHT GREEN", new Color(138,255,150)},
         new Object[]{"GREEN", Color.GREEN},
         new Object[]{"DARK GREEN", new Color(0,168,0)},
         new Object[]{"CYAN", Color.CYAN},
         new Object[]{"LIGHT BLUE", new Color(96,173,255)},
         new Object[]{"BLUE", Color.BLUE},
         new Object[]{"VIOLET", new Color(153,51,255)},
         new Object[]{"MAGENTA", Color.MAGENTA},
         new Object[]{"PINK", new Color(255, 150, 150)},
         new Object[]{"LIGHT BROWN", new Color(220,112,40)},
         new Object[]{"DARK BROWN", new Color(153,51,0)},
         new Object[]{"BLACK", Color.BLACK},
         new Object[]{"DARK GRAY", new Color(80,80,80)},
         new Object[]{"GRAY", new Color(164,164,164)},
         new Object[]{"LIGHT GRAY", new Color(220,220,220)},
         new Object[]{"WHITE", Color.WHITE}        
     };

    private ArrayList<Object[]> extra=null;
    private Color currentColor=null; 
    
    /**
     * Returns a ColorMenu with the provided listener and parent
     * @param title
     * @param listener
     * @param parent 
     */
    public ColorMenu(String title, ColorMenuListener listener, Component parent) {
        this(title,listener,parent,null);
    }    
    
    public ColorMenu(String title, Color currentColor, ColorMenuListener listener, Component parent) {
        this(title,listener,parent,null);
        this.currentColor=currentColor;
    }      
    
    /**
     * Returns a ColorMenu with the provided listener and parent. The menu is fitted with additional color options
     * @param title
     * @param listener
     * @param parent
     * @param extraColors A list of [String,Color [,Icon]] pairs that defines new color options to add to the menu
     */
    public ColorMenu(String title, ColorMenuListener listener, Component parent, ArrayList<Object[]> extraColors) {
        super(title);
        extra=extraColors;
        MenuListener menulistener=new MenuListener(listener, parent);
        for (Object entry:colors) {
              String colorname=(String)((Object[])entry)[0];
              Color color=(Color)((Object[])entry)[1];
              JMenuItem menuitem=new JMenuItem(colorname);
              menuitem.setIcon(new SimpleDataPanelIcon(12,12,color));
              menuitem.addActionListener(menulistener);
              add(menuitem);
        }
        if (extra!=null) {
            for (Object[] entry:extra) {
                  String colorname=(String)entry[0];
                  Color color=(Color)entry[1];
                  JMenuItem menuitem=new JMenuItem(colorname);
                  if (entry.length>=3 && entry[2] instanceof javax.swing.Icon) menuitem.setIcon((javax.swing.Icon)entry[2]);
                  else menuitem.setIcon(new SimpleDataPanelIcon(12,12,color));
                  menuitem.addActionListener(menulistener);
                  add(menuitem);
            }            
        }
        add(new JSeparator());
        JMenuItem menuitemOther=new JMenuItem("Other...");
        menuitemOther.addActionListener(menulistener);
        add(menuitemOther);
    }
    
    public void setCurrentColor(Color current) {
        currentColor=current;
    }
    
    private class MenuListener implements ActionListener {
       ColorMenuListener listener;
       Component parent;
       public MenuListener(ColorMenuListener listener,Component parent) {
           this.listener=listener;
           this.parent=parent;

       }
       @Override
       public void actionPerformed(ActionEvent e) {
            String colorname=e.getActionCommand();
            Color color=null;
             if (colorname.equals("Other...")) {
                color = JColorChooser.showDialog(parent, "Select new color", currentColor);
            } else {
               boolean found=false;
               for (Object entry:colors) {
                  String name=(String)((Object[])entry)[0];
                  if (name.equals(colorname)) {color=(Color)((Object[])entry)[1];found=true;break;}
               }
               if (extra!=null && !found) {
                   for (Object[] entry:extra) {
                      String name=(String)entry[0];
                      if (name.equals(colorname)) {color=(Color)entry[1];found=true;break;}
                   }                   
               }
            }
            listener.newColorSelected(color);
         }
    }  
    
    /** Takes the ColorMenu and wraps it in a JPopupMenu.
     *  Note that this will destroy the original ColorMenu since the menu items
     *  will be moved into the popup menu instead
     */
    public JPopupMenu wrapInPopup() {
        JPopupMenu popup=new JPopupMenu();
        while (this.getMenuComponentCount()>0) {
          popup.add(this.getMenuComponent(0)); 
        }
        return popup;    
    }
}
