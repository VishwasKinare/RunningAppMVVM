package com.example.runningappmvvm.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningappmvvm.R
import com.example.runningappmvvm.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext = Job() + Dispatchers.Main + CoroutineName("🙄 Activity Scope") + CoroutineExceptionHandler { coroutineContext, throwable ->
        println("🤬 Exception $throwable in context:$coroutineContext")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        CoroutineScope( Dispatchers.Unconfined).launch {
//            Log.v("COROUTINES", Thread.currentThread().name)
//            delay(1000)
//        }
//
//        CoroutineScope(Dispatchers.Main).launch {
//            Log.v("COROUTINES", "inside IO thread1")
//            cancel()
//            delay(2000)
//            Log.v("COROUTINES", "inside IO thread")
//        }
//        Log.v("COROUTINES", "main thread")

//        val job = GlobalScope.launch {
//            repeat(5){
//                Log.v("COROUTINES", "Repeat $it")
//            }
//        }

//        val job = GlobalScope.launch(Dispatchers.Default) {
//            Log.d("COROUTINES", "Long Running")
//            withTimeout(1000){
//                for (i in 30..40){
//                    if (isActive)
//                        Log.d("COROUTINES", "Long $i: ${fib(i)}")
//                }
//            }
//        }
//
//        runBlocking {
//            delay(1000)
//            job.cancel()
//            Log.d("COROUTINES", "Job Done")
//        }

        lifecycleScope.launch(Dispatchers.IO){
            val time = measureTimeMillis {
                val str1 = async { networkCall() }
                val str2 = async { networkCall2() }
                Log.d("COROUTINES", str1.await())
                Log.d("COROUTINES", str2.await())
            }
            Log.d("COROUTINES", time.toString())
        }

        navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        bottomNavigationView.setOnItemReselectedListener {

        }

        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id) {
                    R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                    }
                    else -> bottomNavigationView.visibility = View.GONE
                }
            }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?){
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT){
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            navHostFragment.findNavController().navigate(R.id.action_global_tracking_fragment)
        }
    }

    fun fib(n: Int): Long{
        return  if (n==0) 0
            else if (n==1) 1
            else fib(n-1) + fib(n-2)
    }

    fun fib1(n: Int): Long{
        return  if (n==0) 1
        else n * fib1(n-1)
    }

    suspend fun networkCall(): String{
        delay(3000)
        return "call1"
    }

    suspend fun networkCall2(): String{
        delay(3000)
        return "call2"
    }
}