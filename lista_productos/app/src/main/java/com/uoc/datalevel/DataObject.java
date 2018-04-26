package com.uoc.datalevel;

import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;

import com.uoc.pra1.MainActivity;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Salva on 11/12/2015.
 */
public class DataObject {

    public String m_objectId;
    public String m_class_name;
    public Map<String,Object> m_properties;

    public SaveCallback m_saveCallback;

    public Runnable m_runnable;

    public int m_delay;

    public Handler m_handler = null;

    public DataObject(String class_name)
    {
        m_class_name = class_name;
        m_properties = new Hashtable<String,Object>();

        m_objectId = UUID.randomUUID().toString();

        m_delay = 400;

        if(!class_name.equals("")){
            m_handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {


                    if(m_saveCallback!=null){
                        (m_saveCallback).done(DataObject.this, null);
                    }


                    m_saveCallback = null;

                }

            };

        }
    }




    // Properties

    public void put(String key,Object value)
    {

        m_properties.put(key,value);
    }

    public Object get(String key)
    {
        Object obj = m_properties.get(key);

        if(obj!=null) return obj;
        else return "";
    }

    // *******************************************************************
    // Save
    public void save()
    {
        DataLowLevel lowLevel = DataLowLevel.Get();
        lowLevel.save(this);

    }







    public void saveInBackground( SaveCallback<DataObject> callback)
    {
        m_saveCallback = callback;
        m_runnable = new Runnable() {
            public void run() {

                try {


                    Thread.sleep(m_delay);

                    DataLowLevel lowLevel = DataLowLevel.Get();
                    lowLevel.save(DataObject.this);
                    Message msg = m_handler.obtainMessage();
                    m_handler.sendMessage(msg);
                }
                catch(Exception err)
                {


                }


            }
        };

        Thread mythread = new Thread(m_runnable);
        mythread.start();


    }


}
