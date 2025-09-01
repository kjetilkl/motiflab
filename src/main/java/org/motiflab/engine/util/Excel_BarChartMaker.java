package org.motiflab.engine.util;

import java.awt.Color;
import java.util.ArrayList;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFScatterChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveFixedPercentage;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;

/**
 *
 * @author kjetikl
 */
public class Excel_BarChartMaker {
    
    private boolean setupComplete=false;
    private XSSFSheet sheet; // the sheet where the boxplot should be drawn
    private XSSFChart chart=null;
    private XDDFNumericalDataSource<Double> values=null;
    private XDDFCategoryDataSource categories=null;
    private XDDFCategoryAxis xAxis=null;
    private XDDFValueAxis yAxis=null;
    private String title="";
    private String xAxisLabel="";
    private String yAxisLabel="";
    private int datapoints=0; // Size of dataset = number of bars to draw
    private ArrayList<Object[]> extraPoints=null; // additional points to paint on top of the boxes, for instance to indicate average value
    private double[] range=new double[]{0.0,100.0};
    private int[] anchors=new int[]{0,0,10,10}; // used for placement of the plot on the sheet
    private boolean showInPercentage=false;
    private Color barColor = Color.RED;
    private Color borderColor = null;

    public enum PointStyle {CIRCLE, DASH, DIAMOND, DOT, PLUS, SQUARE, STAR, TRIANGLE, X };

    
    /**
     * Creates Bar Charts based on given values for minimum, maximum, median and 1st and 3rd quartiles
     * @param sheet The sheet where the bar chart should be drawn
     * @param col X-coordinate of the top-left corner cell (column number starting at 0)
     * @param row Y-coordinate of the top-left corner cell (row number starting at 0)
     * @param width The number of columns the chart should span
     * @param height The number of rows the chart should span 
     */
    public Excel_BarChartMaker(XSSFSheet sheet, int col, int row, int width, int height) {
        this.sheet=sheet;
        anchors[0]=col;
        anchors[1]=row;
        anchors[2]=col+width;
        anchors[3]=row+height;        
    }
    
    public void setupDataFromColumns(XSSFSheet sheet, int firstRow, int lastRow, int categoryColumn, int valueColumn) {
        datapoints=lastRow-firstRow+1;
        values = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(firstRow, lastRow, valueColumn, valueColumn));
        categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn));
    }
    
    public void setupDataFromRows(XSSFSheet sheet, int firstColumn, int lastColumn, int categoryRow, int valueRow) {
        datapoints=lastColumn-firstColumn+1; //
        values = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(valueRow, valueRow, firstColumn, lastColumn));
        categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(categoryRow, categoryRow, firstColumn, lastColumn));             
    }   
    
    public void addLabels(String title, String xAxis, String yAxis) {
        this.title=title;
        this.xAxisLabel=xAxis;
        this.yAxisLabel=yAxis;
    }
    
    public void setYrange(double minimum, double maximum) {
        range[0]=minimum;
        range[1]=maximum;
    }
    
    public void setShowInPercentage(boolean usePercentage) {
        showInPercentage=usePercentage;
    }
    
    public void setColors(Color barColor, Color borderColor) {
        this.barColor = barColor;
        this.borderColor = borderColor;
    }
    
    public void addExtraPointsFromRow(XSSFSheet sheet, int row, int firstColumn, int lastColumn, String title, PointStyle style, short size, Color color) {
        if (lastColumn-firstColumn+1!=datapoints) throw new AssertionError("Number of cells must have same size as existing dataset when adding extra points to bar chart. Expected "+datapoints+" but got "+(lastColumn-firstColumn+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(row, row, firstColumn, lastColumn));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
    }

    public void addExtraPointsFromColumn(XSSFSheet sheet, int column, int firstRow, int lastRow, String title, PointStyle style, short size, Color color) {
        if (lastRow-firstRow+1!=datapoints) throw new AssertionError("Number of cells must be same as number of groups when adding extra points to existing boxplot. Expected "+datapoints+" but got "+(lastRow-firstRow+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(firstRow, lastRow, column, column));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
    }


    
    public void drawBarChart() {
        if (values==null) throw new AssertionError("Data for barchart has not been specified"); 
        // Create the drawing patriarch
        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        // Define anchor points in the worksheet
        XSSFClientAnchor anchorPoint = drawing.createAnchor(0, 0, 0, 0, anchors[0], anchors[1], anchors[2], anchors[3]);

        // Create the chart
        chart = drawing.createChart(anchorPoint);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        // Define chart axis
        xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        xAxis.setTitle(xAxisLabel);
        yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setMinimum(range[0]);
        yAxis.setMaximum(range[1]);
        yAxis.setTitle(yAxisLabel);
        if (showInPercentage) yAxis.setNumberFormat("0%"); // this does not appear to work?

        // Create the data series
        XDDFBarChartData chartdata = (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
        chartdata.setBarDirection(BarDirection.COL);
        chartdata.setGapWidth(30); ///
      
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) chartdata.addSeries(categories, values);
        series.setTitle("Coverage", null);  
        
        // Set color for the entire series
        if (barColor!=null) {
            XDDFColor barcolor=XDDFColor.from(new byte[]{(byte)barColor.getRed(),(byte)barColor.getGreen(),(byte)barColor.getBlue()});
            XDDFSolidFillProperties fillPropertiesBox = new XDDFSolidFillProperties(barcolor);
            XDDFShapeProperties propertiesBox = new XDDFShapeProperties();
            propertiesBox.setFillProperties(fillPropertiesBox);
            if (borderColor!=null) {
                XDDFColor bcolor=XDDFColor.from(new byte[]{(byte)borderColor.getRed(),(byte)borderColor.getGreen(),(byte)borderColor.getBlue()});
                XDDFSolidFillProperties fillPropertiesBorder = new XDDFSolidFillProperties(bcolor);
                XDDFLineProperties line = new XDDFLineProperties();
                line.setFillProperties(fillPropertiesBorder);
                line.setWidth(1.0); //
                propertiesBox.setLineProperties(line);                                            
            }
            series.setShapeProperties(propertiesBox);
        }
     
        chart.plot(chartdata);
        
        // Gridlines
        XDDFShapeProperties properties = yAxis.getOrAddMajorGridProperties();
        XDDFLineProperties lineProperties = new XDDFLineProperties();
        // lineProperties.setPresetDash(DashStyle.DASH); // Set dash style
        lineProperties.setWidth(0.5); // Set line width
        lineProperties.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.LIGHT_GRAY))); // Set line color to light gray
        properties.setLineProperties(lineProperties);        
        
        // draw extra points on top (if defined)
        if (extraPoints!=null) drawExtraPoints();
         
    }
   
    
    private void drawExtraPoints() {        
        XDDFScatterChartData scatterData = (XDDFScatterChartData) chart.createData(ChartTypes.SCATTER, xAxis, yAxis);        
        for (Object[] extra:extraPoints) {
            XDDFNumericalDataSource<Double> extraValues = (XDDFNumericalDataSource<Double>)extra[0];
            String extratitle = (String)extra[1];            
            PointStyle style = (PointStyle)extra[2];
            short size = (short)extra[3];
            Color color = (Color)extra[4];

            XDDFScatterChartData.Series newSeries = (XDDFScatterChartData.Series) scatterData.addSeries(categories, extraValues);
            newSeries.setTitle(extratitle, null);
            newSeries.setMarkerStyle(getMarkerStyle(style)); // Set marker style
            newSeries.setMarkerSize(size); // Set marker size
            //newSeries.setMarkerColor(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.RED)));
            newSeries.setSmooth(false); 
            
            // set transparency on the line that is drawn between poins
            XDDFSolidFillProperties fillPropertiesTransparent = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.WHITE)); // this will be made transparent later
            CTSolidColorFillProperties ctSolidColorFillProperties = (CTSolidColorFillProperties)fillPropertiesTransparent.getXmlObject();
            CTPresetColor ctPresetColor = ctSolidColorFillProperties.getPrstClr();
            CTPositiveFixedPercentage ctPositiveFixedPercentage = ctPresetColor.addNewAlpha();
            ctPositiveFixedPercentage.setVal(0); // the value range is from 0 (transparent) to 100000 (opaque)

            XDDFShapeProperties noLineProps = new XDDFShapeProperties();
            XDDFLineProperties linePropsTransparent=new XDDFLineProperties();
            linePropsTransparent.setFillProperties(fillPropertiesTransparent);
            noLineProps.setLineProperties(linePropsTransparent);
            newSeries.setLineProperties(linePropsTransparent);            
            
            // set color on marker
            XDDFLineProperties linePropsBlack=new XDDFLineProperties();
            XDDFSolidFillProperties fillPropertiesBlack = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLACK));
            linePropsBlack.setFillProperties(fillPropertiesBlack);     
            linePropsBlack.setWidth(1.0);            
            
            XDDFColor scolor=XDDFColor.from(new byte[]{(byte)color.getRed(),(byte)color.getGreen(),(byte)color.getBlue()});
            XDDFSolidFillProperties markerColor=new XDDFSolidFillProperties(scolor);                   

            XDDFShapeProperties propertiesMarker = new XDDFShapeProperties();
            propertiesMarker.setFillProperties(markerColor);
            propertiesMarker.setLineProperties((style==PointStyle.DASH)?linePropsTransparent:linePropsBlack);
            CTPlotArea plotArea = chart.getCTChart().getPlotArea();
            CTScatterChart sc=plotArea.getScatterChartArray(0);
            CTScatterSer[] scarray=sc.getSerArray();          
            CTScatterSer sr=sc.getSerArray(scarray.length-1); // last added
            CTMarker ctmarker=sr.getMarker();
            CTShapeProperties sp=ctmarker.addNewSpPr();
            sp.set(propertiesMarker.getXmlObject()); 
            
        }
        // Plot the scatter chart on top of the box plot
        chart.plot(scatterData);
    }
    
    private MarkerStyle getMarkerStyle(String style) {
        switch (style) {
            case "circle" : return MarkerStyle.CIRCLE;
            case "dash" : return MarkerStyle.DASH;
            case "diamond" : return MarkerStyle.DIAMOND;
            case "dot" : return MarkerStyle.DOT;
            case "plus" : return MarkerStyle.PLUS;
            case "square" : return MarkerStyle.SQUARE;
            case "star" : return MarkerStyle.STAR;  
            case "triangle" : return MarkerStyle.TRIANGLE;  
            case "x" : return MarkerStyle.X;            
        }
        return MarkerStyle.NONE;                
    }
    
    private MarkerStyle getMarkerStyle(PointStyle style) {
        switch (style) {
            case CIRCLE : return MarkerStyle.CIRCLE;
            case DASH : return MarkerStyle.DASH;
            case DIAMOND : return MarkerStyle.DIAMOND;
            case DOT : return MarkerStyle.DOT;
            case PLUS : return MarkerStyle.PLUS;
            case SQUARE : return MarkerStyle.SQUARE;
            case STAR : return MarkerStyle.STAR;  
            case TRIANGLE : return MarkerStyle.TRIANGLE;  
            case X : return MarkerStyle.X;            
        }
        return MarkerStyle.NONE;                
    }    
    
}
