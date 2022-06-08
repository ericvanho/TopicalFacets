package ericvh.TDT;

import java.util.*;
import java.io.*;

/** Class AdjacentVertices
 *
 * @author  Eric Van Horenbeeck
 * Created on 11 september 2004, 11:46
 */
public class AdjacentVertices  implements Serializable
{
    private Set mAdjacentVertices;
    private int mDocNr;
    private Map mNeighborMap;
    
    /** Constructor
     */
    public AdjacentVertices()
    {
        mNeighborMap = new HashMap();
        mAdjacentVertices = new HashSet();
    }
    
    /** Adds new vertex label (a new neighbor) to this vertex in this document
     * @param docNr : the unique document identificator
     * @param label : new neighbor for this vertex in this document
     */
    public void addDocNeighbor(int docNr, String label)
    {
        Set tempSet = new HashSet();
        Integer docInt = docNr;
        if(mNeighborMap.containsKey(docInt)) tempSet = getAdjVerticesDoc(docNr);
        tempSet.add(label);
        mNeighborMap.put(docInt, tempSet);
    }
    
    /** Adds a map with all neighbors of this vertex from a single textgraph to
     * the general dataGraph.
     * @param adjVerticesMap : a map with docNrs as key and a neighbors as value
     */
    public void addAllNeighbors(Map adjVerticesMap)
    {
        mNeighborMap.putAll(adjVerticesMap);
    }
    
    /** Getter returns the neighbors for this vertex in this document
     * @param docNr : the unique document identificator
     * @return Set with neighbors (String) for this document
     */
    public Set getAdjVerticesDoc(int docNr)
    {
        Integer docInt = docNr;
        return (Set) mNeighborMap.get(docInt);
    }
    
    /** Getter return map with all the adjacent vertices linked to this vertex
     * @return all adjacent vertices per document linked to this vertex
     */
    public Map getAllAdjVertices()
    {
        return mNeighborMap;
    }
}
