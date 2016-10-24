package dor.only.dorking.android.fbprofiles;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import static com.facebook.AccessToken.getCurrentAccessToken;

public class PictureActivity extends AppCompatActivity {
    //The key string we will use to get the parceable listarray of id strings.
    public static final String IDS_DATA_KEY="the key :)";

    private ArrayList<String> ids;
    private TextView personStatsTV;
    private ImageView personImage;
    private Button showNextPersonButton;
    Random rn = new Random();

    //Some string constants related to a picture related to a picture

    private static final String FB_SILHOUETTE="is_silhouette";
    private static final String FB_URL="url";
    private static final String FB_DATA="data";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        ParceableIds theIds=getIntent().getParcelableExtra(IDS_DATA_KEY);
        ids=theIds.getTheList();
        personStatsTV=(TextView)findViewById(R.id.personStatsText);
        personImage=(ImageView)findViewById(R.id.theImage);
        showNextPersonButton=(Button)findViewById(R.id.showNextPerson);
        showSomeoneRandom();
        showNextPersonButton.setOnClickListener(
                new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        showSomeoneRandom();
                                                    }
                                                }


        );



    }


    private class myCallBack implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            //Load the picture
            JSONObject picData=response.getJSONObject();
            try {
                picData=picData.getJSONObject(FB_DATA);
                if (!picData.getBoolean(FB_SILHOUETTE)) {
                    String theUrl=picData.getString(FB_URL);

                    Picasso.with(getApplicationContext()).load(theUrl).into(personImage);

                } else{
                    personStatsTV.setText(personStatsTV.getText()+" User has no profile Pic!");

                }

            }
            catch(Throwable e){
                Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG);

            }
        }
    }




    private void showSomeoneRandom(){
        int theUserChosen=rn.nextInt(ids.size());
        String theId=ids.get(theUserChosen);
        personStatsTV.setText(theId+" was chosen, index is:"+theUserChosen+" out of "+ids.size());
        //Now look for the person's pic path
        AccessToken theUserToken=getCurrentAccessToken();
        Bundle params = new Bundle();
        params.putBoolean("redirect", false);
        GraphRequest searchRequest=new GraphRequest(theUserToken,theId+"/picture?type=square&height=200",params, HttpMethod.GET);
        Toast.makeText(this,theId+"/picture?type=square&height=200",Toast.LENGTH_LONG).show();
        searchRequest.setCallback(new PictureActivity.myCallBack());
        searchRequest.executeAsync();



    }
}
