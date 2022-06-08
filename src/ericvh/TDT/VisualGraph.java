package ericvh.TDT;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


/** Class VisualGraph delivers output in Pajek or UCINET-format, 
 * two graph rendering programs:
 * (http://vlado.fmf.uni-lj.si/pub/networks/pajek/ and 
 * http://www.analytictech.com/ucinet.htm). The Ucinet-application is not installed
 * on this machine at this time.
 * @author Eric Van Horenbeeck
 * Created on 23 maart 2005, 9:42
 * Revision: September 12, 2006
 */
public final class VisualGraph
{
    // Table with information on documents.
    private final DocTable mDocTable;
    // Map conversion between doc-id and a sequential number identifying a graph node.
    private final SortedMap mGraphNodeMap;
    // LinkedList with Pajek cluster number for every doc-id.
    private LinkedList mClusterList;
    // LinkedList with arrays holding two linked documet-ids.
    private LinkedList mArcList;
    // Strings with all the data needed by a visual graph program.
    private String mNetString;
    private String mClusterString;
    private String mGraphData;
    // The number of nodes in this network.
    private int mNodeCount;
    // String with the name is this task.
    private String mTaskName;
    // Boolean 'true' if there is more than one task being processed.
    private boolean mDefault;
    // Boolean flag to identify the Pajek network program.
    private boolean mPajek;
    // List of all relevant documents involved in the task.
    private List mDocs;
    // Map with a focus doc and a set of related documents.
    private LinkedHashMap mClusterMap;
    // Set with doc-ids tht make up the main core.
    private SortedSet mCoreSet;
    // Reading and writing files.
    private final InputOutput io = new InputOutput();
    private final String separator = System.getProperty("file.separator");
    
    /** Constructor
     */
    public VisualGraph()
    {
        mDocTable = ApplicationManager.getDocTable();
        mArcList = new LinkedList();
        mGraphNodeMap = new TreeMap();
        setNetString("");
        mGraphData = "";
        mDefault = false;
    }
    
    /** setVisualGraph: Transforms graph nodes and edges into a String representation.
     * @param graphProgram : the graph visualization program used.
     * @param taskSize : if more than one task is being processed, default values
     * apply. No user dialog.
     */
    public void setVisualGraph(String graphProgram, int taskSize)
    {
        // Boolean 'true' if this program chooses Pajek, if 'false' the program is
        // Ucinet.
        if(graphProgram.equals("pajek")) mPajek = true;
        // Boolean 'true' if there is more than one task being processed.
        if(taskSize > 1) mDefault = true;       
        // Gives a sequential number to each node in the network.
        setNodeNumbers();
        // Formats the data from the clustermap.
        setDataString();
        // Formats the cluster identification for every node.
        prepareClusters();
        // Finalizes and returns all data in the Pajek graph format.
        if(mPajek) setNetString("*TDT Project - " + mTaskName + "\r\n"  
                + "*Vertices " + getNodeCount() +
                "\r\n" + getDataString() + getArcsString(getLinkList()));
        // Finalizes and returns all data in the Ucinet graph format.
        else
        {
            // Removes last comma.
            // mNetString = mNetString.substring(0,graphString.length()-1);
            setNetString("dl n = " + getNodeCount() + " format = edgelist1\r\n"
                    + "labels embedded\r\n"
                    + getArcsString(getLinkList()));
        }
        try {
            // Saves file with these data.
            saveFile();
        } catch (IOException ex) {
            Logger.getLogger(VisualGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /** setNodeNumbers: Gives a sequential number to each node in the network.
     */
    private void setNodeNumbers()
    {
        int graphNode = 0;
        Iterator doc_itr = getAllDocs().iterator();
        while(doc_itr.hasNext())
        {
            Integer doc = (Integer) doc_itr.next();
            graphNode++;
            // Makes a map to translate the doc-id with a serial number identifying 
            // a graph node.
            mGraphNodeMap.put(doc, graphNode);
        }
        setNodeCount(graphNode);
    }
 
    /** setDataString: Analyzes and formats the data in the clustermap.
     */
    private void setDataString()
    {
        int clusterId = 0;
        String graphData = "";
        // Default position parameter values.
        double coordX = 0.0;
        double coordY = 0.0;
        double coordZ = 0.5;
        // Symbol to identify a node: 'ellipse' for a member of a cluster,
        // 'diamond' for the focus doc.
        String symbol;
        // Default magnification of the graph node symbol in the x-direction. 
        // Renders a circle when xFact = yFact.
        int xFact = 2;
        // Default magnification of the 'ellipse' graph node symbol in the y-direction.
        int yFact = 2;
        // 'lr' = distance of the vertex label relative to the vertex center.
        int labelPos = xFact + 4;
        // 'fos' = font size of the label.
        int fontSize = 10;
        // Color keys.
        int nodeKey = 0;
        int borderKey = 0;
        // Lists
        LinkedList arcList = getLinkList();
        LinkedList clusterList = new LinkedList();
        
        // Iterates over the clustermap. Each member of a cluster will be linked to
        // the focus doc and to the main core node and gets a different color.
        Set mainCoreSet = getClusterMap().keySet();
        Iterator map_itr = mainCoreSet.iterator();
        while(map_itr.hasNext())
        {
            // Focus identification number.
            Integer thisDoc = (Integer) map_itr.next();
            Integer focusId = (Integer) mGraphNodeMap.get(thisDoc);
            // Sequential number identifying a cluster.
            clusterId++;
            // Graph node color (interior and boundary).
            nodeKey++;
            if(borderKey > 25) nodeKey = 99;
            else if(nodeKey > 25)
            {
                nodeKey = 0;
                borderKey++;
            }
            String nodeColor = getTopicColor(nodeKey);
            String borderColor = getTopicColor(borderKey);
            
            // Iterates over the cluster set.
            SortedSet clusterSet = (TreeSet) mClusterMap.get(thisDoc);
            Iterator cluster_itr = clusterSet.iterator();
            while(cluster_itr.hasNext())
            {
                // Cluster member identification number.
                Integer docId = (Integer) cluster_itr.next();
                Integer nodeId = (Integer) mGraphNodeMap.get(docId);    
                // If the node is a focus doc, its symbol is changed.
                if(mainCoreSet.contains(docId)) symbol = " diamond ";
                else symbol = " ellipse ";
                // Linking cluster members to the focus doc of the cluster.
                Integer[] nodeLink = new Integer[2];
                nodeLink[0] = focusId;
                nodeLink[1] = nodeId;
                arcList.add(nodeLink);
                clusterList.add(clusterId);
                
                // Formatting the data.
                if(mPajek) graphData += nodeId + "  \" " + docId + "\"  " + 
                        coordX + "  " +  coordY + "  " +  coordZ + symbol + 
                        " x_fact " + xFact + " y_fact " + yFact + " ic " +
                        nodeColor + " bc " + borderColor + " lr " + labelPos +
                        " fos " +  fontSize + "\r\n";
            }
        }
        mGraphData = graphData;
        mArcList = arcList;
        setClusterList(clusterList);
    }
    
    /** getDataString: Analyzed and formated data from the clustermap.
     * @return String with formated data.
     */
    private String getDataString()
    {
        return mGraphData;
    }
    
    /** getLinkList: returns the list with doc-doc links.
     * @return LinkedList with arrays holding two linked doc-ids (Integer).
     */
    private LinkedList getLinkList()
    {
        return mArcList;
    }
    
    /** getNodeCount: Returns the nummber of nodes in this network.
     *  @return the number of nodes (int).
     */
    public int getNodeCount()
    {
        return mNodeCount;
    }
    
    /** getArcsList: Returns a String representation of the arcs (links). 
     * The arcs are then appended to the graphString with the vertices (nodes).
     * @param arcList : LinkedList with Integer arrays holding two linked doc-Ids.
     * @return String with arcs in a graph visualization format.
     */
    private String getArcsString(LinkedList arcList)
    {
        String arcData = "";
        double arcValue = 1.0;
        // Line width;
        int width = 1;
        // Iterates over the arcs and converts to String with visual graph parameters.
        Iterator arc_itr = arcList.listIterator();
        while(arc_itr.hasNext() )
        {
            Integer[] links = (Integer[]) arc_itr.next();
            Integer firstDoc = links[0];
            Integer secondDoc = links[1];
            // Adds a data string in Pajek format.
            if(mPajek) arcData += firstDoc + "  " + secondDoc + " " + arcValue +
                    " w " + width + "\r\n";
            // Adds a data string in Ucinet format.
            else arcData += " " + firstDoc + "  " + secondDoc + "\r\n";
        }
        // Returns all data in the Pajek format.
        if(mPajek) return ("*Edges " + "\r\n" + arcData);
        // Returns all data in the Ucinet format.
        else return "data:\r\n" + arcData;
    }
    
    /** getClusters: Prepares a string with for every node a cluster-id.
     */
    private void prepareClusters()
    {
        String clusterData =  "*" + mTaskName + ".clu" + "\r\n*Vertices " 
                + getNodeCount() + "\r\n";
        // Converts the LinkedList with the cluster number (Integer) of the nodes 
        // to a String.
        Iterator cluster_itr = getClusterList().listIterator();
        while(cluster_itr.hasNext() )
        {
            Integer clusterId = (Integer)cluster_itr.next();
            // Adds a string in Pajek format.
            if(mPajek) clusterData += clusterId + "\r\n";
        }
        setClusterString(clusterData);
    }
    
    /** saveFile: Writes the visual data string to disk.
     */
    private void saveFile() throws IOException
    {
        if(getNetString().equals("")) 
            JOptionPane.showMessageDialog(ApplicationManager.getTabbedPane(),
                "There is nothing to save", "Visual Graph Data",
                JOptionPane.ERROR_MESSAGE);
        else
        {
            String program;
            if(mPajek) program =  "Pajek_";
            else program = "Ucinet_";
            String filePath = ApplicationManager.getResultPath()  + separator +
                    program ;
            String taskPath = makePathName(filePath, 0);
            writeFile(filePath, taskPath, 0);
        }
    }
    
    /** writeFile: Makes a pathname and saves the data string after checking for
     * duplicate filenames. When more tasks are processed there is no user dialog
     * and data are overwritten by default.
     * @param path : String with pathname of the file to save.
     * @param nameCount : a sequential counter to avoid duplicate filenames.
     */
    private void writeFile(String filePath, String path, int nameCount)
    {
        File file = new File(path);
        if(file.exists())
        {
            if(mDefault) writeGraphString(path);
            else
            {
                int answer = JOptionPane.showConfirmDialog(ApplicationManager
                        .getTabbedPane(),
                        "This Visual Graph File exists already. Continue?\nPress "
                                + "'Yes' to override, 'No' to attribute a new name,"
                                + "\nor 'Cancel' to abort.", "Visual Graph Data", +
                        JOptionPane.YES_NO_CANCEL_OPTION);
                switch (answer) {
                    case JOptionPane.YES_OPTION -> writeGraphString(path);
                    case JOptionPane.NO_OPTION -> {
                        path = makePathName(filePath, nameCount++);
                        writeFile(filePath, path, nameCount);
                    }
                    case JOptionPane.CANCEL_OPTION -> {
                    }
                    default -> {
                    }
                }
                // No action necessary.
                            }
        }
        else writeGraphString(path);
    }
    
    /** makePathName: Makes a pathname.
     * @param pathName : String with full path.
     * @param nameCount : sequential number to identify a file (int).
     */
    private String makePathName(String pathName, int nameCount)
    {
        pathName += getNameCount(nameCount) + ".net";
        return pathName;
    }
    
    /** getNameCount: Returns a task name to include in the file name.
     * @param nameCount : sequential counter (int);
     * @return String with task name and a sequential number.
     */
    private String getNameCount(int nameCount)
    {
        String taskName = TopicRetriever.getTaskName();
        if(nameCount < 10) taskName = taskName + "_00" + nameCount;
        else if (nameCount < 100) taskName = taskName + "_0" + nameCount;
        else taskName = taskName + "_" + nameCount;
        return taskName;
    }
    
    /** writeGraphString: Writing the file with the data to disk in the appropriate
     * graph visualization format.
     * @param path : the filepath (String).
     */
    private void writeGraphString(String path)
    {
        String program = "Ucinet";
        if(mPajek) program = "Pajek";
        try
        {
            io.writeASCII(path, getNetString());
            io.writeASCII(path.replace(".net", ".clu"), getClusterString());
            ApplicationManager.showText("Retrieval data were transfered to the "
                    + "visual graph program '" + program +"'.", 0);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(ApplicationManager.getTabbedPane(),
                    "Visual Graph Data Not Saved", "Visual Graph", 
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
        }
    }
    
    /** getClusterMap: A map with clustered documents made by the Clusters class.
     * @return LinkedHashMap with the focus doc (int) as key and a SortedSet 
     * of doc-ids (int) as value.
     */
    private LinkedHashMap getClusterMap()
    {
        return mClusterMap;
    }
    
    /** setMainCore: Clusters sends this set with doc-ids from the main network core.
     * @param finalDocs: LinkedHshSet with the final doc-ids (Integer) from 
     * the main core only.
     */
    public void setMainCore(LinkedHashSet finalDocs)
    {
        mCoreSet = new TreeSet(finalDocs);
    }
    
    /** getCoreSet: Set with doc-ids from the main core.
     * @return SortedSet with the doc-ids (Integer) from the main core only.
     */
    private SortedSet getCoreSet()
    {
        return mCoreSet;
    }
    
    /** getAllDocs: Collects all the documents that are graph nodes in one list.
     * @return List of documents (Integer) sorted.
     */
    private List getAllDocs()
    {
        Set docs = new HashSet();
        docs.addAll(getCoreSet());
        Map clusters = getClusterMap();
        Iterator cluster_itr = clusters.keySet().iterator();
        while(cluster_itr.hasNext())
        {
            Integer docInt = (Integer) cluster_itr.next();
            SortedSet docSet = (TreeSet) clusters.get(docInt);
            docs.addAll(docSet);
        }
        this.mDocs = Arrays.asList(docs.toArray());
        Collections.sort(mDocs);
        return mDocs;
    }
    
    /** setClusterMap: Clusters sends this map with clustered documents.
     * @param clusters : LinkedHashMap with the focus doc (int) as key and a 
     * SortedSet of doc-ids (int) as value.
     */
    public void setClusterMap(LinkedHashMap clusters)
    {
        mClusterMap = clusters;
    }
    
    /** setTaskName: Clusters sends the name of this task.
     * @param name : the name of this task (String)
     */
    public void setTaskName(String name)
    {
        mTaskName = name;
    }
    
    /** setNodeCount: the number of nodes in this network.
     * @param nodeCount : number of nodes (int).
     */
    public void setNodeCount(int nodeCount)
    {
        mNodeCount = nodeCount;
    }
    
    /** getNetString: Returns string with data in visualization format.
     * @return formated data String.
     */
    public String getNetString()
    {
        return mNetString;
    }
    
    /** setNetString: string with data in visualization format.
     * @param net : formated data String.
     */
    public void setNetString(String net)
    {
        this.mNetString = net;
    }
    
    /** getClusterString: Returns string with cluster identification.
     * @return String with cluster identification for every node.
     */
    public String getClusterString()
    {
        return mClusterString;
    }
    
    /** setClusterString: setting cluster identification string.
     * @param clusters : String with cluster identification for every node.
     */
    public void setClusterString(String clusters)
    {
        this.mClusterString = clusters;
    }
    
    /** getClusterList: getting cluster identification.
     * @return LinkedList with cluster identification (Integer) for every node.
     */
    public LinkedList getClusterList()
    {
        return mClusterList;
    }
    
    /** setClusterList: setting cluster identification list.
     * @param clusterList : LinkedList with cluster identification (Integer) for
     * every node.
     */
    public void setClusterList(LinkedList clusterList)
    {
        this.mClusterList = clusterList;
    }
    
    /** getTopicColor: Getter returns the color according to the key it gets from 
     * Topics.
     * @param colorKey : selects a color (int)
     * @return String with a color indication for a graph rendering program
     */
    public String getTopicColor(int colorKey)
    {
        String color;
        color = switch (colorKey) {
            case 1 -> "Yellow";
            case 2 -> "SeaGreen";
            case 3 -> "Salmon";
            case 4 -> "YellowOrange";
            case 5 -> "CornflowerBlue";
            case 6 -> "BurntOrange";
            case 7 -> "Emerald";
            case 8 -> "LightMagenta";
            case 9 -> "LSkyBlue";
            case 10 -> "CarnationPink";
            case 11 -> "Apricot";
            case 12 -> "Thistle";
            case 13 -> "Maroon";
            case 14 -> "Green";
            case 15 -> "Blue";
            case 16 -> "GreenYellow";
            case 17 -> "Grey";
            case 18 -> "Red";
            case 19 -> "Magenta";
            case 20 -> "Pink";
            case 21 -> "Plum";
            case 22 -> "SpringGreen";
            case 23 -> "Sepia";
            case 24 -> "Cyan";
            case 25 -> "Grey30";
            default -> "White";
        };
        return color;
    }
}
