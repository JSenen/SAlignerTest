package com.juansenen.salignertest;

import java.util.ArrayDeque;

interface SerialListener {
    void onSerialRead(ArrayDeque<byte[]> datas);
    void onSerialRead(byte[] data);
    void onSerialConnect();
    void onSerialConnectError(Exception e);
    void onSerialIoError(Exception e);
}
