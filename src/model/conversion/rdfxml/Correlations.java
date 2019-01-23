package model.conversion.rdfxml;

import model.conceptual.Vertex;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Correlations {
    private HashMap<Integer, Pair<String, Vertex>> correlations = new HashMap<>();

    public Correlations(){}

    public void addCorrelation(Integer index, String csvHeader, Vertex ttlClass){
        correlations.put(index, new Pair<>(csvHeader, ttlClass));
    }

    public ArrayList<String> getStringCorrelations(){
        ArrayList<String> a = new ArrayList<>();
        correlations.forEach((ix, pair) -> a.add(ix + ". " + pair.getKey() + " <-> " + pair.getValue().getName()));
        return a;
    }
}
