package ericvh.TDT;

import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Class Tokenizer looks for word boundaries and non-string tokens. Returns a map
 * with the alphanumeric token and a list of positions in the text of that token.
 * @author Eric Van Horenbeeck
 * Created on 26 mei 2004, 12:57
 */
public class Tokenizer
{
    // Loads the locale for this file. Is used by the breakIterator.
    private final Locale currentLocale = ApplicationManager.getThisLocale();
    // A constant representing tokens of the type 'Integer'.
    private final int DIGIT_TOKEN = 1;
    // A constant representing operator and grouping tokens e.g.: (+,-).
    private final int OPERATOR_TOKEN = 2;
    // A constant representing tokens of the type 'Separator' e.g.: */.
    private final int SEPARATOR_TOKEN = 3;
    private final char[] digits = {'1','2','3','4','5','6','7','8','9','0'};
    private final char[] operators =
    {'*','+','-', '<', '>', '/','=', '%', '^','{','(',')','}','[',']','$','�','�'};
    // Separators include blank and tab
    private final char[] separators = {',',':','#','\\','.','"','!','?',' ',' '};
    private final int[] nonStringTypes = {DIGIT_TOKEN, OPERATOR_TOKEN, 
        SEPARATOR_TOKEN};
    private final char[][] tokens = {digits, operators, separators};
    private String tokenType = "";
    private String shortType = "";
    private char currentChar = ' ';
    // Position in the text.
    private int mPosition;
    // The different tokens (types) in this text.
    private int mTotType;
    // The number of tokens (words) in this text.
    private int mTotToken;
    // The number of places a token position has to shift due to replacement of
    // one-letter tokens.
    private final int mPositionShift = 0;
    // Concatenates the one-letter tokens.
    private String tokenMaker = "";
    // Previous token delivered for control.
    private String previousToken = "";
    // Positions to shift a token in this text.
    private int shift = 0;
    // Counts shifted positions in this text.
    private int totalShift = 0;
    // A map with each token-type and its occurrences.
    private final Map mWordMap;
    // Include or exclude digits in the tokenized text. Set by the user in the GUI.
    private final boolean includeDigits = true;
    
    /** Constructor
     */
    public Tokenizer()
    {
        mWordMap = new HashMap();
    }
    
    /** extractTokens: Dispatching method called from WorkFiles and QueryInput.
     * @param text : a parsed textfile ready to be tokenized (String)
     * @param max : the maximum number of tokens to extract (int).
     * @return a map (HashMap) with all words and a stringBuffer with their 
     * positions in the text.
     * todo add end-of-line (EOF) token where appropriate. Use BreakIterator.getSentenceInstance
     */
    public Map extractTokens(String text, int max)
    {
        BreakIterator wordIterator = BreakIterator.getWordInstance(currentLocale);
        extractWords(text, wordIterator, max);
        return mWordMap;
    }
    
    /** extractWords: Central tokenizing method.
     * @param target : the text to analyze (String)
     * @param wordIterator : BreakIterator class instance to find word boundaries
     * @param max : the maximum number of tokens to extract (int).
     */
    private void extractWords(String target, BreakIterator wordIterator, int max)
    {
//        if ( target == null || (target != null && target.length() == 0))
//            throw new RuntimeException("Not a valid text in Tokenizer");
        
        wordIterator.setText(target);
        int start = wordIterator.first();
        int end = wordIterator.next();
        
        while (end != BreakIterator.DONE)
        {
            String word = target.substring(start,end);
            // To leave out all digits, replace next if-instruction with:
            // 'if(Character.isLetter(codePoint))'
            // Also add instructions on line 103-104 (see below).
            int codePoint = (int)word.charAt(0);
            if (Character.isLetterOrDigit(codePoint))
            {
                // The position in the text.
                mPosition++;
                checkTokenLength(word, max);
            }
            else
            {
                // Position in the text of non-string token.
                mPosition++;
                setNonStringToken(nonStringType(word.charAt(0)));
                // To add all digits as one token-type (#d) remove comment slashes 
                // on the next two lines and change line 90 (see above):
                
                // if(getNonStringToken(2).equals("#d"))
                // addToWordMap(getNonStringToken(2), max);
            }
            start = end;
            end = wordIterator.next();
        }
    }
    
    /** checkTokenLength: FSM checks the token length and constructs if necessary
     * a longer word with the consecutive one-letter tokens. 
     * The newly constructed word is passed to the next step. The positionof the
     * next tokens following the replaced one-letter tokens is shifted so as to
     * leave no gap in the position index. When the limit of tokens to be 
     * extracted is reached, a message is send to the QueryInput class and 
     * tokenizing the input stops.
     * @param word : token found in the text (String)
     * @param maxToken : the maximum number of tokens to extract (int).
     */
    private void checkTokenLength(String word, int maxToken)
    {
        if(mTotToken < maxToken || maxToken == 0)
        {
            if(!previousToken.equals("")) tokenMaker += previousToken;
            // Any one-letter token is temporarily saved in this buffer.
            if(word.length() == 1)
            {
                previousToken = word;
                shift++;
            }
            // If this is a longer word the one-letter words in the buffer are 
            // published first, then the current (long) word follows with its
            // position shifted to the left.
            else
            {
                if(tokenMaker.length() > 0)
                {
                    totalShift += shift;
                    addToWordMap(tokenMaker, mPosition - totalShift);
                    // Only published strings and digits are counted.
                    mTotToken++;
                    totalShift -= 2;
                    previousToken = "";
                    tokenMaker = "";
                    shift = 0;
                }
                addToWordMap(word, mPosition - totalShift);
                // Only published strings and digits are counted.
                mTotToken++;
            }
        }
        else QueryInput.getLimitMessage();
    }
    
    /** addToWordMap: Constructs a map with a token as key and a string of 
     * positions of this token as value.
     * @param word : token found in the text (String)
     * @param index : position of the word occurrence in the text (int)
     */
    private void addToWordMap(String word, int index)
    {
        String type = word.toLowerCase();
        String position = String.valueOf(index);
        // Token already in the list: adds its position only.
        if(mWordMap.containsKey(type))
        {
            // List with positions in this text so far.
            Object positionList = mWordMap.get(type);
            StringBuffer posBuffer1 = new StringBuffer(positionList.toString());
            posBuffer1.append(", ").append(position);
            mWordMap.put(type, posBuffer1);
        }
        else // New token found: adds token-type and its position in the text.
        {
            StringBuffer posBuffer2 = new StringBuffer(position);
            mWordMap.put(type, posBuffer2);
            mTotType++;
        }
    }
    
    /** getTokenTypeCount: Getter returns total number of tokens and types in this 
     * text.
     * @return array with the total tokens (int) and types (int).
     */
    public int[] getTokenTypeCount()
    {
        int[] typeToken =
        {mTotToken, mTotType};
        return typeToken;
    }
    
    /** nonStringType: A filter to classify non-alpha tokens into three groups:
     * digits, operators and  separators.
     * @param cr : a non-alpha character
     * @return the type of non-alpha token (int)
     */
    private int nonStringType(char cr)
    {
        int thisType = 0;
        for (int j = 0;j < nonStringTypes.length;j++)
        {
            for ( int i = 0;i < tokens[j].length;i++)
            {
                if (tokens[j][i] == cr)
                {
                    currentChar = cr;
                    thisType = nonStringTypes[j];
                }
            }
        }
        return thisType;
    }
    
    /** setNonStringToken: Setter for a non-alpha type token name.
     * @param type : type of non-alpha token found (int)
     */
    private void setNonStringToken(int type)
    {
        switch (type)
        {
            case DIGIT_TOKEN -> {
                tokenType = "digit" ;
                shortType = "#d";
            }
            case OPERATOR_TOKEN -> {
                tokenType = "operator" ;
                shortType = "#o";
            }
            case SEPARATOR_TOKEN -> {
                tokenType = "separator" ;
                shortType = "#s";
            }
            default -> {
                tokenType = "undefined" ;
                shortType = "#u";
            }
        }
    }
    
    /** getNonStringToken: Getter for a non-alpha type token.
     * @param arg : non-alpha token indicator (int)
     * @return name of the token (String)
     */
    private String getNonStringToken(int arg)
    {
        String type;
        switch (arg) {
            case 1 -> {
                System.out.println("tokenType: " + tokenType);
                type = tokenType;
            }
            case 2 -> type = shortType;
            default -> type = "#u";
        }
        return type;
    }
    
}
