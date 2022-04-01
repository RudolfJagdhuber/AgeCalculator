package de.datavisions.agecalculator;

import android.net.Uri;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Person implements Serializable {

    public final int DATE_YEARS = 0;
    public final int DATE_MONTHS = 1;
    public final int DATE_WEEKS = 2;
    public final int DATE_DAYS = 3;

    private String name;
    private Calendar dob;
    private String picturePath;  // Treated as Uri, only stored as String for gson compatibility

    public Person(String name, Calendar dob, Uri picture) {
        this.name = name;
        this.dob = dob;
        this.picturePath = picture == null ? null : picture.toString();
    }

    public Person(String name, Calendar dob) {
        this.name = name;
        this.dob = dob;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Calendar getDob() {
        return dob;
    }

    public void setDob(Calendar dob) {
        this.dob = dob;
    }

    public Uri getPicture() {
        return picturePath == null ? null : Uri.parse(picturePath);
    }

    public void setPicture(Uri picture) {
        this.picturePath = picture == null ? null : picture.toString();
    }

    public String getDobString() {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy  |  HH:mm", Locale.ENGLISH);
        return dateFormat.format(dob.getTime()) + " Uhr";
    }


    public double getAgeInUnit(int unit) {
        // Get difference to now in milliseconds
        double diffMillis = Calendar.getInstance().getTimeInMillis() - dob.getTimeInMillis();

        switch (unit) {
            case DATE_DAYS:
                return diffMillis / (24 * 60 * 60 * 1000);
            case DATE_WEEKS:
                return diffMillis / (7L * 24 * 60 * 60 * 1000);
            case DATE_MONTHS:
                return diffMillis / (30.4267 * 24 * 60 * 60 * 1000);
            case DATE_YEARS:
                return diffMillis / (365L * 24 * 60 * 60 * 1000);
            default:
                return diffMillis;
        }
    }



}
