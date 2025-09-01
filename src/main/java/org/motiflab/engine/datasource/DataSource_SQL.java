package org.motiflab.engine.datasource;

import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.protocol.ParseError;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The DataSource_SQL class serves data obtained from an SQL server
 * 
 * @author kjetikl
 */
public class DataSource_SQL extends DataSource {
    
    public static final String PROTOCOL_NAME="SQL";    
    
    private String serverAddress=null;
    private int port=-1; // 
    private String databasename=null;
    private String username=null;
    private String password=null; // the password is stored here uncrypted
    private String tablename=null;
    private ArrayList<DBfield> fields=null;
    
    private transient String query=null;
    private transient HashMap<String,DBfield> fieldmap=null; // maps standard properties to db fields for easier access
        
    public DataSource_SQL(DataTrack datatrack, int organism, String genomebuild) {
        super(datatrack,organism, genomebuild, null);
    }    
    
    public DataSource_SQL(DataTrack datatrack, int organism, String genomebuild, String serverAddress,int port, String databasename, String tablename, String username, String password, ArrayList<DBfield> fields) {
        super(datatrack,organism, genomebuild, null);
        this.serverAddress=serverAddress;
        this.databasename=databasename;
        this.username=username;        
        this.password=password;
        this.tablename=tablename;
        this.port=port;  
        setDBfields(fields);
    }      

    private DataSource_SQL() {}
    
    public static DataSource_SQL getTemplateInstance() {
        return new DataSource_SQL();
    }      
    
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        Connection connection = null;
        Properties props = new Properties();
        props.put("user", this.username);
        props.put("password", this.password);
        String portString=(port<0)?"":(":"+port);
        connection = DriverManager.getConnection("jdbc:mysql://"+serverAddress+portString+"/"+databasename,props);
        // for DERBY: connection = DriverManager.getConnection("jdbc:derby:" +this.dbName +";create=true",connectionProps);
        return connection;
    }    
    
    @Override
    public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {
        if (!map.containsKey("Server")) throw new SystemError("Missing parameter: Server");     
        this.serverAddress=map.get("Server").toString();
        if (!map.containsKey("Databasename")) throw new SystemError("Missing parameter: Databasename");     
        this.username=map.get("Databasename").toString();        
        if (!map.containsKey("Username")) throw new SystemError("Missing parameter: Username");     
        this.username=map.get("Username").toString();
        if (!map.containsKey("Password")) throw new SystemError("Missing parameter: Password");     
        this.password=map.get("Password").toString();   
        if (!map.containsKey("Table")) throw new SystemError("Missing parameter: Table");     
        this.tablename=map.get("Table").toString();    
        if (map.containsKey("Port")) {
            Object portstring=map.get("Port");
            if (portstring instanceof Number) port=((Number)portstring).intValue();
            else {
                try {
                    port=Integer.parseInt(portstring.toString());
                } catch (NumberFormatException e) {throw new SystemError("Expected integer value for parameter 'Port'. Got '"+portstring+"'");}
            }
        } else port=-1;      
    }  
    
    @Override
    public HashMap<String,Object> getParametersAsMap() {
        HashMap<String,Object> map=new HashMap<String, Object>();
        map.put("Database", serverAddress);
        map.put("Databasename", databasename);        
        map.put("Username", username);
        map.put("Password", password);
        map.put("Table", tablename);    
        if (port>=0) map.put("Port", port);    
        return map;
    }
    
      
    @Override
    public DataSource clone() {
        DataSource_SQL copy=new DataSource_SQL(dataTrack, organism, genomebuild);
        copy.delay=this.delay;
        copy.serverAddress=this.serverAddress;
        copy.databasename=this.databasename;
        copy.username=this.username;        
        copy.password=this.password;
        copy.tablename=this.tablename;
        copy.port=this.port;
        ArrayList<DBfield> fieldscopy=null;
        if (this.fields!=null) {
            fieldscopy=new ArrayList<DBfield>();
            for (DBfield field:fields){
                DBfield copyfield=(DBfield)field.clone();
                fieldscopy.add(copyfield);
            }
        }
        copy.setDBfields(fieldscopy);
        return copy;
    } 

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public Class[] getSupportedData() {
        return new Class[]{RegionDataset.class};
    }      
       
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataSource_SQL other = (DataSource_SQL) obj;
        if (this.port!=other.port) return false;
        if ((this.serverAddress == null) ? (other.serverAddress != null) : !this.serverAddress.equals(other.serverAddress)) {
            return false;
        }
        if ((this.username == null) ? (other.username != null) : !this.username.equals(other.username)) {
            return false;
        }
        if ((this.databasename == null) ? (other.databasename != null) : !this.databasename.equals(other.databasename)) {
            return false;
        }        
        if ((this.password == null) ? (other.password != null) : !this.password.equals(other.password)) {
            return false;
        }
        if ((this.tablename == null) ? (other.tablename != null) : !this.tablename.equals(other.tablename)) {
            return false;
        }
        if (this.fields != other.fields && (this.fields == null || !this.fields.equals(other.fields))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.serverAddress != null ? this.serverAddress.hashCode() : 0);
        hash = 53 * hash + this.port;
        hash = 53 * hash + (this.username != null ? this.username.hashCode() : 0);
        hash = 53 * hash + (this.databasename != null ? this.databasename.hashCode() : 0);        
        hash = 53 * hash + (this.password != null ? this.password.hashCode() : 0);
        hash = 53 * hash + (this.tablename != null ? this.tablename.hashCode() : 0);
        hash = 53 * hash + (this.fields != null ? this.fields.hashCode() : 0);
        return hash;
    }

    

    @Override
    public String getServerAddress() {
        return serverAddress;
    }
    
    @Override
    public boolean setServerAddress(String address) {
        serverAddress=address;
        return true;
    }    
    
    public String getUsername() {
        return username;
    }
    public void setUsername(String user) {
        this.username=user;
    }
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String pass) {
        this.password=pass;
    }   
    public String getDatabaseName() {
        return databasename;
    }
    public void setDatabaseName(String name) {
        this.databasename=name;
    }      
    public String getTablename() {
        return tablename;
    }
    public void setTablename(String name) {
        this.tablename=name;
    }  
    public int getPortNumber() {
        return port;
    }
    public void setPortNumber(int portnumber) {
        this.port=portnumber;
    }
    
    @Override
    public boolean useCache() {
        return true;
    }    
    
    @Override
    public boolean usesStandardDataFormat() {
        return false;
    }      
    
    public String getQuery() throws ExecutionError {
        String chrField=getDBfieldForRegionProperty("chromosome");        
        String startField=getDBfieldForRegionProperty("start");
        String endField=getDBfieldForRegionProperty("end"); 
        
        StringBuilder builder=new StringBuilder();
        builder.append("SELECT ");
        for (int i=0;i<fields.size();i++) {
            DBfield field=fields.get(i);
            if (!field.hasExplicitValue()) {
                if (i>0) builder.append(", ");
                builder.append(field.getDBfieldName());
            }
        }
        builder.append(" FROM ");
        builder.append(tablename);      
        builder.append(" WHERE ");
        builder.append(chrField);
        builder.append("=? and not (");
        builder.append(endField);
        builder.append("<? or ");
        builder.append(startField);
        builder.append(">?)");
        return builder.toString();
    }

    public ArrayList<DBfield> getDBfields() {
        return fields;
    }
    
    public final void setDBfields(ArrayList<DBfield> fields) {
        this.fields=fields;
        fieldmap=new HashMap<String, DBfield>();
        if (fields!=null) {
            for (DBfield field:fields) {
                fieldmap.put(field.getPropertyName(),field);
            }         
        }
    }
    
    public void initializeSourceFromXML(Element protocol) throws Exception {
         boolean foundChr=false,foundStart=false,foundEnd=false,foundType=false;
         NodeList allDB = protocol.getElementsByTagName("Database");
         if (allDB.getLength()==0) throw new ParseError("Missing 'Database' element");
         Element dbElement = (Element)allDB.item(0);           
         serverAddress=dbElement.getAttribute("server");  
         if (serverAddress==null || serverAddress.isEmpty())  throw new ParseError("Missing 'server' attribute for 'Database' element");
         databasename=dbElement.getAttribute("dbname");  
         if (databasename==null || databasename.isEmpty())  throw new ParseError("Missing 'dbname' attribute for 'Database' element");
         String portString=dbElement.getAttribute("port");
         if (portString!=null && !portString.isEmpty()) {
             try {
                 port=Integer.parseInt(portString);
                 if (port<0) throw new ParseError("Attribute 'port' should be a positive number for 'Database' element");
             } catch (NumberFormatException ex) {
                 throw new ParseError("Unable to parse expected integer number for the 'port' attribute for 'Database' element. Got '"+portString+"'"); 
             }
         }        
         NodeList authElements = protocol.getElementsByTagName("Authentication");
         if (authElements.getLength()==0) throw new ParseError("Missing 'Authentication' element");
         Element authElement = (Element)authElements.item(0);           
         username=authElement.getAttribute("username");  
         if (username==null || username.isEmpty())  throw new ParseError("Missing 'username' attribute for 'Authentication' element");        
         password=authElement.getAttribute("password");  
         if (password==null || password.isEmpty())  throw new ParseError("Missing 'password' attribute for 'Authentication' element");
         String encrypted=authElement.getAttribute("encrypted"); 
         if (encrypted!=null && (encrypted.equalsIgnoreCase("true") || encrypted.equalsIgnoreCase("yes"))) {
             password=decrypt(password);
         }      
         NodeList allTables = protocol.getElementsByTagName("Table");
         if (allTables.getLength()==0) throw new ParseError("Missing 'Table' element");
         Element tableElement = (Element)allTables.item(0);           
         String tableName=tableElement.getAttribute("name");
         if (tableName==null || tableName.isEmpty()) throw new ParseError("Missing 'name' attribute for 'Table' element");
         else tablename=tableName;
         NodeList tablelist=tableElement.getChildNodes();
         for (int p=0;p<tablelist.getLength();p++) {
             Node param = (Node)tablelist.item(p);
             if (param instanceof Element) {
                 String paramName=((Element)param).getTagName();
                 if (paramName.equalsIgnoreCase("Field")) {
                     DBfield newfield=DBfield.initializeFieldFromXML((Element)param);
                     if (fields==null) fields=new ArrayList<DBfield>();
                     fields.add(newfield);
                     if (newfield.getPropertyName().equalsIgnoreCase("chromosome")) foundChr=true;
                     if (newfield.getPropertyName().equalsIgnoreCase("start") ) foundStart=true;                     
                     if (newfield.getPropertyName().equalsIgnoreCase("end") ) foundEnd=true;
                     if (newfield.getPropertyName().equalsIgnoreCase("type") ) foundType=true;
                 }
             }
         }
         if (!foundChr) throw new ParseError("Missing field specification for region property: chromosome");
         if (!foundStart) throw new ParseError("Missing field specification for region property: start");
         if (!foundEnd) throw new ParseError("Missing field specification for region property: end");
         // if (!foundType) throw new ParseError("Missing field specification for region property: type");
         fieldmap=new HashMap<String, DBfield>();
         for (DBfield field:fields) {
            fieldmap.put(field.getPropertyName(),field);
         }         
    }
    
    @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", PROTOCOL_NAME);
        
        org.w3c.dom.Element dbElement=document.createElement("Database");
        dbElement.setAttribute("server",serverAddress);
        if (port>=0) dbElement.setAttribute("port",""+port);
        dbElement.setAttribute("dbname",databasename);        
     
        org.w3c.dom.Element authenticationElement=document.createElement("Authentication");
        authenticationElement.setAttribute("username", username);
        authenticationElement.setAttribute("password", encrypt(password)); // always use encryption when exporting
        authenticationElement.setAttribute("encrypted", "true");
        org.w3c.dom.Element tableElement=document.createElement("Table");
        tableElement.setAttribute("name",tablename);        
                
        if (fields!=null) {
            for (DBfield f:fields) {
                org.w3c.dom.Element fieldEl=f.getXMLrepresentation(document);
                tableElement.appendChild(fieldEl);
            }
            protocol.appendChild(tableElement);
        }
        protocol.appendChild(dbElement);
        protocol.appendChild(authenticationElement);      
        protocol.appendChild(tableElement);          
        element.appendChild(protocol);
        return element;
    }     
    
    
    @Override
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {
        Class type=dataTrack.getDataType();
        String chromosome=segment.getChromosome();
        if (chromosome.equals("?")) throw new ExecutionError("Unknown chromosome");
        if (type==RegionDataset.class) getRegionData(segment);
        else throw new ExecutionError("Unsupported datatype '"+type+"' for SQL server");
        return segment;
    } 
     
   
    private void getRegionData(DataSegment segment) throws ExecutionError {
        String segmentChromosome=segment.getChromosome();
        int segmentStart=segment.getSegmentStart();
        int segmentEnd=segment.getSegmentEnd();  
        segmentChromosome=(String)(fieldmap.get("chromosome").getTransformedValue(segmentChromosome)); // adds 'chr' prefix if specified in configuration (or uses transform map)
        if (query==null) query=getQuery();
        Connection connection=null;
        PreparedStatement statement=null;
        ResultSet rs=null;
        try {
            connection=getConnection();
            statement = connection.prepareStatement(query);          
            statement.setString(1,segmentChromosome);            
            statement.setInt(2,segmentStart);            
            statement.setInt(3,segmentEnd);    
            rs = statement.executeQuery();
            while (rs.next()) {     
                addRegionToTarget(segment, rs);
            }
        } catch (Exception e ) {
            throw new ExecutionError((e==null)?"NullPointerException":e.toString(),e);
        } finally {            
            try {
               if (rs != null) rs.close();               
               if (statement != null) statement.close();
               if (connection != null) connection.close();
            } 
            catch (Exception ce){}          
        }        
    }
    
    private void addRegionToTarget(DataSegment segment, ResultSet rs) throws SQLException, ExecutionError {        
        Region newRegion=new Region(null, 0, 0, "unknown", 0, Region.INDETERMINED); // these are defaults which will be updated!
        for (DBfield field:fields) {
            String property=field.getPropertyName();
            if (property.equals("chromosome")) continue; // this property should not be set in the Region
            String dbfieldname=field.getDBfieldName();
            Object value=null;
            if (field.hasExplicitValue()) value=field.getExplicitValue();
            else value=rs.getObject(dbfieldname);
            if (value==null) continue;
            if (value instanceof Float) value=new Double((Float)value);
            if (!(value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Boolean)) value=value.toString(); // convert other types to text        
            value=field.getTransformedValue(value); // apply transformations
            // System.err.println("["+property+"] Value="+value+" ("+((original!=null)?original.getClass():"null")+")  transformed="+value);
            newRegion.setProperty(property, value);
        }               
        // offset region start/end relative to start of segment        
        int targetStart=targetStart=segment.getSegmentStart();
        newRegion.setRelativeStart(newRegion.getRelativeStart()-targetStart); 
        newRegion.setRelativeEnd(newRegion.getRelativeEnd()-targetStart);         
        segment.addRegion(newRegion);     
    }    
    
    public String getDBfieldForRegionProperty(String regionProperty) throws ExecutionError {
        if (fields==null) throw new ExecutionError("No fields registered in SQL data source");
        for (DBfield field:fields) {
            if (field.getPropertyName().equalsIgnoreCase(regionProperty)) return field.getDBfieldName();
        }
        throw new ExecutionError("No registered field found for property: "+regionProperty);
    }    
    
    
    @Override
    public void debug() {
        super.debug();
        System.err.println("   SQL ["+serverAddress+":"+port+"] ("+username+":"+password+") database="+databasename+"   table="+tablename); 
        if (fields!=null) {
            for (DBfield field:fields) {
                System.err.println("         Field:"+field);
            }
        }
    }  
    

// ------------------ ENCRYPTION --------------------------------
    
    private static final String FORMAT = "ISO-8859-1";
    private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private KeySpec ks=null;
    private SecretKeyFactory skf;
    private Cipher cipher;
    SecretKey key;

    private void updateKey() throws Exception {
        String myEncryptionKey = "4A144BEBF7E5E7B7DCF26491AE79C54C768C514CF1547D23";
        ks = new DESedeKeySpec(myEncryptionKey.getBytes(FORMAT));
        skf = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
        cipher = Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
        key = skf.generateSecret(ks);
    }

    private String encrypt(String unencryptedString) {
       try {          
          if (ks==null) updateKey();
          String encryptedString = null;
          cipher.init(Cipher.ENCRYPT_MODE, key);
          byte[] plainText = unencryptedString.getBytes(FORMAT);
          byte[] encryptedText = cipher.doFinal(plainText);
          encryptedString = Base64.getEncoder().encodeToString(encryptedText);
          return encryptedString;
       } catch (Exception e) {
           e.printStackTrace(System.err);
           return "*** ERROR ***";
       }          
    }

    private String decrypt(String encryptedString) {
       try {  
          if (ks==null) updateKey();
          String decryptedText = null;
          cipher.init(Cipher.DECRYPT_MODE, key);
          byte[] encryptedText = Base64.getDecoder().decode(encryptedString);
          byte[] plainText = cipher.doFinal(encryptedText);
          decryptedText = new String(plainText);
          return decryptedText;       
       } catch (Exception e) {
           e.printStackTrace(System.err);
           return "*** ERROR ***";
       }
    }

//    private static String getSpecialCharacter(int code) {
//        Charset charSet = Charset.forName(FORMAT);
//        String specialCharacter = new String(new byte[] { (byte) code }, charSet);
//        specialCharacter = String.format("%s", specialCharacter);
//        return specialCharacter;
//    }    
    
    
    
}
