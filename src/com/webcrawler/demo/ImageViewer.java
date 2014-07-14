package com.webcrawler.demo;


import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ImageViewer extends Activity {
	
	int memenumber=0;
	ImageView imageView;
	TextView textView;
	SeekBar seekbar;
	LocalBroadcastManager mLocalBroadcastManager;
	BroadcastReceiver broadcastReceiver;
	ProgressBar pb;
	TextView progressnumber;
	
	//needed to override onResume method to have UI thread updated by receiving messages from service
	@Override
	public void onResume(){
		super.onResume();
		IntentFilter filter = new IntentFilter();
	    filter.addAction("UPDATEUI");
	    mLocalBroadcastManager.registerReceiver(broadcastReceiver, filter);
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		SharedPreferences webcrawler = getSharedPreferences("webcrawler",0);
			
		Variables.pics=webcrawler.getInt("pics", 0);
		String titles=webcrawler.getString("titles","");
			
		if(titles.length()>0){
			String delims="[||]";
			
		String[] names=titles.split(delims);
			
		for(int i=0;i<names.length;i++)
			if(names[i].length()>0)
				Variables.titles.add(names[i]);
		}
			
		Variables.hour=webcrawler.getInt("hour",0);
					
		Variables.minute= webcrawler.getInt("minute", 0);
			
		Variables.imagenumber= webcrawler.getString("imagenumber","25");
			
		    //Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_imageviewer);
		
		imageView=(ImageView)findViewById(R.id.mainDisplay);
		textView=(TextView)findViewById(R.id.textView1);
		progressnumber=(TextView)findViewById(R.id.progressnumber);
		pb=(ProgressBar)findViewById(R.id.pbHeaderProgress);
		
		//set fancy fonts
		Typeface font = Typeface.createFromAsset(getAssets(), "Ormont-Light.otf");
		Typeface font2 = Typeface.createFromAsset(getAssets(), "Langdon.otf");
		textView.setTypeface(font2);
		progressnumber.setTypeface(font);
		textView.setTypeface(font2);
		
		FileInputStream fin = null;
		memenumber=0;
		
		//
		if(Variables.pics>0){
			
			//if there are pictures available, load the first one

			try {
				fin = openFileInput("Meme0.jpg");
				if(fin !=null && fin.available() > 0) {
					Bitmap bmp=BitmapFactory.decodeStream(fin); 
					imageView.setImageBitmap(bmp);
					textView.setText(Variables.titles.get(0));
				} 
				else {
	            //input stream has not much data to convert into  Bitmap
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//otherwise, set the main image and text to indicate it's empty
		else{
			textView.setText("YOUR CUP IS EMPTY!");
			imageView.setImageResource(R.drawable.plusifnone);
		}
		
		//setup seekbar
		seekbar = (SeekBar) findViewById(R.id.seek1);
		seekbar.setMax(Variables.pics-1);
		
		//if a download is currently not in progress, hide the progressbar
		if(Variables.loading==false)
			pb.setVisibility(View.INVISIBLE);

		//set seekbar to update image and title accordingly when moved
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
				FileInputStream fin = null;
				
			    try {
			        fin = openFileInput("Meme"+(progress%Variables.pics)+".jpg");
			        
			        if(fin !=null && fin.available() > 0) {
			        	memenumber=progress;
			            Bitmap bmp=BitmapFactory.decodeStream(fin); 
			            imageView.setImageBitmap(bmp);
			            textView.setText(Variables.titles.get((progress%Variables.pics)));
			            //number.setText(""+(progress%Variables.pics+1)+"/"+Variables.pics);
			        } 
			        else {
			            //input stream has not much data to convert into  Bitmap
			         }
			    } catch (Exception e) {
			        // TODO Auto-generated catch block
			        e.printStackTrace();
			    }
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
		});
		
		//setup broadcast maanager/receiver to update UI thread when downloading
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

	    broadcastReceiver = new BroadcastReceiver() {
	    	
	    	//what to do when message is received from running service
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	
	        	if(intent.getAction().equals("UPDATEUI")){
	        	
	        		//service will send a message with a field "type"
	        		
	        		//if type is 0, update progressbar 
	        		switch (intent.getIntExtra("type", 0)){
	        	
	        		case 0:
	            
	        			boolean spinner=intent.getBooleanExtra("started", false);
	        		
	        			if(spinner){
	        				pb.setVisibility(View.VISIBLE);
	        				progressnumber.setText(0+"/"+Variables.imagenumber);
	        			}
	        		
	        			else{
	        				pb.setVisibility(View.INVISIBLE);
	        				progressnumber.setText("");
	        			}
	            
	        			break;
	            
	        			//if type is 1, update "currentlydownloaded/totaltobedownloaded" textview
	        		case 1:
	        			int sofar=intent.getIntExtra("progressnumber",0);
	        			seekbar.setMax(sofar-1);
	        			progressnumber.setText(sofar+"/"+Variables.imagenumber);
	        			memenumber=sofar-2;
	        			changeImage(null);
	        		}
	        	}
	        }
	    };
	}
	
	
	public void deleteAll(View view){
		
		if(Variables.loading==false){

			for(int i=0;i<Variables.pics;i++){
				File dir = getFilesDir();
				File file = new File(dir, "Meme"+i+".jpg");
				
				boolean deleted = file.delete();	
			}
			
			Variables.pics=0;
			Variables.titles.clear();
		
			SharedPreferences prefs = getSharedPreferences("webcrawler", Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
		
			editor.putString("titles", "");
			editor.putInt("pics", 0);
			editor.commit();

			textView.setText("YOUR CUP IS EMPTY!");
			
			imageView.setImageResource(R.drawable.plusifnone);
		}
		else{
			Toast.makeText(this, "Cannot empty cup while download in progress.", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	
	public void getMore(View view){
		
		ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	
		if(mWifi.isConnected()){
			
			if(Variables.loading==false){
				for(int i=0;i<Variables.pics;i++){
					File dir = getFilesDir();
					File file = new File(dir, "Meme"+i+".jpg");
					boolean deleted = file.delete();	
				}
				
				Variables.pics=0;
				Variables.titles.clear();
				
				SharedPreferences prefs = getSharedPreferences("webcrawler", Context.MODE_PRIVATE);
				Editor editor = prefs.edit();
				
				editor.putString("titles", "");
				editor.putInt("pics", 0);
				editor.putBoolean("immediate",true);
				editor.commit();

			    startService(new Intent(this, MyReceiver.class));

				textView.setText("YOUR CUP IS EMPTY!");
				imageView.setImageResource(R.drawable.plusifnone);
			}
			else{
				Toast.makeText(this, "Cannot fill cup while download in progress.", Toast.LENGTH_SHORT).show();
			}
		}
		else{
			Toast.makeText(this, "Please connect to wifi", Toast.LENGTH_SHORT).show();

		}

		
	}
	
	public void goToSettings(View view){
		
		Intent intent = new Intent(this,MainActivity.class);
		startActivity(intent);
	}
	
	public void changeImage(View view){
		
		if(Variables.titles.size()>0||Variables.pics>0){
		

			memenumber++;
			
			if(memenumber==Variables.pics)
				memenumber-=Variables.pics;
			
			
			FileInputStream fin = null;
	
		    try {
		        fin = openFileInput("Meme"+memenumber+".jpg");
		        if(fin !=null && fin.available() > 0) {
		            Bitmap bmp=BitmapFactory.decodeStream(fin); 
		            imageView.setImageBitmap(bmp);
		            textView.setText(Variables.titles.get(memenumber));
		            seekbar.setProgress(memenumber);
		        } else {
		            //input stream has not much data to convert into  Bitmap
		          }
		    } catch (Exception e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		
		}
		else{
			getMore(null);
		}
	}
}
