package com.sargent.mark.todolist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;


import com.sargent.mark.todolist.data.Contract;
import com.sargent.mark.todolist.data.DBHelper;

import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements AddToDoFragment.OnDialogCloseListener, UpdateToDoFragment.OnUpdateDialogCloseListener{

    private RecyclerView rv;
    private FloatingActionButton button;
    private DBHelper helper;
    private Cursor cursor;
    private SQLiteDatabase db;
    ToDoListAdapter adapter;
    private final String TAG = "mainactivity";
    private Menu menu;
    private boolean isMarkingDone = false;
    public static HashSet<Long> toBeMarkedDone = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "oncreate called in main activity");
        button = (FloatingActionButton) findViewById(R.id.addToDo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                AddToDoFragment frag = new AddToDoFragment();
                frag.show(fm, "addtodofragment");
            }
        });
        rv = (RecyclerView) findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (db != null) db.close();
        if (cursor != null) cursor.close();
    }

    @Override
    protected void onStart() {
        super.onStart();

        helper = new DBHelper(this);
        db = helper.getWritableDatabase();
        cursor = getAllItems(db);

        adapter = new ToDoListAdapter(cursor, new ToDoListAdapter.ItemClickListener() {

            @Override
            public void onItemClick(int pos, String description, String duedate, long id, String category, View item) {
                Log.d(TAG, "item click id: " + id);
                String[] dateInfo = duedate.split("-");
                int year = Integer.parseInt(dateInfo[0].replaceAll("\\s",""));
                int month = Integer.parseInt(dateInfo[1].replaceAll("\\s",""));
                int day = Integer.parseInt(dateInfo[2].replaceAll("\\s",""));


                if(!isMarkingDone) {
                    FragmentManager fm = getSupportFragmentManager();

                    UpdateToDoFragment frag = UpdateToDoFragment.newInstance(year, month, day, description, id, category);
                    frag.show(fm, "updatetodofragment");
                }
                else{

                    //add all ids to a set and toggle bg color

                    if(!toBeMarkedDone.contains(id)){
                        toBeMarkedDone.add(id);
                        item.setBackgroundColor(Color.rgb(155, 255, 220));

                    }
                    else{
                        toBeMarkedDone.remove(id);
                        item.setBackgroundColor(Color.WHITE);
                    }

                }
            }
        });

        rv.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                Log.d(TAG, "passing id: " + id);
                removeToDo(db, id);
                adapter.swapCursor(getAllItems(db));
            }
        }).attachToRecyclerView(rv);
    }

    @Override
    public void closeDialog(int year, int month, int day, String description, String category, int isDone) {
        //added category, isDone as params
        addToDo(db, description, formatDate(year, month, day), category, isDone);
        cursor = getAllItems(db);
        adapter.swapCursor(cursor);
    }

    public String formatDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month + 1, day);
    }



    private Cursor getAllItems(SQLiteDatabase db) {
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );

    }

    //added new cursor to filter by x category

    private Cursor filterBy(SQLiteDatabase db, String category){
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME, null, Contract.TABLE_TODO.COLUMN_NAME_CATEGORY + "=" + "'" + category + "'", null,
                null, null, null,null
        );

    }

    private long addToDo(SQLiteDatabase db, String description, String duedate, String category, int isDone) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_IS_DONE, isDone);
        return db.insert(Contract.TABLE_TODO.TABLE_NAME, null, cv);
    }

    private boolean removeToDo(SQLiteDatabase db, long id) {
        Log.d(TAG, "deleting id: " + id);
        return db.delete(Contract.TABLE_TODO.TABLE_NAME, Contract.TABLE_TODO._ID + "=" + id, null) > 0;
    }


    private int updateToDo(SQLiteDatabase db, int year, int month, int day, String description, long id, String category){

        String duedate = formatDate(year, month - 1, day);

        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);

        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }

    @Override
    public void closeUpdateDialog(int year, int month, int day, String description, long id, String category) {
        updateToDo(db, year, month, day, description, id, category);
        adapter.swapCursor(getAllItems(db));
    }

    //menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;

        menu.getItem(0).getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        menu.getItem(1).getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        return true;
    }

    private void setDone(){
        setTitle("Select tasks to mark done");
        isMarkingDone = true;
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        menu.getItem(2).setVisible(true);

    }

    private void doneSelecting(){
        isMarkingDone = false;
        setTitle(getResources().getString(R.string.app_name));
        menu.getItem(0).setVisible(true);
        menu.getItem(1).setVisible(true);
        menu.getItem(2).setVisible(false);

        //query

        for(Long id: toBeMarkedDone){
            ContentValues args = new ContentValues();
            args.put(Contract.TABLE_TODO.COLUMN_NAME_IS_DONE, 1);
            db.update(Contract.TABLE_TODO.TABLE_NAME, args, Contract.TABLE_TODO._ID + "=" + id, null);
        }

        //reset bg color
        toBeMarkedDone.clear();

        for(int i=0; i < rv.getChildCount(); i++){
            LinearLayout item =(LinearLayout) rv.getChildAt(i);
            item.setBackgroundColor(Color.WHITE);
        }
        adapter.notifyDataSetChanged();

        //refresh
        cursor = getAllItems(db);
        adapter.swapCursor(cursor);


    }


    //call filterBy based on option selected

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_done :
                setDone();
                break;
            case R.id.done_setting_done :
                doneSelecting();
                break;
            case R.id.all :
                cursor = getAllItems(db);
                break;
            case R.id.school :
                cursor = filterBy(db, "School");
                break;
            case R.id.work :
                cursor = filterBy(db, "Work");
                break;
            case R.id.free_time :
                cursor = filterBy(db, "Free time");
                break;
            case R.id.family :
                cursor = filterBy(db, "Family");
                break;
            default :
        }

        //prevent crash, this should only be called when submenu is selected
        if(id != R.id.action_filter && id != R.id.action_done && id != R.id.done_setting_done) {

            if(item.getTitle().equals("All")){
                setTitle(getResources().getString(R.string.app_name));
            }else{
                setTitle("Filter: " + item.getTitle());
            }

            adapter.swapCursor(cursor);
        }

        return super.onOptionsItemSelected(item);
    }
}
