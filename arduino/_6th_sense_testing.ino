
int enablePin = 11;
int in1Pin = 10;
int in2Pin = 9;
int switchPin = 7;
int potPin = 0;
int val;
void setup()
{
  pinMode(in1Pin, OUTPUT);
  pinMode(in2Pin, OUTPUT);
  pinMode(enablePin, OUTPUT);
  pinMode(switchPin, INPUT_PULLUP);
  Serial.begin(9600);
}
 
void loop()
{
  int speed = analogRead(potPin);
  Serial.println(speed);
  
    if (speed> 512) {
    val = map(speed, 512, 1024, 0, 255);
    //setMotor(speed, reverse);
    analogWrite(enablePin, val);
    digitalWrite(in1Pin, 1);
    digitalWrite(in2Pin, 0);
    Serial.println(val);
    }
    
    if (speed<= 512){
    val = map(speed, 0, 512, 255, 0);
    //setMotor(speed, reverse);
    analogWrite(enablePin, val);
    digitalWrite(in1Pin, 0);
    digitalWrite(in2Pin, 1);
    Serial.println(val);
  }
  delay(500);
  
}
 
