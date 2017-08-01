package in.arunkumarsampath.bubbly.base;

import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.Collection;
import java.util.LinkedList;

public class MovementTracker {
    private static MovementTracker INSTANCE = new MovementTracker();
    private final SizedQueue<Float> xPoints;
    private final SizedQueue<Float> yPoints;
    private int trackingSize = 0;

    private MovementTracker() {
        trackingSize = 10;
        xPoints = new SizedQueue<>(trackingSize);
        yPoints = new SizedQueue<>(trackingSize);
    }

    @NonNull
    static MovementTracker obtain() {
        return INSTANCE;
    }

    static float[] adjustVelocities(float[] p1, float[] p2, float xVelocity, float yVelocity) {
        float downX = p1[0];
        float downY = p1[1];

        float upX = p2[0];
        float upY = p2[1];

        float x = 0, y = 0;

        if (upX >= downX && upY >= downY) {
            // Bottom right
            x = positive(xVelocity);
            y = positive(yVelocity);
        } else if (upX >= downX && upY <= downY) {
            // Top right
            x = positive(xVelocity);
            y = negate(yVelocity);
        } else if (upX <= downX && upY <= downY) {
            // Top left
            x = negate(xVelocity);
            y = negate(yVelocity);
        } else if (upX <= downX && upY >= downY) {
            // Bottom left
            x = negate(xVelocity);
            y = positive(yVelocity);
        }
        return new float[]{x, y};
    }

    private static float negate(float value) {
        return value > 0 ? -value : value;
    }

    private static float positive(float value) {
        return Math.abs(value);
    }

    /**
     * Adds a motion event to the tracker.
     *
     * @param event The event to be added.
     */
    void addMovement(@NonNull MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        xPoints.add(x);
        yPoints.add(y);
    }

    /**
     * Clear the tracking queue when user begins the gesture.
     */
    void onDown() {
        xPoints.clear();
        yPoints.clear();
    }

    /**
     * Clear the tracking queue when user ends the gesture.
     */
    void onUp() {
        xPoints.clear();
        yPoints.clear();
    }

    float[] getAdjustedVelocities(float xVelocity, float yVelocity) {
        int trackingThreshold = (int) (0.25 * trackingSize);
        float[] velocities;
        if (xPoints.size() >= trackingThreshold) {
            int downIndex = xPoints.size() - trackingThreshold;

            float[] up = new float[]{xPoints.getLast(), yPoints.getLast()};
            float[] down = new float[]{xPoints.get(downIndex), yPoints.get(downIndex)};

            velocities = adjustVelocities(down, up, xVelocity, yVelocity);
        } else {
            velocities = null;
        }
        return velocities;
    }

    @Override
    public String toString() {
        return xPoints.toString() + yPoints.toString();
    }
}

/**
 * A size limited queue structure that evicts the queue head when maximum queue size is reached. At
 * any instant the queue is equal or less than the max queue size.
 *
 * @param <E>
 */
class SizedQueue<E> extends LinkedList<E> {
    /**
     * The maximum size of queue
     */
    private final int limit;

    SizedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) {
            super.remove();
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        throw new UnsupportedOperationException("Not implemented, use add()");
    }

    @Override
    public void add(int location, E object) {
        throw new UnsupportedOperationException("Not implemented, use add()");
    }

    @Override
    public void addFirst(E object) {
        throw new UnsupportedOperationException("Not implemented, use add()");
    }

    @Override
    public void addLast(E object) {
        throw new UnsupportedOperationException("Not implemented, use add()");
    }

    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        throw new UnsupportedOperationException("Not implemented, use add()");
    }
}