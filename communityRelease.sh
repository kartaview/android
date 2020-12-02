##Root
rm osv.keystore
#Dependencies remove
rm volley/buildRelease.gradle
rm spherical/buildRelease.gradle
rm sensorlib/buildRelease.gradle
rm scalablevideoview/buildRelease.gradle
rm photoview/buildRelease.gradle
rm eventbus/buildRelease.gradle
rm dotindicator/buildRelease.gradle
rm connectivity/buildRelease.gradle
rm -r utilities
#App
APP=app/
rm "${APP}google-services.json"
#App sources
APP_SRC="${APP}src/"
rm -r "${APP_SRC}prod"
rm -r "${APP_SRC}mock"
#App resources
rm "${APP_SRC}main/res/values/api_keys.xml"
