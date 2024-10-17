package motiflab.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.Sequence;


/**
 *
 * @author kjetikl
 */
public class ExternalDBLinkMenu extends JMenu implements ActionListener {
    MotifLabEngine engine=null;
    MotifLabGUI gui=null;
    
    public ExternalDBLinkMenu(String identifier, MotifLabGUI gui) {
        super("Databases");
        engine=gui.getEngine();
        this.gui=gui;        
        updateMenu(identifier, true);
    }
    
    public ExternalDBLinkMenu(String identifier, String source, MotifLabGUI gui) {
        super("Databases");
        engine=gui.getEngine();
        this.gui=gui;        
        updateMenu(identifier,source, true);
    }    

    
    /** Updates the menus with databases matching the identifier 
     *  @param identifier This should be the name of a data object (either motif, module or sequence)
     *                    The object will be searched for properties matching external DB references
     *                    and the menu will be based on these properties
     *  @param clearCurrentMenu if TRUE, the current contents of the menu will be reset. If FALSE, the current menu will be updated with new menu items
     */
    public final void updateMenu(String identifier, boolean clearCurrentMenu) {
        if (clearCurrentMenu) this.removeAll();
        if (identifier==null || identifier.isEmpty()) {
            setEnabled(this.getMenuComponentCount()>0); 
            return;
        }
        String[] xDB=engine.getExternalDatabaseNames();
        Data data=engine.getDataItem(identifier);
        int entries=0;      
        Set<String> userprops=null;
        if (data instanceof Motif) userprops=((Motif)data).getUserDefinedProperties();          
        if (data instanceof Sequence) userprops=((Sequence)data).getUserDefinedProperties();     
        if (data instanceof ModuleCRM) userprops=((ModuleCRM)data).getUserDefinedProperties();             
        for (String db:xDB) { // check all known databases against motif/module/sequence properties
            if (userprops!=null && userprops.contains(db)) {
                Object dbObject=null;
                if (data instanceof Motif) {
                    dbObject=((Motif)data).getUserDefinedPropertyValue(db);
                    if (dbObject==null) dbObject=((Motif)data).getUserDefinedPropertyValue(db.toLowerCase()); // just in case...                  
                } else if (data instanceof Sequence) {
                    dbObject=((Sequence)data).getUserDefinedPropertyValue(db);
                    if (dbObject==null) dbObject=((Sequence)data).getUserDefinedPropertyValue(db.toLowerCase()); // just in case...                                      
                } else if (data instanceof ModuleCRM) {
                    dbObject=((ModuleCRM)data).getUserDefinedPropertyValue(db);
                    if (dbObject==null) dbObject=((ModuleCRM)data).getUserDefinedPropertyValue(db.toLowerCase()); // just in case...                                      
                }
                if (dbObject==null) continue;
                ArrayList<String> dbrefs=null;
                if (dbObject instanceof ArrayList) dbrefs=((ArrayList<String>)dbObject);
                else if (dbObject instanceof String) {
                    try {dbrefs=MotifLabEngine.splitOnComma((String)dbObject);} catch (Exception e) {}
                } else if (dbObject!=null) {
                    dbrefs=new ArrayList<String>(1);
                    dbrefs.add(dbObject.toString());
                }
                if (dbrefs!=null) {
                    ArrayList<JMenuItem> menuitems=new ArrayList<>(dbrefs.size());                    
                    for (String dbref:dbrefs) { // for each reference in this database
                        String url=engine.getExternalDatabaseURL(db, dbref);
                        if (url!=null) {
                           JMenuItem item=new JMenuItem(dbref);
                           item.setToolTipText("Lookup \""+dbref+"\" in "+db);
                           item.addActionListener(this);
                           item.setActionCommand(url); 
                           menuitems.add(item);
                        }
                    }
                    if (!menuitems.isEmpty()) entries++;
                    if (menuitems.size()==1) { // add single element directly. The display text will be the database names 
                        JMenuItem item=menuitems.get(0);
                        item.setText(db);
                        this.add(item);                       
                    } else if (menuitems.size()>1) { // add multiple elements to submenu. The display text of the menu with be the database name and the subelements will be named after IDs 
                        JMenu submenu=new JMenu(db);
                        for (JMenuItem item: menuitems) {
                            submenu.add(item);
                        }
                        this.add(submenu);
                    }                                       
                } // endif: dbrefs!=null
            }
        }
       setEnabled(this.getMenuComponentCount()>0);      
    }
    
    /** Updates the menus with databases matching the identifier 
     *  @param identifier This could any identifier string (it does not have to be the name of a data object)
     *  @param databases an explicit list of databases that can use this particular identifier
     */
    public final void updateMenu(String identifier, String[] databases, boolean clearCurrentMenu) {
        if (clearCurrentMenu) this.removeAll();
        if (identifier==null || identifier.isEmpty() || databases==null || databases.length==0) {
            setEnabled(this.getMenuComponentCount()>0); 
            return;
        }
        int entries=0;      
        for (String db:databases) {
            String url=engine.getExternalDatabaseURL(db, identifier);
            if (url!=null) {
               JMenuItem item=new JMenuItem(db);
               item.setToolTipText("Lookup \""+identifier+"\" in "+db);
               item.addActionListener(this);
               item.setActionCommand(url); 
               this.add(item);
               entries++;
            }           
        }
        setEnabled(this.getMenuComponentCount()>0);            
    }    
    
    /** Updates the menus with databases matching the identifier 
     *  @param identifier This could be any identifier string (it does not have to be the name of a data object)
     *  @param The name of the source (could e.g. be a datatrack). MotifLab will search for external databases registered for this source 
     */    
    public final void updateMenu(String identifier, String source, boolean clearCurrentMenu) {   
         String[] databases=null;
         String databasestring=(String)engine.getClient().getVisualizationSettings().getSettingAsType("system.databases."+source,"");
         if (databasestring!=null && !databasestring.isEmpty()) {
             databases=databasestring.trim().split("\\s*,\\s*");
         } 
         updateMenu(identifier,databases,clearCurrentMenu);
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String url=e.getActionCommand();
        try {
           java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(gui.getFrame(), "Unable to open external browser","Database", JOptionPane.ERROR_MESSAGE, null);
        }        
    }
    
}
