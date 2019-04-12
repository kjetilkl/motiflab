/*
 * 
 */
package motiflab.engine;

/**
 * A simple I/O-console that a MotifLab client can use to communicate with the user
 * @author kjetikl
 */
public interface ClientConsole {
    
    public void print(Object obj);
    
    public void println(Object obj);
    
    public String readLine();
    
    public String readPassword();
    
}
