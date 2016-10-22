package dor.only.dorking.android.fbprofiles;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONObject;

import static com.facebook.AccessToken.getCurrentAccessToken;

public class SecondScreen extends AppCompatActivity {
    private TextView theResponseTextView;
    private TextView playgroundTextview;

    private class myCallBack implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            theResponseTextView.setText(response.toString());
            analyzeTheResponse(response.toString());
        }
    }

    private void analyzeTheResponse(String theResponse){
       try{
           JSONObject theResponseInJson = new JSONObject(theResponse);
           JSONObject graphObject=theResponseInJson.getJSONObject("graphObject");
           playgroundTextview.setText(graphObject.toString());
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
