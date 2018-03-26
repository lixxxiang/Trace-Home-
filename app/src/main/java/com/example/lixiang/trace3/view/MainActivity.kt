package com.example.lixiang.trace3.view

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.*
import com.baidu.trace.LBSTraceClient
import com.baidu.trace.Trace
import com.baidu.trace.model.OnTraceListener
import com.baidu.trace.model.PushMessage
import com.baidu.trace.api.track.HistoryTrackRequest
import com.baidu.trace.api.track.HistoryTrackResponse
import com.baidu.trace.api.track.OnTrackListener
import com.example.lixiang.trace3.R
import com.example.lixiang.trace3.util.DeviceUtils
import com.example.lixiang.trace3.util.Logger
import kotlinx.android.synthetic.main.activity_main.*
import com.baidu.location.*
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.Utils
import com.example.emall_core.util.dimen.DimenUtil
import com.example.lixiang.trace3.util.SpannableBuilder
import com.githang.statusbar.StatusBarCompat
import kotlinx.android.synthetic.main.dialog_view.*
import java.lang.System.exit
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    internal var serviceId: Long = 162046
    internal var entityName = ""
    var timeString = ""
    internal var isNeedObjectStorage = false
    var mTrace = Trace()
    lateinit var mTraceClient: LBSTraceClient
    var tag = 1
    var mMapView: MapView? = null
    private var lastX: Double? = 0.0
    private var mCurrentDirection = 0
    private var mCurrentLat = 0.0
    private var mCurrentLon = 0.0
    private var mCurrentAccracy: Float = 0.toFloat()
    var myListener = MyLocationListenner()
    private var mLocClient: LocationClient? = null
    private var mBaiduMap: BaiduMap? = null
    private var isFirstLoc = true
    private var locData: MyLocationData? = null
    private var mSensorManager: SensorManager? = null
    private var timeDiff = ""
    var handler = Handler()
    var handler2 = Handler()
    var handler3 = Handler()
    var handler4 = Handler()
    var inputName = false
    var name = ""
    private var exitTime: Long = 0
    var runnable: Runnable = object : Runnable {
        override fun run() {
            // TODO Auto-generated method stub
            getTime(timeString)
            handler.postDelayed(this, 2000)
        }
    }
    var runnable2: Runnable = object : Runnable {
        override fun run() {
            // TODO Auto-generated method stub
            getTime2(clickTime)
            handler2.postDelayed(this, 2000)
        }
    }
    var clickTime = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SDKInitializer.initialize(applicationContext)
        setContentView(R.layout.activity_main)
        StatusBarCompat.setStatusBarColor(this, Color.WHITE, true)
        Utils.init(application)
        timeString = getToday() + " 08:00:00"
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        handlePermisson()
        initMap()
        start_rl.setOnClickListener {
            if (!inputName){
                Toast.makeText(this,"input name first", Toast.LENGTH_LONG).show()
            }else{
                val gatherInterval = 5
                val packInterval = 10
//                entityName = DeviceUtils.getUniqueId(this)
                entityName = name
                Logger().d(entityName)
                mTrace = Trace(serviceId, entityName, isNeedObjectStorage)
                mTraceClient = LBSTraceClient(applicationContext)
                mTraceClient.setInterval(gatherInterval, packInterval)
                mTraceClient.startTrace(mTrace, mTraceListener)
                mTraceClient.startGather(mTraceListener)
                clickTime = TimeUtils.millis2String(System.currentTimeMillis())
                handler2.postDelayed(runnable2, 0)
                val historyTrackRequest = HistoryTrackRequest(tag, serviceId, entityName)
                val startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60
                val endTime = System.currentTimeMillis() / 1000
                historyTrackRequest.startTime = startTime
                historyTrackRequest.endTime = endTime

                val mTrackListener = object : OnTrackListener() {
                    override fun onHistoryTrackCallback(response: HistoryTrackResponse?) {
//                    Logger().d(response!!)
                    }
                }

                mTraceClient.queryHistoryTrack(historyTrackRequest, mTrackListener)

                start_rl.startAnimation(slideDown(start_rl))
                handler3.postDelayed(
                        {
                            end_rl.startAnimation(slideUp(end_rl))
                        }, 500)
            }

        }

        end_rl.setOnClickListener {
            handler2.removeCallbacks(runnable2)
            mTraceClient.stopTrace(mTrace, mTraceListener)
            mTraceClient.stopGather(mTraceListener)
            end_rl.startAnimation(slideDown(end_rl))
            handler3.postDelayed(
                    {
                        start_rl.startAnimation(slideUp(start_rl))
                    }, 500)
        }

        handler.postDelayed(runnable, 0)

        btn.setOnClickListener {
            showDialog()
        }
    }

    fun showDialog() {
        val customizeDialog = AlertDialog.Builder(this@MainActivity)
        var dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_view, null)
        customizeDialog.setTitle("YOUR NAME IS")
        customizeDialog.setView(dialogView)
        customizeDialog.setPositiveButton("ENTER"
        ) { dialog, which ->
            // 获取EditView中的输入内容
            var et = dialogView.findViewById<EditText>(R.id.edit_text)
            Logger().d(et.text.toString())
            name = et.text.toString()
            inputName = true
        }
        customizeDialog.show()
    }

    private fun slideDown(rl: RelativeLayout): AnimationSet {
        val animationSet = AnimationSet(true)
        val translateAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f)
        translateAnimation.duration = 500
        translateAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                val brParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, DimenUtil().dip2px(applicationContext, 230F))
                brParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                brParams.setMargins(DimenUtil().dip2px(applicationContext, 10F), 0, DimenUtil().dip2px(applicationContext, 10F), DimenUtil().dip2px(applicationContext, -230F))
                rl.layoutParams = brParams
            }
        })
        animationSet.addAnimation(translateAnimation)
        return animationSet
    }

    private fun slideUp(rl: RelativeLayout): AnimationSet {
        val animationSet = AnimationSet(true)
        val translateAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f)
        translateAnimation.duration = 500
        translateAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                handler4.postDelayed(
                        {
                            val brParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, DimenUtil().dip2px(applicationContext, 230F))
                            brParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            brParams.setMargins(DimenUtil().dip2px(applicationContext, 10F), 0, DimenUtil().dip2px(applicationContext, 10F), DimenUtil().dip2px(applicationContext, 6F))
                            rl.layoutParams = brParams
                        }, 10)
            }
        })
        animationSet.addAnimation(translateAnimation)
        return animationSet
    }

    private fun getTime(ts: String): String {

        getTimeDiff(ts)
        if (TimeUtils.millis2String(System.currentTimeMillis()) < timeString) {
            tv1.text = SpannableBuilder.create(this)
                    .append("距离上班时间还有 ", R.dimen.text_size, R.color.grayy)
                    .append(timeDiff, R.dimen.text_size, R.color.redd)
                    .build()
        } else {
            if (timeDiff == "0min")
                timeDiff = "1min"
            tv1.text = SpannableBuilder.create(this)
                    .append("您已迟到 ", R.dimen.text_size, R.color.grayy)
                    .append(timeDiff, R.dimen.text_size, R.color.redd)
                    .build()
        }
        return timeDiff
    }

    private fun getTime2(ts: String) {
        getTimeDiff(ts)
        tv2.text = timeDiff
    }

    private fun getTimeDiff(ts: String) {
        val originalTimeDifference = TimeUtils.getFitTimeSpanByNow(TimeUtils.string2Millis(ts), 4)
        val cutSecondTimeDiffIndex = originalTimeDifference.indexOf("分钟")
        timeDiff = if (cutSecondTimeDiffIndex != -1) {//有分钟
            originalTimeDifference.substring(0, cutSecondTimeDiffIndex + 2).replace("小时", "h").replace("分钟", "min")
        } else {//没有分钟
            val cutSecondTimeDiffIndex2 = originalTimeDifference.indexOf("小时")
            if (cutSecondTimeDiffIndex2 != -1) {//有小时
                originalTimeDifference.substring(0, cutSecondTimeDiffIndex2 + 2).replace("小时", "h")
            } else {//没有小时
                "0min"
            }
        }
    }

    fun getToday(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        month += 1
        return if (month < 10) {
            val date = calendar.get(Calendar.DATE)
            val today = "$year-0$month-$date"
            today
        } else {
            val date = calendar.get(Calendar.DATE)
            val today = "$year-$month-$date"
            today
        }


    }

    private fun initMap() {
        val mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING
        mMapView = findViewById<MapView>(R.id.mMapView)
        mBaiduMap = mMapView!!.map
        mBaiduMap!!.isMyLocationEnabled = true
//        val mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.position)
        val bmp = BitmapFactory.decodeResource(resources, R.drawable.position)
        val bmp2 = rotateBitmap(bmp, 180F)
        val marker = resizeBitmap(bmp2!!, DimenUtil().dip2px(applicationContext, 18F), DimenUtil().dip2px(applicationContext, 36F))
        val mCurrentMarker = BitmapDescriptorFactory.fromBitmap(marker)
        mBaiduMap!!.setMyLocationConfigeration(MyLocationConfiguration(mCurrentMode, true, mCurrentMarker))
        val builder = MapStatus.Builder()
        builder.overlook(0f)
        mBaiduMap!!.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        val child = mMapView!!.getChildAt(1)
        if (child != null && (child is ImageView || child is ZoomControls)) {
            child.visibility = View.INVISIBLE
        }

        mMapView!!.showScaleControl(false)
        mMapView!!.showZoomControls(false)
        val mUiSettings = mBaiduMap!!.uiSettings
        mUiSettings.isScrollGesturesEnabled = true
        mUiSettings.isOverlookingGesturesEnabled = true
        mUiSettings.isZoomGesturesEnabled = true
        mLocClient = LocationClient(this)
        mLocClient!!.registerLocationListener(myListener)
        val option = LocationClientOption()
        option.isOpenGps = true
        option.setCoorType("bd09ll")
        option.setScanSpan(1000)
        option.setAddrType("all")
        option.setIsNeedLocationPoiList(true)
        mLocClient!!.locOption = option
        mLocClient!!.start()
    }

    fun resizeBitmap(bitmap: Bitmap, w: Int, h: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        var scaleWidth = w / width.toFloat()
        var scaleHeight = h / height.toFloat()
        var matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        var resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        return resizedBitmap
    }

    private fun rotateBitmap(origin: Bitmap?, alpha: Float): Bitmap? {
        if (origin == null) {
            return null
        }
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.setRotate(alpha)
        // 围绕原地进行旋转
        val newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        if (newBM == origin) {
            return newBM
        }
        origin.recycle()
        return newBM
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    internal var mTraceListener: OnTraceListener = object : OnTraceListener {
        override fun onBindServiceCallback(i: Int, s: String) {

        }

        override fun onStartTraceCallback(status: Int, message: String) {
            println(String.format("onStartTraceCallback, errorNo:%d, message:%s ", status, message))
        }

        override fun onStopTraceCallback(status: Int, message: String) {
            println(String.format("onStopTraceCallback, errorNo:%d, message:%s ", status, message))

        }

        override fun onStartGatherCallback(status: Int, message: String) {
            println(String.format("onStartGatherCallback, errorNo:%d, message:%s ", status, message))
        }

        override fun onStopGatherCallback(status: Int, message: String) {
            println(String.format("onStopGatherCallback, errorNo:%d, message:%s ", status, message))

        }

        override fun onPushCallback(messageNo: Byte, message: PushMessage) {}

        override fun onInitBOSCallback(i: Int, s: String) {

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun handlePermisson() {
        val permission = Manifest.permission.CAMERA
        val checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            } else {
                myRequestPermission()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun myRequestPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permissions, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

//            entityName = DeviceUtils.getUniqueId(this)
//            mTrace = Trace(serviceId, entityName, isNeedObjectStorage)
//            mTraceClient = LBSTraceClient(applicationContext)
//            mTraceClient.setInterval(gatherInterval, packInterval)
//            mTraceClient.startTrace(mTrace, mTraceListener)
//            mTraceClient.startGather(mTraceListener)


        }
    }

    inner class MyLocationListenner : BDLocationListener {
        var lati: Double = 0.toDouble()
        var longi: Double = 0.toDouble()
        var address: String = ""
        internal lateinit var poi: List<Poi>

        override fun onReceiveLocation(location: BDLocation?) {
            if (location == null || mMapView == null) {
                return
            }

            val locData = MyLocationData.Builder()
                    .accuracy(0F)
                    .direction(mCurrentDirection.toFloat())
                    .latitude(location.latitude)
                    .longitude(location.longitude).build()
            lati = location.latitude
            longi = location.longitude
            mCurrentLat = location.latitude
            mCurrentLon = location.longitude
            address = location.addrStr
            mCurrentAccracy = location.radius
            poi = location.poiList
            mBaiduMap!!.setMyLocationData(locData)
            if (isFirstLoc) {
                isFirstLoc = false
                val ll = LatLng(location.latitude,
                        location.longitude)
                val builder = MapStatus.Builder()
                builder.target(ll).zoom(18.0f)
                mBaiduMap!!.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
            }
        }

        fun onConnectHotSpotMessage(s: String, i: Int) {

        }

    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[SensorManager.DATA_X].toDouble()
        if (Math.abs(x - lastX!!) > 1.0) {
            mCurrentDirection = x.toInt()
            locData = MyLocationData.Builder()
                    .accuracy(0F)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection.toFloat()).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build()
            mBaiduMap!!.setMyLocationData(locData)
        }
        lastX = x

    }

    override fun onPause() {
        mMapView!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        mMapView!!.onResume()
        super.onResume()
        mSensorManager!!.registerListener(this, mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI)
    }

    override fun onDestroy() {
        mTraceClient.stopTrace(mTrace, mTraceListener)
        mTraceClient.stopGather(mTraceListener)
        mLocClient!!.stop()
        mBaiduMap!!.isMyLocationEnabled = false
        mMapView!!.onDestroy()
        mMapView = null
        mSensorManager!!.unregisterListener(this)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    fun exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(applicationContext, "再按一次退出程序",
                    Toast.LENGTH_SHORT).show()
            exitTime = System.currentTimeMillis()
        } else {
            finish()
            System.exit(0)
        }
    }
}
