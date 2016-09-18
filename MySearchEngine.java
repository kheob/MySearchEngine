import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

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
            // TODO: Show help
            System.out.println("No arguments given.");
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
                System.out.println("Wrong arguments given. Correct usage:\nMySearchEngine index COLLECTION_DIR INDEX_DIR STOPWORDS_DIR");
                System.exit(1);
            }
        } else if (args[0].equals("search")) {
            // TODO: Search

            System.out.println("Search");

        } else {
            // TODO: Error message
            System.out.println("Error");
        }
    }

    // Index a collection at the specified path
    private void index(String collectionPath, String indexPath, String stopwordsPath) {
        // Get Indexer
        Indexer indexer = new Indexer(stopwordsPath);

        // Go through the collection path
        Path collection = Paths.get(collectionPath);

        // Create file
        if (!Files.exists(Paths.get("test.txt"))) {
            try {
                Files.createFile(Paths.get("test.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Empty the file
        try {
            Files.write(Paths.get("test.txt"), "".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Walk through files at a path
        // Adapted from: http://stackoverflow.com/a/1846349/6601606
        try {
            Files.walk(collection).forEach(documentPath -> {
                if (Files.isRegularFile(documentPath)) {
                    // Tokenise a document
                    ArrayList<String> tokens = indexer.tokeniseDocument(documentPath);

                    // Write string to file
                    for (String token : tokens) {
                        try {
                            Files.write(Paths.get("test.txt"), (token + "\n").getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Collection directory doesn't contain any documents.");
            System.exit(1);
        }
    }
}