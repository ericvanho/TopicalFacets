package ericvh.TDT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Class QandA returns a set of topical facets to answer a question formulated by 
 * the user. If this question is found to be strongly related to a query saved by 
 * the application, the  topical facets that defined the main core of this query 
 * are retrieved. The facets are then translated into meaningful phrases and
 * associations.
 * Not yet implemented.
 * @author Eric Van Horenbeeck
 * Created on June 7, 2006  13:10
 */
public class QandA
{
    // Map with tokens and their facets.
    private Map mTokenMap;
    // Map with facets and their arcs.
    private Map mArcMap;
    // Map with tokens and descriptive arcs.
    private Map mDescriptionMap;
    // Map with 'named entities' and informative phrases from the Associations class.
    private Map mAssociations;
    // Class with all the labels.
    private LabelTable labels = new LabelTable();
    
    /** Constructor
     */
    public QandA()
    {
        mDescriptionMap = new HashMap();
    }
    
    private void getArcs()
    {
        Iterator vertex_itr = mTokenMap.keySet().iterator();
        while(vertex_itr.hasNext())
        {
            Integer vertex = (Integer) vertex_itr.next();
            List facets = (ArrayList) mTokenMap.get(vertex);
                    
            
        }
    }
    
    /*
            // Retrieves associations and phrases to illustrate the content of the documents.
        mAssociations = ApplicationManager.getAssociationMap();
                // Prepares a string from associations and arcs to illustrate the topic content.
            if(mAssociations.containsKey(docNr))
            {
                List assocList = new ArrayList();
                assocList = (ArrayList) mAssociations.get(new Integer(docNr));
                arcString += "Doc " + docNr + ": " + prepareArcString(assocList, maxWords) + '\n';
                length++;
            }
        
                        Integer localFacet = (Integer) facetList.get(0);
                String facetPath = facetList.get(1).toString();
                // The path to a local topicArcsMap within the same scope.
                String arcPath = facetPath.replace("_Topics.tpc", "_TopArcs.tas");
                // Looks up an already retrieved local topic and arc map.
                LinkedHashMap topicMap = new LinkedHashMap();
                LinkedHashMap arcMap = new LinkedHashMap();
                if(localFacetMaps.containsKey(facetPath))
                {
                    topicMap = (LinkedHashMap) localFacetMaps.get(facetPath);
                    arcMap = (LinkedHashMap) localArcMaps.get(arcPath);
                }
                // Reads the files from the hard disk if this path is used for the first time.
                else
                {
                    try
                    {
                        topicMap = io.readLinkedHashMap(facetPath);
                        arcMap = io.readLinkedHashMap(arcPath);
                    }
                    catch(Exception e)
                    {
                        ApplicationManager.showText("Problems reading localFacet file: " + localFacet +
                                " scope: " + GraphTime.getFullScope(facetPath), 0);
                    }
                    // Keeps these maps available for further use.
                    localFacetMaps.put(facetPath, topicMap);
                    localArcMaps.put(arcPath, arcMap);
                }
     */
}
