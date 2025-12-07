# Volunteer Backend

Backend service cho ứng dụng Volunteer.

## 🔹 Hướng dẫn chạy backend

### 1. Clone source code

```bash
git clone <repository-url>
cd <project-folder>
```

### 2. Cấu hình kết nối Database & JWT

Mở file cấu hình application.yml và chỉnh các thông số sau:

```bash

spring:  
  datasource: 
    url:  
    username:  
    password: 

app:  
  jwt:  
    secret:   
```


### 3. Chạy ứng dụng

Mở IDE chạy hàm main trong file VolunteerApplication:

Ứng dụng sẽ khởi chạy trên port mặc định (8080).

### 4. Truy cập Swagger UI

Mở Swagger UI để kiểm tra và lấy thông tin API cho FE:

http://localhost:8080/swagger-ui.html