import serial
import sys

TAB = "  "

def format_tab():
    print(TAB, end="")

def wait_for_progress(s: serial.Serial, length):
    toolbar_width = 40

    format_tab()
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

def hex_repr(string: str) -> tuple[int, bool]:
    try:
        return int(string, 16), True
    except:
        return 0, False
    
def hex_repr_list(strings: list[str]) -> tuple[list[int], bool]:
    new = []
    for s in strings:
        value, success = hex_repr(s)
        if not success:
            return [], False
        new.append(value)
    return new, True

def hex_str(value: int) -> str:
    return hex(value).removeprefix("0x").upper()

def recv_str_result(conn: serial.Serial) -> str:
    return conn.readline().strip().decode("ascii")

def recv_int_result(conn: serial.Serial) -> int:
    return int(recv_str_result(conn))

def _rom_read(conn: serial.Serial, addr: list[int]) -> int:
    command = bytearray([0x10, *addr])
    conn.write(command)

    return recv_int_result(conn)

def exec_read(conn: serial.Serial, operation: list[str]) -> int:
    if len(operation) < 2:
        format_tab()
        print("Invalid arguments")
        return -1

    values, result = hex_repr_list(operation[:2])

    if not result:
        format_tab()
        print("Read address is not correct")
        return -1

    res = _rom_read(conn, values)
    format_tab()
    print(hex_str(res))
    return 0

def exec_check(conn: serial.Serial, operation: list[str]) -> int:
    if len(operation) < 1:
        format_tab()
        print("Invalid arguments")
        return
    
    filename = operation[0]
    with open(filename, "rb") as f:
        data = f.read()
        length = len(data)
        if length > (8 * 1024):
            format_tab()
            print("File is too big")
            return
        
        for i in range(length):
            addr = [(i >> 8) & 0xFF, i & 0xFF]
            value = _rom_read(conn, addr)

            if value != data[i]:
                print(f"Different byte at {hex_str(i)}")
    
    print("Program is good")
    return 0

def exec_write(conn: serial.Serial, operation: list[str]) -> int:
    if len(operation) < 3:
        format_tab()
        print("Invalid arguments")
        return -1
    
    values, result = hex_repr_list(operation[:3])

    if not result:
        format_tab()
        print("Arguments are not correct")
        return

    command = bytearray([0x20, *values])
    conn.write(command)
    
    status_code = recv_int_result(conn)
    format_tab()
    print("OK" if status_code == 0 else "Uncaught exception")
    return status_code

def exec_load(conn: serial.Serial, operation: list[str]) -> int:
    if len(operation) < 1:
        format_tab()
        print("Invalid arguments")
        return
    
    filename = operation[0]
    with open(filename, "rb") as f:
        data = f.read()
        length = len(data)
        if length > (8 * 1024):
            format_tab()
            print("File is too big")
            return
        
        command = bytearray([0x30, ((length >> 8) & 0xFF), length & 0xFF])
        conn.write(command)

        max_chunk_size = 0x400

        chunks = (length // max_chunk_size) + 1

        for i in range(chunks):
            start = i * max_chunk_size
            end = min((i + 1) * max_chunk_size, length)
            conn.write(data[start:end])

            wait_for_progress(conn, end - start)

            format_tab()
            sys.stdout.write("\nsent, loading...\n")
            sys.stdout.flush()

            wait_for_progress(conn, end - start)

            format_tab()
            sys.stdout.write(f"\ndone {i+1}/{chunks}\n")
            sys.stdout.flush()

    return 0

def main():
    s = serial.Serial("COM3", baudrate=115200, bytesize=8, timeout=10, stopbits=serial.STOPBITS_ONE)
    #

    operations = {
        "read": exec_read,
        "write": exec_write,
        "load": exec_load,
        "check": exec_check,
        "exit": lambda _0, _1: exit(0)
    }

    while True:
        d = input(">>> ").strip()
        op = d.split(" ")

        op_code = op[0]

        if op_code in operations:
            operations[op_code](s, op[1:])
        else:
            print("Invalid operation")

if __name__ == "__main__":
    main()
