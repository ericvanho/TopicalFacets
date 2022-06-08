package ericvh.TDT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class  Derivative calculates the second derivative a a cumulative similarity score.
 * @author Eric Van Horenbeeck
 * Created on 29 juni 2007, 10:54
 *  Revision:
 */
public class Derivative
{
    // Map with the retained documents from the recall-phase sorted on their similarity value.
    private LinkedHashMap mSimValueMap;
    // 2nd derivative of the cumulative document similarity score.
    private double mInflectionPoint;
    
    /**  Constructor
     */
    public Derivative()
    {
    }
    
    /** setDocMap: DocSimilarity sends a HashMap with the document-ids and the matching similarity values
     * that were retained in the SELECT-phase. A LinkedHashMap is prepared with the document-ids sorted
     * from high to low on this similarity value.
     * @param docMap : HashMap with docIds (Integer) as key and as value the scores (double) from
     * the query-by-document similarity matrix.
     */
    public void setDocMap(HashMap docMap)
    {
        Map tmpMap = new HashMap();
        Iterator map_itr = docMap.keySet().iterator();
        while(map_itr.hasNext())
        {
            Integer docId = (Integer) map_itr.next();
            Object[] setArray = (Object[]) docMap.get(docId);
            double docValue = ((Double) setArray[3]);
            // Puts the similarity value (key) and the docId (value) in a map. Adds a small random
            // number to an identical key to avoid data loss.
            if(tmpMap.containsKey(docValue))
            {
                double n = Math.random() / 100000;
                docValue += n;
            }
            tmpMap.put(docValue, docId);
        }
        // Sorted on the similarity value.
        TreeMap tm = new TreeMap(simWeight);
        tm.putAll(tmpMap);
        mSimValueMap = new LinkedHashMap();
        // Reversing the map while observing the ordering.
        Iterator tree_itr = tm.keySet().iterator();
        while(tree_itr.hasNext())
        {
            Double simValue = (Double) tree_itr.next();
            Integer docInt = (Integer) tm.get(simValue);
            mSimValueMap.put(docInt, simValue);
        }
        setInflectionPoint();
    }
    
    /** Comparator sorts the map on the similarity value from high to low.
     */
    private final Comparator simWeight = (Comparator) (Object o1, Object o2) -> {
        Double w1 = (Double) o1;
        Double w2 = (Double) o2;
        return w2.compareTo(w1);
    };
    
    /** setInflectionPoint: the 2nd derivative of the cumulated similarity scores.
     */
    private void setInflectionPoint()
    {
        LinkedHashMap tmpMap = new LinkedHashMap(mSimValueMap);
        double cumValue = 0.0;
        double firstDeriv;
        double secDeriv;
        double minValue = (double) Integer.MAX_VALUE;
        int i = 0;
        Iterator doc_itr = mSimValueMap.keySet().iterator();
        while(doc_itr.hasNext())
        {
            double value = ((Double) mSimValueMap.get((Integer) doc_itr.next()));
            double[] valueArray = new double[3];
            valueArray[2] = value;
            cumValue += value;
            valueArray[0] = cumValue;
            if(i > 1)
            {
                double[] prevArray = (double[] ) tmpMap.get(i - 1);
                firstDeriv =StrictMath.abs(cumValue - prevArray[0]);
                valueArray[1] = firstDeriv;
                if(firstDeriv < minValue)
                {
                    minValue = firstDeriv;
                    mInflectionPoint = value;
                }
            }
            else  valueArray[1] = cumValue;
            /*
            if(i > 2)
            {
                double[] prevArray = (double[] ) tmpMap.get(i - 1);
                secDeriv =StrictMath.abs(firstDeriv - prevArray[1]);
                if(secDeriv < minValue)
                {
                    minValue = secDeriv;
                    mInflectionPoint = value;
                }
            }
             */
            i++;
            tmpMap.put(i, valueArray);
// System.out.println("i " +  i + "\t CumValue  " + cumValue + "\t  firstDeriv  "
// + firstDeriv + "\t secDeriv  " + secDeriv + "\t minVal " + minValue);
        }
    }
 
    /** getInflectionPoint
     * @return InflectionPoint: the 2nd derivative of the cumulated similarity scores (double).
     */
    public double getInflectionPoint()
    {
        return mInflectionPoint;
    }
    
}
