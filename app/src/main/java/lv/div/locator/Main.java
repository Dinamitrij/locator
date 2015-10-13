package lv.div.locator;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Main extends AppCompatActivity {

    public static boolean isService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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


        // Construct our Intent specifying the Service
        Intent i = new Intent(Main.this, BackgroundService.class);
        // Add extras to the bundle
//        i.putExtra("foo", "bar");
        // Start the service
        startService(i);
        finish();

    }


    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(Main.this,
                BackgroundService.class));
        if (isService) {
            isService = false;
        }
    }
}
