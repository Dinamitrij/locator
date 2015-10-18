package lv.div.locator;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import lv.div.locator.actions.HttpReportSender;

public class Main extends AppCompatActivity {
//public class Main extends Application {

    public static boolean isService = false;

    public static BackgroundService mServiceInstance;

    protected static Main mInstance;


    public static Main getInstance() {
        return mInstance;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mServiceInstance = null;

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();


        // Construct our Intent specifying the Service
        Intent i = new Intent(Main.this, BackgroundService.class);
        // Add extras to the bundle
//        i.putExtra("foo", "bar");
        // Start the service
        startService(i);
        finish();
//////////////// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!


    }

    public void onCreate() {
//        super.onCreate();
        mInstance = this;
        mServiceInstance = null;

//        setContentView(R.layout.main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//

//        startService(new Intent(MainActivity.this, BackgroundService.class));
//        Intent startMain = new Intent(Intent.ACTION_MAIN);
//        startMain.addCategory(Intent.CATEGORY_HOME);
//        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(startMain);
//        isService = true;

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();


        // Construct our Intent specifying the Service
        Intent i = new Intent(Main.this, BackgroundService.class);
        // Add extras to the bundle
//        i.putExtra("foo", "bar");
        // Start the service
        startService(i);
        finish();

    }


//    @Override
//    protected void onResume() {
//        super.onResume();
//        stopService(new Intent(Main.this,
//                BackgroundService.class));
//        if (isService) {
//            isService = false;
//        }
//    }

    public void startup() {
//        wakeLock1(true);
        Intent i = new Intent(this, BackgroundService.class);
        startService(i);
    }
}
