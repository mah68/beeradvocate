package com.hoyle.beeradvocate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BeerAdvocateActivity extends Activity {

	private String myURL = "http://beeradvocate.com/beer/profile/73/18421";
	private String myPage;

	// initialize variables with default values
	private String beerName = "Not Found";
	private String breweryName = "Not Found";
	private String baScore = "00";
	private String baClass = "not found";
	private String broScore = "00";
	private String broClass = "not found";
	private String ratingStats = "no stats found";
	private String breweryLoc = "";
	private String style = "Unknown style";
	private String abv = "?";
	private String availability = "Unknown";
	private String notes = "No notes.";

	private Bitmap imageBitmap; // = some default image

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		String url = getIntent().getStringExtra("link");
		if (url != null)
			myURL = "http://beeradvocate.com" + url;

		myPage = loadHTML();
		if (myPage == null) {
			Intent myIntent = new Intent(getBaseContext(), SearchActivity.class);
			startActivity(myIntent);
		}

		extractData(Jsoup.parse(myPage));
		setViews();
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

	private void setViews() {
		TextView beername = (TextView) findViewById(R.id.beerNameTextView);
		TextView brewery = (TextView) findViewById(R.id.breweryTextView);
		TextView bascore = (TextView) findViewById(R.id.baScoreTextView);
		TextView broscore = (TextView) findViewById(R.id.broScoreTextView);
		TextView baclass = (TextView) findViewById(R.id.baClass);
		TextView broclass = (TextView) findViewById(R.id.broClass);
		TextView stats = (TextView) findViewById(R.id.ratingStatsTextView);
		ImageView beerpic = (ImageView) findViewById(R.id.beerpic);
		TextView brewerylower = (TextView) findViewById(R.id.breweryLowerTextView);
		TextView styleabv = (TextView) findViewById(R.id.styleABVTextView);
		TextView availability = (TextView) findViewById(R.id.availabilityTextView);
		TextView notes = (TextView) findViewById(R.id.notesTextView);
		Button search = (Button) findViewById(R.id.searchButton);

		beername.setText(beerName);
		brewery.setText(breweryName);
		bascore.setText(baScore);
		broscore.setText(broScore);
		baclass.setText(baClass);
		broclass.setText(broClass);
		stats.setText(ratingStats);
		beerpic.setImageBitmap(imageBitmap);
		brewerylower.setText(breweryName);
		if (breweryLoc != "")
			brewerylower.setText(breweryName + ", " + breweryLoc);
		styleabv.setText(this.style + " | " + this.abv + " ABV");
		availability.setText(this.availability);
		notes.setText(this.notes);

		search.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent myIntent = new Intent(getBaseContext(), SearchActivity.class);
				startActivity(myIntent);
			}

		});

	}

	private void extractData(Document doc) {
		Element mainContent = doc.getElementsByClass("mainContent").first();
		Element baContent = doc.getElementById("baContent");
		Elements scores = baContent.getElementsByClass("BAscore_big");
		if (scores != null) {
			if (scores.size() >= 2) {
				baScore = scores.get(0).text();
				broScore = scores.get(1).text();
			}
		}
		Elements classes = baContent.getElementsByAttributeValue("href", "/help/index?topic=ratings");
		if (scores != null) {
			if (classes.size() >= 2) {
				baClass = classes.get(0).text();
				broClass = classes.get(1).text();
			}
		}

		Element beerpic = baContent.getElementsByAttributeValueStarting("src", "/im/beers/").first();
		if (beerpic != null) {
			String imagePath = "http://beeradvocate.com" + beerpic.attr("src");
			imageBitmap = loadImage(imagePath);
		}

		Element beernames = mainContent.getElementsByClass("titleBar").first();
		Element beername = null;
		if (beernames != null)
			beername = beernames.getElementsByTag("h1").first();

		if (beername != null) {
			beerName = beername.text();
			if (beerName.contains(" - ")) {
				beerName = beerName.split(" - ")[0];
			}
			if (beername.child(0) != null) {
				breweryName = beername.child(0).text();
				breweryName = breweryName.substring(2);
			}
		}

		Element stats = baContent.getElementsContainingOwnText("rAvg:").first();
		if (stats != null) {
			ratingStats = stats.text();
			String s[] = ratingStats.split(" ");
			if (s.length >= 9)
				ratingStats = s[0] + s[1] + "\t" + s[2] + s[3] + "\n" + s[4] + s[5] + "\t" + s[6] + s[7];
		}

		Elements brewerylocation = mainContent.getElementsByAttributeValueStarting("href", "/beerfly/");
		if (brewerylocation != null) {
			for (Element loc : brewerylocation) {
				breweryLoc += loc.text() + " ";
			}
			breweryLoc = breweryLoc.substring(0, breweryLoc.length() - 1);
		}

		Element allattributes = baContent.getElementsByAttributeValue("style", "padding:10px;").first();
		if (allattributes != null) {
			Element style = allattributes.getElementsByAttributeValueStarting("href", "/beer/style/").first();
			if (style != null) {
				this.style = style.text();
			}
			String attributes = allattributes.text();
			int marker = attributes.indexOf("Notes: ", 0);
			if (marker != -1) {
				this.notes = attributes.substring(marker + 7);
			}
			marker = attributes.indexOf("Availability: ");
			int marker2 = attributes.indexOf(".", marker);
			if (marker != -1 && marker2 != -1) {
				this.availability = attributes.substring(marker + 14, marker2 + 1);
			}
			marker = attributes.indexOf(" | ", attributes.indexOf("Style | ABV ") + 12);
			marker2 = attributes.indexOf("%", marker);
			if (marker != -1 && marker2 != -1) {
				this.abv = attributes.substring(marker + 4, marker2 + 1);
			}

		}

	}

	private String loadHTML() {
		String response = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(myURL);
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

	private Bitmap loadImage(String imagePath) {
		URL url = null;
		try {
			url = new URL(imagePath);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		HttpURLConnection connection;
		InputStream is = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			is = connection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bitmap img = BitmapFactory.decodeStream(is);
		return img;
		// imageView.setImageBitmap(img);

	}
}