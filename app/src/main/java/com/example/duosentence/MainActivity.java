package com.example.duosentence;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Button;
import android.content.res.AssetFileDescriptor;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private TextView sentenceBlock;
    private String inputNo;
    private String sentence;
    private int musicIndex;
    private ConfigPropUtil config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 文章表示
        sentenceBlock = findViewById(R.id.sentence);
        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);
        // 曲インデックス初期値１
        musicIndex = 1;
        // プロパティファイル読み込み
        config = new ConfigPropUtil();

        // リスナーをボタンに登録
        buttonStart.setOnClickListener( v ->  {
            // 番号指定
            inputNo = ((EditText)findViewById(R.id.inputNo)).getText().toString();
            if (mediaPlayer != null) {
                // 音楽停止
                audioStop();
            }
            // 音楽再生
            audioPlay();
        });

//        // 音楽停止ボタン
//        Button buttonStop = findViewById(R.id.stop);
//
//        // リスナーをボタンに登録
//        buttonStop.setOnClickListener( v -> {
//            if (mediaPlayer != null) {
//                // 音楽停止
//                audioStop();
//            }
//        });

        // 前へボタン
        Button buttonPrev = findViewById(R.id.prev);

        // リスナーをボタンに登録
        buttonPrev.setOnClickListener( v -> {
            if (mediaPlayer != null) {
                // 音楽停止
                audioStop();
            }
            musicIndex--;
            if (musicIndex < 1) {
                musicIndex = 560;
            }
            // 音楽再生
            ((EditText)findViewById(R.id.inputNo)).setText(String.valueOf(musicIndex));
            inputNo = "";
            audioPlay();
        });

        // 次へボタン
        Button buttonNext = findViewById(R.id.next);

        // リスナーをボタンに登録
        buttonNext.setOnClickListener( v -> {
            if (mediaPlayer != null) {
                // 音楽停止
                audioStop();
            }
            musicIndex++;
            if (musicIndex > 560) {
                musicIndex = 1;
            }
            // 音楽再生
            ((EditText)findViewById(R.id.inputNo)).setText(String.valueOf(musicIndex));
            inputNo = "";
            audioPlay();
        });
    }


    private boolean audioSetup(){
        // インタンスを生成
        mediaPlayer = new MediaPlayer();
        boolean fileCheck = false;

        //音楽ファイル名, あるいはパス
        String filePath = "sentence001.mp3";
        if (!"".equals(inputNo)) {
            musicIndex = Integer.parseInt(inputNo);
        }
        if (musicIndex < 1 || musicIndex > 560) {
            return fileCheck;
        }
        filePath = filePath.replaceAll("001", String.format("%03d", musicIndex));

        // assetsから mp3 ファイルを読み込み
        try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath)) {
            // MediaPlayerに読み込んだ音楽ファイルを指定
            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    private void audioPlay() {

        if (mediaPlayer == null) {
            // audio ファイルを読出し
            if (audioSetup()){
                sentence = (String) config.get(String.valueOf(musicIndex));
//                System.out.println(sentence);
//                Toast.makeText(getApplication(), "Read audio file", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
//            System.out.println("繰り返し");
        }

        // 再生する
        mediaPlayer.start();
        sentenceBlock.setText(sentence);

        // 終了を検知するリスナー
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                Log.d("debug","end of audio");
//                audioStop();
//            }
//        });
        // lambda
        mediaPlayer.setOnCompletionListener( mp -> {
            Log.d("debug","end of audio");
            audioStop();
        });

    }

    private void audioStop() {
        // 再生終了
        mediaPlayer.stop();
        // リセット
        mediaPlayer.reset();
        // リソースの解放
        mediaPlayer.release();

        mediaPlayer = null;
    }
}