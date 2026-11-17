package com.amir.codmultimate;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.View;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class MainActivity extends Activity {
    private TextView status, network, result, note;
    private Button testButton;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String profile = "تشخیص خودکار";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.rgb(13,18,30));
        getWindow().setNavigationBarColor(Color.rgb(13,18,30));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 30, 28, 24);
        root.setBackgroundColor(Color.rgb(13,18,30));

        TextView title = text("AMIR CODM ULTIMATE", 25, Color.WHITE, true);
        root.addView(title);
        root.addView(text("Network diagnostics • Poco X6 Pro", 13, Color.LTGRAY, false));

        status = text("آماده تست شبکه", 18, Color.rgb(80,220,150), true);
        status.setPadding(0,28,0,12);
        root.addView(status);

        network = text("شبکه: در حال بررسی…", 15, Color.WHITE, false);
        root.addView(network);

        TextView profileTitle = text("پروفایل اینترنت", 16, Color.WHITE, true);
        profileTitle.setPadding(0,24,0,8);
        root.addView(profileTitle);

        Spinner spinner = new Spinner(this);
        String[] profiles = {"تشخیص خودکار", "همراه اول", "مخابرات"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { profile = profiles[pos]; }
        });
        root.addView(spinner);

        testButton = new Button(this);
        testButton.setText("شروع تست شبکه");
        testButton.setAllCaps(false);
        testButton.setTextSize(16);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0,24,0,12);
        root.addView(testButton, bp);

        result = text("نتیجه تست اینجا نمایش داده می‌شود.", 15, Color.WHITE, false);
        result.setPadding(0,20,0,12);
        root.addView(result);

        note = text("توجه: این برنامه پینگ واقعی سرورهای بازی را اندازه‌گیری نمی‌کند. تست TCP و DNS فقط وضعیت مسیر اینترنت تا مقصدهای آزمایشی را نشان می‌دهد. پروفایل‌ها تنظیمات اپراتور را تغییر نمی‌دهند؛ IP اصلی حفظ می‌شود و VPN یا تونل WireGuard ساخته نمی‌شود.", 13, Color.LTGRAY, false);
        note.setPadding(0,22,0,0);
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        updateNetwork();
        testButton.setOnClickListener(v -> runTests());
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void updateNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities c = cm.getNetworkCapabilities(n);
            String type = "نامشخص";
            if (c != null) {
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi‑Fi";
                else if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "دیتای موبایل";
                else if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet";
            }
            network.setText("شبکه فعلی: " + type + "\nپروفایل انتخابی: " + profile);
        } catch (Exception e) { network.setText("شبکه فعلی: قابل تشخیص نیست"); }
    }

    private void runTests() {
        testButton.setEnabled(false);
        status.setText("در حال تست…");
        result.setText("در حال بررسی DNS و اتصال TCP…");
        updateNetwork();
        executor.execute(() -> {
            StringBuilder out = new StringBuilder();
            String[] hosts = {"www.google.com", "www.youtube.com", "www.activision.com"};
            int tcpOk = 0, tcpFail = 0;
            for (String host : hosts) {
                long dnsStart = System.nanoTime();
                String ip = "نامشخص";
                long dnsMs = -1;
                try {
                    InetAddress[] addresses = InetAddress.getAllByName(host);
                    dnsMs = (System.nanoTime() - dnsStart) / 1_000_000;
                    if (addresses.length > 0) ip = addresses[0].getHostAddress();
                } catch (Exception e) { dnsMs = -1; }

                long tcpStart = System.nanoTime();
                long tcpMs = -1;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, 443), 3500);
                    tcpMs = (System.nanoTime() - tcpStart) / 1_000_000;
                    tcpOk++;
                } catch (Exception e) { tcpFail++; }

                out.append(host).append("\n");
                out.append("DNS: ").append(dnsMs >= 0 ? dnsMs + " ms" : "ناموفق").append(" | IP: ").append(ip).append("\n");
                out.append("TCP اتصال 443: ").append(tcpMs >= 0 ? tcpMs + " ms" : "ناموفق").append("\n\n");
            }
            final String summary = "نتیجه تست برای " + profile + "\n"
                + "اتصال‌های موفق TCP: " + tcpOk + " از 3\n"
                + "اتصال‌های ناموفق: " + tcpFail + " از 3\n\n" + out.toString()
                + "این اعداد پینگ COD Mobile یا Packet Loss بازی نیستند.";
            runOnUiThread(() -> {
                result.setText(summary);
                status.setText(tcpFail == 0 ? "تست تمام شد" : "تست تمام شد؛ چند اتصال ناموفق بود");
                status.setTextColor(tcpFail == 0 ? Color.rgb(80,220,150) : Color.rgb(255,190,80));
                testButton.setEnabled(true);
                updateNetwork();
            });
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
