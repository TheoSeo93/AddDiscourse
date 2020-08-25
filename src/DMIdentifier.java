
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java Translation from Perl script of Emily Pitler and Ani Nenkova's addDiscourse.pl
 * @Author Youngho Seo
 *
 * Explicit Discourse Connectives Tagger - December 14, 2009
 * Emily Pitler and Ani Nenkova.  Using Syntax to Disambiguate Explicit
 * Discourse Connectives in Text.  Proceedings of the ACL-IJCNLP 2009
 * Conference Short Papers, pages 13-16.
 */

public class DMIdentifier {

    private static DMIdentifier singleton = null;
    private Map<String, String> longDistConnectives;
    private Map<String, Integer> connectives;
    private Map<String, Map<String, String>> labelWeights;

    private DMIdentifier() throws IOException {

        StringBuilder fin = load("connectives.txt");
        String[] split = fin.toString().split("\n");
        longDistConnectives = new HashMap<>();
        connectives = new HashMap<>();

        for(String connective : split){
            if(connective.contains(".")) {
                String[] longConnective = connective.split("..");
                longDistConnectives.put(longConnective[0].trim(), longConnective[1].trim());
            }
            connectives.put(connective.trim(), 1);
        }

        Map<String, Integer> connectiveTexts = new HashMap<>();
        fin = load("connectiveTexts.txt");
        split = fin.toString().split("\n");

        for(String c : split)
            connectiveTexts.put(c.trim(), 1);

        labelWeights = new HashMap<>();
        fin = load("connectives.info");
        split = fin.toString().split("\n");
        String currentLabel = "";
        Pattern pattern = Pattern.compile("FEATURES FOR CLASS (.*)");
        for(String line: split){
            Matcher matcher = pattern.matcher(line.trim());
            if(matcher.find())
                currentLabel = matcher.group(1);

            String[] featureWeight = line.split("\\s+");
            Map<String, String> weightByFeature = new HashMap<>();
            String feature = featureWeight[0];
            String weight = featureWeight[1];
            weightByFeature.put(feature, weight);
            labelWeights.put(currentLabel, weightByFeature);

        }

    }

    public static DMIdentifier getInstance() throws IOException {
        if (singleton == null)
            singleton = new DMIdentifier();
        return singleton;
    }

    public static StringBuilder load(final String file) throws IOException {
        final InputStream stream = DMIdentifier.class.getClassLoader().getResourceAsStream(file);
        final StringBuilder sb = new StringBuilder();
        final BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String temp;
        while ((temp = r.readLine()) != null) {
            sb.append(temp);
            sb.append(' ');
        }
        r.close();
        return sb;
    }


//    public String analyze(String inputText){
//        String newParsedText = inputText;
//        int idInFile = 0;
//        // Check for everything in connectives
//
//
//    }
}