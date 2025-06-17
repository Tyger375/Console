#pragma once

#include "../Emulator.hpp"
#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>

#include "CPUHandler.hpp"
#include "EmulatorHandler.hpp"
#include "DefaultHandler.hpp"
#include "MemoryHandler.hpp"

class RequestHandlerFactory : public Poco::Net::HTTPRequestHandlerFactory {
public:
    RequestHandlerFactory(Emulator& emulator) : emulator(emulator) {}

    Poco::Net::HTTPRequestHandler* createRequestHandler(const Poco::Net::HTTPServerRequest& request) override {
        std::string uri = request.getURI();
        request.response().set("Access-Control-Allow-Origin", "*");

        if (uri.find("/cpu") == 0)
            return new CPUHandler(emulator);
        if (uri.find("/emulator") == 0)
            return new EmulatorHandler(emulator);
        if (uri.find("/memory") == 0)
            return new MemoryHandler(emulator);

        std::cout << uri << std::endl;
        return new DefaultHandler();
    }

private:
    Emulator& emulator;
};
