package dor.only.dorking.android.fbprofiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

public class FirstScreen extends AppCompatActivity {
    private CallbackManager callbackManager;
    private TextView theMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_first_screen);
        theMessage=(TextView)findViewById(R.id.theMessage) ;
        LoginButton theLoginButton=(LoginButton)findViewById(R.id.fb_login_button);
        theLoginButton.setReadPermissions(Arrays.asList("public_profile","email"));
        // Callback registration
        theLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_LONG).show();
                theMessage.setText("Success");
                Intent goToSecondScreen = new Intent(getApplicationContext(),SecondScreen.class);
                startActivity(goToSecondScreen);
                // App code
            }

            @Override
            public void onCancel() {
               theMessage.setText("cancelled!");
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
               Toast.makeText(getApplicationContext(),exception.getMessage(),Toast.LENGTH_LONG).show();
               theMessage.setText(exception.getMessage());
                // App code
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


}
