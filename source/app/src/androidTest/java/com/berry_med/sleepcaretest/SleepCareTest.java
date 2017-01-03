package com.berry_med.sleepcaretest;

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by ZXX on 2016/11/23.
 */

public class SleepCareTest extends AndroidTestCase {
    public void testABC(){
        Calendar c = Calendar.getInstance();
        byte[] dateTime =  new byte[6];
        dateTime[0] = (byte) (0xff & (c.get(Calendar.YEAR)%100));
        dateTime[1] = (byte) (0xff & (c.get(Calendar.MONTH)+1));
        dateTime[2] = (byte) (0xff & c.get(Calendar.DAY_OF_MONTH));
        dateTime[3] = (byte) (0xff & c.get(Calendar.HOUR_OF_DAY));
        dateTime[4] = (byte) (0xff & c.get(Calendar.MINUTE));
        dateTime[5] = (byte) (0xff & c.get(Calendar.SECOND));

        Log.i("TEST", "testABC: end----------------------" + Arrays.toString(dateTime));
    }
}
