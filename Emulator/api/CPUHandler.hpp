#pragma once

#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/JSON/Object.h>
#include <iostream>

class CPUHandler : public Poco::Net::HTTPRequestHandler {
public:
    CPUHandler(Emulator& emulator) : emulator(emulator) {}

    void handleRequest(Poco::Net::HTTPServerRequest& request, Poco::Net::HTTPServerResponse& response) override {

        std::string uri = request.getURI();
        response.setContentType("application/json");
        std::ostream& out = response.send();

        if (uri == "/cpu/registers") {
            const auto cpu = emulator.cpu;
            const auto reg = cpu.reg;
            const auto pairs = reg.pair;
            Poco::JSON::Object::Ptr res = new Poco::JSON::Object;
            res->set("A", pairs.A);
            res->set("F", pairs.F);
            res->set("B", pairs.B);
            res->set("C", pairs.C);
            res->set("D", pairs.D);
            res->set("E", pairs.E);
            res->set("H", pairs.H);
            res->set("L", pairs.L);
            res->set("SP", reg.SP);
            res->set("PC", reg.PC);
            res->stringify(out);
        }
    }
private:
    Emulator& emulator;
};
