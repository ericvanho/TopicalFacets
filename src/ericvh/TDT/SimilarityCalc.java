package ericvh.TDT;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class SimilarityCalc. A Collection of methods used by DocSelect and DocCompare
 * to calculate the components for the similarity value. This value is stored 
 * in a DocNode, a cell in the SimilarityMatrix. 
 * Transferred from the old DocSimilarity class.
 * @author Eric Van Horenbeeck
 * Created on March 19, 2005, 5:18
 * Revision: 11 juli 2007, 11:31
 * todo look at Similarity.doc (Karov, 1996 #653) for a possible improvement 
  * of the similarity metric;
 */
public class SimilarityCalc
{
    // Informative value of the weakly connected shared vertices.
    private double mWeakVertexWeight;
    // Table with informative arcs in the scope of this task.
    private final ArcsTable mInfoArcTable;
    
    /**  Constructor
     * @param ia a Table with informative arcs
     */
    public SimilarityCalc(ArcsTable ia)
    {
        mInfoArcTable = ia;
    }
    
    /** simMeasure: Calculates the Dice-coefficient. Gives better result than 
     * the cosine metric.
     * @param commonWeight : the value of the intersection of set 1 and set 2 (double).
     * @param denominator : sum of the value of the nodes in a first and 
     * second set (double).
     * @return a Dice value (double).
     */
    public double simMeasure( double commonWeight, double denominator )
    {
        if(denominator > 0) return  (2 * commonWeight) / denominator ;
        else return 0;
    }
    
    /** skewNormalize: The effect of the skewness factor is zero when both sets
     * are balanced, i.e. when there are as much connections with walk 1 as with
     * walk 2 or when set 1 and set 2 have the same number of elements. One set
     * of zero connections and the other with a maximum would yield a result
     * in simW = 0. Taking only the average would not show the difference: 5 + 5 
     * connections have an identical mean as 0 + 10 connections. Having 2 common
     * elements from a set of 11 and a set of 3 would yield the same value as
     * having 2 common elements from two sets of 7.
     * @param set1 : the value of set 1 (double).
     * @param set2 : the value of set 2 (double).
     * @return a correction factor (double) for the difference between two sets.
     */
    public double skewNormalize(double set1, double set2)
    {
        return StrictMath.abs(set1 - set2) / 2;
    }
    
    /** weaklyConnected: Returns the info value of the vertices that are connected
     * to a 'walk' (arcs that form an uninterrupted 'walk' in one document) 
     * and the value of the walk itself.
     * @param sharedVertices : HashSet with the vertex-ids (Integer) shared by 
     * two documents.
     * @param walkSet : a LinkedHashSet with walks (set of arcs) from one document.
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return connectedWeight the summed weight of the walks having an intersected
     * vertex (double).
     */
    public double weaklyConnected(Set sharedVertices, LinkedHashSet walkSet, 
            Integer docInt)
    {
        double connectedWeight = 0.0;
        boolean isConnected = false;
        Iterator walk_itr = walkSet.iterator();
        while(walk_itr.hasNext())
        {
            LinkedHashSet walk = (LinkedHashSet) walk_itr.next();
            Iterator shared_itr = sharedVertices.iterator();
            while(shared_itr.hasNext())
            {
                Integer vertex = (Integer) shared_itr.next();
                if(isConnected(walk, vertex))
                {
                    // Accumulates this vertex's value.
                    setWeakVertexWeight(getWeakVertexWeight() +
                            mInfoArcTable.getVertexValue(vertex, docInt));
                    isConnected = true;
                }
            }
            // The info value of a walk is counted only once for a given set of
            // connected vertices.
            if(isConnected)
            {
                connectedWeight += walkWeight(walk, docInt);
                // Resets the boolean switch.
                isConnected = false;
            }
        }
        return connectedWeight;
    }
    
    /** isConnected: Iterates over a set of arcs (a walk in the network) and 
     * returns 'true' if a vertex from the
     * intersection is connected to it.
     * @param walkSet : a linkedHashSet of arcs (walk) from one of the intersected 
     * documents (Arc).
     * @param vertex : a vertex-id (Integer) from the intersection of two documents.
     * @return boolean 'true' if this vertex is connceted to the walk.
     */
    private boolean isConnected(LinkedHashSet walkSet, Integer vertex)
    {
        boolean hasVertex = false;
        Iterator walk_itr = walkSet.iterator();
        while(walk_itr.hasNext())
        {
            Arc walkArc = (Arc)walk_itr.next();
            if(walkArc.containsVertex(vertex))
            {
                hasVertex = true;
                break;
            }
        }
        return hasVertex;
    }
    
    /** walkWeight: returns the weight of a set of walks.
     * @param walks : a set with linked arcs (walk).
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return the weight (info value) of these walks (double).
     */
    private double walkWeight(LinkedHashSet walks, Integer docInt)
    {
        double weight = 0;
        Iterator walk_itr = walks.iterator();
        while(walk_itr.hasNext())
        {
            Arc walkArc = (Arc)walk_itr.next();
            weight += mInfoArcTable.getArcValue(walkArc.getArcId(), docInt);
        }
        return weight;
    }
    
    /** resetWeakVertexWeight: resets value to '0'.
     */
    public void resetWeakVertexWeight()
    {
        setWeakVertexWeight(0);
    }
    
    /** getWeakVertexWeight: the informative value of the vertices involved 
     * in a weakly connected walk.
     * @return double weakVertexWeight
     */
    public double getWeakVertexWeight()
    {
        return mWeakVertexWeight;
    }
    
    /** setWeakVertexWeight:  the informative value of the vertices involved 
     * in a weakly connected walk.
     * @param double weakVertexWeight
     */
    private  void setWeakVertexWeight(double weakVertexWeight)
    {
        this.mWeakVertexWeight = weakVertexWeight;
    }
    
}
