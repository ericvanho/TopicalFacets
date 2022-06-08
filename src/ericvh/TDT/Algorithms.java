package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;


/** Class Algorithms has common methods used in several different classes.
 * @author  Eric Van Horenbeeck
 */
public class Algorithms implements Serializable
{
    /** Constructor
     */
    public Algorithms()
    { }
    
    /** resizeTable: Adds or removes empty rows in a 2D Object table.
     * @param data : the table with data.
     * @param cols : the number of columns in this table.
     * @param counter : the number of lines with data (int).
     * @param rowSize : the number of rows to add (int). If size = 0 unused rows
     * are removed.
     * @return the adjusted table
     */
    public static Object[][] resizeTable(Object[][] data, int cols, int counter,
            int rowSize)
    {
        Object [][] temp;
        // Expands table with room for new elements.
        if(rowSize > 0) temp = new Object[counter + rowSize][cols];
        // Removes any unused rows left when all elements are added and the 
        // table is ready.
        else temp = new Object[counter][cols];
        System.arraycopy(data, 0, temp, 0, counter);
        return temp;
    }
    
    /** addObject: Adds an object to a sorted set in an existing map.
     * @param aMap : the map to add the object to.
     * @param key : the key to the value.
     * @param newO : the object to add to the set in the map.
     * @return the map with the object added.
     */
    public Map addObject(Map aMap, Object key, Object newO)
    {
        SortedSet tmpSet = new TreeSet();
        if(aMap.containsKey(key)) tmpSet = (TreeSet) aMap.get(key);
        tmpSet.add(newO);
        aMap.put(key,tmpSet);
        return aMap;
    }
    
    /** split: Splits the content of two SortedSets in two. Both parts are 
     * returned in an ArrayList. The set with the shared elements is at index '0'
     * and the set with elements not shared by any set is at index '1'.
     * Non-destructive splitting: the original sets remain untouched.
     * @param s1 : first set
     * @param s2 : second set
     * @return an ArrayList with a SortedSet of common objects (the s2 elements
     * found in s1) at index '0' and a SortedSet with the different elements 
     * at index '1' (elements left over in set s1 and s2 after removing the 
     * common elements);
     */
    public static List split(Set s1, Set s2)
    {
        SortedSet commonElements = new TreeSet();
        SortedSet diffElements1 = new TreeSet(s1);
        SortedSet diffElements2 = new TreeSet(s2);
        List resultList = new ArrayList(2);
        Iterator s2_itr = s2.iterator();
        while (s2_itr.hasNext())
        {
            Object o2 = (Object)s2_itr.next();
            if(diffElements1.contains(o2))
            {
                // Elements common to both are collected here.
                commonElements.add(o2);
                // This set will in the end only have objects not found in set 2.
                diffElements1.remove(o2);
                diffElements2.remove(o2);
            }
        }
        resultList.add(0, commonElements);
        diffElements1.addAll(diffElements2);
        resultList.add(1, diffElements1);
        return resultList;
    }
    
    /** difference: Performs the difference of two sets. Removes all elements 
     * from s1 present in s2.
     * @param s1 : first set
     * @param s2 : second set
     * @return a HashSet with objects present in set s1 and not found in set 2.
     */
    public static Set difference(Set s1, Set s2)
    {
        Set difference = new HashSet(s1);
        Iterator s2_itr = s2.iterator();
        while (s2_itr.hasNext())
        {
            Object o = (Object)s2_itr.next();
            if(difference.contains(o)) difference.remove(o);
        }
        return difference;
    }
    
    /** intersect: Intersection of two sets with no duplicates.
     * @param s1 : first set (Object)
     * @param s2 : second set (Object)
     * @return HashSet with elements shared by both sets
     */
    public static Set intersect(Set s1, Set s2)
    {
        Set sharedObjects = new HashSet();
        Object element;
        Iterator s1_itr = s1.iterator();
        while (s1_itr.hasNext())
        {
            element = s1_itr.next();
            if (s2.contains(element)) sharedObjects.add(element);
        }
        return sharedObjects;
    }
    
    /** getSharedDocCount: The size of the intersection of two vertices gives 0
     * if the tokens never appeared together in their documents.
     * @param intersection : the documents shared by two vertices
     * @return the count of the intersection
     */
    public int getSharedDocCount(Set intersection)
    {
        return intersection.size();
    }
    
    /** iteratorToLinkedList: Constructs a LinkedList from an iterator. 
     * LinkedList guarantees that the elements keep the position they had in 
     * the original map.
     * @param itr : iterator
     * @return linkList
     */
    public static LinkedList iteratorToLinkedList(Iterator itr)
    {
        LinkedList linkList = new LinkedList();
        while(itr.hasNext()) linkList.add(itr.next());
        return linkList;
    }
    
    /** removeMapValues: Removes from a map any value smaller or equal than i.
     * @param m : map
     * @param i : the minimal treshold
     * @return map with small values removed
     */
    public Map removeMapValues(Map m, int i)
    {
        Object [] keys = m.keySet().toArray();
        Object currentKey;
        int currentWeight;
        
        for (Object key : keys)
        {
            currentKey = key;
            currentWeight = ((Integer) m.get(currentKey));
            if(currentWeight <= i) m.remove(currentKey);
        }
        return m;
    }
    
    /** inverseMap: Inverts a map: value becomes key, key becomes value. 
     * All values in the inverted map are collected in a set.
     * @param oldMap : map to be reversed.
     * @return inverted map
     */
    public Map inverseMap(Map oldMap)
    {
        Map newMap = new HashMap();
        Object currentKey;
        Object currentObject;
        Set newValueSet;
        Object [] keys = oldMap.keySet().toArray();
        for (Object key : keys)
        {
            newValueSet = new HashSet();
            currentKey = key;
            currentObject = oldMap.get(currentKey);
            if(newMap.containsKey(currentObject)) newValueSet = (Set) newMap
                    .get(currentObject);
            newValueSet.add(currentKey);
            newMap.put(currentObject, newValueSet);
        }
        return newMap;
    }
    
    /** getSortedSubTopics: Sorts values in a map en puts the keys in an array 
     * sorted on the values. The first in the array is the key to the most 
     * important value (map gets reversed).
     * @param labelAndFrequency : labels and their frequency (Map)
     * @return array with most frequent labels (String).
     */
    public String [] getSortedSubTopics(Map labelAndFrequency)
    {
        String [] subTopics;
        SortedMap sortedMap;
        Set currentSet;
        Integer currentNumber;
        Object [] sortedNumbers;
        int size;
        Iterator setIt;
        int j=0;
        String currentSubTopic;
        
        subTopics = new String[labelAndFrequency.size()];
        
        // Copies everything to a new map.
        sortedMap = valueSortedMap(labelAndFrequency);
        sortedNumbers = sortedMap.keySet().toArray();
        size = sortedNumbers.length;
        
        for(int i = 0; i < size; i++)
        {
            currentNumber = (Integer) sortedNumbers[size-i-1];
            currentSet = (Set) sortedMap.get(currentNumber);
            setIt = currentSet.iterator();
            while(setIt.hasNext())
            {
                currentSubTopic = (String) setIt.next();
                subTopics[j] = currentSubTopic;
                j++;
            }
        }
        return subTopics;
    }
    
    /** valueSortedMap: Getter returns a sorted map with key and value reversed.
     * Values with the same key are collected in a set.
     * @param m : a map
     * @return sorted map
     */
    public SortedMap valueSortedMap(Map m)
    {
        SortedMap sortedMap = new TreeMap();
        Map inverseMap = inverseMap(m);
        sortedMap.putAll(inverseMap);
        return sortedMap;
    }
    
    /** getLineSplit: Converts the comma or tab delimited content of a String 
     * into an array.
     * @param line : a String
     * @return array with the comma or tab delimited elements (String)
     */
    public static String[] getLineSplit(String line)
    {
        String posList = line;
        String[] strArray = {""};
        Pattern p = Pattern.compile("\\s*\t\\s*|\\s*,\\s*");
        try
        {
            strArray = p.split(posList);
        }
        catch(Exception e)
        {
            System.out.println("Splitting the string did not succeed");
            e.printStackTrace(System.err);
        }
        return strArray;
    }

    /** getTopicColor: Getter returns the color according to the key of the 
     * topic. Colors are available for five topic classes in the Topics class.
     * @param colorKey : selects a color (int)
     * @return String with a color indication for a graph rendering program
     */
    public String getTopicColor(int colorKey)
    {
        String color;
        color = switch (colorKey)
        {
            case 1 -> "YellowOrange";
            case 2 -> "Blue";
            case 3 -> "Yellow";
            case 4 -> "Green";
            case 5 -> "Red";
            default -> "Magenta";
        };
        return color;
    }
    
    /** getLinkColor: Getter returns the color according to the key it gets 
     * from Topics.
     * @param colorKey : selects a color (int)
     * @return String with a color indication for a graph rendering program
     */
    public String getLinkColor(int colorKey)
    {
        String color;
        color = switch (colorKey)
        {
            case 1 -> "GreenYellow";
            case 2 -> "SeaGreen";
            case 3 -> "Salmon";
            case 4 -> "Gray";
            case 5 -> "CornflowerBlue";
            case 6 -> "BurntOrange";
            case 7 -> "Emerald";
            case 8 -> "LightMagenta";
            case 9 -> "LSkyBlue";
            case 10 -> "CarnationPink";
            case 12 -> "Apricot";
            case 13 -> "Thistle";
            case 14 -> "Maroon";
            default -> "Pink";
        };
        return color;
    }
}
