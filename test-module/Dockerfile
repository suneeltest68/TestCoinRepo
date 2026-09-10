# --- Stage 1: Build the Application ---
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app
# Copy the entire project to the builder stage
COPY . .
# Run gradle build for the test-module
RUN ./gradlew :test-module:clean :test-module:jar

# --- Stage 2: Production Environment ---
FROM openjdk:17-jdk-slim

# Install Chrome and dependencies for Selenium
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    unzip \
    curl \
    libnss3 \
    libxss1 \
    libasound2 \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Copy the generated JAR from the builder stage
COPY --from=builder /app/test-module/build/libs/test-module-fat.jar app.jar

# Expose the port for the dummy web server
EXPOSE 8080

# Run the bot
ENTRYPOINT ["java", "-jar", "app.jar"]
