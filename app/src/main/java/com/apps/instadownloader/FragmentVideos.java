package com.apps.instadownloader;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adapter.MyAdapter;
import com.example.utils.DBHelper;
import com.example.utils.DownloadItems;
import com.example.utils.Methods;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FragmentVideos extends Fragment {

    DBHelper dbHelper;
    ArrayList<DownloadItems> arrayList;
    ArrayList<Integer> arrayList_id;
    ArrayList<DownloadItems> arrayList_temp;
    ArrayList<Integer> arrayList_id_temp;
    RecyclerView recyclerView;
    MyAdapter myAdapter;
    TextView textView;
    DownloadItems downloadItems;
    LinearLayout linearLayout;
    Boolean isAutoDismiss = true;
    Methods methods;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_download,null);

        dbHelper = new DBHelper(getActivity());
        try {
            dbHelper.createDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }

        methods = new Methods(getActivity());
        linearLayout = (LinearLayout)v.findViewById(R.id.ll);
        textView = (TextView)v.findViewById(R.id.textView_emptyView_latest);
        arrayList = new ArrayList<DownloadItems>();
        arrayList_id = new ArrayList<Integer>();
        arrayList_temp = new ArrayList<DownloadItems>();
        arrayList_id_temp = new ArrayList<Integer>();

        recyclerView = (RecyclerView)v.findViewById(R.id.recyclerView2);
        myAdapter = new MyAdapter(arrayList_temp,getActivity());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        loadCompletedData();

        recyclerView.setAdapter(myAdapter);
        methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                PopupMenu popupMenu = new PopupMenu(getActivity(),view, Gravity.BOTTOM);
                popupMenu.getMenuInflater().inflate(R.menu.popup,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId())
                        {
                            case R.id.item_popup_open:
                                methods.openItem(arrayList_temp.get(position).getVideo()+".mp4");
                                break;
                            case R.id.item_popup_share:
                                methods.shareItem(arrayList_temp.get(position).getVideo()+".mp4","video");
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int p = viewHolder.getAdapterPosition();
                final int id = arrayList_id_temp.get(p);
                downloadItems = arrayList_temp.get(p);
                myAdapter.remove(p);
                arrayList_id_temp.remove(p);
                methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());

                Snackbar.make(linearLayout,"Deleted",Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                isAutoDismiss = false;
                                arrayList_id_temp.add(id);
                                myAdapter.undo(p,downloadItems);
                                methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());
                            }
                        })
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if(isAutoDismiss) {
                                    deleteFromDatabase(id,p);
                                }
                                isAutoDismiss = true;
                                super.onDismissed(snackbar, event);
                            }
                        })
                        .show();
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);

        setHasOptionsMenu(true);
        return v;
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.downloads,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_clear:
                if(myAdapter.getItemCount() > 0) {
                    clearAll();
                } else {
                    Toast.makeText(getActivity(), "No items to clear", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadCompletedData()
    {
        Cursor c = dbHelper.getData("select * from data where type='video' and status = 1");
        if (c!=null && c.getCount()>0) {
            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++) {
                int id = c.getInt(c.getColumnIndex("id"));
                String name = c.getString(c.getColumnIndex("name"));
                String by = c.getString(c.getColumnIndex("by"));
                String image = c.getString(c.getColumnIndex("image"));
                String video = c.getString(c.getColumnIndex("video"));
                String type = c.getString(c.getColumnIndex("type"));
                String temp = c.getString(c.getColumnIndex("temp_url"));

                downloadItems = new DownloadItems(name, by, image, video, type, temp);
                arrayList.add(downloadItems);
                arrayList_id.add(id);
                c.moveToNext();
            }
        }

        File root = new File(Environment.getExternalStorageDirectory()+"/Insta Downloader/videos");
        if(root.exists()) {
            File[] flist = root.listFiles();
            if(flist.length>0) {
                for(int i=0;i<flist.length;i++) {
                    for(int j=0;j<arrayList.size();j++) {
                        if(flist[i].getName().equals(arrayList.get(j).getName()+".mp4")){
                            arrayList_temp.add(arrayList.get(j));
                            arrayList_id_temp.add(arrayList_id.get(j));
                            break;
                        }
                    }
                }
            }
        }
    }

    public void clearAll()
    {
        AlertDialog.Builder ai = new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to clear all")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        myAdapter.removeAll();
                        methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());
                        dbHelper.dml("delete from data where type = 'video' and status = 1");
                        Toast.makeText(getActivity(), "All items cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        ai.show();
    }

    public void deleteFromDatabase(int id,int pos)
    {
        dbHelper.dml("delete from data where id = '"+id+"'");
        methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());
    }

//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//        if(isVisibleToUser) {
//            arrayList.clear();
//            arrayList_id.clear();
//            loadCompletedData();
//            recyclerView.setAdapter(myAdapter);
//            methods.showHideEmptyView(textView,recyclerView,myAdapter.getItemCount());
//        }
//    }
}
