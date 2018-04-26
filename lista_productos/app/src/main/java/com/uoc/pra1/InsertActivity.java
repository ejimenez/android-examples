package com.uoc.pra1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uoc.datalevel.DataException;
import com.uoc.datalevel.DataObject;
import com.uoc.datalevel.SaveCallback;

import java.io.IOException;
import java.io.InputStream;

public class InsertActivity extends AppCompatActivity {

    private static int SELECT_PICTURE = 1;
    private TextView latLong;
    public LocationManager locationManager;
    public LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("PR2 :: Insert");

        // obtenemos textview para visualizar lat y long
        latLong = (TextView)findViewById(R.id.lat_long);

        // Obtenemos locationManager y LocationListener
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener(){
            public void onLocationChanged(Location location) {
                showLocation(location);

            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        // Se pregunta al usuario si concede permisos para obtener su localización
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Ask permission
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101);
        }
        else{
        // Ask last know location
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location!=null){

                showLocation(location);

            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0, locationListener);

        }

        // Se pregunta al usuario si concede permisos para acceder a ficheros media
        // del dispositivo, esto es necesario para escoger fotos de la galería
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Ask permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    102);
        }

        // Defino funcionalidad si el usuario pulsa el botón de seleccionar imagen
        Button buttonSelectImage = (Button) findViewById(R.id.select_image);
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Creo un intent para obtener contenido de tipo imagen
                // y comienzo la actividad para obtener resultados
                // Utilizo el token SELECT_PICTURE para luego usarlo en la función
                // de vuelta de los resultados
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(intent, SELECT_PICTURE);
            }
        });

        // Función que se ejecuta cuando el usuario pulsa el botón insertar producto
        Button buttonInsertItem = (Button) findViewById(R.id.insert_item);
        buttonInsertItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Obtengo nombre del producto introducido por el usuario
                EditText title = (EditText) findViewById(R.id.title);
                String titleText = title.getText().toString();

                ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);
                Bitmap bmThumbnail = null;

                // Obtengo el bitmap de la imagen selecionada y que está en el imageview
                if(thumbnail.getDrawable() != null) {
                    thumbnail.setDrawingCacheEnabled(true);
                    thumbnail.buildDrawingCache();
                    bmThumbnail = Bitmap.createBitmap(thumbnail.getDrawingCache());
                    thumbnail.setDrawingCacheEnabled(false);
                }

                // Obtengo precio del producto introducido por el usuario
                EditText price = (EditText) findViewById(R.id.price);
                String priceText = price.getText().toString();

                // Obtengo descripción del producto introducido por el usuario
                EditText description = (EditText) findViewById(R.id.description);
                String descriptionText = description.getText().toString();

                // Realizo validaciones para que ningún dato sea nulo y si lo es
                // muestro mensaje al usuario
                if (bmThumbnail == null) {
                    Toast.makeText(InsertActivity.this, "Image is required", Toast.LENGTH_LONG).show();
                } else if (priceText == null || priceText.equals("")) {
                    Toast.makeText(InsertActivity.this, "Price is required", Toast.LENGTH_LONG).show();
                } else if (titleText == null || titleText.equals("")) {
                    Toast.makeText(InsertActivity.this, "Name is required", Toast.LENGTH_LONG).show();

                } else if (descriptionText == null || descriptionText.equals("")) {
                    Toast.makeText(InsertActivity.this, "Description is required", Toast.LENGTH_LONG).show();
                } else {

                    // Creo un dataobject con los datos introducidos por el usuario
                    DataObject new_item = new DataObject("item");
                    new_item.put("name", titleText);
                    new_item.put("image", bmThumbnail);
                    new_item.put("price", priceText);
                    new_item.put("description", descriptionText);

                    // Guardo el producto mediante proceso en background.
                    // Si el guardado va bien mediante una función callback,
                    // introduzco en un intent el object_id del producto creado
                    // para pasárselo a la actividad de listado de productos
                    // para que ésta pueda añadirlo. Finalmente finalizo la actividad
                    // de inserción de producto.
                    new_item.saveInBackground(new SaveCallback<DataObject>() {
                        @Override
                        public void done(DataObject object, DataException e) {
                            if (e == null) {
                                Toast.makeText(InsertActivity.this,
                                        "Product created", Toast.LENGTH_LONG).show();

                                Intent data = new Intent();
                                data.putExtra("object_id",object.m_objectId);
                                setResult(Activity.RESULT_OK, data);
                                finish();

                            } else {

                            }
                        }
                    });
                }
            }
        });

    }

    // Función después de seleccionar la imagen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si vengo de seleccionar la imagen, el resultado es ok y recibo datos
        // es que ha ido bien la selección de la imagen y puedo intentar visualizarla
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && null != data) {

            // Obtengo el uri de la imagen
            Uri selectedImage = data.getData();

            // Hago que se visualice en el imageView de la vista de insertar producto
            ImageView imageView = (ImageView) findViewById(R.id.thumbnail);
            imageView.setImageURI(selectedImage);

        }

    }

    // Gestión de la respuesta de usuario sobre la consesión de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        try {
            switch (requestCode) {
                case 101: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    } else {
                    }
                    return;
                }
            }
        }
        catch(SecurityException err_permission) {
        }
    }

    // Función para mostrar la localización
    private void showLocation(Location location) {

        latLong.setText((String)"lat: " + location.getLatitude() +
                " long: " + location.getLongitude());

    }

    // Función para que desaparezca el teclado si pulsamos fuera del edittext
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }
}
