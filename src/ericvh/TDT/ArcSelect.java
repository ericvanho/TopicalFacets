package ericvh.TDT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class  ArcSelect  Used by DocSelect and DocCompare to get meaningful arc 
 * sets from documents. Transferred from the old DocSimilarity class.
 * @author Eric Van Horenbeeck
 * Created on March 19, 2005, 5:18
 *  Revision: 11 juli 2007, 11:31
 */
public class ArcSelect
{
    // Table with informative arcs in the scope of this task.
    private ArcsTable mInfoArcTable;
    // Map with the informative arcs from the dataGraph in the scope of this task.
    private  Map mAllArcMap;
    // Set with  token-types (Integer) from the query.
    private Set mQueryVertices;
    // The minimum number of query tokens required in a document to be related 
    // to the prototype.
    private long mMinimum;
    
    /**  Constructor
     * @param ia ArcsTable
     */
    public ArcSelect(ArcsTable ia)
    {
        setInfoArcTable(ia);
        setQueryVertices(TopicRetriever.getSeedTokens());
    }
    
    /** selectArcs: Returns a limited number of valuable arcs keys for the 
     * doc-by-doc similarity matrix to speed up the processing. Keys are valuable
     * if their info-value is larger than the document mean or if they are 
     * similar to the arcs of the query. Arcs to the left and the right of a
     * selected arc are added (if they exist) in order to enhance the 'walk' 
     * lengths. Vertices are sent along aswell.
     * @param docInt : identification (Integer) from the document in view.
     * @return Array with two sets: one with a counted number of valuable 
     * arc keys (String) and a second with the vertices (Integer) from the 
     * arcs from the second document.
     */
    public Object[] selectArcs( Integer docInt)
    {
        // When the document is a prototype, only query related arcs are
        // retrieved.
        boolean plainDoc = true;
        if(docInt.equals(DocSelect.getPrototype())) plainDoc = false;
        // Tolerance.
        double meanValue = 0.001;
        // A document can have over a thousand arcs making matrix calculations 
        // cumbersome. Therefore the number of arcs that can participate is 
        // restricted. When this limit applies, only arcs with a 
        // more-than-average info-value are selected.
        int maxArcs = 350;
        int totCount = 0;
        int queryCount = 0;
        //  A number of extra arcs are put to the left and the right of an arc 
        // from a restricted document to enhance its walk-length.
        int extraArcs = 2 ;
        // Temporary containers.
        SortedSet newArcSet = new TreeSet();
        SortedSet tmpArcSet = new TreeSet();
        Set tmpVertices;
        Set queryVertices = getQueryVertices();
        SortedSet docVertices = new TreeSet();
        Object[] arcsAndVertices = new Object[2];
        // Location of the arcs in a document.
        SortedMap positionMap = getInfoArcTable().getArcPositions(docInt);
        // All the arcs in this document.
        Map thisArcMap = (HashMap) getAllArcMap().get(docInt);
        Set arcs = new HashSet(thisArcMap.keySet());
        // The average informative value of a document.
        int arcCount = arcs.size();
        if(plainDoc) meanValue = (getInfoArcTable()
                .summedArcWeights(arcs, docInt) / arcCount);
        // Iteration over the arcs of this document.
        Iterator arc_itr = arcs.iterator();
        while(arc_itr.hasNext())
        {
            String thisKey = (String) arc_itr.next();
            boolean keyAdded = false;
            tmpArcSet.add(thisKey);
            tmpVertices = getInfoArcTable().getVertexIds(tmpArcSet, docInt);
            
            // Ensures that document arcs similar to the query arcs are not 
            // thrown away because of a low information value.           
            if(Algorithms.intersect(queryVertices, tmpVertices).size() > 0)
            {
                newArcSet.add(thisKey);
                docVertices.addAll(tmpVertices);
                queryCount += tmpVertices.size() ;
                keyAdded = true;
            }
            // Only arcs with an info-value >= the mean of the document 
            // info-value are retained.
            else if (plainDoc && getInfoArcTable().getArcValue(thisKey,
                    docInt) >= meanValue)
            {
                newArcSet.add(thisKey);
                docVertices.addAll(tmpVertices);
                totCount += tmpVertices.size() ;
                keyAdded = true;
            }
            //Tries to add some flanking arcs to the left and to the right of 
            // the selected arc.
            if(keyAdded)
            {
                Arc thisArc = (Arc) thisArcMap.get(thisKey);
                int thisPosition = thisArc.getArcPosition();
                for(int i = 1; i < extraArcs; i++)
                {
                    if(positionMap.containsKey(thisPosition - i))
                    {
                        Arc leftArc = (Arc) positionMap.get(thisPosition - i);
                        newArcSet.add(leftArc.getArcId());
                        totCount++;
                    }
                    if(positionMap.containsKey(thisPosition + i))
                    {
                        Arc rightArc = (Arc) positionMap.get(thisPosition + i);
                        newArcSet.add(rightArc.getArcId());
                        totCount++;
                    }
                }
            }
            if((totCount + queryCount) >= maxArcs) break;
            tmpArcSet.clear(); tmpVertices.clear();
        }
        //  A document related to the prototype should have a minimum number
        // of query tokens.
        if(queryCount < mMinimum)
        {
            newArcSet.clear();
            docVertices.clear();
        }
        arcsAndVertices[0] = newArcSet;
        arcsAndVertices[1] = docVertices;
        
        return arcsAndVertices;
    }
    
    /** getInfoArcTable: access to the general arc table with all data inside 
     * the scope of the query.
     * @return
     */
    private ArcsTable getInfoArcTable()
    {
        return mInfoArcTable;
    }
    
    /** setInfoArcTable: alowing access to the general arc table with all 
     * data inside the scope of the query.
     * @param ArcsTable.
     */
    private void setInfoArcTable(ArcsTable ia)
    {
        this.mInfoArcTable = ia;
        mAllArcMap = mInfoArcTable.getFullArcsMap();
    }
    
    /** getAllArcMap: map with arcs from one document.
     *  @return map (HashMap) with arcKey (String) as key and arc (Arc) as
     * value of the document  in view.
     */
    private Map getAllArcMap()
    {
        return mAllArcMap;
    }
    
    /** setQueryVertices: sets the informative token-types from the original 
     * query and the minimum number of informative(!) query tokens expected 
     * from a document related to the prototype.
     * @param Set querySet with Integers
     */
    private void setQueryVertices(Set querySet)
    {
        mQueryVertices = querySet;
        int querySize = mQueryVertices.size();
        if(querySize < 4) mMinimum = querySize;
        else mMinimum = Math.round(querySize/2); // 5
    }
    
    /** getQueryVertices: returns the token-types from the original query.
     * @return Set querySet with Integers
     */
    private Set getQueryVertices()
    {
        return mQueryVertices;
    }
    
    /** getMinimumQuery : returns the minimum number of tokens equal to the
     * query required from a document related to the prototype.
     * @return long number of tokens.
     */
    private long getMinimumQuery()
    {
        return mMinimum;
    }
    
    /** getConnectedArcs: a set of arcs forming a walk.
     * @param arcSet Set with Strings
     * @param docInt Integer
     * @param walk the minimum number of connected arcs expected to be 
     * recognized as a walk (int)
     * @return LinkedHashSet
     */
    public LinkedHashSet getConnectedArcs(Set arcSet, Integer docInt, int walk)
    {
        return getInfoArcTable().connectArcs(arcSet, docInt, walk);
    }
    
    /** getSummedVertexWeights: informative value of all the vertices involved 
     * in a walk.
     * @param vertexSet Set
     * @param docInt Integer
     * @return double Informative weight of vertices in the argument.
     */
    public double getSummedVertexWeights(Set vertexSet, Integer docInt)
    {
        return getInfoArcTable().summedVertexWeights(vertexSet, docInt);
    }
    
}
