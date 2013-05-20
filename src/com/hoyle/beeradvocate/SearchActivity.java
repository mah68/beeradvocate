package com.hoyle.beeradvocate;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SearchActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		ListView resultsListView = (ListView) findViewById(R.id.resultsListView);
		EditText searchBar = (EditText) findViewById(R.id.searchEditText);
		searchBar.setText("");
		searchBar.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		TextView emptyTextView = (TextView) findViewById(android.R.id.empty);

		searchBar.setOnEditorActionListener(new SearchOnKeyListener(this, searchBar, resultsListView, emptyTextView));
	}

	public Dialog onCreateDialog(int id) {
		switch (id) {
		case 0:
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Network Error")
					.setMessage(
							"Cannot connect to beeradvocate.com. Please check your network connection and try again.")
					.setCancelable(false).setNegativeButton("Okay", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();
			break;
		}
		return super.onCreateDialog(0);
	}

	private String loadHTML(String url) {
		String response = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse responseGet = client.execute(get);
			HttpEntity resEntityGet = responseGet.getEntity();
			if (resEntityGet != null) {

				response = EntityUtils.toString(resEntityGet);
			}
		} catch (Exception e) {
			showDialog(0);
		}
		return response;
	}

	private List<String[]> parsePage(String page) {
		List<String[]> results = new ArrayList<String[]>();
		Document doc = Jsoup.parse(page);
		Element baContent = doc.getElementById("baContent");
		if (baContent == null)
			return results;
		Element ul = baContent.getElementsByTag("ul").first();
		if (ul == null)
			return results;
		Elements listItems = ul.getElementsByTag("li");
		if (listItems == null) {
			return results;
		}

		for (int i = 0; i < listItems.size(); i++) {
			// index 0: Retired 1: Beer name, 2: Brewery, 3: Location, 4: link
			String[] info = new String[5];
			Element item = listItems.get(i);
			info[0] = "";
			Element retired = item.getElementsByAttributeValue("style", "color:#990000;").first();
			if (retired != null)
				info[0] = retired.text();
			info[1] = item.getElementsByAttributeValueStarting("href", "/beer/profile/").first().text();
			info[2] = item.getElementsByAttributeValueStarting("href", "/beer/profile/").get(1).text();
			int marker = item.text().indexOf("|") + 2;
			info[3] = item.text().substring(marker);
			info[4] = item.getElementsByAttributeValueStarting("href", "/beer/profile/").first().attributes()
					.get("href");
			Log.d("links", "link: " + info[4]);
			results.add(info);
		}

		return results;
	}

	public class ResultsArrayAdapter extends ArrayAdapter<String[]> {
		List<String[]> results;
		int viewId;
		Context context;

		public ResultsArrayAdapter(Context context, int viewId, List<String[]> results) {
			super(context, viewId, results);
			this.results = results;
			this.viewId = viewId;
			this.context = context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater li = getLayoutInflater();
			View v = li.inflate(viewId, parent, false);
			final String info[] = results.get(position);
			TextView retired = (TextView) v.findViewById(R.id.resultRetired);
			TextView beername = (TextView) v.findViewById(R.id.resultBeerName);
			TextView brewerylocation = (TextView) v.findViewById(R.id.resultBreweryLocation);
			if (info[0] == null)
				Log.d("adapter", "info[0] null");

			retired.setText(info[0]);
			beername.setText(info[1]);
			brewerylocation.setText(info[2] + " | " + info[3]);

			v.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					Intent myIntent = new Intent(getBaseContext(), BeerAdvocateActivity.class);
					myIntent.putExtra("link", info[4]);
					startActivity(myIntent);
				}

			});

			return v;
		}

	}

	public class SearchOnKeyListener implements OnEditorActionListener {
		private EditText editText;
		private ListView listView;
		private Context context;
		private TextView emptyTextView;

		public SearchOnKeyListener(Context context, EditText editText, ListView listView, TextView emptyTextView) {
			super();
			this.context = context;
			this.editText = editText;
			this.listView = listView;
			this.emptyTextView = emptyTextView;
		}

		public boolean onEditorAction(TextView v, int keyCode, KeyEvent event) {
			// listView.setAdapter(new ArrayAdapter<String>(context, 0, new
			// ArrayList<String>()));
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

			String search = editText.getText().toString();
			search = urlize(search);
			String url = "http://beeradvocate.com/search?q=" + search + "&qt=beer";
			String page = loadHTML(url);
			if (page == null) return false;
			List<String[]> results = parsePage(page);
			listView.setAdapter(new ResultsArrayAdapter(context, R.layout.result_list_item, results));

			if (results.size() == 0) {
				emptyTextView.setText("No results found");
			} else if (results.size() == 1) {
				emptyTextView.setText("1 result found");
			} else {
				emptyTextView.setText(results.size() + " results found");
			}

			return true;
		}

		private String urlize(String search) {
			String ret = "";
			for (String sub : search.split(" ")) {
				ret += sub + "+";
			}
			return ret.substring(0, ret.length() - 1);
		}
	}
}
