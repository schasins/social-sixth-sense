/* Copyright 2012 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

// Sample Arduino sketch for use with usb-serial-for-android.
// Prints an ever-increasing counter, and writes back anything
// it receives.

#include <Arduino.h>
#include <String.h>

const int potPin = A0;

int potVal = 0;

void setup() {
  Serial.begin(115200);
  pinMode(potPin, INPUT);
}

void loop() {
  // Serial.print(DEC, "-0.5");
  // delay(5000);
  potVal = analogRead(potPin);
  Serial.println(String(potVal));
  delay(250);
}

float potToApproach(int potVal) {
  return 0.0;
}

int approachToPot(float approachVal) {
  return 0;
}
