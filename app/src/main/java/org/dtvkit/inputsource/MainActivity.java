package org.dtvkit.inputsource;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.testbinder).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
    }
}
