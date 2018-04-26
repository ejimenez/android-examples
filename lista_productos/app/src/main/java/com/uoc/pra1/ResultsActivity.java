package com.uoc.pra1;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.uoc.datalevel.DataException;
import com.uoc.datalevel.DataLowLevel;
import com.uoc.datalevel.DataObject;
import com.uoc.datalevel.DataQuery;
import com.uoc.datalevel.FindCallback;
import com.uoc.datalevel.FindXMLCallback;
import com.uoc.datalevel.GetCallback;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ResultsActivity extends AppCompatActivity implements  ListView.OnItemClickListener {

    private View mProgressView;
    private ListView mListView;
    private static int ADD_PRODUCT = 1;

    public static final int INSERT_REQUEST = 100;

    public ResultListAdapter m_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        String user_email = getIntent().getStringExtra("user_email");

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("PR2 :: Results");

        mListView = (ListView) findViewById(R.id.listView);
        mProgressView = findViewById(R.id.progress);


        mListView.setOnItemClickListener(this);

        showProgress(true);

        // ************************************************************************
        // UOC - BEGIN - CODE3
        //
        DataQuery query = DataQuery.get("item");


        query.findInBackground("", "", DataQuery.OPERATOR_ALL, new FindCallback<DataObject>() {
            @Override
            public void done(ArrayList<DataObject> dataObjects, DataException e) {
                if (e == null) {
                    if (dataObjects.size() != 0) {
                        m_adapter = new ResultListAdapter(ResultsActivity.this, null);

                        m_adapter.m_array = dataObjects;
                        m_adapter.mActivity = ResultsActivity.this;

                        showProgress(false);
                        mListView.setAdapter(m_adapter);
                    }
                } else {
                    // Error

                }
            }
        });

        // UOC - END - CODE3
        // ************************************************************************


    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {


        // ************************************************************************
        // UOC - BEGIN - CODE5
        //
        DataObject object = (DataObject) m_adapter.m_array.get(position);

        // Send m_objectId to DetailACtivity

        Intent intent;
        intent = new Intent(this, DetailActivity.class);
        intent.putExtra("object_id", object.m_objectId);

        // UOC - END - CODE5
        // ************************************************************************


        startActivity(intent);

    }

    private void showProgress(final boolean show) {

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mListView.setVisibility(show ? View.GONE : View.VISIBLE);

    }

    // Este método sirve para crear el menú para añadir productos
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Añadimos el menú creado en res/menu/product_menu.xml
        // que tiene la opción de menú add para añadir productos
        inflater.inflate(R.menu.product_menu, menu);
        return true;
    }

    // Añadimos método que ejecuta la función addProduct para añadir productos
    // en caso de que el usuario elija la opción de menú add
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_product:
                addProduct();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addProduct() {
        Intent intent;
        // Se crea un intent para ir para ir a la actividad de insertar producto
        intent = new Intent(this, InsertActivity.class);
        // Activamos la animación de progreso para cuando volvamos se insertar producto
        // poder refrescar la lista de productos sin que el usuario vea como se añade el producto
        showProgress(true);
        // Llamamos a la actividad de insertar producto esperando resultados, pasando el token
        // ADD_PRODUCT que se usará en la función de vuelta de los resultados
        startActivityForResult(intent, ADD_PRODUCT);
    }

    // Función que se ejecuta una vez se ha insertado el producto y hemos vuelto
    // a la actividad del listado
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si venimos de insertar producto, el resultado es ok, y recivo datos del producto
        // recien creado procedemos a refrescar la lista.
        if (requestCode == ADD_PRODUCT && resultCode == RESULT_OK && data != null) {
            // Con el código comentado a continuación podríamos refrescar la lista de forma fácil
            // pero implica más coste ya que hay que finalizar la actividad y volverla a crear
            //finish();
            //startActivity(getIntent());

            // Opto por una solución más fina, actualizando la lista solo con el producto recien
            // creado

            // Obtengo el object_id del producto recien creado
            final String object_id = data.getStringExtra("object_id");

            // Obtengo el dataobject del producto creado,
            // lo añado al array del adaptador y
            // notifico que ha cambiado para que se refresque la lista de productos.
            // Finalmente desactivo el progress para que se visualice la lista ya actualizada
            DataQuery query = DataQuery.get("item");
            query.getInBackground(object_id, new GetCallback<DataObject>() {
                @Override
                public void done(DataObject object, DataException e) {
                    if (e == null) {
                        m_adapter.m_array.add(object);
                        m_adapter.notifyDataSetChanged();


                    } else {
                        // Error

                    }
                    // después de que se ejecute la obtención del objeto
                    // tanto si hay éxito como si no, se desactiva el progress
                    showProgress(false);

                }
            });

        }
        else {
            // Si finalmente no hemos creado ningún producto, pero volvemos al listado
            // de productos hay que desactivar el progress
            showProgress(false);
        }
    }
}

