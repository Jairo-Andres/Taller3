package co.taller3

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        loadUsers()
    }

    private fun loadUsers() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.orderByChild("Estado").equalTo("Disponible").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userList = mutableListOf<User>()
                for (userSnapshot in dataSnapshot.children) {
                    val userUID = userSnapshot.key // Este es el UID del usuario
                    // Ahora accedemos a los detalles del usuario usando el UID
                    val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userUID!!)
                    if(userUID != currentUserID) {
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userDetailSnapshot: DataSnapshot) {
                                val user = userDetailSnapshot.getValue(User::class.java)
                                if (user != null && user.profileImageUrl != FirebaseAuth.getInstance().currentUser?.uid) {
                                    userList.add(user)
                                }
                                val listView: ListView = findViewById(R.id.listViewUsers)
                                val adapter = UserListAdapter(this@UserListActivity, userList)
                                listView.adapter = adapter
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                // Manejar posible error
                            }
                        })
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@UserListActivity, "Error fetching user data: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}