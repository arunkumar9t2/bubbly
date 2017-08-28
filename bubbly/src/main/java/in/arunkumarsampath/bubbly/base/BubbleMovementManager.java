package in.arunkumarsampath.bubbly.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.animation.FlingAnimation;
import android.support.animation.FloatValueHolder;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.LinkedList;
import java.util.List;

import in.arunkumarsampath.bubbly.util.Utils;

import static in.arunkumarsampath.bubbly.base.MovementTracker.adjustVelocities;

/**
 * Created by Arunkumar on 20/05/17.
 */
public class BubbleMovementManager implements BubbleMovement {
    private static final String TAG = BubbleMovementManager.class.getSimpleName();

    private Context context;

    private static final SpringForce DEFAULT_SPRING_FORCE = new SpringForce();
    private SpringForce springForce;
    private GestureDetector gestureDetector;

    private static final float FLING_FRICTION = 0.5f;

    private final List<View> views = new LinkedList<>();

    private final int touchSlop;
    private Rect bounds;

    private View masterView;

    private boolean wasFlung;

    private MovementTracker movementTracker;
    private VelocityTracker velocityTracker = null;

    private SpringAnimation masterXStickyAnim;
    private SpringAnimation masterYStickyAnim;
    private FlingAnimation masterXFlingAnim;
    private FlingAnimation masterYFlingAnim;

    public BubbleMovementManager(@NonNull Context context, @NonNull List<View> bubbles, @Nullable Rect bounds) {
        this.context = context;

        views.addAll(bubbles);
        masterView = bubbles.remove(0);

        springForce = DEFAULT_SPRING_FORCE;
        springForce.setStiffness(SpringForce.STIFFNESS_LOW);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

        final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        touchSlop = viewConfiguration.getScaledTouchSlop();
        movementTracker = MovementTracker.obtain();

        if (bounds == null) {
            initBounds();
        } else {
            this.bounds = bounds;
        }

        gestureDetector = new GestureDetector(context.getApplicationContext(), new GestureDetectorListener());
    }

    private void initBounds() {
        final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        bounds = new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
    }

    public void start() {
        masterView.setOnTouchListener(new TouchListener());
    }

    public void stop() {
        context = null;
        cancelAllAnim();
        masterView.setOnTouchListener(null);
        masterView = null;
        gestureDetector = null;
    }

    @Override
    public void moveX(float x, float velocity) {
        masterView.setTranslationX(x);
    }

    @Override
    public void moveY(float y, float velocity) {
        masterView.setTranslationY(y);
    }

    private void stickToX(final float startVelocity) {
        cancelMasterXStickyAnim();
        masterXStickyAnim = new SpringAnimation(new FloatValueHolder())
                .setSpring(springForce)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationX())
                .addUpdateListener((animation, value, velocity) -> moveX(value, velocity));

        if (masterView.getTranslationX() > bounds.width() / 2) {
            masterXStickyAnim.animateToFinalPosition(bounds.width() - masterView.getWidth());
        } else {
            masterXStickyAnim.animateToFinalPosition(bounds.left);
        }
    }

    private void stickToY(final float startVelocity) {
        cancelMasterYStickyAnim();
        masterYStickyAnim = new SpringAnimation(new FloatValueHolder())
                .setSpring(springForce)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationY())
                .addUpdateListener((animation, value, velocity) -> moveY(value, velocity));

        if (masterView.getTranslationY() < bounds.top) {
            masterYStickyAnim.animateToFinalPosition(bounds.top);
        } else if (masterView.getTranslationY() > bounds.height()) {
            masterYStickyAnim.animateToFinalPosition(bounds.height() - masterView.getWidth());
        }
    }

    private void flingX(float startVelocity) {
        final int xMin = bounds.left;
        final int xMax = bounds.width() - masterView.getWidth();
        final float xStartValue = Math.max(xMin, Math.min(xMax, masterView.getTranslationX()));

        masterXFlingAnim = new FlingAnimation(new FloatValueHolder())
                .setMinValue(xMin)
                .setMaxValue(xMax)
                .setFriction(FLING_FRICTION)
                .setStartValue(xStartValue)
                .setStartVelocity(startVelocity)
                .addUpdateListener((animation, value, velocity) -> moveX(value, value))
                .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                    if (!cancelled) {
                        stickToX(velocity);

                        cancelYFling();
                    }
                });
        masterXFlingAnim.start();
    }

    private void flingY(float startVelocity) {
        final int yMin = bounds.top;
        final int yMax = bounds.height() - masterView.getHeight();
        final float yStartValue = Math.max(yMin, Math.min(yMax, masterView.getTranslationY()));

        masterYFlingAnim = new FlingAnimation(new FloatValueHolder())
                .setMinValue(yMin)
                .setMaxValue(yMax)
                .setFriction(FLING_FRICTION)
                .setStartValue(yStartValue)
                .setStartVelocity(startVelocity)
                .addUpdateListener((animation, value, velocity) -> moveY(value, velocity))
                .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                    if (!cancelled) {
                        stickToY(velocity);
                    }
                });
        masterYFlingAnim.start();
    }

    private void cancelAllAnim() {
        cancelAllFlings();
        cancelAllSticky();
    }

    private void cancelAllSticky() {
        cancelMasterXStickyAnim();
        cancelMasterYStickyAnim();
    }

    void cancelAllFlings() {
        cancelXFling();
        cancelYFling();
    }

    private void cancelYFling() {
        if (masterYFlingAnim != null) {
            masterYFlingAnim.cancel();
        }
    }

    private void cancelXFling() {
        if (masterXFlingAnim != null) {
            masterXFlingAnim.cancel();
        }
    }

    private void cancelMasterXStickyAnim() {
        if (masterXStickyAnim != null) {
            masterXStickyAnim.cancel();
        }
    }

    private void cancelMasterYStickyAnim() {
        if (masterYStickyAnim != null) {
            masterYStickyAnim.cancel();
        }
    }

    private class TouchListener implements View.OnTouchListener {
        private float lastDownX, lastDownY;
        private float lastViewDownX, lastViewDownY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Reset flags
            wasFlung = false;

            gestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelAllAnim();

                    movementTracker.onDown();

                    initVelocityTracker();

                    dragging = false;

                    lastDownX = event.getRawX();
                    lastDownY = event.getRawY();

                    lastViewDownX = v.getX();
                    lastViewDownY = v.getY();

                    velocityTracker.addMovement(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    movementTracker.addMovement(event);

                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);

                    float offsetX = event.getRawX() - lastDownX;
                    float offsetY = event.getRawY() - lastDownY;

                    if (Math.hypot(offsetX, offsetY) > touchSlop) {
                        dragging = true;
                    }

                    if (dragging) {
                        float x = lastViewDownX + offsetX;
                        float y = lastViewDownY + offsetY;

                        moveX(x, velocityTracker.getXVelocity());
                        moveY(y, velocityTracker.getYVelocity());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    movementTracker.onUp();

                    dragging = false;
                    if (!wasFlung) {
                        cancelAllAnim();
                        stickToX(0);
                        stickToY(0);
                    }
                    break;
            }
            return true;
        }
    }

    private void initVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private class GestureDetectorListener extends SimpleOnGestureListener {
        private final int minimumFlingVelocity;

        GestureDetectorListener() {
            final int scaledScreenWidthDp = (Resources.getSystem().getConfiguration().screenWidthDp * 6);
            minimumFlingVelocity = Utils.dpToPx(scaledScreenWidthDp);
        }

        @Override
        public boolean onFling(MotionEvent downEvent, MotionEvent upEvent, float velocityX, float velocityY) {
            float[] adjustedVelocities = movementTracker.getAdjustedVelocities(velocityX, velocityY);

            if (adjustedVelocities == null) {
                float[] down = new float[]{downEvent.getRawX(), downEvent.getRawY()};
                float[] up = new float[]{upEvent.getRawX(), upEvent.getRawY()};
                adjustedVelocities = adjustVelocities(down, up, velocityX, velocityY);
            }
            cancelAllAnim();

            adjustedVelocities = interpolateVelocities(upEvent, adjustedVelocities);

            flingX(adjustedVelocities[0]);
            flingY(adjustedVelocities[1]);

            wasFlung = true;
            return true;
        }


        private float[] interpolateVelocities(@NonNull MotionEvent upEvent, float[] adjustedVelocities) {
            final float xBeforeRampUp = adjustedVelocities[0];
            float xAfterRampUp = xBeforeRampUp;

            // Ramp up X velocity based on screen density and where the user lifted his finger.
            float x = upEvent.getRawX() / bounds.width();
            if (xAfterRampUp > 0) {
                xAfterRampUp = Math.max(xAfterRampUp, minimumFlingVelocity * (1 - x));
            } else {
                xAfterRampUp = -Math.max(xAfterRampUp, minimumFlingVelocity * x);
            }

            // Find out how much percent we ramped up X velocity
            float xPercentageRampUp = xBeforeRampUp / xAfterRampUp;

            // Apply the same amount of ramp up to y velocity.
            final float yAfterRampUp = adjustedVelocities[1] + (adjustedVelocities[1] * xPercentageRampUp);

            // Log.d(TAG, String.format("Initial velocity %f %f, ramped up %f %f", adjustedVelocities[0], adjustedVelocities[1], xAfterRampUp, yAfterRampUp));

            return new float[]{xAfterRampUp, yAfterRampUp};
        }
    }
}
