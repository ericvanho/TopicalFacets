package ericvh.TDT;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


/** Class CollectionTable handles information on collections of documents. 
 * Every document (text) is a member of one and only one collection, identified
 * by a common source and a common date. An instance of this class keeps for
 * every collection a set with the document numbers that are part of it and a
 * array with info-value components that serve to calculate a cutOff point to 
 * separate informative tokens in this collection from the common ones. 
 * These containers are instances of the CollectionData class.
 * @author  Eric Van Horenbeeck
 * Created on 28 juli 2004, 11:20
 */
public class CollectionTable implements Serializable
{
    // The map with info on all collections.
    private final Map mCollections;
    // Set with collection keys that have info values.
    private Set mOldValueSet;
    // Map with all docs-ids of a scope and the set of vertices used in those 
    // documents.
    private Map mDocVertices;
    
    /** Constructor
     * Creates a map with the collKey as collection key and an instance of 
     * CollectionData as value.
     */
    public CollectionTable()
    {
        mCollections = new HashMap();
        mDocVertices = new HashMap();
    }
    
    /** addInfoValue: Adds info value (weight) to the collectionData of this 
     * collection to allow calculation of the cutOff point between informative 
     * and common tokens.
     * @param collKey : the collection key (String).
     * @param value : the individual informative weight of a token (double).
     */
    public void addInfoValue(String collKey, double value)
    {
        // Any old values are removed prior to adding the newly calculated data
        // if this collection key is in the oldValueSet.
        CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
        if(mOldValueSet.contains(collKey))
        {
            tmpCD.removeOldValues();
            // Value removing needs to be done only once in a session.
            mOldValueSet.remove(collKey);
        }
        tmpCD.addCollectionWeight(value);
    }
    
    /** setOldValues: Gets collection keys in the sope of this session that 
     * could have their old info values removed prior to adding new ones.
     * Called by the InfoValueCalculator.
     */
    public void setOldValues()
    {
        mOldValueSet = getScopeCollectionKeys();
    }
    
    /** setCutOff: Sets for every collection in this dataGraph the final cutOff 
     * info value and the background noise factor.
     */
    public void setCutOff()
    {
        Iterator coll_itr = getScopeCollectionKeys().iterator();
        while(coll_itr.hasNext())
        {
            String collectionKey = coll_itr.next().toString();
            CollectionData tmpCD = (CollectionData) mCollections
                    .get(collectionKey);
            tmpCD.setFinalInfoValue();
        }
    }
    
    /** getMeanValue: Getter returns the value that divides informative from
     * common tokens in this collection.
     * @param collKey : the collection key (String).
     * @return the value of the cutOff point (double) for this collection.
     */
    public double getMeanValue(String collKey)
    {
        CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
        return tmpCD.getInfoCutOffValue();
    }
    
    /** getBackgroundNoise: Getter returns the background noise factor that 
     * represents a constant minimum level to be subtracted from the info value 
     * of the vertices.
     * @param collKey : the collection key (String).
     * @return the value of the background noise factor (double) for this
     * collection.
     */
    public double getBackgroundNoise(String collKey)
    {
        CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
        return tmpCD.getBackgroundNoise();
    }
    
    /** getStDev: Getter returns the standard deviation of the info value of
     * this collection.
     * @param collKey : the collection key.
     * @return the standard deviation normalized (double) for this collection.
     */
    public double getStDev(String collKey)
    {
        CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
        return tmpCD.getStandardDeviation();
    }
    
    /** addDocNrToCollection: Adds the unique document number to a collection
     * with collKey as key. If necessary a new instance of CollectionData is 
     * created.
     * @param collKey : the collection key (String).
     * @param docNr : the unique document number (int) as value.
     */
    public void addDocNrToCollection(String collKey, int docNr)
    {
        if(mCollections.containsKey(collKey))
        {
            CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
            tmpCD.addDocNr(docNr);
        }
        else
        {
            CollectionData mCollData = new CollectionData();
            mCollData.addDocNr(docNr);
            mCollections.put(collKey, mCollData);
        }
    }
    
    /** getCollectionDocCount: Getter of the number of documents that are part 
     * of this collection(its size). Not restricted to the collections inside
     * the scope of this session.
     * @param collKey : the key (String) to the collection.
     * @return the number of documents (int) in a collection.
     */
    public int getCollectionDocCount(String collKey)
    {
        int count = 0;
        if(mCollections.containsKey(collKey))
        {
            CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
            count = tmpCD.getDocCount();
        }
        return count;
    }
    
    /** getTokenCount: Getter returns the sum of the number of tokens in all 
     * documents of this collection. Not restricted to the collections inside
     * the scope of this session.
     * @param collKey : the key (String) to the collection.
     * @return the number of tokens (int) in this collection.
     */
 public int getTokenCount(String collKey)
    {
        int allTokens = 0;
        String collectionKey = collKey;
        // Gets the docTable for this dataGraph.
        DocTable dt = ApplicationManager.getDocTable();
        Set docNrsSet = getDocsInCollection(collectionKey);
        Iterator docNrs_itr = docNrsSet.iterator();
        while(docNrs_itr.hasNext())
        {
            int docNr = ((Integer)docNrs_itr.next()).intValue();
            allTokens += dt.getTotTokens(docNr);
        }
        return allTokens;
    }
    
    /** getTokenTypeCount: Getter of the total number of token-types in all
     * documents of this collection. Not restricted to the collections inside 
     * the scope of this session.
     * @param collKey : the key (String) to the collection.
     * @return the total number (int) of token-types in a collection
     */
//    public int getTokenTypeCount(String collKey)
//    {
//        Set totTypes = new HashSet();
//        String collectionKey = collKey;
//        // Gets the docTable for this dataGraph.
//        SortedSet docNrsSet = getDocsInCollection(collectionKey);
//       // Gets the vertices from the documents in this collection
//        mDocVertices = ApplicationManager.getVerticesTable().getDocsWithVertices();
//        Set localVertices;
//        Iterator docNrs_itr = docNrsSet.iterator();
//        while( docNrs_itr.hasNext())
//        {          
//            int docNr = ((Integer)docNrs_itr.next());
//            localVertices = (HashSet) mDocVertices.get(docNr);
//            Iterator vrtx_itr = localVertices.iterator();
//            while(vrtx_itr.hasNext())
//            {
//               totTypes.add(vrtx_itr.next());
//            }
//        }
//        return totTypes.size();
//    }
        public int getTokenTypeCount(String collKey)
    {
        int totTypes = 0;
        String collectionKey = collKey;
        // Gets the docTable for this dataGraph.
        DocTable dt = ApplicationManager.getDocTable();
        SortedSet docNrsSet = getDocsInCollection(collectionKey);
        Iterator docNrs_itr = docNrsSet.iterator();
        while( docNrs_itr.hasNext())
        {
            int docNr = ((Integer)docNrs_itr.next());
            totTypes += dt.getTotTypes(docNr);
        }
        return totTypes;
    }
 
    /** getDocsInCollection: Getter of a set with all the document numbers in 
     * this collection. Not restricted to the collections inside the scope of 
     * this session.
     * @param collKey : the key (String) to the collection.
     * @return HashSet with the document numbers (Integer) of this collection.
     */
    public SortedSet getDocsInCollection(String collKey)
    {
        // A collection contains at least one document.
        if(mCollections.containsKey(collKey))
        {
            CollectionData tmpCD = (CollectionData) mCollections.get(collKey);
            return tmpCD.getDocNrSet();
        }
        else return null;
    }
    
    /** getScopeCollectionKeys: Getter of a set with all the collection keys 
     * in this dataGraph inside the scope of this session.
     * @return HashSet with all the collection keys (String) relevant for this
     * session.
     */
    public Set getScopeCollectionKeys()
    {
        Set collectionSet = new HashSet();
        String scope = ApplicationManager.getScope();
        Calendar fileDate = Calendar.getInstance();
        // Only collects the files within the time interval (scope) of this task.
        Calendar beginDate = ApplicationManager.getBeginDate();
        Calendar endDate = ApplicationManager.getEndDate();
        Iterator coll_itr = mCollections.keySet().iterator();
        while (coll_itr.hasNext())
        {
            String collKey = coll_itr.next().toString();
            if (scope.equals("AllDates"))
            {
                fileDate.set(0000, 00, 00);
            } else
            {
                fileDate = GraphTime.getDate(fileDate, collKey);
            }

            if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
            {
                collectionSet.add(collKey);
            }
        }
        return collectionSet;
    }
    
    /** getAllCollectionKeys: Returns a set with all available collection keys
     * in this dataGraph. Not restricted to the collections inside the scope 
     * of this session.
     * @return HashSet with all the collection keys (String).
     */
    public Set getAllCollectionKeys()
    {
        return mCollections.keySet();
    }
    
    /** getFullTableSize: Returns the total number of collections in this table.
     * Not restricted to the collections inside the scope of this session.
     * @return the size of the collections map (int).
     */
    public int getFullTableSize()
    {
        return mCollections.size();
    }
    
    /** getDocsCollectionKey: Returns the collection key for this document. 
     * A document can only be in one collection. Every collection has at least 
     * one document. Not restricted to the collections inside the scope of 
     * this session.
     * @param docNr : the unique document number (int).
     * @return the collectionKey (String) for this document.
     */
    public String getDocsCollectionKey(int docNr)
    {
        Integer aDocNr = docNr;
        String collectionKey = "";
        // Set with the collection keys.
        Set collKeys = getAllCollectionKeys();
        // Set with document numbers.
        Set docNrsSet;
        Iterator keys_itr = collKeys.iterator();
        search:
        while (keys_itr.hasNext())
        {
            String thisKey = keys_itr.next().toString();
            docNrsSet = getDocsInCollection(thisKey);
            if (docNrsSet.contains(aDocNr))
            {
                collectionKey = thisKey;
                break;  // Early exit.
            }
        }
        return collectionKey;
    }
    
    /** getSourceName: Getter of the source of this document as recorded in
     * his collection key. Not restricted to the collections inside the scope 
     * of this session.
     * @param collKey : key (String) to the collection.
     * @return abbreviated source name (String) of this document.
     */
    public String getSourceName(String collKey)
    {
        StringBuilder source = new StringBuilder();
        String collection = collKey;
        for(int i = 8; i < collection.length(); i++)
        {
            char cr = collection.charAt(i);
            if (Character.isLetter(cr)) source.append(cr);
            else i = collection.length();  // Early exit.
        }
        return source.toString();
    }
    
    /** getTimeStamp: Returns the timestamp (date) as recorded in his 
     * collection key. Not restricted to the collections inside the scope of
     * this session.
     * @param collKey : key to the collection (String).
     * @return date (String) found in this key.
     */
    private String getTimeStamp(String collKey)
    {
        return collKey.substring(0,8);
    }
    
    /** sortDocuments: Distributes a set of miscellaneous documents over the 
     * collections they belong to. Every document always belongs to one and 
     * only one collection. Not restricted to the collections inside the scope 
     * of this session.
     * @param docSet : a set of document-ids (Integer).
     * @return a HashMap with the collection as key (String) and an ArrayList 
     * of document-ids (Integer) belonging to that collection.
     */
    public Map sortDocuments(Set docSet)
    {
        Map documentMap = new HashMap();
        Iterator collKey_itr = getAllCollectionKeys().iterator();
        while(collKey_itr.hasNext())
        {
            // Array to hold the documents per collection.
            List docList = new ArrayList();
            String collKey = collKey_itr.next().toString();
            // Iterates over the doc-set and adds the id-number to the temporary
            // list if it is a member of the collection in view.
            Iterator docs_itr = docSet.iterator();
            while(docs_itr.hasNext())
            {
                Integer docInt = (Integer)docs_itr.next();
                if(getDocsInCollection(collKey).contains(docInt)) 
                    docList.add(docInt);
            }
            if(!docList.isEmpty()) documentMap.put(collKey, docList);
        }
        return documentMap;
    }
    
    /** getSources: Returns a set with the names of the sources of a map with 
     * collections and document-ids. Not restricted to the collections inside
     * the scope of this session.
     * @param docMap : map with collection as key (String) and a doclist as value.
     * @return a set with the sources (String) found in this docMap.
     */
    public Set getSources(Map docMap)
    {
        Set sourceSet = new HashSet();
        Iterator map_itr = docMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            String collKey = map_itr.next().toString();
            sourceSet.add(getSourceName(collKey));
        }
        return sourceSet;
    }
    
    /** getDates: Returns a set with the dates found in a map with collections 
     * and document-ids. Not restricted to the collections inside the scope
     * of this session.
     * @param docMap : map with collection as key (String) and a doclist as value.
     * @return a set with the dates (String) found in this docMap.
     */
    public Set getDates(Map docMap)
    {
        Set dateSet = new HashSet();
        Iterator map_itr = docMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            String collKey = map_itr.next().toString();
            dateSet.add(getTimeStamp(collKey));
        }
        return dateSet;
    }
    
    /** isEmpty: Checks whether this class instance has any elements (documents).
     * @return boolean 'true' if empty.
     */
    public boolean isEmpty()
    {
        return mCollections.keySet().isEmpty();
    }
    
    /** collectionsToString: ToString method returns a list of document numbers
     * per collection and the total numbers of files in the scope of this 
     * session. Format: collKey (docnr1,docnr2,...,docnrN) as String.
     * @return collection information as a String.
     */
    public String collectionsToString()
    {
        int totFiles = 0;
        DecimalFormat formatter = new DecimalFormat("###,###");
        
        String collectionInfo = "CollectionTable - The collections in this "
                + "dataGraph (with the identifying docNrs)\n";
        // Set with document numbers.
        SortedSet docNrs;
        Iterator coll_itr = getScopeCollectionKeys().iterator();
        while(coll_itr.hasNext())
        {
            String keyTemp = coll_itr.next().toString();
            StringBuffer docTemp = new StringBuffer();
            docTemp.append('(');
            docNrs = getDocsInCollection(keyTemp);
            Iterator docNr_itr = docNrs.iterator();
            while(docNr_itr.hasNext()) docTemp.append(docNr_itr.next()
                    .toString()).append(',');
            // Removes last commma before adding closing bracket.
            docTemp = docTemp.deleteCharAt(docTemp.length()-1).append(')');
            collectionInfo += keyTemp + ": " + docNrs.size() + " documents "
                    + docTemp + '\n' ;
            totFiles += docNrs.size();
        }
        String files = formatter.format(totFiles);
        collectionInfo += '\n' + files + " files have been processed\n";
        return collectionInfo;
    }
    
}
