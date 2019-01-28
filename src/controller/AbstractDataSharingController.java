package controller;

import java.util.ArrayList;

public abstract class AbstractDataSharingController<T> {
    public abstract void setData(ArrayList<T> data);
    public abstract ArrayList<T> getData();
}
