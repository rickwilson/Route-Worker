package core.util;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.sql.Timestamp;
import java.util.regex.Pattern;

public class CheckData {

    private static final Pattern hasUppercase = Pattern.compile("[A-Z]");
    private static final Pattern hasLowercase = Pattern.compile("[a-z]");
    private static final Pattern hasNumber = Pattern.compile("\\d");
    private static final Pattern hasSpecialChar = Pattern.compile("[^a-zA-Z0-9 ]");
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 30;

    public static boolean isNumbersOnly(String value) {
        try {
            Long.parseLong(value);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isAlphaOnly(String value) {
        char[] chars = value.toCharArray();
        for (char c : chars) {
            if(!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public static String validatePasswords(String pass1, String pass2) {
        if (pass1 == null || pass2 == null) {
            return "One or both passwords are null.";
        }
        pass1 = pass1.trim();
        pass2 = pass2.trim();
        StringBuilder retVal = new StringBuilder();
        if (pass1.isEmpty() || pass2.isEmpty()) {
            return "One or both password fields are empty. ";
        }
        if (pass1.equals(pass2)) {
            if (pass1.length() < MIN_PASSWORD_LENGTH) {
                retVal.append("Password is too short. Needs to have 6 - 30 characters. ");
            }
            if (pass1.length() > MAX_PASSWORD_LENGTH) {
                retVal.append("Password is too long. Needs to have 6 - 30 characters. ");
            }
            if (!hasUppercase.matcher(pass1).find()) {
                retVal.append("Password needs an uppercase letter. ");
            }
            if (!hasLowercase.matcher(pass1).find()) {
                retVal.append("Password needs a lowercase letter. ");
            }
            if (!hasNumber.matcher(pass1).find()) {
                retVal.append("Password needs a number. ");
            }
            if (!hasSpecialChar.matcher(pass1).find()) {
                retVal.append("Password needs a special character i.e. !,@,#, etc.  ");
            }
        } else {
            System.out.println(pass1 + " != " + pass2);
            retVal.append("Passwords don't match.");
        }
        if (retVal.length() == 0) {
            retVal.append("good");
        }
        return retVal.toString();
    }

    public static int convertMoneyStringToInt(String moneyString) {
        if(moneyString == null || moneyString.trim().length() == 0) {
            moneyString = "0";
        }
        moneyString = moneyString.replaceAll("[^\\d.]", "");
        if(moneyString.contains(".")) {
            int pointlocation = moneyString.indexOf(".")+1;

            // greater than two decimal points
            if(moneyString.length() > pointlocation+2) {
                moneyString = moneyString.substring(0,pointlocation+2);
            }
            // two decimal point
            else if (moneyString.length() == pointlocation+2) {
                // do nothing
            }
            // one decimal point
            else if(moneyString.length() == pointlocation+1) {
                moneyString = moneyString+"0";
            }
            // it ends with a point
            else {
                moneyString = moneyString+"00";
            }
        } else {
            moneyString = moneyString+"00";
        }
        moneyString = moneyString.replaceAll("[^\\d]", "");
        return Integer.parseInt(moneyString);
    }

    public static int getRevShare(int revShare, int revPercent, String insPrice) {
        if(revShare > 0) {
            return revShare;
        } else {
            int rev = convertMoneyStringToInt(insPrice);
            if(rev > 0 && revPercent > 0) {
                return (rev*revPercent)/100;
            }
            return 0;
        }
    }

    public static int getRevShare(int revPercent, String insPrice) {
        int rev = convertMoneyStringToInt(insPrice);
        if(rev > 0 && revPercent > 0) {
            return (rev*revPercent)/100;
        }
        return 0;
    }

    public static int removeCentsFromInt(int rawInt) {
        String moneyString = rawInt+"";
        if(moneyString.trim().length()>2) {
            return Integer.parseInt(moneyString.substring(0,moneyString.length()-2));
        }
        return 0;
    }

    public static Timestamp convertKonnektiveDate(String konnektiveDate) {
        try {
            return Timestamp.valueOf(konnektiveDate);
        } catch (Exception tryOrderDate) {
            tryOrderDate.printStackTrace();
        }
        return null;
    }

    public static String addDecimal(int originalInt) {
        String formattedNumber = Integer.toString(originalInt);
        int numberSize = formattedNumber.length();
        if(numberSize < 2) {
            formattedNumber = "0.00";
        } else if (numberSize == 2) {
            formattedNumber = "0."+formattedNumber;
        } else {
            formattedNumber = new StringBuilder(formattedNumber).insert(numberSize-2, ".").toString();
        }
        return formattedNumber;
    }

    public static String getStartDate(String range) {
        return range.substring(0,range.indexOf("-")-1).trim();
    }

    public static String getEndDate(String range) {
        return range.substring(range.indexOf("-")+2).trim();
    }
}
