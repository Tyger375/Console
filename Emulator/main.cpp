#include <iostream>
#include <string>
#include <thread>
#include <Poco/Net/HTTPServer.h>
#include "api/RequestHandlerFactory.hpp"
#include "memory/memory.hpp"
#include "cpu/cpu.hpp"
#include "timers/timers.hpp"
#include "ppu/ppu.hpp"

int main() {
    Emulator emulator;

    std::thread serverThread([&]() {
        try {
            Poco::Net::ServerSocket svs(8080); // bind on all interfaces on your PC!
            Poco::Net::HTTPServer server(
                new RequestHandlerFactory(emulator),
                svs,
                new Poco::Net::HTTPServerParams()
            );
            server.start();
            std::cout << "Server started" << std::endl;

            while (true) {}
        } catch (const Poco::Exception& ex) {
            std::cerr << "POCO exception: " << ex.displayText() << "\n";
        }
    });

    u32 cycles = 0;
    bool running = true;
    while (running) {
        /*
        SDL_Event Event;
        while(SDL_PollEvent(&Event)) {
            if (joypad.keyboard_event(Event)) continue;

            if (Event.type == SDL_EVENT_WINDOW_CLOSE_REQUESTED) {
                running = false;
                break;
            }
        }

        joypad.update(mem);
        */
        u32 c = emulator.step();
    }

    return 0;
}
