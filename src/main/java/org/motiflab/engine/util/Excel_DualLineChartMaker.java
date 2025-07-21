package org.motiflab.engine.util;

import java.awt.Color;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrossBetween;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickMark;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 *
 * @author kjetikl
 */
public class Excel_DualLineChartMaker {
    
    private XSSFSheet sheet; // the sheet where this chart should be drawn
    private XDDFNumericalDataSource<Double> values1 = null;
    private XDDFNumericalDataSource<Double> values2 = null;    
    private String title = null;    
    private String xAxisLabel = null;
    private String yAxisLabel_left = null;    
    private String yAxisLabel_right = null;      
    private int datapoints = 0;
    private int[] anchors=new int[]{0,0,10,10}; // used for placement of the plot on the sheet
    private Color color1 = Color.RED;
    private Color color2 = Color.BLUE;
    private String seriesname1 = "series 1";
    private String seriesname2 = "series 2";    

    
    /**
     * Creates pie chart with the given dimensions
     * @param sheet The sheet where the pie chart should be drawn
     */
    public Excel_DualLineChartMaker(XSSFSheet sheet, int col, int row, int width, int height) {
        this.sheet = sheet;
        anchors[0] = col;
        anchors[1] = row;
        anchors[2] = col+width;
        anchors[3] = row+height;        
    }
    
  
    public void addDataFromRows(XSSFSheet datasheet, int row1, int row2, int firstColumn, int lastColumn) {
        values1 = XDDFDataSourcesFactory.fromNumericCellRange(datasheet,new CellRangeAddress(row1, row1, firstColumn, lastColumn));
        values2 = XDDFDataSourcesFactory.fromNumericCellRange(datasheet,new CellRangeAddress(row2, row2, firstColumn, lastColumn));      
        datapoints = lastColumn-firstColumn+1;
    }
    
    public void addDataFromColumns(XSSFSheet datasheet, int column1, int column2, int firstRow, int lastRow) {
        values1 = XDDFDataSourcesFactory.fromNumericCellRange(datasheet, new CellRangeAddress(firstRow, lastRow, column1, column1));
        values2 = XDDFDataSourcesFactory.fromNumericCellRange(datasheet, new CellRangeAddress(firstRow, lastRow, column2, column2));        
        datapoints = lastRow-firstRow+1;        
    }
    
    public void setColors(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;        
    }
    
    public void setSeriesNames(String series1, String series2) {
        this.seriesname1 = series1;
        this.seriesname2 = series2;        
    }  
    
    public void setLabels(String title, String xAxis, String yAxis1, String yAxis2) {
        this.title=title;
        this.xAxisLabel=xAxis;
        this.yAxisLabel_left=yAxis1;
        this.yAxisLabel_right=yAxis2;        
    }       
    
    public void drawLineChart() {
        if (values1==null || values2==null) throw new AssertionError("Missing data for lines in Dual Line chart");       
        // Create the chart
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchors[0], anchors[1], anchors[2], anchors[3]);
        XSSFChart chart = drawing.createChart(anchor);

        String[] xValues = new String[datapoints];
        XDDFCategoryDataSource xAxisData = XDDFDataSourcesFactory.fromArray(xValues);
        
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setMajorTickMark(AxisTickMark.NONE);
        bottomAxis.setMinorTickMark(AxisTickMark.NONE);     
        if (xAxisLabel!=null) bottomAxis.setTitle(xAxisLabel);

        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        if (yAxisLabel_left!=null) leftAxis.setTitle(yAxisLabel_left);     
        
              
        XDDFChartData data = chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFChartData.Series series1 = data.addSeries(xAxisData, values1);
        if (seriesname1!=null) series1.setTitle(seriesname1, null);

        // Plot the first series
        chart.plot(data);


        XDDFValueAxis rightAxis = chart.createValueAxis(AxisPosition.RIGHT);
        if (yAxisLabel_right!=null) rightAxis.setTitle(yAxisLabel_right); 
        rightAxis.setCrosses(AxisCrosses.MAX);   
        rightAxis.setCrossBetween(AxisCrossBetween.BETWEEN);
        leftAxis.crossAxis(bottomAxis);
        rightAxis.crossAxis(bottomAxis);
        
        // Create a secondary axis for the second series
        XDDFChartData data2 = chart.createData(ChartTypes.LINE, bottomAxis, rightAxis);
        XDDFChartData.Series series2 = data2.addSeries(xAxisData, values2);
        if (seriesname2!=null) series2.setTitle(seriesname2, null);

        // Plot the second series on the secondary axis
        chart.plot(data2);

        if (title!=null) {
            chart.setTitleText(title);
            chart.setTitleOverlay(false);
        }
        
        XDDFColor scolor1=XDDFColor.from(new byte[]{(byte)color1.getRed(),(byte)color1.getGreen(),(byte)color1.getBlue()});
        XDDFColor scolor2=XDDFColor.from(new byte[]{(byte)color2.getRed(),(byte)color2.getGreen(),(byte)color2.getBlue()});
        XDDFSolidFillProperties fill1 = new XDDFSolidFillProperties(scolor1);
        XDDFSolidFillProperties fill2 = new XDDFSolidFillProperties(scolor2);
        XDDFLineProperties line1 = new XDDFLineProperties();
        XDDFLineProperties line2 = new XDDFLineProperties();
        line1.setFillProperties(fill1);
        line2.setFillProperties(fill2);
        line1.setWidth(1.5);
        line2.setWidth(1.5);        
        series1.setLineProperties(line1);
        series2.setLineProperties(line2);
        ((XDDFLineChartData.Series)series1).setMarkerStyle(MarkerStyle.NONE);
        ((XDDFLineChartData.Series)series2).setMarkerStyle(MarkerStyle.NONE); 
        ((XDDFLineChartData.Series)series1).setSmooth(false);
        ((XDDFLineChartData.Series)series2).setSmooth(false);           
        
        // Get the text body of the axis
        XDDFRunProperties leftAxisProperties = leftAxis.getOrAddTextProperties();
        leftAxisProperties.setFillProperties(fill1);
        
        XDDFRunProperties rightAxisProperties = rightAxis.getOrAddTextProperties();
        rightAxisProperties.setFillProperties(fill2);
        
        // Add a legend
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);        
    }
    
    
 
}
