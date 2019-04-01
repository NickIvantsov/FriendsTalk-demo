package com.example.friendstalk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.concurrent.TimeUnit;

import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnRTCListener;

import static com.example.friendstalk.MainActivity.*;

public class VideoChatActivity extends Activity {

    private static final String TAG = "VideoChatActivity";

    public static final String VIDEO_TRACK_ID = "videoPN"; //уник
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView mVideoView,mVideoView2,mVideoView3,mVideoView4;

    private String username;

    String[] perms = {"android.permission.RECORD_AUDIO", "android.permission.CAMERA"};

    int permsRequestCode = 200;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        //задаем вопрос о разрешении на сьемку
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, permsRequestCode);
        }

        makeLog(TAG,"onCreate");

        Context mContext = getApplicationContext();
        //region
        //получаем нужные значения которые мы вложили в предвидущем активити
        Bundle extras = getIntent().getExtras();
        //проверка на правильное содержимое оно не пустое и содержит ключь для звонка
        if (extras == null || !extras.containsKey(Constants.USER_NAME)) {
            //если не содержит то мы отправляем пользователя обратно в главный класс
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(mContext, "Need to pass username to VideoChatActivity in intent extras (Constants.USER_NAME).",
                    Toast.LENGTH_SHORT).show();
            //завершаем текущую активность
            finish();
            return;
        }
        //получаем имя текущего пользователя и записываем её в переменную
        username = extras.getString(Constants.USER_NAME, "");
        //endregion


        //region #1 инициализируем подключение с нужными для нас параметрами
        PeerConnectionFactory.initializeAndroidGlobals(
                mContext,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();

        pnRTCClient = new PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, username);

        //endregion

        // Сначала нам нужно установить GLSurfaceView, который должен отображаться
        mVideoView = findViewById(R.id.gl_surface);



        //region
        // Returns the number of cams & front/back face device name
        //Выбираем какую из камер мы будем использовать для трансляции потока
        int camNumber = VideoCapturerAndroid.getDeviceCount(); //возращаем количество имеющихся камер
        final String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice(); //получаем фронтальную камеру
        String backFacingCam = VideoCapturerAndroid.getNameOfBackFacingDevice(); //получаем основную камеру

        // Creates a VideoCapturerAndroid instance for the device name
        // Создает экземпляр VideoCapturerAndroid для имени устройства
        final VideoCapturerAndroid capturer = (VideoCapturerAndroid) VideoCapturerAndroid.create(frontFacingCam);
        SystemClock.sleep(800);
        //region Как вариант
       /* runOnUiThread(new Runnable(){
                          public void run() {
                              //make call to your second activity
                              capturer[0] = (VideoCapturerAndroid) VideoCapturerAndroid.create(frontFacingCam);
                          }
                      });*/
        //endregion


        // First create a Video Source, then we can make a Video Track
        // Сначала мы создаем VideoSource
        localVideoSource = pcFactory.createVideoSource(capturer, pnRTCClient.videoConstraints());
       // new MyCamListener(pcFactory,capturer);

        SystemClock.sleep(1500);

        // Как только мы это получим, мы можем создать наш VideoTrack
        // Обратите внимание, что VIDEO_TRACK_ID может быть любой строкой, которая уникально
        // идентифицирует эту видео дорожку в вашем приложении
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        // First we create an AudioSource then we can create our AudioTrack
        // Сначала мы создаем AudioSource
        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());

        // Как только мы это получим, мы можем создать наш AudioTrack
        // Обратите внимание, что AUDIO_TRACK_ID может быть любой строкой, которая уникально
        // идентифицирует эту звуковую дорожку в вашем приложении
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        //endregion

        //region


        // VideoChatActivity#onCreate()
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);
        //endregion


        //region
        // Then we set that view, and pass a Runnable to run once the surface is ready
        // Затем мы устанавливаем это представление и передаем Runnable
        // запускать, когда поверхность готова
        VideoRendererGui.setView(mVideoView, null);//TODO тест добавления потока
        SystemClock.sleep(1000);

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom

        // Теперь, когда VideoRendererGui готов, мы можем получить наш VideoRenderer
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_BALANCED, false);//SCALE_ASPECT_FILL

        // И наконец, с нашим готовым VideoRenderer, мы
        // можем добавить наш рендерер в VideoTrack.
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_BALANCED, true);

        //endregion

/*//region  //TODO задержка
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {

            }
        }, 1000); //specify the number of milliseconds
        //region*/

        //region
        // First attach the RTC Listener so that callback events will be triggered
        //SystemClock.sleep(3000);
        makeLog(TAG,"до attachRTCListener new MyRTCListener()");
        this.pnRTCClient.attachRTCListener(new MyRTCListener());
        /*SystemClock.sleep(1500);*/
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        makeLog(TAG,"до this.pnRTCClient.listenOn(this.username");
        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient.listenOn(this.username);//TODO #3
        SystemClock.sleep(1500);
        this.pnRTCClient.setMaxConnections(4);
        SystemClock.sleep(1500);
        // If Constants.CALL_USER is in the intent extras, auto-connect them.
        if (extras.containsKey(Constants.CALL_USER)) {
            //получаем имя для подключения пользователя
            String callUser = extras.getString(Constants.CALL_USER, "");
            //подключаемся к пользователю
            this.pnRTCClient.connect(callUser);
        }
        //endregion


    }

    private boolean canMakeSmores(){
    //проверяем какая ОС сейчас используеться это нужно для того чтобы вызвать разрешение
        return(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1);

    }

    public void connectToUser(String user) {
        makeLog(TAG,"connectToUser");
        this.pnRTCClient.connect(user);

    }

    private class MyCamListener {

        MyCamListener(final PeerConnectionFactory pcFactory,final VideoCapturer capturer){
            new Thread(new Runnable() {
                @Override
                public void run() {

                    ///TODO доделать
                    localVideoSource = pcFactory.createVideoSource(capturer, pnRTCClient.videoConstraints());
                }
            });
        }
    }


    //region
    private class MyRTCListener extends PnRTCListener {

        // Override methods you plan on using
        //region
        @Override
        public void onLocalStream(final MediaStream localStream) {
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makeLog(TAG,"run at onLocalStream"); //TODO #1
                    //если пользовательский поток видео равен 0 мы выходим
                    if (localStream.videoTracks.size() == 0) return;
                    //добавляем поток видео в пользовательский поток клиента
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            });
            makeLog(TAG,"после run at onLocalStream");
        }//TODO #2
        //endregion

        //region
        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            makeLog(TAG,"onAddRemoteStream");
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makeLog(TAG,"run at onAddRemoteStream"); //TODO #5 проверяем что за тип подключение
                    Toast.makeText(VideoChatActivity.this, "Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();
                    try {
                        if (remoteStream.videoTracks.size() == 0) return;


                            remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                            VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);//SCALE_ASPECT_FILL
                            VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);//SCALE_ASPECT_FIT

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        //endregion
        //region
        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            makeLog(TAG,"onPeerConnectionClosed");
            Intent intent = new Intent(VideoChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        //endregion

        @Override
        public void onMessage(PnPeer peer, Object message) {
            makeLog(TAG,"onMessage");
            if (!(message instanceof JSONObject)) return; //Ignore if not JSONObject
            JSONObject jsonMsg = (JSONObject) message;
            try {
                String user = jsonMsg.getString("msg_user");
                String text = jsonMsg.getString("msg_text");
                final ChatMessage chatMsg = new ChatMessage(user, text);
                VideoChatActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(VideoChatActivity.this, chatMsg.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }
    //endregion

    @Override

    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){

            case 200:

                boolean audioAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;

                boolean cameraAccepted = grantResults[1]==PackageManager.PERMISSION_GRANTED;

                break;

        }

    }

    //region Кнопка зависания
    public void hangup(View view) {
        makeLog(TAG,"hangup");
        //завершаем все подключение к текущему пользователю
        pnRTCClient.closeAllConnections(); //TODO здесь была ошибка при закрытии  FATAL EXCEPTION: main
        //Process: com.example.friendstalk, PID: 29842
       // java.lang.IllegalStateException: Could not execute method of the activity
        //переходим в другую активити
        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
        //завершаем текущую активити
        finish();
    }
    //endregion

    //region
    @Override
    protected void onPause() {
        super.onPause();

        makeLog(TAG,"onPause");
        this.mVideoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeLog(TAG,"onResume");
        this.mVideoView.onResume();
        this.localVideoSource.restart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        makeLog(TAG,"onDestroy");
        if (this.localVideoSource != null) {
            this.localVideoSource.stop();
        }
        if (this.pnRTCClient != null) {
            this.pnRTCClient.onDestroy();
        }
    }
    //endregion

    //region Сообщения
    //region
    private void sendMsg(String msgText) {
        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("msg_user", username);
            msgJson.put("msg_text", msgText);
            this.pnRTCClient.transmitAll(msgJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //endregion



    //endregion


}
