package skvo.quickdraw;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.InterstitialAd;
//import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final String TAG = "MainActivity";
    private static final String ADMOB_APP_ID = "ca-app-pub-4902424516454995~7209343205";
    private static final String ADMOB_AD_ID = "ca-app-pub-4902424516454995/4746831397";

    private static final int PIXEL_WIDTH = 280;

    private TextView tvResult, tvTimer, tvTask;

    private float mLastX;

    private float mLastY;

    private DrawModel mModel;
    private DrawView mDrawView;

    private Button clearButton, nextButton, endButton;

    private LinearLayout back;

    private PointF mTmpPoint = new PointF();

    private LiteClassifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private static final String MODEL_FILE = "graph.tflite";

    private static final float THRESHOLD = 0.20f;
    private static final float SOFT_THRESHOLD = 0.01f;

    private int[] views = {R.id.imageView1,R.id.imageView2,R.id.imageView3,
            R.id.imageView4,R.id.imageView5,R.id.imageView6};

    private Random random = new Random();
    private ArrayList<Integer> used;
    private ArrayList<Integer> tried;
    public static final int countLabels = 282;
    private int currentTask;

    private MyCountDownTimer gameTimer;
    private boolean timerStart = false;
    private int time;

    private ArrayList<Bitmap> bitmapStorage;
    private ArrayList<Boolean> resultStorage;

    private int currentRound;
    private final int roundsInGame = 6;

    private int resId;
    private String labelName;

    private Tracker mTracker;
    //private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        back = (LinearLayout) findViewById(R.id.background);

        mModel = new DrawModel(PIXEL_WIDTH, PIXEL_WIDTH);

        mDrawView = (DrawView) findViewById(R.id.view_draw);
        mDrawView.setModel(mModel);
        mDrawView.setOnTouchListener(this);

        clearButton = (Button)findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClicked();
            }
        });

        nextButton = (Button)findViewById(R.id.buttonNext);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createQuest();
            }
        });

        endButton = (Button)findViewById(R.id.buttonEndGame);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endGame();
            }
        });
        endButton.setVisibility(View.GONE);

        tvResult = (TextView) findViewById(R.id.tvResult);
        tvTask = (TextView) findViewById(R.id.tvTask);
        tvTimer = (TextView) findViewById(R.id.tvTimer);

        initTensorFlowAndLoadModel();

        mTracker = getDefaultTracker();

        try{
            //MobileAds.initialize(this, ADMOB_APP_ID);
            //mInterstitialAd = new InterstitialAd(this);
            //mInterstitialAd.setAdUnitId(ADMOB_AD_ID);

        } catch (Exception e){
            Log.e(TAG, e.toString());
            sendError(e);
        }

        newGame();

    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = new LiteClassifier(getAssets(), MODEL_FILE);
                } catch (final Exception e) {
                    sendError(e);
                }
            }
        });
    }

    private void recognize(){
        float pixels[] = mDrawView.getPixelData();

        if (null == pixels) return;

        int count_non_zero = 0;
        for (float pixel : pixels){
            if (pixel != 0) count_non_zero++;
        }

        if (count_non_zero<40) return;

        try {
            final float[] results_prob = classifier.recognizeSketch(pixels);

            ArrayList<RecognitionResult> results = new ArrayList<>();

            for (int i=0; i<countLabels; i++){
                if (results_prob[i] > SOFT_THRESHOLD){
                    results.add(new RecognitionResult(i, results_prob[i]));
                }
            }

            ArrayList<Integer> recogn_ids = new ArrayList<>();

            for (RecognitionResult result: results){
                Log.d("recognition", result.getIdx() + " " + result.getProb());
                if (result.getProb() > THRESHOLD){
                    recogn_ids.add(result.getIdx());
                }
            }

            if (recogn_ids.contains(currentTask)){
                gameTimer.cancel();
                timerStart = false;
                endRound(true);

                tvResult.setText(getString(R.string.succ_title) + " " +
                        getString(R.string.succ_text) + " " + labelName);
                return;
            }

            if (results.isEmpty()){
                tvResult.setText(getString(R.string.dontknow));
            } else {
                for (RecognitionResult result : results) {
                    int recognition_id = result.getIdx();
                    if (!tried.contains(recognition_id)) {
                        if (result.getProb() > THRESHOLD) {
                            tried.add(recognition_id);
                            int loc_id = getResources().getIdentifier("label" + result.getIdx(), "string", getPackageName());
                            tvResult.setText(getString(R.string.isit) + " " +
                                    getString(loc_id) + "?");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e){
            sendError(e);
        }
    }

    private void onClearClicked() {
        mModel.clear();
        mDrawView.reset();
        mDrawView.invalidate();

        tried = new ArrayList<>();
        tvResult.setText("");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            processTouchDown(event);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }


    private void processTouchDown(MotionEvent event) {
        mLastX = event.getX();
        mLastY = event.getY();
        mDrawView.calcPos(mLastX, mLastY, mTmpPoint);
        float lastConvX = mTmpPoint.x;
        float lastConvY = mTmpPoint.y;
        mModel.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mDrawView.calcPos(x, y, mTmpPoint);
        float newConvX = mTmpPoint.x;
        float newConvY = mTmpPoint.y;
        mModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        mDrawView.invalidate();
    }

    private void processTouchUp() {
        mModel.endLine();
    }

    @Override
    protected void onResume() {

        try {
            if (timerStart) {
                createTimer(time);
            }
        }
        catch (Exception e){
            sendError(e);
        }

        mDrawView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {

        try{
            if (timerStart == true){
                time = gameTimer.getTime();
                gameTimer.cancel();
            }
        }
        catch (Exception e){
            sendError(e);
        }

        mDrawView.onPause();
        super.onPause();
    }

    private void newGame(){
        currentRound =0;
        bitmapStorage = new ArrayList<>();
        resultStorage = new ArrayList<>();

        used = new ArrayList<>();

        createQuest();
        loadAd();
    }

    private void createQuest(){
        back.setBackgroundColor(Color.parseColor("#ebf0ff"));

        currentRound++;
        currentTask = random.nextInt(countLabels);
        Log.d(TAG, "create currentTask " + currentTask);
        if (used.contains(currentTask)){
            while (!used.contains(currentTask)){
                currentTask = (++currentTask)%countLabels;
                Log.d(TAG, "change currentTask to " + currentTask);
            }
        }
        used.add(currentTask);

        tried = new ArrayList<>();

        nextButton.setVisibility(View.GONE);
        endButton.setVisibility(View.GONE);

        resId = getResources().getIdentifier("label" + currentTask, "string", getPackageName());
        labelName = getString(resId);


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.draw))
                .setMessage(labelName + " " + getString(R.string.quest_p2))
                //.setIcon(R.drawable.ic_android_cat)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.start_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                createTimer(21);
                                tvTask.setText(getString(R.string.draw) + " " + labelName);
                                onClearClicked();

                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void endRound(boolean win){
        Bitmap temp = mDrawView.getBitmap();
        if (null == temp) return;
        bitmapStorage.add(Bitmap.createBitmap(temp));
        resultStorage.add(win);

        if (win) {
            back.setBackgroundColor(Color.parseColor("#FFD3FFCE"));
            tvResult.setText(getString(R.string.succ_title) +" " +
                    getString(R.string.succ_text) + " " + labelName);
            sendAction("win");
        } else {
            back.setBackgroundColor(Color.parseColor("#FFFFC9CB"));
            sendAction("fail");
        }

        if (currentRound == roundsInGame){
            endButton.setVisibility(View.VISIBLE);
        } else {
            nextButton.setVisibility(View.VISIBLE);
        }

    }

    private void endGame(){

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.layout_endgame);

        for (int i=0; i<roundsInGame; i++){
            ImageView v = (ImageView) dialog.findViewById(views[i]);
            //v.toString();
            Bitmap b = bitmapStorage.get(i);
            //((ImageView)findViewById(views[i])).setImageBitmap(bitmapStorage.get(i));
            if (resultStorage.get(i)){
                //v.setBackgroundColor(Color.GREEN);
                v.setBackgroundDrawable(getResources().getDrawable(R.drawable.back_green));
            } else {
                //v.setBackgroundColor(Color.RED);
                v.setBackgroundDrawable(getResources().getDrawable(R.drawable.back_red));
            }
            v.setImageBitmap(b);
        }

        dialog.setTitle(getString(R.string.dlg_repeat));

        Button btn_cancel = (Button)(dialog.findViewById(R.id.btn_cancel));
        Button btn_again = (Button)(dialog.findViewById(R.id.btn_again));

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, StartActivity.class));
                //System.exit(0);
                //showAd();
                finish();

            }
        });

        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showAd();
                newGame();
                dialog.cancel();
            }
        });

        dialog.show();

    }

    private void createTimer(int sec){
        if (!timerStart) {
            timerStart = true;
            gameTimer = new MyCountDownTimer(sec * 1000, 1000) {

                public void onTick(long millisUntilFinished) {
                    tvTimer.setText((millisUntilFinished / 1000) + "");
                    if (millisUntilFinished / 1000 < 19) {
                        if ((millisUntilFinished / 1000) % 2 == 0) {
                            try {
                                recognize();
                            } catch (Exception e) {
                                sendError(e);
                            }
                        }
                        //onDetectClicked();
                    }
                }

                public void onFinish() {
                    tvTimer.setText(getString(R.string.oops));
                    //fail();
                    timerStart = false;
                    endRound(false);
//                mTextField.setText("done!");
                }
            };

            gameTimer.start();
        }

    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            try {
                //GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
                //mTracker = analytics.newTracker(getString(R.string.analyticsId));
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }
        return mTracker;
    }

    public void sendAction(String actionName){
        try {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setAction(""+currentTask).setCategory(actionName).build());
        } catch (Exception e) {
            sendError(e);
            //e.printStackTrace();
        }
    }

    public void sendError(Throwable error) {
        error.printStackTrace();
        try {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Error")
                    .setAction(error.toString())
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAd(){
        try {
            //mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }catch (Exception e){
            Log.e(TAG, e.toString());
            sendError(e);
        }
    }
/*
    private void showAd(){
        try {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                mTracker.send(new HitBuilders.EventBuilder()
                        .setAction("show Ads").setCategory("Ads").build());
            } else {
                loadAd();
                mTracker.send(new HitBuilders.EventBuilder()
                        .setAction("no Ads").setCategory("Ads").build());
            }
        }catch (Exception e){
            Log.e(TAG, e.toString());
            sendError(e);
        }
    }

 */

    private class RecognitionResult{
        int idx;
        float prob;

        public RecognitionResult(int idx, float prob) {
            this.idx = idx;
            this.prob = prob;
        }

        public int getIdx() {
            return idx;
        }

        public float getProb() {
            return prob;
        }
    }

}
