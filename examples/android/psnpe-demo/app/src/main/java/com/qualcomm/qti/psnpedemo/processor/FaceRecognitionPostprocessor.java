/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.psnpedemo.processor;

import android.app.Application;
import android.util.Log;

import com.qualcomm.qti.psnpe.PSNPEManager;
import com.qualcomm.qti.psnpedemo.components.BenchmarkApplication;
import com.qualcomm.qti.psnpedemo.networkEvaluation.Result;
import com.qualcomm.qti.psnpedemo.networkEvaluation.FaceRecognitionResult;
import com.qualcomm.qti.psnpedemo.utils.MathUtils;
import com.qualcomm.qti.psnpedemo.utils.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

public class FaceRecognitionPostprocessor extends PostProcessor{
    private static String TAG = "FaceRecognitionPostprocessor";
    private String datasetPath;
    private Boolean[] groundTruth;
    private ArrayList<Double> distances;
    public FaceRecognitionPostprocessor(int imageNumber) {
        super(imageNumber);
        datasetPath = BenchmarkApplication.getExternalDirPath() + "/datasets/lfw";
        distances = new ArrayList<Double>();
        groundTruth = null;
    }

    @Override
    public void getOutputCallback(String fileName, Map<String, float[]> outputs) {

    }

    @Override
    public boolean postProcessResult(ArrayList<File> bulkImage) {
        groundTruth = loadGroundTruth(datasetPath+"/gt_lfw.txt");
        int imgNum = bulkImage.size();
        int processCount = 0;
        float [] output1 = null;
        float [] output2 = null;
        float[] batchOutput = null;
        for (int i = 0; i < imgNum; ++i) {
            /* output:
             * <image1><image2>...<imageBulkSize>
             * split output and handle one by one.
             */
            int batchCount = i % batchSize;
            if(batchCount == 0){
                Map<String, float []> outputMap = PSNPEManager.getOutputSync(i/batchSize);
                String[] outputNames = PSNPEManager.getOutputTensorNames();
                batchOutput = outputMap.get(outputNames[0]);
                if(batchOutput == null) {
                    Log.e(TAG, "batchOutput data is null");
                    return false;
                }
                //save output for debug
                /*
                String outputDirName = String.format("%s/results/FaceRecognition/Result_%d",
                                                     BenchmarkApplication.getExternalDirPath(), i);
                File outputDir = new File(outputDirName);
                outputDir.mkdirs();
                String outputFileName = String.format("%s.raw", outputNames[0].replace(":","_"));
                Util.writeArrayTofile(outputDirName + "/" + outputFileName, batchOutput);
                 */
            }
            int outputSize = batchOutput.length/batchSize;
            if (processCount % 2 == 0){
                output1 = null == output1 ? new float[outputSize] : output1;
                System.arraycopy(batchOutput, outputSize*batchCount, output1, 0, outputSize);
            }
            else if (processCount % 2 == 1){
                output2 = null == output2 ? new float[outputSize] : output2;
                System.arraycopy(batchOutput, outputSize*batchCount, output2, 0, outputSize);
                double dist = calculateDistance(output1, output2);
                distances.add(dist);
            }
            processCount += 1;
        }
        return true;
    }

    @Override
    public void setResult(Result result) {
        double accuracy = 0;
        double accuracy_best = 0;
        double threshold_best = 0;
        double threshold_min = MathUtils.min(distances);
        double threshold_max = MathUtils.max(distances);
        double threshold_step = (threshold_max - threshold_min)/400;
        for (double threshold = threshold_min; threshold < threshold_max; threshold += threshold_step) {
            int tp = 0, fp = 0, tn = 0, fn = 0;
            for (int i = 0; i < distances.size(); ++i) {
                double dist = distances.get(i);
                boolean prediction_true = dist < threshold;
                if (prediction_true){
                    if (groundTruth[i]){
                        tp += 1;
                    }
                    else {
                        fp += 1;
                    }
                }
                else {
                    if (groundTruth[i]){
                        fn += 1;
                    }
                    else {
                        tn += 1;
                    }
                }
            }
            accuracy = (double)(tp + tn) / distances.size();
            if (accuracy > accuracy_best){
                threshold_best = threshold;
                accuracy_best = accuracy;
            }
        }

        FaceRecognitionResult res = (FaceRecognitionResult)result;
        res.setAccuracy(accuracy_best);
        res.setThreshold(threshold_best);
    }

    private double calculateDistance(float[] buf1, float[] buf2) {
        if (buf1.length != buf2.length) {
            throw new AssertionError("buf1.length != buf2.length");
        }
        double distance = 0;
        for (int i = 0; i < buf1.length; ++i) {
            double diff = buf1[i]-buf2[i];
            distance += Math.pow(diff, 2);
        }
        return distance;
    }

    private Boolean[] loadGroundTruth(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            ArrayList<Boolean> truthArray = new ArrayList<>();
            String line = null;
            while (true) {
                line = bufferedReader.readLine();
                if(null == line) {
                    break;
                }
                truthArray.add(line.equals("1"));
            }
            Boolean[] truthBuffer = new Boolean[truthArray.size()];
            truthArray.toArray(truthBuffer);
            return truthBuffer;
        }
        catch (Exception e){
            Log.e(TAG, String.format("load ground truth failed: %s", e.getMessage()));
            e.printStackTrace();
        }
        return null;
    }
}
