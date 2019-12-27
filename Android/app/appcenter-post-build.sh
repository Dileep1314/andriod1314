#!/usr/bin/env bash

#!/usr/bin/env bash

# Post Build Script

set -e # Exit immediately if a command exits with a non-zero status (failure)

echo ""
echo "*****************************"
echo "Post Build Scripts"
echo "*****************************"
echo ""

gitBranch=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')

# Handle potentially null/missing App Center Env Variables
if [[ -z ${AGENT_JOBSTATUS+x} ]]; then jobStatus="Succeeded"; else jobStatus=${AGENT_JOBSTATUS}; fi
if [[ -z ${APPCENTER_SOURCE_DIRECTORY+x} ]]; then sourceDir="."; else sourceDir=${APPCENTER_SOURCE_DIRECTORY}; fi
if [[ -z ${APPCENTER_ANDROID_VARIANT+x} ]]; then variant="debug"; else variant=${APPCENTER_ANDROID_VARIANT}; fi
if [[ -z ${APPCENTER_BRANCH+x} ]]; then branch="${gitBranch}"; else branch=${APPCENTER_BRANCH}; fi
if [[ -z ${TEST_DEVICES+x} ]]; then testDevices="common"; else testDevices=${TEST_DEVICES}; fi
if [[ -z ${TEST_SERIES+x} ]]; then testSeries="master"; else testSeries=${TEST_SERIES}; fi

if [[ ${jobStatus} == "Succeeded" ]] && [[ ${variant} == "debug" ]]; then
    # variables
    appName="Electricity/My-ATCO-Electricity-1"
    deviceSet="Electricity/${testDevices}"
    buildDir="$sourceDir/app/build/outputs/apk/androidTest/debug"
    appPath="$sourceDir/app/build/outputs/apk/debug/app-debug.apk"

    echo "Run Espresso UITesting"
    echo ""
    echo "   App Name: $appName"
    echo " Device Set: $deviceSet"
    echo "Test Series: $testSeries"

    if [[ ! -f ${appPath} ]]; then
        echo ""
        echo "App Build Path not found! Running assembleDebug"
        ${sourceDir}/gradlew assembleDebug
    fi

    if [[ ! -f ${buildDir} ]]; then
        echo ""
        echo "Test Build Directory not found! Running assembleAndroidTest"
        ${sourceDir}/gradlew assembleAndroidTest
    fi
    echo ""

    # Note: Requires APPCENTER_ACCESS_TOKEN to be set in environment variables https://docs.microsoft.com/en-us/appcenter/cli/
    appcenter test run espresso --app ${appName} --devices ${deviceSet}  --app-path ${appPath} --test-series ${testSeries} --locale "en_US" --build-dir ${buildDir}

    echo "UI Tests Complete"
fi

merge() {

    # For some reason, App Center credentials are invalid here, despite being used earlier in build process. Must pass username and password for git transactions
    # &>/dev/null is used to obscure username and password from build output
    git remote set-url origin https://${APPCENTER_GIT_USERNAME}:${APPCENTER_GIT_PASSWORD}@bitbucket.org/ATCOCorporateDigitalPlatforms/outage-android.git/ &>/dev/null

    git fetch && git checkout $1
    git pull
    git fetch && git checkout $2
    git pull
    git merge $1 -m "Update ${2} branch from ${1} branch"
    git push
    git fetch && git checkout $1
}

if [[ ${jobStatus} == "Succeeded" ]] && [[ ! -z ${APPCENTER_GIT_PASSWORD} ]]; then
    echo ""
    echo "Git Merge Checks"
    echo "Current Branch is $branch"
    echo ""

    if [[ ${branch} == "debug" ]]; then
        echo "Merging to Alpha branch for testing"
        merge ${branch} alpha
    elif [[ ${branch} == "alpha" ]]; then
        echo "Merging to Beta branch for testing"
        merge ${branch} beta
    else echo "Branch does not need to be merged";
    fi
fi

echo ""
echo "*****************************"
echo "Post Build Scripts complete"
echo "*****************************"
echo ""