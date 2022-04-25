package com.example.duosentence;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Button;
import android.content.res.AssetFileDescriptor;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private TextView sentenceBlock;
    private String inputNo;
    private String sentence;
    private String sentenceBlank;
    private int musicIndex;
    private ConfigPropUtil config;
    Switch autoPlay;
    Switch fillBlank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 文章表示
        sentenceBlock = findViewById(R.id.sentence);
        // 連続再生スイッチ
        autoPlay = findViewById(R.id.autoSwitch);
        // 虫食いモード
        fillBlank = findViewById(R.id.fillBlank);

        // 曲インデックス初期値１
        musicIndex = 1;
        // プロパティファイル読み込み
        config = new ConfigPropUtil();

        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);
        // リスナーをボタンに登録
        buttonStart.setOnClickListener( v ->  {
            // 番号指定
            inputNo = ((EditText)findViewById(R.id.inputNo)).getText().toString();
            playMusic(null);
        });

        // 前へボタン
        Button buttonPrev = findViewById(R.id.prev);
        // リスナーをボタンに登録
        buttonPrev.setOnClickListener(v -> playMusic("-"));

        // 次へボタン
        Button buttonNext = findViewById(R.id.next);
        // リスナーをボタンに登録
        buttonNext.setOnClickListener(v -> playMusic("+"));

        // 虫食い時
        fillBlank.setOnCheckedChangeListener((fillBlank, isChecked) -> {
            inputNo = ((EditText)findViewById(R.id.inputNo)).getText().toString();
            playMusic("");
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

    private void audioPlay(String flg) {

        if (mediaPlayer == null) {
            sentence = (String) config.get(inputNo);
            setSentenceBlock(flg);
            // audio ファイルを読出し
            if (!audioSetup()){
                Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
        }

        // 再生する
        mediaPlayer.start();

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
            if(autoPlay.isChecked()){
                playMusic("+");
            }
        });

    }

    private void setSentenceBlock(String flg) {
        if(fillBlank.isChecked()){
            if (flg == null && sentenceBlank != null && musicIndex == Integer.parseInt(inputNo)) {
                sentenceBlock.setText(sentenceBlank);
            } else {
                sentenceBlank = changeSentence(sentence);
                sentenceBlock.setText(sentenceBlank);
            }
        } else {
            sentenceBlock.setText(sentence);
        }
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

    private void playMusic(String flg) {
        if (mediaPlayer != null) {
            // 音楽停止
            audioStop();
        }
        if ("+".equals(flg)) {
            musicIndex++;
            if (musicIndex > 560) {
                musicIndex = 1;
            }
            // 音楽再生
            ((EditText)findViewById(R.id.inputNo)).setText(String.valueOf(musicIndex));
            inputNo = "";
        } else if ("-".equals(flg)) {
            musicIndex--;
            if (musicIndex < 1) {
                musicIndex = 560;
            }
            // 音楽再生
            ((EditText)findViewById(R.id.inputNo)).setText(String.valueOf(musicIndex));
            inputNo = "";
        }
        audioPlay(flg);
    }

    private String changeSentence(String sentence) {
        String sentenceEng = sentence.split("\r\n|\r|\n")[1];
        String engWords[] = sentenceEng.split(" |\\.|,|\\!|\\?");
        return getBlankSentence(sentence, engWords);
    }

    private String getBlankSentence(String sentence, String[] words) {
        int len = words.length;
        int count = 0;
        int forceEnd = 0;
        while(count < 2 && forceEnd<10) {
            int index = (int)(Math.random()*len);
            String word = words[index];
            System.out.println("■■■■■■word■■■■■" + word);
            if(word.matches("[a-zA-Z]+")) {
                String changedSentence = replaceStr(sentence, word);
                if(!sentence.equals(changedSentence)) {
                    sentence = changedSentence;
                    count++;
                }
            }

            // 無限ループ対策
            forceEnd++;
        }
        System.out.println("■■■■■■sentence■■■■■" + sentence);
        return sentence;
    }

    private String replaceStr(String sentence, String word) {
        sentence = replaceStrReg(sentence, word, " ", " ");
        sentence = replaceStrReg(sentence, word, " ", "\\.");
        sentence = replaceStrReg(sentence, word, " ", ",");
        sentence = replaceStrReg(sentence, word, " ", "\\?");
        sentence = replaceStrReg(sentence, word, " ", "\\!");
        return sentence;
    }
    private String replaceStrReg(String sentence, String word, String prevRegex, String nextRegex){
        return sentence.replaceFirst(prevRegex + word + nextRegex, prevRegex + underScore(word.length()) + nextRegex);
    }

    private String underScore(int num) {
        StringBuilder sb = new StringBuilder();
        for(int i =0; i < num; i++) {
            sb.append("_");
        }
        return sb.toString();
    }
}