package com.ymahack.challenge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    static final String PREFS="challenge_store", DATA="days", SETTINGS="settings";
    static final String CHANNEL="challenge_reminders";
    static final int BLUE=Color.rgb(54,112,220);
    static final int BG_DARK=Color.rgb(8,12,20), TEXT_DARK=Color.rgb(247,249,255), MUTED_DARK=Color.rgb(159,170,191);
    static final int BG_LIGHT=Color.rgb(240,243,249), TEXT_LIGHT=Color.rgb(21,25,35), MUTED_LIGHT=Color.rgb(101,112,130);
    static final int ACCENT=Color.rgb(111,102,244), GREEN=Color.rgb(47,198,141), RED=Color.rgb(239,106,119), GOLD=Color.rgb(246,190,72);

    JSONObject days=new JSONObject();
    boolean dark=true, reminder=false;
    int themeIndex=0;
    int reminderHour=20, reminderMinute=30;
    int reminderDaysMask=127;
    String syncUrl="";
    boolean autoSync=false;
    boolean syncing=false;

    final String[] themeNames={"אוקיינוס 🌊","יער 🌿","לבנדר 💜","שקיעה 🌅","ענבר ✨"};

    int accent(){
        switch(themeIndex){
            case 1:return Color.rgb(46,143,99);
            case 2:return Color.rgb(126,92,214);
            case 3:return Color.rgb(212,91,101);
            case 4:return Color.rgb(209,142,42);
            default:return Color.rgb(54,112,220);
        }
    }

    int themeDarkBg(){
        switch(themeIndex){
            case 1:return Color.rgb(8,20,15);
            case 2:return Color.rgb(18,13,30);
            case 3:return Color.rgb(28,12,17);
            case 4:return Color.rgb(25,18,8);
            default:return Color.rgb(8,16,28);
        }
    }

    int themeDarkCard(){
        switch(themeIndex){
            case 1:return Color.rgb(22,39,31);
            case 2:return Color.rgb(35,28,49);
            case 3:return Color.rgb(47,28,34);
            case 4:return Color.rgb(46,36,20);
            default:return Color.rgb(24,35,52);
        }
    }

    int themeDarkText(){return Color.rgb(247,249,255);}
    int themeDarkMuted(){
        switch(themeIndex){
            case 1:return Color.rgb(151,181,166);
            case 2:return Color.rgb(177,164,198);
            case 3:return Color.rgb(194,165,171);
            case 4:return Color.rgb(193,176,139);
            default:return Color.rgb(158,175,198);
        }
    }

    int themeLightBg(){
        switch(themeIndex){
            case 1:return Color.rgb(241,248,244);
            case 2:return Color.rgb(247,244,252);
            case 3:return Color.rgb(252,245,246);
            case 4:return Color.rgb(252,248,239);
            default:return Color.rgb(242,247,252);
        }
    }

    int themeLightCard(){
        return Color.WHITE;
    }

    int themeLightText(){return Color.rgb(25,30,40);}
    int themeLightMuted(){
        switch(themeIndex){
            case 1:return Color.rgb(91,113,101);
            case 2:return Color.rgb(103,94,119);
            case 3:return Color.rgb(118,92,99);
            case 4:return Color.rgb(121,103,75);
            default:return Color.rgb(93,108,126);
        }
    }
    final java.util.Calendar cursor=java.util.Calendar.getInstance();
    java.util.Calendar selected=java.util.Calendar.getInstance();

    FrameLayout root;
    LinearLayout content;
    ArrayList<Button> navButtons=new ArrayList<>();
    int screen=0;

    final String[] quotes={
        "אל תחכה למוטיבציה. תתחיל, והמוטיבציה תדביק אותך.",
        "גם צעד קטן הוא תנועה קדימה.",
        "אתה לא צריך לנצח את כל החיים היום. רק את היום הזה.",
        "רצף נבנה מימים אמיתיים, לא מימים מושלמים.",
        "הצלחה היא היכולת לחזור למסלול פעם נוספת.",
        "היום הזה לא צריך להיות מושלם. הוא צריך להיות שלך.",
        "תן לעצמך קרדיט גם על הדרך, לא רק על התוצאה.",
        "לפעמים הניצחון הוא פשוט לא לוותר היום.",
        "מה שעשית אתמול חשוב. מה שתעשה היום חשוב יותר.",
        "כל הרגל גדול התחיל מבחירה קטנה אחת.",
        "אל תמדוד את עצמך לפי נפילה אחת. מדוד לפי החזרה.",
        "יש ימים של אש ויש ימים של שקט — שניהם חלק מהמסע.",
        "העקביות שלך חזקה יותר מהמצב רוח שלך.",
        "היום אפשר לבחור מחדש."
    };
    final int[] quoteImages={
        R.drawable.scene_1,R.drawable.scene_2,R.drawable.scene_3
    };

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    int bg(){return dark?themeDarkBg():themeLightBg();}
    int text(){return dark?themeDarkText():themeLightText();}
    int muted(){return dark?themeDarkMuted():themeLightMuted();}
    int panelColor(){return dark?themeDarkCard():themeLightCard();}
    int panelStrong(){return dark?Color.rgb(31,38,52):Color.WHITE;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        load();
        getWindow().setNavigationBarColor(bg());
        if(Build.VERSION.SDK_INT>=23)getWindow().setStatusBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);
        scheduleReminder();
        showHome();
        if(!syncUrl.trim().isEmpty()) syncAsync(false);
    }

    void load(){
        boolean loaded=false;
        try{
            String saved=getSharedPreferences(PREFS,MODE_PRIVATE).getString(DATA,"");
            if(saved!=null && !saved.isEmpty()){
                days=new JSONObject(saved);
                loaded=true;
            }
        }catch(Exception ignored){}
        if(!loaded){
            try{
                File f=new File(getFilesDir(),"days_backup.json");
                if(f.exists()){
                    FileInputStream in=new FileInputStream(f);
                    byte[] data=new byte[(int)f.length()];
                    int read=in.read(data);
                    in.close();
                    days=new JSONObject(new String(data,StandardCharsets.UTF_8));
                }else days=new JSONObject();
            }catch(Exception e){days=new JSONObject();}
        }
        try{
            JSONObject s=new JSONObject(getSharedPreferences(PREFS,MODE_PRIVATE).getString(SETTINGS,"{}"));
            dark=s.optBoolean("dark",true);
            reminder=s.optBoolean("reminder",false);
            themeIndex=Math.max(0,Math.min(4,s.optInt("theme",0)));
            reminderHour=s.optInt("hour",20);
            reminderMinute=s.optInt("minute",30);
            reminderDaysMask=s.optInt("daysMask",127);
            if(reminderDaysMask<1 || reminderDaysMask>127) reminderDaysMask=127;
            syncUrl=s.optString("syncUrl","");
            autoSync=s.optBoolean("autoSync",false);
        }catch(Exception ignored){}
    }

    void persist(){
        try{
            JSONObject s=new JSONObject();
            s.put("dark",dark).put("reminder",reminder).put("theme",themeIndex)
             .put("hour",reminderHour).put("minute",reminderMinute).put("daysMask",reminderDaysMask)
 .put("syncUrl",syncUrl).put("autoSync",autoSync);
            getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                .putString(DATA,days.toString()).putString(SETTINGS,s.toString()).apply();

            File tmp=new File(getFilesDir(),"days_backup.tmp");
            File outFile=new File(getFilesDir(),"days_backup.json");
            FileOutputStream out=new FileOutputStream(tmp,false);
            out.write(days.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();out.close();
            if(!tmp.renameTo(outFile)){
                FileOutputStream direct=new FileOutputStream(outFile,false);
                direct.write(days.toString().getBytes(StandardCharsets.UTF_8));
                direct.flush();direct.close();tmp.delete();
            }
        }catch(Exception ignored){}
    }

    TextView tv(String s,float size,int color){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(dp(2),dp(2),dp(2),dp(2));return v;
    }

    GradientDrawable glass(int fill,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        return g;
    }

    GradientDrawable outlineShape(int fill,int strokeColor,int strokeWidth,int radius){
        GradientDrawable g=glass(fill,radius);
        g.setStroke(dp(strokeWidth),strokeColor);
        return g;
    }

    String hm(int h,int m){
        return String.format(Locale.US,"%02d:%02d",h,m);
    }

    static final int REQ_EXPORT=7001, REQ_IMPORT=7002;

    void exportBackup(){
        try{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE,"אתגר-יומי-גיבוי-"+new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())+".json");
            startActivityForResult(i,REQ_EXPORT);
        }catch(Exception e){Toast.makeText(this,"לא ניתן לפתוח שמירת גיבוי",Toast.LENGTH_SHORT).show();}
    }

    void importBackup(){
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            startActivityForResult(i,REQ_IMPORT);
        }catch(Exception e){Toast.makeText(this,"לא ניתן לפתוח בחירת גיבוי",Toast.LENGTH_SHORT).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        try{
            InputStream in;
            if(requestCode==REQ_EXPORT){
                JSONObject wrapper=new JSONObject();
                wrapper.put("format","daily-challenge-backup").put("version",1)
                    .put("createdAt",System.currentTimeMillis()).put("days",days);
                OutputStream out=getContentResolver().openOutputStream(uri);
                if(out==null)throw new IllegalStateException();
                out.write(wrapper.toString(2).getBytes(StandardCharsets.UTF_8));
                out.flush();out.close();
                Toast.makeText(this,"הגיבוי נשמר בהצלחה ✅",Toast.LENGTH_LONG).show();
            }else if(requestCode==REQ_IMPORT){
                in=getContentResolver().openInputStream(uri);
                if(in==null)throw new IllegalStateException();
                java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream();
                byte[] chunk=new byte[8192];int n;
                while((n=in.read(chunk))!=-1)buf.write(chunk,0,n);
                in.close();
                JSONObject wrapper=new JSONObject(new String(buf.toByteArray(),StandardCharsets.UTF_8));
                JSONObject restored=wrapper.optJSONObject("days");
                if(restored==null)throw new IllegalArgumentException();
                new AlertDialog.Builder(this)
                    .setTitle("שחזור גיבוי")
                    .setMessage("השחזור יחליף את ימי המעקב הקיימים בגיבוי. להמשיך?")
                    .setNegativeButton("ביטול",null)
                    .setPositiveButton("שחזור",(d,w)->{
                        try{
                            days=new JSONObject(restored.toString());
                            persist();
                            Toast.makeText(this,"הגיבוי שוחזר בהצלחה ✅",Toast.LENGTH_LONG).show();
                            showHome();
                        }catch(Exception ex){
                            Toast.makeText(this,"השחזור נכשל",Toast.LENGTH_LONG).show();
                        }
                    }).show();
            }
        }catch(Exception e){
            Toast.makeText(this,requestCode==REQ_EXPORT?"שמירת הגיבוי נכשלה":"קובץ הגיבוי לא תקין",Toast.LENGTH_LONG).show();
        }
    }

    double averageScore(){
        int count=0,total=0;
        Iterator<String> it=days.keys();
        while(it.hasNext()){
            JSONObject o=days.optJSONObject(it.next());
            if(o==null) continue;
            int s=o.optInt("score",0);
            if(s>=1 && s<=10){ total+=s; count++; }
        }
        return count==0?Double.NaN:(double)total/count;
    }

    Button button(String s){
        Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(text());b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);
        b.setPadding(dp(7),0,dp(7),0);
        b.setBackground(glass(dark?Color.rgb(36,43,57):Color.rgb(232,235,241),15));
        b.setElevation(0);
        return b;
    }

    LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(15),dp(8),dp(15),dp(18));l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    LinearLayout card(){LinearLayout c=vertical();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(glass(panelColor(),28));c.setElevation(dp(5));return c;}
    View space(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return v;}

    void base(String heading,boolean showNav,boolean settingsPage){
        root=new FrameLayout(this);
        root.setBackgroundColor(bg());
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout toolbar=new FrameLayout(this);toolbar.setPadding(dp(14),dp(10),dp(14),dp(7));
        TextView t=tv(heading,27,text());t.setTypeface(null,1);t.setGravity(Gravity.CENTER);
        toolbar.addView(t,new FrameLayout.LayoutParams(-1,dp(62)));
        Button gear=button(settingsPage?"‹":"⚙");gear.setTextSize(23);gear.setPadding(0,0,0,0);
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(dp(52),dp(52),Gravity.RIGHT|Gravity.CENTER_VERTICAL);toolbar.addView(gear,gp);
        gear.setOnClickListener(v->{if(settingsPage)showHome();else showSettings();});
        shell.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(70)));

        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.addView(content);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(showNav)shell.addView(nav(),new LinearLayout.LayoutParams(-1,dp(76)));
        setContentView(root);
    }

    View nav(){
        LinearLayout bar=new LinearLayout(this);
        bar.setPadding(dp(8),dp(5),dp(8),dp(10));
        bar.setGravity(Gravity.CENTER);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bar.setBackgroundColor(Color.TRANSPARENT);

        String[] labels={"בית","לוח שנה","התקדמות"};
        String[] icons={"🏠","📅","💪"};

        for(int i=0;i<3;i++){
            final int page=i;
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4),dp(5),dp(4),dp(5));
            item.setBackground(glass(dark?Color.rgb(28,34,47):Color.WHITE,18));

            TextView icon=tv(icons[i],22,text());
            icon.setGravity(Gravity.CENTER);
            item.addView(icon,new LinearLayout.LayoutParams(-1,dp(30)));

            TextView label=tv(labels[i],12,text());
            label.setGravity(Gravity.CENTER);
            label.setTypeface(null,1);
            item.addView(label,new LinearLayout.LayoutParams(-1,dp(22)));

            item.setOnClickListener(v->{
                if(page==0)showHome();
                else if(page==1)showCalendar();
                else showStats();
            });

            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(62),1);
            ip.setMargins(dp(5),0,dp(5),0);
            bar.addView(item,ip);
        }
        return bar;
    }

    void section(String eyebrow,String heading){
        TextView e=tv(eyebrow.toUpperCase(Locale.ROOT),10,muted());e.setTypeface(null,1);content.addView(e,new LinearLayout.LayoutParams(-1,dp(22)));
        TextView h=tv(heading,25,text());h.setTypeface(null,1);content.addView(h,new LinearLayout.LayoutParams(-1,dp(45)));
    }

    String key(java.util.Calendar d){return String.format(Locale.US,"%04d-%02d-%02d",d.get(Calendar.YEAR),d.get(Calendar.MONTH)+1,d.get(Calendar.DAY_OF_MONTH));}
    java.util.Calendar day(java.util.Calendar d){java.util.Calendar x=(java.util.Calendar)d.clone();x.set(Calendar.HOUR_OF_DAY,12);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);return x;}
    java.util.Calendar parse(String k){String[] a=k.split("-");java.util.Calendar d=java.util.Calendar.getInstance();d.set(Integer.parseInt(a[0]),Integer.parseInt(a[1])-1,Integer.parseInt(a[2]),12,0,0);return d;}
    JSONObject entry(String k){
        JSONObject e=days.optJSONObject(k);
        if(e==null || e.optBoolean("deleted",false)) return null;
        return e;
    }
    JSONObject entry(java.util.Calendar d){return entry(key(d));}

    void saveDay(String k,boolean success,int score,String note){
        JSONObject o=new JSONObject();
        try{
            o.put("success",success).put("score",score).put("note",note==null?"":note)
             .put("deleted",false).put("updatedAt",System.currentTimeMillis());
            days.put(k,o);
        }catch(Exception ignored){}
        persist();
        if(autoSync && !syncUrl.trim().isEmpty()) syncAsync(false);
    }

    void removeDay(String k){
        JSONObject tombstone=new JSONObject();
        try{tombstone.put("deleted",true).put("updatedAt",System.currentTimeMillis());days.put(k,tombstone);}
        catch(Exception ignored){}
        persist();
        if(autoSync && !syncUrl.trim().isEmpty()) syncAsync(false);
    }

    String readStream(InputStream in) throws Exception{
        java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream();
        byte[] chunk=new byte[8192];
        int n;
        while((n=in.read(chunk))!=-1)buf.write(chunk,0,n);
        return new String(buf.toByteArray(),StandardCharsets.UTF_8);
    }

    void syncAsync(final boolean showMessage){
        final String target=syncUrl==null?"":syncUrl.trim().replaceAll("/+$","");
        if(target.isEmpty() || syncing)return;
        syncing=true;
        new Thread(()->{
            String error=null;
            try{
                URL infoUrl=new URL(target+"/api/info");
                HttpURLConnection infoConn=(HttpURLConnection)infoUrl.openConnection();
                infoConn.setConnectTimeout(7000);
                infoConn.setReadTimeout(7000);
                infoConn.setRequestMethod("GET");
                int infoCode=infoConn.getResponseCode();
                InputStream infoIn=infoCode>=200&&infoCode<300?infoConn.getInputStream():infoConn.getErrorStream();
                String infoResponse=infoIn==null?"":readStream(infoIn);
                if(infoIn!=null)infoIn.close();
                infoConn.disconnect();
                if(infoCode<200||infoCode>=300)throw new IllegalStateException("שרת המחשב לא זמין (HTTP "+infoCode+")");

                URL url=new URL(target+"/api/state");
                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");

                JSONObject body=new JSONObject();
                body.put("version",1);
                body.put("days",days);
                OutputStream out=conn.getOutputStream();
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();out.close();

                int code=conn.getResponseCode();
                InputStream in=code>=200 && code<300?conn.getInputStream():conn.getErrorStream();
                String response=in==null?"":readStream(in);
                if(in!=null)in.close();
                conn.disconnect();
                if(code<200 || code>=300)throw new IllegalStateException("שגיאת שרת (HTTP "+code+")");

                JSONObject remote=new JSONObject(response);
                JSONObject merged=remote.optJSONObject("days");
                if(merged==null)throw new IllegalArgumentException("שרת המחשב החזיר תשובה לא תקינה");
                days=new JSONObject(merged.toString());
                persist();
            }catch(Exception ex){
                error=ex.getMessage();
                if(error==null||error.trim().isEmpty())error="לא ניתן להתחבר למחשב";
                if(error.contains("Failed to connect")||error.contains("Connection refused")||error.contains("timeout")){
                    error="לא ניתן להגיע למחשב. בדוק שהמחשב והטלפון באותה רשת ושחומת האש מאפשרת את התוכנה";
                }
            }
            final String finalError=error;
            runOnUiThread(()->{
                syncing=false;
                Toast.makeText(MainActivity.this,
                    finalError==null?"הסנכרון הושלם בהצלחה ✅":(showMessage?"הסנכרון נכשל: "+finalError:"סנכרון נכשל"),
                    showMessage?Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
            });
        },"challenge-sync").start();
    }


    int successCount(){int n=0;Iterator<String>it=days.keys();while(it.hasNext()){JSONObject o=days.optJSONObject(it.next());if(o!=null&&o.optBoolean("success"))n++;}return n;}
    boolean same(java.util.Calendar a,java.util.Calendar b){return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);}
    boolean sameDay(java.util.Calendar a,java.util.Calendar b){return same(a,b);}
    int currentStreak(){
        java.util.Calendar d=day(java.util.Calendar.getInstance());JSONObject today=entry(d);
        if(today==null||!today.optBoolean("success"))d.add(Calendar.DATE,-1);
        int n=0;while(true){JSONObject o=entry(d);if(o==null||!o.optBoolean("success"))break;n++;d.add(Calendar.DATE,-1);}return n;
    }
    int bestStreak(){
        ArrayList<String> ks=new ArrayList<>();Iterator<String>it=days.keys();while(it.hasNext())ks.add(it.next());Collections.sort(ks);
        int n=0,b=0;java.util.Calendar prev=null;
        for(String k:ks){JSONObject o=entry(k);if(o==null||!o.optBoolean("success")){n=0;prev=null;continue;}java.util.Calendar d=parse(k);
            if(prev!=null){java.util.Calendar q=(java.util.Calendar)prev.clone();q.add(Calendar.DATE,1);if(!same(q,d))n=0;}n++;b=Math.max(b,n);prev=d;}return b;
    }

    java.util.Calendar hebrewCalendar(java.util.Calendar g){
        android.icu.util.Calendar h=android.icu.util.Calendar.getInstance(TimeZone.getDefault(),Locale.forLanguageTag("he-IL-u-ca-hebrew"));
        h.setTimeInMillis(g.getTimeInMillis());return g;
    }

    android.icu.util.Calendar hebrew(java.util.Calendar g){
        android.icu.util.Calendar h=android.icu.util.Calendar.getInstance(TimeZone.getDefault(),Locale.forLanguageTag("he-IL-u-ca-hebrew"));
        h.setTimeInMillis(g.getTimeInMillis());return h;
    }

    String hebrewLetters(int n){
        if(n<=0)return "";
        String[] units={"","א","ב","ג","ד","ה","ו","ז","ח","ט"};
        String[] tens={"","י","כ","ל","מ","נ","ס","ע","פ","צ"};
        String[] hundreds={"","ק","ר","ש","ת"};
        StringBuilder out=new StringBuilder();
        int h=n/400; n%=400; while(h-->0)out.append("ת");
        if(n>=300){out.append("ש");n-=300;}else if(n>=200){out.append("ר");n-=200;}else if(n>=100){out.append("ק");n-=100;}
        if(n>=90){out.append("צ");n-=90;}else if(n>=80){out.append("פ");n-=80;}else if(n>=70){out.append("ע");n-=70;}else if(n>=60){out.append("ס");n-=60;}else if(n>=50){out.append("נ");n-=50;}else if(n>=40){out.append("מ");n-=40;}else if(n>=30){out.append("ל");n-=30;}else if(n>=20){out.append("כ");n-=20;}else if(n>=10){out.append("י");n-=10;}
        if(n>0)out.append(units[n]);
        String s=out.toString();
        if(s.length()==1)return s+"׳";
        if(s.equals("יה"))return "ט״ו";
        if(s.equals("יו"))return "ט״ז";
        return s.substring(0,s.length()-1)+"״"+s.substring(s.length()-1);
    }

    String hebrewYearLetters(int y){int shortYear=y>=5000?y-5000:y;return hebrewLetters(shortYear);}

    String hebrewDay(java.util.Calendar g){return hebrewLetters(hebrew(g).get(android.icu.util.Calendar.DAY_OF_MONTH));}
    String hebrewMonth(java.util.Calendar g){try{return new SimpleDateFormat("LLLL",Locale.forLanguageTag("he-IL-u-ca-hebrew")).format(new Date(g.getTimeInMillis()));}catch(Exception e){return "חודש עברי";}}
    String hebrewFull(java.util.Calendar g){
        android.icu.util.Calendar h=hebrew(g);
        String month=hebrewMonth(g);
        String year=hebrewYearLetters(h.get(android.icu.util.Calendar.YEAR));
        return hebrewDay(g)+" "+month+" "+year;
    }

    java.util.Calendar hebrewMonthStart(java.util.Calendar reference){
        android.icu.util.Calendar h=hebrew(reference);
        h.set(android.icu.util.Calendar.DAY_OF_MONTH,1);
        java.util.Calendar g=java.util.Calendar.getInstance();
        g.setTimeInMillis(h.getTimeInMillis());
        return day(g);
    }

    java.util.Calendar hebrewMonthEnd(java.util.Calendar reference){
        android.icu.util.Calendar h=hebrew(reference);
        int last=h.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);
        h.set(android.icu.util.Calendar.DAY_OF_MONTH,last);
        java.util.Calendar g=java.util.Calendar.getInstance();
        g.setTimeInMillis(h.getTimeInMillis());
        return day(g);
    }

    void moveHebrewMonth(int delta){
        android.icu.util.Calendar h=hebrew(cursor);
        h.set(android.icu.util.Calendar.DAY_OF_MONTH,1);
        h.add(android.icu.util.Calendar.MONTH,delta);
        java.util.Calendar g=java.util.Calendar.getInstance();
        g.setTimeInMillis(h.getTimeInMillis());
        cursor.setTimeInMillis(g.getTimeInMillis());
    }

    String hebrewMonthHeader(java.util.Calendar reference){
        android.icu.util.Calendar h=hebrew(reference);
        return hebrewMonth(reference)+" "+hebrewYearLetters(h.get(android.icu.util.Calendar.YEAR));
    }

    String gregNumeric(java.util.Calendar g){return String.format(Locale.US,"%02d/%02d/%04d",g.get(Calendar.DAY_OF_MONTH),g.get(Calendar.MONTH)+1,g.get(Calendar.YEAR));}
    int dayIndex(){java.util.Calendar d=day(java.util.Calendar.getInstance());return d.get(Calendar.DAY_OF_YEAR)-1;}

    void addStatChip(LinearLayout p,String icon,String value,String label){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(dp(5),0,dp(5),0);
        TextView a=tv(icon+" "+value,22,text());a.setGravity(Gravity.CENTER);a.setTypeface(null,1);box.addView(a);
        TextView b=tv(label,10,muted());b.setGravity(Gravity.CENTER);box.addView(b);
        p.addView(box,new LinearLayout.LayoutParams(0,dp(68),1));
    }

    void showHome(){
        screen=0;navButtons.clear();base("אתגר יומי",true,false);
        TextView eyebrow=tv("מעקב אישי  •  "+gregNumeric(day(java.util.Calendar.getInstance())),10,muted());eyebrow.setTypeface(null,1);content.addView(eyebrow);
        TextView heroTitle=tv(currentStreak()>=3?"כבר "+currentStreak()+" ימים ברצף. 🔥":"צעד קטן. רצף גדול. ✨",29,text());heroTitle.setTypeface(null,1);content.addView(heroTitle,new LinearLayout.LayoutParams(-1,dp(54)));
        JSONObject e=entry(day(java.util.Calendar.getInstance()));
        TextView sub=tv(e==null?"היום לא צריך להיות מושלם — רק אמיתי.":e.optBoolean("success")?"הוכחת לעצמך שאתה יכול לבחור שוב. ✅":"יום אחד לא מגדיר אותך. מחר מתחילים שוב. 💪",13,muted());content.addView(sub,new LinearLayout.LayoutParams(-1,dp(40)));

        LinearLayout stats=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);addStatChip(row,"🔥",String.valueOf(currentStreak()),"רצף");addStatChip(row,"✅",String.valueOf(successCount()),"הצלחות");addStatChip(row,"🏆",String.valueOf(bestStreak()),"שיא");stats.addView(row);content.addView(stats);content.addView(space(12));

        java.util.Calendar today=day(java.util.Calendar.getInstance());
        LinearLayout t=card();t.addView(tv("היום 📅",11,accent()));
        TextView tg=tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(today.getTime()),19,text());tg.setTypeface(null,1);t.addView(tg);
        TextView th=tv(hebrewFull(today),22,text());th.setTypeface(null,1);t.addView(th);
        TextView small=tv(gregNumeric(today),10,muted());t.addView(small);
        String status=e==null?"עדיין לא עודכן  •  לחץ לעדכון":e.optBoolean("success")?"הצלחת היום ✅  •  ציון ⭐ "+e.optInt("score")+"/10":"לא הצלחת הפעם 🤍  •  ציון ⭐ "+e.optInt("score")+"/10";
        TextView st=tv(status,14,e==null?accent():(e.optBoolean("success")?GREEN:RED));st.setPadding(0,dp(9),0,dp(9));t.addView(st);
        Button edit=button("עדכן היום");edit.setBackground(glass(accent(),16));edit.setTextColor(Color.WHITE);edit.setOnClickListener(v->openEditor(today));t.addView(edit,new LinearLayout.LayoutParams(-1,dp(50)));content.addView(t);content.addView(space(12));

        LinearLayout quote=card();
        quote.addView(tv("✦ משפט מוטיבציה",11,accent()));
        LinearLayout quoteRow=new LinearLayout(this);
        quoteRow.setGravity(Gravity.CENTER_VERTICAL);
        quoteRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ImageView scene=new ImageView(this);
        scene.setImageResource(quoteImages[(int)(System.currentTimeMillis()/86400000)%quoteImages.length]);
        scene.setScaleType(ImageView.ScaleType.CENTER_CROP);
        quoteRow.addView(scene,new LinearLayout.LayoutParams(dp(130),dp(130)));
        String[] dailyQuotes={"אל תחכה למוטיבציה. תתחיל, והמוטיבציה תדביק אותך.","גם צעד קטן הוא תנועה קדימה.","אתה לא צריך לנצח את כל החיים היום. רק את היום הזה.","רצף נבנה מימים אמיתיים, לא מימים מושלמים.","הצלחה היא היכולת לחזור למסלול פעם נוספת."};
        TextView qText=tv(dailyQuotes[(int)(System.currentTimeMillis()/86400000)%dailyQuotes.length],18,text());
        qText.setPadding(dp(12),0,dp(8),0);
        quoteRow.addView(qText,new LinearLayout.LayoutParams(0,dp(130),1));
        quote.addView(quoteRow);
        quote.addView(tv("גם מחר מחכה לך יום חדש. 🌿",11,muted()));
        content.addView(quote);
        TextView enc=tv(currentStreak()>0?"שמור על הקצב. עוד יום אחד יכול להפוך את הרצף להרגל. 🔥":"גם התחלה מחדש היא הצלחה. היום אפשר לבנות את היום הראשון. 🌱",13,muted());enc.setPadding(dp(5),dp(16),dp(5),dp(10));content.addView(enc);
    }

    void showCalendar(){
        screen=1;navButtons.clear();base("לוח שנה",true,false);
        TextView k=tv("לוח עברי־לועזי",10,muted());k.setTypeface(null,1);content.addView(k);

        LinearLayout monthCard=card();
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);

        Button prev=button("‹"),next=button("›"),todayBtn=button("חזרה להיום");
        LinearLayout monthText=new LinearLayout(this);
        monthText.setOrientation(LinearLayout.VERTICAL);
        monthText.setGravity(Gravity.CENTER);

        TextView hm=tv(hebrewMonthHeader(cursor),27,text());
        hm.setGravity(Gravity.CENTER);hm.setTypeface(null,1);

        java.util.Calendar startHeb=hebrewMonthStart(cursor);
        java.util.Calendar endHeb=hebrewMonthEnd(cursor);
        TextView gy=tv(
                String.format(Locale.US,"%02d/%04d → %02d/%04d",
                        startHeb.get(Calendar.MONTH)+1,startHeb.get(Calendar.YEAR),
                        endHeb.get(Calendar.MONTH)+1,endHeb.get(Calendar.YEAR)),
                10,muted());
        gy.setGravity(Gravity.CENTER);

        monthText.addView(hm,new LinearLayout.LayoutParams(-1,dp(40)));
        monthText.addView(gy,new LinearLayout.LayoutParams(-1,dp(21)));

        head.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(52)));
        head.addView(monthText,new LinearLayout.LayoutParams(0,dp(68),1));
        head.addView(next,new LinearLayout.LayoutParams(dp(48),dp(52)));
        monthCard.addView(head);

        java.util.Calendar realToday=day(java.util.Calendar.getInstance());
        android.icu.util.Calendar currentHeb=hebrew(realToday);
        android.icu.util.Calendar selectedHeb=hebrew(cursor);

        boolean sameHebrewMonth=
                currentHeb.get(android.icu.util.Calendar.YEAR)==selectedHeb.get(android.icu.util.Calendar.YEAR) &&
                currentHeb.get(android.icu.util.Calendar.MONTH)==selectedHeb.get(android.icu.util.Calendar.MONTH);

        todayBtn.setVisibility(sameHebrewMonth?View.GONE:View.VISIBLE);
        todayBtn.setText("חזרה להיום 📅");
        todayBtn.setOnClickListener(v->{cursor.setTimeInMillis(realToday.getTimeInMillis());showCalendar();});
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(44));
        tp.topMargin=dp(6);
        monthCard.addView(todayBtn,tp);

        content.addView(monthCard);
        content.addView(space(10));

        GridLayout grid=new GridLayout(this);
        grid.setColumnCount(7);
        grid.setPadding(dp(5),dp(8),dp(5),dp(8));
        grid.setBackgroundColor(panelColor());

        String[] week={"א","ב","ג","ד","ה","ו","ש"};
        for(String w:week){
            TextView x=tv(w,10,muted());
            x.setGravity(Gravity.CENTER);
            grid.addView(x,cell(26));
        }

        java.util.Calendar firstHeb=hebrewMonthStart(cursor);
        java.util.Calendar lastHeb=hebrewMonthEnd(cursor);

        // Start from the Sunday before/at the first Hebrew date so the grid is aligned.
        java.util.Calendar gridStart=day(firstHeb);
        gridStart.add(Calendar.DATE,1-gridStart.get(Calendar.DAY_OF_WEEK));

        // End from the Saturday after/at the last Hebrew date.
        java.util.Calendar gridEnd=day(lastHeb);
        gridEnd.add(Calendar.DATE,7-gridEnd.get(Calendar.DAY_OF_WEEK));

        java.util.Calendar walk=(java.util.Calendar)gridStart.clone();
        while(!walk.after(gridEnd)){
            java.util.Calendar d=day(walk);
            android.icu.util.Calendar h=hebrew(d);

            boolean inHebrewMonth=
                    h.get(android.icu.util.Calendar.YEAR)==selectedHeb.get(android.icu.util.Calendar.YEAR) &&
                    h.get(android.icu.util.Calendar.MONTH)==selectedHeb.get(android.icu.util.Calendar.MONTH);

            JSONObject en=inHebrewMonth?entry(d):null;
            boolean isToday=inHebrewMonth&&sameDay(d,realToday);
            boolean success=en!=null&&en.optBoolean("success");

            int fill;
            int mainColor;
            if(!inHebrewMonth){
                fill=dark?Color.rgb(20,25,35):Color.rgb(246,247,249);
                mainColor=Color.argb(dark?85:75,120,128,145);
            }else{
                fill=en==null?panelColor():(success?Color.rgb(225,236,255):Color.rgb(252,226,229));
                mainColor=en==null?text():(success?Color.rgb(38,103,214):Color.rgb(199,54,70));
            }

            LinearLayout cellBox=new LinearLayout(this);
            cellBox.setOrientation(LinearLayout.VERTICAL);
            cellBox.setGravity(Gravity.CENTER);
            cellBox.setPadding(dp(2),dp(3),dp(2),dp(3));

            if(inHebrewMonth && isToday){
                cellBox.setBackground(outlineShape(fill,BLUE,2,12));
            }else{
                cellBox.setBackground(glass(fill,12));
            }

            TextView hd=tv(hebrewDay(d),inHebrewMonth?19:15,mainColor);
            hd.setGravity(Gravity.CENTER);
            if(inHebrewMonth)hd.setTypeface(null,1);
            cellBox.addView(hd,new LinearLayout.LayoutParams(-1,dp(32)));

            TextView gd=tv(String.valueOf(d.get(Calendar.DAY_OF_MONTH)),inHebrewMonth?10:9,mainColor);
            gd.setGravity(Gravity.CENTER);
            cellBox.addView(gd,new LinearLayout.LayoutParams(-1,dp(20)));

            if(inHebrewMonth){
                TextView mark=tv(en==null?"":(success?"✓":"×"),11,mainColor);
                mark.setGravity(Gravity.CENTER);
                cellBox.addView(mark,new LinearLayout.LayoutParams(-1,dp(16)));
                cellBox.setOnClickListener(v->openEditor(d));
            }else{
                cellBox.addView(space(16));
            }

            grid.addView(cellBox,cell(72));
            walk.add(Calendar.DATE,1);
        }

        content.addView(grid);
        content.addView(space(7));

        TextView legend=tv(
                "החודש העברי הוא הקובע\n"+
                "הימים של החודש העברי מודגשים • לועזי קטן\n"+
                "היום במסגרת כחולה • כחול = הצלחה ✅ • אדום = לא הצלחתי ❌",
                10,muted());
        legend.setGravity(Gravity.CENTER);
        content.addView(legend);

        prev.setOnClickListener(v->{moveHebrewMonth(-1);showCalendar();});
        next.setOnClickListener(v->{moveHebrewMonth(1);showCalendar();});
    }

    GridLayout.LayoutParams cell(int h){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(h);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}

    void showStats(){
        screen=2;navButtons.clear();base("התקדמות",true,false);
        section("הנתונים שלך","המסע שלך 💪");

        LinearLayout c=card();
        addBig(c,"🔥 רצף נוכחי",String.valueOf(currentStreak()));
        addBig(c,"🏆 שיא אישי",String.valueOf(bestStreak()));
        addBig(c,"✅ ימים שהצלחת",String.valueOf(successCount()));

        double avg=averageScore();
        addBig(c,"⭐ ממוצע ציון",Double.isNaN(avg)?"—":String.format(Locale.US,"%.1f/10",avg));
        content.addView(c);
        content.addView(space(10));

        LinearLayout tip=card();
        tip.addView(tv("עידוד",11,accent()));
        tip.addView(tv(currentStreak()>0?"אתה כבר בתוך רצף. עוד יום אחד יכול לחזק את ההרגל. 🔥":"גם התחלה מחדש היא הצלחה. היום אפשר להתחיל. 🌱",18,text()));
        content.addView(tip);
    }

    void addBig(LinearLayout c,String label,String value){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView v=tv(value,27,text());v.setTypeface(null,1);r.addView(v,new LinearLayout.LayoutParams(dp(135),dp(62)));TextView l=tv(label,13,muted());r.addView(l,new LinearLayout.LayoutParams(0,dp(62),1));c.addView(r);}

    void showSettings(){
        screen=3;navButtons.clear();base("הגדרות",false,true);
        section("התאמה אישית","המראה והתזכורות ⚙️");

        LinearLayout c=card();

        c.addView(tv("ערכת נושא 🎨",13,accent()));
        LinearLayout themeGrid=new LinearLayout(this);
        themeGrid.setOrientation(LinearLayout.VERTICAL);

        final RadioButton[] themeButtons=new RadioButton[themeNames.length];
        for(int i=0;i<themeNames.length;i++){
            final int index=i;
            RadioButton rb=new RadioButton(this);
            rb.setText(themeNames[i]);
            rb.setTextSize(14);
            rb.setTextColor(text());
            rb.setChecked(themeIndex==i);
            rb.setButtonTintList(new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                    new int[]{accent(),muted()}
            ));
            rb.setOnClickListener(v->{themeIndex=index;persist();showSettings();});
            themeButtons[i]=rb;
            themeGrid.addView(rb,new LinearLayout.LayoutParams(-1,dp(46)));
        }
        c.addView(themeGrid);

        c.addView(space(8));

        LinearLayout appearance=new LinearLayout(this);appearance.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=tv("מצב כהה 🌙\nמראה נקי ומודרני",14,text());
        appearance.addView(a,new LinearLayout.LayoutParams(0,dp(64),1));
        Switch sw=new Switch(this);sw.setChecked(dark);
        sw.setOnCheckedChangeListener((b,checked)->{dark=checked;persist();showSettings();});
        appearance.addView(sw,new LinearLayout.LayoutParams(dp(60),dp(55)));
        c.addView(appearance);

        c.addView(space(8));

        LinearLayout remRow=new LinearLayout(this);remRow.setGravity(Gravity.CENTER_VERTICAL);
        remRow.addView(tv("תזכורת יומית 🔔\nהתראה גם כשהאפליקציה סגורה",14,text()),new LinearLayout.LayoutParams(0,dp(64),1));
        Switch rs=new Switch(this);rs.setChecked(reminder);
        rs.setOnCheckedChangeListener((b,checked)->{
            reminder=checked;persist();
            if(checked){requestNotificationPermissionIfNeeded();scheduleReminder();}else cancelReminder();
        });
        remRow.addView(rs,new LinearLayout.LayoutParams(dp(60),dp(55)));
        c.addView(remRow);

        c.addView(space(8));

        Button timeButton=button("שעת התזכורת  ⏰  "+hm(reminderHour,reminderMinute));
        timeButton.setOnClickListener(v->{
            TimePickerDialog picker=new TimePickerDialog(
                    MainActivity.this,
                    (view,hourOfDay,minute)->{
                        reminderHour=hourOfDay;reminderMinute=minute;
                        persist();
                        if(reminder)scheduleReminder();
                        showSettings();
                    },
                    reminderHour,reminderMinute,true
            );
            picker.setTitle("בחר שעה לתזכורת");
            picker.show();
        });
        c.addView(timeButton,new LinearLayout.LayoutParams(-1,dp(52)));

        c.addView(tv("ימים לתזכורת 📆",13,accent()));
        final String[] dayLabels={"א׳","ב׳","ג׳","ד׳","ה׳","ו׳","ש׳"};
        LinearLayout daysRow1=new LinearLayout(this);
        LinearLayout daysRow2=new LinearLayout(this);
        daysRow1.setOrientation(LinearLayout.HORIZONTAL);
        daysRow2.setOrientation(LinearLayout.HORIZONTAL);
        daysRow1.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        daysRow2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        for(int i=0;i<7;i++){
            final int bit=i;
            Button db=button(dayLabels[i]);
            db.setTextSize(13);
            boolean selectedDay=(reminderDaysMask & (1<<bit))!=0;
            db.setBackgroundColor(selectedDay?accent():(dark?Color.rgb(36,43,57):Color.rgb(232,235,241)));
            db.setTextColor(selectedDay?Color.WHITE:text());
            db.setOnClickListener(v->{
                reminderDaysMask ^= (1<<bit);
                if(reminderDaysMask==0)reminderDaysMask=1<<bit;
                persist();
                if(reminder)scheduleReminder();
                showSettings();
            });
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(44),1);
            bp.setMargins(dp(2),dp(2),dp(2),dp(2));
            if(i<4)daysRow1.addView(db,bp);else daysRow2.addView(db,bp);
        }
        c.addView(daysRow1,new LinearLayout.LayoutParams(-1,dp(50)));
        c.addView(daysRow2,new LinearLayout.LayoutParams(-1,dp(50)));
        c.addView(tv("כחול = התזכורת תישלח ביום הזה",10,muted()));
        c.addView(space(8));
        c.addView(tv("גיבוי הנתונים 💾",13,accent()));
        Button exportButton=button("הוצא גיבוי לימים 📤");
        exportButton.setOnClickListener(v->exportBackup());
        c.addView(exportButton,new LinearLayout.LayoutParams(-1,dp(48)));
        Button importButton=button("שחזר גיבוי לימים 📥");
        importButton.setOnClickListener(v->importBackup());
        c.addView(importButton,new LinearLayout.LayoutParams(-1,dp(48)));

        c.addView(space(8));
        c.addView(tv("סנכרון עם המחשב 💻",13,accent()));
        c.addView(tv("פתח את תוכנת ״אתגר יומי״ במחשב. היא תציג כתובת כמו http://192.168.1.25:39225. הטלפון והמחשב צריכים להיות באותה רשת Wi‑Fi.",11,muted()));

        EditText syncField=new EditText(this);
        syncField.setSingleLine(true);
        syncField.setText(syncUrl);
        syncField.setHint("כתובת הסנכרון מהמחשב");
        syncField.setTextColor(text());
        syncField.setHintTextColor(muted());
        syncField.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        c.addView(syncField,new LinearLayout.LayoutParams(-1,dp(54)));

        Button syncSave=button("שמור כתובת וסנכרן עכשיו 🔄");
        syncSave.setBackgroundColor(accent());
        syncSave.setTextColor(Color.WHITE);
        syncSave.setOnClickListener(v->{
            syncUrl=syncField.getText().toString().trim();
            persist();
            if(syncUrl.isEmpty()){
                Toast.makeText(this,"הכנס קודם את כתובת המחשב",Toast.LENGTH_SHORT).show();
                return;
            }
            syncAsync(true);
        });
        c.addView(syncSave,new LinearLayout.LayoutParams(-1,dp(48)));

        Button ping=button("בדוק חיבור למחשב 🧪");
        ping.setOnClickListener(v->{
            syncUrl=syncField.getText().toString().trim();
            persist();
            if(syncUrl.isEmpty()){
                Toast.makeText(this,"הכנס קודם את כתובת המחשב",Toast.LENGTH_SHORT).show();
                return;
            }
            syncAsync(true);
        });
        c.addView(ping,new LinearLayout.LayoutParams(-1,dp(44)));

        LinearLayout syncRow=new LinearLayout(this);
        syncRow.setGravity(Gravity.CENTER_VERTICAL);
        syncRow.addView(tv("סנכרון אוטומטי בעת שינוי נתונים",13,text()),new LinearLayout.LayoutParams(0,dp(52),1));
        Switch syncSwitch=new Switch(this);
        syncSwitch.setChecked(autoSync);
        syncSwitch.setOnCheckedChangeListener((b,checked)->{
            if(checked && syncUrl.trim().isEmpty()){
                b.setChecked(false);
                Toast.makeText(this,"שמור קודם את כתובת המחשב",Toast.LENGTH_SHORT).show();
                return;
            }
            autoSync=checked;
            persist();
            if(checked) syncAsync(false);
        });
        syncRow.addView(syncSwitch,new LinearLayout.LayoutParams(dp(60),dp(52)));
        c.addView(syncRow);

        Button syncNow=button("סנכרן עכשיו בלבד 🔄");
        syncNow.setOnClickListener(v->{
            if(syncField.getText().toString().trim().isEmpty()){
                Toast.makeText(this,"הכנס כתובת מחשב",Toast.LENGTH_SHORT).show();
                return;
            }
            syncUrl=syncField.getText().toString().trim();
            persist();
            syncAsync(true);
        });
        c.addView(syncNow,new LinearLayout.LayoutParams(-1,dp(48)));

        Button test=button("בדוק תזכורת עכשיו 🔔");
        test.setOnClickListener(v->{requestNotificationPermissionIfNeeded();sendTestNotification();});
        c.addView(test,new LinearLayout.LayoutParams(-1,dp(48)));

        Button reset=button("איפוס כל הנתונים");
        reset.setTextColor(RED);
        reset.setOnClickListener(v->showResetWarning());
        c.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));

        content.addView(c);
        content.addView(space(10));
        content.addView(tv("האפליקציה עובדת מקומית ואינה תלויה באתר חיצוני. ✅",12,muted()));
    }

    void showResetWarning(){
        LinearLayout box=vertical();
        TextView title=tv("איפוס כל הנתונים ⚠️",22,RED);title.setTypeface(null,1);box.addView(title);
        box.addView(tv("הפעולה תמחק את כל ימי המעקב, הציונים וההערות. אי אפשר לשחזר את הנתונים לאחר המחיקה.",14,text()));
        CheckBox confirm=new CheckBox(this);
        confirm.setText("אני מבין/ה שלא ניתן לשחזר את הנתונים");
        confirm.setTextColor(text());
        confirm.setTextSize(13);
        box.addView(confirm,new LinearLayout.LayoutParams(-1,dp(58)));

        Button continueButton=button("המשך לאישור");
        continueButton.setTextColor(Color.WHITE);
        continueButton.setBackgroundColor(RED);
        continueButton.setEnabled(false);
        confirm.setOnCheckedChangeListener((b,checked)->{
            continueButton.setEnabled(checked);
            continueButton.setAlpha(checked?1f:.45f);
        });
        box.addView(continueButton,new LinearLayout.LayoutParams(-1,dp(50)));

        AlertDialog first=new AlertDialog.Builder(this).setView(box).setNegativeButton("ביטול",null).create();
        continueButton.setOnClickListener(v->{
            first.dismiss();
            new AlertDialog.Builder(this)
                .setTitle("אישור אחרון")
                .setMessage("למחוק עכשיו את כל הנתונים לצמיתות?")
                .setNegativeButton("לא, ביטול",null)
                .setPositiveButton("כן, מחק הכל",(d,w)->{
                    days=new JSONObject();
                    persist();
                    showHome();
                })
                .show();
        });
        first.show();
    }

    void requestNotificationPermissionIfNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);}

    void openEditor(java.util.Calendar d){
        selected=day(d);String k=key(selected);JSONObject e=entry(k);

        LinearLayout box=vertical();
        box.setPadding(dp(22),dp(16),dp(22),dp(16));
        box.setBackground(glass(panelStrong(),28));

        box.addView(tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(d.getTime()),22,text()));
        TextView heb=tv(hebrewFull(d),25,text());heb.setTypeface(null,1);box.addView(heb);
        box.addView(tv(gregNumeric(d),10,muted()));
        box.addView(space(7));

        final boolean[] chosen={e!=null};
        final boolean[] ok={e!=null&&e.optBoolean("success")};
        final int[] score={e!=null?e.optInt("score",10):10};

        LinearLayout ch=new LinearLayout(this);
        Button yes=button("הצלחתי ✅"),no=button("לא הצלחתי ❌");
        ch.addView(yes,new LinearLayout.LayoutParams(0,dp(52),1));
        ch.addView(no,new LinearLayout.LayoutParams(0,dp(52),1));
        box.addView(ch);
        box.addView(space(5));

        box.addView(tv("איך היה היום? ⭐",12,muted()));
        final Button[] scoreButtons=new Button[10];
        LinearLayout row1=new LinearLayout(this), row2=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        for(int i=1;i<=10;i++){
            final int value=i;
            Button b=button(String.valueOf(i));
            b.setTextSize(14);
            b.setOnClickListener(v->{
                score[0]=value;
                for(int j=0;j<10;j++){
                    int n=j+1;
                    scoreButtons[j].setBackgroundColor(n==score[0]?BLUE:(dark?Color.rgb(36,43,57):Color.rgb(232,235,241)));
                    scoreButtons[j].setTextColor(n==score[0]?Color.WHITE:text());
                }
            });
            scoreButtons[i-1]=b;
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1);
            bp.setMargins(dp(2),dp(2),dp(2),dp(2));
            if(i<=5)row1.addView(b,bp);else row2.addView(b,bp);
        }
        box.addView(row1,new LinearLayout.LayoutParams(-1,dp(54)));
        box.addView(row2,new LinearLayout.LayoutParams(-1,dp(54)));

        for(int j=0;j<10;j++){
            int n=j+1;
            scoreButtons[j].setBackgroundColor(n==score[0]?BLUE:(dark?Color.rgb(36,43,57):Color.rgb(232,235,241)));
            scoreButtons[j].setTextColor(n==score[0]?Color.WHITE:text());
        }

        EditText note=new EditText(this);
        note.setHint("הערה לעצמך (לא חובה)");
        note.setText(e==null?"":e.optString("note",""));
        note.setTextColor(text());note.setHintTextColor(muted());
        box.addView(note,new LinearLayout.LayoutParams(-1,dp(90)));

        Button save=button("שמירת היום");
        save.setTextColor(Color.WHITE);
        save.setBackgroundColor(BLUE);
        box.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));

        Button clear=button("ניקוי הסימון");
        box.addView(clear,new LinearLayout.LayoutParams(-1,dp(48)));

        AlertDialog dialog=new AlertDialog.Builder(this).setView(box).create();

        yes.setOnClickListener(v->{chosen[0]=true;ok[0]=true;yes.setBackgroundColor(GREEN);no.setBackgroundColor(dark?Color.rgb(36,43,57):Color.rgb(232,235,241));});
        no.setOnClickListener(v->{chosen[0]=true;ok[0]=false;no.setBackgroundColor(RED);yes.setBackgroundColor(dark?Color.rgb(36,43,57):Color.rgb(232,235,241));});

        save.setOnClickListener(v->{
            if(!chosen[0]){Toast.makeText(this,"בחר קודם אם הצלחת או לא",Toast.LENGTH_SHORT).show();return;}
            saveDay(k,ok[0],score[0],note.getText().toString());
            dialog.dismiss();
            if(screen==1)showCalendar();else if(screen==2)showStats();else showHome();
        });

        clear.setOnClickListener(v->{
            removeDay(k);dialog.dismiss();
            if(screen==1)showCalendar();else if(screen==2)showStats();else showHome();
        });

        dialog.show();
    }

    void cancelReminder(){
        AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
        PendingIntent p=PendingIntent.getBroadcast(this,911,new Intent(this,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        a.cancel(p);
    }

    boolean reminderDayEnabled(int dayOfWeek){
        return (reminderDaysMask & (1 << (dayOfWeek-1))) != 0;
    }

    java.util.Calendar nextReminderTime(){
        java.util.Calendar now=java.util.Calendar.getInstance();
        for(int offset=0;offset<8;offset++){
            java.util.Calendar candidate=(java.util.Calendar)now.clone();
            candidate.add(java.util.Calendar.DATE,offset);
            candidate.set(java.util.Calendar.HOUR_OF_DAY,reminderHour);
            candidate.set(java.util.Calendar.MINUTE,reminderMinute);
            candidate.set(java.util.Calendar.SECOND,0);
            candidate.set(java.util.Calendar.MILLISECOND,0);
            if(reminderDayEnabled(candidate.get(java.util.Calendar.DAY_OF_WEEK)) && candidate.getTimeInMillis()>System.currentTimeMillis()){
                return candidate;
            }
        }
        return now;
    }

    void scheduleReminder(){
        AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,ReminderReceiver.class);
        PendingIntent p=PendingIntent.getBroadcast(this,911,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        a.cancel(p);
        if(!reminder)return;
        java.util.Calendar next=nextReminderTime();
        if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
        else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
    }

    void sendTestNotification(){
        requestNotificationPermissionIfNeeded();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"תזכורות אתגר יומי",NotificationManager.IMPORTANCE_HIGH));
        Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,912,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("תזכורת בדיקה 🔔").setContentText("האתגר שלך מחכה לך — גם צעד קטן נחשב.").setContentIntent(pi).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);
        nm.notify(912,b.build());
    }

    public static class ReminderReceiver extends BroadcastReceiver{
        @Override public void onReceive(Context c,Intent intent){
            android.content.SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            boolean enabled=false;int h=20,m=30,mask=127;
            try{
                JSONObject s=new JSONObject(p.getString(SETTINGS,"{}"));
                enabled=s.optBoolean("reminder",false);
                h=s.optInt("hour",20);m=s.optInt("minute",30);mask=s.optInt("daysMask",127);
                if(mask<1||mask>127)mask=127;
            }catch(Exception ignored){}
            if(!enabled)return;

            NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"תזכורות אתגר יומי",NotificationManager.IMPORTANCE_HIGH));
            Intent open=new Intent(c,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(c,912,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
            b.setSmallIcon(android.R.drawable.ic_popup_reminder)
             .setContentTitle("האתגר שלך מחכה לך 🔥")
             .setContentText("גם היום אפשר לעשות צעד אחד קטן קדימה.")
             .setContentIntent(pi).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);
            nm.notify(912,b.build());

            AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent np=PendingIntent.getBroadcast(c,911,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            java.util.Calendar now=java.util.Calendar.getInstance(),next=null;
            for(int offset=1;offset<=8;offset++){
                java.util.Calendar candidate=(java.util.Calendar)now.clone();
                candidate.add(java.util.Calendar.DATE,offset);
                candidate.set(java.util.Calendar.HOUR_OF_DAY,h);
                candidate.set(java.util.Calendar.MINUTE,m);
                candidate.set(java.util.Calendar.SECOND,0);
                candidate.set(java.util.Calendar.MILLISECOND,0);
                int dow=candidate.get(java.util.Calendar.DAY_OF_WEEK);
                if((mask & (1<<(dow-1)))!=0){next=candidate;break;}
            }
            if(next!=null){
                if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),np);
                else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),np);
            }
        }
    }

    public static class BootReceiver extends BroadcastReceiver{
        @Override public void onReceive(Context c,Intent intent){
            if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
            android.content.SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            try{
                JSONObject s=new JSONObject(p.getString(SETTINGS,"{}"));
                if(!s.optBoolean("reminder",false))return;
                int h=s.optInt("hour",20),m=s.optInt("minute",30),mask=s.optInt("daysMask",127);
                if(mask<1||mask>127)mask=127;
                AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pi=PendingIntent.getBroadcast(c,911,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                java.util.Calendar now=java.util.Calendar.getInstance(),next=null;
                for(int offset=0;offset<8;offset++){
                    java.util.Calendar candidate=(java.util.Calendar)now.clone();
                    candidate.add(java.util.Calendar.DATE,offset);
                    candidate.set(Calendar.HOUR_OF_DAY,h);candidate.set(Calendar.MINUTE,m);
                    candidate.set(Calendar.SECOND,0);candidate.set(Calendar.MILLISECOND,0);
                    int dow=candidate.get(Calendar.DAY_OF_WEEK);
                    if((mask & (1<<(dow-1)))!=0 && candidate.getTimeInMillis()>System.currentTimeMillis()){next=candidate;break;}
                }
                if(next!=null){
                    if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
                    else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
                }
            }catch(Exception ignored){}
        }
    }

}
