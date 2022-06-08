package ericvh.TDT;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/** Class DocTable creates and updates information on the documents used to 
 * build the dataGraph. It has a map with instances of docStats holding the 
 * following data on these files: the highestfrequency score of a token in a document,
 * total tokens and total token-types in each document and the name of the file.
 * It is a general file not limited to the scope of a session.
 * @author  Eric Van Horenbeeck
 * Created on 26 juli 2004, 17:43
 * Revision: July 7, 2006
 */
public class DocTable implements Serializable
{
    // Sorted map holding the info on each document.
    private final SortedMap mDocMap;
    // A sorted set with all document numbers already known inside the scope of this session.
    private SortedSet mDocNrSet;
    // Temporary list with attributed document-ids that are not yet saved in the mDocMap.
    private final LinkedList mTmpIDList;
    
    /** Constructor
     * Creates a map holding docStats instances with individual document info
     * when 'new' (the unique document number is key) and a set to hold 
     * temporarily the doc-ids already attributed but not yet saved. 
     * The set is initiated with '-1'.
     */
    public DocTable()
    {
        mDocMap = new TreeMap();
        mTmpIDList = new LinkedList();
        mTmpIDList.add(-1);
    }
    
    /** preparePreviousDocList: Collects a sorted set with all previously 
     * attributed doc-ids. Called only by the ApplicationManager after 
     * retrieving an existing DocTable from disk. The last doc-id that was
     * attributed is put in a temporary set. The first set collects doc_ids 
     * inside the scope of this session, the second set looks at all doc-ids 
     * in this docMap.
     */
    public void preparePreviousDocList()
    {
        mDocNrSet = new TreeSet(Arrays.asList(getAllDocNrs()));
        Iterator doc_itr = mDocMap.keySet().iterator();
        LinkedList docList = Algorithms.iteratorToLinkedList(doc_itr);
        if(!docList.isEmpty()) mTmpIDList.add(((Integer) docList.getLast()));
    }
    
    /** addDocInfo: Adds individual document statistics to the table.
     * @param docNr : the key a unique document number (int).
     * @param ds : a DocStats instance.
     */
    public void addDocInfo(int docNr, DocStats ds)
    {
        mDocMap.put(docNr, ds);
    }
    
    /** setTotLinks: Adds the total number of links found in this document to 
     * the document statistics.
     * @param docNr : the key a unique document number (int).
     * @param totLinks : the total number of links (int) in this document.
     */
    public void setTotLinks(int docNr, int totLinks)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        ds.setTotLinks(totLinks);            
    }
    
    /** getTotalLinks: Returns the total number of links (incoming and outgoing)
     * in this document.
     * @param docNr : the key to the document (int).
     * @return the total number of links (int) in this document.
     */
    public int getTotalLinks(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getTotLinks();
    }
    
    /** setMaxLinks: Adds the highest number of links belonging to one token in
     * this document to this 
     * document's statistics.
     * @param docNr : the key a unique document number (int).
     * @param maxLinks : the single highest link count (int) in this document.
     */
    public void setMaxLinks(int docNr, int maxLinks)
    {
        DocStats ds = (DocStats) getDocStats(docNr);   
        ds.setMaxLinks(maxLinks);         
    }
    
    /** getMaxLinks: Returns the highest number of links belonging to one token 
     * in this document
     * @param docNr : the key to the document (int).
     * @return the single highest link count (int) in this document.
     */
    public int getMaxLinks(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getMaxLinks();
    }
    
    /** getDocStats: Getter of docStats that keeps the maximum frequency,
     * total tokens, total types and 
     * the full path name of this file.
     * @param docNr : the key a unique document number (int).
     * @return the docStats instance with the file data.
     */
    public Object getDocStats(int docNr)
    {
        return mDocMap.get(docNr);
    }
    
    /** getSomeDocs: Returns random document-ids from a list of documents 
     * inside the scope of this session.
     * @param count : the number of doc-ids to return (int).
     * @return a list with document-id (Object).
     */
    public List getSomeDocs(int count)
    {
        List docList = new ArrayList();
        Object[] tmpArray = getAllDocNrs();
        int arrayLength = tmpArray.length;
        for(int i = 0; i < arrayLength; i++)
        {
            int random = (int) (arrayLength * Math.random() + 1);
            if(i == count) break;
            else docList.add(tmpArray[random]);
        }
        return docList;
    }
    
    /** docExists: Returns 'true' if in this document map a document exists
     * identified by this number.
     * @param docInt : : the key to the document (Integer).
     * @return 'true' if document exists, 'false' if not.
     */
    public boolean docExists(Integer docInt)
    {
        return mDocMap.keySet().contains(docInt);
    }
    
    /** getMaxFrequency: Getter returns the maximum frequency of a token found 
     * in this document.
     * @param docNr : the key to the document (int).
     * @return the highest token frequency found in this doc (int).
     */
    public int getMaxFrequency(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getHighFrequency();
    }
    
    /** getTotTokens: Getter returns the count of all the 'words' (tokens) 
     * in this document.
     * @param docNr : a unique document number (int).
     * @return the number of tokens (int).
     */
    public int getTotTokens(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getTotTokens();
    }
    
    /** getTotTypes: Getter returns how many different token-types were found 
     * in this document.
     * @param docNr : the key a unique document number (int).
     * @return the number of token-types.
     */
    public int getTotTypes(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getTotTypes();
    }
    
    /** hasNoText: Checks of this document has usefull text or not (NO_TEXT).
     * @param docNr : the key a unique document number (int).
     * @return boolean 'true' is this document is empty.
     */
    public boolean hasNoText(int docNr)
    {
        return getTotTokens(docNr) < 2;
    }
    
    /** getAllDocNrs: Gets all document numbers of this dataGraph inside the 
     * scope of this session.
     * @return Object array with the document numbers (Integer) from every 
     * collection.
     */
    public Object[] getAllDocNrs()
    {
        Set docNrSet = new HashSet();
        Calendar date = Calendar.getInstance();
        date.set(0000, 00, 00);
        Iterator doc_itr = mDocMap.keySet().iterator();
        if (ApplicationManager.getScope().equals("AllDates"))
        {
            while (doc_itr.hasNext())
            {
                Integer docInt = (Integer) doc_itr.next();
                docNrSet.add(docInt);
            }
        } else
        {
            // Only collects the files within the time interval (scope) of this
            // task.

            Calendar beginDate = ApplicationManager.getBeginDate();
            Calendar endDate = ApplicationManager.getEndDate();

            while (doc_itr.hasNext())
            {
                Integer docInt = (Integer) doc_itr.next();
                String filename = getFilename(docInt);
                Calendar fileDate = GraphTime.getDate(date, filename);
                if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                {
                    docNrSet.add(docInt);
                }
            }
        }
        return docNrSet.toArray();
    }
    
    /** isEmpty: Checks whether this docTable has any elements.
     * @return boolean 'true' if empty.
     */
    public boolean isEmpty()
    {
        return mDocMap.isEmpty();
    }
    
    /** getDocCount: Getter counts the number of documents in the docTable
     * inside the actual scope of the
     * dataGraph.
     * @return number of documents (int).
     */
    public int getDocCount()
    {
        Object[] docNrs = getAllDocNrs();
        return docNrs.length;
    }
    
    /** getFullTableSize: Returns the total number of documents in this table.
     * @return the size of the docMap (int).
     */
    public int getFullTableSize()
    {
        return mDocMap.size();
    }
    
    /** getFilename: Getter returns the filename of this document.
     * @param docNr : a unique document number (int).
     * @return String with filename.
     */
    public String getFilename(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getFileName();
    }
    
    /** getPathName: Getter returns the full pathname of this document.
     * @param docNr : a unique document number (int).
     * @return String with full pathname.
     */
    public String getPathName(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getPathName();
    }
    
    /** getDocNr: Returns the file number attached to this document if one was 
     * seen by the application.
     * @param docName : String with the filename of a doc (without path and 
     * without suffix).
     * @return the id (int) of this document if it exists, else returns '-1'.
     */
    public int getDocNr(String docName)
    {
        int docNr = -1;
        Iterator doc_itr = mDocMap.keySet().iterator(); //mDocNrSet.iterator();
        while(doc_itr.hasNext())
        {
            docNr = ((Integer) doc_itr.next());
            String fileName = getFilename(docNr);
            if(fileName.equals(docName)) break;
            else docNr = -1;
        }
        return docNr;
    }
    
    /** getNextId: Returns the next identification number to be attributed
     * to a document.
     * @return the next available id (int) for a document or '0' for a first
     * element when the listwas empty.
     */
    public int getNextId()
    {
        int docNr = ((Integer) mTmpIDList.getLast()) + 1;
        mTmpIDList.add(docNr);
        return docNr;
    }
    
    /** addTopicFacet: TopicTracker adds a topical facet key related to this
     * document.
     * @param docNr : unique document number (int).
     * @param globalFacetKey : global facet key (Integer).
     */
    public void addTopicFacet(int docNr, Integer globalFacetKey)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        ds.addTopicFacet(globalFacetKey);
    }
    
    /** getAllTopicFacets: Getter returns a set with all the topical facet keys
     * linked to this document.
     * @param docNr : unique document number (int).
     * @return SortedSet with the global topical facet keys (Integer).
     */
    public SortedSet getAllTopicFacets(int docNr)
    {
        DocStats ds = (DocStats) getDocStats(docNr);
        return ds.getTopicFacets();
    }
    
    /** toString: ToString method returns the DocTable in a printable format
     * sorted on the document id  between the begin- and enddate of the scope. 
     * Format: docnr (highFreq,totTokens,totTypes - filename).
     * @return the docTable as a string.
     */
    @Override
    public String toString()
    {
        DecimalFormat format = new DecimalFormat("###,###");
        int totTokens = 0;
        String scope = ApplicationManager.getScope();
        String docTableString = "\nDocTable - Info on the files in this dataGraph"
                + " for the period: " +  GraphTime.formatScope(scope) 
                + "\nDocNr\tHighFreq TotToken TotType TotLinks -" +
                "   FileName\n";
        Object[] docNrs = getAllDocNrs();
        List docList = Arrays.asList(docNrs);
        Collections.sort(docList);
        
        for (int i = 0; i < docList.size(); i++)
        {
            String nrTemp = docList.get(i).toString();
            DocStats stats = (DocStats) getDocStats(Integer.parseInt(nrTemp));
            String statTemp = "(" + stats.getHighFrequency() + ",  " 
                    + stats.getTotTokens() + ",  "
                    + stats.getTotTypes() + ",  " + stats.getTotLinks();
            docTableString += nrTemp + '\t' + statTemp + "  -  " 
                    + stats.getFileName() + ")" + '\n';
            totTokens += stats.getTotTokens();
        }
        String tokens = format.format(totTokens);
        docTableString += "\nTotal tokens in this dataGraph: " + tokens + '\n';
        return docTableString;
    }
}
