package com.id.socketio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import com.id.socketio.Servicios.AudioService;
import com.id.socketio.Servicios.FotografiaService;
import com.id.socketio.Utilidades.EnviarImagenes;
import com.id.socketio.Utilidades.Utilidades;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import com.id.socketio.Utilidades.SharedPreferences.*;

import static com.id.socketio.Constantes.TAG;

public class MainActivity extends AppCompatActivity {

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

    Utilidades util = new Utilidades();

    // VARIABLES PARA REPORTE CREADO
    public int idComercio;
    public int idUsuario;
    public int reporteCreado;
    public boolean btnPresionado = false;

    // COMPONENTES DE LA VISTA
    private ImageButton btnAlerta;
    private ImageView imgSenal;

    // ===============================
    // VARIABLES DEL SOCKET
    // ===============================
    private Boolean hasConnection = false;
    private Boolean alertaRecibida = false;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constantes.url);
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAlerta = (ImageButton) findViewById(R.id.sendButton);
        imgSenal = findViewById(R.id.imgSenal);

        // Restaurar los valores
        if(savedInstanceState != null){
            hasConnection = savedInstanceState.getBoolean("hasConnection");
        }
        if(!hasConnection){
            imgSenal.setImageResource(R.drawable.ic_senal_rojo);
        }

        // Valores Shared Preferences
        SharedPreferences preferences = getSharedPreferences("Login", Context.MODE_PRIVATE);
        if (preferences.contains("comercio") && preferences.contains("usuario")){
            idComercio = preferences.getInt("comercio" ,0);
            idUsuario = preferences.getInt("usuario", 0);
            // (Toast.makeText(getApplicationContext(), "Comercio: " + idComercio + " Usuario: " + idUsuario, Toast.LENGTH_SHORT)).show();
        } else {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
            MainActivity.this.finish();
        }

        // MENU DE BOTONES FLOTANTES
        final FloatingActionsMenu menuBotones = findViewById(R.id.grupofab);
        FloatingActionButton fabPerfil = findViewById(R.id.fabPerfil);
        FloatingActionButton fabConf = findViewById(R.id.fabConfig);
        FloatingActionButton fabInf = findViewById(R.id.fabInfo);
        fabPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuBotones.collapse();
            }
        });
        fabConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
                menuBotones.collapse();
            }
        });
        fabInf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuBotones.collapse();
            }
        });
    }


    public void activarBoton(View view){

        // VALIDAR SI PUEDE GENERAR UNA NUEVA ALERTA
        if(util.puedeGenerarNuevaAlerta(getApplicationContext())){
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
                    dataComercio.put("sala", "Comercios");
                    dataComercio.put("fecha", util.obtenerFecha());

                    mSocket.emit("botonActivado", dataComercio);
                    Log.d(TAG,"Según emitió");
                    btnPresionado = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else{
            // Crear servicio especifico para el servicio de emit() contador
            Log.d(TAG, "CONTADOR ++ ");
        }
    }


    public void iniciarProcesoFotografias(){
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            // INICIAR PROCESO CAMARA FRONTAL
            resultFReceiver = new ImageFrontalResultReceiver(new Handler());
            Intent intentFrontal = new Intent(MainActivity.this, FotografiaService.class);
            intentFrontal.putExtra("reporteCreado", reporteCreado);
            intentFrontal.putExtra("tipoCamara", "frontal");
            intentFrontal.putExtra("receiver", resultFReceiver);
            startService(intentFrontal);

        } else if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // INICIAR PROCESO CAMARA TRAERA
            resultTReceiver = new ImageTraseraResultReceiver(new Handler());
            Intent intentTrasera = new Intent(MainActivity.this, FotografiaService.class);
            intentTrasera.putExtra("reporteCreado", reporteCreado);
            intentTrasera.putExtra("tipoCamara", "trasera");
            intentTrasera.putExtra("receiver", resultTReceiver);
            startService(intentTrasera);
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

    // ********************************************
    // INICIAN ESCUCHADORES DE SOCKETS - Dejarlos como estan en SOCKETIO 3
    // ********************************************

    Emitter.Listener onAlertaRecibida = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int length = args.length;
                    if (length == 0) {
                        // No hubo respuesta valida del servidor
                        return;
                    } else{
                        alertaRecibida = true;
                        imgSenal.setImageResource(R.drawable.ic_senal_verde);
                        reporteCreado = Integer.parseInt(String.valueOf(args[0]));
                        (Toast.makeText(getApplicationContext(), "El reporte creado fue el #" + reporteCreado, Toast.LENGTH_SHORT)).show();
                    }

                    if(alertaRecibida){
                        // TOMAR FOTOGRAFIAS
                        iniciarProcesoFotografias();
                        Log.d(TAG, "El reporte fue creado con exito");

                        // GUARDAR INFORMACION DEL ULTIMO REPORTE GENERADO
                        com.id.socketio.Utilidades.SharedPreferences.guardarUltimoReporte(getApplicationContext(), reporteCreado);
                    }
                }
            });
        }
    };

    Emitter.Listener onAlertaNoRecibida = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int length = args.length;
                    if (length == 0) {
                        // No hubo respuesta valida del servido
                        return;
                    } else{
                        alertaRecibida = false;
                        reporteCreado = Integer.parseInt(String.valueOf(args[0]));
                        (Toast.makeText(getApplicationContext(), "El reporte no se pudo crear #" + reporteCreado, Toast.LENGTH_SHORT)).show();
                    }
                }
            });
        }
    };

    // ********************************************
    // TERMINAN ESCUCHADORES DE SOCKETS
    // ********************************************


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
                        Intent intentTrasera = new Intent(MainActivity.this, FotografiaService.class);
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

    // ********************************************
    // TERMINAN RESULT RECEIVER DE FOTOGRAFIAS
    // ********************************************

    public void finProcesoFotografias(){
        Log.d(TAG, "Termina proceso vacio de fotografias");
        // Al menos guardar las fotografía en el dispositivo
    }

    public void grabarAudio(View view){
        Log.d(TAG, "Init grabar audio...");

        Intent intent = new Intent(MainActivity.this, AudioService.class);
        intent.putExtra("nombreAudio", "audioPrueba");
        startService(intent);

        /*String PATH_NAME = "";
        try {
            MediaRecorder recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(PATH_NAME);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }



    public void irConfiguracion(View view){
        Intent intent = new Intent(MainActivity.this, ConfiguracionActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasConnection", hasConnection);
        System.err.println("Guardó datos ");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        hasConnection = savedInstanceState.getBoolean("hasConnection");
        System.err.println("Recuperó  datos ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isFinishing()){
            System.err.println("on Destroy > is Finishing ");
            mSocket.disconnect();
        }else {
            System.err.println("on Destroy > is Rotating ");
        }
    }


}
