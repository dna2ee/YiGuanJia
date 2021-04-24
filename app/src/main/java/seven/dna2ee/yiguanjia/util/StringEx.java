package seven.dna2ee.yiguanjia.util;

public class StringEx {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (CharSequence one : elements) {
            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(one);
        }
        return sb.toString();
    }
}
