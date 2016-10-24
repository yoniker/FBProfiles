package dor.only.dorking.android.fbprofiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;

public class PictureActivity extends AppCompatActivity {
    //The key string we will use to get the parceable listarray of id strings.
    public static final String IDS_DATA_KEY="the key :)";

    private ArrayList<String> ids;
    private TextView textUserPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        textUserPicture=(TextView)findViewById(R.id.text_userPicture);
        Intent huh=getIntent();
        ParceableIds theIds=getIntent().getParcelableExtra(IDS_DATA_KEY);
        ids=theIds.getTheList();
        textUserPicture.setText("I have :"+ids.size()+":"+ids.toString());
    }
}
