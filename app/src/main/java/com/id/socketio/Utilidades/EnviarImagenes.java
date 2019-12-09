package com.id.socketio.Utilidades;

import android.content.Context;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static com.id.socketio.Constantes.TAG;

public class EnviarImagenes {

    public static Boolean enviarImagenFrontal(Context context, String IMAGEN_FRONTAL, String FECHA_FRONTAL, int reporteCreado){
        //Log.d(TAG, "Enviare imagen frontal" + IMAGEN_FRONTAL.length());

        StringRequest requestFrontal;

        int contador = 0;
        Boolean tiempoEspera = false;
        if(reporteCreado >= 1){
            tiempoEspera = true;
        }

        if(reporteCreado >=1 ){

            // COMIENZA HILO PARA ENVIAR IMAGEN FRONTAL
            Log.d(TAG, "Comienza hilo para enviar imagen frontal");

            // Cuerpo de la petición
            String archivo = IMAGEN_FRONTAL;
            String URL = Constantes.URL + "/upload/imagenes/" + reporteCreado;

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            JSONObject jsonObjectBody = new JSONObject();
            try {
                jsonObjectBody.put("fecha", FECHA_FRONTAL);
                jsonObjectBody.put("imagen", archivo);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }

            final String requestBody = jsonObjectBody.toString();
            requestFrontal = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // RECIBIR LA RESPUESTA DEL WEB SERVICE CUANDO TOD ESTA CORRECTO
                    Log.i(TAG, "La respuesta de imagen frontal es: " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // CACHA CUALQUIER TIPO DE ERROR
                    Log.e(TAG , error.toString());
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
            requestQueue.add(requestFrontal);

            return true;


        } else if (reporteCreado == -1){
            return false;

        } else {
            Log.d(TAG, "Se agotó el tiempo de espera frontal!!! ");
            return false;

        }
    }

    public static Boolean enviarImagenTrasera(Context context,  String IMAGEN_TRASERA, String FECHA_TRASERA, int reporteCreado){
        StringRequest requestTrasera;

        //Log.d(TAG, "Enviaré imagen trasera" + IMAGEN_TRASERA.length());

        //int contador = 0;
        //Boolean tiempoEspera = false;
        //if(reporteCreado >= 1){
          //  tiempoEspera = true;
        //}

        if(reporteCreado >=1 ){
            // COMIENZA HILO PARA ENVIAR IMAGEN FRONTAL
            Log.d(TAG, "Comienza hilo para enviar imagen trasera");


            // Cuerpo de la petición
            String archivo = IMAGEN_TRASERA;
            String URL = Constantes.URL + "/upload/imagenes/" + reporteCreado;

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            JSONObject jsonObjectBody = new JSONObject();
            try {
                jsonObjectBody.put("fecha", FECHA_TRASERA);
                jsonObjectBody.put("imagen", archivo);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }

            final String requestBody = jsonObjectBody.toString();
            requestTrasera = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // RECIBIR LA RESPUESTA DEL WEB SERVICE CUANDO TOD ESTA CORRECTO
                    Log.i(TAG, "La respuesta de imagen trasera es: " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // CACHA CUALQUIER TIPO DE ERROR
                    Log.e(TAG , error.toString());
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
            requestQueue.add(requestTrasera);
            return true;

        } else if (reporteCreado == -1){
            return false;

        } else {
            Log.d(TAG, "Se agotó el tiempo de espera trasera!!! ");
            return false;

        }
    }
}
