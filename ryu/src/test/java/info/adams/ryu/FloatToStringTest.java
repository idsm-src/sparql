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



public class FloatToStringTest
{
    private void assertF2sEquals(String expected, float f)
    {
        assertEquals(expected, RyuFloat.floatToString(f));
    }


    @Test
    public void simpleCases()
    {
        assertF2sEquals("0.0E0", 0);
        assertF2sEquals("-0.0E0", Float.intBitsToFloat(0x80000000));
        assertF2sEquals("1.0E0", 1.0f);
        assertF2sEquals("-1.0E0", -1f);
        assertF2sEquals("NaN", Float.NaN);
        assertF2sEquals("INF", Float.POSITIVE_INFINITY);
        assertF2sEquals("-INF", Float.NEGATIVE_INFINITY);
    }


    @Test
    public void switchToSubnormal()
    {
        assertF2sEquals("1.1754944E-38", Float.intBitsToFloat(0x00800000));
    }


    @Test
    public void minAndMax()
    {
        assertF2sEquals("3.4028235E38", Float.intBitsToFloat(0x7f7fffff));
        assertF2sEquals("1.4E-45", Float.intBitsToFloat(0x00000001));
    }


    @Test
    public void roundingModeEven()
    {
        assertF2sEquals("3.355445E7", 3.3554448E7f);
        assertF2sEquals("9.0E9", 8.999999E9f);
        assertF2sEquals("3.436672E10", 3.4366717E10f);
    }


    @Test
    public void lotsOfTrailingZeros()
    {
        assertF2sEquals("2.4414062E-4", 2.4414062E-4f);
        assertF2sEquals("2.4414062E-3", 2.4414062E-3f);
        assertF2sEquals("4.3945312E-3", 4.3945312E-3f);
        assertF2sEquals("6.3476562E-3", 6.3476562E-3f);
    }


    @Test
    public void roundingEvenIfTied()
    {
        assertF2sEquals("3.3007812E-1", 0.33007812f);
    }


    @Test
    public void looksLikePow5()
    {
        // These are all floating point numbers where the mantissa is a power of 5,
        // and the exponent is in the range such that q = 10.
        assertF2sEquals("6.7108864E17", Float.intBitsToFloat(0x5D1502F9));
        assertF2sEquals("1.3421773E18", Float.intBitsToFloat(0x5D9502F9));
        assertF2sEquals("2.6843546E18", Float.intBitsToFloat(0x5E1502F9));
    }


    @Test
    public void regressionTest()
    {
        assertF2sEquals("4.7223665E21", 4.7223665E21f);
        assertF2sEquals("8.388608E6", 8388608.0f);
        assertF2sEquals("1.6777216E7", 1.6777216E7f);
        assertF2sEquals("3.3554436E7", 3.3554436E7f);
        assertF2sEquals("6.7131496E7", 6.7131496E7f);
        assertF2sEquals("1.9310392E-38", 1.9310392E-38f);
        assertF2sEquals("-2.47E-43", -2.47E-43f);
        assertF2sEquals("1.993244E-38", 1.993244E-38f);
        assertF2sEquals("4.1039004E3", 4103.9003f);
        assertF2sEquals("5.3399997E9", 5.3399997E9f);
        assertF2sEquals("6.0898E-39", 6.0898E-39f);
        assertF2sEquals("1.0310042E-3", 0.0010310042f);
        assertF2sEquals("2.882326E17", 2.8823261E17f);
        assertF2sEquals("7.038531E-26", 7.038531E-26f);
        assertF2sEquals("9.223404E17", 9.2234038E17f);
        assertF2sEquals("6.710887E7", 6.7108872E7f);
        assertF2sEquals("1.0E-44", 1.0E-44f);
        assertF2sEquals("2.816025E14", 2.816025E14f);
        assertF2sEquals("9.223372E18", 9.223372E18f);
        assertF2sEquals("1.5846086E29", 1.5846085E29f);
        assertF2sEquals("1.1811161E19", 1.1811161E19f);
        assertF2sEquals("5.368709E18", 5.368709E18f);
        assertF2sEquals("4.6143166E18", 4.6143165E18f);
        assertF2sEquals("7.812537E-3", 0.007812537f);
        assertF2sEquals("1.4E-45", 1.4E-45f);
        assertF2sEquals("1.18697725E20", 1.18697724E20f);
        assertF2sEquals("1.00014165E-36", 1.00014165E-36f);
        assertF2sEquals("2.0E2", 200f);
        assertF2sEquals("3.3554432E7", 3.3554432E7f);
    }


    /*
     * Extracted from f2s_test.cc
     */
    @Test
    public void ryuTest()
    {
        assertF2sEquals("0.0E0", 0.0f);
        assertF2sEquals("-0.0E0", -0.0f);
        assertF2sEquals("1.0E0", 1.0f);
        assertF2sEquals("-1.0E0", -1.0f);
        assertF2sEquals("NaN", Float.NaN);
        assertF2sEquals("INF", Float.POSITIVE_INFINITY);
        assertF2sEquals("-INF", Float.NEGATIVE_INFINITY);
        assertF2sEquals("1.1754944E-38", 1.1754944E-38f);
        assertF2sEquals("3.4028235E38", Float.intBitsToFloat(0x7f7fffff));
        assertF2sEquals("1.4E-45", Float.intBitsToFloat(1));
        assertF2sEquals("3.355445E7", 3.355445E7f);
        assertF2sEquals("9.0E9", 8.999999E9f);
        assertF2sEquals("3.436672E10", 3.4366717E10f);
        assertF2sEquals("3.0540412E5", 3.0540412E5f);
        assertF2sEquals("8.0990312E3", 8.0990312E3f);
        assertF2sEquals("2.4414062E-4", 2.4414062E-4f);
        assertF2sEquals("2.4414062E-3", 2.4414062E-3f);
        assertF2sEquals("4.3945312E-3", 4.3945312E-3f);
        assertF2sEquals("6.3476562E-3", 6.3476562E-3f);
        assertF2sEquals("4.7223665E21", 4.7223665E21f);
        assertF2sEquals("8.388608E6", 8388608.0f);
        assertF2sEquals("1.6777216E7", 1.6777216E7f);
        assertF2sEquals("3.3554436E7", 3.3554436E7f);
        assertF2sEquals("6.7131496E7", 6.7131496E7f);
        assertF2sEquals("1.9310392E-38", 1.9310392E-38f);
        assertF2sEquals("-2.47E-43", -2.47E-43f);
        assertF2sEquals("1.993244E-38", 1.993244E-38f);
        assertF2sEquals("4.1039004E3", 4103.9003f);
        assertF2sEquals("5.3399997E9", 5.3399997E9f);
        assertF2sEquals("6.0898E-39", 6.0898E-39f);
        assertF2sEquals("1.0310042E-3", 0.0010310042f);
        assertF2sEquals("2.882326E17", 2.8823261E17f);
        assertF2sEquals("7.038531E-26", 7.038531E-26f);
        assertF2sEquals("7.038531E-26", 7.0385309E-26f);
        assertF2sEquals("9.223404E17", 9.2234038E17f);
        assertF2sEquals("6.710887E7", 6.7108872E7f);
        assertF2sEquals("1.0E-44", 1.0E-44f);
        assertF2sEquals("2.816025E14", 2.816025E14f);
        assertF2sEquals("9.223372E18", 9.223372E18f);
        assertF2sEquals("1.5846086E29", 1.5846085E29f);
        assertF2sEquals("1.1811161E19", 1.1811161E19f);
        assertF2sEquals("5.368709E18", 5.368709E18f);
        assertF2sEquals("4.6143166E18", 4.6143165E18f);
        assertF2sEquals("7.812537E-3", 0.007812537f);
        assertF2sEquals("1.4E-45", 1.4E-45f);
        assertF2sEquals("1.18697725E20", 1.18697724E20f);
        assertF2sEquals("1.00014165E-36", 1.00014165E-36f);
        assertF2sEquals("2.0E2", 200.0f);
        assertF2sEquals("3.3554432E7", 3.3554432E7f);
        assertF2sEquals("6.7108864E17", Float.intBitsToFloat(0x5D1502F9));
        assertF2sEquals("1.3421773E18", Float.intBitsToFloat(0x5D9502F9));
        assertF2sEquals("2.6843546E18", Float.intBitsToFloat(0x5E1502F9));
        assertF2sEquals("1.0E0", 1.0f);
        assertF2sEquals("1.2E0", 1.2f);
        assertF2sEquals("1.23E0", 1.23f);
        assertF2sEquals("1.234E0", 1.234f);
        assertF2sEquals("1.2345E0", 1.2345f);
        assertF2sEquals("1.23456E0", 1.23456f);
        assertF2sEquals("1.234567E0", 1.234567f);
        assertF2sEquals("1.2345678E0", 1.2345678f);
        assertF2sEquals("1.23456735E-36", 1.23456735E-36f);
    }
}
