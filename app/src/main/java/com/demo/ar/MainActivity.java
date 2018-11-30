package com.demo.ar;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import common.helps.SnackbarHelper;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private ArFragment arFragment;
    private ImageView fitToScanView;
    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, AnchorNode> augmentedImageMap = new HashMap<>();
    private ModelRenderable andyRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

//        arFragment.setOnTapArPlaneListener();
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, Uri.parse("models/z.sfb"))
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(this);

        arFragment.setOnTapArPlaneListener(

                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            SnackbarHelper.getInstance().showMessage(this, " ARCore is not tracking yet");
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image " + augmentedImage.getIndex();
                    SnackbarHelper.getInstance().showMessage(this, text);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    fitToScanView.setVisibility(View.GONE);

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        // Create the Anchor.
                        Anchor anchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        anchorNode.setRenderable(andyRenderable);
//                        // Create the transformable andy and add it to the anchor.
                        TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                        andy.setParent(arFragment.getArSceneView().getScene());
                        andy.setRenderable(andyRenderable);
                        andy.setOnTapListener(new Node.OnTapListener() {
                            @Override
                            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                                arFragment.getArSceneView().getScene().onRemoveChild(andy);
                            }
                        });

//                        augmentedImageMap.put(augmentedImage, anchorNode);
//                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                    }
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }
}
