package com.id.socketio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class PanicoWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Calcular la imagen a utilizar cuando se cambia el tamaño del widget
        Bundle option = appWidgetManager.getAppWidgetOptions(appWidgetId);
        float ancho = option.getInt(appWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        float alto = option.getInt(appWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        int imagen;
        int diseno;
        int imagenMediana = R.drawable.sos_mediana;
        int imagenChica = R.drawable.sos_chica;
        int layoutMediano = R.layout.panico_widget2;
        int layoutChico = R.layout.panico_widget;

        // (Toast.makeText(context, "Alto: " +  alto + " Ancho: " + ancho + "Dif: " + (alto - ancho), Toast.LENGTH_LONG)).show();
        if ( (alto - ancho) <= 30 && (alto - ancho) >= -75) { // Es cuadrado
            imagen = imagenChica;
            diseno = layoutChico;

        } else {
            imagen = imagenMediana;
            diseno = layoutMediano;
        }

        // Dar permiso al widget de que se comunique con la aplicación



        //

        String body = "Notificación de prueba";
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("body", body); // Si
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), diseno);
        views.setImageViewResource(R.id.widgetImage, imagen);
        views.setOnClickPendingIntent(R.id.widgetImage, pendingIntent);
        // Indique al administrador de widgets que actualice el widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        ContextCompat.startForegroundService(context, intent);


        // Dar permiso al widget de que se comunique con la aplicación
        /*Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), diseno);
        views.setImageViewResource(R.id.widgetImage, imagen);
        views.setOnClickPendingIntent(R.id.widgetImage, pendingIntent);
        // Indique al administrador de widgets que actualice el widget
        appWidgetManager.updateAppWidget(appWidgetId, views);*/
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

