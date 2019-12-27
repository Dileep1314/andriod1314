package outage.atco.outageandroid.screens


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.microsoft.appcenter.espresso.Factory
import com.microsoft.appcenter.espresso.ReportHelper
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import outage.atco.outageandroid.R


@SmallTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant("android.permission.ACCESS_FINE_LOCATION")

    @Rule
    @JvmField
    var reportHelper: ReportHelper = Factory.getReportHelper()

    @Before
    fun startUp() {
        reportHelper.label("Main Activity Test Started")
    }

    @Test
    fun mainActivityTest() {

        // Header Exists and Starts with Locations
        val textView = onView(
                allOf(withId(R.id.mainTitle), withText("Locations"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView.check(matches(withText("Locations")))

        val imageButton = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.mainRightButtonsContainer),
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        1)),
                        0),
                        isDisplayed()))
        imageButton.check(matches(isDisplayed()))

        // View starts with Map View Open
        val view = onView(
                allOf(withContentDescription("Google Map"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java),
                                        0),
                                0),
                        isDisplayed()))
        view.check(matches(isDisplayed()))

        // Nav Bar Exists with Correct Tabs
        val frameLayout2 = onView(
                allOf(withId(R.id.navigation_map), withContentDescription("Locations"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                0),
                        isDisplayed()))
        frameLayout2.check(matches(isDisplayed()))

        val frameLayout3 = onView(
                allOf(withId(R.id.navigation_reports), withContentDescription("Reports"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                1),
                        isDisplayed()))
        frameLayout3.check(matches(isDisplayed()))

        val frameLayout4 = onView(
                allOf(withId(R.id.navigation_learn), withContentDescription("Learn"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                2),
                        isDisplayed()))
        frameLayout4.check(matches(isDisplayed()))

        val frameLayout5 = onView(
                allOf(withId(R.id.navigation_settings), withContentDescription("Settings"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                3),
                        isDisplayed()))
        frameLayout5.check(matches(isDisplayed()))

        // Tab Click Events Go To Correct Tabs
        val bottomNavigationItemView = onView(
                allOf(withId(R.id.navigation_reports), withContentDescription("Reports"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                1),
                        isDisplayed()))
        bottomNavigationItemView.perform(click())

        val textView1 = onView(
                allOf(withId(R.id.mainTitle), withText("Reports"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView1.check(matches(withText("Reports")))

        val bottomNavigationItemView3 = onView(
                allOf(withId(R.id.navigation_learn), withContentDescription("Learn"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                2),
                        isDisplayed()))
        bottomNavigationItemView3.perform(click())

        val textView2 = onView(
                allOf(withId(R.id.mainTitle), withText("Learn & Explore"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView2.check(matches(withText("Learn & Explore")))

        val bottomNavigationItemView4 = onView(
                allOf(withId(R.id.navigation_settings), withContentDescription("Settings"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                3),
                        isDisplayed()))
        bottomNavigationItemView4.perform(click())

        val textView3 = onView(
                allOf(withId(R.id.mainTitle), withText("Settings"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView3.check(matches(withText("Settings")))

        val bottomNavigationItemView2 = onView(
                allOf(withId(R.id.navigation_map), withContentDescription("Locations"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.mainTabs),
                                        0),
                                0),
                        isDisplayed()))
        bottomNavigationItemView2.perform(click())

        val textView4 = onView(
                allOf(withId(R.id.mainTitle), withText("Locations"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView4.check(matches(withText("Locations")))

    }

    @After
    fun tearDown() {
        reportHelper.label("Main Activity Test Complete")
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
