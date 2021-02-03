package ch.ubique.notifyme.sdk.backend.ws.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "enable-debug"})
public class DebugControllerTest extends BaseControllerTest {

    @LocalServerPort private int port;

    private String diaryEntriesEndPoint;

    @Before
    public void setUp() {
        final String debugControllerEndPoint = "http://localhost:" + port + "/v1/debug";
        diaryEntriesEndPoint = debugControllerEndPoint + "/diaryEntries";
    }

    @Test
    public void uploadDiaryEntryProtobufShouldReturnOk() throws Exception {
        final long startOf2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final long startOf2021 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2021)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final long startOf2022 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2022)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final ProblematicDiaryEntry diaryEntry0 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("name0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(startOf2020)
                        .setCheckOutTIme(startOf2021)
                        .build();

        final ProblematicDiaryEntry diaryEntry1 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("name1")
                        .setLocation("location1")
                        .setRoom("room1")
                        .setVenueType(VenueType.CAFETERIA)
                        .setCheckinTime(startOf2021)
                        .setCheckOutTIme(startOf2022)
                        .build();

        final ProblematicDiaryEntryWrapper wrapper =
                ProblematicDiaryEntryWrapper.newBuilder()
                        .addDiaryEntries(diaryEntry0)
                        .addDiaryEntries(diaryEntry1)
                        .build();

        mockMvc.perform(
                        post(diaryEntriesEndPoint)
                                .contentType("application/x-protobuf")
                                .content(wrapper.toByteArray()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
