package com.termux.rtlclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.terminal.TerminalTransport;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local-only SSH terminal. No bootstrap, shared UID or access to another app's private files. */
public final class MainActivity extends Activity implements TerminalViewClient, TerminalSessionClient {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private String publicKey;
    private TerminalSession session;
    private TerminalView terminal;
    private TextView status;
    private SharedPreferences preferences;
    private int textSize;
    private boolean control;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("local-ssh", MODE_PRIVATE);
        textSize = preferences.getInt("text-size", Math.round(14 * getResources().getDisplayMetrics().scaledDensity));
        showSetup();
        worker.execute(() -> {
            try {
                String key = LocalSsh.ensureKey(getNoBackupFilesDir());
                runOnUiThread(() -> { if (!destroyed) { publicKey = key; status.setText("المفتاح جاهز. ابدأ بخطوة ١. / Key ready."); } });
            } catch (Exception e) {
                runOnUiThread(() -> { if (!destroyed) status.setText("تعذّر إنشاء مفتاح الاتصال: " + e.getMessage()); });
            }
        });
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 12, 16, 12);
        return layout;
    }

    private TextView text(LinearLayout layout, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(16);
        text.setPadding(0, 10, 0, 10);
        layout.addView(text);
        return text;
    }

    private Button button(LinearLayout layout, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        layout.addView(button);
        return button;
    }

    private void copy(String label, String value) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label, value));
    }

    private void showSetup() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = column();
        scroll.addView(layout);
        setContentView(scroll);
        text(layout, "Termux RTL · SSH\nنفس أدواتك وملفاتك داخل Termux الأصلي");
        text(layout, "اتصال محلي فقط: 127.0.0.1:8023\nلا تحذف Termux الأصلي. الأوامر والتعديلات هنا تؤثر في ملفاتك الأصلية فعلًا. لا تُنشأ بيئة جديدة.");
        status = text(layout, publicKey == null ? "جارٍ تجهيز مفتاح اتصال خاص بهذا التطبيق…" : "المفتاح جاهز.");
        button(layout, "١ · نسخ أمر الإعداد / Copy setup", () -> {
            if (publicKey == null) { status.setText("انتظر تجهيز المفتاح أولًا."); return; }
            copy("Termux RTL local SSH setup", LocalSsh.setupScript(publicKey));
            status.setText("الصق الأمر في Termux الأصلي وشغّله. يثبّت openssh ويهيئ خدمة خاصة على الهاتف فقط. سيطبع اسم المستخدم والبصمة؛ لا ترسل مفاتيحك الخاصة لأي شخص.");
        });
        text(layout, "٢ · بعد تنفيذ أمر الإعداد في الأصلي: أدخل اسم المستخدم وبصمة SHA256 المعروضة هناك. البصمة ليست كلمة مرور. تحقّق منها في التطبيق الأصلي، لا من رسالة خادم مجهول.");
        EditText user = new EditText(this);
        user.setSingleLine(true);
        user.setHint("Username — مثل u0_a123");
        user.setText(preferences.getString("username", ""));
        user.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(user);
        EditText pin = new EditText(this);
        pin.setSingleLine(true);
        pin.setHint("SHA256:…");
        pin.setText(preferences.getString("fingerprint", ""));
        pin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(pin);
        Button connect = button(layout, "٣ · اتصال / Connect", () -> {});
        connect.setOnClickListener(v -> {
            String username = user.getText().toString().trim();
            String fingerprint = pin.getText().toString().trim();
            if (publicKey == null || !username.matches("[A-Za-z0-9_][A-Za-z0-9_-]{0,63}") || !LocalSsh.validFingerprint(fingerprint)) {
                status.setText("أدخل اسم المستخدم وبصمة SHA256 كاملة كما ظهرت في Termux الأصلي.");
                return;
            }
            connect.setEnabled(false);
            status.setText("جارٍ الاتصال والتحقق من بصمة الخادم…");
            worker.execute(() -> {
                try {
                    TerminalTransport transport = LocalSsh.connect(getNoBackupFilesDir(), username, fingerprint);
                    runOnUiThread(() -> {
                        if (destroyed) { transport.close(); return; }
                        preferences.edit().putString("username", username).putString("fingerprint", fingerprint).apply();
                        showTerminal(transport);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (destroyed) return;
                        connect.setEnabled(true);
                        status.setText("فشل الاتصال: " + e.getMessage() + "\nتأكد من تشغيل الخدمة في الأصلي وصحة الاسم والبصمة. لا تتجاوز تحذير تغيّر مفتاح الخادم.");
                    });
                }
            });
        });
        text(layout, "بعد إعادة تشغيل الهاتف، شغّل في الأصلي:\nsshd -f ~/.ssh/termux-rtl-sshd_config -E ~/.ssh/termux-rtl-sshd.log\n\nهذه نسخة تجريبية لجلسة واحدة. استخدم tmux داخل الأصلي للأعمال الطويلة؛ قد ينقطع الاتصال عند إغلاق التطبيق أو قتله في الخلفية.");
    }

    private void showTerminal(TerminalTransport transport) {
        LinearLayout layout = column();
        layout.setPadding(0, 0, 0, 0);
        layout.setBackgroundColor(Color.BLACK);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(toolbar);
        Button ctrl = button(toolbar, "CTRL", () -> {});
        ctrl.setOnClickListener(v -> { control = !control; ctrl.setSelected(control); ctrl.setText(control ? "CTRL ✓" : "CTRL"); });
        button(toolbar, "ESC", () -> { if (session != null) session.writeCodePoint(false, 27); });
        button(toolbar, "TAB", () -> { if (session != null) session.writeCodePoint(false, 9); });
        button(toolbar, "⋮", this::showActions);
        terminal = new TerminalView(this, null);
        terminal.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        terminal.setTerminalViewClient(this);
        terminal.setTextSize(textSize);
        terminal.setRtlEnabled(preferences.getBoolean("rtl", true));
        layout.addView(terminal, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        session = new TerminalSession(transport, 10000, this);
        terminal.attachSession(session);
        terminal.requestFocus();
    }

    private void showActions() {
        new AlertDialog.Builder(this).setItems(new String[]{"لصق / Paste", "تبديل RTL", "↑", "↓", "←", "→", "قطع الاتصال / Disconnect"}, (d, which) -> {
            if (session == null) return;
            if (which == 0) onPasteTextFromClipboard(session);
            else if (which == 1) {
                terminal.setRtlEnabled(!terminal.isRtlEnabled());
                preferences.edit().putBoolean("rtl", terminal.isRtlEnabled()).apply();
            } else if (which < 6) {
                String arrow = new String[]{"A", "B", "D", "C"}[which - 2];
                session.write(("\033[" + arrow).getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, 3);
            } else disconnectDialog();
        }).show();
    }

    private void disconnectDialog() {
        new AlertDialog.Builder(this).setMessage("قطع الاتصال قد ينهي أوامر هذه الجلسة غير المحمية بـ tmux. هل تتابع؟")
            .setNegativeButton("إلغاء", null).setPositiveButton("قطع الاتصال", (d, w) -> {
                if (session != null) session.finishIfRunning();
                session = null;
                terminal = null;
                control = false;
                showSetup();
            }).show();
    }

    @Override public void onBackPressed() { if (session != null) disconnectDialog(); else super.onBackPressed(); }
    @Override protected void onDestroy() {
        destroyed = true;
        if (session != null) session.finishIfRunning();
        worker.shutdownNow();
        super.onDestroy();
    }

    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            textSize = Math.max(12, Math.min(72, textSize + (scale > 1 ? 2 : -2)));
            terminal.setTextSize(textSize);
            preferences.edit().putInt("text-size", textSize).apply();
            return 1;
        }
        return scale;
    }
    public void onSingleTapUp(MotionEvent event) {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(terminal, InputMethodManager.SHOW_IMPLICIT);
    }
    public boolean shouldBackButtonBeMappedToEscape() { return false; }
    public boolean shouldEnforceCharBasedInput() { return false; }
    public boolean shouldUseCtrlSpaceWorkaround() { return false; }
    public boolean isTerminalViewSelected() { return true; }
    public void copyModeChanged(boolean selected) {}
    public boolean onKeyDown(int code, KeyEvent event, TerminalSession current) { return false; }
    public boolean onKeyUp(int code, KeyEvent event) { return false; }
    public boolean onLongPress(MotionEvent event) { return false; }
    public boolean readControlKey() { return control; }
    public boolean readAltKey() { return false; }
    public boolean readShiftKey() { return false; }
    public boolean readFnKey() { return false; }
    public boolean onCodePoint(int code, boolean ctrl, TerminalSession current) { return false; }
    public void onEmulatorSet() {}
    public void onTextChanged(TerminalSession changed) { if (changed == session && terminal != null) terminal.onScreenUpdated(); }
    public void onTitleChanged(TerminalSession changed) {}
    public void onSessionFinished(TerminalSession finished) {
        if (finished == session && !destroyed) new AlertDialog.Builder(this).setMessage("انتهى الاتصال. يمكنك إعادة الاتصال من شاشة الإعداد.")
            .setPositiveButton("الإعداد", (d, w) -> { session = null; terminal = null; showSetup(); }).show();
    }
    public void onCopyTextToClipboard(TerminalSession current, String value) {
        if (!destroyed && current == session) new AlertDialog.Builder(this).setMessage("نسخ النص المحدد / نص الطرفية إلى الحافظة؟")
            .setNegativeButton("إلغاء", null).setPositiveButton("نسخ", (d, w) -> copy("Terminal text", value)).show();
    }
    public void onPasteTextFromClipboard(TerminalSession current) {
        if (destroyed || current != session) return;
        new AlertDialog.Builder(this).setMessage("لصق الحافظة إلى الطرفية الأصلية؟ قد تحتوي أوامر قابلة للتنفيذ.")
            .setNegativeButton("إلغاء", null).setPositiveButton("لصق", (d, w) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (current != session || !current.isRunning() || !clipboard.hasPrimaryClip()) return;
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0 && current.getEmulator() != null)
                    current.getEmulator().paste(clip.getItemAt(0).coerceToText(this).toString());
            }).show();
    }
    public void onBell(TerminalSession current) {}
    public void onColorsChanged(TerminalSession current) { onTextChanged(current); }
    public void onTerminalCursorStateChange(boolean state) { if (terminal != null) terminal.invalidate(); }
    public void setTerminalShellPid(TerminalSession current, int pid) {}
    public Integer getTerminalCursorStyle() { return TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK; }
    public void logError(String tag, String message) { Log.e(tag, message); }
    public void logWarn(String tag, String message) { Log.w(tag, message); }
    public void logInfo(String tag, String message) { Log.i(tag, message); }
    public void logDebug(String tag, String message) { Log.d(tag, message); }
    public void logVerbose(String tag, String message) { Log.v(tag, message); }
    public void logStackTraceWithMessage(String tag, String message, Exception e) { Log.e(tag, message, e); }
    public void logStackTrace(String tag, Exception e) { Log.e(tag, "Terminal error", e); }
}
