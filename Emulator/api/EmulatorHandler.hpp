#pragma once

#include <Poco/Net/HTTPServer.h>
#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPServerParams.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/Net/HTTPServerRequestImpl.h>
#include <Poco/Net/ServerSocket.h>
#include <Poco/Net/PartHandler.h>
#include <Poco/Net/MessageHeader.h>
#include <Poco/Net/HTMLForm.h>
#include <Poco/FileStream.h>
#include <Poco/StreamCopier.h>
#include <Poco/Path.h>
#include <Poco/Exception.h>
#include <Poco/JSON/Object.h>
#include <iostream>

class FilePartHandler : public Poco::Net::PartHandler {
public:
    std::string uploadedFile;

    void handlePart(const Poco::Net::MessageHeader& header, std::istream& stream) override {
        if (header.has("Content-Disposition")) {
            std::string disp;
            Poco::Net::NameValueCollection params;
            Poco::Net::MessageHeader::splitParameters(header["Content-Disposition"], disp, params);

            const auto time = std::chrono::system_clock::now().time_since_epoch().count();
            std::string temp_name = std::to_string(time);
            uploadedFile = "./temp/" + temp_name + ".asm";

            Poco::FileOutputStream out(uploadedFile);
            Poco::StreamCopier::copyStream(stream, out);
            out.close();
            std::cout << "Received file: " << temp_name << std::endl;
        }
    }
};

class EmulatorHandler : public Poco::Net::HTTPRequestHandler {
public:
    EmulatorHandler(Emulator& emulator) : emulator(emulator) {}

    void handleRequest(Poco::Net::HTTPServerRequest& request, Poco::Net::HTTPServerResponse& response) override {
        std::string uri = request.getURI();
        response.setContentType("text/plain");
        std::ostream& out = response.send();

        if (uri == "/emulator/step") {
            emulator.request_step = true;
            out << "OK";
        } else if (uri == "/emulator/load_program") {
            if (request.getMethod() != Poco::Net::HTTPRequest::HTTP_POST) {
                out << "Bad request";
                return;
            }
            std::istream& in = request.stream();
            std::vector<Byte> buffer((std::istreambuf_iterator(in)), std::istreambuf_iterator<char>());

            emulator.mem.clear_rom();
            emulator.mem.load_rom(buffer);
            out << "OK";
        } else if (uri == "/emulator/assemble_and_load") {
            if (request.getMethod() != Poco::Net::HTTPRequest::HTTP_POST) {
                out << "Bad request";
                return;
            }

            try {
                FilePartHandler partHandler;
                Poco::Net::HTMLForm form(request, request.stream(), partHandler);

                std::string asmFile = partHandler.uploadedFile;
                std::string prefix = asmFile.substr(0, asmFile.find_last_of('.'));
                std::string outputFile = prefix + ".bin";
                std::string logFile = prefix + ".lst";

                std::string command = ".\\zasm.exe -o " + outputFile + " " + asmFile;
                int result = std::system(command.c_str());

                Poco::FileInputStream logInput(logFile);
                Poco::StreamCopier::copyStream(logInput, out);
                logInput.close();

                if (result != 0) {
                    response.setStatus(Poco::Net::HTTPResponse::HTTP_INTERNAL_SERVER_ERROR);
                    std::cout << "zasm failed to assemble the file";

                    // Delete temp files
                    std::remove(asmFile.c_str());
                    std::remove(logFile.c_str());
                } else {
                    // Read assembled binary
                    std::ifstream binIn(outputFile, std::ios::binary);
                    std::vector<Byte> assembledCode((std::istreambuf_iterator<char>(binIn)), std::istreambuf_iterator<char>());
                    binIn.close();

                    emulator.mem.clear_rom();
                    emulator.mem.load_rom(assembledCode);

                    // Delete temp files
                    std::remove(asmFile.c_str());
                    std::remove(outputFile.c_str());
                    std::remove(logFile.c_str());

                    response.setStatus(Poco::Net::HTTPResponse::HTTP_OK);
                }
            } catch (const std::exception& e) {
                response.setStatus(Poco::Net::HTTPResponse::HTTP_INTERNAL_SERVER_ERROR);
                out << "Error: " << e.what();
            }
        }
    }
private:
    Emulator& emulator;
};
