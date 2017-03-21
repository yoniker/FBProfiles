package dor.only.dorking.android.fbprofiles;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static com.facebook.AccessToken.getCurrentAccessToken;
import static dor.only.dorking.android.fbprofiles.SearchScreen.SHARED_PREF_NAMES_KEY;

public class PictureActivity extends AppCompatActivity {
    //The key string we will use to get the parceable listarray of id strings.
    public static final String IDS_DATA_KEY="IDS_KEY";
    public static final String NAME_PERSON_KEY="NAME_PERSON_KEY";

    private ArrayList<String> ids;
    private TextView personStatsTV;
    private ImageView personImage;
    private Button downloadPictures;
    private TextView downloadStatusTV;
    private Button showNextPersonButton;
    private String nameOfPerson;
    private String thePicUrl;
    Random rn = new Random();


    //I am not familiar with a better way,so the FB graph callbacks will communicate through the activity's variables
    private Integer currentDownloadIndex;

    //Some string constants related to a picture related to a picture

    private static final String FB_SILHOUETTE="is_silhouette";
    private static final String FB_URL="url";
    private static final String FB_DATA="data";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        ParceableIds theIds=getIntent().getParcelableExtra(IDS_DATA_KEY);
        nameOfPerson=getIntent().getStringExtra(NAME_PERSON_KEY);
        ids=theIds.getTheList();
        personStatsTV=(TextView)findViewById(R.id.personStatsText);
        personImage=(ImageView)findViewById(R.id.theImage);
        showNextPersonButton=(Button)findViewById(R.id.showNextPerson);
        downloadPictures=(Button) findViewById(R.id.downloadPictures);
        downloadStatusTV=(TextView)findViewById(R.id.downloadStatus);
        showSomeoneRandom();
        showNextPersonButton.setOnClickListener(
                new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        showSomeoneRandom();
                                                    }
                                                }


        );

        downloadPictures.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        downloadAllPictures();
                    }
                }

        );

        downloadAllPictures();

    }

    private void downloadAllPictures(){
        showNextPersonButton.setVisibility(View.GONE);
        downloadPictures.setVisibility(View.GONE);
        downloadStatusTV.setVisibility(View.VISIBLE);
        currentDownloadIndex=0;
        downloadCurrentPersonByIndex();

    }


    private class CallbackForRandomPic implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            //Load the picture
            JSONObject picData=response.getJSONObject();
            try {
                picData=picData.getJSONObject(FB_DATA);
                if (!picData.getBoolean(FB_SILHOUETTE)) {
                    thePicUrl=picData.getString(FB_URL);

                    Picasso.with(getApplicationContext()).load(thePicUrl).into(personImage);
                    downloadPictures.setEnabled(true);

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
        searchRequest.setCallback(new PictureActivity.CallbackForRandomPic());
        searchRequest.executeAsync();



    }

    private class CallbackForDownloading implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            //Load the picture
            JSONObject picData=response.getJSONObject();
            try {
                picData=picData.getJSONObject(FB_DATA);
                if (!picData.getBoolean(FB_SILHOUETTE)) {
                    thePicUrl=picData.getString(FB_URL);

                    Picasso.with(getApplicationContext()).load(thePicUrl).into(personImage);
                    downloadThePicture(thePicUrl,ids.get(currentDownloadIndex),currentDownloadIndex.toString());

                } else{
                    //personStatsTV.setText(personStatsTV.getText()+" User has no profile Pic!");

                }
                currentDownloadIndex+=1;
                downloadCurrentPersonByIndex();

            }
            catch(Throwable e){
                Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG);

            }
        }
    }





    public  void removeFirstNameFromSharedPref(){

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String allNames=sharedPref.getString(SHARED_PREF_NAMES_KEY,"");
        allNames=allNames.trim();
        String firstName=allNames.split(" ")[0];
        int firstNameLength=firstName.length();
        allNames=allNames.substring(firstNameLength);
        allNames=allNames.trim();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_NAMES_KEY, allNames);
        editor.commit();

    }


    void downloadCurrentPersonByIndex(){
        int theUserChosen=currentDownloadIndex;
        if(theUserChosen>=ids.size()) {
            removeFirstNameFromSharedPref();
            Intent searchScreen=new Intent(this,SearchScreen.class);
            startActivity(searchScreen);
            return;}
        String theId=ids.get(theUserChosen);
        downloadStatusTV.setText(theId+" Queuing "+theUserChosen+" out of "+ids.size()+" from:"+nameOfPerson);
        //Now look for the person's pic path
        AccessToken theUserToken=getCurrentAccessToken();
        Bundle params = new Bundle();
        params.putBoolean("redirect", false);
        GraphRequest searchRequest=new GraphRequest(theUserToken,theId+"/picture?type=square&height=200",params, HttpMethod.GET);
        searchRequest.setCallback(new PictureActivity.CallbackForDownloading());
        searchRequest.executeAsync();

    }

    private void downloadThePicture(String thePicUrl,String thePersonsId,String thePersonsIndex){
        if(thePicUrl==null || thePicUrl.equals("")){
            return;
        }

        //Toast.makeText(this,thePicUrl,Toast.LENGTH_LONG).show();
        File direct = new File(Environment.getExternalStorageDirectory()
                + "/DorDownloadToHere");

        if (!direct.exists()) {
            direct.mkdirs();
        }

        DownloadManager mgr = (DownloadManager)this.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri downloadUri = Uri.parse(thePicUrl);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(true).setTitle("Demo")
                .setDescription("Something useful. No, really.")
                .setDestinationInExternalPublicDir("/DorDownloadToHere", nameOfPerson+"_"+thePersonsIndex+"_"+thePersonsId+".jpg");

        mgr.enqueue(request);


    }
}
