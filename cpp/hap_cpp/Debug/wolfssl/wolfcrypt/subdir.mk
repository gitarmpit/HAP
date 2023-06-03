################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../wolfssl/wolfcrypt/chacha.c \
../wolfssl/wolfcrypt/chacha20_poly1305.c \
../wolfssl/wolfcrypt/curve25519.c \
../wolfssl/wolfcrypt/ed25519.c \
../wolfssl/wolfcrypt/fe_operations.c \
../wolfssl/wolfcrypt/ge_operations.c \
../wolfssl/wolfcrypt/hash.c \
../wolfssl/wolfcrypt/hmac.c \
../wolfssl/wolfcrypt/integer.c \
../wolfssl/wolfcrypt/misc.c \
../wolfssl/wolfcrypt/poly1305.c \
../wolfssl/wolfcrypt/random.c \
../wolfssl/wolfcrypt/sha256.c \
../wolfssl/wolfcrypt/sha512.c \
../wolfssl/wolfcrypt/srp.c 

O_SRCS += \
../wolfssl/wolfcrypt/chacha.o \
../wolfssl/wolfcrypt/chacha20_poly1305.o \
../wolfssl/wolfcrypt/curve25519.o \
../wolfssl/wolfcrypt/ed25519.o \
../wolfssl/wolfcrypt/fe_operations.o \
../wolfssl/wolfcrypt/ge_operations.o \
../wolfssl/wolfcrypt/hash.o \
../wolfssl/wolfcrypt/hmac.o \
../wolfssl/wolfcrypt/integer.o \
../wolfssl/wolfcrypt/misc.o \
../wolfssl/wolfcrypt/poly1305.o \
../wolfssl/wolfcrypt/random.o \
../wolfssl/wolfcrypt/sha256.o \
../wolfssl/wolfcrypt/sha512.o \
../wolfssl/wolfcrypt/srp.o 

OBJS += \
./wolfssl/wolfcrypt/chacha.o \
./wolfssl/wolfcrypt/chacha20_poly1305.o \
./wolfssl/wolfcrypt/curve25519.o \
./wolfssl/wolfcrypt/ed25519.o \
./wolfssl/wolfcrypt/fe_operations.o \
./wolfssl/wolfcrypt/ge_operations.o \
./wolfssl/wolfcrypt/hash.o \
./wolfssl/wolfcrypt/hmac.o \
./wolfssl/wolfcrypt/integer.o \
./wolfssl/wolfcrypt/misc.o \
./wolfssl/wolfcrypt/poly1305.o \
./wolfssl/wolfcrypt/random.o \
./wolfssl/wolfcrypt/sha256.o \
./wolfssl/wolfcrypt/sha512.o \
./wolfssl/wolfcrypt/srp.o 

C_DEPS += \
./wolfssl/wolfcrypt/chacha.d \
./wolfssl/wolfcrypt/chacha20_poly1305.d \
./wolfssl/wolfcrypt/curve25519.d \
./wolfssl/wolfcrypt/ed25519.d \
./wolfssl/wolfcrypt/fe_operations.d \
./wolfssl/wolfcrypt/ge_operations.d \
./wolfssl/wolfcrypt/hash.d \
./wolfssl/wolfcrypt/hmac.d \
./wolfssl/wolfcrypt/integer.d \
./wolfssl/wolfcrypt/misc.d \
./wolfssl/wolfcrypt/poly1305.d \
./wolfssl/wolfcrypt/random.d \
./wolfssl/wolfcrypt/sha256.d \
./wolfssl/wolfcrypt/sha512.d \
./wolfssl/wolfcrypt/srp.d 


# Each subdirectory must supply rules for building sources it contributes
wolfssl/wolfcrypt/%.o: ../wolfssl/wolfcrypt/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cygwin C Compiler'
	gcc -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


