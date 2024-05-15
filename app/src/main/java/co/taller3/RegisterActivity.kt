package co.taller3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.example.taller3.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import java.io.FileNotFoundException

class RegisterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var mMap: GoogleMap
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private val REQUEST_PICK_IMAGE = 1000
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val lastNameEditText = findViewById<EditText>(R.id.lastNameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val identificationEditText = findViewById<EditText>(R.id.identificationNumberEditText)
        val latitudeEditText = findViewById<EditText>(R.id.latitudeEditText)
        val longitudeEditText = findViewById<EditText>(R.id.longitudeEditText)
        val selectImageButton = findViewById<Button>(R.id.selectImageButton)
        val registerButton = findViewById<Button>(R.id.registerButton)


        setupMap()

        selectImageButton.setOnClickListener {
            openGallery()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val identificationNumber = identificationEditText.text.toString().trim()
            val latitude = latitudeEditText.text.toString().trim()
            val longitude = longitudeEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty() && name.isNotEmpty() && lastName.isNotEmpty() && identificationNumber.isNotEmpty() && imageUri != null) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            uploadImageToFirebase(imageUri) { imageUrl ->
                                val userData = hashMapOf(
                                    "name" to name,
                                    "lastName" to lastName,
                                    "email" to email,
                                    "identificationNumber" to identificationNumber,
                                    "latitude" to latitude,
                                    "longitude" to longitude,
                                    "profileImageUrl" to imageUrl
                                )
                                FirebaseDatabase.getInstance().getReference("Users").child(it.uid).setValue(userData).addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MapasActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Fallo en la autenticación", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingrese todos los campos y seleccione una imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            findViewById<ImageView>(R.id.imageSelectedCheck).visibility = View.VISIBLE
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                findViewById<EditText>(R.id.latitudeEditText).setText(location.latitude.toString())
                findViewById<EditText>(R.id.longitudeEditText).setText(location.longitude.toString())
            }
        }
    }
    private fun uploadImageToFirebase(imageUri: Uri?, callback: (String) -> Unit) {
        val contentResolver = applicationContext.contentResolver
        imageUri?.let { uri ->
            Log.d("URI_DEBUG", "URI: $uri") // Verifica que la URI sea correcta

            val ref = storage.reference.child("users/${FirebaseAuth.getInstance().currentUser?.uid}/profile.jpg")
            contentResolver.openInputStream(uri)?.let { inputStream ->
                val uploadTask = ref.putStream(inputStream)
                uploadTask.addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        callback(downloadUri.toString())
                        findViewById<ImageView>(R.id.imageSelectedCheck).visibility = View.VISIBLE
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Fallo al subir imagen: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } ?: Toast.makeText(this, "No se pudo abrir el InputStream para el URI proporcionado", Toast.LENGTH_LONG).show()
        } ?: Toast.makeText(this, "Error: No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
    }
}
