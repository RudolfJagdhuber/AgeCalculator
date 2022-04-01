package de.datavisions.agecalculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    boolean isProVersion;
    SharedPreferences sp;
    List<Person> personList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        isProVersion = getPackageName().equals(getString(R.string.pro_package));

        // Check if first start. I.e. no data entry exists in SharedPreferences. -> Create it.
        if (sp.getString("personList", null) == null) savePersonList(sp, new ArrayList<>());

        findViewById(R.id.pro_badge).setVisibility(isProVersion ? View.VISIBLE : View.GONE);

        findViewById(R.id.impressum).setOnClickListener(view ->
                startActivity(new Intent(this, Impressum.class)));
        findViewById(R.id.add_button).setOnClickListener(view ->
                startActivity(new Intent(this, EnterData.class)));

        // Ads by Google
        MobileAds.initialize(this, initializationStatus -> {});

    }


    @Override
    protected void onStart() {
        super.onStart();
        // Load the current saved data von SharedPreferences and set the adapter
        personList = loadPersonList(sp);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.intro).setVisibility(personList.size() > 0 ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(personList.size() > 0 ? View.VISIBLE : View.GONE);

        recyclerView.setAdapter(new PersonAdapter(mixListWithAds()));
    }


    private List<Object> mixListWithAds() {
        List<Object> mixedList = new ArrayList<>();
        if (personList.size() > 0) {
            for (Person p : personList) {
                mixedList.add(p);
                // add an Ad after every Card for non-pro users
                if (!isProVersion) mixedList.add(new AdRequest.Builder().build());
            }
        }
        return mixedList;
    }



    public static List<Person> loadPersonList(SharedPreferences sp) {
        return new Gson().fromJson(sp.getString("personList", null),
                new TypeToken<ArrayList<Person>>(){}.getType());
    }


    public static void savePersonList(SharedPreferences sp, List<Person> personList) {
        sp.edit().putString("personList", new Gson().toJson(personList)).apply();
    }


    public class PersonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_PERSON = 0, TYPE_AD = 1;
        public List<Object> data;


        public PersonAdapter(List<Object> data) {
            this.data = data;
        }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).getClass() == Person.class ? TYPE_PERSON : TYPE_AD;
        }


        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }


        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_PERSON) {
                return new PersonViewHolder((LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_person, parent, false));
            } else {
                return new AdViewHolder((FrameLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_ad, parent, false));
            }
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == TYPE_PERSON) {
                ((PersonViewHolder) holder).setDetails((Person) data.get(position));
            } else if (viewType == TYPE_AD) {
                ((AdViewHolder) holder).setAd((AdRequest) data.get(position));
            }
        }


        public class AdViewHolder extends RecyclerView.ViewHolder {
            public AdView adView;

            public AdViewHolder(FrameLayout root) {
                super(root);
                adView = root.findViewById(R.id.adView);
            }

            void setAd(AdRequest adRequest) {
                adView.loadAd(adRequest);
            }
        }


        public class PersonViewHolder extends RecyclerView.ViewHolder{
            public TextView name, dob, ageYears, ageMonths, ageWeeks, ageDays;
            public ShapeableImageView picture;

            public PersonViewHolder(LinearLayout root) {
                super(root);
                picture = root.findViewById(R.id.picture);
                name = root.findViewById(R.id.name);
                dob = root.findViewById(R.id.dob);
                ageYears = root.findViewById(R.id.years);
                ageMonths = root.findViewById(R.id.months);
                ageWeeks = root.findViewById(R.id.weeks);
                ageDays = root.findViewById(R.id.days);
                root.findViewById(R.id.btn_edit).setOnClickListener(
                        view -> startActivity(new Intent(MainActivity.this, EnterData.class)
                                .putExtra("editID", getAdapterPosition())));
            }

            void setDetails(Person p) {
                if (p.getPicture() != null) {
                    picture.setImageURI(null);
                    picture.setImageURI(p.getPicture());
                } else picture.setImageResource(R.drawable.no_img);
                name.setText(p.getName());
                dob.setText(p.getDobString());
                ageYears.setText(String.format(Locale.ENGLISH, "%.2f %s",
                        p.getAgeInUnit(p.DATE_YEARS), getString(R.string.years)));
                ageMonths.setText(String.format(Locale.ENGLISH, "%.2f %s",
                        p.getAgeInUnit(p.DATE_MONTHS), getString(R.string.months)));
                ageWeeks.setText(String.format(Locale.ENGLISH, "%.2f %s",
                        p.getAgeInUnit(p.DATE_WEEKS), getString(R.string.weeks)));
                ageDays.setText(String.format(Locale.ENGLISH, "%.2f %s",
                        p.getAgeInUnit(p.DATE_DAYS), getString(R.string.days)));
            }

        }

    }












}