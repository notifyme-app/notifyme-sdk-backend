package ch.ubique.notifyme.sdk.backend.ws.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.ws.controller.BaseControllerTest;
import ch.ubique.notifyme.sdk.backend.ws.controller.DebugControllerTestHelper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class WebControllerTest extends BaseControllerTest {
    private String webEndPoint;

    @Before
    public void setUp() {
        webEndPoint = "/";
    }

    @Test
    public void shouldReturnHtml() throws Exception{
        final ProblematicDiaryEntryWrapper wrapper =
                DebugControllerTestHelper.getTestProblematicDiaryEntryWrapper();

        mockMvc.perform(get(webEndPoint).content(wrapper.toByteArray()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("<!DOCTYPE html>")));
    }
}
