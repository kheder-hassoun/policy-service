package me.policy.policy_service;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreAnnotations;

import java.util.*;

public class NerService {
    private final StanfordCoreNLP pipeline;
    private final Map<String, String> nerReplacements;

    public NerService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        this.pipeline = new StanfordCoreNLP(props);

        this.nerReplacements = Map.of(
                "PRODUCT", "[APP]",
                "ORGANIZATION", "[ORG]",
                "PERSON", "[USER]"
                // You can add more like "LOCATION", "DATE", etc.
        );
    }

    public String extractKey(String query) {
        Annotation document = new Annotation(query);
        pipeline.annotate(document);

        StringBuilder generalizedQuery = new StringBuilder();
        boolean replaced = false;
        String prevNer = "";

        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.word();
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (nerReplacements.containsKey(ner)) {
                    if (!ner.equals(prevNer)) {
                        generalizedQuery.append(nerReplacements.get(ner)).append(" ");
                        replaced = true;
                    }
                } else {
                    generalizedQuery.append(word).append(" ");
                }

                prevNer = ner;
            }
        }

        return replaced
                ? generalizedQuery.toString().replaceAll("\\s+", " ").trim()
                : query;
    }
}
