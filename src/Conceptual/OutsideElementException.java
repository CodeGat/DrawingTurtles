package Conceptual;

public class OutsideElementException extends Exception {
    public OutsideElementException(String errorMsg){
        super(errorMsg);
    }

    public OutsideElementException(){
        super();
    }
}
