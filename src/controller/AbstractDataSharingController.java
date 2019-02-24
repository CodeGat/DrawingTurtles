package controller;

import java.util.ArrayList;

/**
 * An abstract Controller that faciliitates the passing of data to and from the Controller class.
 * @param <T> the type of data passed between the two controllers.
 */
abstract class AbstractDataSharingController<T> {

    /**
     * The method that passes data from the Caller to the Callee.
     * @param data the data to be passed.
     */
    public abstract void setData(ArrayList<T> data);

    /**
     * The method that passes data to the Caller from the Callee.
     * @return the data to be passed.
     */
    public abstract ArrayList<T> getData();
}
