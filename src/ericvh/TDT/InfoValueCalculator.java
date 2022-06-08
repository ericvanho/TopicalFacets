package ericvh.TDT;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


/** Class InfoValueCalculator computes the informative weight of a token and the
 * cutOff value for an entire collection. It is the second step (BUILD) 
 * of the TDT-procedure. The cutOff value divides common words from informative .
 * tokens. The cutOff value is the mean of the summed weight of the tokens in a 
 * collection. The calculation of a normalization factor allows comparison of 
 * infovalues from different collections. Both are calculated in the 
 * CollectionData class.For each text the number of tokens or the number of
 * links is counted. The individual frequency of a token is used as a partial
 * indication of its informative importance (tf). This 'weight' is damped by an
 * inverse relation between the number of texts wherein the token is used and 
 * all texts with the same date and source (idf). This to reflect that words 
 * used frequently in other texts carry less information on a specific subject. 
 * Words with a high TfIdf mark are relatively rare in a given collection and 
 * thus tend to tell something about the content of that text. 
 * @author  Eric Van Horenbeeck
 * Created on 14 juli 2004, 18:47
 * Revision: Sep 26, 2006
 */
public class InfoValueCalculator implements Serializable
{
    // Vertex a point in this graph.
    private Vertex mCurrentVertex;
    // Table with collections and its members.
    private final CollectionTable mCollT;
    // Table with information on documents.
    private final DocTable mDocT;
    // Central repostory of all token counters.
    private final TokenCount mTokenCount;
    // Table with label information.
    private final LabelTable mLabelT;
    // LinkTable to find links per vertex in a document.
    private final LinkTable mLinksT;
    // Flag to choose between the classic token frequency or the token link
    // based frequency.
    private final boolean classic;
    
    
    /** Constructor
     * @param tc : tokenCount table
    * todo Replace tf*idf with core extraction. The core number serves as 
    * selection tool for more or less features.
     */
    public InfoValueCalculator(TokenCount tc)
    {
        mTokenCount = tc;
        mCollT = ApplicationManager.getCollTable();
        // CollectionTable assembles existing keys to remove old calculated 
        // info values.
        mCollT.setOldValues();
        mDocT = ApplicationManager.getDocTable();
        mLabelT = ApplicationManager.getLabels();
        mLinksT = ApplicationManager.getLinkTable();
        // 'true' = classic Word Frequency, 'false' = Token Link Frequency.
        classic = false;
    }
    
    /** computeVertexInfoValue: Scans over documents in every collection of this 
     * dataGraph to calculate and set the final info value of every vertex. 
     * Arc weights are left as they are at this stage: a common vertex in an 
     * arc would dominate over the informative one. The method is called by the
     * ApplicationManager after all the individual textgraphs (files on disk)
     * have been processed.
     */
    public void computeVertexInfoValue()
    {
        // Map holding all the vertices in this dataGraph.
        Map mVertexMap = ApplicationManager.getVerticesTable().getFullVerticesMap();
        // Calculates the vertex weights (informative TFIDF value inside this
        // dataGraph).
        Iterator vertex_itr = mVertexMap.keySet().iterator();
        while(vertex_itr.hasNext())
        {
            Integer vertexId = (Integer) vertex_itr.next();
            mCurrentVertex = (Vertex) mVertexMap.get(vertexId);
            // Digits can be excluded from further participation.
            // if(!mLabelT.isDigit(vertexId)
            computeInfoValue(vertexId);
        }
        // Instructs the CollectionData class to calculate the final cutOff value
        // for every collection in this dataGraph that is used in the scope of 
        // this session.
        mCollT.setCutOff();
    }
    
    /** computeInfoValue: Computes a modified TfIdf metric for this vertex over
     * all the collections it is involved in. The info value is copied to the 
     * tokenWeight map of the vertex where it replaces the temporary figure. 
     * The value is also passed to collectionData to compute the overall cutOff 
     * value (division between common tokens and the informative ones) and the 
     * normalization factor that allows to compare infovalues from different 
     * collections.
     */
    private void computeInfoValue(Integer vertexId)
    {
        // Visits every collection where this vertex is used.
        Set collectionSet = new HashSet(mCurrentVertex.getAllCollectionKeys());
        Iterator collection_itr = collectionSet.iterator();
        while (collection_itr.hasNext())
        {
            // Token or Link frequency.
            double tokenFreq;       
            // Relative frequency: TF over all tokens in this document.
            double meanTokenFreq = 0.0;   
            // The mean highest frequency or number of links in a document.
            double meanMaxFreq = 0.0;    
            // The mean of all tokens in the documents in a collection.
            double meanTotalTokens = 0.0; 
             // Modified TFIDF.
            double tfIdf;          
            
            String collectionKey = (String) collection_itr.next();
            // Gets all the document numbers in this collection.
            SortedSet allCollectionDocs = mCollT
                    .getDocsInCollection(collectionKey);
            // Gets all the document numbers of this Vertex.
            SortedSet allVertexDocs = mCurrentVertex.getAllVertexDocNrs();
            // Intersection gives the docs of this vertex in this collection.
            Set vertexDocSet = Algorithms.intersect(allCollectionDocs
                    , allVertexDocs);
            int vertexDocs = vertexDocSet.size();
            // The number of documents in this collection.
            int totalDocs = allCollectionDocs.size();
            // Calculating the modified tf and the modified idf. Sums the term 
            // frequencies from all relevant docs of this collection 
            // (see Thorsten Joachims).
            Iterator docNr_itr = vertexDocSet.iterator();
            while(docNr_itr.hasNext())
            {
                Integer docInt = (Integer)docNr_itr.next();
                int thisDoc = docInt;
                // Frequency of this token (or its incoming and outgoing links)
                // in this document.
                int thisTokenFreq;
                // Maximum number of tokens or maximum number of links observed 
                // in this document.
                int maxTokenFreq;
                // Classic token frequency in this document.
                if(classic)
                {
                    thisTokenFreq = mTokenCount.getTokenCount(thisDoc, vertexId);
                    // Normalizer is the maximum token count observed in 
                    // this document.
                    maxTokenFreq = mDocT.getMaxFrequency(thisDoc);
                }
                // Token Link Frequency based on the number of links in this 
                // document (default).
                else
                {
                    thisTokenFreq = getVertexLinks(docInt, vertexId);
                    // Normalizer is the maximum number of links observed in
                    // this document.
                    maxTokenFreq = mDocT.getMaxLinks(thisDoc); 
                }
                // Summed token frequency or token link frequency over all the 
                // docs in this collection.
                meanTokenFreq += (double) thisTokenFreq;
                meanMaxFreq += (double) maxTokenFreq;
            }
            // The mean TF over all the documents in this collection, normalized
            // for document length withthe mean maximum frequency observed. 
            // TF = 0.5 + (0.5 * TF) / maxTF.
            meanMaxFreq = meanMaxFreq / vertexDocs;
            meanTokenFreq = meanTokenFreq / vertexDocs;
            tokenFreq = 0.5 + (0.5 * meanTokenFreq) / meanMaxFreq;
            // Modified TFIDF
            if(tokenFreq > 0.001) tfIdf = tokenFreq 
                    * (StrictMath.log10(totalDocs / vertexDocs));
            else tfIdf = 0;
            // Transfers the resulting metric to the info value table of this 
            // vertex and to the collectionTable. The value is truncated at the
            // 5th decimal place.
            double trunc = StrictMath.ceil(tfIdf * 100000);
            tfIdf = trunc/ 100000;         
            mCurrentVertex.setInfoValue(collectionKey, tfIdf);
            mCollT.addInfoValue(collectionKey, tfIdf);         
            // ...and the same calculation over again for the next collection 
            // of this vertex.
        }
    }
    
    /** getVertexLinks: Returns the total number of links (incoming and outgoing)
     * of a vertex in this document.
     * @param docInt : the document identifier (Integer).
     * @param vertexId : the identifier of a vertex (Integer).
     * @return the total number of links of this vertex (int).
     */
    private int getVertexLinks(Integer docInt, Integer vertexId)
    {
        List tmpList = mLinksT.getVertexLinks(docInt, vertexId);
        return ((Integer) tmpList.get(0)) + ((Integer) tmpList.get(1));
    }
    
    /** getCollectionTable: Getter returns the collectionTable that now has the 
     * updated info weights in this dataGraph for all the collections used in 
     * the scope of this session.
     * @return collectionTable
     */
    public CollectionTable getCollectionTable()
    {
        return mCollT;
    }
    
}
