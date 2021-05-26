package ch.ubique.swisscovid.cn.sdk.backend.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"postgres", "test-config", "dev"})
// @TestPropertySource(properties = {})
public abstract class BaseDataServiceTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ObjectMapper objectMapper;

    @Before
    public void setup() throws Exception {
        this.objectMapper = new ObjectMapper(new JsonFactory());
        this.objectMapper.registerModule(new JavaTimeModule());
        // this makes sure, that the objectmapper does not fail, when no filter is provided.
        this.objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
    }
}
