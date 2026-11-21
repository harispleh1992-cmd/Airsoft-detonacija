package ba.airsoft.detonacija

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.view.*
import android.widget.*
import kotlin.math.max

class MainActivity:Activity(){
 private val h=Handler(Looper.getMainLooper())
 private val prefs by lazy {getSharedPreferences("round",MODE_PRIVATE)}
 private lateinit var clock:TextView
 private lateinit var status:TextView
 private lateinit var phase:TextView
 private lateinit var pinView:TextView
 private lateinit var lockView:TextView
 private lateinit var begin:Button
 private lateinit var arm:Button
 private lateinit var reset:Button
 private lateinit var mute:Button
 private lateinit var pinBox:LinearLayout
 private val keys=mutableListOf<Button>()
 private var typed=""
 private val updater=object:Runnable{override fun run(){render();h.postDelayed(this,200)}}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),10)
  val bg=Color.rgb(16,21,25);val fg=Color.rgb(233,241,241)
  val root=ScrollView(this).apply{setBackgroundColor(bg);isFillViewport=true}
  val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,32,30,30);gravity=Gravity.CENTER_HORIZONTAL}
  root.addView(col);setContentView(root)
  fun text(t:String,size:Float,color:Int=fg)=TextView(this).apply{text=t;textSize=size;setTextColor(color);gravity=Gravity.CENTER;setPadding(5,10,5,10)}
  fun button(t:String,action:()->Unit)=Button(this).apply{text=t;textSize=16f;setOnClickListener{action()};col.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=10})}
  col.addView(text("AIRSOFT · SIMULACIJA",13f))
  col.addView(text("OPERACIJA DETONACIJA",25f))
  phase=text("",14f);col.addView(phase)
  clock=text("20:00",72f,Color.rgb(240,105,96));col.addView(clock)
  status=text("",20f);col.addView(status)
  begin=button("POČNI RUNDU · 20 MINUTA"){send("begin")}
  arm=button("POSTAVI UREĐAJ · 15 MINUTA"){send("arm")}
  pinBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,15,0,15)};col.addView(pinBox)
  pinBox.addView(text("UNESI JEDNU OD 5 ŠIFRI",14f))
  pinView=text("_ _ _ _",29f);pinBox.addView(pinView)
  lockView=text("",15f,Color.rgb(255,189,114));pinBox.addView(lockView)
  val pad=GridLayout(this).apply{columnCount=3};pinBox.addView(pad)
  for(s in listOf("1","2","3","4","5","6","7","8","9","OBRIŠI","0","⌫")){
   val b=Button(this).apply{text=s;textSize=21f;setOnClickListener{key(s)}}
   keys.add(b);pad.addView(b,GridLayout.LayoutParams().apply{width=0;height=125;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})
  }
  reset=button("NOVA RUNDA"){val st=prefs.getString("state","ready");if(st=="active"||st=="planting")AlertDialog.Builder(this).setMessage("Prekinuti aktivnu rundu?").setNegativeButton("ODUSTANI",null).setPositiveButton("DA"){_,_->send("new")}.show() else send("new")}
  button("TEST ZVUKA"){send("test")}
  mute=button("ZVUK: UKLJUČEN"){send("mute")}
  col.addView(text("Šifre: 2846 · 7193 · 4528 · 9361 · 6052",14f))
  col.addView(text("Uključite Prikvači aplikaciju u Android postavkama radi zaštite od slučajnog izlaska. Zvuk podesite tipkama telefona.",13f))
  h.post(updater)
 }
 private fun send(cmd:String,pin:String?=null){val i=Intent(this,RoundService::class.java).putExtra("cmd",cmd);if(pin!=null)i.putExtra("pin",pin);if(Build.VERSION.SDK_INT>=26)startForegroundService(i)else startService(i);render()}
 private fun key(s:String){if(prefs.getString("state","ready")!="active"||System.currentTimeMillis()<prefs.getLong("lock",0))return
  when(s){"OBRIŠI"->typed="";"⌫"->typed=typed.dropLast(1);else->if(typed.length<4)typed+=s}
  if(typed.length==4){send("pin",typed);typed=""};render()
 }
 private fun render(){val state=prefs.getString("state","ready")?:"ready";val deadline=prefs.getLong("deadline",0);val now=System.currentTimeMillis()
  val remaining=if(state=="ready")20*60*1000L else if(state in listOf("planting","active"))max(0,deadline-now) else 0L
  val secs=(remaining+999)/1000;clock.text="%02d:%02d".format(secs/60,secs%60)
  phase.text=when(state){"planting"->"FAZA 1 · POSTAVLJANJE";"active"->"FAZA 2 · DEAKTIVACIJA";else->"AIRSOFT · SIMULACIJA"}
  status.text=when(state){"ready"->"SPREMNO ZA POČETAK";"planting"->"VRIJEME ZA POSTAVLJANJE";"active"->"UREĐAJ AKTIVAN";"defused"->"DEAKTIVIRANO — ODBRANA POBJEĐUJE";"defense"->"ISTEKLO POSTAVLJANJE — ODBRANA POBJEĐUJE";else->"SIMULIRANA DETONACIJA — NAPADAČI POBJEĐUJU"}
  begin.visibility=if(state=="ready")View.VISIBLE else View.GONE;arm.visibility=if(state=="planting")View.VISIBLE else View.GONE;pinBox.visibility=if(state=="active")View.VISIBLE else View.GONE
  val lock=max(0,prefs.getLong("lock",0)-now);lockView.text=if(lock>0)"POGREŠNA ŠIFRA — SAČEKAJ ${(lock+999)/1000} s" else ""
  pinView.text=(typed+"____").take(4).toCharArray().joinToString(" ")
  keys.forEach{it.isEnabled=lock==0L};mute.text=if(prefs.getBoolean("muted",false))"ZVUK: ISKLJUČEN" else "ZVUK: UKLJUČEN"
 }
 override fun onBackPressed(){AlertDialog.Builder(this).setMessage("Napustiti prikaz? Aktivna runda nastavlja odbrojavanje u pozadini.").setNegativeButton("OSTANI",null).setPositiveButton("IZAĐI"){_,_->super.onBackPressed()}.show()}
 override fun onDestroy(){h.removeCallbacks(updater);super.onDestroy()}
}
