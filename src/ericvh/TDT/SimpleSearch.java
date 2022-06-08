package ericvh.TDT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class SimpleSearch performs a search for documents based on one token.
 * @author Eric Van Horenbeeck
 * Created on June 7, 2006, 5:33 PM
 */
public class SimpleSearch
{
    // Map with doc-ids and a set with common facets.
    private final LinkedHashMap mFacetDocMap;
    //
    private SortedSet mDocIdSet;
    //
    private final DocTable mDocTable;
    
    /** Constructor
     * @param dt docTable instance
     * @param facetDocMap : LinkedHashMap with as key the count class and as value
     * a HashMap with the global facet key and  as value a SortedSet with all
     * document-ids (Integer) related to this task.
     */
    public SimpleSearch(LinkedHashMap facetDocMap, DocTable dt)
    {
        mFacetDocMap = new LinkedHashMap(facetDocMap);
        mDocTable = dt;
        System.out.println("Simple Text Search");
    }
    
    /** prepareFileSet: Prepares a set with the full pathnames of the files 
     * necessary to compute in the next step the doc-by-doc similarity by an 
     * instance the DocSimilarity class.
     * @param docList : an ArrayList with ArrayLists with doc-ids (Integer) 
     * and arcs (String) selected by the DocRetrieval class.
     * @return LinkedHashSet with full filepaths (String) to perform the 
     * doc-by-doc similarity.
     * @throws IOException
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
            ApplicationManager.setGraphStoreName(ApplicationManager.
                    getDataStoreArg(0, txtGrphFile));
            String thisGraphPath = ApplicationManager.getGraphPath();
            String fullTextGraphPath = thisGraphPath + txtGrphFile;
            docSet.add(fullTextGraphPath);
        }
        return docSet;
    }
}
