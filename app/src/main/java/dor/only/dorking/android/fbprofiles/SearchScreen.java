package dor.only.dorking.android.fbprofiles;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import static com.facebook.AccessToken.getCurrentAccessToken;

/*second screen. The idea here is to do the following (most of the time we will be running in a background thread):
 *
  * 1.Call the graph API using fb SDK to get the initial ID list,as well as the next page.
  * 2.using that list, and as many 'next' pages as needed (if available), get the number of IDs that we want, and append them in a class level member...
  * 3.After which, we will simply download all of the pictures for those IDs using again the SDK.
  *
  *
  * */

public class SearchScreen extends AppCompatActivity {
    private TextView theResponseTextView;
    private TextView playgroundTextView;
    private ArrayList<String> ids=new ArrayList<>();
    private boolean doneFillingIds=false;

    //Some String constants given the current Facebook graph API (2.8 as of 24-Oct-2016).
    private static final String FB_DATA="data";
    private static final String FB_PAGING="paging";
    private static final String FB_NEXT_PAGE="next";
    private static final String FB_ID="id";


    private class myCallBack implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            theResponseTextView.setText(response.getJSONObject().toString());
            getIds(181,response.getJSONObject().toString());
        }
    }


    /*getIds.
    Parameters:

    numberOfIdsLeft: the number of Ids that we want to get from this point on. Remember that if the actual number of IDs under a certain name is less than this number,
    then we will add only the ones which are available. We add those IDs to the class member ids.

    response: a string of the latest response from Facebook's server which we didn't handle yet.
    */

    private void updateUI(){
        playgroundTextView.setText("So far we have:"+ids.size()+" Ids,"+ ids.toString());
        if(doneFillingIds){
            Intent downloadPicture=new Intent(this,PictureActivity.class);
            ParceableIds theIds=new ParceableIds(ids);
            downloadPicture.putExtra(PictureActivity.IDS_DATA_KEY,theIds);
            startActivity(downloadPicture);

        }

    }

    private void getIds(long numberOfIdsLeft,String serverResponse){
        if(numberOfIdsLeft<=0){
            doneFillingIds=true;
            updateUI();
            return;
        }
        try{
            JSONObject theResponse=new JSONObject(serverResponse);
            JSONArray theData=theResponse.getJSONArray(FB_DATA);
            for(int i=0; i<theData.length(); ++i){
                if(numberOfIdsLeft<=0){
                    doneFillingIds=true;
                    updateUI();
                    return;
                }
                ids.add(theData.getJSONObject(i).getString(FB_ID)); //TODO: in the future we might want to lock ids if multiple threads might access it at the same time.
                numberOfIdsLeft--;


            }

            if(numberOfIdsLeft<=0){
                doneFillingIds=true;
                updateUI();
                return;

            }


            JSONObject paging = theResponse.getJSONObject(FB_PAGING);
            if (!paging.has(FB_NEXT_PAGE)) {
                //This means that there is no next page, so we are done.
                doneFillingIds=true;
                updateUI();
                return;

            }

            String nextPage = paging.getString(FB_NEXT_PAGE);
            updateUI();
            readProfilesPage(nextPage, numberOfIdsLeft);
            return;





        }
        catch(Throwable e){}



    }




    private void readProfilesPage(final String thePageAddress, final long numberOfIdsLeft){
         new AsyncTask<String,Void,String>(){
             @Override
             protected String doInBackground(String... strings) {
                 String theAddress=strings[0];
                 try {
                     URL url = new URL(theAddress);
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
                     getIds(numberOfIdsLeft,s);
             }
         }.execute(thePageAddress);





    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_screen);
        theResponseTextView=(TextView)findViewById(R.id.the_response_tv);
        playgroundTextView=(TextView)findViewById(R.id.playground_tv);
        AccessToken theUserToken=getCurrentAccessToken();
        GraphRequest searchRequest=new GraphRequest(theUserToken,"search?q=jennifer&type=user");
        searchRequest.setCallback(new myCallBack());
        searchRequest.executeAsync();

    }
}
