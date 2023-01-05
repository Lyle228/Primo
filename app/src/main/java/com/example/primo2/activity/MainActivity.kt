package com.example.primo2.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.primo2.PostInfo
import com.example.primo2.screen.Posts
import com.example.primo2.screen.PrimoApp
import com.example.primo2.ui.theme.LazyColumnExampleTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrimoApp(this)
        }
    }
}

//@Preview
//@Composable
//fun DefaultPreview() {
//    LazyColumnExampleTheme() {
//        PrimoApp()
//    }
//}

/*
class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth
        val user = Firebase.auth.currentUser
        if(user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        else{
                val db = Firebase.firestore
                val docRef = db.collection("users").document(user.uid)
                docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.exists()){

                        }
                        else{
                            val intent = Intent(this, MemberInitActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    else {

                    }
                }
                .addOnFailureListener {
                }
            val recyclerView: RecyclerView = findViewById<RecyclerView>(R.id.postRecyclerView)

            val linearLayout = LinearLayoutManager(applicationContext)

            db.collection("posts")
                //.whereEqualTo("capital", true)
                .get()
                .addOnSuccessListener { documents ->
                    val postList:ArrayList<PostInfo> = arrayListOf()

                    for (document in documents) {
                        postList.add(
                            PostInfo(
                                document.getString("title"),
                                document.data["contents"] as ArrayList<String?>,
                                document.get("comments").toString(),
                                document.getString("writer"),
                                document.getString("postDate")
                            )
                        )
                    }



                    recyclerView.layoutManager = linearLayout
                    recyclerView.adapter = PostAdapter(this,postList)
                }
                .addOnFailureListener { exception ->
                    //Log.w(TAG, "Error getting documents: ", exception)
                }


        }
        findViewById<FloatingActionButton>(R.id.uploadPostButton).setOnClickListener{
            uploadPost()
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener{
            logout()
        }
    }
    private fun uploadPost()
    {
        val intent = Intent(this, UploadPostActivity::class.java)
        startActivity(intent)
    }
    private fun logout(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }



}
 */