package de.datavisions.agecalculator;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yalantis.ucrop.UCrop;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EnterData extends AppCompatActivity {

    final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy  |  HH:mm");

    SharedPreferences sp;
    int editID;  // If we edit an existing entry, its id is passed. Else -1

    AutoCompleteTextView nameView;

    LocalDateTime dob = null;
    TextView dobView;

    ShapeableImageView photoV;
    String uniqueFilename;
    Uri selectedPicture = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_data);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        findViewById(R.id.back).setOnClickListener(view -> finish());

        editID = getIntent().getIntExtra("editID", -1);

        nameView = findViewById(R.id.name);
        dobView = findViewById(R.id.chosen_date);
        photoV = findViewById(R.id.picture);

        uniqueFilename = UUID.randomUUID().toString() + ".png";

        findViewById(R.id.btn_delete).setVisibility(editID == -1 ? View.GONE : View.VISIBLE);
        findViewById(R.id.btn_dob).setOnClickListener(view -> showDateTimePicker());
        findViewById(R.id.btn_create).setOnClickListener(view -> saveNewEntry());
        findViewById(R.id.btn_delete).setOnClickListener(view -> deleteEntry());
        findViewById(R.id.select_picture).setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) pickPhoto();
            else requestReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        });
        ((TextView) findViewById(R.id.toolbar_title)).setText(editID == -1 ? R.string.new_person
                : R.string.edit_person);

        // Set values if editing an existing entry
        if(editID != -1) {
            Person p = MainActivity.loadPersonList(sp).get(editID);
            nameView.setText(p.getName());
            dob = p.getDob();
            dobView.setText(p.getDobString(this));
            if (p.getPicture() != null) {
                selectedPicture = p.getPicture();
                photoV.setImageURI(null);
                photoV.setImageURI(selectedPicture);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean Cache directory
        File cache = getCacheDir();
        if (cache.exists()) {
            File[] files = cache.listFiles();
            if (files != null) for (File file : files) file.delete();
        }
        this.getCacheDir().deleteOnExit();
    }


    private void showDateTimePicker() {
        final LocalDateTime currentDate = dob == null ? LocalDateTime.now() : dob;
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            dobView.setError(null);
            dob = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0);
            dobView.setText(DATE.format(dob));
            new TimePickerDialog(view.getContext(), (view1, hourOfDay, minute) -> {
                dob = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, hourOfDay, minute);
                dobView.setText(String.format("%s %s", DATE_TIME.format(dob),
                        getString(R.string.uhr)));
            }, 0, 0, true).show();
        }, currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth()).show();
    }


    // Open an image selection dialog
    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"});
        pickPhotoResult.launch(intent);
    }


    private void saveNewEntry() {
        String name = nameView.getText().toString();
        if (name.trim().isEmpty()) {
            nameView.setError(getString(R.string.error_name));
            nameView.requestFocus();
            return;
        }
        if (dob == null) {
            dobView.setError(getString(R.string.error_dob));
            Toast.makeText(this, R.string.error_dob, Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedPicture != null) {
            // Detect if the file has changed, or if selectedPicture refers to the FilesDir already
            if (selectedPicture.equals(Uri.fromFile(new File(getCacheDir(), uniqueFilename)))) {
                Uri targetUri = Uri.fromFile(new File(getFilesDir(), uniqueFilename));
                try {
                    // Copy file and update reference to FilesDir
                    copyFile(selectedPicture, targetUri);
                    selectedPicture = targetUri;
                } catch (IOException e) {
                    e.printStackTrace();
                    selectedPicture = null;
                }
            }
        }
        // Make the update/insert to the personList in Shared Preferences
        List<Person> personList = MainActivity.loadPersonList(sp);
        if (editID == -1) personList.add(new Person(name, dob, selectedPicture));
        else personList.set(editID, new Person(name, dob, selectedPicture));
        MainActivity.savePersonList(sp, personList);

        // Special case: If it is the first entry, and one or more empty widgets are waiting with no
        // data, call the update function for it to be refreshed with this newly created entry.
        // Of course, also update all widgets if an existing data entry was changed.
        if (personList.size() == 1 || editID != -1) {
            HashMap<Integer, Integer> widgetMap = new Gson().fromJson(sp.getString("widgetMap",
                    "{}"), new TypeToken<HashMap<Integer, Integer>>(){}.getType());
            for (int key : widgetMap.keySet()) {
                SinglePersonWidget.updateAppWidget(this, AppWidgetManager.getInstance(this), key);
            }
        }
        finish();
    }


    private void deleteEntry() {
        if (editID == -1) return;
        List<Person> personList = MainActivity.loadPersonList(sp);
        personList.remove(editID);
        MainActivity.savePersonList(sp, personList);
        // Update affected widgets
        HashMap<Integer, Integer> widgetMap = new Gson().fromJson(sp.getString("widgetMap", "{}"),
                new TypeToken<HashMap<Integer, Integer>>(){}.getType());
        for (int key : widgetMap.keySet()) {
            int personID = widgetMap.get(key);
            // Problem only if the removed entry is below or equal to the widget entry
            if (editID <= personID) {
                // If the widget refers to a number > 0, reduce it by one to adjust for the shift,
                // or if it was the one to be removed, to at least select a different valid one.
                if (personID > 0) widgetMap.put(key, personID - 1);
                // if it is 0 itself (and was removed itself), either a new value 0 be there now, or
                // if there is no value left, the value can stay zero and the widget will catch it.
                // Once there is a new entry there, the widget can be refreshed, and refers to this
                // new 0 element directly.

                // Save the changes
                sp.edit().putString("widgetMap", new Gson().toJson(widgetMap)).apply();
                // Update the widget
                SinglePersonWidget.updateAppWidget(this, AppWidgetManager.getInstance(this), key);
            }
        }
        finish();
    }


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog.
    private final ActivityResultLauncher<String> requestReadPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) pickPhoto();
                        else Toast.makeText(this, R.string.error_read, Toast.LENGTH_LONG).show();
                    });


    ActivityResultLauncher<Intent> cropResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    assert result.getData() != null;
                    selectedPicture = Uri.fromFile(new File(getCacheDir(), uniqueFilename));
                    photoV.setImageURI(null);
                    photoV.setImageURI(selectedPicture);
                } else (new File(getCacheDir(), uniqueFilename)).delete();
            });


    ActivityResultLauncher<Intent> pickPhotoResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    UCrop.Options options = new UCrop.Options();
                    options.setToolbarColor(getResources().getColor(R.color.fgGray1));
                    options.setStatusBarColor(getResources().getColor(R.color.fgGray1));
                    options.setToolbarTitle(getString(R.string.edit_image));
                    options.setActiveWidgetColor(getResources().getColor(R.color.fgBlue));
                    UCrop uCrop = UCrop.of(result.getData().getData(),
                            Uri.fromFile(new File(getCacheDir(), uniqueFilename)))
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(512, 512)
                            .withOptions(options);
                    cropResultLauncher.launch(uCrop.getIntent(this));
                }
            });


    private void copyFile(Uri pathFrom, Uri pathTo) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(pathFrom)) {
            if(in == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(pathTo)) {
                if(out == null) return;
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}