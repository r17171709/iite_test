package com.renyu.iitebletest.bluetooth;

import com.renyu.iitebletest.jniLibs.JNIUtils;

import java.io.UnsupportedEncodingException;

/**
 * Created by renyu on 16/3/1.
 */
public class BLEDataUtils {

    private byte divided_byte[][];
    private String origin_str;
    public final static int PACKET_LENGTH = 20;
    public final static int PACKET_PAYLOAD = 17;

    public BLEDataUtils(String json_str, int command) {
        byte[] bytes = json_str.getBytes();
        int total=bytes.length%PACKET_PAYLOAD==0?bytes.length/PACKET_PAYLOAD:bytes.length/PACKET_PAYLOAD+1;
        divided_byte=new byte[total][];
        for (int i=0;i<total;i++) {
            if (total-1!=i) {
                divided_byte[i]=new byte[PACKET_LENGTH];
                for (int j=0;j<PACKET_LENGTH;j++) {
                    if (j==0) {
                        divided_byte[i][0]= (byte) (i+1);
                    }
                    else if (j==1) {
                        divided_byte[i][1]= (byte) total;
                    }
                    else if (j==2) {
                        divided_byte[i][2]= (byte) command;
                    }
                    else {
                        divided_byte[i][j]= bytes[(i*PACKET_PAYLOAD+(j-3))];
                    }
                }
            }
            else {
                divided_byte[i]=new byte[bytes.length-PACKET_PAYLOAD*(total-1)+3];
                for (int j=0;j<(bytes.length-PACKET_PAYLOAD*(total-1)+3);j++) {
                    if (j==0) {
                        divided_byte[i][0]= (byte) (i+1);
                    }
                    else if (j==1) {
                        divided_byte[i][1]= (byte) total;
                    }
                    else if (j==2) {
                        divided_byte[i][2]= (byte) command;
                    }
                    else {
                        divided_byte[i][j]= bytes[(i*PACKET_PAYLOAD+(j-3))];
                    }
                }
            }
        }
    }

    public BLEDataUtils(byte[][] byte_str) {
        int number=0;
        for (int i=0;i<byte_str.length;i++) {
            for (int j=3;j<byte_str[i].length;j++) {
                if (byte_str[i][j]!=0) {
                    number+=1;
                }
            }
        }
        byte[] bytes=new byte[number];
        int index=0;
        for (int i=0;i<byte_str.length;i++) {
            for (int j=3;j<byte_str[i].length;j++) {
                if (byte_str[i][j]!=0) {
                    bytes[index]=byte_str[i][j];
                    index++;
                }
            }
        }
        try {
            origin_str=new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            origin_str="";
        }
    }

    public byte[][] getDivided_data() {
        return divided_byte;
    }

    public String getOrigin_str()
    {
        return origin_str;
    }

    /**
     * CCM加密
     * byte test[] = {0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02};
     * @param source
     * @return
     */
    public static byte[] encode(byte[] source, int payloadLength) {
        JNIUtils jniUtils=new JNIUtils();
        return jniUtils.sendencode(source, payloadLength);
    }

    /**
     * CCM解密
     * @param passCode
     * @return
     */
    public static byte[] decode(byte[] passCode, int payloadLength) {
        JNIUtils jniUtils=new JNIUtils();
        byte[] msg=new byte[16];
        for (int i=0;i<16;i++) {
            msg[i]=passCode[i];
        }
        byte[] tag=new byte[4];
        for (int i=0;i<4;i++) {
            tag[i]=passCode[16+i];
        }
        return jniUtils.senddecode(msg, tag, payloadLength);
    }
}
