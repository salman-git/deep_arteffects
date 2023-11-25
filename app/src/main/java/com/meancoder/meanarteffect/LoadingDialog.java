package com.meancoder.meanarteffect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class LoadingDialog {
    private final AdView mAdView;
    private Activity activity;
    private AlertDialog dialog;
    public LoadingDialog(Activity ctx) {
        activity = ctx;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.loading_alertdialog_layout, null);
        dialog = alertDialog.setView(v).setCancelable(false).create();
        MobileAds.initialize(ctx, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = v.findViewById(R.id.alertDialogBannerAd);
    }
    public void showDialog() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        dialog.show();
    }
    public void dismissDialog(){
        if(dialog != null) {
            dialog.dismiss();
        }
    }
}
