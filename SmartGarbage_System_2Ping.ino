#include "ThingsBoard.h"
#include <WiFi.h>
#include "ThingSpeak.h"
#define WIFI_AP             "Mr. 777"
#define WIFI_PASSWORD       "forzamilan"

// See https://thingsboard.io/docs/getting-started-guides/helloworld/
// to understand how to obtain an access token
#define TOKEN               "CD9dzTelExfDX5hUW77Q"
#define THINGSBOARD_SERVER  "demo.thingsboard.io"

// Initialize ThingsBoard client
WiFiClient espClient;
// Initialize ThingsBoard instance
ThingsBoard tb(espClient);
// the Wifi radio's status
int status = WL_IDLE_STATUS;

//THINGSPEAK
unsigned long myChannelNumber = 848659;
const char * myWriteAPIKey = "G3ZNUESX67NPMZ0N";

//PIN SENSOR
#define trigPin1 25    // Trigger
#define echoPin1 26    // Echo
#define trigPin2 32    // Trigger
#define echoPin2 33    // Echo
float duration1, cm1, duration2, cm2, capacity;
float trashHeight = 31;

//Variabel System
char* nameTS = "A";
char mergedData[5];
int processCount = 0;

void setup() {
  //Serial Port begin
  Serial.begin (115200);
  //Define inputs and outputs
  pinMode(trigPin1, OUTPUT);
  pinMode(echoPin1, INPUT);
  pinMode(trigPin2, OUTPUT);
  pinMode(echoPin2, INPUT);

  WiFi.begin(WIFI_AP, WIFI_PASSWORD);
  InitWiFi();

  ThingSpeak.begin(espClient);  // Initialize ThingSpeak
}

void loop() {
  readCapacity();

  delay(1000);

  connectToThingsboard();

  connectToThingspeak();

  processCount++;

}

void readCapacity() {
  //SENSOR 1
  // The sensor is triggered by a HIGH pulse of 10 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  digitalWrite(trigPin1, LOW);
  delayMicroseconds(5);
  digitalWrite(trigPin1, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin1, LOW);

  // Read the signal from the sensor: a HIGH pulse whose
  // duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(echoPin1, INPUT);
  duration1 = pulseIn(echoPin1, HIGH);

  // Convert the time into a distance
  cm1 = (duration1 / 2) / 29.1;   // Divide by 29.1 or multiply by 0.0343
  if (cm1 > trashHeight) {
    cm1 = trashHeight;
  }

  //SENSOR 2
  // The sensor is triggered by a HIGH pulse of 10 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  digitalWrite(trigPin2, LOW);
  delayMicroseconds(5);
  digitalWrite(trigPin2, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin2, LOW);

  // Read the signal from the sensor: a HIGH pulse whose
  // duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(echoPin2, INPUT);
  duration2 = pulseIn(echoPin2, HIGH);

  // Convert the time into a distance
  cm2 = (duration2 / 2) / 29.1;   // Divide by 29.1 or multiply by 0.0343
  if (cm2 > trashHeight) {
    cm2 = trashHeight;
  }

  //Count trash capacity
  capacity = 100 - (( ((cm1 / trashHeight) * 100) + ((cm2 / trashHeight) * 100) ) / 2);

  Serial.println("===============================");
  Serial.print("Tinggi sampah dari sensor1: ");
  Serial.print(cm1);
  Serial.println("cm");
  Serial.print("Tinggi sampah dari sensor2: ");
  Serial.print(cm2);
  Serial.println("cm");
  Serial.print("Kapasitas sampah: ");
  Serial.print(capacity);
  Serial.println("%");
}

void connectToThingsboard() {
  if (WiFi.status() != WL_CONNECTED) {
    reconnect();
  }

  if (!tb.connected()) {
    // Connect to the ThingsBoard
    Serial.print("Connecting to: ");
    Serial.print(THINGSBOARD_SERVER);
    Serial.print(" with token ");
    Serial.println(TOKEN);
    if (!tb.connect(THINGSBOARD_SERVER, TOKEN)) {
      Serial.println("Failed to connect");
      return;
    }
  }

  Serial.println("Sending data to Thingsboard...");
  tb.sendTelemetryFloat("distance1", cm1);
  tb.sendTelemetryFloat("distance2", cm2);
  tb.sendTelemetryFloat("capacity", capacity);
  tb.loop();
}

void connectToThingspeak() {
  if (WiFi.status() != WL_CONNECTED) {
    reconnect();
  }

  char capacity_buf[5];
  dtostrf(capacity, 0, 1, capacity_buf);

  strcpy (mergedData, nameTS);
  strcat (mergedData, ":");
  strcat (mergedData, capacity_buf);

  if (processCount >= 16) {
    Serial.println("Sending data to Thingspeak...");
    ThingSpeak.writeField(myChannelNumber, 1, mergedData, myWriteAPIKey);
    processCount = 0;
  }
  
}

void InitWiFi() {
  Serial.println("Connecting to AP ...");
  // attempt to connect to WiFi network

  WiFi.begin(WIFI_AP, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Connected to AP");
}

void reconnect() {
  // Loop until we're reconnected
  status = WiFi.status();
  if ( status != WL_CONNECTED) {
    WiFi.begin(WIFI_AP, WIFI_PASSWORD);
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
    }
    Serial.println("Connected to AP");
  }
}
