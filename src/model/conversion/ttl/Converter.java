package model.conversion.ttl;

import javafx.util.Pair;
import model.conceptual.Class;
import model.conceptual.Edge;
import model.conceptual.Literal;
import model.conceptual.Vertex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static model.conceptual.Vertex.GraphElemType.*;

/**
 * Class that is responsible for the conversion of a visual graph into a .ttl string.
 */
public class Converter {
    private static Map<String, String> prefixes;
    private static ArrayList<Vertex>   classes;
    private static ArrayList<Edge>     properties;
    private static ArrayList<Boolean>  config;

    private static boolean isOntology;

    private static String tabs = "\t";

    /**
     * The overarching method for conversion of a graph into a string.
     * @param prefixes the Arraylist of known prefixes.
     * @param classes the Arraylist of visual Classes and Literals.
     * @param properties the Arraylist of visual Properties.
     * @param config the options specified by the user.
     * @return a String representation of the graph as Turtle RDF syntax.
     */
    public static String convertGraphToTtlString(
            Map<String, String> prefixes,
            ArrayList<Vertex> classes,
            ArrayList<Edge> properties,
            ArrayList<Boolean> config) {
        Converter.prefixes   = prefixes;
        Converter.classes    = classes;
        Converter.properties = properties;
        Converter.config     = config;

        isOntology = config.get(2);

        String fixesNeeded = getFixes();
        String stringPrefixes = convertPrefixes();
        String stringProperties = convertGProperties();
        String stringClasses  = convertGClasses();


        return fixesNeeded + stringPrefixes + stringClasses + stringProperties;
    }

    /**
     * Get potential problems that the user may want to rectify.
     * Checks include: possible renaming of blank node names, and notifying the user of instance-level literal
     *    placeholders.
     * @return the list of fixes.
     */
    private static String getFixes() {
        StringBuilder fixString = new StringBuilder("# Potential issues found: \n");
        final int fixStringInitLength = fixString.length();

        // checking for blank node names, reminding the user to rename them from basic characters.
        if (Class.getBlankNodeNames().size() > 0){
            fixString.append("# Don't forget to rename generic blank node names, namely: \n# ");
            Class.getBlankNodeNames().forEach(n -> fixString.append(n).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n");
        }

        // reminding the user that instance elements will be replaced by their corresponding instance level data when
        //    converted to instance-level .ttl.
        if (classes.stream().anyMatch(c -> c.getElementType() == INSTANCE_LITERAL)){
            fixString.append("# The following Elements are placeholders for instance-level data that will be populate" +
                    "d during instance-level .ttl creation: \n#   ");
            classes.stream()
                    .filter(c -> c.getElementType() == INSTANCE_LITERAL || c.getElementType() == INSTANCE_CLASS)
                    .forEach(c -> fixString.append(c.getName()).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n");
        }

        Stream<String> ttlClassPrefixesStream = classes.stream()
                .filter(c -> c.getElementType() == GLOBAL_CLASS && !((Class) c).isIri())
                .map(c -> c.getName().split(":")[0]);
        Stream<String> ttlPropPrefixesStream = properties.stream()
                .filter(p -> !p.isIri())
                .map(p -> p.getName().split(":")[0]);
        Set<String> ttlPrefixSet = Stream
                .concat(ttlClassPrefixesStream, ttlPropPrefixesStream)
                .filter(p -> !p.equals("_"))
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> addedPrefixesSet = prefixes.keySet();

        if (!addedPrefixesSet.equals(ttlPrefixSet)){
            Set<String> ttlPrefixSetTmp = new HashSet<>(ttlPrefixSet);
            Set<String> addedPrefixesSetTmp = new HashSet<>(addedPrefixesSet);
            ttlPrefixSetTmp.removeAll(addedPrefixesSet);
            addedPrefixesSetTmp.removeAll(ttlPrefixSet);

            if (ttlPrefixSetTmp.size() > 0) {
                fixString.append("# The following prefixes are defined in the graph but not in the prefixes menu (ins" +
                        "tance-level .ttl creation will not work):\n#   ");
                ttlPrefixSetTmp.forEach(p -> fixString.append(p).append(", "));
                fixString.delete(fixString.length() - 2, fixString.length());
                fixString.append(".\n");
            }
            if (addedPrefixesSetTmp.size() > 0){
                fixString.append("# The following prefixes are defined in the prefixes menu but remain unused in the " +
                        "graph:\n#   ");
                addedPrefixesSetTmp.forEach(p -> fixString.append(p).append(", "));
                fixString.delete(fixString.length() - 2, fixString.length());
                fixString.append(".\n\n");
            }
        }

        return fixString.length() > fixStringInitLength ? fixString.toString() : "";
    }

    /**
     * Conversion of prefixes into .ttl prefixes.
     * Helper of {@link #convertGraphToTtlString(Map, ArrayList, ArrayList, ArrayList)}.
     * @return the converted prefixes.
     */
    private static String convertPrefixes() {
        StringBuilder prefixStrs = new StringBuilder();

        for (Map.Entry<String, String> prefix : prefixes.entrySet()){
            String   prefixStr = "@prefix " + prefix.getKey() + " : <" + prefix.getValue() + "> .\n";
            prefixStrs.append(prefixStr);
        }

        return prefixStrs.toString();
    }

    /**
     * Conversion of graph properties into .ttl representation.
     * Finds properties that are common, for example two 'foaf:knows', and determines the common type between them.
     * @return the properties as a valid .tll string.
     */
    private static String convertGProperties() {
        if (!isOntology) return "";

        StringBuilder propStrs = new StringBuilder(
                "\n##################################################\n" +
                "#####          Ontology Properties           #####\n" +
                "##################################################\n\n"
        );

        // Map from the common property name to the associated subject/object pairs.
        HashMap<String, ArrayList<Pair<Vertex, Vertex>>> commonProperties = new HashMap<>();
        for (Edge property : properties){
            Vertex sub = property.getSubject();
            Vertex obj = property.getObject();
            String propertyName = property.getName();

            // rdf:type is explicitly domain rdfs:Resource, range rdfs:Class. No need to constrain.
            if (propertyName.equals("a")) continue;

            if (commonProperties.containsKey(propertyName))
                commonProperties.get(propertyName).add(new Pair<>(sub, obj));
            else {
                ArrayList<Pair<Vertex, Vertex>> pairs = new ArrayList<>();

                pairs.add(new Pair<>(sub, obj));
                commonProperties.put(propertyName, pairs);
            }
        }

        commonProperties.forEach((prop, subObjPairs) -> propStrs.append(getDomainAndRange(prop, subObjPairs)));

        return propStrs.toString();
    }

    /**
     * Finds the rdfs:domain and rdfs:range of the given propName.
     * Squashes all subjects/objects that have the same type together, and attempts to find a common base type.
     * For example:
     *    a:T foaf:knows a:P ;
     *        foaf:knows a:Q .
     *    a:P a a:R .
     *    a:Q a a:R .
     * The domain would be a:T, and the range a:R.
     * If there is no base type, it lists all of the types.
     * For example:
     *    a:T foaf:knows a:P ;
     *        foaf:knows a:Q .
     *    a:P a a:R .
     *    a:Q a a:S .
     * The domain would be a:T, and the range a:R, a:S;
     * @param propName the name of the property.
     * @param subObjPairs the subject/object pairs of the given property.
     * @return the .ttl representation of the domain and range of the property.
     */
    private static String getDomainAndRange(String propName, ArrayList<Pair<Vertex, Vertex>> subObjPairs) {
        String propStrBase = (propName.matches("https?:.*|mailto:.*") ? "<" + propName + ">" : propName) +
                " rdf:type owl:ObjectProperty ;\n\t";
        StringBuilder propStr = new StringBuilder(propStrBase);

        HashSet<String> commonSubNames = new HashSet<>();
        HashSet<String> commonObjNames = new HashSet<>();
        ArrayList<Class> classSubs = new ArrayList<>();
        ArrayList<Class> classObjs = new ArrayList<>();
        ArrayList<Literal> litSubs = new ArrayList<>();
        ArrayList<Literal> litObjs = new ArrayList<>();

        subObjPairs.forEach(p -> {
            Vertex sub = p.getKey();
            Vertex obj = p.getValue();

            commonSubNames.add(sub.getName());
            commonObjNames.add(obj.getName());

            if (sub instanceof Class) classSubs.add((Class) sub);
            else litSubs.add((Literal) sub);

            if (obj instanceof Class) classObjs.add((Class) obj);
            else litObjs.add((Literal) obj);
        });

        HashSet<String> commonSubTypeDefinitions = classSubs.stream().map(Class::getTypeDefinition).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> commonObjTypeDefinitions = classObjs.stream().map(Class::getTypeDefinition).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> commonSubDataTypes = litSubs.stream().map(Literal::getDataType).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> commonObjDataTypes = litObjs.stream().map(Literal::getDataType).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

        propStr.append("rdfs:domain ");
        if (commonSubTypeDefinitions.size() > 0){
            propStr.append(commonSubTypeDefinitions.size() != 1 ? "\n\t\t" : "");
            commonSubTypeDefinitions.forEach(typedef -> propStr.append(typedef).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(";\n\t");
        } else if (commonSubDataTypes.size() > 0){
            propStr.append(commonSubDataTypes.size() != 1 ? "\n\t\t" : "");
            commonSubDataTypes.forEach(datatype -> propStr.append(datatype).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(";\n\t");
        } else {
            propStr.append(commonSubNames.size() != 1 ? "\n\t\t" : "");
            commonSubNames.forEach(s -> propStr.append(s.matches("https?:.*|mailto:.*") ? "<" + s + ">" : s).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(";\n\t");
        }

        propStr.append("rdfs:range ");
        if (commonObjTypeDefinitions.size() > 0){
            propStr.append(commonObjTypeDefinitions.size() != 1 ? "\n\t\t" : "");
            commonObjTypeDefinitions.forEach(typedef -> propStr.append(typedef).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(".\n");
        } else if (commonObjDataTypes.size() > 0){
            propStr.append(commonObjDataTypes.size() != 1 ? "\n\t\t" : "");
            commonObjDataTypes.forEach(datatype -> propStr.append(datatype).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(".\n");
        } else {
            propStr.append(commonObjNames.size() != 1 ? "\n\t\t" : "");
            commonObjNames.forEach(o -> propStr.append(o.matches("https?:.*|mailto:.*") ? "<" + o + ">" : o).append(" ,\n\t\t"));
            propStr.delete(propStr.length() - 4, propStr.length());
            propStr.append(".\n");
        }

        return propStr.toString();
    }

    /**
     * Comversion of visual Classes and Literals into their .ttl string equivalent.
     * @return the converted Classes and Literals into thier .ttl equivalent.
     */
    private static String convertGClasses() {
        StringBuilder classStrs = new StringBuilder(
                "##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n"
        );

        for (Vertex graphClass : classes) {
            boolean isBlanknode = config.get(1) && graphClass instanceof Class && ((Class) graphClass).isBlank();

            if (graphClass.getElementType() == GLOBAL_CLASS && !isBlanknode)
                classStrs.append(convertTriple(graphClass));
        }

        return classStrs.toString();
    }

    /**
     * Converts a triple to a String. Equivalent to https://www.w3.org/TR/turtle/#grammar-production-triples.
     * @param subject the subject of the triple.
     * @return the triple in String form.
     */
    private static String convertTriple(Vertex subject){
        String subjectString = convertSubject(subject);
        String predicateObjectString = convertPredicateObjectList(subject);

        if (predicateObjectString.length() == 0 && !isOntology && ((Class) subject).getTypeDefinition() == null) {
            return "";
        } else if (predicateObjectString.length() == 0)
            subjectString = subjectString.substring(0, subjectString.length() - 4);

        return subjectString + predicateObjectString + " .\n\n";
    }

    /**
     * Creates the predicate-object list of the given subject.
     * Equivalent to https://www.w3.org/TR/turtle/#grammar-production-predicateObjectList
     * @param subject the Vertex of the predicate-object list.
     * @return the predicate-object list in String form.
     */
    private static String convertPredicateObjectList(Vertex subject) {
        StringBuilder predicateObjectListSB = new StringBuilder();
        boolean first = true;

        // a map of objects that share the same predicate.
        HashMap<String, ArrayList<Vertex>> commonObjects = new HashMap<>();
        for (Edge edge : subject.getOutgoingEdges()){
            Vertex obj = edge.getObject();
            String edgeName = edge.getName();

            if (commonObjects.containsKey(edgeName)) commonObjects.get(edgeName).add(obj);
            else commonObjects.put(edgeName, new ArrayList<>(Collections.singletonList(obj)));
        }

        if (isOntology) {
            int initPredObjListSBLength = predicateObjectListSB.length();
            predicateObjectListSB.append(getRdfsProperties((Class) subject));
            if (predicateObjectListSB.length() > initPredObjListSBLength) first = false;
        }

        for (Map.Entry<String, ArrayList<Vertex>> e : commonObjects.entrySet()){
            String predicateObjectListStr = "";
            String propName = e.getKey().matches("https?:.*") ? "<" + e.getKey() + ">" : e.getKey();
            String objectListStr;
            ArrayList<Vertex> objectList = e.getValue();

            objectListStr = convertObjectList(objectList);

            if (first) first = false;
            else predicateObjectListStr = " ;\n" + tabs;

            predicateObjectListStr += propName + " " + objectListStr;
            predicateObjectListSB.append(predicateObjectListStr);
        }

        return predicateObjectListSB.toString();
    }

    /**
     * @param subject the subject of the potential rdfs properties.
     * @return the meta-information about the given subject
     */
    private static String getRdfsProperties(Class subject) {
        String result = "";
        String rdfsLabel = subject.getRdfsLabel();
        String rdfsComment = subject.getRdfsComment();

        if (rdfsLabel != null && rdfsLabel.length() != 0 && rdfsLabel.contains("\n"))
            result += "rdfs:label \"\"\"" + rdfsLabel + "\"\"\" ;\n" + tabs;
        else if (rdfsLabel != null && rdfsLabel.length() != 0)
            result += "rdfs:label \"" + rdfsLabel + "\" ;\n" + tabs;
        if (rdfsComment != null && rdfsComment.length() != 0 && rdfsComment.contains("\n"))
            result += "rdfs:comment \"\"\"" + rdfsComment + "\"\"\" ;\n" + tabs;
        else if (rdfsComment != null && rdfsComment.length() != 0)
            result += "rdfs:comment \"" + rdfsComment + "\" ;\n" + tabs;

        return result.length() != 0 ? result.substring(0, result.length() - 4) : result;
    }

    /**
     * Creates the object list of a predicate.
     * Equivalent to https://www.w3.org/TR/turtle/#grammar-production-objectList
     * @param objectList list of Vertices to be converted.
     * @return the object list in String form.
     */
    private static String convertObjectList(ArrayList<Vertex> objectList) {
        StringBuilder objectListSB = new StringBuilder();
        boolean asCollection = config.get(0);
        boolean first = true;

        if (asCollection && objectList.size() > 1) objectListSB.append("(");
        for (Vertex object : objectList){
            String objectListStr;

            if (objectList.size() == 1 || (first && asCollection)){
                objectListSB.append(convertObject(object));
                first = false;
            } else if (asCollection){
                objectListStr = " " + convertObject(object);
                objectListSB.append(objectListStr);
            } else {
                indentTab();
                objectListStr = (!first ? " ," : "") + "\n" + tabs +  convertObject(object);
                objectListSB.append(objectListStr);
                dedentTab();
                first = false;
            }

        }
        if (asCollection && objectList.size() > 1) objectListSB.append(")");
        return objectListSB.toString();
    }

    /**
     * Creates the object, turning it into a string representation of the Vertex.
     * Closely models https://www.w3.org/TR/turtle/#grammar-production-object
     * @param object the Vertex to be converted.
     * @return the Object as a string.
     */
    private static String convertObject(Vertex object) {
        String objectStr = object.getName();
        boolean asBlankNodeList = config.get(1);

        if (object instanceof Class && asBlankNodeList && ((Class) object).isBlank()) {
            String predicateObjectList = convertPredicateObjectList(object);

            if (predicateObjectList.contains(";") || predicateObjectList.contains(",")) {
                indentTab();
                objectStr = "[\n" + tabs + convertPredicateObjectList(object) + "\n";
                dedentTab();
                objectStr += tabs + "]";
            } else objectStr = "[" + predicateObjectList + "]";
        } else if (object instanceof Literal && object.getElementType() == INSTANCE_LITERAL) {
            Literal literal = (Literal) object;
            String dataType = literal.getDataType();
            objectStr = "\"" + objectStr + "\"" +
                    (dataType != null && dataType.length() != 0 ? "^^" + literal.getDataType() : "");
        } else objectStr = objectStr.matches("https?:.*|mailto:.*") ? "<"+objectStr+">" : objectStr;

        return objectStr;
    }

    /**
     * Converts the subject node into the base class definition.
     * Closely models https://www.w3.org/TR/turtle/#grammar-production-subject
     * @param klass the subject to be converted.
     * @return a string representation of the subjects class.
     */
    private static String convertSubject(Vertex klass){
        Class subject = (Class) klass;
        String subname = klass.getName();
        subname = subname.matches("https?:.*|mailto:.*") ? "<" + subname + ">" : subname;
        String typeDef;

        if (isOntology)
            typeDef = " a " + (subject.getTypeDefinition() != null ? subject.getTypeDefinition() + " ;" : "owl:Class ;");
        else typeDef = subject.getTypeDefinition() != null ? " a " + subject.getTypeDefinition() + " ;" : "    ";

        return subname + typeDef + "\n" + tabs;
    }

    /**
     * Increase the current indentation level.
     */
    private static void indentTab() { tabs += "\t"; }

    /**
     * Decreases the current indentation level.
     */
    private static void dedentTab() { tabs = tabs.substring(0, tabs.length() - 1); }
}
