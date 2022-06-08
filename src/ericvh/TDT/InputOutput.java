package ericvh.TDT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;


/** Class InputOutput takes in the preprocessed files from disk and converts them 
 * into arrays for further treatment by the GraphBuilder class. It also has 
 * methods to save the dataGraph and its associated files and to read them back.
 * @author  Eric Van Horenbeeck
 */
public class InputOutput implements Serializable
{
    private final String separator = System.getProperty("file.separator");
    private final String newline = "\n";
    // Regex patterns.
    private final Pattern stringP = Pattern.compile("\\s(.*?)\\s |(\\d)+");
    private final Pattern countP = Pattern.compile("\\,");
    // The highest frequency of a token in a textfile.
    private int mMaxFrequency = 0;
    // Instance of DocStats holds some simple statistics on a file.
    private DocStats docData;
    // Dictionary with the internal filename (Key) and the original filename (Value).
    public static TreeMap allOriginalFilenames = new TreeMap();
    
    /** Default Constructor called by the ApplicationManager.
     */
    public InputOutput()
    {  }
    
    /** makeTokenArray: Reads a tokenized textfile and converts it into 
     * an array (text_words). For each token the text_words array remembers 
     * the position in the text and the source-date collection key of that 
     * token (word). An instance of DocStats is made with the maximum frequency,
     * total tokens, total types and the filename of the document.
     * @param filePath : the full path to a preprocessed *.tok file (String)
     * @param docNr : the unique document identifier (int)
     * @return dataPackage (arrayList) with instances of TextToken, an object 
     * holding for every token a label-id, its position in the document and a 
     * source-date key of the collection it belongs to, the data for the
     * docTable and a collectionKey. GraphBuilder will distribute this information
     * to all the classes concerned.
     * @throws Exception not specified
     */
    public List makeTokenArray(String filePath, int docNr) throws Exception
    {
        // The path to the file to be processed.
        File fullFilePath = new File(filePath);
        // Array to hold all textTokens
        List text_tokens = new ArrayList();
        // Key to the collection this file belongs to.
        String collectionKey = setCollectionKey(filePath);
        // ArrayList as a container to transfer all document data back to
        // the GraphBuilder.
        List dataPackage = new ArrayList(3);
        //Warning if a file cannot be read or opened.
        if(!fullFilePath.canRead())
        {
            ApplicationManager.showText("The file " + fullFilePath.getName() +
                    " is not available,\ntrying to continue with next file...", 0);
        }
        else
        {
            try
            {
                FileReader fr = new FileReader(fullFilePath);
                try ( BufferedReader in = new BufferedReader(fr)) {
                    int currentPosition = 0;
                    String line = "";
                    String previous = "";
                    
                    // Reads a line and parses the token and its text positions 
                    // and adds to an array.
                    while((line = in.readLine()) != null)
                    {
                        previous = line;
                        String currentToken = "";
                        Matcher matcher = stringP.matcher(line); // Regex pattern.
                        while (matcher.find())
                        {
                            int s = matcher.start();
                            if(s == 0) currentToken = matcher.group().trim();
                            else currentPosition = Integer.parseInt(matcher.group());
                            if(currentPosition > 0 && !currentToken.equals(""))
                            {
                                TextToken currentTextToken = 
                                        new TextToken(currentToken,
                                        currentPosition, collectionKey);
                                text_tokens.add(currentTextToken);
                            }
                            currentPosition = 0;
                        }
                    }
                    // Parses the file statistics included in the last line 
                    // of the file.
                    if(line == null)
                    {
                        // Discards the non-string character.
                        previous = previous.substring(1);
                        String [] stats = Algorithms.getLineSplit(previous);
                        int maxFreq = Integer.parseInt(stats[0]);
                        int totTokens = Integer.parseInt(stats[1]);
                        int totTypes = Integer.parseInt(stats[2]);
                        // Puts the data in a list and adds the filename.
                        docData = new DocStats(maxFreq, totTokens, totTypes, 0, 
                                fullFilePath.getName());
                    }
                }
                // The text_tokens-list is sorted on the position of the tokens 
                // in the text.
                Collections.sort(text_tokens, (Object o1, Object o2) -> 
                        ((TextToken)o1).getPosition() - ((TextToken)o2)
                                .getPosition());
            }
            catch (FileNotFoundException fnfe)
            {
                ApplicationManager.showText("File " + fullFilePath 
                        + " not found", 0);
            }
        }
        dataPackage.add(text_tokens);
        dataPackage.add(docData);
        dataPackage.add(collectionKey);
        return dataPackage;
    }
    
    /** setCollectionKey: Constructs the key to a collection.
     * @param path : the full file path (String)
     * @return the collectionKey (String)
     */
    private String setCollectionKey(String path)
    {
        StringBuilder sourceDate = new StringBuilder();
        String tmpFile = path.substring(path.lastIndexOf(separator) + 1,
                path.lastIndexOf('.'));
        sourceDate.append(tmpFile.substring(0,8));
        for(int i = 9; i < tmpFile.length(); i++)
        {
            char cr = tmpFile.charAt(i);
            if (Character.isLetter(cr)) sourceDate.append(cr);
            else i = tmpFile.length();  // Early exit when ready.
        }
        return sourceDate.toString();
    }
    
    /** writeASCII: Writes file with vertices and arcs from the dataGraph to disk.
     * The file is first converted from 16-bit Unicode used in Java to the ASCII
     * code used by the Pajek graph rendering program.
     * program.
     * @param fileName : destination (String)
     * @param dataGraph : the file with all vertices and arcs (String)
     * @throws IOException input-output exception
     */
    public void writeASCII(String fileName, String dataGraph) throws IOException
    {
        File file = new File(fileName);
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            byte[] utf8Graph = dataGraph.getBytes("UTF8");
            String asciiGraph = new String(utf8Graph, "UTF8");
            out.print(asciiGraph);
        }
    }
    
    /** writeThisObject: Writes an Object (dataGraph, docTable or collectionTable)
     * to a disk file.
     * @param filePath : the path name (String)
     * @param obj : object to be saved on disk (Object)
     * @throws IOException
     */
    public void writeThisObject(String filePath, Object obj) throws IOException
    {
        try (FileOutputStream outStream = new FileOutputStream(filePath)) {
            ObjectOutputStream out = new ObjectOutputStream(outStream);
            out.writeObject(obj);
        }
    }
    
    /** readList: Reads a LinkedList from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the linked list from disk
     */
    public LinkedList readList(String filePath) throws Exception
    {
        LinkedList list;       
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            list = (LinkedList) in.readObject();
        }
        return list;
    }
    
    /** readSet: Reads a SortedSet from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the SortedSetfrom disk
     */
    public SortedSet readSet(String filePath) throws Exception
    {
        SortedSet set;      
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            set = (TreeSet) in.readObject();
        }
        return set;
    }
    
    /** readMap: Reads a HashMap from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the map from disk
     */
    public Map readMap(String filePath) throws Exception
    {
        Map map;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            map = (HashMap) in.readObject();
        }
        return map;
    }
    
     /** readTreeMap: Reads a TreeMap from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the map from disk
     */
    public TreeMap readTreeMap(String filePath) throws Exception
    {
        TreeMap map;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            map = (TreeMap) in.readObject();
        }
        return map;
    }
    
    /** readSortedMap: Reads a SortedMap from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the SortedMap from disk
     */
    public SortedMap readSortedMap(String filePath) throws Exception
    {
        SortedMap map;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            map = (TreeMap) in.readObject();
        }
        return map;
    }
    
    /** readLinkedHashMap: Reads a LinkedHashMap from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the map from disk
     */
    public LinkedHashMap readLinkedHashMap(String filePath) throws Exception
    {
        LinkedHashMap map;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            map = (LinkedHashMap) in.readObject();
        }
        return map;
    }
    
    /** readTextGraph: Reads a TextGraph from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the graph from disk
     */
    public TextGraph readTextGraph(String filePath) throws Exception
    {
        TextGraph tg;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            tg = (TextGraph) in.readObject();
        }
        return tg;
    }
    
    /** readLTable: Reads the LabelTable from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the labelTable from disk
     */
    public LabelTable readLTable(String filePath) throws Exception
    {
        LabelTable lt;
        try ( FileInputStream inStream = new FileInputStream(filePath))
        {
            ObjectInputStream in = new ObjectInputStream(inStream);
            lt = (LabelTable) in.readObject();
        }
        return lt;
    }
    
    /** readVTable: Reads a VerticesTable from a file on disk.
     * @param filePath : the full path to the file (String)
     * @throws Exception not specified
     * @return the verticesTable from disk
     */
    public VerticesTable readVTable(String filePath) throws Exception
    {
        VerticesTable vt;
        try (FileInputStream inStream = new FileInputStream(filePath)) 
        {
            ObjectInputStream in = new ObjectInputStream(inStream);
            vt = (VerticesTable) in.readObject();
        }
        return vt;
    }
    
    /**
     * readDTable: Reads the DocTable from disk.
     * @param filePath : the full path to the files (String)
     * @throws Exception not specified
     * @return DocTable from disk
     */
    public DocTable readDTable(String filePath) throws Exception
    {
        DocTable dt;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            dt = (DocTable) in.readObject();
        }
        return dt;
    }
    
    /** readCTable: Reads the CollectionTable from disk.
     * @param filePath : the full path to the files (String)
     * @throws Exception not specified
     * @return CollectionTable from disk
     */
    public CollectionTable readCTable(String filePath) throws Exception
    {
        CollectionTable ct;
        try (FileInputStream inStream = new FileInputStream(filePath)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            ct = (CollectionTable) in.readObject();
        }
        return ct;
    }
    
    /** readStringFile: Reads a String file from disk
     * @param filePath : the full path to the file (String)
     * @throws java.io.IOException
     * @return a stringfile from disk
     */
    public String readStringFile(String filePath) throws IOException
    {
        String inFile = "";
        String line;
        FileReader fr = new FileReader(filePath);
        try (BufferedReader in = new BufferedReader(fr)) {
            while((line = in.readLine()) != null) inFile += line + '\n';
        }
        return inFile;
    }
    
    /** writeString: Writes a String to a file on disk.
     * @param filePath : the full path to a file (String)
     * @param text : as a String
     * @throws java.io.IOException 
     */
    public void writeString(String filePath, String text) throws IOException
    {
        File file = new File(filePath);
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.print(text);
        }
    }
    
    /** writeTokenizedText: Writes the parsed and tokenized text file with its 
     * indices (positions) to disk. The last record gives maximum token frequency,
     * total tokens and total token-types. Called by the WorkFiles Class at 
     * the end of the Preprocessing Task.
     * @param outMap : the text file after preprocessing (Map)
     * @param outFile : destination filename (String)
     * @param totTokens : total number of tokens in this document (int)
     * @param totTypes : tot number of token-types in this document (int)
     * @throws IOException
     */
    public void writeTokenizedText(Map outMap, String outFile, int totTokens, 
            int totTypes) throws IOException
    {
        String fileName = ApplicationManager.getDataPath() + separator + outFile;
        String fullPath = TikaParser.getPath();
        int index = fullPath.lastIndexOf("\\");
        
        // Dictionary with filename used in this application (Key) and the 
        // orginal filename (Value)       
        String orgFilename = fullPath.substring(index + 1);
        allOriginalFilenames.put(FilenameUtils.removeExtension(outFile),orgFilename);
        
      //  File file = new File(fileName);
        if(!outMap.isEmpty())
        {
            try ( PrintWriter out = new PrintWriter(fileName)) {
                List types = new ArrayList(outMap.keySet());
                ListIterator iter = types.listIterator();
                while (iter.hasNext()) {
                    Object elem = iter.next();
                    String occurr = outMap.get(elem).toString();
                    // Looks for the maximum token-frequency.
                    countFrequency(occurr);
                    out.println(" " + elem + "  " + occurr + newline);
                }
                // The last line holds the file statistics. It starts with a 
                // non-alphabetic character ("]") to put this data appart from
                // the rest of the file.
                out.println("]" + mMaxFrequency + "," + totTokens + "," + totTypes);
                out.close();
                mMaxFrequency = 0;
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace(System.err);
                ApplicationManager.showText("Saving the file to disk did not succeed", 0);             
            }
            catch(Exception e)
            {
                e.printStackTrace(System.err);
                ApplicationManager.showText("Non-IO exception while saving a file", 0); 
            }
        }
        else ApplicationManager.showText("Trying to save an empty file", 0);
    }
    
    /** countFrequency: Searches for the highest frequency of a token in a text.
     * This is done here in the writeTokenizedText method because the tokenized
     * file is scanned from record to record while saving it to disk.
     * @param s : String with the occurrences of one token
     */
    private void countFrequency(String s)
    {
        int freqCount = 1;
        // Gets the matcher object ',' comma.
        Matcher m = countP.matcher(s);
        while(m.find()) freqCount++;
        if (freqCount > mMaxFrequency) mMaxFrequency = freqCount;
    }
}

