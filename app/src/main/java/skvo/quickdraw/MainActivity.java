package skvo.classification;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final String TAG = "MainActivity";

    private static final int PIXEL_WIDTH = 280;

    private TextView tvResult, tvTimer, tvTask;

    private float mLastX;

    private float mLastY;

    private DrawModel mModel;
    private DrawView mDrawView;

    private View detectButton;

    private PointF mTmpPoint = new PointF();

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private static final String MODEL_FILE = "file:///android_asset/graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/labels.txt";

    private static final int INPUT_SIZE = 28;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private int[] views = {R.id.imageView1,R.id.imageView2,R.id.imageView3,
            R.id.imageView4,R.id.imageView5,R.id.imageView6};
/*
    Bitmap bitmap;
    private int textureSize = 256;
    private int brushSize = 4;
    private int imageViewSize = 512; //уточнить
*/
    private volatile ArrayList<String> labels = new ArrayList<>();
    private Random random = new Random();
    private ArrayList<Integer> used;
    private ArrayList<Integer> tried;
    private final int countLabels = 296;
    private int currentTask;

    private MyCountDownTimer gameTimer;

    private ArrayList<Bitmap> bitmapStorage;
    private ArrayList<Boolean> resultStorage;

    private int currentRound;
    private final int roundsInGame = 6;
/*
    private float testApple[] = {  0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,  36, 109,  16,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0, 183, 255, 136,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   2, 243, 255, 209,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   7, 255, 255, 197,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   7, 255, 255, 122,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,  69, 217, 229, 133, 255, 255,
            52,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,  12, 150, 254, 208, 168, 255,
            254, 255, 196, 187, 187, 187, 179, 170, 130,  25,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,  42, 221, 251, 131,   6,
            0,  82, 232, 242, 189, 187, 187, 191, 204, 205, 250, 249,  93,
            0,   0,   0,   0,   0,   0,   0,   0,   0,  20, 222, 225,  52,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  28,
            181, 251,  50,   0,   0,   0,   0,   0,   0,   0,   0, 166, 243,
            46,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0, 102, 255, 102,   0,   0,   0,   0,   0,   0,   0,  16,
            247, 137,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,  72, 251, 179,   4,   0,   0,   0,   0,   0,   0,
            0,  92, 255,  44,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0, 197, 211,  10,   0,   0,   0,   0,   0,
            0,   0,   0, 140, 239,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0, 188, 192,   0,   0,   0,   0,
            0,   0,   0,   0,   0, 181, 199,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0, 145, 254, 174,   0,
            0,   0,   0,   0,   0,   0,   0, 222, 158,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  56, 255,
            141,  96, 118,   7,   0,   0,   0,   0,   9, 253, 118,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            79, 255, 217, 255, 255,  44,   0,   0,   0,   0,  37, 255,  85,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   9, 141, 186, 220, 238,   3,   0,   0,   0,   0,  43,
            255,  79,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0, 214, 174,   0,   0,   0,   0,
            0,  47, 255,  75,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,  46, 254, 105,   0,   0,
            0,   0,   0,  52, 255,  70,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0, 182, 234,  13,
            0,   0,   0,   0,   0,  56, 255,  65,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  69, 255,
            108,   0,   0,   0,   0,   0,   0,  52, 255, 139,  13,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  38,
            236, 209,   6,   0,   0,   0,   0,   0,   0,   1, 161, 254, 238,
            134,  24,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   4,
            111, 239, 228,  28,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            45, 162, 250, 248, 159, 109,  77,  46,  15,   0,   0,  13,  82,
            155, 229, 255, 174,  29,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,  29, 141, 234, 255, 255, 255, 255, 255, 255,
            255, 255, 233, 161,  67,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,  15,  45,  77, 109,
            119, 119, 113,  54,   2,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
            0,   0,   0,   0};
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mModel = new DrawModel(PIXEL_WIDTH, PIXEL_WIDTH);

        mDrawView = (DrawView) findViewById(R.id.view_draw);
        mDrawView.setModel(mModel);
        mDrawView.setOnTouchListener(this);
/*
        detectButton = findViewById(R.id.buttonDetect);
        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDetectClicked();
            }
        });
*/
        View clearButton = findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClicked();
            }
        });

        tvResult = (TextView) findViewById(R.id.tvResult);
        tvTask = (TextView) findViewById(R.id.tvTask);
        tvTimer = (TextView) findViewById(R.id.tvTimer);

        readLabels();
        initTensorFlowAndLoadModel();
/*
        //test
        int[] pixels = new int[28*28];
        float[] pix_float = new float[28*28];
        Bitmap pic = BitmapFactory.decodeResource(getResources(), R.drawable.airplane);
        Log.d("pic width", pic.getWidth()+"");
        Log.d("pic height", pic.getHeight()+"");
        pic.getPixels(pixels, 0, 28, 0,0, 28,28);

        for (int i = 0; i < pixels.length; i++) {
            // Set 0 for white and 255 for black pixel
            ////Log.d("pixels " + i, pixels[i]+"");
            int pix = pixels[i];
            float b = pix & 0xff;
            //Log.d("pixels " + i, b+"");
            //retPixels[i] = 0xff - b;
            pix_float[i] = (0xff - b)/255f;


        }

        for (int i =0; i<testApple.length; i++){
            pix_float[i] = testApple[i]/255f;
        }

        float max =0;
        for (int i=0; i<pix_float.length; i++){
            if (pix_float[i] > 0){
                pix_float[i] = 1;
                Log.d("pixels " + i, pix_float[i]+"");
            }
        }





        final List<Classifier.Recognition> results = classifier.recognizeImage(pix_float);

        for (Classifier.Recognition result: results) {
            Log.d("recognition", result.getTitle() + " " + result.getConfidence());
        }

        //end test
*/
        newGame();

    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);

/*
                    //Log.e("bitmap info" , bitmap.getWidth() + "");
                    final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                    for (Classifier.Recognition result: results){
                        Log.e("recognition", result.getTitle() + " " + result.getConfidence());
                    }
                    */
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onDetectClicked() {
        float pixels[] = mDrawView.getPixelData();

        final List<Classifier.Recognition> results = classifier.recognizeImage(pixels);

        if (results.size() > 0) {
            String value = " Number is : " +results.get(0).getTitle();
            tvResult.setText(value);

            for (Classifier.Recognition result: results){
                Log.e("recognition", result.getTitle() + " " + result.getConfidence());
            }
        }
    }

    private void recognize(){
        float pixels[] = mDrawView.getPixelData();

        if (null == pixels) return;

        int count_non_zero = 0;
        for (float pixel : pixels){
            if (pixel != 0) count_non_zero++;
        }

        if (count_non_zero<50) return;

/*
        for (int i=0; i<pixels.length; i++){
            //float temp = ((pixels[i]/255) - 1)*(-1);
            //pixels[i] = temp;
            Log.d("pixels " + i, pixels[i]+"");
        }
*/
        //Log.d("pixels.length", ""+pixels.length);

        final List<Classifier.Recognition> results = classifier.recognizeImage(pixels);
/*
        if (results.size() > 0) {
            String value = " Number is : " +results.get(0).getTitle();
            //tvResult.setText(value);

            for (Classifier.Recognition result: results){
                Log.e("recognition", result.getTitle() + " " + result.getConfidence());
            }
        }
*/


        for (Classifier.Recognition result: results){
            Log.d("recognition", result.getTitle() + " " + result.getConfidence());
            int recognition_id = Integer.parseInt(result.getId());
            if (!tried.contains(recognition_id)){
                if (recognition_id == currentTask ){
                    //win
                    gameTimer.cancel();
                    //success();
                    endRound(true);
                    break;
                } else {
                    tried.add(recognition_id);
                    tvResult.setText("Is it " + result.getTitle() + "?");
                    break;
                }
            }
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
        mDrawView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mDrawView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }
/*
    private void readLabels(){
    synchronized (labels) {

        Runnable runnable = new Runnable() {
            public void run() {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(getAssets().open(LABEL_FILE))); //throwing a FileNotFoundException?
                    String word;
                    while ((word = br.readLine()) != null)
                        labels.add(word); //break txt file into different words, add to wordList
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br.close(); //stop reading
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
    }
*/
    private void newGame(){
        currentRound =0;
        bitmapStorage = new ArrayList<>();
        resultStorage = new ArrayList<>();

        used = new ArrayList<>();

        createQuest();
    }

    private void createQuest(){
        currentRound++;
        currentTask = random.nextInt(countLabels);
        if (used.contains(currentTask)){
            while (!used.contains(currentTask)){
                currentTask = (++currentTask)%countLabels;
            }
        }
        used.add(currentTask);

        tried = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Draw")
                .setMessage(labels.get(currentTask) + " in 20 seconds")
                //.setIcon(R.drawable.ic_android_cat)
                .setCancelable(false)
                .setNegativeButton("Lets go!",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                createTimer();
                                tvTask.setText("Draw: " + labels.get(currentTask));
                                onClearClicked();

                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }
/*
    private void success(){

        bitmapStorage.add(mDrawView.getBitmap());
        resultStorage.add(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.succ_title))
                .setMessage(getString(R.string.succ_text) + labels.get(currentTask))
                //.setIcon(R.drawable.ic_android_cat)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.succ_next),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                createQuest();
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void fail(){
        bitmapStorage.add(mDrawView.getBitmap());
        resultStorage.add(false);
        createQuest();
    }
   */
    private void endRound(boolean win){
        Bitmap temp = mDrawView.getBitmap();
        if (null == temp) return;
        bitmapStorage.add(Bitmap.createBitmap(temp));
        resultStorage.add(win);

        if (currentRound == roundsInGame){
            endGame();
        } else {

            if (win) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.succ_title))
                        .setMessage(getString(R.string.succ_text) + " " + labels.get(currentTask))
                        //.setIcon(R.drawable.ic_android_cat)
                        .setCancelable(false)
                        .setNegativeButton(getString(R.string.succ_next),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        createQuest();
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                createQuest();
            }
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
                v.setBackgroundColor(Color.GREEN);
            } else {
                v.setBackgroundColor(Color.RED);
            }
            v.setImageBitmap(b);
        }

        dialog.setTitle(getString(R.string.dlg_repeat));

        Button btn_cancel = (Button)(dialog.findViewById(R.id.btn_cancel));
        Button btn_next = (Button)(dialog.findViewById(R.id.btn_next));

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StartActivity.class));
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.cancel();
            }
        });

        dialog.show();

    }

    private void readLabels(){
        String actualFilename = LABEL_FILE.split("file:///android_asset/")[1];
        Log.i(TAG, "Reading labels from: " + actualFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!" , e);
        }
    }

    private void createTimer(){
        gameTimer = new MyCountDownTimer(21000, 1000) {

            public void onTick(long millisUntilFinished) {
                tvTimer.setText((millisUntilFinished / 1000) + "");
                if (millisUntilFinished / 1000 <18) {
                    if ((millisUntilFinished/1000)%2==0) {
                        recognize();
                    }
                    //onDetectClicked();
                }
            }

            public void onFinish() {
                tvTimer.setText("ooops");
                //fail();
                endRound(false);
//                mTextField.setText("done!");
            }
        };

        gameTimer.start();

    }
/*
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //startActivity(new Intent(MainActivity.this, StartActivity.class));
        onPause();
    }*/
}
