package us.abbies.b.tracerm.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

public class RegexMatcher extends TypeSafeMatcher<CharSequence> {
    private final Pattern pat;

    public RegexMatcher(Pattern pat) {
        this.pat = pat;
    }

    public static TypeSafeMatcher<CharSequence> matchesRegex(String regex) {
        return new RegexMatcher(Pattern.compile(regex));
    }

    @Override
    protected boolean matchesSafely(CharSequence item) {
        return pat.matcher(item).matches();
    }

    @Override
    protected void describeMismatchSafely(CharSequence item, Description mismatchDescription) {
        mismatchDescription.appendText("was \"").appendText(String.valueOf(item)).appendText("\"");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string with pattern /").appendText(String.valueOf(pat)).appendText("/");
    }
}
