package ericvh.TDT;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class Vertex instances are the points in the graph representing a token-type.
 * A vertex is identified by a sequential number (index) pointing to its label 
 * (the 'word' or token-type). This label-string is unique inside the entire dataGraph.
 * The labels itself are stored by the labelTable class. The vertex knows what 
 * documents it is used in. Vertex has a map with labels-indices of adjacent vertices
 * per document (its neighbors).
 * @author  Eric Van Horenbeeck
 */
public class Vertex implements Serializable
{
    // Map with info value of a token-type. Key is the collection key; value is 
    // the info value of this vertex in this collection.
    private final Map mTokenInfoValueMap;
    // Map with vertices adjacent to this vertex. Key: docNr; value: neighbor labels.
    private final Map mAdjVerticesMap;
    
    /** Constructor
     * Vertex keeps a pointer to its immediate left and right neighbors 
     * (the adjacent vertices). The vertex also gathers information to allow the
     * calculation of the informative value of a token-type in the collection.
     */
    public Vertex()
    {
        mAdjVerticesMap = new HashMap();
        mTokenInfoValueMap = new HashMap();
    }
    
    /** getAllCollectionKeys: Getter returns a set with the keys of the 
     * collections where this vertex is a member of.
     * @return set with collection keys (String)
     */
    public Set getAllCollectionKeys()
    {
        return mTokenInfoValueMap.keySet();
    }
    
    /** getAllVertexDocNrs: Getter returns all the document numbers in all the 
     * collections of this vertex.
     * @return sorted set with all the document numbers (Integer) in every collection
     */
    public SortedSet getAllVertexDocNrs()
    {
        SortedSet docSet = new TreeSet();
        Iterator key_itr = mAdjVerticesMap.keySet().iterator();
        while (key_itr.hasNext()) docSet.add(key_itr.next());
        return docSet;
    }
      
    /** isInThisDoc: Checks if this vertex appears in this document. Looks in the
     * adjacent vertices map for the key. Every vertex has at least one neighbor
     * (adjacent vertex) at its left or at its right in any meaningful document.
     * @param docNr : the unique document identifier (int).
     * @return boolean 'true' if doc contains vertex, 'false' otherwise.
     */
    public boolean isInThisDoc(int docNr)
    {
        return mAdjVerticesMap.containsKey(docNr);
    }
    
    /** setInfoValue: Replaces the initial info value of this token. The initial
     * value is a placeholder with a negative number. The final value can only be
     * computed when all the files of a collection are processed. The informative 
     * value of this token-type is defined on collection level only.
     * @param collKey : key to the collection (String).
     * @param value : the calculated informative value of this token-type (double).
     */
    public void setInfoValue(String collKey, double value)
    {
        double infoValue = value;
        this.mTokenInfoValueMap.put(collKey, infoValue);
    }
    
    /** getInfoValue: Getter returns the info value of this token-type inside a 
     * collection.
     * @param collKey : key to the collection (String)
     * @return infomative value of this token-type (double)
     */
    public double getInfoValue(String collKey)
    {
        Double tempValue = (Double) mTokenInfoValueMap.get(collKey);
        return tempValue;
    }
    
    /** addAllTokenInfoValue: Adds a map with all the token-type info values per
     * collection of this vertex.
     * @param m : map with info values for this vertex
     */
    public void addAllTokenInfoValue(Map m)
    {
        this.mTokenInfoValueMap.putAll(m);
    }
        
    /** getTokenInfoValueMap: Getter returns a map with the info value of this 
     * vertex for every collection.
     * @return map with collection as key and info weight as value
     */
    public Map getTokenInfoValueMap()
    {
        return mTokenInfoValueMap;
    }
    
    /** addAdjVertex: Adds the index from a vertex adjacent to this vertex to 
     * this document set.
     * @param index : identifies an adjacent vertex (int)
     * @param docNr : unique document number (int)
     */
    public void addAdjVertex(int index, int docNr)
    {
        Set tempSet = new HashSet();
        Integer docInt = docNr;
        if(mAdjVerticesMap.containsKey(docInt)) tempSet = getAdjVerticesDoc(docNr);
        tempSet.add(index);
        mAdjVerticesMap.put(docInt, tempSet);
    }
    
    /** addAllAdjVertices: Adds a map of indices from adjacent vertices to this 
     * vertex.
     * @param m : map with indices identifying adjacent vertices. key : docNr (int),
     * value: set with
     * adjacent vertex indices (Integer).
     */
    public void addAllAdjVertices(Map m)
    {
        Iterator adjVert_itr = m.keySet().iterator();
        while(adjVert_itr.hasNext())
        {
            Integer docInt = (Integer) adjVert_itr.next();
            Set tmpSet = (HashSet) m.get(docInt);
            Iterator idx_itr = tmpSet.iterator();
            while(idx_itr.hasNext())
            {
                Integer index = (Integer) idx_itr.next();
                addAdjVertex(index, docInt);
            }
        }
    }
    
    /** getAdjVerticesDoc: Returns a set with vertices (indices) adjacent to 
     * this vertex and belonging to this document.
     * @param docNr : unique document number (int)
     * @return Set with indices of vertices adjacent to this vertex (Integer)
     */
    public Set getAdjVerticesDoc(int docNr)
    {
        return (Set) mAdjVerticesMap.get(docNr);
    }
    
    /** getAdjVerticesMap: Getter returns a map with all vertices adjacent to 
     * this vertex. Key is the document number; value: indices of the adjacent 
     * vertices (Integer).
     * @return Map with indices of all vertices adjacent to this vertex
     */
    public Map getAdjVerticesMap()
    {
        return mAdjVerticesMap;
    }
    
    /** getSharedDocs: Getter returns a set with doc-ids from all collections 
     * shared by this and another vertex.
     * @param otherVertex : the second vertex
     * @return set of document-ids (Integer) shared by both vertices
     */
    public Set getSharedDocs(Vertex otherVertex)
    {
        Set docsV1 = this.getAllVertexDocNrs();
        Set docsV2 = otherVertex.getAllVertexDocNrs();
        return Algorithms.intersect(docsV1, docsV2);
    }
    
    /** getSharedCollections: Getter returns a set with the collections shared 
     * by this and another vertex.
     * @param otherVertex : the second vertex
     * @return set of collection keys (String) shared by both vertices
     */
    public Set getSharedCollections(Vertex otherVertex)
    {
        Set collV1 = this.getAllCollectionKeys();
        Set collV2 = otherVertex.getAllCollectionKeys();
        return Algorithms.intersect(collV1, collV2);
    } 
}


