package de.datavisions.agecalculator;

import android.content.Context;
import android.net.Uri;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.Serializable;
import java.util.Locale;

public class Person implements Serializable {

    public final int DATE_YEARS = 0;
    public final int DATE_MONTHS = 1;
    public final int DATE_WEEKS = 2;
    public final int DATE_DAYS = 3;
    public final int DATE_FULL = 4;

    private String name;
    private final LocalDateTime dob;
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


    public Uri getPicture() {
        return picturePath == null ? null : Uri.parse(picturePath);
    }


    public String getDobString(Context ctx) {
        return String.format("%s %s",
                DateTimeFormatter.ofPattern("dd.MM.yyyy  |  HH:mm").format(dob),
                ctx.getString(R.string.uhr));
    }


    public String getAgeInUnit(int unit, Context ctx) {

        LocalDateTime now = LocalDateTime.now();
        double exactDays = (double) ChronoUnit.MINUTES.between(dob, now) / (60 * 24);

        // Total amount of weeks, months, years
        long weeks = (long) Math.floor(exactDays / 7);
        long months = ChronoUnit.MONTHS.between(dob, now);
        long years = ChronoUnit.YEARS.between(dob, now);

        // Remainders after taken the "bigger" unit away (Years > Months > Days)
        long remainderMonths = months - years * 12;
        double remainderWeekDays = exactDays - weeks * 7;
        // Get datetime after months were taken. Compute remainder days from that day on
        double remainderDays = exactDays -
                ChronoUnit.DAYS.between(dob, ChronoUnit.MONTHS.addTo(dob, months));

        switch (unit) {
            case DATE_DAYS:
                return String.format(Locale.ENGLISH, "%.2f %s", exactDays,
                        ctx.getString(R.string.days));
            case DATE_WEEKS:
                return String.format(Locale.ENGLISH, "%d %s  |  %.2f %s",
                        weeks, ctx.getString(weeks == 1 ? R.string.week : R.string.weeks),
                        round2(remainderWeekDays), ctx.getString(R.string.days));
            case DATE_MONTHS:
                return String.format(Locale.ENGLISH, "%d %s  |  %.2f %s", months,
                        ctx.getString(months == 1 ? R.string.month : R.string.months),
                        round2(remainderDays), ctx.getString(R.string.days));
            case DATE_YEARS:
                return String.format(Locale.ENGLISH, "%.2f %s", round2(exactDays / 365),
                        ctx.getString(R.string.years));
            case DATE_FULL:
                return String.format(Locale.ENGLISH, "%d %s  |  %d %s  |  %.2f %s",
                        years, ctx.getString(years == 1 ? R.string.year : R.string.years),
                        remainderMonths, ctx.getString(remainderMonths == 1 ? R.string.month
                                : R.string.months),
                        round2(remainderDays), ctx.getString(R.string.days));
            default:
                return "";
        }
    }

    // round to two digits
    public static double round2(double number) {
        return (double) Math.round(100 * number) / 100;
    }

}
