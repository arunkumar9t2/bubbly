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
import android.view.View;
import android.view.ViewConfiguration;

import java.util.LinkedList;
import java.util.List;

import in.arunkumarsampath.bubbly.util.Utils;

import static in.arunkumarsampath.bubbly.base.MovementTracker.adjustVelocities;

/**
 * Created by Arunkumar on 20/05/17.
 */
public class BubbleMovementManager {
    private static final String TAG = BubbleMovementManager.class.getSimpleName();

    private Context context;

    private static final SpringForce DEFAULT_SPRING_FORCE = new SpringForce();
    private SpringForce springForce;
    private GestureDetector gestureDetector;

    private final List<View> views = new LinkedList<>();

    private final int touchSlop;
    private Rect bounds;

    private View masterView;

    private boolean wasFlung;

    private MovementTracker movementTracker;

    private SpringAnimation masterXStickyAnim;
    private SpringAnimation masterYStickyAnim;
    private FlingAnimation masterXFlingAnim;
    private FlingAnimation masterYFlingAnim;

    public BubbleMovementManager(@NonNull Context context, @NonNull List<View> bubbles, @Nullable Rect bounds) {
        this.context = context;

        views.addAll(bubbles);

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

        masterView = bubbles.get(0);
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

    private void moveMasterX(float x) {
        masterView.setTranslationX(x);
    }

    private void moveMasterY(float y) {
        masterView.setTranslationY(y);
    }

    private void stickToX(final float startVelocity) {
        cancelMasterXStickyAnim();
        masterXStickyAnim = new SpringAnimation(new FloatValueHolder())
                .setSpring(springForce)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationX())
                .addUpdateListener((animation, value, velocity) -> moveMasterX(value));

        if (masterView.getTranslationX() > bounds.width() / 2) {
            masterXStickyAnim.animateToFinalPosition(bounds.width() - masterView.getWidth());
        } else {
            masterXStickyAnim.animateToFinalPosition(0);
        }
    }

    private void stickToY(final float startVelocity) {
        cancelMasterYStickyAnim();
        masterYStickyAnim = new SpringAnimation(new FloatValueHolder())
                .setSpring(springForce)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationY())
                .addUpdateListener((animation, value, velocity) -> moveMasterY(value));

        if (masterView.getTranslationY() < bounds.top) {
            masterYStickyAnim.animateToFinalPosition(bounds.top);
        } else if (masterView.getTranslationY() > bounds.height()) {
            masterYStickyAnim.animateToFinalPosition(bounds.height() - masterView.getWidth());
        }
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

                    dragging = false;

                    lastDownX = event.getRawX();
                    lastDownY = event.getRawY();

                    lastViewDownX = v.getX();
                    lastViewDownY = v.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    movementTracker.addMovement(event);

                    float offsetX = event.getRawX() - lastDownX;
                    float offsetY = event.getRawY() - lastDownY;

                    if (Math.hypot(offsetX, offsetY) > touchSlop) {
                        dragging = true;
                    }

                    if (dragging) {
                        float x = lastViewDownX + offsetX;
                        float y = lastViewDownY + offsetY;

                        moveMasterX(x);
                        moveMasterY(y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    movementTracker.onUp();

                    dragging = false;
                    if (!wasFlung) {
                        stickToX(0);
                        stickToY(0);
                    }
                    break;
            }
            return true;
        }
    }

    private class GestureDetectorListener extends SimpleOnGestureListener {
        static final float FLING_FRICTION = 0.5f;

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

            final int xMin = bounds.left;
            final int xMax = bounds.width() - masterView.getWidth();
            final float xStartValue = Math.max(xMin, Math.min(xMax, masterView.getTranslationX()));
            final float xVelocity = adjustedVelocities[0];

            masterXFlingAnim = new FlingAnimation(new FloatValueHolder())
                    .setMinValue(xMin)
                    .setMaxValue(xMax)
                    .setFriction(FLING_FRICTION)
                    .setStartValue(xStartValue)
                    .setStartVelocity(xVelocity)
                    .addUpdateListener((animation, value, velocity) -> moveMasterX(value))
                    .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                        if (!cancelled) {
                            stickToX(velocity);

                            cancelYFling();
                        }
                    });

            final int yMin = bounds.top;
            final int yMax = bounds.height() - masterView.getHeight();
            final float yStartValue = Math.max(yMin, Math.min(yMax, masterView.getTranslationY()));
            final float yVelocity = adjustedVelocities[1];

            masterYFlingAnim = new FlingAnimation(new FloatValueHolder())
                    .setMinValue(yMin)
                    .setMaxValue(yMax)
                    .setFriction(FLING_FRICTION)
                    .setStartValue(yStartValue)
                    .setStartVelocity(yVelocity)
                    .addUpdateListener((animation, value, velocity) -> moveMasterY(value))
                    .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                        if (!cancelled) {
                            stickToY(velocity);
                        }
                    });
            masterXFlingAnim.start();
            masterYFlingAnim.start();
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
