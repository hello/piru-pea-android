package com.hello.suripu.algorithm.event;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 9/5/14.
 */
public class SleepCycleAlgorithm {
    private DataSource<AmplitudeData> dataSource;
    private int slidingWindowSizeInMinutes = 15;


    public SleepCycleAlgorithm(final DataSource<AmplitudeData> dataSource, final int slidingWindowSizeInMinutes){
        this.dataSource = dataSource;
        this.slidingWindowSizeInMinutes = slidingWindowSizeInMinutes;
    }


    protected float getDensity(final List<AmplitudeData> buffer){

        if(buffer.size() == 0){
            return 0f;
        }

        int count = 0;
        for(final AmplitudeData datum:buffer){
            if(datum.amplitude != 0){
                count++;
            }
        }

        return (float)count / ((buffer.get(buffer.size() - 1).timestamp - buffer.get(0).timestamp) / (60 * 1000));
    }



    public ImmutableList<Segment> getCycles(final DateTime dateTime){

        float minDensity = 1f / 6f;
        final ArrayList<Float> densities = new ArrayList<Float>();
        final LinkedList<AmplitudeData> eventBuffer = new LinkedList<AmplitudeData>();

        final ImmutableList<AmplitudeData> data = this.dataSource.getDataForDate(dateTime);
        int count = 0;

        for(final AmplitudeData datum:data){
            if(eventBuffer.size() == slidingWindowSizeInMinutes){

                densities.add(getDensity(eventBuffer));
                eventBuffer.removeFirst();

            }

            eventBuffer.add(datum);
        }

        densities.add(getDensity(eventBuffer));


        final Float[] densityArray =  densities.toArray(new Float[0]);
        Arrays.sort(densityArray, new Comparator<Float>() {
            @Override
            public int compare(final Float aFloat, final Float aFloat2) {
                return aFloat.compareTo(aFloat2);
            }
        });

        float actualMaxDensity = densityArray[densityArray.length - 1];
        if(actualMaxDensity < minDensity && densityArray.length > 2){
            minDensity = (densityArray[densityArray.length - 1] - densityArray[0]) / 2f;
        }

        long segmentStart = -1;
        long segmentEnd = segmentStart;
        eventBuffer.clear();

        ArrayList<Segment> segments = new ArrayList<Segment>();

        for(final AmplitudeData datum:data){
            if(eventBuffer.size() == slidingWindowSizeInMinutes){

                float density = getDensity(eventBuffer);
                if(density >= minDensity) {
                    if (segmentStart == -1) {
                        segmentStart = eventBuffer.getFirst().timestamp;
                        //segmentEnd = segmentStart;
                    }//else {
                        segmentEnd = eventBuffer.getLast().timestamp;
                    //}

                }else{
                    if(segmentStart > 0 && segmentEnd > segmentStart) {
                        final Segment segment = new Segment();
                        segment.setStartTimestamp(segmentStart);
                        segment.setEndTimestamp(segmentEnd);

                        segments.add(segment);
                    }

                    segmentStart = -1;
                    segmentEnd = segmentStart;
                }

                eventBuffer.removeFirst();

            }

            eventBuffer.add(datum);
        }


        if(segmentStart > 0 && segmentEnd > segmentStart){
            final Segment segment = new Segment();
            segment.setStartTimestamp(segmentStart);
            segment.setEndTimestamp(segmentEnd);

            segments.add(segment);
        }


        return ImmutableList.copyOf(segments);
    }

}
