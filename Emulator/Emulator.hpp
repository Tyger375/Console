#pragma once

//#include "cpu/cpu.hpp"
#include "z80.hpp"
#include "memory/memory.hpp"

// memory read request per 1 byte from CPU
inline unsigned char readByte(void* arg, Word addr)
{
    // NOTE: implement switching procedure here if your MMU has bank switch feature
    return ((Memory*)arg)->operator[](addr);
}

// memory write request per 1 byte from CPU
inline void writeByte(void* arg, Word addr, Byte value)
{
    // NOTE: implement switching procedure here if your MMU has bank switch feature
    ((Memory*)arg)->operator[](addr) = value;
}

// IN operand request from CPU
inline unsigned char inPort(void* arg, unsigned short port) {
    // NO I/O PORTS
    return ' ';
}

// OUT operand request from CPU
inline void outPort(void* arg, unsigned short port, unsigned char value)
{
    // NO I/O PORTS
}

class Emulator {
public:
    bool stepping = true;
    bool request_step = false;

    Z80 cpu;
    Memory mem;

    Emulator() : cpu(readByte, writeByte, inPort, outPort, &mem) {
        mem.init();
    }

    u32 step() {
        if (stepping && !request_step) return 0;

        const u32 c = cpu.execute(1);
        cycles += c;
        request_step = false;
        return c;
    }

private:
    u32 cycles = 0;
};