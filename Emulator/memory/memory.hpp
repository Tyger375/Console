#pragma once
#include "../defs.hpp"
#include <fstream>
#include <iostream>
#include <vector>

struct Memory {
private:
    static constexpr u32 MAX_MEM = 64 * 1024;
    Byte Data[MAX_MEM] = {};
public:
    void init();

    void clear_rom();
    void load_rom(std::vector<Byte> rom);

    Byte operator[](u32 addr) const;
    Byte& operator[](u32 addr);

    /**
     * Dumps all the memory in a file,
     * used for debugging purposes
     * */
    void dump(const char* filename);
    void dump_stream(std::ostream& out) const;
};


