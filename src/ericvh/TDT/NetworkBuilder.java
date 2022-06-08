package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/** Class NetworkBuilder builds a network representation containing all the 
 * necessary information from the files presented to it. The main elements of
 * this graph are: the vertex and the arc (an edge with direction and weight). 
 * The labels of vertices and arcs are stored in a labelTable.
 * Two additional containers are initialized: one to hold information on the 
 * individual documents (docTable) and one based on a common source and date
 * that define together a collection (collectionTable) and that holds 
 * information on collection level. DataGraph, docTable and collectionTable 
 * are saved to disk.
 * @author Eric Van Horenbeeck
 */
public class NetworkBuilder implements Serializable
{
    // DocNumber is the unique document identifier attributed by the ApplicationManager.
    private int mDocNr = 0;
    // Current identification number for a vertex label (token-type).
    private int mMainVertexIndex = 0;
    // Last identification number attributed to a token-type in a previous session.
    private Integer mAttributedId = null;
    // Position index of an arc in a document.
    private int mArcPosition;
    // The last vertex processed so far.
    private Vertex mLastVertex;
    // The last index (label identification) processed so far.
    private int mLastIndex;
    // Map with token-type as key and an integer index as value. 
    // Goes into the LabelTable.
    private SortedMap mLabelMap;
    // Map to hold the vertices created in this method. Goes to the VertexTable 
    // when ready.
    private final Map mVerticesMap;
    // Instance of the Textgraph class, a representation of a textfile as a graph.
    private TextGraph mNewGraph;
    // Instance of the LabelTable class with all labels (token-types) in use in
    // this community.
    private LabelTable mLabelTable;
    // DocTable holds the unique document number as key and as value a set of
    // file statistics and
    // the full path name.
    private final DocTable mDocTable;
    // CollTable has a source-date key defining a 'collection' and two containers
    // to hold information on collection level.
    private final CollectionTable mCollTable;
    
    /** Constructor
     * The general labelTable is retrieved and new token-types will be added. 
     * The old table will be replaced with the augmented one and returned to 
     * the ApplicationManager.
     */
    public NetworkBuilder()
    {
        mLabelMap = new TreeMap();
        mVerticesMap = new HashMap();
        mCollTable = ApplicationManager.getCollTable();
        mDocTable = ApplicationManager.getDocTable();
        mLabelTable = ApplicationManager.getLabels();
        mAttributedId = mLabelTable.getLastId();
        if(mAttributedId != null)
        {
            mMainVertexIndex = mAttributedId;
            mLabelMap = mLabelTable.getLabelMap();
        }
    }
    
    /** textToGraph: Constructing this dataGraph starts here. 
     * The method is invoked by the ApplicationManager. CurrentTextToken is an 
     * instance of the TextToken class. It has the label of  the token, 
     * its position in the text and the key to the collection (source and 
     * date String) it  belongs to.
     * @param dataPackage : array with text_tokens (List), docData (DocStats) 
     * and a collectionKey (String).
     * @param docNumber : the unique serial number of a document (int).
     * @return a new textGraph from this textfile. TextGraph has the arcs and 
     * the token count. It is saved on disk for further processing.
     */
    public TextGraph textToGraph(List dataPackage, int docNumber)
    {
        // Array to hold all textTokens of this document.
        List text_tokens;
        // Statistical info about this document.
        DocStats docData;        
        // Key to the collection this document belongs to.
        String collKey;
        // TextToken now being processed.
        TextToken currentTextToken;
        // Last node of the graph processed so far. 
        Vertex mThisLastVertex = null;  
        // The unique document number.
        mDocNr = docNumber;      
        // A TextGraph instance.
        mNewGraph = new TextGraph(mDocNr); 
        // Positions an arc in the text.
        mArcPosition = 0;    
        // Counter.
        int i = 0;                           
        
        text_tokens = (ArrayList) dataPackage.get(0);
        docData = (DocStats) dataPackage.get(1);
        collKey = (String) dataPackage.get(2);
        mDocTable.addDocInfo(mDocNr, docData);
        mCollTable.addDocNrToCollection(collKey, mDocNr);
        // Transforming the array with the textTokens into a graph.
        while (i < text_tokens.size())
        {
            currentTextToken = (TextToken) text_tokens.get(i);
            addTextToken(currentTextToken);
            i++;
        }
        return mNewGraph;
    }
    
    /** addTextToken: Processes one textToken at a time, creating a vertex and 
     * an arc if appropriate. TextToken has a label (the 'word'), a counter on 
     * this token and its key to a collection. The 'word' in the text becomes 
     * the vertex (or node) of a text graph. If this label already exists in this 
     * graph or if it is the first point, no arc is created. The last word 
     * (lastVertex) is then the one but last vertex. The neighbors left and 
     * right to the vertex are added. A token gets a negative initial weight, 
     * this will be adjusted when all files have been processed. Arcs (directed
     * links between vertices) get an initial weight of 0.
     * @ToDo: add end-of-line (EOF) token.
     * @param currentTextToken : the textToken now being handled
     */
    private void addTextToken(TextToken currentTextToken)
    {
        int currentIndex;
        Vertex currentVertex;
        String currentToken = currentTextToken.getToken();
        String collectionKey = currentTextToken.getCollectionKey();
        
        // Checks if this token is already known. Retrieves the corresponding 
        // vertex if one exists with the integer that identifies its label.
        if(mLabelMap.containsKey(currentToken))
            currentIndex = ((Integer) mLabelMap.get(currentToken));
        // Adds a new entry to the labelmap if this label is new.
        else
        {
            mMainVertexIndex++;
            currentIndex = mMainVertexIndex;
            mLabelMap.put(currentToken, currentIndex);
        }
        if(mVerticesMap.containsKey(currentIndex))
            currentVertex = (Vertex) mVerticesMap.get(currentIndex);
        // Creates a new vertex and puts it in the verticesMap.
        else
        {
            currentVertex = new Vertex();
            mVerticesMap.put(currentIndex, currentVertex);
        }
        // Checks if an arc exists with this vertex otherwise creates a new one.
        // Adds neighbors to this vertex starting with the second token of the
        // document.
        if (mArcPosition > 0)
        {
            // Registers the previous token as neighbor to the current token
            // and registers the current token as neighbor to the previous token.
            currentVertex.addAdjVertex(mLastIndex, mDocNr);
            mLastVertex.addAdjVertex(currentIndex, mDocNr);
            // Creates arc index and adds it to the textGraph.
            String arcIndex = "" + mLastIndex + "*" + currentIndex;
            // Forms an arc between the current and last vertex.
            if(!mNewGraph.containsArcIndex(arcIndex)) mNewGraph.addArc(arcIndex,
                    new Arc(mArcPosition, mLastIndex, currentIndex, mDocNr, 0));
        }
        
        // The temporary placeholder value will be recalculated after processing
        // the dataGraph.
        currentVertex.setInfoValue(collectionKey, -1);
        // The textGraph counts each of its tokens.
        mNewGraph.addTokenCount(currentIndex);
        // The arc position allows sorting the arcs and to reconstruct the
        // original text.
        mArcPosition++;
        // Currentvertex is now the last new vertex added to the graph so far.
        mLastVertex = currentVertex;
        mLastIndex = currentIndex;
    }
    
    /** getLabelTable: Returns the labelTable, an instance of the LabelTable 
     * class, holding a map with labels (token-types) of this dataGraph and
     * others and methods to access them.
     * @return labelTable to the ApplicationManager.
     */
    public LabelTable getLabelTable()
    {
        mLabelTable = new LabelTable();
        mLabelTable.addLabelMap(mLabelMap);
        return mLabelTable;
    }
    
    /** getFullVerticesTable: Getter returns the verticesTable, an instance 
     * of the VerticesTable class holding the vertex map and methods to 
     * access the data in it.
     * @return verticesTable to the ApplicationManager
     */
    public VerticesTable getFullVerticesTable()
    {
        VerticesTable vt = new VerticesTable();
        vt.addAllVertices(mVerticesMap);
        return vt;
    }
    
    /** getDocTable: Getter returns instance of the DocTable class associated 
     * with these files with in it for every unique doc number some file 
     * statistics (maximum frequency, total tokens, total types,full file path).
     * @return the info table on the document (DocTable)
     */
    public DocTable getDocTable()
    {
        return mDocTable;
    }
    
    /** getCollectionTable: Getter returns instance of the CollectionTable class
     * associated with this files holding for every collection the document
     * numbers that are part of it, the cutOff value (metric to divide 
     * informative tokens form common ones) and a map of informative tokens and 
     * their info value.
     * @return collections and their document numbers
     */
    public CollectionTable getCollectionTable()
    {
        return mCollTable;
    }
    
    /** release: Eliminates an object in this class that is no longer needed.
     * @param o : the object to eliminate
     */
    public void release(Object o)
    {
        o = null;
    }
    
}
