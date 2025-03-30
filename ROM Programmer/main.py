import serial
import sys

def wait_for_progress(s: serial.Serial, length):
    toolbar_width = 40

    sys.stdout.write("[%s]" % (" " * toolbar_width))
    sys.stdout.flush()
    sys.stdout.write("\b" * (toolbar_width+1))
    while True:
        p = s.readline().strip()
        if p.strip().decode("ascii").startswith("ok"):
            break
        
        progress = int(p)
        filled_length = int(toolbar_width * progress // length)
        bar = "=" * filled_length + " " * (toolbar_width - filled_length)
        
        sys.stdout.write(f"\r[{bar}] {progress}/{length}")
        sys.stdout.flush()

def main():
    s = serial.Serial("COM6", baudrate=115200, bytesize=8, timeout=10, stopbits=serial.STOPBITS_ONE)
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
            str = s.readline()
            v = str.strip()
            print(v)
            print(hex(int(v)).removeprefix("0x").upper())
        elif d == "write":
            if len(op) < 4:
                continue
            b = bytearray([0x20, int(op[1], 16), int(op[2], 16), int(op[3], 16)])
            s.write(b)
            str = s.readline()
            v = str.strip()
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
                b = bytearray([0x30, ((length >> 8) & 0xFF), length & 0xFF])

                s.write(b)

                max_chunk_size = 0x400

                print(s.readline())

                chunks = (length // max_chunk_size) + 1

                print(length, chunks)

                for i in range(chunks):
                    start = i * max_chunk_size
                    end = min((i + 1) * max_chunk_size, length)
                    s.write(data[start:end])

                    wait_for_progress(s, end - start)

                    sys.stdout.write("\nsent, loading...\n")
                    sys.stdout.flush()

                    wait_for_progress(s, end - start)

                    sys.stdout.write(f"\ndone {i+1}/{chunks}\n")
                    sys.stdout.flush()

if __name__ == "__main__":
    main()
