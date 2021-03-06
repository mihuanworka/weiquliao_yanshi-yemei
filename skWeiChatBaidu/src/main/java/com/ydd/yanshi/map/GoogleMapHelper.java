package com.ydd.yanshi.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.ydd.yanshi.util.AsyncUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GoogleMapHelper extends MapHelper {
    private static final String TAG = "GoogleMapHelper";
    @SuppressLint("StaticFieldLeak")
    private static GoogleMapHelper INSTANCE;
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private GoogleMapHelper(Context context) {
        this.context = context;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public static GoogleMapHelper getInstance(Context context) {
        // ?????????????????????????????????context,
        if (INSTANCE == null) {
            synchronized (GoogleMapHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GoogleMapHelper(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public boolean isAvailability() {
        boolean isGoogleAvailability = true;
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            // ???????????????activity??????????????????????????????????????????????????????????????????????????????
/*
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode, 2404).show();
            }
*/
            isGoogleAvailability = false;
        }
        return isGoogleAvailability;
    }

    @Override
    public String getStaticImage(LatLng latLng) {
        // ????????????????????????key???????????????????????????????????????????????????????????????????????????????????????
        // ???????????????key???????????????
        // ???????????????????????????????????????????????????????????????????????????????????????
//        String key = BuildConfig.GOOGLE_API_KEY;
        // ?????????????????????key, ??????????????????
        String key = "AIzaSyDIJ9XX2ZvRKCJcFRrl-lRanEtFUow4piM";
        return "http://maps.googleapis.com/maps/api/staticmap?" +
                "center=" + latLng.getLatitude() + "," + latLng.getLongitude() +
                "&size=640x480&markers=color:blue%7Clabel:S%7C62.107733,-145.541936" +
                "&zoom=15&key=" + key;
    }

    @Override
    public void requestLatLng(final OnSuccessListener<LatLng> onSuccessListener, final OnErrorListener onErrorListener) {
        final Task<Location> lastLocation;
        try {
            lastLocation = fusedLocationProviderClient.getLastLocation();
        } catch (SecurityException e) {
            if (onErrorListener != null) {
                onErrorListener.onError(new RuntimeException("??????????????????,", e));
            }
            return;
        }
        lastLocation.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                // ????????????google service???????????????????????????task????????????location?????????
                if (!task.isSuccessful() || location == null) {
                    if (onErrorListener != null) {
                        onErrorListener.onError(new RuntimeException("????????????,", task.getException()));
                    }
                    return;
                }
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (onSuccessListener != null) {
                    onSuccessListener.onSuccess(latLng);
                }
            }
        });
    }

    @Override
    public void requestPlaceList(LatLng latLng, final OnSuccessListener<List<Place>> onSuccessListener, final OnErrorListener onErrorListener) {
        AsyncUtils.doAsync(this, t -> {
            if (onErrorListener != null) {
                onErrorListener.onError(new RuntimeException("??????????????????????????????", t));
            }
        }, c -> {
            Geocoder geocoder = new Geocoder(context);
            List<Address> addressList = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 5);
            Log.i(TAG, "requestPlaceList: " + addressList);
            List<Place> result = new ArrayList<>();
            if (addressList != null) {
                for (Address address : addressList) {
                    String name = address.getThoroughfare();
                    // Thoroughfare????????????????????????
                    boolean hasThoroughfare = false;
                    if (TextUtils.isEmpty(name)) {
                        name = address.getFeatureName();
                    } else {
                        hasThoroughfare = true;
                    }
                    Place place = new Place(
                            name,
                            address.getAddressLine(0),
                            new LatLng(address.getLatitude(), address.getLongitude())
                    );
                    if (hasThoroughfare) {
                        result.add(0, place);
                    } else {
                        result.add(place);
                    }
                }
            }
            c.uiThread(r -> {
                if (onSuccessListener != null) {
                    onSuccessListener.onSuccess(result);
                }
            });
        });
    }

    @Override
    public void requestCityName(LatLng latLng, final OnSuccessListener<String> onSuccessListener, final OnErrorListener onErrorListener) {
        Geocoder geocoder = new Geocoder(context);
        List<Address> addressList;
        try {
            addressList = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
        } catch (IOException e) {
            if (onErrorListener != null) {
                onErrorListener.onError(new RuntimeException("??????????????????,", e));
            }
            return;
        }
        if (addressList == null || addressList.isEmpty()) {
            if (onErrorListener != null) {
                onErrorListener.onError(new RuntimeException("??????????????????,"));
            }
            return;
        }
        /*
            Address[addressLines=[0:"????????????????????????????????????????????? ????????????: 518000"],
            feature=????????????,admin=?????????,sub-admin=null,locality=?????????,thoroughfare=????????????,
            postalCode=518000,countryCode=CN,countryName=??????,hasLatitude=true,latitude=22.6032427,
            hasLongitude=true,longitude=114.05754379999999,phone=null,url=null,extras=null]
         */
        Address address = addressList.get(0);
        if (address == null || address.getLocality() == null) {
            if (onErrorListener != null) {
                onErrorListener.onError(new RuntimeException("??????????????????,"));
            }
            return;
        }
        if (onSuccessListener != null) {
            onSuccessListener.onSuccess(address.getLocality());
        }
    }

    @Override
    public Picker getPicker(Context context) {
        return new GoogleMapPicker(context);
    }


    private class GoogleMapPicker extends Picker implements OnMapReadyCallback {
        private MapView mapView;
        private Context context;
        private GoogleMap googleMap;
        private OnMapReadyListener onMapReadyListener;
        private com.google.android.gms.maps.model.Marker centerMarker;
        private Bitmap centerBitmap;

        private GoogleMapPicker(Context context) {
            this.context = context;
        }

        private void createMapView() {
            if (mapView == null) {
                mapView = new MapView(context);
                mapView.setClickable(true);
                mapView.setFocusable(true);
            }
        }

        @Override
        public void attack(FrameLayout container, OnMapReadyListener listener) {
            Log.d(TAG, "attack: ");
            this.onMapReadyListener = listener;
            createMapView();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            container.addView(mapView, params);
            mapView.getMapAsync(this);
        }

        @Override
        public MapView getMapView() {
            return mapView;
        }

        @Override
        public void snapshot(final Rect rect, final SnapshotReadyCallback callback) {
            googleMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
                @Override
                public void onSnapshotReady(Bitmap bitmap) {
                    int left = rect.left;
                    int top = rect.top;
                    int rw = rect.right - rect.left;
                    int rh = rect.bottom - rect.top;
                    callback.onSnapshotReady(Bitmap.createBitmap(bitmap, left, top, rw, rh));
                }
            });
        }

        @Override
        public void showMyLocation(LatLng latLng) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
        }

        @Override
        public void addCenterMarker(Bitmap bitmap, @Nullable String info) {
            centerBitmap = bitmap;
            if (googleMap == null) {
                Log.e(TAG, "addMarker: ?????????????????????", new RuntimeException("??????????????????????????????"));
                return;
            }
            createMapView();
            com.google.android.gms.maps.model.LatLng gLatLng = googleMap.getCameraPosition().target;
            // ??????Marker??????
            BitmapDescriptor gBitmap = BitmapDescriptorFactory.fromBitmap(bitmap);
            centerMarker = googleMap.addMarker(new MarkerOptions()
                    .icon(gBitmap)
                    .position(gLatLng)
                    .title(info));
        }

        @Override
        public void addMarker(LatLng latLng, Bitmap bitmap, @Nullable String info) {
            if (googleMap == null) {
                Log.e(TAG, "addMarker: ?????????????????????", new RuntimeException("??????????????????????????????"));
                return;
            }
            createMapView();
            com.google.android.gms.maps.model.LatLng gLatLng = new com.google.android.gms.maps.model.LatLng(latLng.getLatitude(), latLng.getLongitude());
            // ??????Marker??????
            BitmapDescriptor gBitmap = BitmapDescriptorFactory.fromBitmap(bitmap);
            googleMap.addMarker(new MarkerOptions()
                    .icon(gBitmap)
                    .position(gLatLng)
                    .title(info));
        }

        @Override
        public void clearMarker() {
            googleMap.clear();
            if (centerMarker != null) {
                addCenterMarker(centerBitmap, centerMarker.getTitle());
            }
        }

        @Override
        public LatLng currentLatLng() {
            CameraPosition cameraPosition = googleMap.getCameraPosition();
            com.google.android.gms.maps.model.LatLng target = cameraPosition.target;
            return new LatLng(target.latitude, target.longitude);
        }

        @Override
        public void moveMap(LatLng latLng, boolean anim) {
            if (googleMap == null) {
                Log.e(TAG, "moveMap: ?????????????????????", new RuntimeException("??????????????????????????????"));
                return;
            }
            createMapView();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    new com.google.android.gms.maps.model.LatLng(latLng.getLatitude(), latLng.getLongitude()),
                    15f);
            if (anim) {
                googleMap.animateCamera(cameraUpdate);
            } else {
                googleMap.moveCamera(cameraUpdate);
            }
        }

        @Override
        public void onMapReady(final GoogleMap googleMap) {
            Log.d(TAG, "onMapReady() called with: googleMap = [" + googleMap + "]");
            this.googleMap = googleMap;
            googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                @Override
                public void onCameraMoveStarted(int reason) {
                    if (REASON_GESTURE != reason) {
                        // ???????????????????????????????????????
                        // TODO: ????????????????????????????????????????????????move???idle??????????????????
                        Log.d(TAG, "onCameraMoveStarted() called with: i = [" + reason + "]");
                        return;
                    }
                    MapStatus mapStatus = new MapStatus();
                    com.google.android.gms.maps.model.LatLng target = googleMap.getCameraPosition().target;
                    mapStatus.target = new LatLng(target.latitude, target.longitude);
                    if (onMapStatusChangeListener != null) {
                        onMapStatusChangeListener.onMapStatusChangeStart(mapStatus);
                    }
                }
            });
            googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    MapStatus mapStatus = new MapStatus();
                    com.google.android.gms.maps.model.LatLng target = googleMap.getCameraPosition().target;
                    mapStatus.target = new LatLng(target.latitude, target.longitude);
                    if (onMapStatusChangeListener != null) {
                        onMapStatusChangeListener.onMapStatusChange(mapStatus);
                    }
                    if (centerMarker != null) {
                        // ??????????????????marker???????????????
                        centerMarker.setPosition(target);
                    }
                }
            });
            googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    MapStatus mapStatus = new MapStatus();
                    com.google.android.gms.maps.model.LatLng target = googleMap.getCameraPosition().target;
                    mapStatus.target = new LatLng(target.latitude, target.longitude);
                    if (onMapStatusChangeListener != null) {
                        onMapStatusChangeListener.onMapStatusChangeFinish(mapStatus);
                    }
                    if (centerMarker != null) {
                        // ??????????????????marker???????????????
                        centerMarker.setPosition(target);
                    }
                }
            });
/*
            googleMap.setOnCameraMoveCanceledListener(new GoogleMap.OnCameraMoveCanceledListener() {
                @Override
                public void onCameraMoveCanceled() {
                    MapStatus mapStatus = new MapStatus();
                    com.google.android.gms.maps.model.LatLng target = googleMap.getCameraPosition().target;
                    mapStatus.target = new LatLng(target.latitude, target.longitude);
                    onMapStatusChangeListener.onMapStatusChangeFinish(mapStatus);
                }
            });
*/

            googleMap.setOnMarkerClickListener(markerGoogle -> {
                if (onMarkerClickListener != null) {
                    Marker marker = new Marker();
                    // TODO: ?????????marker?????????bitmap,
                    marker.setLatLng(new LatLng(markerGoogle.getPosition().latitude, markerGoogle.getPosition().longitude));
                    marker.setInfo(markerGoogle.getTitle());
                    onMarkerClickListener.onMarkerClick(marker);
                }
                return true;
            });
            if (onMapReadyListener != null) {
                onMapReadyListener.onMapReady();
            }
        }

        @Override
        void create() {
            super.create();
            createMapView();
            mapView.onCreate(null);
        }

        @Override
        void start() {
            super.start();
            mapView.onStart();
        }

        @Override
        void resume() {
            super.resume();
            mapView.onResume();
        }

        @Override
        void pause() {
            super.pause();
            mapView.onPause();
        }

        @Override
        void stop() {
            super.stop();
            mapView.onStop();
        }

        @Override
        void destroy() {
            super.destroy();
            mapView.onDestroy();
        }
    }

}
