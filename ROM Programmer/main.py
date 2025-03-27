import serial

if __name__ == "__main__":
    s = serial.Serial("COM6", baudrate=9600, bytesize=8, timeout=10, stopbits=serial.STOPBITS_ONE)
    #

    while True:
        d = input().strip()
        op = d.split(" ")

        d = op[0]
        b = bytearray([0x00])
        if d == "read":
            if len(op) != 3:
                continue
            first = int(op[1], 16)
            second = int(op[2], 16)
            b = bytearray([0x10, first, second])
            s.write(b)
        elif d == "write":
            if len(op) != 4:
                continue
            b = bytearray([0x20, int(op[1], 16), int(op[2], 16), int(op[3], 16)])
            s.write(b)
        elif d == "load":
            b = bytearray([0x30, 0x00, 0x09])
            s.write(b)
            s.write(bytearray([0xAF, 0x21, 0x00, 0xF0, 0x3C, 0x77, 0x18, 0xFC, 0x00]))

        str = s.readline()
        v = str.strip()
        print(v)
        print(hex(int(v)).removeprefix("0x").upper())