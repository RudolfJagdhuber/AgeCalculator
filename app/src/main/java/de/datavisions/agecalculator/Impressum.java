package de.datavisions.agecalculator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.yalantis.ucrop.BuildConfig;

public class Impressum extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impressum);
        findViewById(R.id.back).setOnClickListener(view -> finish());

        TextView appInfo = findViewById(R.id.app_info);
        appInfo.setText(String.format("%s %s", getString(R.string.info), BuildConfig.VERSION_NAME));

        findViewById(R.id.btn_pro).setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
                        + getString(R.string.pro_package))));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id="
                                + getString(R.string.pro_package))));
            }
        });

        boolean isProVersion = getPackageName().equals(getString(R.string.pro_package));
        findViewById(R.id.pro_badge).setVisibility(isProVersion ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_pro).setVisibility(isProVersion ? View.GONE : View.VISIBLE);
        findViewById(R.id.thanks).setVisibility(isProVersion ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.title_pro)).setText(isProVersion ? R.string.thanks
                : R.string.pro);
        ((TextView) findViewById(R.id.desc_pro)).setText(isProVersion ? R.string.thanks_desc
                : R.string.pro_desc);
    }
}