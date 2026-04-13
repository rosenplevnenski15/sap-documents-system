package service;

import com.sap.documentssystem.entity.AuditAction;
import com.sap.documentssystem.entity.AuditLog;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.AuditLogRepository;
import com.sap.documentssystem.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void shouldSaveAuditLogSuccessfully() {
        // given
        User user = new User();
        AuditAction action = AuditAction.CREATE_DOCUMENT;
        String entityType = "Document";
        UUID entityId = UUID.randomUUID();

        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        // when
        auditLogService.log(user, action, entityType, entityId, details);

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();

        assertThat(savedLog.getUser()).isEqualTo(user);
        assertThat(savedLog.getAction()).isEqualTo(action);
        assertThat(savedLog.getEntityType()).isEqualTo(entityType);
        assertThat(savedLog.getEntityId()).isEqualTo(entityId);
        assertThat(savedLog.getDetails()).isEqualTo(details);
    }

}