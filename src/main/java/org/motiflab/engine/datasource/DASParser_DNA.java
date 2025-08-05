/*
 
 
 */

package org.motiflab.engine.datasource;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.motiflab.engine.util.FilterPatternInputStream;

/**
 *
 * @author kjetikl
 */
public class DASParser_DNA {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    StringBuilder buffer=null;
    String DNAstring=null;
    int buffersize=0;
    boolean inside=true; 

    // The URL to the DTD at BioDAS is not longer valid, and this will result in the saxParser throwing an error.
    // To avoid this problem, we use a FilterPatternInputStream to remove this element from the inputStream before passing it on to the parser
    private final String filterString="<!DOCTYPE DASDNA SYSTEM \"http://www.biodas.org/dtd/dasdna.dtd\">"; 
    
    public char[] parse(String uri, int timeout) throws Exception {
        //System.err.println("Parsing uri:"+uri);
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
        int status = ((HttpURLConnection)connection).getResponseCode();
        String location = ((HttpURLConnection)connection).getHeaderField("Location");
        if (status>300 && status<400 && location!=null) {
                ((HttpURLConnection)connection).disconnect();
                return parse(location, timeout);
        }          
        InputStream inputStream = connection.getInputStream();
        FilterPatternInputStream filterStream = new FilterPatternInputStream(inputStream, filterString);
        saxParser.parse(filterStream, handler);
        if (DNAstring==null) throw new SAXException("No DNA sequence returned");
        if (DNAstring.length()!=buffersize) throw new SAXException("Length of obtained DNA sequence ("+DNAstring.length()+" bp) does not match expected sequence segment size ("+buffersize+" bp)");
        buffersize=0;        
        return DNAstring.toCharArray();
    } 


    
    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("SEQUENCE")) { // previously: if (qName.equals("DNA"))
                String start=attributes.getValue("start");
                String stop=attributes.getValue("stop");
                if (start!=null && !start.isEmpty() && stop!=null && !stop.isEmpty()) {
                    try {
                        buffersize=Integer.parseInt(stop)-Integer.parseInt(start)+1;
                        buffer=new StringBuilder(buffersize);
                        inside=true;
                    } catch (NumberFormatException e) {throw new SAXException("Unable to parse start/stop position for DNA sequence: "+start+"-"+stop);}
                }
            } else if (qName.equals("DNA") && buffersize==0) { // Has inner <DNA> tag. Check if buffer has been created yet
                String stringVal=attributes.getValue("length");
                try {
                    buffersize=Integer.parseInt(stringVal);
                    buffer=new StringBuilder(buffersize);
                    inside=true;                    
                } catch (NumberFormatException e) {throw new SAXException("Unable to parse expected integer for DNA buffer length. Value="+stringVal);}                
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("SEQUENCE")) { // previously: if (qName.equals("DNA"))
                inside=false;
                DNAstring=buffer.toString();
                DNAstring=DNAstring.replaceAll("\\s+", ""); // remove all whitespace
            }
        }


        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inside && buffer!=null) {
                for (int i=start;i<start+length;i++) ch[i]=Character.toUpperCase(ch[i]); // convert to uppercase
                buffer.append(ch, start, length);
            }       
        }
        
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            super.fatalError(e);
        }
        
    } // end internal class ElementParser
    
    
}
