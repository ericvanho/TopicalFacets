package ericvh.TDT;

import java.io.Serializable;
import java.util.Arrays;
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


/** Class FacetMaker selects informative arcs that are found in different collections.
 * They get a weight that allows their ranking and the relevant document numbers 
 * pointing to the original articles (text documents) where these facets were seen.
 * FacetMaker creates topical facets and not topics. A topical facet is a piece 
 * of semantic information shared by several documents. A complete topic is a 
 * construct made with several topical facets. The same topical facet can be
 * used in more than one topic. Only when the user formulates a query, 
 * a definite topic structure is closed!
 * @author Eric Van Horenbeeck
 * Created on 9 mei 2005, 15:20
 * Revision: July 4, 2006
 */
public class FacetMaker implements Serializable
{
    // CollectionTable with information on the collections in this dataGraph.
    private final CollectionTable mCollTable;
    // Table with information on documents to add the final informative value.
    private final DocTable mDocTable;
    // ArcsTable with the all the arcs.
    private final ArcsTable mArcTable;
    // ArcsTable with the informative arcs.
    private final ArcsTable mInfoArcTable;
    // Map with arcs
    private Map mInfoArcMap;
    // List with unused doc-ids getting a second chance.
    private LinkedList mUnusedDocList;  
    // Set with unused docs from this and previous sessions.
    private SortedSet mNewUnusedDocs;
    private SortedSet mAllUnusedDocs;
    // List with successfully rescanned old documents (id).
    private static LinkedList mRescannedDocList;
    // Map with for every unitKey the set of arcs.
    private LinkedHashMap mArcSetMap;
    // Boolean indicates whether to save the topic files or not.
    private boolean mSaveAll = false;
    // SemanticUnit stores semantic information on documents.
    private SemanticUnit mSemanticUnit;
    // Selects topical facets based on semantic information.
    private SemanticAnalysis mSemanticAnalysis;
    
    /** Constructor
     * @param ia : ArcsTable with info arcs.
     * @param at : ArcsTable instance with all arcs needed to reconstruct the documents.
     * @param dt : DocTable with general information on documents.
     * @param ct : CollectionTable general info on collections.
     * todo : Topical Facets as objects.
     */
    public FacetMaker(ArcsTable ia, ArcsTable at, DocTable dt, CollectionTable ct)
    {
        mInfoArcTable = ia;
        mArcTable = at;
        mDocTable = dt;
        mCollTable = ct;
    }
    
    /** constructFacets: topical facet finding starts by making a map holding 
     * all document-ids shared by the same informative arcs. The arc is key and
     * the doc-id set is its value. Only full arcs are used, arcs with a dummy
     * vertex are removed. The program then goes to the performAnalysis method 
     * that instructs the SemanticAnalysis class to assemble the topical facets.
     */
    public void constructFacets()
    {
        Map tempArcDocMap = new HashMap();
        // List with unused docs to be rescanned and inserted by the ApplicationManager
        mUnusedDocList = ApplicationManager.getUnusedList();       
        // ArcsMap key: docNr, value: an arcsMap with all the arcs. Key of the
        // second arcsMap: the arc key (String); value: arc.
        mInfoArcMap = mInfoArcTable.getFullArcsMap();         
        // Makes a map with an informative arc as key and all its document-ids as value.
        Iterator docs_itr = mInfoArcMap.keySet().iterator();
        while(docs_itr.hasNext())
        {
            Integer docInt = (Integer) docs_itr.next();
            // Iterates over the informative arcs in this document.
            Map arcsMap = (HashMap) mInfoArcMap.get(docInt);
            Iterator arcs_itr = arcsMap.keySet().iterator();
            while (arcs_itr.hasNext())
            {
                String arcKey = arcs_itr.next().toString();
                // Checks if this arc has a dummy vertex member.
                boolean containsDummy =
                    mInfoArcTable.getArcWithKey(docInt, arcKey).containsDummy();
                // Dummy arcs are left out: they diminish the descriptive quality
                // of a topical facet.
                if(!containsDummy)
                {
                    SortedSet tmpArcDocSet = new TreeSet();
                    if(tempArcDocMap.containsKey(arcKey))  tmpArcDocSet = 
                            (TreeSet) tempArcDocMap.get(arcKey);
                    tmpArcDocSet.add(docInt);
                    tempArcDocMap.put(arcKey,tmpArcDocSet);
                }
            }
        }
        performAnalysis(tempArcDocMap);
        saveFilesDialog();
    }
    
    /** performAnalysis: Creates an instance of the SemanticUnit class to hold 
     * three separate maps identified by the same key:
     * - Map 1 in the SemanticUnit class has a set of doc-ids;
     * - Map 2 holds the arc keys;
     * - Map 3 has the info-value of each doc set, based on the info-value of 
     * the arcs in that set.
     * This method is not restricted to collections inside the scope of this
     * session: previously unused documents with dates before the current scope 
     * are rescanned and linked to a facet if they are semantically relevant.
     * @param arcMap : HashMap with arc key (String) as key; value: SortedSet 
     * with doc-ids (Integer).
     */
    private void performAnalysis(Map arcMap)
    {
        // SemanticUnit instance to hold the doc-ids, arcs and their info-value.
        mSemanticUnit = new SemanticUnit();
        // Computes the compounded info-value of each arc from high to low
        // over all documents used in this session.
        LinkedHashMap weightMap = infoWeight(arcMap);
        // A sequential number to synchronize the maps. It starts the counting
        // where the previous session stopped.
        int unitKeyCount = 0;
        // Iterates over the weightMap to get the arcs with the highest 
        // info-value first.
        Iterator arc_itr = weightMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            String arcKey = arc_itr.next().toString();
            // The docSet is retrieved from the arcMap.
            SortedSet docSet = (TreeSet) arcMap.get(arcKey);
            double arcValue;
            // Collects arc keys, doc-ids and info-values in maps with the 
            // same sequential key. The arcs are put in a LinkedHashSet to 
            // preserve the ordering from high to low info-value.
            LinkedHashSet arcSet = new LinkedHashSet();
            arcSet.add(arcKey);
            // Gets the info-value for this arc.
            arcValue = ((Double) weightMap.get(arcKey));
            // Makes no new entries if this docSet is already registered. 
            // Only the new arc key is added and the info-value of the set is
            // adjusted with the new arc value.
            if(mSemanticUnit.containsSet(docSet))
            {
                // Finds the index of this set.
                int unitKey = mSemanticUnit.getKey(docSet);
                // Adds the current arc to the set of arcs collected so far.
                mSemanticUnit.addArcs(unitKey, arcSet);
                // Adds the info-value of the current arc.
                mSemanticUnit.addInfoValue(unitKey, arcValue);
                // Updates the frequency count of the documents in this set.
                mSemanticUnit.frequencyUpdate(docSet, 0);
            }
            // Makes a new entry in the SemanticUnit for each of the 
            // three synchronized maps.
            else
            {
                // Transfers the new arc key, doc id-sets and info value to
                // the SemanticUnit.
                mSemanticUnit.addUnit(unitKeyCount, docSet, arcSet, arcValue);
                // Next unit key for an entry in the semantic map.
                unitKeyCount++;
            }
        }
        // Creates an instance of the SemanticAnalysis class with the information
        // gathered here.
        mSemanticAnalysis = new SemanticAnalysis(mSemanticUnit);
        // Searches for topical facets based on this semantic information. 
        // FacetViewer will ask SemanticAnalysis to show the results on screen
        // when the processing is done.
        mSemanticAnalysis.findAllFacets();
        // Collects the unused documents from this session.
        setUnusedDocSet();
    }
    
    /** infoWeight: Map of arcs ranked according to their info-value from high 
     * to low. This is the mean of all arc info-values in the documents where 
     * the arc was involved in.
     * @param arcMap : HashMap with arc key (String) as key; value: SortedSet 
     * of doc-ids (Integer).
     * @return a LinkedHashMap with the arc key (String) as key and the info 
     * value (Double) of that arc as value.
     */
    private LinkedHashMap infoWeight(Map arcMap)
    {
        Map weightMap = new HashMap();
        Iterator arc_itr = arcMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            double arcWeight = 0;
            String arcKey = arc_itr.next().toString();
            SortedSet tmpDocSet = (TreeSet) arcMap.get(arcKey);
            int count = 0;
            // Iterates over the whole docMap.
            Iterator doc_itr = tmpDocSet.iterator();
            while(doc_itr.hasNext())
            {
                Integer docInt = (Integer) doc_itr.next();
                // Gets the info value of this arc in this document.
                arcWeight += mInfoArcTable.getArcValue(arcKey, docInt);
                count++;
            }
            // Computes the mean info-value for this arc over all the documents
            // it is involved in.
            arcWeight = arcWeight / count;
            if(arcWeight > 0)
            {
                // Puts the info-value and the arc key in a map. Changes equal 
                // keys slightly with a small random number to avoid data loss.
                if(weightMap.containsKey(arcWeight))
                {
                    double n = Math.random() / 100000;
                    arcWeight += n;
                }
                weightMap.put(arcWeight, arcKey);
            }
        }
        // Sorts the weightMap from high to low by the 'infoValue' comparator.
        TreeMap tm = new TreeMap(infoValue);
        tm.putAll(weightMap);
        // Reversing the map while observing the ordering to get the arc key 
        // in front as key.
        LinkedHashMap newMap = new LinkedHashMap();
        Iterator tree_itr = tm.keySet().iterator();
        while(tree_itr.hasNext())
        {
            Double arcValue = (Double) tree_itr.next();
            String arcKey = tm.get(arcValue).toString();
            newMap.put(arcKey, arcValue);
        }
        return newMap;
    }
    
    /** Comparator sorts the weight map on the info-value from high to low.
     */
    private final Comparator infoValue = (Comparator) (Object o1, Object o2) ->
    {
        Double w1 = (Double) o1;
        Double w2 = (Double) o2;
        return w2.compareTo(w1);
    };
    
    /** showTopics: Calls the FacetViewer to prepare the results of the topic
     * selection for presentation in the GUI.
     * @param noLabels : if boolean 'true' no labels will be retrieved.
     */
    public void showTopics(boolean noLabels)
    {
        FacetViewer view = new FacetViewer(mInfoArcTable, mArcTable, mDocTable,
                mCollTable);
        view.topicView(mSemanticUnit, mSemanticAnalysis, noLabels, false);
    }
    
    /** showSummary: When no FacetViewer is requested, the summary is displayed 
     * on the 'General Info'-tab.
     * @param noLabels : if boolean 'false' a list with topical facets will be 
     * displayed.
     */
    public void showSummary(boolean noLabels)
    {
        FacetViewer view = new FacetViewer(mInfoArcTable, mArcTable, mDocTable,
                mCollTable);
        view.viewSummary(mSemanticUnit, mSemanticAnalysis, noLabels, true);
    }
    
    /** setUnusedDocSet: Collects new unused documents from this session in a 
     * SortedSet and transmits them to the FacetViewer where they will be 
     * displayed at the bottom of the Topic panel. Prepares a list with unused 
     * docs from this and previous sessions.
     */
    private void setUnusedDocSet()
    {
        Set allDocSet = new HashSet(Arrays.asList(mDocTable.getAllDocNrs()));
        Set topicDocSet = mSemanticAnalysis.getActiveDocs();
        mNewUnusedDocs = new TreeSet(Algorithms.difference(allDocSet, topicDocSet));
        setAllUnusedDocList();
        FacetViewer.setUnusedDocs(mNewUnusedDocs);
    }
    
    /** setAllUnusedDocList: prepares a list with unused documents. Adds newly
     * unused doc-ids to the existing ones, removes from the list previously 
     * unused documents that are now participating in the topical facet formation
     * of this session.
     */
    private void setAllUnusedDocList()
    {
        // Removes from the old unusedList all documents that participated 
        // in this session.
        SortedSet tmpSet = new TreeSet();
        if (mUnusedDocList != null)
        {
            Set rescannedDocSet = mSemanticAnalysis.rescannedDocs(mUnusedDocList);
            mRescannedDocList = new LinkedList(rescannedDocSet);
            mUnusedDocList.removeAll(rescannedDocSet);
            tmpSet.addAll(mUnusedDocList);
        }
        // Merging the modified old list with the new one.
        if (mNewUnusedDocs != null)
        {
            tmpSet.addAll(mNewUnusedDocs);
            mUnusedDocList = new LinkedList(Arrays.asList(tmpSet.toArray()));
        }
    }
    
    /** getFullUnusedDocList: Returns a list with all unused documents saved 
     * so far. The updated list is saved for reuse in the next session.
     * @return LinkedList with doc-ids (int) not used in this and previous 
     * sessions.
     */
    public LinkedList getFullUnusedDocList()
    {
        return mUnusedDocList;
    }
    
    /** getRescannedDocs: Returns a list with the doc-ids from previous sessions
     * that were successfully reused.
     * @return LinkedList with doc-ids (int) reused in this session.
     */
    public static LinkedList getRescannedDocs()
    {
        return mRescannedDocList;
    }
    
    /** getTopicalFacetMap: Returns a copy of the topicalFacetMap. This map is
     * sorted on the info value of the doc-ids. Collects a map with all sets of 
     * topical facets defining arcs for the ApplicationManager to retrieve and
     * save.
     * @return LinkedHashMap with a topical facet key (Integer) and a SortedSet 
     * of doc-ids (Integer) as value.
     */
    public LinkedHashMap getTopicMap()
    {
        LinkedHashMap tmpMap = mSemanticAnalysis.getTopicalFacetMap();
        LinkedHashMap newMap = new LinkedHashMap();
        // Collects the arcs in a map.
        mArcSetMap = new LinkedHashMap();
        Iterator map_itr = tmpMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            Integer unitKey = (Integer) map_itr.next();
            Set docSet = (TreeSet) tmpMap.get(unitKey);
            newMap.put(unitKey, docSet);
            mArcSetMap.put(unitKey, mSemanticUnit.getArcSet(unitKey));
        }
        return newMap;
    }
    
    /** getArcSets: Returns a map with for every unitKey a set of arcs that 
     * defined a topical facet.
     * @return LinkedHashMap with a topical facet key (Integer) and a 
     * LinkedHashSet of arc keys (String) as value.
     */
    public LinkedHashMap getArcSets()
    {
        return mArcSetMap;
    }
    
    /** saveFilesDialog: The user can decide not to save the data that make up 
     * this topical facet construction. The 'Consolidate' procedure makes 
     * pointers to all the topical facets. When some of the facets were changed
     * due to a rerun of a local topic exercise, then the consolidated picture 
     * needs an update. That may or may not be desirable.
     */
    private void saveFilesDialog()
    {
        int answer = JOptionPane.showConfirmDialog(ApplicationManager
                .getTabbedPane(),
                "Saving these files may change the overall picture.\nRunning "
                        + "the 'Consolidate' module is then necessary.\nThe "
                        + "results can always be viewed with the\n'Show Topical"
                        + " Facets' checkbox selected\nin the Parameter "
                        + "settings.\n\nPress 'Yes' to save or 'No' to discard "
                        + "the data", "Topic Maker",
                JOptionPane.YES_NO_OPTION);
        if(answer == JOptionPane.YES_OPTION) mSaveAll = true;
    }
    
    /** saveFiles: Returns the boolean 'true' if the files with topic data are 
     * to be saved on disk.
     * @return boolean 'true' to save topic files by the ApplicationManager, 
     * 'false' otherwise.
     */
    public boolean saveFiles()
    {
        return mSaveAll;
    }
    
}
