package ericvh.TDT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class DocRetrieval prepares a set of documents related to the query to 
 * be retrieved.
 * @author Eric Van Horenbeeck
 * Created on 7 juli 2005, 15:36
 * Revision: June 18, 2007
 */
public class DocRetrieval
{
    // Four frequency categories for documents and an array to hold them.
    private static final int HIGHEST = 0;
    private static final int HIGHUP = 1;
    private static final int MIDUP = 2;
    private static final int LOWUP = 3;
    private static List[] mBasketGroup;
    
    /** Constructor
     */
    public DocRetrieval()
    {
        groupDocs();
    }
    
    /** groupDocs: Documents are grouped in four baskets. The first contains
     * documents having the highest common facet count; then a second basket 
     * with the next highest counts including the high count;
     * then a basket from the middle upwards and finally a basket covering 
     * documents from the low common facet count up to the highest. 
     * The docClass array has an array with lists of doc-ids and arcs in each 
     * category. As the tokens of this task were all extracted from the 
     * same document a high to low relation from this document to other documents
     * can be made using the topical facets. Topical facets link documents to 
     * each other based on semantic similarity.
     * Uses facetDocMap, a LinkedHashMap with as key the count class (sorted 
     * from high to low) and as value a HashMap with the global facet key and 
     * as value a SortedSet with all document-ids (Integer) related to this task.
     */
    private void groupDocs()
    {
        // Map with doc-ids and a set with common facets.
        LinkedHashMap facetDocMap = TopicRetriever.getFacetDocMap();
        // Determines the class boundaries based on the count of shared topical
        // facets.
        LinkedList countList = new LinkedList(facetDocMap.keySet());
        int[] classBounds = calculateBoundaries(countList);
        // Prepares four baskets to hold documents and arcs based on the number 
        // of common facets and one array to hold them.
        List highestBasket = new ArrayList();
        List highUpBasket = new ArrayList();
        List middleUpBasket = new ArrayList();
        List lowUpBasket = new ArrayList();
        mBasketGroup = new ArrayList[4];
        // docMap accumulates all docIds and facet keys from the top down.
        Map docMap = new HashMap();
        int groupCounter = 0;
        // Puts documents and arcs in a basket based on the number of shared 
        // facets.
        Iterator count_itr = facetDocMap.keySet().iterator();
        while(count_itr.hasNext())
        {
            Integer count = (Integer) count_itr.next();
            int facetClass = count;
            // HashMap with for every facetKey a SortedSet of docIds.
            Map facetMap;
            Map tmpMap;
            // Distributes docs and arcs according to their class boundaries.
            if(facetClass >= classBounds[groupCounter]) 
                facetClass = groupCounter;
            else if(groupCounter < 3) groupCounter++;
            switch(groupCounter)
            {
                case 0 -> {
                    facetMap = (HashMap) facetDocMap.get(count);
                    tmpMap = convert(facetMap);
                    docMap.putAll(combine(docMap, tmpMap));
                    highestBasket = fillBasket(docMap);
                }
                case 1 -> {
                    facetMap = (HashMap) facetDocMap.get(count);
                    tmpMap = convert(facetMap);
                    docMap.putAll(combine(docMap, tmpMap));
                    highUpBasket = fillBasket(docMap);
                }
                case 2 -> {
                    facetMap = (HashMap) facetDocMap.get(count);
                    tmpMap = convert(facetMap);
                    docMap.putAll(combine(docMap, tmpMap));
                    middleUpBasket = fillBasket(docMap);
                }
                case 3 -> {
                    facetMap = (HashMap) facetDocMap.get(count);
                    tmpMap = convert(facetMap);
                    docMap.putAll(combine(docMap, tmpMap));
                    lowUpBasket = fillBasket(docMap);
                }
                default -> {
                }
            }
            // The docs and arcs with the highest shared count.
            // High count upwards (includes Highest count).
            // Middle count upwards (includes Case 1).
            // Low count upwards (includes Case 2 ).
                    }
        // Putting all the baskets in one array.
        mBasketGroup[0] = highestBasket;
        mBasketGroup[1] = highUpBasket;
        mBasketGroup[2] = middleUpBasket;
        mBasketGroup[3] = lowUpBasket;
    }
    
    /** fillBasket: A basket (list) is filled with docIds and arcs.
     * @param docMap : HashMap with docId (Integer) as key and a SortedSet of 
     * facetKeys (Integer) as value.
     * @return ArrayList with an ArrayList for every docId(Integer) at index '0'
     * and a LinkedHashSet of common arc keys (String) at index '1'.
     */
    private List fillBasket(Map docMap)
    {
        List basket = new ArrayList(docMap.size());
        if(!docMap.isEmpty())
        {
            Iterator doc_itr = docMap.keySet().iterator();
            while(doc_itr.hasNext())
            {
                Integer docId = (Integer) doc_itr.next();
                SortedSet facets = (TreeSet) docMap.get(docId);
                basket.add(getSharedArcs(docId, facets));
            }
        }
        return basket;
    }
    
    /** convert: Returns a map where the docId is key and a set of facet keys 
     * relating to that doc is the value.
     * @param facetMap : HashMap with the facetKey (Integer) and a SortedSet 
     * of docIds (Integer).
     * @return a HashMap with the docId (Integer) as key and a SortedSet of 
     * facet keys (Integer) as value.
     */
    private Map convert(Map facetMap)
    {
        Map docMap = new HashMap();
        Iterator facet_itr = facetMap.keySet().iterator();
        while(facet_itr.hasNext())
        {
            Integer facetKey = (Integer) facet_itr.next();
            SortedSet docSet = (TreeSet) facetMap.get(facetKey);
            if(!docSet.isEmpty())
            {
                Iterator doc_itr = docSet.iterator();
                while(doc_itr.hasNext())
                {
                    Integer docId = (Integer) doc_itr.next();
                    SortedSet facetSet = new TreeSet();
                    if(docMap.containsKey(docId)) facetSet.addAll((TreeSet) 
                            docMap.get(docId));
                    facetSet.add(facetKey);
                    docMap.put(docId, facetSet);
                }
            }
        }
        return docMap;
    }
    
    /** combine: Merges two maps.
     * @param docMap : HashMap with docId (Integer) as key and as value a 
     * SortedSet of facet keys (Integer). This map accumulates all docs.
     * @param tmpMap : HashMap with same structure as docMap.
     * @return HashMap with docIds (Integer) and as value a SortedSet of 
     * facet keys (Integer) from
     * both input maps.
     */
    private Map combine(Map docMap, Map tmpMap)
    {
        Map thisDocMap = new HashMap(docMap);
        Iterator tmp_itr = tmpMap.keySet().iterator();
        while(tmp_itr.hasNext())
        {
            Integer doc = (Integer) tmp_itr.next();
            SortedSet tmpSet = (TreeSet) tmpMap.get(doc);
            if(thisDocMap.containsKey(doc)) tmpSet.addAll((TreeSet) 
                    thisDocMap.get(doc));
            thisDocMap.put(doc, tmpSet);
        }
        return thisDocMap;
    }
    
    /** getSharedArcs: Collects the arc keys that define the relation between 
     * this doc and the query. Uses facetArcMap, a SortedMap with with as key 
     * the count class (sorted from high to low) and as value a HashMap with 
     * the global facet key (Integer) as key and a LinkedHashSet as value with
     * arc-keys (String) that defined the topical facet.
     * @param docId : the identification of the document in view (Integer).
     * @param facetSet : SortedSet with global facet keys (Integer) from one 
     * document.
     * @return ArrayList with the docId (Integer) and a LinkedHashSet of common 
     * arc keys (String).
     */
    private List getSharedArcs(Integer docId, SortedSet facetSet)
    {
        // Map with facet key and arcs.
        LinkedHashMap facetArcMap = new LinkedHashMap(TopicRetriever
                .getFacetArcMap());
        List docArcs = new ArrayList(2);
        LinkedList commonArcs = new LinkedList();
        SortedSet thisArcSet;
        Iterator facet_itr = facetSet.iterator();
        while(facet_itr.hasNext())
        {
            Integer facetKey = (Integer) facet_itr.next();
            Iterator arc_itr = facetArcMap.values().iterator();
            while(arc_itr.hasNext())
            {
                Map arcMap = (HashMap) arc_itr.next();
                if(arcMap.containsKey(facetKey))
                {
                    thisArcSet = (TreeSet) arcMap.get(facetKey);
                    commonArcs.addAll(thisArcSet);
                }
            }
        }
        docArcs.add(docId);
        docArcs.add(commonArcs);
        return docArcs;
    }
    
    /** calculateBoundaries: Prepares an array with four classes based on the
     * number of count groups. The maximum number of count classes to distribute
     * is 10. Each cell of the array contains the lower bound of its class. 
     * Data are added as long as the lower bound of the token-facet count is not
     * reached.
     * @param countList : LinkedList with the count classes from high to low 
     * (Integer)
     * @return Array (int) with four class boundaries.
     */
    private int[] calculateBoundaries(LinkedList countList)
    {
        int maxClass = 10;
        int highest = ((Integer) countList.getFirst());
        int smallest = ((Integer) countList.getLast());
        if(countList.size() > maxClass) smallest = ((Integer) countList.
                get(maxClass));
        int interval = StrictMath.max((highest - smallest) / 4, 1);
        // Makes 4 classes between high and low facet count to classify 
        // the documents.
        int[] classArray = new int[4];
        for(int i = 0; i < 4 ; i++)
        {
            classArray[i] = highest - ((1 + i) * interval);
        }
        return classArray;
    }
    
    /** getDocGroup: Returns a list of documents based on a common facet 
     * frequency (high...low). Called by the ApplicationManager to start 
     * the doc-by-doc similarity with a group of selected documents.
     * @param docGroup : the frequency identification (int) of a document set.
     * @return ArrayList with ArrayLists with the doc-id (Integer) and a
     * LinkedList of arc-keys (String)).
     */
    public List getDocGroup(int docGroup)
    {
        List docSet = new ArrayList(1);
        String group = "";
        int docSetSize;
        switch(docGroup)
        {
            case HIGHEST -> 
            {
                group = "HIGHEST"; docSet = mBasketGroup[0];
            }
            case HIGHUP -> 
            {
                group = "HIGHUP"; docSet = mBasketGroup[1];
            }
            case MIDUP -> 
            {
                group = "MIDUP"; docSet = mBasketGroup[2];
            }
            case LOWUP -> 
            {
                group = "LOWUP";  docSet = mBasketGroup[3];
            }
            default -> 
            {
            }
        }
        // Delivers another basket if the requested one is empty.
        docSetSize = docSet.size();
        if(docSetSize < 1 )
        {
            docSet = mBasketGroup[--docGroup];
            docSetSize = docSet.size();
            if(docSetSize < 1 )
            {
                docSet = mBasketGroup[--docGroup];
                docSetSize = docSet.size();
                if(docSetSize < 1)
                {
                    docSet = mBasketGroup[--docGroup];
                }
            }
            switch(docGroup)
            {
                case HIGHEST -> ApplicationManager.showText("Document selection"
                        + " adapted: " + group +  " demanded," +
                        " HIGHEST retrieved" , 0);
                case HIGHUP -> ApplicationManager.showText("Document selection"
                        + " adapted: " + group +  " demanded," +
                        "HIGHUP retrieved" , 0);
                case MIDUP -> ApplicationManager.showText("Document selection "
                        + "adapted: " + group +  " demanded," +
                        " MIDUP retrieved" , 0);
                case LOWUP -> ApplicationManager.showText("Document selection"
                        + " adapted: " + group +  " demanded," +
                        "LOWUP retrieved" , 0);
                default -> {
                }
            }
        }
        return docSet;
    }
    
}
