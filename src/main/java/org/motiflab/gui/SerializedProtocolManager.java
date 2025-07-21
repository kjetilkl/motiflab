/*
 
 
 */

package org.motiflab.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.protocol.SerializedStandardProtocol;
import org.motiflab.engine.protocol.StandardProtocol;

 /** 
 * This class represents a Serializable version of the ProtocolManager and its state (including serialized Protocols)
 * The state stored in objects of this class can be restored with the importSettings(SerializedProtocolManager) method in ProtocolManager
 * @author kjetikl
 */
public class SerializedProtocolManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public String currentProtocolName=null;
    public ArrayList<SerializedStandardProtocol> protocolList;
    public HashMap<String,Integer> caretposition; 
    
    public SerializedProtocolManager(ProtocolManager manager) {
        protocolList=new ArrayList<SerializedStandardProtocol>();
        caretposition=new HashMap<String, Integer>();
        currentProtocolName=manager.currentProtocolName;
        for (int i=0;i<manager.size();i++) {
            StandardProtocol prot=(StandardProtocol)manager.get(i);
            String protocolName=prot.getName();
            caretposition.put(protocolName,manager.caretposition.get(prot));
            protocolList.add(prot.getSerializedProtocol());
        }
    }
}    

