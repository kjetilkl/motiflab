/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.motiflab.engine.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author kjetikl
 */
public class ClusterGraph implements GraphStructure<Integer, Integer> {
    HashSet<String> interactions;
    HashSet<Integer> vertices;
    boolean autoSelfConnection=false; // always have edges pointing back to the node itself

    public ClusterGraph(HashSet<String> interactions, boolean autoSelfConnection) {
        this.interactions=interactions;
        vertices=new HashSet<Integer>();
        for (String interaction:interactions) {
           int[] pair=getInteractionPair(interaction);
           vertices.add(pair[0]);
           vertices.add(pair[1]);
        }
        this.autoSelfConnection=autoSelfConnection;
    }
    
    private int[] getInteractionPair(String interactionstring) {
        String first=interactionstring.substring(0,interactionstring.indexOf('_'));
        String second=interactionstring.substring(interactionstring.indexOf('_')+1);
        try {
            int firstCluster=Integer.parseInt(first);
            int secondCluster=Integer.parseInt(second);
            return new int[]{firstCluster,secondCluster};
        } catch (NumberFormatException e) {return null;}
    }
    
    @Override
    public Integer addEdge(Integer sourceVertex, Integer targetVertex){
        throw new UnsupportedOperationException("Not supported yet. 1");
    }
    @Override
    public boolean addEdge(Integer sourceVertex, Integer targetVertex, Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 2");
    }
    @Override
    public boolean addVertex(Integer v) {
        throw new UnsupportedOperationException("Not supported yet. 3");
    }
    @Override
    public boolean containsEdge(Integer sourceVertex, Integer targetVertex) {
        if (autoSelfConnection && sourceVertex.intValue()==targetVertex.intValue()) return true;
        return (interactions.contains(sourceVertex+"_"+targetVertex) || interactions.contains(targetVertex+"_"+sourceVertex));
    }
    @Override
    public boolean containsEdge(Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 4");
    }
    @Override
    public boolean containsVertex(Integer v) {
        throw new UnsupportedOperationException("Not supported yet. 5");
    }
    @Override
    public Set<Integer> edgeSet() {
        throw new UnsupportedOperationException("Not supported yet. 6");
    }
    @Override
    public Set<Integer> edgesOf(Integer vertex) {
        throw new UnsupportedOperationException("Not supported yet. 7");
    }
    @Override
    public Set<Integer> getAllEdges(Integer sourceVertex, Integer targetVertex) {
        throw new UnsupportedOperationException("Not supported yet. 8");
    }
    @Override
    public Integer getEdge(Integer sourceVertex, Integer targetVertex) {
        throw new UnsupportedOperationException("Not supported yet. 9");
    }
    @Override
    public Integer getEdgeSource(Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 10");
    }
    @Override
    public Integer getEdgeTarget(Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 11");
    }
    @Override
    public double getEdgeWeight(Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 12");
    }
    @Override
    public boolean removeAllEdges(Collection<? extends Integer> edges) {
        throw new UnsupportedOperationException("Not supported yet. 13");
    }
    @Override
    public Set<Integer> removeAllEdges(Integer sourceVertex, Integer targetVertex) {
        throw new UnsupportedOperationException("Not supported yet. 14");
    }
    @Override
    public boolean removeAllVertices(Collection<? extends Integer> vertices) {
        throw new UnsupportedOperationException("Not supported yet. 15");
    }
    @Override
    public Integer removeEdge(Integer sourceVertex, Integer targetVertex) {
        throw new UnsupportedOperationException("Not supported yet. 16");
    }
    @Override
    public boolean removeEdge(Integer e) {
        throw new UnsupportedOperationException("Not supported yet. 17");
    }
    @Override
    public boolean removeVertex(Integer v) {
        throw new UnsupportedOperationException("Not supported yet. 18");
    }
    @Override
    public Set<Integer> vertexSet() {
        return vertices;
    }

}