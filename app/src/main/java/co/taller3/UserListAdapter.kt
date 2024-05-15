package co.taller3

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.example.taller3.R

class UserListAdapter(context: Context, private val users: MutableList<User>) :
    ArrayAdapter<User>(context, R.layout.activity_user_list_adapter, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        val viewHolder: ViewHolder

        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.activity_user_list_adapter, parent, false)
            viewHolder = ViewHolder(itemView)
            itemView.tag = viewHolder
        } else {
            viewHolder = itemView.tag as ViewHolder
        }

        val currentUser = users[position]
        viewHolder.nameTextView.text = currentUser.name
        viewHolder.lastNameTextView.text = currentUser.lastName
        Glide.with(context).load(currentUser.profileImageUrl).into(viewHolder.fotoPerfilImageView)

        viewHolder.btnverUbicacion.setOnClickListener {
            val intent = Intent(context, MapasUserActivity::class.java)
            context.startActivity(intent)
        }

        return itemView!!
    }

    private class ViewHolder(view: View) {
        var fotoPerfilImageView: ImageView = view.findViewById(R.id.fotoPerfilImageView)
        var nameTextView: TextView = view.findViewById(R.id.NameTextView)
        var lastNameTextView: TextView = view.findViewById(R.id.LastNameTextView)
        var btnverUbicacion: Button = view.findViewById(R.id.btnverUbicacion)
    }
}