package com.goovis.signtest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private SignInButton mSignInButton;
    private Button mSignOutButton;
    private Button mRevokeAccessButton;
    private TextView mSignInStatusTxt;

    private static final int STATE_SIGNED_IN = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private int mSignInProgress;

    private PendingIntent mSignInIntent;
    private int mSingInError;

    private static final int RC_SIGN_IN = 0;
    private static final int DIALOG_PLAY_SREVICE_ERROR = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSignInButton = (SignInButton)findViewById(R.id.sign_in_button);
        mSignOutButton = (Button)findViewById(R.id.sign_out_button);
        mRevokeAccessButton = (Button)findViewById(R.id.revoke_access_button);
        mSignInStatusTxt = (TextView)findViewById(R.id.sign_in_status);

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mGoogleApiClient.isConnecting()){
                    mSignInStatusTxt.setText("Signing In");
                    resolveSignInError();
                }
            }
        });

        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                mGoogleApiClient.connect();
            }
        });

        mRevokeAccessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                mGoogleApiClient = buildApiClient();
                mGoogleApiClient.connect();
            }
        });

        mGoogleApiClient = buildApiClient();
    }

    public GoogleApiClient buildApiClient(){
        return new GoogleApiClient.Builder(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected");
        mSignInButton.setEnabled(false);
        mSignOutButton.setEnabled(true);
        mRevokeAccessButton.setEnabled(true);

        mSignInProgress = STATE_SIGNED_IN;
        Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        mSignInStatusTxt.setText(String.format("Signed In to G+ as %s", currentUser.getDisplayName()));
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
        Log.i(TAG, "onConnectionSuspended" + cause);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode()= " + connectionResult.getErrorCode());
        if(mSignInProgress != STATE_IN_PROGRESS){
            mSignInIntent = connectionResult.getResolution();
            mSingInError = connectionResult.getErrorCode();

            if(mSignInProgress == STATE_SIGN_IN){
                resolveSignInError();
            }
        }
        onSignedOut();
    }

    private void resolveSignInError(){
        if(mSignInIntent != null){
            try {
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);

            }catch (IntentSender.SendIntentException e){
                Log.i(TAG, "sign in intent could not be sent: " + e.getLocalizedMessage());
                mSignInProgress = STATE_SIGN_IN;
            }
        }else{
            showDialog(DIALOG_PLAY_SREVICE_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case RC_SIGN_IN:
                if(resultCode == RESULT_OK){
                    mSignInProgress = STATE_SIGN_IN;
                }else {
                    mSignInProgress = STATE_SIGNED_IN;
                }

                if(!mGoogleApiClient.isConnecting()){
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    private void onSignedOut(){
        mSignInButton.setEnabled(true);
        mSignOutButton.setEnabled(false);
        mRevokeAccessButton.setEnabled(false);

        mSignInStatusTxt.setText("Signed out");
    }
}
