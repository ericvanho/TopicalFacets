package ericvh.TDT;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JOptionPane;


/** Class DocAnalysis prepares clustering of documents using measures based on
 * the similarity between two documents. 
 * See Montes-y-Gomez e.a., "Information Retrieval with Conceptual 
 * Graph Matching" for the background of the algorithm.
 * Is part of the 'Retrieval' task, main collaborating classes: DocSelect 
 * and DocCompare.
 * @author Eric Van Horenbeeck
 * Created on March 19, 2005, 5:18
 * Revision: July 11, 2007
 */
public class DocAnalysis
{
    // Table with informative arcs in the scope of this task.
    private ArcsTable mInfoArcTable;
    // Documents to be used in this similarity method.
    private SortedSet mDocSet ;
    // Instance of the Cluster class.
    private Clusters mCluster;
    // Summary text string.
    private String mSummary;
    // Prototype is a short document that resembles best the query.
    private Integer mPrototype;
    
    /** Constructor
     * @param ia : an ArcsTable instance with informative arcs.
     */
    public DocAnalysis(ArcsTable ia)
    {
        setInfoArcTable(ia);
    }
    
    /** computeMatrix: Computes twice a similarity matrix: once with a prototype
     * document in the first loop and the candidate documents in the second loop 
     * (handled by the DocSelect class); and a second time where the documents 
     * retained in the first phase are evaluated in pairs to establish their
     * mutual relation (handled by the DocCompare class).
     */
    public void computeMatrix()
    {
        // Map with the informative arcs from the dataGraph in the scope of 
        // this task.
        Map allArcMap = getInfoArcTable().getFullArcsMap();
        // Set with the vertices (Integer) of the query. Used when selecting arcs.
        Set queryVertices = TopicRetriever.getSeedTokens();
        // Set with doc-ids to be evaluated.  In the first passage a prototype 
        // document collects all other docs related to it. 
        // Documents without relation to the query, as represented by the 
        // prototype, are removed.
        mDocSet =  findPrototype(queryVertices, TopicRetriever.getDocIdSet());
        // Candidate documents are sorted and put in the main loop array.
        Object[] loop;
        loop = new Object[mDocSet.size()];
        loop = mDocSet.toArray();
        Arrays.sort(loop);
        // First iteration: computing the similarity of loosely related documents
        // with the prototype.
        DocSelect select = new DocSelect(getInfoArcTable());
        select.findSimilarDocs(getPrototype(), loop);
        // Map to collect the arcSets and the similarity value of the retained 
        // documents for use in the next step.
        int recallSetSize = select.getRecallMap().keySet().size();
        //loop = null;
        // During the second visit the documents retained from the first passage
        // are compared with each other to establish their mutual relation. 
        // The Clusters-class will finalize the query by combining documents
        // based on the similarity value established by the DocCompare class.
        if(recallSetSize > 0)
        {
            ApplicationManager.showText("Found " + recallSetSize 
                    + " documents related to the prototype. " +
                "Started with " + mDocSet.size() + " candidates", 0);
            // No longer needs the original document set.
            mDocSet =  null;
            // Second iteration: the document-by-document similarity..
            DocCompare compare = new DocCompare(getInfoArcTable());
            compare.docSimilarity(select.getRecallMap());
            // Eliminates the first similarity matrix and recallArcMap..
            select.discard();
            // Short summary statistics on the operation.
            mSummary = compare.getSummary();
            // Transfers an instance of the SimilarityMatrix class to the 
            // Clusters class.
            mCluster = new Clusters(compare.getSimMatrix());
            // Transfers a HashMap with documents and similarity values to the
            // Clusters class.
            mCluster.setDocMap(compare.getSimMap(), mPrototype);
            // Clusters class gets the number of non-zero cells in this matrix.
            mCluster.setCellCount(compare.getUseful());
            // The set of documents assumed to be relevant to the query is 
            // treated by the Clusters class.
            mCluster.makeClusterMap();
            // Eliminates the second similarity matrix and recallArcMap..
            compare.discard();
        }
        // Warning when no similar documents were found.
        else issueWarning();
    }
    
    /** findPrototype : looks for the smallest document with the highest number 
     * of tokens similar to the query.
     * @param querySet: Set with token-ids (Integer) from the query.
     * @param docSet: SortedSet with the document-ids (Integer).
     * @return SortedSet with documents (Integer) related to the prototype.
     *@ToDo prototype moet kleinste gemeen veelvoud zijn, niet grootste gemene 
     * deler: zoek topical facet dat de meeste termen uit de query dekt. 
     * Haal prototype uit docs related met die TF.
     */
    private SortedSet findPrototype(Set querySet, SortedSet docSet)
    {
        // A candidate prototype should have a minimal resemblance to the query.
        int minSize = (int) StrictMath.max(3, querySet.size()/2);
        Object[] prototype = new Object[]{minSize,0};
        SortedSet docs = new TreeSet(docSet);
        Iterator doc_itr = docs.iterator();
        while(doc_itr.hasNext())
        {
            int docId = (int) doc_itr.next();
            Set arcSet =  getInfoArcTable().getAllArcKeysInDoc(docId);
            Set vertices = getInfoArcTable().getVertexIds(arcSet, docId);
            Set sharedVertices = Algorithms.intersect(vertices, querySet);
            int sharedSize = sharedVertices.size();
            // Documents with no vertices in common with the query are
            // removed here.
            if(sharedSize == 0) docSet.remove(docId);
            else
            {
                // Searching for a good prototype.
                if(sharedSize >= ((int) prototype[0]))
                {
                    prototype[0] = sharedSize;
                    prototype[1] = docId;
                }
            }
        }
        String protoId = "";
        // When no prototype is detected, a doc-by-doc similarity cannot
        // be calculated.
        if( 0 == (int) prototype[0])
        {
            protoId = "none found";
            issueWarning();
        }
        else protoId = String.valueOf(prototype[1]);
        ApplicationManager.showText("Prototype document: #" + protoId, 0);
        setPrototype((int) prototype[1]);
        return docSet;
    }
    
    /** getClusters: Clusters class returns the results of the document cluster
     * assembly.
     * @return Clusters instance.
     */
    public Clusters getClusters()
    {
        return mCluster;
    }
    
    /** getSummary: Returns a summary on the data used to calculate this 
     * similarity matrix. Shown in the doc-by-doc tabbedPane of the GUI by
     * the FinalViewer class.
     * @return String with some information on the similarity matrix calculation.
     */
    public String getSummary()
    {
        return mSummary;
    }
    
    /** getPrototype : returns Integer identifying an existing document that 
     * represents best the query.
     */
    private Integer getPrototype()
    {
        return mPrototype;
    }
    
    /** setPrototype: a document that represents the query.
     * @param prototype: Integer identifying an existing document that 
     * represents best the query.
     */
    private void setPrototype(Integer prototype)
    {
        mPrototype = prototype;
    }
    
    /** getInfoArcTable: access to the general arc table with all data inside 
     * the scope of the query.
     * @return ArcsTable
     */
    private ArcsTable getInfoArcTable()
    {
        return mInfoArcTable;
    }
    
    /** setInfoArcTable:allowing access to the general arc table with all data
     * inside the scope of the query.
     * @param ArcsTable
     */
    private void setInfoArcTable(ArcsTable ia)
    {
        this.mInfoArcTable = ia;
    }
    
    /** issueWarning: Warning message when no clusters could be made for the 
     * task at hand. The ApplicationManager is asked to halt all activity.
     */
    private void issueWarning()
    {
        JOptionPane.showMessageDialog(ApplicationManager.getTabbedPane(), 
                "No similarity clusters could be made for this task.\nRetrain "
                        + "the system with additional data,\n" +
            "or formulate another query.", "Document Similarity", 
            JOptionPane.WARNING_MESSAGE);
        ApplicationManager.updateStatusBar(-1, "");
    }
    
    /** discard: Reclaims memory by discarding collections that are no 
     * longer needed.
     */
    public void discard()
    {
        setInfoArcTable(null);
    }
}
