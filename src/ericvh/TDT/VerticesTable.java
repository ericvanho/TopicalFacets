package ericvh.TDT;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class VerticesTable is a structure holding the individual vertices in a map
 * and methods to access these data. A vertex is a token-type.
 * @author  Eric Van Horenbeeck
 * Created on 16 september 2004, 15:14
 */
public class VerticesTable implements Serializable
{
    // Map with as key: vertex index (Integer) and as value a vertex.
    private final Map mVerticesMap;
    
    /** Constructor
     */
    public VerticesTable()
    {
        mVerticesMap = new HashMap();
    }
    
    /** addVerticesMap: Adds the map with all the vertices and their info values 
     * in all documents. Key: vertex index, value: vertex. Called by 
     * the ApplicationManager and GraphAnalyzer.
     * @param vm : the vertices map
     */
    public void addVerticesMap(Map vm)
    {
        mVerticesMap.putAll(vm);
    }
    
    /** getVerticesMap: Getter returns the map with all vertices involved in this 
     * arcTable.
     * @return Map with vertices. Key: vertex index, value: the vertex
     */
    public Map getVerticesMap()
    {
        return mVerticesMap;
    }
    
    /** getKeySet: Getter returns the keySet of the verticesMap.
     * @return set with keys (Integer) of this verticesMap
     */
    public Set getKeySet()
    {
        return mVerticesMap.keySet();
    }
    
    /** getThisVertex: Getter returns the vertex identified by this token index in 
     * this graph.
     * @param idx : a token index (Integer)
     * @return vertex
     */
    public Vertex getThisVertex(Integer idx)
    {
        return (Vertex) mVerticesMap.get(idx);
    }
    
    /** containsVertex: Boolean true if this verticesMap contains this vertex.
     * @param v : this vertex
     * @return boolean 'true' if this vertex is in this map, 'false' otherwise
     */
    public boolean containsVertex(Vertex v)
    {
        return mVerticesMap.containsValue(v);
    }
    
    /** getFullVerticesMap: Getter returns the full map will all vertices.
     * @return map with all vertices. Key is Integer index, value is the vertex.
     */
    public Map getFullVerticesMap()
    {
        return mVerticesMap;
    }
    
    /** getAllVrtxIndices: Getter of all indices (Integers) in this verticesMap.
     * @return set with all the vertex indices attributed so far
     */
    public Set getAllVrtxIndices()
    {
        return mVerticesMap.keySet();
    }
    
    /** getDocsWithVertices: Returns a map with the docId (Integer) as key and a 
     * set of all the vertex-ids (Integer) used in that doc as value.
     * @return HashMap with all docs and for every doc all vertices in a HashSet.
     */
    public Map getDocsWithVertices()
    {
        Map docVtrxMap = new HashMap();
        Iterator vrtx_itr = mVerticesMap.keySet().iterator();
        while(vrtx_itr.hasNext())
        {
            Integer vrtxId = (Integer) vrtx_itr.next();
            Vertex vrtx = (Vertex) mVerticesMap.get(vrtxId);
            SortedSet docSet = (TreeSet) vrtx.getAllVertexDocNrs();
            Iterator doc_itr = docSet.iterator();
            while(doc_itr.hasNext())
            {
                Set tmpSet = new HashSet();
                Integer docInt = (Integer) doc_itr.next();
                if(docVtrxMap.containsKey(docInt)) tmpSet = (Set) docVtrxMap
                        .get(docInt);
                tmpSet.add(vrtxId);
                docVtrxMap.put(docInt, tmpSet);
            }
        }
        return docVtrxMap;
    }
    
    /** returnValueList: Returns a list with the info value (double) from a list 
     * with vertex Integer  indices.
     * @param idxs : list with vertex indices (Integer)
     * @param collKey : key to the collection that has the info value of this 
     * vertex (String).
     * @return list with info values (double)
     */
    public LinkedList returnValueList(List idxs, String collKey)
    {
        LinkedList tmpList = new LinkedList();
        double infoValue = 0;
        Iterator idxs_itr = idxs.iterator();
        while(idxs_itr.hasNext())
        {
            try
            {
                Integer key = (Integer) idxs_itr.next();
                Vertex v = getThisVertex(key);
                infoValue = v.getInfoValue(collKey);          
            }
            catch( NullPointerException ne)
            {
                infoValue = 0.0;
            }
        }
        tmpList.add(infoValue);
        return tmpList;
    }
    
    /** isEmpty: Checks whether this verticesTable has any elements.
     * @return boolean 'true' if empty
     */
    public boolean isEmpty()
    {
        return mVerticesMap.isEmpty();
    }
    
    /** getVerticesCount: Getter returns the number of vertices in this verticesTable.
     * @return int
     */
    public int getVerticesCount()
    {
        return mVerticesMap.size();
    }
    
    /** addAllVertices: Adds a map with vertices to this graph.
     * @param vertices : map to add
     */
    public void addAllVertices(Map vertices)
    {
        mVerticesMap.putAll(vertices);
    }
    
}
