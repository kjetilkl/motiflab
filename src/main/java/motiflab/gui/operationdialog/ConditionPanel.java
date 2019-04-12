/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.Component;
import javax.swing.JPanel;
import motiflab.engine.operations.Condition;

/**
 *
 * @author kjetikl
 */
public abstract class ConditionPanel extends JPanel {
    
    public ConditionPanel() {
        super();
    }
    
    public abstract Condition getCondition();
    
    
    @Override 
    public void setEnabled(boolean enabled) {
         Component[] com = getComponents();   
         for (int a = 0; a < com.length; a++) {  
              if (com[a] instanceof JPanel) setEnabledRecursive((JPanel)com[a], enabled);
              else com[a].setEnabled(enabled);  
         }          
    }
    
    private void setEnabledRecursive(JPanel panel, boolean enabled) {
         Component[] com = panel.getComponents();   
         for (int a = 0; a < com.length; a++) {  
              if (com[a] instanceof JPanel) setEnabledRecursive((JPanel)com[a], enabled);
              else com[a].setEnabled(enabled);  
         }           
    }
}
