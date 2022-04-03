package de.datavisions.agecalculator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SinglePersonWidget extends AppWidgetProvider {


    @SuppressLint("UnspecifiedImmutableFlag")
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.single_person_widget);

        // Check if Pro Version
//        if (false) {
        if (!context.getPackageName().equals(context.getString(R.string.pro_package))) {
            rv.setTextViewText(R.id.name, context.getString(R.string.widget_pro));
            rv.setTextViewText(R.id.years, context.getString(R.string.na));
            rv.setTextViewText(R.id.months, context.getString(R.string.na));
            rv.setTextViewText(R.id.weeks, context.getString(R.string.na));
            rv.setTextViewText(R.id.days, context.getString(R.string.na));
            appWidgetManager.updateAppWidget(appWidgetId, rv);
            return;
        }

        // Check if any Data was added yet
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        List<Person> personList = MainActivity.loadPersonList(sp);
        if (personList.size() == 0) {
            rv.setTextViewText(R.id.name, context.getString(R.string.no_data));
            rv.setTextViewText(R.id.years, context.getString(R.string.na));
            rv.setTextViewText(R.id.months, context.getString(R.string.na));
            rv.setTextViewText(R.id.weeks, context.getString(R.string.na));
            rv.setTextViewText(R.id.days, context.getString(R.string.na));
            appWidgetManager.updateAppWidget(appWidgetId, rv);
            return;
        }

        // Get the matching PersonID of this appWidgetId from the widgetMap
        HashMap<Integer, Integer> widgetMap = new Gson().fromJson(sp.getString("widgetMap", "{}"),
                new TypeToken<HashMap<Integer, Integer>>(){}.getType());
        int personID = 0;
        if (widgetMap.containsKey(appWidgetId)) personID = widgetMap.get(appWidgetId);
        else {
            widgetMap.put(appWidgetId, personID);
            sp.edit().putString("widgetMap", new Gson().toJson(widgetMap)).apply();
        }

        // Check if the personID is in the personList. Should always be true, but who knows...
        if (personList.size() <= personID) {
            // Update with the default layout, that says error
            appWidgetManager.updateAppWidget(appWidgetId, rv);
            return;
        }

        // Everything ready, so set the views for the personID
        Person p = personList.get(personID);
        if (p.getPicture() != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                        p.getPicture());
                rv.setImageViewBitmap(R.id.picture, bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else rv.setImageViewResource(R.id.picture, R.drawable.no_img);
        rv.setTextViewText(R.id.name, p.getName());
        rv.setTextViewText(R.id.years, p.getAgeInUnit(p.DATE_YEARS, context));
        rv.setTextViewText(R.id.months, p.getAgeInUnit(p.DATE_MONTHS, context));
        rv.setTextViewText(R.id.weeks, p.getAgeInUnit(p.DATE_WEEKS, context));
        rv.setTextViewText(R.id.days, p.getAgeInUnit(p.DATE_DAYS, context));
        rv.setOnClickPendingIntent(R.id.btn_change_person, PendingIntent.getBroadcast(context, 0,
                new Intent(context, SinglePersonWidget.class).setAction("next")
                        .setData(ContentUris.withAppendedId(Uri.EMPTY, appWidgetId))
                        .putExtra("appWidgetId", appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Map<Integer, Integer> widgetMap = new HashMap<>();
        sp.edit().putString("widgetMap", new Gson().toJson(widgetMap)).apply();
    }


    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove("widgetMap").apply();
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        // Functionality when one or more widgets are deleted
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Map<Integer, Integer> widgetMap = new Gson().fromJson(sp.getString("widgetMap", "{}"),
                new TypeToken<HashMap<Integer, Integer>>(){}.getType());
        for (int id : appWidgetIds) if (widgetMap.get(id) != null) widgetMap.remove(id);
        sp.edit().putString("widgetMap", new Gson().toJson(widgetMap)).apply();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("next")) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Map<Integer, Integer> widgetMap = new Gson().fromJson(sp.getString("widgetMap", "{}"),
                    new TypeToken<HashMap<Integer, Integer>>(){}.getType());
            int appWidgetId = intent.getIntExtra("appWidgetId", -1);
            if (appWidgetId == -1) return;
            if (widgetMap.containsKey(appWidgetId)) {
                int nPersons = MainActivity.loadPersonList(sp).size();
                if (nPersons == 0) return;
                // Increase current index by 1, or set it to 0 if already at max, ie: (nPersons - 1)
                if (widgetMap.get(appWidgetId) == nPersons - 1) widgetMap.put(appWidgetId, 0);
                else widgetMap.put(appWidgetId, widgetMap.get(appWidgetId) + 1);
                // save the updated map
                sp.edit().putString("widgetMap", new Gson().toJson(widgetMap)).apply();
                // Update the widget
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
        }
    }
}