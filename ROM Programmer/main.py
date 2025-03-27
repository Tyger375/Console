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
            if len(op) < 3:
                continue
            first = int(op[1], 16)
            second = int(op[2], 16)
            b = bytearray([0x10, first, second])
            s.write(b)
        elif d == "write":
            if len(op) < 4:
                continue
            b = bytearray([0x20, int(op[1], 16), int(op[2], 16), int(op[3], 16)])
            s.write(b)
        elif d == "load":
            if len(op) < 2:
                continue
            filename = op[1]
            with open(filename, "rb") as f:
                data = f.read()
                length = len(data)
                if length > (8 * 1024):
                    print("file is too big")
                    continue
                b = bytearray([0x30, length & (0xFF00), length & 0xFF])
                s.write(b)
                s.write(data)

        str = s.readline()
        v = str.strip()
        print(v)
        print(hex(int(v)).removeprefix("0x").upper())
