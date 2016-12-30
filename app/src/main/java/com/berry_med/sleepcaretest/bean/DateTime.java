package com.berry_med.sleepcaretest.bean;

/**
 * Created by ZXX on 2016/11/22.
 */

public class DateTime {
    private int year;
    private int month;
    private int day;

    private int hour;
    private int minute;
    private int second;

    public DateTime(int year, int month, int day, int hour, int minute, int second) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "[" + year +"-"+ month +"-"+ day+ " "+ hour +":"+ minute +":"+ second+"]";
    }
}
