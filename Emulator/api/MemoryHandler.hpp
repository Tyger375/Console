#pragma once

#include <iomanip>
#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/JSON/Object.h>
#include "../Emulator.hpp"

Word hex_to_word(
    const std::string& hexStr
) {
    try {
        unsigned long val = std::stoul(hexStr, nullptr, 16);
        if (val > 0xFFFF) {
            throw std::out_of_range("Hex value too large for uint16_t");
        }
        return static_cast<uint16_t>(val);
    } catch (const std::invalid_argument& e) {
        std::cerr << "Invalid hex string: " << hexStr << '\n';
    } catch (const std::out_of_range& e) {
        std::cerr << "Hex value out of range: " << hexStr << '\n';
    }
    return 0;  // or some sentinel/error value
}

std::string word_to_hex(Word value, bool uppercase = true) {
    std::stringstream ss;
    ss << std::setfill('0') << std::setw(4);  // pad to 4 digits
    if (uppercase)
        ss << std::uppercase;
    ss << std::hex << value;
    return ss.str();
}

class MemoryHandler : public Poco::Net::HTTPRequestHandler {
public:
    MemoryHandler(Emulator& emulator) : emulator(emulator) {}

    void handleRequest(Poco::Net::HTTPServerRequest& request, Poco::Net::HTTPServerResponse& response) override {
        std::string uri = request.getURI();
        response.setContentType("application/json");
        std::ostream& out = response.send();
        const auto method = request.getMethod();

        if (uri.find("/memory/address/") == 0) {
            Word addr = hex_to_word(uri.substr(16));

            std::cout << addr << std::endl;

            if (method == Poco::Net::HTTPRequest::HTTP_POST) {
                out << "Not implemented";
            } else if (method == Poco::Net::HTTPRequest::HTTP_GET) {
                out << word_to_hex(emulator.mem[addr]);
            } else {
                out << "Invalid method";
            }
        } else if (uri == "/memory/dump") {
            emulator.mem.dump_stream(out);
        }
    }
private:
    Emulator& emulator;
};
