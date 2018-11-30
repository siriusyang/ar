package com.demo.ar.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;

import common.helps.SnackbarHelper;


public class AugmentedImageFragment extends ArFragment {
    // This is a pre-created database containing the sample image.
    private static final String TAG = "AugmentedImageFragment";
    private static final String SAMPLE_IMAGE_DATABASE = "myimages.imgdb";

    // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
    // application.
    private static final double MIN_OPENGL_VERSION = 3.0;

    // Augmented image configuration and rendering.
    // Load a single image (true) or a pre-generated image database (false).
    private static final boolean USE_SINGLE_IMAGE = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            SnackbarHelper.getInstance()
                    .showError(getActivity(), "Sceneform requires Android N or later");
        }

        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
            SnackbarHelper.getInstance()
                    .showError(getActivity(), "Sceneform requires OpenGL ES 3.0 or later");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Turn off the plane discovery since we're only looking for images
        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);
        return view;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        try (InputStream is = getContext().getAssets().open(SAMPLE_IMAGE_DATABASE)) {
            config.setAugmentedImageDatabase(AugmentedImageDatabase.deserialize(session, is));
        } catch (IOException e) {
            SnackbarHelper.getInstance().showError(getActivity(), "IO exception loading augmented image database.");
            Log.e(TAG, "IO exception loading augmented image database.", e);
        }
        return config;
    }
}
