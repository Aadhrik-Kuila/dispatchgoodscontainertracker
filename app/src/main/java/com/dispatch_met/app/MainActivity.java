package com.dispatch_met.app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.encoder.QRCode;
import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.adapter.ListViewAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    FirebaseFirestore container_management = FirebaseFirestore.getInstance();
    HashMap<String, Boolean> qrcodes = new HashMap<String, Boolean>();

    static SQLiteDatabase myDatabase;
    Date currentTime;
    SimpleDateFormat df;
    ListView qrDetailList;
    List<QrBeanModel> QrObject = new ArrayList<>();
    QrDetailAdapter qrDetailAdapter;
    Cursor c;
    SearchView inputSearch;
    List<QrBeanModel> tempList = new ArrayList<>();

    /** ---- Firebase methods ---- **/

    protected void dropoff() {

    }

    protected void pickup() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        for (int i = 0; i < 10; i++) {
            qrcodes.put("dispatch-goods-" + Integer.valueOf(i), false);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intializeViews();
        qrDetailAdapter = new QrDetailAdapter( this ,R.layout.qr_list_items,QrObject);
        qrDetailList.setAdapter(qrDetailAdapter);
        qrDetailAdapter.notifyDataSetChanged();
        performSql();

        //Main code for search view
        tempList.addAll(QrObject);
        inputSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                QrObject.clear();
                for(QrBeanModel qr: tempList){
                    if(qr.getQrText().contains(s)){
                        //contains
                        QrObject.add(qr);
                    } else if(s.length() == 0) {
                        Toast.makeText(getApplicationContext(),"list is clear", Toast.LENGTH_LONG).show();
                        tempList.addAll(QrObject);
                    }
                }
                qrDetailAdapter.notifyDataSetChanged();
                return true;
            }
        });



        //To perform delete on swipe in the list
        final SwipeToDismissTouchListener<ListViewAdapter> touchListener =
                new SwipeToDismissTouchListener<>(
                        new ListViewAdapter(qrDetailList),
                        new SwipeToDismissTouchListener.DismissCallbacks<ListViewAdapter>() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListViewAdapter view, int position) {
                                qrDetailAdapter.remove(position);
                            }
                        });

        qrDetailList.setOnTouchListener(touchListener);
        qrDetailList.setOnScrollListener((AbsListView.OnScrollListener) touchListener.makeScrollListener());
        qrDetailList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (touchListener.existPendingDismisses()) {
                    touchListener.undoPendingDismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Position " + position, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public void intializeViews(){
        qrDetailList = (ListView) findViewById(R.id.qrDetailList);
        qrDetailList.setTextFilterEnabled(true);
        inputSearch = (SearchView) findViewById(R.id.inputSearch);
    }

    public void scan(){
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                String scannedResult = result.getContents();
                String status = "unknown item";
                if (qrcodes.containsKey(scannedResult)) {
                    if (qrcodes.get(scannedResult)) {
                        status = "scanned out";
                        qrcodes.put(scannedResult, false);
                    } else {
                        status = "scanned in";
                        qrcodes.put(scannedResult, true);
                    }
                }

                currentTime = Calendar.getInstance().getTime();
                df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
                String formattedDate = df.format(currentTime);
                String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
                System.out.println(formattedDate);
                String sql = "INSERT INTO lastfourth (name, date , spec, status) VALUES (? , ?, ?, ?)";
//                String sql = "INSERT INTO lastfourth (name, date , spec, status) VALUES (? , ?, ?, ?)";
                SQLiteStatement statement = myDatabase.compileStatement(sql);
                QrBeanModel qrBeanModel = new QrBeanModel(scannedResult,formattedDate,d,status);
                QrObject.add(qrBeanModel);
                qrDetailAdapter.notifyDataSetChanged();
                statement.bindString(1,scannedResult);
                statement.bindString(2,formattedDate);
                statement.bindString(3,d);
                statement.bindString(4, status);
                statement.execute();
//                Toast.makeText(getApplicationContext(),"ADDED to database",Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.scanQrCode:
                scan();
                break;
            case R.id.sortByDate:
                orderByDate();
                break;
            case R.id.sortByName:
                orderByName();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void orderByDate(){
        Toast.makeText(getApplicationContext(),"order by date working", Toast.LENGTH_LONG).show();
        c = myDatabase.rawQuery("SELECT * FROM lastfourth ORDER BY date Desc",null);
        int nameIndex = c.getColumnIndex("name");
        int dateIndex = c.getColumnIndex("date");
        int dateIDIndex = c.getColumnIndex("spec");
        int statusIndex = c.getColumnIndex("status");

        if(c.moveToFirst()){
            do{
                Log. i ( "user-name" ,c.getString(nameIndex));
                Log.i("date id ",c.getString(dateIDIndex));
                Log.i("status", c.getString(statusIndex));
                QrBeanModel qrBeanModel = new QrBeanModel(c.getString(nameIndex),c.getString(dateIndex),c.getString(dateIDIndex),
                        c.getString(statusIndex));
                QrObject.add(qrBeanModel);
                qrDetailAdapter.notifyDataSetChanged();
                Log. i ( "user-age" ,c.getString(dateIndex));
            }while (c.moveToNext());
        }
    }

    public void orderByName(){
        Toast.makeText(getApplicationContext(),"order by date working", Toast.LENGTH_LONG).show();
        c = myDatabase.rawQuery("SELECT * FROM lastfourth ORDER BY name",null);
        int nameIndex = c.getColumnIndex("name");
        int dateIndex = c.getColumnIndex("date");
        int dateIDIndex = c.getColumnIndex("spec");
        int statusIndex = c.getColumnIndex("status");


        if(c.moveToFirst()){
            do{
                Log. i ( "user-name" ,c.getString(nameIndex));
                Log.i("date id ",c.getString(dateIDIndex));
                Log.i("status", c.getString(statusIndex));
                QrBeanModel qrBeanModel = new QrBeanModel(c.getString(nameIndex),c.getString(dateIndex),
                        c.getString(dateIDIndex), c.getString(statusIndex));
                QrObject.add(qrBeanModel);
                qrDetailAdapter.notifyDataSetChanged();
                Log. i ( "user-age" ,c.getString(dateIndex));
            }while (c.moveToNext());
        }
    }

    public static void sendUniqueKey(String s){
        String sql = "DELETE FROM lastfourth WHERE spec = ? ";
        SQLiteStatement statement = myDatabase.compileStatement(sql);
        statement.bindString(1,s);
        statement.execute();
        Log.i("tag",s+"Deleted");
       // MainActivity m = new MainActivity();
       // m.performSql();
    }

    public void performSql(){
        myDatabase = this .openOrCreateDatabase( "Users" , MODE_PRIVATE , null );

        //Creating the table if not exists
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS lastfourth (name VARCHAR , date VARCHAR , spec VARCHAR, status VARCHAR)");
        showDatabaseInList();
    }

    public void showDatabaseInList(){

        try {
            c = myDatabase.rawQuery("SELECT * FROM lastfourth",null);

            int nameIndex = c.getColumnIndex("name");
            int dateIndex = c.getColumnIndex("date");
            int dateIDIndex = c.getColumnIndex("spec");
            int statusIndex = c.getColumnIndex("status");

            if(c.moveToFirst()){
                do{
                    Log. i ( "user-name" ,c.getString(nameIndex));
                    Log.i("date id ",c.getString(dateIDIndex));
                    Log.i("status", c.getString(statusIndex));
                    QrBeanModel qrBeanModel = new QrBeanModel(c.getString(nameIndex),c.getString(dateIndex),
                            c.getString(dateIDIndex), c.getString(statusIndex));
                    QrObject.add(qrBeanModel);
                    qrDetailAdapter.notifyDataSetChanged();
                    Log. i ( "user-age" ,c.getString(dateIndex));
                }while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
