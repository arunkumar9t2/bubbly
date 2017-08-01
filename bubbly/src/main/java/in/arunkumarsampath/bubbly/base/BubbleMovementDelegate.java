package in.arunkumarsampath.bubbly.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.animation.DynamicAnimation;
import android.support.animation.FlingAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.LinkedList;
import java.util.List;

import in.arunkumarsampath.bubbly.util.Utils;

/**
 * Created by Arunkumar on 20/05/17.
 */
public class BubbleMovementDelegate {
    private static final String TAG = BubbleMovementDelegate.class.getSimpleName();

    private static final SpringForce DEFAULT_SPRING_FORCE = new SpringForce();

    private final List<View> views = new LinkedList<>();

    private Context context;

    private SpringForce springForce;
    private GestureDetector gestureDetector;

    private final int touchSlop;

    private Rect bounds;

    private View masterView;

    private boolean wasFlung;

    private MovementTracker movementTracker;

    public BubbleMovementDelegate(@NonNull Context context, @NonNull List<View> bubbles, @Nullable Rect bounds) {
        this.context = context;

        views.addAll(bubbles);

        springForce = DEFAULT_SPRING_FORCE;
        final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        touchSlop = viewConfiguration.getScaledTouchSlop();
        movementTracker = MovementTracker.obtain();

        springForce.setStiffness(SpringForce.STIFFNESS_LOW);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

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
        masterView.setOnTouchListener(new TouchListener(masterView));
    }

    public void stop() {
        context = null;
        masterView.setOnTouchListener(null);
        masterView = null;
        gestureDetector = null;
    }

    private void stickToX(final float startVelocity) {
        final SpringAnimation animation = new SpringAnimation(masterView, DynamicAnimation.TRANSLATION_X)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationX())
                .setSpring(springForce);

        if (masterView.getTranslationX() > bounds.width() / 2) {
            animation.animateToFinalPosition(bounds.width() - masterView.getWidth());
        } else {
            animation.animateToFinalPosition(0);
        }
    }

    private void stickToY(final float startVelocity) {
        final SpringAnimation animation = new SpringAnimation(masterView, DynamicAnimation.TRANSLATION_Y)
                .setStartVelocity(startVelocity)
                .setStartValue(masterView.getTranslationY())
                .setSpring(springForce);

        if (masterView.getTranslationY() < bounds.top) {
            animation.animateToFinalPosition(bounds.top);
        } else if (masterView.getTranslationY() > bounds.height()) {
            animation.animateToFinalPosition(bounds.height() - masterView.getWidth());
        }
    }

    private class TouchListener implements View.OnTouchListener {

        private float lastDownX, lastDownY;
        private float lastViewDownX, lastViewDownY;
        private boolean dragging;

        private SpringAnimation xAnimation;
        private SpringAnimation yAnimation;

        TouchListener(View view) {
            xAnimation = new SpringAnimation(view, DynamicAnimation.TRANSLATION_X);
            yAnimation = new SpringAnimation(view, DynamicAnimation.TRANSLATION_Y);
            xAnimation.setSpring(springForce);
            yAnimation.setSpring(springForce);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Reset flags
            wasFlung = false;

            gestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
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

                        v.setTranslationX(x);
                        v.setTranslationY(y);
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
        private FlingAnimation xFling;
        private FlingAnimation yFling;

        static final float FLING_FRICTION = 0.6f;

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
                adjustedVelocities = MovementTracker.adjustVelocities(down, up, velocityX, velocityY);
            }
            cancel();

            adjustedVelocities = interpolateVelocities(upEvent, adjustedVelocities);

            xFling = new FlingAnimation(masterView, DynamicAnimation.TRANSLATION_X)
                    .setMinValue(bounds.left)
                    .setMaxValue(bounds.width() - masterView.getWidth())
                    .setFriction(FLING_FRICTION)
                    .setStartValue(masterView.getTranslationX())
                    .setStartVelocity(adjustedVelocities[0])
                    .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                        if (!cancelled) {
                            stickToX(velocity);

                            if (yFling != null) {
                                //  yFling.cancel();
                            }
                        }
                    });

            yFling = new FlingAnimation(masterView, DynamicAnimation.TRANSLATION_Y)
                    .setMinValue(bounds.top)
                    .setMaxValue(bounds.height() - masterView.getHeight())
                    .setFriction(FLING_FRICTION)
                    .setStartValue(masterView.getTranslationY())
                    .setStartVelocity(adjustedVelocities[1])
                    .addEndListener((dynamicAnimation, cancelled, value, velocity) -> {
                        if (!cancelled) {
                            stickToY(velocity);
                        }
                    });
            xFling.start();
            yFling.start();
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

            Log.d(TAG, String.format("Initial velocity %f %f, ramped up %f %f", adjustedVelocities[0], adjustedVelocities[1], xAfterRampUp, yAfterRampUp));

            return new float[]{xAfterRampUp, yAfterRampUp};
        }

        void cancel() {
            if (xFling != null)
                xFling.cancel();
            if (yFling != null)
                yFling.cancel();
        }
    }
}
