package org.motiflab.engine.util;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import javax.imageio.ImageIO;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.OutputDataDependency;
import org.motiflab.gui.ModuleLogo;
import org.motiflab.gui.MotifLogo;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class HTMLUtilities {
    private static final String MOTIF_LOGO_NO="No";
    private static final String MOTIF_LOGO_NEW="New images";
    private static final String MOTIF_LOGO_SHARED="Shared images";
    private static final String MOTIF_LOGO_EMBEDDED="Embedded images";
    private static final String MOTIF_LOGO_INCLUDE="Yes";    
    private static final String MOTIF_LOGO_TEXT="Text";    
    
    
/**
     * Returns a list of formats that can be used for general images
     * @return 
     */
    public static String[] getImageFormatsForHTML() {
        return new String[]{"png","svg","pdf","eps","embed"};      
    } 
    
/**
     * Returns the default image format to use for images in HTML pages
     * @return 
     */
    public static String getDefaultImageFormatForHTML() {
        return "png";
    }     

    /**
     * Returns a list of options for how to handle motif logos in HTML pages
     * @return 
     */
    public static String[] getMotifLogoOptionsForHTML() {
        return new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_EMBEDDED,MOTIF_LOGO_TEXT};        
    }
    
    /**
     * Returns the default option for how to output motif/module logos in HTML pages
     * @return 
     */
    public static String getMotifLogoDefaultOptionForHTML() {
        return MOTIF_LOGO_NO;
    }    
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output
     * either as an image or a textual representation
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutput(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_NEW) || option.equalsIgnoreCase(MOTIF_LOGO_SHARED) || option.equalsIgnoreCase(MOTIF_LOGO_INCLUDE) || option.equalsIgnoreCase(MOTIF_LOGO_EMBEDDED) || option.equalsIgnoreCase(MOTIF_LOGO_TEXT));
    }
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output
     * as regular text rather than an image
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsText(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_TEXT));
    }    
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output as an image
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsImages(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_NEW) || option.equalsIgnoreCase(MOTIF_LOGO_SHARED) || option.equalsIgnoreCase(MOTIF_LOGO_INCLUDE) || option.equalsIgnoreCase(MOTIF_LOGO_EMBEDDED));
    }
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output as a shared image
     * (i.e. the name of the image file is based on the data object, and it is not recreated if it already exists)
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsSharedImages(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_SHARED));
    }     

    /** Creates a motif logo image for the given motif, saves it to a temp-file if necessary and return a String 
     *  or IMG-tag that can be inserted in HTML-documents to display the image or a textual representation thereof
     * @param motif
     * @param outputobject
     * @param sequencelogo
     * @param logoFormat
     * @param engine
     * @return an String containing an img-tag that can be included in HTML output
     */
    
    public static String getMotifLogoTag(Motif motif, OutputData outputobject, MotifLogo sequencelogo, String logoFormat, MotifLabEngine engine) {
        int fontheight=sequencelogo.getFontHeight();
        int height=(int)(fontheight*0.8); // decide on image height that goes well with the font size. Font-size 24 leads to height 19, which is OK
        if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_NO)) return "";
        else if (motif==null) return "?";
        else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_TEXT)) return motif.getConsensusMotif();
        else { // logo as image
            VisualizationSettings settings=engine.getClient().getVisualizationSettings();
            String imageFormat=(String)settings.getSettingAsType("motif.imageFormat","gif");
            boolean border=(Boolean)settings.getSettingAsType("motif.border",Boolean.TRUE);        
            boolean scaleByIC=(Boolean)settings.getSettingAsType("motif.scaleByIC",Boolean.TRUE);                
//            height=(Integer)settings.getSettingAsType("motif.height", new Integer(19));  
            imageFormat=imageFormat.toLowerCase();
            if (!(imageFormat.equals("gif") || imageFormat.equals("png"))) imageFormat="gif";            
            sequencelogo.setMotif(motif);
            sequencelogo.setScaleByIC(scaleByIC);
            int width=sequencelogo.getDefaultMotifWidth();
            File imagefile=null;
            if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_EMBEDDED)) {
                sequencelogo.setMotif(motif);
                sequencelogo.setScaleByIC(scaleByIC); 
                try {
                    byte[] image = getMotifLogoImageAsByteArray(sequencelogo, height, border, imageFormat);
                    String base64String = Base64.getEncoder().encodeToString(image);
                    return "<img src=\"data:image/"+imageFormat+";base64,"+base64String+"\" />";
                } catch (IOException e) {
                   engine.errorMessage("An error occurred when creating image: "+e.toString(),0); 
                }
            } else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_SHARED)) {
                String logofileID=motif.getName();
                boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
                OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, imageFormat, true); // returns new or existing shared dependency
                if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                    try {
                        imagefile=new File(dependency.getInternalPathName());
                        saveMotifLogoImage(imagefile,sequencelogo, height, border, imageFormat); // 
                    } catch (IOException e) {
                        engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                    }
                } else {
                    imagefile=new File(dependency.getInternalPathName());                   
                }            
            } else { // always save any logo to a new file
                imagefile=outputobject.createDependentFile(engine,imageFormat);
                try {              
                    saveMotifLogoImage(imagefile,sequencelogo, height, border, imageFormat); // 
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }           
            }           
            // specifying the size makes it look weird in internal HTML-browser and it does not seem to make much difference in external browsers, so I will just drop it.
            // String sizeString=(width>0)?(" height="+height+" width="+width):"";
            return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\"/>";
        }
    }

    private static void saveMotifLogoImage(File file, MotifLogo sequencelogo, int motifheight, boolean border, String imageFormat) throws IOException {
        int width=sequencelogo.getDefaultMotifWidth();
        BufferedImage image=new BufferedImage(width, motifheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, motifheight);
        sequencelogo.setDrawBorder(false); // I will paint the border myself to avoid problems
        sequencelogo.paintLogo(g);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, motifheight-1);
        }
        if (imageFormat==null) imageFormat="gif";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png"))) imageFormat="gif";
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, imageFormat, output);
        output.close();               
        g.dispose();
    }

    public static byte[] getMotifLogoImageAsByteArray(MotifLogo sequencelogo, int motifheight, boolean border, String imageFormat) throws IOException {
        int width=sequencelogo.getDefaultMotifWidth();
        BufferedImage image=new BufferedImage(width, motifheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, motifheight);
        sequencelogo.setDrawBorder(false); // I will paint the border myself to avoid problems
        sequencelogo.paintLogo(g);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, motifheight-1);
        }
        org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, outputStream);
        g.dispose();     
        byte[] array=outputStream.toByteArray();
        outputStream.close();
        return array;
    }
    
    public static byte[] getModuleLogoImageAsByteArray(ModuleLogo modulelogo, int moduleheight, boolean border, String imageFormat) throws IOException {
        Font modulemotiffont=ModuleLogo.getModuleMotifFont();
        BufferedImage test=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics modulelogometrics=test.getGraphics().getFontMetrics(modulemotiffont);
        int width=ModuleLogo.getLogoWidth(modulelogometrics,modulelogo.getModule())+2;
        BufferedImage image=new BufferedImage(width, moduleheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0)); // make the image translucent white       
        g.clearRect(0,0, width+2, moduleheight+2); // bleed a little just in case
        modulelogo.paintModuleLogo(g,0,0);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, moduleheight-1);
        }
        org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, outputStream);
        g.dispose();     
        byte[] array=outputStream.toByteArray();
        outputStream.close();
        return array;
    }    
    
    public static String getModuleLogoTag(ModuleCRM cisRegModule, OutputData outputobject, ModuleLogo modulelogo, String logoFormat, MotifLabEngine engine) {
        if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_NO)) return "";
        else if (cisRegModule==null) return "?";        
        else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_TEXT)) return escapeHTML(cisRegModule.getModuleLogo());
        else { // logo as image
            VisualizationSettings settings=engine.getClient().getVisualizationSettings();
            String imageFormat=(String)settings.getSettingAsType("module.imageFormat","png");
            boolean border=(Boolean)settings.getSettingAsType("module.border",Boolean.FALSE);            
            int height = (Integer)settings.getSettingAsType("module.height",28); // I think this will be OK...
            imageFormat=imageFormat.toLowerCase();
            if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="png";            
            int width=0;
            File imagefile=null;
            if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_EMBEDDED)) {
                try {
                    if (modulelogo==null) modulelogo=new ModuleLogo(engine.getClient().getVisualizationSettings());
                    modulelogo.setModule(cisRegModule);
                    byte[] image = getModuleLogoImageAsByteArray(modulelogo, height, border, imageFormat);
                    String base64String = Base64.getEncoder().encodeToString(image);
                    return "<img src=\"data:image/"+imageFormat+";base64,"+base64String+"\" />";
                } catch (IOException e) {
                   engine.errorMessage("An error occurred when creating image: "+e.toString(),0); 
                }
            } else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_SHARED)) {
                String logofileID=cisRegModule.getName();
                boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
                OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, imageFormat,true); // returns new or existing shared dependency
                if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                    imagefile=dependency.getFile();
                    try {
                        int[] size=saveModuleLogoImage(imagefile,cisRegModule,engine.getClient().getVisualizationSettings(), height, border, imageFormat); //
                        height=size[0];
                        width=size[1];
                    } catch (IOException e) {
                        engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                    }
                } else {
                    imagefile=new File(dependency.getInternalPathName());
                }
            } else { // always save any logo to a new file
                imagefile=outputobject.createDependentFile(engine,imageFormat);
                try {
                    int[] size=saveModuleLogoImage(imagefile,cisRegModule,engine.getClient().getVisualizationSettings(), height, border, imageFormat); //
                    height=size[0];
                    width=size[1];                
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }
            }     
            // specifying the size makes it look weird in internal HTML-browser and it does not seem to make much difference in external browsers, so I will just drop it.
            // String sizeString=(width>0)?(" height="+height+" width="+width):"";
            return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\"/>";            
        }
                    
    }
            
    private static int[] saveModuleLogoImage(File file, ModuleCRM cisRegModule, VisualizationSettings settings, int height, boolean border, String imageFormat) throws IOException {
        Font modulemotiffont=ModuleLogo.getModuleMotifFont();
        BufferedImage test=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics modulelogometrics=test.getGraphics().getFontMetrics(modulemotiffont);
        int width=ModuleLogo.getLogoWidth(modulelogometrics,cisRegModule)+2;
        if (imageFormat==null) imageFormat="png";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="png";            
        
        if (imageFormat.equals("gif") || imageFormat.equals("png")) {
            BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            g.setBackground(new Color(255, 255, 255, 0)); // make the image translucent white       
            g.clearRect(0,0, width+2, height+2); // bleed a little just in case
            ModuleLogo.paintModuleLogo(g, cisRegModule, 5, 7, settings, null,0); //
            if (border) {
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, width-1, height-1);
            }            
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, imageFormat, output);
            output.close(); 
            g.dispose();
            return new int[]{height,width};         
        } else {                     
            VectorGraphics2D g=null;
                 if (imageFormat.equals("svg")) g = new SVGGraphics2D(0, 0, width, height);
            else if (imageFormat.equals("pdf")) g = new PDFGraphics2D(0, 0, width, height);
            else if (imageFormat.equals("eps")) g = new EPSGraphics2D(0, 0, width, height);
            else throw new IOException("Unknown image format: "+imageFormat);
            g.setClip(0, 0, width,height);
            ModuleLogo.paintModuleLogo(g, cisRegModule, 5, 7, settings, null,0); //    
            if (border) {
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, width-1, height-1);
            }              
            FileOutputStream fileStream = new FileOutputStream(file);
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();  
            return new int[]{height,width};     
        }                       
        
    }

    /**
     * Returns an HTML img tag with the image from the given file
     * embedded in Base64 encoding directly in the tag
     * @param file the image file
     * @param height a height attribute to set in the tag (a value of 0 sets no height)
     * @param width a width attribute to set in the tag (a value of 0 sets no width)
     * @return 
     */
    public static String getEmbeddedTagForImage(File file, int height, int width) {
        try {
            String filename = file.getName();
            String imageFormat = filename.substring(filename.lastIndexOf('.')+1);
            byte[] image = imageFileToByteArray(file,imageFormat);
            String base64String = Base64.getEncoder().encodeToString(image);
            String sizeString="";
            if (height>0) sizeString="height="+height;
            if (width>0) sizeString=sizeString+" width="+width;            
            return "<img src=\"data:image/"+imageFormat+";base64,"+base64String+"\" "+sizeString+" />";
        } catch (IOException e) {
            return "<div style='color:red'>Embedded image error</div>";
        }        
    }
    
    public static byte[] imageFileToByteArray(File imageFile, String formatName) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageFile); // Read the image into a BufferedImage
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream(); // Create a ByteArrayOutputStream
        ImageIO.write(bufferedImage, formatName, outputstream); // Write the BufferedImage to the ByteArrayOutputStream
        return outputstream.toByteArray(); // Get the byte array from the ByteArrayOutputStream
    }    
    

    /**
     * ImagePainters are used in combination with getImageTag to draw images
     * that can be included in HTML documents. These will either be saved
     * to external files or be embedded in the tag itself.
     */
    public interface ImagePainter {
        
        /**
         * A callback method used by getImageTag to draw into a graphics object
         * @param g 
         */
        void paint(Graphics2D g);     
    }
    
    /**
     * Returns an HTML "img" tag referencing an image painted by an ImagePainter 
     * The tag can then be inserted into an HTML document to display the image in a browser
     * @param painter An object that will paint the image into a Graphics context
     * @param file A file to store the image to (For non-embedded images, this should be an OutputDependency file)
     * @param imageformat supported formats are: png, embed (png), svg, pdf and eps
     * @param height original height of image assumed by the painter (unscaled) 
     * @param width original width of image assumed by the painter (unscaled)
     * @param scale
     * @return an HTML "img" tag for the painted image (or a tag containing an error)
     */
    public static String getImageTag(ImagePainter painter, File file, String imageformat, int height, int width, double scale) {
        double scaledWidth=Math.ceil(width*scale);
        double scaledHeight=Math.ceil(height*scale);
        
        if (imageformat.equals("svg") || imageformat.equals("pdf") || imageformat.equals("eps")) {
            VectorGraphics2D g=null;
                 if (imageformat.equals("svg")) g = new SVGGraphics2D(0, 0, scaledWidth, scaledHeight);
            else if (imageformat.equals("pdf")) g = new PDFGraphics2D(0, 0, scaledWidth, scaledHeight);
            else if (imageformat.equals("eps")) g = new EPSGraphics2D(0, 0, scaledWidth, scaledHeight);
            g.setClip(0, 0, (int)scaledWidth,(int)scaledHeight);
            painter.paint(g);                             
            try (FileOutputStream fileStream = new FileOutputStream(file)) {
                fileStream.write(g.getBytes());
            } catch (IOException e) {
                return "<div style='color:red'>Image error: "+e.getMessage()+"</div>";
            }
            g.dispose(); 
            if (imageformat.equals("pdf")) return "<object type=\"application/pdf\" data=\"file:///"+file.getAbsolutePath()+"\" height="+(int)scaledHeight+" width="+(int)scaledWidth+"></object>";
            else {
                return "<img src=\"file:///"+file.getAbsolutePath()+"\" height="+(int)scaledHeight+" width="+(int)scaledWidth+" />";                      
            }                        
        } else if (imageformat.startsWith("embed")) {
            BufferedImage image=new BufferedImage((int)scaledWidth,(int)scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();  
            painter.paint(g);
            try {
                ByteArrayOutputStream outputstream = new ByteArrayOutputStream(); 
                ImageIO.write(image, "png", outputstream);
                byte[] imageBytes=outputstream.toByteArray();
                StringBuilder builder = new StringBuilder();
                builder.append("<img src=\"data:image/png;base64,");
                builder.append(Base64.getEncoder().encodeToString(imageBytes));
                builder.append("\"");
                if (height>0) {builder.append(" height="); builder.append((int)scaledHeight);}
                if (width>0) {builder.append(" width="); builder.append((int)scaledWidth);}  
                builder.append(" />");
                return builder.toString();
            } catch (IOException e) {
                return "<div style='color:red'>Embedded image error: "+e.getMessage()+"</div>";
            } finally {
                g.dispose();
            }
        } else { // png 
            BufferedImage image=new BufferedImage((int)scaledWidth,(int)scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();  
            painter.paint(g);
            try (OutputStream output = MotifLabEngine.getOutputStreamForFile(file)) {
                ImageIO.write(image, "png", output);
            } catch (IOException e) {
                return "<div style='color:red'>Image error: "+e.getMessage()+"</div>";
            }
            g.dispose(); 
            return "<img src=\"file:///"+file.getAbsolutePath()+"\" height="+(int)scaledHeight+" width="+(int)scaledWidth+" />";
        }         
    }    
    
    
    public static String escapeHTML(String string) {
        if (string==null) return "";
        if (string.contains("&")) string=string.replace("&", "&amp;"); // this must be first
        if (string.contains("<")) string=string.replace("<", "&lt;");
        if (string.contains(">")) string=string.replace(">", "&gt;");
        return string;
    }        

    
}
