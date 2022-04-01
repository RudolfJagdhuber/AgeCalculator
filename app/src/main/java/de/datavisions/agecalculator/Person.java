package de.datavisions.agecalculator;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.Period;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;

public class Person implements Serializable {

    public final int DATE_YEARS = 0;
    public final int DATE_MONTHS = 1;
    public final int DATE_WEEKS = 2;
    public final int DATE_DAYS = 3;
    public final int DATE_FULL = 4;

    private String name;
    private LocalDateTime dob;
    private String picturePath;  // Treated as Uri, only stored as String for gson compatibility

    public Person(String name, LocalDateTime dob, Uri picture) {
        this.name = name;
        this.dob = dob;
        this.picturePath = picture == null ? null : picture.toString();
    }

    public Person(String name, LocalDateTime dob) {
        this.name = name;
        this.dob = dob;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDob() {
        return dob;
    }

    public void setDob(LocalDateTime dob) {
        this.dob = dob;
    }

    public Uri getPicture() {
        return picturePath == null ? null : Uri.parse(picturePath);
    }

    public void setPicture(Uri picture) {
        this.picturePath = picture == null ? null : picture.toString();
    }

    public String getDobString(Context ctx) {
        return String.format("%s %s",
                DateTimeFormatter.ofPattern("dd.MM.yyyy  |  HH:mm").format(dob),
                ctx.getString(R.string.uhr));
    }


    public String getAgeInUnit(int unit, Context ctx) {

        long year, month;
        double remainderDays;

        LocalDateTime now = LocalDateTime.now();
        Period period = Period.between(dob.toLocalDate(), now.toLocalDate());

        double exactDays = (double) ChronoUnit.MINUTES.between(dob, now) / (60 * 24);

        switch (unit) {
            case DATE_DAYS:
                return String.format(Locale.ENGLISH, "%.2f %s", exactDays,
                        ctx.getString(R.string.days));
            case DATE_WEEKS:
                return String.format(Locale.ENGLISH, "%.2f %s",
                        (double) Math.round(100 * exactDays / 7) / 100,
                        ctx.getString(R.string.weeks));
            case DATE_MONTHS:
                remainderDays = period.getDays() - 1 + exactDays - Math.floor(exactDays);
                month = ChronoUnit.MONTHS.between(dob, now);
                return String.format(Locale.ENGLISH, "%d %s  |  %.2f %s", month,
                        ctx.getString(month == 1 ? R.string.month : R.string.months),
                        (double) Math.round(100 * remainderDays) / 100,
                        ctx.getString(R.string.days));
            case DATE_YEARS:
                return String.format(Locale.ENGLISH, "%.2f %s",
                        (double) Math.round(100 * exactDays / 365) / 100,
                        ctx.getString(R.string.years));
            case DATE_FULL:
                remainderDays = period.getDays() - 1 + exactDays - Math.floor(exactDays);
                year = period.getYears();
                month = period.getMonths();
                return String.format(Locale.ENGLISH, "%d %s  |  %d %s  |  %.2f %s",
                        year, ctx.getString(year == 1 ? R.string.year : R.string.years),
                        month, ctx.getString(month == 1 ? R.string.month : R.string.months),
                        (double) Math.round(100 * remainderDays) / 100,
                        ctx.getString(R.string.days));
            default:
                return "";
        }
    }



}
