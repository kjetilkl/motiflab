/*
 
 
 */

package motiflab.engine.datasource;

import java.util.ArrayList;
import java.util.Date;



/**
 *
 * @author kjetikl
 */
public class Server implements Cloneable {
    private String serveraddress;
    private int delay=0; // a number of milliseconds that a client must wait between successive requests to this server
    private Date lastAccessed=null; // The "last time" the server was contacted. Note that this time could be in the future if the server has "pending requests" that are still waiting to be scheduled
    private ArrayList<String> mirrors=null;
    private int maxSequenceLength=0; // the maximum sequence region that can be downloaded with one request from this server (a value of 0 means no limit) 
    
    public Server(String serveraddress, int serverdelay) {
        this.serveraddress=serveraddress;
        this.delay=serverdelay;
    }
    
    public Server(String serveraddress, int serverdelay, int maxspan) {
        this.serveraddress=serveraddress;
        this.delay=serverdelay;
        this.maxSequenceLength=maxspan;
    }    
    
    public String getServerAddress() {
        return serveraddress;
    }
    
    /** 
     * Returns the number of milliseconds one should wait between each successive access to this server 
     * in order to not bog down the server with too many requests at once
     */
    public int getServerDelay() {
        return delay;
    }
    
    /** Returns a timestamp for when this server was last accessed */
    public Date getTimeOfLastAccess() {
        return lastAccessed;
    }
    
    /** Sets the time of last access for this server  */
    public void setTimeOfLastAccess(Date time) {
        this.lastAccessed=time;
    }
    
    /** Returns the maximum sequence region that can be downloaded in one single request.
     *  A value of 0 means "no limit"
     */
    public int getMaxSequenceLength() {
        return maxSequenceLength;
    }
    
    /** Sets the maximum sequence region that can be downloaded in one single request.
     *  A value of 0 means "no limit"
     */
    public void setMaxSequenceLength(int span) {
        this.maxSequenceLength=span;
    }    
    
    /** 
     * Adds a mirror for this server. The mirror should have the exact same directory structure as
     * the original for relevant files (i.e. the paths to relevant files should be the same for the
     * mirror and the original except for the name of the server itself (and maybe port number)
     */
    public void addMirror(String serveraddress) {
        if (mirrors==null) mirrors=new ArrayList<String>();
        if (!mirrors.contains(serveraddress)) mirrors.add(serveraddress);
    }
    
    /** Returns the server addresses of all mirrors sites for this server */
    public ArrayList<String> getMirrorSites() {
        return mirrors;
    }

     /** Returns an XML element for this DataTrack that can be included in DataConfiguration XML-documents */
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = document.createElement("Server");
        element.setAttribute("address", serveraddress);
        element.setAttribute("delay", ""+delay);
        element.setAttribute("maxspan", ""+maxSequenceLength);        
        if (mirrors!=null) {
          for (String mirror:mirrors) {
            org.w3c.dom.Element mirrorelement = document.createElement("Mirror");    
            mirrorelement.appendChild(document.createTextNode(mirror));
            element.appendChild(mirrorelement);
          }
        }
        return element;
    }      

    @Override
    public Server clone() {
        Server copy=new Server(this.serveraddress, this.delay);
        copy.maxSequenceLength=this.maxSequenceLength;
        if (this.mirrors==null) copy.mirrors=null;
        else {
            copy.mirrors=new ArrayList<String>(mirrors.size());
            for (String mirror:this.mirrors) copy.mirrors.add(mirror);
        }
        return copy;
    }    
    
}
