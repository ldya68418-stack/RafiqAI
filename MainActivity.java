package com.rafiqai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
  private final int bg=Color.rgb(15,18,28), panel=Color.rgb(27,32,46), accent=Color.rgb(117,96,245), white=Color.WHITE, muted=Color.rgb(190,196,210);
  private LinearLayout messages; private EditText input; private ScrollView scroll; private TextView status; private SharedPreferences prefs;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private JSONArray history=new JSONArray();
  @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("rafiq",MODE_PRIVATE); buildUi(); addMessage("assistant","أهلًا! أنا رفيق AI. أستخدم الآن واجهة g4f العامة للتجربة دون مفتاح API. قد تتوقف الخدمة أو تفرض حدودًا لأنها خدمة عامة.");}
  private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
  private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
  private void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);root.setPadding(14,10,14,10);setContentView(root);
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(4,8,4,12);
    TextView title=text("رفيق AI",25,white);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    Button settings=new Button(this);settings.setText("حول الخدمة ℹ");settings.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("الخدمة المجانية عبر g4f").setMessage("لا يحتاج التطبيق إلى مفتاح API. يتصل بواجهة g4f العامة عبر الإنترنت. هذه خدمة طرف ثالث قد تكون مزدحمة أو تتغير أو تتوقف، ولا يوجد ضمان للاستخدام المجاني غير المحدود. لا ترسل معلومات حساسة.").setPositiveButton("حسنًا",null).show());header.addView(settings);root.addView(header);
    status=text("جاهز للمحادثة",12,muted);root.addView(status);
    scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);sp.setMargins(0,8,0,8);root.addView(scroll,sp);
    messages=new LinearLayout(this);messages.setOrientation(1);messages.setPadding(2,8,2,8);scroll.addView(messages);
    LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(8,6,8,6);bar.setBackground(shape(panel,24));
    input=new EditText(this);input.setTextColor(white);input.setHintTextColor(muted);input.setHint("اكتب رسالتك هنا...");input.setTextSize(16);input.setSingleLine(false);input.setMinLines(1);input.setMaxLines(4);input.setBackgroundColor(Color.TRANSPARENT);bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));
    Button send=new Button(this);send.setText("إرسال");send.setOnClickListener(v->send());bar.addView(send);root.addView(bar);
    TextView note=text("عبر الإنترنت • g4f عام • قد توجد حدود أو انقطاعات",11,muted);note.setGravity(Gravity.CENTER);note.setPadding(0,8,0,0);root.addView(note);
  }
  private void addMessage(String role,String body){TextView bubble=text((role.equals("user")?"أنت\n":"رفيق AI\n")+body,16,white);bubble.setTextIsSelectable(true);bubble.setPadding(14,11,14,11);bubble.setBackground(shape(role.equals("user")?Color.rgb(61,54,115):panel,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(role.equals("user")?42:0,5,role.equals("user")?0:42,7);messages.addView(bubble,p);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
  private void showKeyDialog(){new AlertDialog.Builder(this).setTitle("g4f المجاني").setMessage("لا يلزم مفتاح API. تعتمد الخدمة على مزود عام تابع لطرف ثالث، وقد لا تعمل دائمًا أو قد تفرض حدودًا.").setPositiveButton("حسنًا",null).show();}
  private void send(){String msg=input.getText().toString().trim();if(msg.isEmpty())return;input.setText("");((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);addMessage("user",msg);try{history.put(new JSONObject().put("role","user").put("content",msg));}catch(Exception ignored){}status.setText("رفيق يفكر عبر g4f...");worker.execute(()->callG4f());}
  private void callG4f(){HttpURLConnection c=null;try{URL u=new URL("https://g4f.space/api/auto/chat/completions");c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("Accept","application/json");JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","أنت رفيق AI، مساعد مفيد يجيب باللغة العربية ما لم يطلب المستخدم لغة أخرى."));for(int i=0;i<history.length();i++){JSONObject old=history.getJSONObject(i);String role=old.optString("role","user");if(role.equals("model"))role="assistant";msgs.put(new JSONObject().put("role",role).put("content",old.optString("content","")));}JSONObject payload=new JSONObject().put("model","gpt-4o-mini").put("messages",msgs).put("temperature",0.7).put("stream",false);try(OutputStream os=c.getOutputStream()){os.write(payload.toString().getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))!=-1)out.write(buf,0,n);String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);JSONObject result=new JSONObject(raw);if(code<200||code>=300){throw new Exception("HTTP "+code+": "+raw.substring(0,Math.min(350,raw.length())));}JSONArray choices=result.optJSONArray("choices");if(choices==null||choices.length()==0)throw new Exception("الخدمة لم تُرجع ردًا");JSONObject message=choices.getJSONObject(0).optJSONObject("message");String answer=message==null?"لم يصل نص في الرد":message.optString("content","");if(answer.isEmpty())answer="ردّ فارغ من مزوّد الخدمة.";history.put(new JSONObject().put("role","assistant").put("content",answer));final String finalAnswer=answer;runOnUiThread(()->{addMessage("assistant",finalAnswer);status.setText("جاهز للمحادثة");});}catch(Exception e){runOnUiThread(()->{addMessage("assistant","تعذر الاتصال بخدمة g4f: "+e.getMessage()+"\nجرّب لاحقًا؛ قد تكون الخدمة العامة متوقفة أو وصلت إلى حدودها.");status.setText("تعذر الاتصال بـ g4f");});}finally{if(c!=null)c.disconnect();}}
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
