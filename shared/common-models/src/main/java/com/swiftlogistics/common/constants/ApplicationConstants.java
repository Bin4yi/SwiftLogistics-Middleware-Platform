package com.swiftlogistics.common.constants;

public final class ApplicationConstants {

    private ApplicationConstants() {
        // Utility class
    }

    // Service Names
    public static final String ORDER_SERVICE = "order-service";
    public static final String INTEGRATION_SERVICE = "integration-service";
    public static final String TRACKING_SERVICE = "tracking-service";
    public static final String DRIVER_SERVICE = "driver-service";
    public static final String API_GATEWAY = "api-gateway";

    // Queue Names
    public static final String ORDER_PROCESSING_QUEUE = "order.processing.queue";
    public static final String ORDER_STATUS_QUEUE = "order.status.queue";
    public static final String TRACKING_UPDATES_QUEUE = "tracking.updates.queue";
    public static final String DRIVER_NOTIFICATIONS_QUEUE = "driver.notifications.queue";
    public static final String INTEGRATION_CMS_QUEUE = "integration.cms.queue";
    public static final String INTEGRATION_ROS_QUEUE = "integration.ros.queue";
    public static final String INTEGRATION_WMS_QUEUE = "integration.wms.queue";

    // Exchange Names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String TRACKING_EXCHANGE = "tracking.exchange";
    public static final String DRIVER_EXCHANGE = "driver.exchange";
    public static final String INTEGRATION_EXCHANGE = "integration.exchange";

    // Routing Keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_STATUS_UPDATED_KEY = "order.status.updated";
    public static final String ORDER_ASSIGNED_KEY = "order.assigned";
    public static final String TRACKING_UPDATE_KEY = "tracking.update";
    public static final String DRIVER_NOTIFICATION_KEY = "driver.notification";

    // Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // Cache Keys
    public static final String ORDER_CACHE_PREFIX = "order:";
    public static final String DRIVER_CACHE_PREFIX = "driver:";
    public static final String TRACKING_CACHE_PREFIX = "tracking:";
    public static final String SESSION_CACHE_PREFIX = "session:";

    // Configuration Properties
    public static final String JWT_SECRET_KEY = "jwt.secret";
    public static final String JWT_EXPIRATION = "jwt.expiration";
    public static final String RATE_LIMIT_REQUESTS = "rate.limit.requests";
    public static final String RATE_LIMIT_DURATION = "rate.limit.duration";

    // Business Rules
    public static final int MAX_DELIVERY_ATTEMPTS = 3;
    public static final int ORDER_EXPIRY_DAYS = 30;
    public static final int TRACKING_RETENTION_DAYS = 90;
    public static final int MAX_ORDERS_PER_DRIVER = 20;

    // File Upload
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String[] ALLOWED_FILE_TYPES = {"jpg", "jpeg", "png", "pdf"};

    // External System Timeouts (in milliseconds)
    public static final int CMS_TIMEOUT = 30000;
    public static final int ROS_TIMEOUT = 15000;
    public static final int WMS_TIMEOUT = 20000;
}