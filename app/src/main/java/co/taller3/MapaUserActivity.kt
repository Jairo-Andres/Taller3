package co.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller3.databinding.ActivityMapasBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapasUserActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapasBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var userReference: DatabaseReference
    private lateinit var userId: String
    var latitud = 0.0
    var longitud = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val currentUser = auth.currentUser
        userId = currentUser?.uid ?: ""
        userReference = database.reference.child("users").child(userId)
        getUserStatus()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermission()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this@MapasUserActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MapasUserActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

            }
            ActivityCompat.requestPermissions(
                this@MapasUserActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else if (ActivityCompat.checkSelfPermission(
                this@MapasUserActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MapasUserActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setLocation()
                }
                return
            }

            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setLocation()
                }
                return
            }

            else -> {
                //algo mas
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                this@MapasUserActivity, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MapasUserActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Obtener la última ubicación conocida
            mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    // Marcador para la ubicación actual
                    val miUbicacion = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(miUbicacion).title("Mi ubicación"))

                    // Mover la cámara a la ubicación actual
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))

                    // Llamar a la función para agregar la nueva ubicación y trazar la línea
                    agregarNuevaUbicacionYLinea()
                }
            }
        } else {
            // Si no hay permisos de ubicación, solicitarlos
            requestPermission()
        }

        // Configurar la interfaz de usuario del mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
    }

    // Función para agregar la nueva ubicación y trazar la línea roja
    private fun agregarNuevaUbicacionYLinea() {
        // Crear un marcador en la nueva ubicación
        val nuevaUbicacion = LatLng(4.668333327232298, -74.134278036654)
        mMap.addMarker(MarkerOptions().position(nuevaUbicacion).title("Nueva ubicación"))

        // Obtener la ubicación actual del marcador "Mi ubicación"
        val miUbicacion = mMap.addMarker(MarkerOptions().title("Mi ubicación"))?.position

        // Verificar si se pudo obtener la ubicación actual
        if (miUbicacion != null) {
            // Dibujar una línea roja entre la ubicación actual y la nueva ubicación
            mMap.addPolyline(
                PolylineOptions()
                    .add(miUbicacion, nuevaUbicacion)
                    .width(5f)
                    .color(Color.RED)
            )
        }
    }

    private fun fetchAndUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    updateLocationInFirebase(location.latitude, location.longitude)
                }
            }
            return
        }

    }

    private fun updateLocationInFirebase(lat: Double, lon: Double) {
        val userLocationRef = database.reference.child("Users").child(userId)

        // Escuchar solo una vez para los cambios en el nodo de ubicación del usuario
        userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dbLat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val dbLon = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                // Comprobar si la diferencia es significativa para actualizar
                if (Math.abs(dbLat - lat) > 0.000001 || Math.abs(dbLon - lon) > 0.000001) {
                    // Actualizar la latitud y longitud
                    val updates = mapOf(
                        "latitude" to lat,
                        "longitude" to lon
                    )
                    userLocationRef.updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(
                            this@MapasUserActivity,
                            "Ubicación actualizada en Firebase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener { exception ->
                        Log.e("Firebase", "Error al actualizar la ubicación", exception)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer la ubicación", error.toException())
            }
        })
    }

    private fun setLocation() {
        //setear la localización
        if (ActivityCompat.checkSelfPermission(
                this@MapasUserActivity, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MapasUserActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i("LOCATION", "onSucess location")
                if (location != null) {
                    latitud = location.latitude
                    longitud = location.longitude
                    Log.i("miLatitud", latitud.toString())
                    Log.i("miLongitud", longitud.toString())
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_status -> {
                userReference.child("Estado")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentState = snapshot.getValue(String::class.java)

                            val newStatus =
                                if (currentState == "Disponible") "No disponible" else "Disponible"

                            userReference.child("Estado").setValue(newStatus)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@MapasUserActivity,
                                        "Estado cambiado a $newStatus",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this@MapasUserActivity,
                                        "Error al cambiar el estado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@MapasUserActivity,
                                "Error al obtener el estado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                true
            }

            R.id.verDisponibles -> {
                val intent = Intent(this, UserListActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.action_logout -> {
                auth.signOut()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getUserStatus() {
        userReference.child("Estado").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentStatus = snapshot.getValue(String::class.java)
                if (currentStatus == null) {
                    // Si no hay estado, establecer como "Disponible"
                    userReference.child("Estado").setValue("Disponible")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MapasUserActivity,
                    "Error al obtener el estado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

}
