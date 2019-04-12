/*
 */

package motiflab.engine.data.analysis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashSet;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.OutputData;
import motiflab.engine.dataformat.DataFormat_HTML;
import motiflab.engine.dataformat.DataFormat_RawData;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author Kjetil
 */
public class AnnotatedValueRenderer extends DefaultTableCellRenderer implements Comparator<Object> {
    private Color histogramcolor=new Color(100,100,255);
    private Color selectedhistogramcolor=new Color(200,200,255);
    private VisualizationSettings settings;
    private NumberFormat normalNumberFormat=NumberFormat.getInstance();
    private NumberFormat scientificNumberFormat=new DecimalFormat("0.#####E0");
    private double[] storedhistogram=null;
    private boolean isselected=false;

    public AnnotatedValueRenderer(VisualizationSettings settings) {
        this.settings=settings;
        normalNumberFormat=NumberFormat.getInstance();
        scientificNumberFormat=new DecimalFormat("0.#####E0");
        normalNumberFormat.setMinimumFractionDigits(3);
        scientificNumberFormat.setMinimumFractionDigits(3);
        normalNumberFormat.setMaximumFractionDigits(3);
        scientificNumberFormat.setMaximumFractionDigits(3);
        histogramcolor=settings.getSystemColor("histogram");
        selectedhistogramcolor=settings.getSystemColor("histogramSelected");        
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object datacell, boolean isSelected, boolean hasFocus, int row, int column) {
        String markup=null;
        Object value=datacell;
        if (datacell instanceof AnnotatedValue) {
           value=((AnnotatedValue)datacell).getValue();
           markup=((AnnotatedValue)datacell).getMarkup();
        }
        Component c=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.isselected=isSelected;
        storedhistogram=null;
        Color bgcolor=(markup!=null)?settings.getSystemColor(markup):null;
        if (!isSelected) {
            if (bgcolor!=null) c.setBackground(bgcolor);
            else c.setBackground(table.getBackground());
        } else {
            if (bgcolor!=null) c.setBackground(bgcolor.darker().darker());
            else c.setBackground(table.getSelectionBackground());
        }
        if (value instanceof Number) {
            ((JLabel)c).setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Integer) ((JLabel)c).setText(""+((Integer)value).intValue());
            else if (value instanceof Double) ((JLabel)c).setText(formatNumber((Double)value));
        } else if (value instanceof double[]) {
            storedhistogram=(double[])value;
            setText("");
        } // this is used for histograms
        else ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
        return c;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (storedhistogram!=null) paintHistogram((Graphics2D)g,storedhistogram,this.getWidth(),this.getHeight(),(isselected)?selectedhistogramcolor:histogramcolor,null);
    }

    private void paintHistogram(Graphics2D g, double[] histogram, int width, int height, Color color, Color bgcolor) {
        if (bgcolor!=null) {
            g.setColor(bgcolor);
            g.fillRect(0, 0, width, height);            
        }
        int maxbarheight=height-4;
        AffineTransform save=g.getTransform();
        double scaleX=(double)(width-2)/(double)histogram.length;
        g.scale(scaleX, 1); // scale X-direction so that logo fits irrespective of size
        g.setColor(color);
        double max=0;
        for (int i=0;i<histogram.length;i++) if (histogram[i]>max) max=histogram[i];
        for (int i=0;i<histogram.length;i++) {
            int barheight=(int)Math.round((histogram[i]/max)*maxbarheight);
            g.fillRect(i+1, 3+maxbarheight-barheight, 1, barheight);
        }
        g.setTransform(save);
        g.setColor(Color.BLACK); // border
        g.drawRect(0, 0, width-1, height-1);
    }

    public String formatNumber(Double number) {
        if (number==null) return "";
        if (Double.isNaN(number)) return ""; //return "NaN";
        else if (number==0) return "0";
        NumberFormat formatter;
        if (Math.abs(number) < 1E-3 || Math.abs(number) > 1E3) formatter=scientificNumberFormat; // Use E+-3 since we use three decimals
        else formatter=normalNumberFormat;
        String numberString=formatter.format(number);
        return numberString;
    }

//    private void outputValue(OutputData output, Object value) throws ExecutionError {
//        
//    }

    public void outputHistogramToHTML(OutputData output, double[] histogram, MotifLabEngine engine) throws ExecutionError {
        if (histogram==null || histogram.length==0) return;
        File imagefile=output.createDependentFile(engine,"gif");
        try {
            saveHistogramImage(imagefile,histogram); //
        } catch (IOException e) {
            throw new ExecutionError("An error occurred when creating image file: "+e.toString());
        }
        output.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"/>", DataFormat_HTML.HTML);
    }

    private void saveHistogramImage(File file, double[] histogram) throws IOException {
        int width=100;
        int height=19;
        BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        paintHistogram(g, histogram, width, height, histogramcolor,Color.WHITE);
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "gif", output);
        output.close();         
        g.dispose();
    }

    public void outputHistogramToRaw(OutputData output, double[] histogram) {
        if (histogram==null || histogram.length==0) return;
        output.append(""+histogram[0], DataFormat_RawData.RAWDATA);
        for (int i=1;i<histogram.length;i++) {
           output.append(","+histogram[i], DataFormat_RawData.RAWDATA);
        }
    }
    
    public byte[] outputHistogramToByteArray(double[] histogram) throws IOException {
        int width=100;
        int height=19;
        BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        paintHistogram(g, histogram, width, height, histogramcolor, Color.WHITE);
        org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        g.dispose();     
        byte[] array=outputStream.toByteArray();
        outputStream.close();
        return array;        
    }    

    @Override
    public int compare(Object o1, Object o2) {
        Object value1=(o1 instanceof AnnotatedValue)?((AnnotatedValue)o1).getValue():o1;
        Object value2=(o2 instanceof AnnotatedValue)?((AnnotatedValue)o2).getValue():o2;
        if (value1==null && value2==null) return 0;
        else if (value1!=null && value2==null) return -1;
        else if (value1==null && value2!=null) return 1;
        if (value1 instanceof String && value2 instanceof String) return MotifLabEngine.compareNaturalOrder((String)value1, (String)value2);
        if (value1.getClass()==value2.getClass() && value1 instanceof Comparable) return ((Comparable)value1).compareTo((Comparable)value2);
        else return MotifLabEngine.compareNaturalOrder(value1.toString(),value2.toString());
    }
}