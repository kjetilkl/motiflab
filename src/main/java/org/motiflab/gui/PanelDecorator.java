/*
 * A simple interface class describing PanelDecorators
 * which are object that can draw stuff on top of other panels 
 * (e.g. VisualizationPanel, SequenceVisualizer, DataTrackVisualizer)
 * 
 */
package org.motiflab.gui;

import java.awt.Component;
import java.awt.Graphics2D;

/**
 *
 * @author kjetikl
 */
public interface PanelDecorator {   
    public boolean isActive();  // a PanelDecorator that is active should be drawn. Inactive panels should be skipped when drawing
    public Object getPanelDecoratorProperty(String property); // this might be used for something in the future      
    public Class getTargetClass(); // The type of panels that this decorator should be applied to
    
    /** This method can be overridden to draw into the provided panel */
    public void draw(Graphics2D g, Component panel);    
}
