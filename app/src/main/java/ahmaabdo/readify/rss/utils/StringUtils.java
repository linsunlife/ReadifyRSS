/**
 * spaRSS
 * <p/>
 * Copyright (c) 2015-2016 Arnaud Renaud-Goud
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ahmaabdo.readify.rss.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ahmaabdo.readify.rss.MainApplication;

public class StringUtils {

    static public String getDateTimeString(long timestamp) {
        Calendar calTimestamp = Calendar.getInstance();
        calTimestamp.setTimeInMillis(timestamp);
        Calendar calCurrent = Calendar.getInstance();
        Locale locale = MainApplication.getContext().getResources().getConfiguration().locale;
        String format;
        if (calCurrent.get(Calendar.YEAR) != calTimestamp.get(Calendar.YEAR)) {
            format = android.text.format.DateFormat.getBestDateTimePattern(locale, "d MMM yyyy HH:mm");
        } else if (calCurrent.get(Calendar.MONTH) != calTimestamp.get(Calendar.MONTH) || calCurrent.get(Calendar.DAY_OF_MONTH) != calTimestamp.get(Calendar.DAY_OF_MONTH)) {
            format = android.text.format.DateFormat.getBestDateTimePattern(locale, "d MMM HH:mm");
        } else {
            format = "HH:mm";
        }
        return new SimpleDateFormat(format, locale).format(new Date(timestamp));
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
