package com.amarchaud.estats.service

import android.content.Context
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.utils.TimeTransformation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class ResetDurationWorker @WorkerInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private var myDao: AppDao
) : Worker(appContext, workerParams) {

    companion object {
        const val TAG = "ResetDurationWorker"
        const val TYPE = "TYPE"


        fun prepareNextReset(type: AppDao.DurationType, context: Context) {

            val inputData = Data.Builder()

            // today
            val currentDate = Calendar.getInstance()

            val nextCalendar = Calendar.getInstance()
            nextCalendar.set(Calendar.HOUR_OF_DAY, 0)
            nextCalendar.set(Calendar.MINUTE, 0)
            nextCalendar.set(Calendar.SECOND, 0)
            nextCalendar.set(Calendar.MILLISECOND, 0)

            var timeMsDiff = 0L
            when (type) {
                AppDao.DurationType.DURATION_DAY -> {
                    if (nextCalendar.before(currentDate)) {
                        nextCalendar.add(Calendar.HOUR_OF_DAY, 24)
                        timeMsDiff = nextCalendar.timeInMillis - currentDate.timeInMillis
                    }
                    //timeMsDiff = 1000L
                    Log.d(TAG, "next day reset in $timeMsDiff milli : ${TimeTransformation.millisecondToTimeStr(timeMsDiff)}")
                }
                AppDao.DurationType.DURATION_WEEK -> {
                    nextCalendar.set(Calendar.DAY_OF_WEEK, nextCalendar.firstDayOfWeek);
                    nextCalendar.add(Calendar.WEEK_OF_YEAR, 1)

                    timeMsDiff = nextCalendar.timeInMillis - currentDate.timeInMillis
                    Log.d(TAG, "next week reset in $timeMsDiff milli : ${TimeTransformation.millisecondToTimeStr(timeMsDiff)}")
                }
                AppDao.DurationType.DURATION_MONTH -> {
                    nextCalendar.set(Calendar.DAY_OF_MONTH, 1)
                    nextCalendar.add(Calendar.MONTH, 1)

                    timeMsDiff = nextCalendar.timeInMillis - currentDate.timeInMillis
                    Log.d(TAG, "next month reset in $timeMsDiff milli : ${TimeTransformation.millisecondToTimeStr(timeMsDiff)}")
                }
                AppDao.DurationType.DURATION_YEAR -> {
                    nextCalendar.set(Calendar.DAY_OF_YEAR, 1)
                    nextCalendar.add(Calendar.YEAR, 1)

                    timeMsDiff = nextCalendar.timeInMillis - currentDate.timeInMillis
                    Log.d(TAG, "next year reset in $timeMsDiff milli : ${TimeTransformation.millisecondToTimeStr(timeMsDiff)}")
                }

                else -> {

                }
            }

            inputData.putInt(TYPE, type.value)
            val workRequest = OneTimeWorkRequestBuilder<ResetDurationWorker>()
                .setInitialDelay(timeMsDiff, TimeUnit.MILLISECONDS)
                .setInputData(inputData.build())
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun doWork(): Result {

        lateinit var type: AppDao.DurationType
        when (inputData.getInt(TYPE, -1)) {
            AppDao.DurationType.DURATION_DAY.value -> {
                type = AppDao.DurationType.DURATION_DAY
            }
            AppDao.DurationType.DURATION_WEEK.value -> {
                type = AppDao.DurationType.DURATION_WEEK
            }
            AppDao.DurationType.DURATION_MONTH.value -> {
                type = AppDao.DurationType.DURATION_MONTH
            }
            AppDao.DurationType.DURATION_YEAR.value -> {
                type = AppDao.DurationType.DURATION_YEAR
            }
        }

        Log.d(TAG, "reset duration for : $type")

        GlobalScope.launch {
            myDao.getAllLocationsWithSubs().forEach {
                myDao.resetLocationDuration(it, type)
            }
        }
        prepareNextReset(type, appContext)

        return Result.success()
    }
}