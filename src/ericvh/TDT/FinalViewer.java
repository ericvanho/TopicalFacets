package ericvh.TDT;

import java.text.DecimalFormat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;


/** Class FinalViewer represents in a table the results of the d
 * ocument-by-document similarity computation and the clustering methods. 
 * A tabbed pane is presented with the document in focus compared to the other 
 * documents in a similarity cluster. For each doc the similarity metrics are 
 * given. A list with all the available documents allows the user to inspect 
 * another focus document. This selection will refresh the matrix table with 
 * the updated data. The user can select a related document that will be
 * reconstructed with the terms responsible for its similarity 
 * (vertices and arcs) highlighted. It is possible to save the data so that 
 * later a view on the results can be reconstructed without the need to run 
 * again the full query procedure.
 * @author Eric Van Horenbeeck
 * Created on 3 april 2005, 18:55
 * Revision September 11, 2006
 */
public final class FinalViewer extends JPanel
{
    // Document ID for the protoype document.
    private Integer mPrototype;
    // DocTable with general information and the names of the files.
    private final DocTable mDocTable;
    // The table with the arcs of this dataGraph.
    private ArcsTable mArcTable;
    // String with summary statistics from the doc-by-similarity calculation.
    private String mSummary;
    // Full description of the task.
    private static String mTask;
    // Scope of the task.
    private String mScope;
    // Identification of the focus document
    private int mFocusDoc;
    // Final number of documents retrieved.
    private int mDocCount;
    // LinkedHashMap with doc-ids retained in the final selection as key 
    // and the similarity value as key.
    private LinkedHashMap mAllDocMap;
    // List with lists of doc-ids and arc-keys.
    private List mDocArcList;
    // String with the reconstructed query.
    private String mQueryString;
    // String with parameter settings.
    private String mParameters;
    // Start and end of the scope of this session.
    private Calendar mBeginDate;
    private Calendar mEndDate;
    // List with the informative tokens in the query.
    private List mQueryTokens;
    // LinkedHashMap with the focus-doc and the related docs from the clusters 
    // found in this task.
    private LinkedHashMap mClusterMap;
    // 2D-table for the similarity components for a given document.
    private Object[][] mSimMatrixData;
    // Array with the final document selection for this task.
    private String[] mDocLink;
    // The Clusters instance with the results from the doc-by-similarity 
    // calculation.
    private Clusters mClusters;
    // SimMatrix is a sparse matrix holding the results of the doc-by-doc 
    // similarity calculation.
    private SimilarityMatrix mSimMatrix;
    // Elements of the Table Model.
    private DefaultTableModel tableModel;
    private JTable matrixTable;
    // List with all the cluster available.
    private JList mClusterList;
    // Panel to assemble the cluster list, document list, similarity matrix
    // data and text areas.
    private JPanel similarityPane;
    private JPanel matrixPanel;
    private JPanel queryPanel;
    // Six scroll panels to accomodate cluster and document list, the matrix 
    // data, the query tokens and the text panels.
    private JScrollPane scrollCluster;
    private JScrollPane scrollDoc;
    private JScrollPane scrollTable;
    private JScrollPane scrollQuery;
    private JScrollPane scrollRelatedText;
    private JScrollPane scrollFocusText;
    // Text panes.
    private JTextPane relatedTextPane;
    private JTextPane focusTextPane;
    private JTextPane queryTextPane;
    // Swing buttons and label
    private JButton saveBtn;
    private JLabel saveLbl;
    private static JButton evalBtn;
    private InputOutput io = new InputOutput();
    // Reconstructor for the text of selected documents.
    private Reconstructor rec;
    // Label border.
    private Border border = BorderFactory.
            createEtchedBorder(EtchedBorder.LOWERED);
    // String with the cluster data as an xml-file.
    private String mWord;
    
    /** Constructor with saved data.
     * @param dt : DocTable instance retrieved by the ApplicationManager.
     * @param dataPath : full path (String) to the data from a previous task.
     * @param task : the name of this task (String).
     */
    public FinalViewer(DocTable dt, String dataPath, String task)
    {
        super(new BorderLayout());
        mDocTable = dt;
        mScope = ApplicationManager.getScope();
        setTaskName(task);
        reconstruct(dataPath);
        setTextAreas();
        prepareMatrixView();
        // Saving the data again is not an option.
        saveBtn.setEnabled(false);
        saveLbl.setEnabled(false);
        evalBtn.setEnabled(true);
    }
    
    /** Constructor with new query data.
     * @param dt : DocTable instance retrieved by the ApplicationManager.
     * @param at : ArcsTable instance prepared by the ApplicationManager.
     * @param cl : Clusters class instance.
     * @param summary : general info on the data used to calculate the 
     * similarity matrix.
     */
    public FinalViewer(DocTable dt, ArcsTable at, Clusters cl, String summary)
    {
        super(new BorderLayout());
        mDocTable = dt;
        mArcTable = at;
        mClusters = cl;
        mSummary = summary;
        collectData();
    }
    
    /** collectData: Assembles the necessary data to construct this final viewer.
     */
    private void collectData()
    {
        mSimMatrix = mClusters.getFinalMatrix();
        mPrototype = mClusters.getPrototype();
        setTaskName(TopicRetriever.getTaskName() + "_000");
        mScope = ApplicationManager.getScope();
        rec = new Reconstructor(mArcTable);
        setParameters();
        // Reconstructs the query.
        LinkedList query = ApplicationManager.getQuery();
        StringBuilder queryBuffer = new StringBuilder();
        LabelTable labels = ApplicationManager.getLabels();
        String blank = " ";
        Iterator query_itr = query.iterator();
        while(query_itr.hasNext())
        {
            Integer labelId = (Integer) query_itr.next();
            if(labelId > 0) queryBuffer.append(labels.getVertexLabel(labelId));
            queryBuffer.append(blank);
        }
        mQueryString = queryBuffer.toString().trim() + " - Prototype document: "
                + mPrototype;
        // Informative tokens from the query.
        mQueryTokens = mArcTable.makeLabelList(TopicRetriever.getSeedTokens());
        // List with lists of doc-ids and relevant arc-keys.
        mDocArcList = TopicRetriever.getDocArcLists();
        // mClusterMap is a HashMap with HashMaps with as key the focus
        // document (int) and a SortedSet with all doc-ids (int) that form 
        // the cluster as value.
        mClusterMap = mClusters.getClusterMap();
        // All retained documents (main core).
        mAllDocMap = mClusters.getAllDocs();
        mDocCount = mAllDocMap.keySet().size();
        mBeginDate = ApplicationManager.getBeginDate();
        mEndDate = ApplicationManager.getEndDate();
        setTextAreas();
    }
    
    /** reconstruct: Rebuilds the viewer with saved data.
     *  @param dataPath : full path (String) to the data from a previous task.
     */
    private void reconstruct(String dataPath)
    {
        try
        {
            LinkedList dataList = io.readList(dataPath);
            mArcTable = (ArcsTable) dataList.get(0);
            mSummary = (String) dataList.get(1);
            mSimMatrix = (SimilarityMatrix) dataList.get(2);
            mQueryTokens = (List) dataList.get(3);
            mDocArcList = (List) dataList.get(4);
            mClusterMap = (LinkedHashMap) dataList.get(5);
            mAllDocMap = (LinkedHashMap) dataList.get(6);
            mQueryString = (String) dataList.get(7);
            mBeginDate = (Calendar) dataList.get(8);
            mEndDate = (Calendar) dataList.get(9);
            mParameters = (String) dataList.get(10);
            mPrototype = (Integer) dataList.get(11);
            rec = new Reconstructor(mArcTable);
            mDocCount = mAllDocMap.keySet().size();
        }
        catch (IOException ioe)
        {
            getMessage(3);
            ioe.printStackTrace(System.err);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /** setTable: Adds a table with the similarity components of a doc-by-doc
     * // intersection to a scrollable pane.
     * @param docName : String with name and doc-id of the focus document of
     * this cluster.
     */
    private void setTable(String docName)
    {
        String[] columnNames = {"#", docName, "simV", "simW", "alpha",
            "similarity"};
        tableModel = new DefaultTableModel(mSimMatrixData, columnNames);
        matrixTable = new JTable(tableModel);
        // Disables autoCreateColumnsFromModel otherwise the column 
        // customizations will be lost when the model data are sorted.
        matrixTable.setAutoCreateColumnsFromModel(false);
        // Sorts all the rows in descending order based on the similarity 
        // values in the last column.
        sortAllRows(tableModel, 5, false);
        // Allows single row selection and sets font.
        matrixTable.setRowSelectionAllowed(true);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matrixTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        // Asks to be notified of selection changes.
        ListSelectionModel rowSelect = matrixTable.getSelectionModel();
        rowSelect.addListSelectionListener(new TableSelectionListener());
        // Sets the first column to 30 pixels wide and the second column to 200.
        int colIndex = 0;
        TableColumn col = matrixTable.getColumnModel().getColumn(colIndex);
        int width = 30;
        col.setPreferredWidth(width);
        colIndex = 1;
        col = matrixTable.getColumnModel().getColumn(colIndex);
        width = 200;
        col.setPreferredWidth(width);
        // Puts the table in a scrollable pane and sets dimensions.
        scrollTable = new JScrollPane(matrixTable);
        matrixTable.setPreferredScrollableViewportSize(new Dimension(680, 200));
        // Reconstructs the selected focus document up to the first 200 tokens.
        relatedTextPane.setText("");
        focusTextPane.setText(rec.reconstructText(mFocusDoc, 200));
        focusTextPane.setCaretPosition(0);
    }
    
    /** Table selection listener allows the user to select a document in a 
     * table row of the similarity matrix. This selected document is related 
     * to the focus document and is reconstructed in the 'related text'-panel.
     */
    class TableSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent evt)
        {
            ListSelectionModel lsm = (ListSelectionModel)evt.getSource();
            int selectedRow = lsm.getMinSelectionIndex();
            String docSelect = matrixTable.getValueAt(selectedRow, 1).toString();
            int docNr = Integer.parseInt(docSelect.substring(docSelect
                    .indexOf('(') + 1,
                docSelect.lastIndexOf(')')));
            // Reconstructs text lines of the selected document upto max. 200 words 
            // and highlights info words.
            relatedTextPane.setText(rec.reconstructText(docNr, 200));
            highLightSharedArcs(docNr);
            relatedTextPane.setCaretPosition(0);
        }
    }
    
    /** setMatrixData: Prepares the table representation and the graphic view 
     * of a similarity matrix extracted for this document. The table rows have
     * the document-ids and the columns the similarity measures.
     * @param clusterDocs : SortedSet with doc-ids (int) related to the focus 
     * in this cluster.
     */
    private void setMatrixData(SortedSet clusterDocs)
    {
        int i = 0;
        DecimalFormat df = new DecimalFormat("##.####");
        String docName = mDocTable.getFilename(mFocusDoc)
                + " (" + mFocusDoc +")";
        Object[][] data = new Object[20][6];
        // Iterates over the clusterDocs. The doc-id also represents a row or 
        // column in the matrix.
        Iterator node_itr = clusterDocs.iterator();
        while(node_itr.hasNext())
        {
            // Doc-id of the other document that was compared to the focus doc 
            // cluster.
            int secondDocInt = ((Integer) node_itr.next());
            // Gets the data of docs clustered around the focus doc, but not of 
            // the focus doc itself.
            DocNode thisNode = mSimMatrix.getDocNode(secondDocInt, mFocusDoc);
            if(secondDocInt != mFocusDoc && thisNode != null)
            {
                String secondDoc =  mDocTable.getFilename(secondDocInt);
                 // Counting the rows.
                data[i][0] = i + 1;   
                // Document name and doc-id.
                data[i][1] = secondDoc + " (" + secondDocInt + ")";   
                // simV: vertex similarity.
                data[i][2] = df.format(thisNode.getSimVComponent());  
                // simW: walk similarity.
                data[i][3] = df.format(thisNode.getSimWComponent());    
                 // alpha value.
                data[i][4] = df.format(thisNode.getAlphaComponent());  
                 // similarity value.
                data[i][5] = df.format(thisNode.getDocSimilarity());   
                i++;
                // Increases capacity with 20 rows when the table is full.
                if (i % 20 == 0) data = Algorithms.resizeTable(data, 6, i, 20);
            }
        }
        // Removes any unused rows left over after filling the table with data.
        mSimMatrixData = Algorithms.resizeTable(data, 6, i, 0);
        // Sends the matrix data for this focus document to the table model.
        setTable(docName);
    }
    
    /** constructSimilarityPane: Collects the matrix table and adds the text 
     * areas to the Similarity Pane. The ApplicationManager puts the pane in 
     * the GUI.
     */
    private void constructSimilarityPane()
    {
        saveBtn = new JButton("Save");
        saveBtn.setEnabled(true);
        saveBtn.addActionListener((ActionEvent event) -> {
            try {
                saveViewRequest(event);
            } catch (IOException ex) {
                Logger.getLogger(FinalViewer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        });
        saveLbl = new JLabel("Save all data");
        evalBtn = new JButton("Evaluate");
        evalBtn.setEnabled(false);
        evalBtn.addActionListener((ActionEvent event) -> {
            evaluateRequest(event);
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPanel.add(saveBtn);
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPanel.add(saveLbl);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(evalBtn);
        buttonPanel.setBorder(border);
        buttonPanel.setMaximumSize(new Dimension(1000, 20));
        // Panel with the document description.
        JPanel docPanel =  new JPanel();
        docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.PAGE_AXIS));
        docPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
        docPanel.add(scrollDoc);
        docPanel.add(buttonPanel);
        // Panel to hold the cluster pane.
        JLabel clusterHeader = new JLabel("<html><center>"+ getParameters()
                + "<br>" + mClusterMap.size() + " document clusters with focus"
                        + " document. Most relevant on top. </center></html>",
            JLabel.CENTER);
        JPanel clusterLabel = new JPanel();
        clusterLabel.add(clusterHeader);
        clusterLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        clusterLabel.setBorder(border);
        JPanel tablePanel =  new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));
        tablePanel.setMaximumSize(new Dimension(Short.MAX_VALUE,Short
                .MAX_VALUE));
        tablePanel.add(clusterLabel);
        tablePanel.add(scrollCluster);
        // Panel to hold the similarity matrix.
        JLabel tableHeader =  new JLabel("<html><center> Cluster related to the"
                + " document in focus</center></html>");
        JPanel tableLabel = new JPanel();
        tableLabel.add(tableHeader);
        tableLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        tableLabel.setBorder(border);
        tablePanel.add(tableLabel);
        matrixPanel = new JPanel();
        matrixPanel.setLayout(new BoxLayout(matrixPanel, BoxLayout.PAGE_AXIS));
        matrixPanel.add(scrollTable);
        matrixPanel.validate();
        tablePanel.add(matrixPanel);
        // Panel to show the query tokens.
        JLabel queryHeader =  new JLabel("<html><center>Query tokens for this"
                + " task</center></html>");
        JPanel queryLabel = new JPanel();
        queryLabel.add(queryHeader);
        queryLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        queryLabel.setBorder(border);
        tablePanel.add(queryLabel);
        queryPanel = new JPanel();
        queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.PAGE_AXIS));
        queryPanel.add(scrollQuery);
        tablePanel.add(queryPanel);
        // Panel to render the focus text and related text.
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.PAGE_AXIS));
        textPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
        textPanel.add(scrollFocusText);
        textPanel.add(scrollRelatedText);
        //textPanel.validate();
        // Main panel to hold panels with documents, tables and text.
        similarityPane = new JPanel();
        // Sets a GridBagLayout on the container.
        GridBagLayout gbl = new GridBagLayout();
        similarityPane.setLayout(gbl);
        // Constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 1;
        // Associates the gridbag constraints with the components.
        gbl.setConstraints(docPanel, gbc);
        similarityPane.add(docPanel);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbl.setConstraints(tablePanel, gbc);
        similarityPane.add(tablePanel);
        gbc.gridx = 2;
        gbc.weightx = 0.4;
        gbl.setConstraints(textPanel, gbc);
        similarityPane.add(textPanel);
        ApplicationManager.addTabbedPane(similarityPane, 8);
    }
    
    /** prepareMatrixView: Waits for a focus document text representation at 
     * the top of the document list to become available before starting to build
     * the doc-by-doc matrix representation. When no focus document is found,
     * the task ends. Called by the ApplicationManager.
     */
    void prepareMatrixView()
    {
        // Prepares a table with the similarity matrix for a first focus 
        // document together with the set of doc-ids  that make up that cluster.
        setClusterList();
        if(mFocusDoc > -1)
        {
            SortedSet clusterDocs = new TreeSet((Set) mClusterMap.
                    get(mFocusDoc));
            setMatrixData(clusterDocs);
            setDocList();
            constructSimilarityPane();
            queryTextPane.setText(mQueryString);
        }  
        else ApplicationManager.showText("Retrieval " + getTaskName()
                + " failed.", 0);
    }
    
    /** setClusterList: Prepares a selectable list with all available clusters 
     * sorted from large to small. The sorted cluster set including the 
     * focus-doc is used as key so that duplicate cluster sets are eliminated. 
     * Relevant keywords are added to each cluster.
     */
    private void setClusterList()
    {
        // Default number of keywords pairs (arcs) to display. More is possible
        // for large clusters.
        int maxWords = 5;
        // Opens the table with the prototype document.
        if(mClusterMap.containsKey(mPrototype))  mFocusDoc = mPrototype;
        else mFocusDoc = ((Integer) mClusterMap.keySet().toArray()[0]);
        // Prepares an array with some relevant key words added.
        ArrayList<String> clusterArray = new ArrayList<>();
        LinkedHashSet docSet = new LinkedHashSet(mClusterMap.keySet());
        Iterator doc_itr = docSet.iterator();
        while(doc_itr.hasNext())
        {
            Integer docNr = (Integer) doc_itr.next();
            SortedSet clusters = new TreeSet((Set) mClusterMap.get(docNr));
            // More keywords allowed for large clusters.
            if(clusters.size() > 20) maxWords = 10;
            // Gets the labels from the arc-keys that semantically defined 
            // this topic.
            List arcList = selectArcs(clusters, maxWords);
            String keywords = prepareKeywords(new HashSet(arcList));
            clusterArray.add(docNr + " " + clusters +  " Keywords: "  
                    + keywords);
        }
        //  Collections.sort(clusterArray);
        mClusterList = new JList(clusterArray.toArray());
        // Allows single row selection and sets the font for the cluster list.
        mClusterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mClusterList.setSelectedIndex(0);
        mClusterList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        // Adds selection listener.
        mClusterList.addListSelectionListener(new clusterSelectionListener());
        // Puts cluster list and header in a scrollPane.
        scrollCluster = new JScrollPane(mClusterList);
        scrollCluster.setPreferredSize(new Dimension(680, 300));
        prepareWordExport(clusterArray);
    }
    
    /** Comparator sorts the clusterSet from the highest number of elements 
     * to the lowest.
     */
    private final Comparator size = (Comparator) (Object o1, Object o2) -> {
        SortedSet w1 = (TreeSet) o1;
        SortedSet w2 = (TreeSet) o2;
        return Integer.valueOf(w2.size()).compareTo(w1.size());
    };
    
    /** ClusterSelectionListener allows the user to select a cluster from the 
     * list of available clusters. The similarity matrix table is recalculated 
     * and shown in the GUI.
     */
    class clusterSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent evt)
        {
            JList list = (JList)evt.getSource();
            String clusterSelect = list.getSelectedValue().toString();
            mFocusDoc = Integer.parseInt(clusterSelect.substring(0, 
                    clusterSelect.indexOf("[")-1));
            SortedSet clusterDocs = new TreeSet((Set) mClusterMap.get(mFocusDoc));
            // Prepares a table with the similarity matrix for
            // this focus document.
            setMatrixData(clusterDocs);
            matrixPanel.removeAll();
            matrixPanel.add(scrollTable);
        }
    }
    
    /** setDocList: Puts an array of document descriptions in a grid and adds 
     * it to a panel in the GUI.
     */
    private void setDocList()
    {
        prepareDocArray();
        JList docList = new JList(mDocLink);
        docList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel docHeader =  new JLabel("<html><center>" + mDocCount 
                + " docs retrieved with " +
            "similarity value<br> &nbsp Scope: &nbsp "
                + GraphTime.formatScope(mScope) + "<br> " +
            "&nbsp </center></html>");
        docHeader.setBorder(border);
        scrollDoc = new JScrollPane(docList);
        scrollDoc.setColumnHeaderView(docHeader);
    }
    
    /** prepareDocArray: Collects an array with all the final doc-ids and the
     * similarity value relevant for this retrieval task.
     */
    private void prepareDocArray()
    {
        DecimalFormat df = new DecimalFormat("##.####");
        Object[] docs = (Object[])mAllDocMap.keySet().
                toArray(new Object[mDocCount]);
        // Visits all docs to prepare a string with content info and to collect
        // their filenames.
        mDocLink = new String[mDocCount];
        for(int i = 0; i < mDocCount; i++)
        {
            Integer docNr = (Integer) docs[i];
            double simValue = ((Double) mAllDocMap.get(docNr));
            // Stores the filenames belonging to the doc-ids in an array.
            mDocLink[i] = "(" + docNr + ") " + mDocTable.getFilename(docNr) +
                    " -  " + df.format(simValue);
        }
    }
    
    /** setTextAreas: Prepares two text panes to reproduce the document selected
     * by the user in the similarity tabbedPane and to highlight the tokens 
     * also found in the focus document.
     */
    private void setTextAreas()
    {
        JLabel relatedTextHeader = new JLabel("<html><center>Selected text "
                + "related to the focus document<br>Blue: shared - "
                + "Red: query tokens</center></html>", JLabel.CENTER);
        JLabel focusTextHeader = new JLabel(mSummary, JLabel.CENTER);
        relatedTextHeader.setBorder(border);
        focusTextHeader.setBorder(border);
        relatedTextPane = new JTextPane();
        focusTextPane = new JTextPane();
        queryTextPane = new JTextPane();
        // Sets the layout and style parameters for this text areas.
        relatedTextPane.setMargin(new Insets(5,15,5,5));
        relatedTextPane.setEditable(false);
        relatedTextPane.setContentType("text/plain");
        Style relatedStyle = relatedTextPane.addStyle("Blue Italic", null);
        relatedStyle = relatedTextPane.addStyle("Red Italic", null);
        StyleConstants.setForeground(relatedStyle, Color.blue);
        StyleConstants.setItalic(relatedStyle, true);
        StyleConstants.setForeground(relatedStyle, Color.red);
        StyleConstants.setItalic(relatedStyle, true);
        focusTextPane.setMargin(new Insets(5,15,5,5));
        focusTextPane.setEditable(false);
        focusTextPane.setContentType("text/plain");
        Style focusStyle = focusTextPane.addStyle("Red Italic", null);
        StyleConstants.setForeground(focusStyle, Color.red);
        StyleConstants.setItalic(focusStyle, true);
        focusStyle = focusTextPane.addStyle("Blue Italic", null);
        StyleConstants.setForeground(focusStyle, Color.blue);
        StyleConstants.setItalic(focusStyle, true);
        queryTextPane.setMargin(new Insets(5,15,5,5));
        queryTextPane.setEditable(false);
        queryTextPane.setContentType("text/plain");
        // Puts the text areas and headers in their scrollPanes.
        scrollRelatedText = new JScrollPane(relatedTextPane);
        scrollRelatedText.setColumnHeaderView(relatedTextHeader);
        scrollFocusText = new JScrollPane(focusTextPane);
        scrollFocusText.setColumnHeaderView(focusTextHeader);
        scrollQuery= new JScrollPane(queryTextPane);
    }
    
    /** highLightSharedArcs: Applies a color to the shared arcs in the 
     * reconstructed focus doc and in the related text. Both texts get the 
     * shared tokens in blue. Tokens from the seed token list are highlighted 
     * in red.
     * @param docNr : doc-id (int) of the document selected in the matrix table.
     */
    private void highLightSharedArcs(int docNr)
    {
        List labelList;
        DocNode node = mSimMatrix.getDocNode(docNr, mFocusDoc);
        labelList = mArcTable.makeLabelList(node.getSharedVertices());
        // Shifts the start of the color area to avoid coloring the general 
        // information at the beginning of the panel.
        rec.setOffset(2);
        if(!labelList.isEmpty())
        {
            rec.applyColor(focusTextPane, labelList, 0);
            rec.applyColor(relatedTextPane, labelList, 0);
        }
        if(!mQueryTokens.isEmpty())
        {
            rec.applyColor(focusTextPane, mQueryTokens, 1);
            rec.applyColor(relatedTextPane, mQueryTokens, 1);
        }
    }
    
    /** prepareKeywords: Returns a selected maximum of keywords typical 
     * for a semantic cluster.
     * @param arcSet : HashSet with arc-keys (String) to be rendered as labels.
     * @return String with keywords.
     */
    private String prepareKeywords(Set arcSet)
    {
        List labels = mArcTable.makeLabelList(arcSet);
        StringBuffer keywordString = new StringBuffer();
        keywordString.append("[ ");
        Iterator label_itr = labels.listIterator();
        while(label_itr.hasNext())
        {
            keywordString.append(label_itr.next().toString()).append(", ");
        }
        // Replaces last comma with bracket.
        int end = keywordString.length() - 1;
        int start = end - 1;
        keywordString = keywordString.replace(start, end, " ]");
        return keywordString.toString();
    }
    
    /** selectArcs: Collects in one set the most relevant arc-keys from all 
     * docs that make up a cluster.
     * These are arcs that appear in as many documents as possible.
     * @param clusterSet : HashSet of doc-ids (Integer) that make up a cluster.
     * @param maxWords : the maximum number (int) of arcs to return.
     * @return a List with arc-keys (String) to be rendered as labels and 
     * shown in the GUI.
     */
    private List selectArcs(Set clusterSet, int maxWords)
    {
        List arcList = new ArrayList(maxWords);
        int found = 0;
        int clusterSize = clusterSet.size();
        List tmpList = new ArrayList();
        // Collects a list with all arc-keys belonging to cluster document.
        Iterator list_itr = mDocArcList.listIterator();
        while(list_itr.hasNext())
        {
            List arcArray = (ArrayList) list_itr.next();
            Integer docId = (Integer) arcArray.get(0);
            if(clusterSet.contains(docId))
            {
                tmpList.addAll((LinkedList) arcArray.get(1));
                found++;
            }
            // Stops searching when all the relevant documents were inspected.
            if(found == clusterSize) break;
        }
        // Fills the return list with the necessary arc keys from high to 
        // low frequency up to the maximum allowed.
        for(int i = 0; i < maxWords && !tmpList.isEmpty(); i++)
        {
            String highArc = getHighFrequent(tmpList);
            if(!highArc.equals("")) arcList.add(i, highArc);
            List thisString = new ArrayList();
            thisString.add(highArc);
            tmpList.removeAll(thisString);
        }
        return arcList;
    }
    
    /** getHighFrequent: Return the arcKey with the highest frequency in this 
     * list.
     * @param arcList : ArrayList with arckeys (String);
     * @return arc (String) with the highest frequency in this list.
     */
    private String getHighFrequent(List arcList)
    {
        int high = 0;
        String arc = "";
        Iterator arc_itr = arcList.listIterator();
        while(arc_itr.hasNext())
        {
            String arcKey = arc_itr.next().toString();
            int freq = Collections.frequency(arcList, arcKey);
            if(freq > high)
            {
                high = freq;
                arc = arcKey;
            }
        }
        return arc;
    }
    
    /** sortAllRows: Regardless of sort order (ascending or descending),
     * null values always appear last.
     * @param model : the default table model.
     * @param colIndex : colIndex (int) specifies a column in model.
     * @param ascending : boolean indicates direction of the sort.
     */
    private void sortAllRows(DefaultTableModel model, int colIndex, 
            boolean ascending)
    {
        Vector data = model.getDataVector();
        Collections.sort(data, new ColumnSorter(colIndex, ascending));
        model.fireTableStructureChanged();
    }
    
    // This comparator is used to sort vectors of table data.
    private class ColumnSorter implements Comparator
    {
        int colIndex;
        boolean ascending;
        
        ColumnSorter(int colIndex, boolean ascending)
        {
            this.colIndex = colIndex;
            this.ascending = ascending;
        }
        
        @Override
        public int compare(Object a, Object b)
        {
            Vector v1 = (Vector)a;
            Vector v2 = (Vector)b;
            Object o1 = v1.get(colIndex);
            Object o2 = v2.get(colIndex);
            // Treat empty strings like nulls.
            if (o1 instanceof String && ((String)o1).length() == 0)  o1 = null;
            if (o2 instanceof String && ((String)o2).length() == 0)  o2 = null;
            // Sort nulls so they appear last, regardless of sort order.
            if (o1 == null && o2 == null) return 0;
            else if (o1 == null)  return 1;
            else if (o2 == null)  return -1;
            else if (o1 instanceof Comparable)
            {
                if (ascending)  return ((Comparable)o1).compareTo(o2);
                else return ((Comparable)o2).compareTo(o1);
            }
            else
            {
                if (ascending) return o1.toString().compareTo(o2.toString());
                else return o2.toString().compareTo(o1.toString());
            }
        }
    }
    
    /** getParameters: Returns a string with the parameter settings used for 
     * this task.
     * @return String with the number of main cores used and the document set
     * selected.
     */
    private String getParameters()
    {
        return mParameters;
    }
    
    /** setParameters: Collects the document set used in the doc-by-doc 
     * similarity and the number of main cores in the final selection.
     */
    private void setParameters()
    {
        String coresUsed = "- Main cores used: " + ApplicationManager
                .getCoreNumber();
        String docSet = " - Doc selection: ";
        int selection = ApplicationManager.getRetrievedSelection();
        switch(selection)
        {
            case 0 -> docSet += "highest";
            case 1 -> docSet += "highUp";
            case 2 -> docSet += "middleUp";
            case 3 -> docSet += "all";
        }
        // The group of documents with the highest number of facets in
        // common with the input.
        // High count upwards (includes Highest count).
        // Middle count upwards (includes Case 1).
        // Includes all documents from low up to the highest.
        mParameters = "Parameter settings " + coresUsed + docSet;
    }
    
    /** saveViewRequest: If the 'Save this view' button is pressed, all the data
     * needed to build this view are saved on disk and can be retrieved later
     * without the need to run the retrieval task again.
     * @param event : the button 'Save' was pressed.
     */
    private void saveViewRequest(ActionEvent event) throws IOException
    {
        if(mClusterMap.isEmpty()) getMessage(1);
        else
        {
            String dirPath = ApplicationManager.getResultPath();
            String fileName = mScope + getTaskName() + ".sim";
            saveFile(dirPath, fileName);
        }
    }
    
    /** saveFile: Makes a pathname and saves the file after checking for
     * duplicate filenames.
     * @param dirPath : String with pathname of the file to save.
     * @param fileName : String with the name of the file.
     */
    private void saveFile(String dirPath, String fileName)
    {
        String fullPath = dirPath + fileName;
        File file = new File(fullPath);
        if(file.exists())
        {
            int answer = JOptionPane.showConfirmDialog(similarityPane, 
                    "A file for task " + getTaskName() + " exists already. "
                            + "Continue?\nPress 'Yes' to override or 'No' to" +
                " attribute a new name", "Query Results", +
                        JOptionPane.YES_NO_OPTION);
            if(answer == JOptionPane.YES_OPTION) writeSimilarityData(fullPath,
                    fileName);
            // Recursively searches for a unique name.
            else
            {
                fileName = getNewName(fileName);
                saveFile(dirPath, fileName);
            }
        }
        else writeSimilarityData(fullPath, fileName);
    }
    
    /** getNewName: Returns a new file name.
     * @param oldTaskName : the original file name (String).
     * @return String with a new sequential number added to the existing 
     * task name.
     */
    private String getNewName(String oldTaskName)
    {
        int nameCount;
        String name = oldTaskName.substring(0, oldTaskName.length() - 4);
        int last = name.length()-3;
        String newName = name.substring(0, last);
        nameCount = Integer.parseInt(name.substring(last)) + 1;
        if(nameCount < 10) newName = newName + "00" + nameCount;
        else if (nameCount < 100) newName = newName + "0" + nameCount;
        else newName = newName + "" + nameCount;
        return newName + ".sim";
    }
    
    /** writeSimilarityData: Collects the data and writes it all to disk.
     * @param path : the complete path to this file (String).
     * @param fileName : name of the file (String).
     */
    private void writeSimilarityData(String path, String fileName)
    {
        try
        {
            String clusterPath = path.replace(".sim", ".xml");
            io.writeThisObject(path, getExport());
            io.writeASCII(clusterPath, getWordExport());
            getMessage(2);
            setTaskName(fileName.substring(17, fileName.lastIndexOf(".")));
            saveBtn.setEnabled(false);
        }
        catch (IOException ex)
        {
            getMessage(3);
            ex.printStackTrace(System.err);
        }
    }
    
    /** prepareExport: Collects everything that has to be saved to allow the 
     * reconstructing of this viewer.
     */
    private LinkedList getExport()
    {
        LinkedList dataList = new LinkedList();
        dataList.add(0, mArcTable);
        dataList.add(1, mSummary);
        dataList.add(2, mSimMatrix);
        dataList.add(3, mQueryTokens);
        dataList.add(4, mDocArcList);
        dataList.add(5, mClusterMap);
        dataList.add(6, mAllDocMap);
        dataList.add(7, mQueryString);
        dataList.add(8, mBeginDate);
        dataList.add(9, mEndDate);
        dataList.add(10, mParameters);
        dataList.add(11, mPrototype);
        return dataList;
    }
    
    /** getTaskName: Returns the name for this task.
     * @return name (String)
     */
    public static String getTaskName()
    {
        return mTask;
    }
    
    /** setTaskName: Sets the full name for this task.
     * @param task : unique name for this task (String).
     */
    private void setTaskName(String task)
    {
        mTask = task;
    }
    
    /** enableEvaluation: the evaluation button is turned on when a result map
     * is avaible. Activated by the ApplicationManager.
     */
    public static void enableEvaluation()
    {
        evalBtn.setEnabled(true);
    }
    
    /** evaluateRequest: If the 'Evaluate' button is pressed, Evaluate class 
     * will perfom an evaluation of the retrieved data  against the official 
     * TDT standard. Results are show on the 'General Information' tab.
     * @param event : the button 'Evaluate' was pressed.
     */
    private void evaluateRequest(ActionEvent event)
    {
        Evaluation evaluator = new Evaluation();
        evaluator.evaluate(mDocTable, mScope, mBeginDate, mEndDate,
                getTaskName());
        evalBtn.setEnabled(false);
    }
    
    /** getMessage: Issues various warning messages.
     * @param msge : a message identification (int).
     */
    private void getMessage(int msge)
    {
        switch(msge)
        {
            case 1 -> JOptionPane.showMessageDialog(similarityPane,
                "Nothing to save!", "Query Results", JOptionPane.ERROR_MESSAGE);
            case 2 -> JOptionPane.showMessageDialog(similarityPane,
                "All data for task " + getTaskName() 
                        + "\nwere successfully saved.",
                "Query Results", JOptionPane.INFORMATION_MESSAGE);
            case 3 -> JOptionPane.showMessageDialog(similarityPane, 
                    "IO-exception encountered.", "Query Results", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /** prepareWordExport: Prepares to save the cluster data as an XML-file.
     * @param cluster : ArrayList<String> describing the different document
     * clusters with its keywords.
     */
    private void prepareWordExport(ArrayList<String> cluster)
    {
        String word = "<?xml version=\"1.0\"?> <?mso-application progid=\"Word.Document\"?> " +
            "<w:wordDocument xmlns:w= \"http://schemas.microsoft.com/office/word/2003/wordml\">" +
            "<w:body> <w:p> <w:r>";
        for(int i = 0; i < cluster.size(); i++)
        {
            String s = (String) cluster.get(i);
            word += "<w:t>" + s + "</w:t> <w:br/>";
        }
        word += "</w:r> </w:p> </w:body> </w:wordDocument>";
        setWordExport(word);
    }
    
    /** setWordExport: XML-formated cluster data.
     * @param wordCluster : cluster data as an XML-file (String)
     */
    private void setWordExport(String wordCluster)
    {
        mWord = wordCluster;
    }
    
    /** getClusterWord: Returns XML-formated cluster data to be saved on disk
     * in the Result directory.
     * @return the String with cluster data.
     */
    public String getWordExport()
    {
        return mWord;
    }
    
}
