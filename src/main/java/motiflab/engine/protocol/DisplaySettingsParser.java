/*
 
 
 */

package motiflab.engine.protocol;

import motiflab.engine.task.DisplaySettingTask;

/**
 *
 * @author Kjetil
 */
public abstract class DisplaySettingsParser {

    protected Protocol protocol;
     /**
     * The method parses a string containing a visualization setting
     * and updates this setting in the given VisualizationSettings object
     * @param command A portion of a protocol specifying a visualization setting
     * @return
     */
    public abstract DisplaySettingTask parse(String command) throws ParseError;

    /**
     * Sets a reference back to the "parent" protocol which VisualizationSettingsParsers might
     * need to query the Protocol for information (for instance regarding the
     * type of different data objects)
     */
    public void setProtocol(Protocol protocol) {
        this.protocol=protocol;
    }
}
