################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/http_reply.cpp \
../src/http_request.cpp \
../src/http_server.cpp \
../src/http_session.cpp \
../src/main.cpp \
../src/mdns.cpp \
../src/mdns2.cpp \
../src/pair_setup.cpp \
../src/pair_verify.cpp \
../src/pairing_helper.cpp \
../src/session_crypto.cpp \
../src/tlv.cpp 

OBJS += \
./src/http_reply.o \
./src/http_request.o \
./src/http_server.o \
./src/http_session.o \
./src/main.o \
./src/mdns.o \
./src/mdns2.o \
./src/pair_setup.o \
./src/pair_verify.o \
./src/pairing_helper.o \
./src/session_crypto.o \
./src/tlv.o 

CPP_DEPS += \
./src/http_reply.d \
./src/http_request.d \
./src/http_server.d \
./src/http_session.d \
./src/main.d \
./src/mdns.d \
./src/mdns2.d \
./src/pair_setup.d \
./src/pair_verify.d \
./src/pairing_helper.d \
./src/session_crypto.d \
./src/tlv.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cygwin C++ Compiler'
	g++ -I"C:\TMP\git\HAP\cpp\hap_cpp\wolfcrypt" -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


