package ericvh.TDT;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;


/** Class Consolidator reads and writes the data into nine separate files based 
 * on the first digit of the vertex-id. Keeping all the vertex-facet sets in one
 * file yields a voluminous file that is difficult to handle in the system memory. 
 * Class FacetCollector assembles the vertex-facet maps and TopicRetriever uses 
 * them to find semantic information about a document.
 * @author Eric Van Horenbeeck
 * Created on December 20, 2005, 5:34 PM
 */
public class Consolidator
{
    // Reading and writing methods.
    private static final InputOutput io = new InputOutput();
    // Path to the global Vertex-Facet files.
    private static String mGlobalPath;
    // Remembers the index actually in use.
    private int mIndexInUse = -1;
    // The number of tokens to look up in the repositories.
    private int mTotalTokens;
    // The start of the application.
    private long mStart;
    // The map with facet data linked to a set of vertices.
    private SortedMap mFacetMap;
    private static final String separator = System.getProperty("file.separator");
    
    /** Constructor
     */
    public Consolidator()
    {  }
    
    /** finalizeVertexFacetMaps: Reads the available vertex-facet maps from disk.
     * The SortedMaps have a vertex-id (Integer) as key and a HashSet of related
     * general topical facet keys (Integer) as value. When all the local maps 
     * are processed the nine different collecting maps from this session are 
     * merged with their corresponding global vertex-facet maps that are stored 
     * on disk as nine separate repositories.
     * @throws IOException
     */
    public static void finalizeVertexFacetMaps() throws IOException
    {
        SortedMap globalMap;
        mGlobalPath = ApplicationManager.getGlobalPath();
        int count = -1;
        try
        {
            for (int i = 0; i < 9; i++)
            {
                String vrtxFilePath = mGlobalPath + separator + "VrtxFacet_" 
                        + i + ".fct";
                File vrtxFile = new File(vrtxFilePath);
                if(vrtxFile.exists()) globalMap = io.readSortedMap(vrtxFilePath);
                else globalMap = new TreeMap();
                SortedMap newGlobalMap = FacetCollector
                        .updateVertexFacetMaps(globalMap, i);
                // Saves the updated global vertex-facet map to disk.
                io.writeThisObject(vrtxFilePath, newGlobalMap);
                globalMap.clear();
                count++;
            }
        }
        catch(Exception e)
        {
            getMessage(0, 0, "", "", 0);
            ApplicationManager.updateStatusBar(-1, "");
        }
        getMessage(3, 0, "", "", count);
    }
    
    /** retrieveVertexFacetMap: Retrieves a vertex-facet file from disk. 
     * When this file was retrieved before, it is kept in memory and is handed
     * down directly. Files no longer in demand are discarded.
     * @param index : the identification of the vertex-facet file based on the 
     * first digit of the vertex key (int).
     * @param key : the key (String) of a vertex from the seed document
     * (the 'query).
     * @param count : the number of tokens left to process.
     * @return a SortedMap with vertex-keys (Integer) as key and a SortedSet 
     * of global facet keys (Integer) as value.
     */
    public SortedMap retrieveVertexFacetMap(int index, String key, int count)
    {
        // Text formating string.
        String tab = "\t\t";
        // If this index was used before, the facet map in memory is returned.
        if(mIndexInUse == index)
        {
            getMessage(1, index, key, tab, count);
            return mFacetMap;
        }
        // If the index is new, the corresponding facet map is retrieved from
        // disk.
        else
        {
            mFacetMap = new TreeMap();
            mIndexInUse = index;
            try
            {
                String vrtxFilePath = mGlobalPath + separator + "VrtxFacet_" 
                        + index + ".fct";
                mFacetMap = io.readSortedMap(vrtxFilePath);
            }
            catch(Exception e)
            {
                getMessage(0, 0, "", "", 0);
                ApplicationManager.updateStatusBar(-1, "");
            }
        }
        if(Integer.parseInt(key) > 9999) tab = "\t";
        getMessage(2, index, key, tab, count);
        return mFacetMap;
    }
    
    /** setTotal: TopicRetriever sends the total number of token-types to look
     * up and the starting time of the application.
     * @param appStart : the start moment of this session in milliseconds(long).
     * @param total : the total number of token-types (int).
     */
    public void setTotal(int total, long appStart)
    {
        mTotalTokens = total;
        mStart = appStart;
    }
    
    /** getMessage: Returns a message on the GUI indicating the progress of the 
     * data extraction.
     * @param msge : a message identification (int).
     * @param index : the identification of the vertex-facet file based on the 
     * first digit of the vertex key (int).
     * @param key : the key (String) of a vertex from the seed document (the 'query).
     * @param tab : string with one or more tabs to format the string in 
     * the message area.
     * @param count : the number of tokens left to process.
     */
    private static void getMessage(int msge, int index, String key, String tab, 
            int count)
    {
        switch(msge)
        {
            case 0 -> ApplicationManager.showText("* Vertex-Facet Map retrieval"
                    + " failed *", 0);
            case 1 -> ApplicationManager.showText("Using repository # " + index 
                    + " for vertex " +
                    key + tab + "Token-types left to process: " + count 
                    +  "\tTime elapsed: " +
                    ApplicationManager.getElapsedTime(), 0);
            case 2 -> ApplicationManager.showText("Repository # " + index 
                    + " retrieved for vertex " + key + tab + "Token-types left "
                            + "to process: " + count + "\tTime elapsed: " +
                    ApplicationManager.getElapsedTime(), 0);
            case 3 -> ApplicationManager.showText("\n" + count + " Vertex-Facet"
                    + " maps retrieved and updated.", 0);
        }
    }
    
}
