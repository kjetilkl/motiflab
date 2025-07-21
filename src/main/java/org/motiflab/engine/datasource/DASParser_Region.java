/*
 
 
 */

package org.motiflab.engine.datasource;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Sequence;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.motiflab.engine.util.FilterPatternInputStream;

/**
 *
 * @author kjetikl
 */
public class DASParser_Region {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    //char[] buffer=null;
    ArrayList<Region> regionlist;
    int bufferpos=0;    
    StringBuilder buffer=null;
    int start=Integer.MIN_VALUE;
    int end=Integer.MIN_VALUE;
    int orientation=Sequence.DIRECT;
    double score=-Double.MAX_VALUE;
    String label=null;
    
    // The URL to the DTD at BioDAS is not longer valid, and this will result in the saxParser throwing an error.
    // To avoid this problem, we use a FilterPatternInputStream to remove this element from the inputStream before passing it on to the parser
    private final String filterString="<!DOCTYPE DASGFF SYSTEM \"http://www.biodas.org/dtd/dasgff.dtd\">";     
    
    public ArrayList<Region> parse(String uri, int timeout) throws Exception {
        //System.err.println("Parsing uri:"+uri);
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        regionlist = new ArrayList<Region>();
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
        int status = ((HttpURLConnection)connection).getResponseCode();
        String location = ((HttpURLConnection)connection).getHeaderField("Location");
        if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                ((HttpURLConnection)connection).disconnect();
                return parse(location, timeout);
        }         
        InputStream inputStream = connection.getInputStream();
        FilterPatternInputStream filterStream = new FilterPatternInputStream(inputStream, filterString);
        saxParser.parse(filterStream, handler);        
        return regionlist;
    } 
    
    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("FEATURE")) {
                start=Integer.MIN_VALUE;
                end=Integer.MIN_VALUE;
                orientation=Sequence.DIRECT;
                score=-Double.MAX_VALUE;
                label=attributes.getValue("label");
            }     
            buffer=null;
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (buffer==null) return;
            String text=buffer.toString();
            if (qName.equalsIgnoreCase("START")) {
                try {start=Integer.parseInt(text);} catch (NumberFormatException e) {throw new SAXException("Unable to parse expected numeric value = "+text);}
            } else if (qName.equalsIgnoreCase("END")) {
                try {end=Integer.parseInt(text);} catch (NumberFormatException e) {throw new SAXException("Unable to parse expected numeric value = "+text);}
            } else if (qName.equalsIgnoreCase("SCORE")) {
                if (text.equals("-")) score=0;
                else try {score=Double.parseDouble(text);} catch (NumberFormatException e) {throw new SAXException("Unable to parse expected numeric value = "+text);}
            } else if (qName.equalsIgnoreCase("ORIENTATION")) {
                if (text.equals("+")) orientation=Region.DIRECT;
                else if (text.equals("-")) orientation=Region.REVERSE;
                else orientation=Region.INDETERMINED;
            } else if (qName.equalsIgnoreCase("FEATURE")) {
                regionlist.add(new Region(null, start, end, label, score, orientation));            
            } 
        }


        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer==null) buffer=new StringBuilder();
            buffer.append(ch, start, length);
        }
        
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            super.fatalError(e);
        }
        
    } // end internal class ElementParser
    
    
}
