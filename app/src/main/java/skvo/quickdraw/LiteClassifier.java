package skvo.quickdraw;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class LiteClassifier {

    private static final String TAG = "FeLite";

    private int imgSize = 224;

    private int numBytesPerChannel = 4; //for quantized model = 1, not = 4

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    protected ByteBuffer imgData = null;

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String pathToModel) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(pathToModel);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public LiteClassifier(AssetManager assetManager, String pathToModel) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, pathToModel));
        imgData =
                ByteBuffer.allocateDirect(
                        imgSize
                        * imgSize
                        * numBytesPerChannel);
        imgData.order(ByteOrder.nativeOrder());

        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    public float[] recognizeSketch(float[] pixels){
        float[][] out = new float[1][MainActivity.countLabels];
        tflite.run(pixels, out);
        return out[0];
    }


}
