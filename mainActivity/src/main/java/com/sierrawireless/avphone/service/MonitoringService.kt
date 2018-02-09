package com.sierrawireless.avphone.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Binder
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.telephony.*
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.task.SendDataParams
import com.sierrawireless.avphone.task.SendDataTask
import com.sierrawireless.avphone.tools.Tools
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.jetbrains.anko.toast
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MonitoringService : Service() {

    private val TAG = this::class.java.name

    // system services
    private var telephonyManager: TelephonyManager? = null
    private var activityManager: ActivityManager? = null
    private var connManager: ConnectivityManager? = null
    var set: Boolean? = false

    private var client: MqttPushClient? = null

    var startedSince: Long? = null

    var lastRun: Long? = null
        private set
    var lastLog: String? = null
        private set
    val lastData = NewData()
    /* the date of the last location reading */
    private var lastLocation: Long = 0

    private var networkLocation: Location? = null
    private var gpsLocation: Location? = null
    private var networkLocationListener: LocationListener? = null
    private var gpsLocationListener: LocationListener? = null
    private var objectsManager: ObjectsManager? = null

    private var phoneStateListener: PhoneStateListener? = null

    private var dbm: Int? = null

    private var timer:Timer? = null


    private val lastKnownLocation: Location?
        @SuppressLint("MissingPermission")
        get() {
            val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val locationProvider = locManager.getBestProvider(Criteria(), true)
            Log.i(TAG, "Getting last known location from provider: " + locationProvider!!)

            val location: Location? = locManager.getLastKnownLocation(locationProvider)
            if (location != null) {
                Log.i(TAG, "Last known location : " + location.latitude + "," + location.longitude)
            } else {
                Log.i(TAG, "Read null location")
            }
            if (networkLocation != null) {
                Log.i(TAG, "Last Network Location : " + networkLocation!!.latitude + "," + networkLocation!!.longitude)
            } else {
                Log.i(TAG, "No known network location")
            }
            if (gpsLocation != null) {
                Log.i(TAG, "Last GPS Location : " + gpsLocation!!.latitude + "," + gpsLocation!!.longitude)
            } else {
                Log.i(TAG, "No known GPSlocation")
            }

            return location
        }

    // Service binding

    private val binder = ServiceBinder()

    // MQTT client callback

    private val mqttCallback = object : MqttCallback {

        internal inner class Message {
            var timestamp: Long = 0
            var command: Command? = null
        }

        internal inner class Command {
            var params: Map<String, String>? = null
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, msg: MqttMessage) {
            Log.i(TAG, "MQTT msg received: " + String(msg.payload))

            // parse json payload
            val messages = Gson().fromJson(String(msg.payload, Charset.forName("UTF-8")), Array<Message>::class.java)

            // display a new notification
            @Suppress("DEPRECATION")
            val notification = Notification.Builder(this@MonitoringService.applicationContext) //
                    .setContentTitle(getText(R.string.notif_new_message)) //
                    .setContentText(messages[0].command!!.params!!["message"]) //
                    .setSmallIcon(R.drawable.ic_notif) //
                    .setAutoCancel(true) //
                    .build()

            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(messages[0].timestamp.toInt(), notification)
        }

        override fun deliveryComplete(arg0: IMqttDeliveryToken) {
            //
        }

        override fun connectionLost(arg0: Throwable) {
            //
        }
    }

    override fun onCreate() {
        // Unique Identification Number for the Notification.
        objectsManager = ObjectsManager.getInstance()

        // Display a notification icon


        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (signalStrength == null)
                    return
                if (telephonyManager!!.networkType == TelephonyManager.NETWORK_TYPE_LTE) {
                    val parts = signalStrength.toString().split(" ")
                    dbm = parts[8].toInt() - 240
                }else{
                    if (signalStrength.gsmSignalStrength != 99) {
                        dbm = -113 + 2 * signalStrength.gsmSignalStrength
                    }
                }

            }

        }


        telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)


        startedSince = System.currentTimeMillis()
        start()

    }

    fun start() {
        //protect multiple start
        if (timer == null) {
            //periodic timer of 5 s do avoid
            timer = fixedRateTimer("UItimer", false, 0, 2000L) {
                setCustomDataForUi()
            }
        }
        //setUpLocationListeners()
    }



    fun startSendData() {
        val notif = R.string.notif_title
        // Create an intent to start the activity when clicking the notification
        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        @Suppress("DEPRECATION")
        val notification = Notification.Builder(this.applicationContext) //
                .setContentTitle(getText(R.string.notif_title)) //
                .setContentText(getText(R.string.notif_desc)) //
                .setSmallIcon(R.drawable.ic_notif) //
                .setOngoing(true) //
                .setContentIntent(resultPendingIntent) //
                .build()

        startForeground(notif, notification)
        setUpLocationListeners()
    }

    fun stopSendData() {

        // Cancel the persistent notification.
        stopForeground(true)
        stopLocationListeners()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun setCustomDataForUi():NewData {
        val location = lastKnownLocation

        // retrieve data
        val data = NewData()

        if (dbm != null) {
            data.rssi = dbm!!
        }else {

            val cellInfos = telephonyManager!!.allCellInfo
            if (cellInfos != null && !cellInfos.isEmpty()) {
                val cellInfo = cellInfos[0]
                if (cellInfo is CellInfoGsm) {
                    data.rssi = cellInfo.cellSignalStrength.dbm
                    // } else if (cellInfo instanceof CellInfoWcdma) {
                    // RSSI ?
                    // data.setRssi(((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm());
                } else if (cellInfo is CellInfoLte) {
                    data.rsrp = cellInfo.cellSignalStrength.dbm
                }
            }
        }

        if (telephonyManager!!.phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            @Suppress("DEPRECATION")
            data.imei = telephonyManager!!.deviceId
        }

        data.operator = telephonyManager!!.networkOperatorName

        when (telephonyManager!!.networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> data.networkType = "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> data.networkType = "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> data.networkType = "UMTS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> data.networkType = "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSPAP -> data.networkType = "HSPA+"
            TelephonyManager.NETWORK_TYPE_HSPA -> data.networkType = "HSPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> data.networkType = "HSUPA"
            TelephonyManager.NETWORK_TYPE_LTE -> data.networkType = "LTE"
        }// to be continued


        // location
        if (location != null) {
            data.latitude = location.latitude
            data.longitude = location.longitude
            lastLocation = location.time
        }
        val connManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)

        when {
            mWifi.isConnected -> {
                // bytes sent/received
                data.bytesReceived = TrafficStats.getTotalRxBytes()
                data.bytesSent = TrafficStats.getTotalTxBytes()
            }
            mMobile.isConnected -> {
                data.bytesReceived = TrafficStats.getMobileRxBytes()
                data.bytesSent = TrafficStats.getMobileTxBytes()
            }
            else -> {
                data.bytesReceived = 0
                data.bytesSent = 0
            }
        }
        //execute action on current object datas
        //objectsManager!!.execOnCurrent()
        // Custom data
        data.setCustom()


        // dispatch new data event to update the activity UI
        LocalBroadcastManager.getInstance(this).sendBroadcast(data)
        return data
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        lastRun = System.currentTimeMillis()
        objectsManager = ObjectsManager.getInstance()
        val obj = objectsManager!!.currentObject


        try {
            val mustConnect = intent.getBooleanExtra(CONNECT, true)

            /* First we have to create the system if it doesn't exist */

            if (this.client == null) {

                //
                // Ensure intent is valid
                //
                val deviceId = Tools.buildSerialNumber(intent.getStringExtra(DEVICE_ID), obj!!.name!!)
                val password = intent.getStringExtra(PASSWORD)
                val serverHost = intent.getStringExtra(SERVER_HOST)

                val intentValuesList = Arrays.asList(deviceId, password, serverHost)
                if (intentValuesList.contains(null)) {
                    // Stop service when unable to start MQTT client
                    stopSelfResult(startId)
                    return Service.START_STICKY
                }

                // Now, create client
                client = MqttPushClient(deviceId, password, serverHost, mqttCallback)
            }


            if (mustConnect) {

                val data = setCustomDataForUi()


                // save new data values
                if (data.extras != null) {
                    lastData.putExtras(data.extras!!)
                }


                val sendDataTask = SendDataTask()
                val params = SendDataParams()
                params.client = client
                params.data = data
                params.context = this
                params.alarm = false

                sendDataTask.execute(params)


                sendDataTask.addProgressListener { result -> lastLog = result.lastLog }

               // setUpLocationListeners()
            }

        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "error", e)
            lastLog = "ERROR: " + e.message
            LocalBroadcastManager.getInstance(this).sendBroadcast(LogMessage(lastLog!!, false))
        }


        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            this.client?.disconnect()
        } catch (e: MqttException) {
            Crashlytics.logException(e)
            Log.e(TAG, "error", e)
        }

        // Cancel the persistent notification.
        stopForeground(true)

        stopLocationListeners()
        timer?.cancel()
    }

    fun cancel(){
        timer?.cancel()
      //  stopLocationListeners()
        timer = null
    }

    @SuppressLint("MissingPermission")
    private fun setUpLocationListeners() {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val networkLocationProvider = locManager.getProvider(LocationManager.NETWORK_PROVIDER)
        if (networkLocationProvider != null) {
            networkLocationListener = object : LocationListenerAdapter() {
                override fun onLocationChanged(location: Location) {
                    Log.i(TAG, "Received Network location update " + location.latitude + ";" + location.longitude)
                    networkLocation = location
                }
            }

            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (60 * 1000).toLong(), 5f, networkLocationListener)
        }
        val gpsLocationProvider = locManager.getProvider(LocationManager.GPS_PROVIDER)
        if (gpsLocationProvider != null) {
            gpsLocationListener = object : LocationListenerAdapter() {
                override fun onLocationChanged(location: Location) {
                    Log.i(TAG, "Received GPS location update " + location.latitude + ";" + location.longitude)
                    gpsLocation = location
                }
            }
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (60 * 1000).toLong(), 5f, gpsLocationListener)
        }
    }

    private fun stopLocationListeners() {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (networkLocationListener != null) {
            locManager.removeUpdates(networkLocationListener)
        }
        if (gpsLocationListener != null) {
            locManager.removeUpdates(gpsLocationListener)
        }
    }

    fun sendAlarmEvent(on:Boolean) {

        if (this.client == null) {
            toast("Alarm client is not available,wait...")
            return
        }

        val data = NewData()
        set = on
        data.isAlarmActivated = on

        // save alarm state
        //   if (data.getExtras() != null) {
        //       lastData.putExtras(data.getExtras());
        //   }

        val sendDataTask = SendDataTask()
        val params = SendDataParams()
        params.client = client
        params.data = data
        params.context = this
        params.alarm = true
        params.value = on

        sendDataTask.execute(params)


        sendDataTask.addProgressListener { result ->
            lastLog = result.lastLog
        }

    }

    override fun onBind(arg0: Intent): IBinder? {
        return binder
    }

    inner class ServiceBinder : Binder() {

        val service: MonitoringService
            get() = this@MonitoringService
    }

    companion object {
        // Intent extra keys
        const val DEVICE_ID = "device_id"
        const val SERVER_HOST = "server_host"
        const val PASSWORD = "password"
        const val CONNECT = "connect"
        const val OBJECT_NAME = "objname"
    }

}
