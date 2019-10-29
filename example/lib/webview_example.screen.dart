import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_inappbrowser/flutter_inappbrowser.dart';

class MyInappBrowser extends InAppBrowser {

 @override
 Future onBrowserCreated() async {
  //  print("\n\nBrowser Ready!\n\n");
 }

 @override
 Future onLoadStart(String url) async {
   print("\n\nStarted $url\n\n");
 }

 @override
 Future onLoadStop(String url) async {
  //  print("\n\nStopped $url\n\n");
 }

 @override
 Future onScrollChanged(int x, int y) async {
  //  print("Scrolled: x:$x y:$y");
 }

 @override
 void onLoadError(String url, int code, String message) {
  //  print("Can't load $url.. Error: $message");
 }

 @override
 void onProgressChanged(int progress) {
  //  print("Progress: $progress");
 }

 @override
 void onExit() {
  //  print("\n\nBrowser closed!\n\n");
 }

 @override
 void shouldOverrideUrlLoading(String url) {
  //  print("\n\n override $url\n\n");
   this.webViewController.loadUrl(url);
 }

 @override
 void onLoadResource(WebResourceResponse response, WebResourceRequest request) {
  //  print("Started at: " +
  //      response.startTime.toString() +
  //      "ms ---> duration: " +
  //      response.duration.toString() +
  //      "ms " +
      //  response.url);
 }

 @override
 void onConsoleMessage(ConsoleMessage consoleMessage) {
  //  print("""
  //   console output:
  //     sourceURL: ${consoleMessage.sourceURL}
  //     lineNumber: ${consoleMessage.lineNumber}
  //     message: ${consoleMessage.message}
  //     messageLevel: ${consoleMessage.messageLevel}
  //  """);
 }
  
}

class WebviewExampleScreen extends StatefulWidget {
  final MyInappBrowser browser = new MyInappBrowser();
  @override
  _WebviewExampleScreenState createState() => new _WebviewExampleScreenState();
}

class _WebviewExampleScreenState extends State<WebviewExampleScreen> {
  static InAppBrowser browser = InAppBrowser();
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new Center(
      child: new RaisedButton(
          onPressed: ()  {
            widget.browser.open(
              // browser.open(
                url: "https://36kr.com/p/5256612",
                // url: "https://36kr.com",
                // url: "https://okmagazine.com/photos/brittany-aldean-emotional-tribute-grandmother-dog-died-same-day-instagram/",
                // url: "https://www.teenvogue.com/story/fans-mourning-loss-cameron-boyce",
                // url: "http://www.kidsir.com/",
                // url: "https://okmagazine.com/photos/eva-longoria-felicity-huffman-college-scandal-handled-with-grace/",
                options: {
                  "useShouldOverrideUrlLoading": true,
                  "useOnLoadResource": true,
                  "databaseEnabled": true,
                  "domStorageEnabled": true,
                  "safeBrowsingEnabled":false
                }
            );
          },
          child: Text("Open Webview Browser")),
    );
  }
}
