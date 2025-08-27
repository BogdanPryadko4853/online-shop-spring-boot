import com.audio.service.impl.MinioStorageService;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestStorage {
    @Mock
    private MinioClient minioClient;
    @Mock
    private ObjectWriteResponse objectWriteResponse;
    @Mock
    private GetObjectResponse getObjectResponse;

    private MinioStorageService minioStorageService;

    @BeforeEach
    public void setUp() throws Exception {
        minioStorageService = new MinioStorageService(minioClient, "test-bucket");

    }

    @Nested
    @DisplayName("Загрузка файла")
    class UploadFile {
        @Test
        void uploadFile() throws Exception {
            String objectName = "test-object";
            byte[] content = "test content".getBytes();
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content
            );

            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(objectWriteResponse);

            String result = minioStorageService.uploadFile(file, objectName);

            assertEquals(objectName, result);

            verify(minioClient).putObject(any(PutObjectArgs.class));
        }
    }

    @Nested
    @DisplayName("Удаление файла")
    class DeleteFile {
        @Test
        void deleteFile() throws Exception {
            String objectName = "test-object";
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
            minioStorageService.deleteFile(objectName);

            verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        }
    }

    @Nested
    @DisplayName("Получение файла")
    class GetFile {
        @Test
        void getFile() throws Exception {
            String objectName = "test-object";
            byte[] content = "test content".getBytes();
            when(getObjectResponse.readAllBytes()).thenReturn(content);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

            InputStream result = minioStorageService.getFile(objectName);
            assertNotNull(result);
            assertArrayEquals(content, result.readAllBytes());

            verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        }
    }

}
