/*
 * This class can be used by operations to perform tasks in a thread pool
 * (or by other classes that could also benefit from parallel computation)
 * 
 * Normal classes should not create new task runners directly.
 * Instead, they should call engine.getTaskRunner() to get access to the common
 * task runner kept by the engine so that resources can be divided fairly.
 */
package org.motiflab.engine;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author kjetikl
 */
public class TaskRunner {
    
    private boolean initialized=false;
    private ExecutorService threadpool=null;
    private MotifLabEngine engine=null;
    private int currentThreads=0;
    
    public TaskRunner(MotifLabEngine engine) {
        this.engine=engine;
    }

    
    private void init() {
        int threads=engine.getConcurrentThreads();
        if (threads<=0) threads=1;       
        if (threadpool==null || !initialized || currentThreads!=threads) threadpool=Executors.newFixedThreadPool(threads);        
        currentThreads=threads;
        initialized=true;
    }
    
   
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        init();
        return threadpool.invokeAll(tasks);
    }

    public List<Runnable> shutdownNow() {        
        initialized=false;
        return (threadpool!=null)?threadpool.shutdownNow():null;
    }
    
}
