package com.id.socketio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static com.id.socketio.Constantes.TAG;

public class RegistroActivity extends AppCompatActivity {

    private Button setNickName;
    private EditText codigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        codigo = findViewById(R.id.userNickName);
        setNickName = findViewById(R.id.setNickName);


        codigo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    setNickName.setEnabled(true);
                    Log.d(TAG, "onTextChanged: ABLED");
                } else {
                    Log.d(TAG, "onTextChanged: DISABLED");
                    setNickName.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        setNickName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences("Login", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("comercio", Integer.parseInt(codigo.getText().toString()));
                editor.putInt("usuario", 3);
                editor.commit();


                Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
