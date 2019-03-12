/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.nn.benchmark.app;

import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;

import com.android.nn.benchmark.core.TestModels;
import com.android.nn.benchmark.util.CSVWriter;
import com.android.nn.benchmark.util.TestExternalStorageActivity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests that run all models/datasets/backend that are required for scoring the device.
 * Produces a CSV file with benchmark results.
 * Currently it runs a mobilenet network over provided ~400 image datasets, on NNAPI and CPU.
 *
 * Tu use, please run:
 * adb shell am instrument -w -e size large
 * com.android.nn.benchmark.app.NNScoringTest/androidx.test.runner.AndroidJUnitRunner
 *
 * To fetch results, please run:
 * adb pull /data/data/com.android.nn.benchmark.app/benchmark.csv
 */
// TODO(pszczepaniak): Make it an activity, so it's possible to start from UI
@RunWith(Parameterized.class)
public class NNScoringTest extends BenchmarkTestBase {
    private static final String RESULT_FILENAME = "mlts_benchmark.csv";
    private static final String TAG = NNScoringTest.class.getSimpleName();

    private static File csvPath;
    private static CSVWriter csvWriter;

    public NNScoringTest(TestModels.TestModelEntry model) {
        super(model);
    }

    @Override
    protected void prepareTest() {
        super.prepareTest();
    }

    private static final String[] MODEL_NAMES = new String[]{
            "tts_float",
            "asr_float",
            "mobilenet_v1_1.0_224_quant_topk_aosp",
            "mobilenet_v1_1.0_224_topk_aosp",
            "mobilenet_v1_0.75_192_quant_topk_aosp",
            "mobilenet_v1_0.75_192_topk_aosp",
            "mobilenet_v1_0.5_160_quant_topk_aosp",
            "mobilenet_v1_0.5_160_topk_aosp",
            "mobilenet_v1_0.25_128_quant_topk_aosp",
            "mobilenet_v1_0.25_128_topk_aosp",
            "mobilenet_v2_0.35_128_topk_aosp",
            "mobilenet_v2_0.5_160_topk_aosp",
            "mobilenet_v2_0.75_192_topk_aosp",
            "mobilenet_v2_1.0_224_topk_aosp",
            "mobilenet_v2_1.0_224_quant_topk_aosp",
    };

    @Parameters(name = "{0}")
    public static List<TestModels.TestModelEntry> modelsList() {
        List<TestModels.TestModelEntry> models = new ArrayList<>();
        for (String modelName : MODEL_NAMES) {
            models.add(TestModels.getModelByName(modelName));
        }
        return Collections.unmodifiableList(models);
    }

    @Test
    @LargeTest
    public void testTFLite() throws IOException {
        if (!TestExternalStorageActivity.testWriteExternalStorage(getActivity(), false)) {
            throw new IOException("No permission to store results in external storage");
        }

        setUseNNApi(false);
        setCompleteInputSet(true);
        TestAction ta = new TestAction(mModel, WARMUP_REPEATABLE_SECONDS,
                COMPLETE_SET_TIMEOUT_SECOND);
        runTest(ta, mModel.getTestName());

        try (CSVWriter writer = new CSVWriter(getLocalCSVFile())) {
            writer.write(ta.getBenchmark());
        }
    }

    @Test
    @LargeTest
    public void testNNAPI() throws IOException {
        if (!TestExternalStorageActivity.testWriteExternalStorage(getActivity(), false)) {
            throw new IOException("No permission to store results in external storage");
        }

        setUseNNApi(true);
        setCompleteInputSet(true);
        TestAction ta = new TestAction(mModel, WARMUP_REPEATABLE_SECONDS,
                COMPLETE_SET_TIMEOUT_SECOND);
        runTest(ta, mModel.getTestName());


        try (CSVWriter writer = new CSVWriter(getLocalCSVFile())) {
            writer.write(ta.getBenchmark());
        }
    }

    public static File getLocalCSVFile() {
        return new File("/data/data/com.android.nn.benchmark.app", RESULT_FILENAME);
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        // Clear up CSV file in data directory for result storage
        File localResults = getLocalCSVFile();
        localResults.delete();
        localResults.createNewFile();
        try (CSVWriter writer = new CSVWriter(localResults)) {
            writer.writeHeader();
        }
    }

    @AfterClass
    public static void afterClass() throws IOException {
        // Copy results to external storage.
        // We can't dump result straight there, due to append mode not working on external storage.
        // And we need to store results in external storage for easy adb pull retreival on
        // non user-debug devices.
        File externalStorageCSVFile = new File(Environment.getExternalStorageDirectory(),
                RESULT_FILENAME);
        externalStorageCSVFile.delete();
        Files.copy(getLocalCSVFile().toPath(), externalStorageCSVFile.toPath());
    }

}
