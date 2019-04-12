/*
 * MotifLabApp.java
 */

package motiflab.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.util.ArrayList;
import javax.swing.UIManager;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import motiflab.engine.MessageListener;
import motiflab.engine.MotifLab;
import motiflab.engine.MotifLabClient;
import motiflab.engine.MotifLabEngine;
import org.jdesktop.application.View;

/**
 * The main class of the application.
 * This class just starts up the engine and GUI which are covered by the classes
 * MotifLabEngine and MotifLabGUI respectively
 */
public class MotifLabApp extends SingleFrameApplication implements MessageListener {
    MotifLabClient client=null; // The GUI client to use. This could be MotifLabGUI or MinimalGUI
    Graphics2D graphics=null;
    SplashScreen splash=null;
    int i=1;
    ArrayList<String> errors=null;
    static boolean useMinimalGUI=false;
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override 
    protected void startup() {       
        splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            graphics = (Graphics2D)splash.createGraphics();
        } 
        try {
             UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        catch (Exception e) {}       
        MotifLabEngine engine=MotifLabEngine.getEngine();       
        engine.setMotifLabDirectory(getContext().getLocalStorage().getDirectory().getAbsolutePath());
        engine.addMessageListener(this);
        engine.initialize(); // imports resources etc.     
        logMessage("Launching "+((useMinimalGUI)?"Minimal GUI":"GUI"));
        client=(useMinimalGUI)?new MinimalGUI(this,engine):new MotifLabGUI(this,engine);
        engine.removeMessageListener(this);
        if (splash!=null) splash.close();
        show((View)client);
        if (client instanceof MotifLabGUI) ((MotifLabGUI)client).firstTimeStartup(); // if this is the first time MotifLab is started up, this call will install some resources (and then import them). If not, it will return right away
        if (errors!=null) { // display errors in the GUI log-panel also (in case the user did not see them when they flew by)
            client.logMessage("WARNING: The following errors occurred during initialization:");
            for (String error:errors) client.logMessage("   "+error);
            errors=null; // clear
        }
        if (client instanceof MotifLabGUI) ((MotifLabGUI)client).displayNotifications(false, false);
        if (client instanceof MinimalGUI) ((MinimalGUI)client).setupReady();
        
    }
    
    /**
     * This method is automatically called on shutdown of the Application
     */
    @Override protected void shutdown() {
        if (client instanceof MotifLabGUI) ((MotifLabGUI)client).cleanup();
        else ((MinimalGUI)client).cleanup();
        super.shutdown(); // don't forget this or session state will not be saved!!!
    }

   /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MotifLabApp
     */
    public static MotifLabApp getApplication() {
        return Application.getInstance(MotifLabApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        boolean preferences_from_file=false; 
        boolean cli_mode=false;       
        for (String arg:args) { // check which client to launch. Perhaps I should allow any client on the command line?
                 if (arg.equals("-minimal") || arg.equals("-minimalGUI")) useMinimalGUI=true;
            else if (arg.equalsIgnoreCase("-cli")) cli_mode=true;
            else if (arg.equalsIgnoreCase("-filepreferences")) preferences_from_file=true;
        }
        if (preferences_from_file) { 
            // this option can be used to store "preferences" to a regular file rather than using the default storage solution (e.g. "the registry" in Windows).
            // This may be necessary to activate if the user does not have the proper access rights
            System.setProperty("java.util.prefs.PreferencesFactory", motiflab.engine.util.FilePreferencesFactory.class.getName());
        }
        if (cli_mode) {
            MotifLab.main(args); // start the CLI-client instead (this returns when the specified protocol is finished executing)
        } else {
            // useMinimalGUI=true; // always start minimalGUI (this line is just used for debugging)
            launch(MotifLabApp.class, args);
        }
    }
   
        
    static void renderSplashFrame(Graphics2D g, int frame, String message) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(25,180,800,40);
        g.setPaintMode();
        g.setColor(Color.ORANGE);
        g.drawString(message, 25, 212);
        int length=frame*20;
        if (length>280) length=280;
        g.fillRect(25,216,length,5);
    }


        @Override
        public void logMessage(String msg, int level) {
            if (splash!=null) {
                renderSplashFrame(graphics,i++,msg);
                splash.update();
            } else {
               System.err.println(msg); 
            }
        }
        
        @Override
        public void logMessage(String msg) {
            logMessage(msg, 20);
        }        
        

        @Override
        public void statusMessage(String msg) {}    
        @Override
        public void progressReportMessage(int progress) {}          
        
        @Override
        public void errorMessage(String msg, int errortype) {
            logMessage(msg,30);
            if (errors==null) errors=new ArrayList<String>();
            errors.add(msg);
        }   
        
        public MotifLabEngine getEngine() {
            return (client!=null)?client.getEngine():null;
        }
    
}
