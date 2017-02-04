
#include "aes.h"
#include "hmac.h"
#include "sha2.h"
#include "sha3.h"
#include "modes.h"
#include "salsa20.h"
#include "poly1305.h"
#include "norx.h"
#include "chacha20poly1305.h"

#include <stdio.h>

void aes128ccm_test(uint8_t* msg, uint8_t* cipher, uint8_t* tag, size_t payloadLength)
{
  uint8_t key[16] = { 0xb7,0x80,0x84,0x0e,0x2f,0xcd,0xaa,0x2c,
                      0xe6,0xa8,0xc7,0x70,0xd6,0xc0,0x10,0xec  };

  cf_aes_context ctx;
  cf_aes_init(&ctx, key, sizeof key);

  uint8_t aad[16] = { (uint8_t)1 };
  uint8_t nonce[13] = { 0xa7,0x53,0xe4,0x25,0x91,0x5f,0xc8,0x64,
                        0xfe,0x48,0x39,0xb1,0xbc };

  cf_ccm_encrypt(&cf_aes, &ctx,
                 msg, payloadLength, 2,
                 aad, 1,
                 nonce, sizeof nonce,
                 cipher,
                 tag, 4);
}

void aes128ccm_decrypt(uint8_t* msg, uint8_t* tag, uint8_t* cipher, size_t payloadLength)
{
  uint8_t key[16] = { 0xb7,0x80,0x84,0x0e,0x2f,0xcd,0xaa,0x2c,
                      0xe6,0xa8,0xc7,0x70,0xd6,0xc0,0x10,0xec  };

  cf_aes_context ctx;
  cf_aes_init(&ctx, key, sizeof key);

  uint8_t aad[16] = { 1 };
  uint8_t nonce[13] = { 0xa7,0x53,0xe4,0x25,0x91,0x5f,0xc8,0x64,
                        0xfe,0x48,0x39,0xb1,0xbc };

  cf_ccm_decrypt(
          &cf_aes, &ctx,
          msg, payloadLength,
          2,
          aad, 1,
          nonce, 13,
          tag, 4,
          cipher);
}