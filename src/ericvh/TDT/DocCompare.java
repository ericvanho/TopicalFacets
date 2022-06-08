package ericvh.TDT;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class  DocCompare iterates in a double loop over sets with informative arcs 
 * from retained documents collected during a first iteration by the DocSelect class.
 * Controlled by DocAnalysis. Transferred from the old DocSimilarity class.
 * @author Eric Van Horenbeeck
 * Created on March 19, 2005, 5:18
 *  Revision: 11 juli 2007, 11:31
 */
public class DocCompare
{
    // Counts the number of non-zero nodes in this similarity matrix.
    private int mUseful;
    // Summary text string.
    private String mSummary;
    // Map to collect the retained document-ids and their similarity value.
    private HashMap mSimMap;
    // Instance of the ArcSelect class
    private final ArcSelect mArcSelect;
    // Instance of the SimilarityCalc class
    private final SimilarityCalc mSimCalc;
    // SimMatrix is a sparse matrix holding the results of the doc-by-doc similarity calculation.
    private SimilarityMatrix mSimMatrix;
    
    /**  Constructor
     * @param ia an arcsTable
     */
    public DocCompare(ArcsTable ia)
    {
        mArcSelect = new ArcSelect(ia);
        mSimCalc = new SimilarityCalc( ia);
    }
    
    /** docSimilarity: Iterates in a double loop over sets with informative arcs
     * from retained documents collected during a first iteration by the 
     * DocSelect class. Every set is intersected once with every other set except
     * with itself. A similarity matrix is calculated after each iteration. 
     * There are n(n-1)/2 intersections possible, where n is the number of documents.
     * @param recall : HashMap with docInt (Integer) as key and as value an 
     * Object array with similarity data (length 4) prepared by the DocSelect class.
     */
    public void docSimilarity(HashMap recall)
    {
        // A few counters to keep track of the number of matrix cells in a 
        // paired intersection of  documents.
        int totalCells = 0, count = 0;
        setUseful(0);
        // Minimal acceptable limit.
        double tolerance = 0.0001;
        // The minimal number of shared vertices required.
        int vertexShared = 1;
        // Similarity values for the first and second document from the DocSelect class.
        double firstRValue = 0.0;
        double secondRValue = 0.0;
        // Arc sets for the first and second document.
        SortedSet firstArcSet = new TreeSet();
        SortedSet secondArcSet = new TreeSet();
        // Vertex sets for the first and second document.
        SortedSet firstVSet = new TreeSet();
        SortedSet secondVSet = new TreeSet();
        // Walk sets for the first and second document.
        LinkedHashSet firstWalkSet = new LinkedHashSet();
        LinkedHashSet secondWalkSet = new LinkedHashSet();
        // Topical Facet sets for the first and second document.
        SortedSet firstFSet = new TreeSet();
        SortedSet secondFSet = new TreeSet();
        // SimMatrix is a sparse matrix holding the results of the doc-by-doc similarity calculation.
        setSimMatrix(new SimilarityMatrix());
        // Prepares a map with docId and non-zero similarity value.
        setSimMap(new HashMap());
        // Initializes maps needed for the matrix calculations.
        HashMap recallArcMap = new HashMap(recall);
        // Collects all the document-facet data from the TopicRetriever.
        SortedMap facetMap = new TreeMap();
        facetMap = TopicRetriever.getDocFacets();
        // Set to retain documents that are found to be related to the prototype document.
        Set recallSet = recallArcMap.keySet();
        // Initializing the loops to visit pairwise all the available documents.
        Object[] docLoop1, docLoop2;
        docLoop1  = new Object[recallSet.size()];
        docLoop1 = recallSet.toArray();
        Arrays.sort(docLoop1);
        // Both loops contain the same set of documents.
        docLoop2 = docLoop1;
        
        // The first loop over the documents.
        for(int i = 0; i < docLoop1.length; i++)
        {
            Integer firstDoc = (Integer) docLoop1[i];
            // Getting the arc keys, vertices, the recall similarity value and the 
            // topical facets for the first document as collected during the first 
            // iteration by the DocSelect class.
            Object[] setArray1 = (Object[])  recallArcMap.get(firstDoc);
            firstArcSet = (TreeSet) setArray1[0];
            firstVSet = (TreeSet) setArray1[1];
            firstWalkSet = (LinkedHashSet) setArray1[2];
            firstRValue = ((Double) setArray1[3]);
            setArray1 = null;
            // Topical facets from the first document.
            firstFSet = (TreeSet) facetMap.get(firstDoc);
            // Informative value of the retained vertices from the first document.
            double setWeight1 =  mArcSelect.getSummedVertexWeights(firstVSet, firstDoc);
            
            // The second loop.
            for(int j = 0; j < docLoop2.length; j++)
            {
                // Intersects the first set of vertices with the second set, except when the first and second keys point
                // to each other or when this intersection has been done already: obviously intersecting set 1 and 2
                // and intersecting set 2 and 1 produces the same result.
                if( i < j)
                {
                    Integer secondDoc = (Integer) docLoop2[j];
                    // Counts the matrix cells that have been visited.
                    totalCells++;
                    ApplicationManager.setProgessLabel("Computing similarity between doc " +   firstDoc + " and " +
                        secondDoc);
                    // Boolean flag to indicate a relation between two documents.
                    boolean haveRelation = false;
                    // Getting the arc keys, vertices and recall similarity value for the second document collected in the
                    // first iteration by the DocSelect class.
                    Object[] setArray2 = (Object[])  recallArcMap.get(secondDoc);
                    secondArcSet = (TreeSet) setArray2[0];
                    secondVSet = (TreeSet) setArray2[1];
                    secondWalkSet = (LinkedHashSet) setArray2[2];
                    secondRValue = ((Double) setArray2[3]);
                    setArray2 = null;
                    // Intersects sets of both documents if they are not empty and if at least one of the sets has more
                    // than one element. Arcs shared by either document are collected.
                    int firstSize = firstVSet.size();
                    int secondSize = secondVSet.size();
                    SortedSet sharedVSet = new TreeSet();
                    if((firstSize > 1 && secondSize > 0) || (firstSize > 0 && secondSize > 1))
                    {
                        sharedVSet = new TreeSet(Algorithms.intersect(firstVSet, secondVSet));
                        if(sharedVSet.size() > vertexShared) haveRelation = true;
                    }
                    // The analysis continues if there is a minimal common ground between the two documents.
                    if(haveRelation)
                    {
                        // Vertex similarity (simV) indicates how many vertices the two documents have in common.
                        // Weighted values (from the modified TDIDF) of arcs are used here and in the other similarity
                        // components, not their binary occurrence (0,1).
                        double sharedWeight = mArcSelect.getSummedVertexWeights(sharedVSet, secondDoc);
                        double setWeight2 = mArcSelect.getSummedVertexWeights(secondVSet, secondDoc);
                        // Delta corrects skewed set sizes. It is used in the simV and simW algorithms.
                        double deltaV = mSimCalc.skewNormalize(setWeight1, setWeight2);
                        double simV = mSimCalc. simMeasure(sharedWeight, setWeight1 + setWeight2 + deltaV);
                        // Gets the weight of the shared vertices connected to the first walk.
                        double connect1 = mSimCalc.weaklyConnected(sharedVSet, firstWalkSet, firstDoc);
                        double weakVertexValue1 = mSimCalc.getWeakVertexWeight();
                        mSimCalc.resetWeakVertexWeight();
                        // Gets the weight of the shared vertices connected to the second walk.
                        double connect2 = mSimCalc.weaklyConnected(sharedVSet, secondWalkSet, secondDoc);
                        double weakVertexValue2 =mSimCalc.getWeakVertexWeight();
                        mSimCalc.resetWeakVertexWeight();
                        // Delta is subtracted from simW to rectify a skewed connection.
                        deltaV = mSimCalc.skewNormalize(connect1, connect2);
                        // Walk similarity (simW) indicates how similar the walks between the vertices are. A correction
                        // for skewed sets is applied.
                        double simW = mSimCalc.simMeasure(Math.sqrt(weakVertexValue1 * weakVertexValue2),
                            connect1 + connect2 + deltaV);
                        // A similarity matrix has cells with the similarity information components of doc 1 and doc 2.
                        // The cells know how to calculate the final similarity value.
                        // The alpha coefficient expresses the ratio of information that is delivered by the vertices
                        // proper as compared to the information coming from vertices that are connected by a walk.
                        double alpha = mSimCalc.simMeasure(sharedWeight, 2 * sharedWeight + connect1 +
                            connect2);
                        // Each cell also keeps a set of shared vertices and a set of  shared topical facets.
                        secondFSet = (TreeSet) facetMap.get(secondDoc);
                        Set sharedFSet = new HashSet();
                        sharedFSet = Algorithms.intersect(firstFSet, secondFSet);
                        DocNode cell;
                        cell = new DocNode(firstDoc, secondDoc, simV, simW, alpha, sharedVSet, sharedFSet);
                        getSimMatrix().addDocNode(cell);
                        // Documents with a non-zero similarity are saved for export to the Clusters class.
                        double simValue = cell.getDocSimilarity();
                        if(simValue > tolerance)
                        {
                            // Counts the matrix cells having a usefull non-zero content.
                            setUseful(getUseful() + 1);
                            // The similarity value of the document with the prototype is saved.
                            if(!getSimMap().containsKey(firstDoc)) getSimMap().put(firstDoc, firstRValue);
                            if(!getSimMap().containsKey(secondDoc)) getSimMap().put(secondDoc, secondRValue);
                        }
                    }
                }
            }
        }
        // Prepares a short statistical statement.
        prepareSummary(totalCells, getUseful());
    }
    
    /** prepareSummary: General information about the similarity matrix: files involved, number of cells used.
     * @param totalCells : the number of cells visited (int).
     * @param useful : the number of cells actually used (int).
     */
    private void prepareSummary(int totalCells, int useful)
    {
        // By solving the quadratic equation for n(n-1)/2 the number of files used in this similarity
        // matrix is found.
        double mtrxFiles = (StrictMath.sqrt((totalCells * 2 * 4) + 1) + 1) / 2;
        DecimalFormat nf = new DecimalFormat("###,###");
        NumberFormat pf = NumberFormat.getPercentInstance();
        pf.setMinimumFractionDigits(2);
        int allDocs = ApplicationManager.getDocTable().getDocCount();
        int availableFiles = TopicRetriever.getDocPathSet().size();
        setSummary("<html><center>Total files in this dataGraph: " +  nf.format(allDocs) +
            " &nbsp Relevant files: " +  nf.format(availableFiles) + "<br> &nbsp Retained: " +
            nf.format(mtrxFiles) + " &nbsp Not used: " + nf.format(availableFiles - mtrxFiles) +
            " &nbsp Cells in the matrix: " + nf.format(totalCells) + "<br> &nbsp Cells used: " +
            nf.format(useful) + " &nbsp (" + pf.format(((double) useful) / totalCells) + ") &nbsp " +
            "Cells discarded: " + nf.format(totalCells - useful) + "</center></html>");
    }
    
    /** getUseful : the number of non-empty cells in the similarity matrix containing information that allows
     * computing the similarity value between two documents.
     * @return int useful cells
     */
    public int getUseful()
    {
        return mUseful;
    }
    
    /** setUseful : a counter of cells  in the similarity matrix with useful similarity information.
     * @param int i
     */
    private void setUseful(int i)
    {
        mUseful = i;
    }
    
    /** getSummary : General information about the similarity matrix: files involved, number of cells used.
     * @return String summary
     */
    public String getSummary()
    {
        return mSummary;
    }
    
    /**  setSummary : General information about the similarity matrix: files involved, number of cells used.
     * @param String summary
     */
    private void setSummary(String summary)
    {
        this.mSummary = summary;
    }
    
    /** getSimMap: the retained document-ids and their similarity value.
     * @return HashMap with retained document-ids (Integer) and their similarity value (double).
     */
    public HashMap getSimMap()
    {
        return mSimMap;
    }
    
    /** setSimMap : the retained document-ids and their similarity value.
     * @param HashMap with retained document-ids (Integer) and their similarity value (double).
     */
    private void setSimMap(HashMap simMap)
    {
        this.mSimMap = simMap;
    }
    
    /** getSimMatrix : a sparse matrix holding the results of the doc-by-doc similarity calculation.
     * @return SimilarityMatrix instance
     */
    public SimilarityMatrix getSimMatrix()
    {
        return mSimMatrix;
    }
    
    /** setSimMatrix : instance of the matrix holding the results of the doc-by-doc similarity calculation.
     * @param SimilarityMatrix simMatrix
     */
    private void setSimMatrix(SimilarityMatrix simMatrix)
    {
        this.mSimMatrix = simMatrix;
    }
    
    /** discard: Reclaims memory by discarding collections that are no longer needed.
     */
    public void discard()
    {
        mSimMatrix = null;
        mSimMap = null;
    }
}
