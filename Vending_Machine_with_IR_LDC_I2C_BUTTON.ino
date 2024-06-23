#if defined(ESP32)
#include <WiFi.h>
#elif defined(ESP8266)
#include <ESP8266WiFi.h>
#endif

#include <LiquidCrystal_I2C.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>

LiquidCrystal_I2C lcd(0x27, 16, 2);

#define infraredPin 4
#define buttonPin 14

#define WIFI_SSID "TEKNOKRATAIN"
#define WIFI_PASSWORD "12345678"

#define API_KEY "AIzaSyC1lsufR4HTgOBWLdILFFuoKPUV0jzTrlE"
#define FIREBASE_PROJECT_ID "trashscan-teknokratain"
#define FIREBASE_URL "https://firestore.googleapis.com/v1/projects/" FIREBASE_PROJECT_ID "/databases/(default)/documents/token/KFI6gzd9YCzWGH9z6hUW/"

// Sensor and button states
int previousState = 1;
int currentState = 1;
int sumTrash = 0;
String token = "";

unsigned long sendDataPrevMillis = 0;
unsigned long wifiConnectStartMillis = 0;
const unsigned long wifiTimeoutMillis = 30000;

bool counting = false;
unsigned long lasTrashDetectedMillis = 0;
const unsigned long bottleTimeoutMillis = 5000;

bool buttonState = false;
bool lastButtonState = false;

const String welcomeMessage = "Tekan Tombol Sebelum Memasukkan Sampah Anda!";
int scrollIndex = 0;

// Countdown variables
unsigned long countdownStartMillis = 0;
const unsigned long tokenDisplayDuration = 15000;
int countdownTimeLeft = 0;

void setup() {
  Serial.begin(115200);

  pinMode(infraredPin, INPUT);
  pinMode(buttonPin, INPUT);

  // Welcoming Text
  lcd.init();
  lcd.backlight();
  showScrollingMessage();

  // WiFi Configuration
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  wifiConnectStartMillis = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - wifiConnectStartMillis < wifiTimeoutMillis) {
    Serial.print(".");
    delay(300);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());
    Serial.println();
  } else {
    Serial.println();
    Serial.println("Failed to connect to WiFi. Check credentials or WiFi network.");
  }
}

String generatetoken() {
  String token = "";
  char letters[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  char numbers[] = "0123456789";

  for (int i = 0; i < 4; i++) {
    token += letters[random(26)];
  }

  for (int i = 0; i < 4; i++) {
    token += numbers[random(10)];
  }

  return token;
}

void showScrollingMessage() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Hy TrashScanners");

  String displayMessage = welcomeMessage.substring(scrollIndex, scrollIndex + 16);
  lcd.setCursor(0, 1);
  lcd.print(displayMessage);

  scrollIndex++;
  if (scrollIndex > welcomeMessage.length() - 16) {
    scrollIndex = 0;
  }
}

void showCountingMessage() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Counting...");
  lcd.setCursor(0, 1);
  lcd.print("Obstacles: ");
  lcd.print(sumTrash);
}

void showTokenMessage() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Total: ");
  lcd.print(sumTrash);
  lcd.setCursor(0, 1);
  lcd.print("Token: ");
  lcd.print(token);
  showCountdownMessage();
}

void showCountdownMessage() {
  lcd.setCursor(14, 0);
  lcd.print("  ");
  lcd.setCursor(14, 0);
  int seconds = countdownTimeLeft / 1000;
  lcd.print(seconds);
}

void loop() {
  buttonState = digitalRead(buttonPin);
  if (buttonState != lastButtonState) {
    if (buttonState == HIGH && !counting) {
      counting = true;
      sumTrash = 0;
      lasTrashDetectedMillis = millis();
      showCountingMessage();
    }
    lastButtonState = buttonState;
  }

  if (counting) {
    currentState = digitalRead(infraredPin);
    if (currentState != previousState) {
      if (currentState == LOW) {
        sumTrash++;
        showCountingMessage();
        lasTrashDetectedMillis = millis();
      }
      previousState = currentState;
    }

    // Check if the timeout period has elapsed since the last trash was detected
    if (millis() - lasTrashDetectedMillis >= bottleTimeoutMillis) {
      counting = false;
      token = generatetoken();
      countdownStartMillis = millis(); // Start the countdown timer
      countdownTimeLeft = tokenDisplayDuration;
      showTokenMessage();

      // Prepare JSON content to send to Firestore
      String jsonContent = String("{\"fields\": {") +
                          "\"trash\": {\"integerValue\": \"" + String(sumTrash) + "\"}," +
                          "\"token\": {\"stringValue\": \"" + token + "\"}," 
                          "}}";
      // Send data to Firestore
      if (sendToFirestore(jsonContent)) {
        Serial.print("Jumlah Sampah: ");
        Serial.println(sumTrash);
        Serial.print("Token: ");
        Serial.println(token);
        Serial.println("Data sent to Firestore successfully");
        Serial.print(" ");
      } else {
        Serial.print("Failed to save data");
      }
    }
  } else if (countdownTimeLeft > 0) {
    // Handle countdown timer display
    unsigned long currentMillis = millis();
    countdownTimeLeft = tokenDisplayDuration - (currentMillis - countdownStartMillis);
    showCountdownMessage(); // Update countdown on the LCD

    if (countdownTimeLeft <= 0) {
      // Countdown finished, go back to scrolling message
      scrollIndex = 0; // Reset scroll index
      showScrollingMessage();
    }
  } else {
    showScrollingMessage();
    delay(500);
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Attempting to reconnect...");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    wifiConnectStartMillis = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - wifiConnectStartMillis < wifiTimeoutMillis) {
      Serial.print(".");
      delay(300);
    }

    if (WiFi.status() == WL_CONNECTED) {
      Serial.println();
      Serial.print("Reconnected with IP: ");
      Serial.println(WiFi.localIP());
    } else {
      Serial.println();
      Serial.println("Failed to reconnect to WiFi.");
    }
  }
}

bool sendToFirestore(String jsonContent) {
  WiFiClientSecure client;
  HTTPClient http;

  client.setInsecure(); // Use with caution; for simplicity in development

  http.begin(client, FIREBASE_URL + String("?key=") + API_KEY);
  http.addHeader("Content-Type", "application/json");

  int httpResponseCode = http.PATCH(jsonContent);
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.println("HTTP Response code: " + String(httpResponseCode));
    Serial.println("Response: " + response);
    http.end();
    return true;
  } else {
    Serial.println("Error on sending PATCH: " + String(httpResponseCode));
    http.end();
    return false;
  }
}
