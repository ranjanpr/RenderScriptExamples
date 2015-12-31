package net.hydex11.cameracaptureexample;

import android.graphics.Point;
import android.hardware.Camera;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

/*
* This example application shows how it is possible to use camera preview image to perform
* calculations.
*
* This script will grab input camera image and turn every pixel that's "enough" bright to red.
*
* This example is built using some custom classes:
*
* - CameraHandler, which is used to open the camera and get its image using a callback
* - RSRenderHolder, which is used to define the output surface that our RS script will used to display
*   its result
* - RSCompute, wherein RS components are instantiated and computation is run
*
* Workflow is the following:
*
* 1) First of all we wait for camera preview surface to get initialized
* 2) We open the camera, get the calculated preview size
* 3) Wait for output surface to get ready and instantiate the RS script output Allocation
* 4) Computes RS calculations on possible camera preview frames
*
*
* */
public class MainActivity extends AppCompatActivity {

    static final String TAG = "CameraCaptureExample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Executes our example
        example();
    }

    RenderScript mRS;
    RSCompute rsCompute;

    // Custom class RSRenderHolder helps us define an output surface for our RenderScript computations.
    RSRenderHolder rsRenderHolder;

    private void example() {

        // Here we use the camera preview as below surface and rendering surface as upper one
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.setZOrderOnTop(false);

        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        // Enables support for alpha value in pixels (0 is transparent pixel)
        textureView.setOpaque(false);

        // Creates a new RenderScript context and enables our custom render holder
        mRS = RenderScript.create(this);
        rsRenderHolder = new RSRenderHolder(mRS);

        // Enables surface callback to init the entire process.
        // When surface gets initialized, camera preview is enabled
        surfaceView.getHolder().addCallback(mSurfaceHolderCallback);

    }

    // This callback gets triggered when camera preview surface is ready and instantiated
    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        // Custom camera handler class, used to display preview on surface
        CameraHandler cameraHandler;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "Preview surface created");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            resetCamera(holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (cameraHandler != null)
                cameraHandler.destroy();
            rsCompute = null;
        }

        void resetCamera(SurfaceHolder holder) {
            // Resets custom camera handler if was set before
            // Useful if there is any surface size change (usually should be prevented!)
            if (cameraHandler != null){
                cameraHandler.destroy();
            }

            // Instantiates custom camera handler, to manage camera preview
            cameraHandler = new CameraHandler(MainActivity.this);

            // Here we set camera properties that we want to force:
            // * We want to use 1 buffer, so that we can have only 1 processing function active
            //      For reference, http://developer.android.com/reference/android/hardware/Camera.html#setPreviewCallbackWithBuffer(android.hardware.Camera.PreviewCallback)
            // * We want to use our previously defined surface to preview our camera image. If all buffers are busy, preview will still be real time
            // * Sets maximum camera preview size at an acceptable resolution
            // * Sets camera preview callback, where in to execute RenderScript computations

            CameraHandler.CameraSetup cameraSetup = new CameraHandler.CameraSetup();
            cameraSetup.setCameraBuffersCount(1);
            cameraSetup.setPreviewSurfaceHolder(holder);
            cameraSetup.setMaxPreviewSize(new Point(1280, 720));
            cameraSetup.setPreviewCallback(previewCallback);

            try {
                cameraHandler.openCamera(cameraSetup);

                Camera.Size size = cameraHandler.getCameraSize();

                // Resets custom RenderScript compute class, passing it calculated maximum preview size
                resetRenderHolder(new Point(size.width, size.height));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void resetRenderHolder(Point renderInSize) {
            TextureView textureView = (TextureView) findViewById(R.id.textureView);

            // These two functions are a simple hack to help TextureView behave like we want it to:
            // If we are on landscape mode, it will rotate accordingly. Otherwise, a badly stretched image would be shown.
            textureView.requestLayout();
            textureView.invalidate();

            // Scales down preview size by 2 times, for performance reasons.
            Point renderOutSize = new Point(Math.round(renderInSize.x * 0.5f), Math.round(renderInSize.y * 0.5f));

            // Tells our RenderScript output surface holder which surface will be his
            rsRenderHolder.setRenderTextureView(textureView, renderOutSize);

            // Instantiates a new custom compute class, passing information about input image size (camera preview)
            // and output size (render holder surface scaled size)
            rsCompute = new RSCompute(mRS, renderInSize, renderOutSize);
        }
    };

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // On every camera frame, checks if current RenderScript output surface is valid,
            // instantiated and checks that current RenderScript custom class is istantiated too
            if (rsRenderHolder.isValidHolder() && rsCompute != null) {
                // Execute computation
                rsCompute.compute(data, rsRenderHolder.getRSRenderHolderAllocation());

            }

            // Adds camera buffer back so that can be used on next acquired frame
            camera.addCallbackBuffer(data);
        }
    };

}