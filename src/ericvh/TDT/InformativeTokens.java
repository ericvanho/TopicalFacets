package ericvh.TDT;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;


/** Class InformativeTokens selects the informative tokens by comparing the 
 * individual score of a particular token-type with a cutOff value range of 
 * its collection. The cutOff value range is a zone plus or minus the standard 
 * deviation around the mean of the summed weight (info value) of all token-types
 * in a collection.
 * @author  Eric Van Horenbeeck
 * Created on 16 september 2004, 0:06
 * Revision: Sep 27, 2006
 */
public class InformativeTokens implements Serializable
{
    // Class with the vertices map and methods to access the data.
    private final VerticesTable mVerticesTable;
    // Map holding the informative token-types per collection in this dataGraph.
    private Map mInformativeTokenMap;
    // Map holding the rejected token-types per collection in this dataGraph.
    private Map mRejectedTokenMap;
    private Map mRejectedMap;
    // Map with token count per collection.
    private Map mTokenCountMap;
    // CollectionTable with information on the collection level of this dataGraph.
    private final CollectionTable mCollTable;
    // Baseline contains an array of approx. 250 common words in the language 
    // of the current session.
    private final ArrayList<Integer> mBaseLineArray;
    // Instance of the Algorithms class with frequently used general methods.
    private final Algorithms alg = new Algorithms();
    // Using the common words baseline filter as set by the user.
    private final boolean mUseBaseLIne;
    
    /** Constructor called by the ApplicationManager.
     * @param runBaseline Using the common words baseline filter. If 'true' it
     * will remove common words even if they have a large informative value,  
     * conversely, highly significant words with a low informative score are
     * allowed in. This is necessary when working with a specific corpus
     * of documents that share the same technical vocabulary, making those terms
     * 'common', hence less informative in this context.
     */
    public InformativeTokens(boolean runBaseline)
    {
        mVerticesTable = ApplicationManager.getVerticesTable();
        mCollTable = ApplicationManager.getCollTable();
        mBaseLineArray = ApplicationManager.getBaseLine();
        mUseBaseLIne = runBaseline;
    }
    
    /** collectInfoTokens: Collects the informative and rejected tokens in 
     * two HashMaps. Key: collectionkey(String), value: map (HashMap) with
     * a token index as key (Integer) and an info weight as value (Double).
     */
    public void collectInfoTokens()
    {
        mInformativeTokenMap = new HashMap();
        mRejectedTokenMap = new HashMap();
        mTokenCountMap = new HashMap();
        //Selects informative tokens in each collection inside the scope of
        // this session.
        Set collectionKeys = mCollTable.getScopeCollectionKeys();
        Iterator coll_itr = collectionKeys.iterator();
        while(coll_itr.hasNext())
        {
            String collKey = coll_itr.next().toString();
            mInformativeTokenMap.put(collKey, setInformativeTokens(collKey));
            mRejectedTokenMap.put(collKey, getRejectedTokens());
        }
    }
    
    /** getFullInfoTokenMap: Getter returns the full infoTokenMap.
     * Key: collectionkey (String), value: HashMap with token index (key - Integer)
     * and info weight (value - Double).
     * @return Map with informative tokens (value) per collection (key)
     */
    public Map getFullInfoTokenMap()
    {
        return mInformativeTokenMap;
    }
    
    /** retrieveInfoMap: Retrieves the full infoMap as it was written and read
     * by the ApplicationManager.
     */
    public void retrieveInfoMap()
    {
        mInformativeTokenMap = ApplicationManager.getInfoMap();
    }
    
    /** getCollectionTokenMap: Getter returns an tokenMap for this collection. 
     * Key: collectionKey (String); Value: a map with token-ID as key (Integer)
     * and its info weight (Double) as value. When 'infoTokens' is 'true' 
     * the informative tokens are returned, else rejected.
     * @param collKey : this collection (String).
     * @param infoTokens : boolean 'true' expects infoTokens, 'false' 
     * expects rejected tokens.
     * @return HashMap with token-ids (Integer) and info values (Double) 
     * for this collection.
     */
    private Map getCollectionTokenMap(String collKey, boolean infoTokens)
    {
        if(infoTokens) return (HashMap) mInformativeTokenMap.get(collKey);
        else return (HashMap) mRejectedTokenMap.get(collKey);
    }
    
    /** getSortedInfoMap: Returns a map sorted on the info weight of the 
     * token-types for this collection. Key: sorted infoweight (Double),
     * value: the token-id (Integer).
     * @param collKey : this collection key (String).
     * @param infoTokens : boolean 'true' expects infoTokens, 'false' expects 
     * rejected tokens.
     * @return reversed map sorted on the info weight of the token-types for 
     * this collection
     */
    private SortedMap getSortedInfoMap(String collKey, boolean infoTokens)
    {
        SortedMap sortedInfoMap = alg.valueSortedMap(getCollectionTokenMap(collKey, 
                infoTokens));
        return sortedInfoMap;
    }
    
    /** setInformativeTokens: Selects the token-types of high informative value
     * for this collection. Rejected tokens are saved for inspection in the 
     * mRejectedTokenMap with the same structure as the mInformativeTokenMap.
     * @param collKey : key to this collection (String).
     * @return Hashmap with the informative token-types (Integer) from this 
     * collection with their info values (double).
     */
    private Map setInformativeTokens(String collKey)
    {
        int count = 0;
        String collectionKey = collKey;
        // Motivation for setting an upper and lower boundary is that labels 
        // in the middle of the links-curve are more informative than at the 
        // extremes. See Small World texts.
        double[] boundArray = getInfoRange(collectionKey);
        double vrtxValue;
        double noise;
        Map tempTokenMap = new HashMap();
        Map nonInfoMap = new HashMap();
        Vertex currentVertex;
        Map vertices = mVerticesTable.getFullVerticesMap();
        // Iterates over all vertices in this collection.
        Iterator vertex_itr = vertices.keySet().iterator();
        while (vertex_itr.hasNext())
        {
            Integer vKey = (Integer) vertex_itr.next();
            currentVertex = (Vertex) vertices.get(vKey);
            // Checks first if this vertex is a member of this collection.
            if (currentVertex.getAllCollectionKeys().contains(collectionKey))
            {
                // Background noise is subtracted from the info value.
                noise = mCollTable.getBackgroundNoise(collKey);
                //  System.out.println("Noise: " + noise);
                vrtxValue = currentVertex.getInfoValue(collectionKey) - noise;
                // Puts informative tokens (index) and their info values in a
                // temporary map.
                // If the BaseLineArray is used, the following happens:
                if (mUseBaseLIne)
                {
                    // Collecting of rejected tokens: all the words found in the
                    // BaseLineArray, regardless of their calculated values.
                    if (mBaseLineArray.contains(vKey))
                    {
                        // Rejected tokens get the lower bound value minus an
                        // arbitrarily small decimal putting them under the low 
                        // boundary.
                        nonInfoMap.put(vKey, boundArray[2] - 0.01);
                    } else // Words not in the BaseLineArray...
                    {
                        // .. with a value > the lower threshold, keep their 
                        // calculated value.
                        if (vrtxValue >= boundArray[2])
                        {
                            tempTokenMap.put(vKey, vrtxValue);
                        } // Not in the BaseLineArray with a low value, due to their 
                        // high frequency in this collection and for that reason 
                        // presumed informative...
                        else
                        {
                            // are getting the mean value.
                            tempTokenMap.put(vKey, boundArray[0]);
                        }

                    }
                // Using the normal calculation when the corpus is well-balanced,
                // i.e., containing documents on a variety of general subjects.
                // The vertices keep their values as calculated by the 
                // InfoValueCalculator class if the vrtxValue is greater than
                // boundArray[2], the lower bound.
                } else
                {
                    if (vrtxValue > boundArray[2])
                    {
                        tempTokenMap.put(vKey, vrtxValue);
                    } else
                    {
                        nonInfoMap.put(vKey, vrtxValue);
                    }
                }
                count++;
            }
        }
        // Keeps the number of token-types seen per collection.
        mTokenCountMap.put(collKey, count);
        // Returns the temporay map after processing all vertices of this 
        // collection.
        setRejectedTokens(nonInfoMap);
        return tempTokenMap;
    }
    
    /** setRejectedTokens: Vertices from one collection that did not meet the
     * info value limit.
     * @param rejectedMap : HashMap with vertex-ids as key (Integer) and info 
     * values as value (Double).
     */
    private void setRejectedTokens(Map rejectedMap)
    {
        mRejectedMap = rejectedMap;
    }
    
    /** getRejectedTokens: Returns map with vertices under the info value limit 
     * from one collection.
     * @return HashMap with vertex-ids as key (Integer) and the info values as 
     * value (Double).
     */
    private Map getRejectedTokens()
    {
        return mRejectedMap;
    }
    
    /** getInformativeTokenString: Shows all the informative tokens in this 
     * dataGraph on screen.
     * @param countOnly : boolean 'true' will give only the number of tokens 
     * for a given value, else if 'false' all the token-labels are retrieved.
     * @return String with informative tokens per collection
     */
    public String getInformativeTokenString(boolean countOnly)
    {
        return prepareString(mInformativeTokenMap, countOnly, true);
    }
    
    /** getRejectedTokenString: Shows all the rejected tokens in this dataGraph 
     * on screen.
     * @param countOnly : boolean 'true' will give only the number of tokens 
     * for a given value, else if 'false' all the token-labels are retrieved.
     * @return String with rejected tokens per collection
     */
    public String getRejectedTokenString(boolean countOnly)
    {
        return prepareString(mRejectedTokenMap, countOnly, false);
    }
    
    /** prepareString: Returns a formated string with tokens sorted on their 
     * info value and a summary per collection.
     * @param tokenMap : HashMap with token map (value) per collection (key).
     * Token map is a HashMap with vertex-id as key and the info value as value.
     * @param countOnly : boolean 'true' will give only the number of tokens,
     * not the labels.
     * @param infoTokens : boolean 'true' expects infoTokens, 'false' expects 
     * rejected tokens.
     * @return String with formated tokens and their info value;
     */
    private String prepareString(Map tokenMap, boolean countOnly, boolean infoTokens)
    {
        String tokenString = "";
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(1);
        Iterator collection_itr =  tokenMap.keySet().iterator();
        while(collection_itr.hasNext())
        {
            SortedMap sortTokenMap;
            String collKey = collection_itr.next().toString();
            sortTokenMap = getSortedInfoMap(collKey, infoTokens);
            tokenString += tokensToString(collKey, sortTokenMap, nf, countOnly,
                    infoTokens);
        }
        return tokenString;
    }
    
    /** getInfoRange: Returns an array with three doubles: 
     * first the mean info-value; the second value is the upper bound of 
     * the info-value range and the third value is the lower bound.
     * @param collKey : Collection key (String).
     * @return array with three doubles describing the info-value range for 
     * this collection.
     */
    private double[] getInfoRange(String collKey)
    {
        double[] boundArray = new double[3];
        // Standard deviation of the info-values of this collection.
        double stDev = mCollTable.getStDev(collKey);
        // Mean of the info-values.
        boundArray[0] = mCollTable.getMeanValue(collKey);
        // Upper bound of the info-value range.
        boundArray[1] = boundArray[0] + 4 * stDev;   // 4 * stDev
        // Lower bound of the info-value range.
        boundArray[2] = boundArray[0] - 4 * stDev;   // 4 * stDev
        if(boundArray[2] < 0) boundArray[2] = 0;
        return boundArray;
    }
    
    /** tokensToString: Returns the tokens for this collection in a printable 
     * format. As default only the count of the informative labels per value 
     * is given. The user can request all labels in a format: collKey 
     * (label1, label2,...,labelN) by changing the checkbox in the Parameter 
     * Settings submenu in the main GUI. ApplicationManager can request to 
     * print the rejected tokens aswell.
     * @param collKey : key to a collection (String)
     * @param sortedTokenMap : map with infovalue (key) and set of tokens (value).
     * @param pf : a percentage formatting flag (NumberFormat).
     * @param countOnly : boolean 'true' gives the number of tokens for a given 
     * value. If 'false' all the token-labels are retrieved.
     * @param infoTokens : boolean 'true' expects infoTokens, 'false' expects 
     * rejected tokens.
     * @return informative or rejected tokens per collection as a String with a 
     * summary per collection.
     */
    private String tokensToString(String collKey, Map sortedTokenMap,
            NumberFormat pf, boolean countOnly, boolean infoTokens)
    {
        String collectionKey = collKey;
        String tokenString;
        String infoString;
        DecimalFormat df = new DecimalFormat("###,###.####");
        StringBuffer tokenTemp = new StringBuffer();
        LabelTable allLabels = ApplicationManager.getLabels();
        int total = 0;
        // Attention: this method is wrong: token-type numbers cannot be added,
        // token-types should be collected in a set to eliminate doubles!
        int tokenTypes =  mCollTable.getTokenTypeCount(collectionKey);
        int tokens = mCollTable.getTokenCount(collectionKey);
        int tokenCounted = ((Integer) mTokenCountMap.get(collectionKey));
        if(infoTokens) infoString = "Informative";
        else infoString = "Rejected";
        // Gets the range that divides informative and common tokens.
        double[] boundArray = getInfoRange(collectionKey);
        String infoTokenString ="\n+ " + infoString + " Tokens in Collection: "
                + collKey + '\n';
        Iterator weight_itr = sortedTokenMap.keySet().iterator();
        while(weight_itr.hasNext())
        {
            Double weight = (Double) weight_itr.next();
            // Set with important tokens.
            Set tokenSet = (Set) sortedTokenMap.get(weight);
            int tokenCount = tokenSet.size();
            if(tokenCount > 1) tokenString = " token-types ";
            else tokenString = " token-type ";
            tokenTemp.append("\n").append(tokenCount).append(tokenString).
                    append("with info-weight: ").append(df.format(weight));
            total += tokenCount;
            // Adds the token-labels if the user so whishes.
            if(countOnly == false)
            {
                List tokenLabels = allLabels.returnLabelList(Arrays.
                        asList(tokenSet.toArray()));
                tokenTemp.append('\n');
                Collections.sort(tokenLabels);
                Iterator label_itr = tokenLabels.listIterator();
                while(label_itr.hasNext())
                {
                    tokenTemp.append(label_itr.next().toString()).append(',');
                }
                // Removes last comma.
                tokenTemp = tokenTemp.deleteCharAt(tokenTemp.length()-1);
            }
        }
        // Some global statistics for this collection. 
        // Attention: Type/token ratio calculation is wrong!
        if(tokenTemp.length() == 0) tokenTemp.append("NO_TOPIC*");
        infoTokenString += tokenTemp + "\n\nCollection: " + collKey 
                + "\nUpperbound: " + df.format(boundArray[1]) + " mean: " 
                + df.format(boundArray[0]) + ' ' + "lowerbound: " +
                df.format(boundArray[2]) + " - Files processed: " +
                df.format(mCollTable.getCollectionDocCount(collectionKey)) 
                +  "\n" + infoString + " token-types in these files: " 
                + df.format(total) + " (" + pf.format(((double) total) / tokenCounted)
                + ")\nTotal token-types in all documents of this collection: "
                + df.format(tokenTypes) + " - Total tokens: "  + df.format(tokens)
                + "\nType/Token ratio: " + pf.format(((double) tokenTypes) / tokens)
                + "\n____________________________________________________"
                + "___________________________\n";
        return infoTokenString;
    }
    
    /** getUpdatedVertices: Getter returns the verticesTable updated with the
     * newly calculated values on all the vertices.
     * @return the updated verticesTable
     */
    public VerticesTable getUpdatedVertices()
    {
        return mVerticesTable;
    }
    
}
