package com.example.runningappmvvm.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.runningappmvvm.R
import com.example.runningappmvvm.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningappmvvm.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningappmvvm.other.Constants.ACTION_STOP_SERVICE
import com.example.runningappmvvm.other.Constants.FASTEST_LOCATION_INTERVAL
import com.example.runningappmvvm.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningappmvvm.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningappmvvm.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runningappmvvm.other.Constants.NOTIFICATION_ID
import com.example.runningappmvvm.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningappmvvm.other.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService: LifecycleService() {

    private var isFirstRun = true
    private var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object{
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()

        val timeRunInMillis = MutableLiveData<Long>()
    }

    private fun postInitialValues(){
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInMillis.postValue(0L)
        timeRunInSeconds.postValue(0L)
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    private fun killService(){
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun){
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d(ACTION_PAUSE_SERVICE)
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d(ACTION_PAUSE_SERVICE)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer(){
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking){
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, TrackingUtility.getPendingIntentFlag())
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, TrackingUtility.getPendingIntentFlag())
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        baseNotificationBuilder.clearActions()
//        currentNotificationBuilder.javaClass.getDeclaredField("mAction").apply {
//            isAccessible = true
//            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
//        }
        if (!serviceKilled){
            baseNotificationBuilder
                .addAction(R.drawable.ic_pause, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, baseNotificationBuilder.build())
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean){
        if (isTracking){
            if (TrackingUtility.hasLocationPermissions(this)){
                val request = LocationRequest.create().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!){
                result.locations.let { locations ->
                    for (location in locations){
                        addPathPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location?){
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService(){
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, {
            if (!serviceKilled){
                baseNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedStpWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, baseNotificationBuilder.build())
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}