package com.hjy.bluetooth.utils;

import android.text.TextUtils;

import com.hjy.bluetooth.entity.ScanFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * author : HJY
 * date   : 2021/11/15
 * desc   :
 */
public class ScanFilterUtils {

    public static boolean isInFilter(String deviceName, ScanFilter filter) {

        if (filter.getNames() == null) {
            return true;
        }

        boolean isAllFilterNameEmpty = true;
        for (String name : filter.getNames()) {
            if (!TextUtils.isEmpty(name)) {
                isAllFilterNameEmpty = false;
                break;
            }
        }

        if (isAllFilterNameEmpty) {
            return true;
        }


        if (TextUtils.isEmpty(deviceName)) {
            return false;
        }


        String[] names = filter.getNames();
        if (names != null && names.length > 0) {
            if (filter.isFuzzyMatching()) {
                for (String name : names) {
                    if (deviceName.contains(name)) {
                        return true;
                    }
                }
            } else {
                for (String name : names) {
                    if (name.equals(deviceName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * Parse the real-time device name from scanRecord
     * @param scanRecord
     * @return
     */
    public static String parseDeviceName(byte[] scanRecord) {
        String result = null;
        if (null == scanRecord) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0)
                break;

            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    buffer.get(); // flags
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        buffer.getShort();
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        buffer.getInt();
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    result = new String(sb).trim();
                    return result;
                case (byte) 0xFF: // Manufacturer Specific Data
                    buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                buffer.position(buffer.position() + length);
            }
        }
        return null;
    }
}
