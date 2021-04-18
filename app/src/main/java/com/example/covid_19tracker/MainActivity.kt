package com.example.covid_19tracker

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.AbsListView
import androidx.work.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var stateAdapter: StateAdapter

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //list.addHeaderView(LayoutInflater.from(this).inflate(R.layout.item_header,list,false))


        fetchResult()

        swipeToRefresh.setOnRefreshListener {
            fetchResult()
        }

        initWorker()

        list.setOnScrollListener(object : AbsListView.OnScrollListener{
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}


            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {

                if(list.getChildAt(0)!=null){
                    swipeToRefresh.isEnabled=list.firstVisiblePosition === 0 && list.getChildAt(
                            0).top === 0
                }
            }

        })
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private fun initWorker() {

        val constraints=Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val notificationRequest= PeriodicWorkRequestBuilder<NotificationWorker>(2,TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "JOB_TAG",
                ExistingPeriodicWorkPolicy.KEEP,
                notificationRequest
        )
    }

    private fun fetchResult() {
        GlobalScope.launch {
            val response= withContext(Dispatchers.IO){ Client.api.clone().execute()}

            if(response.isSuccessful){
                swipeToRefresh.isRefreshing=false
                val data=Gson().fromJson(response.body?.string(),Response::class.java)
                launch (Dispatchers.Main) {
                    bindCombinedData(data.statewise[0])
                    bindStateWisedData(data.statewise.subList(1, data.statewise.size))

                }
            }
        }
    }

    private fun bindStateWisedData(subList: List<StatewiseItem>) {
       stateAdapter= StateAdapter(subList)
        list.adapter=stateAdapter
    }


    @SuppressLint("SimpleDateFormat")
    private fun bindCombinedData(data: StatewiseItem) {
        val lastUpdatedTime=data.lastupdatedtime

        val simpleDateFormat=SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

      lastUpdatedTv.text="Last Update\n ${getTimeAgo(simpleDateFormat.parse(lastUpdatedTime))}"

        confirmedTv.text=data.confirmed
        activeTv.text=data.active
        recoveredTv.text=data.recovered
        deceasedTv.text=data.deaths


    }


}
@SuppressLint("SimpleDateFormat")
fun getTimeAgo(past: Date):String{

    val now=Date()
    val seconds=TimeUnit.MILLISECONDS.toSeconds(now.time-past.time)
    val minutes=TimeUnit.MILLISECONDS.toMinutes(now.time-past.time)
    val hours=TimeUnit.MILLISECONDS.toHours(now.time-past.time)
//        Log.i("Main",seconds.toString())
//        Log.i("Main",minutes.toString())
//        Log.i("Main",hours.toString())

    return when{
        seconds<60->{
            "Few Seconds Ago"
        }
        minutes<60->{
            "$minutes minutes ago"
        }
        hours<24->{
            "$hours hour ${minutes%60} min ago"
        }
        else->{
            SimpleDateFormat("dd/MM/yyyy hh:mm a").format(past).toString()
        }
    }

}