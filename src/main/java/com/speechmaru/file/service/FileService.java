package com.speechmaru.file.service;

import com.speechmaru.file.document.File;
import com.speechmaru.file.dto.response.FileResponse;
import com.speechmaru.file.exception.FileNotFoundException;
import com.speechmaru.file.exception.InvalidFileException;
import com.speechmaru.file.exception.SttRequestException;
import com.speechmaru.file.repository.FileRepository;
import com.speechmaru.record.dto.response.SttResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    public static final String PRE_URI = "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnkzdnklb8xy/b/oreo/o/";

    private final FileRepository fileRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    public String saveFile(String filename, String title) {
        File savedFile = fileRepository.save(new File(filename, title));
        return savedFile.getId();
    }

    // python으로 음성 파일을 전달하고 분석 결과를 얻는다.
    public SttResponse analyzeVoiceFile(MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource contentsAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", contentsAsResource);
        } catch (IOException e) {
            throw new InvalidFileException("유효하지 않은 파일입니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<SttResponse> response = restTemplateBuilder.build()
                .exchange("http://flask:8000/stt", HttpMethod.POST, requestEntity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            String errorMessage = responseBody.replaceAll("^.*\"(.*)\"$", "$1");
            throw new SttRequestException(errorMessage);
        }

    }

    public Page<FileResponse> findFiles(Pageable pageable) {
        return fileRepository.findAll(pageable).map(FileResponse::new);
    }

    public FileResponse deleteFile(String id) {
        File file = fileRepository.findById(id)
            .orElseThrow(() -> new FileNotFoundException("파일을 찾을 수 없습니다."));
        fileRepository.delete(file);
        return new FileResponse(file);
    }
}
