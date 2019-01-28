package controller;

import java.util.ArrayList;

/**
 * An abstract Controller that faciliitates the passing of data to and from the Controller class.
 * @param <T> the type of data passed between the classes.
 */
public abstract class AbstractDataSharingController<T> {
    public abstract void setData(ArrayList<T> data);
    public abstract ArrayList<T> getData();
}
