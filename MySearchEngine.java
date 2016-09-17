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


    }
}