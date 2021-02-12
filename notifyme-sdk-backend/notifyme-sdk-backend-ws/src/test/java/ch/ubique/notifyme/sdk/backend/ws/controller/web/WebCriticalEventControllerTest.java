package ch.ubique.notifyme.sdk.backend.ws.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapper.DiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.ws.controller.BaseControllerTest;
import ch.ubique.notifyme.sdk.backend.ws.controller.DebugControllerTestHelper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class WebCriticalEventControllerTest extends BaseControllerTest {
    private String criticalEventEndPoint;

    @Before
    public void setUp() throws Exception {
        criticalEventEndPoint = "/criticalevent";

        final DiaryEntryWrapper wrapper = DebugControllerTestHelper.getTestDiaryEntryWrapper();

        mockMvc.perform(
                post("/v1/debug/criticalevent")
                        .contentType("application/x-protobuf")
                        .content(wrapper.toByteArray()));
    }

    @Test
    public void shouldReturnHtml() throws Exception {
        final DiaryEntryWrapper wrapper = DebugControllerTestHelper.getTestDiaryEntryWrapper();

        mockMvc.perform(
                        get(criticalEventEndPoint)
                                .contentType("application/x-protobuf")
                                .content(wrapper.toByteArray()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("<!DOCTYPE html>")));
    }
}
