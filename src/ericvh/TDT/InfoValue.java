package ericvh.TDT;

import java.util.*;
import java.io.*;

/** Class InfoValue
 *
 * @author  Eric Van Horenbeeck
 * Created on 11 september 2004, 11:46
 */
public class InfoValue implements Serializable
{
    private final Map mTokenInfoValueMap;
    private String mCollectionKey;
    
    /** Constructor
     */
    public InfoValue()
    {
        mTokenInfoValueMap = new HashMap();
    }
    
    /** Replaces the initial info value of this token in this collection.
     * @param sourceDate : key to the collection
     * @param newValue :
     */
    public void updateValue(String sourceDate, double newValue)
    {
        mCollectionKey = sourceDate;
        this.mTokenInfoValueMap.put(mCollectionKey, newValue);
    }
    
    /** Adds a map with values from a single textgraph to this vertex in
     * the general dataGraph.
     * @param tokenValueMap : a map with collection keys as key and info values as value
     */
    public void addAllValues(Map tokenValueMap)
    {
        mTokenInfoValueMap.putAll(tokenValueMap);
    }
    
    /** Getter returns the info value for this vertex in this collection
     * @param sourceDate : key to the collection
     * @return the actual info value of this vertex (double)
     */
    public double getInfoValue(String sourceDate)
    {
        mCollectionKey = sourceDate;
        return ((Double) mTokenInfoValueMap.get(mCollectionKey));
    }
    
    /** Getter returns the map with the info value for this vertex in 
     * every collection
     * @return the map with actual info value of this vertex (Double)
     */
    public Map getAllInfoValue()
    {
        return mTokenInfoValueMap;
    }
    
    /** Getter returns the set with all the collection keys fot this vertex
     * @return set with collection keys (String)
     */
    public Set getCollectionKeys()
    {
        return mTokenInfoValueMap.keySet();
    }
}
