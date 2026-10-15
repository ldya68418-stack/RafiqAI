package com.rafiqai.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(15, 18, 28);
    private final int panel = Color.rgb(27, 32, 46);
    private final int accent = Color.rgb(79, 70, 229);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(190, 196, 210);

    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private TextView status;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final JSONArray history = new JSONArray();

    private final String API_KEY = "sam_5d3f808dcec51df5257addf7b75a5f4c4a20bff11506361298b5971de27ab459";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        addTextMessage("assistant", "أهلاً بك! أنا علي AI، مساعدك الذكي المتكامل.\n• أستطيع الإجابة عن أي سؤال.\n• لتوليد الصور: اكتب (صورة: كذا أو ارسم: كذا)\n• لتوليد الفيديو: اكتب (فيديو: كذا)");
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(16, 12, 16, 12);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(4, 8, 4, 12);

        TextView title = text("علي AI ⚡", 24, white);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button infoBtn = new Button(this);
        infoBtn.setText("الميزات ✨");
        infoBtn.setBackground(shape(panel, 14));
        infoBtn.setTextColor(white);
        infoBtn.setOnClickListener(v -> Toast.makeText(this, "يدعم توليد النصوص، الصور، والفيديوهات بالذكاء الاصطناعي", Toast.LENGTH_LONG).show());
        header.addView(infoBtn);
        root.addView(header);

        status = text("جاهز لخدمتك", 12, muted);
        root.addView(status);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1);
        sp.setMargins(0, 8, 0, 8);
        root.addView(scroll, sp);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(2, 8, 2, 8);
        scroll.addView(messages);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(10, 8, 10, 8);
        bar.setBackground(shape(panel, 26));

        input = new EditText(this);
        input.setTextColor(white);
        input.setHintTextColor(muted);
        input.setHint("اكتب سؤالاً أو (ارسم/فيديو)...");
        input.setTextSize(15);
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setBackgroundColor(Color.TRANSPARENT);
        bar.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(white);
        send.setBackground(shape(accent, 20));
        send.setOnClickListener(v -> handleSend());
        bar.addView(send);
        root.addView(bar);
    }

    private void addTextMessage(String role, String body) {
        TextView bubble = text((role.equals("user") ? "أنت:\n" : "علي AI:\n") + body, 15, white);
        bubble.setTextIsSelectable(true);
        bubble.setPadding(14, 11, 14, 11);
        bubble.setBackground(shape(role.equals("user") ? Color.rgb(61, 54, 115) : panel, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(role.equals("user") ? 40 : 0, 6, role.equals("user") ? 0 : 40, 6);
        messages.addView(bubble, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addMediaView(View mediaView, String caption) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(12, 12, 12, 12);
        container.setBackground(shape(panel, 18));

        TextView cap = text("علي AI: " + caption, 14, white);
        cap.setPadding(0, 0, 0, 8);
        container.addView(cap);
        container.addView(mediaView);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 6, 40, 6);
        messages.addView(container, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void handleSend() {
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;
        input.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);

        addTextMessage("user", msg);

        String lower = msg.toLowerCase();
        if (lower.startsWith("صورة") || lower.startsWith("ارسم") || lower.startsWith("صوره") || lower.startsWith("image")) {
            status.setText("علي يقوم بتوليد الصورة الآن...");
            worker.execute(() -> generateImage(msg));
        } else if (lower.startsWith("فيديو") || lower.startsWith("video")) {
            status.setText("علي يقوم بتجهيز الفيديو الآن...");
            worker.execute(() -> generateVideo(msg));
        } else {
            status.setText("علي يفكر...");
            worker.execute(() -> chatWithAi(msg));
        }
    }

    private void generateImage(String prompt) {
        try {
            String cleanPrompt = prompt.replaceFirst("^(صورة|ارسم|صوره|image)[:\\s]+", "").trim();
            if (cleanPrompt.isEmpty()) cleanPrompt = prompt;
            String encoded = URLEncoder.encode(cleanPrompt, "UTF-8");
            String imageUrl = "https://image.pollinations.ai/prompt/" + encoded + "?width=768&height=768&nologo=true";

            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            InputStream is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);

            runOnUiThread(() -> {
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(bmp);
                iv.setAdjustViewBounds(true);
                addMediaView(iv, "تم إنشاء الصورة بنجاح! 🎨");
                status.setText("جاهز لخدمتك");
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                addTextMessage("assistant", "تعذر توليد الصورة: " + e.getMessage());
                status.setText("حدث خطأ");
            });
        }
    }

    private void generateVideo(String prompt) {
        try {
            String cleanPrompt = prompt.replaceFirst("^(فيديو|video)[:\\s]+", "").trim();
            if (cleanPrompt.isEmpty()) cleanPrompt = prompt;
            String encoded = URLEncoder.encode(cleanPrompt, "UTF-8");
            String videoUrl = "https://gen.pollinations.ai/video/" + encoded;

            runOnUiThread(() -> {
                VideoView vv = new VideoView(this);
                vv.setVideoURI(Uri.parse(videoUrl));
                MediaController mc = new MediaController(this);
                mc.setAnchorView(vv);
                vv.setMediaController(mc);
                vv.setLayoutParams(new LinearLayout.LayoutParams(-1, 550));
                vv.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    vv.start();
                });
                addMediaView(vv, "تم تجهيز الفيديو بنجاح! 🎬");
                status.setText("جاهز لخدمتك");
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                addTextMessage("assistant", "تعذر تجهيز الفيديو: " + e.getMessage());
                status.setText("حدث خطأ");
            });
        }
    }

    private void chatWithAi(String userMsg) {
        HttpURLConnection c = null;
        try {
            history.put(new JSONObject().put("role", "user").put("content", userMsg));

            URL u = new URL("https://gen.pollinations.ai/v1/chat/completions");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(25000);
            c.setReadTimeout(60000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setRequestProperty("Authorization", "Bearer " + API_KEY);

            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "system").put("content", "أنت علي AI، مساعد ذكي ومحترف، لطيف ومتعاون جداً، تتحدث باللغة العربية وتساعد في كل شيء بما في ذلك إنشاء النصوص والبرمجة وحل المشكلات."));

            for (int i = 0; i < history.length(); i++) {
                msgs.put(history.getJSONObject(i));
            }

            JSONObject payload = new JSONObject();
            payload.put("model", "openai");
            payload.put("messages", msgs);
            payload.put("temperature", 0.7);

            try (OutputStream os = c.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = c.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + ": " + sb.toString());
            }

            JSONObject result = new JSONObject(sb.toString());
            JSONArray choices = result.optJSONArray("choices");
            String reply = choices.getJSONObject(0).getJSONObject("message").optString("content", "لم يصل محتوى.");

            history.put(new JSONObject().put("role", "assistant").put("content", reply));

            runOnUiThread(() -> {
                addTextMessage("assistant", reply);
                status.setText("جاهز لخدمتك");
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                addTextMessage("assistant", "مرحباً! أواجه مشكلة في الرد حالياً: " + e.getMessage());
                status.setText("تعذر الرد");
            });
        } finally {
            if (c != null) c.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
