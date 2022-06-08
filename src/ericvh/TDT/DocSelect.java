package ericvh.TDT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class DocSelect iterates over sets with informative arcs from documents. 
 * Every set is intersected once with the set from the prototype. 
 * A similarity matrix is calculated after each iteration. Arc sets and vertices
 * of the candidate documents are saved to allow their fast retrieval during 
 * the second pass by the DocCompare class. Controlled by DocAnalysis. 
 * Transferred from the old DocSimilarity class.
 * @author Eric Van Horenbeeck
 * Created on March 19, 2005, 5:18
 *  Revision: 11 juli 2007, 11:31
 */
public class DocSelect
{
    // The prototype document representing the query.
    private static Integer mPrototype;
    // Map to collect  the arcSets and the similarity value of the retained 
    // documents for use by the DocCompare
    // class.
    private HashMap mRecallArcMap;
    // Instance of the ArcSelect class
    private final  ArcSelect mArcSelect;
    // Instance of the SimilarityCalc class
    private final  SimilarityCalc mSimCalc;
    // SimMatrix is a sparse matrix holding the results of the doc-by-doc 
    //similarity calculation.
    private SimilarityMatrix mSimMatrix;
    
    /**  Constructor
     * @param ia
     */
    public DocSelect(ArcsTable ia)
    {
        mArcSelect = new ArcSelect(ia);
        mSimCalc = new SimilarityCalc(ia);
    }
    
    /** findSimilarDocs: Iterates over sets with informative arcs from documents.
     * Every set is intersected once with the set from the prototype . 
     * A similarity matrix is calculated after each iteration. Arc sets and
     * vertices of the candidate documents are saved to allow their fast 
     * retrieval during the second pass by the DocCompare class.
     * @param prototype.
     * @param loop : array with doc-ids (Integer).
     */
    public void findSimilarDocs(Integer prototype, Object[] loop)
    {
        // Minimal acceptable limit.
        double tolerance = 0.001;
        // The minimal number of shared vertices required.
        int vertexShared = 3;
        // The minimum walk length: number of consecutive arcs required.
        int minWalk = 3;
        // Arc sets for the first and second document.
        SortedSet firstArcSet;
        SortedSet secondArcSet;
        // Vertex sets for the first and second document.
        SortedSet firstVSet;
        SortedSet secondVSet;
        // Walk sets for the first and second document.
        LinkedHashSet firstWalkSet;
        LinkedHashSet secondWalkSet;
        // Topical Facet sets for the first and second document.
        SortedSet firstFSet = new TreeSet();
        SortedSet secondFSet = new TreeSet();
        // Map to collect  the arcSets and the similarity value of the retained
        // documents for use by the DocCompare class.
        mRecallArcMap = new HashMap();
        // SimMatrix is a sparse matrix holding the results of the doc-by-doc 
        // similarity calculation.
        mSimMatrix = new SimilarityMatrix();
        // The first  document is the prototype.
        setPrototype(prototype);
        Integer firstDoc = getPrototype();
        // Initializing the loop to compare all the available documents with the
        // prototype document.
        Object[] docLoop;
        docLoop = loop;
        // Arcs and vertices from the protoype document.
        Object[] sets = mArcSelect.selectArcs(firstDoc);
        firstArcSet = (TreeSet) sets[0];
        firstVSet = (TreeSet) sets[1];
        
        // FirstWalkSet is a set of walks with uninterrupted sequences of vertices
        // connected by a minimun number of arcs or by all the arcs when the
        // document is short. The sequence is defined as a walk because it has 
        // direction and the vertices can be the same, altough without self-loops.
        firstWalkSet = mArcSelect.getConnectedArcs(firstArcSet, firstDoc, minWalk);
        double setWeight1 = mArcSelect.getSummedVertexWeights(firstVSet, firstDoc);
        
        // The  loop.
        for (Object docLoop1 : docLoop) 
        {
            Integer secondDoc = (Integer) docLoop1;      
            // Boolean flag to indicate the relation between two documents.
            boolean haveRelation = false;
            // Arcs and vertices from the second document.
            Arrays.fill(sets, null);
            sets = mArcSelect.selectArcs(secondDoc);
            secondArcSet = (TreeSet) sets[0];
            secondVSet = (TreeSet) sets[1];
            // Constructs a set of uninterrupted sequences of vertices (walk)
            // with arcs from the second document.
            secondWalkSet = mArcSelect.getConnectedArcs(secondArcSet, secondDoc,
                    minWalk);      
            // Intersects the first set of vertices from the prototype with the 
            // second set if they are not empty and if at least one of the sets 
            // has more than one element. Arcs shared by either document are
            // collected.
            int firstSize = firstVSet.size();
            int secondSize = secondVSet.size();
            SortedSet sharedVSet = new TreeSet();
            if((firstSize >= 1 && secondSize > 0) || (firstSize > 0 
                    && secondSize >= 1))
            {
                sharedVSet = new TreeSet(Algorithms.intersect(firstVSet,
                        secondVSet));
                if(sharedVSet.size() > vertexShared) haveRelation = true;
            }
            // Continues if there is a minimal common ground between two documents.
            if(haveRelation)
            {
                // Vertex similarity (simV) indicates how many vertices the two
                // documents have in common. Weighted values (from the modified
                // TDIDF)of arcs are used here and in the other similarity 
                // components, not their binary occurrence (0,1).
                double sharedWeight = mArcSelect
                        .getSummedVertexWeights(sharedVSet, secondDoc);
                double setWeight2 = mArcSelect
                        .getSummedVertexWeights(secondVSet, secondDoc);
                // Delta corrects skewed set sizes. It is used in the simV and 
                // simW algorithms.
                double deltaV = mSimCalc.skewNormalize(setWeight1, setWeight2);
                double simV = mSimCalc. simMeasure(sharedWeight, setWeight1
                        + setWeight2 + deltaV);
                // Gets the weight of the shared vertices connected to the first
                // walk.
                double connect1 = mSimCalc.weaklyConnected(sharedVSet, 
                        firstWalkSet, firstDoc);
                double weakVertexValue1 = mSimCalc.getWeakVertexWeight();
                mSimCalc.resetWeakVertexWeight();
                // Gets the weight of the shared vertices connected to the 
                // second walk.
                double connect2 = mSimCalc.weaklyConnected(sharedVSet
                        , secondWalkSet, secondDoc);
                double weakVertexValue2 =mSimCalc.getWeakVertexWeight();
                mSimCalc.resetWeakVertexWeight();
                // Delta is subtracted from simW to rectify a skewed connection.
                deltaV = mSimCalc.skewNormalize(connect1, connect2);
                // Walk similarity (simW) indicates how similar the walks between 
                // the vertices are. A correction for skewed sets is applied.
                double simW = mSimCalc.simMeasure(StrictMath
                        .sqrt(weakVertexValue1 * weakVertexValue2),
                        connect1 + connect2 + deltaV);
                // The similarity information components are saved in a matrix
                // cell. This cell knows how to calculate its final similarity
                // value. Only cells with a walk similarity > 0 are entered.
                // This guarantees that the similarity of two documents is not 
                // only based on loose words, but also on sharing semantically
                // related constructions. The similarity matrix cell keeps a set 
                // of shared vertices.
                if(simW > tolerance)
                {
                    // The alpha coefficient expresses the ratio of information 
                    // that is delivered by the vertices proper as compared to 
                    // the information coming from vertices that are connected 
                    // by a walk.
                    double alpha = mSimCalc.simMeasure(sharedWeight, 
                            2 * sharedWeight + connect1 + connect2);
                    // Here the shared facet set is a placeholder, it is not 
                    // used in the computation of the simValue.
                    Set sharedFSet = new HashSet();
                    DocNode cell;
                    cell = new DocNode(firstDoc, secondDoc, simV, simW, alpha, 
                            sharedVSet, sharedFSet);
                    mSimMatrix.addDocNode(cell);
                    double simValue = cell.getDocSimilarity();
                    
                    // If a document proves to be related to the prototype, 
                    // it is saved together with all its relevant data sets.
                    // These will be reused by the DocCompare class to evaluate 
                    // the relationship between the retained documents.
                    if(simValue > tolerance)
                    {
                        setRecallData(secondDoc, secondArcSet, secondVSet, 
                                secondWalkSet, simValue);
                    }
                }
            }
        }
    }
    
    /** setRecallData: Prepares a map to hold three sets for reuse by the
     * DocCompare class and one value expressing the similarity of a document
     * with the query as represented by the prototype document.
     * @param docInt : the id (Integer) of the retained document.
     * @param arcSet : a SortedSet with arc keys (String) from this retained 
     * document.
     * @param vertexSet : a SortedSet with vertices (Integer).
     * @param walkSet : LinkedHashSet with 'walk' (String).
     * @param simValue : the similarity value (double) of this document 
     * compared to the query document.
     */
    private void setRecallData(Integer docInt, SortedSet arcSet, 
            SortedSet vertexSet, LinkedHashSet walkSet, double simValue)
    {
        Object[] setArray = new Object[4];
        setArray[0] = arcSet;
        setArray[1] = vertexSet;
        setArray[2] = walkSet;
        setArray[3] = simValue;
        getRecallMap().put(docInt, setArray);
    }
    
    /** getRecallMap Container with data for reuse by the DocCompare class.
     * @return HashMap recallArcMap docInt (Integer) as key and as value an
     * Object array with similarity data.
     */
    public HashMap getRecallMap()
    {
        return mRecallArcMap;
    }
    
    /** discard: Reclaims memory by discarding collections that are no longer 
     * needed.
     */
    public void discard()
    {
        mRecallArcMap = null;
        mSimMatrix = null;
    }

    /** getPrototype Getter of the prototype, a document representing the query.
     * @return Integer prototype ID
     */
    public static Integer getPrototype()
    {
        return mPrototype;
    }

    /** setPrototype setter of the prototype, a document representing the query.
     * @param Integer prototype ID
     */
    private void setPrototype(Integer prototype)
    {
        mPrototype = prototype;
    }
}
