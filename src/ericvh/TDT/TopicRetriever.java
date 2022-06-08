package ericvh.TDT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/** Class TopicRetriever has as input a map with one or more tasks each with a 
 * list of tokens prepared by the QueryInput class. There are six different 
 * tasks that can be executed:
 * 0. Documents retrieval;
 * 1. Retrieval of topical facets that answer a question similar to a saved query;
 * 2. Constructing a timeline inside the scope of this session with documents 
 * and sources related to the input of the task;
 * 3. Performing a simple text search with one keyword.
 * 4. The 'serendipity' application: generates a few random tokens and tries 
 * to find a coherent semantic relation between them.
 * 5. Reconstructing the view on the results of a query task.
 * @author Eric Van Horenbeeck
 * Created on June 24, 2005, 11:30
 * Revision: June 22, 2007
 */
public class TopicRetriever
{
    // The original map with one or more retrieval tasks and seeds as prepared 
    // by QueryInput, a set with the tasks identification and a set with 
    // the query tokens.
    private final Map mOrgTaskMap;
    private Set mTaskSet;
    private Set mTokenSet;
    // Description of the retrieval task and the type of action to perform.
    private static String mRetrieveTask;
    private static String mTask;
    private int mAssign;
    // Map to collect the arcs and associations that defined a topical facet.
    private static SortedMap mFacetArcMap;
    // Map to collect a set of topical facets for every token from the seed 
    //document (query).
    private static SortedMap mTokenFacetMap;
    // Map to collect documents linked to the topical facets per count class.
    private static LinkedHashMap mFacetDocMap;
    // Map to collect the topical facets that belong to a document.
    private static SortedMap mDocFacetMap;
    // Map to collect the topical facets where an arc was used.
    private static SortedMap mArcFacetMap;
    // Map to hold the consolidated facet dictionary.
    private final SortedMap mFacetDictionary;
    // Set with doc paths to perform the doc-by-doc similarity.
    private static LinkedHashSet mDocSimSet;
    // List of lists with the doc-ids and arcs to send to DocAnalysis.
    private static List mDocArcList;
    // Set with doc-ids to send to ApplicationManager.
    private static SortedSet mDocIdSet;
    // The number of informative arcs found in the seed document (the 'query').
    private static int mSeedArcSize;
    // User decision in the RetrievedFacetViewer class: '0' = continue with 
    // removal of negative facet instances; '1' = continue without any 
    // elimination; '2' = abort all retrieval tasks.
    private static int mUserDecision;
    // The user or the application can abort the retrieval task when no 
    // retrievable documents were found.
    private boolean mContinueTask = true;
    // Set with negative facet keys to remove from this retrieval task.
    private static Set mNegativeKeys;
    // Set with the vertex-ids from the informative tokens from the seed document.
    private static Set mInformativeTokens;
    // Total informative value of the tokens from the query.
    private static double mQueryValue;
    // Calendar dates marking the begin- and the enddate of the time interval 
    // used in this session.
    private final Calendar mBeginDate;
    private final Calendar mEndDate;
    // File input and output routines.
    private InputOutput io = new InputOutput();
    // Retrieves the necessary vertex-facet data from disk.
    private Consolidator facetMapRetreiver = new Consolidator();
    // Table with information on documents such as sets with all the facets 
    // related to a document.
    private static final DocTable mDocTable = ApplicationManager.getDocTable();
    // Instance of the RetrievedFacetViewer. A GUI to let the user inspect 
    // the topical facets.
    private RetrievedFacetViewer facetView;
    // File separator.
    private final String separator = System.getProperty("file.separator");
    // LabelTable instance with labels for rendering a token as a string.
    private final LabelTable labels;
    
    /** Constructor
     * @param dictionaryMap : SortedMap providing a direct link between a topical
     * facet and the topic file on disk. Key is the global facet key (Integer), 
     * value an ArrayList with the local topic-keys (Integer) at index '0' and 
     * the full path (String) to the original topicMap at index '1'. It is the
     * 'FacetKeys.dic' file retrieved by the ApplicationManager from 
     * the 'Global' directory.
     */
    public TopicRetriever(SortedMap dictionaryMap)
    {
        this.tokenIndex = (Comparator) (Object o1, Object o2) -> {
            Integer w1 = Integer.valueOf(o1.toString().substring(0, 1)) - 1;
            Integer w2 = Integer.valueOf(o2.toString().substring(0, 1)) - 1;
            int index = w1.compareTo(w2);
            if (index != 0) return index;
            return (o1.hashCode() < o2.hashCode() ? -1
                    : (o1.hashCode() == o2.hashCode() ? 0 : 1 ));
        };
        // HashMap with a taskname (String) as key and as value a HashMap with
        // arc keys and arc positions for a document retrieval task (assign == 0)
        // or with an ArrayList with tokens (Integer) for other tasks (assign > 0).
        mOrgTaskMap = ApplicationManager.getSeeds();
        // LabelTable class instance;
        labels = ApplicationManager.getLabels();
        // Set with the task identification.
        prepareTaskSet();
        // The global facet-document dictionary with the consolidated and 
        // local facets.
        mFacetDictionary = dictionaryMap;
        // Only collects the files within this time interval (scope).
        mBeginDate = ApplicationManager.getBeginDate();
        mEndDate = ApplicationManager.getEndDate();
    }
    
    /** collectFacets: topical facets are collected for every token in the query
     * that is known to the application. These topical facets create the link 
     * between the query and the available data. The content used here is a 
     * subset of the complete data collection and is defined by the time interval
     * of this session. Called by the ApplicationManager.
     * @param taskName : String with the full identification of the task at hand.
     * @throws IOException
     */
    public void collectFacets(String taskName) throws IOException
    {
        // Task identification.
        mRetrieveTask = taskName;
        int uScore = mRetrieveTask.indexOf("_") + 1;
        mTask = mRetrieveTask.substring(0, uScore - 1);
        setAssign(mRetrieveTask.substring(uScore));
        // Preparation of the query token set.
        if(mAssign > 0) prepareTokenList();
        else prepareQueryDoc();
        // SortedSet with all the tokens from the query.
        SortedSet seedTokens = sortQueryTokens(mTokenSet);
        int tokenCount = seedTokens.size();
        // Counting the empty sets linked to a query token.
        int emptySets = 0;
        facetMapRetreiver.setTotal(tokenCount, ApplicationManager
                .getApplicationStartTime());
        // Clears the message area.
        ApplicationManager.clearText();
        ApplicationManager.showText(getTaskName() + " - Scope: " +
                ApplicationManager.getScope() +'\n', 0);
        // Map to collect for every token from the seed document (query) a set
        // of topical facets.
        mTokenFacetMap = new TreeMap();
        // Iterates over all the informative tokens from the seed document.
        Iterator token_itr = seedTokens.iterator();
        while(token_itr.hasNext())
        {
            // This set gathers all the global facet keys that are inside the 
            // scope of this task.
            SortedSet facetInScope = new TreeSet();
            Integer queryTokenId = (Integer) token_itr.next();
            // Retrieves the map with the global facet collection for this token.
            SortedMap allFacetMap = getVertexFacetCollection(queryTokenId, 
                    tokenCount--);
            if(allFacetMap.containsKey(queryTokenId))
            {
                // Gets all the global topical facet keys linked to this token.
                SortedSet facetSet = new TreeSet((HashSet)allFacetMap
                        .get(queryTokenId));
                // Iterates over all facets to check their scope. Only facets 
                // in the scope of this session are retained for further use.
                Iterator facet_itr = facetSet.iterator();
                while(facet_itr.hasNext())
                {
                    Integer globalFacet = (Integer) facet_itr.next();
                    List facetList = (ArrayList) mFacetDictionary
                            .get(globalFacet);
                    // Fetches the path to the local topicMap to check the scope.
                    String facetPath = facetList.get(1).toString();
                    if(isInScope(facetPath)) 
                    {
                        facetInScope.add(globalFacet);
                    }
                }
                // Makes an entry even if the facet set is empty. This allows 
                // the user to see all the tokens from the query and their 
                // contribution to a relevant document in the Facet Viewer. 
                // An empty set means that the application recognizes the token
                // but that its related facets are located outside the 
                // time interval of this session, either before or after the 
                // dates set by the user. Unknown tokens were filtered out 
                // earlier by QueryInput.
                mTokenFacetMap.put(queryTokenId, facetInScope);
                if(facetInScope.isEmpty()) emptySets++;
            }
        }
        
        // The user is warned if more than 2 empty facet sets were found because 
        // this most certainly reduces the quality of the final result.
        if(emptySets > 2) ApplicationManager.showText("\n** Warning for task " 
                + getTaskName() + " with scope " + ApplicationManager.getScope() 
                + " **\nIn this session no topical facets could"
            + " be connected to " + emptySets + " informative query tokens."
                    + "\nThis might seriously reduce the quality of the output! "
                    + "Consider to rerun this task with a different time"
            + " interval.\n", 0);
        
        // Prepares a map for performing a simple document search with one 
        // query term.
        if(mAssign == 3)
        {
            // Collecting all the documents and relevant arcs related to 
            // this search term.
            SortedMap searchFacetMap = new TreeMap();
            Set keySet = mTokenFacetMap.keySet();
            Integer[] keys = (Integer []) keySet.toArray(new Integer[keySet.size()]);
            Set facets = new HashSet();
            SortedSet tmpSet = (TreeSet) mTokenFacetMap.get(keys[0]);
            Iterator tmp_itr = tmpSet.iterator();
            while(tmp_itr.hasNext()) facets.add(tmp_itr.next());
            searchFacetMap.put(1, facets);
            collectDocsAndArcs(searchFacetMap);
            executeTask();
        }
        // Only shared topical facets are retained.
        else  retainSharedFacets();
    }
    
    /** getVertexFacetCollection: Vertex-facet pairs are kept in a repository 
     * identified by the first digit of the vertex key. There are nine separate
     * maps on disk. Consolidator retrieves them on demand and keeps the map 
     * that was last accessed in memory for consultation by other vertices with
     * the same initial digit.
     * @param queryTokenId : Integer identifying a token from the query and 
     * a vertex-facet file.
     * @param count : the number of tokens left to process.
     * @return SortedMap with the vertex-id as key (Integer) and a SortedSet 
     * of global topical facet keys (Integer).
     */
    private SortedMap getVertexFacetCollection(Integer queryTokenId, int count)
    {
        String vrtxKey = queryTokenId.toString();
        int index = Integer.parseInt(vrtxKey.substring(0, 1)) - 1;
        return facetMapRetreiver.retrieveVertexFacetMap(index, vrtxKey, count);
    }
    
    /** sortQueryTokens: Query tokens are sorted on the first digit of their keys 
     * allowing optimized batch access to the voluminous vertex-repositories.
     * @param tokenSet : a Set with vertex keys (Integer).
     * @return a SortedSet with the same vertices sorted on the first digit of the key.
     */
    private SortedSet sortQueryTokens(Set tokenSet)
    {
        SortedSet sortedTokens = new TreeSet(tokenIndex);
        Iterator token_itr = tokenSet.iterator();
        while(token_itr.hasNext())
        {
            sortedTokens.add(token_itr.next());
        }
        return sortedTokens;
    }
    /** Comparator sorts the vertices of the query on the first digit of their
     * key from low to high in two steps. The second part uses the hashCode
     * because it uniquely identifies the objects and avoids data loss when the
     * elements compare to the same value in the first step.
     */
    private final Comparator tokenIndex;
    
    /** retainSharedFacets: Retains those facets that are shared by more than 
     * one token from the query. Every facet collection related to a query token
     * is compared one-by-one to all other facet collections. In the same move 
     * all topical facets with too few query tokens are removed. The resulting 
     * map does not quantify how the shared topical facets are distributed over
     * the tokens. This is done by the countSharedFacets-method and results 
     * in a map that ranks the topic facets from shared by all to shared 
     * by just two tokens from the query (the minimum).
     */
    private void retainSharedFacets() throws IOException
    {
        SortedSet sharedFacetSet = new TreeSet();
        Map sharedFacetMap = new HashMap();
        Object[] loop1, loop2;
        Set tokens = mTokenFacetMap.keySet();
        loop1 = (Object[]) tokens.toArray(new Object[tokens.size()]);
        Arrays.sort(loop1);
        loop2 = loop1;
        // Iterates over the tokens in a double loop.
        for(int i = 0; i < loop1.length; i++)
        {
            Integer firstToken = (Integer) loop1[i];
             SortedSet firstFacets  = (TreeSet)  mTokenFacetMap.get(firstToken);
            // All tokens get an entry in the new map. The set of shared facets
            // may stay empty.
            if(!sharedFacetMap.containsKey(firstToken))
            {
                SortedSet tmpShared = new TreeSet();
                sharedFacetMap.put(firstToken, tmpShared);
            }
            // Second loop: intersecting the two topical facet sets.
            for(int j = 0; j < loop2.length; j++)
            {
                if(i < j)
                {
                    Integer secondToken= (Integer) loop2[j];
                      SortedSet secondFacets  = (TreeSet) mTokenFacetMap.
                              get(secondToken);
                    // Ensures an entry for every token in the new map.
                    if(!sharedFacetMap.containsKey(secondToken))
                    {
                        SortedSet tmpShared = new TreeSet();
                        sharedFacetMap.put(secondToken, tmpShared);
                    }
                    // Intersecting collections of topical facets in both loops 
                    // to extract the shared facets.
                    Set shared = Algorithms.intersect(firstFacets, secondFacets);
                    // Retains the shared facets from two tokens only if these 
                    // tokens have at least two of the facets in common.
                    if(shared.size() >= 2)
                    {
                        // The first token gets its part of the shared global
                        // facets and the set of unshared facets with the 
                        // highest value.
                        SortedSet firstShared = (TreeSet) sharedFacetMap
                                .get(firstToken);
                        firstShared.addAll(shared);
                        sharedFacetMap.put(firstToken, firstShared);
                        // Because these facets are shared, the second token 
                        // gets copy of the same set.
                        SortedSet secondShared = (TreeSet) sharedFacetMap
                                .get(secondToken);
                        secondShared.addAll(shared);
                        sharedFacetMap.put(secondToken, secondShared);
                        // Gathering all topical facets involved in one set.
                        sharedFacetSet.addAll(shared);
                    }
                }
            }
        }
        if(!sharedFacetSet.isEmpty()) retrieveControl(sharedFacetMap, 
                sharedFacetSet);
        // No need to continue with this task when no shared topical facets 
        // were found.
        else mContinueTask = false;
    }
    
    /** retrieveControl: Starts the collection of documents and arcs related 
     * to the query.
     * @param sharedFacetMap : HashMap with as key the id (Integer) of a query 
     * token and as value a SortedSet with topical facets (Integer) shared by 
     * this and possibly by other tokens aswell.
     * @param sharedFacetSet : SortedSet with all the topical facets (Integer) 
     * more or less shared by the tokens from the 'query'.
     * @throws IOException.
     */
    private void retrieveControl(Map sharedFacetMap, SortedSet sharedFacetSet) 
            throws IOException
    {
        // Gets a SortedMap with the number of tokens shared by the same facets 
        // as key and as value a HashSet with those topical facets (Integer).
        SortedMap countedFacetMap = countSharedFacets(sharedFacetMap,
                sharedFacetSet);
        // Collecting all the documents and relevant arcs related to these shared
        // topical facets
        collectDocsAndArcs(countedFacetMap);
        // extractDocFacetMap returns a map with the docId as key and a set of 
        // related topical facets as value.
        mDocFacetMap= extractDocFacetMap();
        // A GUI allows the user to inspect the relation between the query, 
        // the topical facets and the documents linked to them. Not available
        // when tasks are batch processed.
        mUserDecision = 1;
        if((ApplicationManager.showFacets() && mTaskSet.size() == 1) 
                || mAssign == 4) setFacetViewer();
        else
        {
            String warning = "";
            int queryLength =  sharedFacetMap.size();
            int sharedTokens = ((Integer) countedFacetMap.firstKey());
            int totFacets = sharedFacetSet.size();
            int sharedFacets = ((Set) countedFacetMap.get(sharedTokens)).size();
            if((double) sharedTokens / queryLength < 0.8 
                    && (double) sharedFacets / sharedTokens < 0.7)
                warning = "\n\n** Warning: Apparently the relation between the"
                        + " query and the topical facets inside the scope of"
                        + " this session is weak.\nThe quality of the output"
                        + " will be poor! Consider to stop and rerun with a"
                        + " different query, a different time interval and/or"
                        + " other parameter settings. **";
            ApplicationManager.showText("\nTotal query tokens: " + queryLength 
                    + ".\nThe largest group of tokens linked to the same facets "
                            + "has " + sharedTokens + " members sharing " 
                    + sharedFacets + " out of " + totFacets + " loosely related"
                            + " facets. " + warning + "\n\nProceeding without "
                                    + "the Facet Viewer", 0);
        }
        // The application continues.
        if(mUserDecision < 2)
        {
            // The number of topical facets is reduced by taking only the main 
            // core from a unipartite transformation of docs and facets. 
            // Only the facets shared by a majority of documents remain.
            reduceMapSize();
            // Reruns the extractDocFacetMap method to reflect the reduced size
            // of the docs and facet maps.
            mDocFacetMap.clear();
            mDocFacetMap = extractDocFacetMap();
            executeTask();
        }
    }
    
    /** countSharedFacets: Counts the number of query tokens that share a given 
     * topical facet. If there are N tokens involved, a given topical facet f 
     * can have a count Cf between Cf <= N and Cf = 1. The more Cf is close to N,
     * the better this topical facet (or set of facets) relates to the query.
     * @param sharedFacetMap : HashMap with as key the id (Integer) of a query 
     * token and as value a SortedSet with topical facets (Integer) shared by 
     * this and possibly by other tokens aswell.
     * @param sharedFacetSet : SortedSet with all the topical facets (Integer) 
     * more or less shared by the tokens from the 'query'.
     * @return SortedMap with number of query tokens (int) as key and a HashSet
     * of shared topical facets (Integer).
     */
    private SortedMap countSharedFacets(Map sharedFacetMap,
            SortedSet sharedFacetSet)
    {
        // The resulting map is sorted in descending order on the number of 
        // tokens sharing the same topic facets.
        SortedMap countMap = new TreeMap(highLow);
        Iterator facet_itr = sharedFacetSet.iterator();
        while(facet_itr.hasNext())
        {
            int tokenCount = 0;
            Integer facetKey = (Integer) facet_itr.next();
            Iterator token_itr = sharedFacetMap.keySet().iterator();
            while(token_itr.hasNext())
            {
                Integer tokenId = (Integer) token_itr.next();
                SortedSet facetSet = (TreeSet) sharedFacetMap.get(tokenId);
                if(facetSet.contains(facetKey)) tokenCount++;
            }
            Set sharedSet = new HashSet();
            if(countMap.containsKey(tokenCount)) sharedSet.addAll((HashSet) 
                    countMap.get(tokenCount));
            sharedSet.add(facetKey);
            if(!sharedSet.isEmpty()) countMap.put(tokenCount, sharedSet);
        }
        return countMap;
    }
    
    /** Comparator sorts the facet count map on the count value from high to low.
     */
    private final Comparator highLow = (Comparator) (Object o1, Object o2) -> 
    {
        Integer w1 = (Integer) o1;
        Integer w2 = (Integer) o2;
        return w2.compareTo(w1);
    };
    
    /** collectDocsAndArcs: Collects all the documents and relevant arcs available
     * in this dataGraph that are related to one or more of the retrieved topical
     * facets inside the time interval for this session.
     * @param countMap : SortedMap with a count (int) of tokens sharing the same
     * facets as key and a HashSet of shared global topical facet keys(Integer)
     */
    private void collectDocsAndArcs(SortedMap countMap)
    {
        // SortedMap to collect arcs that defined the topical facet per count class.
        mFacetArcMap = new TreeMap(highLow);
        // LinkedHashMap to collect documents linked to the topical facets per
        // count class.
        mFacetDocMap = new LinkedHashMap();
        // Temporary maps to hold topic and arc maps
        Map localFacetMaps = new HashMap();
        Map localArcMaps = new HashMap();
        // The number of count-sets to process is limited to a 'countMax' maximum.
        int countLimit = 0;
        int countMax = StrictMath.min(10, ((Integer) countMap.firstKey()));
        // Iterates over the count classes.
        Iterator count_itr = countMap.keySet().iterator();
        while(count_itr.hasNext() && countLimit < countMax)
        {
            Map facetDocs = new HashMap();
            Map facetArcs = new HashMap();
            int count = ((Integer) count_itr.next());
            Set sharedFacets = (HashSet) countMap.get(count);
            Iterator facet_itr = sharedFacets.iterator();
            while(facet_itr.hasNext())
            {
                SortedSet allDocSet = new TreeSet();
                Integer globalFacet = (Integer) facet_itr.next();
                // mFacetDictionary has a global facet key and as value an 
                // ArrayList with the local facet at index '0' and the full path
                // to the original topicMap at index '1'.
                List facetList = (ArrayList) mFacetDictionary.get(globalFacet);
                // Fetches the local facet key, the path to the local topicMap 
                // and its scope.
                Integer localFacet = (Integer) facetList.get(0);
                String facetPath = facetList.get(1).toString();
                // The path to a local topicArcsMap and associations map within 
                // the same scope.
                String arcPath = facetPath.replace("_Topics.tpc", "_TopArcs.tas");
                // Looks for already retrieved local topic, arc and associations maps.
                LinkedHashMap topicMap = new LinkedHashMap();
                LinkedHashMap arcMap = new LinkedHashMap();
                if(localFacetMaps.containsKey(facetPath))
                {
                    topicMap = (LinkedHashMap) localFacetMaps.get(facetPath);
                    arcMap = (LinkedHashMap) localArcMaps.get(arcPath);
                }
                // Reads the files from the hard disk if this path is used for
                // the first time.
                else
                {
                    try
                    {
                        topicMap = io.readLinkedHashMap(facetPath);
                        arcMap = io.readLinkedHashMap(arcPath);
                    }
                    catch(Exception e)
                    {
                        ApplicationManager.showText("Problems reading local "
                                + "facet file: " + localFacet + " scope: "
                                + GraphTime.getFullScope(facetPath), 0);
                    }
                    // Keeps these maps available for further use.
                    localFacetMaps.put(facetPath, topicMap);
                    localArcMaps.put(arcPath, arcMap);
                }
                // Collects the docIds for this facet.
                try
                {
                    allDocSet.addAll((TreeSet) topicMap.get(localFacet));
                    facetDocs.put(globalFacet, allDocSet);
                    // Gets the number of documents linked to this topical facet.
                    int docSetSize = allDocSet.size();
                    // Composes a map with the global facet key and the topic 
                    // arcs in a set.
                    SortedSet arcSet = new TreeSet((LinkedHashSet) arcMap
                            .get(localFacet));
                    facetArcs.put(globalFacet, arcSet);
                } catch (NullPointerException npe)
                {
                    // ignore for now
                }
                }
                mFacetArcMap.put(count, facetArcs);
                mFacetDocMap.put(count, facetDocs);
                // Counting the number of count-sets processed so far.
                countLimit++;
            }
        }
    /** extractDocFacetMap: Prepares a SortedMap with as value a SortedSet of
     * all topical facets (Integer)related to a document. Is partly used by 
     * DocAnalysis and UnipartiteCore.
     * @return SortedMap with document ID  (Integer) as key and a SortedSet with
     * all related facets (Integer).
     */
    private SortedMap extractDocFacetMap()
    {
        LinkedHashMap workMap;
        SortedMap tmpMap = new TreeMap();
        
        // Extract facets linked to each document.
        workMap = mFacetDocMap;
        Iterator map_itr = workMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            int count = ((Integer) map_itr.next());
            Map facetMap = (HashMap) workMap.get(count);
            Iterator facet_itr = facetMap.keySet().iterator();
            while(facet_itr.hasNext())
            {
                Integer facetKey = (Integer) facet_itr.next();
                SortedSet itemSet = (TreeSet) facetMap.get(facetKey);
                Iterator item_itr = itemSet.iterator();
                while(item_itr.hasNext())
                {
                    Object  id = (Object) item_itr.next();                  
                    SortedSet facetSet = new TreeSet();
                    if(tmpMap.containsKey(id))
                    {
                        facetSet.addAll((TreeSet) tmpMap.get(id));
                    }
                    facetSet.add(facetKey);
                    tmpMap.put(id, facetSet);
                }
            }
        }
        return tmpMap;
    }
    
    /** reduceMapSize : A unipartite graph reduction from facets and docs, 
     * followed by a core extraction, reduces the number of topical facets to 
     * those that are effectively shared by a majority of the documents.
     * All the others are removed from the FacetDocMap and the FacetArcMap.
     */
    private void reduceMapSize()
    {
        UnipartiteCore core = new UnipartiteCore();
        core.setCoreNumber(ApplicationManager.getCoreNumber());
        core.setMainFacetCore();
        Set mainFacets = core.getMainFacetCore();
        // Collects the facetKeys from facetDocMap.
        Set facetKeySet = new HashSet();
        Iterator count_itr = mFacetDocMap.keySet().iterator();
        while(count_itr.hasNext())
        {
            int count = ((Integer) count_itr.next());
            Map facetMap = (HashMap) mFacetDocMap.get(count);
            facetKeySet.addAll(facetMap.keySet());
        }
        // Gets the redundant facet keys: facets found in the facetDocMap and 
        // not in the main core set.
        Set removeSet = Algorithms.difference(facetKeySet, mainFacets);
        // Removes the redundant keys.
        removeNegative(removeSet);
    }
    
    /** executeTask: In the next step these data are transferred to one of five 
     * specific retrieval tasks according to the assignment set by the user:
     * - selecting documents relevant to the seed document (assignment '0');
     * - collecting relevant topical facets answering a question (assignment '1');
     * - constructing a timeline indicating the number of texts related to the 
     * query delivered per source (assignment '2').
     * - performing a simple text search. (assignment '3').
     * - random serendipity. (assignment '4').
     * @ToDo : implementing QandA, TimeRetrieval, Simple Text Search and
     * Serendipity.
     */
    private void executeTask() throws IOException
    {
        // Selection is a flag (int) to indicate what group of selected docs to
        // return. '0' selects the group of documents with the highest number 
        // of facets in common with the input document, '2' selects the group
        // up to the highest, '1' is the middle group up to the highest and '3'
        // returns all documents from low up to the highest.
        int selection = ApplicationManager.getRetrievedSelection();
        switch(mAssign)
        {
            case 0:
                // Retrieves documents and arcs related to this input.
                DocRetrieval prepareDocBaskets = new DocRetrieval();
                mDocSimSet = new LinkedHashSet();
                mDocArcList = new ArrayList();
                mDocArcList = prepareDocBaskets.getDocGroup(selection);
                mDocSimSet = prepareFileSet(mDocArcList);
                break;
            case 1:
                // Q&A. Retrieves the topical facets answering a question.
                // Not yet implemented.
                QandA retrieveFacet = new QandA();
                mContinueTask = false;
                break;
            case 2:
                // Constructs a timeline counting documents and sources about 
                // this input.
                // Not yet implemented.
                TimeRetrieval retrieveTime = new TimeRetrieval();
                mContinueTask = false;
                break;
            case 3:
                // Simple Text Search
                // Not yet implemented.
                SimpleSearch search = new SimpleSearch(mFacetDocMap, mDocTable);
                mContinueTask = false;
                break;
            case 4:
                // Random Serendipity
                // Not yet implemented
                Serendipity serendip = new Serendipity();
                mContinueTask = false;
            default:
                ApplicationManager.showText("* No retrieval task found *", 0);
                mContinueTask = false;
                break;
        }
    }
    
    /** isInScope: Checks if this file falls inside the time interval of this 
     * retrieval task as set by the user.
     * @param filePath : String with full path to the file to be checked.
     * @return boolean 'true' is this file is inside the scope, 'false' otherwise.
     */
    private boolean isInScope(String filePath)
    {     
        if(ApplicationManager.getScope().equals("AllDates")) return true; 
        Calendar date = Calendar.getInstance();
        date.set(0000, 00, 00);
        
        String filename = filePath.substring(filePath.lastIndexOf(separator)+1);
        // Gets the two parts of this topic-file's scope.
        GraphTime.getDate(date, filename);
        Calendar firstDate = GraphTime.getFirstScope(date);
        Calendar lastDate = GraphTime.getLastScope(date);
        // Accepts files when the firstDate and lastDate fall inside the scope
        // of this task.
        return GraphTime.isBetweenDates(firstDate, mBeginDate, mEndDate) &&
                GraphTime.isBetweenDates(lastDate, mBeginDate, mEndDate);
    }
    
    /** translateSeedMap: Prepares a HashSet with the full task identification
     * (String). The first part is the task name and the second part after 
     * the underscore is the task 'assignment'.
     */
    private void prepareTaskSet()
    {
        mTaskSet = new HashSet();
        Iterator task_itr = mOrgTaskMap.keySet().iterator();
        while(task_itr.hasNext()) mTaskSet.add(task_itr.next().toString());
    }
    
    /** prepareTokenList: Prepares a tokenlist for tasks with assign > 0.
     */
    private void prepareTokenList()
    {
        List seedTokens = (ArrayList) mOrgTaskMap.get(mRetrieveTask);
        mTokenSet = new HashSet();
        Iterator token_itr = seedTokens.iterator();
        while(token_itr.hasNext())
        {
            String token = token_itr.next().toString();
            Integer tokenId = labels.getVertexIndex(token);
            if(tokenId != null) mTokenSet.add(tokenId);
        }
    }
    
    /** prepareQueryDoc: Translates the query arcs into a set of informative 
     * vertices. Attributes the next available number in the DocTable as a new 
     * doc_id to this query. Creates a DocStats instance and adds it to 
     * the DocTable. These instances of DocStat and DocTable are not persistent,
     * they only exist for the duration of this session.
     */
    private void prepareQueryDoc()
    {
        mTokenSet = new HashSet();
        Map tokenMap = (HashMap) mOrgTaskMap.get(mRetrieveTask);
        if(tokenMap.containsKey("stat"))  tokenMap.remove("stat");
        // Prepares a set with informative tokens necessary to collect the
        // related topical facets.
        Iterator arc_itr = tokenMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            String arcKey = arc_itr.next().toString();
            Integer vOne = Integer.valueOf(arcKey.substring(0, arcKey
                    .indexOf("*")));
            Integer vTwo = Integer.valueOf(arcKey.substring(arcKey
                    .indexOf("*") + 1));
            if(!vOne.equals(0)) mTokenSet.add(vOne);
            if(!vTwo.equals(0)) mTokenSet.add(vTwo);
        }
    }
    
    /** prepareFileSet: Prepares a set with the full pathnames of the files 
     * necessary to compute in the next step the doc-by-doc similarity by an 
     * instance the DocAnalysis class.
     * @param docList : an ArrayList with ArrayLists with doc-ids (Integer) and
     * arcs (String) selected by the DocRetrieval class.
     * @return LinkedHashSet with full filepaths (String) to perform the 
     * doc-by-doc similarity.
     */
    private LinkedHashSet prepareFileSet(List docList) throws IOException
    {    
        Calendar fileDate = Calendar.getInstance();
        fileDate.set(0000, 00, 00);
        LinkedHashSet docSet = new LinkedHashSet(3);
        mDocIdSet = new TreeSet();
        String thisWorkPath = ApplicationManager.getWorkPath();
        Iterator doc_itr = docList.iterator();
        while(doc_itr.hasNext())
        {
            List docArray = (ArrayList) doc_itr.next();
            Integer docId = (Integer) docArray.get(0);
            mDocIdSet.add(docId);
            String txtGrphFile = mDocTable.getFilename(docId) + ".tgr";
            String thisScope = GraphTime.constructFileScope(fileDate, txtGrphFile);
            String fullVertexPath = thisWorkPath + separator + thisScope
                    + "_Vertices.vrtx";
            String fullInfoArcPath = thisWorkPath + separator + thisScope
                    + "_InfoArc.arcs";
            ApplicationManager.setGraphStoreName(ApplicationManager
                    .getDataStoreArg(0, txtGrphFile));
            String thisGraphPath = ApplicationManager.getGraphPath();
            String fullTextGraphPath = thisGraphPath + separator + txtGrphFile;
            docSet.add(fullTextGraphPath);
            docSet.add(fullVertexPath);
            docSet.add(fullInfoArcPath);
        }     
        return docSet;
    }
    
    /** removeNegative: Removes from the facetDocMap, the facetArcMap the 
     * redundant topical facets or the negative topical facets selected 
     * by the user and the set with documents and arcs linked to them.
     * @param removeSet : Set with topical facet keys (Integer) to remove 
     * from two maps.
     */
    private void removeNegative(Set redundantSet)
    {
        // First count stays as is.
        boolean notFirst = false;
        int negKeys =0;
        // Counting the number of topical facets.
        int facetCount = 0;
        Iterator doc_itr = mFacetDocMap.keySet().iterator();
        while(doc_itr.hasNext())
        {
            Integer count = (Integer) doc_itr.next();
            Map facetDocs = (HashMap) mFacetDocMap.get(count);
            Map facetArcs = (HashMap) mFacetArcMap.get(count);
            facetCount += facetDocs.keySet().size();
            // Intersection tells which facet keys to remove.
            Set remove = Algorithms.intersect(facetDocs.keySet(), redundantSet);
            if(!remove.isEmpty() && notFirst )
            {
                // Removes facets from a copy to avoid  
                // a ConcurrentModificationException.
                Map tmpFacetMap = new HashMap(facetDocs);
                Map tmpArcMap = new HashMap(facetArcs);
                // Eliminating the instances marked for removal.
                Iterator neg_itr = remove.iterator();
                while(neg_itr.hasNext())
                {
                    Integer negKey = (Integer) neg_itr.next();
                    tmpFacetMap.remove(negKey);
                    tmpArcMap.remove(negKey);
                    negKeys++;
                }
                // Saves pruned map if not completely emptied.
                if(tmpFacetMap.size() > 0)
                {
                    mFacetDocMap.put(count, tmpFacetMap);
                    mFacetArcMap.put(count, tmpArcMap);
                }
            }
            notFirst = true;
        }
        String removeString = "Removed " + negKeys + " topical facets from a "
                + "total of " + facetCount;
        if(negKeys == 0) removeString = "No topical facets removed";
        ApplicationManager.showText(removeString + ". Continuing with " 
                + (facetCount - negKeys) + " units." , 0);
    }
    
    /** setFacetViewer: A RetrievedFacetViewer is constructed when the 'Show 
     * Topical Facets'-checkBox was selected
     * in the parameter settings of the main GUI. It allows the user to view 
     * what facets are related to the query, to optionally eliminate some
     * facets (negative instances) or to simply abort the task.
     */
    private void setFacetViewer()
    {
        // Builds a GUI for the user to inspect the selected topical facets. 
        // The RetrievedFacetViewer program waits for the user to press a button 
        // in the RetrievedFacetViewer GUI before continuing.
        facetView = new RetrievedFacetViewer();
        facetView.makeView();
        // One of three possibile decisions are received from the 
        // RetrievedFacetViewer:
        // 1. The user decided to stop all retrieval tasks.
        if(mUserDecision == 2) mContinueTask = false;
        // 2. The user decided to remove the selected negative facet keys.
        else if(mUserDecision == 0)
        {
            if(!mNegativeKeys.isEmpty())
            {
                removeNegative(mNegativeKeys);
                mNegativeKeys.clear();
            }
        }
        // 3. The user wishes to continue without removing anything.
        // In all cases return to continue the program flow.
    }
    
    /** setAssign: the kind of action to perform for this task.
     * @param assign : flag (String) to indicate what kind of taks to perform.
     */
    private void setAssign(String assign)
    {
        try
        {
            mAssign = Integer.parseInt(assign);
        }
        catch (NumberFormatException nfe)
        {
            ApplicationManager.showText("Error: '" + assign 
                    + "' is not a valid assignment", 0);
            ApplicationManager.updateStatusBar(-1, "");
        }
    }
    
    /** setUserDecision: The RetrievedFacetViewer releases the 'wait'-thread
     * and transfers the user decision:
     * '0' = the 'Confirm'-button is pressed: processing continues. 
     * If negative facet instances have been selected they will be removed;
     * '1' = the 'Continue'-button is pressed: processing continues without 
     * removing anything;
     * '2' = the 'Abort'-button is pressed: all the retrieval tasks stop and 
     * the program ends.
     *
     * @param decision : user decision (int)
     */
    public static void setUserDecision(int decision)
    {
        mUserDecision = decision;
    }
    
    /** setNegativeKeySet: RetrievedFacetViewer forwards this set with negative
     * topical facets selected by the user.
     * @param negativeKeys : Set with topical facet keys (Integer) to be removed
     * from this retrieval task.
     */
    public static void setNegativeKeySet(Set negativeKeys)
    {
        mNegativeKeys = negativeKeys;
    }
    
    /** getTaskSet: returns the set with the task keys to the ApplicationManager.
     * @return a HashSet with one or more tasks (String) to perform.
     */
    public Set getTaskSet()
    {
        return mTaskSet;
    }
    
    /** getSeedArcCount: Returns the number of informative arcs from the seed
     * document in this task.
     * @return number of arcs (int).
     */
    public static int getSeedArcCount()
    {
        return mSeedArcSize;
    }
    
    /** getDocPathSet: Returns a set with the full path to all files needed to 
     * start DocCompare.
     * @return LinkedHashSet with all full paths (String) for a doc-by-doc 
     * similarity exercise.
     */
    public static LinkedHashSet getDocPathSet()
    {
        return mDocSimSet;
    }
    
    /** getDocArcLists: Returns a list of lists with the doc-ids and arcs used 
     * by the FinalViewer class.
     * @return ArrayList with ArrayLists with a doc-id (Integer) and a LinkedList
     * with arc-keys (String).
     */
    public static List getDocArcLists()
    {
        return mDocArcList;
    }
    
    /** getDocIdSet: Returns a set with the doc-ids used by the
     * ApplicationManager and DocAnalysis.
     * @return SortedSet with doc-ids (Integer).
     */
    public static SortedSet getDocIdSet()
    {
        return mDocIdSet;
    }
    
    /** getDocFacets: Returns a map with a set of topical facets for every 
     * document involved in this task.
     * @return SortedMap with docId (Integer) as key and a SortedSet of facet
     * keys (Integer) linked to that map.
     */
    public static SortedMap getDocFacets()
    {
        return mDocFacetMap;
    }
    
    /** getFacetDocMap: Returns a LinkedHashMap with documents linked to the 
     * topical facets per count class.
     * @return LinkedHashMap with as key the count class (sorted from high to low) 
     * and as value a HashMap with the global facet key (Integer) and a SortedSet
     * with all document-ids (Integer) related to this task as value.
     */
    public static LinkedHashMap getFacetDocMap()
    {
        return mFacetDocMap;
    }
    
    /** getTokenFacetMap: Map with a set of topical facets for every token from 
     * the seed document (query).
     * @return SortedMap with tokenId (Integer) from the query as key and as 
     * value a SortedSet of shared global facet keys (Integer) related to this 
     * token and active during the scope of this session.
     */
    public static SortedMap getTokenFacetMap()
    {
        return mTokenFacetMap;
    }
    
    /** getFacetArcMap: Returns a map with the arcs that defined a topical facet
     * related to a query token.
     * @return SortedMap with with as key the count class (sorted from high to
     * low) and as value a HashMap with the global facet key (Integer) as key 
     * and a LinkedHashSet as value with arc-keys (String) that defined the
     * topical facet.
     */
    public static SortedMap getFacetArcMap()
    {
        return mFacetArcMap;
    }
    
    /** getDocTable: Returns a table with information on documents such as 
     * sets with all the facets related to a document.
     * @return instance of the DocTable class.
     */
    public static DocTable getDocTable()
    {
        return mDocTable;
    }
    
    /** setSeedTokens : keeps only the informative tokens from the query. 
     * The total informative value of the query is collected.
     * @param ia an ArcsTable with data about informative arcs.
     */
    public void setSeedTokens(ArcsTable ia)
    {
        mInformativeTokens = new HashSet();
        mQueryValue = 0.0;
        double tokenWeight;
        Iterator token_itr = mTokenSet.iterator();
        while(token_itr.hasNext())
        {
            Integer tokenId = (Integer) token_itr.next();
            // The informative value is retrieved from a similar vertex known 
            // by the application.
            tokenWeight = ia.getVertexValue(tokenId, 0);
            if(tokenWeight > 0.0) 
            {
                mInformativeTokens.add(tokenId);
                setQueryValue(tokenWeight);          
            }
        }
    }
    
    /** getSeedTokens: Returns a set with the informative token-ids from the 
     * seed document now being processed.
     * @return HashSet with token-ids (Integer).
     * todo save this set with the topic facets from the main core 
     */
     //Clusters.getMainFacetCore as part of a Q&A application
  
    public static Set getSeedTokens()
    {
        return mInformativeTokens;
    }
    
    /** getQueryValue: returns the informative value of  a query.
     * @return double value of the query tokens.
     */
    public static double getQueryValue()
    {
        return mQueryValue;
    }

    /** setQueryValue: accumulates the informative value of all tokens in a query.
     * @param queryValue a double with the value of one informative token from 
     * the query
     */
    private void setQueryValue(double queryValue)
    {
        mQueryValue += queryValue;
    }
    
    /** getTaskName: Returns the description of the retrieval task now being
     * processed.
     * @return String with full task description.
     */
    public static String getTaskName()
    {
        return mTask;
    }
    
    /** getContinue: Returns the result of the different operations in this class.
     * There is no need to continue this task when essential maps or sets are 
     * empty or when the user has pressed the 'Abort' button.
     * @return boolean mContinueTask = 'true' if the next step of the retrieval
     * task can be taken or 'false' when all activity has to stop.
     */
    public boolean getContinue()
    {
        return mContinueTask;
    }
    
    /** clearFacetMaps: removes all elements from these maps.
     */
    public void clearFacetMaps()
    {
        mTokenFacetMap = null;
        mFacetDocMap = null;
    }
    
    /** clearOtherCollections: removes all mappings from these collections.
     */
    public void clearOtherCollections()
    {
        mTokenSet = null;
        mFacetArcMap = null;
        mDocFacetMap = null;
        mDocSimSet = null;
        mDocIdSet = null;
    }
    
}
