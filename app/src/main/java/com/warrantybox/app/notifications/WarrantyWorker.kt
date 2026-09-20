package com.warrantybox.app.notifications
import android.app.*
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.warrantybox.app.R
import java.util.concurrent.TimeUnit

class WarrantyWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){override suspend fun doWork():Result{val channel="warranty_alerts";val nm=applicationContext.getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel(channel,applicationContext.getString(R.string.notifications),NotificationManager.IMPORTANCE_DEFAULT));val name=inputData.getString("name")?:return Result.failure();nm.notify(inputData.getLong("id",0).toInt(),NotificationCompat.Builder(applicationContext,channel).setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(name).setContentText("A garantia termina em ${inputData.getInt("days",0)} dias").setAutoCancel(true).build());return Result.success()}
 companion object{fun schedule(context:Context,id:Long,name:String,endDate:Long,days:Set<Int>){val wm=WorkManager.getInstance(context);days.forEach{d->val delay=endDate-System.currentTimeMillis()-TimeUnit.DAYS.toMillis(d.toLong());val tag="warranty_${id}_$d";wm.cancelUniqueWork(tag);if(delay>0)wm.enqueueUniqueWork(tag,ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<WarrantyWorker>().setInitialDelay(delay,TimeUnit.MILLISECONDS).setInputData(workDataOf("id" to id,"name" to name,"days" to d)).build())}}}
}
