package ericvh.TDT;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Class QueryDoc collects the identification of the tokens forming the core of 
 * the query and their summed informative value. Data used in DocSimilarity to 
 * assess the relevancy of the retained documents.
 * @author Eric Van Horenbeeck
 * Created on 21 juni 2007, 9:52
 *  Revision:
 */
public class QueryDoc
{
    // HashSet with all arcs provided by UnipartiteCore.
    private static HashSet mCoreArcSet;
    // HashSet with the vertex-ids from the informative tokens from the mainArcSet 
    // provided by UnipartiteCore.
    private static HashSet mInformativeTokens;
    // Value of all informative arcs in the mainArcSet.
    private static double mSummedArcWeight;
    
    
    /**  Constructor
     */
    public QueryDoc()
    {    }
    
    /** collectArcData: All the relevant documents found in the dataGraph are
     * compared to the set of arcs collected by the Unipartite core. This method
     * gathers the vertex ids and the informative value of the arcs.
     * @param ia : an ArcsTable instance with the informative arcs of the 
     * documents in the scope of this task as collected by the ApplicationManager.
     */
    public void collectQueryData(ArcsTable ia)
    {
         // Table with informative arcs.
        ArcsTable infoArcTable = ia;
        HashMap informativeIDs = new HashMap();
        // HashSet with main arcs from UnipartiteCore
       // mCoreArcSet = fetchMainArcs();
        // Transforms the arc key strings extracted by the UnipartiteCore into
        // vertices and looks up their informative value.
        Iterator arc_itr = mCoreArcSet.iterator();
        while(arc_itr.hasNext())
        {
            // Retrieves elements from the arc set.
            String arcKey = arc_itr.next().toString();
            // Parses the arc string into two vertices.
            Integer vrtxA = Integer.valueOf(arcKey.substring(0, arcKey.indexOf("*")));
            Integer vrtxB = Integer.valueOf(arcKey.substring(arcKey.indexOf("*") + 1));
            boolean isInfoArc = true;
            double weightA;
            double weightB;
            // The informative value is retrieved from a similar vertex known 
            // by the application.
            weightA = infoArcTable.getVertexValue(vrtxA, 0);
            weightB = infoArcTable.getVertexValue(vrtxB, 0);
            if(weightA == 0.0)  vrtxA = 0;
            else if(weightB == 0.0) vrtxB = 0;
            // At least one vertex needs to be informative.
            if(vrtxA == 0 && vrtxB == 0) isInfoArc = false;
            // Arcs having twice the same token are also ignored.
            if(Objects.equals(vrtxA, vrtxB)) isInfoArc = false;
            // When at least one of its two vertices is informative, 
            // the combined arcs weight is computed and the relevant vertex ID's 
            // are collected in a set.
            if(isInfoArc)
            {
                // Gathers the informative tokens from the query.
                if(vrtxA > 0) informativeIDs.put(vrtxA, weightA);
                if(vrtxB > 0) informativeIDs.put(vrtxB, weightB);
            }
        }
        setSummedWeight(informativeIDs.values());
        setInformativeIDs(informativeIDs.keySet());
    }
    
    /** fetchMainArcs : Asks for the collection of the main arcs extracted by 
     * the UnipartiteCore method.
     * @return a HashSet with arcs (String).
     */
//    private HashSet  fetchMainArcs()
//    {
//        return (HashSet) UnipartiteCore.getMainArcCore();
//    }
    
    /** setInformativeIDs: Casts the vertex keys from the informativeToken map
     * into a HashSet.
     * @param informativeIDs: Set with the id's (Integer) of the retained
     * vertices relevant to the query.
     */
    private void setInformativeIDs(Set informativeIDs)
    {
        mInformativeTokens = new HashSet(informativeIDs);
    }
    
    /** setSummedWeight: Sums the informative value of each token.
     * @param Collection with information value (double) for every informative
     * token from the query.
     */
    private void setSummedWeight(Collection vertexValues)
    {
        Iterator values_itr = vertexValues.iterator();
        while(values_itr.hasNext())
        {
            mSummedArcWeight += ((Double) values_itr.next());
        }
    }
    
    /** getQueryIDSet : Getter
     * @return  HashSet with informative tokens relevant to the query (Integer)
     */
    public static HashSet getQueryIDSet()
    {
        return mInformativeTokens;
    }
    
    /** getSummedArcWeight : Getter
     * @return double with the summed informative value of the arcs in this query.
     */
    public static double getSummedArcWeight()
    {
        return mSummedArcWeight;
    }
    
    /** getCoreArcSet : Getter
     * @return HashSet with arcs (String) provided by UnipartiteCore method.
     */
    public static HashSet getCoreArcSet()
    {
        return mCoreArcSet;
    }
    
}

