package service;

import com.sap.documentssystem.service.FileService;
import com.sap.documentssystem.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private FileService fileService;

    // ---------------- UPLOAD ----------------

    @Test
    void shouldUploadFileSuccessfully() {

        MultipartFile file = mock(MultipartFile.class);

        when(s3Service.uploadFile(file))
                .thenReturn("https://s3/test-file.txt");

        String result = fileService.upload(file);

        assertThat(result).isEqualTo("https://s3/test-file.txt");

        verify(s3Service).uploadFile(file);
    }

    // ---------------- DELETE ----------------

    @Test
    void shouldDeleteFileSuccessfully() {

        String fileUrl = "https://s3/test-file.txt";

        doNothing().when(s3Service).deleteFile(fileUrl);

        fileService.delete(fileUrl);

        verify(s3Service).deleteFile(fileUrl);
    }

    // ---------------- EDGE CASE ----------------

    @Test
    void shouldPassNullToS3WhenFileIsNull() {

        when(s3Service.uploadFile(null))
                .thenReturn("null-url");

        String result = fileService.upload(null);

        assertThat(result).isEqualTo("null-url");

        verify(s3Service).uploadFile(null);
    }
}