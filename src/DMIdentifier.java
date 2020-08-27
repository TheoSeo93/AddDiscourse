import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java Translation from Perl script of Emily Pitler and Ani Nenkova's addDiscourse.pl
 *
 * @Author Youngho Seo
 * <p>
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

        for (String connective : split) {
            if (connective.contains(".")) {
                String[] longConnective = connective.split("\\.\\.");
                longDistConnectives.put(longConnective[0].trim(), longConnective[1].trim());
                connectives.put(longConnective[0].trim(), 1);
                connectives.put(longConnective[1].trim(), 1);
            } else
                connectives.put(connective.trim(), 1);


        }

        Map<String, Integer> connectiveTexts = new HashMap<>();
        fin = load("connectiveTexts.txt");
        split = fin.toString().split("\n");

        for (String c : split)
            connectiveTexts.put(c.trim(), 1);

        labelWeights = new HashMap<>();
        fin = load("connectives.info");
        split = fin.toString().split("\n");
        String currentLabel = "";
        Pattern pattern = Pattern.compile("FEATURES FOR CLASS (.*)");
        for (String line : split) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find())
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
            sb.append('\n');
        }
        r.close();
        return sb;
    }


    public String analyze(String inputText) {
        String newParsedText = inputText;
        int idInFile = 0;
        // Check for everything in connectives
        List<String> sorted = new ArrayList<>(connectives.keySet());
        sorted.sort(Comparator.comparingInt(String::length));
        for (int idx = sorted.size() - 1; idx >= 0; idx--) {
            int overallId = 0;
            String[] conn = sorted.get(idx).split("\\s");
            String regex = "";
            regex = "\\b" + String.join("\\)*\\s+(\\s|\\(\\S*|\\))*", conn) + "\\)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(inputText);
            while (matcher.find()) {
                String match = matcher.group();
                int pos = matcher.end();
                String selfCat = "S";
                String parentCat = "P";
                String leftSibCat = "L";
                String rightSibCat = "R";
                String rightSibWVP = "RSVPN";
                String rightSibWTrace = "RSTN";
                int parentEndingSelf = 0;
                String parsedText2 = inputText;
                //number of parenthesis around self is min of (open before, close after)
                //find afterCloseBeforeOpen
                int i = pos;
                int afterCloseBeforeOpen = 0;
                int len = parsedText2.length();

                while (i < len && parsedText2.charAt(i) != '(') {
                    if (parsedText2.charAt(i) == ')')
                        afterCloseBeforeOpen += 1;
                    i += 1;
                }
                //find prevOpenBeforeClose
                //Two cases--normal case where is a constituent "but", "on the other hand"
                int diff = 0;
                for (int j = 0; j < match.length(); j++) {
                    if (match.charAt(j) == '(')
                        diff += 1;
                    else if (match.charAt(j) == ')')
                        diff -= 1;
                }

                i = pos - match.length();

                if (diff == 1 || conn.length == 1) {
                    int prevOpenBeforeClose = diff;
                    while (prevOpenBeforeClose < afterCloseBeforeOpen && parsedText2.charAt(i) != ')') {
                        if (parsedText2.charAt(i) == '(')
                            prevOpenBeforeClose += 1;

                        i -= 1;
                    }
                    parentEndingSelf = prevOpenBeforeClose;
                } else {
                    //pathological case where not a constituent ``as soon as'' (take last part as the head)
                    i = pos - 1;
                    int prevOpenBeforeClose = 0;
                    while (prevOpenBeforeClose < afterCloseBeforeOpen) {
                        if (parsedText2.charAt(i) == '(')
                            prevOpenBeforeClose += 1;
                        if (parsedText2.charAt(i) == ')')
                            prevOpenBeforeClose -= 1;
                        i -= 1;
                    }
                    parentEndingSelf = afterCloseBeforeOpen;
                }

                String rest = parsedText2.substring(i);
                Pattern p = Pattern.compile("\\(+(\\S+)\\s|s\\^[^(]*");
                Matcher m = p.matcher(rest);
                if (m.find()) {
                    selfCat = "S" + m.group(1);
                    int posSelf = i + 1;
                    int nOpenParens = 0;
                    i = posSelf - 1;
                    while (i >= 0 && nOpenParens < 1) {
                        char c = parsedText2.charAt(i);
                        if (c == '(') {
                            nOpenParens += 1;
                            if (nOpenParens == 0 && leftSibCat.equals("L")) {
                                Matcher m2 = p.matcher(parsedText2.substring(i));
                                if (m2.find()) {
                                    leftSibCat = m2.group().replace("(", "L");
                                }
                            }
                        } else if (c == ')') {
                            nOpenParens -= 1;
                        }
                        i -= 1;
                    }
                    //Found parent
                    Matcher m2 = p.matcher(parsedText2.substring(i));
                    if (m2.find()) {
                        parentCat = "P" + m2.group(1);
                        //find right sibling if it exists
                        //find end of selfCategory--need to know number of closing parens and spaces expected
                        rest = parsedText2.substring(pos);

                        //take off that many right parens
                        i = 0;
                        while (parentEndingSelf > 0) {
                            if (rest.charAt(i) == ')')
                                parentEndingSelf -= 1;
                            i += 1;
                        }

                        rest = rest.substring(i);

                        Pattern p3 = Pattern.compile("(\\(+)(\\S+)\\s");
                        Matcher m3 = p3.matcher(rest);
                        if (m3.find()) {
                            rightSibCat = "R" + m3.group(2);
                            //Check for VP and traces
                            nOpenParens = 1;
                            i = 1;
                            while (nOpenParens > 0) {
                                if (rest.substring(i, 3).equals("(VP")) {
                                    rightSibWVP = "RSVPP";
                                }
                                if (rest.substring(i, 7).equals("(-NONE-")) {
                                    rightSibWTrace = "RSTP";
                                }
                                if (rest.charAt(i) == '(')
                                    nOpenParens += 1;

                                if (rest.charAt(i) == ')')
                                    nOpenParens -= 1;

                                i += 1;
                            }
                        }
                        String sense = "NonDisc";
                        String outputLine = "";

//                        selfCat = "S"+m2.group();
                    }

//                    p2 = Pattern.compile("^\\s+");

                }


            }
//            String[] words = conn.split("\\s");
        }
        return "";
    }

    public static void main(String[] args) throws IOException {
        DMIdentifier identifier = getInstance();
        String input =
                "(ROOT (FRAG (NP (NNP Washington)) (NP (-LRB- -LRB-) (NNP CNN) (-RRB- -RRB-)) (RRC (NP (NML (NNP Vice) (NNP President)) (NNP Joe) (NNP Biden)) (PP (ADVP (NP (NNS fares)) (RBR better)) (IN against) (NP (JJ top) (NNP GOP) (NNS candidates))) (PP (IN in) (NP (JJ hypothetical) (JJ general) (NN election) (NN match)))) (, -) (NP (NP (NNS ups)) (PP (IN than) (NP (NNP Hillary) (NNP Clinton)))) (, ,) (PP (VBG according) (PP (IN to) (NP (DT a) (JJ new) (JJ national) (NN survey)))) (. .)))\n" +
                        "(ROOT (S (NP (NP (DT The) (NML (NNP Quinnipiac) (NNP University)) (NN poll)) (, ,) (VP (VBN released) (NP-TMP (NNP Thursday))) (, ,)) (ADVP (RB also)) (VP (VBZ shows) (NP (NP (NNP Donald) (NNP Trump)) (VP (VBG smashing) (NP (DT the) (NNP GOP) (JJ presidential) (NN competition)) (S (VP (VBG garnering) (NP (NML (CD 28) (NN %)) (NN support)) (PP (IN from) (NP (NP (VBN registered) (JJ Republican) (NNS voters)) (PP (IN in) (NP (DT the) (NML (CD 17) (HYPH -) (NN member)) (NN field)))))))))) (. .)))\n" +
                        "(ROOT (S (NP (NP (DT The) (JJ real) (NN estate) (NN mogul) (POS 's)) (JJS closest) (NN competitor)) (VP (VBZ is) (NP (NP (NP (JJ retired) (NN neurosurgeon)) (NP (NNP Ben) (NNP Carson))) (, ,) (SBAR (WHNP (WP who)) (S (VP (VBZ tallies) (NP (CD 12) (NN %))))))) (. .)))\n" +
                        "(ROOT (S (NP (QP (RB Just) (CD 7)) (NN %)) (VP (VBD said) (SBAR (S (NP (PRP they)) (VP (MD would) (VP (VB vote) (PP (IN for) (NP (NP (NML (JJ former) (NNP Florida) (NNP Gov.)) (NNP Jeb) (NNP Bush)) (, ,) (ADVP (NP (DT a) (NN record)) (JJ low) (PP (IN since) (NP (NNP November) (CD 2013))))))))))) (. .)))\n" +
                        "(ROOT (S (NP (DT Those) (NNS results)) (VP (VBP show) (SBAR (WHADJP (RB just) (WRB how) (JJ far)) (S (NP (NP (NP (NP (DT both) (NNP Trump)) (RRC (: --) (ADVP (RB now)) (NP (NP (DT the) (JJ Republican) (NN front)) (PP (SYM -) (NP (NN runner)))) (: --))) (CC and) (NP (NNP Bush))) (: --) (NP (DT the) (JJ old) (NN one)) (: --)) (VP (VBP have) (VP (VBN come)))))) (. .)))\n" +
                        "(ROOT (S (NP (NNP Bush)) (VP (VP (VBD led) (NP (JJ national) (NNS polls)) (PP (IN for) (NP (NP (RB much)) (PP (IN of) (NP (NP (DT the) (JJ first) (NN half)) (PP (IN of) (NP (CD 2015)))))))) (, ,) (CC but) (VP (VBD was) (ADVP (RB quickly)) (VP (VBN dislodged) (PP (IN by) (NP (NNP Trump))) (, ,) (SBAR (IN after) (S (NP (PRP he)) (VP (VBD announced) (NP (PRP$ his) (JJ presidential) (NNS ambitions)) (NP-TMP (DT this) (NNP June)))))))) (. .)))\n" +
                        "(ROOT (S (S (VP (VP (VBD RELATED) (: :) (S (NP (NNP Biden)) (ADJP (JJ unsure)) (SBAR (IN if) (S (NP (PRP he)) (VP (VBZ has) (VP (VBN 'em) (NP (JJ otional) (NN fuel)) ('' ') (PP (IN for) (NP (NP (NP (CD 2016) (NNP Sens.) (NNP Ted) (NNP Cruz)) (PP (IN of) (NP (NNP Texas)))) (CC and) (NP (NP (NNP Marco) (NNP Rubio)) (PP (IN of) (NP (NNP Florida)))))))))))) (CC both) (VP (VBP are) (VP (VBN tied) (PP (IN with) (NP (NNP Bush))) (PP (IN at) (NP (CD 7) (NN %))))))) (, ,) (NP (DT the) (NNS polls)) (VP (VBZ shows) (, ,) (PP (IN with) (NP (NP (NP (NNP Wisconsin) (NNP Gov.) (NNP Scott) (NNP Walker)) (PP (IN at) (NP (NML (NML (CD 6) (NN %)) (CC and) (NML (JJ former) (NN tech))) (NNP CEO) (NNP Carly) (NNP Fiorina)))) (CC and) (NP (NP (NNP Ohio) (NNP Gov.) (NNP John) (NNP Kasich)) (VP (VBN tied) (PP (IN at) (NP (CD 5) (NN %)))))))) (. .)))\n" +
                        "(ROOT (SINV (`` \") (S (S (NP (NNP Donald) (NNP Trump)) (VP (VBZ soars) (S (: ;) (S (NP (NNP Ben) (NNP Carson)) (VP (VBZ rises))) (: ;) (S (NP (NNP Jeb) (NNP Bush)) (VP (VBZ slips)))))) (CC and) (S (NP (DT some) (NNP GOP) (NNS hopefuls)) (VP (VBP seem) (S (VP (TO to) (VP (VB disappear))))))) (, ,) ('' \") (VP (VBD said)) (NP (NP (NNP Tim) (NNP Malloy)) (, ,) (NP (NP (JJ assistant) (NN director)) (PP (IN of) (NP (DT the) (NN survey))))) (. .)))\n" +
                        "(ROOT (S (`` \") (NP (NNP Trump)) (VP (VBZ proves) (SBAR (S (NP (PRP you)) (VP (VBP do) (RB n't) (VP (VB have) (S (VP (TO to) (VP (VB be) (VP (VBN loved) (PP (PP (IN by) (NP (NN everyone))) (, ,) (RB just) (PP (IN by) (NP (JJ enough) (NNPS Republicans)))) (S (VP (TO to) (VP (VB lead) (NP (DT the) (NNP GOP) (NN pack)))))))))))))) (. .) ('' \")))\n" +
                        "(ROOT (S (S (CC And) (NP (NNP Trump)) (ADVP (RB certainly)) (VP (VBZ is) (RB n't) (VP (VBN loved) (PP (IN by) (NP (NN everyone)))))) (, ,) (NP (DT the) (NN survey)) (VP (VBZ shows)) (. .)))\n" +
                        "(ROOT (S (NP (QP (RB About) (CD 1) (HYPH -) (IN in) (HYPH -) (CD 4)) (NNP GOP) (NNS voters)) (VP (VBP say) (SBAR (S (NP (PRP they)) (VP (MD would) (ADVP (RB never)) (VP (VB vote) (PP (IN for) (NP (NNP Trump))) (, ,) (S (VP (VBG topping) (NP (DT the) (NN field))))))))) (. .)))\n" +
                        "(ROOT (S (NP (NNP Bush)) (VP (VBZ comes) (PP (IN in) (NP (NP (JJ second)) (PP (IN with) (NP (CD 18) (NN %)))))) (. .)))\n" +
                        "(ROOT (S (NP (NNP Clinton)) (ADVP (RB still)) (VP (VBZ leads) (NP (NP (DT the) (JJ Democratic) (NN race)) (PP (IN at) (NP (NML (CD 45) (NN %)) (NN support)))) (PP (IN from) (NP (NP (VBN registered) (NNPS Democrats)) (, ,) (VP (VBN followed) (PP (IN by) (NP (NP (NP (NNP Vermont) (NNP Sen.) (NNP Bernie) (NNP Sanders)) (PP (IN at) (NP (NP (CD 22) (NN %)) (CC and) (NP (NNP Biden))))) (: --) (SBAR (WHNP (WP who)) (S (VP (VBZ is) (ADVP (RB currently)) (VP (VBG mulling) (NP (DT a) (CD 2016) (NN bid)))))) (: --))) (PP (IN at) (NP (CD 18) (NN %))))))) (. .)))\n" +
                        "(ROOT (S (CC But) (NP (NNP Biden)) (, ,) (S (ADVP (RB currently)) (VP (VBG sporting) (NP (NP (DT the) (JJS highest) (NN favorability) (NN rating)) (PP (IN among) (NP (NP (DT any) (CD 2016) (NNS candidates)) (VP (VBN polled) (PP (IN of) (NP (DT either) (NN party))))))))) (, ,) (VP (VBZ tops) (NP (QP (CD Trump) (CD 48) (NN %) (IN to) (CD 40) (NN %))) (, ,) (PP (VBN compared) (PP (IN to) (NP (NP (NNP Clinton)) (, ,) (SBAR (WHNP (WP who)) (S (VP (VBZ beats) (NP (QP (CD Trump) (CD 45) (NN %) (IN to) (CD 41) (NN %)))))))))) (. .)))\n" +
                        "(ROOT (S (NP (NNP Biden)) (ADVP (RB also)) (VP (VBZ beats) (NP (NP (NNP Bush)) (, ,) (NP (NP (CD 45) (NN %)) (PP (IN to) (NP (CD 39) (NN %)))) (, ,)) (PP (VBN compared) (PP (IN to) (NP (NP (NNP Clinton)) (, ,) (SBAR (WHNP (WP who)) (S (VP (VBZ beats) (NP (NNP Bush)) (PP (NP (CD 42) (NN %)) (IN to) (NP (CD 40) (NN %)))))))))) (. .)))\n" +
                        "(ROOT (S (PP (IN On) (NP (DT the) (JJ other) (NN hand))) (, ,) (NP (PRP he)) (VP (VBD disliked) (NP (PRP her))) (. .)))\n";

        identifier.analyze(input);
    }
}
