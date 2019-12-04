package com.id.socketio.Servicios;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraService;
import com.androidhiddencamera.HiddenCameraUtils;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraFocus;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;
import com.androidhiddencamera.config.CameraRotation;
import com.id.socketio.R;
import com.id.socketio.Utilidades.Utilidades;

import java.io.File;

import static com.id.socketio.Constantes.TAG;

public class FotografiaService extends HiddenCameraService {

    StringRequest request;
    String tipoCamara;
    Bitmap imagenBitmap;
    int reporteCreado;
    String fecha;
    String imagen = "ninguna";

    public static ResultReceiver receiver;
    public static final int SUCCESS = 1;
    public static final int ERROR = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("Va en 8");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Recuperar datos del reporte
            reporteCreado = intent.getIntExtra("reporteCreado", 0);
            tipoCamara = intent.getStringExtra("tipoCamara");

            int tipoFoto = CameraFacing.FRONT_FACING_CAMERA;
            int rotacion = CameraRotation.ROTATION_270;

            if(tipoCamara.equals("frontal")){
                tipoFoto = CameraFacing.FRONT_FACING_CAMERA;
            } else if (tipoCamara.equals("trasera")){
                tipoFoto = CameraFacing.REAR_FACING_CAMERA;
                rotacion = CameraRotation.ROTATION_90;
            } else {
                // No hay foto y termina proceso
            }

            receiver = intent.getParcelableExtra("receiver");

            System.out.println("Va en 9");
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                if (HiddenCameraUtils.canOverDrawOtherApps(getApplicationContext())) {

                    CameraConfig cameraConfig = new CameraConfig()
                            .getBuilder(getApplicationContext())
                            .setCameraFacing(tipoFoto)
                            .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                            .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                            .setCameraFocus(CameraFocus.AUTO)
                            .setImageRotation(rotacion)
                            .build();

                    startCamera(cameraConfig);

                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Toast.makeText(getApplicationContext(),"Capturando imagen" + tipoCamara + "..", Toast.LENGTH_SHORT).show();
                            //Log.d(TAG, "Estoy capturando... "+ tipoCamara);
                            takePicture();
                        }
                    }, 2000L);
                } else {
                    HiddenCameraUtils.openDrawOverPermissionSetting(getApplicationContext());
                    responderReceiver(ERROR, imagen);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Permiso de cámara no disponible", Toast.LENGTH_SHORT).show();
                responderReceiver(ERROR, imagen);
            }
        }catch(Exception e)
        {
            Log.d(TAG, "Callo en el catch");
            responderReceiver(ERROR, imagen);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        System.out.println("Va en 10");
        // Toast.makeText(getApplicationContext(), "El tamaño de la imagen capturada es : " + imageFile.length(), Toast.LENGTH_SHORT).show();

        fecha = Utilidades.obtenerFecha();
        // Convertir la imagen a bitmap
        String filePath = imageFile.getPath();
        imagenBitmap = BitmapFactory.decodeFile(filePath);

        imagen = Utilidades.convertirImgString(imagenBitmap);
        responderReceiver(SUCCESS, imagen);
    }

    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        System.out.println("Va en 11");
        responderReceiver( ERROR, imagen);

        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                Toast.makeText(getApplicationContext(), R.string.error_cannot_open + tipoCamara, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                Toast.makeText(getApplicationContext(), R.string.error_cannot_write + tipoCamara, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                Toast.makeText(getApplicationContext(), R.string.error_cannot_get_permission + tipoCamara, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                // Mostrar cuadro de diálogo de información al usuario con pasos para conceder "Dibujar sobre otra aplicación"
                // Permiso para la aplicación.
                HiddenCameraUtils.openDrawOverPermissionSetting(getApplicationContext());
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(getApplicationContext(), R.string.error_not_having_camera, Toast.LENGTH_LONG).show();
                break;
        }

    }

    public void responderReceiver(int estatus , String imagen){
        Bundle bundle = new Bundle();
        bundle.putString("imagen", imagen);
        bundle.putString("tipoCamara", tipoCamara);
        bundle.putString("fecha", fecha);
        receiver.send(estatus, bundle);
        stopSelf();
    }
}
