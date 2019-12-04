package com.id.socketio.Servicios;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.id.socketio.Broadcast.BotonazoReceiver;
import com.id.socketio.R;
import com.id.socketio.Utilidades.EnviarImagenes;
import com.id.socketio.Utilidades.Utilidades;

import static com.id.socketio.Constantes.CHANNEL_ID;
import static com.id.socketio.Constantes.ID_SERVICIO_PANICO;
import static com.id.socketio.Constantes.TAG;

public class ServicioNotificacion extends Service {

    // VARIABLES PARA USO DE MULTIMEDIA
    ImageTraseraResultReceiver resultTReceiver;
    ImageFrontalResultReceiver resultFReceiver;
    public static final int SUCCESS = 1;
    public static final int ERROR = 0;
    Boolean procesoImagenFrontal = false;
    Boolean procesoImageTrasera = false;
    String IMAGEN_FRONTAL = "Ninguna";
    String IMAGEN_TRASERA = "Ninguna";
    String FECHA_FRONTAL = "";
    String FECHA_TRASERA = "";

    // VARIABLES PARA REPORTE CREADO
    private int reporteCreado = 0;
    private Boolean envioArchivos = false;
    private Boolean respondioServer = false;
    private Boolean datosComercio = false;
    private String message = "";

    Utilidades utilidades = new Utilidades();
    BotonazoReceiver botonazoReceiver;
    private int contador;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "¡Se creó servicio!", Toast.LENGTH_SHORT).show();

        contador = 0;
        botonazoReceiver = new BotonazoReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");

        // Registrar el BroadCast para escuchar los tres botonazos
        registerReceiver(botonazoReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiverBotonazo, new IntentFilter("botonActivado"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        crearNotificacionPersistente();
        return super.onStartCommand(intent, flags, startId);
    }

    // *********************************************************
    // RECIBE LA RESPUESTA DEL BROADCAST BOTONAZOS
    // *********************************************************
    private BroadcastReceiver broadcastReceiverBotonazo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            contador++;
            Bundle parametros = intent.getExtras();
            Boolean respuesta = parametros.getBoolean("Activado", false);

            if(respuesta){
                //Log.d(TAG, "Dice el broadcast que detecto tres botonazos... " + contador);
                if(utilidades.puedeGenerarNuevaAlerta(getApplicationContext())){
                    LocalBroadcastManager.getInstance(getApplication()).registerReceiver(broadcastReceiverGenerarAlerta, new IntentFilter("generarAlertaService"));
                    iniciarServicioGenerarAlerta();
                } else{
                    // Crear servicio especifico para el servicio de emit() contador
                    Log.d(TAG, "CONTADOR ++ ");
                }
            }
        }
    };

    // *********************************************************
    // RECIBE LA RESPUESTA DEL SERVICIO DE CREAR ALERTA
    // *********************************************************
    private BroadcastReceiver broadcastReceiverGenerarAlerta = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle parametros = intent.getExtras();
            reporteCreado = parametros.getInt("reporteCreado", 0);
            envioArchivos = parametros.getBoolean("envioArchivos", false);
            respondioServer = parametros.getBoolean("respondioServer", false);
            datosComercio = parametros.getBoolean("datosComercio", false);
            message = parametros.getString("message");

            /*Log.d(TAG, "BPF - La respuesta completa es: Reporte creado: " + reporteCreado +
                    " Archivos: " + envioArchivos +
                    " Respondio server: " + respondioServer +
                    " Datos Comercio: " + datosComercio +
                    " El mensaje: " + message);*/

            if (reporteCreado != 0) { // Se inicia el servicio de fotografías
                Log.d(TAG, "Se creó el reporte desde notificacion persistente: " + reporteCreado);
                iniciarProcesoFotografias();
            }
        }
    };

    private void iniciarServicioGenerarAlerta(){
        Intent intent = new Intent(getApplicationContext(), GenerarAlertaService.class);
        intent.putExtra("comercio", 5);
        intent.putExtra("usuario", 3);
        intent.putExtra("sala", "Comercios");
        intent.putExtra("fecha", Utilidades.obtenerFecha());
        intent.setPackage("com.id.socketio");
        getApplicationContext().startService(intent);
    }

    // ********************************************
    // INICIAN RESULT RECEIVER DE FOTOGRAFIAS
    // ********************************************

    private class ImageTraseraResultReceiver extends ResultReceiver {
        public ImageTraseraResultReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            procesoImageTrasera = true;
            FECHA_TRASERA = resultData.getString("fecha");
            Log.d(TAG, "La fecha de trasera es: " + FECHA_TRASERA);

            // GUARDAR LA IMAGEN DE RETORNO
            String imagenTrasera = resultData.getString("imagen");
            if(!imagenTrasera.equals("Ninguna")){
                IMAGEN_TRASERA = imagenTrasera;
            }

            switch (resultCode) {
                case ERROR:
                    Log.d(TAG, "Ocurrió un error al devolver camara trasera");
                    break;

                case SUCCESS:
                    // ENVIAR MULTIMEDIA (1)
                    if(procesoImagenFrontal){
                        Boolean res = EnviarImagenes.enviarImagenFrontal(getApplicationContext(), IMAGEN_FRONTAL, FECHA_FRONTAL, reporteCreado);
                        Log.d(TAG, "El resultado del envio de frontal es: "+ res);
                    }
                    if(procesoImageTrasera){
                        Boolean res = EnviarImagenes.enviarImagenTrasera(getApplicationContext(), IMAGEN_TRASERA, FECHA_TRASERA, reporteCreado);
                        Log.d(TAG, "El resultado del envio de trasera es: "+ res);
                    }
                    break;
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }

    private class ImageFrontalResultReceiver extends ResultReceiver {
        public ImageFrontalResultReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            procesoImagenFrontal = true;
            FECHA_FRONTAL = resultData.getString("fecha");
            Log.d(TAG, "La fecha de frontal es: " + FECHA_FRONTAL);

            // GUARDAR LA IMAGEN DE RETORNO
            String imagenFrontal = resultData.getString("imagen");
            if(!imagenFrontal.equals("Ninguna")){
                IMAGEN_FRONTAL = imagenFrontal;
            }

            switch (resultCode) {
                case ERROR:

                    Log.d(TAG, "Ocurrió un error al devolver camara frontal");
                    // TU SIGUELE CON LA TRASERA
                    break;
                case SUCCESS:

                    if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){

                        // INICIAR PROCESO CAMARA TRAERA
                        resultTReceiver = new ImageTraseraResultReceiver(new Handler());
                        Intent intentTrasera = new Intent(getApplicationContext(), FotografiaService.class);
                        intentTrasera.putExtra("reporteCreado", reporteCreado);
                        intentTrasera.putExtra("tipoCamara", "trasera");
                        intentTrasera.putExtra("receiver", resultTReceiver);
                        startService(intentTrasera);

                    }else {
                        if(procesoImagenFrontal){
                            // ENVIAR LA IMAGEN FRONTAL (5)
                            Boolean res = EnviarImagenes.enviarImagenFrontal(getApplicationContext(), IMAGEN_FRONTAL, FECHA_FRONTAL, reporteCreado);
                            Log.d(TAG, "El resultado del envio de frontal es: "+ res);
                        } else{
                            // FIN DEL PROCESO (6)
                            finProcesoFotografias();
                        }

                    }

                    break;
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }

    public void iniciarProcesoFotografias(){

        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            // INICIAR PROCESO CAMARA FRONTAL
            resultFReceiver = new ImageFrontalResultReceiver(new Handler());
            Intent intentFrontal = new Intent(getApplicationContext(), FotografiaService.class);
            intentFrontal.putExtra("reporteCreado", reporteCreado);
            intentFrontal.putExtra("tipoCamara", "frontal");
            intentFrontal.putExtra("receiver", resultFReceiver);
            getApplicationContext().startService(intentFrontal);

        } else if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // INICIAR PROCESO CAMARA TRAERA
            resultTReceiver = new ImageTraseraResultReceiver(new Handler());
            Intent intentTrasera = new Intent(getApplicationContext(), FotografiaService.class);
            intentTrasera.putExtra("reporteCreado", reporteCreado);
            intentTrasera.putExtra("tipoCamara", "trasera");
            intentTrasera.putExtra("receiver", resultTReceiver);
            getApplicationContext().startService(intentTrasera);
        } else {
            if(procesoImagenFrontal){
                // ENVIA LA IMAGEN FRONTAL (2)
                Boolean res = EnviarImagenes.enviarImagenFrontal(getApplicationContext(), IMAGEN_FRONTAL, FECHA_FRONTAL, reporteCreado);
                Log.d(TAG, "El resultado del envio de frontal es: "+ res);
            } else{
                // TERMINA PROCESO Y NO ENVIA NADA (3)
                finProcesoFotografias();
            }
        }
    }

    public void finProcesoFotografias(){
        Log.d(TAG, "Termina proceso vacio de fotografias");
        // Al menos guardar las fotografía en el dispositivo
    }

    // ********************************************
    // TERMINAN RESULT RECEIVER DE FOTOGRAFIAS
    // ********************************************

    public void crearNotificacionPersistente(){

        Log.d(TAG,"Se creó notificación persistente!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(getApplicationContext(), ServicioNotificacion.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

            // Crear notificación de servicio activo
            Notification notification =
                    new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                            .setColor(Color.WHITE)
                            .setContentText("Alerta al presionar el botón de encendido.")
                            .setSmallIcon(R.drawable.ic_notification_siren)
                            .setContentIntent(pendingIntent)
                            .build();

            // Crear al canal de notificación, pero solo en API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence nombre = "Botón de pánico para comercios"; //2
                String descripcion = "El uso de este servicio le permite detectar cuando se presiona tres veces el botón de bloqueo y genera la alerta de pánico"; //
                int importancia = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, nombre, importancia);
                notificationChannel.setDescription(descripcion);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            startForeground(ID_SERVICIO_PANICO, notification);
            Log.d(TAG,"Se inicio servicio para API 26+ - Desde servicio prueba!");

        } else{
            // Mostrar el otro tipo de notificación
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancela el registro del BroadCast
        unregisterReceiver(botonazoReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiverBotonazo);
    }
}
