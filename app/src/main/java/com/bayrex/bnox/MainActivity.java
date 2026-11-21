package com.bayrex.bnox;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;
import android.os.UserHandle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQUEST_PROVISION = 410;
    private static final String PREFS = "bnox_private";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_FOLDERS = "folders";
    private static final String KEY_FILTER = "active_folder";

    private int bg = Color.rgb(15, 17, 27);
    private int panel = Color.rgb(27, 30, 44);
    private int accent = Color.rgb(143, 104, 255);
    private LinearLayout root;
    private String activeFolder = "Все";
    private final List<AppEntry> apps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null || !dpm.isProfileOwnerApp(getPackageName())) {
            showProvisionScreen();
            return;
        }

        activeFolder = getPreferences().getString(KEY_FILTER, "Все");
        if (getPreferences().contains(KEY_HASH)) {
            showPinScreen(false);
        } else {
            showPinScreen(true);
        }
    }

    private android.content.SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void showProvisionScreen() {
        root = baseLayout();
        addText(root, "B-Nox", 32, Color.WHITE, true);
        addText(root, "Защищённая папка для Android", 16, 0xFFB6B8C8, false);
        addSpace(root, 28);
        addText(root, "Для работы B-Nox нужен отдельный системный рабочий профиль Android. Он создаётся только с подтверждением на экране системы.", 16, Color.WHITE, false);
        addSpace(root, 18);
        Button setup = makeButton("Создать защищённый профиль");
        root.addView(setup);
        setup.setOnClickListener(v -> beginProvisioning());
        addSpace(root, 12);
        addText(root, "Если производитель телефона отключил создание рабочих профилей, B-Nox сообщит об этом. Подменять системную изоляцию обычной виртуализацией приложение не будет.", 13, 0xFF9296AA, false);
        setContentView(root);
    }

    private void beginProvisioning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, "Для B-Nox требуется Android 7.0 или новее.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                new ComponentName(this, AdminReceiver.class));
        try {
            startActivityForResult(intent, REQUEST_PROVISION);
        } catch (Exception e) {
            Toast.makeText(this, "На этом устройстве системная настройка рабочего профиля недоступна.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROVISION) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Профиль создаётся. Откройте B-Nox с иконки рабочего профиля.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Создание профиля отменено или не поддерживается устройством.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showPinScreen(boolean create) {
        root = baseLayout();
        addSpace(root, 72);
        TextView logo = addText(root, "B-Nox", 36, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        TextView subtitle = addText(root, create ? "Создайте пароль папки" : "Введите пароль", 17, 0xFFB6B8C8, false);
        subtitle.setGravity(Gravity.CENTER);
        addSpace(root, 26);

        EditText pin = new EditText(this);
        pin.setSingleLine(true);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setHint(create ? "Новый PIN-код (от 4 цифр)" : "PIN-код");
        pin.setHintTextColor(0xFF85899B);
        pin.setTextColor(Color.WHITE);
        pin.setTextSize(18);
        pin.setPadding(18, 14, 18, 14);
        pin.setBackground(round(panel, 18));
        root.addView(pin, matchWrap());
        addSpace(root, 14);

        EditText confirm = null;
        if (create) {
            confirm = new EditText(this);
            confirm.setSingleLine(true);
            confirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            confirm.setHint("Повторите PIN-код");
            confirm.setHintTextColor(0xFF85899B);
            confirm.setTextColor(Color.WHITE);
            confirm.setTextSize(18);
            confirm.setPadding(18, 14, 18, 14);
            confirm.setBackground(round(panel, 18));
            root.addView(confirm, matchWrap());
            addSpace(root, 14);
        }

        final EditText confirmField = confirm;
        Button unlock = makeButton(create ? "Сохранить и открыть" : "Открыть B-Nox");
        root.addView(unlock);
        unlock.setOnClickListener(v -> {
            String value = pin.getText().toString();
            if (create) {
                if (value.length() < 4) {
                    pin.setError("Минимум 4 цифры");
                    return;
                }
                if (!value.equals(confirmField.getText().toString())) {
                    confirmField.setError("PIN-коды не совпадают");
                    return;
                }
                savePin(value);
                showHome();
            } else if (verifyPin(value)) {
                showHome();
            } else {
                pin.setError("Неверный PIN-код");
            }
        });
        setContentView(root);
    }

    private void savePin(String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = derive(pin, salt);
            getPreferences().edit()
                    .putString(KEY_SALT, toHex(salt))
                    .putString(KEY_HASH, toHex(hash))
                    .apply();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сохранить пароль.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean verifyPin(String pin) {
        try {
            String saltText = getPreferences().getString(KEY_SALT, "");
            String hashText = getPreferences().getString(KEY_HASH, "");
            if (saltText.isEmpty() || hashText.isEmpty()) return false;
            byte[] actual = derive(pin, fromHex(saltText));
            return MessageDigest.isEqual(actual, fromHex(hashText));
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] derive(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
        return out.toString();
    }

    private byte[] fromHex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private void showHome() {
        root = baseLayout();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("B-Nox");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button lock = makeSmallButton("Заблокировать");
        header.addView(lock);
        lock.setOnClickListener(v -> showPinScreen(false));
        root.addView(header);
        addText(root, "Защищённая папка", 14, 0xFF9EA2B6, false);
        addSpace(root, 18);

        LinearLayout folderHeader = new LinearLayout(this);
        folderHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView foldersLabel = addText(folderHeader, "Папки", 18, Color.WHITE, true);
        folderHeader.addView(foldersLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Button addFolder = makeSmallButton("+ Папка");
        folderHeader.addView(addFolder);
        root.addView(folderHeader);
        addFolder.setOnClickListener(v -> addFolderDialog());
        addSpace(root, 10);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, 0, 8);
        addFolderChip(chips, "Все");
        Set<String> folders = getPreferences().getStringSet(KEY_FOLDERS, new HashSet<>());
        List<String> sorted = new ArrayList<>(folders);
        Collections.sort(sorted);
        for (String folder : sorted) addFolderChip(chips, folder);
        ScrollView chipScroll = new ScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(chips);
        root.addView(chipScroll, new LinearLayout.LayoutParams(-1, dp(54)));

        addSpace(root, 8);
        Button addApps = makeButton("+ Добавить приложения");
        root.addView(addApps);
        addApps.setOnClickListener(v -> showAvailableApps());
        addSpace(root, 14);
        TextView section = addText(root, "Приложения", 18, Color.WHITE, true);
        addSpace(root, 8);

        ScrollView listScroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(list);
        root.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        loadApps();
        int shown = 0;
        for (AppEntry app : apps) {
            String folder = getPreferences().getString("app_folder_" + app.packageName, "");
            if (!"Все".equals(activeFolder) && !activeFolder.equals(folder)) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(round(panel, 16));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(68));
            cardParams.bottomMargin = dp(8);
            list.addView(card, cardParams);

            TextView icon = new TextView(this);
            icon.setText("▣");
            icon.setTextSize(24);
            icon.setTextColor(accent);
            card.addView(icon, new LinearLayout.LayoutParams(dp(42), -2));
            LinearLayout textBox = new LinearLayout(this);
            textBox.setOrientation(LinearLayout.VERTICAL);
            TextView name = addText(textBox, app.label, 15, Color.WHITE, true);
            String sub = folder.isEmpty() ? "Без папки" : folder;
            addText(textBox, sub, 12, 0xFF9EA2B6, false);
            card.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));
            card.setOnClickListener(v -> launchApp(app));
            card.setOnLongClickListener(v -> {
                chooseFolder(app);
                return true;
            });
            shown++;
        }
        if (shown == 0) {
            TextView empty = addText(list, apps.isEmpty()
                    ? "В рабочем профиле пока нет приложений. Установите приложения именно в профиль B-Nox."
                    : "В этой папке пока нет приложений. Нажмите и удерживайте приложение, чтобы переместить его.",
                    14, 0xFF9EA2B6, false);
            empty.setPadding(dp(8), dp(24), dp(8), dp(24));
        }
        addText(root, "Нажмите для запуска • удерживайте для выбора папки", 12, 0xFF777D93, false);
        setContentView(root);
    }

    private void showAvailableApps() {
        DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (dpm == null || launcherApps == null) {
            Toast.makeText(this, "Список приложений недоступен.", Toast.LENGTH_LONG).show();
            return;
        }

        List<LauncherActivityInfo> available = new ArrayList<>();
        UserHandle current = Process.myUserHandle();
        try {
            for (UserHandle profile : launcherApps.getProfiles()) {
                if (profile.equals(current)) continue;
                available.addAll(launcherApps.getActivityList(null, profile));
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Android не разрешил просмотреть приложения другого профиля.", Toast.LENGTH_LONG).show();
            return;
        }

        final java.util.LinkedHashMap<String, String> unique = new java.util.LinkedHashMap<>();
        for (LauncherActivityInfo item : available) {
            String pkg = item.getApplicationInfo().packageName;
            if (!pkg.equals(getPackageName())) unique.put(pkg, item.getLabel().toString());
        }
        if (unique.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Добавить приложения")
                    .setMessage("В личном профиле не найдено приложений, которые можно добавить. Сначала установите нужное приложение обычным способом.")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }

        List<String> packages = new ArrayList<>(unique.keySet());
        List<String> labels = new ArrayList<>(unique.values());
        boolean[] checked = new boolean[packages.size()];
        String[] names = labels.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Добавить в B-Nox")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    int added = 0;
                    for (int i = 0; i < packages.size(); i++) {
                        if (!checked[i]) continue;
                        try {
                            if (dpm.installExistingPackage(
                                    new ComponentName(this, AdminReceiver.class), packages.get(i))) {
                                added++;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    Toast.makeText(this, "Добавлено приложений: " + added, Toast.LENGTH_LONG).show();
                    showHome();
                }).show();
    }

    private void loadApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : results) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = info.loadLabel(pm).toString();
            apps.add(new AppEntry(label, pkg, info.activityInfo.name));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void launchApp(AppEntry app) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launch == null) {
            Toast.makeText(this, "Не удалось открыть приложение.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Приложение недоступно в профиле B-Nox.", Toast.LENGTH_LONG).show();
        }
    }

    private void addFolderChip(LinearLayout parent, String name) {
        Button chip = makeSmallButton(name);
        chip.setTextColor(name.equals(activeFolder) ? Color.WHITE : 0xFFB9BBD0);
        chip.setBackground(round(name.equals(activeFolder) ? accent : panel, 22));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(42));
        p.rightMargin = dp(8);
        parent.addView(chip, p);
        chip.setOnClickListener(v -> {
            activeFolder = name;
            getPreferences().edit().putString(KEY_FILTER, name).apply();
            showHome();
        });
    }

    private void addFolderDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Название папки");
        new android.app.AlertDialog.Builder(this)
                .setTitle("Новая папка")
                .setView(input)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty() || name.equals("Все")) return;
                    Set<String> folders = new HashSet<>(getPreferences().getStringSet(KEY_FOLDERS, new HashSet<>()));
                    folders.add(name);
                    getPreferences().edit().putStringSet(KEY_FOLDERS, folders).apply();
                    activeFolder = name;
                    showHome();
                }).show();
    }

    private void chooseFolder(AppEntry app) {
        Set<String> folders = getPreferences().getStringSet(KEY_FOLDERS, new HashSet<>());
        List<String> options = new ArrayList<>();
        options.add("Без папки");
        options.addAll(folders);
        String[] values = options.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Папка для " + app.label)
                .setItems(values, (dialog, which) -> {
                    String value = values[which];
                    getPreferences().edit()
                            .putString("app_folder_" + app.packageName, value.equals("Без папки") ? "" : value)
                            .apply();
                    showHome();
                }).show();
    }

    private LinearLayout baseLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(24), dp(22), dp(16));
        layout.setBackgroundColor(bg);
        return layout;
    }

    private TextView addText(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(view);
        return view;
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setBackground(round(accent, 16));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setMinHeight(dp(52));
        return button;
    }

    private Button makeSmallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setMinHeight(dp(38));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(round(panel, 20));
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppEntry {
        final String label;
        final String packageName;
        final String activityName;
        AppEntry(String label, String packageName, String activityName) {
            this.label = label;
            this.packageName = packageName;
            this.activityName = activityName;
        }
    }
}
