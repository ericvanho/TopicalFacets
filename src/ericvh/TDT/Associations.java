package ericvh.TDT;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;


/** Class Associations connects two vertices that have some relation with each other: 
 * both are informative tokens, are next to each other in the text and have the same
 * informative value. Several associations can be combined into one associative 
 * string.
 * @author  Eric Van Horenbeeck
 * Created on 24 september 2004, 21:17
 */
public class Associations implements Serializable
{
    // Map to collect meaningful arcs (collocation, named entity).
    private final SortedMap mArcAssociations;
    // Informatove arcs collected by the GraphAnlyzer.
    private ArcsTable mNewArcsTable;
    // CollectionTable information on collections of documents.
    private CollectionTable mCollTable;
    // VerticesTable needed to attribute a value to the association.
    private VerticesTable mVertTable;
    // Map to store the associations.
    private Map mAssociationMap;
    // Boolean flag to indicate if a combination of associations has been made.
    private boolean mCombined;
    
    /** Constructor
     */
    public Associations()
    {
        mArcAssociations = new TreeMap();
    }
    
    /** addAssociations: GraphAnalyzer adds this arc key in this document to the map.
     * Arcs keys are added according to their position in the text. LinkedLists are 
     * used to keep this order.
     * @param docNr : the unique document identifier (int)
     * @param arcKey : key to the special arc to be added to the map (String)
     */
    public void addAssociations(int docNr, String arcKey)
    {
        Integer docInt = docNr;
        LinkedList arcList = new LinkedList();
        if(mArcAssociations.containsKey(docInt))
            arcList.addAll((LinkedList) mArcAssociations.get(docInt));
        arcList.add(arcKey);
        mArcAssociations.put(docInt, arcList);
    }
    
    /** combineLinks: Reads the map with associated arcs and tries to combine these 
     * into longer structures based on vertex identity.
     */
    public void combineLinks()
    {
        mVertTable = GraphAnalyzer.getNewVerticesTable();
        mNewArcsTable = GraphAnalyzer.getNewArcsTable();
        mCollTable = ApplicationManager.getCollTable();
        mAssociationMap = new HashMap();
        // Iterates over the association candidates of a document
        Iterator links_itr = mArcAssociations.keySet().iterator();
        while(links_itr.hasNext())
        {
            Integer previousIdx = null;
            LinkedList combinedList = new LinkedList();
            Integer docInt = (Integer) links_itr.next();
            LinkedList arcList = (LinkedList) mArcAssociations.get(docInt);
            // Checks if an association can be combined with another arc.
            Iterator arc_itr = arcList.iterator();
            while(arc_itr.hasNext())
            {
                String arcKey = arc_itr.next().toString();
                Arc a = mNewArcsTable.getArcWithKey(docInt, arcKey);
                Integer aIdx1 = a.getVertex1Index();
                Integer aIdx2 = a.getVertex2Index();
                // Finite State Machine constructs the associations.
                if(!Objects.equals(aIdx1, previousIdx))
                {
                    if(!combinedList.isEmpty()) addAssociationList(docInt, 
                            combinedList);
                    combinedList = new LinkedList();
                    combinedList.add(aIdx1);
                }
                previousIdx = aIdx2;
                combinedList.add(previousIdx);
            }
        }
    }
    
    /** addAssociationList: Adds a list of combined vertices (associations) to 
     * the final association map.
     * Key: document-id, value: SortedMap with the info value (double) of the
     * association as key and a LinkedList of combined vertices as value.
     * @param docInt : document identification (Integer)
     * @param association : LinkedList with associations (Integer) found in 
     * this document
     */
    private void addAssociationList(Integer docInt, LinkedList association)
    {
        // Flag to indicate whether a list of new vertices has been combined 
        // with an existing one.
        mCombined = false;
        SortedMap tmpMap = new TreeMap();
        // Finds the info value for this association.
        double infoValue = associationValue(docInt, association);
        // If an entry exists for this document it is cleaned from duplicates
        // before the new association is added.
        if(mAssociationMap.containsKey(docInt))
            tmpMap = removeAndCombine((TreeMap) mAssociationMap.get(docInt), 
                    association, infoValue);
        // This step is skipped when a new association was combined with an 
        // exisiting one, it should not be added once more to the association map.
        if(!mCombined)
        {
            // Puts the info value and the new association list in a map after 
            // checking and adjusting duplicate keys.
            if(tmpMap.containsKey(infoValue)) infoValue = adjustKey(infoValue);
            if (infoValue > 0.1) tmpMap.put(infoValue, association);
        }
        sortDocAssociations(docInt, tmpMap);
    }
    
    /** sortDocAssociations: Sorts the final map with associations for this
     * document from high info value to low.
     * @param docInt : document identification (Integer)
     * @param docAssocMap : map (SortedMap) with associations for this document.
     * Key: infoValue(double), value: Linkedlist with vertices (Integer)
     * forming an association.
     */
    private void sortDocAssociations(Integer docInt, Map docAssocMap)
    {
        TreeMap tm = new TreeMap(valueComparator);
        tm.putAll(docAssocMap);
        docAssocMap = tm;
        mAssociationMap.put(docInt, docAssocMap);
    }
    
    /** Comparator sorts the association map on the info value from hight to low.
     */
    private final Comparator valueComparator = (Comparator) (Object o1, Object o2) -> 
    {
        Double w1 = (Double) o1;
        Double w2 = (Double) o2;
        return w2.compareTo(w1);
    };
    
    /** adjustKey: Changes equal keys slightly with a small random number to 
     * avoid data loss.
     * @param thisKey : a value (double) used as key in the map with associations.
     * @return same key with a small decimal value (double) added to make it 
     * different from a similar value.
     */
    private double adjustKey(double thisKey)
    {
        double n = Math.random() / 100000;
        return thisKey += n;
    }
    
    /** removeAndCombine: Checks if this association map has already any of 
     * these vertices and removes the duplicates when necessary. If possible
     * the new association is combined with an existing one. Its info value is 
     * then updated.
     * @param oldMap : the current association map (SortedMap) linked to a document
     * with info value (double) as key and a LinkedList of vertex-ids (Integer) 
     * as value.
     * @param newAssoc : LinkedList with new associations (Integer) found in
     * this document
     * @param newValue : the informative value of this new association.
     * @return a new cleaned association map.
     */
    private SortedMap removeAndCombine(SortedMap oldMap, LinkedList newAssoc, 
            double newValue)
    {
        double finalValue;
        // Changes are made on a shallow copy from the exisiting oldMap
        // This copy is returned.
        SortedMap copyMap = new TreeMap(oldMap);
        Iterator old_itr = oldMap.keySet().iterator();
        while(old_itr.hasNext())
        {
            double oldKey = ((Double) old_itr.next());
            LinkedList oldList = (LinkedList) oldMap.get(oldKey);
            // The first vertex of the new list equals the last vertex of the old list.
            // The last vertex is removed and the new list is then added at the 
            // end of the old list.
            if(newAssoc.getFirst().equals(oldList.getLast()))
            {
                oldList.removeLast();
                oldList.addAll(newAssoc);
                mCombined = true;
            }
            // The last vertex of the new list equals the first vertex of the
            // old list.
            // The first vertex is removed and the new list is added in front 
            // of the old list.
            else if(newAssoc.getLast().equals(oldList.getFirst()))
            {
                oldList.removeFirst();
                oldList.addAll(0, newAssoc);
                mCombined = true;
            }
            else if(newAssoc.getLast().equals(oldList.getLast()) ||
                    newAssoc.getFirst().equals(oldList.getFirst()) ||
                    newAssoc.getFirst().equals(oldList.get(1)))
            {
                copyMap.remove(oldKey);
            }
            // Updates the combined value by adding the value of the new list
            // to the exisiting one.
            if(mCombined)
            {
                finalValue = oldKey + newValue;
                if(copyMap.containsKey(finalValue)) finalValue = adjustKey(finalValue);
                copyMap.put(finalValue, oldList);
                copyMap.remove(oldKey);
            }
        }
        return copyMap;
    }
    
    /** associationValue: Calculates the info value of this association or phrase.
     * @param assocList : LinkedList with vertex-ids (Integer) that constitute
     * an association.
     * @return the info value of this association (double).
     */
    private double associationValue(Integer docInt, LinkedList assocList)
    {
        // The collection key needed to retrieve the infoValue of the vertices.
        String collKey = mCollTable.getDocsCollectionKey(docInt);
        double assocValue = 0;
        LinkedList valueList;
        // VerticesTable returns the info value of each vertex in this list.
        valueList = mVertTable.returnValueList(assocList, collKey);
        Iterator value_itr = valueList.iterator();
        // The mean of the individual values is the info value of the association.
        while(value_itr.hasNext()) assocValue += ((Double) value_itr.next());
        
        assocValue = assocValue / valueList.size();
        return assocValue;
    }
    
    /** simplifiedMap: Reduces the associations map to the 15 most informative
     * phrases per document (or less, possibly even none) and removes the info value.
     * @return HashMap with key: docInt (Integer), value: ArrayList with 
     * LinkedLists with Integer.
     */
    private Map simplifiedMap()
    {
        Map simplifiedMap = new HashMap();
        int maxAssoc = 15;
        int assocCounter = 0;
        Iterator comb_itr = mAssociationMap.keySet().iterator();
        while(comb_itr.hasNext())
        {
            Integer docKey = (Integer) comb_itr.next();
            List docList = new ArrayList();
            SortedMap docMap = (TreeMap) mAssociationMap.get(docKey);
            Iterator value_itr = docMap.keySet().iterator();
            while(value_itr.hasNext())
            {
                double vKey = ((Double) value_itr.next());
                LinkedList assocList = (LinkedList) docMap.get(vKey);
                if(assocCounter < maxAssoc)
                {
                    docList.add(assocList);
                    assocCounter++;
                }
            }
            simplifiedMap.put(docKey, docList);
            assocCounter = 0;
        }
        return simplifiedMap;
    }
    
    /** getAssociationsMap: Getter returns the simplified map with the 
     * associations: only the most informative phrases per document are returned.
     * The info value of the associations is removed
     * @return HashMap with Key: docInt (Integer), value: a list (ArrayList) 
     * with associations (LinkedList with Integer).
     */
    public Map getAssociationsMap()
    {
        return simplifiedMap();
    }
    
    /** associationsToString: Returns a String of associations sorted per document
     * and inside a document according to their info value. Asked by the GraphAnalyzer.
     * @param countOnly : boolean 'true' gives the number of associations per 
     * document. If 'false' all the associations are retrieved.
     * @return String to a tabbed pane in the GUI if so requested by the user 
     * (Parameter setting).
     */
    public String associationsToString(boolean countOnly)
    {
        LabelTable allLabels = ApplicationManager.getLabels();
        DecimalFormat df = new DecimalFormat("##.######");
        String combString = "\n* Long Associations in this dataGraph *\n";
        Iterator comb_itr = mAssociationMap.keySet().iterator();
        while(comb_itr.hasNext())
        {
            List labelList;
            Integer docKey = (Integer) comb_itr.next();
            // Formating the associations.
            combString += '\n'+ ApplicationManager.getDocTable().getFilename(docKey)
            + " (" + docKey.toString() +"):\n";
            SortedMap docMap = (TreeMap) mAssociationMap.get(docKey);
            int counter = docMap.keySet().size();
            if(countOnly == false)
            {
                Iterator value_itr = docMap.keySet().iterator();
                while(value_itr.hasNext())
                {
                    Double vKey = (Double) value_itr.next();
                    String value = df.format(vKey);
                    LinkedList assocList = (LinkedList) docMap.get(vKey);
                    labelList = allLabels.returnLabelList(assocList);
                    Iterator label_itr = labelList.iterator();
                    while (label_itr.hasNext()) combString += label_itr.next().
                            toString() + " ";
                    combString += " - info value: " + value + "\n";
                }
            }
            combString += "Associations counted: " + counter + '\n';
        }
        return combString;
    }
    
    /** isEmpty: Returns true if there are no associations available.
     * @return boolean 'true' if the associations map is empty
     */
    public boolean isEmpty()
    {
        return mArcAssociations.isEmpty();
    }

    void addThreeArc(Integer integer, Integer previousVKey, Integer key2)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
