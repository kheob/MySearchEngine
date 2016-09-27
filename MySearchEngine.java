import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Main class for the MySearchEngine program.
 *
 * Created by BaiChanKheo on 17/09/2016.
 */
public class MySearchEngine {

    // Properties
    Indexer indexer;

    // Default constructor
    public MySearchEngine() {

    }

    // Main method
    public static void main(String[] args) {
        MySearchEngine mySearchEngine = new MySearchEngine();

        // Check if any arguments given
        if (args.length == 0) {
            System.out.println("Error: No arguments given. Type 'java MySearchEngine -h' for help.");
            System.exit(1);
        }

        // Check arguments
        if (args[0].equals("index")) {
            // Check if right number of arguments given
            if (args.length == 4) {
                // Index the collection
                mySearchEngine.index(args[1], args[2], args[3]);
            } else {
                // Quit with message
                System.out.println("Error: Wrong arguments given. Type 'java MySearchEngine -h' for help.");
                System.exit(1);
            }
        } else if (args[0].equals("search")) {
            // Check if right number of arguments given
            if (args.length >= 4) {
                // Join the query string
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    sb.append(args[i] + " ");
                }

                // Parse number of results
                int numberOfResults = 0;
                try {
                    numberOfResults = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    System.out.println("Error: Incorrect number format for number of results to retrieve.");
                    System.exit(1);
                }

                // Perform search
                mySearchEngine.search(args[1], numberOfResults, sb.toString().trim());
            } else {
                System.out.println("Error: Wrong arguments given. Type 'java MySearchEngine -h' for help.");
                System.exit(1);
            }
        } else if (args[0].equals("-h")) {
            System.out.println("=== Indexing ===: \nUsage: 'java MySearchEngine index COLLECTION_DIR INDEX_DIR STOPWORDS.TXT_PATH'\n\n" +
                    "=== Searching ===: \nUsage: 'java MySearchEngine search INDEX_DIR NUMBER_OF_RESULTS KEYWORD1 [KEYWORD2 ... KEYWORDN]'\n\n" +
                    "=== Help ===: \nQuerying: To use multiple words as one term, either type the words as capital case (e.g. Monash University), \nor enclose the words with escaped single quotes (e.g. \\'monash university\\')");
        } else {
            System.out.println("Error: Command not found. Type 'java MySearchEngine -h' for help.");
        }
    }

    // Index a collection at the specified path
    private void index(String collectionPath, String indexPath, String stopwordsPath) {
        // Get Indexer
        Indexer indexer = new Indexer(stopwordsPath);

        // Go through the collection path
        Path collection = Paths.get(collectionPath);

        // Get number of files in the collection
        int tempNumberOfDocuments = 0;
        try {
            tempNumberOfDocuments = new File(collectionPath).list().length;
        } catch (Exception e) {
        }

        int numberOfDocuments = tempNumberOfDocuments;

        // Exit the system if no documents found
        if (numberOfDocuments == 0) {
            System.out.println("Error: Collection directory doesn't contain any documents.");
            System.exit(1);
        }

        // Set up index file path
        String indexFile = indexPath + "/index.txt";

        // Create folder if doesn't exist
        if (!Files.exists(Paths.get(indexPath))) {
            try {
                Files.createDirectory(Paths.get(indexPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create file if doesn't exist
        if (!Files.exists(Paths.get(indexFile))) {
            try {
                Files.createFile(Paths.get(indexFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Empty the file
        try {
            Files.write(Paths.get(indexFile), "".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Log information to user
        System.out.println("Indexing files in directory [" + collectionPath + "] to [" + indexFile + "]");
        System.out.println("This may take a while...");

        // Walk through files at a path
        // Adapted from: http://stackoverflow.com/a/1846349/6601606
        try {
            Files.walk(collection).forEach(documentPath -> {
                if (Files.isRegularFile(documentPath)) {
                    // Tokenise a document
                    ArrayList<String> tokens = indexer.tokeniseDocument(documentPath);

                    // Index a document
                    // Removes commas from file names
                    indexer.indexDocument(documentPath.getFileName().toString().replace(",", ""), tokens);
                }
            });

            // Write index to file
            try {
                // Write an entry to the index file for each term
                indexer.getIndex().forEach((term, posting) -> {
                    // Create a string for each document listing
                    StringBuilder sb = new StringBuilder();
                    posting.forEach((document, occurrences) -> {
                        sb.append(document + "," + occurrences + ",");
                    });

                    // Compute IDF for the term
                    double idf = indexer.computeIDF(posting, numberOfDocuments);

                    // Round to 3 decimal places
                    // Source: http://stackoverflow.com/a/153785/6601606
                    DecimalFormat df = new DecimalFormat("#.###");
                    df.setRoundingMode(RoundingMode.CEILING);

                    try {
                        Files.write(Paths.get(indexFile), (term + "," + sb.toString() + df.format(idf) + "\n").getBytes(), StandardOpenOption.APPEND);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                System.out.println("Indexing complete! You can view the inverted file at: [" + indexFile + "]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Error: Collection directory doesn't contain any documents.");
            System.exit(1);
        }
    }

    // Search an index at the specified location with a query
    private void search(String indexPath, int numberOfResults, String queryString) {
        // Read the index
        List<String> indexRows = new ArrayList<>();
        try {
            indexRows = Files.readAllLines(Paths.get(indexPath + "/index.txt"));
        } catch (Exception e) {
            System.out.println("Error: Index doesn't exist at the specified location.");
            System.exit(1);
        }

        // Create new searcher object with the index
        Searcher searcher = new Searcher(indexRows);

        // Get new indexer object so we can pre-process the query string
        Indexer indexer = new Indexer();

        // Tokenise the query
        ArrayList<String> queryTokens = indexer.tokeniseQuery(queryString);

        // Create a query vector
        List<Double> queryVector = searcher.createQueryVector(queryTokens);

        System.out.println(queryVector.toString());
    }
}