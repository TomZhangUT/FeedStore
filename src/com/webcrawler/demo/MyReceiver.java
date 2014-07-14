package com.webcrawler.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class MyReceiver extends Service {
	
int picnumber;
Button mDemoBtn;
String names;
String[] Tokens;
TextView mHTMLText;
EditText pics;
TimePicker time;
String title;
LocalBroadcastManager mLocalBroadcastManager=LocalBroadcastManager.getInstance(this);

    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	SharedPreferences webcrawler = getSharedPreferences("webcrawler",0);
		
    	//get all saved variables for the app before starting service
    	Variables.pics=webcrawler.getInt("pics", 0);
		Variables.hour=webcrawler.getInt("hour",0);
		Variables.minute= webcrawler.getInt("minute", 0);
		Variables.immediate=webcrawler.getBoolean("immediate", true);
		Variables.imagenumber= webcrawler.getString("imagenumber","25");
			
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(Variables.imagenumber, pos);
		
		//if the number of images to be downloaded is a valid integer
		//AND Variables.immediate is true ==> prevent execution when pressing "DONE"
		if(Variables.imagenumber.length() == pos.getIndex()&&Variables.immediate){
			
			picnumber=Integer.parseInt(Variables.imagenumber);
			
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			//check for wifi connection
			if (mWifi.isConnected()) {
			
				Variables.loading=true;
			
				AsyncTask <Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>(){
			
					public int numProcessed = 0;
			
					@Override
					protected void onPreExecute() {
						super.onPreExecute();
				
						//before starting, delete all previously stored images 
						for(int i=0;i<Variables.pics;i++){
							File dir = getFilesDir();
							File file = new File(dir, "Meme"+i+".jpg");
							boolean deleted = file.delete();	
						}
						
						//clear this junk too
						Variables.pics=0;
						Variables.titles.clear();
				
						//sends a message to the imageviewer class so, if you started the service
						//by pressing " + " to get more content, updates progressbar and images downloaded #
						Intent broadcastIntent = new Intent("UPDATEUI");
						broadcastIntent.putExtra("type",0);
						broadcastIntent.putExtra("started", true);
						mLocalBroadcastManager.sendBroadcast(broadcastIntent);
				
					}
			
					@Override
					protected String doInBackground(Void... params) {
				
						Document doc;
					
						try {
							doc = Jsoup.connect("http://9gag.com/").get();
						} catch (IOException e) {
							return null;
						}
			
						//start scraping, content can be identified by "jsid-latest-entries" ID
						Element stuff = doc.getElementById("jsid-latest-entries");
						
						//jsoup junk to get the right element
						names= stuff.ownText();
						String delims="[,]";
						Tokens= names.split(delims);
						String URL= "http://9gag.com/gag/"+Tokens[0];
				
						//while more content still needs to be downloaded
						while(numProcessed<picnumber){
				
							try {
								doc = Jsoup.connect(URL).get();
							} catch (IOException e) {
								return null;
							}

							//get anything with some sort of image/gif extension
							Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g)|gif]");
				
							for (Element image : images) {
							
								//for each element get the srs url
								String src = image.absUrl("src");
								String extension = src.substring(src.lastIndexOf('.') + 1);
								title=image.attr("alt");
					
								//right now it doesn't do gifs, so if it sees a gif or something with an invalid title/src
								//fuck it and break out of this loop
								if(extension.equals("gif")||title.length()<1||src.length()<1)
									break;
								
								try {
									//if it's good, go and download the image
									getImage(src);
									break;
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
				
							//crawler goes to next page specified by "div.post-nav"
							Element allDiv = doc.select("div.post-nav").first();
							Elements allClass=allDiv.children();
							Element firstClass=allClass.first();
							URL = firstClass.attr("abs:href");

						}
						
						return doc.title();
					}
					
					private void getImage(String src) throws IOException {
				
						String filename = "Meme" + numProcessed+".jpg";
						numProcessed++;
						Variables.pics=numProcessed;
				
						//Open a URL Stream
						URL url = new URL(src);
						InputStream in = url.openStream();
		        
						FileOutputStream out = openFileOutput(filename, Context.MODE_PRIVATE);
						
						long size=0;
		        
						for (int b; (b = in.read()) != -1;) {
							size++;
							if(size>120000){
								//if the image size is larger than 120kb, fuck it
								numProcessed--;
								return;
							}
							out.write(b);
						}

						//change title to uppercase (looks better for font used)
						Variables.titles.add(title.toUpperCase());
				
						out.close();
						in.close();
		        
						Variables.pics=numProcessed;
				
						//update UI for imageviewer, just incase someone is watching as this is happening
						Intent broadcastIntent = new Intent("UPDATEUI");
						broadcastIntent.putExtra("type",1);
						broadcastIntent.putExtra("progressnumber", numProcessed);
						mLocalBroadcastManager.sendBroadcast(broadcastIntent);
		        
						SharedPreferences prefs = getSharedPreferences("webcrawler", Context.MODE_PRIVATE);
						Editor editor = prefs.edit();
				
						//right now, i'm serializing all post titles into one giant ass string,
						//delimited by "||"
						//kind of sloppy, but does the job for now (works for what i've seen so far ~500+ posts)
						String appended=Variables.titles.get(0)+"||";
						
						for(int i=1; i<Variables.titles.size();i++){
							appended=appended+Variables.titles.get(i)+"||";
						}
				
						editor.putString("titles", appended);
				
						editor.putInt("pics", Variables.pics);
				
						editor.commit();
					}

					@Override
					protected void onPostExecute(String result) {
						
						super.onPostExecute(result);
				
						//change volatile loading flag so UI thread knows
						Variables.loading=false;
				
						Intent broadcastIntent = new Intent("UPDATEUI");
						broadcastIntent.putExtra("type",0);
						broadcastIntent.putExtra("started",false);
						mLocalBroadcastManager.sendBroadcast(broadcastIntent);
					
					}
			
				};
				asyncTask.execute();//executes async task
			}
			else{
			//do nothing
			}
		}
			
			SharedPreferences prefs = getSharedPreferences("webcrawler", Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			
			//finish up service and reset immediate flag
			editor.putBoolean("immediate", true);
			editor.commit();
    	
			return startId;
        }


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
}