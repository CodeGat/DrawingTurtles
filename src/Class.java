public class Class {
    enum Type {
        CLASS, LITERAL
    }

    private Type type;
    private String name;

    public Class(Type type, String name){
        this.type = type;
        this.name = name;
    }
}
