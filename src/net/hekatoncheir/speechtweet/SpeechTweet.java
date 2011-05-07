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

import java.util.ArrayList;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class SpeechTweet extends Activity {
	public final static String TAG = "net.hekatoncheir.speechtweet";
	
	private final static int ACTIVITY_RESULT_OAUTH = 0;

	private Handler mHandler = new Handler();
	
    private Twitter mTwitter;
	private RequestToken mRequestToken;
	
	private String mOAuthCallbackURL; 
	private String mOAuthConsumerKey; 
	private String mOAuthConsumerSecret; 
	
	private boolean mIsOpenOAuth;
	SpeechRecognizer mSpeechRecognizer = null;
	
	private boolean mIsVerifyCredentials;
	MediaPlayer mRingPlayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mIsOpenOAuth = false;
        mIsVerifyCredentials = false;
        
        mOAuthConsumerKey = getResources().getString(R.string.OAUTH_CONSUMER_KEY);
        mOAuthConsumerSecret = getResources().getString(R.string.OAUTH_CONSUMER_SECRET);
        mOAuthCallbackURL = getResources().getString(R.string.OAUTH_CALLBACK_URL);
        
    }
	
    public void onStart()
    {
    	super.onStart();
    	
    	Log.d(TAG, "onStart");
    	
    	if( mIsOpenOAuth ){
    		mIsOpenOAuth = false;
    		return;
    	}
    	
        if( !verifyCredentials() ){
        	startOAuth();
        }
    	
    }
    public void onStop()
    {
    	super.onStop();
    	finalizeSpeechRecognizer();
    	
    	Log.d(TAG, "onStop");
    }
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if( requestCode == ACTIVITY_RESULT_OAUTH ){
			if( resultCode == RESULT_OK){
				getAccessToken(data.getStringExtra("oauth_verifier"));
			}
		}
	}
	
	private void finalizeSpeechRecognizer()
	{
		if( mSpeechRecognizer != null){
			mSpeechRecognizer.cancel();
			mSpeechRecognizer.stopListening();
			mSpeechRecognizer.destroy();
			mSpeechRecognizer = null;			
		}
	}
	
	
	private void startRecognize(final boolean isRetry)
	{
		finalizeSpeechRecognizer();

		MediaPlayer mp;
		if( isRetry ){
			mp = MediaPlayer.create(SpeechTweet.this, R.raw.oncemore);
		}else{
			mp = MediaPlayer.create(SpeechTweet.this, R.raw.lets_tweet);
		}
		mp.setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				SpeechTweet.this.startRecognizeReal(isRetry);
			}
		});
		mp.start();
		
	}
	
	private void startRecognizeReal(final boolean isRetry)
	{
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecognizer.setRecognitionListener(new RecognitionListener(){
			@Override public void onBeginningOfSpeech() {
				Log.d(TAG, "onBeginningOfSpeech");
			}

			@Override public void onBufferReceived(byte[] arg0) {
//				Log.d(TAG, "onBufferReceived");			
			}

			@Override public void onEndOfSpeech() {
				Log.d(TAG, "onEndOfSpeech");
			}

			@Override public void onError(int error) {
				
				String message = "SpeechRecognizer ";
				switch(error){
				case SpeechRecognizer.ERROR_AUDIO:
					message += "Audio recording error.";
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					message += "Other client side errors.";
					break;
				case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
					message += "Insufficient permissions";
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					message += "Other network related errors.";
					break;
				case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
					message += "Network operation timed out.";
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					message += "No recognition result matched.";
					break;
				case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
					message += "No speech input";
					break;
				case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
					message += "RecognitionService busy.";
					break;
				case SpeechRecognizer.ERROR_SERVER:
					message += "Server sends error status.";
					break;
				}
				
				if(	!isRetry && 
 					(error != SpeechRecognizer.ERROR_NO_MATCH 
 							|| error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) )
				{
					SpeechTweet.this.startRecognize(true);
					
				}
				
				Log.d(TAG, message);
				SpeechTweet.this.showError(message);
				
			}

			@Override public void onEvent(int arg0, Bundle arg1) {
				Log.d(TAG, "onEvent");
			}

			@Override public void onPartialResults(Bundle arg0) {
				Log.d(TAG, "onPartialResults");
			}

			@Override public void onReadyForSpeech(Bundle arg0) {
				Log.d(TAG, "onReadyForSpeech");
			}

			@Override public void onResults(Bundle bundle){
				Log.d(TAG, "onResults");
				String message = "";
				ArrayList<String> st = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				if( st.size() > 0 ){
					message = st.get(0);
				}
				finishVoiceRecognize(message);
			}

			@Override public void onRmsChanged(float arg0) {
//				Log.d(TAG, "onRmsChanged");
			}
        });
        
        Intent intent = RecognizerIntent.getVoiceDetailsIntent(this);
        mSpeechRecognizer.startListening(intent);
        
    }

	
	private boolean verifyCredentials()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String accessToken = sharedPreferences.getString("accessToken", null);
		String accessTokenSecret = sharedPreferences.getString("accessTokenSecret", null);
		if( accessToken == null || accessTokenSecret == null)return false;
		
		// Read configuration
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
			.setOAuthConsumerKey(mOAuthConsumerKey)
			.setOAuthConsumerSecret(mOAuthConsumerSecret)
			.setOAuthAccessToken(accessToken)
			.setOAuthAccessTokenSecret(accessTokenSecret);
		
		//
		mIsVerifyCredentials = true;
		mRingPlayer = MediaPlayer.create(SpeechTweet.this, R.raw.ring);
		mRingPlayer.setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				if( mIsVerifyCredentials ){
					mRingPlayer.start();
				}
			}
		});
		mRingPlayer.start();
		
			
		TwitterFactory factory = new TwitterFactory(cb.build());
		mTwitter = factory.getInstance();
		new Thread(new Runnable(){
			@Override
			public void run() {
				User user = null;
				try {
					user = mTwitter.verifyCredentials();
				} catch (TwitterException e) {
				}
				final User u = user;
				mHandler.post(new Runnable() { public void run() {
					SpeechTweet.this.finishVerifyCredentials(u);
				}});
			}
		}).start();
		return true;
	}
	public void finishVerifyCredentials(User user)
	{
		if( mRingPlayer != null){
			mRingPlayer.stop();
			mRingPlayer = null;
		}
		mIsVerifyCredentials = false;
		
		if( user != null){
			startRecognize(false);
		}
	}

	private void startOAuth()
    {
		mIsOpenOAuth = true;
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(mOAuthConsumerKey)
		  .setOAuthConsumerSecret(mOAuthConsumerSecret);
       
		TwitterFactory factory = new TwitterFactory(cb.build());
		mTwitter = factory.getInstance();

		new Thread(new Runnable(){
			@Override
			public void run() {
				Boolean isSuccess = false;
				try {
					mRequestToken = mTwitter.getOAuthRequestToken(mOAuthCallbackURL);
					isSuccess = true;
				} catch (TwitterException e) {
				}
				final Boolean s = isSuccess;
				mHandler.post(new Runnable() { public void run() {
					SpeechTweet.this.finishGetOAuthRequestToken(s);
				}});
			}
		}).start();
    }
	
	public void finishGetOAuthRequestToken(Boolean isSuccess)
	{
		if( isSuccess ){
	        Intent intent = new Intent(this, OAuthActivity.class);
	        intent.putExtra("open_url", mRequestToken.getAuthorizationURL());
	
	        startActivityForResult(intent, ACTIVITY_RESULT_OAUTH);
		}else{
			showError("An error occurred during authentication.");
		}
	}
	
	private void getAccessToken(final String oauth_verifier)
	{
		new Thread(new Runnable(){
			@Override
			public void run() {
				AccessToken at = null;
				try {
					at = mTwitter.getOAuthAccessToken(oauth_verifier);
				} catch (TwitterException e) {
				}
				final AccessToken a = at;
				mHandler.post(new Runnable() { public void run() {
					SpeechTweet.this.finishGetOAuthAccessToken(a);
				}});
			}
		}).start();
	}
	public void finishGetOAuthAccessToken(AccessToken at)
	{
		if( at == null){
			return;
		}
		
		// success
		String accessToken = at.getToken();
		String accessTokenSecret = at.getTokenSecret();
		
		// save auth parameter
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("accessToken", accessToken);
		editor.putString("accessTokenSecret", accessTokenSecret);
		editor.commit();
		
		verifyCredentials();
//		startRecognize(false);
	}
	
	//
	private void finishVoiceRecognize(String message)
	{
		// I—¹‚·‚é
		finalizeSpeechRecognizer();
		
		String plusMessage = " //tweet with speech recognition";
		
		int len = message.length() + plusMessage.length();
		if( len > 140){
			message = message.substring(0, 140-plusMessage.length());
		}
		message += plusMessage;
		
		final StatusUpdate status = new StatusUpdate(message);
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				Boolean isSuccess = false;
				try {
					mTwitter.updateStatus(status);
					isSuccess = true;
				} catch (TwitterException e) {
				}
				final Boolean s = isSuccess;
				mHandler.post(new Runnable() { public void run() {
					SpeechTweet.this.finishTweet(s);
				}});
			}
		}).start();
		
	}
	
	private void finishTweet(boolean isSuccess)
	{
		MediaPlayer mp = MediaPlayer.create(this, R.raw.complete);
		mp.setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				// Exit
				SpeechTweet.this.finish();
			}
		});
		mp.start();
	}
	private void showError(String message)
	{
		MediaPlayer mp = MediaPlayer.create(this, R.raw.error);
		mp.start();
		
		// Show ErrorDialog
		{
			new AlertDialog.Builder(this)
				.setTitle("Error")
				.setMessage(message)
		        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	SpeechTweet.this.finish();
	                    }
                }).create().show();
		}
	}

}