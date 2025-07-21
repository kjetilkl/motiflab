package org.motiflab.engine.util;

import java.awt.Color;
import java.util.ArrayList;
import org.motiflab.engine.data.analysis.Analysis;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFScatterChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTErrBars;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTErrValType;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.STErrBarType;
import org.openxmlformats.schemas.drawingml.x2006.chart.STErrDir;
import org.openxmlformats.schemas.drawingml.x2006.chart.STErrValType;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveFixedPercentage;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;

/**
 *
 * @author kjetikl
 */
public class Excel_BoxplotMaker {
    
    private boolean setupComplete=false;
    private XSSFSheet sheet; // the sheet where the boxplot should be drawn
    private XSSFSheet doodlesheet; // extra sheet used for intermediate calculations
    private XSSFChart chart=null;
    private XDDFDataSource<String> groups=null;
    private XDDFCategoryAxis bottomAxis=null;
    private XDDFValueAxis leftAxis=null;
    private int groupCount=0; // number of boxplots to make = number of data rows (excluding padding rows)
    private int headerRows=2; // number of rows used for headers above the data. Final header row contains column titles
    private String title="";
    private String xAxisLabel="";
    private String yAxisLabel="";
    private ArrayList<Object[]> extraPoints=null; // additional points to paint on top of the boxes, for instance to indicate average value
    private int extraPointsColumn = 6;
    private double[] range=new double[]{0.0,100.0};
    private int[] anchors=new int[]{0,0,10,10}; // used for placement of the plot on the sheet
    private boolean showInPercentage=false;

    public enum PointStyle {CIRCLE, DASH, DIAMOND, DOT, PLUS, SQUARE, STAR, TRIANGLE, X };

    
    /**
     * Creates boxplots (with whiskers) based on given values for minimum, maximum, median and 1st and 3rd quartiles
     * @param sheet The sheet where the boxplots should be drawn
     * @param doodlesheet An extra sheet that can be used for intermediate calculations
     */
    public Excel_BoxplotMaker(XSSFSheet sheet, XSSFSheet doodlesheet, int col, int row, int width, int height, boolean showInPercentage) {
        this.sheet=sheet;
        this.doodlesheet=doodlesheet;
        anchors[0]=col;
        anchors[1]=row;
        anchors[2]=col+width;
        anchors[3]=row+height;
        this.showInPercentage = showInPercentage;
    }
    
    public void setupDataFromColumns(int firstRow, int lastRow, int groupColumn, int minColumn, int maxColumn, int medianColumn, int firstQuartileColumn, int thirdQuartileColumnm) {
        int numRows=lastRow-firstRow+1;
        int startRow=headerRows+1; // accounts for 2 rows of headers and a padding row in doodle sheet
        addRow(doodlesheet, 0, new Object[]{"This sheet contains data needed to create the boxplots on the "+sheet.getSheetName()+" sheet"});
        addRow(doodlesheet, 1, new Object[]{"  ","Bottom", "2Q box", "3Q box", "Whisker-", "Whisker+"});
        addRow(doodlesheet, 2, new Object[]{"  ", 0, 0, 0, 0, 0}); // this extra row is used to create some padding in the boxplot  
        Object[] values=new Object[6];
        for (int i=0;i<numRows;i++) {
             int sourceRowIndex = firstRow+i;
             int targetRowIndex = startRow+i;   
             XSSFRow sourceRow = sheet.getRow(sourceRowIndex);
             String group = sourceRow.getCell(groupColumn).getStringCellValue();
             double min = sourceRow.getCell(minColumn).getNumericCellValue();
             double max = sourceRow.getCell(maxColumn).getNumericCellValue();
             double median = sourceRow.getCell(medianColumn).getNumericCellValue();
             double firstQ = sourceRow.getCell(firstQuartileColumn).getNumericCellValue();
             double thirdQ = sourceRow.getCell(thirdQuartileColumnm).getNumericCellValue();             
             values[0] = group;             
             values[1] = firstQ; // Bottom
             values[2] = (median-firstQ); // 2Q box
             values[3] = (thirdQ-median); // 3Q box
             values[4] = (firstQ-min); // whisker-
             values[5] = (max-thirdQ); // whisker+                       
             addRow(doodlesheet, targetRowIndex, values);
        }
        addRow(doodlesheet, startRow+numRows, new Object[]{"  ",0, 0, 0, 0, 0}); // this extra row is used to create some padding in the boxplot               
        groupCount=numRows;
        setupComplete = true;
    }
    
    public void setupDataFromRows(int firstColumn, int lastColumn, int groupRow, int minRow, int maxRow, int medianRow, int firstQuartileRow, int thirdQuartileRow) {
        int numRows=lastColumn-firstColumn+1; // number of rows in doodle sheet
        int startRow=headerRows+1; // accounts for 2 rows of headers and a padding row
        addRow(doodlesheet, 0, new Object[]{"This sheet contains data needed to create boxplots"});
        addRow(doodlesheet, 1, new Object[]{"  ","Bottom", "2Q box", "3Q box", "Whisker-", "Whisker+"});
        addRow(doodlesheet, 2, new Object[]{"  ", 0, 0, 0, 0, 0}); // this extra row is used to create some padding in the boxplot
        Object[] values=new Object[6];
        for (int i=0;i<numRows;i++) {
             int sourceColumnIndex=firstColumn+i;
             int targetRowIndex=startRow+i;   
             String group  = sheet.getRow(groupRow).getCell(sourceColumnIndex).getStringCellValue();
             double min    = sheet.getRow(minRow).getCell(sourceColumnIndex).getNumericCellValue();
             double max    = sheet.getRow(maxRow).getCell(sourceColumnIndex).getNumericCellValue();
             double median = sheet.getRow(medianRow).getCell(sourceColumnIndex).getNumericCellValue();
             double firstQ = sheet.getRow(firstQuartileRow).getCell(sourceColumnIndex).getNumericCellValue();
             double thirdQ = sheet.getRow(thirdQuartileRow).getCell(sourceColumnIndex).getNumericCellValue();            
             values[0] = group;             
             values[1] = (firstQ); // Bottom
             values[2] = (median-firstQ); // 2Q box
             values[3] = (thirdQ-median); // 3Q box
             values[4] = (firstQ-min); // whisker-
             values[5] = (max-thirdQ); // whisker+                       
             addRow(doodlesheet, targetRowIndex, values);             
        }
        addRow(doodlesheet, startRow+numRows, new Object[]{"  ", 0, 0, 0, 0, 0}); // this extra row is used to create some padding in the boxplot    
        groupCount=numRows;        
        setupComplete = true;               
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
    
    private void formatAsPercentage() {
        XSSFWorkbook workbook = doodlesheet.getWorkbook();
        CellStyle noDecimalsPercentage = workbook.createCellStyle();    
        noDecimalsPercentage.setDataFormat(workbook.createDataFormat().getFormat("0%"));
        // this following a hack to format the Doodle table with percentage styles because the axis.setNumberFormat("0%") does not seem to work
        int totalRows=groupCount+2;
        int totalColumns=5+extraPoints.size();
        for (int rowIndex=headerRows;rowIndex<totalRows+headerRows;rowIndex++) {
           Row row = doodlesheet.getRow(rowIndex);
           for (int col=1;col<=totalColumns;col++) {
               row.getCell(col).setCellStyle(noDecimalsPercentage);
           }
       }       
    }
    
    public void addExtraPointsFromRow(int row, int firstColumn, int lastColumn, String title, PointStyle style, short size, Color color) {
        if (lastColumn-firstColumn+1!=groupCount) throw new AssertionError("Number of cells must be same as number of groups when adding extra points to existing boxplot. Expected "+groupCount+" but got "+(lastColumn-firstColumn+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        Object[] values = new Object[groupCount];
        for (int i=0;i<groupCount;i++) {
            Double value = sheet.getRow(row).getCell(firstColumn+i).getNumericCellValue();
            values[i]=value;
        }
        addExtraColumn(extraPointsColumn, title, values);
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(headerRows, headerRows+values.length+2, extraPointsColumn, extraPointsColumn));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
        extraPointsColumn++; // make room for more columns
    }
    /**
     * Adds a new set of points on top of the boxplots where the values are based on the sum of two values on different rows.
     * @param row1
     * @param row2
     * @param subtract If true, the second value will be negated and thus subtracted from the first value instead.
     * @param firstColumn
     * @param lastColumn
     * @param title
     * @param style
     * @param size
     * @param color 
     */
    public void addExtraPointsFromRowSum(int row1, int row2, boolean subtract, int firstColumn, int lastColumn, String title, PointStyle style, short size, Color color) {
        if (lastColumn-firstColumn+1!=groupCount) throw new AssertionError("Number of cells must be same as number of groups when adding extra points to existing boxplot. Expected "+groupCount+" but got "+(lastColumn-firstColumn+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        Object[] values = new Object[groupCount];
        for (int i=0;i<groupCount;i++) {
            Double value1 = sheet.getRow(row1).getCell(firstColumn+i).getNumericCellValue();
            Double value2 = sheet.getRow(row2).getCell(firstColumn+i).getNumericCellValue();
            values[i]=(subtract)?(value1-value2):(value1+value2);
        }
        addExtraColumn(extraPointsColumn, title, values);
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(headerRows, headerRows+values.length+2, extraPointsColumn, extraPointsColumn));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
        extraPointsColumn++; // make room for more columns
    }    
    
    public void addExtraPointsFromColumn(int column, int firstRow, int lastRow, String title, PointStyle style, short size, Color color) {
        if (lastRow-firstRow+1!=groupCount) throw new AssertionError("Number of cells must be same as number of groups when adding extra points to existing boxplot. Expected "+groupCount+" but got "+(lastRow-firstRow+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        Object[] values = new Object[groupCount];
        for (int i=0;i<groupCount;i++) {
            Double value = sheet.getRow(firstRow+i).getCell(column).getNumericCellValue();
            values[i]=value;
        }
        addExtraColumn(extraPointsColumn, title, values);
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(headerRows, headerRows+values.length+2, extraPointsColumn, extraPointsColumn));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
        extraPointsColumn++; // make room for more columns
    }

    public void addExtraPointsFromColumnSum(int column1, int column2, boolean subtract, int firstRow, int lastRow, String title, PointStyle style, short size, Color color) {
        if (lastRow-firstRow+1!=groupCount) throw new AssertionError("Number of cells must be same as number of groups when adding extra points to existing boxplot. Expected "+groupCount+" but got "+(lastRow-firstRow+1));
        if (extraPoints==null) extraPoints = new ArrayList<>();
        Object[] values = new Object[groupCount];
        for (int i=0;i<groupCount;i++) {
            Double value1 = sheet.getRow(firstRow+i).getCell(column1).getNumericCellValue();
            Double value2 = sheet.getRow(firstRow+i).getCell(column2).getNumericCellValue();
            values[i]=(subtract)?(value1-value2):(value1+value2);
        }
        addExtraColumn(extraPointsColumn, title, values);
        XDDFNumericalDataSource<Double> series = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(headerRows, headerRows+values.length+2, extraPointsColumn, extraPointsColumn));
        Object[] newvalues = new Object[]{series, title, style, size, color};
        extraPoints.add(newvalues);
        extraPointsColumn++; // make room for more columns
    }        
    
    private void addRow(XSSFSheet doodlesheet, int rowIndex, Object[] values) {
        Row row = doodlesheet.createRow(rowIndex);
        for (int j = 0; j < values.length; j++) {
           if (values[j] instanceof String) {
               row.createCell(j).setCellValue((String) values[j]);
           } else if (values[j] instanceof Number) {
               row.createCell(j).setCellValue(((Number) values[j]).doubleValue());
           }
       }       
    }
    
    private void addExtraColumn(int column, String title, Object[] values) { // creates extra column for extra values and adds padding and title
        Row row = getOrCreateRow(headerRows-1);
        row.createCell(column).setCellValue(title); // first row is title
        row = getOrCreateRow(headerRows);
        row.createCell(column).setCellValue(range[0]-100); // second row is padding row. Use value outside of range so it will not be visible            
        for (int j = 0; j < values.length; j++) {
           row = getOrCreateRow(headerRows+1+j); 
           if (values[j] instanceof String) {
               row.createCell(column).setCellValue((String) values[j]);
           } else if (values[j] instanceof Number) {
               row.createCell(column).setCellValue(((Number) values[j]).doubleValue());
           }
        }
        row = getOrCreateRow(headerRows+values.length+1);
        row.createCell(column).setCellValue(range[0]-100); // last row is padding row. Use value outside of range so it will not be visible               
    }
    
    private Row getOrCreateRow(int index) {
        Row row = doodlesheet.getRow(index);
        if (row==null) doodlesheet.createRow(index);     
        return row;
    }
    
    public void drawBoxplot() {
        if (!setupComplete) throw new AssertionError("Data for boxplot has not been specified"); 
        if (showInPercentage) {
            formatAsPercentage(); // this is a hack because the axis.setNumberFormat("0%") call does not appear to work
        }
        // Create the drawing patriarch
        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        // Define anchor points in the worksheet
        XSSFClientAnchor anchorPoint = drawing.createAnchor(0, 0, 0, 0, anchors[0], anchors[1], anchors[2], anchors[3]);

        // Create the chart
        chart = drawing.createChart(anchorPoint);
        if (title!=null) {
            chart.setTitleText(title);
            chart.setTitleOverlay(false);
        }

        // Define chart axis
        bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(xAxisLabel);
        leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setMinimum(range[0]);
        leftAxis.setMaximum(range[1]);
        leftAxis.setTitle(yAxisLabel); 
        if (showInPercentage) leftAxis.setNumberFormat("0%"); // this does not appear to work?        
                    
        // Define data sources for the chart
        int firstRow = headerRows; // since rows are 0-indexed, this is the first row after the headers
        int lastrow = headerRows + groupCount + 1; // Includes 2 padding rows (before and after)
        groups = XDDFDataSourcesFactory.fromStringCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 0, 0));
        XDDFNumericalDataSource<Double> values1 = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 1, 1));
        XDDFNumericalDataSource<Double> values2 = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 2, 2));
        XDDFNumericalDataSource<Double> values3 = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 3, 3));      

        // Create the data series
        XDDFBarChartData chartdata = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        chartdata.setBarDirection(BarDirection.COL);
        chartdata.setBarGrouping(BarGrouping.STACKED);
        chartdata.setOverlap((byte)100); // correcting the overlap so bars really are stacked and not side by side
        chartdata.setGapWidth(50); //
      
        XDDFBarChartData.Series series1 = (XDDFBarChartData.Series) chartdata.addSeries(groups, values1);
        series1.setTitle("Bottom", null);
        XDDFBarChartData.Series series2 = (XDDFBarChartData.Series) chartdata.addSeries(groups, values2);
        series2.setTitle("2Q", null);
        XDDFBarChartData.Series series3 = (XDDFBarChartData.Series) chartdata.addSeries(groups, values3);
        series3.setTitle("3Q", null);     
        
        // Set color for the entire series
        // XDDFSolidFillProperties transparentFill = Analysis.getExcelFillColor(new Color(255,255,255,0)); // transparent color
        XDDFSolidFillProperties fillPropertiesBox = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.CORNFLOWER_BLUE));
        XDDFSolidFillProperties fillPropertiesBorder = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLACK));       
        // Transparent color
        XDDFSolidFillProperties transparentFill = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.WHITE)); // NB! It does not work with other colors besides PresetColor here
        CTSolidColorFillProperties ctSolidColorFillProperties = (CTSolidColorFillProperties)transparentFill.getXmlObject();
        CTPresetColor ctPresetColor = ctSolidColorFillProperties.getPrstClr();
        CTPositiveFixedPercentage ctPositiveFixedPercentage = ctPresetColor.addNewAlpha();
        ctPositiveFixedPercentage.setVal(0); // the value range is from 0 (transparent) to 100000 (opaque)
        
        XDDFShapeProperties transparent = new XDDFShapeProperties();
        transparent.setFillProperties(transparentFill);
        series1.setShapeProperties(transparent);
        
        XDDFShapeProperties propertiesBox = new XDDFShapeProperties();
        propertiesBox.setFillProperties(fillPropertiesBox);
        XDDFLineProperties line = new XDDFLineProperties();
        line.setFillProperties(fillPropertiesBorder);
        line.setWidth(1.5); //
        propertiesBox.setLineProperties(line);             
        series2.setShapeProperties(propertiesBox);
        series3.setShapeProperties(propertiesBox);         

        // Add whiskers
        XDDFNumericalDataSource<Double> whiskersBottom = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 4, 4));
        XDDFNumericalDataSource<Double> whiskersTop    = XDDFDataSourcesFactory.fromNumericCellRange(doodlesheet, new CellRangeAddress(firstRow, lastrow, 5, 5));
        addErrorBars(chart, whiskersBottom, 0, STErrBarType.MINUS);        
        addErrorBars(chart, whiskersTop, 2, STErrBarType.PLUS); 
                    
        // Plot the chart with the data
        chart.plot(chartdata);
        
        // Gridlines
        XDDFShapeProperties properties = leftAxis.getOrAddMajorGridProperties();
        XDDFLineProperties lineProperties = new XDDFLineProperties();
        // lineProperties.setPresetDash(DashStyle.DASH); // Set dash style
        lineProperties.setWidth(0.5); // Set line width
        lineProperties.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.LIGHT_GRAY))); // Set line color to light gray
        properties.setLineProperties(lineProperties);        
        
        // draw extra points on top (if defined)
        if (extraPoints!=null) drawExtraPoints();
         
    }


    private void addErrorBars(XSSFChart chart, XDDFNumericalDataSource<Double> errorValues, int seriesIndex, STErrBarType.Enum bartype) {
        // Get the chart's underlying XML
        CTChart ctChart = chart.getCTChart();
        CTPlotArea plotArea = ctChart.getPlotArea();
        
        CTBarChart barChart = plotArea.getBarChartArray(0);
        CTBarSer ser = barChart.getSerArray(seriesIndex);        

        CTErrBars errBars = ser.addNewErrBars();
        errBars.addNewErrDir().setVal(STErrDir.Y);
        errBars.addNewErrBarType().setVal(bartype);
        errBars.addNewNoEndCap().setVal(false);
        CTErrValType errType=CTErrValType.Factory.newInstance();
        errType.setVal(STErrValType.CUST);
        errBars.setErrValType(errType);
        if (bartype==STErrBarType.PLUS) errBars.addNewPlus().addNewNumRef().setF(errorValues.getFormula());
        else if (bartype==STErrBarType.MINUS) errBars.addNewMinus().addNewNumRef().setF(errorValues.getFormula());
        else {
            errBars.addNewPlus().addNewNumRef().setF(errorValues.getFormula());
            errBars.addNewMinus().addNewNumRef().setF(errorValues.getFormula());
        }     
    }   
    
    
    private void drawExtraPoints() {        
        XDDFScatterChartData scatterData = (XDDFScatterChartData) chart.createData(ChartTypes.SCATTER, bottomAxis, leftAxis);        
        for (Object[] extra:extraPoints) {
            XDDFNumericalDataSource<Double> values = (XDDFNumericalDataSource<Double>)extra[0];
            String extratitle = (String)extra[1];            
            PointStyle style = (PointStyle)extra[2];
            short size = (short)extra[3];
            Color color = (Color)extra[4];

            XDDFScatterChartData.Series newSeries = (XDDFScatterChartData.Series) scatterData.addSeries(groups, values);
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
