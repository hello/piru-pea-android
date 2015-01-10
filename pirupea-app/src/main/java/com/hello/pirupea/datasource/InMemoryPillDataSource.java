package com.hello.pirupea.datasource;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.utils.DataCutter;
import com.hello.suripu.core.db.models.TempTrackerData;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 9/5/14.
 */
public class InMemoryPillDataSource implements DataSource<AmplitudeData> {

    private ArrayList<AmplitudeData> data = new ArrayList<AmplitudeData>();

    public InMemoryPillDataSource(final List<TempTrackerData> pillData){
        for(final TempTrackerData datum:pillData){
            data.add(new AmplitudeData(datum.timestamp, Math.sqrt(datum.value == -1 ? 0 : datum.value), DateTimeZone.getDefault().getOffset(datum.timestamp)));
        }
    }

    @Override
    public List<AmplitudeData> getDataForDate(final DateTime day) {
        final DataCutter cutter = new DataCutter(day.minusDays(1).withTimeAtStartOfDay().plusHours(18),
                day.withTimeAtStartOfDay().plusHours(18));

        return cutter.process(data);
    }
}
