
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that handles all of the indexing functionality.
 *
 * Stopwords source: http://www.ranks.nl/stopwords
 *
 * Created by BaiChanKheo on 17/09/2016.
 */
public class Indexer {

    // Properties
    private String[] stopwords;
    Stemmer stemmer; // Porter Stemmer (Source: https://tartarus.org/martin/PorterStemmer/)

    // Default constructor
    public Indexer(String stopwordsFilepath) {
        stopwords = this.readStopwords(stopwordsFilepath);
        Stemmer stemmer = new Stemmer();
    }

    // Reads a file containing stopwords and stores them in a property
    private String[] readStopwords(String filepath) {
        // Get filepath
        Path path = Paths.get(filepath);

        // Array to hold each stopword
        ArrayList<String> words = new ArrayList<>();

        // Read file at that path
        try {
            Files.readAllLines(path).forEach(word -> {
                // Add each stopword
                words.add(word);
            });
        } catch (IOException e) {
            System.out.println("No stopwords list found.");
            System.exit(1);
        }

        // Cast to String array and return
        String[] wordsArray = new String[words.size()];
        wordsArray = words.toArray(wordsArray);
        return wordsArray;
    }

    // Tokenises a document and returns an arraylist of tokens
    public ArrayList<String> tokeniseDocument(Path path) {
        ArrayList<String> tokens;
        StringBuilder sb = new StringBuilder();

        // Read the file
        try {
            Files.readAllLines(path).forEach(line -> {
                // Check if line ends with hyphen
                if (line.endsWith("-")) {
                    // Remove the hyphen
                    line = line.substring(0, line.length() - 1);

                    // The next line will not have a space.
                    sb.append(line);
                } else {
                    // Add a space between line breaks
                    sb.append(line + " ");
                }
            });
        } catch (IOException e) {
            System.out.println("Error reading document.");
        }

        // Tokenise the string
        String documentString = sb.toString();
        tokens = this.tokeniseString(documentString);

        // Normalise the tokens
        tokens = this.normaliseTokens(tokens);

        // Remove commas
        tokens = this.removeCommas(tokens);

        // Remove the stopwords
        this.removeStopwords(tokens);

        // Remove empty tokens that resulted from normalisation
        this.removeEmptyTokens(tokens);

        // TODO: Stem

        return tokens;
    }

    // Find tokens with consecutive capitals and within quotes, emails, urls, and IPs
    // Sources: http://stackoverflow.com/a/4113082/6601606
    // http://stackoverflow.com/a/16746437/6601606
    // http://stackoverflow.com/a/16053961/6601606
    // http://stackoverflow.com/a/1500501/6601606
    private ArrayList<String> tokeniseString(String string) {
        ArrayList<String> tokens = new ArrayList<>();

        // Regex that tokenises based on the rules of the assignment
        String regex = "(https?:\\/\\/[^\\s]+)|((www)?[^\\s]+\\.[^\\s]+)|(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])|([A-Z][a-zA-Z0-9-]*)([\\s][A-Z][a-zA-Z0-9-]*)+|(?:^|\\s)'([^']*?)'(?:$|\\s)|[^\\s{.,:;”’()?!}]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);

        // Add all terms to the array
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        ArrayList<String> processedTokens = new ArrayList<>();

        // Need to remove quotes, commas, etc. leading and trailing
        for (String token : tokens) {
            token = token.trim();

            // Remove leading punctuation
            while (token.startsWith(".") || token.startsWith(",") || token.startsWith("'") || token.startsWith("\"") || token.startsWith("_") || token.startsWith("[")) {
                token = token.substring(1);
            }

            // Remove trailing punctuation
            while (token.endsWith(".") || token.endsWith(",") || token.endsWith("'") || token.endsWith("\"") || token.endsWith("_") || token.endsWith("]")) {
                token = token.substring(0, token.length() - 1);
            }

            // Add to new array
            processedTokens.add(token);
        }

        return processedTokens;
    }

    // Normalise an arraylist of tokens
    private ArrayList<String> normaliseTokens(ArrayList<String> tokens) {
        ArrayList<String> normalisedTokens = new ArrayList<>();

        // Normalise to lower case and add
        for (String token : tokens) {
            token = token.toLowerCase();
            normalisedTokens.add(token);
        }

        return normalisedTokens;
    }

    // Remove stop words
    private void removeStopwords(ArrayList<String> tokens) {
        Iterator<String> it = tokens.iterator();
        List<String> stopwords = Arrays.asList(this.stopwords);
        while (it.hasNext()) {
            if (stopwords.contains(it.next())) {
                it.remove();
            }
        }
    }

    // Remove empty tokens from a provided list
    private void removeEmptyTokens(ArrayList<String> tokens) {
        Iterator<String> it = tokens.iterator();
        while (it.hasNext()) {
            if (it.next().equals("")) {
                it.remove();
            }
        }
    }

    // Remove commas from each token in an arraylist as they are used as delimiters
    private ArrayList<String> removeCommas(ArrayList<String> tokens) {
        ArrayList<String> newTokens = new ArrayList<>();
        for (String s : tokens) {
            if (s.contains(",")) {
                s = s.replace(",", "");
            }
            newTokens.add(s);
        }

        return newTokens;
    }

    // Stems an arraylist of tokens
    // TODO: Unfinished
    private ArrayList<String> stemTokens(ArrayList<String> tokens) {
        ArrayList<String> stemmedTokens = new ArrayList<>();
        for (String token : tokens) {
            stemmedTokens.add(token);
        }

        return stemmedTokens;
    }

    /**
     * Accessors and Mutators
     */
    public String[] getStopwords() {
        return this.stopwords;
    }
}