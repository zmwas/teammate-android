package com.mainstreetcode.teammates.viewmodel;

import android.arch.lifecycle.ViewModel;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.mainstreetcode.teammates.App;
import com.mainstreetcode.teammates.R;
import com.mainstreetcode.teammates.model.ContentAd;
import com.mainstreetcode.teammates.model.Identifiable;
import com.mainstreetcode.teammates.model.InstallAd;
import com.mainstreetcode.teammates.util.ModelUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.functions.BiFunction;


abstract class BaseViewModel extends ViewModel {

    private static final int AD_THRESH = 5;

    BiFunction<List<Identifiable>, List<Identifiable>, List<Identifiable>> preserveList = (source, additions) -> {
        filterAds(source);
        ModelUtils.preserveList(source, additions);
        distributeAds(source);
        return source;
    };

    private LinkedList<Identifiable> ads = new LinkedList<>();

    BaseViewModel() {fetchAds();}

    void distributeAds(List<Identifiable> source) {
        if (source.isEmpty() || ads.isEmpty()) return;

        int numToShuffle = 0;
        int adSize = ads.size();
        int sourceSize = source.size();
        int count = 0;

        if (sourceSize <= AD_THRESH) {
            source.add(ads.get(0));
            shuffleAds(++numToShuffle);
            return;
        }

        for (int i = AD_THRESH; i < sourceSize; i += AD_THRESH) {
            if (count >= adSize) break;
            source.add(i, ads.get(count));
            numToShuffle++;
            count++;
        }
        shuffleAds(numToShuffle);
    }

    private void fetchAds() {
        App app = App.getInstance();
        AdLoader adLoader = new AdLoader.Builder(app, app.getString(R.string.admob_ad_id))
                .forAppInstallAd(appInstallAd -> ads.add(new InstallAd(appInstallAd)))
                .forContentAd(contentAd -> ads.add(new ContentAd(contentAd)))
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();

        adLoader.loadAds(new AdRequest.Builder().build(), 2);
    }

    private void filterAds(List<Identifiable> source) {
        Iterator<Identifiable> iterator = source.iterator();
        while (iterator.hasNext()) if (iterator.next() instanceof ContentAd) iterator.remove();
    }

    private void shuffleAds(int count) {
        for (int i = 0; i < count; i++) ads.add(ads.removeFirst());
    }
}