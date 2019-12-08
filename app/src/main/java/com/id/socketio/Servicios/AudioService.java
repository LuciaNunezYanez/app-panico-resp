package com.id.socketio.Servicios;

import android.app.IntentService;
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
import android.widget.Toast;

import com.id.socketio.Utilidades.Utilidades;

import java.io.IOException;

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
    EnviarAudioBackground enviarAudioBackground;


    String audios[] = new String[4];
    int contadorRecorder = 0;
    int contadorEnvio = 0;

    public AudioService() {
        //super("AudioService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // (Hilo 1)
    // (Hilo 2)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*if (intent != null) {
            intentG = intent;
            nombreAudio = intent.getStringExtra("nombreAudio");
            //iniciarProceso();
            iniciarHilo();
        }*/

        if (intent != null) {
            intentG = intent;
            nombreAudio = intent.getStringExtra("nombreAudio");

            //iniciarProceso();
            //iniciarHilo();
            comenzarHiloGrabacionAudio(contadorRecorder);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void comenzarHiloGrabacionAudio(int posicionGuardar){
       grabarAudioBackground = new GrabarAudioBackground();
        grabarAudioBackground.execute(posicionGuardar);

    }

    public void comenzarHiloEnvioAudio(int posicionEnviar){
        enviarAudioBackground = new EnviarAudioBackground();
        enviarAudioBackground.execute(posicionEnviar);
    }

    // Escucha cuando se termino el hilo de la grabación
    Handler handlerTerminoGrabacion = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Terminó de ejecutar la tarea
                    //Log.d(TAG, "Ya termino de grabar");
                    //Log.d(TAG, "(handlerGrabacion) El estatus de mi hilo es: "  + grabarAudioBackground.getStatus());
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
                    Log.d(TAG, "(Hilo 2) (handlerEnvioGrabacion) Ya termino de enviar audio.");
                    //Log.d(TAG, "(handlerEnvioGrabacion) El estatus de mi hilo es: "  + enviarAudioBackground.getStatus());
                    Log.d(TAG, "(Hilo 2) ------- FIN " + contadorEnvio + "-------");
                    contadorEnvio++;
                    break;
                default:
                    break;
            }
            // Aquí se notifica que se envió correctamente
        }
    };


    public class EnviarAudioBackground extends AsyncTask<Integer, Integer, Boolean>{

        String pathParaEnviar;
        int posicionEnviar;

        @Override
        protected Boolean doInBackground(Integer... info) {
            // Aqui se recibe ruta, se codifica y se envía
            posicionEnviar = info[0];
            pathParaEnviar = audios[posicionEnviar];

            Log.d(TAG, "(Hilo 2) Path: " + pathParaEnviar);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            // Log.d(TAG, "(onPostExecute) Terminó de enviar el audio: " + aBoolean);
            handlerTerminoEnvio.sendEmptyMessage(0);
        }
    }

    public class GrabarAudioBackground extends AsyncTask<Integer, Integer, String> implements MediaRecorder.OnInfoListener {

        int posicionParaGuardar;

        @Override
        protected String doInBackground(Integer... posicion) {
            //Log.d(TAG, "Se inició la grabación desde doInBackground");


            posicionParaGuardar = posicion[0];
            Log.d(TAG, "(Hilo 1) Posicion para guardar el presente audio: " + posicionParaGuardar);
            iniciarGrabacion();
            return "fin";
        }

        private void iniciarGrabacion(){
            AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + nombreAudio + "_" + posicionParaGuardar + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setMaxDuration(5000); // 5 seg
            mediaRecorder.setOutputFile(AudioSavePathInDevice);
            mediaRecorder.setOnInfoListener(this);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
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

                    // Log.d(TAG, "Llego a su maxima duracion");
                    mediaRecorder.stop();
                    onPostExecute(AudioSavePathInDevice);

                    //reproducir();
                    //mr.release();
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