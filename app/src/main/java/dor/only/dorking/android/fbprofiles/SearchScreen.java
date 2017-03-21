package dor.only.dorking.android.fbprofiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
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

    //The views which the user can see
    private EditText theNamesToSearchEditText;
    private NumberPicker numberOfPeople;
    private Button goSearchButton;
    private Button saveToSharedPrefButton;
    private TextView progressMadeTV;


    private ArrayList<String> ids=new ArrayList<>();
    private ArrayList<String> firstNames;
    Integer namesGotSoFar=new Integer(0);
    private boolean doneFillingIds=false;

    public static final String SHARED_PREF_NAMES_KEY="the_names";

    //Some String constants given the current Facebook graph API (2.8 as of 24-Oct-2016).
    private static final String FB_DATA="data";
    private static final String FB_PAGING="paging";
    private static final String FB_NEXT_PAGE="next";
    private static final String FB_ID="id";
    private static final String FB_FIRST_NAME="first_name";

    private AccessToken theUserToken;


    private class FirstCallback implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            //When completing our first graph request, go ahead and get all of the ids..
            getIds(numberOfPeople.getValue(),response.getJSONObject().toString());
        }
    }

    private class PersonDetailsCallback implements GraphRequest.Callback{
        @Override
        public void onCompleted(GraphResponse response) {
            JSONObject theResponse=response.getJSONObject();
            try {
                int theIndex = ids.indexOf(theResponse.getString(FB_ID));
                String theFirstName=theResponse.getString(FB_FIRST_NAME);
                synchronized(firstNames){
                    firstNames.set(theIndex,theFirstName);
                }

                synchronized (namesGotSoFar){
                    namesGotSoFar++;
                    runOnUiThread( new Runnable() {
                        @Override
                        public void run() {
                            progressMadeTV.setText("Sifted "+namesGotSoFar+'/'+ids.size());
                        }
                    });
                    //



                    if(namesGotSoFar==ids.size()){
                        siftByFirstName();
                        goToPictureActivity();

                    }

                    else{

                        String thePath=ids.get(namesGotSoFar)+"?fields=id,first_name,age_range";
                        GraphRequest moreDetailsAboutThisPerson=new GraphRequest(theUserToken,thePath);
                        moreDetailsAboutThisPerson.setCallback(new PersonDetailsCallback());
                        moreDetailsAboutThisPerson.executeAsync();


                    }
                }
            }
            catch (Throwable e){
                Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();

            }
        }
    }




    private void updateUI(){
        progressMadeTV.setText(ids.size()+"/"+numberOfPeople.getValue());
        if(doneFillingIds){
            moveToNextScreen();


        }

    }

    private void moveToNextScreen(){
        firstNames=new ArrayList<>();
        for(int i=0; i<ids.size(); ++i){
            firstNames.add("");
        }
        //Creating threads in a for loop such as for(int i=0; i<ids.size(); ++i){ doesnt work (for example 133 threads are created on a Nexus 5 phone but no more than that)
        //So instead,I will call the first one here and make the other calls in the callback from the fb API.
            String thePath=ids.get(0)+"?fields=id,first_name,age_range";
            GraphRequest moreDetailsAboutThisPerson=new GraphRequest(theUserToken,thePath);
            moreDetailsAboutThisPerson.setCallback(new PersonDetailsCallback());
            moreDetailsAboutThisPerson.executeAsync();










    }


    //Removes all of the people without the first name being the same as the one we want.
    private void siftByFirstName(){
        ArrayList<String> idsWithRelevantFirstName=new ArrayList<>();
        String theNameWeWant=getNameToSearch();
        for(int i=0; i<ids.size(); ++i){
            if(firstNames.get(i).equalsIgnoreCase(theNameWeWant)){
                idsWithRelevantFirstName.add(ids.get(i));

            }



        }

        ids=idsWithRelevantFirstName;
        return;
    }


    private void goToPictureActivity(){

        Intent downloadPicture=new Intent(this,PictureActivity.class);
        ParceableIds theIds=new ParceableIds(ids);
        downloadPicture.putExtra(PictureActivity.IDS_DATA_KEY,theIds);
        String theNameWeWant=getNameToSearch();
        downloadPicture.putExtra(PictureActivity.NAME_PERSON_KEY,theNameWeWant);
        startActivity(downloadPicture);


    }



    /*getIds.
            Parameters:

    numberOfIdsLeft: the number of Ids that we want to get from this point on. Remember that if the actual number of IDs under a certain name is less than this number,
    then we will add only the ones which are available. We add those IDs to the class member ids.

    response: a string of the latest response from Facebook's server which we didn't handle yet.
    */

    private void getIds(int numberOfIdsLeft,String serverResponse){
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


    private String getNameToSearch(){
        SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(this);
        String allNames = sharedPref.getString(SHARED_PREF_NAMES_KEY, "");
        allNames=allNames.trim();
        if(allNames.equals("")){
            return "";

        }

        return allNames.split(" ")[0];

    }




    private void readProfilesPage(final String thePageAddress, final int numberOfIdsLeft){
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
        theNamesToSearchEditText=(EditText)findViewById(R.id.theNamesToSearch);
        numberOfPeople=(NumberPicker)findViewById(R.id.numberOfPeople);
        goSearchButton=(Button)findViewById(R.id.goSearchButton);
        saveToSharedPrefButton=(Button)findViewById(R.id.saveNamesToSharedPreferences);
        progressMadeTV=(TextView)findViewById(R.id.progressMadeTV);



        numberOfPeople.setMaxValue(1000);
        numberOfPeople.setMinValue(1);
        numberOfPeople.setValue(1000);
        theUserToken=getCurrentAccessToken();


        saveToSharedPrefButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String namesEntered=theNamesToSearchEditText.getText().toString();
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(SHARED_PREF_NAMES_KEY, namesEntered);
                        editor.commit();

                    }}
            );

        goSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch();
            }
        });
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);;
        String allNames = sharedPref.getString(SHARED_PREF_NAMES_KEY, "");
        theNamesToSearchEditText.setText(allNames);
        if(allNames.trim().length()>0){
            if(allNames.equals("LetsGo")){
                allNames="Noah Liam Mason Jacob William Ethan James Alexander Michael Benjamin Elijah Daniel Aiden Logan Matthew Lucas Jackson David Oliver Jayden Joseph Gabriel Samuel Carter Anthony John Dylan Luke Henry Andrew Isaac Christopher Joshua Wyatt Sebastian Owen Caleb Nathan Ryan Jack Hunter Levi Christian Jaxon Julian Landon Grayson Jonathan Isaiah Charles Thomas Aaron Eli Connor Jeremiah Cameron Josiah Adrian Colton Jordan Brayden Nicholas Robert Angel Hudson Lincoln Evan Dominic Austin Gavin Nolan Parker Adam Chase Jace Ian Cooper Easton Kevin Jose Tyler Brandon Asher Jaxson Mateo Jason Ayden Zachary Carson Xavier Leo Ezra Bentley Sawyer Kayden Blake Nathaniel Ryder Theodore Elias Tristan Roman Leonardo Camden Brody Luis Miles Micah Vincent Justin Greyson Declan Maxwell Juan Cole Damian Carlos Max Harrison Weston Brantley Braxton Axel Diego Abel Wesley Santiago Jesus Silas Giovanni Bryce Jayce Bryson Alex Everett George Eric Ivan Emmett Kaiden Ashton Kingston Jonah Jameson Kai Maddox Timothy Ezekiel Ryker Emmanuel Hayden Antonio Bennett Steven Richard Jude Luca Edward Joel Victor Miguel Malachi King Patrick Kaleb Bryan Alan Marcus Preston Abraham Calvin Colin Bradley Jeremy Kyle Graham Grant Jesse Kaden Alejandro Oscar Jase";
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(SHARED_PREF_NAMES_KEY, allNames);
                editor.commit();

            }
            startSearch();

        }




    }
    private void startSearch(){
        String theName=getNameToSearch();
        theName=theName.trim();
        if(theName.equals("")){return;}
        theNamesToSearchEditText.setEnabled(false);
        GraphRequest searchRequest=new GraphRequest(theUserToken,"search?q="+'"'+theName+'"'+"&type=user");
        searchRequest.setCallback(new FirstCallback());
        searchRequest.executeAsync();

    }
}
