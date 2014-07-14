package com.webcrawler.demo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class WebCrawlerFragment extends Fragment {
	
	Button mDemoBtn;
	
	// Progress dialog
	ProgressDialog pDialog;
	TextView mHTMLText;
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		TextView textView = (TextView) rootView
				.findViewById(R.id.section_label);
		textView.setText(Integer.toString(getArguments().getInt(
				ARG_SECTION_NUMBER)));
		
		mHTMLText = (TextView) rootView.findViewById(R.id.web_page);
		
		mDemoBtn = (Button) rootView.findViewById(R.id.startWebCrawl);
		
		mDemoBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				grabContent();
			}
		});
		return rootView;
	}
	
	protected void grabContent() {
		//Brief demo workaround until proper threads are implemented
		AsyncTask <Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>(){
			public int numProcessed = 0;
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				pDialog = ProgressDialog.show(getActivity(),"Retrieving Web Data", "Please wait"); 
			}
			
			@Override
			protected String doInBackground(Void... params) {
				Document doc;
				try {
					doc = Jsoup.connect("http://9gag.com/").get();
				} catch (IOException e) {
					return null;
				}
				
				Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
				for (Element image : images) {
					//for each element get the srs url
					String src = image.absUrl("src");
					try {
						getImage(src);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				return doc.title();
				
			}
			
			private void getImage(String src) throws IOException {
				
				String filename = "Meme" + numProcessed;
				numProcessed++;
				
				 //Open a URL Stream
		        URL url = new URL(src);
		        InputStream in = url.openStream();

		        FileOutputStream out = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);

		        for (int b; (b = in.read()) != -1;) {
		            out.write(b);
		        }
		        out.close();
		        in.close();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				if (result == null){
					mHTMLText.setText("Async Task did not work");
				}
				else{
					mHTMLText.setText(result);
				}
				pDialog.dismiss();
			}
		};
		asyncTask.execute();//executes async task
	}
}