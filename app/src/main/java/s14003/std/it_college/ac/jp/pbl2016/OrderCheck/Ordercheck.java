package s14003.std.it_college.ac.jp.pbl2016.OrderCheck;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import s14003.std.it_college.ac.jp.pbl2016.MyDatabase;
import s14003.std.it_college.ac.jp.pbl2016.Product.ProductView;
import s14003.std.it_college.ac.jp.pbl2016.R;

public class Ordercheck extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private MyDatabase myHelper;
    private Handler mHandler;
    private List<OrderItem> orderItemList;

    private class OrderItem {
        public int orderId;
        public String mailAddress;
        public String productName;
        public int quantity;
        public int price;
        public int productId;
    }

    @Override
    public void onClick(View view) {

        String msg = "";
        int priceSum = 0;

        // 1. SQLiteDatabaseオブジェクトを取得
        SQLiteDatabase db = myHelper.getReadableDatabase();

        // 2. query()を呼び、検索を行う
        Cursor cursor =
                db.query(MyDatabase.TABLE_NAME_ORDER, null, null, null, null, null,
                        MyDatabase.ColumnsOrder.ORDERID + " ASC");

        // 3. 読込位置を先頭にする。falseの場合は結果0件
        if(!cursor.moveToFirst()){
            cursor.close();
            db.close();
            return;
        }

        // 4. 列のindex(位置)を取得する
        int _idIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.ORDERID);
        int nameIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.PRODUCTNAME);
        int priceIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.PRICE);
        int quantityIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.QUANTITY);

        // 5. 行を読み込む
        msg += "これらの商品を購入してもよろしいですか？\n\n";
        //int i = 0;
        do {
            ProductItem item = new ProductItem();
            item._id = cursor.getInt(_idIndex);
            item.name = cursor.getString(nameIndex);
            item.price = cursor.getInt(priceIndex);
            item.num = item.idx;

            Log.d("selectProductList",
                    "_id = " + item._id + "\n" +
                            "name = " + item.name + "\n" +
                            "price = " + item.price + "\n" +
                            "stock = " + item.num);


            msg += item.name + "  　　" + itemList.get(item._id).idx + "個 　　 " + item.price * itemList.get(item._id ).idx + "円\n";
            priceSum += (item.price * itemList.get(item._id ).idx);
            //msg += item.name + "  　　" + itemList.get(i).idx + "個 　　 " + item.price * itemList.get(i).idx + "円\n";
            //priceSum += (item.price * itemList.get(i).idx);

            // 読込位置を次の行に移動させる
            // 次の行が無い時はfalseを返すのでループを抜ける
            //i++;
        }while (cursor.moveToNext());

        // 6. Cursorを閉じる
        cursor.close();

        // 7. データベースを閉じる
        db.close();

        msg += "\n合計金額:  " + priceSum + "円";


        AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
        alertDlg.setMessage(msg);
        alertDlg.setTitle("確認");
        alertDlg.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // OKボタンクリック処理
                        insertProduct();
                        delete_stock();
                    }
                }
        );
        alertDlg.setNegativeButton(
                "キャンセル",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        alertDlg.create().show();

    }

    //データベースに発注したものを登録
    private void insertProduct() {

        SQLiteDatabase db = myHelper.getReadableDatabase();


        ProductItem item = new ProductItem();

        ContentValues values = new ContentValues();
        SharedPreferences spData = getSharedPreferences("Maildata", Context.MODE_PRIVATE);
        String mailAddr = spData.getString("Mailsave", "");

        for(int i = 0; i< itemList.size(); i++) {
            values.put(MyDatabase.ColumnsOrderAfter.MAILADDRESS, mailAddr);
            values.put(MyDatabase.ColumnsOrderAfter.PRODUCTNAME, itemList.get(i).name);
            //values.put(MyDatabase.ColumnsOrderAfter.PRICE, item.price * itemList.get(item._id + i).idx);
            values.put(MyDatabase.ColumnsOrderAfter.PRICE, itemList.get(i).price * itemList.get(i).idx);
            values.put(MyDatabase.ColumnsOrderAfter.QUANTITY, itemList.get(item._id + i).idx);

            // データベースに行を追加する
            Log.d("NOWDE", String.valueOf(itemList.get(i).name) + String.valueOf(itemList.get(i).price) +
                    String.valueOf(itemList.get(i).idx));
            long id = db.insert(MyDatabase.TABLE_NAME_ORDERAFTER, null, values);
            if (id == -1) {
                Log.d("Database", "Insert Failed");
                Toast.makeText(this, "注文確定できませんでした。", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("db", String.valueOf(itemList.get(item._id + i)._id));
                Toast.makeText(this, "注文確定できました。", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProductView.class));
            }
        }
        db.close();
    }

    public void delete_stock() {

        SQLiteDatabase db = myHelper.getReadableDatabase();

        Cursor cursor = db.query(MyDatabase.TABLE_NAME_PRODUCTS, null, null, null, null, null,
                MyDatabase.ColumnsOrder._ID + " ASC");

        int stock_num = cursor.getColumnIndex(MyDatabase.ColumnsProducts.STOCK);

        ProductItem item = new ProductItem();
        ContentValues values = new ContentValues();


        for(int i = 0; i< itemList.size(); i++) {

            values.put(MyDatabase.ColumnsProducts.STOCK, stock_num - itemList.get(item._id + i).idx);

            // データベースに行を追加する
            long id = db.insert(MyDatabase.TABLE_NAME_PRODUCTS, null, values);

        }
        db.close();

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    private class ProductItem {
        int _id;
        String name;
        int price;
        int num;
        int idx = 1;
    }

    private List<ProductItem> itemList;
    private ItemAdapter adapter;


    private Spinner productSpinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordercheck);

        myHelper = new MyDatabase(this);

        mHandler = new Handler();

//        initTable();
        SharedPreferences spData = getSharedPreferences("Maildata", Context.MODE_PRIVATE);
        String mailAddr = spData.getString("Mailsave", "");
        Log.d("NOW", "Ordercheck.onCreate " + mailAddr);

        orderItemList = new ArrayList<>();

        itemList = new ArrayList<ProductItem>();

        adapter = new ItemAdapter(getApplicationContext(), 0, itemList);

        adapter.setNotifyOnChange(true);
        ListView listView =
                (ListView)findViewById(R.id.listProducts);
        listView.setAdapter(adapter);
        setProductData();


        (new Thread(new Runnable() {
            @Override
            public void run() {
                setProductData();

                //メインスレッドのメッセージキューにメッセージを登録します。
                mHandler.post(new Runnable (){
                    //run()の中の処理はメインスレッドで動作されます。
                    public void run(){
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        })).start();


        listView.setOnItemClickListener(this);

        Button btn_cancel = (Button)findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button btnBuy = (Button)findViewById(R.id.btnBuy);
        btnBuy.setOnClickListener(this);


    }

    //Toastで表示
    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void setProductData(){
        selectProductList();

    }

    private class ItemAdapter extends ArrayAdapter<ProductItem> {
        private LayoutInflater inflater;

        public ItemAdapter(Context context, int resource,
                           List<ProductItem> objects) {
            super(context, resource, objects);
            inflater =
                    (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            Log.d("ProductList", "getView");
            Log.d("product", String.valueOf(position));


            View view = inflater.inflate(R.layout.order_row, null, false);
            TextView nameView = (TextView)view.findViewById(R.id.name);
            TextView priceView = (TextView)view.findViewById(R.id.price);


            ArrayAdapter<String> adapter = new ArrayAdapter<>(Ordercheck.this, R.layout.my_spinner_item);

            for (int i = 1; i <= 100/*ここに数量を入れる*/; i++) {
                adapter.add(String.valueOf(i));
            }
            adapter.setDropDownViewResource(R.layout.my_spinner_drop_down_item);
            productSpinner = (Spinner)view.findViewById(R.id.productspinner);
            productSpinner.setAdapter(adapter);
            productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Spinner spinner = (Spinner) adapterView;
                    Log.d("onItemSelected: i", String.valueOf(i));
                    Log.d("onItemSelected", (String)spinner.getSelectedItem());

                    ProductItem Item = new ProductItem();

                    Item._id = itemList.get(position)._id;
                    Item.name = itemList.get(position).name;
                    Item.price = itemList.get(position).price;
                    Item.idx = Integer.parseInt(spinner.getSelectedItem().toString());

                    itemList.set(position, Item);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            adapter.setNotifyOnChange(true);

            ProductItem item = getItem(position);
            nameView.setText(item.name);
            priceView.setText(String.valueOf(item.price));
//            quantityView.setText(String.valueOf(item.num));
            return view;

        }

    }

    private class ProductDbItem {
        String name;
        int price;
        int num;
    }

    private List<ProductDbItem> itemDbList;

    private void selectProductList() {
        // 1. SQLiteDatabaseオブジェクトを取得
        SQLiteDatabase db = myHelper.getReadableDatabase();

        // 2. query()を呼び、検索を行う
        // メールアドレス、商品名、値段、数量を指定
        String[] cols = {
                MyDatabase.ColumnsOrder.MAILADDRESS,
                MyDatabase.ColumnsOrder.PRODUCTNAME,
                MyDatabase.ColumnsOrder.PRICE,
                MyDatabase.ColumnsOrder.QUANTITY,
                MyDatabase.ColumnsOrder.ORDERID
        };
        // 発注者のメールアドレスとログインアドレスが同じレコードのみを指定
        String selection = MyDatabase.ColumnsOrder.MAILADDRESS + " = ?";
        SharedPreferences data = getSharedPreferences("Maildata", Context.MODE_PRIVATE);
        String mailAddr = data.getString("Mailsave", "");
        Log.d("NOW", "Ordercheck.select " + mailAddr);
        String[] selectionArgs = {mailAddr};
        Cursor cursor = db.query(MyDatabase.TABLE_NAME_ORDER, cols, selection, selectionArgs, null, null,
                MyDatabase.ColumnsOrder.ORDERID + " ASC");

        // 3. 読み込み位置を先頭にする、falseの場合は結果０件
        if (!cursor.moveToFirst()) {
            Log.d("NOW", "ordercheckclose" + mailAddr);
            cursor.close();
            db.close();
            return;
        }

        // 4. 列のindex(位置)を取得する
        int productnameIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.PRODUCTNAME);
        int priceIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.PRICE);
        int quantityIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.QUANTITY);
        int orderIdIndex = cursor.getColumnIndex(MyDatabase.ColumnsOrder.ORDERID);

        // 5. 行を読み込む
        itemList.removeAll(itemList);
        do {
            ProductItem item = new ProductItem();
            item.name = cursor.getString(productnameIndex);
            item.price = cursor.getInt(priceIndex);
            item.num = cursor.getInt(quantityIndex);
            item._id = cursor.getInt(orderIdIndex);

            itemList.add(item);
        } while (cursor.moveToNext());

        // 6. Cursorを閉じる
        cursor.close();

        // 7. データベースを閉じる
        db.close();
    }


}