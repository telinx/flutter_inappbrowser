/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.pichillilorenzo.flutter_inappbrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.provider.Browser;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;

import com.pichillilorenzo.flutter_inappbrowser.ChromeCustomTabs.ChromeCustomTabsActivity;
import com.pichillilorenzo.flutter_inappbrowser.ChromeCustomTabs.CustomTabActivityHelper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * InAppBrowserFlutterPlugin
 */
public class InAppBrowserFlutterPlugin implements MethodCallHandler {

  public Registrar registrar;
  public static MethodChannel channel;
  public static Map<String, InAppBrowserActivity> webViewActivities = new HashMap<>();
  public static Map<String, ChromeCustomTabsActivity> chromeCustomTabsActivities = new HashMap<>();

  protected static final String LOG_TAG = "IABFlutterPlugin";

  public InAppBrowserFlutterPlugin(Registrar r) {
    registrar = r;
    channel = new MethodChannel(registrar.messenger(), "com.pichillilorenzo/flutter_inappbrowser");
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    Activity activity = registrar.activity();
    // registrar.activity() may return null because of Flutter's background execution feature
    // described here: https://medium.com/flutter-io/executing-dart-in-the-background-with-flutter-plugins-and-geofencing-2b3e40a1a124
    if (activity != null) {
      final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.pichillilorenzo/flutter_inappbrowser");
      channel.setMethodCallHandler(new InAppBrowserFlutterPlugin(registrar));

      new MyCookieManager(registrar);

      registrar
              .platformViewRegistry()
              .registerViewFactory(
                      "com.pichillilorenzo/flutter_inappwebview", new FlutterWebViewFactory(registrar));
    }
  }

  @Override
  public void onMethodCall(final MethodCall call, final Result result) {
    String source;
    String urlFile;
    final Activity activity = registrar.activity();
    final String uuid = (String) call.argument("uuid");

    switch (call.method) {
      case "open":
        boolean isData = (boolean) call.argument("isData");
        if (!isData) {
          final String url_final = call.argument("url").toString();

          final boolean useChromeSafariBrowser = (boolean) call.argument("useChromeSafariBrowser");

          final Map<String, String> headers = (Map<String, String>) call.argument("headers");

          Log.d(LOG_TAG, "use Chrome Custom Tabs = " + useChromeSafariBrowser);

          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

              if (useChromeSafariBrowser) {

                final String uuidFallback = (String) call.argument("uuidFallback");

                final HashMap<String, Object> options = (HashMap<String, Object>) call.argument("options");

                final HashMap<String, Object> optionsFallback = (HashMap<String, Object>) call.argument("optionsFallback");

                open(activity, uuid, uuidFallback, url_final, options, headers, true, optionsFallback, result);
              } else {

                String url = url_final;

                final HashMap<String, Object> options = (HashMap<String, Object>) call.argument("options");

                final boolean isLocalFile = (boolean) call.argument("isLocalFile");
                final boolean openWithSystemBrowser = (boolean) call.argument("openWithSystemBrowser");

                if (isLocalFile) {
                  // check if the asset file exists
                  try {
                    url = Util.getUrlAsset(registrar, url);
                  } catch (IOException e) {
                    e.printStackTrace();
                    result.error(LOG_TAG, url + " asset file cannot be found!", e);
                    return;
                  }
                }
                // SYSTEM
                if (openWithSystemBrowser) {
                  Log.d(LOG_TAG, "in system");
                  openExternal(activity, url, result);
                } else {
                  //Load the dialer
                  if (url.startsWith(WebView.SCHEME_TEL)) {
                    try {
                      Log.d(LOG_TAG, "loading in dialer");
                      Intent intent = new Intent(Intent.ACTION_DIAL);
                      intent.setData(Uri.parse(url));
                      activity.startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                      Log.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                    }
                  }
                  // load in InAppBrowserFlutterPlugin
                  else {
                    Log.d(LOG_TAG, "loading in InAppBrowserFlutterPlugin");
                    open(activity, uuid, null, url, options, headers, false, null, result);
                  }
                }
              }
            }
          });
        }
        else {
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              HashMap<String, Object> options = (HashMap<String, Object>) call.argument("options");
              String data = call.argument("data").toString();
              String mimeType = call.argument("mimeType").toString();
              String encoding = call.argument("encoding").toString();
              String baseUrl = call.argument("baseUrl").toString();
              openData(activity, uuid, options, data, mimeType, encoding, baseUrl);
              result.success(true);
            }
          });
        }
        break;
      case "getUrl":
        result.success(getUrl(uuid));
        break;
      case "getTitle":
        result.success(getTitle(uuid));
        break;
      case "getProgress":
        result.success(getProgress(uuid));
        break;
      case "loadUrl":
        loadUrl(uuid, call.argument("url").toString(), (Map<String, String>) call.argument("headers"), result);
        break;
      case "postUrl":
        postUrl(uuid, call.argument("url").toString(), (byte[]) call.argument("postData"), result);
        break;
      case "loadData":
        {
          String data = call.argument("data").toString();
          String mimeType = call.argument("mimeType").toString();
          String encoding = call.argument("encoding").toString();
          String baseUrl = call.argument("baseUrl").toString();
          loadData(uuid, data, mimeType, encoding, baseUrl, result);
        }
        break;
      case "loadFile":
        loadFile(uuid, call.argument("url").toString(), (Map<String, String>) call.argument("headers"), result);
        break;
      case "close":
        close(activity, uuid, result);
        break;
      case "injectScriptCode":
        source = call.argument("source").toString();
        injectScriptCode(uuid, source, result);
        break;
      case "injectScriptFile":
        urlFile = call.argument("urlFile").toString();
        injectScriptFile(uuid, urlFile);
        result.success(true);
        break;
      case "injectStyleCode":
        source = call.argument("source").toString();
        injectStyleCode(uuid, source);
        result.success(true);
        break;
      case "injectStyleFile":
        urlFile = call.argument("urlFile").toString();
        injectStyleFile(uuid, urlFile);
        result.success(true);
        break;
      case "show":
        show(uuid);
        result.success(true);
        break;
      case "hide":
        hide(uuid);
        result.success(true);
        break;
      case "reload":
        reload(uuid);
        result.success(true);
        break;
      case "goBack":
        goBack(uuid);
        result.success(true);
        break;
      case "canGoBack":
        result.success(canGoBack(uuid));
        break;
      case "goForward":
        goForward(uuid);
        result.success(true);
        break;
      case "canGoForward":
        result.success(canGoForward(uuid));
        break;
      case "goBackOrForward":
        goBackOrForward(uuid, (Integer) call.argument("steps"));
        result.success(true);
        break;
      case "canGoBackOrForward":
        result.success(canGoBackOrForward(uuid, (Integer) call.argument("steps")));
        break;
      case "stopLoading":
        stopLoading(uuid);
        result.success(true);
        break;
      case "isLoading":
        result.success(isLoading(uuid));
        break;
      case "isHidden":
        result.success(isHidden(uuid));
        break;
      case "takeScreenshot":
        result.success(takeScreenshot(uuid));
        break;
      case "setOptions":
        {
          String optionsType = (String) call.argument("optionsType");
          switch (optionsType){
            case "InAppBrowserOptions":
              InAppBrowserOptions inAppBrowserOptions = new InAppBrowserOptions();
              HashMap<String, Object> inAppBrowserOptionsMap = (HashMap<String, Object>) call.argument("options");
              inAppBrowserOptions.parse(inAppBrowserOptionsMap);
              setOptions(uuid, inAppBrowserOptions, inAppBrowserOptionsMap);
              break;
            default:
              result.error(LOG_TAG, "Options " + optionsType + " not available.", null);
          }
        }
        result.success(true);
        break;
      case "getOptions":
        result.success(getOptions(uuid));
        break;
      case "getCopyBackForwardList":
        result.success(getCopyBackForwardList(uuid));
        break;
      default:
        result.notImplemented();
    }

  }

  private void injectScriptCode(String uuid, String source, final Result result) {
    final InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      inAppBrowserActivity.injectScriptCode(source, result);
    } else {
      Log.d(LOG_TAG, "webView is null");
    }
  }

  private void injectScriptFile(String uuid, String urlFile) {
    final InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      inAppBrowserActivity.injectScriptFile(urlFile);
    }
  }

  private void injectStyleCode(String uuid, String source) {
    final InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      inAppBrowserActivity.injectStyleCode(source);
    }
  }

  private void injectStyleFile(String uuid, String urlFile) {
    final InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      inAppBrowserActivity.injectStyleFile(urlFile);
    }
  }

  public static String getMimeType(String url) {
    String type = null;
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (extension != null) {
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    return type;
  }

  /**
   * Display a new browser with the specified URL.
   *
   * @param url the url to load.
   * @param result
   * @return "" if ok, or error message.
   */
  public void openExternal(Activity activity, String url, Result result) {
    try {
      Intent intent;
      intent = new Intent(Intent.ACTION_VIEW);
      // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
      // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
      Uri uri = Uri.parse(url);
      if ("file".equals(uri.getScheme())) {
        intent.setDataAndType(uri, getMimeType(url));
      } else {
        intent.setData(uri);
      }
      intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.getPackageName());
      // CB-10795: Avoid circular loops by preventing it from opening in the current app
      this.openExternalExcludeCurrentApp(activity, intent);
      result.success(true);
      // not catching FileUriExposedException explicitly because buildtools<24 doesn't know about it
    } catch (java.lang.RuntimeException e) {
      Log.d(LOG_TAG, url + " cannot be opened: " + e.toString());
      result.error(LOG_TAG, url + " cannot be opened!", null);
    }
  }

  /**
   * Opens the intent, providing a chooser that excludes the current app to avoid
   * circular loops.
   */
  private void openExternalExcludeCurrentApp(Activity activity, Intent intent) {
    String currentPackage = activity.getPackageName();
    boolean hasCurrentPackage = false;
    PackageManager pm = activity.getPackageManager();
    List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
    ArrayList<Intent> targetIntents = new ArrayList<Intent>();
    for (ResolveInfo ri : activities) {
      if (!currentPackage.equals(ri.activityInfo.packageName)) {
        Intent targetIntent = (Intent) intent.clone();
        targetIntent.setPackage(ri.activityInfo.packageName);
        targetIntents.add(targetIntent);
      } else {
        hasCurrentPackage = true;
      }
    }
    // If the current app package isn't a target for this URL, then use
    // the normal launch behavior
    if (!hasCurrentPackage || targetIntents.size() == 0) {
      activity.startActivity(intent);
    }
    // If there's only one possible intent, launch it directly
    else if (targetIntents.size() == 1) {
      activity.startActivity(targetIntents.get(0));
    }
    // Otherwise, show a custom chooser without the current app listed
    else if (targetIntents.size() > 0) {
      Intent chooser = Intent.createChooser(targetIntents.remove(targetIntents.size() - 1), null);
      chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[]{}));
      activity.startActivity(chooser);
    }
  }

  public void open(Activity activity, String uuid, String uuidFallback, String url, HashMap<String, Object> options, Map<String, String> headers, boolean useChromeSafariBrowser, HashMap<String, Object> optionsFallback, Result result) {

    Intent intent = null;
    Bundle extras = new Bundle();
    extras.putString("fromActivity", activity.getClass().getName());
    extras.putString("url", url);
    extras.putBoolean("isData", false);
    extras.putString("uuid", uuid);
    extras.putSerializable("options", options);
    extras.putSerializable("headers", (Serializable) headers);

    if (useChromeSafariBrowser && CustomTabActivityHelper.isAvailable(activity)) {
      intent = new Intent(activity, ChromeCustomTabsActivity.class);
    }
    // check for webview fallback
    else if (useChromeSafariBrowser && !CustomTabActivityHelper.isAvailable(activity) && !uuidFallback.isEmpty()) {
      Log.d(LOG_TAG, "WebView fallback declared.");
      // overwrite with extras fallback parameters
      extras.putString("uuid", uuidFallback);
      if (optionsFallback != null)
        extras.putSerializable("options", optionsFallback);
      else
        extras.putSerializable("options", (new InAppBrowserOptions()).getHashMap());
      extras.putSerializable("headers", (Serializable) headers);
      intent = new Intent(activity, InAppBrowserActivity.class);
    }
    // native webview
    else if (!useChromeSafariBrowser) {
      intent = new Intent(activity, InAppBrowserActivity.class);
    }
    else {
      Log.d(LOG_TAG, "No WebView fallback declared.");
    }

    if (intent != null) {
      intent.putExtras(extras);
      activity.startActivity(intent);
      result.success(true);
      return;
    }

    result.error(LOG_TAG, "No WebView fallback declared.", null);
  }

  public void openData(Activity activity, String uuid, HashMap<String, Object> options, String data, String mimeType, String encoding, String baseUrl) {
    Intent intent = new Intent(activity, InAppBrowserActivity.class);
    Bundle extras = new Bundle();

    extras.putBoolean("isData", true);
    extras.putString("uuid", uuid);
    extras.putSerializable("options", options);
    extras.putString("data", data);
    extras.putString("mimeType", mimeType);
    extras.putString("encoding", encoding);
    extras.putString("baseUrl", baseUrl);

    intent.putExtras(extras);
    activity.startActivity(intent);
  }

  private String getUrl(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.getUrl();
    return null;
  }

  private String getTitle(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.getWebViewTitle();
    return null;
  }

  private Integer getProgress(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.getProgress();
    return null;
  }

  public void loadUrl(String uuid, String url, Map<String, String> headers, Result result) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      if (headers != null)
        inAppBrowserActivity.loadUrl(url, headers, result);
      else
        inAppBrowserActivity.loadUrl(url, result);
    }
  }

  public void postUrl(String uuid, String url, byte[] postData, Result result) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.postUrl(url, postData, result);
  }

  public void loadData(String uuid, String data, String mimeType, String encoding, String baseUrl, Result result) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.loadData(data, mimeType, encoding, baseUrl, result);
  }

  public void loadFile(String uuid, String url, Map<String, String> headers, Result result) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      if (headers != null)
        inAppBrowserActivity.loadFile(url, headers, result);
      else
        inAppBrowserActivity.loadFile(url, result);
    }
  }

  public void show(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.show();
  }

  public void hide(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.hide();
  }

  public void reload(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.reload();
  }

  public boolean isLoading(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.isLoading();
    return false;
  }

  public boolean isHidden(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.isHidden;
    return false;
  }

  public void stopLoading(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.stopLoading();
  }

  public void goBack(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.goBack();
  }

  public boolean canGoBack(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.canGoBack();
    return false;
  }

  public void goForward(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.goForward();
  }

  public boolean canGoForward(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.canGoForward();
    return false;
  }

  public void goBackOrForward(String uuid, int steps) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.goBackOrForward(steps);
  }

  public boolean canGoBackOrForward(String uuid, int steps) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.canGoBackOrForward(steps);
    return false;
  }

  public static void close(Activity activity, final String uuid, final Result result) {
    final InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {

          Map<String, Object> obj = new HashMap<>();
          obj.put("uuid", uuid);
          channel.invokeMethod("onExit", obj);

          // The JS protects against multiple calls, so this should happen only when
          // close() is called by other native code.
          if (inAppBrowserActivity == null) {
            if (result != null) {
              result.success(true);
            }
            return;
          }

          inAppBrowserActivity.webView.setWebViewClient(new WebViewClient() {
            // NB: wait for about:blank before dismissing
            public void onPageFinished(WebView view, String url) {
              inAppBrowserActivity.close();
            }
          });

//          inAppBrowserActivity.close();
//          return;

          // NB: From SDK 19: "If you call methods on WebView from any thread
          // other than your app's UI thread, it can cause unexpected results."
          // http://developer.android.com/guide/webapps/migrating.html#Threads

           inAppBrowserActivity.webView.loadUrl("about:blank");
           if (result != null) {
             result.success(true);
           }
        }
      });
    }
    else if (result != null) {
      result.success(true);
    }
  }

  public byte[] takeScreenshot(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.takeScreenshot();
    return null;
  }

  public void setOptions(String uuid, InAppBrowserOptions options, HashMap<String, Object> optionsMap) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      inAppBrowserActivity.setOptions(options, optionsMap);
  }

  public HashMap<String, Object> getOptions(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.getOptions();
    return null;
  }

  public HashMap<String, Object> getCopyBackForwardList(String uuid) {
    InAppBrowserActivity inAppBrowserActivity = webViewActivities.get(uuid);
    if (inAppBrowserActivity != null)
      return inAppBrowserActivity.getCopyBackForwardList();
    return null;
  }

}
