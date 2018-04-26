package com.uoc.datalevel;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Created by Salva on 11/12/2015.
 */
public class DataLowLevel {

    private static DataLowLevel instance = null;

    public ReentrantLock m_lock;
    public String mBasePath;
    public AssetManager m_assetManager;

    DataLowLevel()
    {

        m_lock = new ReentrantLock();

    }

    static public DataLowLevel Get()
    {
        return instance;
    }

    static public DataLowLevel Open(Context context)
    {
        if(instance==null){
            instance = new DataLowLevel();



            instance.mBasePath =  "" + context.getFilesDir();
            instance.m_assetManager = context.getAssets();

            if(!instance.Exists()){
                instance.Create();
            }

        }

        return instance;
    }

    public boolean Exists()
    {
        return ExistsFile(mBasePath + "/database.xml");
    }

    public void Create()
    {
        // Move first xml with your images
        CopyAssetDirToExternal(m_assetManager);
    }

    static public void Close()
    {


    }

    public void saveImage(Bitmap bmp,String filename)
    {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // compression factor (100)
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(DataObject data)
    {

            if (m_lock.tryLock()) {
                try {
                    // manipulate protected state

                    String id_new_image =  UUID.randomUUID().toString();

                    // Save picture in images folder
                    String pathName = mBasePath + "/images/" + id_new_image + ".jpg";
                    Bitmap bitmap = (Bitmap)data.get("image");

                    saveImage(bitmap,pathName);


                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    try {
                        String xmlData = ReadAllText("database.xml");
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));


                       String str_xpath = "data/class[@name = 'item']";

                        XPathFactory factory_xpath=XPathFactory.newInstance();
                        XPath xPath=factory_xpath.newXPath();


                        Node node = (Node) xPath.compile(str_xpath).evaluate(doc, XPathConstants.NODE);


                        String new_node_str = String.format(" <object Id=\"%s\">\n" +
                                "        <properties>\n" +
                                "            <property name=\"type\"><![CDATA[%s]]></property>\n" +
                                "            <property name=\"name\"><![CDATA[%s]]></property>\n" +
                                "            <property name=\"description\"><![CDATA[%s]]></property>\n" +
                                "            <property name=\"price\"><![CDATA[%s]]></property>\n" +
                                "            <property name=\"image\"><![CDATA[%s]]></property>\n" +
                                "        </properties>\n" +
                                "    </object>", data.m_objectId,
                                data.m_properties.get("type"),
                                data.m_properties.get("name"),
                                data.m_properties.get("description"),
                                data.m_properties.get("price"),
                                id_new_image + ".jpg"
                                );

                        Document new_doc = builder.parse(new InputSource(new StringReader(new_node_str)));

                        Node root_new_doc = new_doc.getFirstChild();
                        Node root_new_doc_my_doc = doc.importNode(root_new_doc, true);
                        node.appendChild(root_new_doc_my_doc);





                        try {
                           // String pathNameDatabase = mBasePath + "/database.xml";


                            DOMSource domSource = new DOMSource(doc);
                            StringWriter writer = new StringWriter();
                            StreamResult result = new StreamResult(writer);
                            TransformerFactory tf = TransformerFactory.newInstance();
                            Transformer transformer = tf.newTransformer();
                            transformer.transform(domSource, result);



                            WriteAllText("database.xml", writer.toString());


                        }
                        catch(Exception err_serialize)
                        {



                        }

                    }
                    catch(Exception e)
                    {
                        Log.d("%s",e.getMessage());
                    }


                }
                catch(Exception e)
                {

                }
                finally {
                    m_lock.unlock();
                }
            }
            else {
                // perform alternative actions
            }

    }

    public String getStringXpath(String class_name, String property,Object value,int operator)
    {
        String str_xpath = "";

        if(operator == DataQuery.OPERATOR_EQUAL) {
            str_xpath = String.format("data/class[@name = '%s']/object/properties/property[@name='%s' and text()='%s']/../..", class_name, property, (String) value);
        }
        else if (operator == DataQuery.OPERATOR_ALL) {
            str_xpath = String.format("data/class[@name = '%s']/object", class_name);

        }
        else if (operator == DataQuery.OPERATOR_OBJECT_ID) {
            str_xpath = String.format("//object[@Id = '%s']", value);
        }



        return str_xpath;
    }

    public String findXML(String class_name, String property,Object value,int operator) {
        String resul = "";

        ArrayList<DataObject> lista = find(class_name,property,value,operator);
        resul += "<items>";

        int i=0;
        for(i=0;i<lista.size();i++){
            DataObject obj = lista.get(i);
            String tmp="";

            tmp += String.format("<item Id='%s' price='%s'><properties>",obj.m_objectId,obj.get("price"));

            tmp += "<property name=\"type\"><![CDATA[console]]></property>";

            tmp += String.format("<property name=\"name\"><![CDATA[%s]]></property>",obj.get("name"));
            tmp += String.format("<property name=\"description\"><![CDATA[%s]]></property>",obj.get("description"));

            tmp += String.format("<property name=\"image\"><![CDATA[%s]]></property>",obj.get("image_id"));

            tmp += "</properties></item>";
            resul += tmp;
        }


        resul += "</items>";

        return resul;
    }


    public ArrayList<DataObject> find(String class_name, String property,Object value,int operator)
    {
        ArrayList<DataObject> result = new ArrayList<DataObject>();

        if (m_lock.tryLock()) {
            try {
                // manipulate protected state

                String xmlData = ReadAllText("database.xml");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

                    XPathFactory factory_xpath=XPathFactory.newInstance();
                    XPath xPath=factory_xpath.newXPath();

                    XPathExpression xpath_xpression = xPath.compile(getStringXpath(class_name,property, value, operator));

                    NodeList list = (NodeList) xpath_xpression.evaluate(doc, XPathConstants.NODESET);
                    for(int index = 0; index < list.getLength(); index ++) {
                        Node node = list.item(index);
                        //String name = node.getNodeValue();
                        DataObject obj = null;
                        try {
                            obj = new DataObject("");
                        }
                        catch(Exception err)
                        {
                            int a;
                            a = 1;

                        }
                        obj.m_objectId = node.getAttributes().getNamedItem("Id").getNodeValue();




                        XPathExpression xpath_xpression2 =  xPath.compile("./properties/property");

                        NodeList properties_list = (NodeList) xpath_xpression2.evaluate(node, XPathConstants.NODESET);


                        for(int i=0;i<properties_list.getLength();i++) {
                            Node pro_node = properties_list.item(i);
                            String pro_name =  pro_node.getAttributes().getNamedItem("name").getNodeValue();



                            if(pro_name.equals("image")){
                                String pro_value =  pro_node.getFirstChild().getNodeValue();

                                String pathName = mBasePath + "/images/" + pro_value;

                                Bitmap bitmap = BitmapFactory.decodeFile(pathName);

                                obj.put("image_id",pro_value);

                                obj.put(pro_name,bitmap);

                            }
                            else{
                                String pro_value =  pro_node.getFirstChild().getNodeValue();
                                obj.put(pro_name,pro_value);
                            }



                        }

                        result.add(obj);
                    }

                }
                catch (Exception err)
                {


                }




            }
            catch(Exception e)
            {

            }
            finally {
                m_lock.unlock();
            }
        }
        else {
            // perform alternative actions
        }

        return result;
    }


    // ********************************************************************
    // File System

    public boolean ExistsFile(String path)
    {
        File tFile = new File(path);
        return tFile.exists();
    }


    public void copyFile(InputStream in, OutputStream out) throws IOException {
        //  byte[] buffer = new byte[1024];
        byte[] buffer = new byte[65535];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void copyFileName(String in1, String out1,AssetManager assetManager){

        InputStream in = null;
        OutputStream out = null;

        try {
            in = assetManager.open(in1);
            out = new FileOutputStream(mBasePath + "/" + out1);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(IOException e) {
            Log.e("ERROR", "Failed to copy asset file: " + in1, e);
        }


    }

    public  void CopyAssetDirToExternal(AssetManager assetManager) {

        String the_path = "";


        copyFileName("database.xml", the_path + "/" + "database.xml",assetManager);

        File folder = new File( mBasePath + "/images");
        folder.mkdir();



        copyFileName("images/" + "067e6162-3b6f-4ae2-a171-2470b63dff10.jpg", the_path + "/images/" + "067e6162-3b6f-4ae2-a171-2470b63dff10.jpg",assetManager);
        copyFileName("images/" + "067e6162-3b6f-4ae2-a171-2470b63dff11.jpg", the_path + "/images/" + "067e6162-3b6f-4ae2-a171-2470b63dff11.jpg",assetManager);
        copyFileName( "images/" + "067e6162-3b6f-4ae2-a171-2470b63dff12.jpg", the_path + "/images/" + "067e6162-3b6f-4ae2-a171-2470b63dff12.jpg",assetManager);


    }

    public  String ReadAllText(String filename)
    {
        String resul = "";

        File file = new File(mBasePath + "/" + filename);

        int length = (int) file.length();

        byte[] bytes = new byte[length];
        StringBuffer buffer = new StringBuffer();

        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, "UTF8");
            Reader in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1) {
                buffer.append((char)ch);
            }
            in.close();
            return buffer.toString();
        }
        catch(Exception err) {
            resul = "";
        }


        return resul;
    }

    public  void WriteAllText(String filename,String data)
    {
        String resul = "";
        try {


            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(mBasePath + "/" + filename), "UTF8"));

            out.append(data);


            out.flush();
            out.close();
        }
        catch (Exception err)
        {


        }

    }

}
