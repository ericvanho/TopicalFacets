package ericvh.TDT;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;


/** Class LinkTable makes for every collection a sortable table with for every 
 * token-type of that collection the number of incoming and outgoing links 
 * and their total.
 * @author  Eric Van Horenbeeck
 * Created on 29 oktober 2004, 23:11
 * Revision: Oct 7, 2006
 */
public class LinkTable extends JPanel
{
    // Map with as key the vertex index (Integer) and as value a vertex.
    private final Map mVrtxMap;
    // ArcsTable manages information on arcs.
    private final ArcsTable mArcTable;
    // DocTable with information on document level.
    private final DocTable mDocTable;
    // Map to collect in- and outgoing links.
    private final Map mLinkDataMap;
    // Map to collect the total number of links in a document and the maximum
    // number of links seen.
    private final Map mDocLinkMap;
    // String with the name of the collection now being processed.
    private String mCollKey;
    // 2D-table with for every vertex the number of incoming and outgoing
    // links and the total.
    private Object[][] linkData;
    // Components to build the link table in the GUI.
    private JTable linkTable;
   // private TableSorter linkSorter;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    
    /** Constructor
     * @param vt : VerticesTable with all vertices in this dataGraph.
     * @param at : ArcsTable with all arcs in this table.
     * @param dt : DocTable with information on documents.
     */
    public LinkTable(VerticesTable vt, ArcsTable at, DocTable dt)
    {
        super(new BorderLayout());
        mVrtxMap = vt.getFullVerticesMap();
        mArcTable = at;
        mDocTable = dt;
        mLinkDataMap = new HashMap();
        mDocLinkMap = new HashMap();
    }
    
    /** Inner class LinkTableModel translates data into a table.
     */
    class LinkTableModel extends AbstractTableModel
    {
        // Sets the header of the table.
        private final String[] columnNames = {mCollKey, "In", "Out", "Total"};
        // Initializes 2D array.
        private Object[][] tableData = setTable();
        // Copies the incoming data-array into this table.
        private Object[][] setTable()
        {
            tableData = new Object[linkData.length][4];
            System.arraycopy(linkData, 0, tableData, 0, linkData.length);
            return tableData;
        }
        // Gets the number of columns.
        @Override
        public int getColumnCount()
        {
            return columnNames.length;
        }
        //Gets the number of rows in the table.
        @Override
        public int getRowCount()
        {
            return tableData.length;
        }
        // Gets the name of a column.
        @Override
        public String getColumnName(int col)
        {
            return columnNames[col];
        }
        // Gets the content of a cell in the table.
        @Override
        public Object getValueAt(int row, int col)
        {
            return tableData[row] [col];
        }
    }
    
    /** addTable: Adds a sortable table with the number of links to and from 
     * token-types in a collection to the tabbedPane. The pane is added to the 
     * mainFrame of the GUI.
     * @param collKey : the collection now being processed (String).
     */
    private void addTable(String collKey)
    {
        mCollKey = collKey;
        TableSorter thisLinkSorter = new TableSorter(new LinkTableModel());
        linkTable = new JTable(thisLinkSorter);
        thisLinkSorter.setTableHeader(linkTable.getTableHeader());
        JScrollPane scrollPane = new JScrollPane(linkTable);
        tabbedPane.addTab(mCollKey, scrollPane);
        ApplicationManager.addLinkTable(tabbedPane, 7);
    }
    
    /** countInOutLinks: Constructs a map with the vertex indices as key and an 
     * array as value. This array has two elements: first the number of incoming 
     * links and secondly the number of outgoing links to that vertex (int) inside a
     * collection.
     * @param allDocSet : a set with all the document numbers of a collection
     * @return map (HashMap) with all vertices of one collection and the number of 
     * incoming and outgoing links of every vertex.
     */
    private Map countInOutLinks(Set allDocSet)
    {
        Map connectionMap = new HashMap();
        // Iterates over all vertices in this dataGraph
        Iterator vertex_itr = mVrtxMap.keySet().iterator();
        while(vertex_itr.hasNext())
        {
            List connectionList = new ArrayList(2);
            int inLink = 0;
            int outLink = 0;
            Integer vrtxInt = (Integer) vertex_itr.next();
            Vertex v = (Vertex) mVrtxMap.get(vrtxInt);
            // SortedSet with all documents over all collections of this vertex.
            SortedSet vertexDocSet = v.getAllVertexDocNrs();
            // Intersection gives the docs of this collection for this vertex.
            Set docSet = Algorithms.intersect(allDocSet, vertexDocSet);
            // Iterates over docs and collects the arcs of this vertex.
            Iterator docs_itr = docSet.iterator();
            while(docs_itr.hasNext())
            {
                Integer docInt = (Integer) docs_itr.next();
                // Gets the incoming an outgoing links for this vertex in this 
                // document.
                List tmpList = getVertexLinks(docInt, vrtxInt);
                int thisInLink = ((Integer) tmpList.get(0));
                int thisOutLink = ((Integer) tmpList.get(1));
                inLink += thisInLink;
                outLink += thisOutLink;
                // Collects the links of this vertex in this document.
                addDocLinks(docInt, thisInLink + thisOutLink);
            }
            // Puts the incoming and outgoing links in an array if both are non-zero.
            // Then makes a connectionMap with the total links per vertex for all 
            // docs in this collection with the vertex as key and the array as value.
            connectionList.add(0, inLink);
            if (inLink > 0 || outLink > 0)
            {
                connectionList.add(1, outLink);
                connectionMap.put(vrtxInt, connectionList);
            }
        }
        return connectionMap;
    }
    
    /** addDocLinks: While counting the in- and outlinks of the vertices, the total 
     * number of links of a document is computed together with the maximum number of 
     * links attributed to some token. This is the equivalent of the maximum token 
     * frequency and used to normalize the link frequency of individual tokens. 
     * The data are collected in a HasMap with docInt as key and as value an array 
     * with the total number of links and the maximum link count. The map is 
     * transferred to the dataTable and ultimately to the docStat instance of every 
     * document.
     * @param docInt : the document identifier (Integer) for a vertex.
     * @param thisLinks : the sum of in- and outlinks of a single vertex (int).
     */
    private void addDocLinks(Integer docInt, int thisLinks)
    {
        int[] linkArray;
        if(mDocLinkMap.containsKey(docInt))
        {
            linkArray = (int[]) mDocLinkMap.get(docInt);
            linkArray[0] += thisLinks;
            int maxLink = ((Integer) linkArray[1]);
            if(thisLinks > maxLink) linkArray[1] = thisLinks;
        }
        else
        {
            linkArray = new int[2];
            linkArray[0] = thisLinks;
            linkArray[1] = thisLinks;
        }
        mDocLinkMap.put(docInt, linkArray);
    }
    
    /** putLinksInTable: Scans over all arcs of every collection to make a table of 
     * incoming and outgoing links per vertex. A linkDataMap is made to be saved on 
     * disk as invoked by the ApplicationManager.
     */
    public void putLinksInTable()
    {
        CollectionTable collTable = ApplicationManager.getCollTable();
        LabelTable allLabels = ApplicationManager.getLabels();
        // Iterates over the collections of the dataGraph inside the scope of this 
        // session.
        Set collectionKeys = collTable.getScopeCollectionKeys();
        Iterator coll_itr = collectionKeys.iterator();
        while(coll_itr.hasNext())
        {
            String collectionKey = coll_itr.next().toString();
            // Set with all the doc-ids that populate this collection.
            Set docSet = collTable.getDocsInCollection(collectionKey);
            // Iterates over all vertices to collect links to and from a vertex.
            Map inOutMap = countInOutLinks(docSet);
            // LinkDataMap is saved on disk.
            mLinkDataMap.put(collectionKey, inOutMap);
            // Data are shown on screen, when so requested by the user (GUI parameter
            // settting).
            if(ApplicationManager.getShowLinksStatus())
            {
                int i = 0;
                // Iterates over the map to put the data in a 2D matrix.
                Object[][] data = new Object[500][4] ;
                Iterator inOut_itr = inOutMap.keySet().iterator();
                while(inOut_itr.hasNext())
                {
                    Integer vrtxInt = (Integer) inOut_itr.next();
                    Object[] tmpArray;
                    tmpArray = ((List) inOutMap.get(vrtxInt)).toArray();
                    data[i][0] = allLabels.getVertexLabel(vrtxInt);
                    // Incoming links.
                    data[i][1] = tmpArray[0];   
                    // Outgoing links.
                    data[i][2] = tmpArray[1];  
                     // Total links.
                    data[i][3] = (Integer) tmpArray[0] + (Integer) tmpArray[1];
                    i++;
                    // Increases table capacity with 500 rows when full.
                    if (i % 500 == 0) data = Algorithms.resizeTable(data, 4, i, 500);
                }
                // Removes any unused rows when the matrix is ready
                linkData = Algorithms.resizeTable(data, 4, i, 0);
                // Puts the matrix for this collection in a table, showing it in 
                // the GUI.
                addTable(collectionKey);
            }
        }
        // Updates the docTable with link information.
        setDocLinks();
    }
    
    /** setDocLinks: Transfers general link information to the docTable.
     */
    private void setDocLinks()
    {
        Iterator link_itr = mDocLinkMap.keySet().iterator();
        while(link_itr.hasNext())
        {
            Integer docInt = (Integer) link_itr.next();
            int[] linkArray = (int[]) mDocLinkMap.get(docInt);
            mDocTable.setTotLinks(docInt, linkArray[0]);
            mDocTable.setMaxLinks(docInt, linkArray[1]);
        }
    }
    
    /** getVertexLinks: Returns the total number of links for this vertex in this
     * document.
     * @param docInt : the unique document identifier (Integer).
     * @param vrtxInt : identifier for a vertex (Integer).
     * @return list with the number of incoming and outgoing links for this 
     * vertex (int).
     */
    public List getVertexLinks(Integer docInt, Integer vrtxInt)
    {
        List linkList = new ArrayList(2);
        int inLink = 0;
        int outLink = 0;
        int vrtxIdx = vrtxInt;
        // List with all arcs having this vertex in this document
        List arcList = mArcTable.getArcsWithKey(docInt, vrtxIdx);
        // Iterates over the arcs in this doc and counts the incoming and outgoing 
        // links.
        Iterator arcs_itr = arcList.iterator();
        while(arcs_itr.hasNext())
        {
            Arc currentArc = (Arc) arcs_itr.next();
            // Vertex at the left of the arc: outgoing link;
            if(currentArc.containsIndexFirst(vrtxIdx)) outLink++;
            // Vertex at the right of the arc: incoming link.
            else if(currentArc.containsIndexSecond(vrtxIdx)) inLink++;
        }
        linkList.add(0, inLink);
        linkList.add(1, outLink);
        return linkList;
    }
    
    /** getLinksMap: Getter returns a map with incoming and outgoing links for every 
     * vertex(label) in every collection.
     * @return to the ApplicationManager a map with links for all the collections.
     * Structure: Key collection: Key (String), Value: map with the vertex-id as 
     * Key (Integer) and as Value a list with 2 elements: incoming links (Integer),
     * outgoing links (Integer).
     */
    public Map getLinksMap()
    {
        return mLinkDataMap;
    }
    
}

