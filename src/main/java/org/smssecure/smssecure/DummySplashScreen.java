package org.smssecure.smssecure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DummySplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate (@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_splash);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, ConversationListActivity.class));
            finish();
        }, 2500);
    }
}
