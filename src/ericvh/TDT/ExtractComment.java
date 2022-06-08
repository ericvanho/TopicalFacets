package ericvh.TDT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Class ExtractComment extracts all lines with comment (/* or //)out of the TDT program files and
 * saves them to disk as 'TDTComments.txt'.
 * @author Eric Van Horenbeeck
 * Created on 30 januari 2005, 13:19
 */
public class ExtractComment
{

    private static final String tdtFiles = "\\Doctoraat\\Computing\\NetBeans\\TDT"
            + "\\TopicalFacets\\src\\main\\java\\ericvh\\TDT";
    private static final String dir = "C:\\Users\\ASUS\\Documents\\";
    private static final String outFile = "\\Doctoraat\\Computing\\Processed\\"
            + "SourceComment\\TDTComments.txt";
    private static String comment = "";
    private static final String separator = System.getProperty("file.separator");

    /** main: Collects and prints all the comments found in *.java classes to a *.txt file.
     * @param args
     * @throws java.lang.Exception 
     */
    public static void main(String[] args) throws Exception
    {
        Calendar cal = Calendar.getInstance();
        String  year = String.valueOf( cal.get(Calendar.YEAR));           
        String  month = String.valueOf(cal.get(Calendar.MONTH));         
        String day =String.valueOf( cal.get(Calendar.DAY_OF_MONTH));   
        String today = day + "-" + month + "-" + year;

        // Creates a pattern to match comments.
        Pattern p = Pattern.compile("\\*+?\\s?/?(.*)|//\\s?(.*)");
        // Gets a channel for the source file.
        File programDir = new File(dir + tdtFiles);
        String[] selectedFiles = programDir.list();
        String allFileNames = "";
        int count = 0;
        // Process only files in the 'dir' directory.
        // Iterates over the selectedFiles.
        for (String selected : selectedFiles) {
            File thisFile = new File(selected);
            String fileName = thisFile.getName();
            File f = new File(programDir + separator + fileName);
            // Directories are skipped.
            if (!f.isDirectory())
            {
                try (FileInputStream fis = new FileInputStream(f)) {
                    FileChannel fc = fis.getChannel();
                    // Gets a CharBuffer from the source file.
                    ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
                    Charset cs = Charset.forName("8859_1");
                    CharsetDecoder cd = cs.newDecoder();
                    CharBuffer cb = cd.decode(bb);
                    // Files not written by me are skipped.
                    if (!fileName.equals("SwingWorker.java") && 
                            !fileName.equals("TableSorter.java") &&
                            !fileName.equals("ExtractComment.java") &&
                            !fileName.equals("ArrayListTransferHandler.java"))
                    {
                        System.out.println("Program: " + fileName);
                        allFileNames += "- " + fileName;
                        count++;
                        Matcher m = p.matcher(cb);
                        while (m.find())
                        {
                            comment += m.group();
                            comment += '\n';
                        }
                        comment += '\n';
                    }
                }
            }
        }
        comment = allFileNames + "\n" + count + " classes visited on: " + 
                today + "\n" + comment;
        File file = new File(dir + outFile);
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.print(comment);
        }
    }
}

