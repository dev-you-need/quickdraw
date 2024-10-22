package skvo.quickdraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by ml on 19.04.17.
 */

public class DrawView extends View {
    private Paint mPaint = new Paint();
    private DrawModel mModel;
    // 28x28 pixel Bitmap
    private Bitmap mOffscreenBitmap;
    private Canvas mOffscreenCanvas;

    private Matrix mMatrix = new Matrix();
    private Matrix mInvMatrix = new Matrix();
    private int mDrawnLineSize = 0;
    private boolean mSetuped = false;

    private float mTmpPoints[] = new float[2];

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setModel(DrawModel model) {
        this.mModel = model;
    }

    public void reset() {
        mDrawnLineSize = 0;
        if (mOffscreenBitmap != null) {
            mPaint.setColor(Color.WHITE);
            int width = mOffscreenBitmap.getWidth();
            int height = mOffscreenBitmap.getHeight();
            mOffscreenCanvas.drawRect(new Rect(0, 0, width, height), mPaint);
        }
    }

    private void setup() {
        mSetuped = true;

        // View size
        float width = getWidth();
        float height = getHeight();

        // Model (bitmap) size
        float modelWidth = mModel.getWidth();
        float modelHeight = mModel.getHeight();

        float scaleW = width / modelWidth;
        float scaleH = height / modelHeight;

        float scale = scaleW;
        if (scale > scaleH) {
            scale = scaleH;
        }

        float newCx = modelWidth * scale / 2;
        float newCy = modelHeight * scale / 2;
        float dx = width / 2 - newCx;
        float dy = height / 2 - newCy;

        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(dx, dy);
        mMatrix.invert(mInvMatrix);
        mSetuped = true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mModel == null) {
            return;
        }
        if (!mSetuped) {
            setup();
        }
        if (mOffscreenBitmap == null) {
            return;
        }

        int startIndex = mDrawnLineSize - 1;
        if (startIndex < 0) {
            startIndex = 0;
        }

        DrawRenderer.renderModel(mOffscreenCanvas, mModel, mPaint, startIndex);
        canvas.drawBitmap(mOffscreenBitmap, mMatrix, mPaint);

        mDrawnLineSize = mModel.getLineSize();
    }

    /**
     * Convert screen position to local pos (pos in bitmap)
     */
    public void calcPos(float x, float y, PointF out) {
        mTmpPoints[0] = x;
        mTmpPoints[1] = y;
        mInvMatrix.mapPoints(mTmpPoints);
        out.x = mTmpPoints[0];
        out.y = mTmpPoints[1];
    }

    public void onResume() {
        createBitmap();
    }

    public void onPause() {
        releaseBitmap();
    }

    private void createBitmap() {
        if (mOffscreenBitmap != null) {
            mOffscreenBitmap.recycle();
        }
        mOffscreenBitmap = Bitmap.createBitmap(mModel.getWidth(), mModel.getHeight(), Bitmap.Config.ARGB_8888);
        mOffscreenCanvas = new Canvas(mOffscreenBitmap);
        reset();
    }

    private void releaseBitmap() {
        if (mOffscreenBitmap != null) {
            mOffscreenBitmap.recycle();
            mOffscreenBitmap = null;
            mOffscreenCanvas = null;
        }
        reset();
    }

    /**
     * Get 28x28 pixel data for tensorflow input.
     */
    public float[] getPixelData() {
        if (mOffscreenBitmap == null) {
            return null;
        }

        int width = 28;
        int height = 28;

        // Get 28x28 pixel data from bitmap
        int[] pixels = new int[width * height];
        float[] retPixels = new float[pixels.length];
        int[] resize = new int[280*280];

        mOffscreenBitmap.getPixels(resize, 0, 280, 0, 0, 280, 280);


        for (int i=0; i<pixels.length; i++){
            pixels[i] = 255;
        }


        for (int i=0; i<resize.length; i++){
            int pix = resize[i];
            int b = (pix&0xff);
            int k = (((i/2800))*28) + ((i%280)/10);
            if (pixels[k] > b) pixels[k]=b;
        }

        float max = 0;
        int count_not_zero =0;
        for (int i=0; i<pixels.length; i++){

            retPixels[i] = 1 - (pixels[i]/255f);

            if (retPixels[i]!=0) {
                count_not_zero++;
            }
        }

        return retPixels;
    }

    public Bitmap getBitmap() {
        return mOffscreenBitmap;
    }
}
