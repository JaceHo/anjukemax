package com.anjuke.max58.activity;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.anjuke.max58.app.BrowserApp;
import com.anjuke.max58.R;

import javax.inject.Inject;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.react.Action;
import acr.browser.lightning.react.Observable;
import acr.browser.lightning.react.OnSubscribe;
import acr.browser.lightning.react.Schedulers;
import acr.browser.lightning.react.Subscriber;
import acr.browser.lightning.react.Subscription;
import acr.browser.lightning.reading.HtmlFetcher;
import acr.browser.lightning.reading.JResult;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import butterknife.Bind;
import butterknife.ButterKnife;

public class ReadingActivity extends AppCompatActivity {

    private static final String TAG = ReadingActivity.class.getSimpleName();

    @Bind(R.id.textViewTitle)
    TextView mTitle;

    @Bind(R.id.textViewBody)
    TextView mBody;

    @Inject PreferenceManager mPreferences;

    private boolean mInvert;
    private String mUrl = null;
    private int mTextSize;
    private ProgressDialog mProgressDialog;
    private Subscription mPageLoaderSubscription;

    private static final float XXLARGE = 30.0f;
    private static final float XLARGE = 26.0f;
    private static final float LARGE = 22.0f;
    private static final float MEDIUM = 18.0f;
    private static final float SMALL = 14.0f;
    private static final float XSMALL = 10.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BrowserApp.getAppComponent().inject(this);

        overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out_scale);
        mInvert = mPreferences.getInvertColors();
        final int color;
        if (mInvert) {
            setTheme(R.style.Theme_SettingsTheme_Dark);
            color = ThemeUtils.getPrimaryColorDark(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        } else {
            setTheme(R.style.Theme_SettingsTheme);
            color = ThemeUtils.getPrimaryColor(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reading_view);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextSize = mPreferences.getReadingTextSize();
        mBody.setTextSize(getTextSize(mTextSize));
        mTitle.setText(getString(R.string.untitled));
        mBody.setText(getString(R.string.loading));

        mTitle.setVisibility(View.INVISIBLE);
        mBody.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        if (!loadPage(intent)) {
            setText(getString(R.string.untitled), getString(R.string.loading_failed));
        }
    }

    private static float getTextSize(int size) {
        switch (size) {
            case 0:
                return XSMALL;
            case 1:
                return SMALL;
            case 2:
                return MEDIUM;
            case 3:
                return LARGE;
            case 4:
                return XLARGE;
            case 5:
                return XXLARGE;
            default:
                return MEDIUM;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reading, menu);
        MenuItem invert = menu.findItem(R.id.invert_item);
        MenuItem textSize = menu.findItem(R.id.text_size_item);
        int iconColor = mInvert ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        if (invert != null && invert.getIcon() != null)
            invert.getIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        if (textSize != null && textSize.getIcon() != null)
            textSize.getIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean loadPage(Intent intent) {
        if (intent == null) {
            return false;
        }
        mUrl = intent.getStringExtra(Constants.LOAD_READING_URL);
        if (mUrl == null) {
            return false;
        }
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(Utils.getDomainName(mUrl));
        mPageLoaderSubscription = loadPage(mUrl).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.main())
                .subscribe(new OnSubscribe<ReaderInfo>() {
                    @Override
                    public void onStart() {
                        mProgressDialog = new ProgressDialog(ReadingActivity.this);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setMessage(getString(R.string.loading));
                        mProgressDialog.show();
                    }

                    @Override
                    public void onNext(@Nullable ReaderInfo item) {
                        if (item == null || item.getTitle().isEmpty() || item.getBody().isEmpty()) {
                            setText(getString(R.string.untitled), getString(R.string.loading_failed));
                        } else {
                            setText(item.getTitle(), item.getBody());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        setText(getString(R.string.untitled), getString(R.string.loading_failed));
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                            mProgressDialog = null;
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                            mProgressDialog = null;
                        }
                    }
                });
        return true;
    }

    private static Observable<ReaderInfo> loadPage(@NonNull final String url) {
        return Observable.create(new Action<ReaderInfo>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<ReaderInfo> subscriber) {
                HtmlFetcher fetcher = new HtmlFetcher();
                try {
                    JResult result = fetcher.fetchAndExtract(url, 2500, true);
                    subscriber.onNext(new ReaderInfo(result.getTitle(), result.getText()));
                } catch (Exception e) {
                    subscriber.onError(new Throwable("Encountered exception"));
                    Log.e(TAG, "Error parsing page", e);
                } catch (OutOfMemoryError e) {
                    System.gc();
                    subscriber.onError(new Throwable("Out of memory"));
                    Log.e(TAG, "Out of memory", e);
                }
                subscriber.onComplete();
            }
        });
    }

    private static class ReaderInfo {
        @NonNull private final String mTitleText;
        @NonNull private final String mBodyText;

        public ReaderInfo(@NonNull String title, @NonNull String body) {
            mTitleText = title;
            mBodyText = body;
        }

        @NonNull
        public String getTitle() {
            return mTitleText;
        }

        @NonNull
        public String getBody() {
            return mBodyText;
        }
    }

    private void setText(String title, String body) {
        if (mTitle == null || mBody == null)
            return;
        if (mTitle.getVisibility() == View.INVISIBLE) {
            mTitle.setAlpha(0.0f);
            mTitle.setVisibility(View.VISIBLE);
            mTitle.setText(title);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mTitle, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mTitle.setText(title);
        }

        if (mBody.getVisibility() == View.INVISIBLE) {
            mBody.setAlpha(0.0f);
            mBody.setVisibility(View.VISIBLE);
            mBody.setText(body);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mBody, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mBody.setText(body);
        }
    }

    @Override
    protected void onDestroy() {
        mPageLoaderSubscription.unsubscribe();

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_out_to_right);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_item:
                mPreferences.setInvertColors(!mInvert);
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, mUrl);
                startActivity(read);
                finish();
                break;
            case R.id.text_size_item:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                View view = inflater.inflate(R.layout.seek_layout, null);
                final SeekBar bar = (SeekBar) view.findViewById(R.id.text_size_seekbar);
                bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar view, int size, boolean user) {
                        mBody.setTextSize(getTextSize(size));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar arg0) {
                    }

                });
                bar.setMax(5);
                bar.setProgress(mTextSize);
                builder.setView(view);
                builder.setTitle(R.string.size);
                builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mTextSize = bar.getProgress();
                        mBody.setTextSize(getTextSize(mTextSize));
                        mPreferences.setReadingTextSize(bar.getProgress());
                    }

                });
                builder.show();
                break;
            default:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
