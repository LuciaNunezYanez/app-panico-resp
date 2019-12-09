package com.id.socketio.Servicios;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.id.socketio.Constantes;
import com.id.socketio.Utilidades.Utilidades;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import static com.id.socketio.Constantes.TAG;

public class GenerarAlertaService extends Service {

    Intent intentGlobal;

    // ===============================
    // VARIABLES GENERALES
    // ===============================
    private int idComercio;
    private int idUsuario;
    private String sala;
    private String fecha;
    public int reporteCreado;
    public boolean btnPresionado = false;

    // ===============================
    // UTILIDADES
    // ===============================
    Utilidades util = new Utilidades();

    // ===============================
    // VARIABLES DEL SOCKET
    // ===============================
    private Boolean hasConnection = false;
    private Boolean alertaRecibida = false;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constantes.URL);
        } catch (URISyntaxException e) {}
    }

    public GenerarAlertaService() {
        // super("GenerarAlertaService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intentGlobal = intent;

        // Recuperar valores de entrada
        try{
            idComercio = intent.getIntExtra("comercio", 0);
            idUsuario = intent.getIntExtra("usuario", 0);
            sala = intent.getStringExtra("sala");
            fecha = intent.getStringExtra("fecha");
        }catch ( Exception e ){
            Log.d(TAG, "Debe regresar que no se encontrarón los datos parametros de entrada");
            darResultados(0, false, false, false,
                    "Los datos del comercio son incorrectos");
        }

        if (idComercio != 0 && idUsuario != 0){
            Log.d(TAG, "Recibí:" + " Comercio "+ idComercio + " Usuario " + idUsuario + " Sala " + sala + " Fecha " + fecha);
            generarReporte();

        } else {
            Log.d(TAG, "No venian los datos del comercio " + idComercio + " User: " + idUsuario);
            darResultados(0, false, false, false,
                    "Los datos del comercio son incorrectos");
        }
        return super.onStartCommand(intent, flags, startId);
    }


    public void generarReporte(){

        mSocket.connect();
        mSocket.on("alertaRecibida", onAlertaRecibida);
        mSocket.on("alertaNoRecibida", onAlertaNoRecibida);
        hasConnection = true;

        if (hasConnection) {
            try {
                // Crear JSON con los datos del comercio
                JSONObject dataComercio = new JSONObject();
                dataComercio.put("idComercio", idComercio);
                dataComercio.put("idUsuario", idUsuario);
                dataComercio.put("sala", sala); //"Comercios"
                dataComercio.put("fecha", fecha);

                mSocket.emit("botonActivado", dataComercio);
                Log.d(TAG , "Según emitió");
                btnPresionado = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // ===============================
    // ESCUCHADORES DE SOCKETS
    // ===============================

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "error " + args[0].toString());
        }
    };
    Emitter.Listener onAlertaRecibida = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            //Log.d(TAG, "Entro a alerta recibida ");
            int length = args.length;
            if (length == 0) {
                darResultados(0, false, true, true,
                        "Ocurrió un error al crear reporte, por favor intentar de nuevo.");
                /*
                 * No hubo respuesta valida del servidor
                 */
                return;
            } else{
                alertaRecibida = true;
                reporteCreado = Integer.parseInt(String.valueOf(args[0]));
                //Log.d(TAG, "El reporte creado fue el #" + reporteCreado);
            }

            if(alertaRecibida){
                /*
                 * Cuando el servidor responda la alerta se comienza con el envio de archivos multimedia
                 */
                // Guardar los datos del reporte creado
                SharedPreferences preferences = getSharedPreferences("UltimoReporte", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                int ultimoReporte = reporteCreado;
                long fechaUltimoReporte = System.currentTimeMillis();

                editor.putInt("ultimoReporte", ultimoReporte);
                editor.putLong("fechaUltimoReporte", fechaUltimoReporte);
                editor.commit();

                darResultados(reporteCreado, true, true, true,
                        "Reporte creado con éxito");

            }
        }
    };

    Emitter.Listener onAlertaNoRecibida = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            int length = args.length;
            if (length == 0) {
                /**
                 * No hubo respuesta valida del servido
                 **/
                darResultados(0, false, true, true,
                        "No hubo respuesta válida del servidor");

                return;
            } else{
                darResultados(0, false, true, true,
                        "Ocurrió un error al crear reporte, por favor intentar de nuevo.");
                Log.d(TAG, "El reporte no creado");
                alertaRecibida = false;
                reporteCreado = Integer.parseInt(String.valueOf(args[0]));
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.err.println("on Destroy > is Finishing ");
        stopSelf();
        mSocket.disconnect();
    }

    private void darResultados(int reporteCreado, boolean envioArchivos, boolean respondioServer, boolean datosComercio, String message){
        Intent intent = new Intent("generarAlertaService");
        intent.putExtra("reporteCreado", reporteCreado);
        intent.putExtra("envioArchivos",  envioArchivos);
        intent.putExtra("respondioServer", respondioServer);
        intent.putExtra("datosComercio", datosComercio);
        intent.putExtra("message", message);

        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
        stopSelf();
    }

}




/**
 * Agregar por si el servidor no responde en X tiempo el servicio se cancele y retorne error
 *
 * */

/*
 * Ninguno funcionará mientras que el componente o actividad que lo inicie siga iniciado
 * stopService(intentGlobal);
 * stopForeground(true);
 * GenerarAlertaService.this.stopForeground(true);
 * GenerarAlertaService.this.stopSelf();
 * */