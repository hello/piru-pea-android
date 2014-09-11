package pea.priu.hello.com.priupea;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.pirupea.SmartAlarmTestService;
import com.hello.pirupea.core.IO;
import com.hello.suripu.algorithm.core.Segment;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);



    }

    public void testSmartAlarmTime(){
        final long alarmDeadLine = DateTime.parse("09/11/2014 08:35:00", DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")).getMillis();
        final File sleepCyclesFile = IO.getFile("cycles_11_09_2014.json");
        assertEquals(true, sleepCyclesFile.exists());


        final ObjectMapper mapper = new ObjectMapper();
        try {
            final List<Segment> sleepCycles = mapper.readValue(sleepCyclesFile, new TypeReference<List<Segment>>() {});
            final DateTime smartAlarmTime = SmartAlarmTestService.getSmartAlarmTimestamp(sleepCycles, alarmDeadLine);
            assertEquals(true, smartAlarmTime.getMillis() >= alarmDeadLine - 20 * DateTimeConstants.MILLIS_PER_MINUTE);
            assertEquals(true, smartAlarmTime.getMillis() <= alarmDeadLine);
        } catch (IOException e) {
            e.printStackTrace();
            assertEquals(false, true);
        }
    }
}