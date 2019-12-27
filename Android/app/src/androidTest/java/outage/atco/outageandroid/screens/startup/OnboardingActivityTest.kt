package outage.atco.outageandroid.screens.startup


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.microsoft.appcenter.espresso.Factory
import com.microsoft.appcenter.espresso.ReportHelper
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import outage.atco.outageandroid.R

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(OnboardingActivity::class.java)

    @Rule
    @JvmField
    var reportHelper: ReportHelper = Factory.getReportHelper()

    @Before
    fun startUp() {
        reportHelper.label("Onboarding Activity Test Started")
    }

    @Test
    fun onboardingActivityTest() {
        val imageView = onView(
                allOf(withId(R.id.onboardingImage), withContentDescription("OUTAGE MAP"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                0),
                        isDisplayed()))
        imageView.check(matches(isDisplayed()))

        val textView = onView(
                allOf(withId(R.id.onboardingTitle), withText("OUTAGE MAP"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                2),
                        isDisplayed()))
        textView.check(matches(withText("OUTAGE MAP")))

        val textView2 = onView(
                allOf(withId(R.id.onboardingText), withText("The outage map displays active electric outages throughout ATCO service areas. Tap a marker to zoom to the outage area."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                3),
                        isDisplayed()))
        textView2.check(matches(withText("The outage map displays active electric outages throughout ATCO service areas. Tap a marker to zoom to the outage area.")))

        val button = onView(
                allOf(withId(R.id.exitTourButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        button.check(matches(isDisplayed()))

        onView(withId(R.id.onboardingRecycler)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(1))

        val imageView2 = onView(
                allOf(withId(R.id.onboardingImage), withContentDescription("OUTAGE DETAILS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                0),
                        isDisplayed()))
        imageView2.check(matches(isDisplayed()))

        val textView3 = onView(
                allOf(withId(R.id.onboardingTitle), withText("OUTAGE DETAILS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                2),
                        isDisplayed()))
        textView3.check(matches(withText("OUTAGE DETAILS")))

        val textView4 = onView(
                allOf(withId(R.id.onboardingText), withText("Tap on the outage marker to view the estimated repair time and current status for the outage."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                3),
                        isDisplayed()))
        textView4.check(matches(withText("Tap on the outage marker to view the estimated repair time and current status for the outage.")))

        onView(withId(R.id.onboardingRecycler)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(2))

        val imageView3 = onView(
                allOf(withId(R.id.onboardingImage), withContentDescription("REPORT SERVICE ISSUES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                0),
                        isDisplayed()))
        imageView3.check(matches(isDisplayed()))

        val textView5 = onView(
                allOf(withId(R.id.onboardingTitle), withText("REPORT SERVICE ISSUES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                2),
                        isDisplayed()))
        textView5.check(matches(withText("REPORT SERVICE ISSUES")))

        val textView6 = onView(
                allOf(withId(R.id.onboardingText), withText("Press for 3 seconds on the map to drop a pin on a specific location, or by using the search. \n\nThen select an option to report a service outage, street light issue, or general problem. \nOr save as a point-of-interest."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                3),
                        isDisplayed()))
        textView6.check(matches(withText("Press for 3 seconds on the map to drop a pin on a specific location, or by using the search. \n\nThen select an option to report a service outage, street light issue, or general problem. \nOr save as a point-of-interest.")))

        onView(withId(R.id.onboardingRecycler)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(3))

        val imageView4 = onView(
                allOf(withId(R.id.onboardingImage), withContentDescription("OUTAGE NOTIFICATIONS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                0),
                        isDisplayed()))
        imageView4.check(matches(isDisplayed()))

        val textView7 = onView(
                allOf(withId(R.id.onboardingTitle), withText("OUTAGE NOTIFICATIONS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                2),
                        isDisplayed()))
        textView7.check(matches(withText("OUTAGE NOTIFICATIONS")))

        val textView8 = onView(
                allOf(withId(R.id.onboardingText), withText("Get notified of active outages that impact locations you care about via email, text message, and/or notifications.\n\nStay informed of new outages, current stations and estimated resolution."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.onboardingRecycler),
                                        0),
                                3),
                        isDisplayed()))
        textView8.check(matches(withText("Get notified of active outages that impact locations you care about via email, text message, and/or notifications.\n\nStay informed of new outages, current stations and estimated resolution.")))
    }

    @After
    fun tearDown() {
        reportHelper.label("Onboarding Activity Test Complete")
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
