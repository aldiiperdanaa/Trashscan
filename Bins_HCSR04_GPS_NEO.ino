#include <ESP8266WiFi.h>
#include <Firebase_ESP_Client.h>
#include <TinyGPS++.h>
#include <SoftwareSerial.h>
#include <WiFiClientSecure.h>
#include <ESP8266HTTPClient.h>

// GPS configuration
SoftwareSerial GPS_SoftSerial(4, 3);  // Rx, Tx for GPS
TinyGPSPlus gps;

// WiFi configuration
#define WIFI_SSID "TEKNOKRATAIN"
#define WIFI_PASSWORD "12345678"

// Firestore configuration
#define API_KEY "AIzaSyC1lsufR4HTgOBWLdILFFuoKPUV0jzTrlE"
#define FIREBASE_PROJECT_ID "trashscan-teknokratain"
#define FIREBASE_URL "https://firestore.googleapis.com/v1/projects/" FIREBASE_PROJECT_ID "/databases/(default)/documents/point/CerMQFEWMIQMoNbNe8cN/"

// Ultrasonic sensor pins
#define triggerPin 13
#define echoPin 12

double lat, lng;
long duration;

const float maxHeight = 77.6; // Maximum height of the bin in cm
const unsigned long wifiTimeoutMillis = 20000; // WiFi connection timeout in milliseconds

// Static variables for storing the last valid GPS location
static double lastValidLat = -5.149138;
static double lastValidLng = 119.392622;

String name = "Titik Sampah Trashscan";
String address = "Kawasan CitraLand City CPI, Jalan Sunset Boulevard, Kota Makassar, Sulawesi Selatan 90224";

void setup() {
  Serial.begin(9600);
  GPS_SoftSerial.begin(9600);
  pinMode(triggerPin, OUTPUT);
  pinMode(echoPin, INPUT);
  connectToWiFi();
}

void loop() {
  smartDelay(10000);

  // Read GPS data
  if (gps.location.isValid()) {
    lat = gps.location.lat();
    lng = gps.location.lng();
    lastValidLat = lat;
    lastValidLng = lng;
  } else {
    lat = lastValidLat;
    lng = lastValidLng;
  }

  // Read HC-SR04 sensor
  float height = measureDistance();
  float percentageFull = calculatePercentageFull(height);

  // Prepare JSON content to send to Firestore
  String jsonContent = String("{\"fields\": {") +
                      "\"latitude\": {\"doubleValue\": \"" + String(lat, 6) + "\"}," +
                      "\"longitude\": {\"doubleValue\": \"" + String(lng, 6) + "\"}," +
                      "\"status\": {\"doubleValue\": " + String(percentageFull) + "}," +
                      "\"name\": {\"stringValue\": \"" + name + "\"}," +
                      "\"address\": {\"stringValue\": \"" + address + "\"}" +
                      "}}";

  // Send data to Firestore
  if (sendToFirestore(jsonContent)) {
    Serial.print("Latitude       : ");
    Serial.println(lat, 6);
    Serial.print("Longitude      : ");
    Serial.println(lng, 6);
    Serial.print("Volume         : ");
    Serial.print(percentageFull);
    Serial.println(" %");
    Serial.println("Data sent to Firestore successfully");
    Serial.println();
  } else {
    Serial.println("Failed to send data to Firestore");
    Serial.println();
  }
}

void connectToWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  unsigned long wifiConnectStartMillis = millis();
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
    Serial.println();
  }
}

static void smartDelay(unsigned long ms) {
  unsigned long start = millis();
  do {
    while (GPS_SoftSerial.available())
      gps.encode(GPS_SoftSerial.read());
  } while (millis() - start < ms);
}

float measureDistance() {
  digitalWrite(triggerPin, LOW);
  delayMicroseconds(2);

  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(triggerPin, LOW);

  duration = pulseIn(echoPin, HIGH);
  return duration * 0.034 / 2; // Distance in cm
}

float calculatePercentageFull(float height) {
  float percentageFull = ((maxHeight - height) / maxHeight) * 100.0;
  if (percentageFull < 0) percentageFull = 0;
  if (percentageFull > 100) percentageFull = 100;
  return percentageFull;
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
    Serial.println("Error on sending POST: " + String(httpResponseCode));
    http.end();
    return false;
  }
}
