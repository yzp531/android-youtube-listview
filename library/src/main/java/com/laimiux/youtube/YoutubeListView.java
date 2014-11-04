package com.laimiux.youtube;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by laimiux on 11/2/14.
 */
public class YoutubeListView extends ListView {
    // Needed variables to operate
    private YouTube mYouTube;
    private String mBrowserDevKey;
    private List<String> mYoutubeVideoIDs;

    // Requests
    private Observable<String> mObservableEmitsVideoIDs;


    public YoutubeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setKey(String browserDevKey) {
        mBrowserDevKey = browserDevKey;
    }

    public void setYoutubeVideoIDs(List<String> ids) {
        if (mBrowserDevKey == null) {
            throw new IllegalStateException("Call setKey() before calling this method");
        }

        final Context context = getContext();
        if (context instanceof YouTubeProvider) {
            mYouTube = ((YouTubeProvider) context).getYouTube();
        } else if (context.getApplicationContext() instanceof YouTubeProvider) {
            mYouTube = ((YouTubeProvider) context.getApplicationContext()).getYouTube();
        } else {
            throw new IllegalStateException("Your activity or application must extend YouTubeProvider");
        }


        mYoutubeVideoIDs = ids;

        mObservableEmitsVideoIDs = Observable.from(ids);


        loadPage(1);

    }

    private void loadPage(int page) {
        mObservableEmitsVideoIDs
                .flatMap(new Func1<String, Observable<VideoListResponse>>() {
                    @Override
                    public Observable<VideoListResponse> call(final String videoId) {
                        return Observable.create(new Observable.OnSubscribe<VideoListResponse>() {
                            @Override
                            public void call(Subscriber<? super VideoListResponse> subscriber) {
                                try {
                                    final YouTube.Videos.List contentDetails;
                                    contentDetails = mYouTube.videos().list("snippet");
                                    contentDetails.setKey(mBrowserDevKey);
                                    contentDetails.setId(videoId);

                                    final VideoListResponse videoListResponse = contentDetails.execute();

                                    if (!subscriber.isUnsubscribed()) {
                                        subscriber.onNext(videoListResponse);
                                        subscriber.onCompleted();
                                    }
                                } catch (IOException e) {
                                    if (!subscriber.isUnsubscribed()) {
                                        subscriber.onError(e);
                                    }

                                }

                            }
                        });
                    }
                })
                .map(new Func1<VideoListResponse, List<Video>>() {
                    @Override
                    public List<Video> call(VideoListResponse videoListResponse) {
                        return videoListResponse.getItems();
                    }
                })
                .reduce(new Func2<List<Video>, List<Video>, List<Video>>() {
                    @Override
                    public List<Video> call(List<Video> videos, List<Video> videos2) {
                        videos.addAll(videos2);
                        return videos;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Video>>() {
                    @Override
                    public void call(List<Video> videoListResponses) {

                        SimpleYoutubeListAdapter adapter = new SimpleYoutubeListAdapter(getContext(), videoListResponses);
                        setAdapter(adapter);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("YoutubeListView", "Failed to load VideoListResponses", throwable);
                    }
                });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


}
