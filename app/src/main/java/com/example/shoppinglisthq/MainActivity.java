package com.example.shoppinglisthq;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;


import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ShoppingMemoDataSource dataSource;
    private boolean isButtonClick = true;
    private ListView mShoppingMemosListView;
    CheckedTextView checkedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ShoppingMemo testMemo = new ShoppingMemo("Birnen", 5, 102, false);
        Log.d(TAG, "Inhalt der Testmemo: " + testMemo.toString());
        dataSource = new ShoppingMemoDataSource(this);

        initializeShoppingMemosListView();
        activateAddButton();
        initializeContextualActionBar();
    }

    private void initializeShoppingMemosListView() {

        List<ShoppingMemo> emptyListForInitialization = new ArrayList<>();
        mShoppingMemosListView = findViewById(R.id.listview_shopping_memos);

        // Erstellen des ArrayAdapters für unseren ListView: integrieren // Adapter soll zeigen durchgestrichen oder nicht
        ArrayAdapter<ShoppingMemo> shoppingMemoArrayAdapter = new ArrayAdapter<ShoppingMemo> (
                                    this,
                                     android.R.layout.simple_list_item_multiple_choice,
                                     emptyListForInitialization) {

            // Wird immer dann aufgerufen, wenn der übergeordnete ListView die Zeile neu zeichnen muss
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view =  super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

               // checkedTextView = (CheckedTextView) view; //
               //  checkedTextView.setCheckMarkDrawable(null); // ohne checked Box

                ShoppingMemo memo = (ShoppingMemo) mShoppingMemosListView.getItemAtPosition(position);

                // prüfen, ob Eintrag abgehakt ist. Falls ja, Text durchstreichen
                if (memo.isChecked()) {
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    textView.setTextColor(Color.rgb(175,175,175));
                }
                else {
                    textView.setPaintFlags( textView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                    textView.setTextColor(Color.DKGRAY);
                }

                return view;
            }
        };

        mShoppingMemosListView.setAdapter(shoppingMemoArrayAdapter);

      /*  mShoppingMemosListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ShoppingMemo memo = (ShoppingMemo) adapterView.getItemAtPosition(position);

                // Hier den checked-Wert des Memo-Objekts umkehren, bspw. von true auf false
                // Dann ListView neu zeichnen mit showAllListEntries()
                ShoppingMemo updatedShoppingMemo = dataSource.updateShoppingMemo(memo.getId(), memo.getProduct(), memo.getQuantity(), (!memo.isChecked()));
                Log.d(TAG, "Checked-Status von Eintrag: " + updatedShoppingMemo.toString() + " ist: " + updatedShoppingMemo.isChecked());
                showAllListEntries();
            }
        });*/

        mShoppingMemosListView.setOnItemClickListener( (parent, view, position, id )-> {

                    ShoppingMemo memo = (ShoppingMemo) parent.getItemAtPosition(position);

                    ShoppingMemo updateMemo = dataSource.updateShoppingMemo(memo.getId(), memo.getProduct(),
                            memo.getQuantity(), !memo.isChecked());

                        showAllListEntries();

        }


        );


    }
    private void initializeContextualActionBar() {


        final ListView shoppingMemosListView = findViewById(R.id.listview_shopping_memos);


        /*shoppingMemosListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                checkedTextView.setChecked(true);

            }
        });*/
        shoppingMemosListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        shoppingMemosListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            int selCount = 0;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                if(checked){
                    selCount++;
                }else{
                    selCount--;
                }
                String cabTitle = selCount + " " + getString(R.string.cab_checked_string);
                mode.setTitle(cabTitle);
                mode.invalidate(); // sofort ausgewählt
               // checkedTextView.setChecked(true);

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getMenuInflater().inflate(R.menu.menu_contextual_action_bar, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

                MenuItem item = menu.findItem(R.id.cab_change);
                if (selCount == 1) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }

                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {



                boolean returnValue = true;
                SparseBooleanArray touchedShoppingMemosPositions = shoppingMemosListView.getCheckedItemPositions();

                //  auf Klicks auf CAB-Actions reagieren.
                switch (item.getItemId()) {
                    case R.id.cab_delete:


                        for (int i = 0; i < touchedShoppingMemosPositions.size(); i++) {
                            boolean isChecked = touchedShoppingMemosPositions.valueAt(i);
                            if (isChecked) {
                                int postitionInListView = touchedShoppingMemosPositions.keyAt(i);  // Schlüssel ist position,
                                ShoppingMemo shoppingMemo = (ShoppingMemo) shoppingMemosListView.getItemAtPosition(postitionInListView);
                                Log.d(TAG, "Position im ListView: " + postitionInListView + " Inhalt: " + shoppingMemo.toString());
                                dataSource.deleteShoppingMemo(shoppingMemo);  // hier wird wirklich gelöscht
                            }
                        }
                        showAllListEntries(); // undere View wieder gezeichnet werden ...
                        mode.finish();
                        break;

                    case R.id.cab_change:

                        for (int i = 0; i < touchedShoppingMemosPositions.size(); i++) {

                            boolean isChecked = touchedShoppingMemosPositions.valueAt(i);
                            if (isChecked) {
                                int postitionInListView = touchedShoppingMemosPositions.keyAt(i);
                                ShoppingMemo shoppingMemo = (ShoppingMemo) shoppingMemosListView.getItemAtPosition(postitionInListView);
                                Log.d(TAG, "Position im ListView: " + postitionInListView + " Inhalt: " + shoppingMemo.toString());
                                AlertDialog editShoppingMemoDialog = createEditShoppingMemoDialog(shoppingMemo);
                                editShoppingMemoDialog.show();
                            }
                        }

                        mode.finish(); // schliess die Contextual Action Mode
                        break;

                    default:
                        returnValue = false;

                }
                return returnValue;
            }


            @Override
            public void onDestroyActionMode(ActionMode mode) {
                selCount = 0;
            }
        });
    }

    private void activateAddButton() {

        final EditText editTextQuantity = (EditText) findViewById(R.id.editText_quantity);
        final EditText editTextProduct = (EditText) findViewById(R.id.editText_produkt);
        Button buttonAddProduct = (Button) findViewById(R.id.button_add_product);

        buttonAddProduct.setOnClickListener(v -> {

            String quantityString = editTextQuantity.getText().toString();
            String product = editTextProduct.getText().toString();

            if (TextUtils.isEmpty(quantityString)) {
                editTextQuantity.setError(getString(R.string.editText_errorMessage));
                return;
            }
            if (TextUtils.isEmpty(product)) {
                editTextProduct.setError(getString(R.string.editText_errorMessage));
                return;
            }

            int quantity = Integer.parseInt(quantityString);
            editTextQuantity.setText("");
            editTextProduct.setText("");

            dataSource.createShoppingMemo(product, quantity, false);  // !!!

            InputMethodManager inputMethodManager;
            inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null && isButtonClick) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);  // Tastatur ist unsichtig

            }

            showAllListEntries();

        });

        editTextProduct.setOnEditorActionListener( (textView, pos, keyEvent)-> {

            //onEditorAction(TextView v, int actionId, KeyEvent event)
            // KEY EVENT ist wichtig !! von der Soft tastatur oder nicht
            //Log.d("TAG", "activateAddButton" + keyEvent.toString());

            isButtonClick = false;
            buttonAddProduct.performClick();
            editTextQuantity.requestFocus();
            isButtonClick = true;
            return true;
        });


    }
    private AlertDialog createEditShoppingMemoDialog(final ShoppingMemo shoppingMemo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogsView = inflater.inflate(R.layout.dialog_edit_shopping_memo, null);
        final EditText editTextNewQuantity = dialogsView.findViewById(R.id.editText_new_quantity);
        editTextNewQuantity.setText(String.valueOf(shoppingMemo.getQuantity()));
        //editTextNewQuantity.setText(shoppingMemo.getQuantity());
        final EditText editTextNewProduct = (EditText) dialogsView.findViewById(R.id.editText_new_product);
        editTextNewProduct.setText(shoppingMemo.getProduct());

        builder.setView(dialogsView)
                .setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.dialog_button_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String quantityString = editTextNewQuantity.getText().toString(); // Werte von der Felder
                        String product = editTextNewProduct.getText().toString();

                        if ((TextUtils.isEmpty(quantityString)) || (TextUtils.isEmpty(product))) { // Felder prüfen, ob leer ist
                            Log.d(TAG, "Ein Eintrag enthielt keinen Text. Daher Abbruch der Änderung.");
                            Toast.makeText(MainActivity.this, " Felder dürfen nicht leer sein", Toast.LENGTH_LONG).show();
                            // Wenn JA, METHODE wird verlassen.
                            return;
                        }

                        int quantity = Integer.parseInt(quantityString); // String zu den Int
                        // An dieser Stelle schreiben wir die geänderten Daten in die SQLite Datenbank
                        ShoppingMemo updatedShoppingMemo = dataSource.updateShoppingMemo(shoppingMemo.getId(), product, quantity, shoppingMemo.isChecked());

                        Log.d(TAG, "Alter Eintrag - ID: " + shoppingMemo.getId() + " Inhalt: " + shoppingMemo.toString());
                        Log.d(TAG, "Neuer Eintrag - ID: " + updatedShoppingMemo.getId() + " Inhalt: " + updatedShoppingMemo.toString());

                        showAllListEntries();
                        dialog.dismiss();
                        //InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        // imm.showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_FORCED);

                    }
                })
                .setNegativeButton(R.string.dialog_button_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        editTextNewProduct.setSelection(0, editTextNewQuantity.length());

        return builder.create();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Datenquelle wird geöffnett .. ");
        dataSource.open();
        showAllListEntries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Datenquelle wird geschlossen .. ");
        dataSource.close();
    }

    private void showAllListEntries() {
       /* List<ShoppingMemo> shoppingMemoList = dataSource.getAllShoppingMemos();

        ArrayAdapter<ShoppingMemo> shoppingMemoArrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                shoppingMemoList);

        ListView shoppingMemosListView = findViewById(R.id.listview_shopping_memos);
        shoppingMemosListView.setAdapter(shoppingMemoArrayAdapter);



        shoppingMemosListView.setSelection(shoppingMemoArrayAdapter.getCount() - 1);*/


       // Anpassung der neue Anforderungen
       List<ShoppingMemo> shoppingMemoList = dataSource.getAllShoppingMemos(); // immer aktuellen Zusatand der DB
        ArrayAdapter<ShoppingMemo> adapter = (ArrayAdapter<ShoppingMemo>) mShoppingMemosListView.getAdapter();
        adapter.clear();
        adapter.addAll(shoppingMemoList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {

            case R.id.action_setting:
                Toast.makeText(this, "Setttings wurde gedrückt", Toast.LENGTH_LONG);
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    public void startScan(View view) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");


        intent.putExtra("SCAN_MODE", "PRODUCT_MODE");   // hier immer produckt code

        try {
            startActivityForResult(intent, 1);  // Aktivity für ein Result starten
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Scanner nicht installiert ", Toast.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if(requestCode==1 && resultCode == RESULT_OK) {

        TextView product = findViewById(R.id.editText_produkt);
        product.setText(getProductName(data.getStringExtra("SCAN_RESULT"))); // Text von der Intent kommt
        TextView quantity = findViewById(R.id.editText_quantity);
        quantity.requestFocus();


    }
    }


    private String getProductName(String scanResult){

        HoleDatenTask task = new HoleDatenTask();
        String result = null;
        try {
            result = task.execute(scanResult).get();

            JSONObject rootObject = new JSONObject(result);
            Log.d(TAG, "rootJSON : " + rootObject.toString(2));

            if(rootObject.has("product")) {
                JSONObject productObject = rootObject.getJSONObject("product");  //gescheifte Klammer ist object
                if(productObject.has("product_name" )) {

                    Log.d("test", "product_name   --- >" + productObject.getString("product_name"));
                    return productObject.getString("product_name");
                }

            }
        } catch (ExecutionException e) {
            Log.e(TAG, "", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }

        return "";

    }


    public class HoleDatenTask extends AsyncTask<String, Void, String> {

        // was zurück kommt, ist die JSON String

        @Override
        protected String doInBackground(String... strings) {

            // https://world.openfoodfacts.org/api/v0/product/5000112548167.json  // BARCODE !!
            final String baseURL = "https://world.openfoodfacts.org/api/v0/product/";
            final String requestURL = baseURL + strings[0] + ".json";

            Log.e(TAG, "doInBackround " +  requestURL + " strings[0]: " + strings[0]);
            StringBuilder result = new StringBuilder();

            URL url = null;
            try {
                url = new URL(requestURL);
            } catch (MalformedURLException e) {
                Log.e("TAG", "" );
            }

            //Stream zusammenbauen



            try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))){
                String line; // platzhalter

                while((line=reader.readLine()) != null){

                    result.append(line);
                }

            } catch (IOException e) {


            }

            Log.d("... ", "doBackground" + result.toString());
            return result.toString();
        }
    }
}
