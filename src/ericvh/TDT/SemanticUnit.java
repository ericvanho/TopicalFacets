package ericvh.TDT;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class SemanticUnit stores semantic information on documents. There is only 
 * one SemanticUnit instance. It has four maps: one with sets of doc-ids.
 * Doc-ids are put together on the condition that they have one or more arcs in common.
 * The arcs in question are kept in a second map with the same key as the doc-id
 * sets. Thirdly there is a map with the (cumulative) informative value of these
 * arcs again with the same key. Finally a document frequency map counts how 
 * often a document appears in these collections.
 * @author Eric Van Horenbeeck
 * Created on 17 mei 2005, 16:10
 * Revision: Oct. 13, 2006
 */
public class SemanticUnit implements Serializable
{
    // Map to hold the doc-id sets.
    private final Map mDocSetMap;
    // Map to hold the arc sets.
    private final Map mArcMap;
    // Map to hold the informative value of the arcs.
    private final Map mInfoValueMap;
    // Map to hold the document frequency.
    private final Map mDocFreqMap;
    // Instruction to add or to subtract while updating the document frequency counter.
    private final int ADD = 0;
    private final int SUB = 1;
    
    /** Constructor.
     */
    public SemanticUnit()
    {
        mDocSetMap = new HashMap();
        mArcMap = new HashMap();
        mInfoValueMap = new HashMap();
        mDocFreqMap = new HashMap();
    }
    
    /** addUnit: Adds a new set of doc-ids, a new set of arcs and the new info
     * value of the arcs to this class. The frequency counter of the documents 
     * is updated. Called by TopicMaker.
     * @param unitKey : the common key to the maps (Integer).
     * @param docSet : a set of doc-ids (SortedSet with Integer).
     * @param arcSet : a LinkedHashSet of arc keys (String).
     * @param infoValue : the combined info value of the arcs in the arc set (double).
     */
    public void addUnit(Integer unitKey, SortedSet docSet, LinkedHashSet arcSet,
            double infoValue)
    {
        mDocSetMap.put(unitKey, docSet);
        mArcMap.put(unitKey, arcSet);
        mInfoValueMap.put(unitKey, infoValue);
        frequencyUpdate(docSet, ADD);
    }
    
    /** addArcs: Adds a LinkedHashSet of arcs to an existing entry in the 
     * SemanticUnit.
     * @param unitKey : a sequential key to the maps (Integer).
     * @param arcSet : a LinkedHashSet of arc keys (String).
     */
    public void addArcs(Integer unitKey, LinkedHashSet arcSet)
    {
        LinkedHashSet tmpSet = (LinkedHashSet)mArcMap.get(unitKey);
        tmpSet.addAll(arcSet);
        mArcMap.put(unitKey, tmpSet);
    }
    
    /** addInfoValue: Adds the info value of new arcs to an existing entry in 
     * the SemanticUnit.
     * @param sequentialKey : the sequential key to the maps (Integer).
     * @param infoValue : combined info value of the arcs in the arc set(double).
     */
    public void addInfoValue(Integer sequentialKey, double infoValue)
    {
        double tmpValue = ((Double) mInfoValueMap.get(sequentialKey));
        tmpValue += infoValue;
        mInfoValueMap.put(sequentialKey, tmpValue);
    }
    
    /** containsSet: Checks whether this docSet is registered here.
     * @param docSet : a SortedSet with doc-ids (Integer).
     * @return boolean 'true' if this docSet is contained in this SemanticUnit.
     */
    public boolean containsSet(Set docSet)
    {
        return mDocSetMap.containsValue(docSet);
    }
    
    /** containsDocId: Returns 'true' if this document is known by the SemanticUnit.
     * @param docId : the identification number of a document (Integer).
     * @return boolean 'true' if this doc-id is registered with the SemanticUnit.
     */
    public boolean containsDocId(Integer docId)
    {
        return getAllDocIds().contains(docId);
    }
    
    /** getKey: Returns the unit key for this docSet in the SemanticUnit or -1 
     * when not found.
     * @param docSet : a set of doc-ids (SortedSet with Integer).
     * @return the unit key for this set (int).
     */
    public int getKey(SortedSet docSet)
    {
        int unitKey = -1;
        Iterator set_itr = mDocSetMap.keySet().iterator();
        while(set_itr.hasNext())
        {
            Integer thisKey = (Integer) set_itr.next();
            SortedSet thisSet = (TreeSet) mDocSetMap.get(thisKey);
            if(thisSet.equals(docSet))
            {
                unitKey = thisKey;
                break;   // Early exit when found.
            }
        }
        return unitKey;
    }
    
    /** getDocKeySets: Collects the unit keys of all the sets where this 
     * doc-id was seen.
     * @param docId : the identification number of a document (Integer).
     * @return a LinkedList with unit keys (Integer).
     */
    public LinkedList getDocKeySets(Integer docId)
    {
        LinkedList docKeyList = new LinkedList();
        Iterator set_itr = mDocSetMap.keySet().iterator();
        while(set_itr.hasNext())
        {
            Integer unitKey = (Integer) set_itr.next();
            SortedSet docSet = (TreeSet) mDocSetMap.get(unitKey);
            if(docSet.contains(docId)) docKeyList.add(unitKey);
        }
        Collections.sort(docKeyList);
        return docKeyList;
    }
    
    /** getRelatedDocs: Collects all doc-ids that are in a set linked to a 
     * starting file in the SemanticUnit.
     * @param startId : the identification number of a document (Integer).
     * @return a SortedSet with the doc-ids (Integer) linked to this starting file.
     */
    public SortedSet getRelatedDocs(Integer startId)
    {
        SortedSet relatedDocSet = new TreeSet();
        Iterator set_itr = mDocSetMap.keySet().iterator();
        while(set_itr.hasNext())
        {
            Integer unitKey = (Integer) set_itr.next();
            SortedSet docSet = (TreeSet) mDocSetMap.get(unitKey);
            if(docSet.contains(startId)) relatedDocSet.addAll(docSet);
        }
        return relatedDocSet;
    }
    
    /** getAllDocIds: Gets all the doc-ids in the SemanticUnit sorted in one set.
     * @return a SortedSet with all the registered doc-ids (Integer).
     */
    public SortedSet getAllDocIds()
    {
        Set allDocIdSet = new HashSet();
        Iterator set_itr = mDocSetMap.keySet().iterator();
        while(set_itr.hasNext())
        {
            Integer key = (Integer) set_itr.next();
            SortedSet docSet = (TreeSet) mDocSetMap.get(key);
            allDocIdSet.addAll(docSet);
        }
        return new TreeSet(allDocIdSet);
    }
    
    /** getDocSet: Returns the doc-id set linked to this key.
     * @param unitKey : the common key (Integer) to the maps in the SemanticUnit.
     * @return a SortedSet with all doc-ids (Integer).
     */
    public SortedSet getDocSet(Integer unitKey)
    {
        return (TreeSet) mDocSetMap.get(unitKey);
    }
    
    /** getArcSet: Returns the arc set linked to this key.
     * @param unitKey : the common key (Integer) to the maps in the SemanticUnit.
     * @return a LinkedHashSet with all arc-keys (String).
     */
    public LinkedHashSet getArcSet(Integer unitKey)
    {
        return (LinkedHashSet) mArcMap.get(unitKey);
    }
    
    /** getValue: Returns the info value linked to this key. It is the sum 
     * of the individual arc values.
     * @param unitKey : the common key (Integer) to the maps in the SemanticUnit.
     * @return the total info value (Double) for this unitKey.
     */
    public Double getValue(Integer unitKey)
    {
        return (Double) mInfoValueMap.get(unitKey);
    }
    
    /** getValueArray: Returns the total info value of all arcs and the sum of 
     * the squared values.
     * @return array with the summed arc values (double) at index '0' and the
     * squared sums (double) at index '1'.
     */
    private double[] getValueArray()
    {
        double [] valueArray = new double[2];
        double summedValue = 0.0;
     //   double sumSqrd = 0.0;
        Iterator value_itr =  mInfoValueMap.values().iterator();
        while(value_itr.hasNext())
        {
            double infoValue = ((Double) value_itr.next());
            summedValue += infoValue;
    //        sumSqrd += infoValue * infoValue;
        }
        valueArray[0] = summedValue;
    //    valueArray[1] = sumSqrd;
        return valueArray;
    }
    
    /** getFullArcCount: Returns the number of arcs in the ArcMap.
     * @return the total number of arcs (int).
     */
    private int getFullArcCount()
    {
        int arcCount = 0;
        Iterator arc_itr = mArcMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            Integer key = (Integer) arc_itr.next();
            LinkedHashSet arcSet = (LinkedHashSet) mArcMap.get(key);
            arcCount += arcSet.size();
        }
        return arcCount;
    }
    
    /** getValueLimit: Returns the limit value for a topical facet to be 
     * acceptable. It is the mean of the info value over all arcs plus one
     * standard deviation.
     * @return the limit value (double) for a topical facet to be acceptable.
     */
    public double getValueLimit()
    {
        double[] valueArray = getValueArray();
        int totCnt = getFullArcCount();
        double mean = valueArray[0] / totCnt;  
       // double stDev = StrictMath.sqrt((valueArray[1] - valueArray[0] *
       // valueArray[0] / totCnt) / totCnt);  
        return mean; // + stDev;
    }
    
    /** frequencyUpdate: Counts the occurrences of each doc-id and keeps this 
     * number in a frequency map (HashMap) with the doc-id as key (Integer) and
     * its frequency counter (int) as value. Updates when new docSets are added 
     * to the SemanticUnit.
     * @param docSet : a SortedSet of doc-ids (Integer).
     * @param update : instruction to ADD or to SUBtract '1' (int) from the 
     * document count.
     */
    public void frequencyUpdate(SortedSet docSet, int update)
    {
        // Iterates over the frequencyMap and updates the count or adds 
        // a new doc entry.
        Iterator doc_itr = docSet.iterator();
        while(doc_itr.hasNext())
        {
            int frequencyCount = 1;
            Integer docInt = (Integer) doc_itr.next();
            if(mDocFreqMap.keySet().contains(docInt))
            {
                frequencyCount = ((Integer) mDocFreqMap.get(docInt));
                if(update == ADD) frequencyCount++;
                else frequencyCount--;
            }
            mDocFreqMap.put(docInt, frequencyCount);
        }
    }
    
    /** getDocFrequency: Returns how often this document was seen in the SemanticUnit.
     * @param docInt : the identification number of a document (Integer).
     * @return the overall frequency of this document (int).
     */
    public int getDocFrequency(Integer docInt)
    {
        return ((Integer) mDocFreqMap.get(docInt));
    }
    
    /** getUnitMap: Returns the full doc-id map of the SemanticUnit.
     * @return the docSet map (HashMap) with a sequential key (Integer) 
     * and SortedSet of doc-ids (Integer) as value.
     */
    public Map getUnitMap()
    {
        return mDocSetMap;
    }
    
    /** semanticUnitSize: Returns the size of the SemanticUnit based on the 
     * number of entries in the docSet map.
     * @return the size of the docSet map (int).
     */
    public int semanticUnitSize()
    {
        return mDocSetMap.size();
    }
    
    /** isEmpty: Checks whether the SemanticUnit is empty.
     * @return boolean 'true' when there are no entries in the doc-id map.
     */
    public boolean isEmpty()
    {
        return mDocSetMap.isEmpty();
    }
    
}
