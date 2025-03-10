package com.example.flyshooter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.Random;

public class FlyShooterView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread gameThread;
    private Bitmap flyBitmap;
    private float flyX, flyY;
    private int score = 0;
    private Paint scorePaint, timerPaint;
    private Random random;
    private MediaPlayer hitSound;
    private CountDownTimer gameTimer;
    private int timeLeft = 30;
    private boolean gameOver = false;

    public FlyShooterView(Context context) {
        super(context);
        getHolder().addCallback(this);
        gameThread = new GameThread(getHolder(), this);
        flyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fly);
        random = new Random();
        moveFly();
        
        // Thiết lập điểm số
        scorePaint = new Paint();
        scorePaint.setTextSize(50);
        scorePaint.setColor(0xFFFF0000);
        
        // Thiết lập bộ đếm thời gian
        timerPaint = new Paint();
        timerPaint.setTextSize(50);
        timerPaint.setColor(0xFF0000FF);
        
        // Âm thanh khi bấm trúng ruồi
        hitSound = MediaPlayer.create(context, R.raw.hit_sound);
        
        // Bắt đầu bộ đếm thời gian
        gameTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = (int) (millisUntilFinished / 1000);
            }
            
            @Override
            public void onFinish() {
                gameOver = true;
                gameThread.setRunning(false);
                showGameOverScreen();
            }
        };
        gameTimer.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        gameThread.setRunning(false);
        gameTimer.cancel();
    }

    public void update() {
        if (!gameOver && random.nextInt(100) < 2) {
            moveFly();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(0xFF87CEEB);
            canvas.drawBitmap(flyBitmap, flyX, flyY, null);
            canvas.drawText("Score: " + score, 50, 100, scorePaint);
            canvas.drawText("Time: " + timeLeft, 50, 160, timerPaint);
            
            if (gameOver) {
                Paint gameOverPaint = new Paint();
                gameOverPaint.setTextSize(80);
                gameOverPaint.setColor(0xFFFF0000);
                canvas.drawText("GAME OVER", getWidth() / 4, getHeight() / 2, gameOverPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!gameOver && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (event.getX() >= flyX && event.getX() <= flyX + flyBitmap.getWidth() &&
                event.getY() >= flyY && event.getY() <= flyY + flyBitmap.getHeight()) {
                score += 10;
                hitSound.start();
                moveFly();
            }
        }
        return true;
    }

    private void moveFly() {
        flyX = random.nextInt(getWidth() - flyBitmap.getWidth());
        flyY = random.nextInt(getHeight() - flyBitmap.getHeight());
    }

    private void showGameOverScreen() {
        post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Game Over");
            builder.setMessage("Your Score: " + score);
            builder.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    restartGame();
                }
            });
            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);
                }
            });
            builder.setCancelable(false);
            builder.show();
        });
    }

    private void restartGame() {
        score = 0;
        timeLeft = 30;
        gameOver = false;
        moveFly();
        gameThread = new GameThread(getHolder(), this);
        gameThread.setRunning(true);
        gameThread.start();
        gameTimer.start();
    }

    private class GameThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private FlyShooterView gameView;
        private boolean running;

        public GameThread(SurfaceHolder surfaceHolder, FlyShooterView gameView) {
            this.surfaceHolder = surfaceHolder;
            this.gameView = gameView;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        gameView.update();
                        gameView.draw(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
