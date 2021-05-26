package ch.ubique.swisscovid.cn.sdk.backend.ws.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.BaseControllerTest;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.DebugControllerTestHelper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"dev", "enable-debug"})
public class WebControllerTest extends BaseControllerTest {
  private String webEndPoint;

  public WebControllerTest() {
    super(false);
  }

  @Before
  public void setUp() {
    webEndPoint = "/";
  }

  @Test
  public void shouldReturnHtml() throws Exception {
    final var wrapper = DebugControllerTestHelper.getTestDiaryEntryWrapper();

    mockMvc
        .perform(get(webEndPoint).content(wrapper.toByteArray()))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.startsWith("<!DOCTYPE html>")));
  }
}
