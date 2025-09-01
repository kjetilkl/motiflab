/*
 * LayeredNetVisualizer.java
 *
 */

package org.motiflab.engine.data.classifier.neuralnet;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.text.NumberFormat;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierOutput;
import org.motiflab.engine.data.classifier.ClassifierVisualizer;

/**
 *
 * @author  Kjetil Klepper
 * @version
 */
public class LayeredNetVisualizer extends ClassifierVisualizer {

  private static final int NONE=0;
  private static final int NODE=1;
  private static final int LAYER=2;

  private  int selection=NONE;

  private NeuralNet network;
  private LayeredNet topology;
  private int netlayers;
  private double[] layerwidths;
  private double[] layerYpositions;
  private NumberFormat nf;
  private int nodesize=26;
  private Node selectednode;
  private boolean paintvalues=false;

  private int selectedlayer=0;
  private int edgelabeloffset=4;

  double internwidth=1000f,internheight=1000f;
  double width=500,height=500;
  double weigthmax=0, weightmin=Double.MAX_VALUE; // range of weights (not considering sign)

  private static String filename="ga_network.dta";

  public static void setSaveAs(String f) {
    filename=f.trim();
  }

  public static String getSaveAs() {
    return filename;
  }

  /**
   * Creates a new LayeredNetVisualizer
   */
  public LayeredNetVisualizer() {
    super();
    constructor();
  }

  /**
   * Creates a new LayeredNetVisualizer
   */
  public LayeredNetVisualizer(NeuralNet net) {
    super();
    constructor();
    setNetwork(net);
    calculateStuff();
  }

  /** A convenience method to gather common code from several constructors*/
  private void constructor() {
    nf=NumberFormat.getInstance();
    nf.setMaximumFractionDigits(2);

    this.setLayout(new BorderLayout());
    this.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {} //resolveSelect(e.getX(),e.getY());
      public void mousePressed(MouseEvent e) {resolveSelect(e.getX(),e.getY());}
    });

    this.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {layerDrag(e.getX(),e.getY());}
    });
    NeuralPanel neuralPanel=new NeuralPanel();
    this.add(neuralPanel,BorderLayout.CENTER);

  }

  public void setNetwork(NeuralNet net) {
     network=net;
     topology=(LayeredNet)network.getTopology();
     if (topology==null) System.err.println("topology==NULL");
     netlayers=topology.numlayers();
     layerwidths=new double[netlayers];
     for (int i=0;i<netlayers;i++) layerwidths[i]=0.9f;
  }

  @Override
  public void round(ClassifierOutput info) {
    processClassifierOutput(info);
  }
  @Override
  public void start(ClassifierOutput info) {
    processClassifierOutput(info);
  }
  @Override
  public void finished(ClassifierOutput info) {
    processClassifierOutput(info);
  }

  private void processClassifierOutput(ClassifierOutput info) {
     if (info!=null) {
         if (info.get(Classifier.NEURAL_NETWORK)==null) return;
         if (network==null) setNetwork((NeuralNet)info.get(Classifier.NEURAL_NETWORK));
     }
     SwingUtilities.invokeLater(new Runnable() {
       public void run() {
            calculateStuff();
            repaint();
        }
     });
  }


  /** Find the node or layer that was selected by a mousePressed event, also notifies listeneres */
  private void resolveSelect(int xpos, int ypos) {
    selectedlayer=-1;selection=NONE;
    int selectednodenumber=-1;
    double adjustednodesizeX=nodesize*(internwidth/width);
    double adjustednodesizeY=nodesize*(internheight/height);
    double x=xpos*(internwidth/width), y=ypos*(internheight/height);
    //System.err.println("mouseclick at "+x+","+y);
    toploop:
    for (int i=0;i<netlayers;i++) {
      //System.err.println("CheckLayer["+i+"]  mouse("+xpos+","+ypos+"=>"+x+","+y+")   target=[y:"+(layerYpositions[i]-adjustednodesizeY)+"-"+(layerYpositions[i]+adjustednodesizeY)+"]");
      if (y<layerYpositions[i]+adjustednodesizeY/2 && y>layerYpositions[i]-adjustednodesizeY/2) {
         selectedlayer=i;
         ArrayList list=topology.getLayer(i);
         for (int n=0;n<list.size();n++) {
           Node node=(Node)list.get(n);
           //System.err.println("CheckNode["+i+","+n+"]  mouse("+xpos+","+ypos+"=>"+x+","+y+")   target=[x:"+(node.getX()-adjustednodesizeX/2)+"-"+(node.getX()+adjustednodesizeX/2)+"    y:"+(layerYpositions[i]-adjustednodesizeY)+"-"+(layerYpositions[i]+adjustednodesizeY)+"]");
           if (x<node.getX()+adjustednodesizeX/2 && x>node.getX()-adjustednodesizeX/2) {
              selectednode=node;
              selection=NODE;
              selectednodenumber=n;
              break toploop;
           }
         }
         selection=LAYER;
         break;
      }
    }
    if (selection==NONE || selection==LAYER || selectedlayer>0 || selectednodenumber<0) notifyListenersOfFeatureSelected(-1); // no feature selected
    else {
        notifyListenersOfFeatureSelected(selectednodenumber);
    }
    repaint();
  }


  @Override
  public void setSelectedFeature(int featureNumber) {
       if (featureNumber<0) {
           selection=NONE;
           selectedlayer=-1;
       } else {
           ArrayList list=topology.getLayer(0);
           Node node=(Node)list.get(featureNumber);
           selectednode=node;
           selectedlayer=0;
           selection=NODE;
       }
       repaint();
  }


  private void layerDrag(int xpos, int ypos) {
     if (selection!=LAYER) return;
     setLayerYPositions(selectedlayer,ypos*(internheight/height));

     double percentage=Math.abs(xpos-(width/2))/(width/2);
     layerwidths[selectedlayer]=percentage;
     calculateLayerXPositions(selectedlayer);
     repaint();


  }


  private void calculateLayerYPositions(int layer) {
       double layerheight=internheight/netlayers;
         double layerYpos=(layer+0.5f)*layerheight;
         int lsize=topology.layerSize(layer);
         ArrayList layerlist=topology.getLayer(layer);
         layerYpositions[layer]=layerYpos;
        for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            node.setY(layerYpos);
         }
         // do thresholds for layers>0
         if (layer>0) {
         double thresholdYpos=layerYpos-(layerheight*0.05f);
         layerlist=topology.getLayerThresholds(layer);
         for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            node.setY(thresholdYpos);
         }
         }
      }

  private void setLayerYPositions(int layer, double layerYpos) {
       double layerheight=internheight/netlayers;
         int lsize=topology.layerSize(layer);
         ArrayList layerlist=topology.getLayer(layer);
         layerYpositions[layer]=layerYpos;
        for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            node.setY(layerYpos);
         }
         // do thresholds for layers>0
         if (layer>0) {
         double thresholdYpos=layerYpos-(layerheight*0.05f);
         layerlist=topology.getLayerThresholds(layer);
         for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            node.setY(thresholdYpos);
         }
         }
      }

  private void calculateLayerXPositions(int layer) {
         int lsize=topology.layerSize(layer);
         ArrayList layerlist=topology.getLayer(layer);
         int usesize=lsize;
         if (layer>0) usesize*=2; // remember threshold nodes
         double nodeslotwidth=(internwidth*layerwidths[layer])/usesize;
         double xoffset;
         if (layer==0) xoffset=(internwidth-usesize*nodeslotwidth)/2; // offset for center alignment
         else xoffset=(internwidth-(usesize+1)*nodeslotwidth)/2;

        for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            if (layer>0) node.setX(xoffset+nodeslotwidth*(2*n+1.5f));
            else node.setX(xoffset+nodeslotwidth*(n+0.5f)); // odd slots for normal nodes when thresholds are present
         }
         // do thresholds for layers>0
         if (layer>0) {
         layerlist=topology.getLayerThresholds(layer);
         for (int n=0;n<lsize;n++) {
            Node node=(Node)layerlist.get(n);
            node.setX(xoffset+nodeslotwidth*(2*n+0.5f));  // factor 2 means even slots are for thresholds
         }
        }
      }


  private void calculateStuff() {
      // nodecoordinates
      width=this.getWidth();
      height=this.getHeight();
      double layerheight=internheight/netlayers;
      layerYpositions=new double[netlayers];
      for (int l=0;l<netlayers;l++) {
         calculateLayerYPositions(l);
         calculateLayerXPositions(l);
      }


      // do edges
      if (network==null) System.err.println("Network==NULL");
      weigthmax=0; weightmin=Double.MAX_VALUE;
      ArrayList edges=network.getEdges();
      for (int i=0;i<edges.size();i++) {
         double w=((Edge)edges.get(i)).getWeight();
         if (Math.abs(w)<weightmin) weightmin=Math.abs(w);
         if (Math.abs(w)>weigthmax) weigthmax=Math.abs(w);
      }

  }


  private void drawNode(Node node, Color node_color, boolean paintvalue, Graphics2D g) {
     //  getHSBColor(double h, double s, double b)
     int nodesize;
     if (paintvalue) {
      float[] hsbvals=Color.RGBtoHSB(node_color.getRed(),node_color.getGreen(),node_color.getBlue(),null);
      //if (node.getOutput()<0.5) hsbvals[2]=(double)node.getOutput();
      //else hsbvals[1]=1.5f-(double)node.getOutput();
      hsbvals[2]=(float)node.getOutput();
      node_color=Color.getHSBColor(hsbvals[0],hsbvals[1],hsbvals[2]);
     }
     if (node instanceof ThresholdNode) nodesize=(int)(this.nodesize*0.6); else nodesize=this.nodesize;
     g.setStroke(new BasicStroke(1f));
     g.setColor(node_color);
     g.fillOval((int)(node.getX()*(width/internwidth)-(nodesize/2)),(int)(node.getY()*(height/internheight)-(nodesize/2)),nodesize,nodesize);
     g.setColor(Color.black);
     g.drawOval((int)(node.getX()*(width/internwidth)-(nodesize/2)),(int)(node.getY()*(height/internheight)-(nodesize/2)),nodesize,nodesize);
     //System.err.println("drawing node at x="+(int)(node.getX()*(width/internwidth)-(nodesize/2))+", y="+(int)(node.getY()*(height/internheight)-(nodesize/2)));
     //System.err.println("Event Dispatch="+SwingUtilities.isEventDispatchThread()+" width="+width+",height="+height);
     if (false) { // show node output (as text)
        g.setFont(new Font("Garamond",Font.PLAIN,16));
        g.setColor(Color.black);
        //g.drawString(""+nf.format(node.getOutput())+"/"+nf.format(node.getErrorTerm()),(int)(node.getX()*(width/internwidth)+nodesize*0.7),(int)(node.getY()*(height/internheight)));
        g.drawString(""+nf.format(node.getOutput()),(int)(node.getX()*(width/internwidth)+nodesize*0.7),(int)(node.getY()*(height/internheight)));
     }
  }



  private void drawEdge(Edge edge, boolean shaded, Graphics2D g) {
     Node n1=edge.getSource(), n2=edge.getDestination();
     int nodenum=((LayeredNodeID)n1.getID()).getNumber();
     if (Math.IEEEremainder(nodenum,2)==0) edgelabeloffset=4; else edgelabeloffset=3;
     double weight=edge.getWeight();
     Color color; if (weight==0) color=Color.lightGray; else if (weight>0) color=Color.green; else color=Color.red;
     if (shaded) color=Color.lightGray;
     g.setColor(color);
     float minstroke=0.5f, maxstroke=5f;
     float strokesize=(float)(Math.abs((weight-weightmin))/(weigthmax-weightmin))*maxstroke;
     if (strokesize<minstroke) strokesize=minstroke;
     if (strokesize>maxstroke) strokesize=maxstroke;
     Stroke s=new BasicStroke(strokesize);
     g.setStroke(s);
     int c1_x=(int)(n1.getX()*(width/internwidth)),
         c1_y=(int)(n1.getY()*(height/internheight)),
         c2_x=(int)(n2.getX()*(width/internwidth)),
         c2_y=(int)(n2.getY()*(height/internheight));
     g.drawLine(c1_x, c1_y, c2_x, c2_y);

     // edge-weight
     if (!shaded && false) { // draw edge-weights
         Font f=g.getFont();
         g.setColor(Color.black);
         g.setFont(new Font("Times New Roman",Font.PLAIN,14));
         //double delta_x=Math.abs(c1_x-c2_x), delta_y=Math.abs(c1_y-c2_y);
         double delta_x=(c2_x-c1_x), delta_y=(c2_y-c1_y);
         //g.drawString(""+nf.format(edge.getLastUpdate()),(int)(c1_x+delta_x/edgelabeloffset)-10,(int)(c1_y+delta_y/edgelabeloffset));
         g.drawString(""+nf.format(weight),(int)(c1_x+delta_x/edgelabeloffset)-10,(int)(c1_y+delta_y/edgelabeloffset));
         g.setFont(f);
         // System.err.println("Edgeweigth="+weight);
     }

  }

  class NeuralPanel extends JPanel {
      public void paintComponent(Graphics g) {
      width=this.getWidth();
      height=this.getHeight();
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
      /*if (network==null) {
        network = NNPhenotype.getNetwork();
        topology=(LayeredNet)network.getTopology();
        netlayers=topology.numlayers();
        calculateStuff();
      }*/
       setBackground(Color.white);
      drawNetwork(g2);
     }
  }

  public void drawNetwork(Graphics2D g) {
     if (network==null) return;
     //System.err.println("Drawing network!");

     ArrayList edges=network.getEdges();
     ArrayList nodes=network.getNodes();
     edgelabeloffset=4;
    // highlight selected layer
    if (selection==LAYER) {
      g.setColor(new Color(255,255,154));
      double selectedypos=layerYpositions[selectedlayer];
      g.fillRect(0,(int)(selectedypos*(height/internheight))-nodesize,(int)width,nodesize*2);
    }

    Color inputnodecolor=Color.blue, nodecolor=Color.white, thresholdcolor=Color.black;
    boolean shade=false;
    if (selection==NODE) {shade=true; inputnodecolor=Color.lightGray; nodecolor=Color.white; thresholdcolor=Color.darkGray;}
      //g.setColor(new Color(255,255,154));
      //double selectedypos=selectednode.getY(), selectedxpos=selectednode.getX();
      //g.fillRect((int)((selectedxpos*(width/internwidth))-nodesize),(int)((selectedypos*(height/internheight))-nodesize),nodesize*2,nodesize*2);

     // draw edges
     for (int i=0;i<edges.size();i++) {
        Edge edge=(Edge)edges.get(i);
        drawEdge(edge, shade ,g);
     }
     // draw selected edges
     if (selection==NODE) {
       ArrayList tmpedges=selectednode.getInputEdges();
       for (int i=0;i<tmpedges.size();i++) {Edge edge=(Edge)tmpedges.get(i); drawEdge(edge,false,g);}
       tmpedges=selectednode.getOutputEdges();
       for (int i=0;i<tmpedges.size();i++) {Edge edge=(Edge)tmpedges.get(i); drawEdge(edge,false,g);}
     }
     // draw nodes
     for (int i=0;i<nodes.size();i++) {
        Node node=(Node)nodes.get(i);
        if (node instanceof InputNode) drawNode(node, inputnodecolor, paintvalues,g);
        else if (node instanceof ThresholdNode) drawNode(node, thresholdcolor, paintvalues,g);
        else drawNode(node, nodecolor, paintvalues, g);
     }
     // draw selected node
     if (selection==NODE) {
       inputnodecolor=Color.blue; nodecolor=Color.white; thresholdcolor=Color.black;
       if (selectednode instanceof InputNode) drawNode(selectednode, inputnodecolor, paintvalues, g);
       else if (selectednode instanceof ThresholdNode) drawNode(selectednode, thresholdcolor, paintvalues, g);
       else drawNode(selectednode, nodecolor, paintvalues, g);
     }

      /*
     g.setColor(Color.black);
     Font f=g.getFont();
     g.setFont(new Font("Times New Roman",Font.BOLD,16));
     g.drawString("Generation: "+generation+"   Edges: "+edges_found+"/"+optimal_edges_found+"/"+tour.length+"  Length: "+(int)tour_length+"/"+(int)optimal_length,borderwidth,height+borderwidth+borderwidth);
     g.setFont(f);
     */
  }


  private int[] screen_positions;

  public void setPaintValues(boolean v) {paintvalues=v;}

}