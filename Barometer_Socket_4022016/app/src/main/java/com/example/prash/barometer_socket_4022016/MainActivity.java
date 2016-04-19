package com.example.prash.barometer_socket_4022016;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button ServerSelect, ClientSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerSelect = (Button) findViewById(R.id.ServerSelect);
        ClientSelect = (Button) findViewById(R.id.ClientSelect);

        ServerSelect.setOnClickListener(this);
        ClientSelect.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ServerSelect:
                Intent INTENT_SERVER = new Intent(getApplicationContext(), ServerActivity.class);
                startActivity(INTENT_SERVER);
                finish();
                break;
            case R.id.ClientSelect:
                Intent INTENT_CLIENT = new Intent(getApplicationContext(), ClientActivity.class);
                startActivity(INTENT_CLIENT);
                finish();
                break;
        }
    }
}
