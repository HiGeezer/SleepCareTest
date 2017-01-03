package com.berry_med.sleepcaretest.bluetooth;

import android.util.Log;
import com.berry_med.sleepcaretest.bean.DateTime;
import java.util.Arrays;

/**
 * Created by ZXX on 2016/1/8.
 */
public class DataParser {

    //Const
    public String TAG = this.getClass().getSimpleName();

    //package
    private int[]         PACKAGE_HEAD                 = new int[]{0x55,0xaa};
    private final int     PKG_RECORD_START_DATETIME    = 0x00;
    private final int     PKG_RECORD_STOP_DATETIME     = 0x01;
    private final int     PKG_RECORD_SPO2              = 0x02;
    private final int     PKG_RECORD_PULSE_RETE        = 0x03;
    private final int     PKG_RECORD_RR_INTERVAL       = 0x04;
    private final int     PKG_RECORD_ACC               = 0x05;
    private final int     PKG_BATT_LEVEL               = 0x10;
    private final int     PKG_DEVICE_TIME              = 0x11;
    private final int     PKG_DEVICE_ID                = 0x12;
    private final int     PKG_RECORD_STATE             = 0x13;
    private final int     PKG_BUZZ_STATE               = 0x14;
    private final int     PKG_RECORDS_COUNT            = 0x15;
    private final int     PKG_FIRMWARE_VER             = 0xe0;
    private final int     PKG_HARDWARE_VER             = 0xe1;
    private final int     PKG_MEMORY_SIZE              = 0xe2;

    //command
    public static byte[]  CMD_FETCH_START_DATETIME  = new byte[]{0x55, (byte) 0xaa, 0x03, 0x00, (byte)0xfc};
    public static byte[]  CMD_FETCH_STOP_DATETIME   = new byte[]{0x55, (byte) 0xaa, 0x03, 0x01, (byte)0xfb};
    public static byte[]  CMD_FETCH_SPO2            = new byte[]{0x55, (byte) 0xaa, 0x03, 0x02, (byte)0xfa};
    public static byte[]  CMD_FETCH_PULSE_RATE      = new byte[]{0x55, (byte) 0xaa, 0x03, 0x03, (byte)0xf9};
    public static byte[]  CMD_FETCH_RR_INTERVAL     = new byte[]{0x55, (byte) 0xaa, 0x03, 0x04, (byte)0xf8};
    public static byte[]  CMD_FETCH_ACC             = new byte[]{0x55, (byte) 0xaa, 0x03, 0x05, (byte)0xf7};
    public static byte[]  CMD_BATT_LEVEL            = new byte[]{0x55, (byte) 0xaa, 0x03, 0x10, (byte)0xec};
    public static byte[]  CMD_DEVICE_TIME           = new byte[]{0x55, (byte) 0xaa, 0x03, 0x11, (byte)0xeb};
    public static byte[]  CMD_DEVICE_ID             = new byte[]{0x55, (byte) 0xaa, 0x03, 0x12, (byte)0xea};
    public static byte[]  CMD_RECORD_STATE          = new byte[]{0x55, (byte) 0xaa, 0x03, 0x13, (byte)0xe9};
    public static byte[]  CMD_BUZZ_STATE            = new byte[]{0x55, (byte) 0xaa, 0x03, 0x14, (byte)0xe8};
    public static byte[]  CMD_FETCH_RECORDS_COUNT   = new byte[]{0x55, (byte) 0xaa, 0x03, 0x15, (byte)0xe7};
    public static byte[]  CMD_TURNON_RECORD         = new byte[]{0x55, (byte) 0xaa, 0x04, 0x20, 0x01,(byte) 0xda};
    public static byte[]  CMD_TURNOFF_RECORD        = new byte[]{0x55, (byte) 0xaa, 0x04, 0x20, 0x00,(byte) 0xdb};
    public static byte[]  CMD_TURNON_BUZZ           = new byte[]{0x55, (byte) 0xaa, 0x04, 0x21, 0x01,(byte) 0xd9};
    public static byte[]  CMD_TURNOFF_BUZZ          = new byte[]{0x55, (byte) 0xaa, 0x04, 0x21, 0x00,(byte) 0xda};
    public static byte[]  CMD_SYNC_DATETIME         = new byte[]{0x55, (byte) 0xaa, 0x09, 0x22, 0x01, 0x02, 0x03, 0x04, 0x05,0x06,0x07};
    public static byte[]  CMD_ERASE_RECORDS         = new byte[]{0x55, (byte) 0xaa, 0x03, 0x30, (byte) 0xcc};

    public static byte[]  CMD_FIRMWARE_VER         = new byte[]{0x55, (byte) 0xaa, 0x03, (byte)0xe0, 0x1c};
    public static byte[]  CMD_HARDWARE_VER         = new byte[]{0x55, (byte) 0xaa, 0x03, (byte)0xe1, 0x1b};
    public static byte[]  CMD_MEMORY_SIZE          = new byte[]{0x55, (byte) 0xaa, 0x03, (byte)0xe2, 0x1a};


    private onPackageReceivedListener mListener;
    private int                       recordNumber = 0;
    private final static int          BUFFER_SIZE  = 1024;
    private byte[]                    recvData     = new byte[BUFFER_SIZE];
    private int                       emptyIndex   = 0;
    private int                       parseIndex   = 0;

    /**
     * Add the data from bluetooth into a circular queue, the parse the data of the queue,
     *
     * @param buf
     * @param bufSize
     */
    public void add(byte[] buf, int bufSize)
    {
        boolean pkgStart = false;
        int pkgIndex = 0;
        int pkgLength = 0;
        int[] pkgData = null;

        Log.d(TAG, "add: " + Arrays.toString(buf));

        if(bufSize+emptyIndex <= BUFFER_SIZE)
        {
            System.arraycopy(buf, 0, recvData, emptyIndex, bufSize);
            emptyIndex = (emptyIndex+bufSize) % BUFFER_SIZE;
        }
        else if( (bufSize+emptyIndex > BUFFER_SIZE) && (bufSize+emptyIndex < 2*BUFFER_SIZE))
        {
            System.arraycopy(buf, 0, recvData, emptyIndex, BUFFER_SIZE-emptyIndex);
            int temp = emptyIndex;
            emptyIndex = 0;
            System.arraycopy(buf, BUFFER_SIZE-temp, recvData, emptyIndex, bufSize-(BUFFER_SIZE-temp));
            emptyIndex = bufSize-(BUFFER_SIZE-temp);
        }
        else {
            Log.e(TAG, "Receive too much data.");
            return;
        }


        int i = parseIndex;
        while (i != emptyIndex) {

            if ((recvData[i]&0xff) == PACKAGE_HEAD[0]) {
                int j = (i + 1)%BUFFER_SIZE;
                if (j != emptyIndex && (recvData[j]&0xff) == PACKAGE_HEAD[1]) {
                    int k = (j+1)%BUFFER_SIZE;
                    if(k != emptyIndex)
                    {
                        pkgLength = toUnsignedInt(recvData[k]);
                        pkgData = new int[pkgLength+2];
                        pkgStart = true;
                        pkgIndex = 0;
                        parseIndex = i;
                    }
                }
            }
            if (pkgStart && pkgLength > 0) {
                pkgData[pkgIndex] = toUnsignedInt(recvData[i]);
                pkgIndex++;

                if ((pkgLength != 0) && (pkgIndex == pkgLength + 2)) {
                    if(CheckSum(pkgData)){
                        ParsePackage(pkgData);
                    }
                    else{
                        Log.e(TAG, "-------------Check Sum ERROR-------------");
                    }
                    pkgStart = false;
                    parseIndex = (i + 1) % BUFFER_SIZE;
                }
            }
            i = (i + 1) % BUFFER_SIZE;
        }
    }

    /**
     * interface for parameters changed.
     */
    public interface onPackageReceivedListener
    {
        void onBatteryLevelReceived(int level);
        void onDateTimeReceived(DateTime datetime);
        void onBuzzStateReceived(boolean state);
        void onRecordStateReceived(int state);

        void onRecordsCountReceived(int count);
        void onRecordStartDateTimeReceived(DateTime dt);
        void onRecordStopDateTimeReceived(DateTime dt);
        void onSPO2RecordFinished(int num);
        void onPulseRateRecordFinished(int num);
        void onRRIntervalRecordFinished(int num);
        void onACCRecordFinished(int num);

        void onFirmwareVersionReceived(String ver);
        void onHardwareVersionReceived(String ver);
        void onMemorySizeReceived(String ver);

    }

    //Constructor
    public DataParser(onPackageReceivedListener listener) {
        this.mListener = listener;
    }

    private void ParsePackage(int[] pkgData) {
        // TODO Auto-generated method stub
        int pkgType = pkgData[3];

        switch (pkgType) {
            case PKG_RECORD_START_DATETIME:
                DateTime dateTime0 = new DateTime(pkgData[4],pkgData[5],pkgData[6],
                        pkgData[7],pkgData[8],pkgData[9]);
                mListener.onRecordStartDateTimeReceived(dateTime0);
                Log.i(TAG, "ParsePackage: start Date & time " + pkgData[4]+"-"+pkgData[5]+"-"+pkgData[6]+ "  " +
                        pkgData[7]+":"+pkgData[8]+":"+pkgData[9]);
                break;
            case PKG_RECORD_STOP_DATETIME:
                DateTime dateTime1 = new DateTime(pkgData[4],pkgData[5],pkgData[6],
                        pkgData[7],pkgData[8],pkgData[9]);
                mListener.onRecordStopDateTimeReceived(dateTime1);
                Log.i(TAG, "ParsePackage: stop Date & time " + pkgData[4]+"-"+pkgData[5]+"-"+pkgData[6]+ "  " +
                        pkgData[7]+":"+pkgData[8]+":"+pkgData[9]);
                break;
            case PKG_RECORD_SPO2:
                //Log.i(TAG, "ParsePackage: SPO2 Record-----"+ recordNumber + Arrays.toString(pkgData));
                Log.i(TAG, "*"+Arrays.toString(pkgData));
                if(pkgData[2] == 3) {
                    mListener.onSPO2RecordFinished(recordNumber);
                    recordNumber = 0;
                }
                else{
                    recordNumber += (pkgData[2] - 3);
                }
                break;
            case PKG_RECORD_PULSE_RETE:
                Log.i(TAG, "ParsePackage: Pulse Rate Record-----"+ recordNumber + Arrays.toString(pkgData));
                if(pkgData[2] == 3) {
                    mListener.onPulseRateRecordFinished(recordNumber);
                    recordNumber = 0;
                }
                else{
                    recordNumber += (pkgData[2] - 3);
                }
                break;
            case PKG_RECORD_RR_INTERVAL:
                Log.i(TAG, "ParsePackage: RR Interval Record-----"+ recordNumber + Arrays.toString(pkgData));
                if(pkgData[2] == 3) {
                    mListener.onRRIntervalRecordFinished(recordNumber);
                    recordNumber = 0;
                }
                else{
                    recordNumber += (pkgData[2] - 3)/2;
                }
                break;
            case PKG_RECORD_ACC:
                Log.i(TAG, "ParsePackage: ACC Interval Record-----"+ recordNumber + Arrays.toString(pkgData));
                if(pkgData[2] == 3) {
                    mListener.onACCRecordFinished(recordNumber);
                    recordNumber = 0;
                }
                else{
                    recordNumber += (pkgData[2] - 3)/3;
                }
                break;
            case PKG_BATT_LEVEL:
                mListener.onBatteryLevelReceived(pkgData[4]);
                /*Log.i(TAG, "ParsePackage: Battery Level " + pkgData[4]);*/
                break;
            case PKG_DEVICE_TIME:
                DateTime dateTime2 = new DateTime(pkgData[4],pkgData[5],pkgData[6],
                        pkgData[7],pkgData[8],pkgData[9]);
                mListener.onDateTimeReceived(dateTime2);
                /*Log.i(TAG, "ParsePackage: Date & time " + pkgData[4]+"-"+pkgData[5]+"-"+pkgData[6]+ "  " +
                        pkgData[7]+":"+pkgData[8]+":"+pkgData[9]);*/
                break;
            case PKG_BUZZ_STATE:
                mListener.onBuzzStateReceived(pkgData[4]!=0);
                break;
            case PKG_RECORDS_COUNT:
                int count = (pkgData[4] << 16) + (pkgData[5] << 8) + pkgData[6];
                Log.d(TAG, "ParsePackage: count" + count);
                mListener.onRecordsCountReceived(count);
                break;
            case PKG_RECORD_STATE:
                mListener.onRecordStateReceived(pkgData[4]);
                break;
            case PKG_FIRMWARE_VER:
                StringBuffer sb = new StringBuffer("");
                for(int i = 4; i < pkgData.length - 1; i++){
                    sb.append((char)(pkgData[i]));
                }
                mListener.onFirmwareVersionReceived(sb.toString());
                break;
            case PKG_HARDWARE_VER:
                StringBuffer sb1 = new StringBuffer("");
                for(int i = 4; i < pkgData.length - 1; i++){
                    sb1.append((char)(pkgData[i]));
                }
                mListener.onHardwareVersionReceived(sb1.toString());
                break;
            case PKG_MEMORY_SIZE:
                mListener.onMemorySizeReceived(pkgData[4] + "M");
                break;
            default:
                break;
        }

    }

    private boolean CheckSum(int[] packageData) {
        // TODO Auto-generated method stub
        int sum = 0;
        for(int i = 2; i < packageData.length-1; i++)
        {
            sum+=(packageData[i]);
        }

        if(((~sum)&0xff) == (packageData[packageData.length-1]&0xff))
        {
            return true;
        }

        return false;
    }


    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }
}
