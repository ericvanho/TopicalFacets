package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;


/** Class GraphAnalyzer constructs a smaller dataGraph with only the important 
 * tokens from the original dataGraph. After selecting informative vertices,
 * new arcs are made to connect them. GraphAnalyzer also collects Associations:
 * tokens that form a strong link between each other.
 * @author  Eric Van Horenbeeck
 * Created on 3 augustus 2004, 21:33
 */
public class GraphAnalyzer implements Serializable
{
    // The table with the arcs of this dataGraph.
    private static ArcsTable mArcTable;
    // Class with informative tokens.
    private final InformativeTokens mInfoTok;
    // Associations class constructs 'named entities' or collocations.
    private final Associations mAssociations;
    // Table with the original vertices from the dataGraph.
    private static VerticesTable mVrtxTable;
    // Table with informative vertices in this reduced dataGraph.
    private static Map mInfoVertices;
    // Map from the informative vertices table
    private static Map mInfoVertexMap;
    // HashMap with informative arcs in this reduced dataGraph.
    private static Map mInfoArcs;
    // Instance of the ArcsTable, holding a map with the new arcs and methods to 
    // access them.
    private static ArcsTable newArcsTable;
    // Instance of the VerticesTable to hold the new informative vertices.
    private static VerticesTable newVrtxTable;
    // CollectionTable with information on the collections in this dataGraph.
    private CollectionTable ct;
    // Table with information on documents to add the final informative value.
    private final DocTable mDocTable;
    // Table with all labels.
    private LabelTable mAllLabels;
    // The arcPassed method checks the current arc against the previous one.
    private Arc previousArc = new Arc();
    private final double previousVValue = 0;
    private final Integer previousVKey = 0;
    // Switch set by the user. If 'true' associations are showed on a tabbedPane.
    private static boolean mShowAssoc = false;
    
    /** Constructor called by the ApplicationManager.
     * @param at : ArcsTable instance prepared by the ApplicationManager.
     * @param vt : VerticesTable instance retrieved by the ApplicationManager.
     * @param it : InformativeTokens instance retrieved by the ApplicationManager.
     * @param dt : DocTable instance retrieved by the ApplicationManager.
     */
    public GraphAnalyzer(ArcsTable at, VerticesTable vt, InformativeTokens it, 
            DocTable dt)
    {
        mArcTable = at;
        mVrtxTable = vt;
        mInfoTok = it;
        mDocTable = dt;
        // Constructs 'named entities' and informative phrases.
        mAssociations = new Associations();
        // CheckBox switch to show the associations to screen.
        mShowAssoc = ApplicationManager.getAssociationStatus();    
    }
    
    /** extractArcsAndVertices: Takes the original data tables and extracts 
     * informative vertices and arcs to construct a new reduced graph representation
     * of the information in these texts.
     * @param countOnly : boolean 'true' gives the number of associations per 
     * document. If 'false' all the associations are retrieved.
     */
    public void extractArcsAndVertices(boolean countOnly)
    {
        // CollectionTable information on collections of documents.
        ct = ApplicationManager.getCollTable();
        // Table with all labels.
        mAllLabels = ApplicationManager.getLabels();
        // Adds important vertices to the new graph based on informative vertices.
        mInfoVertices = collectAllImportantVertices();
        // Creates new ArcsTable and new VerticesTable.
        newVrtxTable = new VerticesTable();
        newVrtxTable.addVerticesMap(mInfoVertices);
        newArcsTable = new ArcsTable(newVrtxTable);
        // Iterates over all relevant documents, constructing and adding arcs.
        mInfoArcs = new HashMap();
        Set docNrSet = new TreeSet(Arrays.asList(mDocTable.getAllDocNrs()));
        Iterator docNrs_itr = docNrSet.iterator();
        while(docNrs_itr.hasNext())
        {
            int docNr = ((Integer)docNrs_itr.next());
            // Sorts the info arcsMap on the final value of the document.
            Map newArcMap = constructImportantArcs(docNr);
            if(!newArcMap.isEmpty()) mInfoArcs.put(docNr, newArcMap);
            // Updates progressbar on the GUI.
            ApplicationManager.setFileCount(1);
        }
        // Updates progressbar on the GUI.
        ApplicationManager.setFileCount(ApplicationManager.getDocTable()
                .getDocCount() - docNrSet.size());
        // The infoArcMap is sorted on the document-ids and added to the new arcsTable.
        sortInfoArcs();
        newArcsTable.addFullArcsMap(mInfoArcs);
        // Searches for combined associations.
        mAssociations.combineLinks();
        // If the 'Show Associations' CheckBox is 'true' (default) associations are
        // displayed.
        if(mShowAssoc)
        {
            ApplicationManager.updateStatusBar(5, "");
            ApplicationManager.clearText();
            ApplicationManager.showText(mAssociations
                    .associationsToString(countOnly), 2);
        }
        else ApplicationManager.showText("* Associations calculated, but not "
                + "shown here. *\n* To view the list tick the 'Show Associations'"
                + " checkbox in the Parameters menu. *", 2);
    }
    
    /** sortInfoArcs: The keyset of infoArcs is first sorted on the document-id. 
     * The infoMarcMap is then reordered in ascending order of the docNrs.
     */
    private void sortInfoArcs()
    {
        LinkedHashMap tmpMap = new LinkedHashMap();
        Object[] docArray;
        docArray = (Object[])mInfoArcs.keySet().toArray(new Object[mInfoArcs.
                keySet().size()]);
        Arrays.sort(docArray);
        // Reorders the infoArcMap with the sorted docArray on docNr.
        for (Object docArray1 : docArray) {
            Integer docKey = (Integer) docArray1;
            tmpMap.put(docKey, mInfoArcs.get(docKey));
        }
        mInfoArcs = tmpMap;
    }
    
    /** collectAllImportantVertices: Puts the informative vertices from all the 
     * collections in one map 
     * (HashMap). Informative tokens have values < mean. See link-table and 
     * Small World texts.
     * @return map with the vertex-id (Integer) as key and the vertex itself 
     * as value
     */
    private Map collectAllImportantVertices()
    {
        Vertex currentVertex;
        Map allInfoVertices = new HashMap();
        mInfoVertexMap = mInfoTok.getFullInfoTokenMap();
        // Iterates over all the collections in the original graph inside 
        // the scope of this session.
        Set collectionKeys = ct.getScopeCollectionKeys();
        Iterator keys_itr = collectionKeys.iterator();
        while(keys_itr.hasNext())
        {
            String collKey = keys_itr.next().toString();
            Map collectionMap = (HashMap) mInfoVertexMap.get(collKey);
            Iterator coll_itr = collectionMap.keySet().iterator();
            while(coll_itr.hasNext())
            {
                Integer vKey = (Integer) coll_itr.next();
                currentVertex = (Vertex) mVrtxTable.getThisVertex(vKey);
                // Adds this informative vertex to the map.
                allInfoVertices.put(vKey, currentVertex);
            }
        }
        return allInfoVertices;
    }
    
    /** constructImportantArcs: Connects the informative vertices of a document 
     * with arcs. There are four possible outcomes for every 
     * Collection(C) - Document(D) combination: C+ D+, C- D+, C- D-, C+ D-.
     * This because vertices can have an informative value in one collection 
     * but not in another; vertices can also be present in one document but 
     * not in another of the same collection.
     * @param docNr : the unique document number (int)
     * @return HashMap with informative arcs in this document
     */
    private Map constructImportantArcs(int docNr)
    {
        // The collection key for this document.
        String collKey = ct.getDocsCollectionKey(docNr);
        // Map with all the informative vertices in this collection.
        Map vertexMap = (HashMap) mInfoVertexMap.get(collKey);
        // Map to collect the newly formed informative arcs.
        Map allInfoArcs = new HashMap();
        // Temporary list to collect all full arcs (having a real vertex on 
        // both sides).
        List tmpFullArcList = new ArrayList();
        // Iterates over the sortedMap with arc postions in this document 
        // as key and the arc as value.
        SortedMap positionMap = mArcTable.getArcPositions(docNr);
        Iterator pos_itr = positionMap.keySet().iterator();
        while(pos_itr.hasNext())
        {
            boolean isInfoArc = true;
            // The position of this arc in the text.
            Integer position = (Integer) pos_itr.next();
            // Retrieves the existing arc at this position in this doc.
            Arc currentArc = (Arc) positionMap.get(position);
            // Checks if one or both of the two vertices of this arc is an 
            // infoVertex.
            int firstIdx = currentArc.getVertex1Index();
            int secondIdx = currentArc.getVertex2Index();
            boolean first = vertexMap.containsKey(firstIdx);
            boolean second = vertexMap.containsKey(secondIdx);
            // Both elements of the arc are infoVertices.
            if(first && second)
            {
                // These vertices are collected in a list to allow pruning loose 
                // ends afterwards.
                tmpFullArcList.add(firstIdx);
                tmpFullArcList.add(secondIdx);
            }
            // If only the first vertex is informative the second vertex is 
            // replaced by 'zero' as a placeholder.
            else if(first) secondIdx = 0;
            // Only the second vertex is informative. Replaces the first vertex
            // with a placeholder.
            else if(second) firstIdx = 0;
            // None of the points are infoVertices: new arc creation is skipped.
            else isInfoArc = false;
            // Also eliminates arcs having twice the same token.
            if(firstIdx == secondIdx) isInfoArc = false;
            // A new info arc is constructed when at least one of its two 
            // vertices is informative.
            if(isInfoArc)
            {
                Arc newArc = new Arc(position, firstIdx, secondIdx, docNr, 0);
                String arcKey = firstIdx + "*" + secondIdx;
                // Adjust the informative arc value with the info value of 
                // its vertices.
                newArcsTable.adjustThisArcWeight(newArc);
                // An association candidate (named entities, collocations, etc.) 
                // is an arc with two informative vertices at one point distance
                // and having the same info value.
                if(arcPassed(docNr, newArc, firstIdx, secondIdx, collKey)) 
                    mAssociations.addAssociations(docNr, arcKey);
                // Puts the new arcs in a map linked to this document.
                allInfoArcs.put(arcKey, newArc);
            }
        }
        // New arc construction is finished for this document.
        return allInfoArcs;
    }
    
    /** arcPassed: For an arc to be considered an association it has its two vertices
     * at one point distance in the text. Both vertices need to have approx. the same 
     * value and the second token vertex should not be a digit.
     * The special case of two dummy arcs with the '0' facing each other at one
     * point distance and having the same infoValue is also considered an 
     * association candidate.
     * @param docNr : the unique document identification (int)
     * @param thisArc : the arc to evaluate.
     * @param firstIdx : index to the first vertex of this arc (int).
     * @param secondIdx : index to the second vertex of this arc (int).
     * @param collKey : The collection key for this document (String).
     * @return boolean 'true' if both vertices have the same informative value
     */
    private boolean arcPassed(int docNr, Arc thisArc, int firstIdx, int secondIdx,
            String collKey)
    {
        double vValue1 = 0;
        double vValue2 = 0;
        boolean sameValue = true;
        Integer key1 = firstIdx;
        Integer key2 = secondIdx;
        Vertex v1 = (Vertex) mVrtxTable.getThisVertex(key1);
        Vertex v2 = (Vertex) mVrtxTable.getThisVertex(key2);
        if(!thisArc.containsIndexFirst(0)) 
           vValue1 = Math.round(v1.getInfoValue(collKey) * 100);
        if(!thisArc.containsIndexSecond(0)) 
            vValue2 = Math.round(v2.getInfoValue(collKey) * 100);
        // Equality rounded to two decimal places: more associations.
        if((vValue1/100) != (vValue2/100)) sameValue = false;  

        // if(mAllLabels.isDigit(key2) || 
        
        // Does not work at this moment.
        /*The special case goes directly to the Associations, if 'true'.
        try
        {
            if(thisArc.containsIndexFirst(0) &&
                    (thisArc.getArcPosition() - previousArc.getArcPosition()) == 1 &&
                    vValue2 == previousVValue)
            {
                mAssociations.addThreeArc(new Integer(docNr), previousVKey, key2);
                sameValue = false;
                previousArc = null;
                previousVKey = null;
                previousVValue = 0;
            }
            else if(thisArc.containsIndexSecond(0))
            {
                previousArc = thisArc;
                previousVValue = vValue1;
                previousVKey = key1;
                sameValue = false;
            }
        }
        catch(NullPointerException ne)
        {
             If there is not yet a previousArc, the NullPointerException is raised.
        }
        */
        
        return sameValue;
    }
    
    /** getNewArcsTable: Getter returns infoArcs, a table with informative arcs,
     * collected here by the GraphAnalyzer. Called by Associations.
     * @return ArcsTable with arcs connecting informative tokens
     */
    public static ArcsTable getNewArcsTable()
    {
        return newArcsTable;
    }
    
    /** getNewVerticesTable: Returns the new VerticesTable.
     * @return VerticesTable with the informative vertices only.
     */
    public static VerticesTable getNewVerticesTable()
    {
        return newVrtxTable;
    }
    
    /** getFinalArcsMap: Getter returns infoArcMap, a map with the final 
     * informative arcs.
     * @return HashMap with for every docNr (Integer) a HashMap as value with 
     * an arc key (String) and the arcs (Arc) with an informative content.
     */
    public Map getFinalArcsMap()
    {
        return newArcsTable.getFullArcsMap();
    }
    
    /** getAssociationMap: Getter returns associationsMap, a map with links 
     * that are collocations, named entities or other similar structures. 
     * Only the 15 most informative associations are returned.
     * @return HashMap with associations. Key: docInt (Integer); value: SortedMap
     * with the info value as key and a LinkedList with arc keys (String) as value.
     */
    public Map getAssociationMap()
    {
        if(!mAssociations.isEmpty()) return mAssociations.getAssociationsMap();
        else return null;
    }
    
}
