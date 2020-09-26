/*
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * BartDate.java
 *
 * Created on 5 maj 2006, 08:53
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package name.prokop.bart.fps.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author bart
 */
public class BartDate {

    public static Date getBeginingOfDay() {
        return getBeginingOfDay(new Date());
    }

    public static Date getEndOfDay() {
        return getEndOfDay(new Date());
    }

    public static Date getBeginingOfDay(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        myCal.set(Calendar.HOUR_OF_DAY, 0);
        myCal.set(Calendar.MINUTE, 0);
        myCal.set(Calendar.SECOND, 0);

        return myCal.getTime();
    }

    public static Date getEndOfDay(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        myCal.set(Calendar.HOUR_OF_DAY, 23);
        myCal.set(Calendar.MINUTE, 59);
        myCal.set(Calendar.SECOND, 59);
        myCal.set(Calendar.MILLISECOND, 999);

        return myCal.getTime();
    }

    /**
     * m zaczyna się od 1==Styczeń.
     * Czas kodowany jest jako 0:00
     * @param y Rok
     * @param m Miesiąc
     * @return Zakodowana data
     */
    public static Date getFirstDayOfMonth(int y, int m) {
        Calendar myCal = Calendar.getInstance();
        myCal.clear();
        myCal.set(Calendar.YEAR, y);
        myCal.set(Calendar.MONTH, m - 1);
        myCal.set(Calendar.DAY_OF_MONTH, 1);

        return myCal.getTime();
    }

    /**
     * m zaczyna się od 1==Styczeń.
     * Czas kodowany jest jako 0:00
     * @param y Rok
     * @param m Miesiąc
     * @return Zakodowana date
     */
    public static Date getLastDayOfMonth(int y, int m) {
        Calendar myCal = Calendar.getInstance();
        myCal.clear();
        myCal.set(Calendar.YEAR, y);
        myCal.set(Calendar.MONTH, m - 1);
        myCal.set(Calendar.DAY_OF_MONTH, myCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        return myCal.getTime();
    }

    /**
     * Encodes String as Date object
     * Time format is: "HH:mm"
     *
     * @param time to encode
     * @return encoded Date
     */
    public static Date encodeTime(String time) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm");
        try {
            return dateFormater.parse(time);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Encodes String as Date object
     * Time format is: "HH:mm"
     *
     * @param time to encode
     * @return encoded Date
     */
    public static Date encodeUTCTime(String time) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm");
        dateFormater.setTimeZone(TimeZone.getTimeZone(""));
        try {
            return dateFormater.parse(time);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Decodes formated date
     *
     * @param format date format
     * @param date as String
     * @return decoded date or null if unsuccessfull
     */
    public static Date parseDateChecked(String format, String date) {
        SimpleDateFormat dateFormater = new SimpleDateFormat(format);
        try {
            return dateFormater.parse(date);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Decodes formated date
     *
     * @param format date format
     * @param date as String
     * @return decoded date
     * @throws ParseException - when unable to parse date
     */
    public static Date parseDateUnchecked(String format, String date) throws ParseException {
        SimpleDateFormat dateFormater = new SimpleDateFormat(format);
        return dateFormater.parse(date);
    }

    /**
     * Converts string to date
     *
     * @param d date as YYYYMMDD
     * @return date extracted from string
     */
    public static Date encodeDate(String d) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd");
        try {
            return dateFormater.parse(d);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Converts string to date
     *
     * @param d date as YYYYMMDD
     * @return encided date
     */
    public static Date encodeUTCDate(String d) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd");
        dateFormater.setTimeZone(TimeZone.getTimeZone(""));
        try {
            return dateFormater.parse(d);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     *
     * @param d - timestamp as yyyyMMdd HH:mm
     * @return string representation of Date
     */
    public static Date encodeDateTime(String d) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd HH:mm");
        try {
            return dateFormater.parse(d);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    public static Date combine(Date day, Date time) {
        return new Date(day.getTime()+time.getTime());
    }

    public static boolean isBetween(Date date, Date from, Date to) {
        return (from.getTime() < date.getTime()) && (date.getTime() < to.getTime());
    }

    public static long ticksBetween(Date d1, Date d2) {
        return d2.getTime() - d1.getTime();
    }

    public static long daysBetween(Date d1, Date d2) {
        final long ONE_HOUR = 60 * 60 * 1000L;
        return ((d2.getTime() - d1.getTime() + ONE_HOUR) / (ONE_HOUR * 24));
    }

    public static Date getDayPartOfDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getTimePartOfDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.YEAR, 1970);
        c.set(Calendar.DAY_OF_YEAR, 1);
        return c.getTime();
    }

    public static String sqlDate(Date d) {
        if (d == null) {
            return "NULL";
        }

        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormater.format(d);
    }

    /**
     * HH:mm
     *
     * @param format format date will be written as
     * @param date date to be formatted
     * @return formated date
     */
    public static String getFormatedDate(String format, Date date) {
        SimpleDateFormat dateFormater = new SimpleDateFormat(format);
        return dateFormater.format(date);
    }

    public static void main(String[] argv) {
        System.out.println(new Date(0));
        System.out.println(new Date(Long.MAX_VALUE));
        System.out.println("1. " + encodeTime("00:00"));
        System.out.println("2. " + encodeTime("24:00"));
        System.out.println(encodeDate("20050403"));
        System.out.println(getFirstDayOfMonth(2006, 2));
        System.out.println(getLastDayOfMonth(2006, 2));
        System.out.println(daysBetween(getFirstDayOfMonth(2006, 1), getLastDayOfMonth(2006, 1)));
        System.out.println(getDayPartOfDate(new Date()));
        System.out.println(getBeginingOfDay());
        System.out.println(getEndOfDay());
        System.out.println(getFormatedDate("yyyy-M-d H:m:s", new Date()));
        System.out.println(parseDateChecked("yyyy-M-d H:m:s", "2081-1-2 1:2:3"));
    }
}
