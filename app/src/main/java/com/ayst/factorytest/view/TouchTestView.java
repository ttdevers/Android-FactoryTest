package com.ayst.factorytest.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ayst.factorytest.R;

import java.util.ArrayList;

public class TouchTestView extends View {
    private static final String TAG = "TouchTestView";

    private Paint mNormalPaint;
    private Paint mPassPaint;
    private Paint mLinePaint;
    private ArrayList<TestRect> mRects;
    private ArrayList<TestDiagonalRect> mDiagonalRects;
    private ArrayList<ArrayList<PT>> mLines;
    private ArrayList<ArrayList<PT>> mDiagonalLines;
    private ArrayList<PT> mCurrentLine;
    private ArrayList<PT> mDiagonalCurrentLineTotalPoints;
    private CallBack mCallBack;

    private int mLineWidth;
    private float mLastPointX;
    private float mLastPointY;
    private float mPointX;
    private float mPointY;
    private float mRecordStep = 4;
    private int mNormalColor;
    private int mPassColor;
    private int mLineColor;
    private boolean mTestPass;
    private int mCurrentDrawingRectQuadrant;

    // 象限
    class Quadrant {
        public static final int TOP_LEFT = 1 << 0;
        public static  final int TOP_RIGHT = 1 << 1;
        public static  final int BOTTOM_LEFT = 1 << 2;
        public static  final int  BOTTOM_RIGHT = 1 << 3;
    }



    public TouchTestView(Context context) {
        this(context, null);
    }

    public TouchTestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchTestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLineWidth = 1;
        mTestPass = false;
        mNormalColor = context.getResources().getColor(R.color.black);
        mPassColor = context.getResources().getColor(R.color.green);
        mLineColor = context.getResources().getColor(R.color.black);
        mNormalPaint = new Paint();
        mNormalPaint.setColor(mNormalColor);
        mNormalPaint.setAntiAlias(true);
        mNormalPaint.setStyle(Paint.Style.STROKE);
        mPassPaint = new Paint();
        mPassPaint.setColor(mPassColor);
        mPassPaint.setAntiAlias(true);
        mPassPaint.setStyle(Paint.Style.FILL);
        mLinePaint = new Paint();
        mLinePaint.setColor(mLineColor);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mRects = new ArrayList<>();
        mLines = new ArrayList<>();
        mCurrentLine = new ArrayList<>();
        mDiagonalRects = new ArrayList<>();
        mDiagonalCurrentLineTotalPoints = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            createRect();
        }
    }

    // 计算逆旋转后的坐标
    private float[] inverseRotatePoint(float xRotated, float yRotated, float cx, float cy, float angle) {
        // 将角度转换为弧度
        double radian = Math.toRadians(angle);

        // 使用逆旋转矩阵公式计算原始坐标
        float xOriginal = (float) ((xRotated - cx) * Math.cos(radian) + (yRotated - cy) * Math.sin(radian) + cx);
        float yOriginal = (float) (-(xRotated - cx) * Math.sin(radian) + (yRotated - cy) * Math.cos(radian) + cy);

        return new float[]{xOriginal, yOriginal};
    }


    // 方法：计算并打印旋转后的矩形坐标
    private void drawRotatedRectangle(Canvas canvas, TestDiagonalRect rect, Paint paint) {
        // 矩形的四个顶点
        float[] points = new float[]{
            rect.left, rect.top,         // 左上角
            rect.right , rect.top,       // 右上角
            rect.left, rect.bottom,      // 左下角
            rect.right, rect.bottom      // 右下角
        };

        // 旋转中心点（矩形的中心）
        float cx = getWidth() / 2;
        float cy = getHeight() / 2;

        // 打印旋转前的坐标
//        Log.d(TAG, "Original Points:");
//        for (int i = 0; i < 4; i++) {
//            Log.d(TAG, ("Point " + (i + 1) + ": (" + points[i * 2] + ", " + points[i * 2 + 1] + ")"));
//        }

        // 旋转矩阵，旋转角度为 rotationAngle
        canvas.save();  // 保存当前状态
        canvas.rotate(rect.rotationAngle, cx, cy);  // 绕矩形中心点旋转

        // 绘制旋转后的矩形
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);

        // 恢复旋转前的状态
        canvas.restore();

        // 计算旋转后的四个顶点

        for (int i = 0; i < 4; i++) {
            float x = points[i * 2];
            float y = points[i * 2 + 1];

            // 应用旋转变换
            float rotatedX = (float) (Math.cos(Math.toRadians(rect.rotationAngle)) * (x - cx) - Math.sin(Math.toRadians(rect.rotationAngle)) * (y - cy) + cx);
            float rotatedY = (float) (Math.sin(Math.toRadians(rect.rotationAngle)) * (x - cx) + Math.cos(Math.toRadians(rect.rotationAngle)) * (y - cy) + cy);

            if (i == 0) {
                rect.leftRotated = rotatedX;
                rect.topRotated = rotatedY;
            }

            if (i == 3) {
                rect.rightRotated = rotatedX;
                rect.bottomRotated = rotatedY;
            }
            // 打印旋转后的坐标
            //Log.d(TAG,"Rotated dregree(" + rect.rotationAngle + ") " +  "Point " + (i + 1) + ": (" + rotatedX + ", " + rotatedY + ")");
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        TestRect rect = null;
        Paint paint = null;
        for (int i = 0; i < mRects.size(); i++) {
            rect = mRects.get(i);
            if (rect.isPass) {
                paint = mPassPaint;
            } else {
                paint = mNormalPaint;
            }
            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
        }

        TestDiagonalRect diagonalRect = null;
        for (int i = 0; i < mDiagonalRects.size(); i++) {
            diagonalRect = mDiagonalRects.get(i);
            if (diagonalRect.isPass) {
                paint = mPassPaint;
            } else {
                paint = mNormalPaint;
            }
            drawRotatedRectangle(canvas, diagonalRect, paint);
        }

        float lastX = 0;
        float lastY = 0;
        PT pt;
        ArrayList<PT> line;
        for (int i = 0; i < mLines.size(); i++) {
            line = mLines.get(i);
            for (int j = 0; j < line.size(); j++) {
                pt = line.get(j);
                if (line.size() == 1) {
                    canvas.drawPoint(pt.mX, pt.mY, mLinePaint);
                } else {
                    if (j > 0) {
                        canvas.drawLine(lastX, lastY, pt.mX, pt.mY, mLinePaint);
                    }
                    lastX = pt.mX;
                    lastY = pt.mY;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN");
                if (!mTestPass) {
                    mLastPointX = mPointX = event.getX();
                    mLastPointY = mPointY = event.getY();
                    mCurrentLine = new ArrayList<PT>();
                    PT pt = new PT(mPointX, mPointY);
                    mCurrentLine.add(pt);
                    mLines.add(mCurrentLine);
                    mCurrentDrawingRectQuadrant = getQuadrant(mPointX, mPointY);
                    mDiagonalCurrentLineTotalPoints.clear();
                    mDiagonalCurrentLineTotalPoints.add(pt);
                    testRectPass(mPointX, mPointY);
                    isPass();
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!mTestPass) {
                    mPointX = event.getX();
                    mPointY = event.getY();
                    if (Math.abs(mPointX - mLastPointX) >= mRecordStep
                            || Math.abs(mPointY - mLastPointY) >= mRecordStep) {
                        PT pt = new PT(mPointX, mPointY);
                        mCurrentLine.add(pt);

                        // 判断点是否在矩形内
                        for (TestDiagonalRect tdr : mDiagonalRects) {
                            if ((tdr.quadrant & mCurrentDrawingRectQuadrant) != 0) {
                                if (tdr.contain(mPointX, mPointY, tdr.rotationAngle)) {
                                    mDiagonalCurrentLineTotalPoints.add(pt);
                                }
                            }
                        }

                        mLastPointX = mPointX;
                        mLastPointY = mPointY;
                    }
                    testRectPass(mPointX, mPointY);
                    isPass();
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                testDiagonalRectPass();
                Log.d(TAG, "ACTION_UP");
                invalidate();
                break;
        }
        return true;
    }

    private void testRectPass(float x, float y) {
        for (TestRect tr : mRects) {
            if (!tr.isPass) {
                if (tr.contain(x, y)) {
                    tr.isPass = true;
                }
            }
        }
    }

    public int getQuadrant(float x, float y) {
        if (x < getWidth() / 2 && y < getHeight() / 2) {
            return Quadrant.TOP_LEFT;
        } else if (x >= getWidth() / 2 && y < getHeight() / 2) {
            return Quadrant.TOP_RIGHT;
        } else if (x < getWidth() / 2 && y >= getHeight() / 2) {
            return Quadrant.BOTTOM_LEFT;
        } else {
            return Quadrant.BOTTOM_RIGHT;
        }
    }

    private void testDiagonalRectPass() {
        float distance = 200;
        int size = mDiagonalCurrentLineTotalPoints.size();
        PT ptFirst = mDiagonalCurrentLineTotalPoints.get(0);
        PT ptLast = mDiagonalCurrentLineTotalPoints.get(size - 1);

        // 对mDiagonalCurrentLineTotalPoints进行遍历，判断每个相邻的点的mX值不大于4
        for (int i = 0; i < size - 1; i++) {
            PT pt = mDiagonalCurrentLineTotalPoints.get(i);
            PT ptNext = mDiagonalCurrentLineTotalPoints.get(i + 1);
            if (Math.abs(pt.mX - ptNext.mX) > 50) {
                Log.d(TAG, "i=:" + i + " testDiagonalRectPass: " + pt.toString() + ", " + ptNext.toString());
                return;
            }
        }

        if (size > 0) {
            int h = getHeight();
            int w = getWidth();
            if (Math.abs(ptLast.mX - ptFirst.mX) >= (w - distance) && Math.abs(ptLast.mY - ptFirst.mY) >= (h - distance)) {
                for (TestDiagonalRect tdr : mDiagonalRects) {
                    if ((tdr.quadrant & mCurrentDrawingRectQuadrant) != 0) {
                        tdr.isPass = true;
                    }
                }
            }
        }
    }

    private void isPass() {
        int passCount = 0;
        for (TestRect tr : mRects) {
            if (tr.isPass) {
                passCount++;
            } else {
                break;
            }
        }

        if (passCount == mRects.size()) {
            mTestPass = true;
            Log.i(TAG, "isPass, completed");
            if (mCallBack != null) {
                mCallBack.onTestCompleted();
            }
        }
    }

    private void createRect() {
        int width = getWidth();
        int height = getHeight();
        float rectWidth = (float) Math.max(width, height) / 15;
        float rectHeight = (rectWidth * height) / width;

        Log.i(TAG, "createRect, width: " + width + ", height: " + height + ", rectWidth=" + rectWidth);

        mRects = new ArrayList<>();
        // 区分屏幕方向，尽量排满整个屏幕
        if (width > height) {
            // 计算原矩形的对角线长度
            float diagonalLength = (float) Math.sqrt((width * width)+ (height * height));
            float halfDiagonalLength = diagonalLength / 2;

            int newRectWidth = 100;
            int halfNewRectWidth = newRectWidth / 2;

            float centerX = width / 2;
            float centerY = height / 2;
            float left = centerX - halfDiagonalLength;
            float top =  centerY - halfNewRectWidth;
            float right = centerX + halfDiagonalLength;
            float bottom = centerY + halfNewRectWidth;

            float angle = (float)  Math.toDegrees(Math.atan2(getHeight(), getWidth()));  // 计算对角线与水平边的夹角
            TestDiagonalRect rect_1 = new TestDiagonalRect();
            rect_1.left = left ;
            rect_1.top = top;
            rect_1.right = right;
            rect_1.bottom = bottom;
            rect_1.rotationAngle = angle;
            rect_1.quadrant = Quadrant.TOP_LEFT | Quadrant.BOTTOM_RIGHT;
            mDiagonalRects.add(rect_1);

            TestDiagonalRect rect_2 = new TestDiagonalRect();
            rect_2.left = left ;
            rect_2.top = top;
            rect_2.right = right;
            rect_2.bottom = bottom;
            rect_2.rotationAngle = 180.0f - angle;
            rect_2.quadrant = Quadrant.TOP_RIGHT | Quadrant.BOTTOM_LEFT;
            mDiagonalRects.add(rect_2);

            // 竖向左边、右边、中间方块
            for (int i = 1; i < (height / rectHeight) - 1; i++) {
                TestRect rect1 = new TestRect();
                rect1.top = i * rectHeight;
                rect1.left = width - rectWidth;
                rect1.right = width;
                rect1.bottom = rect1.top + rectHeight;
                mRects.add(rect1);

                TestRect rect2 = new TestRect();
                rect2.top = rect1.top;
                rect2.left = 0;
                rect2.right = rectWidth;
                rect2.bottom = rect2.top + rectHeight;
                mRects.add(rect2);

                TestRect rect3 = new TestRect();
                rect3.top = rect1.top;
                rect3.left = (width / 2) - (rectWidth / 2);
                rect3.right = (width / 2) + (rectWidth / 2);;
                rect3.bottom = rect3.top + rectHeight;
                mRects.add(rect3);
            }

            // 横向顶部、中部、底部方块
            for (int i = 0; i < width / rectWidth; i++) {
                TestRect rect1 = new TestRect();
                rect1.top = 0;
                rect1.left = i * rectWidth;
                rect1.right = rect1.left + rectWidth;
                rect1.bottom = rectHeight;
                mRects.add(rect1);

                TestRect rect2 = new TestRect();
                rect2.top = (height / 2) - (rectHeight / 2);
                rect2.left = i * rectWidth;
                rect2.right = rect1.left + rectWidth;
                rect2.bottom = (height / 2) + (rectHeight / 2);
                mRects.add(rect2);

                TestRect rect3 = new TestRect();
                rect3.top = height - rectHeight;
                rect3.left = i * rectWidth;
                rect3.right = rect1.left + rectWidth;
                rect3.bottom = height;
                mRects.add(rect3);
            }
        } else {
            for (int i = 0; i < height / rectWidth; i++) {
                TestRect rect1 = new TestRect();
                rect1.top = i * rectWidth;
                rect1.left = i * (rectWidth * width / height);
                rect1.right = rect1.left + rectWidth;
                rect1.bottom = rect1.top + rectWidth;
                mRects.add(rect1);

                TestRect rect2 = new TestRect();
                rect2.top = rect1.top;
                rect2.left = width - rect1.left - rectWidth;
                rect2.right = rect2.left + rectWidth;
                rect2.bottom = rect2.top + rectWidth;
                mRects.add(rect2);
            }
        }

        if (mTestPass) {
            for (TestRect tr : mRects) {
                tr.isPass = true;
            }
        }
    }

    public void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }

    public interface CallBack {
        void onTestCompleted();
    }

    class TestRect {
        float top;
        float left;
        float right;
        float bottom;
        boolean isPass;

        public boolean contain(float x, float y) {
            if (x >= left && x <= right && y >= top && y <= bottom) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + top + ", " + left + ", " + right + ", " + bottom + ", " + isPass + "]";
        }
    }

    class TestDiagonalRect {
        float top;
        float left;
        float right;
        float bottom;
        float topRotated;
        float leftRotated;
        float rightRotated;
        float bottomRotated;
        float rotationAngle;
        boolean isPass;
        int quadrant;

        public boolean contain(float x, float y, float angle) {
            float[] originCoordinates = null;
            originCoordinates = inverseRotatePoint(x, y, getWidth() / 2, getHeight() / 2, angle);
            float originX = originCoordinates[0];
            float originY = originCoordinates[1];
            if (originX >= left && originX <= right && originY >= top && originY <= bottom) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + top + ", " + left + ", " + right + ", " + bottom + ", " + isPass + "]";
        }
    }


    class PT {
        public float mX;
        public float mY;

        public PT(float x, float y) {
            this.mX = x;
            this.mY = y;
        }

        public String toString() {
            return "[" + mX + ", " + mY + "]";
        }
    }
}
