package org.motiflab.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

/**
 * Custom ObjectInputStream to handle renamed classes and dynamic class loading.
 */
public class DynamicClassLoaderObjectInputStream extends ObjectInputStream {

    private MotifLabEngine engine;

    public DynamicClassLoaderObjectInputStream(InputStream inputStream, MotifLabEngine engine)
            throws IOException, StreamCorruptedException {
        super(inputStream);
        this.engine = engine;
        this.enableResolveObject(true); // allow resolveObject to be called
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();
        // Remap old MotifLab classes to new package structure
        if (className.startsWith("motiflab.")) {
            String remappedName = "org." + className;
            try {
                return Class.forName(remappedName, false, engine.getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                // Fall through to plugin loader or default
            }
        }
        // Attempt to load class via plugin mechanism
        Class<?> pluginClass = engine.getPluginClassForName(className);
        if (pluginClass != null) {
            return pluginClass;
        }
        // Default resolution
        return super.resolveClass(desc);
    }

    @Override
    protected Object resolveObject(Object obj) throws IOException {
        if (obj instanceof org.motiflab.engine.data.Module) {
            try {
                return org.motiflab.engine.data.ModuleCRM.fromLegacy(obj);
            } catch (Exception e) {
                throw new IOException("Failed to convert legacy Module data", e);
            }
        }
        return super.resolveObject(obj);
    }
    
}
