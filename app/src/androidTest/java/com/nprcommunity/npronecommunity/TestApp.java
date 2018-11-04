package com.nprcommunity.npronecommunity;

import android.Manifest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import okreplay.AndroidTapeRoot;
import okreplay.OkReplay;
import okreplay.OkReplayConfig;
import okreplay.OkReplayRuleChain;
import okreplay.TapeMode;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class TestApp {

    private final ActivityTestRule<Navigate> activityTestRule =
            new ActivityTestRule<>(Navigate.class);

//    @Test
//    public void test_add_to_queue() {
        // Type text and then press the button.
//        onView(withId(R.id.editTextUserInput))
//                .perform(typeText(mStringToBetyped), closeSoftKeyboard());
//        onView(withId(R.id.changeTextBt)).perform(click());
//
//        // Check that the text was changed.
//        onView(withId(R.id.textToBeChanged))
//                .check(matches(withText(mStringToBetyped)));
//    }

    public static Matcher<View> isNotEqualText(final CharSequence value){
        return new TypeSafeMatcher<View>() {

            @Override
            protected boolean matchesSafely(View item) {
                if(!(item instanceof TextView)) return false;
                return !((TextView) item).getText().equals(value);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Check text is wrong");
            }
        };
    }

    public static Matcher<View> isEqualText(final CharSequence value){
        return new TypeSafeMatcher<View>() {

            @Override
            protected boolean matchesSafely(View item) {
                if(!(item instanceof TextView)) return false;
                return ((TextView) item).getText().equals(value);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Check text is wrong");
            }
        };
    }

    private final OkReplayConfig configuration = new OkReplayConfig.Builder()
            .tapeRoot(new AndroidTapeRoot(getContext(), getClass()))
            .defaultMode(TapeMode.READ_WRITE) // or TapeMode.READ_ONLY
            .sslEnabled(true)
            .interceptor(Config.OK_HTTP_REPLAY_INTERCEPTOR)
            .build();
    @Rule public final TestRule testRule =
            new OkReplayRuleChain(configuration, activityTestRule).get();

    @Test
    @OkReplay
    public void test_init_has_internet_and_fully_downloaded_media() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.current_song_text))
                .check(matches(isNotEqualText("Unknown")));

        onView(withId(R.id.button_rewind)).check(matches(isEnabled()));

        onView(withId(R.id.button_pause_play)).check(matches(isEnabled()));

        onView(withId(R.id.button_next)).check(matches(isEnabled()));
    }

    @ClassRule
    public static GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    );
}