package com.speechmaru.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.speechmaru.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class CorsConfigTest extends IntegrationTest {

    @Test
    @DisplayName("헤더에 CORS 설정값이 들어간다.")
    void checkAllowedOrigins() throws Exception {
        //given
        String url = "/api/test";
        String origin = "http://localhost:3000";

        //when
        //options http method 사용
        ResultActions actions = mockMvc.perform(options(url)
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers",
                "Origin, X-Requested-With, Content-Type, Accept"));

        //then
        actions.andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(
                header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE"))
            .andExpect(header().string("Access-Control-Allow-Headers",
                "Origin, X-Requested-With, Content-Type, Accept"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andDo(print());
    }
}