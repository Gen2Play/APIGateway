# Sử dụng OpenJDK làm base image
FROM openjdk:21-jdk-slim

# Đặt thư mục làm việc trong container
WORKDIR /app

# Copy toàn bộ code vào container
COPY . .

# Cấp quyền thực thi cho Maven Wrapper
RUN chmod +x ./mvnw

# Build ứng dụng bằng Maven
RUN ./mvnw clean package -DskipTests

# Chạy ứng dụng khi container khởi động
CMD ["java", "-jar", "target/APIGateway-3.4.2.jar"]
