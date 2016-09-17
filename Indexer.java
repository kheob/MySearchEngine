import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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

    // Default constructor
    public Indexer(String stopwordsFilepath) {
        stopwords = this.readStopwords(stopwordsFilepath);
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

    /**
     * Accessors and Mutators
     */
    public String[] getStopwords() {
        return this.stopwords;
    }
}