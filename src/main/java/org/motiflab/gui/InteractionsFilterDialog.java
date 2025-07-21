/*
 * InteractionsFilterDialog.java
 *
 *
 * Notes to self:
 * The current implementation stores the affected regions and sequences
 * direcly in datastructures and then compares these to regions queried by filters etc.
 * However, if the underlying data track is updated (for instance by someone deleting a single region)
 * The objects stored in this tools internal data structures will not be the same as those
 * updated objects used by the rest of the system so the comparisons (as done here) will fail
 * in those cases even if the two objects compared are believed to be the same since equals() would return true.
 * I think the reason why they are not regarded as equal is because the hashCode() method (which I have not always overriden)
 * would return two different values for the two objects that are considered to be the same for practical purposes here.
 *
 *
 * Created on 11.feb.2011, 12:41:07
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import org.motiflab.engine.DataListener;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.NumericConstant;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.gui.prompt.Prompt_MotifCollection;

/**
 *
 * @author kjetikl
 */
public class InteractionsFilterDialog extends javax.swing.JDialog implements RegionVisualizationFilter, MouseListener, DataListener {
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private VisualizationSettings settings;
    private boolean isForciblyClosed=false;
    private Object[] distanceConstraints=null;
    private int defaultMinDistance=0;
    private int defaultMaxDistance=16; // I read this number somewhere...
    private HashMap<RegionSequenceData,HashMap<Region,Integer>> networks=null;
    private Color ORANGE=new Color(255,204,0);
    private Color VIOLET=new Color(153,51,255);
    private Color LIGHT_BLUE=new Color(56,163,255);
    private Color DARK_GRAY=new Color(80,80,80);
    private Color GRAY=new Color(164,164,164);
    private Color VERY_LIGHT_GRAY=new Color(220,220,220);
    private Color LIGHTER_GRAY=new Color(205,205,205);
    private boolean showSingle=true;
    private HashSet<String> targetSet=null;
    private HashSet<String> interactingSet=null;
    private MotifCollection targetSetMotifs=null;
    private TargetSetTableModel tablemodel;
    private Timer timer=null;
    private Region currentSelectedRegion=null;
    private final String lock="Lock";
    private Color[] filteredMotifColors=new Color[]{LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY};


    /** Creates new form InteractionsFilterDialog */
    public InteractionsFilterDialog(MotifLabGUI guiclient) {
        super(guiclient.getFrame(), false);
        this.gui=guiclient;
        this.engine=gui.getEngine();
        this.settings=gui.getVisualizationSettings();
        initComponents();
        //animationPanel.setVisible(false); // just for now... I will include this again later
        minDistanceCombobox.setModel(getNumericDataCandidates(defaultMinDistance));
        maxDistanceCombobox.setModel(getNumericDataCandidates(defaultMaxDistance));
        gui.addRegionVisualizationFilter(this);
        gui.getVisualizationPanel().addMouseListener(this);
        this.setMinimumSize(new Dimension(520,280));
        pack();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                networks=null;
                if (!isForciblyClosed) closeButton.doClick();
            }
        });
        networks=new HashMap<RegionSequenceData,HashMap<Region,Integer>>();
        targetSet=new HashSet<String>();
        interactingSet=new HashSet<String>();
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index=((JTabbedPane)e.getSource()).getSelectedIndex();
                showSingle=(index==0);
                gui.redraw();
            }
        });
        targetSetMotifs=new MotifCollection("targetSetMotifs");
        tablemodel=new TargetSetTableModel();
        GenericMotifBrowserPanel motiftablepanel=new GenericMotifBrowserPanel(gui, tablemodel, false);
        JButton refreshButton=new JButton("Select Motifs");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshButtonClicked();
            }
        });
        motiftablepanel.getControlsPanel().add(refreshButton); // add it here to save space

        Dimension dim=new Dimension(380, 92);
        motiftablepanel.setPreferredSize(dim);
        motiftablepanel.setMinimumSize(dim);
        motiftablepanel.setMaximumSize(dim);
        motifstableouterpanel.add(motiftablepanel);
        engine.addDataListener(this); // this is necessary in order to update the datastructure references if the underlying data is updated
    }

    private void refreshButtonClicked() {
        Prompt_MotifCollection prompt=new Prompt_MotifCollection(gui, null, targetSetMotifs, true, "Select target motifs", true);
        prompt.disableNameEdit();
        prompt.setHeaderVisible(false);
        prompt.setLocation(gui.getFrame().getWidth()/2-prompt.getWidth()/2, gui.getFrame().getHeight()/2-prompt.getHeight()/2);
        prompt.setVisible(true);
        if (prompt.isOKPressed()) {
            targetSet.clear();
            interactingSet.clear();
            targetSetMotifs=(MotifCollection)prompt.getData();
            for (String motifname:targetSetMotifs.getAllMotifNames()) {
                targetSet.add(motifname);
                Data motif=engine.getDataItem(motifname);
                if (motif instanceof Motif) {
                    ArrayList<String> partners=((Motif)motif).getInteractionPartnerNames();
                    if (partners==null || partners.isEmpty()) continue;
                    for (String partner:partners) interactingSet.add(partner);
                }
            }
            tablemodel.fireTableDataChanged();
        }
        prompt.dispose();
        gui.redraw();
    }

    @Override
    public boolean shouldVisualizeRegion(Region region) {
        if (showSingle) {
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !networks.containsKey(sequence)) return true; // the sequence has not been filtered on yet
            if (!hideNonInteractions.isSelected()) return true; // visualize all regions (but 'gray out' non-interactions)
            return (networks.get(sequence).containsKey(region)); // visualize if the region is within the currently stored interaction network for this sequence
        } else {
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !sequence.isMotifTrack()) return true; //
            String motifname=region.getType();
            if (targetSet.contains(motifname) || interactingSet.contains(motifname)) return true;
            else return false;
        }
    }

    @Override
    public String getRegionVisualizationFilterName() {
        return "Interactions Filter";
    }

    @Override
    public Color getDynamicRegionColor(Region region) {
        if (showSingle) {
            boolean useNormalColor=!colorLevelsCheckbox.isSelected();
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !networks.containsKey(sequence)) return null; // the sequence has not been filtered on yet
            Integer levelInt=networks.get(sequence).get(region);
            if (levelInt==null) return VERY_LIGHT_GRAY; // 'gray out' non-interaction regions
            int level=levelInt.intValue();
            switch (level) {
                case 0: return (useNormalColor)?null:Color.BLACK; // use black color for first level
                //case 0: return null; // use default color for first level
                case 1: return (useNormalColor)?null:Color.RED;
                case 2: return (useNormalColor)?null:ORANGE;
                case 3: return (useNormalColor)?null:Color.YELLOW;
                case 4: return (useNormalColor)?null:Color.GREEN;
                case 5: return (useNormalColor)?null:Color.CYAN;
                case 6: return (useNormalColor)?null:LIGHT_BLUE;
                case 7: return (useNormalColor)?null:Color.BLUE;
                case 8: return (useNormalColor)?null:VIOLET;
                case 9: return (useNormalColor)?null:DARK_GRAY;
                case 10: return (useNormalColor)?null:GRAY;
                default: return (useNormalColor)?null:GRAY;//Color.LIGHT_GRAY; // use this for all levels higher than 10
            }
        } else {
            String motifname=region.getType();
            if (targetSet.contains(motifname)) return Color.BLACK;
            else if (interactingSet.contains(motifname)) return Color.RED;
            else return null;
        }
    }

    @Override
    public Color getDynamicRegionLabelColor(Region region) {
        if (showSingle) {
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !networks.containsKey(sequence)) return null; // the sequence has not been filtered on yet
            Integer levelInt=networks.get(sequence).get(region);
            if (levelInt==null) return Color.WHITE;// LIGHT_GRAY; // 'gray out' non-interaction regions
            else return null;
        } else return null;
    }

    @Override
    public Color getDynamicRegionBorderColor(Region region) {
        if (showSingle) {
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !networks.containsKey(sequence)) return null; // the sequence has not been filtered on yet
            Integer levelInt=networks.get(sequence).get(region);
            boolean drawborder=(settings.drawRegionBorders(region.getTrackName())>0);
            if (levelInt==null && drawborder) return Color.LIGHT_GRAY;// 'gray out' non-interaction regions
            else return null;
        } else return null;
    }


    @Override
    public java.awt.Color[] getDynamicMotifLogoColors(Region region) {
        if (showSingle) {
            RegionSequenceData sequence=region.getParent();
            if (sequence==null || !networks.containsKey(sequence)) return null; // the sequence has not been filtered on yet
            Integer levelInt=networks.get(sequence).get(region);
            if (levelInt==null) return filteredMotifColors;
            else return null;
        } else return null;
    }

    @Override
    public boolean appliesToTrack(String featureName) {
        return true;
    }

    @Override
    public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer) {

    }

    @Override
    public boolean drawsOverlay() {
        return false;
    }

    public int getPriority() { // filter priority
        return FILTER_PRIORITY_HIGH;
    }

    @Override
    public String[] getTargets() {
        return null;
    }


    @Override
    public void sequenceRepainted(String sequenceName, String featureName) {

    }


    @Override
    public void shutdown() {
        synchronized(lock) {
            if (timer!=null) {timer.stop();timer=null;}
        }
        gui.getVisualizationPanel().removeMouseListener(this);
        engine.removeDataListener(this);
        isForciblyClosed=true;
        gui.redraw();
        setVisible(false);
        this.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (timer!=null) {
            animateStartStopButton.doClick();
            return;
        }
        if (e.isShiftDown()) { // remove all networks in current sequence, regardless...
            FeatureSequenceData seq=gui.getVisualizationPanel().getCurrentFeatureSequenceData();
            if (!(seq instanceof RegionSequenceData) || !((RegionSequenceData)seq).isMotifTrack()) return;
            networks.remove((RegionSequenceData)seq);
            currentSelectedRegion=null;
        } else {
            Region region=gui.getVisualizationPanel().getCurrentRegion();
            currentSelectedRegion=region;
            if (region==null) {
                FeatureSequenceData seq=gui.getVisualizationPanel().getCurrentFeatureSequenceData();
                if (!(seq instanceof RegionSequenceData) || !((RegionSequenceData)seq).isMotifTrack()) return;
                networks.remove((RegionSequenceData)seq);
            } else {
                RegionSequenceData sequence=region.getParent();
                if (sequence==null) return;
                if (!((RegionSequenceData)sequence).isMotifTrack()) return;
                if (region!=null) updateNetwork(region);
            }
        }
        gui.redraw();
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}



    /** Returns ComboboxModels containing all NumericVariables and MotifNumericMaps as well as a default constant integer */
    private DefaultComboBoxModel getNumericDataCandidates(int defaultvalue) {
        ArrayList<String>candidateNames=new ArrayList<String>();
        for (Data item:engine.getAllDataItemsOfType(NumericVariable.class)) {
            candidateNames.add(item.getName());
        }
        for (Data item:engine.getAllDataItemsOfType(MotifNumericMap.class)) {
            candidateNames.add(item.getName());
        }
        Collections.sort(candidateNames);
        candidateNames.add(0,""+defaultvalue);
        String[] entries=new String[candidateNames.size()];
        entries=candidateNames.toArray(entries);
        DefaultComboBoxModel model=new DefaultComboBoxModel(entries);
        return model;
    }


    /** */
    private Object[] getDistanceRange() {
        Object[] range=new Object[2];
        String minDistString=(String)minDistanceCombobox.getSelectedItem();
        Object minDistString2=minDistanceCombobox.getEditor().getItem();
        if (minDistString2 instanceof String) minDistString=(String)minDistString2; // a hack to 'commit' values in the combobox
        Data minData=engine.getDataItem(minDistString);
        if (minData!=null) {
            if (minData instanceof NumericVariable || minData instanceof NumericConstant || minData instanceof MotifNumericMap) range[0]=minData;
            else return null;
        } else {
            try {
              range[0]=new Integer(Integer.parseInt(minDistString));
            } catch(NumberFormatException e) {return null;}
        }
        String maxDistString=(String)maxDistanceCombobox.getSelectedItem();
        Object maxDistString2=maxDistanceCombobox.getEditor().getItem();
        if (maxDistString2 instanceof String) maxDistString=(String)maxDistString2; // a hack to 'commit' values in the combobox
        Data maxData=engine.getDataItem(maxDistString);
        if (maxData!=null) {
            if (maxData instanceof NumericVariable || maxData instanceof NumericConstant || maxData instanceof MotifNumericMap) range[1]=maxData;
            else return null;
        } else {
            try {
              range[1]=new Integer(Integer.parseInt(maxDistString));
            } catch(NumberFormatException e) {return null;}
        }
        return range;
    }

     private void updateAllNetworks() {
        ArrayList<Region> targets=new ArrayList<Region>();
        for (RegionSequenceData seq:networks.keySet()) {
            HashMap<Region,Integer> list=networks.get(seq);
            for (Region reg:list.keySet()) {
                if (list.get(reg)==0) {targets.add(reg);break;}
            }
        }
        for (Region reg:targets) {
            updateNetwork(reg);
        }
    }

    /** Update the visualized interaction network spawned from the target region */
    private void updateNetwork(Region region) {
         Object[] range=getDistanceRange();
         if (range==null) return; // something is wrong with the user's input. I should probably notify!
         int maxlevels=(Integer)levelsSpinner.getValue();
         distanceConstraints=range;
         HashMap<Region,Integer> included=new HashMap<Region,Integer>();
         ArrayList<Region> list=new ArrayList<Region>();
         list.add(region);
         included.put(region,0);
         for (int i=0;i<list.size();i++) {
             Region nextnode=list.get(i);
             int nodelevel=included.get(nextnode);
             if (nodelevel==maxlevels) break;
             ArrayList<Region> partners=getInteractionPartners(nextnode,included);
             if (partners==null || partners.isEmpty()) continue;
             list.addAll(partners); // add interaction partners to list
             for (Region p:partners) included.put(p,nodelevel+1);
         }
         networks.put(region.getParent(),included);
    }


    /** Returns a list of interaction partners for the target region that are within the currently selected distance constraints */
    private ArrayList<Region> getInteractionPartners(Region region, HashMap<Region, Integer> exclude) {
        int mindist=(int)((distanceConstraints[0] instanceof MotifNumericMap)?((MotifNumericMap)distanceConstraints[0]).getValue(region.getType()):((Integer)distanceConstraints[0]).intValue());
        int maxdist=(int)((distanceConstraints[1] instanceof MotifNumericMap)?((MotifNumericMap)distanceConstraints[1]).getValue(region.getType()):((Integer)distanceConstraints[1]).intValue());
        Data motif=engine.getDataItem(region.getType());
        if (!(motif instanceof Motif)) return null;
        RegionSequenceData parent=region.getParent();
        int regStart=region.getRelativeStart();
        int regEnd=region.getRelativeEnd();
        int start=regStart-(maxdist+1); // +1 creates a 1bp interval-overlap with maximally distant regions
        int end=regEnd+(maxdist+1);  // +1 creates a 1bp interval-overlap with maximally distant regions
        ArrayList<Region> partners=parent.getRegionsOverlappingGenomicInterval(parent.getGenomicPositionFromRelative(start), parent.getGenomicPositionFromRelative(end));
        partners.remove(region); // the target region will be among the ones returned so just remove it
        // filter this list
        Iterator<Region> iter=partners.iterator();
        while (iter.hasNext()) {
            Region partner=iter.next();
            int dist=0;
            int partnerStart=partner.getRelativeStart();
            int partnerEnd=partner.getRelativeEnd();
                 if (partnerStart>regEnd) dist=partnerStart-(regEnd+1); // partner lies after
            else if (regStart>partnerEnd) dist=regStart-(partnerEnd+1); // partner lies before
            else dist=-1; // signals overlap
            if (dist<mindist || dist>maxdist) {iter.remove(); continue;}
            if (!((Motif)motif).isKnownInteractionPartner(partner.getType())) {iter.remove();continue;} // not an interaction partner
            if (exclude.containsKey(partner)) {iter.remove();continue;} // this region has been processed before
        }
        return partners;
    }

    @Override
    public void dataAdded(Data data) {}

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}

    @Override
    public void dataRemoved(Data data) {
        if (networks==null || networks.isEmpty()) return;
        if (!(data instanceof RegionDataset)) return; //
        if (!((RegionDataset)data).isMotifTrack()) return; //
        String trackName=data.getName();
        ArrayList<RegionSequenceData> toBeRemoved=new ArrayList<>();
        for (RegionSequenceData seq:networks.keySet()) {
            FeatureDataset parent=seq.getParent();
            if (parent!=null && parent.getName().equals(trackName)) {
                String seqName=seq.getSequenceName();
                RegionSequenceData newsequence=(RegionSequenceData)((RegionDataset)data).getSequenceByName(seqName);
                RegionSequenceData toremove=removeNetworkForUpdatedTrack(newsequence);
                if (toremove!=null) toBeRemoved.add(toremove);
            }
        }
        for (RegionSequenceData seq:toBeRemoved) {
            networks.remove(seq);
        }
    }

    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {}

    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}

    @Override
    public void dataUpdated(Data data) {
        if (networks==null || networks.isEmpty()) return;
        if (!(data instanceof RegionDataset)) return; //
        if (!((RegionDataset)data).isMotifTrack()) return; //
        String trackName=data.getName();
        ArrayList<Object[]> updates=new ArrayList<>();
        for (RegionSequenceData seq:networks.keySet()) {
            FeatureDataset parent=seq.getParent();
            if (parent!=null && parent.getName().equals(trackName)) {
                String seqName=seq.getSequenceName();
                RegionSequenceData newsequence=(RegionSequenceData)((RegionDataset)data).getSequenceByName(seqName);
                Object[] replaced=replaceNetworkForUpdatedTrack(newsequence);
                if (replaced!=null) updates.add(replaced);
            }
        }
        for (Object[] newdata:updates) {
            removeFromNetwork((RegionSequenceData)newdata[0]); // remove old binding
            networks.put((RegionSequenceData)newdata[0], (HashMap<Region,Integer>)newdata[1]);
        }
    }

    private void removeFromNetwork(RegionSequenceData seq) {
        Iterator iter=networks.keySet().iterator();
        while (iter.hasNext()) {
            RegionSequenceData oldsequence=(RegionSequenceData)iter.next();
            if (isSameSequence(oldsequence, seq)) {
                iter.remove();
            }
        }
    }

    private RegionSequenceData getFromNetwork(RegionSequenceData seq) {
        for (RegionSequenceData other:networks.keySet()) {
            if (isSameSequence(other, seq)) {
                return other;
            }
        }
        return null;
    }

    private Object[] replaceNetworkForUpdatedTrack(RegionSequenceData newsequence) {
        Iterator iter=networks.keySet().iterator();
        while (iter.hasNext()) {
            RegionSequenceData oldsequence=(RegionSequenceData)iter.next();
            if (isSameSequence(oldsequence, newsequence)) {
                HashMap<Region,Integer> rebuild=rebuildNetwork(oldsequence, newsequence);
                return new Object[]{newsequence, rebuild};
            }
        }
        return null;
    }

    private HashMap<Region,Integer> rebuildNetwork(RegionSequenceData oldsequence, RegionSequenceData newsequence) {
        HashMap<Region,Integer> oldNetwork=networks.get(oldsequence);
        HashMap<Region,Integer> newNetwork=new HashMap<Region,Integer>(oldNetwork.size());
        for (Region region:oldNetwork.keySet()) {
            ArrayList<Region> list=newsequence.getSameRegion(region); // find same region in new sequence
            if (!list.isEmpty()) {
                Region newregion=list.get(0); // just use the first one
                Integer value=oldNetwork.get(region);
                newNetwork.put(newregion,value);
            }
        }
        return newNetwork;
    }

    private RegionSequenceData removeNetworkForUpdatedTrack(RegionSequenceData newsequence) {
        Iterator iter=networks.keySet().iterator();
        while (iter.hasNext()) {
            RegionSequenceData oldsequence=(RegionSequenceData)iter.next();
            if (isSameSequence(oldsequence, newsequence)) {
                return oldsequence;
            }
        }
        return null;
    }

    /** Returns TRUE if the two sequences have the same name and are associated with the same track (by name) */
    private boolean isSameSequence(RegionSequenceData seq1, RegionSequenceData seq2) {
        if (seq1.getSequenceName().equals(seq2.getSequenceName())) {
           FeatureDataset parent1=seq1.getParent();
           FeatureDataset parent2=seq2.getParent();
           return (parent1!=null && parent2!=null && parent1.getName().equals(parent2.getName()));
        } else return false;
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        topPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        internalPanel = new javax.swing.JPanel();
        colorLevelsCheckbox = new javax.swing.JCheckBox();
        rangeLabel = new javax.swing.JLabel();
        levelsLabel = new javax.swing.JLabel();
        minDistanceCombobox = new javax.swing.JComboBox();
        maxDistanceCombobox = new javax.swing.JComboBox();
        levelsSpinner = new javax.swing.JSpinner();
        hideNonInteractions = new javax.swing.JCheckBox();
        toLabel = new javax.swing.JLabel();
        animationPanel = new javax.swing.JPanel();
        previousRegionButton = new javax.swing.JButton();
        nextRegionButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        animateStartStopButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        animationDelaySpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        allInteractionsPanel = new javax.swing.JPanel();
        motifstableouterpanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        bottomPanel = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(InteractionsFilterDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(400, 5));

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 456, Short.MAX_VALUE)
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        tabbedPane.setName("tabbedPane"); // NOI18N

        mainPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        internalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        internalPanel.setName("internalPanel"); // NOI18N
        internalPanel.setLayout(new java.awt.GridBagLayout());

        colorLevelsCheckbox.setSelected(true);
        colorLevelsCheckbox.setText(resourceMap.getString("colorLevelsCheckbox.text")); // NOI18N
        colorLevelsCheckbox.setName("colorLevelsCheckbox"); // NOI18N
        colorLevelsCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorLevelsSelectionUpdated(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        internalPanel.add(colorLevelsCheckbox, gridBagConstraints);

        rangeLabel.setText(resourceMap.getString("rangeLabel.text")); // NOI18N
        rangeLabel.setName("rangeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        internalPanel.add(rangeLabel, gridBagConstraints);

        levelsLabel.setText(resourceMap.getString("levelsLabel.text")); // NOI18N
        levelsLabel.setName("levelsLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        internalPanel.add(levelsLabel, gridBagConstraints);

        minDistanceCombobox.setEditable(true);
        minDistanceCombobox.setName("minDistanceCombobox"); // NOI18N
        minDistanceCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                distanceConstraintsUpdated(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 12, 10, 0);
        internalPanel.add(minDistanceCombobox, gridBagConstraints);

        maxDistanceCombobox.setEditable(true);
        maxDistanceCombobox.setName("maxDistanceCombobox"); // NOI18N
        maxDistanceCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                distanceConstraintsUpdated(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        internalPanel.add(maxDistanceCombobox, gridBagConstraints);

        levelsSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(0), null, Integer.valueOf(1)));
        levelsSpinner.setMinimumSize(new java.awt.Dimension(70, 24));
        levelsSpinner.setName("levelsSpinner"); // NOI18N
        levelsSpinner.setPreferredSize(new java.awt.Dimension(70, 24));
        levelsSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                levelsChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 12, 10, 0);
        internalPanel.add(levelsSpinner, gridBagConstraints);

        hideNonInteractions.setText(resourceMap.getString("hideNonInteractions.text")); // NOI18N
        hideNonInteractions.setName("hideNonInteractions"); // NOI18N
        hideNonInteractions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideOthersSelectionUpdated(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 4, 0);
        internalPanel.add(hideNonInteractions, gridBagConstraints);

        toLabel.setText(resourceMap.getString("toLabel.text")); // NOI18N
        toLabel.setName("toLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        internalPanel.add(toLabel, gridBagConstraints);

        animationPanel.setName("animationPanel"); // NOI18N
        animationPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 5));

        previousRegionButton.setText(resourceMap.getString("previousRegionButton.text")); // NOI18N
        previousRegionButton.setToolTipText(resourceMap.getString("previousRegionButton.toolTipText")); // NOI18N
        previousRegionButton.setName("previousRegionButton"); // NOI18N
        previousRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousRegionPressed(evt);
            }
        });
        animationPanel.add(previousRegionButton);

        nextRegionButton.setText(resourceMap.getString("nextRegionButton.text")); // NOI18N
        nextRegionButton.setToolTipText(resourceMap.getString("nextRegionButton.toolTipText")); // NOI18N
        nextRegionButton.setName("nextRegionButton"); // NOI18N
        nextRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextRegionPressed(evt);
            }
        });
        animationPanel.add(nextRegionButton);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        animationPanel.add(jLabel4);

        animateStartStopButton.setText(resourceMap.getString("animateStartStopButton.text")); // NOI18N
        animateStartStopButton.setToolTipText(resourceMap.getString("animateStartStopButton.toolTipText")); // NOI18N
        animateStartStopButton.setName("animateStartStopButton"); // NOI18N
        animateStartStopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                animateButtonPressed(evt);
            }
        });
        animationPanel.add(animateStartStopButton);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        animationPanel.add(jLabel3);

        animationDelaySpinner.setModel(new javax.swing.SpinnerNumberModel(200, 10, 10000, 10));
        animationDelaySpinner.setName("animationDelaySpinner"); // NOI18N
        animationDelaySpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                animationDelayChanged(evt);
            }
        });
        animationPanel.add(animationDelaySpinner);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 6, 0);
        internalPanel.add(animationPanel, gridBagConstraints);

        mainPanel.add(internalPanel, java.awt.BorderLayout.CENTER);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        mainPanel.add(jLabel1, java.awt.BorderLayout.PAGE_START);

        tabbedPane.addTab(resourceMap.getString("mainPanel.TabConstraints.tabTitle"), mainPanel); // NOI18N

        allInteractionsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        allInteractionsPanel.setName("allInteractionsPanel"); // NOI18N
        allInteractionsPanel.setLayout(new java.awt.BorderLayout());

        motifstableouterpanel.setName("motifstableouterpanel"); // NOI18N
        motifstableouterpanel.setLayout(new java.awt.BorderLayout());
        allInteractionsPanel.add(motifstableouterpanel, java.awt.BorderLayout.CENTER);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        allInteractionsPanel.add(jLabel2, java.awt.BorderLayout.PAGE_START);

        tabbedPane.addTab(resourceMap.getString("allInteractionsPanel.TabConstraints.tabTitle"), allInteractionsPanel); // NOI18N

        getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);
        tabbedPane.getAccessibleContext().setAccessibleName(resourceMap.getString("tabbedPane.AccessibleContext.accessibleName")); // NOI18N

        bottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 6));
        bottomPanel.setMinimumSize(new java.awt.Dimension(0, 40));
        bottomPanel.setName("bottomPanel"); // NOI18N
        bottomPanel.setPreferredSize(new java.awt.Dimension(400, 40));
        bottomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setMaximumSize(new java.awt.Dimension(75, 27));
        closeButton.setMinimumSize(new java.awt.Dimension(75, 27));
        closeButton.setName("closeButton"); // NOI18N
        closeButton.setPreferredSize(new java.awt.Dimension(75, 27));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonPressed(evt);
            }
        });
        bottomPanel.add(closeButton);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.PAGE_END);

        getAccessibleContext().setAccessibleName(resourceMap.getString("Form.AccessibleContext.accessibleName")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonPressed
        synchronized(lock) {
            if (timer!=null) {timer.stop();timer=null;}
        }
        gui.getVisualizationPanel().removeMouseListener(this);
        gui.removeRegionVisualizationFilter(this);
        gui.redraw();
        networks=null;
        setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonPressed


    private void levelsChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_levelsChanged
        updateAllNetworks();
        gui.redraw();
    }//GEN-LAST:event_levelsChanged

    private void colorLevelsSelectionUpdated(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorLevelsSelectionUpdated
        gui.redraw();
    }//GEN-LAST:event_colorLevelsSelectionUpdated

    private void distanceConstraintsUpdated(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_distanceConstraintsUpdated
        updateAllNetworks();
        gui.redraw();
    }//GEN-LAST:event_distanceConstraintsUpdated

    private void hideOthersSelectionUpdated(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideOthersSelectionUpdated
        gui.redraw();
    }//GEN-LAST:event_hideOthersSelectionUpdated

    private void animateButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_animateButtonPressed
       synchronized(lock) {
            if (timer==null) { // start new
                int delay=(Integer)animationDelaySpinner.getValue();
                timer=new Timer(delay, new Animator());
                animateStartStopButton.setText("Stop");
                timer.start();
            } else { // stop current
                timer.stop();
                timer=null;
                animateStartStopButton.setText("Start");
            }
        }
    }//GEN-LAST:event_animateButtonPressed

    private void previousRegionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousRegionPressed
        synchronized(lock) {
            if (timer!=null) animateStartStopButton.doClick();
        }
        if (currentSelectedRegion==null) return;
        RegionSequenceData sequence=currentSelectedRegion.getParent();
        if (sequence==null || !((RegionSequenceData)sequence).isMotifTrack()) return;
        Region nextregion=sequence.getPreviousRegion(currentSelectedRegion);
        currentSelectedRegion=nextregion;
        if (currentSelectedRegion==null) return;
        else updateNetwork(nextregion);
        gui.redraw();
    }//GEN-LAST:event_previousRegionPressed

    private void nextRegionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextRegionPressed
        synchronized(lock) {
            if (timer!=null) animateStartStopButton.doClick();
        }
        if (currentSelectedRegion==null) return;
        RegionSequenceData sequence=currentSelectedRegion.getParent();
        if (sequence==null || !((RegionSequenceData)sequence).isMotifTrack()) return;
        Region nextregion=sequence.getNextRegion(currentSelectedRegion);
        currentSelectedRegion=nextregion;
        if (currentSelectedRegion==null) return;
        else updateNetwork(nextregion);
        gui.redraw();
    }//GEN-LAST:event_nextRegionPressed

    private void animationDelayChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_animationDelayChanged
        synchronized(lock) {
           if (timer!=null) timer.setDelay((Integer)animationDelaySpinner.getValue());
        }
    }//GEN-LAST:event_animationDelayChanged


    private class Animator implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentSelectedRegion==null) return;
            RegionSequenceData sequence=currentSelectedRegion.getParent();
            if (sequence==null || !((RegionSequenceData)sequence).isMotifTrack()) return;
            Region nextregion=sequence.getNextRegion(currentSelectedRegion);
            currentSelectedRegion=nextregion;
            if (currentSelectedRegion==null) animateStartStopButton.doClick();
            else updateNetwork(nextregion);
            gui.redraw();
        }

    }

    private class TargetSetTableModel extends AbstractTableModel {

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public int getRowCount() {
                if (targetSetMotifs==null) return 0;
                else return targetSetMotifs.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (targetSetMotifs==null) return null;
                Motif motif=targetSetMotifs.getMotifByIndex(rowIndex,engine);
                if (columnIndex==0) return motif.getPresentationName();
                else return motif;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex==0) return String.class; else return Motif.class;
            }

            @Override
            public String getColumnName(int column) {
                if (column==0) return "Motif"; else return "Logo";
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel allInteractionsPanel;
    private javax.swing.JButton animateStartStopButton;
    private javax.swing.JSpinner animationDelaySpinner;
    private javax.swing.JPanel animationPanel;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JCheckBox colorLevelsCheckbox;
    private javax.swing.JCheckBox hideNonInteractions;
    private javax.swing.JPanel internalPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel levelsLabel;
    private javax.swing.JSpinner levelsSpinner;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JComboBox maxDistanceCombobox;
    private javax.swing.JComboBox minDistanceCombobox;
    private javax.swing.JPanel motifstableouterpanel;
    private javax.swing.JButton nextRegionButton;
    private javax.swing.JButton previousRegionButton;
    private javax.swing.JLabel rangeLabel;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JLabel toLabel;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

}
