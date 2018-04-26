package com.uoc.pra1;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.uoc.datalevel.DataException;
import com.uoc.datalevel.DataObject;
import com.uoc.datalevel.DataQuery;
import com.uoc.datalevel.FindCallback;
import com.uoc.datalevel.GetCallback;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    private ProgressBar mProgressView;
    private TableLayout mDetailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("PR2 :: Detail");

        mDetailView = (TableLayout) findViewById(R.id.product_detail);
        mProgressView = (ProgressBar) findViewById(R.id.progress);

        // ************************************************************************
        // UOC - BEGIN - CODE6
        //

        // Relleno con código de la prac1 para poder mostrar el detalle del producto
        showProgress(true);
        String object_id = getIntent().getStringExtra("object_id"); // Get object_id from Intent

        DataQuery query = DataQuery.get("item");
        query.getInBackground(object_id, new GetCallback<DataObject>() {
            @Override
            public void done(DataObject object, DataException e) {
                if (e == null) {
                    TextView title = (TextView)findViewById(R.id.title);
                    title.setText((String) object.get("name"));

                    ImageView  thumbnail=  (ImageView)findViewById(R.id.thumbnail);
                    thumbnail.setImageBitmap((Bitmap) object.get("image"));

                    String euro = "\u20ac";
                    TextView price = (TextView)findViewById(R.id.price);
                    price.setText((String) object.get("price") + euro);

                    TextView description = (TextView)findViewById(R.id.description);
                    description.setText((String) object.get("description"));
                    description.setMovementMethod(LinkMovementMethod.getInstance());
                    showProgress(false);


                } else {
                    // Error

                }
            }
        });
        // UOC - END - CODE6
        // ************************************************************************

    }

    private void showProgress(final boolean show) {

        mProgressView.setVisibility(show ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
        mDetailView.setVisibility(show ? TableLayout.INVISIBLE : TableLayout.VISIBLE);

    }
}
