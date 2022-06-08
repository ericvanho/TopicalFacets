package ericvh.TDT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class UnipartiteCore prepares a unipartite graph from a doc - facet map. 
 * The main core is calculated
 * @author Eric  Van Horenbeeck
 * Created on 12 juni 2007, 13:14
 */

public class UnipartiteCore
{
    // The main core facet set.
    private Set mMainFacetCore;
    // The main core doc set.
    private Set mMainDocCore;
    // The number of cores allowed.
    private int mCoreNumber;
    
    /** Constructor
     */
    public UnipartiteCore()
    {    }
    
    /** setMainFacetCore: Finds the main core of a unipartite graph of topical 
     * facets, related to each other by the documents they link to.
     */
    public void setMainFacetCore()
    {
        // TopicRetriever returns a SortedMap with the docId as key and a SortedSet
        // of facet keys. The global facets shared by the main core documents 
        // are collected in a set.
        SortedMap docFacetMap = TopicRetriever.getDocFacets();
        // TopicRetriever returns a LinkedHashMap with as key the count class 
        // (sorted from high to low) and as value a HashMap with the global facet
        // key (Integer) and a SortedSet with all the document-ids (Integer)as value.
        LinkedHashMap tmpMap = TopicRetriever.getFacetDocMap();
        SortedMap facetDocMap =  new TreeMap();
        // Convert into a sortedMap without the count
        Iterator count_itr = tmpMap.keySet().iterator();
        while(count_itr.hasNext())
        {
            int count = ((Integer) count_itr.next());
            Map facetMap = (HashMap)tmpMap.get(count);
            Iterator facet_itr = facetMap.keySet().iterator();
            while(facet_itr.hasNext())
            {
                Integer facetKey = (Integer) facet_itr.next();
                SortedSet docSet = (TreeSet) facetMap.get(facetKey);
                facetDocMap.put(facetKey, docSet);
            }
        }
        // Converts facets into a unipartite graph. A map is made with for every
        // topical facet a set of related topical facets.
        Map unipartiteFacets = makeUnipartiteMap( facetDocMap, docFacetMap);
        // Extracts main core of the shared global topical facets.
        setMainFacetSet( findCores(unipartiteFacets));
    }
    
    /** setMainDocCore : Extracts main core  documents from map with document 
     * clusters.
     * @param docMap:  Map with for every document (Integer) a HashSet of 
     * adjacent documents (Integer).
     */
    public void setMainDocCore(Map docMap)
    {
        setMainDocSet( findCores(docMap));
    }
    
    /** makeUnipartiteMap:  Converts facet-doc map into a unipartite graph.
     * A map is made with for every topical facet a set of related facets. 
     * Related because the elements in the set link to the same document.
     * @param fMap : SortedMap with doc key (Integer) and a SortedSet of related 
     * facets (Integer) as value .
     * @param sMap : SortedMap with facet key (Integer) and a SortedSet of 
     * related docs (Integer) as value.
     * @return unipartiteMap : HashMap with Integer key and a HashSet (Integer) 
     * as value.
     */
    private Map makeUnipartiteMap(SortedMap fMap, SortedMap sMap)
    {
        Map unipartiteMap = new HashMap();
        Iterator fKey_itr = fMap.keySet().iterator();
        while(fKey_itr.hasNext())
        {
            Object firstKey = (Object) fKey_itr.next();
            SortedSet firstSet = (TreeSet) fMap.get(firstKey);
            Set finalSet = new HashSet();
            Iterator fSet_itr = firstSet.iterator();
            while(fSet_itr.hasNext())
            {
                Object firstId = (Object) fSet_itr.next();
                try
                {
                    SortedSet tmpSet = (TreeSet) sMap.get(firstId);
                    if(tmpSet.size() > 1)  finalSet.addAll(tmpSet);
                }
                catch(NullPointerException np)
                {
                    // no action necessary if key does not exist.
                }
            }
            // In order to avoid a self loop, the key is removed from the set.
            if(finalSet.contains(firstKey)) finalSet.remove(firstKey);
            unipartiteMap.put(firstKey, finalSet);
        }
        return unipartiteMap;
    }
    
    /** findCores: prepares a map with the core number for every relevant  
     * topical facet. The next method (extractCores) will gather all topical 
     * facets with the highest core number..
     * @param unipartiteMap : Map of the initial unipartite network with for 
     * every vertex ( topical facet) a set of adjacent elements.
     *  @return Set mainCoreSet.
     */
    private Set  findCores(Map unipartiteMap)
    {
        // Initializes list and maps.
        LinkedList vrtxList = new LinkedList();
        SortedMap positionMap = new TreeMap();
        SortedMap binMap = new TreeMap();
        SortedMap coreMap = new TreeMap();
        Map networkMap = unipartiteMap;
        int n = networkMap.keySet().size();
        Object vertex;
        int vDegree;
        int maxDegree = 0;
        int minDegree = n-1;
        // Puts the degree of every vertex in a coreMap. Finds the maximum 
        // and minimum degree of this set of vertices.
        Iterator item_itr = networkMap.keySet().iterator();
        while(item_itr.hasNext())
        {
            Object item = (Object) item_itr.next();
            Set neighbors = (HashSet) networkMap.get(item);
            vDegree = neighbors.size();
            coreMap.put(item, vDegree);
            if(vDegree > maxDegree) maxDegree = vDegree;
            if(vDegree < minDegree) minDegree = vDegree;
            // Initializes a LinkedList with enough room for every vertex in 
            // the network.
            vrtxList.add(0);
        }
        // Initializes a binMap with '0' as placeholder in every bin > 0.
        for(int i = minDegree; i < maxDegree + 1; i++)
        {
            binMap.put(i, 0);
        }
        // The bin key of the binMap is the degree of the vertex. BinSize is
        // the number of vertices with the same degree in the bin (value of the binMap).
        Iterator degree_itr = coreMap.keySet().iterator();
        while(degree_itr.hasNext())
        {
            vertex = (Object) degree_itr.next();
            int bin = ((Integer) coreMap.get(vertex));
            int binSize = ((Integer) binMap.get(bin));
            binSize++;
            binMap.put(bin, binSize);
        }
        // BinMap has for every bin the start position in the vrtxList of the 
        // first vertex. There may be several vertices with the same degree that
        // share the same bin. Bin '0' starts at position '0'. The other bins 
        // start at the next position being the previous start + the next binSize.
        int start = 0;
        Iterator bin_itr = binMap.keySet().iterator();
        while(bin_itr.hasNext())
        {
            int bin = ((Integer) bin_itr.next());
            int binSize = ((Integer) binMap.get(bin));
            binMap.put(bin, start);
            start = start + binSize;
        }
        // Puts the vertices sorted by their degrees in the vrxtList.
        degree_itr = coreMap.keySet().iterator();
        while(degree_itr.hasNext())
        {
            vertex = (Object) degree_itr.next();
            // Gets the degree of this vertex. This is the key to the binMap.
            int bin = ((Integer) coreMap.get(vertex));
            // BinMap returns the start position of the degree group (bin).
            int position = ((Integer) binMap.get(bin));
            // PositionMap remembers the position in the vrtxList of this vertex.
            positionMap.put(vertex, position);
            // The initial placeholder in the vrtxList is replaced with the 
            // real vertex-id.
            vrtxList.set(position, vertex);
            // Shifting one place to the right for the position of the next
            // vertex in the same degree group.
            position++;
            binMap.put(bin, position);
        }
        // Resets the starting positions in the binMap. To get the position index
        // for a new vertex in a given bin the start index was increased with one.
        // After putting all the vertices in the right bins, the actual start
        // position of a bin has become the start position of the next bin. 
        // To restore the correct positions, every bin has to get the position 
        // of the previous bin.
        int previousBin = 0;
        SortedMap tmpMap = new TreeMap(binMap);
        bin_itr = tmpMap.keySet().iterator();
        while(bin_itr.hasNext())
        {
            int bin = ((Integer) bin_itr.next());
            int prevPosition;
            if(previousBin == 0) prevPosition = 0;
            else prevPosition = ((Integer) tmpMap.get(previousBin));
            binMap.put(bin, prevPosition);
            previousBin = bin;
        }
        // This main iteration loops over the sorted vertices to calculate
        // their core value.
        for(int i = 0; i < n; i++)
        {
            vertex = (Object) vrtxList.get(i);
            // Degree of the vertex in focus.
            vDegree = ((Integer) coreMap.get(vertex));
            // Iterating over the neighbors of this doc.
            Set neighbors = (HashSet) networkMap.get(vertex);
            Iterator neighbor_itr = neighbors.iterator();
            while(neighbor_itr.hasNext())
            {
                Object adjacentV = (Object) neighbor_itr.next();
                // Degree of an adjacent vertrex.
                int adjDegree = ((Integer) coreMap.get(adjacentV));
                // If the adjacent vertex has a higher degree than the vertex
                // in focus this degree is decreased by 1 and the vrxtList 
                // is resorted.
                if(adjDegree > vDegree)
                {
                    // Position of this adjacent vertex in the vrtxList.
                    int adjPosition = ((Integer) positionMap.get(adjacentV));
                    // Start position of adjacent degree group (bin) in the vrtxList.
                    int adjBinPosition = ((Integer) binMap.get(adjDegree));
                    // Vertex at the start position of this adjacent degree group.
                    Object anotherVertex = (Object) vrtxList.get(adjBinPosition);
                    // Swapping the adjacent vertex with the first (different)
                    // vertex of the same bin.
                    if(adjacentV != anotherVertex)
                    {
                        positionMap.put(adjacentV, adjBinPosition);
                        positionMap.put(anotherVertex, adjPosition);
                        vrtxList.set(adjPosition, anotherVertex);
                        vrtxList.set(adjBinPosition, adjacentV);
                    }
                    // Shifts the position of the adjacent degree group in the
                    // vrxtList one point to the right.
                    adjBinPosition++;
                    binMap.put(adjDegree, adjBinPosition);
                    // Reduces the degree of the adjacent vertex.
                    adjDegree--;
                    coreMap.put(adjacentV, adjDegree);
                }
            }
        }
        return extractCores(coreMap);
    }
    
    /** extractCores: Extracts a subgraph based on the core number of the vertices.
     * The core of the maximum order is the main core that defines those elements
     * (facets/arcs) shared the highest number of  docs. Not just the main core 
     * is extracted: if enough links exist more cores are allowed. The elements
     * populating the selected cores are identified, others are removed from 
     * the selection.
     * See: 'An O(m) Algorithm for Cores Decomposition of Networks', V. Batagelj
     * & M. Zaversnik.
     * @param coreMap : SortedMap with the element-id as key and the corresponding
     * core number (int)as value.
     * @return Set mainCoreSet.
     */
    private Set  extractCores(SortedMap coreMap)
    {
        // The core number that limits the extracting of elements.
        int limitCore = 0;
        int highCore = 0;
        int previous = 0;
        int count = 0;
        int coreNumber = 5;
        // Gets a set with the core values sorted from high to low.
        LinkedList coreValues = new LinkedList(getCoreSet(coreMap));
        Iterator core_itr = coreValues.iterator();
        while(core_itr.hasNext())
        {
            int coreValue = ((Integer) core_itr.next());
            if(previous - coreValue == 1 && count < coreNumber && coreValue > 10) 
                limitCore = coreValue;
            if(coreValue > highCore) highCore = coreValue;
            previous = coreValue;
            count++;
        }
        // If no high core values were found, the highest core value remains 
        // the selection limit.
        if(limitCore == 0)  limitCore = highCore;
        // Collects a set with the ids that are member of the main cores group
        // and a separate set with
        // the ids from the next but highest core.
        Set coreSet = new HashSet();
        Set mainCoreSet = new HashSet();
        core_itr = coreMap.keySet().iterator();
        while(core_itr.hasNext())
        {
            Object coreID = (Object) core_itr.next();
            int coreValue = ((Integer) coreMap.get(coreID));
            if(coreValue >= limitCore) coreSet.add(coreID);
            if(coreValue >= (highCore - getCoreNumber())) mainCoreSet.add(coreID);
        }
        return mainCoreSet;
    }
    
    /** getCoreSet: Returns a SortedSet with core values sorted from high to low.
     * @param coreMap : SortedMap with as key the docId and the core number as value.
     * @return SortedSet with core values (int).
     */
    private SortedSet getCoreSet(SortedMap coreMap)
    {
        SortedSet coreSet = new TreeSet(highLow);
        Iterator core_itr = coreMap.keySet().iterator();
        while(core_itr.hasNext())
        {
            Object coreItem = (Object) core_itr.next();
            int coreValue = ((Integer) coreMap.get(coreItem));
            coreSet.add(coreValue);
        }
        return coreSet;
    }
    
    /** Comparator sorts the map on the core value from high to low.
     */
    private final Comparator highLow = (Comparator) (Object o1, Object o2) -> 
    {
        Integer w1 = (Integer) o1;
        Integer w2 = (Integer) o2;
        return w2.compareTo(w1);
    };
    
    /** setMainFacetSet :
     * @param Set with main core global facet keys (Integer).
     */
    private void setMainFacetSet(Set facetSet)
    {
        mMainFacetCore = facetSet;
    }
    
    /** getMainFacetCore :
     * @return Set with main core global facet keys (Integer).
     */
    public Set getMainFacetCore()
    {
        return mMainFacetCore;
    }
    
    /** setMainDocSet :
     * @param Set with main core global doc keys (Integer).
     */
    private void setMainDocSet(Set docSet)
    {
        mMainDocCore = docSet;
    }
    
    /** getMainDocCore :
     * @return Set with main core global doc keys (Integer).
     */
    public Set getMainDocCore()
    {
        return mMainDocCore;
    }
    
    /** getCoreNumber returns the number of main cores
     * @return int with the number of main cores as used by ExtractCores.
     */
    private int getCoreNumber()
    {
        return mCoreNumber;
    }
    
    /** setCoreNumber : the number of main cores from the parameter settings
     * in the main GUI.
     * @param number with the number of main cores.
     */
    public void setCoreNumber(int number)
    {
        if(number > 0) mCoreNumber = number - 1;
        else mCoreNumber = 0;
    }   
}
