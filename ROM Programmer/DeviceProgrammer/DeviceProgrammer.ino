const int WE = 52;
const int OE = 53;
const int CE = 50;

const int ADDRESS_START = 37;
const int ADDRESS_LENGTH = 13;

const int DATA_START = 22;
const int DATA_LENGTH = 8;

const int MAX_CHUNK_SIZE = 0x400;

typedef byte Byte;
typedef unsigned short Word;

void set_read_data_bus() {
  for (int i = 0; i < DATA_LENGTH; ++i) {
    pinMode(DATA_START + i, INPUT_PULLUP);
  }
}

void set_write_data_bus() {
  for (int i = 0; i < DATA_LENGTH; ++i) {
    pinMode(DATA_START + i, OUTPUT);
  }
}

bool executing = false;
bool io_mode = 0; // 0 -> read | 1 -> write

void setup() {
  pinMode(WE, OUTPUT);
  pinMode(OE, OUTPUT);
  pinMode(CE, OUTPUT);

  for (int i = 0; i < ADDRESS_LENGTH; ++i) {
    pinMode(ADDRESS_START + i, OUTPUT);
  }

  io_mode = 0;
  set_read_data_bus();
  read_mode();

  Serial.begin(115200);
}

void read_mode() {
  digitalWrite(OE, LOW);
  digitalWrite(CE, LOW);
  digitalWrite(WE, HIGH);
}

const int MICROSECONDS_WAIT = 1;

void write(Word addr, Byte data) {
  address_change(addr);
  write_data(data);

  digitalWrite(OE, HIGH);
  digitalWrite(WE, HIGH);
  digitalWrite(CE, LOW);

  delay(10);

  digitalWrite(WE, LOW);

  delay(10);

  digitalWrite(WE, HIGH);

  delay(10);

  digitalWrite(OE, LOW);
  digitalWrite(CE, LOW);
}

void address_change(Word address) {
  for (int i = 0; i < ADDRESS_LENGTH; ++i) {
    bool value = (address & (1 << i)) > 0;
    digitalWrite(ADDRESS_START + i, value ? HIGH : LOW);
  }
}

void write_data(Byte data) {
  for (int i = 0; i < DATA_LENGTH; ++i) {
    bool value = (data & (1 << i)) > 0;
    digitalWrite(DATA_START + i, value ? HIGH : LOW);
  }
}

Byte read_data() {
  Byte data = 0;
  for (int i = 0; i < DATA_LENGTH; ++i) {
    bool value = digitalRead(DATA_START + i);
    data |= (value << i);
  }
  return data;
}

String hex(Byte data) {
  auto string = String(data, HEX);
  string.toUpperCase();
  return string;
}

/**
 * 0x10 nn nn -> read from addr
 * 0x20 nn nn nn -> write to addr
 * 0x30 nn nn -> load program with size
 */

void loop() {
  if (executing) return;

  if (Serial.available() > 0) {
    Byte op = 0;
    // read the incoming byte:
    size_t size = Serial.readBytes(&op, 1);

    if (size > 0) {
      if (op == 0x10) {
        byte data[2];
        size = Serial.readBytes(data, 2);
        if (size != 2) {
          Serial.println("invalid");
          return;
        }
        if (io_mode == 1) {
          io_mode = 0;
          set_read_data_bus();
          read_mode();
          delay(10);
        }

        Word addr = (data[0] << 8) | data[1];

        address_change(addr);
        delay(10);
        Serial.println(read_data());
      }
      else if (op == 0x20) {
        byte data[3];
        size = Serial.readBytes(data, 3);
        if (size != 3) {
          Serial.println("invalid");
          return;
        }

        if (io_mode == 0) {
          io_mode = 1;
          set_write_data_bus();
          delay(10);
        }

        Word addr = (data[0] << 8) | data[1];

        write(addr, data[2]);
        delay(10);
        Serial.println(0);
      }
      else if (op == 0x30) {
        byte data[2];
        size = Serial.readBytes(data, 2);
        if (size != 2) {
          Serial.println("invalid");
          return;
        }

        Word length = (data[0] << 8) | data[1];
        
        byte* file = (byte*)malloc(MAX_CHUNK_SIZE);
        if (!file) {
          Serial.println("failed to malloc");
          return;
        }

        int chunks = (length / MAX_CHUNK_SIZE) + 1;

        if (io_mode == 0) {
          io_mode = 1;
          set_write_data_bus();
          delay(10);
        }

        for (int i = 0; i < chunks; ++i) {
          size_t num = (i == chunks - 1) ? (length % MAX_CHUNK_SIZE) : MAX_CHUNK_SIZE;
          size_t totalReads = 0;
          while (totalReads < num) {
            size_t read = Serial.readBytes(&file[totalReads], num - totalReads);
            if (read == 0) break;
            totalReads += read;
            Serial.println(totalReads);
          }
          delay(1000);

          Serial.println("ok");

          for (int j = 0; j < totalReads; ++j) {
            write((MAX_CHUNK_SIZE * i) + j, file[j]);
            Serial.println(j+1);
            delay(1);
          }

          delay(1000);

          Serial.println("ok");
        }

        free(file);
      }
    }
  }
}
