package com.webcrawler.demo;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class WebCrawlerFragment extends Fragment {
	
	//variables and junk
	String names;
	String[] Tokens;
	Button viewstuff;
	ProgressDialog pDialog;
	TextView mHTMLText;
	String title;
	int picnumber=25;
	EditText pics;
	TimePicker time;
	TextView spacer;
	TextView memetitle;
	TextView minitext2;
	TextView minitext3;
	
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static WebCrawlerFragment newInstance(int sectionNumber) {
		WebCrawlerFragment fragment = new WebCrawlerFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public WebCrawlerFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		//find UI elements
		
		viewstuff= (Button) rootView.findViewById(R.id.button1);
		memetitle=(TextView) rootView.findViewById(R.id.textView1);
		minitext2=(TextView) rootView.findViewById(R.id.minitext2);
		minitext3=(TextView) rootView.findViewById(R.id.minitext3);
		spacer = (TextView) rootView.findViewById(R.id.spacer);
		
		pics=(EditText) rootView.findViewById(R.id.pics);
		pics.setText(Variables.imagenumber);
		
		time=(TimePicker) rootView.findViewById(R.id.timePicker1);
		time.setCurrentHour(Variables.hour);
		time.setCurrentMinute(Variables.minute);
		
		//Setting fancy fonts...
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "Ormont-Light.otf");
		Typeface font2 = Typeface.createFromAsset(getActivity().getAssets(), "Langdon.otf");
		
		minitext3.setTypeface(font2);
		spacer.setTypeface(font);
		pics.setTypeface(font2);
		minitext2.setTypeface(font2);
		viewstuff.setTypeface(font2);
		
		//set onclicklistener for "DONE" button
		viewstuff.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				
				//see what was entered in the "how many images do you want" field
				String num=pics.getText()+"";
				
				NumberFormat formatter = NumberFormat.getInstance();
				  ParsePosition pos = new ParsePosition(0);
				  formatter.parse(num, pos);

				  
				//check if edittext field is empty
				if(num.length()>0){

				//check if what was entered in edittext field is a valid integer
					if(num.length() == pos.getIndex()){
				
						//variables.loading is a volatile variable indicating when a download is currently in progress
						if(Variables.loading==false){
							
							//save all variables entered in settings
							SharedPreferences prefs = getActivity().getSharedPreferences("webcrawler", Context.MODE_PRIVATE);
							Editor editor = prefs.edit();
				
							editor.putInt("hour", time.getCurrentHour());
				
							Calendar now=Calendar.getInstance();
							int hours=now.get(Calendar.HOUR_OF_DAY);
							int minutes=now.get(Calendar.MINUTE);
				
				
							//AlarmManager tries to execute the service immediately if the time set for the alarm is earlier than the current time
							//prevent this so it saves your preferences but doesn't execute immediately
							//reserved for the " + " button
							if(time.getCurrentHour()<hours||(time.getCurrentHour()==hours&&time.getCurrentMinute()<minutes)){
								
								//set variable "immediate" to false
								//When AlarmManager tries to execute the service immediately, it will see this flag 
								//and halt execution
								editor.putBoolean("immediate",false);
							}
				
							editor.putInt("hour", time.getCurrentHour());
							editor.putInt("minute", time.getCurrentMinute());
							editor.putString("imagenumber", pics.getText()+"");
				
							editor.commit();
				
							//prepare AlarmManager to call service daily at specified time
							
							Calendar calendar = Calendar.getInstance();
							calendar.set(Calendar.HOUR_OF_DAY, time.getCurrentHour());
							calendar.set(Calendar.MINUTE, time.getCurrentMinute());
				    

							Intent alarmintent = new Intent(getActivity(), MyReceiver.class);
							PendingIntent pintent = PendingIntent.getService(getActivity(), 0, alarmintent, 0);


							AlarmManager alarmManager = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
							alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24*60*60*1000 , pintent);

						}

						//pressing the done button returns you to the imageviewer screen
						Intent intent = new Intent(getActivity(),ImageViewer.class);
						getActivity().startActivity(intent);
						getActivity().finish();
						
					}
					else{
					Toast.makeText(getActivity(), "Please enter a number for the number of images.", Toast.LENGTH_SHORT).show();
					}
				
				}
			}
		});
		
		
		return rootView;
	}
}
