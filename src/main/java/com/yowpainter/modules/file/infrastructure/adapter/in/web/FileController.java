package com.yowpainter.modules.file.infrastructure.adapter.in.web;

import com.yowpainter.modules.file.application.service.FileService;
import com.yowpainter.modules.file.infrastructure.adapter.in.web.dto.FileResponseDto;
import com.yowpainter.shared.kernel.port.KernelFilePort;
import com.yowpainter.shared.security.KernelAccessTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files BFF", description = "Gestion des fichiers BFF")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un fichier (BFF)")
    public ResponseEntity<FileResponseDto> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "GENERAL") String documentType) {
        String accessToken = KernelAccessTokenResolver.requireAccessToken(authentication);
        FileResponseDto response = fileService.uploadFile(file, documentType, accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Telecharger un fichier (BFF)")
    public ResponseEntity<byte[]> downloadFile(
            Authentication authentication,
            @PathVariable("fileId") UUID fileId) {
        String accessToken = KernelAccessTokenResolver.resolveAccessToken(authentication);
        KernelFilePort.DownloadFileView fileView = fileService.downloadFile(fileId, accessToken);

        HttpHeaders headers = new HttpHeaders();
        if (fileView.contentType() != null) {
            headers.setContentType(MediaType.parseMediaType(fileView.contentType()));
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        if (fileView.contentDisposition() != null) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, fileView.contentDisposition());
        }
        if (fileView.content() != null) {
            headers.setContentLength(fileView.content().length);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileView.content());
    }
}
