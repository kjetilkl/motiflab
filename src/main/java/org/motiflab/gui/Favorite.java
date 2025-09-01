
package org.motiflab.gui;

import java.io.Serializable;

/**
 * This class represents a single entry in the Favorites list
 * @author kjetikl
 */
public class Favorite implements Serializable, Cloneable {
    private String name;
    private String type;
    private String parameter;
    private String description;

    public Favorite(String name, String type, String parameter, String description) {
        this.name=name;
        this.type=type;
        this.parameter=parameter;
        this.description=description;
    }
 
    public String getName() {return name;}
    public String getType() {return type;}
    public String getParameter() {return (parameter==null)?"":parameter;}
    public String getDescription() {return description;}
    
    public void setName(String name) {this.name=name;}
    public void setType(String type) {this.type=type;}
    public void setParameter(String parameter) {this.parameter=parameter;}
    public void setDescription(String description) {this.description=description;}

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Favorite(name, type, parameter, description);
    }
    
    
}
