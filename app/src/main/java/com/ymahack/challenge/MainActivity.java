package com.ymahack.challenge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MainActivity extends Activity {
    static final String PREFS="challenge_store", DATA="days", SETTINGS="settings";
    static final String CHANNEL="challenge_reminders";
    static final int BG_DARK=Color.rgb(8,12,20), TEXT_DARK=Color.rgb(247,249,255), MUTED_DARK=Color.rgb(159,170,191);
    static final int BG_LIGHT=Color.rgb(240,243,249), TEXT_LIGHT=Color.rgb(21,25,35), MUTED_LIGHT=Color.rgb(101,112,130);
    static final int ACCENT=Color.rgb(111,102,244), GREEN=Color.rgb(47,198,141), RED=Color.rgb(239,106,119), GOLD=Color.rgb(246,190,72);

    JSONObject days=new JSONObject();
    boolean dark=true, reminder=false;
    int reminderHour=20, reminderMinute=30;
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
        R.drawable.motivation_1,R.drawable.motivation_2,R.drawable.motivation_3,
        R.drawable.motivation_4,R.drawable.motivation_5,R.drawable.motivation_6,R.drawable.motivation_7
    };

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    int bg(){return dark?BG_DARK:BG_LIGHT;}
    int text(){return dark?TEXT_DARK:TEXT_LIGHT;}
    int muted(){return dark?MUTED_DARK:MUTED_LIGHT;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        load();
        getWindow().setNavigationBarColor(bg());
        if(Build.VERSION.SDK_INT>=23)getWindow().setStatusBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);
        scheduleReminder();
        showHome();
    }

    void load(){
        try{days=new JSONObject(getSharedPreferences(PREFS,MODE_PRIVATE).getString(DATA,"{}"));}catch(Exception e){days=new JSONObject();}
        try{
            JSONObject s=new JSONObject(getSharedPreferences(PREFS,MODE_PRIVATE).getString(SETTINGS,"{}"));
            dark=s.optBoolean("dark",true); reminder=s.optBoolean("reminder",false);
            reminderHour=s.optInt("hour",20); reminderMinute=s.optInt("minute",30);
        }catch(Exception ignored){}
    }

    void persist(){
        try{
            JSONObject s=new JSONObject();
            s.put("dark",dark).put("reminder",reminder).put("hour",reminderHour).put("minute",reminderMinute);
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(DATA,days.toString()).putString(SETTINGS,s.toString()).apply();
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

    Button button(String s){
        Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(text());b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);
        b.setPadding(dp(7),0,dp(7),0);
        b.setBackground(glass(dark?Color.rgb(36,43,57):Color.rgb(232,235,241),15));
        b.setElevation(0);
        return b;
    }

    LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(15),dp(8),dp(15),dp(18));l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    LinearLayout card(){LinearLayout c=vertical();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(glass(panel(),28));c.setElevation(dp(5));return c;}
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
        if(showNav)shell.addView(nav(),new LinearLayout.LayoutParams(-1,dp(80)));
        setContentView(root);
    }

    View nav(){
        LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(9),dp(7),dp(9),dp(9));bar.setGravity(Gravity.CENTER);bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);bar.setBackground(glass(dark?Color.argb(98,255,255,255):Color.argb(220,255,255,255),26));bar.setElevation(dp(10));
        String[] labels={"בית","לוח שנה","התקדמות"};
        String[] icons={"⌂","▦","◒"};
        for(int i=0;i<3;i++){
            Button b=button(icons[i]+"\n"+labels[i]);b.setTextSize(11);final int s=i;b.setOnClickListener(v->{if(s==0)showHome();else if(s==1)showCalendar();else showStats();});
            bar.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));navButtons.add(b);
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
    JSONObject entry(String k){return days.optJSONObject(k);}
    JSONObject entry(java.util.Calendar d){return entry(key(d));}

    void saveDay(String k,boolean success,int score,String note){
        JSONObject o=new JSONObject();try{o.put("success",success).put("score",score).put("note",note==null?"":note);days.put(k,o);}catch(Exception ignored){}persist();
    }
    void removeDay(String k){days.remove(k);persist();}

    int successCount(){int n=0;Iterator<String>it=days.keys();while(it.hasNext()){JSONObject o=days.optJSONObject(it.next());if(o!=null&&o.optBoolean("success"))n++;}return n;}
    boolean same(java.util.Calendar a,java.util.Calendar b){return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);}
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
        LinearLayout t=card();t.addView(tv("היום 📅",11,ACCENT));
        TextView tg=tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(today.getTime()),19,text());tg.setTypeface(null,1);t.addView(tg);
        TextView th=tv(hebrewFull(today),22,text());th.setTypeface(null,1);t.addView(th);
        TextView small=tv(gregNumeric(today),10,muted());t.addView(small);
        String status=e==null?"עדיין לא עודכן  •  לחץ לעדכון":e.optBoolean("success")?"הצלחת היום ✅  •  ציון ⭐ "+e.optInt("score")+"/10":"לא הצלחת הפעם 🤍  •  ציון ⭐ "+e.optInt("score")+"/10";
        TextView st=tv(status,14,e==null?ACCENT:(e.optBoolean("success")?GREEN:RED));st.setPadding(0,dp(9),0,dp(9));t.addView(st);
        Button edit=button(e==null?"עדכן את היום":"ערוך את היום");edit.setBackground(glass(ACCENT,16));edit.setTextColor(Color.WHITE);edit.setOnClickListener(v->openEditor(today));t.addView(edit,new LinearLayout.LayoutParams(-1,dp(50)));content.addView(t);content.addView(space(12));

        LinearLayout quote=card();quote.setPadding(0,0,0,0);
        ImageView image=new ImageView(this);image.setImageResource(quoteImages[dayIndex()%quoteImages.length]);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        quote.addView(image,new LinearLayout.LayoutParams(-1,dp(205)));
        LinearLayout overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);overlay.setPadding(dp(18),dp(16),dp(18),dp(18));overlay.setBackgroundColor(Color.argb(105,0,0,0));
        overlay.addView(tv("✦ משפט היום",11,Color.WHITE));
        overlay.addView(tv(quotes[dayIndex()%quotes.length],20,Color.WHITE));
        overlay.addView(tv("חוזרים מחר לעוד יום. 🌿",11,Color.argb(225,255,255,255)));
        FrameLayout quoteFrame=new FrameLayout(this);quote.removeAllViews();quoteFrame.addView(image,new FrameLayout.LayoutParams(-1,dp(205)));quoteFrame.addView(overlay,new FrameLayout.LayoutParams(-1,dp(205),Gravity.BOTTOM));quote.addView(quoteFrame);
        content.addView(quote);
        TextView enc=tv(currentStreak()>0?"שמור על הקצב. עוד יום אחד יכול להפוך את הרצף להרגל. 🔥":"גם התחלה מחדש היא הצלחה. היום אפשר לבנות את היום הראשון. 🌱",13,muted());enc.setPadding(dp(5),dp(16),dp(5),dp(10));content.addView(enc);
    }

    void showCalendar(){
        screen=1;navButtons.clear();base("לוח שנה",true,false);
        TextView k=tv("עברית ↔ לועזי",10,muted());k.setTypeface(null,1);content.addView(k);
        LinearLayout monthCard=card();
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=button("‹"),next=button("›");
        LinearLayout monthText=new LinearLayout(this);monthText.setOrientation(LinearLayout.VERTICAL);monthText.setGravity(Gravity.CENTER);
        TextView hm=tv(hebrewMonth((java.util.Calendar)cursor.clone()),27,text());hm.setGravity(Gravity.CENTER);hm.setTypeface(null,1);
        TextView gm=tv(String.format(Locale.US,"%02d/%04d",cursor.get(Calendar.MONTH)+1,cursor.get(Calendar.YEAR)),11,muted());gm.setGravity(Gravity.CENTER);
        monthText.addView(hm,new LinearLayout.LayoutParams(-1,dp(42)));monthText.addView(gm,new LinearLayout.LayoutParams(-1,dp(22)));
        head.addView(prev,new LinearLayout.LayoutParams(dp(52),dp(52)));head.addView(monthText,new LinearLayout.LayoutParams(0,dp(74),1));head.addView(next,new LinearLayout.LayoutParams(dp(52),dp(52)));monthCard.addView(head);content.addView(monthCard);content.addView(space(10));

        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);grid.setPadding(dp(7),dp(10),dp(7),dp(10));grid.setBackground(glass(panel(),24));
        String[] week={"א","ב","ג","ד","ה","ו","ש"};
        for(String w:week){TextView x=tv(w,10,muted());x.setGravity(Gravity.CENTER);GridLayout.LayoutParams p=cell(30);grid.addView(x,p);}
        java.util.Calendar first=(java.util.Calendar)cursor.clone();first.set(Calendar.DAY_OF_MONTH,1);
        int offset=first.get(Calendar.DAY_OF_WEEK)-1,max=cursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<offset;i++)grid.addView(new Space(this),cell(64));
        for(int n=1;n<=max;n++){
            java.util.Calendar d=(java.util.Calendar)cursor.clone();d.set(Calendar.DAY_OF_MONTH,n);JSONObject en=entry(d);
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(2),dp(3),dp(2),dp(3));
            String hd=hebrewDay(d);
            TextView a=tv(hd,17,text());a.setGravity(Gravity.CENTER);a.setTypeface(null,1);c.addView(a,new LinearLayout.LayoutParams(-1,dp(30)));
            TextView b=tv(String.valueOf(n),10,muted());b.setGravity(Gravity.CENTER);c.addView(b,new LinearLayout.LayoutParams(-1,dp(20)));
            TextView dot=tv(en==null?"○":en.optBoolean("success")?"●":"●",10,en==null?muted():en.optBoolean("success")?GREEN:RED);dot.setGravity(Gravity.CENTER);c.addView(dot,new LinearLayout.LayoutParams(-1,dp(17)));
            boolean isToday=same(d,day(java.util.Calendar.getInstance()));
            c.setBackground(glass(isToday?Color.argb(48,111,102,244):Color.TRANSPARENT,14));
            c.setOnClickListener(v->openEditor(d));
            grid.addView(c,cell(72));
        }
        content.addView(grid);content.addView(space(7));
        TextView legend=tv("האותיות = תאריך עברי גדול  •  המספר = תאריך לועזי קטן\n✅ הצלחה   •   🔴 לא הצלחתי   •   ○ טרם עודכן",10,muted());legend.setGravity(Gravity.CENTER);content.addView(legend);
        prev.setOnClickListener(v->{cursor.add(Calendar.MONTH,-1);showCalendar();});next.setOnClickListener(v->{cursor.add(Calendar.MONTH,1);showCalendar();});
    }

    GridLayout.LayoutParams cell(int h){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(h);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}

    void showStats(){
        screen=2;navButtons.clear();base("התקדמות",true,false);
        section("המסע שלך","מה קורה כאן? 🚀");
        LinearLayout c=card();addBig(c,"רצף נוכחי 🔥",String.valueOf(currentStreak()));addBig(c,"שיא אישי 🏆",String.valueOf(bestStreak()));addBig(c,"הצלחות ✅",String.valueOf(successCount()));
        int count=0,total=0;Iterator<String>it=days.keys();while(it.hasNext()){JSONObject o=entry(it.next());if(o!=null){count++;total+=o.optInt("score");}}
        addBig(c,"ממוצע ציון ⭐",count==0?"—":String.format(Locale.US,"%.1f/10",total/(double)count));content.addView(c);content.addView(space(12));
        LinearLayout m=card();m.addView(tv("עידוד 💜",11,ACCENT));m.addView(tv(currentStreak()>0?"אתה בתוך רצף. אל תזלזל ביום אחד נוסף.":"גם התחלה מחדש היא הצלחה. היום אפשר להתחיל את היום הראשון.",20,text()));content.addView(m);
    }

    void addBig(LinearLayout c,String label,String value){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView v=tv(value,27,text());v.setTypeface(null,1);r.addView(v,new LinearLayout.LayoutParams(dp(135),dp(62)));TextView l=tv(label,13,muted());r.addView(l,new LinearLayout.LayoutParams(0,dp(62),1));c.addView(r);}

    void showSettings(){
        screen=3;navButtons.clear();base("הגדרות",false,true);
        section("התאמה אישית","המראה והתזכורות ⚙️");
        LinearLayout c=card();
        LinearLayout appearance=new LinearLayout(this);appearance.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=tv("מצב כהה 🌙\nעיצוב זכוכית ואווירה מודרנית",14,text());appearance.addView(a,new LinearLayout.LayoutParams(0,dp(64),1));
        Switch sw=new Switch(this);sw.setChecked(dark);sw.setOnCheckedChangeListener((b,checked)->{dark=checked;persist();showSettings();});appearance.addView(sw,new LinearLayout.LayoutParams(dp(64),dp(55)));c.addView(appearance);

        View line=space(1);line.setBackgroundColor(Color.argb(30,255,255,255));c.addView(line);
        LinearLayout remRow=new LinearLayout(this);remRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView rt=tv("תזכורת יומית 🔔\nתופיע גם כשהאפליקציה סגורה",14,text());remRow.addView(rt,new LinearLayout.LayoutParams(0,dp(70),1));
        Switch rs=new Switch(this);rs.setChecked(reminder);rs.setOnCheckedChangeListener((b,checked)->{reminder=checked;persist();if(checked){requestNotificationPermissionIfNeeded();scheduleReminder();}else cancelReminder();});remRow.addView(rs,new LinearLayout.LayoutParams(dp(64),dp(55)));c.addView(remRow);

        LinearLayout time=new LinearLayout(this);time.setGravity(Gravity.CENTER_VERTICAL);
        TextView tl=tv("שעה ⏰",13,muted());time.addView(tl,new LinearLayout.LayoutParams(0,dp(64),1));
        TimePicker picker=new TimePicker(this);picker.setIs24HourView(true);picker.setHour(reminderHour);picker.setMinute(reminderMinute);
        picker.setOnTimeChangedListener((v,h,m)->{reminderHour=h;reminderMinute=m;persist();if(reminder)scheduleReminder();});
        time.addView(picker,new LinearLayout.LayoutParams(dp(145),dp(75)));c.addView(time);

        TextView note=tv("טיפ: כדאי לבחור שעה קבועה שבה סביר שתהיה עם הטלפון. 🔔",11,muted());note.setPadding(0,dp(7),0,dp(8));c.addView(note);
        Button test=button("שלח בדיקת תזכורת עכשיו");test.setOnClickListener(v->sendTestNotification());c.addView(test,new LinearLayout.LayoutParams(-1,dp(50)));

        Button reset=button("איפוס כל הנתונים");reset.setTextColor(RED);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("איפוס מעקב").setMessage("כל הימים, הציונים וההערות יימחקו מהמכשיר.").setNegativeButton("ביטול",null).setPositiveButton("איפוס",(d,w)->{days=new JSONObject();persist();showHome();}).show());c.addView(reset,new LinearLayout.LayoutParams(-1,dp(50)));
        content.addView(c);content.addView(space(12));content.addView(tv("המעקב עובד מקומית. אין אתר חיצוני, אין Vercel, ואין צורך בחיבור קבוע לאינטרנט. ✅",12,muted()));
    }

    void requestNotificationPermissionIfNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);}

    void openEditor(java.util.Calendar d){
        selected=day(d);String k=key(selected);JSONObject e=entry(k);
        LinearLayout box=vertical();box.setPadding(dp(22),dp(16),dp(22),dp(16));box.setBackground(glass(panelStrong(),28));
        box.addView(tv(new SimpleDateFormat("EEEE, d בMMMM",new Locale("he","IL")).format(d.getTime()),22,text()));
        TextView heb=tv(hebrewFull(d),25,text());heb.setTypeface(null,1);box.addView(heb);
        box.addView(tv(gregNumeric(d),10,muted()));
        box.addView(space(7));
        final boolean[] chosen={e!=null};final boolean[] ok={e!=null&&e.optBoolean("success")};final int[] score={e!=null?e.optInt("score",10):10};
        LinearLayout ch=new LinearLayout(this);Button yes=button("הצלחתי ✅");Button no=button("לא הצלחתי 🤍");ch.addView(yes,new LinearLayout.LayoutParams(0,dp(52),1));ch.addView(no,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(ch);
        box.addView(tv("ציון ⭐ 1–10",11,muted()));
        LinearLayout rs=new LinearLayout(this);rs.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);for(int i=1;i<=10;i++){final int s=i;Button b=button(String.valueOf(i));b.setOnClickListener(v->score[0]=s);rs.addView(b,new LinearLayout.LayoutParams(0,dp(45),1));}box.addView(rs);
        EditText note=new EditText(this);note.setHint("הערה לעצמך (לא חובה)");note.setText(e==null?"":e.optString("note",""));note.setTextColor(text());note.setHintTextColor(muted());box.addView(note,new LinearLayout.LayoutParams(-1,dp(90)));
        Button save=button("שמירת היום");save.setTextColor(Color.WHITE);save.setBackground(glass(ACCENT,16));box.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));
        Button clear=button("ניקוי הסימון");box.addView(clear,new LinearLayout.LayoutParams(-1,dp(48)));
        AlertDialog dialog=new AlertDialog.Builder(this).setView(box).create();
        yes.setOnClickListener(v->{chosen[0]=true;ok[0]=true;yes.setBackground(glass(GREEN,15));no.setBackground(glass(panelStrong(),15));});
        no.setOnClickListener(v->{chosen[0]=true;ok[0]=false;no.setBackground(glass(RED,15));yes.setBackground(glass(panelStrong(),15));});
        save.setOnClickListener(v->{if(!chosen[0]){Toast.makeText(this,"בחר קודם אם הצלחת או לא",Toast.LENGTH_SHORT).show();return;}saveDay(k,ok[0],score[0],note.getText().toString());dialog.dismiss();if(screen==1)showCalendar();else if(screen==2)showStats();else showHome();});
        clear.setOnClickListener(v->{removeDay(k);dialog.dismiss();if(screen==1)showCalendar();else showHome();});
        dialog.getWindow();dialog.show();
    }

    void cancelReminder(){
        AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
        PendingIntent p=PendingIntent.getBroadcast(this,911,new Intent(this,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        a.cancel(p);
    }

    void scheduleReminder(){
        AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,ReminderReceiver.class);
        PendingIntent p=PendingIntent.getBroadcast(this,911,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        a.cancel(p);if(!reminder)return;
        java.util.Calendar next=java.util.Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,reminderHour);next.set(Calendar.MINUTE,reminderMinute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DATE,1);
        if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
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
            String json=p.getString(SETTINGS,"{}");boolean enabled=false;int h=20,m=30;
            try{JSONObject s=new JSONObject(json);enabled=s.optBoolean("reminder",false);h=s.optInt("hour",20);m=s.optInt("minute",30);}catch(Exception ignored){}
            if(!enabled)return;
            NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"תזכורות אתגר יומי",NotificationManager.IMPORTANCE_HIGH));
            Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,912,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
            b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("האתגר שלך מחכה לך 🔥").setContentText("גם היום אפשר לעשות צעד אחד קטן קדימה.").setContentIntent(pi).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);
            nm.notify(912,b.build());

            AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            Intent ni=new Intent(c,ReminderReceiver.class);
            PendingIntent np=PendingIntent.getBroadcast(c,911,ni,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            java.util.Calendar next=java.util.Calendar.getInstance();next.add(java.util.Calendar.DATE,1);next.set(java.util.Calendar.HOUR_OF_DAY,h);next.set(java.util.Calendar.MINUTE,m);next.set(java.util.Calendar.SECOND,0);next.set(java.util.Calendar.MILLISECOND,0);
            if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),np);else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),np);
        }
    }

    public static class BootReceiver extends BroadcastReceiver{
        @Override public void onReceive(Context c,Intent intent){
            if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
            android.content.SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            try{
                JSONObject s=new JSONObject(p.getString(SETTINGS,"{}"));
                if(!s.optBoolean("reminder",false))return;
                int h=s.optInt("hour",20),m=s.optInt("minute",30);
                AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
                Intent i=new Intent(c,ReminderReceiver.class);
                PendingIntent pi=PendingIntent.getBroadcast(c,911,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                java.util.Calendar next=java.util.Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,h);next.set(Calendar.MINUTE,m);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
                if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DATE,1);
                if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else a.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
            }catch(Exception ignored){}
        }
    }
}
