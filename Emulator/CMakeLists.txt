cmake_minimum_required(VERSION 3.10)
project(ConsoleEmulator CXX)

set(CMAKE_CXX_STANDARD 17)

# Tell CMake where to find PocoConfig.cmake
list(APPEND CMAKE_PREFIX_PATH "C:/poco/cmake-build/Poco")

# Now find Poco components
find_package(Poco REQUIRED COMPONENTS Foundation Net Util JSON)

add_executable(ConsoleEmulator
        main.cpp
        defs.hpp

        cpu/cpu.hpp
        cpu/cpu.cpp
        cpu/cpu_utils.cpp
        cpu/cpu_ops.cpp
        cpu/cpu_loads.cpp
        cpu/interrupts.cpp
        z80.hpp

        ppu/ppu.hpp
        ppu/ppu.cpp
        ppu/bg_ppu.cpp
        ppu/oam_ppu.cpp
        ppu/LCD/LCD.hpp
        ppu/LCD/LCD.cpp
        ppu/FIFO/FIFO.hpp
        ppu/FIFO/FIFO.cpp
        ppu/utils/utils.hpp
        ppu/utils/utils.cpp

        timers/timers.hpp
        timers/timers.cpp

        memory/memory.hpp
        memory/memory.cpp

        Emulator.hpp
        api/RequestHandlerFactory.hpp
        api/DefaultHandler.hpp
        api/CPUHandler.hpp
        api/EmulatorHandler.hpp
        api/MemoryHandler.hpp
)

# Link Poco targets
target_link_libraries(ConsoleEmulator PRIVATE
        Poco::Foundation
        Poco::Net
        Poco::Util
        Poco::JSON
)