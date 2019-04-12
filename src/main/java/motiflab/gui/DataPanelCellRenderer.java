/*
 
 
 */

package motiflab.gui;


import motiflab.engine.data.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import motiflab.engine.MotifLabEngine;

/**
 * Implements List cell renderers to display loaded datasets in the left
 * panels (dataset panels "Features" and "Data Objects") of the GUI
 * 
 * @author kjetikl
 */
public class DataPanelCellRenderer extends JPanel implements ListCellRenderer  {
    JLabel typeiconlabel; // a JLabel which is a placeholder for the typeicon
    JLabel name; // a JLabel which displays the dataset name
    SimpleDataPanelIcon typeicon; // the icon to the left of the dataset name which shows the type of dataset
    SimpleDataPanelIcon typeiconInstance; // Used for non-feature data (reusable)
    Color dragOntoColor = new Color(220,220,220);
    VisualizationSettings visualizationsettings;
    Font font;
    Font boldfont;
    Font bolditalicfont;
    Font italicfont;    
    
    GroupedTrackBorder groupedTrackBorder=new GroupedTrackBorder(16);
    Border iconBorder;
            
    public DataPanelCellRenderer(VisualizationSettings visualizationsettings) {
        super();
        this.visualizationsettings=visualizationsettings;
        setOpaque(false);
        setLayout(new BorderLayout());
        typeicon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.UNKNOWN_ICON,visualizationsettings);
        typeicon.drawBorder(false);
        typeicon.setBackgroundColor(this.getBackground());
        typeiconInstance=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.UNKNOWN_ICON,visualizationsettings);
        typeiconInstance.drawBorder(false);
        typeiconInstance.setBackgroundColor(this.getBackground());        
        typeiconlabel=new JLabel(typeicon);
        name=new JLabel("");
        name.setMinimumSize(new Dimension(30,20));

        add(typeiconlabel,BorderLayout.WEST);
        add(name,BorderLayout.CENTER);
        setPreferredSize(new Dimension(40,21));
        setMinimumSize(new Dimension(40,21));
        name.setOpaque(true);
        iconBorder = new BevelBorder(BevelBorder.RAISED, new Color(214,217,223), new Color(233,234,237), new Color(104,105,109), new Color(149,151,156));
        typeiconlabel.setBorder(iconBorder);        
        font=this.getFont();
        boldfont=font.deriveFont(java.awt.Font.BOLD);
        bolditalicfont=font.deriveFont(java.awt.Font.BOLD+java.awt.Font.ITALIC);
        italicfont=font.deriveFont(java.awt.Font.ITALIC);        
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Data dataobject=(Data)value;
        String trackName=dataobject.getName();        
        typeicon=null;
        if (dataobject instanceof FeatureDataset && visualizationsettings.isTrackVisible(trackName)) {
            String graphType=visualizationsettings.getGraphType(trackName);
            typeicon=getGraphTypeIcon(graphType);
        }
        if (typeicon==null) typeicon=typeiconInstance; // reuse common instance for non-feature datasets
        typeicon.setVisualizationSettings(visualizationsettings);
        typeicon.drawBorder(false);
        typeicon.setBackgroundColor(this.getBackground());
        typeiconlabel.setIcon(typeicon);            
        if (isSelected) {
            name.setBackground(list.getSelectionBackground());
            name.setForeground(list.getSelectionForeground());
        } else {
            name.setBackground(list.getBackground());
            name.setForeground(list.getForeground());      
        }
        if (list.getDropLocation()!=null && !list.getDropLocation().isInsert() && list.getDropLocation().getIndex()==index) {
            name.setBackground(dragOntoColor);
        }
        if (dataobject==null) { // this really should not happen
             typeicon.setIconType(SimpleDataPanelIcon.HIDDEN_ICON);
             name.setText("   ");
             return this; 
        }
        if (dataobject instanceof FeatureDataset) { 
            typeicon.setTrackName(trackName);
            boolean isGrouped=visualizationsettings.isGroupedTrack(trackName);            
            boolean trackVisible=visualizationsettings.isTrackVisible(trackName); 
            if (trackVisible) {
                typeicon.setForegroundColor(visualizationsettings.getForeGroundColor(trackName));
                typeicon.setBackgroundColor(visualizationsettings.getBackGroundColor(trackName));
                typeicon.setSecondaryColor(visualizationsettings.getSecondaryColor(trackName));          
                typeicon.setBaselineColor(visualizationsettings.getBaselineColor(trackName));             
                if (dataobject instanceof NumericDataset) {
                    typeicon.setGradient(visualizationsettings.getColorGradient(trackName));
                    typeicon.setSecondaryGradient(visualizationsettings.getSecondaryGradient(trackName));
                }
                if (isGrouped) { // this track is "visible" but the parent may be hidden (or non-existing), and if that is the case we should use the "hidden" form of the border
                   String parentTrack=visualizationsettings.getParentTrack(trackName);
                   boolean parentIsHidden=(parentTrack==null || !visualizationsettings.isTrackVisible(parentTrack));
                   groupedTrackBorder.setHidden(parentIsHidden);
                }
            } else { // track is hidden
                typeicon.setIconType(SimpleDataPanelIcon.HIDDEN_ICON);
                name.setForeground(Color.LIGHT_GRAY); // gray out name of hidden tracks in FeaturesPanel
                groupedTrackBorder.setHidden(true); // in case we need it                                   
            }
            this.setBorder((isGrouped)?groupedTrackBorder:null); // use a border to create an indent for grouped tracks
            
        } else { // non-feature dataset
            typeicon.setIconType(SimpleDataPanelIcon.getIconTypeForData(dataobject));
            typeicon.setForegroundColor(Color.black);
            typeicon.setBackgroundColor(Color.white);            
        }       

        String typedescription=dataobject.getTypeDescription();
        if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isModuleTrack()) {
            typedescription+=", Module track";
            name.setFont(bolditalicfont);
        }
        else if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isMotifTrack()) {
            typedescription+=", Motif track";
            name.setFont(boldfont);
        } else if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isNestedTrack()) {
            typedescription+=", Linked track";
            name.setFont(italicfont);            
        } else name.setFont(font);
        this.setToolTipText(trackName+"    [ "+typedescription+" ]");
        name.setText("   "+trackName);
        return this;
    }

    private SimpleDataPanelIcon getGraphTypeIcon(String graphtype) {  
        MotifLabEngine engine=visualizationsettings.getEngine();
        if (engine!=null) {
            Object icon=engine.getResourceIcon(graphtype,"DTV");
            if (icon instanceof javax.swing.Icon) return (SimpleDataPanelIcon)icon;
        }
        return new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.UNKNOWN_ICON, null);
    }    
    

    // ------------   Border class used to inset grouped tracks -------------------------
    
private class GroupedTrackBorder extends EmptyBorder {
    
    protected Color[] background=new Color[]{new Color(150,150,150),new Color(200,200,200)};
    protected Color[] arrowColor=new Color[]{Color.BLACK,new Color(150,150,150)};
    protected Color[] aliasColor=new Color[]{new Color(75,75,75),new Color(175,175,175)}; // this should be somewhere between background color and arrow color

    protected boolean hidden=false;   

    /**
     * Creates a GroupedDataTrackBorder border with the specified indent.
     * @param indent the left inset of the border
     */
    public GroupedTrackBorder(int indent)   {
        super(0, indent, 0, 0);
    }
    
    public void setHidden(boolean hidden) {
        this.hidden=hidden;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int index=(hidden)?1:0;
        Insets insets = getBorderInsets(c);
        Color oldColor = g.getColor();
        g.translate(x, y);
        g.setColor(background[index]);
        g.fillRect(0, 0, width - insets.right, insets.top);
        g.fillRect(0, insets.top, insets.left, height - insets.top);
        g.fillRect(insets.left, height - insets.bottom, width - insets.left, insets.bottom);
        g.fillRect(width - insets.right, 0, insets.right, height - insets.bottom);       
        int toph=2, hh=12;
        g.setColor(aliasColor[index]); // draw "hook"
        g.fillRect(6,toph-1,2,1); // aliasing...
        g.fillRect(5,toph+1,4,1);
        g.fillRect(4,toph+3,6,1);
        g.fillRect(3,toph+5,8,1);    
        g.setColor(arrowColor[index]); // 
        g.fillRect(6,toph,2,hh);  // vertical of L
        g.fillRect(6,toph+hh-2,7,2); // horisontal of L 
        g.fillRect(5,toph+2,4,2); // arrow head
        g.fillRect(4,toph+4,6,2); // arrow head       
        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        return computeInsets(insets);
    }

    public Insets getBorderInsets() {
        return computeInsets(new Insets(0,0,0,0));
    }

    private Insets computeInsets(Insets insets) {
        insets.left = left;
        insets.top = top;
        insets.right = right;
        insets.bottom = bottom;       
        return insets;
    }

    public boolean isBorderOpaque() {
        return true;
    }


} // end of private class: GroupedTrackBorder   

}

