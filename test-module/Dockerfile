# --- Stage 1: Build the Application ---
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
# Copy the entire project
COPY . .
# Build the fat jar
RUN ./gradlew :test-module:clean :test-module:jar

# --- Stage 2: Production Environment ---
FROM eclipse-temurin:17-jre

# Install dependencies and Google Chrome using the modern keyring method
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    --no-install-recommends \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmor > /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y \
    google-chrome-stable \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/test-module/build/libs/test-module-fat.jar app.jar

# Expose port for the dummy web server
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
