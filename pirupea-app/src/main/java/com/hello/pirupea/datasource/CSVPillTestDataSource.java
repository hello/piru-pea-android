package com.hello.pirupea.datasource;

import com.hello.ble.util.IO;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 5/22/14.
 */
public class CSVPillTestDataSource implements DataSource<AmplitudeData> {

    private final File targetFile;
    public CSVPillTestDataSource(final String fileName){
        targetFile = IO.getFile(fileName);
    }

    /*
    * The user can change the data, I don't want to make a return copy since it is designed for android.
     */
    @Override
    public List<AmplitudeData> getDataForDate(final DateTime targetDate) {

        final LinkedList<AmplitudeData> rawData = new LinkedList<AmplitudeData>();

        try {
            final BufferedReader reader = new BufferedReader(new FileReader(this.targetFile), 100 * 1024);
            String headerLine = reader.readLine();
            String[] columns = headerLine.split(",");

            String line = reader.readLine();
            while (line != null){
                columns = line.split(",");
                if(columns.length == 0){
                    line = reader.readLine();
                    continue;
                }


                final long timestamp = Long.valueOf(columns[0]);
                final long amplitude = Long.valueOf(columns[1]) == -1 ? 0 : Long.valueOf(columns[1]);
                rawData.add(new AmplitudeData(timestamp, Math.sqrt(amplitude), DateTimeZone.getDefault().getOffset(DateTime.now())));
                line = reader.readLine();
            }

            reader.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }

        return rawData;

    }
}
