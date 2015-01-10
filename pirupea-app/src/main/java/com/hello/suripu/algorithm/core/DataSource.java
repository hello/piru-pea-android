package com.hello.suripu.algorithm.core;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by pangwu on 5/22/14.
 */
public interface DataSource<T> {
    List<T> getDataForDate(final DateTime day);
}
