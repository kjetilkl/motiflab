/*
 
 
 */

package motiflab.engine.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import motiflab.engine.ImportantNotification;

/**
 *
 * @author kjetikl
 */
public class ImportantNotificationParser {
    private SAXParserFactory factory;
    private SAXParser saxParser;
    private ElementParser handler;
    private ArrayList<ImportantNotification> notifications;
    private int bufferpos=0;    
    private StringBuilder buffer=null;
    private int minLevel=0;
    private int minIndex=0;
    
    private Date date=null;
    private int level=ImportantNotification.MINOR;
    private int notificationIndex=-1;
    
    
    public ArrayList<ImportantNotification> parseNotifications(String uri, int minLevel, int minIndex) throws Exception {
        //System.err.println("Parsing uri:"+uri);
        this.minLevel=minLevel;
        this.minIndex=minIndex;
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        notifications=new ArrayList<>();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(10000); // 10 seconds
        int status = ((HttpURLConnection)connection).getResponseCode();
        String location = ((HttpURLConnection)connection).getHeaderField("Location");
        if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                ((HttpURLConnection)connection).disconnect();
                return parseNotifications(location, minLevel, minIndex);
        }        
        InputStream inputStream = connection.getInputStream();
        try {
            saxParser.parse(inputStream, handler);
        } catch (SAXException e) {
            String error=e.getMessage();
            if (error==null || !error.equals("FINISHED")) throw e;
        }
        return notifications;
    } 
    
    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("notification")) {
                buffer=null;
                date=null;
                notificationIndex=0;
                level=ImportantNotification.MINOR;
                String dateString=attributes.getValue("date");
                if (dateString!=null) {
                    SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                    try {
                       date=formatter.parse(dateString);
                    } catch (Exception e) {
                       throw new SAXException("MotifLab Server Error: Unable to parse datestring in notification:"+dateString);
                    }                    
                }
                level=ImportantNotification.getLevelFromString(attributes.getValue("level"));
                String indexString=attributes.getValue("index");
                if (indexString==null || indexString.isEmpty()) throw new SAXException("MotifLab Server Error: New MotifLab notification is missing index number");
                try {
                    notificationIndex=Integer.parseInt(indexString);
                } catch (NumberFormatException e) {
                    throw new SAXException("MotifLab Server Error: Unable to parse index number in notification:"+dateString);
                }                
            }     
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("notification")) {
                if (notificationIndex>=minIndex && level>=minLevel) {
                    String message=(buffer!=null)?buffer.toString():"";
                    // unescape html
                    message=message.replace("&lt;", "<");
                    message=message.replace("&gt;", ">");
                    message=message.replace("&amp;", "&");
                    ImportantNotification newmessage=new ImportantNotification(notificationIndex,message, date, level);                                    
                    notifications.add(newmessage);
                    
                }                
                // assuming that notifications are ordered decreasingly by notificationIndex, we can
                // abort if the encountered index is smaller than the limit
                if (notificationIndex<minIndex) throw new SAXException("FINISHED");
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