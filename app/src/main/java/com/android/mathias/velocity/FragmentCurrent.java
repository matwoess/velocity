package com.android.mathias.velocity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FragmentCurrent extends android.support.v4.app.Fragment {

    private static int NOTIFICATION_ID = 1;
    TextView mTimeView;
    TimeState mTimeState;
    long mStartTime;
    long mLastStopTime;
    Route mCurrentWalkRoute;
    ObjectAnimator mAnimator;
    NotificationManager mNotificationManager;

    FloatingActionButton mFab;
    Button mBtnR;
    private Handler mHandler;
    ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_current, container, false);
        setHasOptionsMenu(true);
        mTimeView = (TextView) view.findViewById(R.id.timer);
        mTimeState = TimeState.STOPPED;
        mFab = (FloatingActionButton) view.findViewById(R.id.fab_current_toggle);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleStopwatch();
            }
        });
        mBtnR = (Button) view.findViewById(R.id.fab_current_stop);
        mBtnR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopWalk();
            }
        });
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", 0, 600);
        mAnimator.setDuration(120000);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        return view;
    }

    private void toggleStopwatch() {
        switch (mTimeState) {
            case STOPPED: startWalk(); break;
            case RUNNING: pauseStopwatch(); break;
            case PAUSED:  resumeStopwatch(); break;
            default: break;
        }
    }

    private void startWalk() {
        final List<String> routeNames = new ArrayList<>();
        for (Route r : DBManager.getRoutes(getContext(), null)) { routeNames.add(r.getName()); }
        String defaultRouteName = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("default_route", "None");
        if (defaultRouteName.equals("None")) { mCurrentWalkRoute = new Route("No route set"); }
        else { mCurrentWalkRoute = DBManager.getRoutes(getContext(), defaultRouteName).get(0); }
        if (routeNames.size() > 1 && defaultRouteName.equals("None")) {
            final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item);
            adapter.addAll(routeNames);
            adapter.add("None");
            final ListPopupWindow lpw = new ListPopupWindow(getContext());
            lpw.setAdapter(adapter);
            lpw.setAnchorView(mFab);
            lpw.setContentWidth((int) (((View)mFab.getParent()).getWidth()/2.5));
            lpw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (adapterView.getItemAtPosition(i).toString().equals("None")) {
                        mCurrentWalkRoute = new Route("No route set");
                    } else {
                        mCurrentWalkRoute = DBManager.getRoutes(getContext(), adapterView.getItemAtPosition(i).toString()).get(0);
                    }
                    startStopwatch();
                    lpw.dismiss();
                }
            });
            lpw.show();
        } else {
            startStopwatch();
        }
    }

    private void startStopwatch() {
        mStartTime = SystemClock.elapsedRealtime();
        mFab.setImageResource(android.R.drawable.ic_media_pause);
        mTimeState = TimeState.RUNNING;
        mBtnR.setClickable(true);
        mBtnR.setVisibility(View.VISIBLE);
        ((TextView)getActivity().findViewById(R.id.txt_current_route)).setText(mCurrentWalkRoute.getName());
        mAnimator.start();
        mHandler = new Handler();
        mHandler.post(mRunnable);
    }

    private void pauseStopwatch() {
        mLastStopTime = SystemClock.elapsedRealtime();
        mFab.setImageResource(android.R.drawable.ic_media_play);
        mTimeState = TimeState.PAUSED;
        mAnimator.pause();
        mHandler.removeCallbacks(mRunnable);
    }

    private void resumeStopwatch() {
        mStartTime = mStartTime + (SystemClock.elapsedRealtime() - mLastStopTime);
        mFab.setImageResource(android.R.drawable.ic_media_pause);
        mTimeState = TimeState.RUNNING;
        mAnimator.resume();
        mHandler.post(mRunnable);
    }

    private void stopWalk() {
        long walkTime = SystemClock.elapsedRealtime() - mStartTime;
        Walk walk = new Walk(walkTime, new Date(), mCurrentWalkRoute);
        DBManager.saveWalk(getContext(), walk);
        mLastStopTime = 0;
        mFab.setImageResource(android.R.drawable.ic_media_play);
        mTimeState = TimeState.STOPPED;
        mBtnR.setClickable(false);
        mBtnR.setVisibility(View.INVISIBLE);
        mCurrentWalkRoute = null;
        ((TextView)getActivity().findViewById(R.id.txt_current_route)).setText("");
        mAnimator.cancel();
        mNotificationManager.cancel(NOTIFICATION_ID);
        mHandler.removeCallbacks(mRunnable);
    }

    private void buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_current)
                .setContentTitle(mTimeState == TimeState.RUNNING ? "Ongoing walk" : "Walk paused")
                .setSubText(mCurrentWalkRoute.getName())
                .setContentText(DateFormat.format("mm:ss", new Date((SystemClock.elapsedRealtime()-mStartTime))))
                //.setProgress(600, (int) mAnimator.getAnimatedValue(), false)
                .setOngoing(true);
        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_app));
        //builder.setAutoCancel(true);
        Intent resultIntent = new Intent(getActivity(), ActivityMain.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
        stackBuilder.addParentStack(ActivityMain.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_current, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), ActivitySettings.class));
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    private enum TimeState {
        RUNNING, PAUSED, STOPPED
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long time = (SystemClock.elapsedRealtime() - mStartTime);
            mTimeView.setText(DateFormat.format("mm:ss", new Date(time)));
            buildNotification();
            mHandler.postDelayed(this, 1000);
        }
    };
}


