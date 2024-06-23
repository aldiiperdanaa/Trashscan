package com.devforge.trashscan.model

import com.google.firebase.firestore.GeoPoint

data class Bank (
    var id: String?,
    var name: String,
    var photoUrl: String,
    var latitude: String,
    var longitude: String,
    var location: String,
    var exchange: Int
)