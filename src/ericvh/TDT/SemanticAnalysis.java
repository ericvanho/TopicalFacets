package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/** Class SemanticAnalysis searches for topical facets. Based on the selection 
 * of one specific document ('Topics' tabbed pane in the GUI) the user can bring
 * together all topical facet sets having that document as a member to inspect 
 * the relevance of this assembly.
 * @author Eric Van Horenbeeck
 * Created on 17 mei 2005, 22:02
 * Revision: Oct. 13, 2006
 * todo: Make for every Topical Facet a tree with the root-node (initial arc) on top 
 */
public class SemanticAnalysis implements Serializable
{
    // Map with a topical facet key and a set of semantically related doc-ids as value.
    private final LinkedHashMap mTopicalFacetMap;
    // Instance of the SemanticUnit class linked to this SemanticAnalysis.
    private final SemanticUnit mSemUnit;
    // Set with all registered doc-ids.
    private final SortedSet mDocIds;
    // Set with all doc-ids participating in one or more topic facets.
    private SortedSet mActiveDocs;
    // Instance of the Algorithms class with frequently used general methods.
    private Algorithms alg = new Algorithms();
    
    /** Constructor
     * @param semUnit : the SemanticUnit instance to be linked to this SemanticAnalysis.
     */
    public SemanticAnalysis(SemanticUnit semUnit)
    {
        mTopicalFacetMap = new LinkedHashMap();
        mSemUnit = semUnit;
        mDocIds = mSemUnit.getAllDocIds();
    }
    
    /** findAllFacets: Runs over all the doc-ids and searches for topical facet clues.
     */
    public void findAllFacets()
    {
        // Tells the progress bar on the GUI how many elements to expect.
        ApplicationManager.setFilesToProcess(mDocIds.size());
        Iterator doc_itr = mDocIds.iterator();
        while(doc_itr.hasNext())
        {
            Integer docId = (Integer) doc_itr.next();
            mTopicalFacetMap.putAll(getTopicalFacet(docId));
            // Updates the progressbar.
            ApplicationManager.setFileCount(1);
        }
        // Reduces the map dimension by absorbing small sets into larger ones.
        absorbSmallSets();
        // Finally collects all doc-ids that are active in one or more topical facets.
        collectActiveDocs();
    }
    
    /** getTopicalFacet: Collects the documents that use the same arcs as the 
     * startFile together with the unit keys of the doc-sets of these arcs. 
     * The onset for computing a topical facet can beany file that is a member 
     * of this dataGraph.
     * @param startFile : the document-id (Integer) of the file that triggers 
     * the search for a topical facet.
     * @return SortedMap with topical facet information.
     */
    private SortedMap getTopicalFacet(Integer startFile)
    {
        // Gets all the doc-ids related to this starting file.
        SortedSet relatedDocs = mSemUnit.getRelatedDocs(startFile);
        List setKeyList = new ArrayList();
        // Iterates over the related doc-set and collects all the associated unit keys.
        Iterator related_itr = relatedDocs.iterator();
        while(related_itr.hasNext())
        {
            Integer docId = (Integer) related_itr.next();
            // Adds a set with the unit keys from this document to a list.
            setKeyList.add(mSemUnit.getDocKeySets(docId));
        }
        return searchFacets(setKeyList);
    }
    
    /** searchFacets: Arcs carry two semantic elements (vertex). The more several
     * arcs appear together in two documents, the higher the probability that 
     * these documents are about the same subject. Topical facets are detected
     * by compairing documents that have the same arc-sets. Being seen in at
     * least 2 key-lists is a minimal condition.
     * @param unitKeyList : an ArrayList with a range of unit key-lists (LinkedList
     * with Integer).
     * @return a SortedMap with unit keys (Integer) as map key and the set of 
     * doc-ids (Integer) related to the unit key as map value.
     */
    private SortedMap searchFacets(List unitKeyList)
    {
        // Counts how often a given unit key was found in the key lists.
        int keyPassed = 0;
        // Map with the set keys and their count score.
        SortedMap facetMap = new TreeMap();
        // Iterates over the lists with unit keys in a double loop.
        Iterator list_itr1 = unitKeyList.iterator();
        while(list_itr1.hasNext())
        {
            LinkedList setKeyList = (LinkedList) list_itr1.next();
            // Gets a unit key from a list with keys. This key is not a doc-id 
            // but identifies a set of doc-ids. The same unit key is used to
            // synchronize the different data maps in the SemanticUnit class.
            Iterator unitKey_itr = setKeyList.iterator();
            while(unitKey_itr.hasNext())
            {
                Integer unitKey = (Integer) unitKey_itr.next();
                // Iterates again over the list to see if this unit key is 
                // found in other lists.
                Iterator list_itr2 = unitKeyList.iterator();
                while(list_itr2.hasNext())
                {
                    LinkedList nextSetKeyList = (LinkedList) list_itr2.next();
                    // Counts how many times a unit key is found in this list.
                    if(nextSetKeyList.contains(unitKey)) keyPassed++;
                }
                SortedSet docSet = mSemUnit.getDocSet(unitKey);
                // Puts a unit key seen in at least 3 doc-lists in a map with 
                // its set of corresponding doc-ids.
                if(keyPassed > 2) facetMap.put(unitKey, docSet);  
                // Resets the counter for the next unit key.
                keyPassed = 0;
            }
        }
        return facetMap;
    }
    
    /** absorbSmallSets: If a set of doc-ids in the topicalFacetMap is a subset 
     * of a larger set, the smaller set is absorbed by the larger one. This is 
     * done until the list is left with sets that are either fully or partially 
     * unrelated to each other.
     * @TODO Test without absorbing small sets. Reason: the larger set contains 
     * elements that can get activated when not needed. Also review the code in
     * the loght of Tom's remarks: see excel sheet for a problem description.
     * @ToDo: compare with Tom's code for Alexandria.Works
     */
    private void absorbSmallSets()
    {
        // The map with topical facets is inverted: the docSet becomes the key.
        Map tmpMap = alg.inverseMap(mTopicalFacetMap);
        // The doc-id sets are sorted on their size, small sets first.
        Set[] docArray = (Set[])tmpMap.keySet().toArray(new Set[tmpMap.keySet().size()]);
        LinkedList docSetList = new LinkedList(Arrays.asList(docArray));
        Collections.sort(docSetList, setSize);
        // Finalizes the new inverted and sorted map.
        LinkedHashMap invertedMap = new LinkedHashMap();
        Iterator list_itr = docSetList.iterator();
        while(list_itr.hasNext())
        {
            SortedSet docSet = (TreeSet) list_itr.next();
            // Gets the key out of this set.
            List tmpSet = Arrays.asList(((HashSet) tmpMap.get(docSet)).toArray());
            invertedMap.put(docSet, tmpSet.get(0));
        }
        // Makes two arrays to iterate over the list with doc-sets in a double loop.
        // A map is made with the small sets to be merged as key and the larger 
        // target set as value.
        LinkedHashMap mergeMap = new LinkedHashMap();
        TreeSet[] loop1, loop2;
        loop1 = (TreeSet[])docSetList.toArray(new TreeSet[docSetList.size()]);
        loop2 = loop1;
        for(int i = 0; i < loop1.length; i++)
        {
            SortedSet thisSet = (TreeSet) loop1[i];
            Integer thisKey = (Integer) invertedMap.get(thisSet);
            for(int j = 0; j < loop2.length; j++)
            {
                // Checks which element of thisSet is contained in the nextSet 
                // on the condition that the first loop-index and the second 
                // loop-index do not point to each other(symmetric matrix).
                if( i < j)
                {
                    SortedSet nextSet = (TreeSet) loop2[j];
                    Integer nextKey = (Integer) invertedMap.get(nextSet);
                    // The smaller set is merged with a larger set on the condition
                    // that the smaller set is an integral part the larger one.
                    List splitSets = Algorithms.split(thisSet, nextSet);
                    int commonSize = ((TreeSet) splitSets.get(0)).size();
                    if(commonSize == thisSet.size())
                    {
                        // Keeps track of small sets that should be removed,
                        // possibly more than once.
                        List largeKeyList = new ArrayList();
                        largeKeyList.add(nextKey);
                        if(mergeMap.keySet().contains(thisKey))
                            largeKeyList.addAll((ArrayList) mergeMap.get(thisKey));
                        mergeMap.put(thisKey, largeKeyList);
                    }
                }
            }
        }
        updateAllUnits(mergeMap);
    }
    
    /** This Comparator sorts a map on set size from small to large. 
     * The Comparator is tuned so that it produces an ordering that is compatible
     * with 'equals'. It's done in a two-part comparison, where the first part 
     * is the one that defines the ordering (compareTo) and where the second
     * part is an attribute (the hashCode) that uniquely identifies the objects 
     * to avoid data loss when the elements compare to the same value.
     */
    public final Comparator setSize = (Comparator) (Object o1, Object o2) -> {
        Integer set1 = ((TreeSet) o1).size();
        Integer set2 = ((TreeSet) o2).size();
        int size = set1.compareTo(set2);
        if (size != 0) return size;
        return (o1.hashCode() < o2.hashCode() ? -1 : (o1.hashCode() == 
                o2.hashCode() ? 0 : 1 ));
    };
    
    /** updateAllUnits: After absorbing the small doc-sets, the semanticUnit files
     * have to be updated: arcs and their info values are transferred from the 
     * small set to the absorbing sets. The frequency of the doc-ids is reduced
     * and finally the key to the small set is removed.
     * @param mergeMap : a LinkedHashMap with the unitKey (Integer) of the set 
     * to be merged as key and an ArrayList with unitKeys (Integer) of the 
     * larger absorbing set as value.
     */
    private void updateAllUnits(LinkedHashMap mergeMap)
    {
        Iterator merge_itr = mergeMap.keySet().iterator();
        while(merge_itr.hasNext())
        {
            Integer smallKey = (Integer) merge_itr.next();
            List largeKeyList = (ArrayList) mergeMap.get(smallKey);
            Iterator large_itr = largeKeyList.iterator();
            while(large_itr.hasNext())
            {
                Integer largeKey = (Integer) large_itr.next();
                SortedSet docSet = mSemUnit.getDocSet(smallKey);
                mSemUnit.frequencyUpdate(docSet, 1);
                LinkedHashSet arcSet = mSemUnit.getArcSet(smallKey);
                mSemUnit.addArcs(largeKey, arcSet);
                double tmpValue = mSemUnit.getValue(smallKey);
                mSemUnit.addInfoValue(largeKey, tmpValue);
            }
            // Removes the small data set from the topicMap after updating.
            mTopicalFacetMap.remove(smallKey);
        }
    }
    
    /** findThisFacet: Looks for a topical facet starting with this particular 
     * starting file.
     * @param startFile : the doc-id (Integer) that triggers the search for a 
     * topical facet.
     * @return a SortedMap with keys and scores.
     */
    public SortedMap findThisFacet(Integer startFile)
    {
        return getTopicalFacet(startFile);
    }
    
    /** containsDocId: Returns 'true' if this document is registered in the 
     * SemanticUnit class.
     * @param docInt : the document identifier (Integer).
     * @return boolean 'true' when this doc-id is in the SemanticUnit.
     */
    public boolean containsDocId(Integer docInt)
    {
        return mDocIds.contains(docInt);
    }
    
    /** collectActiveDocs: Collects in a SortedSet all 'active' doc-ids, i.e.
     * documents that are seen in one or more topical facets. Topical facets 
     * with a low info value are removed. The low value limit is calculated in 
     * the SemanticUnit class and is the mean info value over all arcs plus 
     * one standard deviation.
     */
    private void collectActiveDocs()
    {
       double limitValue = mSemUnit.getValueLimit();
        mActiveDocs = new TreeSet();
        // Iterates over a copy of the topicFacetMap to allow the removing of 
        // keys in the original.
        LinkedHashMap tmpFacetMap = new LinkedHashMap(mTopicalFacetMap); 
        Iterator top_itr = tmpFacetMap.keySet().iterator();
        while(top_itr.hasNext())
        {
            Integer unitKey = (Integer) top_itr.next();
           double arcValue = mSemUnit.getValue(unitKey);                  
            // Removes small topical facets with a low info value.
            if(arcValue < limitValue) mTopicalFacetMap.remove(unitKey);    
           else 
           {
                SortedSet docSet = (TreeSet) tmpFacetMap.get(unitKey);
                mActiveDocs.addAll(docSet);
           }
        }        
    }
    
    /** relatedFacetSet: Returns a set with the unit-keys of all collections
     * where this doc-id was observed.
     * @param docInt : the document identifier (Integer).
     * @return a HashSet with unitkeys (Integer) having this doc-id in their sets.
     */
    public Set relatedFacetSet(Integer docInt)
    {
        Set unitKeySet = new HashSet();
        Iterator top_itr = mTopicalFacetMap.keySet().iterator();
        while(top_itr.hasNext())
        {
            Integer unitKey = (Integer) top_itr.next();
            SortedSet docSet = (TreeSet) mTopicalFacetMap.get(unitKey);
            if(docSet.contains(docInt)) unitKeySet.add(unitKey);
        }
        return unitKeySet;
    }
    
    /** getTopicalFacetMap: Returns the full map with topical facets.
     * @return a SortedMap with the unit key (Integer) SortedSet of doc-ids 
     * (Integer) as value.
     */
    public LinkedHashMap getTopicalFacetMap()
    {
        return mTopicalFacetMap;
    }
    
    /** getFacetMapSize: Returns the total number of topical facet entries in this map.
     * @return the size of the topic map (int).
     */
    public int getFacetMapSize()
    {
        return mTopicalFacetMap.size();
    }
    
    /** getActiveDocCount: Returns the number of active documents involved 
     * in the topic facets.
     * @return the size of the set with active doc-ids (int).
     */
    public int getActiveDocCount()
    {
        return mActiveDocs.size();
    }
    
    /** getActiveDocs: Returns all active doc-ids from the TopicMap sorted.
     * @return SortedSet with all active doc-ids (Integer).
     */
    public SortedSet getActiveDocs()
    {
        return mActiveDocs;
    }
    
    /** getFacetKeys: Returns all the unit keys that make up the topical facets.
     * @return SortedSet with all facet keys (Integer).
     */
    public SortedSet getFacetKeys()
    {
        SortedSet facetKeys = new TreeSet(mTopicalFacetMap.keySet());
        return facetKeys;
    }
    
    /** getAllDocs: Returns all doc-ids available in the SemanticUnit.
     * @return SortedSet with all available doc-ids (Integer).
     */
    public SortedSet getAllDocs()
    {
        return mDocIds;
    }
    
    /** rescannedDocs: Returns a set with unused documents that were rescanned 
     * and used in this session.
     * @param unusedDocs : LinkedList with all the unused doc-ids (Integer) 
     * having a date before the begindate of this session and that are getting 
     * another chance.
     * @return HashSet of doc-ids (Integer) that successfully participated 
     * in this session, i.e. they got connected to other documents by a topical facet.
     */
    public Set rescannedDocs(LinkedList unusedDocs)
    {
        Object[] tmpArray = unusedDocs.toArray();
        HashSet unusedDocSet = new HashSet(Arrays.asList(tmpArray));
        return Algorithms.intersect(getActiveDocs(), unusedDocSet);
    }
    
    /** isEmpty: Returns 'true' when this SemanticAnalysis instance has not been used.
     * @return boolean 'true' if the main map is empty.
     */
    public boolean isEmpty()
    {
        return mTopicalFacetMap.isEmpty();
    }
    
}
