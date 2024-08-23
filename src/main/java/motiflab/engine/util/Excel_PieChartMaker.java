package motiflab.engine.util;

import java.awt.Color;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFPieChartData;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDPt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;

/**
 *
 * @author kjetikl
 */
public class Excel_PieChartMaker {
    
    private XSSFSheet sheet; // the sheet where the pie chart should be drawn
    private XDDFNumericalDataSource<Double> values = null;
    private Color[] colors = null;
    private String[] labels = null;
    private String title = null;
    private int[] anchors=new int[]{0,0,10,10}; // used for placement of the plot on the sheet

    
    /**
     * Creates pie chart with the given dimensions
     * @param sheet The sheet where the pie chart should be drawn
     */
    public Excel_PieChartMaker(XSSFSheet sheet, int col, int row, int width, int height) {
        this.sheet = sheet;
        anchors[0] = col;
        anchors[1] = row;
        anchors[2] = col+width;
        anchors[3] = row+height;        
    }
    
    public void setupDataFromRow(int row, int firstColumn, int lastColumn, String[] labels, Color[] colors) {
        if (labels!=null && labels.length!=(lastColumn-firstColumn+1)) throw new AssertionError("Number of category labels in pie chart "+(labels.length)+" is different from number of values "+(lastColumn-firstColumn+1));
        if (colors!=null && colors.length!=(lastColumn-firstColumn+1)) throw new AssertionError("Number of colors in pie chart "+(labels.length)+" is different from number of values "+(lastColumn-firstColumn+1));
        this.labels = labels;
        this.colors = colors;
        values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,new CellRangeAddress(row, row, firstColumn, lastColumn));
    }
    
    public void setupDataFromColumn(int column, int firstRow, int lastRow, String[] labels, Color[] colors) {
        if (labels!=null && labels.length!=(lastRow-firstRow+1)) throw new AssertionError("Number of category labels in pie chart "+(labels.length)+" is different from number of values "+(lastRow-firstRow+1));
        if (colors!=null && colors.length!=(lastRow-firstRow+1)) throw new AssertionError("Number of colors in pie chart "+(labels.length)+" is different from number of values "+(lastRow-firstRow+1));
        this.labels = labels;
        this.colors = colors;      
        values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,new CellRangeAddress(firstRow, lastRow, column, column));
    }

    /**
     * 
     * @param cells a list of individual cells specified as int[2] pairs of row and column
     * @param labels
     * @param colors 
     */
    public void setupDataFromCells(int[][] cells, String[] labels, Color[] colors) {
        if (labels!=null && labels.length!=cells.length) throw new AssertionError("Number of category labels in pie chart "+(labels.length)+" is different from number of values "+cells.length);
        if (colors!=null && colors.length!=cells.length) throw new AssertionError("Number of colors in pie chart "+(labels.length)+" is different from number of values "+cells.length);
        this.labels = labels;
        this.colors = colors;      
        // ArrayList<XDDFNumericalDataSource<Double>> list = new ArrayList<>(cells.length);
        Double[] dvalues = new Double[cells.length];
        int index=0;
        for (int[] cell:cells) {
            int row = cell[0];
            int col = cell[1];
            XDDFNumericalDataSource<Double> singleValue = XDDFDataSourcesFactory.fromNumericCellRange(sheet,new CellRangeAddress(row, row, col, col));
            dvalues[index] = singleValue.getPointAt(0);
            index++;
        }
        values = XDDFDataSourcesFactory.fromArray(dvalues);        
    }     
    
    public void drawPieChart() {
        if (values==null) throw new AssertionError("Data for pie chart has not been specified"); 

        // Create a drawing canvas on the sheet
        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        // Define the anchor for the chart position
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchors[0], anchors[1], anchors[2], anchors[3]);

        // Create the chart
        XSSFChart chart = drawing.createChart(anchor);
        if (title!=null) {
            chart.setTitleText(title);
            chart.setTitleOverlay(false);
        }

        // Define the data sources for the chart
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromArray(labels);
        
        // Create the pie chart data
        XDDFPieChartData pieChartData = (XDDFPieChartData) chart.createData(ChartTypes.PIE, null, null);
        pieChartData.setFirstSliceAngle(270);

        // Add the series to the chart
        XDDFPieChartData.Series series = (XDDFPieChartData.Series) pieChartData.addSeries(categories, values);
        // series.setTitle("Categories", null); // optional: set a title for the series
        pieChartData.setVaryColors(true);

        for (int i=0;i<colors.length;i++) {
            setSliceColorAndBorder(series, i, colors[i]); // new XDDFColorRgbBinary(new byte[]{(byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue()}));          
        }          

        // Move legend to the right
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);        
        
        // Plot the chart with the data
        chart.plot(pieChartData);                  
    }
    
    // Helper method to set slice color and border
    private static void setSliceColorAndBorder(XDDFPieChartData.Series series, int index, Color color) {
        CTDPt ctDPt = series.getCTPieSer().addNewDPt();
        ctDPt.addNewIdx().setVal(index);

        byte[] colorBytes = new byte[]{(byte)color.getRed(),(byte)color.getGreen(),(byte)color.getBlue()};
        // Set fill color
        ctDPt.addNewSpPr().addNewSolidFill().addNewSrgbClr().setVal(colorBytes);

        // Add black border
        CTLineProperties ctLineProperties = ctDPt.getSpPr().addNewLn();
        ctLineProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{0, 0, 0}); // Black color
        ctLineProperties.setW(9525); // Width of the line (1 point = 9525 units)
    }
 
}
