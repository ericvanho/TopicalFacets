package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/** Class LabelTable is the central repository for all the labels used in this 
 * dataGraph. Vertices and arcs are identified by integer indices. 
 * Only when displaying data these labels are retrieved.
 * @author  Eric Van Horenbeeck
 * Created on 16 september 2004, 13:47
 */
public class LabelTable implements Serializable
{
    // SortedMap to hold the labels identifying all token-types in this datagraph.
    private final SortedMap mLabelMap;
    
    /** Constructor
     * The labelMap has one default label: the dummy vertex label 'dDummy' 
     * as key and 0 as value.
     */
    public LabelTable()
    {
        mLabelMap = new TreeMap();
        mLabelMap.put("dDummy", 0);
    }
    
    /** addLabelMap: Adds a full map with labels for this dataGraph. Key: label,
     * value: vertex index.
     * @param labelMap : map with labels (String) and their indices (Integer)
     */
    public void addLabelMap(Map labelMap)
    {
        mLabelMap.putAll(labelMap);
    }
    
    /** getLabelMap: Returns a sorted map with labels for this dataGraph.
     * Key: label (String); value: vertex-id (Integer).
     * @return labelMap
     */
    public SortedMap getLabelMap()
    {
        return mLabelMap;
    }
    
    /** getVertexIndex: Getter returns the index for this label.
     * @param label : the String identifying a token-type
     * @return the index (Integer) for this vertex
     */
    public Integer getVertexIndex(String label)
    {
        return (Integer) mLabelMap.get(label);
    }
    
    /** getVertexLabel: Getter returns the label for this vertex.
     * @param vrtxId : Integer identifying this vertex
     * @return the label (String) for this vertex
     */
    public String getVertexLabel(Integer vrtxId)
    {
        String label = "";
        Iterator label_itr = mLabelMap.keySet().iterator();
        search:
            while(label_itr.hasNext())
            {
            label = label_itr.next().toString();
            Integer index = (Integer) mLabelMap.get(label);
            if(index.equals(vrtxId)) break;
            else label = "";
            }
        return label;
    }
    
    /** isDigit: Getter returns 'true' when the label of this vertex is a digit,
     * 'false' otherwise.
     * @param vrtxId : Integer identifying this vertex
     * @return boolean 'true' when label is digit, 'false' if not.
     */
    public boolean isDigit(Integer vrtxId)
    {
        boolean digit = false;
        String tmpLabel = getVertexLabel(vrtxId);
        if(Character.isDigit(tmpLabel.charAt(0))) digit = true;
        return digit;
    }
    
    /** returnLabelList: Returns a list with labels (Strings) from a list with
     * vertex Integer indices.
     * @param idxs : ArrayList with vertex indices (Integer)
     * @return ArrayList with labels (Integer)
     */
    public List returnLabelList(List idxs)
    {
        List tmpList = new ArrayList();
        Iterator idxs_itr = idxs.iterator();
        while(idxs_itr.hasNext())
        {
            Integer key = (Integer) idxs_itr.next();
            String label = getVertexLabel(key);
            if(label.equals("dDummy")) label = "=";
            tmpList.add(label);
        }
        return tmpList;
    }
    
    /** getLastId: Returns the last identification number attributed to a 
     * token-type (label).
     * @return the last id (Integer).
     */
    public Integer getLastId()
    {
        Iterator labelId_itr = mLabelMap.values().iterator();
        List idList = new ArrayList(Algorithms.iteratorToLinkedList(labelId_itr));
        Collections.sort(idList);
        int size = idList.size();
        if(size >= 1) return (Integer) idList.get(size - 1);
        return null;
    }
    
    /** getTableSize: Returns the total number of token-types stored in this table.
     * @return the size of the labelMap (int).
     */
    public int getTableSize()
    {
        return mLabelMap.size();
    }
    
    /** isEmpty: Returns 'true' if this map has only the default 'dummy' label 
     * and is otherwise empty.
     * @return 'true' when empty, 'false' otherwise
     */
    public boolean isEmpty()
    {
        return mLabelMap.keySet().size() == 1;
    }
    
}
