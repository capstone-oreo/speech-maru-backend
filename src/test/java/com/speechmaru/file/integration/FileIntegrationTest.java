package com.speechmaru.file.integration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.DeleteObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.speechmaru.IntegrationTest;
import com.speechmaru.file.document.File;
import com.speechmaru.file.dto.response.FileResponse;
import com.speechmaru.file.repository.FileRepository;
import com.speechmaru.record.document.Record;
import com.speechmaru.record.dto.response.SttResponse;
import com.speechmaru.record.repository.RecordRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;

public class FileIntegrationTest extends IntegrationTest {

    @Autowired
    FileRepository fileRepository;

    @Autowired
    RecordRepository recordRepository;

    @AfterEach
    void tearDown() {
        recordRepository.deleteAll();
        fileRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/files")
    void postFiles() throws Exception {
        //given
        String uri = "/api/files";
        String title = "file title";
        List<String> texts = List.of("hello", "world");
        SttResponse mockStt = new SttResponse(texts, null, null, null, null, null);

        MockMultipartFile mockFile = new MockMultipartFile("file", "test.wav", "audio/wav",
            "test data".getBytes());

        given(objectStorage.putObject(any(PutObjectRequest.class))).willReturn(
            mock(PutObjectResponse.class));
        RestTemplate restTemplate = mock(RestTemplate.class);
        given(restTemplateBuilder.build()).willReturn(restTemplate);

        ResponseEntity<SttResponse> response = ResponseEntity.ok(mockStt);
        given(
            restTemplate.exchange(eq("http://flask:8000/stt"), eq(HttpMethod.POST), any(),
                any(ParameterizedTypeReference.class))).willReturn(response);

        //when
        ResultActions actions = mockMvc.perform(
            multipart(uri).file(mockFile).param("title", title));

        //then
        actions.andExpect(status().isOk())
            .andExpect(result -> {
                String id = result.getResponse().getContentAsString();
                File file = fileRepository.findById(id).orElseThrow();
                assertThat(file.getTitle()).isEqualTo(title);

                Record record = recordRepository.findByFile_Id(id).orElseThrow();
                assertThat(record.getText()).isEqualTo(List.of("hello", "world"));
                System.out.println(record.getVolume());
            })
            .andDo(print());
    }

    @Nested
    @DisplayName("GET /api/files?page={}&size={}")
    class FindFiles {

        String uri = "/api/files";
        List<File> savedFiles;

        @BeforeEach
        void setUp() {
            List<File> files = List.of(new File("a.com", "t1"), new File("b.com", "t2"),
                new File("c.com", "t3"), new File("d.com", "t4"));
            savedFiles = fileRepository.saveAll(files);
        }

        @Test
        @DisplayName("페이지네이션하여 File을 조회한다.")
        void findFiles() throws Exception {
            //given
            int page = 1;
            int size = 2;
            String pagingUri = uri + "?page=" + page + "&size=" + size;

            //when
            ResultActions actions = mockMvc.perform(
                get(pagingUri).contentType(MediaType.APPLICATION_JSON));

            //then
            List<FileResponse> expected = savedFiles.stream().skip(2).map(FileResponse::new)
                .toList();

            actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.currentSize").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content", equalTo(asParsedJson(expected))))
                .andDo(print());
        }

        @Test
        @DisplayName("페이지네이션 Default 값으로 File을 조회한다.")
        void findFilesDefault() throws Exception {
            //given
            String pagingUri = uri + "?page=" + "&size=";

            //when
            ResultActions actions = mockMvc.perform(
                get(pagingUri).contentType(MediaType.APPLICATION_JSON));

            //then
            List<FileResponse> expected = savedFiles.stream().map(FileResponse::new)
                .toList();

            actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.currentSize").value(4))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content", equalTo(asParsedJson(expected))))
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("DELETE /api/files/{id}")
    class DeleteFile {

        List<File> savedFiles;
        List<Record> savedRecords;

        String getUri(String id) {
            return "/api/files/" + id;
        }

        @BeforeEach
        void setUp() {
            List<File> files = List.of(new File("a.com", "t1"), new File("b.com", "t2"));
            savedFiles = fileRepository.saveAll(files);
            List<Record> records = List.of(
                Record.builder()
                    .text(List.of("text1")).file(savedFiles.get(0))
                    .build(),
                Record.builder()
                    .text(List.of("text2")).file(savedFiles.get(1))
                    .build());
            savedRecords = recordRepository.saveAll(records);
        }

        @Test
        @DisplayName("File과 record를 삭제한다.")
        void deleteFileAndRecord() throws Exception {
            //given
            String id = savedFiles.get(0).getId();
            String uri = getUri(id);
            given(objectStorage.deleteObject(any(DeleteObjectRequest.class))).willReturn(
                mock(DeleteObjectResponse.class));

            //when
            ResultActions actions = mockMvc.perform(
                delete(uri).contentType(MediaType.APPLICATION_JSON));

            //then
            actions.andExpect(status().isNoContent())
                .andDo(print());

            List<Record> records = recordRepository.findAll();
            assertThat(records).hasSize(1);
            assertThat(records.get(0)).usingRecursiveComparison().
                ignoringFields("file").isEqualTo(savedRecords.get(1));

            List<File> files = fileRepository.findAll();
            assertThat(files).hasSize(1);
            assertThat(files.get(0)).usingRecursiveComparison().isEqualTo(savedFiles.get(1));
        }
    }
}
