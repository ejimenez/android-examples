package com.uoc.datalevel;

import java.util.ArrayList;

/**
 * Created by Salva on 20/02/2016.
 */
public interface FindXMLCallback<DataObject> {
    public void done(String xml_objects, DataException e);
}
