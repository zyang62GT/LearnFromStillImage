package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.jetpac.deepbelief.DeepBelief.JPCNNLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class LearnActivity extends Activity {
    String predictorName;
    File f;


    public enum predictionState{
        eWaiting,
        ePositiveLearning,
        eNegativeWaiting,
        eNegativeLearning,
        eEnd
    };



    String DirectoryName;
    Activity act;
    Context ctx;

    TextView Label;

    //Pointer varaibles for jpcnn api
    Pointer networkHandle = null;
    Pointer trainerHandle = null;
    Pointer predictor = null;

    //Limit values for predictions
    int kPosPreT = 200;
    int kNegPreT = 500;


    int posPreC;
    int negPreC;

    float preVal = 0;
    String labelsText;

    //begin in eWaiting state
    predictionState state = predictionState.eWaiting;

    //function to trigger next state
    public void triggerNextState(){
        switch (state){
            case eWaiting:
                startPosL();
                break;
            case ePositiveLearning:
                startNegW();
                break;
            case eNegativeWaiting:
                startNegL();
                break;
            case eNegativeLearning:
                startPre();
                break;
            case eEnd:
                finish();
                break;
        }}

    public void startPosL(){
        if (trainerHandle != null){
            JPCNNLibrary.INSTANCE.jpcnn_destroy_trainer(trainerHandle);
        }
        trainerHandle = JPCNNLibrary.INSTANCE.jpcnn_create_trainer();
        posPreC = 0;
        state = state.ePositiveLearning;
        //load and process positive learning images here
        String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/"+DirectoryName;
        f = new File(path);
        File file[] = f.listFiles();
        //kPosPreT = file.length;
        for(int i=0;i<file.length;i++){
            Log.i("pos", path+"/"+file[i].getName().toString());
            //Bitmap bm = getBitmapFromAsset(file[i].getName().toString());
            //classifyBitmap(bm);
            if(file[i].getName().toString().contains("neg")){
                continue;
            }
            File imgFile = new  File(path+"/"+file[i].getName().toString());
            if(imgFile.exists()){
                Bitmap bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                //ImageView myImage = (ImageView) findViewById(R.id.imageView);
                //myImage.setImageBitmap(bm);
                classifyBitmap(bm);
                Log.i("sssssssssssssss",path+"/neg");
            }
        }

        triggerNextState();


    }

    public void startNegW(){
        state = state.eNegativeWaiting;
        Log.i("sssswaitsss","/neg");
        triggerNextState();
    }

    public void startNegL(){
        negPreC = 0;
        state = state.eNegativeLearning;
        //load and process negative learning images here
        String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/"+DirectoryName+"/neg";
        f = new File(path);
        File file[] = f.listFiles();
        //kNegPreT = file.length;
        for(int i=0;i<file.length;i++){
            Log.i("neg", path+"/"+file[i].getName().toString());
            //Bitmap bm = getBitmapFromAsset(file[i].getName().toString());
            //classifyBitmap(bm);
            File imgFile = new  File(path+"/"+file[i].getName().toString());
            if(imgFile.exists()){
                Bitmap bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                //ImageView myImage = (ImageView) findViewById(R.id.imageView);
               // myImage.setImageBitmap(bm);
                classifyBitmap(bm);
            }
        }
        triggerNextState();
    }







    public File getDir(String albumName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),albumName);
        if (!file.mkdirs()) {
            Log.e("LOG_TAG", "Directory not created");
        }
        return file;
    }

    public void startPre(){
        state = state.eEnd;
        if (predictor != null){
            JPCNNLibrary.INSTANCE.jpcnn_destroy_predictor(predictor);
        }
        predictor = JPCNNLibrary.INSTANCE.jpcnn_create_predictor_from_trainer(trainerHandle);
        AssetManager am = ctx.getAssets();
        String baseFileName = predictorName.trim()+".txt";
        String dataDir = ctx.getFilesDir().getAbsolutePath();
        String preFile = dataDir + "/" + baseFileName;
        try{

            JPCNNLibrary.INSTANCE.jpcnn_save_predictor(preFile, predictor);
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.i("prediction file", preFile);
        String newFile = "newFile";
        File gtem = new File(getDir(newFile),predictorName.trim()+".txt");
        File pFile = new File(dataDir,predictorName.trim()+".txt");
        try {
            copy(pFile,gtem);
        } catch (IOException e) {
            e.printStackTrace();
        }

        finish();


        this.onDestroy();
    }






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        act = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_learn);

        Intent intent = this.getIntent();
        predictorName = intent.getStringExtra(DirectoryActivity.EXTRA_MESSAGE);
        predictorName+="PP";
        Log.i("String", predictorName);

        DirectoryName = intent.getStringExtra(DirectoryActivity.EXTRA_MESSAGE);

        Label = (TextView) findViewById(R.id.labelsView);





        initDeepBelief();
    }










    void initDeepBelief() {
        AssetManager am = ctx.getAssets();
        String baseFileName = "jetpac.ntwk";
        String dataDir = ctx.getFilesDir().getAbsolutePath();
        String networkFile = dataDir + "/" + baseFileName;
        copyAsset(am, baseFileName, networkFile);
        networkHandle = JPCNNLibrary.INSTANCE.jpcnn_create_network(networkFile);
        Bitmap lenaBitmap = getBitmapFromAsset("lena.png");
        classifyBitmap(lenaBitmap);



    }


    void classifyBitmap(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int pixelCount = (width * height);
        final int bytesPerPixel = 4;
        final int byteCount = (pixelCount * bytesPerPixel);
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        bitmap.copyPixelsToBuffer(buffer);
        byte[] pixels = buffer.array();
        Pointer imageHandle = JPCNNLibrary.INSTANCE.jpcnn_create_image_buffer_from_uint8_data(pixels, width, height, 4, (4 * width), 0, 0);

        PointerByReference predictionsValuesRef = new PointerByReference();
        IntByReference predictionsLengthRef = new IntByReference();
        PointerByReference predictionsNamesRef = new PointerByReference();
        IntByReference predictionsNamesLengthRef = new IntByReference();
        long startT = System.currentTimeMillis();
        JPCNNLibrary.INSTANCE.jpcnn_classify_image(
                networkHandle,
                imageHandle,
                2, //SAMPLE FLAGS: 0 = DEFAULT(CENTERED), 1 = MULTISAMPLE, 2 = RANDOM_SAMPLE
                -2, //LAYEROFFSET
                predictionsValuesRef,
                predictionsLengthRef,
                predictionsNamesRef,
                predictionsNamesLengthRef);
        long stopT = System.currentTimeMillis();
        float duration = (float)(stopT-startT) / 1000.0f;
        System.err.println("jpcnn_classify_image() took " + duration + " seconds.");

        JPCNNLibrary.INSTANCE.jpcnn_destroy_image_buffer(imageHandle);

        Pointer predictionsValuesPointer = predictionsValuesRef.getValue();
        final int predictionsLength = predictionsLengthRef.getValue();

        System.err.println(String.format("predictionsLength = %d", predictionsLength));

        float[] predictionsValues = predictionsValuesPointer.getFloatArray(0, predictionsLength);
        predictionHandler(predictionsValuesPointer, predictionsLength);

        switch(state){
            case eWaiting:
                break;
            case ePositiveLearning:
                labelsText = Cast(state) + ", progress: " + posPreC*100/kPosPreT + "%";
                Label.setText(labelsText);
                break;
            case eNegativeWaiting:
                labelsText = Cast(state);
                Label.setText(labelsText);
                break;
            case eNegativeLearning:
                labelsText = Cast(state) + ", progress: " + negPreC * 100 /kNegPreT + "%";
                Label.setText(labelsText);
                break;
            case eEnd:
                finish();
                break;
        }
        Log.i(Cast(state), "state");
    }
    //for logging state
    public String Cast(predictionState e){
        switch(e){
            case eWaiting:
                return "eWaiting";
            case ePositiveLearning:
                return "ePositiveLearning";
            case eNegativeWaiting:
                return "eNegativeWaiting";
            case eNegativeLearning:
                return "eNegativeLearning";
            case eEnd:
                return "eEnd";
        }
        return "none";
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void process(){

    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private Bitmap getBitmapFromAsset(String strName) {
        AssetManager assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(strName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
    }

    public void predictionHandler (Pointer predictions,int predictionsLength){
        switch (state){
            case eWaiting:
                triggerNextState();
                break;
            case ePositiveLearning:
                JPCNNLibrary.INSTANCE.jpcnn_train(trainerHandle, 1.0f, predictions.getFloatArray(0,predictionsLength), predictionsLength);
                posPreC += 1;
                if (posPreC >= kPosPreT){
                    triggerNextState();
                }
                break;
            case eNegativeWaiting:
                triggerNextState();
                break;
            case eNegativeLearning:
                JPCNNLibrary.INSTANCE.jpcnn_train(trainerHandle, 0.0f, predictions.getFloatArray(0,predictionsLength), predictionsLength);
                negPreC += 1;
                if (negPreC >= kNegPreT){
                    triggerNextState();
                }
                break;
            case eEnd:
                finish();
                break;
        }
    }
}
