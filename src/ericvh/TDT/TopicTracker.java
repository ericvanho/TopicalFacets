package ericvh.TDT;

import java.util.*;

/** Class TopicTracker scans over the documents grouped per topic as prepared by
 * the TopicMaker. Between two dates set by the user, the TopicTracker will 
 * deliver topic chains, i.e. a subject picked up by different sources over time.
 * @author Eric Van Horenbeeck
 * Created on 1 februari 2005, 23:39
 */
public class TopicTracker
{
    // LinkedList to store topic maps of the different overlapping pair-days 
    // inside the search scope.
    private final List mTopicList;
    // LinkedList to store the full identification of documents inside each topic map.
    private final List mDocList;
    // Map to hold the chains discovered.
    private LinkedHashMap mChainMap;
    // Used to send results to the screen and to a textfile.
    private StringBuffer mChainString;
    // Begin- and enddate of the scope.
    private String mScope;
    
    /** Constructor
     */
    public TopicTracker()
    {
        mTopicList = new LinkedList();
        mDocList = new LinkedList();
    }
    
    /** TopicMaps are added by the ApplicationManager in ascending date order.
     * A linkedList assures a fixed iteration order.
     * @param topicMap : map with topic key (Integer) and a set of document-ids 
     * as value (Integer).
     */
    public void addTopicMap(Map topicMap)
    {
        mTopicList.add(topicMap);
    }
    
    /** DocTables are added by the ApplicationManager in the same order and for 
     * the same scope as the topic maps. This allows to retrieve the exact 
     * identification and description of a document.
     * @param docTable : docTable instance holding a map with the unique 
     * documentNr as key and instances of DocStats as value and methods to 
     * access these data.
     */
    public void addDocTable(DocTable docTable)
    {
        mDocList.add(docTable);
    }
    
    /** Sets the begin and enddate of the topic chain searching period as 
     * selected by the user.
     * @param dateScope : the start and end date of the topic chain search (String)
     */
    public void setDates(String dateScope)
    {
        mScope = dateScope;
    }
    
    /** Routine called by the ApplicationManager to perform the different 
     * steps necessary to collect
     * documents forming a topic chain inside a scope (and community) set by the
     * user.
     */
    public void makeTopicChain()
    {
        try
        {
            findChain();
            chainToString();
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /** The topic maps are scanned to find documents shared between them. 
     * A topic chain is made if the same document-id is reused from one day-pair
     * (day 1 - day 2) to the next overlapping day-pair (day 2 - day 3) and so on.
     * The first and last day of a track is the scope as set by the user.
     */
    private void findChain()
    {
        int topicChain = 0;
        int secondMap = 1;
        mChainMap = new LinkedHashMap();
        //Visits all the topicMaps from the earliest day to the last day.
        Iterator list_itr = mTopicList.iterator();
        while(secondMap < mTopicList.size())
        {
            //Opens the first topicMap
            Map topicMap1 = (TreeMap) list_itr.next();
            // Opens the second topicMap
            Map topicMap2 = (TreeMap) mTopicList.get(secondMap);
            // Iterates over the topic keys in the first map
            Iterator topMap1_itr = topicMap1.keySet().iterator();
            while(topMap1_itr.hasNext())
            {
                Integer topKey1 = (Integer) topMap1_itr.next();
                // Opens the set with doc-ids of this topic key of the first 
                // topicmap
                SortedSet docSet1 = (TreeSet) topicMap1.get(topKey1);
                Iterator doc1_itr = docSet1.iterator();
                while(doc1_itr.hasNext())
                {
                    // Takes the first doc-ids to compare with the doc-ids 
                    // of the second topic map
                    Integer docInt1 = (Integer) doc1_itr.next();
                    // Iterates over the topic keys in the second map
                    Iterator topMap2_itr = topicMap2.keySet().iterator();
                    while(topMap2_itr.hasNext())
                    {
                        Integer topKey2 = (Integer) topMap2_itr.next();
                        // Opens the set with doc-ids of this topic key of the
                        // second topicmap
                        SortedSet docSet2 = (TreeSet) topicMap2.get(topKey2);
                        // If a docSet of the second map contains the doc-id 
                        // from the first map an entry is made in the chainMap 
                        // and both docSets are added to the chain.
                        if(docSet2.contains(docInt1))
                        {
                            SortedSet keySet = new TreeSet();
                            boolean noEntry = true;
                            String tKey1 = String.valueOf(secondMap - 1) + "_" 
                                    + topKey1;
                            String tKey2 = String.valueOf(secondMap) + "_" 
                                    + topKey2;
                            keySet.add(tKey1);
                            keySet.add(tKey2);
                            // Checks if an entry for this key already exists
                            Set chainKeySet = mChainMap.keySet();
                            Iterator chainKey_itr = chainKeySet.iterator();
                            while (chainKey_itr.hasNext())
                            {
                                Integer chainKey = (Integer)chainKey_itr.next();
                                SortedSet tmpSet = (TreeSet) mChainMap
                                        .get(chainKey);
                                // Adds docSets to the chain if an entry exists 
                                // for this document.
                                if(tmpSet.contains(tKey1) || tmpSet.contains(tKey2))
                                {
                                    tmpSet.add(tKey2);
                                    noEntry = false;
                                }
                            }
                            // Makes new topic chain if no entry is found.
                            if(noEntry)
                            {
                                topicChain++;
                                mChainMap.put(topicChain,keySet);
                            }
                        }
                    }
                }
            }
            secondMap++;
        }
    }
    
    /** Transforms the result of the chain search into a displayable string.
     */
    private void chainToString()
    {
        try
        {
            mChainString = new StringBuffer();
            mChainString.append(mChainMap.size()).append(" Topic Chains tracked"
                    + " in the period ").append(mScope).append('\n');
            Iterator chain_itr = mChainMap.keySet().iterator();
            while(chain_itr.hasNext())
            {
                Integer cKey = (Integer) chain_itr.next();
                mChainString.append("\nTopic chain # ").append(cKey).append('\n');
                SortedSet tmpSet = (TreeSet) mChainMap.get(cKey);
                Iterator tmp_itr = tmpSet.iterator();
                while(tmp_itr.hasNext())
                {
                    String tKey = tmp_itr.next().toString();
                    // Splits key in two elements: first part pointing to the 
                    // original topicMap and the second part pointing to the set
                    // of documents inside that map.
                    Integer mapKey = Integer.parseInt(tKey.substring(0,tKey
                            .indexOf('_')));
                    Integer topKey = Integer.parseInt(tKey.substring(tKey
                            .indexOf('_') + 1));
                    Map topicMap =(TreeMap) mTopicList.get(mapKey);
                    SortedSet docSet = (TreeSet) topicMap.get(topKey);
                    // Opens the docTable that goes with this topic set to 
                    // identify the documents.
                    DocTable tmpDocTable = (DocTable) mDocList.get(mapKey);
                    // Iterates over the docSet of this chain topic
                    Iterator doc_itr = docSet.iterator();
                    while (doc_itr.hasNext())
                    {
                        // Looks up the full document identification of this 
                        // docNr and adds it to the printline.
                        int docNr = ((Integer) doc_itr.next());
                        String docLabel = tmpDocTable.getFilename(docNr);
                        mChainString.append(docLabel).append(", ");
                    }
                }
                // Removes last commma from the print line.
                mChainString = mChainString.deleteCharAt(mChainString
                        .length()-2);
                mChainString.append('\n');
            }
        }
        catch(NullPointerException npe)
        {
            mChainString.append("No Topic Chains found for the period ")
                    .append(mScope);
        }
    }
    
    /** Delivers topic chains, i.e. a subject picked up by different sources
     * over a period in time.
     * @return Map containing topic chains
     */
    public LinkedHashMap getTopicChainMap()
    {
        return mChainMap;
    }
    
    /** Getter returns the String with the documents forming a chain of topics
     * over a period in time as set by the user.
     * @return mChainString a readable String with the results from the 
     * chain search.
     */
    public String getChainString()
    {
        return mChainString.toString();
    }
    
}