/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motiflab.engine;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.text.Document;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.Data;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.OutputDataDependency;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.analysis.Analysis;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.datasource.DataLoader;
import motiflab.engine.datasource.DataRepository;
import motiflab.engine.operations.Operation;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.ProtocolTask;
import motiflab.engine.util.MotifComparator;
import motiflab.external.ExternalProgram;
import motiflab.gui.SimpleDataPanelIcon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kjetikl
 */
public class MotifLabEngineTest {
    
    public MotifLabEngineTest() {
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

    /**
     * Test of getEngine method, of class MotifLabEngine.
     */
    @Test
    public void testGetEngine() {
        System.out.println("getEngine");
        Object result1 = MotifLabEngine.getEngine();
        Object result2 = MotifLabEngine.getEngine();
        assertTrue(result1!=null && result1 instanceof MotifLabEngine);
        assertTrue(result1==result2); // getEngine should return the same singleton object
    }

//    /**
//     * Test of initialize method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInitialize() {
//        System.out.println("initialize");
//        MotifLabEngine instance = null;
//        instance.initialize();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getConfigurations method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetConfigurations() {
//        System.out.println("getConfigurations");
//        MotifLabEngine instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.getConfigurations();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setConfigurations method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetConfigurations() throws Exception {
//        System.out.println("setConfigurations");
//        HashMap<String, Object> settings = null;
//        MotifLabEngine instance = null;
//        instance.setConfigurations(settings);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setPluginConfigurations method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetPluginConfigurations() throws Exception {
//        System.out.println("setPluginConfigurations");
//        HashMap settings = null;
//        MotifLabEngine instance = null;
//        instance.setPluginConfigurations(settings);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getVersion method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetVersion() {
//        System.out.println("getVersion");
//        String expResult = "";
//        String result = MotifLabEngine.getVersion();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getReleaseDate method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetReleaseDate() {
//        System.out.println("getReleaseDate");
//        Date expResult = null;
//        Date result = MotifLabEngine.getReleaseDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getReleaseDateAsString method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetReleaseDateAsString() {
//        System.out.println("getReleaseDateAsString");
//        String expResult = "";
//        String result = MotifLabEngine.getReleaseDateAsString();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of compareVersions method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCompareVersions() {
//        System.out.println("compareVersions");
//        String versionString = "";
//        int expResult = 0;
//        int result = MotifLabEngine.compareVersions(versionString);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
    /**
     * Test of compareVersions method, of class MotifLabEngine.
     */
    @Test
    public void testCompareVersions_2args() {
        System.out.println("compareVersions_2args");
        assertEquals(MotifLabEngine.compareVersions("2.0","2.0"), 0);
        assertEquals(MotifLabEngine.compareVersions("2.0","2.0.0"), 0);
        
        assertEquals(MotifLabEngine.compareVersions("3","2"), 1);       
        assertEquals(MotifLabEngine.compareVersions("3","2.0"), 1);       
        assertEquals(MotifLabEngine.compareVersions("3.0","2.0"), 1);       
        assertEquals(MotifLabEngine.compareVersions("3.0","2"), 1);       
        assertEquals(MotifLabEngine.compareVersions("2.1","2.0"), 1);       
        assertEquals(MotifLabEngine.compareVersions("2.0.1","2.0"), 1);  
        assertEquals(MotifLabEngine.compareVersions("2.0.1","2.0.0"), 1);         
        assertEquals(MotifLabEngine.compareVersions("2.0.1","2.0.0.1"), 1);   
        assertEquals(MotifLabEngine.compareVersions("2.0.-1","2.0.0"), -1);         
        assertEquals(MotifLabEngine.compareVersions("2.0.-1","2"), -1);   
        
        assertEquals(MotifLabEngine.compareVersions("2","3"), -1);       
        assertEquals(MotifLabEngine.compareVersions("2.0","3"), -1);       
        assertEquals(MotifLabEngine.compareVersions("2.0","3.0"), -1);       
        assertEquals(MotifLabEngine.compareVersions("2","3.0"), -1);       
        assertEquals(MotifLabEngine.compareVersions("2.0","2.1"), -1);       
        assertEquals(MotifLabEngine.compareVersions("2.0","2.0.1"), -1);  
        assertEquals(MotifLabEngine.compareVersions("2.0.0","2.0.1"), -1);         
        assertEquals(MotifLabEngine.compareVersions("2.0.0.1","2.0.1"), -1);   
        assertEquals(MotifLabEngine.compareVersions("2.0.0","2.0.-1"), 1);         
        assertEquals(MotifLabEngine.compareVersions("2","2.0.-1"), 1);                        
        assertEquals(MotifLabEngine.compareVersions("2.0.-1","2"), -1);  
        assertEquals(MotifLabEngine.compareVersions("2.0.-1","2.0.-2"), 1);         
        assertEquals(MotifLabEngine.compareVersions("2.0.-1","2.0.0.-7"), -1);          
    }    
//
//    /**
//     * Test of executeProtocolTask method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExecuteProtocolTask() throws Exception {
//        System.out.println("executeProtocolTask");
//        ProtocolTask task = null;
//        boolean silent = false;
//        MotifLabEngine instance = null;
//        instance.executeProtocolTask(task, silent);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of executeProtocol method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExecuteProtocol() throws Exception {
//        System.out.println("executeProtocol");
//        Protocol protocol = null;
//        boolean silent = false;
//        MotifLabEngine instance = null;
//        instance.executeProtocol(protocol, silent);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of executeTask method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExecuteTask() throws Exception {
//        System.out.println("executeTask");
//        ExecutableTask task = null;
//        MotifLabEngine instance = null;
//        instance.executeTask(task);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setClient method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetClient() {
//        System.out.println("setClient");
//        MotifLabClient client = null;
//        MotifLabEngine instance = null;
//        instance.setClient(client);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addMacro method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAddMacro() {
//        System.out.println("addMacro");
//        String macroname = "";
//        String macrodefinition = "";
//        MotifLabEngine instance = null;
//        instance.addMacro(macroname, macrodefinition);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setMacros method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetMacros() {
//        System.out.println("setMacros");
//        HashMap<String, String> macros = null;
//        MotifLabEngine instance = null;
//        instance.setMacros(macros);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isMacroDefined method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsMacroDefined() {
//        System.out.println("isMacroDefined");
//        String macroname = "";
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isMacroDefined(macroname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMacros method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMacros() {
//        System.out.println("getMacros");
//        MotifLabEngine instance = null;
//        ArrayList expResult = null;
//        ArrayList result = instance.getMacros();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMacroTerms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMacroTerms() {
//        System.out.println("getMacroTerms");
//        MotifLabEngine instance = null;
//        Set<String> expResult = null;
//        Set<String> result = instance.getMacroTerms();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clearMacros method, of class MotifLabEngine.
//     */
//    @Test
//    public void testClearMacros() {
//        System.out.println("clearMacros");
//        MotifLabEngine instance = null;
//        instance.clearMacros();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getClient method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetClient() {
//        System.out.println("getClient");
//        MotifLabEngine instance = null;
//        MotifLabClient expResult = null;
//        MotifLabClient result = instance.getClient();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addClientListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAddClientListener() {
//        System.out.println("addClientListener");
//        PropertyChangeListener listener = null;
//        MotifLabEngine instance = null;
//        instance.addClientListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeClientListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveClientListener() {
//        System.out.println("removeClientListener");
//        PropertyChangeListener listener = null;
//        MotifLabEngine instance = null;
//        instance.removeClientListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setMaxSequenceLength method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetMaxSequenceLength() {
//        System.out.println("setMaxSequenceLength");
//        int newmax = 0;
//        MotifLabEngine instance = null;
//        instance.setMaxSequenceLength(newmax);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMaxSequenceLength method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMaxSequenceLength() {
//        System.out.println("getMaxSequenceLength");
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.getMaxSequenceLength();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setConcurrentThreads method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetConcurrentThreads() {
//        System.out.println("setConcurrentThreads");
//        int threads = 0;
//        MotifLabEngine instance = null;
//        instance.setConcurrentThreads(threads);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getConcurrentThreads method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetConcurrentThreads() {
//        System.out.println("getConcurrentThreads");
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.getConcurrentThreads();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTaskRunner method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetTaskRunner() {
//        System.out.println("getTaskRunner");
//        MotifLabEngine instance = null;
//        TaskRunner expResult = null;
//        TaskRunner result = instance.getTaskRunner();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMotifLabDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMotifLabDirectory() {
//        System.out.println("getMotifLabDirectory");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getMotifLabDirectory();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginsDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginsDirectory() {
//        System.out.println("getPluginsDirectory");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getPluginsDirectory();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginDirectory() {
//        System.out.println("getPluginDirectory");
//        Plugin plugin = null;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getPluginDirectory(plugin);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setMotifLabDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetMotifLabDirectory() {
//        System.out.println("setMotifLabDirectory");
//        String motiflabDirectoryPath = "";
//        MotifLabEngine instance = null;
//        instance.setMotifLabDirectory(motiflabDirectoryPath);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTempDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetTempDirectory() {
//        System.out.println("getTempDirectory");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getTempDirectory();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNewWorkDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNewWorkDirectory() {
//        System.out.println("getNewWorkDirectory");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getNewWorkDirectory();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createTempFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateTempFile_0args() {
//        System.out.println("createTempFile");
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.createTempFile();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createTempFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateTempFile_String() {
//        System.out.println("createTempFile");
//        String workdir = "";
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.createTempFile(workdir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createTempFileWithSuffix method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateTempFileWithSuffix() {
//        System.out.println("createTempFileWithSuffix");
//        String suffix = "";
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.createTempFileWithSuffix(suffix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createTempFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateTempFile_String_String() {
//        System.out.println("createTempFile");
//        String suffix = "";
//        String workdir = "";
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.createTempFile(suffix, workdir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createSharedOutputDependency method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateSharedOutputDependency() {
//        System.out.println("createSharedOutputDependency");
//        String identifier = "";
//        String suffix = "";
//        MotifLabEngine instance = null;
//        OutputDataDependency expResult = null;
//        OutputDataDependency result = instance.createSharedOutputDependency(identifier, suffix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of shareOutputDependency method, of class MotifLabEngine.
//     */
//    @Test
//    public void testShareOutputDependency() {
//        System.out.println("shareOutputDependency");
//        OutputDataDependency dependency = null;
//        String sharedID = "";
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.shareOutputDependency(dependency, sharedID);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSharedOutputDependency method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetSharedOutputDependency() {
//        System.out.println("getSharedOutputDependency");
//        String identifier = "";
//        MotifLabEngine instance = null;
//        OutputDataDependency expResult = null;
//        OutputDataDependency result = instance.getSharedOutputDependency(identifier);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of resetOutputDataDependencyProcessedFlags method, of class MotifLabEngine.
//     */
//    @Test
//    public void testResetOutputDataDependencyProcessedFlags() {
//        System.out.println("resetOutputDataDependencyProcessedFlags");
//        MotifLabEngine instance = null;
//        instance.resetOutputDataDependencyProcessedFlags();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataItem_String() {
//        System.out.println("getDataItem");
//        String key = "";
//        MotifLabEngine instance = null;
//        Data expResult = null;
//        Data result = instance.getDataItem(key);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataItem_String_Class() {
//        System.out.println("getDataItem");
//        String key = "";
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        Data expResult = null;
//        Data result = instance.getDataItem(key, classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getClassForDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetClassForDataItem() {
//        System.out.println("getClassForDataItem");
//        String key = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getClassForDataItem(key);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of storeDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStoreDataItem() throws Exception {
//        System.out.println("storeDataItem");
//        Data dataitem = null;
//        MotifLabEngine instance = null;
//        instance.storeDataItem(dataitem);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of storeDataItem_useBackdoor method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStoreDataItem_useBackdoor() throws Exception {
//        System.out.println("storeDataItem_useBackdoor");
//        Sequence dataitem = null;
//        MotifLabEngine instance = null;
//        instance.storeDataItem_useBackdoor(dataitem);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of updateDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUpdateDataItem() throws Exception {
//        System.out.println("updateDataItem");
//        Data dataitem = null;
//        MotifLabEngine instance = null;
//        instance.updateDataItem(dataitem);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveDataItem() throws Exception {
//        System.out.println("removeDataItem");
//        String key = "";
//        MotifLabEngine instance = null;
//        Data expResult = null;
//        Data result = instance.removeDataItem(key);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of renameDataItem method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRenameDataItem() throws Exception {
//        System.out.println("renameDataItem");
//        String oldname = "";
//        String newname = "";
//        MotifLabEngine instance = null;
//        Data expResult = null;
//        Data result = instance.renameDataItem(oldname, newname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataUpdatesIsAllowed method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataUpdatesIsAllowed() {
//        System.out.println("dataUpdatesIsAllowed");
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.dataUpdatesIsAllowed();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setAllowDataUpdates method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetAllowDataUpdates() {
//        System.out.println("setAllowDataUpdates");
//        boolean allow = false;
//        MotifLabEngine instance = null;
//        instance.setAllowDataUpdates(allow);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isTemporary method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsTemporary() {
//        System.out.println("isTemporary");
//        Data item = null;
//        boolean expResult = false;
//        boolean result = MotifLabEngine.isTemporary(item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataExists method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataExists() {
//        System.out.println("dataExists");
//        String name = "";
//        Class type = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.dataExists(name, type);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeTemporaryDataItems method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveTemporaryDataItems() {
//        System.out.println("removeTemporaryDataItems");
//        MotifLabEngine instance = null;
//        instance.removeTemporaryDataItems();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clearAllData method, of class MotifLabEngine.
//     */
//    @Test
//    public void testClearAllData() {
//        System.out.println("clearAllData");
//        MotifLabEngine instance = null;
//        instance.clearAllData();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clearMotifAndModuleDataData method, of class MotifLabEngine.
//     */
//    @Test
//    public void testClearMotifAndModuleDataData() {
//        System.out.println("clearMotifAndModuleDataData");
//        boolean justModules = false;
//        MotifLabEngine instance = null;
//        instance.clearMotifAndModuleDataData(justModules);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clearAllSequences method, of class MotifLabEngine.
//     */
//    @Test
//    public void testClearAllSequences() {
//        System.out.println("clearAllSequences");
//        MotifLabEngine instance = null;
//        instance.clearAllSequences();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of executeDisplayDirectives method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExecuteDisplayDirectives() {
//        System.out.println("executeDisplayDirectives");
//        String displayDirectives = "";
//        String targetName = "";
//        MotifLabEngine instance = null;
//        instance.executeDisplayDirectives(displayDirectives, targetName);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNextAvailableDataName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNextAvailableDataName() {
//        System.out.println("getNextAvailableDataName");
//        String prefix = "";
//        int digits = 0;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getNextAvailableDataName(prefix, digits);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNextAvailableDataNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNextAvailableDataNames() {
//        System.out.println("getNextAvailableDataNames");
//        String prefix = "";
//        int digits = 0;
//        int numberofnames = 0;
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getNextAvailableDataNames(prefix, digits, numberofnames);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllDataItemsOfType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllDataItemsOfType() {
//        System.out.println("getAllDataItemsOfType");
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        ArrayList<Data> expResult = null;
//        ArrayList<Data> result = instance.getAllDataItemsOfType(classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllDataItemsOfTypeMatchingExpression method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllDataItemsOfTypeMatchingExpression() {
//        System.out.println("getAllDataItemsOfTypeMatchingExpression");
//        String expression = "";
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        ArrayList<Data> expResult = null;
//        ArrayList<Data> result = instance.getAllDataItemsOfTypeMatchingExpression(expression, classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllDataItemsOfTypeMatchingExpressionInNumericRange method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllDataItemsOfTypeMatchingExpressionInNumericRange() {
//        System.out.println("getAllDataItemsOfTypeMatchingExpressionInNumericRange");
//        String prefix = "";
//        String suffix = "";
//        int start = 0;
//        int end = 0;
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        ArrayList<Data> expResult = null;
//        ArrayList<Data> result = instance.getAllDataItemsOfTypeMatchingExpressionInNumericRange(prefix, suffix, start, end, classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNamesForAllDataItemsOfType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNamesForAllDataItemsOfType() {
//        System.out.println("getNamesForAllDataItemsOfType");
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.getNamesForAllDataItemsOfType(classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNamesForAllDataItemsOfTypes method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNamesForAllDataItemsOfTypes() {
//        System.out.println("getNamesForAllDataItemsOfTypes");
//        Class[] classtypes = null;
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.getNamesForAllDataItemsOfTypes(classtypes);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of filterNamesWithoutPrefix method, of class MotifLabEngine.
//     */
//    @Test
//    public void testFilterNamesWithoutPrefix() {
//        System.out.println("filterNamesWithoutPrefix");
//        ArrayList<String> list = null;
//        String prefix = "";
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.filterNamesWithoutPrefix(list, prefix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of filterNamesWithoutSuffix method, of class MotifLabEngine.
//     */
//    @Test
//    public void testFilterNamesWithoutSuffix() {
//        System.out.println("filterNamesWithoutSuffix");
//        ArrayList<String> list = null;
//        String suffix = "";
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.filterNamesWithoutSuffix(list, suffix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of hasDataItemsOfType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testHasDataItemsOfType() {
//        System.out.println("hasDataItemsOfType");
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.hasDataItemsOfType(classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of countDataItemsOfType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCountDataItemsOfType() {
//        System.out.println("countDataItemsOfType");
//        Class classtype = null;
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.countDataItemsOfType(classtype);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNumberOfSequences method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNumberOfSequences() {
//        System.out.println("getNumberOfSequences");
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.getNumberOfSequences();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultSequenceCollection method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultSequenceCollection() {
//        System.out.println("getDefaultSequenceCollection");
//        MotifLabEngine instance = null;
//        SequenceCollection expResult = null;
//        SequenceCollection result = instance.getDefaultSequenceCollection();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultSequenceCollectionName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultSequenceCollectionName() {
//        System.out.println("getDefaultSequenceCollectionName");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getDefaultSequenceCollectionName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultOutputObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultOutputObject() {
//        System.out.println("getDefaultOutputObject");
//        MotifLabEngine instance = null;
//        OutputData expResult = null;
//        OutputData result = instance.getDefaultOutputObject();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultOutputObjectName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultOutputObjectName() {
//        System.out.println("getDefaultOutputObjectName");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getDefaultOutputObjectName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllOperations method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllOperations() {
//        System.out.println("getAllOperations");
//        MotifLabEngine instance = null;
//        ArrayList<Operation> expResult = null;
//        ArrayList<Operation> result = instance.getAllOperations();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getOperation method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetOperation() {
//        System.out.println("getOperation");
//        String name = "";
//        MotifLabEngine instance = null;
//        Operation expResult = null;
//        Operation result = instance.getOperation(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllDataFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllDataFormats() {
//        System.out.println("getAllDataFormats");
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getAllDataFormats();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataOutputFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataOutputFormats_Data() {
//        System.out.println("getDataOutputFormats");
//        Data data = null;
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getDataOutputFormats(data);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataOutputFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataOutputFormats_Class() {
//        System.out.println("getDataOutputFormats");
//        Class dataclass = null;
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getDataOutputFormats(dataclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataInputFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataInputFormats_Data() {
//        System.out.println("getDataInputFormats");
//        Data data = null;
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getDataInputFormats(data);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataInputFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataInputFormats_Class() {
//        System.out.println("getDataInputFormats");
//        Class dataclass = null;
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getDataInputFormats(dataclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFeatureDataInputFormats method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFeatureDataInputFormats() {
//        System.out.println("getFeatureDataInputFormats");
//        MotifLabEngine instance = null;
//        ArrayList<DataFormat> expResult = null;
//        ArrayList<DataFormat> result = instance.getFeatureDataInputFormats();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataFormat() {
//        System.out.println("getDataFormat");
//        String name = "";
//        MotifLabEngine instance = null;
//        DataFormat expResult = null;
//        DataFormat result = instance.getDataFormat(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setDefaultDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetDefaultDataFormat() {
//        System.out.println("setDefaultDataFormat");
//        Class dataclass = null;
//        String dataformatname = "";
//        MotifLabEngine instance = null;
//        instance.setDefaultDataFormat(dataclass, dataformatname);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultDataFormat_Data() {
//        System.out.println("getDefaultDataFormat");
//        Data data = null;
//        MotifLabEngine instance = null;
//        DataFormat expResult = null;
//        DataFormat result = instance.getDefaultDataFormat(data);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDefaultDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDefaultDataFormat_Class() {
//        System.out.println("getDefaultDataFormat");
//        Class dataclass = null;
//        MotifLabEngine instance = null;
//        DataFormat expResult = null;
//        DataFormat result = instance.getDefaultDataFormat(dataclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataLoader method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataLoader() {
//        System.out.println("getDataLoader");
//        MotifLabEngine instance = null;
//        DataLoader expResult = null;
//        DataLoader result = instance.getDataLoader();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getGeneIDResolver method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetGeneIDResolver() {
//        System.out.println("getGeneIDResolver");
//        MotifLabEngine instance = null;
//        GeneIDResolver expResult = null;
//        GeneIDResolver result = instance.getGeneIDResolver();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getGeneOntologyEngine method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetGeneOntologyEngine() {
//        System.out.println("getGeneOntologyEngine");
//        MotifLabEngine instance = null;
//        GOengine expResult = null;
//        GOengine result = instance.getGeneOntologyEngine();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNetworkTimeout method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNetworkTimeout() {
//        System.out.println("getNetworkTimeout");
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.getNetworkTimeout();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setNetworkTimeout method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetNetworkTimeout() {
//        System.out.println("setNetworkTimeout");
//        int milliseconds = 0;
//        MotifLabEngine instance = null;
//        instance.setNetworkTimeout(milliseconds);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of autoCorrectSequenceNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAutoCorrectSequenceNames() {
//        System.out.println("autoCorrectSequenceNames");
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.autoCorrectSequenceNames();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setAutoCorrectSequenceNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetAutoCorrectSequenceNames() {
//        System.out.println("setAutoCorrectSequenceNames");
//        boolean correct = false;
//        MotifLabEngine instance = null;
//        instance.setAutoCorrectSequenceNames(correct);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setDataCacheDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetDataCacheDirectory() {
//        System.out.println("setDataCacheDirectory");
//        String cacheDirectory = "";
//        MotifLabEngine instance = null;
//        instance.setDataCacheDirectory(cacheDirectory);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setGeneIDCacheDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSetGeneIDCacheDirectory() {
//        System.out.println("setGeneIDCacheDirectory");
//        String cacheDirectory = "";
//        MotifLabEngine instance = null;
//        instance.setGeneIDCacheDirectory(cacheDirectory);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAnalysisNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAnalysisNames() {
//        System.out.println("getAnalysisNames");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAnalysisNames();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAvailableMotifDiscoveryAlgorithms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAvailableMotifDiscoveryAlgorithms() {
//        System.out.println("getAvailableMotifDiscoveryAlgorithms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAvailableMotifDiscoveryAlgorithms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAvailableMotifScanningAlgorithms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAvailableMotifScanningAlgorithms() {
//        System.out.println("getAvailableMotifScanningAlgorithms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAvailableMotifScanningAlgorithms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAvailableModuleDiscoveryAlgorithms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAvailableModuleDiscoveryAlgorithms() {
//        System.out.println("getAvailableModuleDiscoveryAlgorithms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAvailableModuleDiscoveryAlgorithms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAvailableModuleScanningAlgorithms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAvailableModuleScanningAlgorithms() {
//        System.out.println("getAvailableModuleScanningAlgorithms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAvailableModuleScanningAlgorithms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAvailableEnsemblePredictionAlgorithms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAvailableEnsemblePredictionAlgorithms() {
//        System.out.println("getAvailableEnsemblePredictionAlgorithms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAvailableEnsemblePredictionAlgorithms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getOtherExternalPrograms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetOtherExternalPrograms() {
//        System.out.println("getOtherExternalPrograms");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getOtherExternalPrograms();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getExternalProgram method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetExternalProgram() {
//        System.out.println("getExternalProgram");
//        String programname = "";
//        MotifLabEngine instance = null;
//        ExternalProgram expResult = null;
//        ExternalProgram result = instance.getExternalProgram(programname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllExternalPrograms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllExternalPrograms() {
//        System.out.println("getAllExternalPrograms");
//        MotifLabEngine instance = null;
//        Collection<ExternalProgram> expResult = null;
//        Collection<ExternalProgram> result = instance.getAllExternalPrograms();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAnalysis method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAnalysis() {
//        System.out.println("getAnalysis");
//        String analysisName = "";
//        MotifLabEngine instance = null;
//        Analysis expResult = null;
//        Analysis result = instance.getAnalysis(analysisName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNewAnalysis method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNewAnalysis() {
//        System.out.println("getNewAnalysis");
//        String analysisName = "";
//        MotifLabEngine instance = null;
//        Analysis expResult = null;
//        Analysis result = instance.getNewAnalysis(analysisName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllMotifComparators method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllMotifComparators() {
//        System.out.println("getAllMotifComparators");
//        MotifLabEngine instance = null;
//        ArrayList<MotifComparator> expResult = null;
//        ArrayList<MotifComparator> result = instance.getAllMotifComparators();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerMotifComparator method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterMotifComparator() {
//        System.out.println("registerMotifComparator");
//        MotifComparator motifcomparator = null;
//        MotifLabEngine instance = null;
//        instance.registerMotifComparator(motifcomparator);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of uninstallMotifComparator method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUninstallMotifComparator() {
//        System.out.println("uninstallMotifComparator");
//        MotifComparator motifcomparator = null;
//        MotifLabEngine instance = null;
//        instance.uninstallMotifComparator(motifcomparator);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAllMotifComparatorNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAllMotifComparatorNames() {
//        System.out.println("getAllMotifComparatorNames");
//        boolean abbreviations = false;
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getAllMotifComparatorNames(abbreviations);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMotifComparator method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMotifComparator() {
//        System.out.println("getMotifComparator");
//        String name = "";
//        MotifLabEngine instance = null;
//        MotifComparator expResult = null;
//        MotifComparator result = instance.getMotifComparator(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isDefaultSequenceCollection method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsDefaultSequenceCollection() {
//        System.out.println("isDefaultSequenceCollection");
//        Data data = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isDefaultSequenceCollection(data);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isReservedWord method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsReservedWord() {
//        System.out.println("isReservedWord");
//        String word = "";
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isReservedWord(word);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getReservedWords method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetReservedWords() {
//        System.out.println("getReservedWords");
//        MotifLabEngine instance = null;
//        HashSet<String> expResult = null;
//        HashSet<String> result = instance.getReservedWords();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerReservedWord method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterReservedWord() {
//        System.out.println("registerReservedWord");
//        String word = "";
//        MotifLabEngine instance = null;
//        instance.registerReservedWord(word);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterReservedWord method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterReservedWord() {
//        System.out.println("unregisterReservedWord");
//        String word = "";
//        MotifLabEngine instance = null;
//        instance.unregisterReservedWord(word);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isDataStorageEmpty method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsDataStorageEmpty() {
//        System.out.println("isDataStorageEmpty");
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isDataStorageEmpty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNumericDataForString method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNumericDataForString() {
//        System.out.println("getNumericDataForString");
//        String string = "";
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.getNumericDataForString(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getBasicValueForStringAsObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetBasicValueForStringAsObject() {
//        System.out.println("getBasicValueForStringAsObject");
//        String string = "";
//        Object expResult = null;
//        Object result = MotifLabEngine.getBasicValueForStringAsObject(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTypeNameForDataClass method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetTypeNameForDataClass() {
//        System.out.println("getTypeNameForDataClass");
//        Class dataclass = null;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getTypeNameForDataClass(dataclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTypeNameForDataOrBasicClass method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetTypeNameForDataOrBasicClass() {
//        System.out.println("getTypeNameForDataOrBasicClass");
//        Class dataclass = null;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getTypeNameForDataOrBasicClass(dataclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataClassForTypeName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataClassForTypeName() {
//        System.out.println("getDataClassForTypeName");
//        String typeName = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getDataClassForTypeName(typeName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkNameValidity method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCheckNameValidity() {
//        System.out.println("checkNameValidity");
//        String name = "";
//        boolean checkInUse = false;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.checkNameValidity(name, checkInUse);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkSequenceNameValidity method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCheckSequenceNameValidity() {
//        System.out.println("checkSequenceNameValidity");
//        String name = "";
//        boolean checkInUse = false;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.checkSequenceNameValidity(name, checkInUse);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertToLegalDataName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertToLegalDataName() {
//        System.out.println("convertToLegalDataName");
//        String name = "";
//        String expResult = "";
//        String result = MotifLabEngine.convertToLegalDataName(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertToLegalSequenceName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertToLegalSequenceName() {
//        System.out.println("convertToLegalSequenceName");
//        String name = "";
//        String expResult = "";
//        String result = MotifLabEngine.convertToLegalSequenceName(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSessionRequirements method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetSessionRequirements() {
//        System.out.println("getSessionRequirements");
//        List list = null;
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getSessionRequirements(list);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addMessageListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAddMessageListener() {
//        System.out.println("addMessageListener");
//        MessageListener listener = null;
//        MotifLabEngine instance = null;
//        instance.addMessageListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeMessageListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveMessageListener() {
//        System.out.println("removeMessageListener");
//        MessageListener listener = null;
//        MotifLabEngine instance = null;
//        instance.removeMessageListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of errorMessage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testErrorMessage() {
//        System.out.println("errorMessage");
//        String msg = "";
//        int errortype = 0;
//        MotifLabEngine instance = null;
//        instance.errorMessage(msg, errortype);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of logMessage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLogMessage_String() {
//        System.out.println("logMessage");
//        String msg = "";
//        MotifLabEngine instance = null;
//        instance.logMessage(msg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of logMessage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLogMessage_String_int() {
//        System.out.println("logMessage");
//        String msg = "";
//        int level = 0;
//        MotifLabEngine instance = null;
//        instance.logMessage(msg, level);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of statusMessage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStatusMessage() {
//        System.out.println("statusMessage");
//        String msg = "";
//        MotifLabEngine instance = null;
//        instance.statusMessage(msg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of progressReportMessage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testProgressReportMessage() {
//        System.out.println("progressReportMessage");
//        int progress = 0;
//        MotifLabEngine instance = null;
//        instance.progressReportMessage(progress);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addDataListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAddDataListener() {
//        System.out.println("addDataListener");
//        DataListener listener = null;
//        MotifLabEngine instance = null;
//        instance.addDataListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeDataListener method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveDataListener() {
//        System.out.println("removeDataListener");
//        DataListener listener = null;
//        MotifLabEngine instance = null;
//        instance.removeDataListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataAdded method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataAdded() {
//        System.out.println("dataAdded");
//        Data data = null;
//        MotifLabEngine instance = null;
//        instance.dataAdded(data);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataAddedToSet method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataAddedToSet() {
//        System.out.println("dataAddedToSet");
//        Data parent = null;
//        Data child = null;
//        MotifLabEngine instance = null;
//        instance.dataAddedToSet(parent, child);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataRemoved method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataRemoved() {
//        System.out.println("dataRemoved");
//        Data data = null;
//        MotifLabEngine instance = null;
//        instance.dataRemoved(data);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataRemovedFromSet method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataRemovedFromSet() {
//        System.out.println("dataRemovedFromSet");
//        Data parent = null;
//        Data child = null;
//        MotifLabEngine instance = null;
//        instance.dataRemovedFromSet(parent, child);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataUpdated method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataUpdated() {
//        System.out.println("dataUpdated");
//        Data data = null;
//        MotifLabEngine instance = null;
//        instance.dataUpdated(data);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataUpdate method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataUpdate() {
//        System.out.println("dataUpdate");
//        Data olddata = null;
//        Data newdata = null;
//        MotifLabEngine instance = null;
//        instance.dataUpdate(olddata, newdata);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dataOrderChanged method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDataOrderChanged() {
//        System.out.println("dataOrderChanged");
//        Data data = null;
//        Integer oldpos = null;
//        Integer newpos = null;
//        MotifLabEngine instance = null;
//        instance.dataOrderChanged(data, oldpos, newpos);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getExecutionLock method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetExecutionLock() {
//        System.out.println("getExecutionLock");
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.getExecutionLock();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertRegionTrackToMotifTrack method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertRegionTrackToMotifTrack() {
//        System.out.println("convertRegionTrackToMotifTrack");
//        RegionDataset dataset = null;
//        HashMap<String, String> namemap = null;
//        DNASequenceDataset dna = null;
//        boolean force = false;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.convertRegionTrackToMotifTrack(dataset, namemap, dna, force);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertRegionTrackToModuleTrack method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertRegionTrackToModuleTrack() {
//        System.out.println("convertRegionTrackToModuleTrack");
//        RegionDataset dataset = null;
//        DNASequenceDataset dna = null;
//        boolean force = false;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.convertRegionTrackToModuleTrack(dataset, dna, force);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertRegionTrackToNestedTrack method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertRegionTrackToNestedTrack() {
//        System.out.println("convertRegionTrackToNestedTrack");
//        RegionDataset dataset = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.convertRegionTrackToNestedTrack(dataset);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importGeneIDResolverConfig method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportGeneIDResolverConfig() {
//        System.out.println("importGeneIDResolverConfig");
//        MotifLabEngine instance = null;
//        instance.importGeneIDResolverConfig();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importOrganisms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportOrganisms() {
//        System.out.println("importOrganisms");
//        MotifLabEngine instance = null;
//        instance.importOrganisms();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importMotifClassifications method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportMotifClassifications() {
//        System.out.println("importMotifClassifications");
//        MotifLabEngine instance = null;
//        instance.importMotifClassifications();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getWebSiteURL method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetWebSiteURL() {
//        System.out.println("getWebSiteURL");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getWebSiteURL();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRepositoryURL method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetRepositoryURL() {
//        System.out.println("getRepositoryURL");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getRepositoryURL();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginsRepositoryURL method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginsRepositoryURL() {
//        System.out.println("getPluginsRepositoryURL");
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getPluginsRepositoryURL();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of storeDataRepositoryConfigurations method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStoreDataRepositoryConfigurations() {
//        System.out.println("storeDataRepositoryConfigurations");
//        MotifLabEngine instance = null;
//        instance.storeDataRepositoryConfigurations();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importDataRepositories method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportDataRepositories() {
//        System.out.println("importDataRepositories");
//        MotifLabEngine instance = null;
//        instance.importDataRepositories();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importExternalPrograms method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportExternalPrograms() {
//        System.out.println("importExternalPrograms");
//        MotifLabEngine instance = null;
//        instance.importExternalPrograms();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of importPlugins method, of class MotifLabEngine.
//     */
//    @Test
//    public void testImportPlugins() {
//        System.out.println("importPlugins");
//        MotifLabEngine instance = null;
//        instance.importPlugins();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readPluginMetaDataFromDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadPluginMetaDataFromDirectory() throws Exception {
//        System.out.println("readPluginMetaDataFromDirectory");
//        File plugindir = null;
//        MotifLabEngine instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.readPluginMetaDataFromDirectory(plugindir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readPluginMetaDataFromZIP method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadPluginMetaDataFromZIP() throws Exception {
//        System.out.println("readPluginMetaDataFromZIP");
//        File zipFile = null;
//        MotifLabEngine instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.readPluginMetaDataFromZIP(zipFile);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of instantiatePluginFromDirectory method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstantiatePluginFromDirectory() throws Exception {
//        System.out.println("instantiatePluginFromDirectory");
//        File plugindir = null;
//        MotifLabEngine instance = null;
//        Plugin expResult = null;
//        Plugin result = instance.instantiatePluginFromDirectory(plugindir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginClassLoader method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginClassLoader() {
//        System.out.println("getPluginClassLoader");
//        File pluginDir = null;
//        MotifLabEngine instance = null;
//        ClassLoader expResult = null;
//        ClassLoader result = instance.getPluginClassLoader(pluginDir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginClassLoaderFromClassName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginClassLoaderFromClassName() {
//        System.out.println("getPluginClassLoaderFromClassName");
//        String pluginClassName = "";
//        MotifLabEngine instance = null;
//        ClassLoader expResult = null;
//        ClassLoader result = instance.getPluginClassLoaderFromClassName(pluginClassName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginClassForName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginClassForName() {
//        System.out.println("getPluginClassForName");
//        String className = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getPluginClassForName(className);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerPlugin method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterPlugin() {
//        System.out.println("registerPlugin");
//        Plugin plugin = null;
//        HashMap<String, Object> metadata = null;
//        MotifLabEngine instance = null;
//        instance.registerPlugin(plugin, metadata);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of uninstallPlugin method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUninstallPlugin() throws Exception {
//        System.out.println("uninstallPlugin");
//        Plugin plugin = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.uninstallPlugin(plugin);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPlugins method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPlugins() {
//        System.out.println("getPlugins");
//        MotifLabEngine instance = null;
//        Plugin[] expResult = null;
//        Plugin[] result = instance.getPlugins();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPlugin method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPlugin() {
//        System.out.println("getPlugin");
//        String pluginname = "";
//        MotifLabEngine instance = null;
//        Plugin expResult = null;
//        Plugin result = instance.getPlugin(pluginname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginMetaData method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginMetaData() {
//        System.out.println("getPluginMetaData");
//        String pluginName = "";
//        MotifLabEngine instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.getPluginMetaData(pluginName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPluginProperty method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPluginProperty() {
//        System.out.println("getPluginProperty");
//        String pluginName = "";
//        String propertyName = "";
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.getPluginProperty(pluginName, propertyName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterDataFormat_Class_ClassArr() {
//        System.out.println("registerDataFormat");
//        Class dataformatClass = null;
//        Class[] useasdefaultfordatatypes = null;
//        MotifLabEngine instance = null;
//        instance.registerDataFormat(dataformatClass, useasdefaultfordatatypes);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterDataFormat_DataFormat() {
//        System.out.println("registerDataFormat");
//        DataFormat newformat = null;
//        MotifLabEngine instance = null;
//        instance.registerDataFormat(newformat);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterDataFormat method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterDataFormat() {
//        System.out.println("unregisterDataFormat");
//        DataFormat format = null;
//        MotifLabEngine instance = null;
//        instance.unregisterDataFormat(format);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerAnalysis method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterAnalysis() {
//        System.out.println("registerAnalysis");
//        Analysis analysis = null;
//        MotifLabEngine instance = null;
//        instance.registerAnalysis(analysis);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterAnalysis method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterAnalysis() {
//        System.out.println("unregisterAnalysis");
//        Analysis analysis = null;
//        MotifLabEngine instance = null;
//        instance.unregisterAnalysis(analysis);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getClassForAnalysis method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetClassForAnalysis() {
//        System.out.println("getClassForAnalysis");
//        String analysisname = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getClassForAnalysis(analysisname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAnalysisForClass method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAnalysisForClass() {
//        System.out.println("getAnalysisForClass");
//        Class analysisclass = null;
//        MotifLabEngine instance = null;
//        Analysis expResult = null;
//        Analysis result = instance.getAnalysisForClass(analysisclass);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerExternalProgram method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterExternalProgram() {
//        System.out.println("registerExternalProgram");
//        ExternalProgram program = null;
//        MotifLabEngine instance = null;
//        instance.registerExternalProgram(program);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterExternalProgram method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterExternalProgram() {
//        System.out.println("unregisterExternalProgram");
//        ExternalProgram program = null;
//        MotifLabEngine instance = null;
//        instance.unregisterExternalProgram(program);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerDataRepositoryType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterDataRepositoryType() {
//        System.out.println("registerDataRepositoryType");
//        DataRepository repository = null;
//        MotifLabEngine instance = null;
//        instance.registerDataRepositoryType(repository);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterDataRepositoryType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterDataRepositoryType() {
//        System.out.println("unregisterDataRepositoryType");
//        String typename = "";
//        MotifLabEngine instance = null;
//        instance.unregisterDataRepositoryType(typename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepositoryTypes method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepositoryTypes() {
//        System.out.println("getDataRepositoryTypes");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getDataRepositoryTypes();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepositoryType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepositoryType() {
//        System.out.println("getDataRepositoryType");
//        String name = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getDataRepositoryType(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepositoryTypeFromClassName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepositoryTypeFromClassName() {
//        System.out.println("getDataRepositoryTypeFromClassName");
//        String classname = "";
//        MotifLabEngine instance = null;
//        Class expResult = null;
//        Class result = instance.getDataRepositoryTypeFromClassName(classname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerDataRepository method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterDataRepository() {
//        System.out.println("registerDataRepository");
//        DataRepository repository = null;
//        MotifLabEngine instance = null;
//        instance.registerDataRepository(repository);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterDataRepository method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnregisterDataRepository() {
//        System.out.println("unregisterDataRepository");
//        DataRepository repository = null;
//        MotifLabEngine instance = null;
//        instance.unregisterDataRepository(repository);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepository method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepository() {
//        System.out.println("getDataRepository");
//        String name = "";
//        MotifLabEngine instance = null;
//        DataRepository expResult = null;
//        DataRepository result = instance.getDataRepository(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepositoryNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepositoryNames() {
//        System.out.println("getDataRepositoryNames");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getDataRepositoryNames();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataRepositories method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataRepositories() {
//        System.out.println("getDataRepositories");
//        MotifLabEngine instance = null;
//        DataRepository[] expResult = null;
//        DataRepository[] result = instance.getDataRepositories();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeExternalProgram method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveExternalProgram() {
//        System.out.println("removeExternalProgram");
//        String programName = "";
//        boolean deleteConfig = false;
//        MotifLabEngine instance = null;
//        ExternalProgram expResult = null;
//        ExternalProgram result = instance.removeExternalProgram(programName, deleteConfig);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFilenameForMotifCollection method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFilenameForMotifCollection() {
//        System.out.println("getFilenameForMotifCollection");
//        String motifCollectionName = "";
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getFilenameForMotifCollection(motifCollectionName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSizeForMotifCollection method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetSizeForMotifCollection() {
//        System.out.println("getSizeForMotifCollection");
//        String motifCollectionName = "";
//        MotifLabEngine instance = null;
//        int expResult = 0;
//        int result = instance.getSizeForMotifCollection(motifCollectionName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPredefinedMotifCollections method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPredefinedMotifCollections() {
//        System.out.println("getPredefinedMotifCollections");
//        MotifLabEngine instance = null;
//        Set<String> expResult = null;
//        Set<String> result = instance.getPredefinedMotifCollections();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerPredefinedMotifCollection method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterPredefinedMotifCollection() throws Exception {
//        System.out.println("registerPredefinedMotifCollection");
//        MotifCollection collection = null;
//        MotifLabEngine instance = null;
//        instance.registerPredefinedMotifCollection(collection);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of registerPredefinedMotifCollectionFromStream method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRegisterPredefinedMotifCollectionFromStream() throws Exception {
//        System.out.println("registerPredefinedMotifCollectionFromStream");
//        InputStream stream = null;
//        String filename = "";
//        String collectionName = "";
//        MotifLabEngine instance = null;
//        instance.registerPredefinedMotifCollectionFromStream(stream, filename, collectionName);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isFirstTimeStartup method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsFirstTimeStartup() {
//        System.out.println("isFirstTimeStartup");
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isFirstTimeStartup();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of raiseInstalledFlag method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRaiseInstalledFlag() {
//        System.out.println("raiseInstalledFlag");
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.raiseInstalledFlag();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of savePredefinedMotifCollectionsConfiguration method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSavePredefinedMotifCollectionsConfiguration() throws Exception {
//        System.out.println("savePredefinedMotifCollectionsConfiguration");
//        MotifLabEngine instance = null;
//        instance.savePredefinedMotifCollectionsConfiguration();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadPredefinedMotifCollectionsConfiguration method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLoadPredefinedMotifCollectionsConfiguration() throws Exception {
//        System.out.println("loadPredefinedMotifCollectionsConfiguration");
//        MotifLabEngine instance = null;
//        HashMap expResult = null;
//        HashMap result = instance.loadPredefinedMotifCollectionsConfiguration();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveDataToFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSaveDataToFile() throws Exception {
//        System.out.println("saveDataToFile");
//        Data data = null;
//        DataFormat dataformat = null;
//        String filename = "";
//        MotifLabEngine instance = null;
//        instance.saveDataToFile(data, dataformat, filename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveSerializedObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSaveSerializedObject() throws Exception {
//        System.out.println("saveSerializedObject");
//        Object object = null;
//        String filename = "";
//        MotifLabEngine instance = null;
//        instance.saveSerializedObject(object, filename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadSerializedObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLoadSerializedObject_String_boolean() throws Exception {
//        System.out.println("loadSerializedObject");
//        String filename = "";
//        boolean throwException = false;
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.loadSerializedObject(filename, throwException);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadSerializedObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLoadSerializedObject_String() throws Exception {
//        System.out.println("loadSerializedObject");
//        String filename = "";
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.loadSerializedObject(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of storeSystemObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStoreSystemObject() throws Exception {
//        System.out.println("storeSystemObject");
//        Object object = null;
//        String filename = "";
//        MotifLabEngine instance = null;
//        instance.storeSystemObject(object, filename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadSystemObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLoadSystemObject_String() throws Exception {
//        System.out.println("loadSystemObject");
//        String filename = "";
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.loadSystemObject(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of loadSystemObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testLoadSystemObject_String_boolean() throws Exception {
//        System.out.println("loadSystemObject");
//        String filename = "";
//        boolean throwException = false;
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.loadSystemObject(filename, throwException);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of systemObjectFileExists method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSystemObjectFileExists() {
//        System.out.println("systemObjectFileExists");
//        String filename = "";
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.systemObjectFileExists(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reportError method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReportError() {
//        System.out.println("reportError");
//        Throwable e = null;
//        MotifLabEngine instance = null;
//        instance.reportError(e);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getErrorReport method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetErrorReport() {
//        System.out.println("getErrorReport");
//        Throwable e = null;
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getErrorReport(e);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of uncaughtException method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUncaughtException() {
//        System.out.println("uncaughtException");
//        Thread t = null;
//        Throwable e = null;
//        MotifLabEngine instance = null;
//        instance.uncaughtException(t, e);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deleteTempFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDeleteTempFile() {
//        System.out.println("deleteTempFile");
//        File tempdir = null;
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.deleteTempFile(tempdir);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deleteOnExit method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDeleteOnExit() {
//        System.out.println("deleteOnExit");
//        File file = null;
//        MotifLabEngine.deleteOnExit(file);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of shutdown method, of class MotifLabEngine.
//     */
//    @Test
//    public void testShutdown() {
//        System.out.println("shutdown");
//        MotifLabEngine instance = null;
//        instance.shutdown();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of installBundledResource method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstallBundledResource() throws Exception {
//        System.out.println("installBundledResource");
//        String resourcepath = "";
//        String configFilename = "";
//        MotifLabEngine instance = null;
//        instance.installBundledResource(resourcepath, configFilename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of installConfigFileFromStream method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstallConfigFileFromStream() throws Exception {
//        System.out.println("installConfigFileFromStream");
//        String filename = "";
//        InputStream stream = null;
//        MotifLabEngine instance = null;
//        instance.installConfigFileFromStream(filename, stream);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createDataObject method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateDataObject() throws Exception {
//        System.out.println("createDataObject");
//        Class type = null;
//        String tempName = "";
//        MotifLabEngine instance = null;
//        Data expResult = null;
//        Data result = instance.createDataObject(type, tempName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getClassForName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetClassForName() throws Exception {
//        System.out.println("getClassForName");
//        String classname = "";
//        Class expResult = null;
//        Class result = MotifLabEngine.getClassForName(classname);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of extractSequencesFromFeatureDataset method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExtractSequencesFromFeatureDataset() {
//        System.out.println("extractSequencesFromFeatureDataset");
//        DNASequenceDataset dataset = null;
//        SequenceCollection result_2 = null;
//        MotifLabEngine instance = null;
//        SequenceCollection expResult = null;
//        SequenceCollection result = instance.extractSequencesFromFeatureDataset(dataset, result_2);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createHTMLheader method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateHTMLheader() {
//        System.out.println("createHTMLheader");
//        String title = "";
//        String extraHeader = "";
//        String internalCSS = "";
//        boolean includeJavascript = false;
//        boolean includeCSS = false;
//        boolean includeDefaultCSS = false;
//        OutputData outputdata = null;
//        MotifLabEngine instance = null;
//        instance.createHTMLheader(title, extraHeader, internalCSS, includeJavascript, includeCSS, includeDefaultCSS, outputdata);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createInternalHTMLheader method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCreateInternalHTMLheader() {
//        System.out.println("createInternalHTMLheader");
//        OutputData outputdata = null;
//        MotifLabEngine instance = null;
//        instance.createInternalHTMLheader(outputdata);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of appendDefaultCSSstyle method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAppendDefaultCSSstyle() {
//        System.out.println("appendDefaultCSSstyle");
//        StringBuilder builder = null;
//        MotifLabEngine instance = null;
//        instance.appendDefaultCSSstyle(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of executeStartupConfigurationProtocol method, of class MotifLabEngine.
//     */
//    @Test
//    public void testExecuteStartupConfigurationProtocol() {
//        System.out.println("executeStartupConfigurationProtocol");
//        MotifLabEngine instance = null;
//        instance.executeStartupConfigurationProtocol();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getExternalDatabaseURL method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetExternalDatabaseURL() {
//        System.out.println("getExternalDatabaseURL");
//        String idformat = "";
//        String identifier = "";
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getExternalDatabaseURL(idformat, identifier);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getExternalDatabaseNames method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetExternalDatabaseNames() {
//        System.out.println("getExternalDatabaseNames");
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.getExternalDatabaseNames();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isExternalDatabaseName method, of class MotifLabEngine.
//     */
//    @Test
//    public void testIsExternalDatabaseName() {
//        System.out.println("isExternalDatabaseName");
//        String name = "";
//        MotifLabEngine instance = null;
//        boolean expResult = false;
//        boolean result = instance.isExternalDatabaseName(name);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseDataReference method, of class MotifLabEngine.
//     */
//    @Test
//    public void testParseDataReference() {
//        System.out.println("parseDataReference");
//        String ref = "";
//        MotifLabEngine instance = null;
//        String[] expResult = null;
//        String[] result = instance.parseDataReference(ref);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of resolveDataReferences method, of class MotifLabEngine.
//     */
//    @Test
//    public void testResolveDataReferences() throws Exception {
//        System.out.println("resolveDataReferences");
//        String reference = "";
//        String lineSeparator = "";
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.resolveDataReferences(reference, lineSeparator);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNaturalSortOrderComparator method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNaturalSortOrderComparator() {
//        System.out.println("getNaturalSortOrderComparator");
//        boolean sortAscending = false;
//        Comparator<String> expResult = null;
//        Comparator<String> result = MotifLabEngine.getNaturalSortOrderComparator(sortAscending);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sortNaturalOrder method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSortNaturalOrder_List_boolean() {
//        System.out.println("sortNaturalOrder");
//        List<String> list = null;
//        boolean ascending = false;
//        MotifLabEngine.sortNaturalOrder(list, ascending);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sortNaturalOrder method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSortNaturalOrder_StringArr_boolean() {
//        System.out.println("sortNaturalOrder");
//        String[] list = null;
//        boolean ascending = false;
//        MotifLabEngine.sortNaturalOrder(list, ascending);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNewNotifications method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetNewNotifications() {
//        System.out.println("getNewNotifications");
//        int level = 0;
//        boolean showAll = false;
//        boolean updateIndex = false;
//        MotifLabEngine instance = null;
//        ArrayList<ImportantNotification> expResult = null;
//        ArrayList<ImportantNotification> result = instance.getNewNotifications(level, showAll, updateIndex);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveSession method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSaveSession() throws Exception {
//        System.out.println("saveSession");
//        ObjectOutputStream outputStream = null;
//        HashMap<String, Object> info = null;
//        ProgressListener progressListener = null;
//        MotifLabEngine instance = null;
//        instance.saveSession(outputStream, info, progressListener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of restoreSession method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRestoreSession() {
//        System.out.println("restoreSession");
//        InputStream input = null;
//        ProgressListener progressListener = null;
//        MotifLabEngine instance = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = instance.restoreSession(input, progressListener);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of installResourcesFirstTime method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstallResourcesFirstTime() throws Exception {
//        System.out.println("installResourcesFirstTime");
//        MotifLabEngine instance = null;
//        instance.installResourcesFirstTime();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of installConfigFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstallConfigFile() throws Exception {
//        System.out.println("installConfigFile");
//        File newfile = null;
//        MotifLabEngine instance = null;
//        instance.installConfigFile(newfile);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of installConfigFilesFromZip method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInstallConfigFilesFromZip() throws Exception {
//        System.out.println("installConfigFilesFromZip");
//        File zipfile = null;
//        MotifLabEngine instance = null;
//        instance.installConfigFilesFromZip(zipfile);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

   
    /**
     * Test of registerResource method, of class MotifLabEngine.
     */
    @Test
    public void testRegisterResource() {
        System.out.println("registerResource");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First", "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", null, NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", null, String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();       
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere   
        assertEquals(engine.registerResource(resource1), false); // returns false since it does not replace an existing resource
        assertEquals(engine.getNumberOfResources(), 1);       
        assertEquals(engine.registerResource(resource2), false); // returns false since it does not replace an existing resource  
        assertEquals(engine.getNumberOfResources(), 2);  
        assertEquals(engine.getResource("Second",null), resource2.getResourceInstance()); //              
        assertEquals(engine.registerResource(resource3), false); // returns false since it does not replace an existing resource  
        assertEquals(engine.getNumberOfResources(), 3);                 
        assertEquals(engine.registerResource(resource4), false); // returns false since it does not replace an existing resource  
        assertEquals(engine.getNumberOfResources(), 4);   
        assertEquals(engine.getResource("First",null),  resource1.getResourceInstance()); // check that the namespace is correct     
        assertEquals(engine.getResource("First","DTV"), resource4.getResourceInstance()); // check that the namespace is correct     
        assertEquals(engine.getResource("DTV|First",null), resource4.getResourceInstance()); // white-box testing the registry name             
        assertEquals(engine.registerResource(resource5), true); // returns true since this should replace resource2 which has the same name 
        assertEquals(engine.getNumberOfResources(), 4);
        assertEquals(engine.getResource("Second",null), resource5.getResourceInstance()); // check that the resource has been replaced      
        assertEquals(engine.registerResource(resource6), false); // returns false since it does not replace an existing resource  
        assertEquals(engine.getNumberOfResources(), 5);  
    }

    /**
     * Test of deregisterResource method, of class MotifLabEngine.
     */
    @Test
    public void testDeregisterResource() {
        System.out.println("deregisterResource");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First", "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", null, NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", null, String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere        
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5); // this will replace resource2
        engine.registerResource(resource6);
        assertEquals(engine.getNumberOfResources(), 5);
        assertEquals(engine.deregisterResource("Second", null), resource5);
        assertEquals(engine.getNumberOfResources(), 4);     
        assertEquals(engine.deregisterResource("Third", null), null); // this is not a registered resource, so it should return null and not remove anything
        assertEquals(engine.getNumberOfResources(), 4);           
        assertEquals(engine.deregisterResource("Third", "DTV"), resource3);
        assertEquals(engine.getNumberOfResources(), 3);              
    }

    /**
     * Test of getResourceClass method, of class MotifLabEngine.
     */
    @Test
    public void testGetResourceClass() {
        System.out.println("getResourceClass");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));     
        MotifLabEngine engine = MotifLabEngine.getEngine();
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere         
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        assertEquals(engine.getResourceClass("First", null), String.class);
        assertEquals(engine.getResourceClass("Second", null), Integer.class);
        assertEquals(engine.getResourceClass("Third", null), null); // this is not registered
        assertEquals(engine.getResourceClass("Third", "DTV"), NullPointerException.class);
    }

    /**
     * Test of getResourceIcon method, of class MotifLabEngine.
     */
    @Test
    public void testGetResourceIcon() {
        System.out.println("getResourceIcon");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, new SimpleDataPanelIcon(0, 0, Color.yellow), "This is the first resource");
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));     
        MotifLabEngine engine = MotifLabEngine.getEngine();    
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource3);
        assertTrue(engine.getResourceIcon("First", null) instanceof SimpleDataPanelIcon);
        assertEquals(engine.getResourceIcon("Third", null), null); // this is not registered
        assertEquals(engine.getResourceIcon("Third", "DTV"), null); // this is registered but does not have an icon 
    }

    /**
     * Test of getResource method, of class MotifLabEngine.
     */
    @Test
    public void testGetResource() {
        System.out.println("getResource");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First", "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", null, NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", null, String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();     
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5);
        engine.registerResource(resource6);
        assertEquals(engine.getResource("First",null),  resource1.getResourceInstance()); // check that the namespace is correct    
        assertEquals(engine.getResource("Second",null), resource5.getResourceInstance()); //  resource2 should have been replaced by resource5 since they have the same name and type              
        assertEquals(engine.getResource("First","DTV"), resource4.getResourceInstance()); // check that the namespace is correct     
        assertEquals(engine.getResource("DTV|First",null), resource4.getResourceInstance()); // white-box testing the registry name             
        assertEquals(engine.getResource("Sixth",null), "This is the sixth resource"); //        
    }

    /**
     * Test of getResources method, of class MotifLabEngine.
     */
    @Test
    public void testGetResources_Class() {
        System.out.println("getResources");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First", "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", null, NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", null, String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();    
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5);
        engine.registerResource(resource6);
        ArrayList<MotifLabResource> result_String = engine.getResources(String.class);
        assertEquals(result_String.size(), 2); // note that the returned list is not required to be ordered, so I check it indirectly 
        assertTrue(result_String.contains(resource1)); // 
        assertTrue(result_String.contains(resource6)); //
        ArrayList<MotifLabResource> result_NPE = engine.getResources(NullPointerException.class);
        assertEquals(result_NPE.size(), 3); // note that the returned list are is required to be ordered, so I check it indirectly 
        assertTrue(result_NPE.contains(resource3)); // 
        assertTrue(result_NPE.contains(resource4)); //        
        assertTrue(result_NPE.contains(resource5)); //        
    }

    /**
     * Test of getResources method, of class MotifLabEngine.
     */
    @Test
    public void testGetResources_String() {
        System.out.println("getResources");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", "numbers", Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third",  "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First",  "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", "DTV", NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", "numbers", String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();     
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5);
        engine.registerResource(resource6);
        assertEquals(engine.getResources((String)null).size(),0); // no type name specified. The method should return an empty list
        assertEquals(engine.getResources("storage").size(),0); // unknown type name specified. The method should return an empty list
        ArrayList<MotifLabResource> result_numbers = engine.getResources("numbers");
        assertEquals(result_numbers.size(), 2); // note that the returned list is not required to be ordered, so I check it indirectly 
        assertTrue(result_numbers.contains(resource2)); // 
        assertTrue(result_numbers.contains(resource6)); //
        ArrayList<MotifLabResource> result_DTV = engine.getResources("DTV");
        assertEquals(result_DTV.size(), 3); // note that the lists is not required to be ordered, so I check it indirectly 
        assertTrue(result_DTV.contains(resource3)); // 
        assertTrue(result_DTV.contains(resource4)); //        
        assertTrue(result_DTV.contains(resource5)); //      
    }

    /**
     * Test of getResourceNames method, of class MotifLabEngine.
     */
    @Test
    public void testGetResourceNames_Class() {
        System.out.println("getResourceNames");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", null, Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third", "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First", "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", null, NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", null, String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();  
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5);
        engine.registerResource(resource6);
        ArrayList<String> expResult_String = new ArrayList<>(2);
        expResult_String.add(resource1.getResourceName());
        expResult_String.add(resource6.getResourceName());       
        ArrayList<String> result_String = engine.getResourceNames(String.class);       
        Collections.sort(expResult_String);
        Collections.sort(result_String);         
        assertEquals(expResult_String, result_String);
        ArrayList<String> expResult_NPE = new ArrayList<>(3);
        expResult_NPE.add(resource3.getResourceName());
        expResult_NPE.add(resource4.getResourceName());       
        expResult_NPE.add(resource5.getResourceName());          
        ArrayList<String> result_NPE = engine.getResourceNames(NullPointerException.class);       
        Collections.sort(expResult_NPE);
        Collections.sort(result_NPE);         
        assertEquals(expResult_NPE, result_NPE);        
    }

    /**
     * Test of getResourceNames method, of class MotifLabEngine.
     */
    @Test
    public void testGetResourceNames_String() {
        System.out.println("getResourceNames");
        MotifLabResource resource1 = new MotifLabResourceWrapper("First", null, String.class, null, "This is the first resource");
        MotifLabResource resource2 = new MotifLabResourceWrapper("Second", "numbers", Integer.class, null, new Integer(2));
        MotifLabResource resource3 = new MotifLabResourceWrapper("Third",  "DTV", NullPointerException.class, null, new NullPointerException("Third resource"));
        MotifLabResource resource4 = new MotifLabResourceWrapper("First",  "DTV", NullPointerException.class, null, new NullPointerException("Fourth resource"));
        MotifLabResource resource5 = new MotifLabResourceWrapper("Second", "DTV", NullPointerException.class, null, new NullPointerException("Fifth resource"));
        MotifLabResource resource6 = new MotifLabResourceWrapper("Sixth", "numbers", String.class, null, "This is the sixth resource");        
        MotifLabEngine engine = MotifLabEngine.getEngine();
        engine.removeAllResources(); // ensure there are no existing resources left over from other tests that can interfere          
        engine.registerResource(resource1);
        engine.registerResource(resource2);
        engine.registerResource(resource3);
        engine.registerResource(resource4);
        engine.registerResource(resource5);
        engine.registerResource(resource6);
        assertEquals(engine.getResourceNames((String)null).size(),0); // no type name specified. The method should return an empty list
        assertEquals(engine.getResourceNames("storage").size(),0); // unknown type name specified. The method should return an empty list
        ArrayList<String> result_numbers = engine.getResourceNames("numbers");
        assertEquals(result_numbers.size(), 2); // note that the returned list is not required to be ordered, so I check it indirectly 
        assertTrue(result_numbers.contains(resource2.getResourceName())); // 
        assertTrue(result_numbers.contains(resource6.getResourceName())); //
        ArrayList<String> result_DTV = engine.getResourceNames("DTV");
        assertEquals(result_DTV.size(), 3); // note that the lists is not required to be ordered, so I check it indirectly 
        assertTrue(result_DTV.contains(resource3.getResourceName())); // 
        assertTrue(result_DTV.contains(resource4.getResourceName())); //        
        assertTrue(result_DTV.contains(resource5.getResourceName())); //     
    }
    
    /**
     * Test of escapeQuotedString method, of class MotifLabEngine.
     */
    @Test
    public void testEscapeQuotedString() {
        String string    = "This string contains \"double-quoted\" words, tabs\tand newlines\n as well as \\backslashes\\";
        String expResult = "This string contains \\\"double-quoted\\\" words, tabs\\tand newlines\\n as well as \\\\backslashes\\\\";
        // System.out.println("escapeQuotedString: "+string);        
        String result = MotifLabEngine.escapeQuotedString(string);
        assertEquals(expResult, result);
        assertEquals(string, MotifLabEngine.unescapeQuotedString(MotifLabEngine.escapeQuotedString(string))); // Test that the two operations (escape+unescape) are truly inverse of eachother               
    }    

    /**
     * Test of unescapeQuotedString method, of class MotifLabEngine.
     */
    @Test
    public void testUnescapeQuotedString() {
        System.out.println("unescapeQuotedString");
        String string     = "This string contains \\\"double-quoted\\\" words, tabs\\tand newlines\\n as well as \\\\backslashes\\\\";        
        String expResult  = "This string contains \"double-quoted\" words, tabs\tand newlines\n as well as \\backslashes\\";
        String result = MotifLabEngine.unescapeQuotedString(string);
        assertEquals(expResult, result);
        assertEquals(string, MotifLabEngine.escapeQuotedString(MotifLabEngine.unescapeQuotedString(string))); // Test that the two operations (escape+unescape) are truly inverse of eachother  
    }

    /**
     * Test of inArray method, of class MotifLabEngine.
     */
    @Test
    public void testInArray_3args_1() {
        System.out.println("inArray");
        String[] list = new String[]{"First","Second","Third","Fourth","Fifth","Sixth","Seventh"};
        String[] list2 = new String[]{"First"};
        String[] list3 = new String[0];

        // Test first element in list
        assertEquals(MotifLabEngine.inArray("First", list, false), true); 
        assertEquals(MotifLabEngine.inArray("First", list, true),  true);
        assertEquals(MotifLabEngine.inArray("first", list, false), false);
        assertEquals(MotifLabEngine.inArray("first", list, true), true);
        assertEquals(MotifLabEngine.inArray("firST", list, true), true);
        // Test middle element in list
        assertEquals(MotifLabEngine.inArray("Third", list, false), true);
        assertEquals(MotifLabEngine.inArray("Third", list, true),  true);
        assertEquals(MotifLabEngine.inArray("third", list, false), false);
        assertEquals(MotifLabEngine.inArray("third", list, true), true);
        assertEquals(MotifLabEngine.inArray("thiRD", list, true), true);
        // Test last element in list
        assertEquals(MotifLabEngine.inArray("Seventh", list, false), true);
        assertEquals(MotifLabEngine.inArray("Seventh", list, true),  true);
        assertEquals(MotifLabEngine.inArray("seventh", list, false), false);
        assertEquals(MotifLabEngine.inArray("seventh", list, true), true);
        assertEquals(MotifLabEngine.inArray("seveNTH", list, true), true);        
        // Test element that is not present
        assertEquals(MotifLabEngine.inArray("Eight", list, false), false);
        assertEquals(MotifLabEngine.inArray("Eight", list, true), false);        
        // Test shorter list
        assertEquals(MotifLabEngine.inArray("First", list2, false), true); 
        assertEquals(MotifLabEngine.inArray("First", list2, true),  true);
        assertEquals(MotifLabEngine.inArray("first", list2, false), false);
        assertEquals(MotifLabEngine.inArray("first", list2, true), true);
        assertEquals(MotifLabEngine.inArray("firST", list2, true), true);    
        assertEquals(MotifLabEngine.inArray("Third", list2, false), false);
        assertEquals(MotifLabEngine.inArray("Third", list2, true),  false);
        assertEquals(MotifLabEngine.inArray("third", list2, false), false);
        assertEquals(MotifLabEngine.inArray("third", list2, true), false);
        assertEquals(MotifLabEngine.inArray("thiRD", list2, true), false);     
        // Test empty list        
        assertEquals(MotifLabEngine.inArray("First", list3, false), false); 
        assertEquals(MotifLabEngine.inArray("First", list3, true),  false);
        assertEquals(MotifLabEngine.inArray("first", list3, false), false);
        assertEquals(MotifLabEngine.inArray("first", list3, true), false);
        assertEquals(MotifLabEngine.inArray("firST", list3, true), false);         
    }

    /**
     * Test of inArray method, of class MotifLabEngine.
     */
    @Test
    public void testInArray_3args_2() {
        System.out.println("inArray");
        ArrayList<String> list  = new  ArrayList<>(7);
        list.addAll(Arrays.asList(new String[]{"First","Second","Third","Fourth","Fifth","Sixth","Seventh"}));
        ArrayList<String> list2 = new  ArrayList<>(1); list2.add("First");
        ArrayList<String> list3 = new  ArrayList<>(0);

        // Test first element in list
        assertEquals(MotifLabEngine.inArray("First", list, false), true); 
        assertEquals(MotifLabEngine.inArray("First", list, true),  true);
        assertEquals(MotifLabEngine.inArray("first", list, false), false);
        assertEquals(MotifLabEngine.inArray("first", list, true), true);
        assertEquals(MotifLabEngine.inArray("firST", list, true), true);
        // Test middle element in list
        assertEquals(MotifLabEngine.inArray("Third", list, false), true);
        assertEquals(MotifLabEngine.inArray("Third", list, true),  true);
        assertEquals(MotifLabEngine.inArray("third", list, false), false);
        assertEquals(MotifLabEngine.inArray("third", list, true), true);
        assertEquals(MotifLabEngine.inArray("thiRD", list, true), true);
        // Test last element in list
        assertEquals(MotifLabEngine.inArray("Seventh", list, false), true);
        assertEquals(MotifLabEngine.inArray("Seventh", list, true),  true);
        assertEquals(MotifLabEngine.inArray("seventh", list, false), false);
        assertEquals(MotifLabEngine.inArray("seventh", list, true), true);
        assertEquals(MotifLabEngine.inArray("seveNTH", list, true), true);        
        // Test element that is not present
        assertEquals(MotifLabEngine.inArray("Eight", list, false), false);
        assertEquals(MotifLabEngine.inArray("Eight", list, true), false);        
        // Test shorter list
        assertEquals(MotifLabEngine.inArray("First", list2, false), true); 
        assertEquals(MotifLabEngine.inArray("First", list2, true),  true);
        assertEquals(MotifLabEngine.inArray("first", list2, false), false);
        assertEquals(MotifLabEngine.inArray("first", list2, true), true);
        assertEquals(MotifLabEngine.inArray("firST", list2, true), true);    
        assertEquals(MotifLabEngine.inArray("Third", list2, false), false);
        assertEquals(MotifLabEngine.inArray("Third", list2, true),  false);
        assertEquals(MotifLabEngine.inArray("third", list2, false), false);
        assertEquals(MotifLabEngine.inArray("third", list2, true), false);
        assertEquals(MotifLabEngine.inArray("thiRD", list2, true), false);     
        // Test empty list        
        assertEquals(MotifLabEngine.inArray("First", list3, false), false); 
        assertEquals(MotifLabEngine.inArray("First", list3, true),  false);
        assertEquals(MotifLabEngine.inArray("first", list3, false), false);
        assertEquals(MotifLabEngine.inArray("first", list3, true), false);
        assertEquals(MotifLabEngine.inArray("firST", list3, true), false); 
    }

//    /**
//     * Test of listcompare method, of class MotifLabEngine.
//     */
//    @Test
//    public void testListcompare() {
//        System.out.println("listcompare");
//        int[] list1 = null;
//        int[] list2 = null;
//        boolean expResult = false;
//        boolean result = MotifLabEngine.listcompare(list1, list2);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of inArray method, of class MotifLabEngine.
//     */
//    @Test
//    public void testInArray_int_intArr() {
//        System.out.println("inArray");
//        int element = 0;
//        int[] list = null;
//        boolean expResult = false;
//        boolean result = MotifLabEngine.inArray(element, list);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPage_URL() throws Exception {
//        System.out.println("getPage");
//        URL url = null;
//        String expResult = "";
//        String result = MotifLabEngine.getPage(url);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPage method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPage_URL_Document() throws Exception {
//        System.out.println("getPage");
//        URL url = null;
//        Document document = null;
//        MotifLabEngine.getPage(url, document);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPageAsList method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPageAsList() throws Exception {
//        System.out.println("getPageAsList");
//        URL url = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.getPageAsList(url);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPageUsingHttpPost method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPageUsingHttpPost() throws Exception {
//        System.out.println("getPageUsingHttpPost");
//        URL url = null;
//        Map<String, Object> parameters = null;
//        int timeout = 0;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.getPageUsingHttpPost(url, parameters, timeout);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readFileContents method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadFileContents() throws Exception {
//        System.out.println("readFileContents");
//        String filenameOrURL = "";
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.readFileContents(filenameOrURL);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readStreamContents method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadStreamContents() throws Exception {
//        System.out.println("readStreamContents");
//        InputStream inputStream = null;
//        MotifLabEngine instance = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = instance.readStreamContents(inputStream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataSourceForString method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetDataSourceForString() throws Exception {
//        System.out.println("getDataSourceForString");
//        String filenameOrURL = "";
//        MotifLabEngine instance = null;
//        Object expResult = null;
//        Object result = instance.getDataSourceForString(filenameOrURL);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFile_String() {
//        System.out.println("getFile");
//        String filename = "";
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.getFile(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFile_String_String() {
//        System.out.println("getFile");
//        String dir = "";
//        String filename = "";
//        MotifLabEngine instance = null;
//        File expResult = null;
//        File result = instance.getFile(dir, filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFilePath method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFilePath() {
//        System.out.println("getFilePath");
//        String dir = "";
//        String filename = "";
//        MotifLabEngine instance = null;
//        String expResult = "";
//        String result = instance.getFilePath(dir, filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFile_File_String() {
//        System.out.println("getFile");
//        File parent = null;
//        String filename = "";
//        File expResult = null;
//        File result = MotifLabEngine.getFile(parent, filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getInputStreamForDataSource method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetInputStreamForDataSource() throws Exception {
//        System.out.println("getInputStreamForDataSource");
//        Object source = null;
//        InputStream expResult = null;
//        InputStream result = MotifLabEngine.getInputStreamForDataSource(source);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getInputStreamForFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetInputStreamForFile() throws Exception {
//        System.out.println("getInputStreamForFile");
//        File file = null;
//        InputStream expResult = null;
//        InputStream result = MotifLabEngine.getInputStreamForFile(file);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getOutputStreamForFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetOutputStreamForFile() throws Exception {
//        System.out.println("getOutputStreamForFile");
//        File file = null;
//        OutputStream expResult = null;
//        OutputStream result = MotifLabEngine.getOutputStreamForFile(file);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRandomNumberGenerator method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetRandomNumberGenerator() {
//        System.out.println("getRandomNumberGenerator");
//        Random expResult = null;
//        Random result = MotifLabEngine.getRandomNumberGenerator();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of percentEncode method, of class MotifLabEngine.
//     */
//    @Test
//    public void testPercentEncode() {
//        System.out.println("percentEncode");
//        String string = "";
//        String expResult = "";
//        String result = MotifLabEngine.percentEncode(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of percentDecode method, of class MotifLabEngine.
//     */
//    @Test
//    public void testPercentDecode() {
//        System.out.println("percentDecode");
//        String string = "";
//        String expResult = "";
//        String result = MotifLabEngine.percentDecode(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of calculateMatthewsCorrelationCoefficient method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCalculateMatthewsCorrelationCoefficient() {
//        System.out.println("calculateMatthewsCorrelationCoefficient");
//        int TP = 0;
//        int FP = 0;
//        int TN = 0;
//        int FN = 0;
//        double expResult = 0.0;
//        double result = MotifLabEngine.calculateMatthewsCorrelationCoefficient(TP, FP, TN, FN);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseSequence method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseSequence_String() {
//        System.out.println("reverseSequence");
//        String seq = "";
//        String expResult = "";
//        String result = MotifLabEngine.reverseSequence(seq);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseSequence method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseSequence_StringBuilder() {
//        System.out.println("reverseSequence");
//        StringBuilder seq = null;
//        StringBuilder expResult = null;
//        StringBuilder result = MotifLabEngine.reverseSequence(seq);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseSequence method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseSequence_charArr() {
//        System.out.println("reverseSequence");
//        char[] seq = null;
//        char[] expResult = null;
//        char[] result = MotifLabEngine.reverseSequence(seq);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseIUPACSequence method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseIUPACSequence() {
//        System.out.println("reverseIUPACSequence");
//        char[] seq = null;
//        char[] expResult = null;
//        char[] result = MotifLabEngine.reverseIUPACSequence(seq);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseArray method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseArray() {
//        System.out.println("reverseArray");
//        double[] data = null;
//        double[] expResult = null;
//        double[] result = MotifLabEngine.reverseArray(data);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reverseBase method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReverseBase() {
//        System.out.println("reverseBase");
//        char c = ' ';
//        char expResult = ' ';
//        char result = MotifLabEngine.reverseBase(c);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getBaseIndex method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetBaseIndex() {
//        System.out.println("getBaseIndex");
//        char c = ' ';
//        int expResult = 0;
//        int result = MotifLabEngine.getBaseIndex(c);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sortCaseInsensitive method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSortCaseInsensitive() {
//        System.out.println("sortCaseInsensitive");
//        String[] list = null;
//        String[] expResult = null;
//        String[] result = MotifLabEngine.sortCaseInsensitive(list);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitQuotedStringListOnComma method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitQuotedStringListOnComma() throws Exception {
//        System.out.println("splitQuotedStringListOnComma");
//        CharSequence text = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitQuotedStringListOnComma(text);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnCommaSimple method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnCommaSimple() {
//        System.out.println("splitOnCommaSimple");
//        String string = "";
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitOnCommaSimple(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnComma method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnComma() throws Exception {
//        System.out.println("splitOnComma");
//        CharSequence text = null;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitOnComma(text);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnCharacter method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnCharacter_CharSequence_char() throws Exception {
//        System.out.println("splitOnCharacter");
//        CharSequence text = null;
//        char splitchar = ' ';
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitOnCharacter(text, splitchar);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnCommaToArray method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnCommaToArray() throws Exception {
//        System.out.println("splitOnCommaToArray");
//        CharSequence text = null;
//        String[] expResult = null;
//        String[] result = MotifLabEngine.splitOnCommaToArray(text);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnSpace method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnSpace() throws Exception {
//        System.out.println("splitOnSpace");
//        CharSequence text = null;
//        boolean stripquotes = false;
//        boolean removeEscapesFromQuotes = false;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitOnSpace(text, stripquotes, removeEscapesFromQuotes);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of stripQuotes method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStripQuotes() {
//        System.out.println("stripQuotes");
//        String string = "";
//        String expResult = "";
//        String result = MotifLabEngine.stripQuotes(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addQuotes method, of class MotifLabEngine.
//     */
//    @Test
//    public void testAddQuotes() {
//        System.out.println("addQuotes");
//        String string = "";
//        String expResult = "";
//        String result = MotifLabEngine.addQuotes(string);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of stripBraces method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStripBraces() {
//        System.out.println("stripBraces");
//        String string = "";
//        String prefix = "";
//        String suffix = "";
//        String expResult = "";
//        String result = MotifLabEngine.stripBraces(string, prefix, suffix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnCharacter method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnCharacter_4args() throws Exception {
//        System.out.println("splitOnCharacter");
//        CharSequence text = null;
//        char separator = ' ';
//        char open = ' ';
//        char close = ' ';
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.splitOnCharacter(text, separator, open, close);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splice method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplice_StringArr_String() {
//        System.out.println("splice");
//        String[] list = null;
//        String separator = "";
//        String expResult = "";
//        String result = MotifLabEngine.splice(list, separator);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splice method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplice_List_String() {
//        System.out.println("splice");
//        List list = null;
//        String separator = "";
//        String expResult = "";
//        String result = MotifLabEngine.splice(list, separator);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of splitOnNumericPart method, of class MotifLabEngine.
//     */
//    @Test
//    public void testSplitOnNumericPart() {
//        System.out.println("splitOnNumericPart");
//        String string = "";
//        String[] expResult = null;
//        String[] result = MotifLabEngine.splitOnNumericPart(string);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of convertToType method, of class MotifLabEngine.
//     */
//    @Test
//    public void testConvertToType() throws Exception {
//        System.out.println("convertToType");
//        Object value = null;
//        Class type = null;
//        Object expResult = null;
//        Object result = MotifLabEngine.convertToType(value, type);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getIndexOfFirstMatch method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetIndexOfFirstMatch() {
//        System.out.println("getIndexOfFirstMatch");
//        double[] values = null;
//        double value = 0.0;
//        int expResult = 0;
//        int result = MotifLabEngine.getIndexOfFirstMatch(values, value);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMinimumValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMinimumValue() {
//        System.out.println("getMinimumValue");
//        double[] values = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getMinimumValue(values);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMaximumValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMaximumValue() {
//        System.out.println("getMaximumValue");
//        double[] values = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getMaximumValue(values);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFirstIndexOfSmallestValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFirstIndexOfSmallestValue() {
//        System.out.println("getFirstIndexOfSmallestValue");
//        int[] values = null;
//        int expResult = 0;
//        int result = MotifLabEngine.getFirstIndexOfSmallestValue(values);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFirstIndexOfLargestValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFirstIndexOfLargestValue() {
//        System.out.println("getFirstIndexOfLargestValue");
//        int[] values = null;
//        int expResult = 0;
//        int result = MotifLabEngine.getFirstIndexOfLargestValue(values);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMedianValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMedianValue_doubleArr() {
//        System.out.println("getMedianValue");
//        double[] sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getMedianValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMedianValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetMedianValue_ArrayList() {
//        System.out.println("getMedianValue");
//        ArrayList<Double> sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getMedianValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFirstQuartileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFirstQuartileValue_doubleArr() {
//        System.out.println("getFirstQuartileValue");
//        double[] sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getFirstQuartileValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFirstQuartileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetFirstQuartileValue_ArrayList() {
//        System.out.println("getFirstQuartileValue");
//        ArrayList<Double> sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getFirstQuartileValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getThirdQuartileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetThirdQuartileValue_doubleArr() {
//        System.out.println("getThirdQuartileValue");
//        double[] sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getThirdQuartileValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getThirdQuartileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetThirdQuartileValue_ArrayList() {
//        System.out.println("getThirdQuartileValue");
//        ArrayList<Double> sortedvalues = null;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getThirdQuartileValue(sortedvalues);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPercentileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPercentileValue_ArrayList_double() {
//        System.out.println("getPercentileValue");
//        ArrayList<Double> sortedvalues = null;
//        double percentile = 0.0;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getPercentileValue(sortedvalues, percentile);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPercentileValue method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetPercentileValue_doubleArr_double() {
//        System.out.println("getPercentileValue");
//        double[] sortedvalues = null;
//        double percentile = 0.0;
//        double expResult = 0.0;
//        double result = MotifLabEngine.getPercentileValue(sortedvalues, percentile);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAverageAndStandardDeviation method, of class MotifLabEngine.
//     */
//    @Test
//    public void testGetAverageAndStandardDeviation() {
//        System.out.println("getAverageAndStandardDeviation");
//        ArrayList<Double> values = null;
//        double[] expResult = null;
//        double[] result = MotifLabEngine.getAverageAndStandardDeviation(values);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readTextFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadTextFile_String() throws Exception {
//        System.out.println("readTextFile");
//        String filename = "";
//        String expResult = "";
//        String result = MotifLabEngine.readTextFile(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of writeTextFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testWriteTextFile() throws Exception {
//        System.out.println("writeTextFile");
//        File file = null;
//        String text = "";
//        MotifLabEngine.writeTextFile(file, text);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readTextFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testReadTextFile_InputStream() throws Exception {
//        System.out.println("readTextFile");
//        InputStream inputStream = null;
//        String expResult = "";
//        String result = MotifLabEngine.readTextFile(inputStream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of compareNaturalOrder method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCompareNaturalOrder() {
//        System.out.println("compareNaturalOrder");
//        String string1 = "";
//        String string2 = "";
//        int expResult = 0;
//        int result = MotifLabEngine.compareNaturalOrder(string1, string2);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeDuplicateLines method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveDuplicateLines() {
//        System.out.println("removeDuplicateLines");
//        ArrayList<String> lines = null;
//        boolean onlyconsecutive = false;
//        ArrayList<String> expResult = null;
//        ArrayList<String> result = MotifLabEngine.removeDuplicateLines(lines, onlyconsecutive);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of flattenCommaSeparatedSublists method, of class MotifLabEngine.
//     */
//    @Test
//    public void testFlattenCommaSeparatedSublists() {
//        System.out.println("flattenCommaSeparatedSublists");
//        String[] list = null;
//        String[] expResult = null;
//        String[] result = MotifLabEngine.flattenCommaSeparatedSublists(list);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeDuplicates method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveDuplicates_StringArr_boolean() {
//        System.out.println("removeDuplicates");
//        String[] list = null;
//        boolean removeEmpty = false;
//        String[] expResult = null;
//        String[] result = MotifLabEngine.removeDuplicates(list, removeEmpty);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeDuplicates method, of class MotifLabEngine.
//     */
//    @Test
//    public void testRemoveDuplicates_intArr() {
//        System.out.println("removeDuplicates");
//        int[] list = null;
//        int[] expResult = null;
//        int[] result = MotifLabEngine.removeDuplicates(list);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unzipFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testUnzipFile() throws Exception {
//        System.out.println("unzipFile");
//        File sourcefile = null;
//        File directory = null;
//        MotifLabEngine instance = null;
//        ArrayList<File> expResult = null;
//        ArrayList<File> result = instance.unzipFile(sourcefile, directory);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of startsWithIgnoreCase method, of class MotifLabEngine.
//     */
//    @Test
//    public void testStartsWithIgnoreCase() {
//        System.out.println("startsWithIgnoreCase");
//        String string = "";
//        String prefix = "";
//        boolean expResult = false;
//        boolean result = MotifLabEngine.startsWithIgnoreCase(string, prefix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseINIfile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testParseINIfile() {
//        System.out.println("parseINIfile");
//        ArrayList<String> inifile = null;
//        HashMap<String, Object> expResult = null;
//        HashMap<String, Object> result = MotifLabEngine.parseINIfile(inifile);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of copyFile method, of class MotifLabEngine.
//     */
//    @Test
//    public void testCopyFile() throws Exception {
//        System.out.println("copyFile");
//        File source = null;
//        File destination = null;
//        MotifLabEngine.copyFile(source, destination);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of formatTime method, of class MotifLabEngine.
//     */
//    @Test
//    public void testFormatTime() {
//        System.out.println("formatTime");
//        long milliseconds = 0L;
//        String expResult = "";
//        String result = MotifLabEngine.formatTime(milliseconds);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of debugOutput method, of class MotifLabEngine.
//     */
//    @Test
//    public void testDebugOutput() {
//        System.out.println("debugOutput");
//        String line = "";
//        int indent = 0;
//        MotifLabEngine.debugOutput(line, indent);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
    @Test
    public void testGroupDigitsInNumber() {
        System.out.println("GroupDigitsInNumber");
        assertEquals(MotifLabEngine.groupDigitsInNumber(0), "0");    
        assertEquals(MotifLabEngine.groupDigitsInNumber(1), "1");         
        assertEquals(MotifLabEngine.groupDigitsInNumber(12), "12");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(123), "123");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(1234), "1,234");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(12345), "12,345");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(123456), "123,456");  
        assertEquals(MotifLabEngine.groupDigitsInNumber(1234567), "1,234,567");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(12345678), "12,345,678");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(123456789), "123,456,789");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(1234567890), "1,234,567,890");     
        assertEquals(MotifLabEngine.groupDigitsInNumber(-1), "-1");         
        assertEquals(MotifLabEngine.groupDigitsInNumber(-12), "-12");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-123), "-123");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-1234), "-1,234");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-12345), "-12,345");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-123456), "-123,456");  
        assertEquals(MotifLabEngine.groupDigitsInNumber(-1234567), "-1,234,567");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-12345678), "-12,345,678");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-123456789), "-123,456,789");        
        assertEquals(MotifLabEngine.groupDigitsInNumber(-1234567890), "-1,234,567,890");        
    }
    
    @Test
    public void testBreakLine() {    
        System.out.println("BreakLine");        
        String originalText="Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed condimentum massa sit amet cursus porttitor. Nam ullamcorper placerat neque quis blandit. Proin congue nulla a elementum tempus. Nulla ultrices iaculis eleifend. Phasellus aliquam diam tincidunt, posuere lectus non, commodo sem.";
        String lineWidth50="Lorem ipsum dolor sit amet, consectetur adipiscing\nelit. Sed condimentum massa sit amet cursus\nporttitor. Nam ullamcorper placerat neque quis\nblandit. Proin congue nulla a elementum tempus.\nNulla ultrices iaculis eleifend. Phasellus aliquam\ndiam tincidunt, posuere lectus non, commodo sem.";
        String lineWidth30="Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit.\nSed condimentum massa sit amet\ncursus porttitor. Nam\nullamcorper placerat neque\nquis blandit. Proin congue\nnulla a elementum tempus.\nNulla ultrices iaculis\neleifend. Phasellus aliquam\ndiam tincidunt, posuere lectus\nnon, commodo sem.";
        String lineWidth20="Lorem ipsum dolor\nsit amet,\nconsectetur\nadipiscing elit. Sed\ncondimentum massa\nsit amet cursus\nporttitor. Nam\nullamcorper placerat\nneque quis blandit.\nProin congue nulla a\nelementum tempus.\nNulla ultrices\niaculis eleifend.\nPhasellus aliquam\ndiam tincidunt,\nposuere lectus non,\ncommodo sem.";
        String lineWidth15="Lorem ipsum\ndolor sit amet,\nconsectetur\nadipiscing\nelit. Sed\ncondimentum\nmassa sit amet\ncursus\nporttitor. Nam\nullamcorper\nplacerat neque\nquis blandit.\nProin congue\nnulla a\nelementum\ntempus. Nulla\nultrices\niaculis\neleifend.\nPhasellus\naliquam diam\ntincidunt,\nposuere lectus\nnon, commodo\nsem.";
        String lineWidth10="Lorem\nipsum\ndolor sit\namet,\nconsectet-\nur\nadipiscing\nelit. Sed\ncondiment-\num massa\nsit amet\ncursus\nporttitor.\nNam\nullamcorp-\ner\nplacerat\nneque quis\nblandit.\nProin\ncongue\nnulla a\nelementum\ntempus.\nNulla\nultrices\niaculis\neleifend.\nPhasellus\naliquam\ndiam\ntincidunt,\nposuere\nlectus\nnon,\ncommodo\nsem.";
        String lineWidth30html="Lorem ipsum dolor sit amet,<br>consectetur adipiscing elit.<br>Sed condimentum massa sit amet<br>cursus porttitor. Nam<br>ullamcorper placerat neque<br>quis blandit. Proin congue<br>nulla a elementum tempus.<br>Nulla ultrices iaculis<br>eleifend. Phasellus aliquam<br>diam tincidunt, posuere lectus<br>non, commodo sem.";
        String lastLineText1="This is a test of the last linexxxxxxx";
        String lastLineSplit1="This is a test of\nthe last linexxxxxxx";
        String lastLineText2="This is a test of the last linexxxxxxxx";
        String lastLineSplit2="This is a test of\nthe last\nlinexxxxxxxx";
        String Supercalifragilisticexpialidocious="Supercalifragilisticexpialidocious";
        String Supercalifragilisticexpialidocious10="Supercali-\nfragilist-\nicexpiali-\ndocious";
        
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,50,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,30,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,20,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,15,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,10,"\n").replace("\n", "|"));     
 //       System.out.println(" * "+MotifLabEngine.breakLine(originalText,30,"<br>").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(lastLineText1,20,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(lastLineText2,20,"\n").replace("\n", "|"));
 //       System.out.println(" * "+MotifLabEngine.breakLine(Supercalifragilisticexpialidocious,10,"\n").replace("\n", "|"));        
        
        assertEquals(MotifLabEngine.breakLine(originalText,50,"\n"), lineWidth50);        
        assertEquals(MotifLabEngine.breakLine(originalText,30,"\n"), lineWidth30);        
        assertEquals(MotifLabEngine.breakLine(originalText,20,"\n"), lineWidth20);        
        assertEquals(MotifLabEngine.breakLine(originalText,15,"\n"), lineWidth15);    
        assertEquals(MotifLabEngine.breakLine(originalText,10,"\n"), lineWidth10);          
        assertEquals(MotifLabEngine.breakLine(originalText,30,"<br>"), lineWidth30html);        
        assertEquals(MotifLabEngine.breakLine(lastLineText1,20,"\n"), lastLineSplit1);       
        assertEquals(MotifLabEngine.breakLine(lastLineText2,20,"\n"), lastLineSplit2);       
        assertEquals(MotifLabEngine.breakLine(Supercalifragilisticexpialidocious,10,"\n"), Supercalifragilisticexpialidocious10);         
    }
    
//    @Test
//    public void testResolveURL() {    
//        System.out.println("ResolveURL");
//        String original="https://www.motiflab.org/datasets/HOCOMOCO_v10_mouse.mlx";
//        String target="https://tare.medisin.ntnu.no/motiflab/datasets/HOCOMOCO_v10_mouse.mlx";
//        try {
//            assertEquals(MotifLabEngine.resolveURL(new URL(original)).toString(), new URL(target).toString()); // 
//        } catch (Exception e) {
//            assertEquals("An exception occured when comparing two URLs. ",0,1);
//        };    
//    }        

    
}
