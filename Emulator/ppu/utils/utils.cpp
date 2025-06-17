#include "utils.hpp"

Byte VideoUtils::palette_to_color(Pixel pixel)  {
    switch (pixel.color) {
        case 0: {
            return pixel.palette & 0b11;
        }
        case 1: {
            return (pixel.palette >> 2) & 0b11;
        }
        case 2: {
            return (pixel.palette >> 4) & 0b11;
        }
        case 3: {
            return (pixel.palette >> 6) & 0b11;
        }
    }
    return 0;
}

VideoUtils::OAM::Sprite VideoUtils::OAM::fetch_sprite(Word index, Memory &mem)  {
    Byte id = (index - 0xFE00) / 4;
    return {
            id,
            mem[index],
            mem[index+1],
            mem[index+2],
            mem[index+3]
    };
}