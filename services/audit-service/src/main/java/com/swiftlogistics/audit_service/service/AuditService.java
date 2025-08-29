import com.swiftlogistics.audit.entity.AuditLog;
import com.swiftlogistics.audit.enums.AuditAction;
import com.swiftlogistics.audit.enums.AuditEntity;
import com.swiftlogistics.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLog logEvent(AuditEntity entity, String entityId, AuditAction action,
                             String userId, String details, String ipAddress) {
        logger.debug("Logging audit event: entity={}, action={}, user={}", entity, action, userId);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntity(entity);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setUserId(userId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());

        return auditLogRepository.save(auditLog);
    }

    public void logOrderEvent(String orderNumber, AuditAction action, String userId, String details, String ipAddress) {
        logEvent(AuditEntity.ORDER, orderNumber, action, userId, details, ipAddress);
    }

    public void logDriverEvent(String driverId, AuditAction action, String userId, String details, String ipAddress) {
        logEvent(AuditEntity.DRIVER, driverId, action, userId, details, ipAddress);
    }

    public void logSystemEvent(AuditAction action, String userId, String details, String ipAddress) {
        logEvent(AuditEntity.SYSTEM, "SYSTEM", action, userId, details, ipAddress);
    }

    public Page<AuditLog> searchAuditLogs(AuditEntity entity, AuditAction action,
                                          String userId, LocalDateTime from, LocalDateTime to,
                                          Pageable pageable) {
        if (entity != null && action != null && userId != null) {
            return auditLogRepository.findByEntityAndActionAndUserIdAndTimestampBetween(
                    entity, action, userId, from, to, pageable);
        } else if (entity != null && userId != null) {
            return auditLogRepository.findByEntityAndUserIdAndTimestampBetween(
                    entity, userId, from, to, pageable);
        } else if (entity != null) {
            return auditLogRepository.findByEntityAndTimestampBetween(entity, from, to, pageable);
        } else {
            return auditLogRepository.findByTimestampBetween(from, to, pageable);
        }
    }

    public List<AuditLog> getEntityHistory(AuditEntity entity, String entityId) {
        return auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(entity, entityId);
    }
}