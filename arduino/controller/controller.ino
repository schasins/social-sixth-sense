/* 
  controller.cpp

  Used to communicate between the Arduino and the Android.
  More notes go here.
 */

#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE_UART.h"

/* Constants */
#define POT_MIN 155
#define POT_MID 500
#define POT_MAX 845
#define THERM_MIN 0
#define THERM_HI 225
#define THERM_MAX 255

#define TEMP_POLL_INTERVAL 5000

/* Pin setup */
const int enablePin = 6; //changed from 11
const int in1Pin = 5; //changed from 10
const int in2Pin = 4; //changed from 9
const int switchPin = 7;
const int potPin = 0;
const int vibPin = 3;

/* Bluetooth Stuff */

// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9
Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ,
                              ADAFRUITBLE_RDY, ADAFRUITBLE_RST);
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;
aci_evt_opcode_t status = ACI_EVT_DISCONNECTED;

/* Function prototypes */
// void setDirectionHot(bool);
// void actuateMotor(void);

/* Variables */
int val = THERM_MIN;
int knob;
unsigned long lastTempCheck = millis();

void setup()
{
  pinMode(in1Pin, OUTPUT);
  pinMode(in2Pin, OUTPUT);
  pinMode(enablePin, OUTPUT);
  pinMode(switchPin, INPUT_PULLUP);
  pinMode(vibPin, OUTPUT);

  Serial.begin(9600);

  /* Wait for serial before initializing bluetooth */
  while(!Serial);
  BTLEserial.begin();
}
 
void loop()
{
  pollBluetoothStatus();
  if (status == ACI_EVT_CONNECTED) {
    tryReadValue();
    // tryWriteValue();
  }

  knob = analogRead(potPin);
  // Serial.println(knob);

  actuateMotor();
  
  if (millis() - lastTempCheck >= TEMP_POLL_INTERVAL) {
    pollKnob();
    tryWriteValue(val);
    lastTempCheck = millis();
  }
  
}

void pollBluetoothStatus(void) {
  // Tell the nRF8001 to do whatever it should be working on.
  BTLEserial.pollACI();

  // Ask what is our current status
  status = BTLEserial.getState();
  // If the status changed....
  if (status != laststatus) {
    // print it out!
    if (status == ACI_EVT_DEVICE_STARTED) {
        Serial.println(F("* Advertising started"));
    }
    if (status == ACI_EVT_CONNECTED) {
        Serial.println(F("* Connected!"));
    }
    if (status == ACI_EVT_DISCONNECTED) {
        Serial.println(F("* Disconnected or advertising timed out"));
    }
    // OK set the last status change to this one
    laststatus = status;
  }  
}

void tryReadValue(void) {
  if (status != ACI_EVT_CONNECTED)
    return;
  if (BTLEserial.available()) {
    Serial.print("* "); Serial.print(BTLEserial.available()); Serial.println(F(" bytes available from BTLE"));
  }
  // OK while we still have something to read, get a character and print it out
  while (BTLEserial.available()) {
    char c = BTLEserial.read();
    Serial.print(c);
  } 
}

void tryWriteValue(int val) {
  if (status != ACI_EVT_CONNECTED)
    return;

  Serial.setTimeout(100);
  String *_s = new String(val);
  String s = *_s;

  // We need to convert the line to bytes, no more than 20 at this time
  uint8_t sendbuffer[20];
  s.getBytes(sendbuffer, 20);
  char sendbuffersize = min(20, s.length());

  Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
  
  // write the data
  BTLEserial.write(sendbuffer, sendbuffersize);

  // if (Serial.available()) {
  //   // Read a line from Serial
  //   Serial.setTimeout(100); // 100 millisecond timeout
  //   String s = Serial.readString();

  //   // We need to convert the line to bytes, no more than 20 at this time
  //   uint8_t sendbuffer[20];
  //   s.getBytes(sendbuffer, 20);
  //   char sendbuffersize = min(20, s.length());

  //   Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");

  //   // write the data
  //   BTLEserial.write(sendbuffer, sendbuffersize);
  // } 
}

void pollKnob(void) {
  /* Hot scenario */
  if (knob > POT_MID) {
    val = map(knob, POT_MID, POT_MAX, THERM_MIN, THERM_HI);
    analogWrite(enablePin, val);
    setDirectionHot(true);
    Serial.print("Writing to thermo: ");
    Serial.println(val);
    delay(5000);
    analogWrite(enablePin, LOW);
  }
  
  /* Cold scenario */
  if (knob <= POT_MID){
    val = map(knob, POT_MIN, POT_MID, THERM_MAX, THERM_MIN);
    analogWrite(enablePin, val);
    setDirectionHot(false);
    Serial.print("Writing to thermo: ");
    Serial.println(val);
    delay(5000);
    analogWrite(enablePin, LOW);
  }
}

/* Sets the current flow for the thermoelectric. If IS_HOT is true,
   sets the wires such that the actuation is hot. If IS_HOT is false,
   sets the actuation to be cold. */
void setDirectionHot(bool isHot) {
  if (isHot) {
    digitalWrite(in1Pin, 1);
    digitalWrite(in2Pin, 0);
  } else {
    digitalWrite(in1Pin, 0);
    digitalWrite(in2Pin, 1);
  }
}

/* Rapidly switches the motor on and off. */
void actuateMotor(void) {
  digitalWrite(vibPin, HIGH);
  delay(200);
  digitalWrite(vibPin, LOW);
  delay(100);
  digitalWrite(vibPin, HIGH);
  delay(200);
  digitalWrite(vibPin, LOW);
}