package citofono.navile.com.citofono;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Wakelock

        TextView start = (TextView) findViewById(R.id.start);
        TextView stop = (TextView) findViewById(R.id.stop);
    updateTextView(R.id.start,"Avvia Servizio");
        updateTextView(R.id.stop,"Ferma Servizio");
        Button configuration = (Button) findViewById(R.id.configuration);
        configuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iinent= new Intent(MainActivity.this,ConfigurationActivity.class);
                startActivity(iinent);
                return;
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startNewService();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopNewService();
            }
        });

    }

    public void startNewService( ) {

        startService(new Intent(this, Services.class));
    }

    // Stop the  service
    public void stopNewService( ) {

        stopService(new Intent(this, Services.class));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void updateTextView(int text_id, String toThis) {
        TextView val = (TextView) findViewById(text_id);
        val.setText(toThis);
        return;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }




}