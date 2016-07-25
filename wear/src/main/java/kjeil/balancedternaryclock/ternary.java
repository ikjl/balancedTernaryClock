/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kjeil.balancedternaryclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

// screen width and height

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class ternary extends CanvasWatchFaceService {
    // for trouble
    // boolean greebo = false; // Greebo is on the prowl.
    // long x = 0; // count onDraw
    // long y = 0; // count handleUpdateTimeMessage
    // long z = 0;
    // long w = 0;
    // float floaty;
    // float floatz;
    // float floatx;
    // long tic = 0; // number of times onTimeTick called handler.

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    /**
     * Update rate in milliseconds for interactive mode. We update once every half second
     * for simplicity until method applied to update mid second, mid minute, mid hour
     * and mid night
     * with conservation mode to update mid minute, mid hour and mid night.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 500;
    // interval for alarm to wake in ambient.
    private static final long AMBIENT_INTERVAL_MS = 30000;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {return new Engine();}

    private static class EngineHandler extends Handler {
        private final WeakReference<ternary.Engine> mWeakReference;

        public EngineHandler(ternary.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            ternary.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        // Matrix by Dheera Venkatraman Backdrop (gethub.com/dheera/android-wearface-matrix)
        boolean yesMatrix = false; // dispaly matrix Smith scanner.
        private final int mSettingsNumRows = 23; 
        private int mMatrixBaseColor = Color.GREEN; 
        private Random random = new Random(); 
        private Paint[] mMatrixPaints = new Paint[8];
        private final String[] matrixChars = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4",
                "5", "6", "7", "8", "9", "@", "$", "&", "%", "(", ")", "*", "%", "!", "#", "ア", "イ", "ウ", "エ",
                "オ", "カ", "キ", "ク", "ケ", "コ", "サ", "シ", "ス", "セ", "ソ", "タ", "チ", "ツ", "テ", "ト",
                "ナ", "ニ", "ヌ", "ネ", "ノ", "ハ", "ヒ", "フ", "ヘ", "ホ"} ; 
        private String[][] mMatrixValues = new String[mSettingsNumRows][mSettingsNumRows];
        private int[][] mMatrixIntensities = new int[mSettingsNumRows][mSettingsNumRows] ;
        private int mCharWidth; 
        private int matriXOffset;  

        // calculated date
        byte [] yearTryte;
        byte [] monthTryte;
        byte [] dayTryte;
        byte [] wkDayTryte; 
        int [] wkDay; // stors the day of the week for wkDayTrits()
        List<byte[]> dateTrytes = new ArrayList<byte[]>(3);

        // screen size:
        float wideness;
        float tallness;
        float tallable; // portion of screen above cards.
        float wideable; 
        float xMargin; 
        float dispTop;
        float dispLeft;
        float ledge; // useable bottom of display
        float baseLedge; // ledge without cards
        float ledgeSpace; 
        float mantle; // portion of display above ledge
        boolean yesLiftDate = false; // keep date above the ledge
        float density;
        float textSize; // normal text size
        float smallTextSize; // small text size
        float xRatio = (float).618; // ratio of text width to height
        float yRoundRatio = (float).847; // the ratio of usable height to diamater for round screens.
        float textRatio = (float)0.875; // the ratio of the text size to yShift
        float puff = 1; // magnification of display to shirnk with cards.
        float tSize; // textSize * puff
        float stSize; 

        // array for time.
        byte [] timeray = new byte [11];
        float posX; // offset position while printing to screen.
        float posY;
        float xShiftDefault = 50; // default movement between characters
        float xShift = xShiftDefault; // movement between characters
        float yShiftDefault = 80;
        float yShift = yShiftDefault;
        float yThird; // 1/3rd yShift
        float xThird;
        float dateOffset;
        byte rayTrit;
        // the * is for the minute in ambient mode which does not update on the half minute and would be off
        // yet the display remains accurate to the nearest three minutes, so a wild card is shown for the
        // minute. This may be removed once AlarmManger is successfuly implemented.
        String [] stringRay = {"¬", "☯", "1", "•"};
        // character codes, 1/ratio, yShift
        // 1, 2/3, -2 not ¬ 1.5 , 0 gear ⚙  1/4, -17, ☯ 1/2, -9 bullet • 1, 2
        float [] stringRatio = {(float)1, (float)1/(float)2, (float).8, (float)1};
        float [] vShiftRay = new float[4]; // yShift/2 - hiehgt / 2
        float [] smallVShiftRay = new float[4];
        float [] vShiftRayDef = { 0 , -11, -5, 0}; 
        float [] smallVShiftRayDef = new float[4]; 
        float [] greyVShiftRay = new float[4]; 

        // for yShiftStringArray
        Rect rectangle = new Rect();

        // on tick
        boolean yesTic = true;

        //small text
        Paint smallTextPaint;

        // swtich between small dim and normal depending on mode
        Paint greenTextPaint;
        Paint blueTextPaint;
        Paint redTextPaint;
        Paint greyTextPaint;
        Paint smallRTextPaint;
        Paint smallGTextPaint;
        Paint smallBTextPaint;
        Paint smallGreyTextPaint; 
        Paint commonRTextPaint;
        Paint commonGTextPaint;
        Paint commonBTextPaint;
        Paint commonGreyTextPaint;
        Paint greyRTextPaint;
        Paint greyGTextPaint;
        Paint greyBTextPaint;
        Paint greyGreyTextPaint;


        Paint [] textRay = {redTextPaint, greenTextPaint, blueTextPaint, greyTextPaint};
        Paint [] smallTextRay = {smallRTextPaint, smallGTextPaint, smallBTextPaint, smallGreyTextPaint};
        Paint [] commonTextRay = {commonRTextPaint, commonGTextPaint, commonBTextPaint, commonGreyTextPaint}; 
        Paint [] greyTextRay = {greyRTextPaint, greyGTextPaint, greyBTextPaint, greyGreyTextPaint}; 

        // up date ambientScheme
        // color schemes 0 = bright rgb,  1 = dim rgb, 2 = dim r(grey)b 3 = orange aqua purple
        // 4 = bright orange theme, 5 = translucent orange theme, 6 = orange ambient
        //  r = red, g = green, b = blue, o = orange, t = teal, p = purple, y = yellow, c = cyan
        //                  rgb          rgb          dim rgb      dim rgb     otp       dim otp      roy         dim roy     rwy          cgt         dim cgt   cbp         dim cbp      rwb       dim rwb     dim rwb   
        int [] redRay =   {0xFFFF0000, 0xFFFF0000, 0xFF800000, 0xFF800000, 0xFFFF8000, 0xFF804000, 0xFFFF4000, 0x80FF4000, 0x80FF4000, 0xFF80FF00, 0xFF408000, 0xFF8000FF, 0xFF400080, 0xFFFF0000, 0xFF800000, 0xFF800000};
        int [] greenRay = {0xFF00FF00, 0xFF008000, 0xFF008000, 0xFF004000, 0xFF00FFA0, 0xFF008050, 0xFFD08000, 0x80D08000, 0x80808080, 0xFF00FF80, 0xFF004020, 0xFF0000FF, 0xFF000040, 0xFFFFFFFF, 0xFF808080, 0xFF404040};
        int [] blueRay =  {0xFF0000FF, 0xFF0000FF, 0xFF000080, 0xFF000080, 0xFF8000FF, 0xFF400080, 0xFFC0C000, 0x80FFFF00, 0x80FFFF00, 0xFF00FFFF, 0xFF008080, 0xFF00FFFF, 0xFF008080, 0xFF0000FF, 0xFF000080, 0xFF000080};
        int [] smallRay = {0xFF00FF00, 0xFF00FF00, 0xFF008000, 0xFF404040, 0xFF00FFA0, 0xFF008050, 0xFFD08000, 0x80D08000, 0x80808080, 0xFF00FF80, 0xFF008040, 0xFF0000FF, 0xFF000080, 0xFFFFFFFF, 0xFF808080, 0xFF808080};
        int [] greyRay =  {0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080}; 
        List<int[]> rgbRays = new ArrayList<int[]>();

        // ambient map modTapCount -> ambient scheme number
        byte [] ambientScheme = {2, 3, 3, 3, 5, 5, 8, 8, 8, 10, 10, 12, 12, 14, 14, 15};

        // draw ran without handler
        boolean unhandled = true;
        boolean uncalled = true; // handler hasn't been called in over a tic.

        // leapSecond and leapMinute method boolean and incrament in milliseconds.
        boolean yesLeap = false;
        int leap = 0;

        //for handleUpdateTimeMessage()
        long timeMs;
        long delayMs = 0;
        // long wasTime;
        // long nextTime = 0;

        boolean wasInAm = false;
        long millisUTM = 999;

        // first run.
        boolean yesFirstRun = true;
        // flag for seconds
        boolean yesSecs;
        // prior state of yesSecs
        boolean wasSecs = true;
        // flag for conservation mode (no seconds in ambient)
        boolean yesConcerve = false;
        // flag for allowing seconds
        boolean allowSecs = true;
        // for execution control
        boolean openGate = false;

        // updateTim
        boolean yesRefresh = true;


        // counter for time loops
        int k;
        int j; // secondary counter
        // register for milliseconds
        long millis = 0;
        // float for calculating trits from time.
        float tim = 0;
        // ints for minute and hour offsets.
        int minInt = 0;
        int hourInt = 0;

        // lists for time to trit for loop conversions.
        int [] trits3 = {9, 3, 1};
        int [] trits4 = {27, 9, 3, 1};

        // find the height of a card
        final Rect mCardBounds = new Rect();

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        boolean mAmbient;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                yesterdayTomorrow(); 
                yesRefresh=true; 
            }
        };

        Calendar now = new GregorianCalendar();
        Calendar tomorrow = new GregorianCalendar();
        Calendar yesterday = new GregorianCalendar();
        
        int mTapCount;
        int modTapCount = 0;

        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        private boolean isTrue = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(ternary.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    //.setCardPeekMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setCardPeekMode(WatchFaceStyle.PEEK_OPACITY_MODE_OPAQUE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    //.setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_PERSISTENT)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = ternary.this.getResources();

            // Matrix Continued.
            int i, j;
            for(i = 0; i <=5 ; i++) {
                mMatrixPaints[i] = new Paint();
                mMatrixPaints[i].setColor(Color.rgb(0, i * 32, 0));
                /*mMatrixPaints[i].setColor(
                        (mMatrixBaseColor & 0xFF) / 8 * i +
                        ((mMatrixBaseColor>>8 & 0xFF) / 8 * i) << 8 +
                        ((mMatrixBaseColor>>16 & 0xFF) / 8 * i) << 16 +
                        ((mMatrixBaseColor>>24 & 0xFF) / 8 * i) << 24
                ); */
                mMatrixPaints[i].setTextSize(mCharWidth - 1);
                mMatrixPaints[i].setAntiAlias(false);
            }

            mMatrixPaints[6] = new Paint();
            mMatrixPaints[6].setColor(Color.rgb(63, 255, 63));
            mMatrixPaints[6].setTextSize(mCharWidth - 1);
            mMatrixPaints[6].setAntiAlias(false);
            mMatrixPaints[7] = new Paint();
            mMatrixPaints[7].setColor(Color.rgb(191, 255, 191));
            mMatrixPaints[7].setTextSize(mCharWidth - 1);
            mMatrixPaints[7].setAntiAlias(false);

            for(i = 0; i < mSettingsNumRows; i++) {
                for (j = 0; j < mSettingsNumRows; j++) {
                    mMatrixValues[i][j] = matrixChars[random.nextInt(matrixChars.length)];
                    mMatrixIntensities[i][j]=0;
                }
            }

            // list of arrays cannot be defined before onCreate.
            dateTrytes.add(yearTryte); dateTrytes.add(monthTryte); dateTrytes.add(dayTryte); 
            dateTrytes.add(wkDayTryte); 
            rgbRays.add(redRay); rgbRays.add(greenRay); rgbRays.add(blueRay); rgbRays.add(greyRay); 

            yesterdayTomorrow();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(0xFFFFFFFF);

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(0xFFFF8000);

            // small text
            smallTextPaint = new Paint();
            smallTextPaint = createTextPaint(0xFFFF8000);

            createTextArrayColor(textRay, 0, false); createTextArrayColor(smallTextRay, 0, false);
            createTextArrayColor(commonTextRay, 0, false);createTextArrayColor(greyTextRay, 0, true);

            // initialize date trits.
            yearTrits(); monthTrits(); dayTrits(); wkDayTrits();  
            invalidate(); // first draw of watch

        }

        // call when changing timezones.
        private void yesterdayTomorrow(){
            now.setTimeZone(TimeZone.getDefault()); 
            yesterday.setTimeZone(TimeZone.getDefault()); 
            tomorrow.setTimeZone(TimeZone.getDefault()); 
            yesterday.set(Calendar.HOUR_OF_DAY, 0);
            yesterday.set(Calendar.MINUTE, 0);
            yesterday.set(Calendar.SECOND, 0);
            yesterday.set(Calendar.MILLISECOND, 0);
            tomorrow = (Calendar)yesterday.clone(); tomorrow.add(Calendar.DATE, 1);
            if (yesterday.getTimeInMillis() > now.getTimeInMillis()){
               tomorrow.add(Calendar.DATE, -1); yesterday.add(Calendar.DATE, -1);
               yearTrits(); monthTrits(); dayTrits(); wkDayTrits();
            }
        }
           
        // array of paints -> none
        private void createTextArrayColor(Paint[] array, int c, boolean isGrey){
            for (byte i = 0; i < array.length; i++) {
                array[i] = new Paint(); array[i] = createTextPaint(rgbRays.get(
                         isGrey ? 3 : i)[c]);
                array[i].setTextAlign(Paint.Align.CENTER);
            }
        }

        // array of paints, byte no greater than array length , [boolena] -> none
        private void setTextArrayColor(Paint[] array, int c, boolean isGrey){
            for (byte i = 0; i < array.length; i++) {
               array[i].setColor(rgbRays.get(isGrey ? 3 : i)[c]); }
        }

        // array of paints, float, [boolean] -> none, 
        private void setTextArraySize(Paint[] array, float ts, boolean isCommon){
            for (int i = 0; i < array.length; i++) {
                array[i].setTextSize(isCommon ? ts : ts * stringRatio[i]);
            } 
        }

        // array of paints -> none
        private void setTextArrayAntiAlias(Paint[] array, boolean yesAA){
            for (Paint i : array) { i.setAntiAlias(yesAA); }
        }

        // flaot [], float -> none
        // multiplies each element in the array
        private void puffFloatArray(float[] floats, float[] floats2, float aFloat){
           for (int i = 0; i < floats.length; i++){floats[i] = floats2[i] * aFloat; }}

        // sets verticle centering values for characters.
        // paint array must correspond to string array. 
        // float[], String[], paint[], float -> none
        /*
        private void yShiftStringArray(float [] yShifts, String [] strings, Paint [] paints, float midShift){
           for (int i = 0; i < strings.length ; i++){ 
              paints[i].getTextBounds(strings[i], 0, strings[i].length(), rectangle); 
              yShifts[i] = midShift - (float)rectangle.height() * (float).5;
           } 
        } */  

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        // detect the height of a card. And call resizers.
        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);
                if (bounds.top == 0){bounds.top = (int)tallness;}
                if (bounds.top < baseLedge || ledge != baseLedge) {
                    ledge = Math.min((float)bounds.top, baseLedge);
                    // if (greebo) {ledge -= (float).4*yShiftDefault;} // greebo
                    puff = (ledge - dispTop - ledgeSpace) / mantle; 
                    if (puff < (float)0) {puff =0; }
                    puffer (); // resize
                } else { invalidate();} // watch unaffected by card movement
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                yesterdayTomorrow(); 
                yesSecs = !isInAmbientMode() && allowSecs; yesRefresh = true; handleUpdateTimeMessage();
            } else {unregisterReceiver();}
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED); 
            ternary.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ternary.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);


            // Load resources that have alternate values for round watches.
            Resources resources = ternary.this.getResources();
            boolean isRound = insets.isRound();

            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            density = displayMetrics.density;
            tallness = displayMetrics.heightPixels;
            wideness = displayMetrics.widthPixels;
            xMargin = resources.getDimension(R.dimen.digital_x_offset)/(float)2; 
            ledgeSpace = xMargin; 

            // find margins as a proportion of 8 / 5 ratio for normal monospace text block.
            if (isRound) {
               tallable = tallness * yRoundRatio; wideable = tallable * xRatio;
            } else {
               wideable = wideness - xMargin;
               if (wideness/tallness < xRatio) { tallable = wideable / xRatio;}
               else {tallable = tallness - xMargin; wideable = tallable * xRatio; }
            }
            yShift = tallable / (float)4;
            xShift = yShift * xRatio; 
            yThird = yShift/(float)3; xThird = xShift/(float)3; 
            dispTop = (tallness - tallable)/(float)2;
            mantle = tallable - ledgeSpace - (yesLiftDate ? 0 : yShift); 
            dispLeft = (wideness - wideable)/(float)2;
            baseLedge = tallness - dispTop - (yesLiftDate ? 0 : yShift); ledge = baseLedge;

            textSize = yShift * textRatio; tSize = textSize; 
            smallTextSize = textSize / (float)3; stSize = smallTextSize;
            smallTextPaint.setTextSize(smallTextSize); 
            
            xShiftDefault = xShift; yShiftDefault = yShift;

            mYOffset = dispTop + yShift; 
            mXOffset = (wideness - wideable + xThird * (float)3)/(float)2; 
            dateOffset = mXOffset + xShift/(float)3;
            posX = mXOffset; posY = mYOffset;
            mTextPaint.setTextSize(tSize);

            setTextArraySize(textRay, tSize, false); setTextArraySize(smallTextRay, stSize, false);
            setTextArraySize(commonTextRay, tSize, true);setTextArraySize(greyTextRay, tSize, false);
            puffFloatArray(smallVShiftRayDef, vShiftRayDef, (float)1/(float)3);
            puffFloatArray(smallVShiftRay, smallVShiftRayDef, (float)1);
            puffFloatArray(vShiftRay, vShiftRayDef, (float)1);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            yesRefresh = true; handleUpdateTimeMessage();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            // tic++; 
            if (isInAmbientMode()) {yesRefresh = true; handleUpdateTimeMessage();}
            else {if (uncalled) {yesRefresh = true; handleUpdateTimeMessage();}
            else {uncalled = true;}}
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    setTextArrayAntiAlias(textRay, !inAmbientMode);
                    setTextArrayAntiAlias(commonTextRay, !inAmbientMode);
                    setTextArrayAntiAlias(smallTextRay, !inAmbientMode);
                    setTextArrayAntiAlias(greyTextRay, !inAmbientMode);
                }

                if (isInAmbientMode()){
                    yesSecs = false;
                    setTextArrayColor(smallTextRay, ambientScheme[modTapCount], false);
                    setTextArrayColor(textRay, ambientScheme[modTapCount], false);
                    setTextArrayColor(commonTextRay, ambientScheme[modTapCount], false);
                    setTextArrayColor(greyTextRay, ambientScheme[modTapCount], true);
                }
                else {
                    yesSecs = allowSecs; yesRefresh = true; 
                    setTextArrayColor(smallTextRay, modTapCount, false);
                    setTextArrayColor(textRay, modTapCount, false);
                    setTextArrayColor(commonTextRay, modTapCount, false);
                    setTextArrayColor(greyTextRay, modTapCount, true); 
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            //? not sure of the relevance because the timer seems to stop in ambient mode, vissible or not.
            yesRefresh = !isInAmbientMode(); handleUpdateTimeMessage();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = ternary.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    //Tap top
                    if (y < tallness * (float).25) { yesMatrix = !yesMatrix;}
                    else {
                       // right and left
                    if (x > wideness * (float).5) {mTapCount++;} else {mTapCount -= 1;}
                    modTapCount = (mTapCount % ambientScheme.length + ambientScheme.length) % ambientScheme.length;

                    setTextArrayColor(smallTextRay, modTapCount, false);
                    setTextArrayColor(textRay, modTapCount, false);
                    setTextArrayColor(commonTextRay, modTapCount, false);
                    setTextArrayColor(greyTextRay, modTapCount, true);
                    }
                    break;
            }
            yesTic = true;  handleUpdateTimeMessage();
        }

        // none -> none changes size of text and text movements.
        public void puffer(){
            tSize = textSize * puff; stSize = smallTextSize * puff; 
            yShift = yShiftDefault * puff; xShift = xShiftDefault * puff;
            setTextArraySize(smallTextRay, stSize, false); setTextArraySize(textRay, tSize, false);
            setTextArraySize(commonTextRay, tSize, true); setTextArraySize(greyTextRay, tSize, false);
            xShift = xShiftDefault * puff; yShift = yShiftDefault * puff;
            yThird = yShift/(float)3; xThird = xShift/(float)3;
            mXOffset =  (wideness - wideable * puff + xThird * (float)3)/(float)2;
            mYOffset = dispTop + yShift; 
            dateOffset = mXOffset + xShift/(float)3;
            puffFloatArray(vShiftRay, vShiftRayDef, puff); 
            puffFloatArray(smallVShiftRay, smallVShiftRayDef, puff);
            
            invalidate(); 
        }
            
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // x++; // greebo

            uncalled = true; // the handler was not called if this remains true.

            // Draw the background.
            canvas.drawColor(Color.BLACK);

            // Ready on Matrix
            if (yesMatrix && !isInAmbientMode()) {
               int width = bounds.width();
               int height = bounds.height();

               mCharWidth = width / mSettingsNumRows + 1;
               matriXOffset = (width - mCharWidth * mSettingsNumRows)/2;
               int i, j;
               for (i = 0; i < mSettingsNumRows; i++) {
                   for (j = mSettingsNumRows - 1; j > 0; j--) {
                       if (mMatrixIntensities[i][j] == 7 || (j < 5 && random.nextInt(24) == 0)) {
                           canvas.drawText(mMatrixValues[i][j], matriXOffset + i * mCharWidth, j * mCharWidth, mMatrixPaints[7]);
                           if (random.nextInt(2) == 0) {
                               if (j < mSettingsNumRows - 1) {
                                   mMatrixIntensities[i][j + 1] = 7;
                                   mMatrixValues[i][j + 1] = matrixChars[random.nextInt(matrixChars.length)];
                               }
                               mMatrixIntensities[i][j] = 6;
                           }
                       } else {
                           if (mMatrixIntensities[i][j] > 0) {
                               canvas.drawText(mMatrixValues[i][j], matriXOffset + i * mCharWidth, j * mCharWidth, mMatrixPaints[mMatrixIntensities[i][j]]);
                           }
                           mMatrixIntensities[i][j] += random.nextInt(5) - 3;
                           if (mMatrixIntensities[i][j] < 0) mMatrixIntensities[i][j] = 0;
                           if (mMatrixIntensities[i][j] >= 7) mMatrixIntensities[i][j] = 6;
                       }
                   }
               }
            }

            
            updateTim(); 

            // Draw hms trits
            posX = (mXOffset + xShift); posY = mYOffset;
            k = 0; for (byte i : timeray ) {
                j = i + (byte)1; 
                if (k == 3 || k ==7) {
                    if (k==7 && isInAmbientMode()){break;}
                    posX = mXOffset; posY += yShift; 
                }
                /* // alternate symbols indicate ambient mode inacuracy
                else { 
                   if (k==6 && isInAmbientMode()){ // no mid minute updates
                      canvas.drawText(stringRay[0], posX, posY + vShiftRay[0], greyTextRay[0]);
                      canvas.drawText(stringRay[2], posX, posY + vShiftRay[2], greyTextRay[2]);
                      canvas.drawText(stringRay[3], posX, posY + vShiftRay[3], commonTextRay[j]);
                      break; 
                   }
                } */
                canvas.drawText(stringRay[j], posX, posY + vShiftRay[j], textRay[j]);
                k += 1; posX += xShift;
            }

            // draw datetrytes
            posX = mXOffset + xThird * ((float)1.5 - wkDayTryte.length); posY += yThird * (float)2; // or 1.8
            for (byte i : wkDayTryte) {
                j = i + (byte)1;
                canvas.drawText(stringRay[j], posX, posY + smallVShiftRay[j], smallTextRay [j]);
                posX += xThird;
            }
            posX += xThird*((float)5 - dayTryte.length);
            for (byte i : dayTryte) {
                j = i + (byte)1;
                canvas.drawText(stringRay[j], posX, posY + smallVShiftRay[j], smallTextRay [j]);
                posX += xThird;
            }
            posX += xThird * ((float)4 - monthTryte.length); 
            for (byte i : monthTryte) {
                j = i + (byte)1;
                canvas.drawText(stringRay[j], posX, posY + smallVShiftRay[j], smallTextRay [j]);
                posX += xThird; 
            }
            posX = mXOffset + xThird * ((float)5 - (yearTryte.length)*(float).5); posY += yThird; // or 4.8
            for (byte i : yearTryte) {
                j = i + (byte)1; 
                canvas.drawText(stringRay[j], posX, posY + smallVShiftRay[j], smallTextRay [j]);
                posX += xThird;
            }

            
            // if (greebo) {
            //     canvas.drawText(String.format("%1$.3f,%2$d,%3$d"
            //             ,floaty, tic, x)
            //             , xMargin ,(mCardBounds.top == 0) ? tallness :
            //                     mCardBounds.top, smallTextPaint);
            //     canvas.drawText(String.format("%1$d,%2$d,%3$ 5d, %4$d"
            //             ,y, z, w, (int)vShiftRay[(int)x%3])
            //             , xMargin,-yShiftDefault/(float)3 + 
            //             ((mCardBounds.top == 0) ? tallness : (float)mCardBounds.top), smallTextPaint);
            // }

            // Draw every frame as long as we're visible and in interactive mode.
            // id est, burn battery burn!!!
            if (yesMatrix && isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        // none -> none
        private void updateTim(){
            // z++; // greebo
            now.setTimeInMillis(System.currentTimeMillis());
            if ( now.getTimeInMillis() >= tomorrow.getTimeInMillis() ){
                tomorrow.add(Calendar.DATE, 1); yesterday.add(Calendar.DATE, 1);
                now.setTimeInMillis(System.currentTimeMillis());
                yearTrits(); monthTrits(); dayTrits(); wkDayTrits();
            }
            tim = (float)(now.getTimeInMillis() - yesterday.getTimeInMillis())/3600000 - 12;
            // floaty = tim; // greebo
            tritTim(); 
        }

        // none -> none
        private void tritTim(){
            // find hrs 9s, 3s, 1s.
            openGate = false;
            if (yesRefresh || now.get(Calendar.MINUTE) == 30) {
                openGate = true;
            } else {
                openGate = now.get(Calendar.SECOND) == 0 && now.get(Calendar.MINUTE) == 0 
                   && now.get(Calendar.HOUR_OF_DAY) == 0;
            }
            hourTrits(openGate);

            // minutes 27s, 9s, 3s, 1s
            if (openGate || wasInAm || now.get(Calendar.SECOND) == 30) {
                openGate = true;
            } else {
                openGate = now.get(Calendar.SECOND) == 0 && (now.get(Calendar.MINUTE) == 30
                        || (now.get(Calendar.MINUTE) == 0 && now.get(Calendar.HOUR_OF_DAY) == 0));
            }
            minTrits(openGate);

            // seconds 27s, 9s, 3s, 1s
            if (yesRefresh||yesSecs) {
                secTrits();
            }
            if (yesRefresh) {yesRefresh = false; }
        }
        
        // bool -> none
        private void hourTrits(boolean yesRefresh) {
            if (yesRefresh) {
                k = 0;
                hourInt = 0;
                // i is for item.
                for (int i : trits3) {
                    if (tim > ((float) i / 2)) {
                        tim -= i;
                        hourInt -= i;
                        timeray[k] = 1;
                    } else {
                        if (tim < ((float) i / -2)) {
                            tim += i;
                            hourInt += i;
                            timeray[k] = -1;
                        } else {
                            timeray[k] = 0;
                        }
                    }
                    k += 1;
                }
            } else { tim += hourInt; }
        }

        private void minTrits (boolean yesRefresh) {
            if (yesRefresh) {
                if (k != 3) {k = 3;}
                tim *= 60;
                minInt = 0;
                // i is for item.
                for (int i : trits4) {
                    if (tim > ((float) i / 2)) {
                        tim -= i;
                        if (allowSecs) {minInt -= i;}
                        timeray[k] = 1;
                    } else {
                        if (tim < ((float) i / -2)) {
                            tim += i;
                            if (allowSecs) {minInt += i;}
                            timeray[k] = -1;
                        } else { timeray[k] = 0; }
                    }
                    k += 1;
                }
            } else { if (yesSecs) { tim *= 60; tim += minInt; } }
        }

        private void yearTrits () {
            yearTryte = tritArrayer((float) now.get(Calendar.YEAR));
        }

        private void monthTrits () {monthTryte = tritArrayer((float)(now.get(Calendar.MONTH))+1);}

        private void dayTrits () {dayTryte = tritArrayer((float)(now.get(Calendar.DAY_OF_MONTH)));}
        
        private void wkDayTrits () {
           wkDayTryte = tritArrayer(((float)((now.get(Calendar.DAY_OF_WEEK) + 1) % 7) - 3 ));
        }

        // float -> byte[array]
        private byte [] tritArrayer (float num) {
            byte length = (byte)Math.round(Math.log(Math.abs(num))/ Math.log((float)3));
            byte [] tryte = new byte[length + 1];
            num /= Math.pow(3, length);
            for (byte i = 0; i < length + 1; i++ ) {
                if (num <= -.5) { tryte[i] = (byte)-1; num += 1; }
                else {if (num >= .5){tryte[i] = (byte)1; num -= 1;}}
                num *= 3;
            }
        return tryte;

        }

        // none -> none
        private void secTrits () {
            if (k != 7) {k = 7;}
            tim *= 60;
            for (int i : trits4) {
                if (tim > ((float) i / 2)) {
                    tim -= i;
                    timeray[k] = 1;
                } else {
                    if (tim < ((float) i / -2)) {
                        tim += i;
                        timeray[k] = -1;
                    } else {
                        timeray[k] = 0;
                    }
                }
                k += 1;
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            // y++; 
            invalidate();
            uncalled = false;
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            // unhandled = false;
            // wasTime = nextTime;
            // w = now.getTimeInMillis() - timeMs; 
            now.setTimeInMillis(System.currentTimeMillis());
            timeMs = now.getTimeInMillis();
            millis = now.get(Calendar.MILLISECOND);

            // schedule a new alarm
            if (isInAmbientMode()) {
                wasInAm = true;
                delayMs = 0;
                //if (isVisible()) {
                    //leapMinute();
                    //delayMs = AMBIENT_INTERVAL_MS - (timeMs % AMBIENT_INTERVAL_MS) + leap;
                    //mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                }
            else {
                wasInAm = false;
                leapSecond();
                delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS) + leap;
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }

            // prevent over handleing
            // openGate = false; if (yesTic||yesRefresh) {openGate = true;}
            // else { openGate = timeMs - wasTime > 400; }

            // if (openGate){ yesTic = false; nextTime = timeMs; invalidate();}
            // invalidate(); 
            // unhandled = true;
        }

        // to wait a half or whole second. leap = 30 -> wait a second.
        private void leapSecond () {
            yesLeap = false;
            leap = 500;
            if (millis > 490) {yesLeap = now.get(Calendar.SECOND) != 29;}
            // millis < 500
            if (!yesLeap) {leap = 0;}
        }

            // to wait a half or whole minute. leap = 30 -> wait a minute.
        private void leapMinute () {
            yesLeap = true;
            leap = 30000;
            if (now.get(Calendar.SECOND) > 29) {
                if (now.get(Calendar.MINUTE) == 29) {yesLeap = false;}
                else { if (now.get(Calendar.MINUTE) == 59 && now.get(Calendar.HOUR_OF_DAY) == 23)
                {yesLeap = false;}}
            // second > 30
            } else {yesLeap = false;}
            if (!yesLeap) { leap = 0;}
        }
    }
}
