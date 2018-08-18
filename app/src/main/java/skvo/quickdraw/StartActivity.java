package skvo.classification;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by ml on 24.05.17.
 */

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

    private View btnStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartActivity.this, MainActivity.class));
            }
        });

    }
/*
    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        finish();
        System.exit(0);
    }*/
}
