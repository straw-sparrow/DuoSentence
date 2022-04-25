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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private TextView sentenceBlock;
    private String inputNo;
    private String sentence;
    private String sentenceBlank;
    private int musicIndex;
    private int prevMusicIndex;
    private ConfigPropUtil config;
    Switch autoPlay;
    Switch fillBlank;
    SeekBar sBar;

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
        // 割合
        sBar = findViewById(R.id.seekBar);
        sBar.setProgress(50);
        sBar.setMax(100);

        // 曲インデックス初期値１
        musicIndex = 1;
        prevMusicIndex = 0;
        // プロパティファイル読み込み
        config = new ConfigPropUtil();

        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);
        // リスナーをボタンに登録
        buttonStart.setOnClickListener( v ->  {
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
            playMusic("");
        });
    }

    private boolean audioSetup(){
        // インタンスを生成
        mediaPlayer = new MediaPlayer();
        boolean fileCheck = false;

        //音楽ファイル名, あるいはパス
        String filePath = "sentence001.mp3";
        musicIndex = Integer.parseInt(inputNo);
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

        sentence = (String) config.get(inputNo);
        setSentenceBlock(flg);
        if (mediaPlayer == null) {
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
            System.out.println(Integer.parseInt(inputNo));
            System.out.println(prevMusicIndex);
            if (flg == null && sentenceBlank != null && Integer.parseInt(inputNo) == prevMusicIndex) {
                sentenceBlock.setText(sentenceBlank);
            } else {
                sentenceBlank = changeSentence(sentence);
                sentenceBlock.setText(
                        sentenceBlank
                                .replaceAll(" \" ", "\"")
                                .replaceAll(" \\. ", "\\.")
                                .replaceAll("\n ", "\n")
                );
            }
        } else {
            sentenceBlock.setText(
                    sentence
                            .replaceAll(" \" ", "\"")
                            .replaceAll(" \\. ", "\\.")
                            .replaceAll("\n ", "\n")
            );
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
        prevMusicIndex = musicIndex;
        inputNo = ((EditText)findViewById(R.id.inputNo)).getText().toString();
        if ("".equals(inputNo)){
            inputNo = "1";
        }
        if (mediaPlayer != null) {
            // 音楽停止
            audioStop();
        }
        if ("+".equals(flg)) {
            musicIndex++;
            if (musicIndex > 560) {
                musicIndex = 1;
            }
            inputNo = String.valueOf(musicIndex);
        } else if ("-".equals(flg)) {
            musicIndex--;
            if (musicIndex < 1) {
                musicIndex = 560;
            }
            inputNo = String.valueOf(musicIndex);
        }
        // 音楽再生
        audioPlay(flg);
        ((EditText)findViewById(R.id.inputNo)).setText(inputNo);
    }

    private String changeSentence(String sentence) {
        String sentenceEng = sentence.split("\r\n|\r|\n")[1];
        String words[] = sentenceEng.split(" |\\.|,|\\!|\\?");
        return getBlankSentence(sentence, new ArrayList<String>(Arrays.asList(words)));
    }

    private String getBlankSentence(String sentence, List<String> wordsList) {
        wordsList.removeAll(Arrays.asList("","\""));
        int len = wordsList.size();
        int count = 0;
        int countlimit = (int)(sBar.getProgress() * len/100);
        int forceEnd = 0;
        while(count < countlimit && forceEnd < len + 1) {
            int index = (int)(Math.random()* (len - count));
            String word = wordsList.get(index);
            if(word.matches("[a-zA-Z|0-9|'|\\-|\\$]+")) {
                String changedSentence = replaceStr(sentence, word);
                if(!sentence.equals(changedSentence)) {
                    sentence = changedSentence;
                    count++;
                    wordsList.remove(index);
                }
            }

            // 無限ループ対策
            forceEnd++;
        }
        return sentence;
    }

    private String replaceStr(String sentence, String word) {
        sentence = replaceStrReg(sentence, word, " ", " ");
        sentence = replaceStrReg(sentence, word, "\"", " ");
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