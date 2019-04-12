/*
 
 
 */

package motiflab.gui;

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import javax.swing.JPanel;

/**
 *
 * @author kjetikl
 */
public class MemoryMonitor extends JPanel {
    private MemoryUsage memory;
    private DecimalFormat formatter;
    private int usewidth=0;
    private MotifLabGUI gui;
    
    public MemoryMonitor(MotifLabGUI motiflabgui, int width) {
        super();    
        formatter = new DecimalFormat("0.00");
        this.usewidth=width;
        gui=motiflabgui;
        this.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 Runtime.getRuntime().gc();
                 gui.logMessage("Garbage collecting...",20);
                 repaint();
             }
        
        });        
    }
    
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width=(usewidth>0)?usewidth:getWidth();
        int height=getHeight();
        memory=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        
        long total=memory.getMax();
        long used=memory.getUsed();
        int usage=(int)((used*100)/total);
        double free=(total-used)/(total*1.0);
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, width, height);
        if (usage<70) g.setColor(java.awt.Color.GREEN);
        else if (usage<90) g.setColor(java.awt.Color.YELLOW);
        else g.setColor(java.awt.Color.RED);
        int offset=(int)(free*(height-2));
        String msg="Memory used: "+usage+"%  ("+formatter.format(used/(1024.0*1024.0))+" of "+formatter.format(total/(1024.0*1024.0))+" MB)";   
        //String msg="height="+height+", offset="+offset+"  rect="+(height-(offset+2));
        setToolTipText(msg); 
        g.fillRect(1, 1+offset, width-2, height-(offset+2));
    }

}
