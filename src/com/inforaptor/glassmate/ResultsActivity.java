package com.inforaptor.glassmate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;

public class ResultsActivity extends Activity {
	
	private List<Card> mCards;
    private CardScrollView mCardScrollView;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_results);
		// Get voice input
		ArrayList<String> voiceResults = getIntent().getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
		// Request from server
		String query = voiceResults.get(0);
		String url = "http://pacific-plateau-2886.herokuapp.com/api/search/?q=";
		// Encode the query
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e){
			System.out.println("bad encoding");
		}
		url += query;
		try {
			HttpResponse response = makeHTTPCall(url);
			// Handle the response
			StatusLine statusLine = response.getStatusLine();
		    if (statusLine.getStatusCode() == HttpStatus.SC_OK){
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        //Assume we get a JSON object, parsed from the the response string
		        JSONObject mainResponseObject = new JSONObject(responseString);
		        // Create top level result
		        ArrayList<String> for_listview = parseJsonResult(mainResponseObject);
				String[] resultsArray = for_listview.toArray(new String[voiceResults.size()]);
				// Create cards from first top level result (but passing in all)
				createCards(mainResponseObject);
				// Create card view
				if (savedInstanceState == null) {
					// Create a card with some simple text
					Card card1 = new Card(getBaseContext());
					card1.setText(resultsArray[0]);
					// Don't call this if you're using TimelineManager
					View card1View = card1.toView();
					//setContentView(card1View);/////////////////////////////
					// Create scrolling card view
			        mCardScrollView = new CardScrollView(this);
			        ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
			        mCardScrollView.setAdapter(adapter);
			        mCardScrollView.activate();
			        setContentView(mCardScrollView);
				}
		    } else {
		        //Closes the connection.
		        response.getEntity().getContent().close();
		        throw new IOException(statusLine.getReasonPhrase());
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HttpResponse makeHTTPCall(String url) {
		 HttpResponse response = null;
		 try {        
		        HttpClient client = new DefaultHttpClient();
		        HttpGet request = new HttpGet();
		        request.setURI(new URI(url));
		        response = client.execute(request);
		    } catch (URISyntaxException e) {
		        e.printStackTrace();
		    } catch (ClientProtocolException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		 return response;
	}

	ArrayList<String> parseJsonResult(JSONObject json_object) {
		ArrayList<String> arr_list = new ArrayList<String>();
		try {
			JSONArray jArray = json_object.getJSONArray("results");
			//iterate through it:
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject json_obj = jArray.getJSONObject(i);
				//appending the name:
				String title = json_obj.getString("name");
				arr_list.add(title);
			}
		} catch (JSONException e) {
			System.out.println("bad json");
		}
		return arr_list;	
	}
	
    private void createCards(JSONObject mainResponseObject) {
        mCards = new ArrayList<Card>();
        Card card;
        // Get first response
        try {
			JSONArray jArray = mainResponseObject.getJSONArray("results");
			JSONArray jArr = jArray.getJSONObject(0).getJSONArray("steps");
			// Create cards
			for (int i = 0; i < jArr.length(); i++) {
				JSONObject json_obj = jArr.getJSONObject(i);
		        card = new Card(this);
		        card.setText(json_obj.getString("description"));
		        // Read out json_obj.getString("text")
		        card.setImageLayout(Card.ImageLayout.FULL);
		        // Load image card.addImage(json_obj.getString("image_url"));
		        mCards.add(card);
			}
		} catch (JSONException e) {
			System.out.println("bad json");
		}
    }
    
    private class ExampleCardScrollAdapter extends CardScrollAdapter {
        @Override
        public int findIdPosition(Object id) {
            return -1;
        }
        @Override
        public int findItemPosition(Object item) {
            return mCards.indexOf(item);
        }
        @Override
        public int getCount() {
            return mCards.size();
        }
        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).toView();
        }
    }
}