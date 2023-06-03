################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../wolfcrypt/chacha.c \
../wolfcrypt/chacha20_poly1305.c \
../wolfcrypt/curve25519.c \
../wolfcrypt/ed25519.c \
../wolfcrypt/fe_operations.c \
../wolfcrypt/ge_operations.c \
../wolfcrypt/hash.c \
../wolfcrypt/hmac.c \
../wolfcrypt/integer.c \
../wolfcrypt/misc.c \
../wolfcrypt/poly1305.c \
../wolfcrypt/random.c \
../wolfcrypt/sha256.c \
../wolfcrypt/sha512.c \
../wolfcrypt/srp.c 

O_SRCS += \
../wolfcrypt/chacha.o \
../wolfcrypt/chacha20_poly1305.o \
../wolfcrypt/curve25519.o \
../wolfcrypt/ed25519.o \
../wolfcrypt/fe_operations.o \
../wolfcrypt/ge_operations.o \
../wolfcrypt/hash.o \
../wolfcrypt/hmac.o \
../wolfcrypt/integer.o \
../wolfcrypt/misc.o \
../wolfcrypt/poly1305.o \
../wolfcrypt/random.o \
../wolfcrypt/sha256.o \
../wolfcrypt/sha512.o \
../wolfcrypt/srp.o 

OBJS += \
./wolfcrypt/chacha.o \
./wolfcrypt/chacha20_poly1305.o \
./wolfcrypt/curve25519.o \
./wolfcrypt/ed25519.o \
./wolfcrypt/fe_operations.o \
./wolfcrypt/ge_operations.o \
./wolfcrypt/hash.o \
./wolfcrypt/hmac.o \
./wolfcrypt/integer.o \
./wolfcrypt/misc.o \
./wolfcrypt/poly1305.o \
./wolfcrypt/random.o \
./wolfcrypt/sha256.o \
./wolfcrypt/sha512.o \
./wolfcrypt/srp.o 

C_DEPS += \
./wolfcrypt/chacha.d \
./wolfcrypt/chacha20_poly1305.d \
./wolfcrypt/curve25519.d \
./wolfcrypt/ed25519.d \
./wolfcrypt/fe_operations.d \
./wolfcrypt/ge_operations.d \
./wolfcrypt/hash.d \
./wolfcrypt/hmac.d \
./wolfcrypt/integer.d \
./wolfcrypt/misc.d \
./wolfcrypt/poly1305.d \
./wolfcrypt/random.d \
./wolfcrypt/sha256.d \
./wolfcrypt/sha512.d \
./wolfcrypt/srp.d 


# Each subdirectory must supply rules for building sources it contributes
wolfcrypt/%.o: ../wolfcrypt/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cygwin C Compiler'
	gcc -I"C:\TMP\git\HAP\cpp\hap_cpp\wolfcrypt" -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


