
package motiflab.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;

/**
 * This class contains functionality to access the "Favorites" data repository.
 * This is just the model/controller part. The GUI is provided by the FavoritesDialog class
 * @author kjetikl
 */
public class Favorites {
    private ArrayList<Favorite> favorites=new ArrayList<Favorite>();
    private boolean isDirty=false; // this will be set to true if unsaved changes have been made to the repository
    private File favoritesFile=null;
    
    public Favorites() {
        
    }
    
    public Favorites(File file) {
       favoritesFile=file; 
       try {
           loadFavoritesFromFile(favoritesFile);
       } catch (Exception e) {}
    }    
    
    public void setRepositoryFile(File file) {
       favoritesFile=file; 
    }
        
    public ArrayList<Favorite> getFavorites() {
        return (ArrayList<Favorite>)favorites.clone();
    }
    
    public void addFavorite(Favorite favorite) {
        favorites.add(favorite);
        isDirty=true;
    }
    
    public void addFavorite(Data dataitem, MotifLabEngine engine) {
        if (dataitem==null) return;
        String parameter=dataitem.getValueAsParameterString();
        String type=engine.getTypeNameForDataClass(dataitem.getClass());
        Favorite favorite=new Favorite(dataitem.getName(), type, parameter, "");
        favorites.add(favorite);
        isDirty=true;
    }    
    
    public void removeFavorite(Favorite favorite) {
        favorites.remove(favorite);
        isDirty=true;       
    }
    public boolean removeFavorite(String name) {
        Favorite fav=getFavoriteByName(name);
        if (fav!=null) {
            favorites.remove(fav);
            isDirty=true;   
            return true;
        } else return false;
    }    
    
    public void replaceFavorite(Favorite oldfavorite, Favorite newfavorite) {
        int index=favorites.indexOf(oldfavorite);
        if (index>=0 && index<=favorites.size()) {
            favorites.remove(oldfavorite);
            favorites.add(index, newfavorite);
        }
        else favorites.add(newfavorite);
        isDirty=true;
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public Favorite getFavoriteByName(String name) {
        for (Favorite fav:favorites) {
            if (fav.getName().equals(name)) return fav;
        }
        return null;
    }

    public boolean hasFavorite(String name) {
        for (Favorite fav:favorites) {
            if (fav.getName().equals(name)) return true;
        }
        return false;
    }    
    
    public void saveUpdates() throws Exception {
        if (!isDirty || favoritesFile==null) return; // don't bother to save repository if no updates have been made
        saveFavoritesToFile(favoritesFile);
    }
    
    private boolean loadFavoritesFromFile(Object source) throws Exception {
        ObjectInputStream stream=null;
        try {
             InputStream input=MotifLabEngine.getInputStreamForDataSource(source);
             if (input==null) return false; // do not attempt to load
             stream=new ObjectInputStream(new BufferedInputStream(input));
             Object value=stream.readObject();
             stream.close();
             if (value instanceof ArrayList) favorites=(ArrayList<Favorite>)value;   
             return true;
        } catch (Exception e) {
             throw e;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}        
    }
   
    private void saveFavoritesToFile(File file) throws Exception {
        ObjectOutputStream stream=null;
        try { 
             OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
             stream = new ObjectOutputStream(new BufferedOutputStream(output));
             stream.writeObject(favorites);
             stream.close();         
             isDirty=false;              
        } catch (Exception e) {
             throw e;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}
    }   
}
