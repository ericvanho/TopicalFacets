package ericvh.TDT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.Serializable;


/** Class TokenCount is the central repostory of all token counters in all 
 * the documents of this dataGraph. It is composed by the ApplicationManager 
 * with data from the individual textGraph.
 * @author  Eric Van Horenbeeck
 * Created on 20 september 2004, 11:11
 */
public class TokenCount implements Serializable
{
    // Document identifier.
    private Integer mDocInt;
    // Map with the token counters.
    private final Map mTokenCount;
    
    /** Constructor
     */
    public TokenCount()
    {
        mTokenCount = new HashMap();
    }
    
    /** addTokenCountMap: Adds a map of token counts from a textGraph to this 
     * central token count dictionary. The incoming map has a tokenId as key 
     * and the counting as value. The final map has the doc-id as key and a map 
     * of token-ids and their counts as value.
     * @param docNr : the unique document identifier (int)
     * @param tc : the map from a single textgraph with tokenId as key and count
     * as value
     */
    public void addTokenCountMap(int docNr, Map tc)
    {
        mDocInt = docNr;
        Integer tokenId;
        Map tmpMap = new HashMap();
        Iterator tc_itr = tc.keySet().iterator();
        while(tc_itr.hasNext())
        {
            tokenId = (Integer) tc_itr.next();
            if(mTokenCount.containsKey(tokenId)) tmpMap = (HashMap) mTokenCount
                    .get(tokenId);
            tmpMap.put(mDocInt,tc.get(tokenId));
            mTokenCount.put(tokenId, tmpMap);
        }
    }
    
    /** getTokenCount: Getter returns the count of a specific token in a document.
     * @param docNr : the unique document identifier (int)
     * @param tokenId : the unique token identifier (Integer)
     * @return token-type counter in a document (int)
     */
    public int getTokenCount(int docNr, Integer tokenId)
    {
        mDocInt = docNr;
        Map tmpMap = (HashMap) mTokenCount.get(tokenId);
        return ((Integer) tmpMap.get(mDocInt));
    }
    
    /** getTokenCountMap: Getter returns the full map of token counts with 
     * the docNr as key and a second map as value. In the second map the token
     * id is key and the token count is value.
     * @return the full map with countings of all tokens per document
     */
    public Map getTokenCountMap()
    {
        return mTokenCount;
    }
    
}
