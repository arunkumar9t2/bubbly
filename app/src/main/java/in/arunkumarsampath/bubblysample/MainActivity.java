package in.arunkumarsampath.bubblysample;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import in.arunkumarsampath.bubbly.base.BubbleMovementDelegate;

public class MainActivity extends AppCompatActivity {
    private BubbleMovementDelegate bubbleMovementDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final RelativeLayout relativeLayout = findViewById(R.id.root_frame);

        final ArrayList<View> bubblies = new ArrayList<View>() {{
            add(findViewById(R.id.circle));
        }};

        relativeLayout.post(new Runnable() {
            @Override
            public void run() {
                bubbleMovementDelegate = new BubbleMovementDelegate(getApplicationContext(), bubblies, new Rect(0, 0, relativeLayout.getWidth(), relativeLayout.getHeight()));
                bubbleMovementDelegate.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bubbleMovementDelegate.stop();
    }
}
