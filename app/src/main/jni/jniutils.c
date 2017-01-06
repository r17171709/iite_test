#include <jni.h>
#include <string.h>

#include "stdint.h"
#include "stdio.h"

int aes128ccm_decrypt(uint8_t* msg, uint8_t* tag, uint8_t* cipher);
void aes128ccm_test(uint8_t* msg, uint8_t* cipher, uint8_t* tag);

JNIEXPORT jstring JNICALL
Java_com_renyu_iitebletest_jniLibs_JNIUtils_stringFromJni(JNIEnv *env, jobject instance) {

    // TODO

    uint8_t msg[16] = { 0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55 };
    uint8_t cipher[16];
    uint8_t tag[16];
    aes128ccm_test(msg, cipher, tag);
    char result__[129];
    memcmp(result__, (char*) cipher, 16);

    char temp[256];
    char output[256];

    *temp = *output = 0;

    for (int i=0; i<16; i++)
    {
        sprintf(temp, "%02x ", cipher[i]);
        strcat(output, temp);
    }

    *result__ = 0;
    strcpy(result__, output);

    return (*env)->NewStringUTF(env, result__);
}

JNIEXPORT jbyteArray JNICALL
Java_com_renyu_iitebletest_jniLibs_JNIUtils_sendencode(JNIEnv *env, jobject instance,
                                                       jbyteArray values_) {

    jbyte *values = (*env)->GetByteArrayElements(env, values_, NULL);
    // TODO
    unsigned char output_[32];
    memcpy((void*)output_, (void*)values, 16);

    uint8_t cipher[16];
    uint8_t tag[16];

    aes128ccm_test(output_, cipher, tag);

    char result__[129];
    memcpy(result__, (char*) cipher, 16);

    result__[16] = tag[0];
    result__[17] = tag[1];
    result__[18] = tag[2];
    result__[19] = tag[3];

    jbyteArray array = (*env)->NewByteArray(env, 20);
    (*env)->SetByteArrayRegion(env, array, 0, 20, result__);

    (*env)->ReleaseByteArrayElements(env, values_, values, 0);

    return array;
}

JNIEXPORT jbyteArray JNICALL
Java_com_renyu_iitebletest_jniLibs_JNIUtils_senddecode(JNIEnv *env, jobject instance,
                                                       jbyteArray values_, jbyteArray tags_) {
    jbyte *values = (*env)->GetByteArrayElements(env, values_, NULL);
    jbyte *tags = (*env)->GetByteArrayElements(env, tags_, NULL);

    unsigned char msg[32];
    memcpy((void*)msg, (void*)values, 16);
    unsigned char tag[8];
    memcpy((void*)tag, (void*)tags, 4);

    uint8_t cipher[16];

    aes128ccm_decrypt(msg, tag, cipher);

    char result__[129];
    memcpy(result__, (char*) cipher, 16);

    jbyteArray array = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, array, 0, 16, result__);

    // TODO

    (*env)->ReleaseByteArrayElements(env, values_, values, 0);
    (*env)->ReleaseByteArrayElements(env, tags_, tags, 0);

    return array;
}