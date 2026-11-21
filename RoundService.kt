package ba.airsoft.detonacija

import android.app.*
import android.content.Intent
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.*
import kotlin.math.*
import kotlin.random.Random

class RoundService : Service() {
 companion object {
  const val CHANNEL="airsoft_round"
  const val ACTION="ba.airsoft.detonacija.ACTION"
  const val PLANT=20*60*1000L
  const val DETONATE=15*60*1000L
  const val PENALTY=30*1000L
  val CODES=listOf("2846","7193","4528","9361","6052")
 }
 private val h=Handler(Looper.getMainLooper())
 private val prefs by lazy { getSharedPreferences("round",Context.MODE_PRIVATE) }
 private var player: AudioTrack?=null
 private var soundThread: Thread?=null
 @Volatile private var soundRunning=false
 private var foreground=false
 private val tick=object:Runnable { override fun run() { refresh(); h.postDelayed(this,500) } }
 override fun onCreate() { super.onCreate(); val channel=NotificationChannel(CHANNEL,"Aktivna airsoft runda",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(channel); h.post(tick) }
 override fun onBind(intent:Intent?)=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  when(intent?.getStringExtra("cmd")) {
   "begin" -> if(state()=="ready") setState("planting",System.currentTimeMillis()+PLANT)
   "arm" -> if(state()=="planting" && System.currentTimeMillis()<deadline()) { selectCode();setState("active",System.currentTimeMillis()+DETONATE);prefs.edit().putLong("lock",0).apply() }
   "pin" -> if(state()=="active" && System.currentTimeMillis()>=prefs.getLong("lock",0)) {
      if(intent.getStringExtra("pin")==prefs.getString("secret","")) { setState("defused",0);stopSound() }
      else prefs.edit().putLong("lock",System.currentTimeMillis()+PENALTY).apply()
   }
   "new" -> { stopSound(); prefs.edit().putString("state","ready").putLong("deadline",0).putLong("lock",0).apply() }
   "mute" -> { val muted=!prefs.getBoolean("muted",false);prefs.edit().putBoolean("muted",muted).apply();if(muted)stopSound() }
   "test" -> playOneShot()
  }
  refresh()
  return START_STICKY
 }
 private fun state()=prefs.getString("state","ready")?:"ready"
 private fun deadline()=prefs.getLong("deadline",0)
 private fun setState(s:String,d:Long){prefs.edit().putString("state",s).putLong("deadline",d).apply()}
 private fun selectCode(){
  val saved=prefs.getString("deck","")!!.split(",").filter{it.isNotBlank()}.toMutableList()
  if(saved.isEmpty()) saved.addAll(CODES.shuffled())
  val code=saved.removeAt(saved.lastIndex)
  prefs.edit().putString("secret",code).putString("deck",saved.joinToString(",")).apply()
 }
 private fun refresh(){
  val now=System.currentTimeMillis()
  if(state()=="planting" && now>=deadline()){setState("defense",0);startSound(false)}
  if(state()=="active" && now>=deadline()){setState("expired",0);startSound(true)}
  val ongoing=state() in listOf("planting","active","defense","expired")
  if(ongoing){
   val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
   val remaining=max(0,(deadline()-now+999)/1000)
   val msg=when(state()){"planting"->"Postavljanje: ${remaining/60}:${(remaining%60).toString().padStart(2,'0')}";"active"->"Aktivno: ${remaining/60}:${(remaining%60).toString().padStart(2,'0')}";"defense"->"Odbrana pobijedila";else->"Vrijeme isteklo"}
   val n=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Airsoft Detonacija").setContentText(msg).setContentIntent(pi).setOngoing(true).build()
   if(!foreground){startForeground(1,n);foreground=true}else getSystemService(NotificationManager::class.java).notify(1,n)
   if(state() in listOf("defense","expired") && !prefs.getBoolean("muted",false) && !soundRunning)startSound(state()=="expired")
  } else if(foreground){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;stopSound()}
 }
 private fun startSound(explosion:Boolean){if(prefs.getBoolean("muted",false)||soundRunning)return; soundRunning=true
  soundThread=Thread { try { do { playSound(explosion) ; if(soundRunning)Thread.sleep(if(explosion)1800 else 700) }while(soundRunning) }catch(_:InterruptedException){} finally {soundRunning=false} }.also{it.start()}
 }
 private fun stopSound(){soundRunning=false;soundThread?.interrupt();soundThread=null;try{player?.pause();player?.flush();player?.release()}catch(_:Exception){};player=null}
 private fun playOneShot(){Thread{playSound(false)}.start()}
 private fun playSound(explosion:Boolean){
  val sr=22050;val length=if(explosion)sr*2 else sr/2
  val samples=ShortArray(length); var lp=0.0
  for(i in samples.indices){val t=i.toDouble()/sr;val x=if(explosion){lp=lp*.94+(Random.nextDouble()*2-1)*.06; (lp*2+sin(2*PI*(75-35*min(t,1.0))*t)*.25)*exp(-t*2.2)}else sin(2*PI*(if(t<.24)660 else 880)*t)*.22
   samples[i]=(x.coerceIn(-.8,.8)*Short.MAX_VALUE).toInt().toShort() }
  val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
  try {track.write(samples,0,samples.size);track.play();Thread.sleep(length*1000L/sr+100)}catch(_:Exception){}finally{track.release()}
 }
 override fun onDestroy(){h.removeCallbacks(tick);stopSound();super.onDestroy()}
}
