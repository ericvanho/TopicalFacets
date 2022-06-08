package ericvh.TDT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JOptionPane;


/** Class Clusters builds clusters by comparing the similarity value between a 
 * cluster candidate and all other related documents. The first step is to extract
 * the main core from the text network. Every document can be the focus of 
 * a document cluster based on the semantic similarity between that focus
 * document and other texts. The first cluster is made starting with the documen
 * most related to the prototype document (having the highest similarity value). 
 * To this cluster new documents are added om two conditions:
 * - there is a relation between the new document and the document starting the cluster ;
 * - the new document has its highest similarity score with this starting document,
 * i.e., no similarity score between the new document and any other document is higher; 
 * When all documents have been evaluated a second cluster is started with the 
 * document having the next highest similarity score with the prototype document.
 * The remaining documents that didn't join the first cluster are scanned again.
 * The process is repeated until all documents are grouped.
 * @author Eric Van Horenbeeck
 * Created on 24 oktober 2005, 20:05
 * Revision: July 25, 2007
 */

public class Clusters
{
    // LinkedHashMap holding clusters of documents related to a focus document.
    private LinkedHashMap mClusterMap;
    // The matrix with the non-zero row-column nodes from the doc-by-similarity
    // calculation.
    private SimilarityMatrix mSimMatrix;
    // Map with the retained documents from the recall-phase sorted on their 
    // similarity value.
    private LinkedHashMap mRetainedMap;
    // A set with the final selected documents
    private LinkedHashSet mRetainedSet;
    // Map of the initial network with for every vertex (document) a set
    // of adjacent documents.
    private Map mNetworkMap;
    // The SortedSet with the doc-ids from the main core.
    private SortedSet mCoreSet;
    // The number of non-zero cells in the similarity matrix.
    private int mCellCount;
    // Prototype is a short document that resembles best the query.
    private Integer mPrototype;
    // A set to collect documents appearing in a cluster.
    private Set mDocUsedSet;
    // Prepares an instance of the VisualGraph class.
    private VisualGraph mVisualGraph;
    
    /** Constructor
     * @param sm : sm is a sparse matrix with the results of the doc-by-doc 
     * similarity calculation.
     */
    public Clusters(SimilarityMatrix sm)
    {
        mSimMatrix = sm;
    }
    
    /** makeClusterMap: Makes a map with document clusters. A warning is issued
     * when no clusters are found. Frequent facet sets can be used in a way similar
     * to the frequent itemset. See 'Frequent Set Mining' B. Goethals.
     */
    public void makeClusterMap()
    {
        ApplicationManager.setProgessLabel("Clustering relevant documents");
        // Initializing maps and sets.
        mClusterMap = new LinkedHashMap();
        mNetworkMap = new HashMap();
        setDocUsedSet(new HashSet());
        // Gets the initial network of relevant documents prepared by the
        // DocAnalysis class.
        collectNetwork();
        if(mNetworkMap.isEmpty()) issueWarning();
        else
        {
            // Extracts the relevant cores in this network and removes all
            // documents from the retained set that are not part of it.
            UnipartiteCore core = new UnipartiteCore();
            core.setCoreNumber(2);
            core.setMainDocCore(mNetworkMap);
            setMainCoreSet( core.getMainDocCore());
            retainCore();
            // The final semantic relation of the retained documents is revealed
            // by clustering the documents around the main core documents sorted 
            // on their similarity from high to low.
            Iterator focus_itr =mRetainedSet.iterator();
            while(focus_itr.hasNext())
            {
                int focusDoc = ((Integer) focus_itr.next());
                if( !getDocUsedSet().contains(focusDoc) && mSimMatrix.
                        getNodeListSize(focusDoc) > 0)
                {
                    collectClusters(focusDoc);
                }
            }
            // FocusDocs with an empty cluster (no docs used) have no significant 
            // relations with other documents and are removed.
            removeUnrelated(Algorithms.difference(mRetainedSet, mDocUsedSet));
            if(mClusterMap.isEmpty()) issueWarning();
        }
    }
    
    /** collectClusters: Collects the documents that are semantically clustered
     * around one focus document,
     * starting with the prototype document.
     * @param focusDoc : the document in focus (int).
     */
    private void collectClusters(int focusDoc)
    {
        SortedSet cluster = new TreeSet();
        // Puts all the nodes found for the focus document in a list and iterates
        // over them.
        LinkedList nodeList = mSimMatrix.extractDocNodes(focusDoc);
        Iterator node_itr = nodeList.iterator();
        while(node_itr.hasNext())
        {
            DocNode thisNode = (DocNode) node_itr.next();
            // Doc-id of the second document in the node with the focus doc.
            int secondDoc = thisNode.getOtherMatrixCoordinate(focusDoc);
            if( !getDocUsedSet().contains(secondDoc) && mRetainedSet.contains(secondDoc))
            {
                // If  secondDoc has its highest similarity value with this focusDoc, 
                // secondDoc is added to the cluster.
                Object[] highValue = mSimMatrix.findHighestValue(secondDoc);
                if (focusDoc == ((Integer) highValue[0]))
                {
                    cluster.add(secondDoc);
                    // A document is added only once to a cluster.
                    getDocUsedSet().add(secondDoc);
                }
            }
            if(cluster.size() > 0)
            {
                cluster.add(focusDoc);
                getDocUsedSet().add(focusDoc);
                addToClusterMap(focusDoc, cluster);
            }
        }
    }
    
    /** addToClusterMap: mClusterMap is a HashMap with as key the focus doc (int)
     * and as value a SortedSet with all the doc-ids (int) that make the cluster. 
     * This set includes the focus document. Before adding the new set, mClusterMap
     * is tested on the existence of an equivalent collection introduced by another
     * focusDoc or on the existence of a set that contains allready the elements 
     * of a smaller cluster.
     * @param newFocus : the id of the document now in view (int).
     * @param newCluster : a SortedSet with document ids (int) related to the new focus.
     */
    private void addToClusterMap(int newFocus, SortedSet newCluster)
    {
        boolean addCluster = true;
        Iterator cluster_itr = mClusterMap.keySet().iterator();
        while(cluster_itr.hasNext())
        {
            int thisFocus = ((Integer) cluster_itr.next());
            SortedSet thisCluster = (TreeSet) mClusterMap.get(thisFocus);
            if(thisCluster.containsAll(newCluster))  addCluster = false;
            else if(newCluster.containsAll(thisCluster))
            {
                thisCluster.addAll(newCluster);
                mClusterMap.put(thisFocus, thisCluster);
                addCluster = false;
            }
        }
        if(addCluster)  mClusterMap.put(newFocus, newCluster);
    }
    
    /**  retainCore :  removes documents from the retained documents that do not
     * belong to the selected cores.
     * @param SortedSet with documents (Integer) belonging to the main core.
     */
    private void retainCore()
    {
        SortedSet coreSet = getMainCoreSet();
        Set removeSet = new HashSet();
        Iterator core_itr = mRetainedMap.keySet().iterator();
        while(core_itr.hasNext())
        {
            Integer coreDoc = (Integer) core_itr.next();
            if(!coreSet.contains(coreDoc)) removeSet.add(coreDoc);
        }
        removeUnrelated(removeSet);
    }
    
    /** removeUnrelated: docs to be removed from the set of retained documents.
     * @param unrelatedDocs Set with document id's (Integer).
     */
    private void removeUnrelated(Set unrelatedDocs)
    {
        // Removes from the mRetainedMap map every unrelated document.
        Iterator remove_itr = unrelatedDocs.iterator();
        while(remove_itr.hasNext())
        {
            Integer removeDoc = (Integer) remove_itr.next();
            mRetainedMap.remove(removeDoc);
        }
        mRetainedSet = new LinkedHashSet(mRetainedMap.keySet());
    }
    
    /** collectNetwork: Constructs a view of the network with for every vertex
     * (document) a set of adjacent vertices (related documents). 
     * The similarity value with the prototype of all retained documents
     * is published on the general info tab of the GUI.
     */
    private void collectNetwork()
    {
        Iterator doc_itr = mRetainedMap.keySet().iterator();
        String tableHeadStr = "\nDocument " + '\t' + "Similarity Value\n" ;
        String lineStr = repeatStr("--", tableHeadStr.length());
        String dataStr = "";
        while(doc_itr.hasNext())
        {
            Set clusterSet = new HashSet();
            Integer focus = (Integer) doc_itr.next();
            int focusDoc = focus ;
            // String with document ID and  the similiarity value.
            dataStr += "\n" + focusDoc + '\t' + mRetainedMap.get(focus);
            // Puts all the nodes found for the focus document in a list and
            // iterates over them.
            LinkedList nodeList = mSimMatrix.extractDocNodes(focusDoc);
            Iterator node_itr = nodeList.iterator();
            while(node_itr.hasNext())
            {
                DocNode thisNode = (DocNode) node_itr.next();
                // Doc-id of the second document in the node with the focus doc.
                int secondDoc = thisNode.getOtherMatrixCoordinate(focusDoc);
                clusterSet.add(secondDoc);
            }
            mNetworkMap.put(focusDoc, clusterSet);
        }
        // Puts the similarity values on the general info tab.
        ApplicationManager.showText(tableHeadStr +lineStr + dataStr, 0 );
    }
    
    /** getAllDocs: Returns a map with all the doc-ids and the similarity values
     * that make up the final document selection for this task.
     * @return LinkedHashMap with final doc-ids (int) as key and the similarity
     * value (double) as value.
     */
    public LinkedHashMap getAllDocs()
    {
        return mRetainedMap;
    }
    
    /** setDocMap: DocAnalysis sends a HashMap with the document-ids and the 
     * matching similarity values that were retained in the DocSelect phase.
     * A LinkedHashMap is prepared with the document-ids sorted from high to low 
     * on this similarity value.
     * @param docMap : HashMap with docIds (Integer) as key and as value the
     * scores (double) from the query-by-document similarity matrix.
     * @param prototype is a short document (Integer) that resembles best the query.
     */
    public void setDocMap(HashMap docMap, Integer prototype)
    {
        Map tmpMap = new HashMap();
        mPrototype = prototype;
        Iterator map_itr = docMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            Integer docId = (Integer) map_itr.next();
            double value = ((Double) docMap.get(docId));
            // Puts the similarity value (key) and the docId (value) in a map.
            // Adds a small random number to an identical key to avoid data loss.
            if(tmpMap.containsKey(value))
            {
                double n = Math.random() / 100000;
                value += n;
            }
            tmpMap.put(value, docId);
        }
        // Sorted on the similarity value.
        TreeMap tm = new TreeMap(simWeight);
        tm.putAll(tmpMap);
        mRetainedMap = new LinkedHashMap();
        // Reversing the map while observing the ordering.
        Iterator tree_itr = tm.keySet().iterator();
        while(tree_itr.hasNext())
        {
            Double simValue = (Double) tree_itr.next();
            Integer docInt = (Integer) tm.get(simValue);
            mRetainedMap.put(docInt, simValue);
        }
    }
    
    /** Comparator sorts the map on the similarity value from high to low.
     */
    private final Comparator simWeight = (Comparator) (Object o1, Object o2) -> 
    {
        Double w1 = (Double) o1;
        Double w2 = (Double) o2;
        return w2.compareTo(w1);
    };
    
    /** setCellCount: DocAnalysis sends the number of non-zero cells in the
     * active similarity matrix.
     * @param cells : the number of cells (int).
     */
    public void setCellCount(int cells)
    {
        mCellCount = cells;
    }
    
    /** getFinalMatrix: Sparse matrix with the results of the doc-by-doc 
     * similarity calculation.
     * @return SimilarityMatrix instance.
     */
    public SimilarityMatrix getFinalMatrix()
    {
        return mSimMatrix;
    }
    
    /** getMainCoreSet: returns the doc-ids that make up the main core.
     * @return SortedSet with doc-ids (Integer).
     */
    private SortedSet getMainCoreSet()
    {
        return mCoreSet;
    }
    
    /** setMainCoreSet: Setter of  the doc-ids that make up the main core.
     * @param mainCoreSet : Set with the doc-ids (Integer) from the main core only.
     */
    private void setMainCoreSet(Set mainCoreSet)
    {
        mCoreSet =  new TreeSet(mainCoreSet);
    }
    
    /** getPrototype : returns the prototype document used to represent the query.
     * @return Integer prototype document identification.
     */
    public Integer getPrototype()
    {
        return mPrototype;
    }
    
    /** prepareVisualGraph: Initializes the VisualGraph class and sends a 
     * HashMap with the focus doc (int) as key and a SortedSet of doc-ids (int)
     * as value and a HashSet with doc-ids that make up the
     * main core (Integer).
     */
    public void prepareVisualGraph()
    {
        mVisualGraph = new VisualGraph();
        mVisualGraph.setMainCore(mRetainedSet);
        mVisualGraph.setClusterMap(getClusterMap());
        mVisualGraph.setTaskName(TopicRetriever.getTaskName());
    }
    
    /** getVisualGraph: returns this prepared instance of the VisualGraph to
     * the ApplicationManager.
     * @return instance of VisualGraph.
     */
    public VisualGraph getVisualGraph()
    {
        return mVisualGraph;
    }
    
    /** getClusterMap: Returns a map where the value is a set of documents that
     * make up a cluster.
     * @return LinkedHashMap with the focus doc (int) as key and a SortedSet of
     * doc-ids (int) as value.
     */
    public LinkedHashMap getClusterMap()
    {
        return mClusterMap;
    }
    
    /** getDocUsedSet
     * @return Set with docIds (int) used in all similarity clusters.
     */
    public Set getDocUsedSet()
    {
        return mDocUsedSet;
    }
    
    /** setDocUsedSet
     * @param docUsedSet
     */
    public void setDocUsedSet(Set docUsedSet)
    {
        mDocUsedSet = docUsedSet;
    }
    
    /** issueWarning: Warning message when no clusters could be made for the 
     * task at hand. The ApplicationManager is asked to halt all activity.
     */
    public void issueWarning()
    {
        JOptionPane.showMessageDialog(ApplicationManager.getTabbedPane(),
                "No similarity clusters could be made for this task.\nRetrain"
                        + " the system with additional data,\nor formulate another"
                        + " query.", "Document Clustering", 
                        JOptionPane.WARNING_MESSAGE);
        ApplicationManager.updateStatusBar(-1, "");
    }
    
    /** discard: Reclaims memory by discarding a collection that is no longer 
     * needed and by forcing a garbage collection.
     */
    public void discard()
    {
        mSimMatrix = null;
        System.gc();
    }
    
    /** repeatStr: a line (String) with 'repeat'  times a copy of the 'inputStr'
     * string.
     * @param inputStr String as input for a line of identical copies.
     * @param repeat int length of the line.
     * @return a String of length 'repeat' with a NewLine.
     * todo method should go to the Algorithm class
     */
    public String repeatStr(String inputStr, int repeat)
    {
        StringBuilder buf=new StringBuilder();
        for ( int i=0; i<repeat; i++)
        {
            buf.append(inputStr);
        }
        return buf.toString();
    } 
    
}
