package citofono.navile.com.citofono;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import citofono.navile.com.citofono.data.Preferences;

/**
 * Created by raeli on 14/11/2015.
 */
public class ConfigurationActivity extends ActionBarActivity implements View.OnClickListener{
    SharedPreferences preferences;
    EditText millsBeforeShutdown;
    EditText threshold;
    Button save;
    CheckBox sensors ;
    CheckBox motions ;

    public ConfigurationActivity(){
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration);
         preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        millsBeforeShutdown = (EditText) findViewById(R.id.millsBeforeShutdown);
        threshold = (EditText) findViewById(R.id.threshold);
        save = (Button) findViewById(R.id.save);
        sensors = (CheckBox) findViewById(R.id.sensors);
        motions = (CheckBox) findViewById(R.id.motions);
        save.setOnClickListener(this);
        boolean state = preferences.getBoolean(Preferences.sensors, false);
        boolean motion = preferences.getBoolean(Preferences.motions, false);
        int millsBeforeShutdownNumber = preferences.getInt(Preferences.millsBeforeShutdown, -1);
        int thresholdNumber = preferences.getInt(Preferences.threshold, -1);
        if(state)
            sensors.setChecked(true);
        if(motion)
            motions.setChecked(true);
        if(millsBeforeShutdownNumber != -1 && thresholdNumber != -1){
            millsBeforeShutdown.setText(""+millsBeforeShutdownNumber);
            threshold.setText(""+thresholdNumber);
        }
        else{
            millsBeforeShutdown.setText("not setted");
            threshold.setText("not setted");
        }

    }

    @Override
    public void onClick(View v) {
        if(v == save){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Preferences.millsBeforeShutdown, Integer.parseInt(millsBeforeShutdown.getText().toString()));
            editor.putInt(Preferences.threshold, Integer.parseInt(threshold.getText().toString()));
            editor.putBoolean(Preferences.sensors, sensors.isChecked());
            editor.putBoolean(Preferences.motions, motions.isChecked());
            editor.commit();
            finish();
            return;
        }
    }
}
