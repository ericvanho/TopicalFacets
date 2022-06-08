package ericvh.TDT;

import java.util.*;
import java.util.regex.Pattern;

/** Class Neighbors looks for a token to the left and to the right of a target word.
 * @author  Eric Van Horenbeeck
 * Created on 29 mei 2004, 18:55
 */

public class Neighbors
{
    private final Map detailMap;   // Exact postion of a token in a text
    private Map positionMap; // List of all wordpositions as key and the 
    //corresponding word as value
    private List positions;
    private String[] neighborList;
    private final int depth = 1;  // Number of levels to search for neighbors 
    //(of neighbors...)
    private static final Pattern pttrn = Pattern.compile("\\s*,\\s*");
    
     /**  Constructor
     */
    public Neighbors()
    {
        detailMap = new HashMap();
        positionMap = new HashMap();
    }
    
    /** Returns the indices of the neighbors at the left and the right of a word, 
     * given a list of positions of a word.
     * @param word : the word that wants to know its neighbors
     * @param map : the map of all words and its positions in a text
     * @return a list with the neighbors of a word
     */
    public String[] getNeighbors(String word, Map map)
    {
        String temp;
        positionMap = makePositionMap(map);
        try
        {
           // Retrieve the string with the word occurrences
            temp = map.get(word).toString(); 
        }
        catch (NullPointerException ne)
        {
            System.out.println("Word not found in this text");
            String[] thisNeighborList = {"no such word"};
            return thisNeighborList;
        }
        return makeNeighborList(temp);
    }
    
    /** Converts wordMap into a position map with as key = position, value = word
     * @param wordMap the word map
     * @return m the position map
     */
    private Map makePositionMap(Map wordMap)
    {
        Map m = new HashMap();
        List types = new ArrayList(wordMap.keySet());
        ListIterator iterator = types.listIterator();
        while(iterator.hasNext())
        {
            Object elem = iterator.next();
            m.putAll(splitPositions(elem.toString(),(wordMap.get(elem)).toString()));
        }
        positions = new ArrayList(m.keySet());
        return m;
    }
    
    /** Searches for the occurrences of a word and put its neighbors in a list
     * @param strList : the positions of one word
     * @return neighborList all the neighbors 
     */
    private String[] makeNeighborList(String strList)
    {
        String[] temp;
        // Get array with the positions to search for
        String[] posList = getDetailArray(strList); 
        // Array with neighbors to be returned
        neighborList = new String[posList.length * depth]; 
        
        for(int i= 0; i < posList.length; i++)
        {
            Object pos = Integer.valueOf(posList[i]);
            int idx = Collections.binarySearch(positions, pos);
            String item = positionMap.get(positions.get(idx)).toString();
            temp = getNeighbors(idx);           
            neighborList[i] = temp[0] + " - " + item + " - " + temp[1];
        }
        return neighborList;
    }
    
    /** Looks up the word to the left and the right of this index
     * The first and last words have no left or right neighbors, this is indicated
     * with a '*'
     * @param idx : the index of a word position in the text
     * @return a string-array with left and right neighbors
     */
    private String[] getNeighbors(int idx)
    {
        String item1 = "*"; String item2 = "*";
        String[] neighbors = new String[2];        
        
        if(idx - 1 > 0)
        {   // Neighbor to the left
            item1 = positionMap.get(positions.get(idx-1)).toString() ;
        }
        if(idx + 1 < positionMap.size()) 
        {   // Neighbor to the right
            item2 = positionMap.get(positions.get(idx+1)).toString();
        }
        neighbors[0] = item1;
        neighbors[1] = item2;
        return neighbors;
    }
    
    /** Converts the comma delimited content of a string into a string array
     * @param list : the occurrences of a word as one string
     * @return array with the splitted positions of a word in a text
     */
    public static String[] getDetailArray(String list)
    {
        String posList = list;
        String[] strArray = {""};
        try
        {
            strArray = pttrn.split(posList); // pttrn is a regex pattern
        }
        catch(Exception e)
        {
            System.out.println("Not a valid list in Neighbors");
            e.printStackTrace(System.err);
        }
        return strArray;
    }
    
    /** Makes a map with the position(s) in the doc as key and the input word as value
     * @param word : value in the postion map
     * @param list : list of all the occurrences of this word
     * @return the detailMap
     */
    private Map splitPositions(String word, String list)
    {
        String[] detList = getDetailArray(list);
        for (String detList1 : detList) {
            detailMap.put(Integer.valueOf(detList1), word);
        }
        return detailMap;
    }
}
