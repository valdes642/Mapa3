package com.example.mapa3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.maplibre.android.MapLibre;
import org.maplibre.android.WellKnownTileServer;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 1001;


    private MapView mapViewMapLibre;
    private com.google.android.gms.maps.MapView mapViewGoogle;
    private WebView vistaMapaWeb;


    private MapLibreMap mapLibreMap;
    private Style mapStyle;
    private GoogleMap googleMap;


    private int currentMapMode = 0;


    private static final LatLngBounds LA_FLORIDA_BOUNDS_MAPLIBRE = new LatLngBounds.Builder()
            .include(new LatLng(-33.4750, -70.4900))
            .include(new LatLng(-33.5750, -70.6350))
            .build();

    private static final com.google.android.gms.maps.model.LatLngBounds LA_FLORIDA_BOUNDS_GOOGLE =
            new com.google.android.gms.maps.model.LatLngBounds(
                    new com.google.android.gms.maps.model.LatLng(-33.5750, -70.6350), // Suroeste
                    new com.google.android.gms.maps.model.LatLng(-33.4750, -70.4900)  // Noreste
            );

    private static final LatLng LA_FLORIDA_CENTER = new LatLng(-33.5227, -70.5856);
    private static final com.google.android.gms.maps.model.LatLng LA_FLORIDA_CENTER_GOOGLE =
            new com.google.android.gms.maps.model.LatLng(-33.5227, -70.5856);

    private static final LatLng MI_CASA = new LatLng(-33.54782541642939, -70.58948501973495);
    private static final com.google.android.gms.maps.model.LatLng MI_CASA_GOOGLE =
            new com.google.android.gms.maps.model.LatLng(-33.54782541642939, -70.58948501973495);

    private static final String STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this, "", WellKnownTileServer.MapLibre);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapViewMapLibre = findViewById(R.id.mapViewMapLibre);
        mapViewGoogle = findViewById(R.id.mapViewGoogle);
        vistaMapaWeb = findViewById(R.id.vistaMapaWeb);
        MaterialButton btnToggleMap = findViewById(R.id.btnToggleMap);
        FloatingActionButton btnLocation = findViewById(R.id.btnLocation);

        mapViewMapLibre.onCreate(savedInstanceState);
        mapViewGoogle.onCreate(savedInstanceState);


        setupWebView();

        Bitmap houseBitmap = getBitmapFromVector(R.drawable.ic_house);
        Bitmap deliveryBitmap = getBitmapFromVector(R.drawable.ic_delivery);


        setupMapLibre(houseBitmap, deliveryBitmap);


        setupGoogleMaps(houseBitmap, deliveryBitmap);


        btnToggleMap.setOnClickListener(v -> {
            currentMapMode = (currentMapMode + 1) % 3;
            switch (currentMapMode) {
                case 0: // MapLibre (OSM)
                    mapViewGoogle.setVisibility(View.GONE);
                    vistaMapaWeb.setVisibility(View.GONE);
                    mapViewMapLibre.setVisibility(View.VISIBLE);
                    btnToggleMap.setText("Mapa: MapLibre (OSM)");
                    btnLocation.setVisibility(View.VISIBLE);
                    break;

                case 1: // Google Maps SDK
                    mapViewMapLibre.setVisibility(View.GONE);
                    vistaMapaWeb.setVisibility(View.GONE);
                    mapViewGoogle.setVisibility(View.VISIBLE);
                    btnToggleMap.setText("Mapa: Google Maps (SDK)");
                    btnLocation.setVisibility(View.VISIBLE);
                    break;

                case 2: // Google Maps Web HTML (assets/Mapa.html)
                    mapViewMapLibre.setVisibility(View.GONE);
                    mapViewGoogle.setVisibility(View.GONE);
                    vistaMapaWeb.setVisibility(View.VISIBLE);
                    btnToggleMap.setText("Mapa: Web (HTML Assets)");
                    btnLocation.setVisibility(View.GONE);
                    break;
            }
        });


        btnLocation.setOnClickListener(v -> {
            if (!hasLocationPermission()) {
                requestLocationPermissions();
                return;
            }

            if (currentMapMode == 1 && googleMap != null) {
                enableGoogleMapsLocation();
                android.location.Location loc = googleMap.getMyLocation();
                if (loc != null) {
                    com.google.android.gms.maps.model.LatLng currentPos =
                            new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 16f));
                } else {
                    Toast.makeText(this, "Obteniendo ubicación en Google Maps...", Toast.LENGTH_SHORT).show();
                }
            } else if (currentMapMode == 0 && mapLibreMap != null && mapStyle != null) {
                LocationComponent locationComponent = mapLibreMap.getLocationComponent();
                if (locationComponent.isLocationComponentActivated() && locationComponent.getLastKnownLocation() != null) {
                    LatLng userLocation = new LatLng(
                            locationComponent.getLastKnownLocation().getLatitude(),
                            locationComponent.getLastKnownLocation().getLongitude()
                    );
                    mapLibreMap.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(userLocation, 16.0)
                    );
                } else {
                    enableLocationComponent(mapStyle, mapLibreMap);
                    Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = vistaMapaWeb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        vistaMapaWeb.setWebViewClient(new WebViewClient());
        vistaMapaWeb.loadUrl("file:///android_asset/Mapa.html");
    }

    private void setupMapLibre(Bitmap houseBitmap, Bitmap deliveryBitmap) {
        mapViewMapLibre.getMapAsync(map -> {
            this.mapLibreMap = map;

            map.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
                this.mapStyle = style;

                CameraPosition position = new CameraPosition.Builder()
                        .target(MI_CASA)
                        .zoom(16.0)
                        .build();
                map.setCameraPosition(position);

                map.setLatLngBoundsForCameraTarget(LA_FLORIDA_BOUNDS_MAPLIBRE);
                map.setMinZoomPreference(13.0);
                map.setMaxZoomPreference(19.0);

                MarkerOptions deliveryOptions = new MarkerOptions()
                        .position(LA_FLORIDA_CENTER)
                        .title("Hola")
                        .snippet("Repartidor cerca");
                if (deliveryBitmap != null) {
                    deliveryOptions.icon(IconFactory.getInstance(this).fromBitmap(deliveryBitmap));
                }
                map.addMarker(deliveryOptions);

                MarkerOptions houseOptions = new MarkerOptions()
                        .position(MI_CASA)
                        .title("Mi Casa")
                        .snippet("Ubicación guardada");
                if (houseBitmap != null) {
                    houseOptions.icon(IconFactory.getInstance(this).fromBitmap(houseBitmap));
                }
                Marker casaMarker = map.addMarker(houseOptions);
                map.selectMarker(casaMarker);

                enableLocationComponent(style, map);
            });
        });
    }

    private void setupGoogleMaps(Bitmap houseBitmap, Bitmap deliveryBitmap) {
        mapViewGoogle.getMapAsync(gMap -> {
            this.googleMap = gMap;

            gMap.setLatLngBoundsForCameraTarget(LA_FLORIDA_BOUNDS_GOOGLE);
            gMap.setMinZoomPreference(13.0f);
            gMap.setMaxZoomPreference(19.0f);

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MI_CASA_GOOGLE, 16.0f));

            com.google.android.gms.maps.model.MarkerOptions deliveryOptions =
                    new com.google.android.gms.maps.model.MarkerOptions()
                            .position(LA_FLORIDA_CENTER_GOOGLE)
                            .title("Hola")
                            .snippet("Repartidor cerca");
            if (deliveryBitmap != null) {
                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(deliveryBitmap);
                deliveryOptions.icon(descriptor);
            }
            gMap.addMarker(deliveryOptions);

            com.google.android.gms.maps.model.MarkerOptions houseOptions =
                    new com.google.android.gms.maps.model.MarkerOptions()
                            .position(MI_CASA_GOOGLE)
                            .title("Mi Casa")
                            .snippet("Ubicación guardada");
            if (houseBitmap != null) {
                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(houseBitmap);
                houseOptions.icon(descriptor);
            }
            com.google.android.gms.maps.model.Marker houseMarker = gMap.addMarker(houseOptions);
            if (houseMarker != null) {
                houseMarker.showInfoWindow();
            }

            if (hasLocationPermission()) {
                enableGoogleMapsLocation();
            }
        });
    }

    private void enableGoogleMapsLocation() {
        if (googleMap != null && hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_LOCATION
        );
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle, @NonNull MapLibreMap map) {
        if (!hasLocationPermission()) {
            return;
        }

        LocationComponent locationComponent = map.getLocationComponent();
        LocationComponentActivationOptions options = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .build();

        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.NONE);
        locationComponent.setRenderMode(RenderMode.COMPASS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mapLibreMap != null && mapStyle != null) {
                    enableLocationComponent(mapStyle, mapLibreMap);
                }
                enableGoogleMapsLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap getBitmapFromVector(@DrawableRes int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return null;

        int width = vectorDrawable.getIntrinsicWidth() > 0 ? vectorDrawable.getIntrinsicWidth() : 96;
        int height = vectorDrawable.getIntrinsicHeight() > 0 ? vectorDrawable.getIntrinsicHeight() : 96;

        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapViewMapLibre != null) mapViewMapLibre.onStart();
        if (mapViewGoogle != null) mapViewGoogle.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewMapLibre != null) mapViewMapLibre.onResume();
        if (mapViewGoogle != null) mapViewGoogle.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewMapLibre != null) mapViewMapLibre.onPause();
        if (mapViewGoogle != null) mapViewGoogle.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapViewMapLibre != null) mapViewMapLibre.onStop();
        if (mapViewGoogle != null) mapViewGoogle.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewMapLibre != null) mapViewMapLibre.onSaveInstanceState(outState);
        if (mapViewGoogle != null) mapViewGoogle.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewMapLibre != null) mapViewMapLibre.onLowMemory();
        if (mapViewGoogle != null) mapViewGoogle.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapViewMapLibre != null) mapViewMapLibre.onDestroy();
        if (mapViewGoogle != null) mapViewGoogle.onDestroy();
    }
}