#include "memory.hpp"
#include <cmath>

void Memory::init() {
    for (int i = 0; i < MAX_MEM; ++i) {
        Data[i] = 0;
    }
}

void Memory::clear_rom() {
    for (int i = 0; i < 0x1FF; ++i) {
        Data[i] = 0;
    }
}

void Memory::load_rom(std::vector<Byte> rom) {
    const auto size = static_cast<int>(rom.size());
    for (int i = 0; i < std::min(size, 0x1FF); ++i) {
        Data[i] = rom[i];
    }
}

Byte Memory::operator[](u32 addr) const {
    return Data[addr];
}

Byte& Memory::operator[](u32 addr) {
    return Data[addr];
}

void Memory::dump(const char* filename) {
    std::ofstream file;
    file.open(filename, std::ios::out | std::ios::binary);

    if (file.is_open()) {
        for (size_t i = 0; i < MAX_MEM; ++i) {
            file << Data[i];
        }
    } else {
        std::cerr << "Failed to open file " << filename << std::endl;
    }
    file.close();
}

void Memory::dump_stream(std::ostream &out) const {
    for (size_t i = 0; i < MAX_MEM; ++i) {
        out << Data[i];
    }
}
