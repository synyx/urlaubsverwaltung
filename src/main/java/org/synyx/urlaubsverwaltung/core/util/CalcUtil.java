package org.synyx.urlaubsverwaltung.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * Contains helper methods for handling {@link java.math.BigDecimal}s.
 *
 * @author  Aljona Murygina
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalcUtil {

    public static boolean isZero(BigDecimal number) {

        /**
         * NOTE: {@link java.math.BigDecimal#signum()} returns:
         * -1 if the number is negative,
         * 0 if the number is zero,
         * 1 if the number is positive
         */
        return number.signum() == 0;
    }


    public static boolean isNegative(BigDecimal number) {

        return number.signum() == -1;
    }


    public static boolean isPositive(BigDecimal number) {

        return number.signum() == 1;
    }
}
