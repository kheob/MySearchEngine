import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

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
            Files.walk(collection).forEachOrdered(documentPath -> {
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
        System.out.println("Searching for query: '" + queryString + "' in [" + indexPath + "]...");

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

        // Do the search
        performSearch(searcher, queryVector, numberOfResults);
    }

    // Does the calculations and performs the search, printing out the results
    private void performSearch(Searcher searcher, List<Double> queryVector, int numberOfResults) {
        // Compute cosine similarity for each document and the query
        HashMap<String, List<Double>> documentVectors = searcher.getVectors();
        HashMap<String, Double> rankedResults = new HashMap<>();

        documentVectors.forEach((documentName, documentVector) -> {
            double cosineSim = searcher.computeCosineSimilarity(queryVector, documentVector);

            // Added to hashmap
            rankedResults.put(documentName, cosineSim);
        });

        // Sort the hashmap
        // Source :http://stackoverflow.com/a/19671853/6601606
        Map<String, Double> sortedResults =
                rankedResults.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));

        // Format the cosine sim
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);

        // Add sorted results to an array
        ArrayList<String> sortedResultsArray = new ArrayList<>();
        sortedResults.forEach((documentName, cosineSim) -> {
            // Only add if word exists in the document
            if (cosineSim > 0.0) {
                sortedResultsArray.add(documentName + "," + df.format(cosineSim));
            }
        });

        // Show message if not enough results found
        int resultsToPrint = numberOfResults;
        if (sortedResultsArray.size() < numberOfResults) {
            resultsToPrint = sortedResultsArray.size();
            if (sortedResultsArray.size() == 0) {
                System.out.println("Sorry, no results could be found matching your query. Please try again.");
                System.exit(0);
            } else {
                System.out.println("You requested " + numberOfResults + " results, but only " + sortedResultsArray.size() + " results were found matching your query:");
            }
        } else {
            System.out.println("\n===== Showing top " + numberOfResults + " results: =====");
        }

        // Print out top results
        for (int i = 0; i < resultsToPrint; i++) {
            System.out.println(sortedResultsArray.get(i));
        }

        // Ask the user if they wish to provide relevance feedback
        System.out.print("\nWould you like to perform relevance feedback for a better set of results? (y/n): ");
        Scanner scanner = new Scanner(System.in);
        char answer = scanner.next().charAt(0);
        if (answer == 'y') {
            // Add the document vectors to a map
            HashMap<String, List<Double>> resultVectors = new HashMap<>();
            int vectorsToAdd = resultsToPrint;
            sortedResults.forEach((documentName, cosineSim) -> {
                // Only add if word exists in the document
                if (cosineSim > 0.0) {
                    if (resultVectors.size() < vectorsToAdd) {
                        resultVectors.put(documentName, documentVectors.get(documentName));
                    }
                }
            });

            // Do relevance feedback
            List<Double> newQueryVector = searcher.performRelevanceFeedback(resultVectors, queryVector);

            // Perform a new search
            performSearch(searcher, newQueryVector, numberOfResults);
        } else {
            System.exit(0);
        }
    }
}