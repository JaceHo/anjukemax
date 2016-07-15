package com.anjuke.max58.app;

import com.anjuke.max58.activity.BrowserActivity;
import com.anjuke.max58.activity.ReadingActivity;
import com.anjuke.max58.activity.TabsManager;
import com.anjuke.max58.activity.ThemableBrowserActivity;
import com.anjuke.max58.activity.ThemableSettingsActivity;

import javax.inject.Singleton;

import acr.browser.lightning.browser.BrowserPresenter;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.download.LightningDownloadListener;
import acr.browser.lightning.fragment.BookmarkSettingsFragment;
import acr.browser.lightning.fragment.BookmarksFragment;
import acr.browser.lightning.fragment.DebugSettingsFragment;
import acr.browser.lightning.fragment.LightningPreferenceFragment;
import acr.browser.lightning.fragment.PrivacySettingsFragment;
import acr.browser.lightning.fragment.TabsFragment;
import acr.browser.lightning.search.SuggestionsAdapter;
import acr.browser.lightning.utils.AdBlock;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.view.LightningView;
import acr.browser.lightning.view.LightningWebClient;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(SuggestionsAdapter adapter);

    void inject(LightningDialogBuilder builder);

    void inject(TabsFragment fragment);

    void inject(LightningView lightningView);

    void inject(ThemableBrowserActivity activity);

    void inject(LightningPreferenceFragment fragment);

    void inject(BrowserApp app);

    void inject(ProxyUtils proxyUtils);

    void inject(ReadingActivity activity);

    void inject(LightningWebClient webClient);

    void inject(ThemableSettingsActivity activity);

    void inject(AdBlock adBlock);

    void inject(LightningDownloadListener listener);

    void inject(PrivacySettingsFragment fragment);

    void inject(StartPage startPage);

    void inject(BrowserPresenter presenter);

    void inject(TabsManager manager);

    void inject(DebugSettingsFragment fragment);

}
