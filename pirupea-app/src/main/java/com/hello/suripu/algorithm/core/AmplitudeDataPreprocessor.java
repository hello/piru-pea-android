package com.hello.suripu.algorithm.core;

import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public interface AmplitudeDataPreprocessor {
    List<AmplitudeData> process(final List<AmplitudeData> rawData);
}
