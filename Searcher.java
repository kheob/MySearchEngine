import java.util.*;

/**
 * Class that handles all the search functionality for the MySearchEngine program.
 *
 * Created by BaiChanKheo on 27/09/2016.
 */
public class Searcher {

    private List<String> invertedIndex;
    private HashMap<String, List<Double>> vectors;

    public Searcher(List<String> index) {
        this.invertedIndex = index;

        // Sort the index by alpha
        Collections.sort(this.invertedIndex);

        this.createDocumentVectors();
    }

    // Creates the document vectors with current index
    private void createDocumentVectors() {
        this.vectors = new HashMap<>();
        invertedIndex.forEach(term -> {
            // Get the substring containing the postings
            String postings = term.substring(term.indexOf(",") + 1, term.lastIndexOf(","));

            // Add document name to vectors if it isn't already there
            String[] splitPostings = postings.split(",");
            for (String s : splitPostings) {
                // Don't add if it's a number
                // Source: http://stackoverflow.com/a/10575676/6601606
                if (s.matches("[0-9]+")) {
                    continue;
                } else if (this.vectors.containsKey(s)) {
                    continue;
                } else {
                    this.vectors.put(s, new ArrayList<>());
                }
            }
        });

        // Search through the index for each term, and compute the tf.idf for each term in each document
        invertedIndex.forEach(term -> {
            // Check if a document contains that term
            vectors.forEach((document, weightings) -> {
                int tf = 0;

                // Get the tf if a document contains the term
                if (term.contains(document)) {
                    // Get the tf of the term in that document
                    // This finds the first occurrence of the document name in the posting, and then gets the next number (which is the tf)
                    String tfString = term.substring(term.indexOf(document) + document.length() + 1);

                    tf = Integer.parseInt(tfString.substring(0, tfString.indexOf(",")));
                }

                // Compute the tf.idf
                double idf = Double.parseDouble(term.substring(term.lastIndexOf(",") + 1, term.length()));
                double tfidf = tf * idf;

                // Insert into vector array
                this.vectors.get(document).add(tfidf);
            });
        });
    }

    // Create a query vector with the current index
    public List<Double> createQueryVector(ArrayList<String> queryTokens) {
        ArrayList<Double> queryVector = new ArrayList<>();

        // Create hashmap of query tokens and tf
        HashMap<String, Integer> queryTF = new HashMap<>();
        queryTokens.forEach(token -> {
            if (!queryTF.containsKey(token)) {
                queryTF.put(token, 1);
            } else {
                queryTF.put(token, queryTF.get(token) + 1);
            }
        });

        // Get collection vocabulary
        this.invertedIndex.forEach(line -> {
            String term = line.substring(0, line.indexOf(",")); // Gets the term from the index

            // Get term frequency
            int tf = 0;
            if (queryTF.containsKey(term)) {
                tf = queryTF.get(term);
            }

            // Get the idf
            double idf = Double.parseDouble(line.substring(line.lastIndexOf(",") + 1, line.length()));
            double tfidf = tf * idf;

            // Add to query vector
            queryVector.add(tfidf);
        });

        return queryVector;
    }

    // Compute cosine similarity for two given vectors
    public double computeCosineSimilarity(List<Double> query, List<Double> document) {
        double dotProduct = 0.0;
        double docLengthSquared = 0.0;
        double queryLengthSquared = 0.0;

        // Computations for each dimension
        for (int i = 0; i < query.size(); i++) {
            double w = document.get(i);
            double q = query.get(i);

            dotProduct += w * q;
            docLengthSquared += w * w;
            queryLengthSquared += q * q;
        }

        // Square root the sums to get the document and query lengths
        double docLength = Math.sqrt(docLengthSquared);
        double queryLength = Math.sqrt(queryLengthSquared);

        // Compute the cosine similarity
        double cosineSim = dotProduct / (docLength * queryLength);

        return cosineSim;
    }

    // Performs relevance feedback
    // Uses Rocchio's Algorithm
    // Weight of relevant is 0.5, weight is non relevant is 0.25
    public List<Double> performRelevanceFeedback(HashMap<String, List<Double>> resultVectors, List<Double> queryVector) {
        List<List<Double>> relevant = new ArrayList<>();
        List<List<Double>> nonRelevant = new ArrayList<>();

        // Loop through documents
        resultVectors.forEach((docName, vector) -> {
            System.out.print("Is the document '" + docName + "' relevant? (y/n): ");
            Scanner scanner = new Scanner(System.in);
            char answer = scanner.next().charAt(0);
            if (answer == 'y') {
                // Add to list of relevant vectors
                relevant.add(vector);
            } else {
                // Add to list of non relevant vectors
                nonRelevant.add(vector);
            }
        });

        // Compute the relevant and non relevant centroids
        List<Double> relevantCentroid = new ArrayList<>();
        List<Double> nonRelevantCentroid = new ArrayList<>();
        int vocabSize = queryVector.size();
        for (int i = 0; i < vocabSize; i++) {
            // Relevant
            double relevantSum = 0.0;
            for (List<Double> vector : relevant) {
                relevantSum += vector.get(i);
            }
            double relevantAverage = 0.0;
            if (relevant.size() > 0) {
                relevantAverage = relevantSum / relevant.size();
            }
            relevantCentroid.add(relevantAverage);

            // Non relevant
            double nonRelevantSum = 0.0;
            for (List<Double> vector : nonRelevant) {
                nonRelevantSum += vector.get(i);
            }
            double nonRelevantAverage = 0.0;
            if (nonRelevant.size() > 0) {
                nonRelevantAverage = nonRelevantSum / nonRelevant.size();
            }
            nonRelevantCentroid.add(nonRelevantAverage);
        }

        // Add relevant centroid to query vector
        List<Double> newQueryVector = new ArrayList<>();
        for (int i = 0; i < vocabSize; i++) {
            newQueryVector.add(queryVector.get(i) + (0.5 * relevantCentroid.get(i)) + (0.25 * nonRelevantCentroid.get(i)));
        }

        return newQueryVector;
    }

    /**
     * Getters and setters
     */
    public List<String> getInvertedIndex() {
        return invertedIndex;
    }

    public void setInvertedIndex(List<String> invertedIndex) {
        this.invertedIndex = invertedIndex;
    }

    public HashMap<String, List<Double>> getVectors() {
        return vectors;
    }

    public void setVectors(HashMap<String, List<Double>> vectors) {
        this.vectors = vectors;
    }
}
