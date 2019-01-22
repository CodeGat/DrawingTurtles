package rdfxml;

import Conceptual.Vertex;
import javafx.util.Pair;

import java.util.HashMap;

public class Correlation {
    private HashMap<Integer, Pair<String, Vertex>> correlations = new HashMap<>();

    Correlation(){}

    public void addCorrelation(Integer index, String csvHeader, Vertex ttlClass){
        correlations.put(index, new Pair<>(csvHeader, ttlClass));
    }
}
