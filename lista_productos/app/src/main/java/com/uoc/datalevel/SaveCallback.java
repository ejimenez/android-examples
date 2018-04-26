package com.uoc.datalevel;

/**
 * Created by Salva on 20/02/2016.
 */
public interface SaveCallback<DataObject> {
    public void done(DataObject object, DataException e);
}
