package com.id.socketio.Utilidades;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.id.socketio.Constantes.TAG;

public class Utilidades {

    public static String obtenerFecha(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date()); // Salida:  2019-10-28 15:24:55
    }

    public static String convertirImgString(Bitmap bitmap){
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,array);
        byte[] imagenByte = array.toByteArray();
        String imagenString = Base64.encodeToString(imagenByte, Base64.DEFAULT);


        return imagenString;
    }

    public static String convertirAudioString(String pathAudio) {

        String audioString = "";
        byte[] audioBytes;
        try {
            // Log.d(TAG, "El peso del archivo es: " + new File(pathAudio).length());
            // 25 KB aprox para 15 segundos

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(pathAudio));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
            audioBytes = baos.toByteArray();

            audioString = Base64.encodeToString(audioBytes, Base64.DEFAULT);

        } catch (Exception e) {
            Log.d(TAG, "Ocurrió un error al codificar audio a Base64");
        }
        return audioString;
    }


    public Boolean puedeGenerarNuevaAlerta(Context context){
        // Antes de generar nuevo reporte ver cuando fue el ultimo que hizo
        SharedPreferences preferences = context.getSharedPreferences("UltimoReporte", Context.MODE_PRIVATE);
        if (preferences.contains("ultimoReporte") && preferences.contains("fechaUltimoReporte")){

            //Log.d(TAG, "SI ENCONTRO LAS PREFERENCIAS GUARDADAS");
            int ultimoReeporte = preferences.getInt("ultimoReporte", 0 );
            long fechaUltimoReporte = preferences.getLong("fechaUltimoReporte", 0);
            Log.d(TAG, "El ultimo reporte fue: " + ultimoReeporte);

            if(ultimoReeporte == 0){
                // Se realiza envio de reporte nuevo
                return true;
            } else {
                long fechaActual = System.currentTimeMillis();
                long diferencia = fechaActual - fechaUltimoReporte;

                Log.d(TAG, "La diferencia de fechas es: " + diferencia);
                if(diferencia >= 1 && diferencia <= 30000){
                    //     1,000 = 1 seg
                    //    60,000 = 1 minuto
                    // 1,800,000 = 30 minutos
                    // 3,600,000 = 1 hora

                    // Unicamente se genera un "Volvio a presionar el boton"
                    return false;
                } else{
                    // Se realiza envio correctamente
                    return true;
                }
            }
        } else {
            Log.d(TAG, "NO ENCONTRO LAS PREFERENCIAS GUARDADAS");
            // No hay ningun reporte por lo tanto si se puede generar reporte nuevo
            return true;
        }
    }




}
