package com.safecircle.app.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.safecircle.app.data.EmergencyContact
import com.safecircle.app.data.SafeCirclePreferences
import com.safecircle.app.safety.NearbyCircleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SafeCircleViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs = SafeCirclePreferences(context)
    private val nearby = NearbyCircleManager(context)
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _name = MutableStateFlow(prefs.name)
    val name: StateFlow<String> = _name.asStateFlow()
    private val _contacts = MutableStateFlow(prefs.contacts())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()
    private val _circleCode = MutableStateFlow(prefs.circleCode)
    val circleCode: StateFlow<String> = _circleCode.asStateFlow()
    private val _emergencyNumber = MutableStateFlow(prefs.emergencyNumber)
    val emergencyNumber: StateFlow<String> = _emergencyNumber.asStateFlow()
    val peerCount = nearby.peerCount
    private val _lastStatus = MutableStateFlow("Ready")
    val lastStatus: StateFlow<String> = _lastStatus.asStateFlow()

    init { nearby.start() }

    fun saveProfile(name: String, code: String, emergencyNumber: String) {
        prefs.name = name.trim().ifBlank { "Me" }
        prefs.circleCode = code.filter(Char::isDigit).take(8).ifBlank { "246810" }
        prefs.emergencyNumber = emergencyNumber.trim().ifBlank { "999" }
        _name.value = prefs.name
        _circleCode.value = prefs.circleCode
        _emergencyNumber.value = prefs.emergencyNumber
        nearby.stop(); nearby.start()
        _lastStatus.value = "Settings saved"
    }

    fun saveContact(index: Int, name: String, phone: String) {
        val contact = if (name.isBlank() || phone.isBlank()) null else EmergencyContact(name.trim(), phone.trim())
        prefs.saveContact(index, contact)
        _contacts.value = prefs.contacts()
    }

    fun triggerSos() {
        _lastStatus.value = "Sending SOS…"
        getLastLocation { location ->
            nearby.broadcastSos(location?.latitude, location?.longitude)
            sendSmsAlerts(location)
            _lastStatus.value = if (peerCount.value > 0 || contacts.value.isNotEmpty()) {
                "SOS sent to your available safety channels"
            } else {
                "SOS prepared, but no circle/contact channel is available"
            }
        }
    }

    fun callEmergency() {
        val number = prefs.emergencyNumber
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(intent)
        } else {
            _lastStatus.value = "Phone permission is required to call $number"
        }
    }

    private fun sendSmsAlerts(location: Location?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return
        val locationText = location?.let { " Location: https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: " Location unavailable."
        val message = "SafeCircle SOS from ${prefs.name}. I may need help.$locationText"
        val sms = SmsManager.getDefault()
        contacts.value.forEach { contact ->
            runCatching { sms.sendTextMessage(contact.phone, null, message, null, null) }
        }
    }

    private fun getLastLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback(null); return
        }
        locationClient.lastLocation.addOnSuccessListener(callback).addOnFailureListener { callback(null) }
    }

    override fun onCleared() {
        nearby.stop()
        super.onCleared()
    }
}
