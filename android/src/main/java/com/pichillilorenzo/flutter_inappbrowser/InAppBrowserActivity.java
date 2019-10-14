package com.pichillilorenzo.flutter_inappbrowser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.pichillilorenzo.flutter_inappbrowser.InAppWebView.InAppWebView;
import com.pichillilorenzo.flutter_inappbrowser.InAppWebView.InAppWebViewOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.app.FlutterActivity;
import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodChannel;

public class InAppBrowserActivity extends AppCompatActivity {

    static final String LOG_TAG = "InAppBrowserActivity";
    public String uuid;
    public InAppWebView webView;
    public ActionBar actionBar;
    public Menu menu;
    public SearchView searchView;
    public InAppBrowserOptions options;
    public Map<String, String> headers;
    public ProgressBar progressBar;
    public boolean isHidden = false;
    public String fromActivity;
    public ImageView ivClose, ivMore, ivBack, ivForward;
    public RelativeLayout back_layout, forward_layout;
    public TextView tvTitle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_web_view);

    webView = findViewById(R.id.webView);
    webView.inAppBrowserActivity = this;

    Bundle b = getIntent().getExtras();
    uuid = b.getString("uuid");
    fromActivity = b.getString("fromActivity");

    HashMap<String, Object> optionsMap = (HashMap<String, Object>) b.getSerializable("options");

    options = new InAppBrowserOptions();
    options.parse(optionsMap);

    InAppWebViewOptions webViewOptions = new InAppWebViewOptions();
    webViewOptions.parse(optionsMap);
    webView.options = webViewOptions;

    InAppBrowserFlutterPlugin.webViewActivities.put(uuid, this);

    actionBar = getSupportActionBar();

    initView();
    prepareView();
    actionBar.hide();
    Boolean isData = b.getBoolean("isData");
    if (!isData) {
      headers = (HashMap<String, String>) b.getSerializable("headers");
      String url = b.getString("url");
      webView.loadUrl(url, headers);
    }
    else {
      String data = b.getString("data");
      String mimeType = b.getString("mimeType");
      String encoding = b.getString("encoding");
      String baseUrl = b.getString("baseUrl");
      webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, null);
    }

    Map<String, Object> obj = new HashMap<>();
    obj.put("uuid", uuid);
    InAppBrowserFlutterPlugin.channel.invokeMethod("onBrowserCreated", obj);


//    //这里注意下 因为在评论区发现有网友调用setRootViewFitsSystemWindows 里面 winContent.getChildCount()=0 导致代码无法继续
//    //是因为你需要在setContentView之后才可以调用 setRootViewFitsSystemWindows
//    //当FitsSystemWindows设置 true 时，会在屏幕最上方预留出状态栏高度的 padding
//    StatusBarUtil.setRootViewFitsSystemWindows(this,true);
//    //设置状态栏透明
//    StatusBarUtil.setTranslucentStatus(this);
//    //一般的手机的状态栏文字和图标都是白色的, 可如果你的应用也是纯白色的, 或导致状态栏文字看不清
//    //所以如果你是这种情况,请使用以下代码, 设置状态使用深色文字图标风格, 否则你可以选择性注释掉这个if内容
//    if (!StatusBarUtil.setStatusBarDarkTheme(this, true)) {
//      //如果不支持设置深色风格 为了兼容总不能让状态栏白白的看不清, 于是设置一个状态栏颜色为半透明,
//      //这样半透明+白=灰, 状态栏的文字能看得清
//      StatusBarUtil.setStatusBarColor(this,0x55000000);
//    }

  }

    private void initView() {
      tvTitle = (TextView) findViewById(R.id.tv_title);
      ivClose = (ImageView) findViewById(R.id.iv_close);
      ivMore = (ImageView) findViewById(R.id.iv_more);
      ivBack = (ImageView) findViewById(R.id.iv_back);
      ivForward = (ImageView) findViewById(R.id.iv_forward);
      back_layout = (RelativeLayout) findViewById(R.id.back_layout);
      forward_layout = (RelativeLayout) findViewById(R.id.forward_layout);
      ivClose.setOnClickListener(onClickListener);
      ivMore.setOnClickListener(onClickListener);
      back_layout.setOnClickListener(onClickListener);
      forward_layout.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener =new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.iv_close) {
              InAppBrowserFlutterPlugin.close(InAppBrowserActivity.this, uuid, null);
            } else if (i == R.id.iv_more) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                startActivity(Intent.createChooser(share, "Share"));
            } else if (i == R.id.back_layout) {
                goBack();
            } else if (i == R.id.forward_layout) {
                goForward();
            }
        }
    };



  private void prepareView() {

    webView.prepare();

    if (options.hidden)
      hide();
    else
      show();

    progressBar = findViewById(R.id.progressBar);
    progressBar.getProgressDrawable().setColorFilter(Color.rgb(252, 66, 169), PorterDuff.Mode.SRC_IN);
    if (!options.progressBar)
      progressBar.setMax(0);
    else
      progressBar.setMax(100);

    actionBar.setDisplayShowTitleEnabled(!options.hideTitleBar);

    if (!options.toolbarTop)
      actionBar.hide();

    if (!options.toolbarTopBackgroundColor.isEmpty())
      actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(options.toolbarTopBackgroundColor)));

    if (!options.toolbarTopFixedTitle.isEmpty())
      actionBar.setTitle(options.toolbarTopFixedTitle);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu m) {
    menu = m;

    MenuInflater inflater = getMenuInflater();
    // Inflate menu to add items to action bar if it is present.
    inflater.inflate(R.menu.menu_main, menu);

    searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
    searchView.setFocusable(true);

    if (options.hideUrlBar)
      menu.findItem(R.id.menu_search).setVisible(false);

    searchView.setQuery(webView.getUrl(), false);

    if (options.toolbarTopFixedTitle.isEmpty())
      actionBar.setTitle(webView.getTitle());

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        if (!query.isEmpty()) {
          webView.loadUrl(query);
          searchView.setQuery("", false);
          searchView.setIconified(true);
          return true;
        }
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }

    });

    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        if (searchView.getQuery().toString().isEmpty())
          searchView.setQuery(webView.getUrl(), false);
        return false;
      }
    });

    searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean b) {
        if (!b) {
          searchView.setQuery("", false);
          searchView.setIconified(true);
        }
      }
    });

    return true;
  }

  public String getUrl() {
    if (webView != null)
      return webView.getUrl();
    return null;
  }

  public String getWebViewTitle() {
    if (webView != null)
      return webView.getTitle();
    return null;
  }

  public Integer getProgress() {
    if (webView != null)
      return webView.getProgress();
    return null;
  }

  public void loadUrl(String url, MethodChannel.Result result) {
    if (webView != null) {
      webView.loadUrl(url, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public void loadUrl(String url, Map<String, String> headers, MethodChannel.Result result) {
    if (webView != null) {
      webView.loadUrl(url, headers, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public void postUrl(String url, byte[] postData, MethodChannel.Result result) {
    if (webView != null) {
      webView.postUrl(url, postData, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public void loadData(String data, String mimeType, String encoding, String baseUrl, MethodChannel.Result result) {
    if (webView != null) {
      webView.loadData(data, mimeType, encoding, baseUrl, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public void loadFile(String url, MethodChannel.Result result) {
    if (webView != null) {
      webView.loadFile(url, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public void loadFile(String url, Map<String, String> headers, MethodChannel.Result result) {
    if (webView != null) {
      webView.loadFile(url, headers, result);
    } else {
      result.error(LOG_TAG, "webView is null", null);
    }
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
      if (canGoBack())
        goBack();
      else if (options.closeOnCannotGoBack)
        InAppBrowserFlutterPlugin.close(this, uuid, null);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void close() {
    hide();
    finish();
  }

  public void reload() {
    if (webView != null)
      webView.reload();
  }

  public void goBack() {
    if (webView != null && canGoBack())
      webView.goBack();
  }

  public boolean canGoBack() {
    if (webView != null)
      return webView.canGoBack();
    return false;
  }

  public void goForward() {
    if (webView != null && canGoForward())
      webView.goForward();
  }

  public boolean canGoForward() {
    if (webView != null)
      return webView.canGoForward();
    return false;
  }

  public void goBackOrForward(int steps) {
    if (webView != null && canGoBackOrForward(steps))
      webView.goBackOrForward(steps);
  }

  public boolean canGoBackOrForward(int steps) {
    if (webView != null)
      return webView.canGoBackOrForward(steps);
    return false;
  }

  public void hide() {
    try {
      isHidden = true;
      Intent openActivity = new Intent(this, Class.forName(fromActivity));
      openActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      startActivityIfNeeded(openActivity, 0);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      Log.d(LOG_TAG, e.getMessage());
    }
  }

  public void show() {
    isHidden = false;
    Intent openActivity = new Intent(this, InAppBrowserActivity.class);
    openActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    startActivityIfNeeded(openActivity, 0);
  }

  public void stopLoading() {
    if (webView != null)
      webView.stopLoading();
  }

  public boolean isLoading() {
    if (webView != null)
      return webView.isLoading;
    return false;
  }

  public void goBackButtonClicked(MenuItem item) {
    goBack();
  }

  public void goForwardButtonClicked(MenuItem item) {
    goForward();
  }

  public void shareButtonClicked(MenuItem item) {
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("text/plain");
    share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
    startActivity(Intent.createChooser(share, "Share"));
  }

  public void reloadButtonClicked(MenuItem item) {
    reload();
  }

  public void closeButtonClicked(MenuItem item) {
    InAppBrowserFlutterPlugin.close(this, uuid, null);
  }

  public byte[] takeScreenshot() {
    if (webView != null) {
      Picture picture = webView.capturePicture();
      Bitmap b = Bitmap.createBitmap( webView.getWidth(),
              webView.getHeight(), Bitmap.Config.ARGB_8888);
      Canvas c = new Canvas(b);

      picture.draw(c);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      b.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
      try {
        byteArrayOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return byteArrayOutputStream.toByteArray();
    }
    return null;
  }

  public void setOptions(InAppBrowserOptions newOptions, HashMap<String, Object> newOptionsMap) {

    InAppWebViewOptions newInAppWebViewOptions = new InAppWebViewOptions();
    newInAppWebViewOptions.parse(newOptionsMap);
    webView.setOptions(newInAppWebViewOptions, newOptionsMap);

    if (newOptionsMap.get("hidden") != null && options.hidden != newOptions.hidden) {
      if (newOptions.hidden)
        hide();
      else
        show();
    }

    if (newOptionsMap.get("progressBar") != null && options.progressBar != newOptions.progressBar && progressBar != null) {
      if (newOptions.progressBar)
        progressBar.setMax(0);
      else
        progressBar.setMax(100);
    }

    if (newOptionsMap.get("hideTitleBar") != null && options.hideTitleBar != newOptions.hideTitleBar)
      actionBar.setDisplayShowTitleEnabled(!newOptions.hideTitleBar);

    if (newOptionsMap.get("toolbarTop") != null && options.toolbarTop != newOptions.toolbarTop) {
      if (!newOptions.toolbarTop)
        actionBar.hide();
      else
        actionBar.show();
    }

    if (newOptionsMap.get("toolbarTopBackgroundColor") != null && options.toolbarTopBackgroundColor != newOptions.toolbarTopBackgroundColor && !newOptions.toolbarTopBackgroundColor.isEmpty())
      actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(newOptions.toolbarTopBackgroundColor)));

    if (newOptionsMap.get("toolbarTopFixedTitle") != null && options.toolbarTopFixedTitle != newOptions.toolbarTopFixedTitle && !newOptions.toolbarTopFixedTitle.isEmpty())
      actionBar.setTitle(newOptions.toolbarTopFixedTitle);

    if (newOptionsMap.get("hideUrlBar") != null && options.hideUrlBar != newOptions.hideUrlBar) {
      if (newOptions.hideUrlBar)
        menu.findItem(R.id.menu_search).setVisible(false);
      else
        menu.findItem(R.id.menu_search).setVisible(true);
    }

    options = newOptions;
  }

  public HashMap<String, Object> getOptions() {
    HashMap<String, Object> webViewOptionsMap = webView.getOptions();
    if (options == null || webViewOptionsMap == null)
      return null;

    HashMap<String, Object> optionsMap = options.getHashMap();
    optionsMap.putAll(webViewOptionsMap);
    return optionsMap;
  }

  public void injectScriptCode(String source, MethodChannel.Result result) {
    if (webView != null)
      webView.injectScriptCode(source, result);
    else
      result.success("");
  }

  public void injectScriptFile(String urlFile) {
    if (webView != null)
      webView.injectScriptFile(urlFile);
  }

  public void injectStyleCode(String source) {
    if (webView != null)
      webView.injectStyleCode(source);
  }

  public void injectStyleFile(String urlFile) {
    if (webView != null)
      webView.injectStyleFile(urlFile);
  }

  public HashMap<String, Object> getCopyBackForwardList() {
    if (webView != null)
      return webView.getCopyBackForwardList();
    return null;
  }

}