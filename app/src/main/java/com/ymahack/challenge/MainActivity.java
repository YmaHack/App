package com.ymahack.challenge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final String PREFS = "challenge_store";
    static final String DATA = "days";
    static final String SETTINGS = "settings";
    static final int BG = Color.rgb(11,14,21);
    static final int PANEL = Color.rgb(27,34,49);
    static final int TEXT = Color.rgb(244,246,255);
    static final int MUTED = Color.rgb(160,171,192);
    static final int ACCENT = Color.rgb(128,112,255);
    static final int GREEN = Color.rgb(53,203,145);
    static final int RED = Color.rgb(240,120,128);

    final java.util.Calendar cursor = java.util.Calendar.getInstance();
    java.util.Calendar selected = java.util.Calendar.getInstance();
    JSONObject days = new JSONObject();
    boolean dark = true;
    boolean reminder = false;
    int reminderHour = 20, reminderMinute = 30;
    LinearLayout root, content;
    TextView title;
    final ArrayList<Button> navButtons = new ArrayList<>();

    int dp(float v){ return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        loadState();
        getWindow().setNavigationBarColor(BG);
        if(Build.VERSION.SDK_INT >= 23) getWindow().setStatusBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        showHome();
    }

    void loadState(){
        String d = getSharedPreferences(PREFS, MODE_PRIVATE).getString(DATA, "{}");
        String s = getSharedPreferences(PREFS, MODE_PRIVATE).getString(SETTINGS, "{}");
        try { days = new JSONObject(d); } catch(Exception e) { days = new JSONObject(); }
        try {
            JSONObject x = new JSONObject(s);
            dark = x.optBoolean("dark", true);
            reminder = x.optBoolean("reminder", false);
            reminderHour = x.optInt("hour",20);
            reminderMinute = x.optInt("minute",30);
        } catch(Exception ignored){}
    }

    void persist(){
        try {
            JSONObject x = new JSONObject();
            x.put("dark", dark).put("reminder", reminder)
             .put("hour", reminderHour).put("minute", reminderMinute);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(DATA, days.toString())
                .putString(SETTINGS, x.toString()).apply();
        } catch(JSONException ignored) {}
    }

    int bg(){ return dark ? BG : Color.rgb(239,242,248); }
    int panel(){ return dark ? PANEL : Color.argb(225,255,255,255); }
    int text(){ return dark ? TEXT : Color.rgb(20,24,35); }
    int muted(){ return dark ? MUTED : Color.rgb(100,111,130); }

    TextView tv(String s, float size, int color){
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.RIGHT);
        v.setFontFeatureSettings("kern");
        v.setPadding(dp(3),dp(3),dp(3),dp(3));
        return v;
    }

    GradientDrawable rounded(int color, int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), Color.argb(dark?28:18,255,255,255));
        return g;
    }

    View spacer(int h){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return v; }

    LinearLayout column(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(15),dp(10),dp(15),dp(20));
        l.setBackgroundColor(bg());
        return l;
    }

    LinearLayout card(){
        LinearLayout c=column();
        c.setPadding(dp(18),dp(18),dp(18),dp(18));
        c.setBackground(rounded(panel(),26));
        return c;
    }

    Button btn(String s){
        Button b=new Button(this);
        b.setText(s); b.setTextSize(11); b.setTextColor(text());
        b.setAllCaps(false); b.setMinHeight(0); b.setMinWidth(0);
        b.setPadding(dp(10),0,dp(10),0);
        b.setBackground(rounded(dark?Color.rgb(38,46,64):Color.WHITE,15));
        return b;
    }

    void base(String heading){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        title=tv(heading,28,text()); title.setTypeface(null,1); title.setPadding(dp(18),dp(20),dp(18),dp(6));
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(62)));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(nav(),new LinearLayout.LayoutParams(-1,dp(78)));
        setContentView(root);
    }

    View nav(){
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER); bar.setPadding(dp(9),dp(8),dp(9),dp(8));
        bar.setBackground(rounded(dark?Color.rgb(18,23,34):Color.argb(235,255,255,255),23));
        String[] names={"בית","לוח","התקדמות","הגדרות"};
        for(String n:names){
            Button b=btn(n); b.setTextSize(10);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1); p.setMargins(dp(3),0,dp(3),0);
            bar.addView(b,p); navButtons.add(b);
        }
        navButtons.get(0).setOnClickListener(v->showHome());
        navButtons.get(1).setOnClickListener(v->showCalendar());
        navButtons.get(2).setOnClickListener(v->showStats());
        navButtons.get(3).setOnClickListener(v->showSettings());
        return bar;
    }

    void header(String eyebrow,String h){
        TextView e=tv(eyebrow.toUpperCase(Locale.ROOT),10,muted()); e.setTypeface(null,1);
        content.addView(e,new LinearLayout.LayoutParams(-1,dp(20)));
        TextView x=tv(h,23,text()); x.setTypeface(null,1); content.addView(x,new LinearLayout.LayoutParams(-1,dp(42)));
    }

    String key(java.util.Calendar c){ return String.format(Locale.US,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)); }
    java.util.Calendar cloneDate(java.util.Calendar c){ java.util.Calendar x=(java.util.Calendar)c.clone(); x.set(Calendar.HOUR_OF_DAY,12);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);return x; }

    JSONObject get(String k){ return days.optJSONObject(k); }
    void put(String k, boolean success, int score, String note){
        JSONObject x=new JSONObject(); try{x.put("success",success).put("score",score).put("note",note==null?"":note);}catch(JSONException ignored){}
        try{days.put(k,x);}catch(JSONException ignored){} persist();
    }
    void remove(String k){ days.remove(k); persist(); }

    int successes(){
        int n=0; Iterator<String> it=days.keys(); while(it.hasNext()){ JSONObject x=days.optJSONObject(it.next()); if(x!=null && x.optBoolean("success"))n++; } return n;
    }
    int streak(){
        java.util.Calendar d=cloneDate(java.util.Calendar.getInstance());
        if(get(key(d))==null || !get(key(d)).optBoolean("success")) d.add(java.util.Calendar.DATE,-1);
        int n=0; while(true){ JSONObject x=get(key(d)); if(x==null||!x.optBoolean("success"))break; n++; d.add(java.util.Calendar.DATE,-1);} return n;
    }
    int best(){
        ArrayList<String> ks=new ArrayList<>(); Iterator<String> it=days.keys(); while(it.hasNext())ks.add(it.next()); Collections.sort(ks);
        int n=0,b=0; java.util.Calendar prev=null;
        for(String k:ks){ if(!days.optJSONObject(k).optBoolean("success")){n=0;prev=null;continue;} java.util.Calendar d=parse(k); if(prev!=null){java.util.Calendar q=(java.util.Calendar)prev.clone();q.add(java.util.Calendar.DATE,1); if(!sameDay(q,d))n=0;} n++;b=Math.max(b,n);prev=d;} return b;
    }
    java.util.Calendar parse(String k){String[] a=k.split("-");java.util.Calendar d=java.util.Calendar.getInstance();d.set(Integer.parseInt(a[0]),Integer.parseInt(a[1])-1,Integer.parseInt(a[2]),12,0,0);return d;}
    boolean sameDay(java.util.Calendar a,java.util.Calendar b){return a.get(java.util.Calendar.YEAR)==b.get(java.util.Calendar.YEAR)&&a.get(java.util.Calendar.DAY_OF_YEAR)==b.get(java.util.Calendar.DAY_OF_YEAR);}

    String hebrewDate(java.util.Calendar g){
        try{
            android.icu.util.Calendar c=android.icu.util.Calendar.getInstance(TimeZone.getDefault(), Locale.forLanguageTag("he-IL-u-ca-hebrew"));
            c.set(g.get(java.util.Calendar.YEAR),g.get(java.util.Calendar.MONTH),g.get(java.util.Calendar.DAY_OF_MONTH));
            DateFormat f=DateFormat.getDateInstance(DateFormat.LONG,new Locale("he","IL","u-ca-hebrew"));
            return f.format(new Date(g.getTimeInMillis()));
        }catch(Exception e){return "לוח עברי";}
    }

    void showHome(){
        navButtons.clear(); base("אתגר יומי"); header("מעקב אישי","צעד קטן. רצף גדול.");
        LinearLayout hero=card(); TextView pill=tv(streak()>0?"רצף בתנועה":"עוד יום אחד בדרך",11,ACCENT); pill.setTypeface(null,1); hero.addView(pill);
        TextView h=tv(streak()>2?"כבר "+streak()+" ימים ברצף. ממשיכים.":"היום לא צריך להיות מושלם — רק אמיתי.",25,text());h.setTypeface(null,1);h.setPadding(0,dp(10),0,dp(5));hero.addView(h);
        TextView sub=tv("כל יום שאתה בוחר לנסות הוא עוד לבנה בדרך.",13,muted());hero.addView(sub);
        LinearLayout stats=new LinearLayout(this); stats.setPadding(0,dp(16),0,0);
        addStat(stats,"רצף",String.valueOf(streak())); addStat(stats,"הצלחות",String.valueOf(successes())); addStat(stats,"שיא",String.valueOf(best()));
        hero.addView(stats); content.addView(hero);
        content.addView(spacer(12));
        java.util.Calendar t=cloneDate(java.util.Calendar.getInstance()); JSONObject e=get(key(t));
        LinearLayout today=card(); today.addView(tv("היום",10,muted())); TextView td=tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(t.getTime()),18,text());td.setTypeface(null,1);today.addView(td); today.addView(tv(hebrewDate(t),12,muted()));
        String status=e==null?"עדיין לא עודכן":e.optBoolean("success")?"✓ הצלחתי · ציון "+e.optInt("score")+"/10":"✕ לא הצלחתי · ציון "+e.optInt("score")+"/10"; today.addView(tv(status,14,e==null?ACCENT:(e.optBoolean("success")?GREEN:RED)));
        Button edit=btn("עדכון היום"); edit.setBackground(rounded(ACCENT,15)); edit.setTextColor(Color.WHITE); edit.setOnClickListener(v->openEditor(t)); today.addView(edit,new LinearLayout.LayoutParams(-1,dp(48)));content.addView(today);
        content.addView(spacer(12));
        LinearLayout q=card(); q.setBackground(rounded(dark?Color.rgb(32,38,65):Color.rgb(245,245,255),26)); q.addView(tv("✦ תזכורת לעצמך",11,dark?Color.LTGRAY:ACCENT)); String[] qs={"אל תחכה למוטיבציה. תתחיל, והמוטיבציה תדביק אותך.","גם צעד קטן הוא תנועה קדימה.","אתה לא צריך לנצח את כל החיים היום. רק את היום הזה.","רצף נבנה מימים אמיתיים, לא מימים מושלמים.","הצלחה היא היכולת לחזור למסלול פעם נוספת."}; q.addView(tv(qs[(int)(System.currentTimeMillis()/86400000)%qs.length],20,text())); q.addView(tv("כל יום הוא התחלה חדשה.",12,muted()));content.addView(q);
        TextView enc=tv("גם אם היום לא הלך כמו שרצית, זה לא מוחק שום דבר. מחר מחכה לך הזדמנות חדשה.",14,muted());enc.setPadding(dp(4),dp(16),dp(4),dp(8));content.addView(enc);
    }

    void addStat(LinearLayout p,String a,String b){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.RIGHT);
        box.addView(tv(b,25,text()));box.addView(tv(a,10,muted()));p.addView(box,new LinearLayout.LayoutParams(0,dp(62),1));
    }

    void showCalendar(){
        navButtons.clear(); base("לוח עברי־לועזי"); header("מעקב","הימים שלך");
        LinearLayout head=card(); LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=btn("‹");Button next=btn("›"); TextView m=tv(new SimpleDateFormat("MMMM yyyy",new Locale("he","IL")).format(cursor.getTime()),17,text());m.setGravity(Gravity.CENTER);row.addView(prev,new LinearLayout.LayoutParams(dp(55),dp(48)));row.addView(m,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(next,new LinearLayout.LayoutParams(dp(55),dp(48)));head.addView(row);
        TextView hm=tv(hebrewDateAtMonth(),11,muted());hm.setGravity(Gravity.CENTER);head.addView(hm);content.addView(head);content.addView(spacer(10));
        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);grid.setBackground(rounded(panel(),22));grid.setPadding(dp(6),dp(8),dp(6),dp(8));
        String[] w={"א","ב","ג","ד","ה","ו","ש"};
        for(String s:w){
            TextView t=tv(s,10,muted()); t.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams wp=new GridLayout.LayoutParams();
            wp.width=0; wp.height=dp(30); wp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);
            grid.addView(t,wp);
        }
        java.util.Calendar first=(java.util.Calendar)cursor.clone();first.set(java.util.Calendar.DAY_OF_MONTH,1);int offset=first.get(java.util.Calendar.DAY_OF_WEEK)-1;int max=cursor.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        for(int i=0;i<offset;i++)grid.addView(new Space(this),cellParams());
        for(int n=1;n<=max;n++){java.util.Calendar d=(java.util.Calendar)cursor.clone();d.set(java.util.Calendar.DAY_OF_MONTH,n);String k=key(d);JSONObject e=get(k);TextView cell=tv(String.valueOf(n)+"\n"+hebrewDay(d),10,text());cell.setGravity(Gravity.CENTER);if(e!=null)cell.setTextColor(e.optBoolean("success")?GREEN:RED);cell.setBackground(rounded(n==java.util.Calendar.getInstance().get(java.util.Calendar.DATE)&&cursor.get(java.util.Calendar.MONTH)==java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)&&cursor.get(java.util.Calendar.YEAR)==java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)?Color.argb(55,ACCENT>>>16,(ACCENT>>>8)&255,ACCENT&255):Color.TRANSPARENT,13));cell.setOnClickListener(v->openEditor(d));grid.addView(cell,cellParams());}
        content.addView(grid); TextView help=tv("ירוק = הצלחה   •   אדום = לא הצלחתי   •   לחיצה על יום לעריכה",10,muted());help.setGravity(Gravity.CENTER);help.setPadding(0,dp(12),0,0);content.addView(help);
        prev.setOnClickListener(v->{cursor.add(java.util.Calendar.MONTH,-1);showCalendar();});next.setOnClickListener(v->{cursor.add(java.util.Calendar.MONTH,1);showCalendar();});
    }

    GridLayout.LayoutParams cellParams(){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(60);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);p.rowSpec=GridLayout.spec(GridLayout.UNDEFINED);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}
    String hebrewDay(java.util.Calendar d){try{android.icu.util.Calendar c=android.icu.util.Calendar.getInstance(new Locale("he","IL","u-ca-hebrew"));c.setTimeInMillis(d.getTimeInMillis());return String.valueOf(c.get(android.icu.util.Calendar.DAY_OF_MONTH));}catch(Exception e){return "";}}
    String hebrewDateAtMonth(){java.util.Calendar d=(java.util.Calendar)cursor.clone();d.set(java.util.Calendar.DAY_OF_MONTH,15);return hebrewDate(d);}

    void showStats(){
        navButtons.clear();base("התקדמות");header("תמונה רחבה","המסע שלך");
        LinearLayout c=card(); addBig(c,"רצף נוכחי",String.valueOf(streak()));addBig(c,"שיא אישי",String.valueOf(best()));addBig(c,"ימים שהצלחת",String.valueOf(successes()));
        int count=0,total=0;Iterator<String> it=days.keys();while(it.hasNext()){JSONObject x=days.optJSONObject(it.next());if(x!=null){count++;total+=x.optInt("score");}}
        addBig(c,"ממוצע ציון",count==0?"—":String.format(Locale.US,"%.1f / 10",total/(double)count));content.addView(c);
        content.addView(spacer(12));LinearLayout q=card();q.addView(tv("✦ עידוד",11,ACCENT));q.addView(tv(streak()>0?"אתה כבר בתוך רצף. אל תזלזל ביום אחד נוסף.":"גם התחלה מחדש היא הצלחה. היום אפשר לבנות את היום הראשון.",20,text()));content.addView(q);
    }
    void addBig(LinearLayout c,String label,String value){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView v=tv(value,28,text());v.setTypeface(null,1);r.addView(v,new LinearLayout.LayoutParams(dp(115),dp(65)));r.addView(tv(label,13,muted()),new LinearLayout.LayoutParams(0,dp(65),1));c.addView(r);}

    void showSettings(){
        navButtons.clear();base("הגדרות");header("התאמה אישית","המראה והתזכורות");
        LinearLayout c=card();
        Switch darkSwitch=new Switch(this);darkSwitch.setText("מצב כהה");darkSwitch.setTextColor(text());darkSwitch.setChecked(dark);darkSwitch.setOnCheckedChangeListener((b,x)->{dark=x;persist();showSettings();});c.addView(darkSwitch,new LinearLayout.LayoutParams(-1,dp(55)));
        Switch rem=new Switch(this);rem.setText("תזכורת יומית");rem.setTextColor(text());rem.setChecked(reminder);rem.setOnCheckedChangeListener((b,x)->{reminder=x;persist();scheduleReminder();});c.addView(rem,new LinearLayout.LayoutParams(-1,dp(55)));
        LinearLayout timeRow=new LinearLayout(this);timeRow.setGravity(Gravity.CENTER_VERTICAL);timeRow.addView(tv("שעת תזכורת",13,muted()),new LinearLayout.LayoutParams(0,dp(55),1));TimePicker tp=new TimePicker(this);tp.setIs24HourView(true);tp.setHour(reminderHour);tp.setMinute(reminderMinute);tp.setOnTimeChangedListener((v,h,m)->{reminderHour=h;reminderMinute=m;persist();if(reminder)scheduleReminder();});timeRow.addView(tp,new LinearLayout.LayoutParams(dp(130),dp(70)));c.addView(timeRow);
        Button reset=btn("איפוס כל הנתונים");reset.setTextColor(RED);reset.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle("איפוס מעקב").setMessage("כל הימים, הציונים וההערות יימחקו מהמכשיר.").setNegativeButton("ביטול",null).setPositiveButton("איפוס",(d,w)->{days=new JSONObject();persist();showHome();}).show();});c.addView(reset,new LinearLayout.LayoutParams(-1,dp(55)));
        content.addView(c);content.addView(spacer(12));content.addView(tv("המעקב נשמר מקומית במכשיר. אין צורך באתר חיצוני כדי להשתמש באפליקציה.",12,muted()));
    }

    void openEditor(java.util.Calendar d){
        selected=cloneDate(d);String k=key(selected);JSONObject e=get(k);final boolean[] ok={e!=null?e.optBoolean("success"):false};final boolean[] chosen={e!=null};final int[] score={e!=null?e.optInt("score",10):10};
        LinearLayout box=column();box.setPadding(dp(22),dp(16),dp(22),dp(16));box.setBackground(rounded(panel(),28));
        TextView h=tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(d.getTime()),22,text());h.setTypeface(null,1);box.addView(h);box.addView(tv(hebrewDate(d),12,muted()));
        LinearLayout choices=new LinearLayout(this);Button yes=btn("✓ הצלחתי");Button no=btn("✕ לא הצלחתי");choices.addView(yes,new LinearLayout.LayoutParams(0,dp(52),1));choices.addView(no,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(choices);box.addView(tv("ציון 1–10",11,muted()));
        LinearLayout ratings=new LinearLayout(this);ratings.setGravity(Gravity.CENTER);for(int i=1;i<=10;i++){final int sc=i;Button b=btn(String.valueOf(i));b.setOnClickListener(v->score[0]=sc);ratings.addView(b,new LinearLayout.LayoutParams(0,dp(46),1));}box.addView(ratings);
        final EditText note=new EditText(this);note.setHint("הערה לעצמך (לא חובה)");note.setText(e==null?"":e.optString("note",""));note.setTextColor(text());note.setHintTextColor(muted());box.addView(note,new LinearLayout.LayoutParams(-1,dp(100)));
        Button save=btn("שמירת היום");save.setBackground(rounded(ACCENT,16));save.setTextColor(Color.WHITE);box.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));
        Button clear=btn("ניקוי הסימון");box.addView(clear,new LinearLayout.LayoutParams(-1,dp(50)));
        AlertDialog dialog=new AlertDialog.Builder(this).setView(box).create(); if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(rounded(bg(),28));
        yes.setOnClickListener(v->{chosen[0]=true;ok[0]=true;yes.setBackground(rounded(GREEN,15));no.setBackground(rounded(panel(),15));});
        no.setOnClickListener(v->{chosen[0]=true;ok[0]=false;no.setBackground(rounded(RED,15));yes.setBackground(rounded(panel(),15));});
        save.setOnClickListener(v->{if(!chosen[0]){Toast.makeText(this,"בחר אם הצלחת או לא",Toast.LENGTH_SHORT).show();return;}put(k,ok[0],score[0],note.getText().toString());dialog.dismiss();refreshCurrent();});
        clear.setOnClickListener(v->{remove(k);dialog.dismiss();refreshCurrent();});
        dialog.getWindow();
        dialog.show();
    }

    void refreshCurrent(){showHome();}

    void scheduleReminder(){
        AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,ReminderReceiver.class);
        PendingIntent p=PendingIntent.getBroadcast(this,911, i, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarm.cancel(p);if(!reminder)return;
        java.util.Calendar d=java.util.Calendar.getInstance();d.set(java.util.Calendar.HOUR_OF_DAY,reminderHour);d.set(java.util.Calendar.MINUTE,reminderMinute);d.set(java.util.Calendar.SECOND,0);d.set(java.util.Calendar.MILLISECOND,0);if(d.getTimeInMillis()<=System.currentTimeMillis())d.add(java.util.Calendar.DATE,1);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,d.getTimeInMillis(),AlarmManager.INTERVAL_DAY,p);
    }

    public static class ReminderReceiver extends BroadcastReceiver{
        public void onReceive(Context c,Intent i){
            String channel="challenge_reminders";
            NotificationManager nm=(NotificationManager)c.getSystemService(NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(channel,"תזכורות אתגר יומי",NotificationManager.IMPORTANCE_DEFAULT));
            Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,912,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);
            b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("האתגר שלך מחכה לך").setContentText("גם היום אפשר לעשות צעד אחד קטן קדימה.").setContentIntent(pi).setAutoCancel(true);
            nm.notify(912,b.build());
        }
    }
}