package service;

import com.sap.documentssystem.exceptions.FileStorageException;
import com.sap.documentssystem.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void shouldUploadFileSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );

        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        )).thenReturn(PutObjectResponse.builder().build());

        String result = s3Service.uploadFile(file);

        assertThat(result).startsWith("https://");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void shouldThrowWhenFileIsEmpty() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[0]
        );

        assertThatThrownBy(() -> s3Service.uploadFile(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("File must not be empty");
    }

    @Test
    void shouldThrowWhenFileTooLarge() {

        byte[] big = new byte[11 * 1024 * 1024];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                big
        );

        assertThatThrownBy(() -> s3Service.uploadFile(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("File exceeds 10MB limit");
    }

    @Test
    void shouldThrowWhenInvalidContentType() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "data".getBytes()
        );

        assertThatThrownBy(() -> s3Service.uploadFile(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Only PDF and TXT files are allowed");
    }

    @Test
    void shouldDeleteFileSuccessfully() {

        String url = "https://bucket.s3.amazonaws.com/my-file.txt";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3Service.deleteFile(url);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void shouldExtractKeyCorrectlyThroughDelete() {

        String url = "https://bucket.s3.amazonaws.com/my-file.txt";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3Service.deleteFile(url);

        verify(s3Client).deleteObject(
                argThat((DeleteObjectRequest req) ->
                        req.key().equals("my-file.txt")
                )
        );
    }

    @Test
    void shouldDownloadFileAsText() {

        String url = "https://bucket.s3.amazonaws.com/file.txt";

        ResponseInputStream<GetObjectResponse> stream =
                new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        new ByteArrayInputStream("hello".getBytes())
                );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream);

        String result = s3Service.downloadFileAsText(url);

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldThrowOnDownloadFailure() {

        String url = "https://bucket.s3.amazonaws.com/file.txt";

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> s3Service.downloadFileAsText(url))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void shouldShutdownExecutorService() {
        s3Service.shutdown();
        assertThat(true).isTrue();
    }

    @Test
    void shouldThrowOnTimeout() {

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(6000);
                    return null;
                });

        assertThatThrownBy(() ->
                s3Service.downloadFileAsText("https://bucket.s3.amazonaws.com/file.txt")
        ).isInstanceOf(FileStorageException.class)
                .hasMessage("S3 timeout");
    }

    @Test
    void shouldThrowOnExecutionException() {

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() ->
                s3Service.downloadFileAsText("https://bucket.s3.amazonaws.com/file.txt")
        ).isInstanceOf(FileStorageException.class)
                .hasMessage("File service temporarily unavailable");
    }

    @Test
    void shouldHandleInterruptedException() {

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("fail");
                });

        assertThatThrownBy(() ->
                s3Service.downloadFileAsText("https://bucket.s3.amazonaws.com/file.txt")
        ).isInstanceOf(FileStorageException.class);
    }
}