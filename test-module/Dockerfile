# --- Stage 1: Build the Application ---
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
# Copy the entire project
COPY . .
# Build the fat jar
RUN ./gradlew :test-module:clean :test-module:jar

# --- Stage 2: Production Environment ---
FROM eclipse-temurin:17-jre

# Install dependencies and Google Chrome
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg \
    ca-certificates \
    libnss3 \
    libgconf-2-4 \
    libxss1 \
    libasound2 \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    --no-install-recommends \
    && wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y ./google-chrome-stable_current_amd64.deb \
    && rm google-chrome-stable_current_amd64.deb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/test-module/build/libs/test-module-fat.jar app.jar

# Expose port for the dummy web server
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
