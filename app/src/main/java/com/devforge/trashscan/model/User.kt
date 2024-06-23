package com.devforge.trashscan.model

import com.google.firebase.Timestamp

data class User (
    var fullname: String,
    var username: String,
    var password: String,
    var created: Timestamp
)