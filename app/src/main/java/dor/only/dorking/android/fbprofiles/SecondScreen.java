package dor.only.dorking.android.fbprofiles;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static com.facebook.AccessToken.getCurrentAccessToken;

public class SecondScreen extends AppCompatActivity {
    private TextView theResponseTextView;
    private TextView playgroundTextview;

    private class myCallBack implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            theResponseTextView.setText(response.getJSONObject().toString());
            analyzeTheResponse(response);
        }
    }


    private void readProfilesPage(final String thePageAddress){
         new AsyncTask<String,Void,String>(){
             @Override
             protected String doInBackground(String... strings) {
                 String theAddress=strings[0];
                 try {
                     URL url = new URL(thePageAddress);
                     HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                     try {
                         InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                         java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                         return s.hasNext() ? s.next() : "";

                     } finally {
                         urlConnection.disconnect();
                     }
                 } catch(Throwable e){}
                 return "";

             }

             @Override
             protected void onPostExecute(String s) {
                 try{
                     JSONObject theResponse=new JSONObject(s);
                     JSONArray theDataArray=theResponse.getJSONArray("data");
                     playgroundTextview.setText("The next page string translates to:"+theDataArray.toString()+" and it has "+theDataArray.length()+" entries");

                 }
                 catch(Throwable e){
                     Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG);

                 }
                 //playgroundTextview.setText(s);
             }
         }.execute(thePageAddress);





    }

    private void analyzeTheResponse(GraphResponse theResponse){
       try{
           JSONObject theResponseInJson = theResponse.getJSONObject();
           JSONArray theData=theResponseInJson.getJSONArray("data");
           JSONObject paging=theResponseInJson.getJSONObject("paging");
           String nextPage=paging.getString("next");
           playgroundTextview.setText(nextPage);
           //OK let's try to read some data!
           readProfilesPage(nextPage);


       }
       catch(Throwable e){
           //Here if it could not make a json object from the string.
           Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
           String s=e.toString();
           playgroundTextview.setText(e.toString());

       }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_screen);
        theResponseTextView=(TextView)findViewById(R.id.the_response_tv);
        playgroundTextview=(TextView)findViewById(R.id.playground_tv);
        AccessToken theUserToken=getCurrentAccessToken();
        GraphRequest searchRequest=new GraphRequest(theUserToken,"search?q=jennifer&type=user");
        searchRequest.setCallback(new myCallBack());
        searchRequest.executeAsync();

    }
}
