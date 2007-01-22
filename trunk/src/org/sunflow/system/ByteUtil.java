package org.sunflow.system;

public class ByteUtil {

    public static final byte[] get2Bytes(int i) {
        byte[] b = new byte[2];

        b[0] = (byte) (i & 0xFF);
        b[1] = (byte) ((i >> 8) & 0xFF);

        return b;
    }

    public static final byte[] get4Bytes(int i) {
        byte[] b = new byte[4];

        b[0] = (byte) (i & 0xFF);
        b[1] = (byte) ((i >> 8) & 0xFF);
        b[2] = (byte) ((i >> 16) & 0xFF);
        b[3] = (byte) ((i >> 24) & 0xFF);

        return b;
    }

    public static final byte[] get4BytesInv(int i) {
        byte[] b = new byte[4];

        b[3] = (byte) (i & 0xFF);
        b[2] = (byte) ((i >> 8) & 0xFF);
        b[1] = (byte) ((i >> 16) & 0xFF);
        b[0] = (byte) ((i >> 24) & 0xFF);

        return b;
    }

    public static final byte[] get8Bytes(long i) {
        byte[] b = new byte[8];

        b[0] = (byte) (i & 0xFF);
        b[1] = (byte) ((long) ((long) i >> (long) 8) & (long) 0xFF);
        b[2] = (byte) ((long) ((long) i >> (long) 16) & (long) 0xFF);
        b[3] = (byte) ((long) ((long) i >> (long) 24) & (long) 0xFF);

        b[4] = (byte) ((long) ((long) i >> (long) 32) & (long) 0xFF);
        b[5] = (byte) ((long) ((long) i >> (long) 40) & (long) 0xFF);
        b[6] = (byte) ((long) ((long) i >> (long) 48) & (long) 0xFF);
        b[7] = (byte) ((long) ((long) i >> (long) 56) & (long) 0xFF);

        return b;
    }

    public static final long toLong(byte[] in) {
        return (long) (((long) (toInt(in[0], in[1], in[2], in[3]))) | ((long) (toInt(in[4], in[5], in[6], in[7])) << (long) 32));
    }

    public static final int toInt(byte in0, byte in1, byte in2, byte in3) {
        return (in0 & 0xFF) | ((in1 & 0xFF) << 8) | ((in2 & 0xFF) << 16) | ((in3 & 0xFF) << 24);
    }

    public static final int toInt(byte[] in) {
        return toInt(in[0], in[1], in[2], in[3]);
    }

    public static final int toInt(byte[] in, int ofs) {
        return toInt(in[ofs + 0], in[ofs + 1], in[ofs + 2], in[ofs + 3]);
    }

    public static final int floatToHalf(float f) {
        int i = Float.floatToRawIntBits(f);
        // unpack the s, e and m of the float
        int s = (i >> 16) & 0x00008000;
        int e = ((i >> 23) & 0x000000ff) - (127 - 15);
        int m = i & 0x007fffff;
        // pack them back up, forming a half
        if (e <= 0) {
            if (e < -10) {
                // E is less than -10. The absolute value of f is less than
                // HALF_MIN
                // convert f to 0
                return 0;
            }
            // E is between -10 and 0.
            m = (m | 0x00800000) >> (1 - e);
            // Round to nearest, round "0.5" up.
            if ((m & 0x00001000) == 0x00001000)
                m += 0x00002000;
            // Assemble the half from s, e (zero) and m.
            return s | (m >> 13);
        } else if (e == 0xff - (127 - 15)) {
            if (m == 0) {
                // F is an infinity; convert f to a half infinity
                return s | 0x7c00;
            } else {
                // F is a NAN; we produce a half NAN that preserves the sign bit
                // and the 10 leftmost bits of the significand of f
                m >>= 13;
                return s | 0x7c00 | m | ((m == 0) ? 0 : 1);
            }
        } else {
            // E is greater than zero. F is a normalized float. Round to
            // nearest, round "0.5" up
            if ((m & 0x00001000) == 0x00001000) {
                m += 0x00002000;
                if ((m & 0x00800000) == 0x00800000) {
                    m = 0;
                    e += 1;
                }
            }
            // Handle exponent overflow
            if (e > 30) {
                // overflow (); // Cause a hardware floating point overflow;
                return s | 0x7c00; // if this returns, the half becomes an
            } // infinity with the same sign as f.
            // Assemble the half from s, e and m.
            return s | (e << 10) | (m >> 13);
        }
    }
}