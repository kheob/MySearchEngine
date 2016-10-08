
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    private HashMap<String, HashMap<String, Integer>> index;
    private HashMap<String, String> localisation;

    // Default constructor
    public Indexer(String stopwordsFilepath) {
        stopwords = this.readStopwords(stopwordsFilepath);
        index = new HashMap<>();
        localisation = new HashMap<>();
        readLocalisation();
    }

    public Indexer() {
        stopwords = new String[0];
        index = new HashMap<>();
        localisation = new HashMap<>();
        readLocalisation();
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

    // Tokenises a query and returns an arraylist of tokens
    public ArrayList<String> tokeniseQuery(String query) {
        ArrayList<String> tokens;

        tokens = this.tokeniseString(query);

        // Localise
        tokens = this.localiseTokens(tokens);

        // Normalise the tokens
        tokens = this.normaliseTokens(tokens);

        // Remove commas
        tokens = this.removeCommas(tokens);

        // Remove the stopwords
        this.removeStopwords(tokens);

        // Remove empty tokens that resulted from normalisation
        this.removeEmptyTokens(tokens);

        // Stem the tokens
        tokens = this.stemTokens(tokens);

        return tokens;
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

        // Localise
        tokens = this.localiseTokens(tokens);

        // Normalise the tokens
        tokens = this.normaliseTokens(tokens);

        // Remove commas
        tokens = this.removeCommas(tokens);

        // Remove the stopwords
        this.removeStopwords(tokens);

        // Remove empty tokens that resulted from normalisation
        this.removeEmptyTokens(tokens);

        // Stem the tokens
        tokens = this.stemTokens(tokens);

        return tokens;
    }

    // Takes an arraylist of string and indexes it
    public void indexDocument(String documentName, ArrayList<String> documentTokens) {
        for (String token : documentTokens) {
            if (!this.index.containsKey(token)) {
                // Create a key with that token
                this.index.put(token, new HashMap<>());
            }

            // Get the HashMap associated with that term
            HashMap<String, Integer> term = this.index.get(token);

            // Check if term has a posting for the document
            if (term.containsKey(documentName)) {
                // Increase its occurrence by 1
                int occurrences = term.get(documentName);
                term.put(documentName, ++occurrences);
            } else {
                // Create a new posting for the term
                term.put(documentName, 1);
            }
        }
    }

    // Computes the IDF for a given term
    // IDF formula denominator +1 to stop divide by zero errors
    public double computeIDF(HashMap<String, Integer> termPostings, int documents) {
        // Document frequency is the number of postings
        int df = termPostings.size();

        return Math.log(((double) documents) / (df + 1));
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

    // Stems an arraylist of tokens using the Porter Stemmer
    // Porter Stemmer (Source: https://tartarus.org/martin/PorterStemmer/)
    private ArrayList<String> stemTokens(ArrayList<String> tokens) {
        ArrayList<String> stemmedTokens = new ArrayList<>();
        for (String token : tokens) {
            Stemmer stemmer = new Stemmer();
            // Add each character into the stemmer
            for (int i = 0; i < token.length(); i++) {
                stemmer.add(token.charAt(i));
            }

            // Stem the token
            stemmer.stem();

            // Retrieve the stemmed token
            String stemmedToken = stemmer.toString();

            stemmedTokens.add(stemmedToken);
        }

        return stemmedTokens;
    }

    // Reads the localisation file
    // Source of spelling differences: http://www.tysto.com/uk-us-spelling-list.html
    private void readLocalisation() {
        ArrayList<String> linesArray = new ArrayList<>();
        try {
            Files.readAllLines(Paths.get("localisation.txt")).forEach(line -> {
                linesArray.add(line);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Split into two arrays
        List<String> british = linesArray.subList(0, linesArray.size()/2);
        List<String> american = linesArray.subList(linesArray.size()/2, linesArray.size());

        // Put into hashmap
        for (int i = 0; i < british.size(); i++) {
            this.localisation.put(british.get(i), american.get(i));
        }
    }

    // Converts all British spellings to American spellings for better indexing
    private ArrayList<String> localiseTokens(ArrayList<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            // Check if token is a key
            if (this.localisation.containsKey(tokens.get(i))) {
                // Convert to American
                tokens.set(i, this.localisation.get(tokens.get(i)));
            }
        }

        return tokens;
    }

    /**
     * Accessors and Mutators
     */
    public String[] getStopwords() {
        return this.stopwords;
    }

    public HashMap<String, HashMap<String, Integer>> getIndex() {
        return index;
    }

    public void setIndex(HashMap<String, HashMap<String, Integer>> index) {
        this.index = index;
    }
}