package ru.isu.swa.diplom.controller;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.List;

import ru.isu.swa.diplom.model.Coordinate;

/**
 * Created by swa on 09.03.2018.
 */

//Класс для определения и получения данных о местоположении. Не должен дублироваться в процессе выполнения
public class GPSLocation {
    private LocationManager manager;
    private Context context;
    private static Coordinate coord;
    private static Double accuracy;


    private GPSLocation(Context context){
        this.context = context;
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<String> providers = manager.getProviders(true);
        for(String provider: providers) {
            manager.requestLocationUpdates(provider, 1000*10, 1, listener);
        }
    }

    // Внутренний класс для получения объекта основного.
    public static class GPSLocationHolder {
        private static GPSLocation location;
        public static GPSLocation getGPSLocation(Context context){
            if(location == null) location = new GPSLocation(context);
            return location;
        }
    }

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location!=null) {
                //берём наиболее точные и актуальные данные. Новые значения должны иметь выше accuracy ИЛИ выходить за пределы круга радиуса точности предыдущего значения
                //если предыдущих значений нет, берём любые данные
                if(coord==null || accuracy == null || Math.floor(location.getAccuracy())<accuracy || Math.pow(location.getLatitude()-coord.getLatitude(),2)+Math.pow(location.getLongitude()-coord.getLongitude(),2)>Math.pow((accuracy)/10000,2)) {
                    coord = new Coordinate(Function.round(location.getLatitude()), Function.round(location.getLongitude()));
                    accuracy = Math.floor(location.getAccuracy());
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
public static Coordinate getCoords(){
    return coord;
}
public static Double getAccuracy() {return accuracy;}
}
