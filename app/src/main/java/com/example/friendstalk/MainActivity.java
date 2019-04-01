package com.example.friendstalk;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences mSharedPreferences;
    private TextView mUsernameTV;
    private EditText mCallNumET,room_et;

    private Pubnub mPubNub;

    private String username;
    ImageButton callBtn;

    /**
     * TODO: "Login" by subscribing to PubNub channel + Constants.SUFFIX
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCallNumET = (EditText) findViewById(R.id.call_num);
        room_et = (EditText) findViewById(R.id.room_et); //имя комнаты для трансляции потоков
        mUsernameTV = (TextView) findViewById(R.id.main_username);

        callBtn = findViewById(R.id.call_btn);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeLog(TAG,"makeCall");
                //совершить звонок

                //получаем имя пользователя которому намерены позвонить
                String callNum = mCallNumET.getText().toString();

                //проверяем на валидность введеного имени
                if (callNum.isEmpty() || callNum.equals(username)) {
                    Toast.makeText(MainActivity.this, "Enter a valid number.", Toast.LENGTH_SHORT).show();
                }else {
                    //если все хорошо тогда совершаем звонок
                    Log.d(TAG,callNum);
                    dispatchCall(callNum);
                }
            }
        });

        makeLog(TAG,"onCreate");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        // Return to Log In screen if no user is logged in.
        if (!mSharedPreferences.contains(Constants.USER_NAME)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        username = mSharedPreferences.getString(Constants.USER_NAME, "");



        mUsernameTV.setText(username);  // Set the username to the username text view

        //TODO: Create and instance of Pubnub and subscribe to standby channel
        // In pubnub subscribe callback, send user to your VideoActivity
        initPubNub();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;

            case R.id.action_sign_out:
                signOut();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Log out, remove username from SharedPreferences, unsubscribe from PubNub, and send user back
     * to the LoginActivity
     */
    public void signOut() {
        // TODO: Unsubscribe from all channels with PubNub object ( pn.unsubscribeAll() )
        makeLog(TAG,"signOut");
        SharedPreferences.Editor edit = this.mSharedPreferences.edit();
        edit.remove(Constants.USER_NAME);
        edit.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", username);
        startActivity(intent);
    }

    public void initPubNub() {
        makeLog(TAG,"initPubNub");
        //получаем имя пользователя
        String stdbyChannel = username + Constants.STDBY_SUFFIX;
        //инициализируем сигнал с нужными для него параметрами
        mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
            //инициализируем уникальный id для ининтификации пользователя
        mPubNub.setUUID(username);

        try {
            //инициализируем наш ключь и добавляем колбек для переопределения методов
            mPubNub.subscribe(stdbyChannel, new Callback() {

                //в случаее успешного соединения
                @Override
                public void successCallback(String channel, Object message) {

                    makeLog(TAG,"successCallback");

                    Log.d("MA-success", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    // instanceof
                    // - это ключевое слово, которое используется для проверки,
                    //содержит ли ссылочная переменная заданный тип ссылки на объект или нет.

                    //получаем содержимое json для подключения
                    JSONObject jsonMsg = (JSONObject) message;

                    try {
                        //если содержимое не равно позвонить пользователю мы выходим
                        if (!jsonMsg.has(Constants.JSON_CALL_USER)) return;

                        //если проинициализировано то мы получаем троку в которой указано кому позвонить
                        String user = jsonMsg.getString(Constants.JSON_CALL_USER);

                        int user_num = mSharedPreferences.getInt(Constants.USER_NUM,0);


                        /*if (user_num<=0){*/
                            // Consider Accept/Reject call here
                            //зесь мы инициализируем открытие нового окна для запуска видео/аудио трансляции
                            Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
                            //кладем все нужное содержимое в переменные для передачи в другое activity
                            intent.putExtra(Constants.USER_NAME, username);
                            intent.putExtra(Constants.JSON_CALL_USER, user);
                            //запускаем новое activity
                            Log.d(TAG," " +user_num );
                            user_num = 0;
                            SharedPreferences.Editor edit = mSharedPreferences.edit();
                            edit.putInt(Constants.USER_NUM,user_num);
                            edit.apply();
                            startActivity(intent);
                            Log.d("Fix","открыл окно чата когда позвонили мне!");
                            //завершаем старое activity
                            finish();
                       /* }else{
                            //TODO здесь мы должны подключиться к потоку но без запуска активити
                        }*/

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("Error","error JSONException = " + e.getMessage());
                    }
                }
            });
        } catch (PubnubException e) {
            e.printStackTrace();
            Log.d("Error","error PubnubException = " + e.getMessage());
        }
    }

    public void makeCall(View view) {
        makeLog(TAG,"makeCall");
        //совершить звонок

        //получаем имя пользователя которому намерены позвонить
        String callNum = mCallNumET.getText().toString();

        //проверяем на валидность введеного имени
        if (callNum.isEmpty() || callNum.equals(username)) {
            Toast.makeText(this, "Enter a valid number.", Toast.LENGTH_SHORT).show();
        }else {
            //если все хорошо тогда совершаем звонок
            dispatchCall(callNum);
        }

    }

    public void dispatchCall(final String callNum) {
        makeLog(TAG,"dispatchCall");
        //создаем поток для звонка
        final String callNumStdBy = callNum + Constants.STDBY_SUFFIX;
        //создаем json обьект для совершения звонка
        JSONObject jsonCall = new JSONObject();
        try {
            //кладем в него нужную нам информацию для отправки
            jsonCall.put(Constants.JSON_CALL_USER, username);

            mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {

                    makeLog(TAG,"successCallback");

                    Log.d("MA-dCall", "SUCCESS: " + message.toString());
                    //открываем окно трансляции видео звонка
                    Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
                    intent.putExtra(Constants.USER_NAME, username);
                    intent.putExtra(Constants.CALL_USER, callNum);
                    startActivity(intent);
                    //завершаем текущую активность
                    finish();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeLog(TAG,"END dispatchCall");
    }

    @Override
    protected void onPause() {
        super.onPause();
        makeLog(TAG,"onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeLog(TAG,"onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        makeLog(TAG,"onDestroy");
    }

    public static void makeLog(String TAG, String messege){
        Log.d(TAG,messege);
    }
}