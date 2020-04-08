package com.example.joaodroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import androidx.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ChronoChart extends View {
    int gridColor = Color.BLACK;
    String mChronoName;
    private Canvas mCanvas;

    Paint mTextPaint;
    Paint mGridPaint;
    Paint mStartPaint;
    Paint mEndPaint;
    Paint mDurationPaint;
    Paint mDashPaint;

    ArrayList<LocalDateTime> startDates = new ArrayList<>();
    ArrayList<LocalDateTime> endDates = new ArrayList<>();
    ArrayList<Float> startTimes = new ArrayList<>();
    ArrayList<Float> endTimes = new ArrayList<>();

    int mAvgTime;
    int mLowerBound = 0;
    int mUpperBound = 12;
    LocalDateTime mLeftBound;
    LocalDateTime mRightBound;

    // Grid variables.
    int mPadding = 100;
    int mPadding2 = 32;
    int mGridLeft = 0;
    int mGridBottom = 0;
    int mGridTop = 0;
    int mGridRight = 0;
    int mGridWidth = 0;
    int mGridHeight = 0;
    float vStep = 0;
    float hStep = 0;
    int mWindow = 14;
    int mInitX = 0;
    int mInitY = 0;

    public ChronoChart(Context context, @Nullable AttributeSet attrs, String chronoName) {
        super(context, attrs);

        mChronoName = chronoName;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void createPaints() {
        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(22);

        mGridPaint = new Paint();
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setColor(gridColor);
        mGridPaint.setStrokeWidth(1);

        mStartPaint = new Paint();
        mStartPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mStartPaint.setColor(Color.BLUE);
        mStartPaint.setStrokeWidth(1);

        mEndPaint = new Paint();
        mEndPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mEndPaint.setColor(Color.RED);
        mEndPaint.setStrokeWidth(1);

        mDurationPaint = new Paint();
        mDurationPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mDurationPaint.setColor(Color.BLACK);
        mDurationPaint.setStrokeWidth(1);

        mDashPaint = new Paint();
        mDashPaint.setStyle(Paint.Style.STROKE);
        mDashPaint.setColor(Color.DKGRAY);
        mDashPaint.setStrokeWidth(1);
        mDashPaint.setPathEffect(new DashPathEffect(new float[]{5, 10}, 0));
    }

    private void drawHorizontalLines(Canvas canvas) {
        vStep = mGridHeight / (mUpperBound - mLowerBound);

        float y = mInitY;
        for (int hour = mLowerBound; hour <= mUpperBound; ++hour) {
            if (hour < 0) {
                canvas.drawText(String.format("%02dh", 24 + hour), mPadding2, y + 7, mTextPaint);
            } else {
                canvas.drawText(String.format("%02dh", hour), mPadding2, y + 7, mTextPaint);
            }

            canvas.drawLine(90, y, mPadding, y, mGridPaint);
            canvas.drawLine(mGridLeft, y, mGridRight, y, mDashPaint);
            y -= vStep;
        }
    }

    private void drawVerticalLines(Canvas canvas) {
        hStep = mGridWidth / mWindow;

        float x = mInitX;
        LocalDateTime current = mLeftBound;
        while (current.isBefore(mRightBound) || current.isEqual(mRightBound)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
            String strDate = formatter.format(current);

            canvas.drawText(strDate, x - 14, mGridBottom + mPadding2 + 20, mTextPaint);
            canvas.drawLine(x, mGridBottom, x, mGridBottom + 14, mGridPaint);
            canvas.drawLine(x, mGridBottom, x, mGridTop, mDashPaint);

            current = current.plusDays(1);
            x += hStep;
        }
    }

    private void drawGrid(Canvas canvas) {
        mGridLeft = mPadding;
        mGridBottom = getHeight() - mPadding;
        mGridTop = mPadding2;
        mGridRight = getWidth() - mPadding2;
        mGridWidth = mGridRight - mGridLeft - 2 * mPadding2;
        mGridHeight = mGridBottom - mGridTop - 2 * mPadding2;
        mInitX = mGridLeft + mPadding2;
        mInitY = mGridBottom - mPadding2;

        // Draw bounding lines;
        canvas.drawLine(mGridLeft, mGridBottom, mGridLeft, mGridTop, mGridPaint);
        canvas.drawLine(mGridLeft, mGridBottom, mGridRight, mGridBottom, mGridPaint);

        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
    }

    private LocalDateTime toLocalDatetime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private float getTimeInHours(int timeInSecs) {
        int negativeTimeInSecs = timeInSecs - 86400; // Minus one day.
        if (Math.abs(timeInSecs - mAvgTime) < Math.abs(negativeTimeInSecs - mAvgTime)) {
            return (float) timeInSecs / 3600.0f;
        }
        return (float) negativeTimeInSecs / 3600.0f;
    }

    private void processDataPoints() {
        LogReader.Chrono chrono = LogReader.chronos.get(mChronoName);

        mLeftBound = LocalDateTime.now().minusDays(mWindow - 1);
        mLeftBound = mLeftBound.with(LocalTime.of(0, 0));
        mRightBound = LocalDateTime.now();
        mRightBound = mRightBound.with(LocalTime.of(0, 0));
        mRightBound = mRightBound.plusDays(1);

        startDates = new ArrayList<>();
        endDates = new ArrayList<>();
        startTimes = new ArrayList<>();
        endTimes = new ArrayList<>();
        mAvgTime = (chrono.avgStartTimeSecs + chrono.avgEndTimeSecs) / 2;

        for (int i = 0; i < chrono.rawStartDates.size(); ++i) {
            LocalDateTime dt = toLocalDatetime(chrono.rawStartDates.get(i));
            if (dt.isBefore(mLeftBound)) continue;
            startDates.add(dt);
            startTimes.add(getTimeInHours(chrono.startDates.get(i)));

        }

        for (int i = 0; i < chrono.rawEndDates.size(); ++i) {
            LocalDateTime dt = toLocalDatetime(chrono.rawEndDates.get(i));
            if (dt.isBefore(mLeftBound)) continue;
            endDates.add(dt);
            endTimes.add(getTimeInHours(chrono.endDates.get(i)));
        }

        if (startTimes.size() > 0 && endTimes.size() > 0) {
            mLowerBound = (int) (Math.floor(Collections.min(startTimes)) - 1);
            mUpperBound = (int) (Math.ceil(Collections.max(endTimes)) + 1);
        }
    }

    private float distanceToLeftBoundInDays(LocalDateTime dateTime) {
        long seconds = mLeftBound.until(dateTime, ChronoUnit.SECONDS);
        return (float) seconds / 86400.0f; // One day.
    }

    private void drawLines(Canvas canvas, ArrayList<Pair<Float, Float>> pairs, Paint paint) {
        for (int i = 0; i < pairs.size(); ++i) {
            canvas.drawCircle(pairs.get(i).first, pairs.get(i).second, 5, paint);

            if (i > 0) {
                Pair<Float, Float> last = pairs.get(i-1);
                canvas.drawLine(last.first, last.second, pairs.get(i).first, pairs.get(i).second, paint);
            }
        }
    }

    private void drawBars(Canvas canvas, ArrayList<Pair<Float, Float>> starts,
                          ArrayList<Pair<Float, Float>> ends) {
        int i = 0;
        int j = 0;

        while (i < starts.size() && j < ends.size()) {
            Pair<Float, Float> ePoint = ends.get(j);

            Pair<Float, Float> sPoint = starts.get(i++);
            while (i < starts.size() && starts.get(i).first < ePoint.first) {
                canvas.drawCircle(sPoint.first, sPoint.second, 5, mStartPaint);
                sPoint = starts.get(i);
                i++;
            }

            if (sPoint.first > ePoint.first) {
                canvas.drawCircle(ePoint.first, ePoint.second, 5, mEndPaint);
                i--;
                j++;
                continue;
            }

            canvas.drawLine(sPoint.first, sPoint.second, ePoint.first, ePoint.second, mDurationPaint);
            // canvas.drawRect(sPoint.first, sPoint.second, ePoint.first, ePoint.second, mEndPaint);
            canvas.drawCircle(sPoint.first, sPoint.second, 5, mStartPaint);
            canvas.drawCircle(ePoint.first, ePoint.second, 5, mEndPaint);
            j++;
        }
    }

    private void drawData(Canvas canvas) {
        ArrayList<Pair<Float, Float>> starts = new ArrayList<>();
        for (int i = 0; i < startTimes.size(); i++) {
            float secs = distanceToLeftBoundInDays(startDates.get(i));
            float x = mInitX + hStep * secs;
            float y = startTimes.get(i) - mLowerBound;
            y = mInitY - vStep * y;
            starts.add(new Pair<>(x, y));
        }

        ArrayList<Pair<Float, Float>> ends = new ArrayList<>();
        for (int i = 0; i < endTimes.size(); i++) {
            float secs = distanceToLeftBoundInDays(endDates.get(i));
            float x = mInitX + hStep * secs;
            float y = endTimes.get(i) - mLowerBound;
            y = mInitY - vStep * y;
            ends.add(new Pair<>(x, y));
        }

        // drawLines(canvas, starts, mStartPaint);
        // drawLines(canvas, ends, mEndPaint);

        drawBars(canvas, starts, ends);
    }

    // Called when the view should render its content.
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mCanvas = canvas;

        createPaints();
        processDataPoints();
        drawGrid(canvas);
        drawData(canvas);
    }
}
