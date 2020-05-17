#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h> /* Unix System Calls */
#include <sys/socket.h> /* socket specific definitions */
#include <arpa/inet.h>  /* IP address conversion stuff */

#define  DNS_A   1 
#define  DNS_PTR 12 
#define  DNS_TXT 16
#define  DNS_SRV 33 

#define MDNS_PORT 5353
#define MDNS_GROUP "224.0.0.251"

static uint8_t  header[] = { 0, 0, 0x84, 0, 0, 0, 0, 4, 0, 0, 0, 0 };
static const char* host_name = "volo.local";  //A
static const char* hap = "_hap._tcp.local";
static char full_service_name[64];
static char txtProps[64];

//static const uint8_t ip[] = { 192, 168, 1, 109 };

static int rec_count;

const static int HAP_PORT = 9123;

static int ttl = 30;  //sec

#define BUFSIZE 512

static uint8_t buf[BUFSIZE];
static uint16_t buflen;

void reset()
{
    memset (buf, 0, BUFSIZE);
    buflen = 0;
}

static void write_byte(uint8_t v)
{
    buf[buflen++] = v;
}

static void write_short (uint16_t v)
{
    v &= 0xffff;
    write_byte (v>>8);
    write_byte (v&0xff);
}

void write_int (uint32_t v)
{
    write_short (v>>16);
    write_short (v&0xffff);
}

void write_bytes (const uint8_t* array, int len)
{
    for (int i = 0; i < len; ++i)
    {
        write_byte (array[i]);
    }
}

static void write_name (const char* name)
{
    uint8_t len = 0;
    uint8_t i =0;
    const char* pname = name;
    while (*pname)
    {
        if (name[i] == '.')
        {
            buf [buflen  + i - len] = len;
            len = 0;
        }
        else
        {
            ++len;
            buf[ buflen + i +1] = name[i];
        }
        ++pname;
        ++i;
    }
    if (len != 0)
    {
        buf [buflen + i - len] = len;
    }

    buflen += i + 1;
    write_byte(0);
}

static void build_A(const uint32_t ip) 
{
    ++rec_count;
    write_name (host_name);
    write_short(DNS_A);
    write_short(0x8001); //class
    write_int(ttl);
    write_short(4); //reclen
    write_int(ip);
}

static void build_PTR() 
{
    ++rec_count;
    write_name (hap);
    write_short(DNS_PTR); //PTR
    write_short(0x0001); //class
    write_int(ttl);
    write_short(strlen(full_service_name) + 2); //reclen
    write_name (full_service_name);
}

static void build_SRV() 
{
    ++rec_count;
    write_name (full_service_name);
    write_short(DNS_SRV);
    write_short(0x8001); //class
    write_int(ttl);
    write_short(strlen(host_name) + 2 + 6); //reclen
    write_short(1); //prio
    write_short(1); //weight
    write_short(HAP_PORT); //port
    write_name (host_name);
}

static void build_TXT(const char* service_name, const char* mac) 
{
    ++rec_count;
    sprintf(txtProps, "md=%s.c#=1.s#=1.ff=0.ci=1.id=%s.sf=1", service_name, mac);
    write_name (full_service_name);
    write_short(DNS_TXT); 
    write_short(0x8001); //class
    write_int(ttl);
    write_short(strlen(txtProps) + 2); //reclen
    write_name (txtProps);
}

void build_packet(const char* service_name, const char* mac, const uint32_t ip)
{
    sprintf(full_service_name, "%s.%s", service_name, hap);
    write_bytes(header, (int)sizeof(header));
    build_PTR();
    build_SRV();
    build_TXT(service_name, mac);
    build_A(ip);
    buf[7] = rec_count;
}

static void send_to(const uint8_t* packet, uint16_t len) 
{
    static int fd;
    static struct sockaddr_in addr;
    if (fd == 0) 
    {
        if( (fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
        {
            perror("Problem creating socket\n");
            exit(1);
        }

        memset(&addr,0,sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = inet_addr(MDNS_GROUP);
        addr.sin_port = htons(MDNS_PORT);
    }

    sendto(fd, packet, len, 0, (struct sockaddr *) &addr, sizeof(addr));
}

void advertise(const char* service_name, const char* id, const char* ip)
{
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);

    build_packet(service_name, id, htonl(inet_addr(ip)));

    while (1)
    {
        send_to (buf, buflen);
        sleep(1);
    }
}
