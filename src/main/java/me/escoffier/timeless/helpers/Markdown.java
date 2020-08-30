package me.escoffier.timeless.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Markdown {

    public static final Pattern LINK = Pattern.compile("\\[(?<text>.+)\\]\\((?<url>[^ ]+)(?: \"(?<title>.+)\")?\\)");

    public static boolean isMarkdownLink(String text) {
        return LINK.matcher(text).matches();
    }

    public static String getText(String text) {
        Matcher matcher = LINK.matcher(text);
        if (matcher.matches()) {
            return matcher.group("text");
        }
        return text;
    }

}
