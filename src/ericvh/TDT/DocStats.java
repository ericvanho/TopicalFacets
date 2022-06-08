package ericvh.TDT;

import java.io.Serializable;
import java.util.Calendar;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class DocStats instances information on a document: the highest frequency 
 * of a token, the total number of tokens, total number of token-types, 
 * the total number of links and the full path name to the file. 
 * The TopicTracker adds sets of topical facets this document has a part in. 
 * DocStats are stored in a DocTable.
 * @author  Eric Van Horenbeeck
 * Created on 26 juli 2004, 17:56
 * Revision: July 7, 2006
 */
public class DocStats implements Serializable
{
    // The score of the token with the highest frequency.
    private final int mHighestFreq;
    // The total number of 'words' in a document.
    private final int mTotToken;
    // The total number of different token-types in a doc.
    private final int mTotType;
    // The total number of incoming and outgoing links for all tokens.
    private int mTotLinks;
    // The maximum number of links belonging to one token.
    private int mMaxLinks;
    // The full path name of the document.
    private final String mDocPath;
    // Set with all the topical facets this document is involved in.
    private final SortedSet mTopicSet;
    // System separator.
    private final String separator = System.getProperty("file.separator");
    
    /** Constructor
     * @param freq : the highest frequency of a token encountered in this doc (int)
     * @param tokens : the total number of tokens (raw 'words') in this doc (int)
     * @param types : the total number of types (same tokens) in this doc (int)
     * @param totLinks : the total number of incoming and outgoing lins in this doc (int).
     * @param docPath : the full pathname of this document (String)
     */
    public DocStats(int freq, int tokens, int types, int totLinks, String docPath)
    {
        mHighestFreq = freq;
        mTotToken = tokens;
        mTotType = types;
        mTotLinks = totLinks;
        mMaxLinks = 0;
        mDocPath = docPath;
        mTopicSet = new TreeSet();
    }
    
    /** getTotTypes: Getter of the number of different token-types in this document.
     * @return total types (int)
     */
    public int getTotTypes()
    {
        return mTotType;
    }
    
    /** getHighFrequency: Getter of the highest frequency of a token in this document.
     * @return highest frequency of any token in this document (int)
     */
    public int getHighFrequency()
    {
        return mHighestFreq;
    }
    
    /** getTotTokens: Getter of the number of tokens in this document.
     * @return total tokens (int)
     */
    public int getTotTokens()
    {
        return mTotToken;
    }
    
    /** getPathName: Getter of the full path name of this document.
     * @return path with filename (String)
     */
    public String getPathName()
    {
        return mDocPath;
    }
    
    /** getFileName: Getter of the filename of this document with path and 
     * suffix stripped off. In the case this document is a seedFile (document 
     * introduced by SeedInput to perform a document retrieval exercise) 
     * a 'NOPATH'-string is returned.
     * @return filename (String)
     */
    public String getFileName()
    {
        if(mDocPath.equals("seedDoc")) return "SEEDDOC";
        else return mDocPath.substring(mDocPath.lastIndexOf(separator) + 1,
                mDocPath.lastIndexOf('.'));
    }
    
    /** getDate: Getter of the date of this document as recorded in the filename.
     * @return date of this document in Calendar format
     */
    public Calendar getDate()
    {
        Calendar date = Calendar.getInstance();
        date.set(0000, 00, 00);
        return GraphTime.getDate(date, getFileName());
    }
    
    /** getTotLinks: Returns the total number of links (incoming and outgoing)
     * for all tokens in this document.
     * @return the number of links (int). Is '0' at creation when the number
     * has not been updated by the LinkTable.
     */
    public int getTotLinks()
    {
        return mTotLinks;
    }
    
    /** setTotLinks: Updates the number of links (incoming and outgoing) 
     * for all tokens in this document.
     * @param totLinks : the sum of all links in this doc (int).
     */
    public void setTotLinks(int totLinks)
    {
        mTotLinks = totLinks;
    }
    
    /** setMaxLinks: Sets the highest link count for one token in this document.
     * @param maxLinks : the highest link-count found for one token (int).
     */
    public void setMaxLinks(int maxLinks)
    {
        mMaxLinks = maxLinks;
    }
    
    /** getMaxLinks: Returns the highest link count for one token in this document.
     * @return the highest single link count (int) seen in this document.
     */
    public int getMaxLinks()
    {
        return mMaxLinks;
    }
    
    /** addTopicFacet: TopicTracker sends a gobal topical facet key linked
     * to this document. It's a set there will be no duplicates.
     * @param facetKey : global facet keys (Integer).
     */
    public void addTopicFacet(Integer facetKey)
    {
        mTopicSet.add(facetKey);
    }
    
    /** getTopicFacets: Returns the set of all topical facet keys where this 
     * document plays a part.
     * @return SortedSet with global topical facet keys (Integer).
     */
    public SortedSet getTopicFacets()
    {
        return mTopicSet;
    }
    
}
