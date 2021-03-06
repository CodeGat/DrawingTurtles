package model.dataintegration;

import model.conceptual.Vertex;

/**
 * The link between a .csv header and an associated graph class.
 */
public class Correlation {
    private Integer index;
    private String csvHeader;
    private Vertex ttlClass;

    public Correlation(Integer index, String csvHeader, Vertex ttlClass){
        this.index = index;
        this.csvHeader = csvHeader;
        this.ttlClass = ttlClass;
    }

    @Override public String toString(){
        return index + ". " + csvHeader + " <-> " + ttlClass.getName();
    }

    Integer getIndex() { return index; }
    Vertex  getTtlClass() { return ttlClass; }
}
