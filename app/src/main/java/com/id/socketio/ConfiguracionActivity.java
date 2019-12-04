package com.id.socketio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.id.socketio.Servicios.ServicioNotificacion;

public class ConfiguracionActivity extends AppCompatActivity {

    private Switch switchServicioActivo;
    private Boolean notificacionActiva = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);
        switchServicioActivo = findViewById(R.id.switchServicioActivo);
        obtenerPreferencias();
        switchServicioActivo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    iniciarServicioPersistente();
                } else {
                    detenerServicioPersistente();
                }
            }
        });

    }
    private void obtenerPreferencias(){
        SharedPreferences preferences = getApplication().getSharedPreferences("NotificacionPersistente", Context.MODE_PRIVATE);
        if (preferences.contains("notificacionActiva")){
            notificacionActiva = preferences.getBoolean("notificacionActiva" ,false);
            switchServicioActivo.setChecked(notificacionActiva);
        } else {
            notificacionActiva = false;
            switchServicioActivo.setChecked(notificacionActiva);
        }
    }
    public void iniciarServicioPersistente(){
        Intent notificationIntent = new Intent(getApplication(), ServicioNotificacion.class);
        getApplication().startService(notificationIntent);
        notificacionActiva = true;
        actualizarPreferencias(notificacionActiva);
    }
    public void detenerServicioPersistente(){
        Intent notificationIntent = new Intent(getApplication(), ServicioNotificacion.class);
        getApplication().stopService(notificationIntent);
        notificacionActiva = false;
        actualizarPreferencias(notificacionActiva);
    }

    private void actualizarPreferencias(boolean nuevoValor){
        SharedPreferences preferences = getApplication().getSharedPreferences("NotificacionPersistente", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("notificacionActiva",nuevoValor);
        editor.commit();
    }


}