/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motiflab.engine.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.data.DataSegment;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.task.ExecutableTask;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author kjetikl
 */
public class DataSource_http_GETTest {
    
    public DataSource_http_GETTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

//    /**
//     * Test of getTemplateInstance method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetTemplateInstance() {
//        System.out.println("getTemplateInstance");
//        DataSource_http_GET expResult = null;
//        DataSource_http_GET result = DataSource_http_GET.getTemplateInstance();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSupportedData method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetSupportedData() {
//        System.out.println("getSupportedData");
//        DataSource_http_GET instance = null;
//        Class[] expResult = null;
//        Class[] result = instance.getSupportedData();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of initializeDataSourceFromMap method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testInitializeDataSourceFromMap() throws Exception {
//        System.out.println("initializeDataSourceFromMap");
//        HashMap<String, Object> map = null;
//        DataSource_http_GET instance = null;
//        instance.initializeDataSourceFromMap(map);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getParametersAsMap method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetParametersAsMap() {
//        System.out.println("getParametersAsMap");
//        DataSource_http_GET instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.getParametersAsMap();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of equals method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testEquals() {
//        System.out.println("equals");
//        DataSource other = null;
//        DataSource_http_GET instance = null;
//        boolean expResult = false;
//        boolean result = instance.equals(other);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reportFirstDifference method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testReportFirstDifference() {
//        System.out.println("reportFirstDifference");
//        DataSource other = null;
//        DataSource_http_GET instance = null;
//        String expResult = "";
//        String result = instance.reportFirstDifference(other);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getServerAddress method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetServerAddress() {
//        System.out.println("getServerAddress");
//        DataSource_http_GET instance = null;
//        String expResult = "";
//        String result = instance.getServerAddress();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of setServerAddress method, of class DataSource_http_GET.
     */
    @Test
    public void testSetServerAddress() {
        System.out.println("setServerAddress");
        DataSource_http_GET instance;
        
        // test #1a: 
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org", "test", "");
        instance.setServerAddress("newserver.com");
        assertEquals("http://newserver.com", instance.getBaseURL());
        
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/", "test", "");
        instance.setServerAddress("newserver.com");
        assertEquals("http://newserver.com/", instance.getBaseURL());        
        
        // test #1b: 
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("newserver.com");
        assertEquals("http://newserver.com/database", instance.getBaseURL());        
        
        // test #2a: new server has different protocol
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org", "test", "");
        instance.setServerAddress("https://newserver.com");
        assertEquals("https://newserver.com", instance.getBaseURL());
        
        // test #2b: new server has different protocol
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("https://newserver.com");
        assertEquals("https://newserver.com/database", instance.getBaseURL());         
   
        
        // test #3a: new server includes a path
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org", "test", "");
        instance.setServerAddress("https://newserver.com/subfolder");
        assertEquals("https://newserver.com/subfolder", instance.getBaseURL());
        
        // test #3b: new server includes a path
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org", "test", "");
        instance.setServerAddress("https://newserver.com/subfolder/");
        assertEquals("https://newserver.com/subfolder/", instance.getBaseURL());
        
        // test #3c: new server includes a path
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("https://newserver.com/subfolder/");
        assertEquals("https://newserver.com/subfolder/database", instance.getBaseURL());     
        
        // test #3d: new server includes a path
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("https://newserver.com/subfolder");
        assertEquals("https://newserver.com/subfolder/database", instance.getBaseURL());  
        
        // test #4a: new server includes credentials and port
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("http://user:password@newserver.com:8080/subfolder");
        assertEquals("http://user:password@newserver.com:8080/subfolder/database", instance.getBaseURL());

        // test #4b: new server includes credentials and port
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("https://user:password@newserver.com:8080/subfolder");
        assertEquals("https://user:password@newserver.com:8080/subfolder/database", instance.getBaseURL());
        
        // test #4b: new server includes credentials and port
        instance = new DataSource_http_GET(null, 0, "test", "http://www.xxx.org/database", "test", "");
        instance.setServerAddress("user:password@newserver.com:8080/subfolder");
        assertEquals("http://user:password@newserver.com:8080/subfolder/database", instance.getBaseURL());         
    }
//
//    /**
//     * Test of getProtocol method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetProtocol() {
//        System.out.println("getProtocol");
//        DataSource_http_GET instance = null;
//        String expResult = "";
//        String result = instance.getProtocol();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getParameter method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetParameter() {
//        System.out.println("getParameter");
//        DataSource_http_GET instance = null;
//        String expResult = "";
//        String result = instance.getParameter();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setParameter method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testSetParameter() {
//        System.out.println("setParameter");
//        String parameter = "";
//        DataSource_http_GET instance = null;
//        instance.setParameter(parameter);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getBaseURL method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetBaseURL() {
//        System.out.println("getBaseURL");
//        DataSource_http_GET instance = null;
//        String expResult = "";
//        String result = instance.getBaseURL();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setBaseURL method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testSetBaseURL() {
//        System.out.println("setBaseURL");
//        String baseURL = "";
//        DataSource_http_GET instance = null;
//        instance.setBaseURL(baseURL);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of useCache method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testUseCache() {
//        System.out.println("useCache");
//        DataSource_http_GET instance = null;
//        boolean expResult = false;
//        boolean result = instance.useCache();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of usesStandardDataFormat method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testUsesStandardDataFormat() {
//        System.out.println("usesStandardDataFormat");
//        DataSource_http_GET instance = null;
//        boolean expResult = false;
//        boolean result = instance.usesStandardDataFormat();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of filterProtocolSupportedDataFormats method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testFilterProtocolSupportedDataFormats() {
//        System.out.println("filterProtocolSupportedDataFormats");
//        ArrayList<DataFormat> list = null;
//        DataSource_http_GET instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.filterProtocolSupportedDataFormats(list);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadDataSegment method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testLoadDataSegment() throws Exception {
//        System.out.println("loadDataSegment");
//        DataSegment segment = null;
//        ExecutableTask task = null;
//        DataSource_http_GET instance = null;
//        DataSegment expResult = null;
//        DataSegment result = instance.loadDataSegment(segment, task);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clone method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testClone() {
//        System.out.println("clone");
//        DataSource_http_GET instance = null;
//        DataSource expResult = null;
//        DataSource result = instance.clone();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getXMLrepresentation method, of class DataSource_http_GET.
//     */
//    @Test
//    public void testGetXMLrepresentation() {
//        System.out.println("getXMLrepresentation");
//        Document document = null;
//        DataSource_http_GET instance = null;
//        Element expResult = null;
//        Element result = instance.getXMLrepresentation(document);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
