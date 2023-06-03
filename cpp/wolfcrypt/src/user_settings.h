//#define WOLFSSL_MISC_INCLUDED
//#define FREERTOS
#define WOLFSSL_LWIP
#define NO_WOLFSSL_MEMORY
#define WOLFCRYPT_HAVE_SRP
#define NO_MD5
#define NO_SHA
#define WOLFSSL_SHA512
#define WOLFSSL_SMALL_STACK
#define HAVE_HKDF
#define HAVE_CHACHA
#define HAVE_POLY1305
#define HAVE_ED25519
#define HAVE_CURVE25519
//#define CUSTOM_RAND_GENERATE_BLOCK os_get_random
#define NO_DH
#define NO_DSA

#define ICACHE_RODATA_ATTR   //this is esp8266 specific attr to  store data in SRAM