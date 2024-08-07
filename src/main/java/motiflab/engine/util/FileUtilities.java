
package motiflab.engine.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * This class contains some static library functions to download large (>2GB) remote files 
 * Most of this is stolen from Apache Commons-IO but I have added some functionality to be able to track progress
 * 
 * @author kjetikl
 */
public class FileUtilities {

    private static final int EOF = -1;    
    
    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a <code>BufferedInputStream</code>.
     * <p>
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @param listener A <code>PropertyChangeListener</code> that will receive periodic progress reports
     * @param cancelFlag a reference to a boolean array. The copying process can be cancelled by setting the value of the first boolean in this array to TRUE 
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs or 
     * @since 2.2
     */
    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer, PropertyChangeListener listener, boolean[] cancelFlag) throws IOException {
        if (buffer==null) buffer=new byte[16*1024]; // 16KB as default buffer	
        long count = 0;
        int n = 0;
        try {
                while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
                if (listener!=null) listener.propertyChange(new PropertyChangeEvent(input, "progress", count, count));   
                 if (cancelFlag!=null && cancelFlag.length>0 && cancelFlag[0]) throw new IOException("cancelled");
            }
            if (listener!=null) listener.propertyChange(new PropertyChangeEvent(input, "done", count, count)); 
        } catch (IOException e) {
            if (listener!=null) {
                if ("cancelled".equals(e.getMessage())) listener.propertyChange(new PropertyChangeEvent(e, "cancelled", e, e));
                else listener.propertyChange(new PropertyChangeEvent(e, "error", e, e));
            }
            throw e;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
        return count;
    }


    /** Copies a large file (possibly greater than 2GB to a destination file */
    public static void copyLargeURLToFile(URL source, File destination, PropertyChangeListener listener, boolean[] cancelFlag) throws IOException {
        URLConnection connection = source.openConnection();
        if (connection instanceof HttpURLConnection) {
            int status = ((HttpURLConnection)connection).getResponseCode();
            String location = ((HttpURLConnection)connection).getHeaderField("Location");
            if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(source.getProtocol()) && location.startsWith("https")) {
                    ((HttpURLConnection)connection).disconnect();
                    copyLargeURLToFile(new URL(location), destination, listener, cancelFlag);
                    return;
            }
        }             
        InputStream input = connection.getInputStream();
        copyLargeInputStreamToFile(input, destination, listener, cancelFlag);
    }

    public static void copyLargeURLToFile(URL source, File destination, int connectionTimeout, int readTimeout, PropertyChangeListener listener, boolean[] cancelFlag) throws IOException {
        URLConnection connection = source.openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        if (connection instanceof HttpURLConnection) {
            int status = ((HttpURLConnection)connection).getResponseCode();
            String location = ((HttpURLConnection)connection).getHeaderField("Location");
            if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(source.getProtocol()) && location.startsWith("https")) {
                    ((HttpURLConnection)connection).disconnect();
                    copyLargeURLToFile(new URL(location), destination, connectionTimeout, readTimeout, listener, cancelFlag);
                    return;
            }
        }
        InputStream input = connection.getInputStream();
        copyLargeInputStreamToFile(input, destination, listener, cancelFlag);
    }


    public static void copyLargeInputStreamToFile(InputStream source, File destination, PropertyChangeListener listener, boolean[] cancelFlag) throws IOException {
        try {
            FileOutputStream output = openOutputStream(destination, false);
            try {
                copyLarge(source, output, null, listener, cancelFlag);
                output.close(); // don't swallow close Exception if copy completes normally
            } finally {
                closeQuietly(output);
            }
        } finally {
            closeQuietly(source);
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    public static void closeQuietly(Closeable closeable) {
          try {
              if (closeable != null) {
                  closeable.close();
              }
          } catch (IOException ioe) {
              // ignore
          }
    }    
    
    public static void gunzipFile(File inputFile, File outputFile) throws IOException {
        byte[] buffer = new byte[1024];  
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(inputFile));
            FileOutputStream fos = new FileOutputStream(outputFile)) {           
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }           
        } catch (IOException ex) {
            throw ex;
        } 
    }
    
}
