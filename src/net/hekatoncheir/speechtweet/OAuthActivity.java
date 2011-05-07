/*
 * Copyright (C) 2011 Kouji Ohura
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hekatoncheir.speechtweet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;

public class OAuthActivity extends Activity
{
	public final static String TAG = "net.hekatoncheir.speechtweet";

	private String mOAuthCallbackURL;
	
	private boolean mIsFinish;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mOAuthCallbackURL = getResources().getString(R.string.OAUTH_CALLBACK_URL);
        
        mIsFinish = false;
        
        Intent intent = getIntent();
        String url = intent.getStringExtra("open_url");
        
        WebView webview = new WebView(this);
        webview.setWebChromeClient(new WebChromeClient(){
        	
        });
        webview.setWebViewClient(new WebViewClient() {
        	public void onPageStarted(WebView view, String url, Bitmap favicon)
        	{
        		if( url.indexOf(mOAuthCallbackURL) != 0 )return;
        		if( mIsFinish )return;
        		
        		// auth finish
        		Uri u = Uri.parse(url);
        		String oauth_verifier = u.getQueryParameter("oauth_verifier");
        		
        		// exit intent
        		Intent data = new Intent();
        		data.putExtra("oauth_verifier", oauth_verifier);
        		setResult(RESULT_OK, data);
        		finish();
        		mIsFinish = true;
        	}
        });
        
        webview.loadUrl(url);
        
        setContentView(webview);
    }
    
}
