package com.example.chatcircle.data.remote
//talks to firebase authentication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor( //when FirebaseAuthDataSource is created, give it an object FirebaseAuth
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun signUp(
        email: String,
        password: String
    ): FirebaseUser? {
        return firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .await()
            .user
    }

    suspend fun signIn(
        email: String,
        password: String
    ): FirebaseUser? {
        return firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
            .user
    }

    suspend fun signInWithGoogle(
        idToken: String
    ): FirebaseUser? {
        val credential = com.google.firebase.auth.GoogleAuthProvider
            .getCredential(idToken, null)

        return firebaseAuth
            .signInWithCredential(credential)
            .await()
            .user
    }

    fun currentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}