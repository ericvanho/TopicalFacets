package ericvh.TDT;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/** Class FacetCollector collects all topical facet maps between a start- and 
 * an enddate set by the user. One by one the topicMaps are read and nine 
 * meta-tables are made with a set of topical facets of every token-type known 
 * by the system based on the first digit of the vertex-id. The facet dictionary
 * has the unique file name to allow fast retrieval of the orginal from disk.
 * The DocTable will remember all topical facets facet found in its documents. 
 * By looking up the facet key in the facet dictionary it is possible to retrieve
 * the relevant documents. This is handled by the TopicRetriever class.
 * @author Eric Van Horenbeeck
 * Created on 16 june 2005, 22:00
 */
public class FacetCollector implements Serializable
{
    // Map to collect the individual topicMaps made by the TopicMaker.
    private LinkedHashMap mLocalTopics;
    // Map with all docs-ids of a scope and the set of vertices used in those 
    // documents.
    private Map mDocVertices;
    // Set to assemble all informative vertices relevant for this session.
    private final Set mInfoVertexSet;
    // List with nine local maps with vertex-facet combinations.
    private static List mCollectArray;
    // Map with a global facet-key as key and as value a list with the local 
    // topic-keys and the full path to the original topicMap.
    private SortedMap mAllFacetKeys;
    // Table with information on documents to add the final informative value.
    private final DocTable mDocTable;
    // The last global key attributed in this or in a previous session.
    private int mLastGlobalKey;
    // LinkedList with doc-ids that were successfully rescanned and have a 
    // different scope.
    private LinkedList mRescannedList;
    // Set with scopes treated by the FacetCollector now and in previous sessions.
    private final SortedSet mScopeSet;
    // The scope of the files being treaed.
    private String mScope;
    // Boolean flag to indicate if an map is introduced with a scope that was 
    // previously treated.
    private boolean mScopeExists;
    // Reading and writing methods.
    private InputOutput io = new InputOutput();
    private final String separator = System.getProperty("file.separator");
    
    /** Constructor
     */
    public FacetCollector()
    {
        mAllFacetKeys = new TreeMap();
        mLocalTopics = new LinkedHashMap();
        mDocVertices = new HashMap();
        mInfoVertexSet = new HashSet();
        mScopeSet = new TreeSet();
        mRescannedList = new LinkedList();
        mLastGlobalKey = -1;
        mScopeExists = false;
        mDocTable = ApplicationManager.getDocTable();
        prepareCollectMaps();
    }
    
    /** prepareCollectMaps: The vertex-facet combinations collected in this 
     * session are kept in nine maps identified by the first digit of the vertex
     * key. Keeping all the vertex-facet sets in one file would yield a 
     * voluminous file that is hard to load in the system memory.
     */
    private void prepareCollectMaps()
    {
        mCollectArray = new ArrayList();
        SortedMap sm;
        for (int i = 0; i < 9; i++)
        {
            sm = new TreeMap();
            mCollectArray.add(sm);         
        }
    }
    
    /** prepareFacetKeyList: ApplicationManager sends the facet keys that were 
     * attributed in a previous session. The last key from the first file is put
     * in a temporary list to allow the numbering of new data.
     * @param allFacetMap : SortedMap has a global facet key (Integer) as key 
     * and an ArrayList as value with the local facet key (Integer) and the
     * path (String) to the local facet map.
     */
    public void prepareFacetKeyList(SortedMap allFacetMap)
    {
        mAllFacetKeys = allFacetMap;
        Iterator key_itr = mAllFacetKeys.keySet().iterator();
        LinkedList keyList = Algorithms.iteratorToLinkedList(key_itr);
        if(!keyList.isEmpty()) mLastGlobalKey = ((Integer) keyList.getLast());
    }
    
    /** addScopeSet: ApplicationManager sends this set with all the scope-dates
     * that have been collected in the global topic overview. The set is used to
     * check whether certain files were already treated.
     * @param scopeSet : SortedSet with scopes (String).
     */
    public void addScopeSet(SortedSet scopeSet)
    {
        mScopeSet.addAll(scopeSet);
    }
    
    /** setScope: ApplicationManager sets the scope of the files now being 
     * treated and checks if the scope in view was already seen in a previous 
     * session by comparing the actual scopes with the scope dates in the 
     * scopeSet. A boolean flag is set to 'true' when such is the case.
     * @param scope : the begin- and enddate of these topic files (String).
     */
    public void setScope(String scope)
    {
        mScope = scope;
        mScopeExists = mScopeSet.contains(mScope);
    }
    
    /** addRescannedDocs: ApplicationManager delivers this list with rescanned 
     * doc-ids that have data scopes different from the scope being treated now.
     * @param rescannedDocs : a LinkedList with doc-ids (Integer) from old files
     * that were rescanned during this scope.
     */
    public void addRescannedDocs(LinkedList rescannedDocs)
    {
        mRescannedList = rescannedDocs;
    }
    
    /** addTopicMap: Local topicMaps are added by the ApplicationManager one by
     * one. Each local topic facet key gets a global identification number while
     * remembering its local key and its local path.
     * @param topicMap : LinkedHashMap with topic key (Integer) and a SortedSet
     * of document-ids as value (Integer).
     * @param facetPath : String with path to the topical facet file.
     */
    public void addTopicMap(LinkedHashMap topicMap, String facetPath)
    {
        mLocalTopics = topicMap;
        allocateFacetKeys(facetPath);
    }
    
    /** addVertices: Retrieves from the VerticesTable a HashMap with all 
     * docs-ids (Integer) as key and a set (HashSet) of all vertices (Integer)
     * used in that document.
     * @param vt : a VerticesTable for the scope now in view.
     */
    public void addVertices(VerticesTable vt)
    {
        mDocVertices = vt.getDocsWithVertices();
    }
    
    /** addInfoVertices: ApplicationManager delivers a map with informative 
     * vertices from every scope in view. They are collected in a global vertex 
     * set. Every informative vertex will get a pointer to all the topical facets
     * it is involved in.
     * @param infoVrtx : the HashMap with informative vertices for the scope now
     * in view. Key: a collection key (String), value: HashMap with the vertex-id
     * (Integer) as key and the vertex info-value (double).
     */
    public void addInfoVertices(Map infoVrtx)
    {
        mInfoVertexSet.addAll(extractInfoVertices(infoVrtx));
    }
    
    /** extractInfoVertices: Extracts a set with only the informative vertices 
     * from the infoVertices map.
     * @param infoVrtx : the HashMap with informative vertices. 
     * Key: collection key (String), value: HashMap with vertex-id (Integer) 
     * as key and vertex info-value (double).
     * @return HashSet with the informative vertex-ids (Integer) extracted.
     */
    private Set extractInfoVertices(Map infoVrtx)
    {
        Set infoSet = new HashSet();
        Iterator info_itr = infoVrtx.values().iterator();
        while(info_itr.hasNext())
        {
            Map infoMap = (HashMap) info_itr.next();
            infoSet.addAll(infoMap.keySet());
        }
        return infoSet;
    }
    
    /** getGlobalKey: Returns a global facet-key from the facet dictionary. 
     * This dictionary makes a link between the global key and the local topic
     * key with its file path from one day-to-day scope.
     * @param localKey : the local topic key (Integer).
     * @return the global facet-key (Integer) or 'null' when none is found.
     */
    private Integer getGlobalKey(Integer localKey)
    {
        Iterator facet_itr = mAllFacetKeys.keySet().iterator();
        while(facet_itr.hasNext())
        {
            Integer globalKey = (Integer) facet_itr.next();
            List detailList = (ArrayList) mAllFacetKeys.get(globalKey);
            if(detailList.get(0) == localKey) 
                return globalKey;
        }
        return null;
    }
    
    /** makeFacetMap: This method makes a SortedMap with a vertex-id as key
     * (Integer) and a HashSet with topical facet keys (Integer) as value. 
     * The local topical facet key is replaced by a global facet key that allows
     * the retrieval of all doc-id sets relevant to this token-type. 
     * Only informative tokens are remembered. All the facet keys that relate to
     * the same document are transferred to the DocTable. 
     * The facet keys may point to documents that were successfully rescanned but
     * have a different scope date. In that case additional vertices and 
     * info-vertices from the correct scope are added.
     * @throws IOException
     */
    public void makeFacetMap() throws IOException
    {
        // Prepares a list with nine temporary SortedMaps to hold the data from
        // this set of docs.
        List tmpArray = new ArrayList();
        SortedMap sm;
        for (int i = 0; i < 9; i++)
        {
            sm = new TreeMap();
            tmpArray.add(sm);
        }
        // Iterates over the local topicMap.
        Iterator top_itr = mLocalTopics.keySet().iterator();
        while(top_itr.hasNext())
        {
            // Gets a global facet key to point to the local topical facet key. 
            // Global keys were set or retrieved for all local keys as a 
            // preliminary step.
            Integer localFacetKey = (Integer) top_itr.next();
            Integer globalFacetKey = getGlobalKey(localFacetKey);
            // Iterates over the documents linked to this topical facet to 
            // collect all facets for every informative vertex in that text.
            SortedSet docSet = (TreeSet) mLocalTopics.get(localFacetKey);
            // Checks the scope. When a document has a different scope its 
            // vertices are added separately.
            controlDocSet(docSet);
            Iterator doc_itr = docSet.iterator();
            while(doc_itr.hasNext())
            {
                Integer docId = (Integer) doc_itr.next();
                // The DocTable is asked to remember the global facet keys 
                // found in its documents.
                mDocTable.addTopicFacet(docId, globalFacetKey);
                // Iterates over the vertices from this document.
                Set localVertices = (HashSet) mDocVertices.get(docId);
                Iterator vertex_itr = localVertices.iterator();
                while(vertex_itr.hasNext())
                {
                    Integer vrtxId = (Integer) vertex_itr.next();
                    // Only when this vertex is informative it is added to 
                    // the VertexFacetMap.
                    if(mInfoVertexSet.contains(vrtxId))
                    {
                        // The vertex-id and a set of topical facet keys are 
                        // added in a map identified  by the first digit of the
                        // vertex key. All the different topic keys for that 
                        // vertex are collected here.
                        String vrtxKey = vrtxId.toString();
                        int index = Integer.parseInt(vrtxKey.substring(0, 1)) - 1;
                        SortedMap tmpFacetMap = (TreeMap) tmpArray.get(index);
                        tmpArray.remove(index);
                        Set keySet = new HashSet();
                        if(tmpFacetMap.containsKey(vrtxId))
                            keySet.addAll((HashSet) tmpFacetMap.get(vrtxId));
                        keySet.add(globalFacetKey);
                        tmpFacetMap.put(vrtxId, keySet);                
                        tmpArray.add(index, tmpFacetMap);               
                    }
                }
            }
        }
        // The local map is combined with the corresponding collecting map 
        // holding data from all the local maps from this session. At the end 
        // of the session they will in turn be combined with the global data maps.
        for(int i = 0; i < 9; i++)
        {
            SortedMap sessionMap = (TreeMap) tmpArray.get(i);
            SortedMap globalMap = (TreeMap) mCollectArray.get(i);
            SortedMap newGlobalMap = combine(sessionMap, globalMap);
            mCollectArray.remove(i);
            mCollectArray.add(i, newGlobalMap);
        }
        // Finalizes this session by adding the scope now in view to the scopeSet.
        mScopeSet.add(mScope);
    }
    
    /** combine: Combines a topic-facet and vertices map with a global facet map.
     * @param facetMap : SortedMap holding all vertices between the dates set
     * by the user. Key: a vertex-id (Integer); value: HashSet with all 
     * keys (Integer) to the topic-facets where this token-type (represented by 
     * its vertex-id) was seen.
     * @param collectingMap : SortedMap to collect the local map, it has the
     * same structure as the facetMap.
     * @return collectingMap updated.
     */
    public static SortedMap combine(SortedMap facetMap, SortedMap collectingMap)
    {
        // The split-method performs an intersection and generates a list with 
        // two sets: one with common elements at index '0' and one with elements
        // not shared by both intersecting sets at index '1'.
        Set localKeySet = facetMap.keySet();
        List splitSets = Algorithms.split(localKeySet, collectingMap.keySet());
        SortedSet commonVertices = (TreeSet) splitSets.get(0);
        SortedSet diffVertices = (TreeSet) splitSets.get(1);
        // Iterates first over the common vertices to merge the information of 
        // the local map with the collecting map.
        Iterator common_itr = commonVertices.iterator();
        while(common_itr.hasNext())
        {
            Integer vertexID = (Integer) common_itr.next();
            Set localFacetSet = (HashSet) facetMap.get(vertexID);
            Set generalFacetSet = (HashSet) collectingMap.get(vertexID);
            generalFacetSet.addAll(localFacetSet);
            collectingMap.put(vertexID, generalFacetSet);
        }
        // Iterates next over the remaining vertices that are new to the 
        // collecting map and that can be added as such.
        Iterator diff_itr = diffVertices.iterator();
        while(diff_itr.hasNext())
        {
            Integer vertexID = (Integer) diff_itr.next();
            // Makes sure that this vertex belongs to the local set.
            if(localKeySet.contains(vertexID))
            {
                Set localFacetSet = (HashSet) facetMap.get(vertexID);
                collectingMap.put(vertexID, localFacetSet);
            }
        }
        return collectingMap;
    }
    
    /** getProcessed: Returns the total size of this session's maps.
     * @return how many elements have been processed so far (int).
     */
    public int getProcessed()
    {
        return countVertexKeys();
    }
    
    /** updateVertexFacetMaps: When all the local maps are processed, the nine 
     * different collecting maps from this session are merged with their 
     * corresponding global vertex-facet maps.
     * @param globalMap : SortedMap with vertex-id (Integer) as key and a 
     * SortedSet with global facet-keys (Integer).
     * @param index : identification of the collecting map.
     * @return SortedMap with the same structure as the globalMap updated with
     * the data from this session.
     */
    public static SortedMap updateVertexFacetMaps(SortedMap globalMap, int index)
    {
        SortedMap sessionMap = (TreeMap) mCollectArray.get(index);
        return combine(sessionMap, globalMap);
    }
    
    /** allocateFacetKeys: Iterates over a local facet map and allocates 
     * a global facet key. The original local key and the path to the original 
     * file are saved in a list. Everything is then put in a map.
     * @param facetPath : String with path to the topical facet file.
     */
    private void allocateFacetKeys(String facetPath)
    {
        Iterator facet_itr = mLocalTopics.keySet().iterator();
        while(facet_itr.hasNext())
        {
            Integer localFacetKey = (Integer) facet_itr.next();
            List localFacetList = new ArrayList();
            localFacetList.add(localFacetKey);
            localFacetList.add(facetPath);
            // A check is made to see if this file was entered earlier.
            Integer globalKey = getNextKey(localFacetKey, facetPath);
            mAllFacetKeys.put(globalKey, localFacetList);
        }
    }
    
    /** getNextKey: Returns the next number to be attributed as a global 
     * facet key. To avoid duplicate data an input a check is performed when 
     * maps from a previous session are introduced.
     * @param localKey : the local key (Integer).
     * @param facetPath : the path to the local topic map (String).
     * @return the next available global key (Integer) for a topical facet 
     * or '0' for the first element when key list is empty.
     */
    private Integer getNextKey(Integer localKey, String facetPath)
    {
        int newGlobalKey = -1;
        if(mScopeExists) newGlobalKey = getIfKeyExists(localKey, facetPath);
        if(newGlobalKey == -1)
        {
            newGlobalKey = mLastGlobalKey + 1;
            // Remembers the most recent key.
            mLastGlobalKey = newGlobalKey;
        }
        return newGlobalKey;
    }
    
    /** getIfKeyExists: If an existing topic map is used duplicate information
     * could be entered. To avoid this the local file path is compared to other 
     * paths already registered. If the same file is found then the existing 
     * global key is returned, if not a new key will be attributed.
     * @param localKey : the local key (Integer).
     * @param facetPath : the path to the local topic map (String).
     * @return the global key (int) if found or '-1' if not.
     */
    private int getIfKeyExists(Integer localKey, String facetPath)
    {
        int globalKeyFound = -1;
        Iterator key_itr = mAllFacetKeys.keySet().iterator();
        while(key_itr.hasNext())
        {
            Integer key = (Integer) key_itr.next();
            List topicList = (ArrayList) mAllFacetKeys.get(key);
            Integer oldLocalKey = (Integer) topicList.get(0);
            String thisOldPath = topicList.get(1).toString();
            if(thisOldPath.equals(facetPath) && oldLocalKey.equals(localKey))
            {
                globalKeyFound = key;
                break;
            }
        }
        return globalKeyFound;
    }
    
    /** controlDocSet: Controls if this doc set contains doc-ids with a scope 
     * different from the main group. These doc-ids belong to rescanned files. 
     * When found the correct vertices and info-vertices are imported and 
     * added to the relevant collections.
     */
    private void controlDocSet(SortedSet docSet) throws IOException
    {
        List docList = new ArrayList();
        Map rescannedMap = new HashMap();
        Calendar fileDate = Calendar.getInstance();
        fileDate.set(0000, 00, 00);
        Iterator doc_itr = docSet.iterator();
        while(doc_itr.hasNext())
        {
            int docNr = ((Integer) doc_itr.next());
            // Makes a temporary map with the special scope (special because 
            // it's out-of-sync with the scope in view) as key and a list of 
            // doc-ids with vertices that should be added to the vertices now 
            // being handled.
            if(mRescannedList.contains(docNr))
            {
                docList.add(docNr);
                String fileScope;
                String filePath = mDocTable.getFilename(docNr) + ".";
                if(filePath.substring(0,8).equals("AllDates")) 
                    fileScope = "AllDates";
                else
                {
                   fileScope = GraphTime.constructFileScope(fileDate, filePath);
                }
                if(rescannedMap.containsKey(fileScope))
                    docList.addAll((ArrayList) rescannedMap.get(fileScope));
                docList.add(docNr);
                rescannedMap.put(fileScope, docList);
            }
        }
        Iterator path_itr = rescannedMap.keySet().iterator();
        while(path_itr.hasNext())
        {
            String fileScope = path_itr.next().toString();
            List docIdList = (ArrayList) rescannedMap.get(fileScope);
            expandVertices(fileScope, docList);
        }
    }
    
    /** expandVertices: For every document with a scope different from the 
     * main scope the vertices are extracted from the old files and added to
     * the active map. Returns the vertices that were found to allow the 
     * extraction of any related informative vertices.
     * @param fileScope : scope of the old 'Vertices' map (String).
     * @param docIdList : ArrayList with the doc-ids (int) to be extracted.
     * @throws IOException
     */
    private void expandVertices(String fileScope, List docIdList) 
            throws IOException
    {
        String vertexPath = ApplicationManager.getWorkPath() + separator 
                + fileScope + "_Vertices.vrtx";
        String infoPath = ApplicationManager.getWorkPath() + separator 
                + fileScope + "_InfoTok.info";
        try
        {
            // Reads the info vertices map and extracts a set with all 
            // informative vertices.
            Map infoVertexMap = io.readMap(infoPath);
            Set infoSet = extractInfoVertices(infoVertexMap);
            // Reads the vertices table and extracts a map with all doc-ids 
            // and all their vertices.
            Map verticesMap = io.readVTable(vertexPath).getDocsWithVertices();
            Iterator map_itr = verticesMap.keySet().iterator();
            while(map_itr.hasNext())
            {
                int docNr = ((Integer) map_itr.next());
                // Puts all the vertices in the DocVertices map when a relevant
                // doc-id is found.
                // Adds the vertex-id to the InfoVertexSet if a vertex of the 
                // set is informative.
                if(docIdList.contains(docNr))
                {
                    Integer docInt = docNr;
                    Set vertexSet = (HashSet) verticesMap.get(docInt);
                    mDocVertices.put(docInt, vertexSet);
                    Iterator vrtx_itr = vertexSet.iterator();
                    while(vrtx_itr.hasNext())
                    {
                        Integer vrtxId = (Integer) vrtx_itr.next();
                        if(infoSet.contains(vrtxId)) mInfoVertexSet.add(vrtxId);
                    }
                }
            }
        }
        catch(Exception e)
        {
            ApplicationManager.showText("* Vertex extraction for scope: " 
                    + fileScope + " failed *", 0);
            e.printStackTrace(System.err);
        }
    }
    
    /** This Comparator sorts a map on set size from small to large. 
     * The Comparator is tuned so that it produces an ordering that is compatible
     * with 'equals'. It's done in a two-part comparison, where the first part
     * is the one that defines the ordering (compareTo) and where the second
     * part is an attribute (the hashCode) that uniquely identifies the objects
     * to avoid data loss when elements would compare to the same value.
     */
    public final Comparator setSize = (Comparator) (Object o1, Object o2) -> 
    {
        Integer set1 = ((TreeSet) o1).size();
        Integer set2 = ((TreeSet) o2).size();
        int size = set1.compareTo(set2);
        if (size != 0) return size;
        // Order them on their hashcode if the objects are of the same size.
        return (o1.hashCode() < o2.hashCode() ? -1 : (o1.hashCode()
                == o2.hashCode() ? 0 : 1 ));
    };
    
    /** getFacetDictionary: Returns all topic keys in a map with the global 
     * facet-key as key and a list with the local topic-key it points to and 
     * the full path to the original topicMap.
     * @return SortedMap with global facet key (Integer) as key and as value 
     * an ArrayList with the local topic key (Integer) at index '0' and the 
     * full path (String) to the original topicMap at index '1'.
     */
    public SortedMap getFacetDictionary()
    {
        return mAllFacetKeys;
    }

    /** getScopeSet: Returns a sorted set with all scopes treated so far by 
     * the FacetCollector.
     * @return SortedSet with the scopes (String).
     */
    public SortedSet getScopeSet()
    {
        return mScopeSet;
    }
    
    /** countVertexKeys: Counts the number of keys from the nine collecting maps.
     */
    private int countVertexKeys()
    {
        int keyCount = 0;
        Iterator array_itr = mCollectArray.iterator();
        while(array_itr.hasNext())
        {
            SortedMap sm = (TreeMap) array_itr.next();
            Set keys = sm.keySet();
            keyCount += keys.size();
        }  
        return keyCount;
    }
    
    /** clearFiles: Clears the content of these local collections 
     * to reclaim memory.
     */
    public void clearFiles()
    {
        mLocalTopics.clear();
        mRescannedList.clear();
        mDocVertices.clear();
        mInfoVertexSet.clear();
    }
    
}