package com.id.socketio.Servicios;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.id.socketio.Constantes;
import com.id.socketio.Utilidades.Utilidades;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.id.socketio.Constantes.DURACION_AUDIO;
import static com.id.socketio.Constantes.EXTENSION_AUDIO;

public class AudioService extends Service  {

    public static final int RequestPermissionCode = 1;
    String AudioSavePathInDevice = null;
    private String nombreAudio = "";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    Intent intentG;
    Boolean hasStart = false;
    String TAG = "Debug";

    GrabarAudioBackground grabarAudioBackground;

    String audios[] = new String[4];
    String fechas[] = new String[4];

    int contadorRecorder = 0;
    int contadorEnvio = 0;
    int reporteCreado = 0;

    public AudioService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            intentG = intent;
            nombreAudio = intent.getStringExtra("nombreAudio");
            reporteCreado = intent.getIntExtra("reporteCreado", 0);
            comenzarHiloGrabacionAudio(contadorRecorder);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void comenzarHiloGrabacionAudio(int posicionGuardar){
       grabarAudioBackground = new GrabarAudioBackground();
        grabarAudioBackground.execute(posicionGuardar);
    }

    public void comenzarHiloEnvioAudio(int posicionEnviar){
        enviarAudioVolley(posicionEnviar);
    }

    // Escucha cuando se termino el hilo de la grabación
    Handler handlerTerminoGrabacion = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Terminó de ejecutar la tarea
                    //Log.d(TAG, "Ya termino de grabar");
                    audios[contadorRecorder] = AudioSavePathInDevice;
                    contadorRecorder++;

                    Log.d(TAG, "(Hilo 1) Contador: " + contadorRecorder);
                    Log.d(TAG, "(Hilo 1) Audios[" + contadorEnvio + "]: = " + audios[contadorEnvio]);
                    if(contadorRecorder <= 3)
                        comenzarHiloGrabacionAudio(contadorRecorder);

                    if(contadorEnvio <= 3)
                        comenzarHiloEnvioAudio(contadorEnvio);

                    break;
                default:
                    break;
            }
        }
    };

    Handler handlerTerminoEnvio = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Terminó de ejecutar la tarea
                    Log.d(TAG, "(Hilo 2) ------- FIN " + contadorEnvio + "-------");
                    contadorEnvio++;

                    if(contadorEnvio == 4){
                        stopSelf();
                        Log.d(TAG, "stopSelf()");
                    }
                    break;
                default:
                    break;
            }
            // Aquí se notifica que se envió correctamente
        }
    };

    public Boolean enviarAudioVolley(int posicionEnviar){

        String pathParaEnviar = audios[posicionEnviar];
        String fechaParaEnviar = fechas[posicionEnviar];

        Log.d(TAG, "(Hilo 2) Path: " + pathParaEnviar);

        String audioBase64 = Utilidades.convertirAudioString(pathParaEnviar);
        // Log.d(TAG, "La longitud de caracteres base 64 es de: " + audioBase64.length()); //33,000 aprox
        String URL = Constantes.URL + "/upload/audio/" + reporteCreado;

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JSONObject jsonObjectBody = new JSONObject();
        try {
            jsonObjectBody.put("fecha", fechaParaEnviar);
            jsonObjectBody.put("audio", audioBase64);
            jsonObjectBody.put("parte", posicionEnviar);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        final String requestBody = jsonObjectBody.toString();
        StringRequest requestAudio = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // RECIBIR LA RESPUESTA DEL WEB SERVICE CUANDO TOD ESTA CORRECTO
                Log.i(TAG, "(Respuesta audio) La respuesta del envio de audio es: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // CACHA CUALQUIER TIPO DE ERROR
                Log.e(TAG , "(Respuesta audio) " + error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Codificación no compatible al intentar obtener los bytes de% s usando %s", requestBody, "utf-8");
                    return null;
                }
            }
        };
        requestQueue.add(requestAudio);

        // Avisar que se terminó el envio
        handlerTerminoEnvio.sendEmptyMessage(0);
        return true;
    }

    public class GrabarAudioBackground extends AsyncTask<Integer, Integer, String> implements MediaRecorder.OnInfoListener {

        int posicionParaGuardar;

        @Override
        protected String doInBackground(Integer... posicion) {
            posicionParaGuardar = posicion[0];
            Log.d(TAG, "(Hilo 1) Posicion para guardar el presente audio: " + posicionParaGuardar);
            iniciarGrabacion();
            return "fin";
        }

        private void iniciarGrabacion(){

            AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + nombreAudio + "_" + posicionParaGuardar + "." + EXTENSION_AUDIO;
            mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            // mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

            // ó

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mediaRecorder.setMaxDuration(DURACION_AUDIO);
            mediaRecorder.setOutputFile(AudioSavePathInDevice);
            mediaRecorder.setOnInfoListener(this);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();

                fechas[posicionParaGuardar] = Utilidades.obtenerFecha();

                hasStart= true;
            } catch (IllegalStateException e) {
                Log.d(TAG, "(Hilo 1) Catch 1");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "(Hilo 1) Catch 2");
                e.printStackTrace();
            }
            Log.d(TAG, "(Hilo 1) (iniciarGrabacion()) Empezó grabación. Contador: " + posicionParaGuardar);
        }

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            switch (what){
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    mediaRecorder.stop();
                    onPostExecute(AudioSavePathInDevice);
                    break;
            }
        }
        @Override
        protected void onPostExecute(String path) {
            super.onPostExecute(path);
            Log.d(TAG, "(Hilo 1) " + path);

            if(path.length()>5){ // Si se tiene la ruta correcta
                handlerTerminoGrabacion.sendEmptyMessage(0);
            } else{
                //Log.d(TAG, "Disque termino.");
            }
        }
    }


    private void reproducir(String path) {
        // Log.d(TAG, "Recibi el path: " + path);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();
        Log.d(TAG, "(reproducir) Reproduciendo audio.");
    }

     /*public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }*/

}

