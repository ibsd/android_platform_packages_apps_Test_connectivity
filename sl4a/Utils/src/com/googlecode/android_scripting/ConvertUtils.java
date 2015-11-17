
package com.googlecode.android_scripting;

public class ConvertUtils {
    /**
     * Converts a String of comma separated bytes to a byte array
     *
     * @param value The value to convert
     * @return the byte array
     */
    public static byte[] convertStringToByteArray(String value) {
        if (value.equals("")) {
            return new byte[0];
        }
        String[] parseString = value.split(",");
        byte[] byteArray = new byte[parseString.length];
        if (byteArray.length > 0) {
            for (int i = 0; i < parseString.length; i++) {
                byte byteValue = Byte.valueOf(parseString[i].trim());
                byteArray[i] = byteValue;
            }
        }
        return byteArray;
    }

    /**
     * Converts a byte array to a comma separated String
     *
     * @param byteArray
     * @return comma separated string of bytes
     */
    public static String convertByteArrayToString(byte[] byteArray) {
        String ret = "";
        if (byteArray != null) {
            for (int i = 0; i < byteArray.length; i++) {
                if ((i + 1) != byteArray.length) {
                    ret = ret + Byte.valueOf(byteArray[i]) + ",";
                }
                else {
                    ret = ret + Byte.valueOf(byteArray[i]);
                }
            }
        }
        return ret;
    }

}
