package com.example.salim.googlemap1;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener,DialogAdapter.DialogAdapterListener {



    private LocationManager lm;
    //Pour récupérer le fragment Map
    private MapFragment mapFragment;
    //Pour manipuler Map
    private GoogleMap googleMap;
    //Constante du requestcode pour la demande de permission
    private static final int PERMS_CALL_ID=1234;




    //Pour les fichiers
    File sdcard,file;

    double longi,lat;

    NumberFormat format;

    //Dialog
    FloatingActionButton badd;


    LatLng start,waypoint,end;


    //List des routes
    List<LatLng> listRoad;


    //Liste des coordonnées
    List<StringBuilder> coordonneeList;

    //Liste des coordonnees avec LatLng
    List<LatLng> latlngList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lm=(LocationManager) getSystemService(LOCATION_SERVICE);

        //
        badd=findViewById(R.id.button_add);

        /*
        * Récupération des attributs
        * FragmentManager pour récupérer des fragments
        * */
        FragmentManager fragmentManager=getFragmentManager();
        mapFragment= (MapFragment) fragmentManager.findFragmentById(R.id.map);

        //Récupérer le chemin de sdcard
        //sdcard=Environment.getExternalStorageDirectory();

        //Récupérer le fichier concerné dans internal storage
        file=new File("/data/","datalog.txt");

        format= NumberFormat.getInstance(Locale.US);


        //Liste des routes
        listRoad=new ArrayList<>();

        //Liste des coordonnees
        coordonneeList=new ArrayList<>();

        //Liste des coordonnees après conversion
        latlngList=new ArrayList<>();

       //Programmation du bouton flottant
        badd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();

            }
        });

//récupérer depuis le fichier
       /*StringBuilder coordonnee=getStringFile();
        longi=castDouble(getLongitude(coordonnee));
        lat=castDouble(getLatitude(coordonnee));
*/

       getCoordonneeFromString(getStringFile());
    }



    @Override
    public void onLocationChanged(Location location) {

        double latitude=location.getLatitude();
        double longitude=location.getLongitude();

        /**
        *Vérifier d'abord si l'objet googlemap a été bien chargé
        * Mettre la camera à la position actuelle
         */
        if (googleMap != null){
            LatLng position=new LatLng(latitude,longitude);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(position));
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

    //Cycle de vie de l'activité
    @Override
    protected void onResume() {
        super.onResume();

        checkPermissions();
    }

    //Méthode pour vérifier les permissions

    public void checkPermissions(){
        /*
         * Vérifier si l'application a accès aux permissions
         * Obligatoire pour les requestUpdates
         * */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /*
             * Si aucune n'est activé
             * on demande à l'utilisateur d'avoir la permisssion
             * */
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMS_CALL_ID);

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //Vérifier si on peut recevoir la localisation
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        }
        if (lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10000, 0, this);
        }
        //Pour charger la map avec les options prédéfinies
        loadMap();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Vérifier si le résultat vient de la demande de permission
        if (requestCode== PERMS_CALL_ID){
            checkPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (lm != null){
            lm.removeUpdates(this);
        }
    }

    /*
    * Récupérer l'objet pour travailler avec la map
    * */

    @SuppressWarnings("MissingPermission")
    private void loadMap() {
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                    MainActivity.this.googleMap=googleMap;

                    //On peut ajouter des markers, move camera etc..
                  googleMap.moveCamera(CameraUpdateFactory.zoomBy(3));

                  /*
                  * Pour cette dernière instruction, il faut les permissions
                  * On a déjà une méthode checkPermissions donc on va appeler loadMap à la fin de la méthode checkPermissions
                  * On rajoute un SupressWarning pour enlever le warning
                  * */
             // googleMap.setMyLocationEnabled(true);





                //Ajout des marqueurs depuis la liste du fichier
          //     addMarkerFromList(latlngList);

               googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                   @Override
                   public boolean onMarkerClick(final Marker marker) {
                       AlertDialog.Builder builder;

                           builder=new AlertDialog.Builder(MainActivity.this);

                       builder.setTitle("Confirmer la suppression")
                               .setMessage("Voulez-vous supprimer ce marqueur ?")
                               .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {

                                   }
                               })
                               .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {
                                       marker.remove();
                                       listRoad.remove(new LatLng(marker.getPosition().latitude,marker.getPosition().longitude));
                                       makeRoute(listRoad);
                                   }
                               });
                       AlertDialog alertDialog=builder.create();
                       alertDialog.show();
                       return false;
                   }
               });
            }
        });
    }

    private List<StringBuilder> getStringFile() {
        StringBuilder text = null;
        String line = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));


            while ((line = br.readLine()) != null) {
                text.append(line);
                coordonneeList.add(text);
               // text.append(System.getProperty("line.separator"));


            }
            br.close();
        } catch (IOException e) {
            Log.d("fais chier",e.getMessage());
        }
        return coordonneeList;
    }
    //Convertir les coordonnees en double
    /*private double castDouble(String chaine){
        double cast=0;
        Number number = null;

        try{
            number=format.parse(chaine);
            cast=Double.parseDouble(number.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cast;
    }*/

    //Récupérer la chaine de caractère qui concerne la longitude
    private double getLongitude(StringBuilder coordonnees) {

        int i = 0;
        //Passer la chaine jusqu'à arriver à ;
        while (coordonnees.charAt(i) != ';') {
            i++;
        }
        i=i+2;
        String longi=coordonnees.substring(i);
        return Double.parseDouble(longi);
    }
    //Récupérer la chaine de caractère concernant la latitude
    private double getLatitude(StringBuilder coordonnees) {

        int i = 0;
        //Parcourir la chaine jusqu'à ';'
        while (coordonnees.charAt(i) != ';') {
            i++;
        }


        String lat=coordonnees.substring(1,i);


        return Double.parseDouble(lat);
    }


    //Dialog
    public void openDialog(){
/*        DialogAdapter dialogAdapter=new DialogAdapter();
        dialogAdapter.show(getSupportFragmentManager(),"add dialog");
*/
       // addMarkerFromList(latlngList);
    }


    @Override
    public void addMarker(double latitude, double longitude) {
        googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude)));

        listRoad.add(new LatLng(latitude,longitude));

            makeRoute(listRoad);




    }

    private void makeRoute(List<LatLng> listRoad){


        if (listRoad.size()>1) {
            final List<Polyline> polylines;
            polylines = new ArrayList<>();
            final Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.WALKING)
                    .withListener(new RoutingListener() {
                        @Override
                        public void onRoutingFailure(RouteException e) {


                        }

                        @Override
                        public void onRoutingStart() {

                        }

                        @Override
                        public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {

                            for (i = 0; i < arrayList.size(); i++) {
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.BLUE);
                                polylineOptions.width(10 + i * 3);
                                polylineOptions.addAll(arrayList.get(i).getPoints());
                                Polyline polyline = googleMap.addPolyline(polylineOptions);
                                polylines.add(polyline);
                            }

                        }

                        @Override
                        public void onRoutingCancelled() {

                        }
                    }).waypoints(listRoad).build();
            routing.execute();
        }


    }

    private void getCoordonneeFromString(List<StringBuilder> listString){
        int i=0;
        double lon;
        double lat;
        while (i < listString.size()){

            lon = getLongitude(listString.get(i));
            lat=getLatitude(listString.get(i));

            latlngList.add(new LatLng(lat,lon));
           i++;
        }

    }


    //Ajout des marqueurs depuis le fichier
    private void addMarkerFromList(List<LatLng> coordonneeList){
              int i=0;
                while (i < 2){
                    googleMap.addMarker(new MarkerOptions().position(coordonneeList.get(i)));
                }
            }


}
