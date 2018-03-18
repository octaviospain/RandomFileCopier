/******************************************************************************
 * Copyright 2016-2018 Octavio Calleya                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package com.transgressoft.commons.util;

import java.math.*;
import java.text.*;

/**
 * Class that converts a given amount of bytes into a textual representation.
 *
 * @author Octavio Calleya
 * @version 0.2.6
 */
public class ByteSizeRepresentation {

    private long bytes;

    /**
     * Default constructor
     *
     * @param bytes The {@code bytes} to be represented
     */
    public ByteSizeRepresentation(long bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns a {@link String} representing the given {@code bytes}, with a textual representation
     * depending if the given amount can be represented as KB, MB, GB or TB with no decimal part.
     *
     * @return The {@code String} that represents the given bytes
     *
     * @throws IllegalArgumentException Thrown if {@code bytes} is negative
     */
    public String withNoDecimals() {
        return withMaximumDecimals(0, RoundingMode.UNNECESSARY);
    }

    /**
     * Returns a {@link String} representing the given {@code bytes}, with a textual representation
     * depending if the given amount can be represented as KB, MB, GB or TB, limiting the number
     * of decimals, if there are any.
     *
     * @param numDecimals  The maximum number of decimals to put after the comma
     * @param roundingMode The {@code RoundingMode} of the decimals
     *
     * @return The {@code String} that represents the given bytes
     *
     * @throws IllegalArgumentException Thrown if {@code bytes} or {@code numDecimals} are negative
     */
    public String withMaximumDecimals(int numDecimals, RoundingMode roundingMode) {
        if (numDecimals < 0)
            throw new IllegalArgumentException("Given number of decimals can't be less than zero");

        String byteSizeString = withAllDecimals();
        StringBuilder decimalSharps = new StringBuilder();
        for (int n = 0; n < numDecimals; n++)
            decimalSharps.append("#");
        DecimalFormat decimalFormat = new DecimalFormat("#." + decimalSharps.toString());
        if (roundingMode != RoundingMode.UNNECESSARY)
            decimalFormat.setRoundingMode(roundingMode);

        int unitPos = byteSizeString.lastIndexOf(' ');
        String stringValue = byteSizeString.substring(0, unitPos);
        stringValue = stringValue.replace(',', '.');
        float floatValue = Float.parseFloat(stringValue);
        String dFo = decimalFormat.format(floatValue);
        if (dFo.charAt(dFo.length() - 1) == ',')
            dFo = dFo.substring(0, dFo.length() - 1);
        byteSizeString = dFo + byteSizeString.substring(unitPos);
        return byteSizeString;
    }

    /**
     * Returns a {@link String} representing the given {@code bytes}, with a textual representation
     * depending if the given amount can be represented as KB, MB, GB or TB with all the necessary decimals.
     *
     * @return The {@code String} that represents the given bytes
     *
     * @throws IllegalArgumentException Thrown if {@code bytes} or {@code numDecimals} are negative
     */
    public String withAllDecimals() {
        if (bytes < 0)
            throw new IllegalArgumentException("Given bytes can't be less than zero");

        String sizeText;
        String[] bytesUnits = {"B", "KB", "MB", "GB", "TB"};
        long bytes = this.bytes;
        short binRemainder;
        float decRemainder = 0;
        int unit;
        for (unit = 0; bytes > 1024 && unit < bytesUnits.length; unit++) {
            bytes /= 1024;
            binRemainder = (short) (bytes % 1024);
            decRemainder += Float.valueOf((float) binRemainder / 1024);
        }
        String remainderStr = String.format("%f", decRemainder).substring(2);
        sizeText = bytes + ("0".equals(remainderStr) ? "" : "," + remainderStr) + " " + bytesUnits[unit];
        return sizeText;
    }
}
