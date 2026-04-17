# File Upload 500 Error - FIXED

## Problem
When uploading files to create tickets, the attachment upload was failing with a **500 error**:
```
AxiosError: Request failed with status code 500
FileSizeLimitExceededException: The field file exceeds its maximum permitted size of 1048576 bytes (1MB)
```

The frontend was validating files up to **5MB**, but Tomcat's default multipart limit was only **1MB**.

## Root Cause
The Spring Boot backend was missing multipart file upload configuration:
- **Tomcat** was using default limit: **1MB** (1048576 bytes)
- **Spring Servlet** multipart settings were not configured
- Frontend allowed: **5MB** ❌ Mismatch

## Solution
Updated `backend/api/src/main/resources/application.yaml` with two configuration sections:

### 1. Spring Servlet Multipart Configuration
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

### 2. Tomcat Server Configuration
```yaml
server:
  tomcat:
    max-http-post-size: 5242880 # 5 MB in bytes (same as frontend limit)
```

## Changes Made
✅ Added Spring servlet multipart size limits (5MB for both file and request)
✅ Added Tomcat HTTP POST size limit (5MB = 5242880 bytes)
✅ Rebuilt backend container with updated configuration
✅ All services restarted and healthy

## Testing
The file upload should now work correctly for files up to **5MB**:
- JPG, PNG, GIF, WebP images ✅
- PDF documents ✅
- Maximum 3 attachments per ticket ✅
- Maximum 5MB per file ✅

## Configuration Alignment
| Setting | Value | Component |
|---------|-------|-----------|
| Frontend limit | 5MB | TicketDashboard.jsx |
| Spring max-file-size | 5MB | application.yaml |
| Spring max-request-size | 5MB | application.yaml |
| Tomcat max-http-post-size | 5MB (5242880 bytes) | application.yaml |
| Backend app limit | 5MB (5242880 bytes) | FileStorageConfig.java |

All components now have **consistent 5MB limits**!
