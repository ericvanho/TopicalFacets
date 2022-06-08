package ericvh.TDT;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MimeTypes;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;

/**
 * Class TikaParser uses Tika to detect the document type and to parse its content.
 *
 * @author Eric Van Horenbeeck Created on 17 maart 2022
 */
public class TikaParser
{
    // The Apache Tika dependency.
    private final Tika tika;
    private final GraphTime graphDate = new GraphTime();
    private String docDate = "";
    private String docSource = "";
    private String source ="";
    private static String filePath= "";
    private static int docCounter;
    private String docNo ="";
    private String suffix ="";
    MimeTypes mimeRegistry = TikaConfig.getDefaultConfig().getMimeRepository();
    /**
     * Constructor
     */
    public TikaParser()
    {
        tika = new Tika();
    }

    /**
     * parse: Parsing method called by the ApplicationManager.
     *
     * @param path : the filepath (String)
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     * @throws org.apache.tika.exception.TikaException
     */
    public void parse(String path) throws IOException, SAXException, TikaException
    {
        filePath = path;
        source = getSource().trim();
        String text = tika.parseToString(new File(filePath));
        if (text.length() < 2 )
        {
            text = "NO_TEXT";
            suffix = "emp";
        } // The suffix for a normal tokenized files is *.tok.
        else
        {
            suffix = "tok";
        }
        String fileName = getDate() + "_" + source + getdocNo() + "." + suffix;
        WorkFiles workFile = new WorkFiles(text, fileName);
        workFile.extract();
        docNo = "";
    }
    
    /**
     * getDate: Getter returns the document date. If no date is found in the text,
     * the last modified file date is returned, when that fails it's today's date.
     *
     * @return document date as String
     */
    private String getDate()
    {
        if (docDate.equals(""))
        {
            docDate = graphDate.getModifiedDate(filePath);
            if (docDate.equals(""))
            {
                docDate = graphDate.getDateNow();
            }
        }
        return docDate;
    }

    /**
     * getSource: Getter of the source of this file. Sets the filename when no source
     * was found in the text and no shortname is available.
     *
     * @return sourcename of the document (String)
     */
    private String getSource()
    {
        String shortName = ApplicationManager.getShortName();
        String longName = ApplicationManager.getLongName();
        if (docSource.equals(""))
        {
            // Gets the file name if no source is found in text.
            if (shortName.hashCode() == 1088)
            {
                docSource = filePath.substring(filePath.lastIndexOf("/") + 1,
                        filePath.lastIndexOf("."));
            } // Uses the shortname if available when no source is found.
            else
            {
                docSource = shortName;
            }
        } else if (docSource.trim().equals(longName))
        {
            docSource = shortName;
        }
        return docSource;
    }

    /**
     * getdocNo: Getter of the document id, sets a default number when id is empty.
     *
     * @return document id as String.
     */
    private String getdocNo()
    {
        docCounter++;
        // Sequential number if no docNo is available in the document.
        if (docNo.equals(""))
        {
            docNo = String.valueOf(docCounter);
        }
        return docNo.trim();
    }
    
        /** getPath: Getter returns the full filepath being processed.
     * @return this filepath (String)
     */
    public static String getPath()
    {
        return filePath;
    }
}
