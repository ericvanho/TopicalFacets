package ericvh.TDT;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/** Class TextGraph builds a representation of a text as a graph with the 'words'
 * (or labels or token-types) as points (vertices) and the links between these
 * points as the arcs of the graph.* Besides the name of the point (a label), 
 * the graph keeps information on the documents its tokens are used in. 
 * The vertices of this graph remember their position in the texts, their informative 
 * importance and neighbors. The graph knows what collections it is a member of.
 * A collection is a set of documents with the same date and source. 
 * The arcs have a weight (info importance) and a direction (incoming or outgoing
 * with respect to the vertex they connect). Arcs allow travelling from one vertex
 * to another and from one document to another.
 * @author  Eric Van Horenbeeck
 */
public class TextGraph implements Serializable
{
    // Map with counter of a token in each text. Key: token index; value: 
    // counting the appearences of a token in this document.
    private Map mTokenCountMap;
    // Unique document number identifying this graph.
    private int mDocNr;
    // Map with the arcs found in this document.
    private Map mArcsMap;
    
    /** Default Constructor
     */
    public TextGraph()
    { }
    
    /** Constructor.
     * @param docNr : unique document identifier (int)
     */
    public TextGraph(int docNr)
    {
        mTokenCountMap = new HashMap();
        mArcsMap = new HashMap();
        mDocNr = docNr;
    }
    
    /** getGraphDocNr: Getter returns the unique document number associated 
     * with this graph. There is one and only one graph for every text file.
     * @return the doc number (int) associated with this graph.
     */
    public int getGraphDocNr()
    {
        return mDocNr;
    }
    
    /** addTokenCount: Adds one to the counter of this vertex in this document.
     * @param tokenIndex : the unique identifier (int) of this token-type.
     */
    public void addTokenCount(int tokenIndex)
    {
        Integer tokInt = tokenIndex;
        int tmpCount = 0;
        // Checks if this token is already recorded in the count map and copies 
        // whatever value the counter in this document already has.
        if(mTokenCountMap.containsKey(tokInt)) tmpCount = ((Integer) mTokenCountMap
                .get(tokInt));
        // Adds 1 to the counter.
        tmpCount ++;
        // Updates the tokenIndexMap.
        mTokenCountMap.put(tokInt, tmpCount);
    }
    
    /** getTokenCountMap: Getter returns map with the counts of all the 
     * token-types in this document.
     * @return HashMap with count of all token-types in this document. 
     * Key: token (Integer), value: count (Integer)
     */
    public Map getTokenCountMap()
    {
        return mTokenCountMap;
    }
    
    /** isEmpty: Returns 'true' if this graph doesn't have any points (vertices).
     * @return boolean 'true' if graph is empty.
     */
    public boolean isEmpty()
    {
        return mTokenCountMap.isEmpty();
    }
    
    /** addArc: Adds a new arc the the map.
     * @param arcIdx : the arc index (String) made from the indices of its vertices.
     * @param arc : the new arc.
     */
    public void addArc(String arcIdx, Arc arc)
    {
        mArcsMap.put(arcIdx, arc);
    }
    
    /** addAllArcs: Adds a map with arcs to this map.
     * @param arcs : a HashMap with arcs.
     */
    public void addAllArcs(Map arcs)
    {
        mArcsMap.putAll(arcs);
    }
    
    /** containsArc: Boolean is returned after checking if arcsMap contains arc.
     * @param a : an arc
     * @return boolean 'true' if this arc exists in this graph.
     */
    public boolean containsArc(Arc a)
    {
        return mArcsMap.containsValue(a);
    }
    
    /** containsArcIndex: Returns 'true' if the arc map contains an element 
     * identified by this index.
     * @param arcIdx : the arc index (String)
     * @return boolean 'true' if map contains this arc
     */
    public boolean containsArcIndex(String arcIdx)
    {
        return mArcsMap.containsKey(arcIdx);
    }
    
    /** getArcKey: Returns the key of an arc if found in this collection.
     * @param arc : arc being processed.
     * @return the key of this element (String).
     */
    public String getArcKey(Arc arc)
    {
        Arc currentArc;
        String key = "";
        Iterator arc_itr = mArcsMap.keySet().iterator();
        search:
            while(arc_itr.hasNext())
            {
                key = arc_itr.next().toString();
                currentArc = (Arc) mArcsMap.get(key);
                if(arc.equals(currentArc)) break;
                else key ="";
            }
            return key;
    }
    
    /** getThisArc: Getter returns the arc identified by this arc index.
     * @param arcIdx : the arc index (String)
     * @return an arc identified by its index
     */
    public Arc getThisArc(String arcIdx)
    {
        return (Arc) mArcsMap.get(arcIdx);
    }
    
    /** getAllArcIndices: Getter of all indices in this arcsMap.
     * @return Object array with all the arc indices (String) attributed so far.
     */
    public Object [] getAllArcIndices()
    {
        return mArcsMap.keySet().toArray();
    }
    
    /** arcsMapIsEmpty: Checks whether this arcsTable has any elements.
     * @return boolean 'true' if empty
     */
    public boolean arcsMapIsEmpty()
    {
        return mArcsMap.isEmpty();
    }
    
    /** getArcsCount: Getter counts the number of arcs in this TextGraph.
     * @return size (int) of arcsMap.
     */
    public int getArcsCount()
    {
        return mArcsMap.size();
    }
    
    /** getFullArcsMap: Getter returns all the arcs in this graph.
     * @return  HashMap with arcs. Key: arcID (String), value: arc (Arc).
     */
    public Map getFullArcsMap()
    {
        return mArcsMap;
    }
    
    /** getArcKey: Getter returns the key of an arc in this graph made with
     * the indices of the two connecting vertices.
     * @param vrtxIndx1 : first point of the arc (int)
     * @param vrtxIndx2 : second point of the arc (int)
     * @return arc key (String)
     */
    private String getArcKey(int vrtxIndx1, int vrtxIndx2)
    {
        String key = vrtxIndx1 + "*" + vrtxIndx2;
        return key;
    }
    
    /** getArc: Getter returns an arc in this graph identified by its key or 'null'.
     * @param vrtxIndx1 : vertex to the left of the arc (int).
     * @param vrtxIndx2 : vertex to the right of the arc (int).
     * @return arc if found or 'null'.
     */
    public Arc getArc(int vrtxIndx1, int vrtxIndx2)
    {
        String key = getArcKey(vrtxIndx1, vrtxIndx2);
        if(mArcsMap.containsKey(key)) return ((Arc) mArcsMap.get(key));
        else return null;
    }
    
}
