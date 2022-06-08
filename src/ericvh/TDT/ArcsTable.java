package ericvh.TDT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Class ArcsTable adds all arcs found in this dataGraph. It can search the 
 * arcsmap with indices, document numbers or vertices. Arcsmap set info weights 
 * on arcs and helps the linkTable in counting incoming and outgoing links to vertices.
 * @author  Eric Van Horenbeeck
 * Created on 16 september 2004, 15:16
 */
public class ArcsTable implements Serializable
{
    // Map with arc index as key and the arc as value.
    private Map mArcsMap;
    // Map with as key: vertex index (Integer) and as value a vertex.
    private final Map mVrtxMap;
    
    /** Constructor.
     * @param vt : an instance of VerticesTable, holding the vertices for the 
     * arcs in this table
     */
    public ArcsTable(VerticesTable vt)
    {
        mVrtxMap = vt.getFullVerticesMap();
    }
    
    /** addFullArcsMap: Adds the full map with all arcs over all documents. 
     * Key: docNr, value: an arcsMap.
     * @param am : HashMap with doc-id (Integer) as key and a HashMap as value 
     * with an arcKey(String) as key and an Arc as value.
     */
    public void addFullArcsMap(Map am)
    {
        mArcsMap = new HashMap();
        mArcsMap.putAll(am);
    }
    
    /** containsArcIndex: Returns true if the arc map contains an element 
     * identified by this index.
     * @param arcIdx : the arc index (String).
     * @return boolean 'true' if map contains this arc.
     */
    public boolean containsArcIndex(String arcIdx)
    {
        return mArcsMap.containsKey(arcIdx);
    }
    
    /** getArcKey: Returns the key of this arc in this document.
     * @param docNr : the unique document identifier (int).
     * @param arc : Arc being processed.
     * @return the arc key (String).
     */
    public String getArcKey(int docNr, Arc arc)
    {
        Integer docInt = docNr;
        Arc currentArc;
        String key = "";
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        Iterator arc_itr = tmpMap.keySet().iterator();
        search:
            while(arc_itr.hasNext())
            {
            key = arc_itr.next().toString();
            currentArc = (Arc) mArcsMap.get(key);
            if(arc.equals(currentArc)) break;
            else key ="";
            }
        return key;
    }
    
    /** getAllArcKeysInDoc: Returns a set with all the arc keys in this document.
     * @param docInt : the unique document identifier (Integer).
     * @return HashSet with arc keys (String) from this document.
     */
    public Set getAllArcKeysInDoc(Integer docInt)
    {
        Set arcKeySet = new HashSet();
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        Iterator arc_itr = tmpMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            arcKeySet.add(arc_itr.next().toString());
        }
        return arcKeySet;
    }
    
    /** getArcKeyList: Returns a list with the arc keys associated with these arcs.
     * @param arcs : ArrayList with arcs (Arc).
     * @param docNr : the unique document identifier (int)
     * @return ArrayList with arc keys (String).
     */
    public List getArcKeyList(List arcs, int docNr)
    {
        List arcKeyList = new ArrayList();
        Iterator arc_itr = arcs.iterator();
        while(arc_itr.hasNext())
        {
            Arc a = (Arc) arc_itr.next();
            arcKeyList.add(getArcKey(docNr, a));
        }
        return arcKeyList;
    }
    
    /** getArcWithKey: Getter returns the arc identified by this arc-index in 
     * this document.
     * @param docNr : the unique document identifier (int).
     * @param arcIdx : the arc index (String).
     * @return an arc (Arc) identified by its index.
     */
    public Arc getArcWithKey(int docNr, String arcIdx)
    {
        Integer docInt = docNr;
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        return (Arc) tmpMap.get(arcIdx);
    }
    
    /** getAllArcIndices: Getter of all arc keys in this arcsMap over all
     * documents in every Collection.
     * @return ArrayList with all the arc keys (String) attributed so far.
     */
    public List getAllArcIndices()
    {
        List tmpArray = new ArrayList();
        Iterator arc_itr = mArcsMap.values().iterator();
        while( arc_itr.hasNext())
        {
            Map tmpMap = (HashMap) arc_itr.next();
            tmpArray.addAll(tmpMap.keySet());
        }
        return tmpArray;
    }
    
    /** isEmpty: Checks whether this arcsTable has any elements.
     * @return boolean 'true' if empty.
     */
    public boolean isEmpty()
    {
        return mArcsMap.isEmpty();
    }
    
    /** getArcsMapSize: Getter counts the number of arcs in this arcsTable.
     * @return size of the arc map (int).
     */
    public int getArcsMapSize()
    {
        return getAllArcIndices().size();
    }
    
    /** getFullArcsMap: Getter returns all the arcs in this graph (all documents).
     * Key: docNr, value: an arcsMap. Structure of the arcsMap: HashMap with 
     * arc index as key (String) and the arc (Arc) as value.
     * @return HashMap with arcs (directed edges) collected per document.
     */
    public Map getFullArcsMap()
    {
        return mArcsMap;
    }
    
    /** makeArcKey: Makes the key of an arc with the indices of the two 
     * connecting vertices.
     * @param vrtxIndx1 : first (left) point of the arc (int).
     * @param vrtxIndx2 : second (right) point of the arc (int).
     * @return key (String)
     */
    private String makeArcKey(int vrtxIndx1, int vrtxIndx2)
    {
        String key = vrtxIndx1 + "*" + vrtxIndx2;
        return key;
    }
    
    /** getArc: Getter returns an arc in this graph identified by its vertices 
     * or null.
     * @param docNr : the unique document identifier (int).
     * @param vrtxIndx1 : index to the vertex left of the arc (int).
     * @param vrtxIndx2 : index to the vertex right of the arc (int).
     * @return Arc if found or null.
     */
    public Arc getArc(int docNr, int vrtxIndx1, int vrtxIndx2)
    {
        Integer docInt = docNr;
        String key = makeArcKey(vrtxIndx1, vrtxIndx2);
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        if(tmpMap.containsKey(key)) return ((Arc) tmpMap.get(key));
        else return null;
    }
    
    /** getArcList: Returns a list with arcs from a set with arc keys.
     * @param arcKeys : set with arc keys (String).
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return ArrayList with the arcs (Arc) linked to these keys.
     */
    public List getArcList(Set arcKeys, Integer docInt)
    {
        List arcList = new ArrayList();
        Map arcsMap = (HashMap) mArcsMap.get(docInt);
        // Fills a list with the arcs retrieved by their keys.
        Iterator key_itr = arcKeys.iterator();
        while(key_itr.hasNext()) arcList.add(arcsMap.get(key_itr.next().toString()));
        return arcList;
    }
    
    /** getVertex1Key: Getter returns the index of the first vertex of an arc.
     * @param a : Arc in view.
     * @return the index of the left vertex linked by this arc (Integer).
     */
    public Integer getVertex1Key(Arc a)
    {
        return a.getVertex1Index();
    }
    
    /** getVertex2Key: Getter returns the index of the second vertex of an arc.
     * @param a : Arc in view.
     * @return the index of the right vertex linked by this arc (Integer).
     */
    public Integer getVertex2Key(Arc a)
    {
        return a.getVertex2Index();
    }
    
    /** getLeftArc: Returns the arc that has a vertex with this key at 
     * its left position.
     * @param docNr : the unique document number (int).
     * @param leftKey : the key of a vertex (int).
     * @return Arc having a vertex with this key at its left position.
     */
    public Arc getLeftArc(int docNr, int leftKey)
    {
        Arc thisArc = null;
        List arcs = getArcsInDoc(mArcsMap, docNr);
        // Sorts the arcs.
        List sortedArcs = sortArcsOnPosition(arcs);
        // Adds the indices to a set, no duplicates here.
        Iterator arcs_itr = sortedArcs.iterator();
        search:
            while (arcs_itr.hasNext())
            {
            thisArc = (Arc) arcs_itr.next();
            if(thisArc.containsIndexFirst(leftKey)) break;
            }
        return thisArc;
    }
    
    /** getVertexIndex: Getter returns the index that identifies this vertex.
     * @param v : this vertex (Vertex).
     * @return the index of this vertex (Integer).
     */
    private Integer getVertexIndex(Vertex v)
    {
        Integer key = null;
        Iterator vertx_itr = mVrtxMap.keySet().iterator();
        search:
            while(vertx_itr.hasNext())
            {
            key = (Integer) vertx_itr.next();
            if(mVrtxMap.get(key).equals(v))
            {
                break;
            }
            else key = null;
            }
        return key;
    }
    
    /** adjustThisArcWeight: The arcWeight of this arc is the average info value
     * of its vertices. It is adjusted for this arc when all files are processed
     * and when the info value of its constituting vertices is calculated.
     * @param a : this Arc.
     */
    public void adjustThisArcWeight(Arc a)
    {
        String collKey = a.getArcCollection();
        double value1 = 0.0;
        double value2 = 0.0;
        Vertex v1;
        Vertex v2;
        Integer key1 = getVertex1Key(a);
        Integer key2 = getVertex2Key(a);
        if(key1 > 0)
        {
            v1 = (Vertex) mVrtxMap.get(key1);
            value1 = v1.getInfoValue(collKey);
        }
        if(key2 > 0)
        {
            v2 = (Vertex) mVrtxMap.get(key2);
            value2 = v2.getInfoValue(collKey);
        }
        // The info value of the arc is the sum of the info value of both its 
        // vertices.
        a.addWeight((value1 + value2));
    }
    
    /** adjustAllArcWeights: Adjusting in one step the weight (info value) of 
     * all the arcs in this graph with the info weight of the two constituting
     * vertices of the arc. Called by the GraphAnalyser after collecting infoArcs.
     */
    public void adjustAllArcWeights()
    {
        Arc currentArc;
        // Iterates over all arcs to adjust their weight
        Iterator arcs_itr = mArcsMap.keySet().iterator();
        while(arcs_itr.hasNext())
        {
            Integer thisDocNr = (Integer) arcs_itr.next();
            Map tmpMap = (HashMap) mArcsMap.get(thisDocNr);
            Iterator tmpArc_itr = tmpMap.values().iterator();
            while(tmpArc_itr.hasNext())
            {
                currentArc = (Arc) tmpArc_itr.next();
                adjustThisArcWeight(currentArc);
            }
        }
    }
    
    /** summedArcWeights: Returns the summed weight (info value) of the 
     * non-zero arcs from a set of arc keys.
     * @param arcSet : a set with arc-keys (String) from a document.
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return the summed info value of the non-zero arcs (double).
     */
    public double summedArcWeights(Set arcSet, Integer docInt)
    {
        double weight = 0;
        Iterator arc_itr = arcSet.iterator();
        while(arc_itr.hasNext())
        {
            String arcKey = arc_itr.next().toString();
            weight += getArcValue(arcKey, docInt);
        }
        return weight;
    }
    
    /** summedVertexWeights: Returns the summed weight (info value) of 
     * the non-zero vertices.
     * @param vertexSet : a set with vertex-ids (Integer) from a document.
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return the summed info value of the non-zero vertices (double).
     */
    public double summedVertexWeights(Set vertexSet, Integer docInt)
    {
        double weight = 0;
        Iterator arc_itr = vertexSet.iterator();
        while(arc_itr.hasNext())
        {
            Integer vertexKey = (Integer) arc_itr.next();
            weight += getVertexValue(vertexKey, docInt);
        }
        return weight;
    }
    
    /** getArcValue: Returns the value of this arc.
     * @param arcKey : the key to an arc (String)
     * @param docInt : the document-id of the arcs-set (Integer).
     * @return the value associated with this arc (double).
     */
    public double getArcValue(String arcKey, Integer docInt)
    {
        Arc thisArc = getArcWithKey(docInt, arcKey);
        return thisArc.getArcWeight();
    }
    
    /** getArcListIfContains: Returns list with arc(s) with one or both 
     * of these vertices in this document.
     * @param docNr : the unique document identifier (int).
     * @param v1 : the first vertex (Vertex).
     * @param v2 : the second vertex (Vertex).
     * @return ArrayList with arc(s) (Arc) having one or both of these 
     * vertices as element.
     */
    public List getArcListIfContains(int docNr, Vertex v1, Vertex v2)
    {
        Integer docInt = docNr;
        int v1Idx = -1;
        int v2Idx = -1;
        if(mVrtxMap.containsValue(v1)) v1Idx = getVertexIndex(v1);
        if(mVrtxMap.containsValue(v2)) v2Idx = getVertexIndex(v2);
        List arcList = new ArrayList();
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        Iterator arc_itr = tmpMap.values().iterator();
        while(arc_itr.hasNext())
        {
            Arc currentArc = (Arc) arc_itr.next();
            if(currentArc.containsVertex(v1Idx) || currentArc.containsVertex(v2Idx))
                arcList.add(currentArc);
        }
        return arcList;
    }
    
    /** containsVertex: Returns 'true' if this infoArcTable contains an 
     * informative arc composed with an informative vertex.
     * @param vertexKey : the key (Integer) to a vertex.
     * @return boolean 'true' is this key is a member of this table, 'false' otherwise.
     */
    public boolean containsVertex(Integer vertexKey)
    {
        return mVrtxMap.containsKey(vertexKey);
    }
    
    /** getVertexValue: Returns the infovalue of the first vertex that matches 
     * this key. Method to be used only in the case of a document retrieval 
     * where the vertex comes from an external document not seen by this 
     * application before and a suitable replacement value has to be found.
     * @param vertexKey : the key (Integer) to a vertex.
     * @param docNr : the unique document identifier (int).
     * @return informative value of this vertex (double).
     */
    public double getVertexValue(Integer vertexKey, int docNr)
    {
        double value = 0.0;
        if(containsVertex(vertexKey))
        {
            // Searches for a suitable arcKey in the document collection of 
            // the arcsMap.
            Iterator doc_itr = mArcsMap.keySet().iterator();
            search:
                while(doc_itr.hasNext())
                {
                Integer docInt;
                // Proceeds directly to the right document if the docId is known,
                // else iterates.
                if(docNr > 0) docInt = docNr;
                else docInt = (Integer) doc_itr.next();
                Map allArcs = (HashMap) mArcsMap.get(docInt);
                Iterator arc_itr = allArcs.keySet().iterator();
                while(arc_itr.hasNext())
                {
                    String arcKey = arc_itr.next().toString();
                    // Adapts the arcKey string so that the pattern matcher is 
                    // sure to find only the full key and not a similar digit 
                    // sequence inside a larger number.
                    String arcString = "*" + arcKey  + "*";
                    // Regex pattern: the key between two nondigit characters.
                    String key = "\\D" + vertexKey.toString() + "\\D";
                    Matcher match = Pattern.compile(key).matcher(arcString);
                    if(match.find())
                    {
                        // Retrieves the arc value if this arc contains the 
                        // target key. The arc value is half the value of its 
                        // two constituent vertices.
                        value = getArcValue(arcKey, docInt);
                        break search;
                    }
                }
                }
        }
        return value;
    }
    
    /** getArcsWithKey: Returns list with arc(s) having this vertex in this document.
     * @param docInt : the unique document identifier (Integer).
     * @param vKey : the vertex key (int)?
     * @return ArrayList with arc(s) having this vertex.
     */
    public List getArcsWithKey(Integer docInt, int vKey)
    {
        List arcList = new ArrayList();
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        Iterator arc_itr = tmpMap.values().iterator();
        while(arc_itr.hasNext())
        {
            Arc currentArc = (Arc) arc_itr.next();
            if(currentArc.containsVertex(vKey)) arcList.add(currentArc);
        }
        return arcList;
    }
    
    /** getArcPositions: Returns a sorted map with the positions of the arcs 
     * in this doc.
     * @param docInt : the unique document identifier (Integer).
     * @return SortedMap with arc position (int) as key and the arc (Arc) as value.
     */
    public SortedMap getArcPositions(Integer docInt)
    {
        SortedMap arcPositionMap = new TreeMap();
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        if(tmpMap == null) return null;
        Iterator arc_itr = tmpMap.values().iterator();
        while(arc_itr.hasNext())
        {
            Arc currentArc = (Arc) arc_itr.next();
            arcPositionMap.put(currentArc.getArcPosition(), currentArc);
        }
        return arcPositionMap;
    }
    
    /** getArcOnPosition: Returns the arc that sits on this position in 
     * this document.
     * @param docInt : the unique document identifier (Integer).
     * @param position : location in the text of this arc (int).
     * @return Arc found on this position in this document or 'null'.
     */
    public Arc getArcOnPosition(Integer docInt, int position)
    {
        SortedMap arcs = getArcPositions(docInt);
        Iterator pos_itr = arcs.keySet().iterator();
        while(pos_itr.hasNext())
        {
            int thisPosition = ((Integer) pos_itr.next());
            if(thisPosition == position) return (Arc) arcs.get(thisPosition);
        }
        return null;
    }
    
    /** removeArc: Removes from this table this arc identified by its key.
     * @param docNr : the unique document identifier (int).
     * @param a : Arc to remove.
     */
    public void removeArc(int docNr, Arc a)
    {
        Integer docInt = docNr;
        String key = getArcKey(docNr, a);
        Map tmpMap = (HashMap) mArcsMap.get(docInt);
        tmpMap.remove(key);
    }
    
    /** removeArcs: Removes from this graph a list of arcs.
     * @param arcMap : HashMap with arcs (Arc) to remove.
     */
    public void removeArcs(Map arcMap)
    {
        Arc currentArc;
        Iterator map_itr = arcMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            Integer docInt = (Integer) map_itr.next();
            List arcList = (ArrayList) arcMap.get(docInt);
            Iterator arcs_itr = arcList.iterator();
            while(arcs_itr.hasNext())
            {
                currentArc = (Arc) arcs_itr.next();
                removeArc(docInt, currentArc);
            }
        }
    }
    
    /** getArcsInDoc: Returns all the arcs contained in this document 
     * in this graph.
     * @param docNr : the unique document number (int).
     * @param arcsMap : HashMap with all the arcs (Arc) of this dataGraph.
     * @return ArrayList with arcs (Arc) in this document.
     */
    public List getArcsInDoc(Map arcsMap, int docNr)
    {
        Integer docInt = docNr;
        List arcsInDoc = new ArrayList();
        Map tmpMap = (HashMap)arcsMap.get(docInt);
        Iterator arcs_itr = tmpMap.values().iterator();
        while (arcs_itr.hasNext())
        {
            Arc currentArc = (Arc) arcs_itr.next();
            arcsInDoc.add(currentArc);
        }
        return arcsInDoc;
    }
    
    /** getArcsCountInDoc: Returns the number of arcs in this document.
     * @param docInt : the unique document number (Integer).
     * @return the number of arcs in this document (int).
     */
    public int getArcsCountInDoc(Integer docInt)
    {
        Map tmpMap = (HashMap)mArcsMap.get(docInt);
        return tmpMap.keySet().size();
    }
    
    /** getVertexIds: Delivers a set with all the non-zero vertex-ids retrieved
     * from this set of arc-keys.
     * @param arcKeys : HashSet with arc-keys (String).
     * @param docInt : the unique document number (Integer).
     * @return HashSet with vertex-ids (Integer) in this document.
     */
    public Set getVertexIds(Set arcKeys, Integer docInt)
    {
        Set vertexIdSet = new HashSet();
        Iterator aKeys_itr = arcKeys.iterator();
        while (aKeys_itr.hasNext())
        {
            String currentArcKey = aKeys_itr.next().toString();
            Arc thisArc = getArcWithKey(docInt, currentArcKey);
            vertexIdSet.add(thisArc.getVertex1Index());
            vertexIdSet.add(thisArc.getVertex2Index());
        }
        try
        {
            vertexIdSet.remove(0);
        }
        catch(NullPointerException npe)
        {
            // No action necessary when the element '0' is not in the set.
        }
        return vertexIdSet;
    }
    
    /** connectArcs: Returns a linked set with set(s) of connected arcs inside
     * a document. An arc is connected to another arc when there is no more than
     * one point distance between them. Only walks with a given minimal length 
     * are considered.
     * @param arcKeys : HashSet with arc keys (String).
     * @param docInt : the unique document number (Integer).
     * @param minLength : the minimal acceptable dimension of the walk (int).
     * @return LinkedSet with LinkedSet(s) of arcs (Arc).
     */
    public LinkedHashSet connectArcs(Set arcKeys, Integer docInt, int minLength)
    {
        // Retrieves arcs with their keys and sorts them on their position in the text.
        List arcList = sortArcsOnPosition(getArcList(arcKeys, docInt));
        int prevPosition = -1;
        Arc prevArc = new Arc();
        LinkedHashSet connectedSet = new LinkedHashSet();
        LinkedHashSet arcWalk = new LinkedHashSet();
        Iterator arc_itr = arcList.iterator();
        while(arc_itr.hasNext())
        {
            Arc thisArc = (Arc) arc_itr.next();
            int position = thisArc.getArcPosition();
            if(position - prevPosition == 1)
            {
                arcWalk.add(prevArc);
                arcWalk.add(thisArc);
            }
            else
            {
                // Walks shorter than the minimal length are thrown away.
                if (arcWalk.size() >= minLength) connectedSet.add(arcWalk);
                arcWalk = new LinkedHashSet();
            }
            prevPosition = position;
            prevArc = thisArc;
        }
        if (arcWalk.size() >= minLength) connectedSet.add(arcWalk);
        return connectedSet;
    }
    
    /** sortArcsOnPosition: Sorts a list of arcs on their position in the text.
     * @param arcs : ArrayList with arcs (Arc) to be sorted
     * @return sorted ArrayList with arcs (Arc).
     */
    public static List sortArcsOnPosition(List arcs)
    {
        List tempArcs;
        tempArcs = arcs;
        Collections.sort(tempArcs, (Object o1, Object o2) -> ((Arc)o1).getArcPosition()
                - ((Arc)o2).getArcPosition());
        return tempArcs;
    }
    
    /** makeLabelList: Returns a list with the labels from the vertices in this
     * set of arc keys.
     * @param arcSet : HashSet with arc keys (String).
     * @return an ArrayList with labels (String).
     */
    public List makeLabelList(Set arcSet)
    {
        String blank = " ";
        LabelTable allLabels = ApplicationManager.getLabels();
        List labelList = new ArrayList();
        Iterator arc_itr = arcSet.iterator();
        while (arc_itr.hasNext())
        {
            String arcKey = arc_itr.next().toString();
            // Two vertex-ids representing the left and right vertex of this arc.
            Integer vrtxA;
            Integer vrtxB;
            try
            {
                vrtxA = Integer.valueOf(arcKey.substring(0, arcKey.indexOf("*")));
                vrtxB = Integer.valueOf(arcKey.substring(arcKey.indexOf("*") + 1));
                if(vrtxA > 0 && vrtxB > 0) labelList.add(allLabels.
                        getVertexLabel(vrtxA) + blank + allLabels.getVertexLabel(vrtxB));
                else if(vrtxA > 0) labelList.add(allLabels.getVertexLabel(vrtxA));
                else if(vrtxB > 0) labelList.add(allLabels.getVertexLabel(vrtxB));
            }
            // Catches a key that is not an arc.
            catch (StringIndexOutOfBoundsException oe)
            {
                vrtxA = Integer.valueOf(arcKey);
                if(vrtxA > 0) labelList.add(allLabels.getVertexLabel(vrtxA));
            }
        }
        return labelList;
    }    
}
