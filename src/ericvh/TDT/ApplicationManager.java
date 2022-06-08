package ericvh.TDT;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
/**
 * Class ApplicationManager starts the application, builds the GUI and
 * dispatches the work to be done to the different collaborating classes.
 * Created on 20 mei 2004, 20:50 Review Mrch 2022
 *
 * @author Eric Van Horenbeeck
 */
public class ApplicationManager extends JPanel implements Serializable
{
    // Where the raw data are.
    private static final String corpusFiles = "\\TopicalFacets\\Computing\\Corpus";
    // Location of the files after preprocessing.
    private static final String workFiles = "\\TopicalFacets\\Computing\\Processed";
    // Miscellaneous variables declarations
    private static final String separator = System.getProperty("file.separator");
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private Clipboard clip;
    private DefaultHighlightPainter painter;
    private static String icon = "";        // Newspaper icon in the menu bar of GUI.
    private final int PREPROCESS = 1;       // Task: html-parsing and tokenizing of a file.
    private final int NETWORK = 2;          // Task: building the text network.
    private final int LINKS = 3;            // Task: collecting links between tokens.
    private final int TFIDF = 4;            // Task: computes the information value (modified TFIDF).
    private final int ASSOCIATE = 5;        // Task: finds informative arcs and associations.
    private final int TOPICS = 6;           // Task: searching for topics in the network.
    private final int CONSOLIDATE = 7;      // Task: combines all learning.
    private final int RETRIEVE = 8;         // Task: retrieves data.
    private final int RECONSTRUCT = 9;      // Task: reconstruct
    private final int EVALUATE = 10;        // Task: evaluation of the retrieval tasks.
    private final int DOCSIM = 11;          // Task: combining files.
    private final long appStart = 0;              // Moment this application started.
    private int emptyCount = 0;             // Counts empty files.
    private int scopeSpan = 0;              // Number of days between begin- and enddate.
    private static int fileCount = 0;       // Counting the files being processed.
    private static int filesToProcess = 0;  // Total number of files to process.
    private static int tokensToShow = 0;    // Default number of tokens in reconstructed text.
    private static int progress;            // Id of the task being handled.
    private static long mAppStart = 0;      // Time in milliseconds at the start of this session.
    private boolean countOnly = true;       // Shows count of info-tokens if 'true',else the labels.
    private boolean noLabels = true;        // No labels in the topic-facet view.
    private boolean docListExists = false;  // List from previous sessions is not empty.
    private static String communityName = "";   // Name of subfolder with Result- and Workfile maps.
    private String sourceName = "";         // Name of the source of a text file.
    private static String mResultPath = ""; // The path to saved query data.
    private static String prevProgressText = ""; // Text being displayed in the progressLabel.
    private static String taskName = "";    // Name of the selected task.
    private static String beginDateString = "";   // beginDate of interval as String.
    private static String endDateString = "";     // endDate of interval as String.
    private static String isAllDates = "false"; //boolean indicating whether all data are to be processed.
    private static String scope = "";       // Date interval string for file identification.
    private static String myText = "";      // String used to output result to the screen.
    private static String dataStore = "";   // String with the name of the raw data directory.
    private static String dataPath = "";   // String with the path to the raw data directory.
    private static String graphStore = "";  // String with the name of the text graph directory.
    private static String graphPath = "";   // String with the path to the text graph directory.
    private static String[] fingerPrint;    // Characteristics of an input file with raw data.
    private static List selectedFiles;      // Array with files selected by the user.
    private static List<Integer> tabArray;  // Array with titles of selected tabs added by the user.
    private static List dataFileList;       // Array with the location of the datafiles.
    private static LinkedList docMatrixList; // List with matrix nodes.
    private static LinkedList unusedList;   // List with unused doc-ids to be rescanned.
    private static LinkedList mQuery;       // List with token ids to reconstruct a query.
    private final ArrayList<String> taskList; // List with tasks this app can perfom
    private final ArrayList<String> sourceList; // List with available corpus sources
    private final ArrayList<String> communityList; // List with communities.
    private static ArrayList<Integer> mBaseLineArray; // Array with common words.
    private LinkedList rescannedList;       // List with doc-ids that were reused.
    private Map arcsMap;                    // Map containing all arcs in a document.
    private static Map infoMap;             // HashMap with all infoTokens and weights.
    private SortedMap scopeMap;             // Map to collect all files with the same scope.
    private static Map infoArcMap;          // Map with informative arcs per document.
    private static Map assocMap;            // Map with associations.
    private static Map seedMap;             // Map with categoryName and seeds.
    private SortedSet docsRetrieved;        // Set with retrieved doc-ids.
    private static Set mUnknownTokenSet;    // Set with tokens unknown from QueryInput.
    private static File dir;                // Current directory of the application.
    private static Calendar beginDate = null;     // Allows the setting of a date interval...
    private static Calendar endDate = null;       // ...when selecting files.
    private static Calendar limitDate = null;     // Before this date no old files are accepted.
    // Classes needed to run the application.
    private static LabelTable lt;          // LabelTable token-label is key, the id is its value.
    private TokenCount tc;                 // TokenCount count of all tokens in every doc.
    private static VerticesTable vt;       // Table holding the individual vertices.
    private static DocTable dt;            // DocTable: document information.
    private static CollectionTable ct;     // CollectionTable: collection information.
    private static LinkTable lkt;          // LinkTable: methods to collect links per vertex.
    private NetworkBuilder gb;               // Class with methods to construct the text network.
   // private NewsParser parseDocument;          // Class with parsing methods.
    private TikaParser parseDocument;          // Class with parsing methods.
    private FacetCollector collector;     // Class consolidates topic data.
    private ResultExport results;          // Class to collect and save the results of a retrieval.
    private Evaluation evaluator;          // Class to evaluate the results of a retrieval task.
    private static FinalViewer simView;   // Presents the results of a query taks in a GUI.
    private SwingWorker worker;            // Provides a separate thread on the main application.
    private static final InputOutput io = new InputOutput(); // Reading and writing methods.
    private final Lock lock = new ReentrantLock();  // Wait/notify for user query to appear.
    private final Condition notReady;
    private boolean mRunBaseLineFilter; //User selected choice when calculating the informative values.
    private static TreeMap mOriginalFilenames; // Dictionary with internal and original filenames.
    /**
     * ApplicationManager Constructor: initializes the GUI and its
     * functionalities. The necessary GUI components are declared at the bottom
     * of this class file.
     */
    public ApplicationManager()
    {
        super(new BorderLayout());
        this.notReady = lock.newCondition();
        mainFrame = new JFrame("Topic Detection and Tracking");
        // Creates tabbed textPanes and adds them to the main frame.
        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(true);
        messageArea = new JTextArea [5];
        for (int i = 0; i < 3; i++)
        {
            messageArea[i] = new JTextArea ();
            messageArea[i].setFocusable(true);
            messageArea[i].setMargin(new Insets(5, 15, 5, 5));
            messageArea[i].setEditable(false);
            messageArea[i].setLineWrap(true);
            messageArea[i].addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    //right mouse click event
                    if (SwingUtilities.isRightMouseButton(e) 
                            && e.getClickCount() == 1)
                    {
                        cutpasteMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            JScrollPane messagePane = new JScrollPane(messageArea[i]);
            messagePane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.
                    VERTICAL_SCROLLBAR_AS_NEEDED);
            tabbedPane.add(getTabTitle(i), messagePane);
        }
        add(tabbedPane, BorderLayout.CENTER);
        tabArray = new ArrayList<>();
        // Creates a file chooser, file saver and progress bar.
        fc = new JFileChooser();
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressLabel = new JLabel("");
        // Initializes the menu bar and its components.
        menuBar = new JMenuBar();
        dir = fc.getCurrentDirectory();
        icon = dir.toString() + workFiles + separator + "tdtIcon.gif";
        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        copyItem = new JMenuItem("Copy");
        copyItem.addActionListener((ActionEvent event) ->
        {
            copyItemActionPerformed(event);
        });
        findItem = new JMenuItem("Find");
        findItem.addActionListener((ActionEvent event) ->
        {
            findItemActionPerformed(event);
        });
        selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener((ActionEvent event) ->
        {
            selectAllItemActionPerformed(event);
        });
        clearItem = new JMenuItem("Clear");
        clearItem.addActionListener((ActionEvent event) ->
        {
            clearItemActionPerformed(event);
        });
        aboutMenu = new JMenu("About");
        aboutMenuItem = new JMenuItem("The TDT-Project");
        aboutMenuItem.addActionListener((ActionEvent event) ->
        {
            aboutActionPerformed(event);
        });
        aboutText = new JTextArea();
        aboutText.setEditable(false);
        aboutText.setMargin(new Insets(10, 10, 10, 10));
        aboutFrame = new JFrame("The TDT-Project");
        aboutFrame.setIconImage(new ImageIcon(icon).getImage());
        aboutFrame.getContentPane().add(aboutText);
        aboutFrame.setLocation(90, 20);
        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener((ActionEvent event) ->
        {
            exitMenuItemActionPerformed(event);
        });
        saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener((ActionEvent event) ->
        {
            saveMenuItemActionPerformed(event);
        });
        dirMenuItem = new JMenuItem("Select Main Directory");
        dirMenuItem.addActionListener((ActionEvent event) ->
        {
            dirMenuItemActionPerformed(event);
        });
        // Construction of the main action menu.
        actionMenu = new JMenu("Action");
        // Initializes community selection
        communityFrame = new JFrame("Community");
        communityFrame.setResizable(false);
        communityFrame.setIconImage(new ImageIcon(icon).getImage());
        communityList = getIniList("tdtCommunity.txt", dir + workFiles + separator);
        String[] communities = communityList.toArray(new String[communityList.size()]);
        communityBox = new JComboBox(communities);
        confirmCommunityBtn = new JButton("Confirm");
        communityBox.setSelectedIndex(0);
        communityLabel = new JLabel("Select Community");
        Container communityContent = communityFrame.getContentPane();
        communityContent.setLayout(null);
        communityContent.add(communityLabel).setBounds(10, 10, 140, 20);
        communityContent.add(communityBox).setBounds(10, 40, 120, 30);
        communityContent.add(confirmCommunityBtn).setBounds(140, 40, 80, 30);
        communityMenuItem = new JMenuItem("Select Community");
        communityMenuItem.addActionListener((ActionEvent event) ->
        {
            communityMenuItemActionPerformed(event);
        });
        // Initializes source selection.
        sourceFrame = new JFrame("Source");
        sourceFrame.setResizable(false);
        sourceFrame.setIconImage(new ImageIcon(icon).getImage());
        sourceList = getIniList("tdtSource.txt", dir + workFiles + separator);
        String[] sources = sourceList.toArray(new String[sourceList.size()]);
        sourceBox = new JComboBox(sources);
        confirmSourceBtn = new JButton("Confirm");
        sourceBox.setSelectedIndex(0);
        sourceLabel = new JLabel("Select Source");
        sourceLabel2 = new JLabel("Select 'Undefined' to process all files");
        Container sourceContent = sourceFrame.getContentPane();
        sourceContent.setLayout(null);
        sourceContent.add(sourceLabel).setBounds(10, 10, 85, 20);
        sourceContent.add(sourceBox).setBounds(10, 40, 210, 30);
        sourceContent.add(confirmSourceBtn).setBounds(230, 40, 80, 30);
        sourceContent.add(sourceLabel2).setBounds(10, 80, 240, 20);
        sourceMenuItem = new JMenuItem("Select Source");
        sourceMenuItem.addActionListener((ActionEvent event) ->
        {
            sourceMenuItemActionPerformed(event);
        });
        // Initializes scope selection (begin date - end date).
        scopeFrame = new JFrame("Time Interval Setting");
        scopeFrame.setResizable(false);
        scopeFrame.setIconImage(new ImageIcon(icon).getImage());
        startDay = new JTextField("dd");
        startDay.selectAll();
        startMonth = new JTextField("mm");
        startMonth.selectAll();
        startYear = new JTextField("yyyy");
        startYear.selectAll();
        startLabel = new JLabel("Start Date");
        endDay = new JTextField("dd");
        endDay.selectAll();
        endMonth = new JTextField("mm");
        endMonth.selectAll();
        endYear = new JTextField("yyyy");
        endYear.selectAll();
        endLabel = new JLabel("End date");
        timeLimit = new JTextField("365");
        timeLimit.selectAll();
        limitLabel = new JLabel("Time Limit (days)");
        allDateBx = new JCheckBox();
        allDateBx.setSelected(false);
        allDatesLabel = new JLabel("All Dates");
        Container scopeContent = scopeFrame.getContentPane();
        scopeContent.setLayout(null);
        scopeContent.add(startDay).setBounds(10, 10, 26, 20);
        scopeContent.add(startMonth).setBounds(40, 10, 26, 20);
        scopeContent.add(startYear).setBounds(70, 10, 33, 20);
        scopeContent.add(startLabel).setBounds(110, 10, 65, 20);
        scopeContent.add(endDay).setBounds(10, 45, 26, 20);
        scopeContent.add(endMonth).setBounds(40, 45, 26, 20);
        scopeContent.add(endYear).setBounds(70, 45, 33, 20);
        scopeContent.add(endLabel).setBounds(110, 45, 65, 20);
        scopeContent.add(allDateBx).setBounds(175, 8, 20, 20);
        scopeContent.add(allDatesLabel).setBounds(200, 8, 65, 20);
        scopeContent.add(timeLimit).setBounds(175, 45, 30, 20);
        scopeContent.add(limitLabel).setBounds(210, 45, 130, 20);
        confirmScopeBtn = new JButton("Confirm");
        scopeFrame.getContentPane().add(confirmScopeBtn).setBounds(10, 80, 80, 30);
        scopeMenuItem = new JMenuItem("Set Scope");
        scopeMenuItem.addActionListener((ActionEvent event) ->
        {
            scopeMenuItemActionPerformed(event);
        });
        // Initializes task and baseline selection 
        taskFrame = new JFrame("Task Selection");
        taskFrame.setResizable(false);
        taskFrame.setIconImage(new ImageIcon(icon).getImage());
        taskList = getIniList("tdtTask.txt", dir + workFiles + separator);
        String[] tasks = taskList.toArray(new String[taskList.size()]);
        taskBox = new JComboBox(tasks);
        confirmTaskBtn = new JButton("Confirm");
        taskBox.setSelectedIndex(0);
        taskLabel = new JLabel("Select Task");
        taskLabel2 = new JLabel("Confirm and press 'Start' to proceed");
        
        // A limited list of the most frequent common words from a general
        // language corpus. Initializing with a dummy value to prevent possible
        // NullPointerException downstream.
        mBaseLineArray = new ArrayList<>();
        mBaseLineArray.add(-1);
        baseLineBx = new JCheckBox();
        baseLineBx.setSelected(true);
        baseLineLbl = new JLabel("Baseline Filter");
        baseLineLbl.setToolTipText("Used when calculating the informative value of words");
       
        Container taskContent = taskFrame.getContentPane();
        taskContent.setLayout(null);
        taskContent.add(taskLabel).setBounds(10, 10, 85, 20);
        taskContent.add(baseLineLbl).setBounds(145, 10, 85, 20);
        taskContent.add(baseLineBx).setBounds(230, 10, 20, 20);
        taskContent.add(taskBox).setBounds(10, 40, 150, 30);
        taskContent.add(confirmTaskBtn).setBounds(170, 40, 80, 30);
        taskContent.add(taskLabel2).setBounds(10, 80, 240, 20);
        taskMenuItem = new JMenuItem("Select Task");
        taskMenuItem.addActionListener((ActionEvent event) ->
        {
            taskMenuItemActionPerformed(event);
        });

        // Initializes task related parameter setting.
        paraMenu = new JMenu("Parameters");
        paraFrame = new JFrame("Task related settings");
        paraFrame.setResizable(false);
        paraFrame.setIconImage(new ImageIcon(icon).getImage());
        showAssocBx = new JCheckBox();
        showAssocBx.setSelected(true);
        showAssocCountBx = new JCheckBox();
        showAssocCountBx.setSelected(false);
        showLinkBx = new JCheckBox();
        showLinkBx.setSelected(false);
        showInfoTBx = new JCheckBox();
        showInfoTBx.setSelected(true);
        showCountBx = new JCheckBox();
        showCountBx.setSelected(false);
        showTopicsBx = new JCheckBox();
        showTopicsBx.setSelected(true);
        showNoLabelBx = new JCheckBox();
        showNoLabelBx.setSelected(false);
        showFacetBx = new JCheckBox();
        showFacetBx.setSelected(true);
        showSimBx = new JCheckBox();
        showSimBx.setSelected(true);
        showAssocLabel = new JLabel("Show Associations");
        showAssocCountLabel = new JLabel("No Labels");
        showLinksLabel = new JLabel("Show Links");
        showInfoTLabel = new JLabel("Show Info Tokens");
        showCountLabel = new JLabel("No Labels");
        showTopicsLabel = new JLabel("View All Topical Facets");
        showNoLabelLabel = new JLabel("No Labels");
        showFacetLabel = new JLabel("View Retrieved Facets");
        showSimLabel = new JLabel("Show Results");
        coreNumber = new JTextField("5");
        coreLabel = new JLabel("Main cores to extract");
        retrievePanel = new JPanel(new GridLayout(1, 0));
        retrieveGroup = new ButtonGroup();
        highRB = new JRadioButton();
        highRB.setActionCommand("Highest");
        middleRB = new JRadioButton();
        middleRB.setActionCommand("HighUp");
        lowRB = new JRadioButton();
        lowRB.setActionCommand("MidUp");
        allRB = new JRadioButton();
        allRB.setActionCommand("LowUp");
        retrieveGroup.add(highRB);
        retrieveGroup.add(middleRB);
        retrieveGroup.add(lowRB);
        retrieveGroup.add(allRB);
        retrievePanel.add(highRB);
        retrievePanel.add(middleRB);
        retrievePanel.add(lowRB);
        retrievePanel.add(allRB);
        retrieveLabel = new JLabel("Retrieved docs");
        retrievePanelLabel = new JLabel("Highest  HighUp  MidUp  LowUp");
        confirmParaBtn = new JButton("Confirm");
        Container paraContent = paraFrame.getContentPane();
        paraContent.setLayout(null);
        paraContent.add(showLinksLabel).setBounds(10, 10, 100, 20);
        paraContent.add(showLinkBx).setBounds(148, 10, 20, 20);
        paraContent.add(showInfoTLabel).setBounds(10, 35, 125, 20);
        paraContent.add(showInfoTBx).setBounds(148, 35, 20, 20);
        paraContent.add(showCountLabel).setBounds(178, 35, 125, 20);
        paraContent.add(showCountBx).setBounds(240, 35, 20, 20);
        paraContent.add(showAssocLabel).setBounds(10, 60, 125, 20);
        paraContent.add(showAssocBx).setBounds(148, 60, 20, 20);
        paraContent.add(showAssocCountLabel).setBounds(178, 60, 125, 20);
        paraContent.add(showAssocCountBx).setBounds(240, 60, 20, 20);
        paraContent.add(showTopicsBx).setBounds(148, 85, 20, 20);
        paraContent.add(showTopicsLabel).setBounds(10, 85, 130, 20);
        paraContent.add(showNoLabelLabel).setBounds(178, 85, 125, 20);
        paraContent.add(showNoLabelBx).setBounds(240, 85, 20, 20);
        paraContent.add(showFacetBx).setBounds(148, 110, 20, 20);
        paraContent.add(showFacetLabel).setBounds(10, 110, 150, 20);
        paraContent.add(showSimBx).setBounds(148, 135, 20, 20);
        paraContent.add(showSimLabel).setBounds(10, 135, 120, 20);
        paraContent.add(retrievePanelLabel).setBounds(82, 165, 175, 20);
        paraContent.add(retrieveLabel).setBounds(10, 184, 100, 20);
        paraContent.add(retrievePanel).setBounds(102, 185, 160, 20);
        paraContent.add(coreLabel).setBounds(10, 210, 130, 20);
        paraContent.add(coreNumber).setBounds(148, 210, 20, 20);
        paraContent.add(confirmParaBtn).setBounds(10, 240, 80, 30);
        paraMenuItem = new JMenuItem("Set GUI-Parameters");
        paraMenuItem.addActionListener((ActionEvent event) ->
        {
            paraMenuItemActionPerformed(event);
        });
        // Initializes look up utility.
        lookUpMenu = new JMenu("Look Up");
        lookUpFrame = new JFrame("Label & Doc Look Up");
        lookUpFrame.setResizable(true);
        lookUpFrame.setIconImage(new ImageIcon(icon).getImage());
        lookUpQuery = new JTextField();
        lookUpAnswer = new JTextField();
        lookUpAnswer.setEditable(true);
        queryLabel = new JLabel("Look up:");
        answerLabel = new JLabel("Answer:");
        vertexLookUpBtn = new JButton("Label");
        labelLookUpBtn = new JButton("Vertex #");
        docNameLookUpBtn = new JButton("<html>Internal<br>Filename</html>");
        orgDocLookUpBtn = new JButton("<html>Original<br>Filename</html>");
        docIdLookUpBtn = new JButton("<html>Doc<br>ID</html>");
        copyBtn = new JButton("Copy");
        clearBtn = new JButton("Clear");
        confirmLookUpBtn = new JButton("Quit");
        allOrgNamesBtn = new JButton("All Org.Filenames");
        Container lookUpContent = lookUpFrame.getContentPane();
        lookUpContent.setLayout(null);
        lookUpContent.add(queryLabel).setBounds(27, 9, 65, 20);
        lookUpContent.add(lookUpQuery).setBounds(95, 10, 230, 20);
        lookUpContent.add(vertexLookUpBtn).setBounds(15, 35, 82, 20);
        lookUpContent.add(labelLookUpBtn).setBounds(15, 60, 82, 20);
        lookUpContent.add(docNameLookUpBtn).setBounds(103, 37, 78, 40);
        lookUpContent.add(orgDocLookUpBtn).setBounds(185, 37, 78, 40);
        lookUpContent.add(docIdLookUpBtn).setBounds(267, 37, 55, 40);
        lookUpContent.add(answerLabel).setBounds(27, 86, 65, 20);
        lookUpContent.add(lookUpAnswer).setBounds(95, 87, 230, 20);
        lookUpContent.add(copyBtn).setBounds(15, 115, 82, 20);
        lookUpContent.add(clearBtn).setBounds(101, 115, 82, 20);
        lookUpContent.add(allOrgNamesBtn).setBounds(189, 115, 133, 20);
        lookUpContent.add(confirmLookUpBtn).setBounds(15, 142, 82, 20);
        
        lookUpFrame.pack();
        lookUpMenuItem = new JMenuItem("Look up an index");
        lookUpMenuItem.addActionListener((ActionEvent event) ->
        {
            lookUpMenuItemActionPerformed(event);
        });

        // Puts the main components in the menubar using FlowLayout.
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(lookUpMenu);
        menuBar.add(paraMenu);
        menuBar.add(actionMenu);
        menuBar.add(aboutMenu);
        fileMenu.add(saveMenuItem);
        fileMenu.add(exitMenuItem);
        fileMenu.add(dirMenuItem);
        editMenu.add(copyItem);
        editMenu.add(findItem);
        editMenu.add(selectAllItem);
        editMenu.add(clearItem);
        actionMenu.add(communityMenuItem);
        actionMenu.add(sourceMenuItem);
        actionMenu.add(scopeMenuItem);
        actionMenu.add(taskMenuItem);
        paraMenu.add(paraMenuItem);
        lookUpMenu.add(lookUpMenuItem);
        aboutMenu.add(aboutMenuItem);
        // Adds the progressbar and a 'start' and 'cancel' button at the bottom 
        // of the main frame.
        progressPanel = new JPanel();
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener((ActionEvent event) ->
        {
            cancelPerformed(event);
        });
        cancelBtn.setEnabled(true);
        startBtn = new JButton("Start");
        startBtn.addActionListener((ActionEvent event) ->
        {
            startPerformed(event);
        });
        startBtn.setEnabled(true);
        progressPanel.add(startBtn);
        progressPanel.add(cancelBtn);
        progressPanel.add(progressBar);
        progressPanel.add(progressLabel);
        add(progressPanel, BorderLayout.SOUTH);
        setSettings();

        // Cut, copy, paste popup with rght mouse click events
        cutMenuItem.addActionListener((ActionEvent event ) ->
        {
            mouseActionPerformed(event);
        });
        copyMenuItem.addActionListener((ActionEvent event ) ->
        {
            mouseActionPerformed(event);
        });
        pasteMenuItem.addActionListener((ActionEvent event ) ->
        {
            mouseActionPerformed(event);
        });
        cutpasteMenu.add(cutMenuItem);
        cutpasteMenu.add(copyMenuItem);
        cutpasteMenu.add(pasteMenuItem);

    } // End of the ApplicationManager GUI constructor.

    /**
     * Utility to handle right mouseclick in a JTextArea allowing cut, copy, paste
     * actions on selected text.
     * @param evt the mouse trigger
     */
    public void mouseActionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();
        if (source == cutMenuItem)
        {
            JTextArea jte = (JTextArea) cutpasteMenu.getInvoker();
            jte.cut();
        }
        if (source == copyMenuItem)
        {
            JTextArea jte = (JTextArea) cutpasteMenu.getInvoker();
            jte.copy();
        }
        if (source == pasteMenuItem)
        {
            JTextArea jte = (JTextArea) cutpasteMenu.getInvoker();
            jte.paste();
        }
    }
    
    /**
     * addLinkTable: Adds a tabbedPane with links for every collection to the
     * mainFrame. Called by the LinkTable.
     *
     * @param pane : a tabbedPane with in and out links of vertices in a
     * Collection (LinkTable).
     * @param title : the number of the tabbed pane title (int).
     */
    public static void addLinkTable(JTabbedPane pane, int title)
    {
        String tabTitle = getTabTitle(title);
        tabbedPane.add(tabTitle, pane);
        int tabIndex = tabbedPane.indexOfTab(tabTitle);
        finalizeNewTab(tabIndex);
    }

    /**
     * addTabbedPane: MatrixTable adds a tableFrame with matrix data in a
     * tabbedPane indicated by this tab index.
     *
     * @param panel : a panel with a list of documents and a table with the
     * similarity matrix.
     * @param title : the number of the tabbed pane title (int).
     */
    public static void addTabbedPane(JPanel panel, int title)
    {
        String tabTitle = getTabTitle(title);
        tabbedPane.add(tabTitle, panel);
        int tabIndex = tabbedPane.indexOfTab(tabTitle);
        finalizeNewTab(tabIndex);
    }

    /**
     * removeTab: Removes the tabbed pane previous to receiving new data.
     *
     * @param tab : the identification number of the tabbed pane (int).
     */
    public static void removeTab(int tab)
    {
        tabbedPane.remove(tab);
        mainFrame.validate();
    }

    /**
     * addTopicPane: FacetViewer adds a scrollable pane with the topic
     * candidates and the related documents.
     *
     * @param scroll : a scrollable panel with topic candidates and documents.
     * @param title : the number of the tabbed pane title (int).
     */
    public static void addTopicPane(JScrollPane scroll, int title)
    {
        String tabTitle = getTabTitle(title);
        tabbedPane.add(tabTitle, scroll);
        int tabIndex = tabbedPane.indexOfTab(tabTitle);
        finalizeNewTab(tabIndex);
    }

    /**
     * addSelectedPane: FacetViewer adds a scrollable pane with a selection of
     * topic candidates.
     *
     * @param scroll : a scrollable panel with topic candidates and documents.
     * @param title : the number of the tabbed pane title (int).
     * @param selection : the selection to be added to the tab title (String).
     */
    public static void addSelectedPane(JScrollPane scroll, int title
            , String selection)
    {
        String tabTitle = getTabTitle(title);
        tabbedPane.add(tabTitle + selection, scroll);
        int tabIndex = tabbedPane.indexOfTab(tabTitle + selection);
        // Keeps the tab titles in an array to allow their removal later.
        tabArray.add(tabIndex);
        // Checks if the remove button exists or is enabled.
        try
        {
            removeBtn.setEnabled(true);
        } // Makes a button if there is none.
        catch (NullPointerException npe)
        {
            addRemoveButton();
        }
        finalizeNewTab(tabIndex);
    }

    /**
     * addRemoveButton: When the user clicks a doc-id in the 'Topics' tab, a new
     * tabbed pane is made with details on the selected doc. At the same time a
     * 'Remove' button is added in the GUI border. When this button is pressed
     * all 'Selected Topic' tabs are removed and the button is made inactive.
     */
    private static void addRemoveButton()
    {
        removeBtn = new JButton("Remove Selected Facets");
        removeBtn.addActionListener((ActionEvent event) ->
        {
            removePerformed(event);
        });
        removeBtn.setEnabled(true);
        progressPanel.add(removeBtn);
        progressPanel.validate();
    }

    /**
     * removePerformed: Removes all the 'Selected Topic' tabs.
     *
     * @param event : 'Remove (...)' button pressed (ActionEvent).
     */
    private static void removePerformed(ActionEvent event)
    {
        try
        {
            Iterator tab_itr = tabArray.iterator();
            while (tab_itr.hasNext())
            {
                int tabIndex = ((Integer) tab_itr.next());
                tabbedPane.remove(tabIndex);
            }
            tabArray.clear();
            removeBtn.setEnabled(false);
            mainFrame.validate();
        } catch (IndexOutOfBoundsException iob)
        {
            // If there is nothing to remove, no action is necessary.
        } catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * finalizeNewTab: Performs housekeeping tasks to finalize the construction
     * of a new tabbed pane.
     *
     * @param tab : the index of the tabbedPane (int).
     */
    private static void finalizeNewTab(int tab)
    {
        mainFrame.validate();
        mainFrame.pack();
        tabbedPane.setVisible(true);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        tabbedPane.setSelectedIndex(tab);
        tabbedPane.requestFocusInWindow();
    }

    /**
     * restoreDown: Returns screen to its original state. Focus in the first
     * tabbed pane.
     */
    public static void restoreDown()
    {
        tabbedPane.setSelectedIndex(0);
        tabbedPane.requestFocusInWindow();
        mainFrame.setSize(905, 400);
        mainFrame.setLocation((screenSize.width - 890) / 2,
                (screenSize.height - 650) / 2);
        mainFrame.validate();
    }

    /**
     * getTabTitle: Returns a title for a tabbed pane in the mainFrame.
     *
     * @param t : tab index (int).
     * @return the tab title (String).
     */
    private static String getTabTitle(int t)
    {
        String tabTitle = "";
        switch (t)
        {
            case 0 ->
                tabTitle = "General Info";
            case 1 ->
                tabTitle = "Informative Tokens";
            case 2 ->
                tabTitle = "Associations";
            case 3 ->
                tabTitle = "Retrieved Facets";
            case 4 ->
                tabTitle = "Topic Chains";
            case 5 ->
                tabTitle = "Selected ";
            case 6 ->
                tabTitle = "All Facets";
            case 7 ->
                tabTitle = "Links";
            case 8 ->
                tabTitle = FinalViewer.getTaskName();
        }
        return tabTitle;
    }

    /**
     * createAndShowGUI: Creates the GUI and shows it.
     */
    private static void createAndShowGUI()
    {
        //Creates and sets up the content pane and the window.
        JComponent newContentPane = new ApplicationManager();
        newContentPane.setOpaque(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.setContentPane(newContentPane);
        mainFrame.setIconImage(getIcon());
        //Displays the window.
        mainFrame.setSize(905, 400);
        mainFrame.setLocation((screenSize.width - 890) / 2,
                (screenSize.height - 650) / 2);
        mainFrame.setVisible(true);
        mainFrame.setResizable(true);
    }

    /**
     * cancelPerformed: Interrupts the execution of the program.
     *
     * @param event : 'cancel' button pressed (ActionEvent).
     */
    private void cancelPerformed(ActionEvent event)
    {
        Runnable cancel = () ->
        {
            try
            {
                worker.interrupt();
                showText("* Program interrupted *", 0);
                updateStatusBar(-1, getElapsedTime());             
            } catch (NullPointerException ne)
            {
                // If there is no thread, there is nothing to cancel.
                showText("No thread to cancel", 0);
            }
        };
        SwingUtilities.invokeLater(cancel);
    }

    /**
     * setSeeds: QueryInput class transfers a map with task names and lists of
     * tokens from the query and a set with tokens unrecognized by the
     * application.
     *
     * @param seeds : HashMap with a task name (String) as key and an ArrayList
     * of tokens (String) from the query that act as seeds in the search for a
     * topic.
     * @param unknownTokens : HashSet with tokens (String) from the query not
     * known by this application.
     * @param query : a LinkedList with the ordered query ids to reconstruct a
     * clean query string.
     */
    public static void setSeeds(Map seeds, Set unknownTokens, LinkedList query)
    {
        seedMap = seeds;
        mUnknownTokenSet = unknownTokens;
        mQuery = query;
    }

    /**
     * getQuery: Returns a list with tokenIds to reconstruct the query for this
     * task.
     *
     * @return a LinkedList with the ordered token ids (Integer) from the query.
     */
    public static LinkedList getQuery()
    {
        return mQuery;
    }

    /**
     * getSeeds: Returns the seedMap to the TopicRetriever class on demand.
     *
     * @return a HashMap with a query task name as key (String) and an ArrayList
     * of tokens (String).
     */
    public static Map getSeeds()
    {
        return seedMap;
    }

    /**
     * getUnknownTokens: Returns a set with unknown tokens gathered by the
     * QueryInput.
     *
     * @return HashSet with tokens (String) unknown to this application.
     */
    public static Set getUnknownTokens()
    {
        return mUnknownTokenSet;
    }

    /**
     * setResultFile: QueryInput sends this path. The file has data on the
     * results of a query task. The FinalViewer will use them to reconstruct an
     * interactive GUI.
     *
     * @param resultPath : full path (String) to query results.
     */
    public static void setResultFile(String resultPath)
    {
        taskName = "Reconstruct";
        mResultPath = resultPath;
    }

    /**
     * findItemActionPerformed: Searches for a string in the visible textArea
     * and adds highlight if found.
     *
     * @param event : 'find' menu item selected (ActionEvent).
     * @ToDo : make switch/case method to select the appropriate text area using
     * the tabindex!
     */
    private void findItemActionPerformed(ActionEvent event)
    {
        String searchString;
        JTextArea textComp = new JTextArea();
        int tabIndex = tabbedPane.getSelectedIndex();
        try
        {
            searchString = JOptionPane.showInputDialog(tabbedPane,
                    "Enter your search term.\nCase sensitive!", "Search",
                    JOptionPane.PLAIN_MESSAGE);
            if (searchString.equals(""))
            {
                searchString = JOptionPane.showInputDialog(tabbedPane,
                        "Empty string!", "Search", JOptionPane.ERROR_MESSAGE);
            }
            // Highlights the occurrences of the search string.
            if (!searchString.equals(""))
            {
                if (tabIndex < 3)
                {
                    textComp = messageArea[tabIndex];
                    search(textComp, searchString, 0);
                } else
                {
                    if (tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()).
                            equals("Retrieved Facets"))
                    {
                        textComp = FacetViewer.getTextPane();
                        search(textComp, searchString, 0);
                    } else
                    {
                        if (tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()).
                                equals("Facet Viewer"))
                        {
                            search(textComp, searchString, 1);
                        }
                    }
                }
            }
        } catch (NullPointerException npe)
        {
            // If 'cancel' was pressed, there is no String. No action.
        }
    }

    /**
     * search: Searches for a string from the 'Find' dialog. Creates highlights
     * around all occurrences found in the active text area or in a list of
     * topical facets.
     *
     * @param textComp : text to search in text area (JTextComponent).
     * @param searchString : the string to look for.
     * @param type : the type of search to perform : '0' search a text area; '1'
     * search a list.
     * @ToDo : step through the list of highlighted words one by one with
     * F3-key.
     */
    private void search(final JTextComponent textComp, String searchString,
            int type)
    {
        try
        {
            int counter = 0;
            int start = 0;
            int firstPosition = 0;
            ArrayList<Integer> found = new ArrayList<>();
            if (type == 0)
            {
                // Removes first all highlights from a previous search.
                removeHighlights(textComp);
                painter = new DefaultHighlightPainter(Color.yellow);
                Highlighter highLight = textComp.getHighlighter();
                Document doc = textComp.getDocument();
                String text = doc.getText(0, doc.getLength());
                // Searches for pattern. Sets firstPosition to the index of the 
                //first instance found.
                while ((start = text.indexOf(searchString, start)) >= 0)
                {
                    // Applies a highlight to the matching string.
                    highLight.addHighlight(start, start + searchString.length(),
                            painter);
                    start += searchString.length();
                    // highLights.add(start);
                    if (firstPosition == 0)
                    {
                        firstPosition = start;
                    }
                    found.add(start);
                    counter++;
                }
            } else
            {
                JList facetList = RetrievedFacetViewer.getFocusList();
                if (facetList != null)
                {
                    // Gets the number of lines in the list
                    int size = facetList.getModel().getSize();
                    for (int i = 0; i < size; i++)
                    {
                        start = 0;
                        String item = facetList.getModel().getElementAt(i).toString();
                        while ((start = item.indexOf(searchString, start)) >= 0)
                        {
                            facetList.setSelectedIndex(i);
                            facetList.ensureIndexIsVisible(i);
                            facetList.setSelectionBackground(Color.YELLOW);
                            start += searchString.length();
                            counter++;
                        }
                    }
                }
            }
            if (counter == 1)
            {
                textComp.setCaretPosition(firstPosition);
                JOptionPane.showMessageDialog(tabbedPane, "One instance of '"
                        + searchString + "' found.", "Search Result",
                        JOptionPane.INFORMATION_MESSAGE);
            } else
            {
                if (counter > 0)
                {
                    textComp.setCaretPosition(firstPosition);
                    int count = 0;
                    String[] options =
                    {
                        "Next", "Cancel"
                    };
                    int result = JOptionPane.showOptionDialog(tabbedPane, counter
                            + " instances of '" + searchString + "' found.",
                            "Search Result", JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                    if (result == JOptionPane.YES_OPTION)
                    {
                        //Go to next highlight
                        while (found.size() > count)
                        {
                            textComp.setCaretPosition(count);
                            count++;
                        }
                        
                    } else if (result == JOptionPane.NO_OPTION)
                    {
                        // Cancel 
                    }
                } else
                {
                    JOptionPane.showMessageDialog(tabbedPane, "No instances of '"
                            + searchString + "' found.", "Search Result",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (BadLocationException be)
        {
            showText("BadLocationException while highlighting search-results", 0);
            be.printStackTrace(System.err);
        }
    }

    /**
     * removeHighlights: Removes all highlights of a previous search.
     *
     * @param textComp : text in text area (JTextComponent).
     */
    private void removeHighlights(JTextComponent textComp)
    {
        Highlighter highLight = textComp.getHighlighter();
        Highlighter.Highlight[] oldHighLight = highLight.getHighlights();

        for (Highlighter.Highlight oldHighLight1 : oldHighLight)
        {
            highLight.removeHighlight(oldHighLight1);
        }
    }

    /**
     * selectAllItemActionPerformed: Selects all text in the textarea.
     *
     * @param event : 'select all' menu item selected (ActionEvent).
     */
    private void selectAllItemActionPerformed(ActionEvent event)
    {
        int tabIndex = tabbedPane.getSelectedIndex();
        if (tabIndex < 3)
        {
            messageArea[tabIndex].selectAll();
        }
    }

    /**
     * clearItemActionPerformed: Removes all text in the text area.
     *
     * @param event : 'clear' menu item selected (ActionEvent)
     */
    private void clearItemActionPerformed(ActionEvent event)
    {
        int tabIndex = tabbedPane.getSelectedIndex();
        if (tabIndex < 3)
        {
            messageArea[tabIndex].setText("");
        }
    }

    /**
     * copyItemActionPerformed: Copies the selected text to the System
     * Clipboard.
     *
     * @param event : 'copy' menu item selected (ActionEvent).
     */
    private void copyItemActionPerformed(ActionEvent event)
    {
        String stringCopy = "";
        int tabIndex = tabbedPane.getSelectedIndex();
        clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (tabIndex < 3)
        {
            stringCopy = messageArea[tabIndex].getSelectedText();
        }
        StringSelection clipString = new StringSelection(stringCopy);
        clip.setContents(clipString, clipString);
    }

    /**
     * aboutActionPerformed: Opens the 'About' text area.
     *
     * @param event : 'about' menu item selected (ActionEvent).
     */
    private void aboutActionPerformed(ActionEvent event)
    {
        aboutFrame.setSize(300, 170);
        aboutFrame.setLocationRelativeTo(menuBar);
        aboutFrame.setVisible(true);
        aboutText.setText("Topic Detection & Tracking\n" + "Unsupervised, "
                + "graph-based content finding in\n" + "unrestricted text.\n"
                + "A doctoral project by Eric Van Horenbeeck\n"
                + "Supervisor: prof. dr. Walter Daelemans\n"
                + "CLiPS - University of Antwerp, 2008");
    }

    /**
     * exitMenuItemActionPerformed: Program exit.
     *
     * @param event : 'exit' menu item selected.
     */
    private void exitMenuItemActionPerformed(ActionEvent event)
    {
        System.exit(0);
    }
   
    /**
     * dirMenuItemActionPerformed: select the main directory.
     * 
     * @param event: Directory selection button clicked
     */
    private void dirMenuItemActionPerformed(ActionEvent event)
    {
        fc.setCurrentDirectory(new java.io.File("."));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            dir = fc.getCurrentDirectory();
        }
    }
    
    /**
     * saveMenuItemActionPerformed: Saves the content of the selected text to a
     * text file on disk.
     *
     * @param event : Save button pressed (ActionEvent).
     */
    private void saveMenuItemActionPerformed(ActionEvent event)
    {
        fc.setCurrentDirectory(new File(dir + workFiles));
        int validate = fc.showSaveDialog(mainFrame);
        int index = tabbedPane.getSelectedIndex();
        if (validate == JFileChooser.APPROVE_OPTION)
        {
            File filePath = fc.getSelectedFile();
            if (filePath != null)
            {
                BufferedWriter out;
                try
                {
                    out = new BufferedWriter(new FileWriter(filePath));
                    out.write(messageArea[index].getSelectedText());
                    out.close();
                } catch (IOException ioe)
                {
                    showText("Saving file did not succeed", 0);
                    ioe.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * scopeMenuItemActionPerformed: Takes the begin date and the end date as
     * set by the user.
     *
     * @param event : date setting selected (ActionEvent).
     */
    private void scopeMenuItemActionPerformed(ActionEvent event)
    {
        scopeFrame.setSize(340, 180);
        scopeFrame.setLocationRelativeTo(menuBar);
        scopeFrame.setVisible(true);
        scopeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Settings are ready when the 'confirm' button is pushed.
        // Settings are saved to disk.
        confirmScopeBtn.addActionListener((ActionEvent event1) ->
        {
            // Controls the input before accepting.
            if (setDates())
            {
                // Date interval identification for the filename.
                if (beginDate == null || allDateBx.isSelected())
                {
                    scope = "AllDates";
                } else
                {
                    scope = beginDateString + "-" + endDateString;
                }
                updateSettings(beginDateString, endDateString, isAllDates,
                        communityName, sourceName, taskName);
                scopeFrame.dispose();
            }
        });
    }

    /**
     * communityMenuItemActionPerformed: Takes the information community
     * selected by the user.
     *
     * @param event : information community setting selected (ActionEvent).
     */
    private void communityMenuItemActionPerformed(ActionEvent event)
    {
        communityFrame.setSize(270, 150);
        communityFrame.setVisible(true);
        communityFrame.setLocationRelativeTo(menuBar);
        communityFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Settings are ready when the 'confirm' button is pushed.
        // Settings are saved to disk.
        confirmCommunityBtn.addActionListener((ActionEvent event1) ->
        {
            communityName = (String) communityBox.getSelectedItem();
            // Sets 'Undefined' when nothing was selected.
            if (communityName.equals(""))
            {
                communityName = communityList.get(0);
            } // check if subdir exists, else create one
            else
            {
                File newDir = new File(dir + workFiles + separator
                        + communityName);
                if (!newDir.isDirectory())
                {
                    newDir.mkdir();
                }
            }
            updateSettings(beginDateString, endDateString, isAllDates,
                    communityName, sourceName, taskName);
            communityFrame.dispose();
        });
    }

    /**
     * sourceMenuItemActionPerformed: Takes the information source selected by
     * the user.
     *
     * @param event : source setting selected (ActionEvent).
     */
    private void sourceMenuItemActionPerformed(ActionEvent event)
    {
        sourceFrame.setSize(350, 150);
        sourceFrame.setVisible(true);
        sourceFrame.setLocationRelativeTo(menuBar);
        sourceFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Settings are ready when the 'confirm' button is pushed.
        // Settings are saved to disk.
        confirmSourceBtn.addActionListener((ActionEvent event1) ->
        {
            sourceName = (String) sourceBox.getSelectedItem();
            // Sets 'Undefined' when nothing was selected.
            if (sourceName.equals(""))
            {
                sourceName = sourceList.get(0);
            }
            updateSettings(beginDateString, endDateString, isAllDates,
                    communityName, sourceName, taskName);
            sourceFrame.dispose();
        });
    }

    /**
     * taskMenuItemActionPerformed: Takes the task to be performed as selected
     * by the user.
     *
     * @param event : task setting selected (ActionEvent).
     */
    private void taskMenuItemActionPerformed(ActionEvent event)
    {
        taskFrame.setSize(300, 150);
        taskFrame.setVisible(true);
        taskFrame.setLocationRelativeTo(menuBar);
        taskFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        confirmTaskBtn.addActionListener((ActionEvent event1) ->
        {
            if (getCommunity().isBlank() || getScope().isBlank()
                    || getSource().isBlank())
            {
                makeWarningMessage(10);
                return;
            } else
            // Checks if the scope setting is consistent.
            {
                if ((null == beginDate && null == endDate
                        && !allDateBx.isSelected())
                        || (null != beginDate && beginDate.equals(endDate))
                        || (null != beginDate && null != endDate
                        && endDate.before(beginDate)))
                {
                    makeWarningMessage(6);
                    return;
                }
            }
            if (baseLineBx.isSelected())
            {
                mRunBaseLineFilter = true;
            } else
            {
                mRunBaseLineFilter = false;
            }
            taskName = (String) taskBox.getSelectedItem();
            isAllDates = String.valueOf(allDateBx.isSelected());
            updateSettings(beginDateString, endDateString, isAllDates,
                    communityName, sourceName, taskName);
            taskFrame.dispose();
        });
    }

    /**
     * updateSettings: Updates the settingsFile with the recent selections as
     * set by the user in the main TopicDection directory.
     *
     * @param beginDate : start date for file selections (String).
     * @param endDate : end date for file selections (String).
     * @param community : the community name used (String).
     * @param source : the data source linked to this community (String).
     * @param task : the most recent task (String).
     */
    private void updateSettings(String beginDate, String endDate, String allDates,
            String community, String source, String task)
    {
        if (community.equals("") || task.equals(""))
        {
            // No update of the settings file if one of these fields is empty.
        } else
        {
            String datesPath = dir + workFiles + separator + "tdtSettings.txt";
            File file = new File(datesPath);
            try (
                     PrintWriter out = new PrintWriter(new FileWriter(file)))
            {
                out.print(beginDate + '\n' + endDate + '\n' + allDates + '\n'
                        + community + '\n' + source + '\n' + task + '\n'
                        + getCoreNumber() + '\n' + getRetrievedSelection());
            } catch (IOException ioe)
            {
                showText("* Updating tdtSettings.txt did not succeed *", 0);
                ioe.printStackTrace(System.err);
            }
        }
    }

    /**
     * lookUpMenuItemActionPerformed: Looks up the value behind an index, e.g.
     * the label (word) of a vertex or a document name.
     *
     * @param event : 'look up' menu item selected (ActionEvent).
     */
    private void lookUpMenuItemActionPerformed(ActionEvent event)
    {
        lookUpFrame.setSize(350, 205);
        lookUpFrame.setVisible(true);
        lookUpFrame.setLocationRelativeTo(menuBar);
        lookUpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // Gets the labels of a list of vertex-ids when the 'label' button is 
        // pressed.
        vertexLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                lookUpAnswer.setText("");
                String[] queryList = Algorithms.getLineSplit(lookUpQuery.
                        getText().trim());
                List idxsList = new ArrayList(queryList.length);
                for (String queryList1 : queryList)
                {
                    Integer vrtxId = Integer.parseInt(queryList1);
                    idxsList.add(vrtxId);
                }
                List labelList = ApplicationManager.getLabels().
                        returnLabelList(idxsList);
                if (!labelList.isEmpty())
                {
                    lookUpAnswer.setText(labelList.toString());
                } else
                {
                    lookUpAnswer.setText("Nothing found");
                }
            } catch (NumberFormatException nf)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("Not a valid entry");
            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("No table. Run BUILD or TOPICS first.");
            }
        });
        // Gets the vertex-id of the label when the 'vertex' button is pressed.
        labelLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                lookUpAnswer.setText("");
                String label = lookUpQuery.getText().trim();
                Integer vertexId = ApplicationManager.getLabels().
                        getVertexIndex(label);
                if (vertexId != null)
                {
                    lookUpAnswer.setText(vertexId.toString());
                } else
                {
                    lookUpAnswer.setText("Nothing found");
                }
            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("No table. Run BUILD or TOPICS first.");
            } catch (Exception e)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("Nothing found");
            }
        });
        // Gets the name of the document behind the doc-id when the 
        //'File' button is pressed.
        docNameLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                lookUpAnswer.setText("");
                int docId = Integer.parseInt(lookUpQuery.getText().trim());
                String doc = ApplicationManager.getDocTable().getFilename(docId);
                if (!doc.equals(""))
                {
                    lookUpAnswer.setText(doc);
                } else
                {
                    lookUpAnswer.setText("Nothing found");
                }
            } catch (NumberFormatException nf)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("Not a valid entry");
            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("No table. Run BUILD or TOPICS first.");
            }
        });
        // Gets the name of the document behind the doc-id when the 
        //'DocId' button is pressed.
        docIdLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                lookUpAnswer.setText("");
                String docName = lookUpQuery.getText().trim();
                int docId = ApplicationManager.getDocTable().getDocNr(docName);
                if (docId != -1)
                {
                    lookUpAnswer.setText("" + docId);
                } else
                {
                    lookUpAnswer.setText("Nothing found");
                }
            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("No table. Run BUILD or TOPICS first.");
            }
        });
        
        // Gets the original name of the document behind the doc-id when the 
        //'orgDocLookUpBtn' button is pressed.
        orgDocLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                lookUpAnswer.setText("");
                String docName = lookUpQuery.getText().trim();
                String orgDoc = getOriginalFilename(docName);
                if (!orgDoc.isBlank())
                {
                    lookUpAnswer.setText("" + orgDoc);
                } else
                {
                    lookUpAnswer.setText("Nothing found");
                }
            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("Empty table. Nothing found");
            }
        });

        // Returns the full dictionary with the internal filenames and the
        // original document filenames, printed on the general tab
        allOrgNamesBtn.addActionListener((ActionEvent event1) ->
        {
            try
            {
                String allNames = getAllOriginalFilenames();
                if (null == allNames || allNames.isBlank())
                {
                    showText("No list found", 0);
                } else
                {
                    clearAllText();
                    showText(allNames, 0);
                    lookUpQuery.setText("");
                    lookUpQuery.requestFocusInWindow();
                    lookUpAnswer.setText("See 'General Info'-tab for the list");
                }

            } catch (NullPointerException np)
            {
                lookUpQuery.setText("");
                lookUpQuery.requestFocusInWindow();
                lookUpAnswer.setText("No table found");
            }
        });


        // Copies the content of the answer field to the clipboard when 'copy' 
        // is pressed.
        copyBtn.addActionListener((ActionEvent event1) ->
        {
            lookUpAnswer.selectAll();
            String stringCopy = lookUpAnswer.getSelectedText();
            StringSelection clipString = new StringSelection(stringCopy);
            clip.setContents(clipString, clipString);
        });
        // Input and output textfield are cleared when the 'Clear' button is pressed.
        clearBtn.addActionListener((ActionEvent event1) ->
        {
            lookUpQuery.setText("");
            lookUpAnswer.setText("");
            lookUpQuery.requestFocusInWindow();
        });
        // Look Up is finished when the 'Quit' button is pressed.
        confirmLookUpBtn.addActionListener((ActionEvent event1) ->
        {
            lookUpFrame.dispose();
        });
    }

    /**
     * paraMenuItemActionPerformed: Sets different parameters concerning the
     * program execution.
     *
     * @param event : parameter settings selected (ActionEvent).
     */
    private void paraMenuItemActionPerformed(ActionEvent event)
    {
        paraFrame.setSize(300, 320);
        paraFrame.setVisible(true);
        paraFrame.setLocationRelativeTo(menuBar);
        paraFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Settings are ready when the 'confirm' button is pushed.
        confirmParaBtn.addActionListener((ActionEvent event1) ->
        {
            paraFrame.dispose();
        });
    }

    /**
     * startPerformed: Starts the execution of the main program by showing a
     * message with the name and the scope of the task and the location of the
     * files generated by the application.
     *
     * @param event : 'start' button pressed (ActionEvent).
     */
    private void startPerformed(ActionEvent event)
    {
        try
        {
            // Sets begin- and enddate of the chosen time interval in Calendar 
            //and String format.
            setDates();
            // The dataFileList has the name of the directory of the data files.
            dataFileList = getDataStorage(getCommunity());

            // Fields are set to 'Undefined' when nothing was selected in 
            // previous steps. Delivers various warning messages.
            if (taskName.equals("Preprocess"))
            {
                if (sourceName.equals("") || communityName.equals(""))
                {
                    if (communityName.equals(""))
                    {
                        communityName = communityList.get(0);
                    }
                    if (sourceName.equals(""))
                    {
                        sourceName = sourceList.get(0);
                    }
                    makeWarningMessage(1);
                } else if (scope.equals("AllDates"))
                {
                    makeWarningMessage(9);
                } else
                {
                    makeWarningMessage(2);
                }
            }

            if (communityName.equals("") && ((taskName.equals("Build Network")
                    || taskName.equals("Compute Info-value")
                    || taskName.equals("Count Links"))))
            {
                communityName = communityList.get(0);
//                makeWarningMessage(3);
            }

//            if (taskName.equals("Build Network")) {
//                if (scope.equals("AllDates")) {
//                    makeWarningMessage(9);
//                } else {
//                    makeWarningMessage(4);
//                }
//            }
//            if (taskName.equals("Compute Info-value")
//                    || taskName.equals("Topical Facets")
//                    || taskName.equals("Consolidate")
//                    || taskName.equals("Count Links")
//                    || taskName.equals("Arcs & Associations")) {
//                if (scope.equals("AllDates")) {
//                    makeWarningMessage(9);
//                } else {
//                    makeWarningMessage(5);
//                }
//            }
            startThread(taskName);

        } catch (NullPointerException ne)
        {
            showText("* No selections made, nothing to start *", 0);
        }
    }

    /**
     * startThread: Creates a new thread to perform the main actions when the 'start'
     * button is pressed. A timer is started.
     *
     * @param taskName : action to perform (String).
     */
    private void startThread(final String taskName)
    {
        worker = new SwingWorker()
        {
            @Override
            public Object construct()
            {
                // The time at the start of this session in milliseconds.
                mAppStart = System.currentTimeMillis();
                try
                {
                    return performTask(taskName);
                } catch (IOException ex)
                {
                    updateStatusBar(-1, "");
                    showText("IOException", 0);
                    ex.printStackTrace(System.err);
                } catch (SAXException ex)
                {
                    updateStatusBar(-1, "");
                    showText("SAXException", 0);
                    ex.printStackTrace(System.err);
                } catch (TikaException ex)
                {
                    updateStatusBar(-1, "");
                    showText("TikaException", 0);
                    ex.printStackTrace(System.err);
                }
                return null;
            }

            @Override
            public void finished()
            {
                if (progress > -1)
                {
                    updateStatusBar(4, getElapsedTime());
                }
            }
        };
        worker.start();
    }

    /**
     * getElapsedTime: Returns the time since this application started.
     *
     * @return the elapsed time (String) since the start in
     * 'hr.:min.:sec,mmsec'-format .
     */
    public static String getElapsedTime()
    {
        if (mAppStart < 0.00001)
        {
            return "0";
        } else
        {
            return GraphTime.hourMinSec(System.currentTimeMillis() - mAppStart);
        }
    }

    /**
     * updateStatusBar: Updates the progress bar on the GUI with the number of
     * files to process and the number of files seen so far.
     *
     * @param progress : the kind of task at hand (int).
     * @param elapsedTime : time needed to execute the tasks (String).
     */
    public static void updateStatusBar(final int progress, final String 
            elapsedTime)
    {
        Runnable setProgressBar = () ->
        {
            try
            {
                JComponent component1;
                int tabIndex = tabbedPane.getSelectedIndex();
                if (tabIndex < 3)
                {
                    component1 = messageArea[tabIndex];
                } else
                {
                    component1 = getTabbedPane();
                }
                switch (progress)
                {
                    case 1 ->
                    {
                        // Processing files with file count from disk.
                        component1.setCursor(Cursor.
                                getPredefinedCursor(Cursor.WAIT_CURSOR));
                        progressBar.setMaximum(filesToProcess);
                        progressBar.setValue(fileCount);
                        progressLabel.setText("Processed:  " + fileCount + " of "
                                + filesToProcess + " elements");
                    }
                    case 2 ->
                    {
                        // Processsing without file count.
                        component1.setCursor(Cursor.
                                getPredefinedCursor(Cursor.WAIT_CURSOR));
                        progressBar.setString("");
                        progressBar.setIndeterminate(true); // Shows moving bar.
                        progressLabel.setText("Processing the data");
                    }
                    case 3 ->
                    {
                        // Processing files with file count from program.
                        component1.setCursor(Cursor
                                .getPredefinedCursor(Cursor.WAIT_CURSOR));
                        progressBar.setMaximum(filesToProcess);
                        progressBar.setValue(fileCount);
                        progressLabel.setText("Processed:  " + fileCount + " of "
                                + filesToProcess + " elements");
                    }
                    case 4 ->
                    {
                        // File processing ready, the elapsed time is shown.
                        component1.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        messageArea[1].setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0);
                        progressLabel.setText("...Ready" + "  -  Elapsed Time: "
                                + elapsedTime);
                    }
                    case 5 ->
                    {
                        // Printing and saving.
                        component1.setCursor(Cursor
                                .getPredefinedCursor(Cursor.WAIT_CURSOR));
                        progressBar.setString("");
                        progressBar.setIndeterminate(true); // Shows moving bar.
                        progressLabel.setText("Writing Files to Disk");
                    }
                    case -1 ->
                    {
                        // Error condition.
                        component1.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0);
                        progressLabel.setText("Task interrupted  -  Elapsed time: "
                                + elapsedTime);
                    }
                    default ->
                    {
                    }
                }
            } catch (Exception ex)
            {
                updateStatusBar(-1, "");
                showText("General exception not specified", 0);
                ex.printStackTrace(System.err);
            }
        };
        SwingUtilities.invokeLater(setProgressBar);
    }

    /**
     * performTask: Real action starts here. This method sets parameters related
     * to the task at hand. First Step
     *
     * @param taskName : action to perform (String)
     */
    private Object performTask(String taskName) throws IOException, SAXException,
            TikaException
    {
        try
        {
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
            int myTask;
            String directoryName;
            // Array with formatting info about the files being opened.
            getFingerprint(sourceName);
            // When the task is 'Preprocess' all directories and files are deleted
            // to prevent a mixup of new and existing data.
            if ("Preprocess".equals(taskName))
            {
                Path pathToBeDeleted = 
                        Paths.get(dir + workFiles + separator + communityName);
                File[] allContents = (pathToBeDeleted.toFile()).listFiles();
                if (allContents != null)
                {
                    for (File file : allContents)
                    {
                        FileUtils.deleteDirectory(file);
                    }
                }
            }
            // The program reads the global DocTable (table with all the 
            // document-ids and filenames) the global CollectionTable  
            //(table with collections of documents identified by a common source 
            // and a common date) and the global LabelTable (table with all the
            // token-ids and token-types) of this community. New data from this 
            // session will be added by the NetworkBuilder. The 'Previous DocList' 
            // is a sorted set with all the previously attributed doc-ids.
            try
            {
                lt = io.readLTable(getWorkPath() + separator + "AllLabels.lbls");
                dt = io.readDTable(getWorkPath() + separator + "AllDocs.dtab");
                ct = io.readCTable(getWorkPath() + separator + "AllColls.ctab");
                mOriginalFilenames = io.readTreeMap(getWorkPath() + separator 
                        + "AllFilenames.nms");

            } // Initiates a new LabelTable, a new DocTable and a new 
            // CollectionTable when none exists (all three are needed together).
            catch (Exception e)
            {
                lt = new LabelTable();
                File ltFile = new File(getWorkPath() + separator 
                        + "AllLabels.lbls");
                ltFile.createNewFile();
                dt = new DocTable();
                File dtFile = new File(getWorkPath() + separator 
                        + "AllDocs.dtab");
                dtFile.createNewFile();
                ct = new CollectionTable();
                File ctFile = new File(getWorkPath() + separator 
                        + "AllColls.ctab");
                ctFile.createNewFile();
            }
            // Identifies the '*.tgr'-file directory based on the scope date.
            // Here the first month of the scope or the filename is
            // used to identify the data storage directory. The second month of 
            // the scope is checked if the second month is different from the  
            // first. When there is a datum overlap the application has to work  
            // with two directories. As default value or when 'AllDates' is the 
            // string, everything goes into the first data store. 
            // In another application other arguments to distribute the data 
            // over several files may be used and should be implemented here.

            // storeFlag (int) to indicate whether to use the first (flag
            // 0) or the second month (flag 1) of the string as an argument. 
            // Flag 2 extracts from US file; flag 3 from Belgian file.
            int storeFlag = 0;
            if (getScopeOverlap())
            {
                storeFlag = 1;
            }
            String arg = getDataStoreArg(storeFlag, scope);
            // Sets the location of the '*.tok'-files directory.
            setDataPath(arg);
            // Sets the location of the '*.tgr'-files directory.
            setGraphStoreName(arg);
            if ("Preprocess".equals(taskName))
            {
                fc.setCurrentDirectory(new File(dir + corpusFiles));
            } else
            {
                fc.setCurrentDirectory(new File(dir + workFiles + separator
                        + communityName));
                dt.preparePreviousDocList();
            }

            switch (taskName)
            {
                // First Task 
                case "Preprocess" ->
                {
                    // Other specialized parsers are possible and should be added
                    // to the Tika parser.
                    parseDocument = new TikaParser();
                    // The user selects the files to be preprocessed.
                    startFileDialog();
                }
                case "Build Network" ->
                {
                    myTask = 2;

                    // Checks if a list is available of doc-ids allocated 
                    // in a previous session.
                    if (dt.getAllDocNrs().length > 0)
                    {
                        docListExists = true;
                    }
                    // Initiates an instance of the NetworkBuilder class.
                    gb = new NetworkBuilder();
                    directoryName = getDataPath();
                    selectFiles(myTask, directoryName);
                    // Gets the data from the second month if there is an 
                    // overlap between two months.
                    if (getScopeOverlap())
                    {
                        switchDirectory(myTask);
                    }
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Collects links between tokens and saves the result to disk.
                case "Count Links" ->
                {
                    myTask = 3;
                    arcsMap = new HashMap();
                    // Processes the selected '*.tgr'-files from disk first.
                    directoryName = getGraphPath();
                    selectFiles(myTask, directoryName);
                    // Gets the data from the second month if there is an 
                    // overlap between two months.
                    if (getScopeOverlap())
                    {
                        switchDirectory(myTask);
                    }
                    // Processes other files.
                    directoryName = getWorkPath();
                    selectFiles(myTask, directoryName);
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Calculates the informative value of all token-types,
                // a modified TFIDF metric based on the links between tokens.
                case "Compute Info-value" ->
                {
                    myTask = 4;

                    arcsMap = new HashMap();
                    tc = new TokenCount();
                    try
                    {
                        if (mRunBaseLineFilter)
                        {
                            prepareBaseLineArray();
                        }
                    } catch (Exception ex)
                    {
                    }
                    // Processes the selected '*.tgr'-files from disk first.
                    directoryName = getGraphPath();
                    selectFiles(myTask, directoryName);
                    // Gets the data from the second month if there is 
                    // an overlap between two months.
                    if (getScopeOverlap())
                    {
                        switchDirectory(myTask);
                    }
                    // Processes other files.
                    directoryName = getWorkPath();
                    selectFiles(myTask, directoryName);
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Reduces the search space by reconstructing a 
                //network with only informative elements.
                case "Arcs & Associations" ->
                {
                    myTask = 5;

                    arcsMap = new HashMap();
                    infoArcMap = new HashMap();
                    // Processes the selected '*.tgr'-files from disk first.
                    directoryName = getGraphPath();
                    selectFiles(myTask, directoryName);
                    // Gets the data from the second month if there 
                    // is an overlap between two months.
                    if (getScopeOverlap())
                    {
                        switchDirectory(myTask);
                    }
                    // Processes other files.
                    directoryName = getWorkPath();
                    selectFiles(myTask, directoryName);
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Extracts topical facets. Topical facets are stiches 
                // (shared phrases) used to sew documents together.
                case "Topical Facets" ->
                {
                    myTask = 6;

                    arcsMap = new HashMap();
                    docMatrixList = new LinkedList();
                    // infoArcMap = new HashMap();
                    // assocMap = new HashMap();
                    //  unusedList = new LinkedList();

                    // Processes the selected '*.tgr'-files from
                    // disk first.
                    directoryName = getGraphPath();
                    selectFiles(myTask, directoryName);
                    // Gets the data from the second month if 
                    // there is an overlap between two months.
                    if (getScopeOverlap())
                    {
                        switchDirectory(myTask);
                    }
                    // Processes other files.
                    directoryName = getWorkPath();
                    selectFiles(myTask, directoryName);
                    // Rescan the available unused files.
                    rescan();
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Sewing the day-by-day topical facets into one structure.
                case "Consolidate" ->
                {
                    myTask = 7;
                    collector = new FacetCollector();
                    scopeMap = new TreeMap();
                    // Number of days between the begin- and
                    // enddate of the scope.
                    scopeSpan = 1;
                    try
                    {
                        // Reads the facet dictionary from disk. This SortedMap
                        // has a general facet key as key and as value a list with
                        // the local facet key and the path to the local facet map.
                        collector.prepareFacetKeyList(io.readSortedMap(getGlobalPath()
                                + separator + "FacetKeys.dic"));
                        // Reads the scope set from disk. This SortedSet keeps 
                        // track of all the scope dates already treated by the 
                        // FacetCollector to avoid redoing the work when the same
                        // files are loaded again.
                        collector.addScopeSet(io.readSet(getGlobalPath()
                                + separator + "ScopeFacet.scp"));
                    } // Initiates new maps when none were found.
                    catch (Exception e)
                    {
                        // No action necessary when these files don't exist, 
                        // they will be created later.
                    }
                    directoryName = getWorkPath();
                    selectFiles(myTask, directoryName);
                    // Finally, finishes any remaining task.
                    completeTasks(myTask);
                }
                // Performing a query.
                case "Retrieve" ->
                {
                    myTask = 8;

                    // Starts a user interface (QueryInput) to provide a list 
                    // of tokens (seeds) that triggers the collecting of topical   
                    // facets related to these tokens.
                    // Seeds are transferred to the TopicRetriever.
                    QueryInput.getQueryInputInstance();
                    
                    updateStatusBar(2, "");
                    results = new ResultExport(dt);
                    vt = new VerticesTable();
                    infoArcMap = new HashMap();
                    arcsMap = new HashMap();
                    try
                    {
                        // Reads a map with the results of previous retrieval
                        // tasks.
                        results.addResultMap(io.readSortedMap(getResultPath()
                                + separator + "TaskResult.map"));
                    } catch (Exception e)
                    {
                        // ignore if file doesn't exist.
                    }
                    // Wait untill the seedMap is ready before
                    // activating the TopicRetriever.
                    lock.lock();
                    {
                        try
                        {
                            while (null == seedMap)
                            {
                                notReady.await();
                            }
                        } catch (InterruptedException e)
                        {
                        } finally
                        {
                            lock.unlock();
                        }
                    }
                    
                    // Finally, finishes any remaining task.                
                    completeTasks(myTask);
                }
                // Reconstructs the similarity viewer with saved data.
                case "Reconstruct" ->
                {
                    myTask = 9;
                    updateStatusBar(2, "");
                    completeTasks(myTask);
                }
                // The documents retrieved by this application as part of the
                // query are compared to the official human annotated results.
                // of the TDT-corpus. All available tasks are shown.
                case "Evaluate" ->
                {
                    updateStatusBar(2, "");
                    evaluator = new Evaluation();
                    evaluator.evaluate(dt, scope, beginDate, endDate, "all");
                }
                default ->
                {
                    showText("* No task available *", 0);
                }

            }
        } catch (InterruptedException ie)
        {
            updateStatusBar(-1, getElapsedTime());
            return "Interrupted";
        } catch (NullPointerException e)
        {
            showText("General exception not specified.", 0);
            updateStatusBar(-1, getElapsedTime());
            e.printStackTrace(System.err);
        }
        return "All Done";
    }

    /**
     * switchDirectory: Switches to another directory with data from the second
     * month when there is an overlap between two months in the scope of this
     * task.
     *
     * @param task : a flag (int) to indicate what kind of task to start on the
     * files.
     */
    private void switchDirectory(int task) throws IOException, SAXException,
            TikaException
    {
        if (task == 2)
        {
            setGraphStoreName(getDataStoreArg(1, scope));
            setDataPath(getDataStoreArg(1, scope));
            selectFiles(task, getDataPath());
        } else
            if (task > 2)
            {
                setGraphStoreName(getDataStoreArg(1, scope));
                selectFiles(task, getGraphPath());
            }
    }

    /**
     * startFileDialog: Shows an Open File Dialog Box to the user to select the
     * appropriate original data files for parsing and tokenizing (PREPROCESS).
     */
    private void startFileDialog() throws IOException, SAXException, TikaException
    {
        try
        {
            JFrame frame = new JFrame();
            frame.setIconImage(getIcon());
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setMultiSelectionEnabled(true);
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File[] files = fc.getSelectedFiles();
                if (files.length == 1 && files[0].isDirectory())
                {
                    File[] subFiles = files[0].listFiles();
                    traverseFileList(1, subFiles);
                } else
                {
                    traverseFileList(1, files);
                }
            } // Program execution ends when no files were selected.
            else
            {
                if (returnVal == JFileChooser.CANCEL_OPTION
                        || returnVal == JFileChooser.ERROR_OPTION)
                {
                    showText("No files were selected *\n* Application ended.", 0);
                    updateStatusBar(-1, getElapsedTime());
                }
            }
            frame.dispose();
        } catch (NullPointerException ne)
        {
            showText("NullPointerException", 0);
            updateStatusBar(-1, getElapsedTime());
            ne.printStackTrace(System.err);
        } catch (HeadlessException e)
        {
            showText("General exception not specified", 0);
            updateStatusBar(-1, getElapsedTime());
            e.printStackTrace(System.err);
        }
    }

    /**
     * selectFiles: Retrieves an aray of files from the directory in view and
     * hands it over to the traverseFileList method.
     *
     * @param task : a flag (int) to indicate what kind of task to start on the
     * files.
     * @param dirName : location of the directory with the files to retrieve and
     * process.
     */
    private void selectFiles(int task, String dirName) throws IOException, 
            SAXException, TikaException
    {
        fileCount = 0;
        File directory = new File(dirName);
        File[] files = directory.listFiles();
        // Processes the selected files.
        traverseFileList(task, files);
    }

    /**
     * traverseFileList: Processing the files that were selected in the
     * fileChooser.
     *
     * @param task : a flag (int) to indicate what kind of task to start on the
     * files.
     * @param fileList : Array with Files.
     * @throws IOException
     */
    private void traverseFileList(int task, File[] fileList) throws IOException, 
            SAXException, TikaException
    {
        progress = 1;
        // Casts array of Files into an array of Strings.
        List selectList = Arrays.asList(fileList);
        // Counts the files to process.
        filesToProcess = selectList.size();
        Iterator select_itr = selectList.iterator();
        while (select_itr.hasNext())
        {
            String thisFile = select_itr.next().toString();
            fileCount++;
            selectTask(task, thisFile);
            updateStatusBar(progress, "");
        }
        fileCount = filesToProcess;
        showText("* File processing completed*\n*Proceeding with the "
                + "next task *", 0);
        clearText();
    }

    /**
     * rescan: Documents not involved in a previous topic finding session are
     * saved separately. They get another chance in this TOPICS session (task
     * 6). Arcs, info-arcs and associations from the documents that are involved
     * in the rescan are extracted and added to the normal tables. Documents
     * that were previously successfully rescanned in a scope similar to this
     * session are combined with the unused doc-list where they have been
     * removed in order to reconstruct the initial situation.
     */
    private void rescan()
    {
        // Files older than the limit in days relative to the begin date of the 
        // scope of this session are ignored. This time limit is set by the user
        // in 'Scope' panel of the GUI. It prevents the unusedDoc list to 
        // accumulate indefinitely. The default value is 365 days.
        Map unusedPathMap = new HashMap();
        Calendar fileDate = Calendar.getInstance();
        try
        {
            // Combines unused and reused list.
            SortedSet combinedRescan = new TreeSet();
            LinkedList rescannedDocList;
            // Reads the reused doc list if there exists one in the scope of
            // this session.
            String rsePath = getWorkPath() + separator + scope + "_Rescanned.rse";
            File rseFile = new File(rsePath);
            if (rseFile.exists())
            {
                rescannedDocList = io.readList(rsePath);
                combinedRescan.addAll(rescannedDocList);
            }
            // Opens the list with all unused doc-ids.
            String udcPath = getWorkPath() + separator + "Unused.und";
            LinkedList unusedDocList = io.readList(udcPath);
            combinedRescan.addAll(unusedDocList);
            // Iterates over the list with doc-ids to be rescanned.
            Iterator rescan_itr = combinedRescan.iterator();
            while (rescan_itr.hasNext())
            {
                // Constructs the filepath to the appropriate data store. 
                // Here it is assumed that the month in the filename can be used
                // to identify the datastore.
                int docNr = ((Integer) rescan_itr.next());
                String file = dt.getFilename(docNr) + ".tgr";
                setGraphStoreName(getDataStoreArg(0, file));
                String filePath = getGraphPath() + separator + file;
                // Only files with a date before the scope of this session are 
                // rescanned on the condition that they are not too old (in days
                // before the begin date). Texts inside the scope of this session
                // are included in the normal process. Any other file is ignored.
                String fileName = filePath.substring(filePath.
                        lastIndexOf(separator) + 1);
                if (scope.equals("AllDates"))
                {
                    fileDate.set(0000, 00, 00);
                } else
                {
                    fileDate = GraphTime.getDate(fileDate, filePath
                            .substring(filePath.lastIndexOf(separator) + 1));
                }

                if (GraphTime.isBeforeBegin(fileDate, beginDate, endDate)
                        && fileDate.after(limitDate))
                {
                    // Gets the arcs from the old files and adds them to the
                    // standard ArcsTable.
                    processTextGraph(filePath, 1);
                    // An 'infoArc' filename is constructed for every collection
                    // involved in the rescan. It is saved in a temporary map 
                    // together with the relevant doc-ids.
                    String fileScope = GraphTime.constructFileScope(fileDate, fileName);
                    List docList = new ArrayList();
                    if (unusedPathMap.containsKey(fileScope))
                    {
                        docList.addAll((ArrayList) unusedPathMap.get(fileScope));
                    }
                    docList.add(docNr);
                    unusedPathMap.put(fileScope, docList);
                }
            }
            // For every document to be rescanned, informative arcs will be 
            // extracted from the old infoArc files and added to the regular 
            // infoArc map. The same is done with the associations linked to
            // these documents.
            Iterator path_itr = unusedPathMap.keySet().iterator();
            while (path_itr.hasNext())
            {
                String fileScope = path_itr.next().toString();
                List docIdList = (ArrayList) unusedPathMap.get(fileScope);
                expandInfoArcs(fileScope, docIdList);
                expandAssociations(fileScope, docIdList);
            }
        } catch (Exception e)
        {
            showText("* No list found with unused documents. *\n* "
                    + "Continue with next task. *", 0);
        }
    }

    /**
     * expandInfoArcs: For every document to be rescanned, informative arcs are
     * extracted from the old infoArc files and added to the active infoArcMap
     * used in this session.
     *
     * @param scope : scope of old 'InfoArc' map (String).
     * @param docIdList : ArrayList with the doc-ids (int) to be rescanned.
     */
    private void expandInfoArcs(String scope, List docIdList) throws IOException
    {
        String arcPath = getWorkPath() + separator + scope + "_InfoArc.arcs";
        try
        {
            // Reads the infoArc map and extracts the infoArcs linked to the 
            // old doc-ids in view.
            Map oldInfoMap = io.readMap(arcPath);
            Iterator map_itr = oldInfoMap.keySet().iterator();
            while (map_itr.hasNext())
            {
                int docNr = ((Integer) map_itr.next());
                // A relevant doc-id is found.
                if (docIdList.contains(docNr))
                {
                    Integer docInt = docNr;
                    Map thisInfoMap = (HashMap) oldInfoMap.get(docInt);
                    infoArcMap.put(docInt, thisInfoMap);
                }
            }
        } catch (Exception e)
        {
            showText("* No InfoArc-map found. *\n* Continue with next task. "
                    + "*", 0);
        }
    }

    /**
     * expandAssociations: For every document to be rescanned, associations are
     * extracted from old association files and added to the active
     * associationsMap used in this session.
     *
     * @param scope : scope of the old associations map (String).
     * @param docIdList : ArrayList with the doc-ids (int) to be rescanned.
     */
    private void expandAssociations(String scope, List docIdList) throws IOException
    {
        String assocPath = getWorkPath() + separator + scope + "_Assoc.assc";
        try
        {
            // Reads the assoc map and extracts the associations linked to the 
            // old doc-ids in view.
            Map oldAssocMap = io.readMap(assocPath);
            Iterator map_itr = oldAssocMap.keySet().iterator();
            while (map_itr.hasNext())
            {
                int docNr = ((Integer) map_itr.next());
                // A relevant doc-id is found.
                if (docIdList.contains(docNr))
                {
                    Integer docInt = docNr;
                    List thisAssocMap = (ArrayList) oldAssocMap.get(docInt);
                    assocMap.put(docInt, thisAssocMap);
                }
            }
        } catch (Exception e)
        {
            showText("* No Associations map found. *\n* Continue with next task."
                    + " *", 0);
            e.printStackTrace(System.err);
        }
    }

    /**
     * processTextGraph: Assembles information from the individual text files
     * and puts it in the ArcsTable.
     *
     * @param filePath : the path to the textfiles (String).
     * @param rescan : a flag (int) to signal a normal TextGraph process (0) or
     * a rescan of previous unused files (1).
     */
    private void processTextGraph(String filePath, int rescan)
    {
        // Processes only files with the appropriate TextGraph-suffix.
        String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
        if (suffix.endsWith("tgr"))
        {
            try
            {
                // Collects the arcs from the selected files and puts them in 
                // a map.
                TextGraph tg = io.readTextGraph(filePath);
                int docNr = tg.getGraphDocNr();
                Map thisArcsMap = tg.getFullArcsMap();
                if (!thisArcsMap.isEmpty())
                {
                    Integer docInt = docNr;
                    arcsMap.put(docInt, thisArcsMap);
                    // Populates a list with the unused docs to be rescanned.
                    if (rescan == 1)
                    {
                        unusedList.add(docInt);
                    }
                }
            } catch (Exception e)
            {
                showText("This is not a right TEXTGRAPH file.\nWill try "
                        + "the next file. Probably you'll\nhave to restart "
                        + "the file selection procedure", 0);
                updateStatusBar(-1, getElapsedTime());
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * combineFiles: The RETRIEVE task has to combine files with different
     * scopes into one VertexTable and one InfoArc Table.
     *
     * @param filePath : the path to the files (String).
     */
    private void combineFiles(String filePath)
    {
        String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
        try
        {
            if (suffix.endsWith("vrtx"))
            {
                VerticesTable tmpVT = io.readVTable(filePath);
                vt.addVerticesMap(tmpVT.getFullVerticesMap());
            } else
            {
                if (suffix.endsWith("arcs"))
                {
                    Map tmpArcMap = io.readMap(filePath);
                    Iterator doc_itr = tmpArcMap.keySet().iterator();
                    while (doc_itr.hasNext())
                    {
                        Integer docId = (Integer) doc_itr.next();
                        if (docsRetrieved.contains(docId))
                        {
                            infoArcMap.put(docId, tmpArcMap.get(docId));
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            showText("This is not a right RETRIEVE file.\nWill try the next file. "
                    + "Probably you'll\nhave to restart the file selection "
                    + "procedure.", 0);
            updateStatusBar(-1, getElapsedTime());
            e.printStackTrace(System.err);
        }
    }

    /**
     * selectTask: Part One of the main program controller (MVC model) for
     * processing selected files according to the task at hand. Part Two is the
     * 'completeTasks' method. Together these methods control the program flow
     * for the 8 components that form the ccre of the application.
     *
     * @param task : flag (int) to select preprocessing, graph building, topic
     * detection, etc.
     * @param filePath : path to the file to process (String).
     * @TODO Regex pattern in the fingerprint-file to extract sourcename from
     * the filename. This to allow selection based on the source name for every
     * task, not just for preprocessing.
     * @ToDo Add language detection
     */
    private void selectTask(int task, String filePath) throws IOException,
            SAXException, TikaException
    {
        Calendar fileDate = Calendar.getInstance();
        fileDate.set(0000, 00, 00);
        switch (task)
        {
            // No date span for the preprocessing. All source files in the directory
            // are tokenized.
            case PREPROCESS ->
            {
                try
                {
                    // Identifies the '*.tok-file directory based on the file date.
                    int start = filePath.lastIndexOf(separator) + 1;
                    setDataPath(getDataStoreArg(Integer.valueOf(getDataLocator()),
                            filePath.substring(start)));
                    parseDocument.parse(filePath);

                    // Processes all files in the directory when the source is 
                    // undefined.
                    if (sourceName.equals("Undefined"))
                    {
                        System.out.println("Start undefined");
                        parseDocument.parse(filePath);

                        // Processes files with the selected source name if one
                        //was defined by the user.
                        String fileSource = "Undefined";
                        CharSequence srce = (filePath.substring(filePath.
                                lastIndexOf(separator) + 1));
                        // Compiles a regular expression. Only works for 
                        // the US TDT files now.
                        Matcher match = getRegexSource().matcher(srce);
                        if (match.find())
                        {
                            fileSource = match.group();
                            parseDocument.parse(filePath);
                        } else
                        {
                            showText("* No REGEX-pattern available for "
                                    + "this file. *", 0);
                            parseDocument.parse(filePath);
                        }
                    }
                    
                    // Save the dictionary with the internal and the original 
                    // filenames
                    String path = getWorkPath() + separator + "AllFilenames.nms";
                     io.writeThisObject(path, InputOutput.allOriginalFilenames);
                     
                } catch (NumberFormatException | PatternSyntaxException e)
                {
                    showText("* Exception encountered during the parsing "
                            + "task *", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case NETWORK ->
            {
                try
                {
                    // Checks if this file's date falls within the selected time
                    // interval(scope). A file with a single date is a data file
                    // with one single text.
//                    if (scope.equals("AllDates"))
//                        fileDate.set(0000, 00, 00);
//                    else
                    
                        fileDate = GraphTime.getDate(fileDate, filePath
                                .substring(filePath.lastIndexOf(separator) + 1));
                    
                    if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                    {
                        // Processes only tokenized files with the appropriate 
                        // '*.tok' suffix.
                        String suffix = filePath.substring(filePath
                                .lastIndexOf(".") + 1);
                        if (suffix.endsWith("tok"))
                        {
                            // Allocates an unique identification number to 
                            // this text file.
                            int docId = attributeDocId(filePath);
                            String fileName = getTextGraphName(filePath);
                            String textGraphPath = getGraphPath() + separator 
                                    + fileName;
                            // Transforms a text file into a text graph with 
                            //vertices and arcs and writes the data to the 
                            // harddisk file with a '*.tgr' suffix.
                            io.writeThisObject(textGraphPath,
                                    gb.textToGraph(io.makeTokenArray(filePath, 
                                            docId), docId));
                        }
                        // Counts the empty files within this scope. These 
                        // files are not used.
                        else
                        {
                            if (suffix.endsWith("emp"))
                            {
                                emptyCount++;
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("These are not the right files *\n* Restart the "
                            + "file selection procedure.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case LINKS ->
            {
                try
                {
                    // Checks if this file's date is within the time 
                    // interval (scope). A file with a single date is a data 
                    // file with one single text.
//                    if (scope.equals("AllDates"))
//                        fileDate.set(0000, 00, 00);
//                    else
//                    {
                        fileDate = GraphTime.getDate(fileDate, filePath
                                .substring(filePath.lastIndexOf(separator) + 1));
//                    }
                    if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                    {
                        processTextGraph(filePath, 0);
                    } // If this file has a scope in front and not a single date, 
                    // it is a file with specific computed results from a 
                    // previous session.
                    else
                    {
                        String fileScope = GraphTime.getFileScope();
                        if (fileScope.equals(scope))
                        {
                            // Processes only files with the appropriate suffix.
                            String suffix = filePath.substring(filePath
                                    .lastIndexOf(".") + 1);
                            // Reads the VerticesTable.
                            if (suffix.endsWith("vrtx"))
                            {
                                vt = io.readVTable(filePath);
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("Exception in the LINKS Task", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case TFIDF ->
            {
                try
                {
                    // Checks if this file's date falls within the time 
                    // interval (scope). A file with single date is a data file
                    // with one single text.
//                    if (scope.equals("AllDates"))
//                        fileDate.set(0000, 00, 00);
//                    else
//                    {
                        fileDate = GraphTime.getDate(fileDate, filePath
                                .substring(filePath.lastIndexOf(separator) + 1));
//                    }

                    if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                    {
                        // Processes only files with the appropriate
                        // TextGraph-suffix.
                        String suffix = filePath.substring(filePath
                                .lastIndexOf(".") + 1);
                        if (suffix.endsWith("tgr"))
                        {
                            TextGraph tg = io.readTextGraph(filePath);
                            int docNr = tg.getGraphDocNr();
                            tc.addTokenCountMap(docNr, tg.getTokenCountMap());
                            arcsMap.put(docNr, tg.getFullArcsMap());
                        }
                    } // If this file has a time interval in front and not a 
                    // single date, it is a file with specific computed results 
                    // from a previous session.
                    else
                    {
                        String fileScope = GraphTime.getFileScope();
                        if (fileScope.equals(scope))
                        {
                            // Processes only files with the appropriate suffix
                            String suffix = filePath.substring(filePath
                                    .lastIndexOf(".") + 1);
                            // Reads the VerticesTable.
                            if (suffix.endsWith("vrtx"))
                            {
                                vt = io.readVTable(filePath);
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("Exception in the BUILD Task.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case ASSOCIATE ->
            {
                try
                {
                    // Checks if this file's date is within the time 
                    // interval (scope). A file with a single date is a data 
                    // file with one single text.
//                    if (scope.equals("AllDates"))
//                        fileDate.set(0000, 00, 00);
//                    else
//                    {
                        fileDate = GraphTime.getDate(fileDate, filePath
                                .substring(filePath.lastIndexOf(separator) + 1));
//                    }
                    if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                    {
                        processTextGraph(filePath, 0);
                    } // If this file has a scope in front and not a single date,
                    // it is a file with specific computed results from a
                    // previous session.
                    else
                    {
                        String fileScope = GraphTime.getFileScope();
                        if (fileScope.equals(scope))
                        {
                            // Processes only files with the appropriate suffix.
                            String suffix = filePath.substring(filePath
                                    .lastIndexOf(".") + 1);
                            // Reads the VerticesTable.
                            if (suffix.endsWith("vrtx"))
                            {
                                vt = io.readVTable(filePath);
                            } // Reads the infoMap.
                            else
                            {
                                if (suffix.endsWith("info"))
                                {
                                    infoMap = io.readMap(filePath);
                                }
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("Exception in the ASSOCIATE Task.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case TOPICS ->
            {
                try
                {
                    // Checks if this file's date is within the time interval
                    // (scope). A file with a single date is a data file with 
                    // one single text.
//                    if (scope.equals("AllDates"))
//                        fileDate.set(0000, 00, 00);
//                    else
//                    {
                        fileDate = GraphTime.getDate(fileDate, filePath
                                .substring(filePath.lastIndexOf(separator) + 1));
//                    }
                    if (GraphTime.isBetweenDates(fileDate, beginDate, endDate))
                    {
                        processTextGraph(filePath, 0);
                    } // If this file has a scope in front and not a single date,
                    // it is a file with specific computed results from 
                    // a previous session.
                    else
                    {
                        String fileScope = GraphTime.getFileScope();
                        if (fileScope.equals(scope))
                        {
                            // Processes only files with the appropriate suffix.
                            String suffix = filePath.substring(filePath
                                    .lastIndexOf(".") + 1);
                            // Reads the VerticesTable.
                            if (suffix.endsWith("vrtx"))
                            {
                                vt = io.readVTable(filePath);
                            } // Reads the info arc map.
                            else
                            {
                                if (suffix.endsWith("arcs"))
                                {
                                    infoArcMap = io.readMap(filePath);
                                } // Reads the associations map.
                                else
                                {
                                    if (suffix.endsWith("assc"))
                                    {
                                        assocMap = io.readMap(filePath);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("This is not the right file. Will try the "
                            + "next file. \nProbably you'll have to restart "
                            + "the file selection procedure.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case CONSOLIDATE ->
            {
                try
                {
                    String filename = filePath.substring(filePath
                            .lastIndexOf(separator) + 1);
                    Calendar firstScope = null;
                    Calendar lastScope = null;
                    // Accepts files if the firstScope and lastScope are 
                    // between the dates set by the user. 
                    // This task processes only files with a {DAYn, DAYn+1} 
                    // time window.
                    if (filename.charAt(8) == ('-'))
                    {
                        fileDate = GraphTime.getDate(fileDate, filename);
                        // Gets the two parts of this topic-file's scope.
                        firstScope = GraphTime.getFirstScope(fileDate);
                        lastScope = GraphTime.getLastScope(fileDate);
                    }
                    if (scope.equals("AllDates") || (GraphTime
                            .isBetweenDates(lastScope, beginDate, endDate)
                            && GraphTime.isBetweenDates(firstScope,
                                    beginDate, endDate)
                            && GraphTime.scopeDays() == scopeSpan))
                    {
                        String localScope = getScope(filePath);
                        // Processes only files with the appropriate suffix.
                        String suffix = filePath.substring(filePath.
                                lastIndexOf(".") + 1);
                        List fileList = new ArrayList(4);
                        // Initiates a list with four placeholders because 
                        // it is not known at this stage when and where the 
                        // files with the right dates will emerge.
                        for (int i = 0; i < 4; i++)
                        {
                            fileList.add("");
                        }
                        if (scopeMap.containsKey(localScope))
                        {
                            fileList = ((ArrayList) scopeMap.get(localScope));
                        }
                        switch (suffix)
                        {
                            // Reads the topicMap path into the scopeMap.
                            case "tpc" ->
                            {
                                fileList.remove(0);
                                fileList.add(0, filePath);
                                scopeMap.put(localScope, fileList);
                            }
                            // Reads the rescanned document list path into 
                            // the scopeMap.
                            case "rse" ->
                            {
                                fileList.remove(1);
                                fileList.add(1, filePath);
                                scopeMap.put(localScope, fileList);
                            }
                            // Reads the VerticesTable path into the scopeMap.
                            case "vrtx" ->
                            {
                                fileList.remove(2);
                                fileList.add(2, filePath);
                                scopeMap.put(localScope, fileList);
                            }
                            // Reads the infoVerticesMap path into the scopeMap.
                            case "info" ->
                            {
                                fileList.remove(3);
                                fileList.add(3, filePath);
                                scopeMap.put(localScope, fileList);
                            }
                        }
                    }
                } catch (Exception e)
                {
                    showText("This is not the right file.\nWill try the "
                            + "next file. Probably you'll\nhave to restart "
                            + "the file selection procedure.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            case DOCSIM ->
            {
                // Processes only files with the appropriate suffix.
                String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
                if (suffix.endsWith("tgr"))
                {
                    processTextGraph(filePath, 0);
                } else
                {
                    // Reads the VerticesTable and InfoArcMap. DocAnalysis 
                    // expects only one table. Many from different scopes have 
                    // to be opened to extract the relevant doc-ids and
                    // are concatenated into one vertex table and one 
                    // infoArc table.
                    if (suffix.endsWith("vrtx") || suffix.endsWith("arcs"))
                    {
                        combineFiles(filePath);
                    }
                }
            }
        }
    }

    /**
     * completeTasks: Part Two of the main program controller (MVC model).
     * Performs any additional work to be done on one of the 8 tasks after
     * processing the files from disk handled by the first part. Task 1
     * Tokenizes a raw text document and saves it as a '*.tok' file. Task 2
     * Makes TextGraphs with a '*.tgr' suffix from the tokenized files. It reads
     * the VerticesTable. Constructs the ArcTable. Task 3 Starts making a table
     * with in- and outlinks. Task 4 Calculates the infovalue of every vertex
     * and selects informative tokens with the '*.tgr'-files. Task 5 Starts Arcs
     * & Associations. Reads VerticesTable and matrix list and constructs the
     * ArcTable. Task 6 Starts the topic finding. Task 5 Consolidates all past
     * learning into one structure. Task 6 Retrieve. It reads files for the
     * similarity matrix as a part of the RETRIEVE task in the
     * 'completeTasks(8)'- method.
     *
     * @param task : a flag (int) to indicate what kind of task to complete.
     * @todo : Saving the informative query tokens (from the TopicRetriever) and
     * the facets (from Clusters). Finalizing the Q&A module.
     */
    private void completeTasks(int task) throws NullPointerException, IOException
    {
        switch (task)
        {
            // Coming from case "Build Network"
            // After making the TextGraphs, the DataTable and the CollectionTable 
            // NETWORK constructs a table with all the vertices and a table with 
            // all the labels for all files within the time interval (scope)
            // of this session.
            case 2 ->
            {
                showText(" * " + emptyCount + "  empty files discarded *", 0);
                emptyCount = 0;
                String dtName = getWorkPath() + separator + "AllDocs.dtab";
                String ltName = getWorkPath() + separator + "AllLabels.lbls";
                String ctName = getWorkPath() + separator + "AllColls.ctab";
                String vtName = getWorkPath() + separator + scope 
                        + "_Vertices.vrtx";
                File vtFile = new File(vtName);
                vtFile.createNewFile();

                updateStatusBar(5, "");
                // Writes these files to disk and shows a data summary 
                // on the screen.
                try
                {
                    // Writes and shows the DocTable.
                    dt = gb.getDocTable();
                    io.writeThisObject(dtName, dt);
                    //showText(gb.getDocTable().toString(), 0);
                    // Writes and shows the CollectionTable.
                    ct = gb.getCollectionTable();
                    io.writeThisObject(ctName, ct);
                    //showText(gb.getCollectionTable().collectionsToString(), 0);
                    // Writes the LabelTable. The labels in the NetworkBuilder are 
                    // explicitly removed.
                    lt = gb.getLabelTable();
                    io.writeThisObject(ltName, lt);
                    gb.release(lt);
                    // Writes the VerticesTable. The vertices in the NetworkBuilder 
                    // are explicitly removed.
                    vt = gb.getFullVerticesTable();
                    io.writeThisObject(vtName, vt);
                    gb.release(vt);

                    showText("* Four network files generated and successfully"
                            + " saved *", 0);
                    clearText();
                } catch (IOException ioe)
                {
                    showText("Saving one or more NETWORK-files did not "
                            + "succeed", 0);
                    updateStatusBar(-1, getElapsedTime());
                    ioe.printStackTrace(System.err);
                }
            }
            // LINKS collects, shows and saves links between tokens in this 
            // network.
            case 3 ->
            {
                if (!vt.isEmpty() && !dt.isEmpty() && !ct.isEmpty() 
                        && !lt.isEmpty())
                {
                    updateStatusBar(2, "");
                    // Finalizes construction of the ArcsTable with arcs from 
                    // the individual files.
                    ArcsTable at = new ArcsTable(vt);
                    at.addFullArcsMap(arcsMap);
                    LinkTable lk = new LinkTable(vt, at, dt);
                    // Counts all incoming and outgoing links.
                    lk.putLinksInTable();
                    // Shows general info on screen about the files that were 
                    // used. The final doc-value is added to the DocTable on
                    // screen (boolean 'true').
                    clearText();
                    showText(getDocTable().toString(), 0);
                    showText(getCollTable().collectionsToString(), 0);
                    clearText();
                    try
                    {
                        updateStatusBar(5, "");
                        // Saves DocTable updated with total links per document 
                        // to disk.
                        String dtName = getWorkPath() + separator 
                                + "AllDocs.dtab";
                        io.writeThisObject(dtName, dt);
                        // Saves incoming and outgoing links to disk.
                        String linksPath = getWorkPath() + separator + scope
                                + "_Links.link";
                        Map linksFile = lk.getLinksMap();
                        io.writeThisObject(linksPath, linksFile);
                    } catch (IOException ioe)
                    {
                        showText("Saving LINK-file to disk did not succeed", 0);
                        updateStatusBar(-1, getElapsedTime());
                        ioe.printStackTrace(System.err);
                    }
                } else
                {
                    showText("\nNecessary components are missing, could not "
                            + "collect links.", 0);
                    updateStatusBar(-1, getElapsedTime());
                }
            }
            // TFIDF computes info values and saves the informative tokens.
            case 4 ->
            {
                if (!dt.isEmpty() && !ct.isEmpty() && !vt.isEmpty() 
                        && !lt.isEmpty())
                {
                    updateStatusBar(2, "");
                    // Finalizes construction of the ArcsTable with arcs from
                    // the individual files.
                    ArcsTable at = new ArcsTable(vt);
                    at.addFullArcsMap(arcsMap);
                    lkt = new LinkTable(vt, at, dt);
                    // Calculates the info values of all the token-types.
                    InfoValueCalculator ivc = new InfoValueCalculator(tc);
                    ivc.computeVertexInfoValue();
                    ct = ivc.getCollectionTable();
                    // Disposes of the TokenCount table after info calculation.
                    tc = null;
                    // Gets a list of all the informative token-types in every
                    // document.
                    InformativeTokens it = 
                            new InformativeTokens(mRunBaseLineFilter);
                    it.collectInfoTokens();
                    // Informative tokens shown in the 'Informative Tokens' tab 
                    // on screen when asked. If countOnly is 'true' only the  
                    // number of tokens per info-value will be shown, else if 
                    // 'false' all the token-labels are retrieved. Showing all 
                    // tokens as strings slows down considerably the computer
                    // performance.
                    if (showInfoTBx.isSelected())
                    {
                        if (!showCountBx.isSelected())
                        {
                            countOnly = false;
                        }
                        showText(it.getInformativeTokenString(countOnly), 1);
                        // Comment next line out to exclude the rejected tokens 
                        // from screen.
                        showText(it.getRejectedTokenString(countOnly), 1);
                        clearText();
                        showText("* Informative and rejected tokens available on "
                                + "the 'Informative Tokens' tab *", 0);
                    } else
                    {
                        showText("* Informative tokens calculated, but not shown "
                                + "now. *\n* To view the list, tick the 'Show"
                                + " Info Tokens' checkbox in the Parameters menu "
                                + "before running the application. *", 1);
                    }
                    // New information is saved to disk.
                    try
                    {
                        updateStatusBar(5, "");
                        String vtName = getWorkPath() + separator + scope
                                + "_Vertices.vrtx";
                        String ctName = getWorkPath() + separator + "AllColls.ctab";
                        String InfoName = getWorkPath() + separator + scope
                                + "_InfoTok.info";
                        io.writeThisObject(vtName, it.getUpdatedVertices());
                        io.writeThisObject(ctName, ct);
                        infoMap = it.getFullInfoTokenMap();
                        io.writeThisObject(InfoName, infoMap);
                        showText("* Informative tokens generated and saved *", 0);
                    } catch (IOException ioe)
                    {
                        showText("Saving one or more TFIDF-files did not"
                                + " succeed.", 0);
                        ioe.printStackTrace(System.err);
                    }
                } else
                {
                    showText("\nNecessary components are missing, no TFIDF "
                            + "calculation performed.", 0);
                    updateStatusBar(-1, getElapsedTime());
                }
            }
            // ASSOCIATE finds informative arcs and constructs associations.
            case 5 ->
            {
                if (!dt.isEmpty() && !ct.isEmpty() && !vt.isEmpty() 
                        && !lt.isEmpty()
                        && !infoMap.isEmpty())
                {
                    updateStatusBar(3, "");
                    fileCount = 0;
                    // Finalizes construction of the ArcsTable with arcs from
                    // the individual files.
                    ArcsTable at = new ArcsTable(vt);
                    at.addFullArcsMap(arcsMap);
                    // Gets informative tokens
                    InformativeTokens it = 
                            new InformativeTokens(mRunBaseLineFilter);
                    it.retrieveInfoMap();
                    // Gets informative arcs and special links.
                    GraphAnalyzer ga = new GraphAnalyzer(at, vt, it, dt);
                    filesToProcess = getDocTable().getDocCount();
                    // Retains only the informative token-types (vertices) and 
                    // their arcs.
                    String assocStr = "* Associations counted";
                    if (!showAssocCountBx.isSelected())
                    {
                        countOnly = false;
                        assocStr = "* Full associations";
                    }
                    ga.extractArcsAndVertices(countOnly);
                    clearText();
                    if (showAssocBx.isSelected())
                    {
                        showText(assocStr + " per document available on the "
                                + "'Associations' tab *", 0);
                    }
                    // Saving files on disk.
                    try
                    {
                        updateStatusBar(5, "");
                        // Saves the informative arcs to the workFiles directory 
                        // on disk.
                        String infoArcFile = getWorkPath() + separator + scope
                                + "_InfoArc.arcs";
                        infoArcMap = ga.getFinalArcsMap();
                        io.writeThisObject(infoArcFile, infoArcMap);
                        String assocFile = getWorkPath() + separator + scope
                                + "_Assoc.assc";
                        assocMap = ga.getAssociationMap();
                        io.writeThisObject(assocFile, assocMap);
//                        String fullArcFile = getWorkPath() + separator + scope
//                                + "_FullArc.alls";
//                        io.writeThisObject(fullArcFile, arcsMap);
                    } catch (IOException ioe)
                    {
                        updateStatusBar(-1, getElapsedTime());
                        showText("Saving one or more ASSOCIATE-files to disk did "
                                + "not succeed.", 0);
                        ioe.printStackTrace(System.err);
                    }
                } else
                {
                    showText("\nNecessary components are missing, could not run"
                            + " the GraphAnalyzer", 0);
                    updateStatusBar(-1, getElapsedTime());
                }
            }
            // TOPICS detects topic-facets based on informative arcs shared by 
            // several documents.
            case 6 ->
            {
                if (dt == null || ct == null || vt == null || lt == null
                        || assocMap == null || infoArcMap == null)
                {
                    try
                    {
                        vt = io.readVTable(getWorkPath() + separator + scope
                                + "_Vertices.vrtx");
                        infoArcMap = io.readMap(getWorkPath() + separator + scope
                                + "_InfoArc.arcs");
                        arcsMap = io.readMap(getWorkPath() + separator + scope
                                + "_FullArc.alls");
                    } catch (Exception e)
                    {
                        showText("Necessary components are missing,\n"
                                + "could not collect topics.", 0);
                        updateStatusBar(-1, getElapsedTime());
                        e.printStackTrace(System.err);
                    }
                }
                updateStatusBar(3, "");
                fileCount = 0;
                // Finalizes construction of the arcsTable with arcs from the 
                // individual files.
                ArcsTable at = new ArcsTable(vt);
                at.addFullArcsMap(arcsMap);
                ArcsTable ia = new ArcsTable(vt);
                ia.addFullArcsMap(infoArcMap);
                // Initializes the topical facet maker class.
                FacetMaker TFmaker = new FacetMaker(ia, at, dt, ct);
                // Performs the topical facet construction.
                TFmaker.constructFacets();
                updateStatusBar(5, "");

                // Defines the path to the topicMap.
                String tpc = getWorkPath() + separator + scope
                        + "_Topics.tpc";
                // Defines the path to topic defining arcs.
                String tas = getWorkPath() + separator + scope
                        + "_TopArcs.tas";
                // Defines the path to the successfully rescanned documents.
                String rse = getWorkPath() + separator + scope
                        + "_Rescanned.rse";
                // Path to all arcs map.
                String alls = getWorkPath() + separator + scope
                        + "_FullArc.alls";
                // Defines the path to the unused documents
                String udc = getWorkPath() + separator + "Unused.und";

                // Shows the result of the topic selection on screen if checkBox
                // is selected. Default is not to show the topic table. 
                // If noLabels is 'true' only the statistical info will be shown
                // else all the labels of the facets are retrieved. Showing 
                // strings instead of integers slows down the computer 
                // performance considerably.
                if (!showNoLabelBx.isSelected())
                {
                    noLabels = false;
                }
                if (showTopicsBx.isSelected())
                {
                    TFmaker.showTopics(noLabels);
                } // Shows a general statistic on the number of facets found 
                // in the 'General Info'-tab.
                else
                {
                    TFmaker.showSummary(noLabels);
                }
                // User may choose not to save these files.
                if (TFmaker.saveFiles())
                {
                    //Saving files on disk.
                    try
                    {
                        io.writeThisObject(tpc, TFmaker.getTopicMap());
                        io.writeThisObject(tas, TFmaker.getArcSets());
                        io.writeThisObject(alls, arcsMap);
                        LinkedList thisUnusedList = TFmaker.getFullUnusedDocList();
                        int unused = 0;
                        if (thisUnusedList != null && !thisUnusedList.isEmpty())
                        {
                            io.writeThisObject(udc, thisUnusedList);
                            unused = thisUnusedList.size();
                        }
                        var thisRescannedList = FacetMaker.getRescannedDocs();
                        int rescanned = 0;
                        if (thisRescannedList != null && !thisRescannedList.isEmpty())
                        {
                            io.writeThisObject(rse, thisRescannedList);
                            rescanned = thisRescannedList.size();
                        }
                        showText("* Unused documents saved: "
                                + unused + " *\n* "
                                + "Successfully rescanned: "
                                + rescanned + " *", 0);
                    } catch (IOException ioe)
                    {
                        showText("Saving one or more TOPICS-files to disk"
                                + " did not succeed.", 0);
                        updateStatusBar(-1, getElapsedTime());
                        ioe.printStackTrace(System.err);
                    }
                }
            }
            // CONSOLIDATE collects all day-by-day topic files into maps between
            // the begin- and enddate as set by the user. The facet dictionary  
            // gives every facet a unique global identification number.
            case 7 ->
            {
                // Tells the progress bar on the GUI how many elements to expect.
                updateStatusBar(3, "");
                fileCount = 0;
                int files = scopeMap.size();
                setFilesToProcess(files);
                showText("* Topical Facet files to process: " + files + " *\n", 0);
                int count = 1;

                //What about "AllDates"?
                Iterator scope_itr = scopeMap.keySet().iterator();

                while (scope_itr.hasNext())
                {
                    try
                    {
                        // Gets the scope of these files and turns them over to 
                        // the FacetCollector.
                        String thisScope = scope_itr.next().toString();
                        collector.setScope(thisScope);
                        List fileList = (ArrayList) scopeMap.get(thisScope);
                        // Adds topicMap within this scope.
                        String filePath = fileList.get(0).toString();
                        LinkedHashMap thisFacetMap = io.readLinkedHashMap(filePath);
                        collector.addTopicMap(thisFacetMap, filePath);
                        // Adds the list of rescanned doc-ids in this scope.
                        filePath = fileList.get(1).toString();
                        File rseFile = new File(filePath);
                        if (rseFile.exists())
                        {
                            collector.addRescannedDocs(io.readList(filePath));
                        }
                        // Adds the vertices within this scope.
                        filePath = fileList.get(2).toString();
                        collector.addVertices(io.readVTable(filePath));
                        // Adds the informative vertices in this scope.
                        filePath = fileList.get(3).toString();
                        collector.addInfoVertices(io.readMap(filePath));
                        // Constructs the facet map and cleans up all temporary 
                        // files when ready with this time interval.
                        collector.makeFacetMap();
                        collector.clearFiles();
                        // Shows how many elements have been processed so far.
                        showText("#" + count + "  Scope: " + thisScope + ".  "
                                + "Token-types processed: " + collector
                                        .getProcessed()
                                + ".  Elapsed time: " + getElapsedTime(), 0);
                        count++;
                        // Updates progressbar
                        setFileCount(1);
                    } catch (Exception e)
                    {
                        showText("Reading one or more CONSOLIDATE-files did "
                                + "not succeed.", 0);
                        updateStatusBar(-1, getElapsedTime());
                        e.printStackTrace(System.err);
                    }
                }
                try
                {
                    updateStatusBar(5, "");
                    // Combines the session maps with the global facet maps.
                    Consolidator.finalizeVertexFacetMaps();
                    // Saves the global facet dictionary to disk
                    String dictPath = getGlobalPath() + separator 
                            + "FacetKeys.dic";
                    io.writeThisObject(dictPath, collector.getFacetDictionary());
                    // Saves a list with all scope dates treated so far.
                    String scopePath = getGlobalPath() + separator 
                            + "ScopeFacet.scp";
                    io.writeThisObject(scopePath, collector.getScopeSet());
                    // Saves the DocTable updated with Topical Facet data.
                    String dtName = getWorkPath() + separator + "AllDocs.dtab";
                    io.writeThisObject(dtName, dt);
                } catch (IOException ioe)
                {
                    showText("Saving one or more FACET/DICTIONARY maps did not"
                            + " succeed.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    ioe.printStackTrace(System.err);
                }
            }
            // RETRIEVE finds topical information related to a seed-file 
            // (the 'query').
            case 8 ->
            {
                try
                {
                    if (dt.isEmpty() || ct.isEmpty() || lt.isEmpty())
                    {
                        showText("\nNecessary components are missing, could not "
                                + "execute one or more retrieval tasks.", 0);
                        updateStatusBar(-1, getElapsedTime());
                    }
                    //Reads the global dictionary map that provides the link 
                    // between the global (consolidated) facetkey and the 
                    // local facets.
                    String keyPath = getGlobalPath() + separator 
                            + "FacetKeys.dic";
                    SortedMap dictionaryMap = io.readSortedMap(keyPath);
                    // Initializes the TopicRetriever.
                    TopicRetriever topRetriever = 
                            new TopicRetriever(dictionaryMap);
                    // TopicRetriever searches for relevant topical facets. 
                    // Multiple tasks are possible.
                    Set taskSet = topRetriever.getTaskSet();
                    int taskSetSize = taskSet.size();
                    progress = 3;
                    // Informs ResultExport on the number of tasks to expect.
                    results.setTaskSize(taskSetSize);
                    Iterator task_itr = taskSet.iterator();
                    while (task_itr.hasNext())
                    {
                        String thisTask = task_itr.next().toString();
                        // First step: documents and facets relevant to the
                        // retrieve-task are collected.
                        topRetriever.collectFacets(thisTask);
                        if (topRetriever.getContinue() == false)
                        {
                            showText("Nothing useful found to retrieve!", 0);
                        } else
                        {
                            // Removes mappings no longer needed.
                            topRetriever.clearFacetMaps();
                            // Full paths to the retrieved documents.
                            docsRetrieved = TopicRetriever.getDocIdSet();
                            LinkedHashSet docPaths = TopicRetriever
                                    .getDocPathSet();
                            // Tells the progress bar on the GUI how many 
                            // elements to expect.
                            fileCount = 0;
                            setFilesToProcess(docPaths.size());
                            updateStatusBar(progress, "");
                            // Reads and prepares files for the similarity matrix.
                            Iterator doc_itr = docPaths.iterator();
                            while (doc_itr.hasNext())
                            {
                                setFileCount(1);
                                //setProgessLabel(" Files to combine: " 
                                // + filesToProcess + " - Working on file " 
                                //  + fileCount);
                                String path = doc_itr.next().toString();
                                selectTask(DOCSIM, path);
                            }
                        }             
                        vt = io.readVTable(getWorkPath() + separator + scope
                                + "_Vertices.vrtx");
                        infoArcMap = io.readMap(getWorkPath() + separator + scope
                                + "_InfoArc.arcs");
                        arcsMap = io.readMap(getWorkPath() + separator + scope
                                + "_FullArc.alls");
                        
                        if (vt == null || vt.isEmpty() || infoArcMap == null 
                                || infoArcMap.isEmpty() || arcsMap == null ||
                                        arcsMap.isEmpty())
                        {
                            showText("\nNecessary components are missing, cannot "
                                    + "compute the doc-by-doc similarity matrix "
                                    + "for this task.", 0);
                        } else
                        {
                            // Second step: Calculates and saves the doc-by-doc 
                            // similarity matrix.
                            updateStatusBar(2, "");
                            fileCount = 0;
                            String simTask = TopicRetriever.getTaskName();
                            // Finalizes the construction of the ArcsTable with  
                            // arcs from the individua files that were found to 
                            // be related to this retrieval task.
                            ArcsTable at = new ArcsTable(vt);
                            at.addFullArcsMap(arcsMap);
                            ArcsTable ia = new ArcsTable(vt);
                            ia.addFullArcsMap(infoArcMap);
                            // Keeps only the informative tokens from the query.
                            topRetriever.setSeedTokens(ia);
                            // Computes the doc-by-doc similarity.
                            DocAnalysis docSim = new DocAnalysis(ia);
                            docSim.computeMatrix();
                            progressLabel.setText("Finalizing " + simTask);
                            Clusters cluster = docSim.getClusters();
                            // Reclaims memory.
                            docSim.discard();
                            // Shows parts of the result matrix in a tabbed pane
                            // if there is only one task to process.
                            if (showSimBx.isSelected() && taskSetSize == 1)
                            {
                                // Gets some statistics on the doc-by-doc 
                                // similarity.
                                String summary = docSim.getSummary();
                                // Builds the doc-by-doc similarity view.
                                FinalViewer view = new FinalViewer(dt, at, 
                                        cluster, summary);
                                view.prepareMatrixView();
                            }
                            // Removes the mappings to collections that are no 
                            // longer needed.
                            topRetriever.clearOtherCollections();
                            // Prepares export of the cluster data. Writes the 
                            // similarity data to the Results directory for export 
                            // to Pajek or UCINET, graph visualization programs.
                            results.setClusterMap(new LinkedHashSet(cluster
                                    .getAllDocs().keySet()), simTask);
                            cluster.prepareVisualGraph();
                            VisualGraph vg = cluster.getVisualGraph();
                            vg.setVisualGraph("pajek", taskSetSize);
                            // Retrieves the facets from the main core of this
                            // query. The facets make up the acquired awareness 
                            // of the system about the subjet of the query. 
                            // It is (the base of) an answer about this subjet 
                            // in a Q&A set-up. 
                            
                            // cluster.getMainFacetSet();
                            // UCINET commented out: the program is not installed 
                            // on this machine.
                            // vg.setVisualGraph("ucinet", taskSetSize);
                            
                            // Reclaiming memory.
                            cluster.discard();
                        }
                    }
                    // Saves the results of the retrieval tasks to the Results
                    // directory.
                    if (topRetriever.getContinue())
                    {
                        if (results.resultExists())
                        {
                            String resultPath = getResultPath() + separator
                                    + "TaskResult.map";
                            io.writeThisObject(resultPath, results
                                    .getResultMap());
                            // Turns the evaluation button on in the viewer when 
                            // an evaluation is available.
                            if (showSimBx.isSelected() && taskSetSize == 1)
                            {
                                FinalViewer.enableEvaluation();
                            }
                        }
                        // Presents the results of the different tasks on screen.
                        results.viewResults();
                    }
                } catch (Exception e)
                {
                    showText("Exception while executing the RETRIEVAL task.", 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
            // RECONSTRUCT uses saved data from a query task to rebuild the 
            // similarity viewer.
            case 9 ->
            {
                try
                {
                    if (dt.isEmpty() || ct.isEmpty() || lt.isEmpty())
                    {
                        showText("\nNecessary components are missing, could not "
                                + "execute one or more retrieval tasks.", 0);
                        updateStatusBar(-1, getElapsedTime());
                    }
                    String subPath = mResultPath.substring(mResultPath.
                            lastIndexOf(separator) + 1);
                    String queryName = subPath.substring(17, subPath
                            .lastIndexOf('.'));
                    // Reconstructs the doc-by-doc similarity view with saved
                    // data.
                    progressLabel.setText("Rebuilding the query results");
                    FinalViewer finalView = new FinalViewer(dt, mResultPath,
                            queryName);
                } catch (Exception e)
                {
                    showText("Exception while executing the RECONSTRUCT task."
                            , 0);
                    updateStatusBar(-1, getElapsedTime());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * setDates: Sets the begin- and enddate of a time interval in Calendar and
     * in String format. If 'null' is found all text files are processed, else
     * only those files inside the time interval (scope) are considered.
     * LimitDate restricts the admission of old files to be rescanned to a
     * maximum of days before the begindate. This maximum number of days is set
     * at a default value of 365 but the user can alter this in the 'Scope'
     * entry panel of the Action Menu.
     *
     * @return boolean 'true' if a valid date has been entered, else shows
     * warning message and resets the text fields.
     */
    private boolean setDates()
    {
        boolean dateStatus = false;
        int year;
        int month;
        int day;
        int limitDays;
        // The user selected the 'All Dates' checkbox in the GUI.
        if (allDateBx.isSelected())
        {
            beginDate = null;
            endDate = null;
            limitDate = null;
            dateStatus = true;
        } // A date has been entered.
        else
        {
            try
            {
                // Begin date formatting.
                year = (Integer.parseInt(startYear.getText()));
                month = (Integer.parseInt(startMonth.getText()));
                day = (Integer.parseInt(startDay.getText()));
                limitDays = (Integer.parseInt(timeLimit.getText()));
                beginDateString = year + GraphTime.timeString(month)
                        + GraphTime.timeString(day);
                beginDate = GraphTime.setCalendar(year, month - 1, day);
                limitDate = GraphTime.setCalendar(year, month - 1, day);
                limitDate.add(Calendar.DAY_OF_YEAR, -limitDays);
                // End date formatting.
                year = (Integer.parseInt(endYear.getText()));
                month = (Integer.parseInt(endMonth.getText()));
                day = (Integer.parseInt(endDay.getText()));
                endDateString = year + GraphTime.timeString(month)
                        + GraphTime.timeString(day);
                endDate = GraphTime.setCalendar(year, month - 1, day);
                if (endDate.before(beginDate))
                {
                    throw new NumberFormatException();
                }
                dateStatus = true;
            } // Resets the entry fields in the GUI.
            catch (NumberFormatException nfe)
            {
                beginDate = null;
                endDate = null;
                scopeFrame.dispose();
                startYear.setText("yyyy");
                startYear.selectAll();
                startMonth.setText("mm");
                startMonth.selectAll();
                startDay.setText("dd");
                startDay.selectAll();
                endYear.setText("yyyy");
                endYear.selectAll();
                endMonth.setText("mm");
                endMonth.selectAll();
                endDay.setText("dd");
                endDay.selectAll();
                timeLimit.setText("365");
                timeLimit.selectAll();
                setSettings();
                makeWarningMessage(6);
            }
        }
        return dateStatus;
    }

    /**
     * setSettings: Reads the last used settings (dates, community, task, core
     * and doc selection) from a *.txt-file in the main TopicDetection directory
     * and sends it to the appropriate fields. The 'updateSettings' method
     * writes any changes made by the user to this file.
     */
    private void setSettings()
    {
        int i = 0;
        String[] tmpSettings = new String[8];
        String datesPath = dir + workFiles + separator + "tdtSettings.txt";
        File file = new File(datesPath);
        try
        {
            FileReader fr = new FileReader(file);
            try ( BufferedReader in = new BufferedReader(fr))
            {
                String line;
                while ((line = in.readLine()) != null)
                {
                    tmpSettings[i] = line;
                    i++;
                }
            }
        } catch (IOException ioe)
        {
            showText("* No Settings File found *", 0);
            ioe.printStackTrace(System.err);
        }
        String start = tmpSettings[0];
        String end = tmpSettings[1];
        String allDates = tmpSettings[2];
        String myCommunity = tmpSettings[3];
        String mySource = tmpSettings[4];
        String myTask = tmpSettings[5];
        String core = tmpSettings[6];
        int selection = Integer.parseInt(tmpSettings[7]);
        if (!start.equals(""))
        {
            startYear.setText(start.substring(0, 4));
            startYear.selectAll();
            startMonth.setText(start.substring(4, 6));
            startMonth.selectAll();
            startDay.setText(start.substring(6, 8));
            startDay.selectAll();
        }
        if (!end.equals(""))
        {
            endYear.setText(end.substring(0, 4));
            endYear.selectAll();
            endMonth.setText(end.substring(4, 6));
            endMonth.selectAll();
            endDay.setText(end.substring(6, 8));
            endDay.selectAll();
        }
        if (allDates.equals("true"))
        {
            allDateBx.setSelected(true);
        }
        if (!myCommunity.equals(""))
        {
            communityBox.setSelectedIndex(communityList.indexOf(myCommunity));
        }
        if (!mySource.equals(""))
        {
            sourceBox.setSelectedIndex(sourceList.indexOf(mySource));
        }
        if (!myTask.equals(""))
        {
            taskBox.setSelectedIndex(taskList.indexOf(myTask));
        }
        if (!core.equals(""))
        {
            coreNumber.setText(core);
        }
        switch (selection)
        {
            case 0 ->
                highRB.setSelected(true);
            case 1 ->
                middleRB.setSelected(true);
            case 2 ->
                lowRB.setSelected(true);
            case 3 ->
                allRB.setSelected(true);
            default ->
                middleRB.setSelected(true);
        }
    }

    /**
     * setFileCount: Adds '1' to the file counter after processing a file. Used
     * to update the progress bar of the GUI.
     *
     * @param count : int 1.
     */
    public static void setFileCount(int count)
    {
        fileCount += count;
        updateStatusBar(progress, "");
    }

    /**
     * setFilesToProcess: Sets the number of activities to process. Used in the
     * progressbar of the GUI.
     *
     * @param number : number of things to do (int).
     */
    public static void setFilesToProcess(int number)
    {
        filesToProcess = number;
    }

    /**
     * setProgessLabel: Changes the text being displayed in the progresslabel.
     * The actual text is saved so that it can be reset.
     *
     * @param newText : String to display in the progessLabel.
     */
    public static void setProgessLabel(String newText)
    {
        prevProgressText = progressLabel.getText();
        progressLabel.setText(newText);
    }

    /**
     * setDataPath: Sets the name of the tokenized data directory identified by
     * the argument.
     *
     * @param arg : String that identifies the tokenized data (*.tok) directory.
     */
    private void setDataPath(String arg) throws IOException
    {
        String store[] = new String[4];
        Iterator store_itr = dataFileList.iterator();
        while (store_itr.hasNext())
        {
            store = (String[]) store_itr.next();
            if (arg.equals("1") || store[1].equals(arg))
            {
                break;
            }
        }
        dataStore = store[2];
        File pathToData = new File(dir + workFiles + separator + communityName
                + separator + dataStore + separator);
        pathToData.mkdir();

        dataPath = pathToData.toString();
    }

    /**
     * setGraphStoreName: Sets the name of the TextGraph directory identified by
     * the argument.
     *
     * @param arg : String that identifies the TextGraph directory.
     * @throws throws IOException
     */
    public static void setGraphStoreName(String arg) throws IOException
    {
        String store[] = new String[4];
        Iterator store_itr = dataFileList.iterator();
        while (store_itr.hasNext())
        {
            store = (String[]) store_itr.next();
            if (arg.equals("1") || store[1].equals(arg))
            {
                break;
            }
        }
        graphStore = store[3];
        File pathToGraph = new File(dir + workFiles + separator + communityName
                + separator + graphStore + separator);
        pathToGraph.mkdir();

        graphPath = pathToGraph.toString();
    }

    /**
     * resetProgressText: Resets the text previously in the progressLabel.
     */
    public static void resetProgressText()
    {
        progressLabel.setText(prevProgressText);
    }

    /**
     * showText: Shows a text output in the message panel of the selected tab.
     *
     * @param text : to print in the message panel (String).
     * @param tab : the index of the tabbedPane where this text should go (int).
     */
    public static void showText(String text, int tab)
    {
        try
        {
            myText += text + '\n';
            messageArea[tab].setText(myText);
            messageArea[tab].setCaretPosition(myText.length());
            messageArea[tab].requestFocusInWindow();
        } catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * clearText: Empties the myText String for the showText method.
     */
    public static void clearText()
    {     
       myText = "";    
    }

    /**
     * clearAllText: removes all text from the 'General' tab
     */
    public static void clearAllText()
    {     
        messageArea[0].setText("");   
    }
    
    /**
     * getBeginDate: Returns the begin date of a scope (a time interval bounding
     * any action by the application).
     *
     * @return the beginDate (Calendar) now in use.
     */
    public static Calendar getBeginDate()
    {
        return beginDate;
    }

    /**
     * getEndDate: Returns the end date of a scope (a time interval bouding
     * action by the application).
     *
     * @return the endDate (Calendar) now in use.
     */
    public static Calendar getEndDate()
    {
        return endDate;
    }

    /**
     * getVerticesTable: Getter returns a VerticesTable instance holding a map with 
     * the label-index as key and a vertex instance as value and methods to access 
     * these data.
     *
     * @return the VerticesTable.
     */
    public static VerticesTable getVerticesTable()
    {
        return vt;
    }

    /**
     * getDocTable: Getter returns a DocTable instance holding a map with the
     * unique documentNr as key and instances of DocStats as value together with
     * methods to access these data.
     *
     * @return the DocTable.
     */
    public static DocTable getDocTable()
    {
        return dt;
    }

    /**
     * getCollTable: Getter returns a CollectionTable instance holding a map
     * with a collection string as key and an instance of CollectionData as
     * value and methods to access these data.
     *
     * @return the CollectionTable.
     */
    public static CollectionTable getCollTable()
    {
        return ct;
    }

    /**
     * getInfoMap: Getter returns the infoMap. Key: collectionkey (String),
     * value: map with token index (key - Integer) and info weight (value -
     * Double).
     *
     * @return the infoMap (HashMap).
     */
    public static Map getInfoMap()
    {
        return infoMap;
    }

    /**
     * getAssociationMap: Getter returns the assocMap. Key: docInt (Integer),
     * value: list with LinkedList of arc keys (String) that represent an
     * association (named entity, collocation, etc.) or phrase.
     *
     * @return the assocMap (HashMap).
     */
    public static Map getAssociationMap()
    {
        return assocMap;
    }

    /**
     * getInfoArcMap: Getter returns the infoArcMap. Key: docInt (Integer),
     * value: list with informative arcs from that document.
     *
     * @return the infoArcMap (HashMap)
     */
    public Map getInfoArcMap()
    {
        return infoArcMap;
    }

    /**
     * getDocMatrixList: Getter returns the doc-by-doc matrix list. Key:
     * collectionkey (String), value: map with token index (key - Integer) and
     * info weight (value - Double).
     *
     * @return the doc-by-doc matrix list (linkedList).
     */
    public static LinkedList getDocMatrixList()
    {
        return docMatrixList;
    }

    /**
     * getLabels: Getter returns a LabelTable class holding a map with the
     * token-type as value and an index as key (Integer) together with methods
     * to access these data.
     *
     * @return the LabelTable
     */
    public static LabelTable getLabels()
    {
        return lt;
    }

    /**
     * getLinkTable: Getter returns a LinkTable instance to allow the counting
     * of the links of a vertex by the InfoValueCalculator.
     *
     * @return the LinkTable.
     */
    public static LinkTable getLinkTable()
    {
        return lkt;
    }

    /**
     * getUnusedList: Returns a list with doc-ids to be rescanned to the
     * FacetMaker.
     *
     * @return LinkedList with doc-ids (int).
     */
    public static LinkedList getUnusedList()
    {
        return unusedList;
    }

    /**
     * getTextGraphName: Constructs a filename and filepath for a text graph.
     *
     * @param filePath : the original path (String).
     * @return the new file path (String).
     */
    private String getTextGraphName(String filePath)
    {
        StringBuilder tmpFile = new StringBuilder();
        int start = filePath.lastIndexOf(separator) + 1;
        int end = filePath.lastIndexOf('.');
        tmpFile.append(filePath.substring(start, end)).append(".tgr");
        return tmpFile.toString();
    }

    /**
     * getScope: Gets the scope of a dataFile (the begin- and enddate of a time
     * interval bounding all action).
     *
     * @param filePath : the original path (String).
     * @return the scope of this data file (String).
     */
    private String getScope(String filePath)
    {
        String tmpFile;
        if (scope.equals("AllDates"))
        {
            tmpFile = "AllDates";
        } else
        {
            int start = filePath.lastIndexOf(separator) + 1;
            int end = filePath.lastIndexOf('_');
            tmpFile = filePath.substring(start, end);
        }
        return tmpFile;
    }

    /**
     * getIniList: Gets the content of any initiation file tdt***txt.file to
     * fill an array (ArrayList) in the menu.
     *
     * @ToDo: refactor into ArrayList
     *
     * @param listName : the txt.file (String).
     * @return the combo content (ArrayList).
     */
    private ArrayList getIniList(String listName, String path)
    {
        ArrayList arrayList = new ArrayList();
        String fileName = path + listName;
        File file = new File(fileName);
        try ( FileReader fr = new FileReader(file);  BufferedReader in
                = new BufferedReader(fr))
        {
            String line;
            while ((line = in.readLine()) != null)
            {
                arrayList.add(line);
            }
        } catch (FileNotFoundException fnfe)
        {
            showText("* No such file found *", 0);
        } catch (IOException ioe)
        {
            showText("* No such file found *", 0);
            ioe.printStackTrace(System.err);
        }
        return arrayList;
    }

    /**
     * getFingerprint: tdtFingerprint.txt is prepared manually prior to using
     * this program. It describes a number of properties of the data files
     * available for processing. Fills an array with 6 slots to hold formatting
     * info based on the name of the source. If the source name or date format
     * is undefined, empty strings are set. The fingerPrint txt-file contains
     * source specific info: the full name of the source (0), a short name (1),
     * the date format used by this source (2), a data store indicator (3), the
     * file locale (4) and a regex pattern to extract the source name from the
     * file name (5).
     *
     * @param sourceName : the name of the selected source (String).
     */
    private void getFingerprint(String sourceName)
    {
        String[] temp;
        String thisFile = "Undefined";
        if (!sourceName.equals("Undefined"))
        {
            thisFile = sourceName;
        }
        String fileName = dir + workFiles + separator + "tdtFingerprint.txt";
        File file = new File(fileName);
        try ( FileReader fr = new FileReader(file);  BufferedReader in
                = new BufferedReader(fr))
        {
            String line;
            while ((line = in.readLine()) != null)
            {
                temp = Algorithms.getLineSplit(line);
                if (temp[0].equalsIgnoreCase(thisFile))
                {
                    fingerPrint = temp;
                }
            }
        } catch (IOException ioe)
        {
            showText("* No FingerPrint File found for " + thisFile + " *", 0);
            ioe.printStackTrace(System.err);
        }
    }

    /**
     * getDataStorage: tdtDataStorage.txt is prepared manually prior to using
     * this program. It lists the directory name for the tokenized files (Data1,
     * Data2, ...) and for the text graph data (TextGraph1, TextGraph2, ...) for
     * every 'community' that has data available. Method to retrieve the names
     * of the files where the data are stored for this community.
     *
     * @param community : name of the community now being processed (String).
     * @return an ArrayList with the names of the files where data are stored
     * (String).
     */
    private List getDataStorage(String community)
    {
        String[] temp;
        List dataStoreList = new ArrayList();
        String thisCommunity = "undefined";
        if (!community.equals("undefined"))
        {
            thisCommunity = community;
        }
        String fileName = dir + workFiles + separator + "tdtDataStorage.txt";
        File file = new File(fileName);
        try ( FileReader fr = new FileReader(file);  BufferedReader in
                = new BufferedReader(fr))
        {
            String line;
            while ((line = in.readLine()) != null)
            {
                temp = Algorithms.getLineSplit(line);
                if (temp[0].equalsIgnoreCase(thisCommunity))
                {
                    dataStoreList.add(temp);
                }
            }
        } catch (IOException ioe)
        {
            showText("* No DataStorage File found for " + thisCommunity
                    + " *", 0);
            ioe.printStackTrace(System.err);
        }
        return dataStoreList;
    }

    /**
     * getScopeOverlap: Sets 'true' if the first and second month of the scope
     * are different and 'false'if they are equal. This to catch the possible
     * overlap between a set of data from one month with the set of data from
     * the next month. Used to find the right data store.
     *
     * @return boolean 'false' if scope is "AllDates" or if both months are the
     * same, 'true' otherwise.
     */
    private boolean getScopeOverlap()
    {
        if (scope.equals("AllDates"))
        {
            return false;
        }
        String firstMonth = scope.substring(4, 6);
        String secondMonth = scope.substring(13, 15);
        return !firstMonth.equals(secondMonth);
    }

    /**
     * getDataStoreArg: Returns the argument to identify the data file directory
     * for this community. Here the first month of the scope or the filename is
     * used to identify the data storage directory. The second month of the
     * scope is checked. If the second month is different from the first (when
     * there is a datum overlap) the application has to work with two
     * directories. As default value or when 'AllDates' is the string,
     * everything goes into the first available data store. In another
     * application other arguments to distribute the data over several files may
     * be used and should be implemented here.
     *
     * @param storeFlag : flag (int) to indicate whether to use the first (flag
     * 0) or the second month (flag 1) of the string as an argument. Flag 2
     * extracts from the US TDT file; flag 3 from Belgian file.
     * @param storeString : String with either the scope or the filename needed
     * to extract the month.
     * @return the String used to select the appropriate data directory.
     */
    public static String getDataStoreArg(int storeFlag, String storeString)
    {
        String argString = "1";
        if (storeString.equals("AllDates"))
        {
            return "1";
        }
        switch (storeFlag)
        {
            case 0, 2 ->
                argString = storeString.substring(4, 6);
            case 1 ->
                argString = storeString.substring(13, 15);
            case 3 ->
                argString = storeString.substring(2, 4);
        }
        return argString;
    }

    /**
     * getLongName: Getter with the long name of this file.
     *
     * @return longname (String).
     */
    public static String getLongName()
    {
        return fingerPrint[0];
    }

    /**
     * getShortName: Getter returns the short name of this file.
     *
     * @return short name (String).
     */
    public static String getShortName()
    {
        return fingerPrint[1];
    }

    /**
     * getDateFormat: Getter returns the date format of this file.
     *
     * @return the date format (String).
     */
    private String getDateFormat()
    {
        return fingerPrint[2];
    }

    /**
     * getDataLocator: Getter returns format for the data locator.
     *
     * @return the data locator (String).
     */
    private String getDataLocator()
    {
        return fingerPrint[3];
    }

    /**
     * getLanguage: Getter returns the language used in this session.
     *
     * @return the language (String).
     */
    private static String getLanguage()
    {
        return fingerPrint[4];
    }

    
    /**
     * getRegexSource: Getter returns from fingerPrint[5] the compiled regex pattern
     * to extract the source's name from its filename, or 'NOREGEX' is there is none.
     *
     * @return the regex pattern (Pattern).
     */
    private Pattern getRegexSource() throws PatternSyntaxException
    {
        Pattern sourcePattern = Pattern.compile(fingerPrint[5]);
        return sourcePattern;
    }
    
    /**
     * getThisLocale: Getter returns the locale for this file. Sets the default
     * locale if empty.
     *
     * @return the locale.
     */
    public static Locale getThisLocale()
    {
        Locale loc;
        try
        {
            loc = new Locale(fingerPrint[4]);
        } catch (NullPointerException npe)
        {
            loc = Locale.getDefault();
        }
        return loc;
    }

    /**
     * getScope: Getter returns the scope (begin- and enddate of a time interval
     * bounding all action). If all the data are used, the value is 'AllDates'.
     *
     * @return the scope (String).
     */
    public static String getScope()
    {
        return scope;
    }
    
    /**
     * getSource: Getter returns the name of the location of the sources as
     * selected by the user for this session.
     * 
     * @return the name of the source location.
     */
    public String getSource()
    {
        return sourceName;
    }

    /**
     * getCoreNumber: The number of main cores to be extracted by the Clusters
     * class.
     *
     * @return the number of cores (int) as set by the user in the parameter
     * settings.
     */
    public static int getCoreNumber()
    {
        return Integer.parseInt(coreNumber.getText());
    }

    /**
     * getCommunity: Getter returns the community name.
     *
     * @return the community name (String).
     */
    public static String getCommunity()
    {
        return communityName;
    }

    /**
     * getLastDate: Returns the last date for files in the workfiles directory
     * in the scope of this session.
     *
     * @return String with the last file date.
     */
    public static String getLastDate()
    {
        return endDateString;
    }

    /**
     * getCorpusfiles: Getter returns the pathname to the directory containing
     * the corpus data.
     *
     * @return path to the corpus directory (String).
     */
    public static String getCorpusfiles()
    {
        return corpusFiles;
    }

    /**
     * getDataPath: Getter returns the pathname to the data directory with the
     * raw parsed and tokenized data ('*.tok' and '*.emp' suffixes).
     *
     * @return path to the 'Data*'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getDataPath() throws IOException
    {
        return dataPath;
    }

    /**
     * getGraphPath: Getter returns the pathname to the TextGraph directory with
     * the (*.tgr) text graph files. There is one '*.tgr'-file for every useful
     * document in the repository.
     *
     * @return path to the 'TextGraph*'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getGraphPath() throws IOException
    {
        return graphPath;
    }

    /**
     * getWorkPath: Getter returns the pathname for the workfiles (.grph, dtab,
     * etc.). Workfiles of a community are actively used as input for computing
     * the TF*IDF, the Topical Facets and doc-doc similarity.
     *
     * @return path to the 'WorkFiles'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getWorkPath() throws IOException
    {
        File workPath = new File(dir + workFiles + separator + communityName
                + separator + "WorkFiles");
        workPath.mkdir();
        return workPath.toString();
    }

    /**
     * getResultPath: Getter returns the pathname for the result-files (.top,
     * .net, etc.) of a community. Result files do not serve as data input but
     * present final results and can be used by third party programs (such as
     * Pajek) for further analysis.
     *
     * @return path to the 'Results'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getResultPath() throws IOException
    {
        File resultPath = new File(dir + workFiles + separator + communityName
                + separator + "Results");
        resultPath.mkdir();
        return resultPath.toString();
    }

    /**
     * getGlobalPath: Getter returns the pathname for the global files. Global
     * files consolidate the vertex-facet data and allow the expansion of a
     * 'query' and the link to the relevant documents.
     *
     * @return path to the 'Global'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getGlobalPath() throws IOException
    {
        File globalPath = new File(dir + workFiles + separator + communityName
                + separator + "Global");
        globalPath.mkdir();
        return globalPath.toString();
    }

    /**
     * getRetrievalPath: Getter returns the pathname for the retrieval files.
     * Retrieval files describe saved retrieval tasks to be performed.
     *
     * @return path to the 'Retrieval'-directory of the community (String).
     * @throws throws IOException
     */
    public static String getRetrievalPath() throws IOException
    {
        File retrievalPath = new File(dir + workFiles + separator + communityName
                + separator + "Retrieval");
        retrievalPath.mkdir();
        return retrievalPath.toString();
    }

    /**
     * getAssociationStatus: Returns status of the user selection
     * 'Associations'. Default is 'false'.
     *
     * @return status (boolean 'true' or 'false') of the checkbox to the
     * GraphAnalyzer.
     */
    public static boolean getAssociationStatus()
    {
        return showAssocBx.isSelected();
    }

    /**
     * getShowLinksStatus: Returns status of the user selection 'Show Links'. If
     * 'true' the incoming and outgoing links are shown. Default is 'false'.
     *
     * @return status (boolean 'true' or 'false') of the 'show links' checkbox.
     */
    public static boolean getShowLinksStatus()
    {
        return showLinkBx.isSelected();
    }

    /**
     * getShowTopicsStatus: Returns status of the user selection 'Show Topics'.
     * Default is 'false'.
     *
     * @return status (boolean 'true' or 'false') of the 'Show Topics' checkbox.
     */
    public static boolean getShowTopicsStatus()
    {
        return showTopicsBx.isSelected();
    }

    /**
     * showFacets: Returns status of the user selection 'Show Query Expansion'.
     * If 'false' (default) the FacetViewerGUI is not displayed.
     *
     * @return status ('true' or 'false') of the 'Show Query Expansion'
     * checkbox.
     */
    public static boolean showFacets()
    {
        return showFacetBx.isSelected();
    }

    /**
     * showSimilarity: Returns status of the user selection 'Show Results' in 
     * the Parameter settings of the GUI.
     * If 'true' (default) the doc-by-doc similarity panel in the
     * GUI is displayed.
     *
     * @return status ('true' or 'false') of the 'Show Results' checkbox.
     */
    public static boolean showSimilarity()
    {
        return showSimBx.isSelected();
    }

    /**
     * getRetrievedSelection: Getter returns the user selection for the
     * retrieved documents in the RETRIEVE task. Choices are: '0' for docs with
     * high facet count, '1' for average facet count, '2'for low facet count and
     * '3' for returning all docs that have been retrieved.
     *
     * @return user selection (int) in the Parameter setting 'Retrieved Docs'.
     */
    public static int getRetrievedSelection()
    {
        int selection = 0;
        // Defines the number of retrieved docs in the RETRIEVE task (8).
        if (highRB.isSelected())
        {
            selection = 0;
        } // Docs with highest facet count.
        else
        {
            if (middleRB.isSelected())
            {
                selection = 1;
            } // Docs with a highUp facet count.
            else
            {
                if (lowRB.isSelected())
                {
                    selection = 2;
                } // Docs with midUp facet count.
                else
                {
                    if (allRB.isSelected())
                    {
                        selection = 3;
                    }
                }
            }
        }    // All retrieved docs (lowUp).
        return selection;
    }

    /**
     * getTabbedPane: Returns the active tabbedPane in the GUI
     *
     * @return tabbedPane (JTabbedPane).
     */
    public static JTabbedPane getTabbedPane()
    {
        return tabbedPane;
    }

    /**
     * getIcon: Getter returns the icon to be displayed in the frame border of
     * the GUI.
     *
     * @return image of the frame icon (Image).
     */
    public static Image getIcon()
    {
        return new ImageIcon(icon).getImage();
    }

    /**
     * getScreensize: Getter returns the screensize of this machine.
     *
     * @return dimension of this screen.
     */
    public static Dimension getScreensize()
    {
        return screenSize;
    }

    /**
     * attributeDocId: Attributes an unique identification number to each
     * document. If an id already exists for this file it will be used.
     * Otherwise, DocTable returns the last available identification number
     * increased by one.
     *
     * @param filePath : the full path to a text file (String).
     * @return a document identification (int).
     */
    private int attributeDocId(String filePath)
    {
        int docNr = -1;
        // If this document was seen before the existing document-id is retrieved.
        if (docListExists)
        {
            String docName = filePath.substring(filePath.lastIndexOf(separator)
                    + 1, filePath.lastIndexOf('.'));
            docNr = dt.getDocNr(docName);
        }
        // When no existing identification was found, the last id + 1 is returned
        // by the DocTable.
        if (docNr == -1)
        {
            docNr = dt.getNextId();
        }
        return docNr;
    }

    /**
     * setCursor: Sets a cursor in the tabbedPane.
     *
     * @param cursor : cursor identification number (int).
     */
    public static void setCursor(int cursor)
    {
        if (cursor == 0)
        {
            tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else
        {
            if (cursor == 1)
            {
                tabbedPane.setCursor(Cursor
                        .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
     * getApplicationStartTime: Returns the start time of this application.
     *
     * @return start of the application in milliseconds (long).
     */
    public static long getApplicationStartTime()
    {
        return mAppStart;
    }
    
    /**
     * prepareBaseLineArray: reading an array with common words in the language 
     * of this session and transforming it into an array of label-integers to be
     * used by the InformativeTokens class.
     * Reason: in a specialized collection of documents technical terms may
     * appear so frequently that their informative value is low according to 
     * the standard TF/IDF procedure, resulting in a poor retrieval performance 
     * with queries that contain these terms.
     * By comparing these high frequent special tokens with a small set 
     * of frequent words from a general language corpus, their status of 
     * important words is restored.
     * @throws throws exception
     */
    public static void prepareBaseLineArray() throws Exception
    {
        String language = getLanguage();
        // Set with tokens unknown to the application.
        mUnknownTokenSet = new HashSet();
        // The id of a token delivered by the application.
        Integer tokenId;
        // Central repository for all the labels used in this dataGraph with 
        // an id (integer) and the name (string) of every token-type encountered
        // in the corpus in view.
        LabelTable labels = new LabelTable();
        // Counters
        int zeroLabel = 0;
        int tokenCount = 0;
        // Acquiring the common words
        ArrayList<String> BaseLineArray = new ArrayList<>();
        String fileName = dir + workFiles + separator + language
                + "_baseLine.txt";
        File file = new File(fileName);
        try ( FileReader fr = new FileReader(file);  
                BufferedReader in = new BufferedReader(fr))
        {
            String line;
            while ((line = in.readLine()) != null)
            {
                BaseLineArray.add(line);
            }
        } catch (FileNotFoundException fnfe)
        {
            showText("* No baseLine words found *", 0);
        } catch (IOException ioe)
        {
            showText("* Could not read the baseLine words *", 0);
            ioe.printStackTrace(System.err);
        }

        // Comparing the words used in this session with the common word list.
        try
        {
            labels = io.readLTable(getWorkPath() + separator + "AllLabels.lbls");
        } catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }

        Iterator tok_itr = BaseLineArray.iterator();
        while (tok_itr.hasNext())
        {
            String token = tok_itr.next().toString();
            try
            {
                tokenId = labels.getVertexIndex(token);
                if (tokenId == null)
                {
                    mUnknownTokenSet.add(token);
                    zeroLabel++;
                } else
                {
                    mBaseLineArray.add(tokenId);
                    tokenCount++;
                }
            } catch (Exception e)
            {
                mUnknownTokenSet.add(token);
            }
        }
        Collections.sort(mBaseLineArray);
        showText("Baseline tokens for '" + language + "' prepared.\n" + tokenCount 
                + " tokens recognized, "+ zeroLabel + " not recognized", 0);
    }
    
        /**
     * getBaseLine: array with common words in the language of this session.
     * 
     * @return baseLineArray
     */
    public static ArrayList<Integer> getBaseLine()
    {
          return mBaseLineArray; 
    }

        /**
     * getOriginalFilename 
     * @param docName the internal name as argument
     * @return the orignal name (String)
     */
    public static String getOriginalFilename(String docName)
    {       
        return (String) mOriginalFilenames.get(docName);
    }
    
    /**
     * getAllOriginalFilenames returns a String with the internal name (String) as 
     * Key and the original filename (String) as Value for all filenames in a 
     * printable presentation.
     *
     * @return String with all internal and original document names.
     */
    public static String getAllOriginalFilenames()
    {
        StringBuilder originalFilesSb = new StringBuilder();
        Iterator iter = mOriginalFilenames.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
            originalFilesSb.append(entry.getKey());
            originalFilesSb.append(" = ").append('"');
            originalFilesSb.append(entry.getValue());
            originalFilesSb.append('"');
            if (iter.hasNext())
            {
                originalFilesSb.append('\n');
            }
        }
        return originalFilesSb.toString();
    }
    
    /**
     * makeWarningMessage Constructs different messages for use in this GUI.
     *
     * @param msge : Message flag (int).
     */
    private void makeWarningMessage(int msge)
    {
        switch (msge)
        {
            case 1 -> // Warning message about default source and destination
                //(PREPROCESSING).
                JOptionPane.showMessageDialog(tabbedPane,
                        "Files will be saved to the directory:\n" 
                                + communityName + "/" + dataStore 
                                + "\nNo source was selected\n< " + sourceName 
                                + " >\nis set as default.",
                        "Default Source", JOptionPane.WARNING_MESSAGE);
            case 2 -> // Message about source and destination choice for 
                // the workFiles (PREPROCESSING).
                JOptionPane.showMessageDialog(tabbedPane,
                        "Files will be saved to the directory :\n" 
                                + communityName + "/"+ dataStore + "\n< " 
                                + sourceName + " >\nwas selected as source.",
                        "Preprocessing", JOptionPane.PLAIN_MESSAGE);
            case 3 -> // Warning message concerning destination of the graphs.
                JOptionPane.showMessageDialog(tabbedPane,
                        "No community was selected.\nThe files will be saved "
                                + "to the\n" + communityName + "/" + dataStore 
                                + " directory.", "Default Community",
                        JOptionPane.WARNING_MESSAGE);
            case 4 -> // Message at the start of TEXTGRAPH.            
                JOptionPane.showMessageDialog(tabbedPane,
                        "A Time Interval is used for this task.\n"
                        + "Only files between " + beginDateString
                        + " and " + endDateString + "\nwill be processed "
                        + "from the community:\n'"
                        + communityName + "' and saved in the\n"
                        + communityName + "/" + graphStore + " directory",
                        taskName, JOptionPane.PLAIN_MESSAGE);
            case 5 ->
                // Message at the start of BUILD, DOCSIM or TOPICS.
                JOptionPane.showMessageDialog(tabbedPane,
                        "A Time Interval is used for this task.\n"
                        + "Only files between " + beginDateString
                        + " and " + endDateString + "\nwill be processed"
                        + " from the" + " community:\n'"
                        + communityName + "' and saved in the\n"
                        + communityName + "/" + graphStore + " and the\n"
                        + communityName + "/WorkFiles' directory.",
                        taskName, JOptionPane.PLAIN_MESSAGE);
            case 6 -> // Warning message after wrong date setting.
                JOptionPane.showMessageDialog(tabbedPane,
                        "Wrong dates were set.\nPlease enter again"
                                + "\nand check all settings!",
                        "Inconsistent Scope", JOptionPane.ERROR_MESSAGE);
            case 7 -> // Search Topic Instructions.
                JOptionPane.showMessageDialog(tabbedPane,
                        "The program will search between " + beginDateString 
                                + " and "+ endDateString + "\nfor topics from "
                                        + "the community: *" + communityName 
                                + "*.\n\nWait for the 'Global' directory to "
                                        + "appear, then open it.", 
                        "Retrieval Task", JOptionPane.PLAIN_MESSAGE);
            case 8 -> // Warning when no seed-file was found.
                JOptionPane.showMessageDialog(tabbedPane,
                        "No taskMap was found\nPlease prepare the appropriate "
                                + "data first, then retry",
                        "Retrieval Task", JOptionPane.ERROR_MESSAGE);
            case 9 -> // No time interval will be used
                JOptionPane.showMessageDialog(tabbedPane,
                        "No Time Interval is used for this task.\n"
                        + "All the files from the selected directory \n"
                        + "will be processed and results saved in the\n"
                        + communityName + "/" + graphStore + " and the\n"
                        + communityName + "/WorkFiles directory.",
                        taskName, JOptionPane.PLAIN_MESSAGE);
            case 10 -> // Empty fields
                JOptionPane.showMessageDialog(tabbedPane,
                       "Before you proceed, please close the 'Task Selection',"
                               + "\nthen select the Community, the Source "
                               + "Location\nand the Scope of your query under "
                               + "'Action' in the Menu."
                               + "\nNecessary once at the start of a session.",
                        "Empty Settings", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * main: Main Start of this TDT-application package.
     *
     * @param args : Default arguments.
     * @throws Exception not specified.
     */
    public static void main(String[] args) throws Exception
    {
        createAndShowGUI();
    }
    // GUI components declaration.
    private static JFrame mainFrame;
    private final JFrame scopeFrame;
    private final JFrame communityFrame;
    private final JFrame sourceFrame;
    private final JFrame taskFrame;
   //private JFrame seedFrame;
    private final JFrame paraFrame;
    private final JFrame aboutFrame;
    private final JFrame lookUpFrame;
    private final JPanel retrievePanel;
    private static JPanel progressPanel;
    private static JTabbedPane tabbedPane;
    private static JTextArea[] messageArea;
    private static JScrollPane scrollPane;
    private static JProgressBar progressBar;
    private static JMenuBar menuBar;
    private final JMenu fileMenu;
    private final JMenu editMenu;
    private final JMenu aboutMenu;
    private final JMenu actionMenu;
    private final JMenu paraMenu;
    private final JMenu lookUpMenu;
    private final JMenuItem exitMenuItem;
    private final JMenuItem saveMenuItem;
    private final JMenuItem dirMenuItem;
    private final JMenuItem copyItem;
    private final JMenuItem findItem;
    private final JMenuItem selectAllItem;
    private final JMenuItem clearItem;
    private final JMenuItem aboutMenuItem;
    private final JMenuItem scopeMenuItem;
    private final JMenuItem communityMenuItem;
    private final JMenuItem sourceMenuItem;
    private final JMenuItem taskMenuItem;
    private final JMenuItem paraMenuItem;
    private final JMenuItem lookUpMenuItem;
    private final JTextArea aboutText;
    private final JComboBox sourceBox;
    private final JComboBox communityBox;
    private final JComboBox taskBox;
    private final JTextField startDay;
    private final JTextField startMonth;
    private final JTextField startYear;
    private final JTextField endDay;
    private final JTextField endMonth;
    private final JTextField endYear;
    private final JTextField timeLimit;
    private final JTextField lookUpQuery;
    private final JTextField lookUpAnswer;
    private static JTextField coreNumber;
    private final ButtonGroup retrieveGroup;
    private static JRadioButton middleRB;
    private static JRadioButton lowRB;
    private static JRadioButton allRB;
    private static JRadioButton highRB;
    private final JCheckBox allDateBx;
    private static JCheckBox showLinkBx;
    private static JCheckBox showAssocBx;
    private static JCheckBox showAssocCountBx;
    private static JCheckBox showInfoTBx;
    private static JCheckBox showCountBx;
    private static JCheckBox showTopicsBx;
    private static JCheckBox showNoLabelBx;
    private static JCheckBox showFacetBx;
    private static JCheckBox showSimBx;
    private static JCheckBox baseLineBx;
    private static JLabel progressLabel;
    private final JLabel communityLabel;
    private final JLabel sourceLabel;
    private final JLabel sourceLabel2;
    private final JLabel taskLabel;
    private final JLabel taskLabel2;
    private final JLabel startLabel;
    private final JLabel endLabel;
    private final JLabel limitLabel;
    private final JLabel allDatesLabel;
    private final JLabel showInfoTLabel;
    private final JLabel showCountLabel;
    private final JLabel showAssocLabel;
    private final JLabel showAssocCountLabel;
    private final JLabel showLinksLabel;
    private final JLabel showTopicsLabel;
    private final JLabel showNoLabelLabel;
    private final JLabel showFacetLabel;
    private final JLabel showSimLabel;
    private final JLabel retrieveLabel;
    private final JLabel retrievePanelLabel;
    private final JLabel queryLabel;
    private final JLabel answerLabel;
    private final JLabel coreLabel;
    private final JLabel baseLineLbl;
    private final JButton cancelBtn;
    private final JButton startBtn;
    private final JButton confirmScopeBtn;
    private final JButton confirmCommunityBtn;
    private final JButton confirmSourceBtn;
    private final JButton confirmTaskBtn;
    private final JButton confirmParaBtn;
    private final JButton confirmLookUpBtn;
    private final JButton vertexLookUpBtn;
    private final JButton docNameLookUpBtn;
    private final JButton docIdLookUpBtn;
    private final JButton orgDocLookUpBtn;
    private final JButton allOrgNamesBtn;
    private final JButton labelLookUpBtn;
    private final JButton clearBtn;
    private final JButton copyBtn;
    private static JButton removeBtn;
    private final JFileChooser fc;
    private final JPopupMenu cutpasteMenu = new JPopupMenu();
    private final JMenuItem cutMenuItem = new JMenuItem("Cut");
    private final JMenuItem copyMenuItem = new JMenuItem("Copy");
    private final JMenuItem pasteMenuItem = new JMenuItem("Paste");
    
    
}
