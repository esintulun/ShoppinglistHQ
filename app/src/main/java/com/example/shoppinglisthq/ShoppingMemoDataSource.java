package com.example.shoppinglisthq;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ShoppingMemoDataSource {

    private static final String TAG = ShoppingMemoDataSource.class.getSimpleName();
    private SQLiteDatabase database;
    private ShoppingMemoDbHelper dbHelper;

    private String[] columns = {
            ShoppingMemoDbHelper.COLUMN_ID,
            ShoppingMemoDbHelper.COLUMN_PRODUCT,
            ShoppingMemoDbHelper.COLUMN_QUANTITY,
            ShoppingMemoDbHelper.COLUMN_CHECKED
    };

    public ShoppingMemoDataSource(Context context) {
        Log.d(TAG, "DataSource erzeugt den dbHelper.");
        dbHelper = new ShoppingMemoDbHelper(context);
    }

    /**
     *
     * @param product
     * @param quantity
     * @return
     *
     * INSERT INTO table_name (column1, column2, column3, ...)
     * VALUES (value1, value2, value3, ...);
     */
    public ShoppingMemo createShoppingMemo(String product, int quantity, boolean checked) {

        ContentValues values = new ContentValues();
        values.put(ShoppingMemoDbHelper.COLUMN_PRODUCT, product);
        values.put(ShoppingMemoDbHelper.COLUMN_QUANTITY, quantity);
        values.put(ShoppingMemoDbHelper.COLUMN_CHECKED, checked);


        long insertId = database.insert(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST, null, values);

        Cursor cursor = database.query(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST, columns,
                ShoppingMemoDbHelper.COLUMN_ID + "=" + insertId,
                null, null, null, null);


        cursor.moveToFirst();
        ShoppingMemo shoppingMemo = cursorToShoppingMemo(cursor);
        cursor.close();
        return shoppingMemo;
    }

    public ShoppingMemo updateShoppingMemo(long id, String newProduct, int newQuantity, boolean newChecked) {

        // boolean zu int
        int intValueChecked = newChecked? 1: 0;
        ContentValues values = new ContentValues();

        values.put(ShoppingMemoDbHelper.COLUMN_PRODUCT, newProduct);
        values.put(ShoppingMemoDbHelper.COLUMN_QUANTITY, newQuantity);
        values.put(ShoppingMemoDbHelper.COLUMN_CHECKED, intValueChecked);


        //UPDATE Customers SET ContactName = 'Alfred Schmidt', City= 'Frankfurt'WHERE CustomerID = 1;
        database.update(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST,
                values,
                ShoppingMemoDbHelper.COLUMN_ID + "=" + id,
                null);


        // SELECT * from TABLE_SHOPPING_LIST WHERE id = id;  selectionsArgs= null ist *
        Cursor cursor = database.query(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST, columns, ShoppingMemoDbHelper.COLUMN_ID + "=" + id,
                null, null, null, null);

        cursor.moveToFirst();
        ShoppingMemo shoppingMemo = cursorToShoppingMemo(cursor);
        cursor.close();

        return shoppingMemo;

    }

    private ShoppingMemo cursorToShoppingMemo(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(ShoppingMemoDbHelper.COLUMN_ID);
        int idProduct = cursor.getColumnIndex(ShoppingMemoDbHelper.COLUMN_PRODUCT);
        int idQuantity = cursor.getColumnIndex(ShoppingMemoDbHelper.COLUMN_QUANTITY);
        int idChecked = cursor.getColumnIndex(ShoppingMemoDbHelper.COLUMN_CHECKED);

        String product = cursor.getString(idProduct);
        int quantity = cursor.getInt(idQuantity);
        long id = cursor.getLong(idIndex);

        int inValueChecked = cursor.getInt(idChecked);

        // nun muss wieder boelean gemacht werden

        boolean isChecked = inValueChecked != 0;

        ShoppingMemo shoppingMemo = new ShoppingMemo(product, quantity, id, isChecked);
        return shoppingMemo;
    }

    /*
        DELETE Customer WHERE Id = 21;
     */
    public void deleteShoppingMemo(ShoppingMemo shoppingMemo) {

        long id = shoppingMemo.getId();
        database.delete(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST, ShoppingMemoDbHelper.COLUMN_ID + "=" + id,
                null);

        Log.d(TAG, "Eintrag gelöscht! ID: " + id + " Inhalt: " + shoppingMemo.toString());
    }

    public List<ShoppingMemo> getAllShoppingMemos() {
        List<ShoppingMemo> shoppingMemoList = new ArrayList<>();

        Cursor cursor = database.query(ShoppingMemoDbHelper.TABLE_SHOPPING_LIST,
                columns, null, null, null, null, null);

        cursor.moveToFirst();
        ShoppingMemo shoppingMemo;

        while (!cursor.isAfterLast()) {
            shoppingMemo = cursorToShoppingMemo(cursor);
            shoppingMemoList.add(shoppingMemo);
            Log.d(TAG, "ID: " + shoppingMemo.getId() + ", Inhalt: " + shoppingMemo.toString());
            cursor.moveToNext();
        }

        cursor.close();
        return shoppingMemoList;
    }

    public void open() {

        Log.d(TAG, "Eine Referenz auf die Datenbank wird jetzt angefragt.");
        database = dbHelper.getWritableDatabase();
        Log.d(TAG, "Datenbank-Referenz erhalten. Pfad zur Datenbank: " + database.getPath());

    }

    public void close() {
        dbHelper.close();
        Log.d(TAG, "Datenbank mit Hilfe des DbHelpers geschlossen.");
    }

}
