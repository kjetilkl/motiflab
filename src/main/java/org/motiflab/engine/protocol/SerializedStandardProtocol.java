/*
 
 
 */

package org.motiflab.engine.protocol;

import java.io.Serializable;
import javax.swing.text.PlainDocument;
import org.motiflab.engine.MotifLabEngine;

/**
 *
 * @author kjetikl
 */
 public class SerializedStandardProtocol implements Serializable {
        public String name=null;
        public String text=null;
        public boolean isDirtyFlag=false;
        public String savedFileName=null;  
        
        public SerializedStandardProtocol(StandardProtocol protocol) {
            this.name=protocol.getName();
            PlainDocument document=(PlainDocument)protocol.getDocument();
            try {
                this.text=document.getText(0, document.getLength());
            } catch (Exception e) {}
            this.isDirtyFlag=protocol.isDirty();
            this.savedFileName=protocol.getFileName();           
         }
        
        public StandardProtocol getProtocol(MotifLabEngine engine) {
            StandardProtocol protocol=new StandardProtocol(engine, text);
            protocol.setDirtyFlag(isDirtyFlag);
            protocol.setName(name);
            protocol.setFileName(savedFileName);
            return protocol;
        }
        
        private static final long serialVersionUID = 7400309437025483160L;        
}
