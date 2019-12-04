package com.id.socketio.Utilidades;

import android.content.Context;

public class SharedPreferences {

    public String guardarEstatusCamaras(Context context, Boolean camara_trasera, Boolean camara_frontal){

        android.content.SharedPreferences preferences = context.getSharedPreferences("Camaras", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("trasera", camara_trasera);
        editor.putBoolean("frontal", camara_frontal);
        editor.commit();

        if(camara_frontal && camara_trasera){
            return "Cámara frontal y cámara trasera validadas con éxito.";
        } else if (camara_frontal && !camara_trasera ){
            return "No se encontro cámara trasera";
        } else if (!camara_frontal && camara_trasera){
            return "No se encontro cámara frontal";
        } else {
            return "No se encontro ninguna cámara";
        }
    }

    public static Boolean guardarUltimoReporte(Context context, int reporteCreado){
        try{
            android.content.SharedPreferences preferences = context.getSharedPreferences("UltimoReporte", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();
            int ultimoReporte = reporteCreado;
            long fechaUltimoReporte = System.currentTimeMillis();

            editor.putInt("ultimoReporte", ultimoReporte);
            editor.putLong("fechaUltimoReporte", fechaUltimoReporte);
            editor.commit();
            return true;
        } catch (Exception e){
            return false;
        }
    }
}
