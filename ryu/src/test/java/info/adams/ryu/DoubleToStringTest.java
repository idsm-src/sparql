// This file is derived from code in https://github.com/ulfjack/ryu.
// It has been modified to conform to the XSD datatype representation
// for IEEE floating-point values.
//
// Copyright 2018 Ulf Adams
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package info.adams.ryu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;



public class DoubleToStringTest
{
    private void assertD2sEquals(String expected, double f)
    {
        assertEquals(expected, RyuDouble.doubleToString(f));
    }


    @Test
    public void simpleCases()
    {
        assertD2sEquals("0.0E0", 0);
        assertD2sEquals("-0.0E0", Double.longBitsToDouble(0x8000000000000000L));
        assertD2sEquals("1.0E0", 1.0d);
        assertD2sEquals("-1.0E0", -1.0d);
        assertD2sEquals("NaN", Double.NaN);
        assertD2sEquals("INF", Double.POSITIVE_INFINITY);
        assertD2sEquals("-INF", Double.NEGATIVE_INFINITY);
    }


    @Test
    public void switchToSubnormal()
    {
        assertD2sEquals("2.2250738585072014E-308", Double.longBitsToDouble(0x0010000000000000L));
    }


    @Test
    public void minAndMax()
    {
        assertD2sEquals("1.7976931348623157E308", Double.longBitsToDouble(0x7fefffffffffffffL));
        assertD2sEquals("4.9E-324", Double.longBitsToDouble(1));
    }


    @Test
    public void roundingModeEven()
    {
        assertD2sEquals("-2.109808898695963E16", -2.109808898695963E16);
    }


    @Test
    public void regressionTest()
    {
        assertD2sEquals("4.940656E-318", 4.940656E-318d);
        assertD2sEquals("1.18575755E-316", 1.18575755E-316d);
        assertD2sEquals("2.989102097996E-312", 2.989102097996E-312d);
        assertD2sEquals("9.0608011534336E15", 9.0608011534336E15d);
        assertD2sEquals("4.708356024711512E18", 4.708356024711512E18);
        assertD2sEquals("9.409340012568248E18", 9.409340012568248E18);
        // This number naively requires 65 bit for the intermediate results if we reduce the lookup
        // table by half. This checks that we don't loose any information in that case.
        assertD2sEquals("1.8531501765868567E21", 1.8531501765868567E21);
        assertD2sEquals("-3.347727380279489E33", -3.347727380279489E33);
        // Discovered by Andriy Plokhotnyuk, see #29.
        assertD2sEquals("1.9430376160308388E16", 1.9430376160308388E16);
        assertD2sEquals("-6.9741824662760956E19", -6.9741824662760956E19);
        assertD2sEquals("4.3816050601147837E18", 4.3816050601147837E18);
    }


    /*
     * Extracted from d2s_test.cc
     */
    @Test
    public void ryuTest()
    {
        long maxMantissa = (1L << 53) - 1;

        assertD2sEquals("0.0E0", 0.0);
        assertD2sEquals("-0.0E0", -0.0);
        assertD2sEquals("1.0E0", 1.0);
        assertD2sEquals("-1.0E0", -1.0);
        assertD2sEquals("NaN", Double.NaN);
        assertD2sEquals("INF", Double.POSITIVE_INFINITY);
        assertD2sEquals("-INF", Double.NEGATIVE_INFINITY);
        assertD2sEquals("2.2250738585072014E-308", 2.2250738585072014E-308);
        assertD2sEquals("1.7976931348623157E308", Double.longBitsToDouble(0x7fefffffffffffffL));
        assertD2sEquals("4.9E-324", Double.longBitsToDouble(1L));
        assertD2sEquals("2.9802322387695312E-8", 2.98023223876953125E-8);
        assertD2sEquals("-2.109808898695963E16", -2.109808898695963E16);
        assertD2sEquals("4.940656E-318", 4.940656E-318);
        assertD2sEquals("1.18575755E-316", 1.18575755E-316);
        assertD2sEquals("2.989102097996E-312", 2.989102097996E-312);
        assertD2sEquals("9.0608011534336E15", 9.0608011534336E15);
        assertD2sEquals("4.708356024711512E18", 4.708356024711512E18);
        assertD2sEquals("9.409340012568248E18", 9.409340012568248E18);
        assertD2sEquals("1.2345678E0", 1.2345678);
        assertD2sEquals("5.764607523034235E39", Double.longBitsToDouble(0x4830F0CF064DD592L));
        assertD2sEquals("1.152921504606847E40", Double.longBitsToDouble(0x4840F0CF064DD592L));
        assertD2sEquals("2.305843009213694E40", Double.longBitsToDouble(0x4850F0CF064DD592L));
        assertD2sEquals("1.0E0", 1);
        assertD2sEquals("1.2E0", 1.2);
        assertD2sEquals("1.23E0", 1.23);
        assertD2sEquals("1.234E0", 1.234);
        assertD2sEquals("1.2345E0", 1.2345);
        assertD2sEquals("1.23456E0", 1.23456);
        assertD2sEquals("1.234567E0", 1.234567);
        assertD2sEquals("1.2345678E0", 1.2345678);
        assertD2sEquals("1.23456789E0", 1.23456789);
        assertD2sEquals("1.234567895E0", 1.234567895);
        assertD2sEquals("1.2345678901E0", 1.2345678901);
        assertD2sEquals("1.23456789012E0", 1.23456789012);
        assertD2sEquals("1.234567890123E0", 1.234567890123);
        assertD2sEquals("1.2345678901234E0", 1.2345678901234);
        assertD2sEquals("1.23456789012345E0", 1.23456789012345);
        assertD2sEquals("1.234567890123456E0", 1.234567890123456);
        assertD2sEquals("1.2345678901234567E0", 1.2345678901234567);
        assertD2sEquals("4.294967294E0", 4.294967294);
        assertD2sEquals("4.294967295E0", 4.294967295);
        assertD2sEquals("4.294967296E0", 4.294967296);
        assertD2sEquals("4.294967297E0", 4.294967297);
        assertD2sEquals("4.294967298E0", 4.294967298);
        assertD2sEquals("1.7800590868057611E-307", ieeeParts2Double(false, 4, 0));
        assertD2sEquals("2.8480945388892175E-306", ieeeParts2Double(false, 6, maxMantissa));
        assertD2sEquals("2.446494580089078E-296", ieeeParts2Double(false, 41, 0));
        assertD2sEquals("4.8929891601781557E-296", ieeeParts2Double(false, 40, maxMantissa));
        assertD2sEquals("1.8014398509481984E16", ieeeParts2Double(false, 1077, 0));
        assertD2sEquals("3.6028797018963964E16", ieeeParts2Double(false, 1076, maxMantissa));
        assertD2sEquals("2.900835519859558E-216", ieeeParts2Double(false, 307, 0));
        assertD2sEquals("5.801671039719115E-216", ieeeParts2Double(false, 306, maxMantissa));
        assertD2sEquals("3.196104012172126E-27", ieeeParts2Double(false, 934, 0x000FA7161A4D6E0CL));
        assertD2sEquals("9.007199254740991E15", 9007199254740991.0);
        assertD2sEquals("9.007199254740992E15", 9007199254740992.0);
        assertD2sEquals("1.0E0", 1.0e+0);
        assertD2sEquals("1.2E1", 1.2e+1);
        assertD2sEquals("1.23E2", 1.23e+2);
        assertD2sEquals("1.234E3", 1.234e+3);
        assertD2sEquals("1.2345E4", 1.2345e+4);
        assertD2sEquals("1.23456E5", 1.23456e+5);
        assertD2sEquals("1.234567E6", 1.234567e+6);
        assertD2sEquals("1.2345678E7", 1.2345678e+7);
        assertD2sEquals("1.23456789E8", 1.23456789e+8);
        assertD2sEquals("1.23456789E9", 1.23456789e+9);
        assertD2sEquals("1.234567895E9", 1.234567895e+9);
        assertD2sEquals("1.2345678901E10", 1.2345678901e+10);
        assertD2sEquals("1.23456789012E11", 1.23456789012e+11);
        assertD2sEquals("1.234567890123E12", 1.234567890123e+12);
        assertD2sEquals("1.2345678901234E13", 1.2345678901234e+13);
        assertD2sEquals("1.23456789012345E14", 1.23456789012345e+14);
        assertD2sEquals("1.234567890123456E15", 1.234567890123456e+15);
        assertD2sEquals("1.0E0", 1.0e+0);
        assertD2sEquals("1.0E1", 1.0e+1);
        assertD2sEquals("1.0E2", 1.0e+2);
        assertD2sEquals("1.0E3", 1.0e+3);
        assertD2sEquals("1.0E4", 1.0e+4);
        assertD2sEquals("1.0E5", 1.0e+5);
        assertD2sEquals("1.0E6", 1.0e+6);
        assertD2sEquals("1.0E7", 1.0e+7);
        assertD2sEquals("1.0E8", 1.0e+8);
        assertD2sEquals("1.0E9", 1.0e+9);
        assertD2sEquals("1.0E10", 1.0e+10);
        assertD2sEquals("1.0E11", 1.0e+11);
        assertD2sEquals("1.0E12", 1.0e+12);
        assertD2sEquals("1.0E13", 1.0e+13);
        assertD2sEquals("1.0E14", 1.0e+14);
        assertD2sEquals("1.0E15", 1.0e+15);
        assertD2sEquals("1.000000000000001E15", 1.0e+15 + 1.0e+0);
        assertD2sEquals("1.00000000000001E15", 1.0e+15 + 1.0e+1);
        assertD2sEquals("1.0000000000001E15", 1.0e+15 + 1.0e+2);
        assertD2sEquals("1.000000000001E15", 1.0e+15 + 1.0e+3);
        assertD2sEquals("1.00000000001E15", 1.0e+15 + 1.0e+4);
        assertD2sEquals("1.0000000001E15", 1.0e+15 + 1.0e+5);
        assertD2sEquals("1.000000001E15", 1.0e+15 + 1.0e+6);
        assertD2sEquals("1.00000001E15", 1.0e+15 + 1.0e+7);
        assertD2sEquals("1.0000001E15", 1.0e+15 + 1.0e+8);
        assertD2sEquals("1.000001E15", 1.0e+15 + 1.0e+9);
        assertD2sEquals("1.00001E15", 1.0e+15 + 1.0e+10);
        assertD2sEquals("1.0001E15", 1.0e+15 + 1.0e+11);
        assertD2sEquals("1.001E15", 1.0e+15 + 1.0e+12);
        assertD2sEquals("1.01E15", 1.0e+15 + 1.0e+13);
        assertD2sEquals("1.1E15", 1.0e+15 + 1.0e+14);
        assertD2sEquals("8.0E0", 8.0);
        assertD2sEquals("6.4E1", 64.0);
        assertD2sEquals("5.12E2", 512.0);
        assertD2sEquals("8.192E3", 8192.0);
        assertD2sEquals("6.5536E4", 65536.0);
        assertD2sEquals("5.24288E5", 524288.0);
        assertD2sEquals("8.388608E6", 8388608.0);
        assertD2sEquals("6.7108864E7", 67108864.0);
        assertD2sEquals("5.36870912E8", 536870912.0);
        assertD2sEquals("8.589934592E9", 8589934592.0);
        assertD2sEquals("6.8719476736E10", 68719476736.0);
        assertD2sEquals("5.49755813888E11", 549755813888.0);
        assertD2sEquals("8.796093022208E12", 8796093022208.0);
        assertD2sEquals("7.0368744177664E13", 70368744177664.0);
        assertD2sEquals("5.62949953421312E14", 562949953421312.0);
        assertD2sEquals("9.007199254740992E15", 9007199254740992.0);
        assertD2sEquals("8.0E3", 8.0e+3);
        assertD2sEquals("6.4E4", 64.0e+3);
        assertD2sEquals("5.12E5", 512.0e+3);
        assertD2sEquals("8.192E6", 8192.0e+3);
        assertD2sEquals("6.5536E7", 65536.0e+3);
        assertD2sEquals("5.24288E8", 524288.0e+3);
        assertD2sEquals("8.388608E9", 8388608.0e+3);
        assertD2sEquals("6.7108864E10", 67108864.0e+3);
        assertD2sEquals("5.36870912E11", 536870912.0e+3);
        assertD2sEquals("8.589934592E12", 8589934592.0e+3);
        assertD2sEquals("6.8719476736E13", 68719476736.0e+3);
        assertD2sEquals("5.49755813888E14", 549755813888.0e+3);
        assertD2sEquals("8.796093022208E15", 8796093022208.0e+3);
    }


    static double ieeeParts2Double(final boolean sign, final int ieeeExponent, final long ieeeMantissa)
    {
        assert ieeeExponent >= 0 && ieeeExponent <= 2047;
        assert ieeeMantissa >= 0 && ieeeMantissa <= (1L << 53) - 1;
        return Double.longBitsToDouble(((sign ? 1L : 0L) << 63) | ((long) ieeeExponent << 52) | ieeeMantissa);
    }
}
