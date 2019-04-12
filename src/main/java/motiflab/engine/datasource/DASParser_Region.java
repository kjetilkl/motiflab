/*
 
 
 */

package motiflab.engine.datasource;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import motiflab.engine.data.Region;
import motiflab.engine.data.Sequence;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
    
    public ArrayList<Region> parse(String uri, int timeout) throws Exception {
        //System.err.println("Parsing uri:"+uri);
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        regionlist=new ArrayList<Region>();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(timeout);
        InputStream inputStream = connection.getInputStream();
        saxParser.parse(inputStream, handler);
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
